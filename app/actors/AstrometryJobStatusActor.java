package actors;

import actors.messages.AstrometryResultData;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * Actor responsible for polling Astrometry for job status information.
 */
public class AstrometryJobStatusActor extends UntypedActor {
    private final ActorRef annotationsActor;
    private final ActorRef databaseActor;

    public AstrometryJobStatusActor(ActorRef databaseActor) {
        Logger.info("Starting AstrometryJobStatusActor.");
        this.annotationsActor = getContext().actorOf(Props.create(AstrometryAnnotationsActor.class, databaseActor), "annotationsActor");
        this.databaseActor = databaseActor;
    }

    @Override
    public void onReceive(Object message) {
        ActorRef self = getSelf(); // Use instead of getSelf() to be safe.

        if (message instanceof AstrometryResultData) {
            AstrometryResultData data = (AstrometryResultData) message;

            if (data.getAstJobId() == null) {
                Future<String> jobId = fetchJobId(data.getAstSessionKey(), data.getAstSubmissionId());
                jobId.onComplete(new OnComplete<String>() {
                    @Override
                    public void onComplete(Throwable failure, String jobId) throws Throwable {
                        if (failure != null) {
                            // Fail
                        } else if (jobId.equals("processing")) {
                            Akka.system().scheduler().scheduleOnce(Duration.create(5, TimeUnit.SECONDS), self, data, Akka.system().dispatcher(), null);
                        } else {
                            // JobId received!. Create new AstrometryResultData containing the newly found Job ID.
                            AstrometryResultData newData = new AstrometryResultData(data.getStarMapId(),
                                    data.getAstSessionKey(), data.getAstSubmissionId(), jobId, null);

                            Akka.system().scheduler().scheduleOnce(Duration.create(5, TimeUnit.SECONDS), self, newData, Akka.system().dispatcher(), null);
                        }
                    }
                }, Akka.system().dispatcher());

            } else { // Have JobId, now query job status.
                // Now need to query the job status, and when solved, can grab annotation with jobId as well.
                // Query Status with jobId, and also get annotations with jobId.

                Future<String> jobStatus = checkJobStatus(data.getAstSessionKey(), data.getAstJobId());

                jobStatus.onComplete(new OnComplete<String>() {
                    public void onComplete(Throwable failure, String jobStatus) throws Throwable {
                        if (failure != null) {
                            // Fail
                        } else {
                            switch (jobStatus) {
                                case "success":
                                    Logger.debug("[AstrometryJobStatusActor] - Job " + data.getAstJobId() + " has been successfully processed!");
                                    annotationsActor.tell(data, self);
                                    break;
                                case "solving":
                                    Logger.debug("[AstrometryJobStatusActor] - Job " + data.getAstJobId() + " is still being processed. Checking again...!");
                                    Akka.system().scheduler().scheduleOnce(Duration.create(5, TimeUnit.SECONDS), self, data, Akka.system().dispatcher(), null);
                                    break;
                                case "failure":
                                    Logger.debug("[AstrometryJobStatusActor] - Job " + data.getAstJobId() + " was unable to be processed.");
                                    break;
                                default:
                                    Logger.debug("[AstrometryJobStatusActor] - Unknown job status. This shouldn't happen. Job #" + data.getAstJobId());
                                    break;
                            }
                        }
                    }
                }, Akka.system().dispatcher());

            }

        } else {
            unhandled(message);
        }
    }

    public Future<String> fetchJobId(String sessionKey, String submissionId) {
        Promise<String> promise = Futures.promise();

        //Create Json object required for post request. {"session":"given key"}
        JsonNode sessionKeyJson = Json.newObject().put("session", sessionKey);

        // Astrometry's "submission status" api call url.
        WS.WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/submissions/" + submissionId).setContentType("application/x-www-form-urlencoded");

        // Submit
        F.Promise<WS.Response> responsePromise = holder.post("request-json=" + sessionKeyJson);
        
        // Change from Play Promise to Akka Future because I just prefer to work with Futures here.
        Future<WS.Response> responseFuture = responsePromise.wrapped();

        // No need to create new variable here because we only care about the Promise.
        responseFuture.map(new Mapper<WS.Response, JsonNode>() {
            @Override
            public JsonNode apply(WS.Response response) {
                // Json result will look like this, if called 3 times at 5 second intervals.
                // Note the evolution of the "jobs" array, and how it takes time for Astrometry to generate an actual Job ID value for the submission.
                // {"processing_started":"None","job_calibrations":[],"jobs":[],"processing_finished":"None","user":4616,"user_images":[]}
                // {"processing_started":"2015-07-05 00:25:06.496690","job_calibrations":[],"jobs":[null],"processing_finished":"2015-07-05 00:25:06.559952","user":4616,"user_images":[715293]}
                // {"processing_started":"2015-07-05 00:25:06.496690","job_calibrations":[],"jobs":[1154374],"processing_finished":"2015-07-05 00:25:06.559952","user":4616,"user_images":[715293]}
                JsonNode json = response.asJson();

                // The "jobs" key/value pair in the Json response holds a 1-element array containing the Job ID.
                // Initially will be null, or (for some reason) the actual string "null", until the
                // API has processed the image submission and gives us an actual Job ID value, which we grab.
                JsonNode jobsArray = json.findValue("jobs");

                // Initially, {..., "jobs":[], ...}, OR {..., "jobs":[null], ...}. So we need to check for that.
                if (jobsArray.has(0) && !jobsArray.get(0).isNull()) {
                    // Grab the Job ID.
                    String jobId = json.findPath("jobs").get(0).asText();
                    Logger.debug("[AstrometryJobStatusActor] - JobId found for submission #" + submissionId + ": " + jobId);
                    promise.success(jobId);
                } else {
                    Logger.debug("[AstrometryJobStatusActor] - Submission #" + submissionId + " is still being processed.");
                    // Note how we still mark this as a success, not a failure, because we actually expect the
                    // submission to take some time to finish processing.
                    promise.success("processing");
                }

                return json;
            }
        }, Akka.system().dispatcher());

        return promise.future();
    }


    /**
     * Calls Astrometry API once to get the current job status.
     *
     * @param sessionKey The Astrometry session key to use.
     * @param jobId The Astrometry job ID to check.
     * @return The current job status - either "success", "failure", or "solving".
     */
    public Future<String> checkJobStatus(String sessionKey, String jobId) {

        //Create Json object required for post request. {"session":"given key"}
        JsonNode sessionKeyJson = Json.newObject().put("session", sessionKey);

        // Astrometry's "job status" api call url.
        WS.WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/jobs/" + jobId).setContentType("application/x-www-form-urlencoded");

        F.Promise<WS.Response> responsePromise = holder.post("request-json=" + sessionKeyJson);

        // Prefer to work with Akka Future rather than Play Promise.
        Future<WS.Response> responseFuture = responsePromise.wrapped();

        Future<String> jobStatus = responseFuture.map(new Mapper<WS.Response, String>() {
            @Override
            public String apply(WS.Response response) {
                // Json result looks like: {"status": "solving"}. Can also be "success" or "failure".
                JsonNode json = response.asJson();
                String jobStatus = json.findPath("status").asText();
                Logger.debug("[AstrometryJobStatusActor] - Job status for Job ID #" + jobId + ": " + jobStatus);
                return jobStatus;
            }
        }, Akka.system().dispatcher());

        return jobStatus;
    }



}
