package actors;

import actors.messages.AstSubmission;
import actors.messages.AstrometryResultData;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.io.*;

/**
 * Root actor for Astrometry submissions.
 */
public class AstrometryActor extends UntypedActor {;

    // Actor to login and get session key
    private final ActorRef loginActor;
    private final ActorRef astrometryJobStatusActor;
    private final ActorRef databaseActor;


    // Actor to submit image (with session key), get submission id.

    // Actor to get job id (with submission id)

    // Actor to get annotation data (with job id)

    public AstrometryActor(ActorRef databaseActor) {
        Logger.info("Starting AstrometryActor.");
        this.loginActor = getContext().actorOf(Props.create(AstrometryLoginActor.class), "loginActor");
        this.astrometryJobStatusActor = getContext().actorOf(Props.create(AstrometryJobStatusActor.class, databaseActor), "astrometryJobStatusActor");
        this.databaseActor = databaseActor;
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof AstSubmission) {
            final AstSubmission submissionData = (AstSubmission) message;

            //loginActor.tell(loginKey, getSelf());
            // Alternatively, we can ask() loginActor for key, get a future, and pipe it to the next actor.
            // Pipe is preferred over Await.result() or Await.ready() because the latter two will block.
            final Future<Object> sessionKey = Patterns.ask(loginActor, submissionData.getLoginKey(), 100000); // Must use Future<Object>, cast later if needed.

//            final Future<String> submissionId = sessionKey.flatMap(new Mapper<Object, Future<String>>() {
//                @Override
//                public Future<String> apply(Object sessionKey) {
//                    Logger.debug("From onReceive: " + sessionKey);
//                    Future<String> subId = submitImage(submissionData.getFile(), (String) sessionKey);
//                    return subId;
//                }
//            }, Akka.system().dispatcher());
//
//            Future<AstrometryResultData> astData = submissionId.map(new Mapper<String, AstrometryResultData>() {
//                @Override
//                public AstrometryResultData apply(String subId) {
//                    AstrometryResultData data = new AstrometryResultData(submissionData.getStarMapId(), "asdf", subId, null, null);
//                    return data;
//                }
//            }, Akka.system().dispatcher());
//
//            Patterns.pipe(astData, Akka.system().dispatcher()).to(astrometryJobStatusActor);



            final Future<AstrometryResultData> astData = sessionKey.flatMap(new Mapper<Object, Future<AstrometryResultData>>() {
                @Override
                public Future<AstrometryResultData> apply(Object sessionKey) {
                    Future<String> subId = submitImage(submissionData.getFile(), (String) sessionKey);
                    return subId.map(new Mapper<String, AstrometryResultData>() {
                        @Override
                        public AstrometryResultData apply(String subId) {
                            AstrometryResultData data = new AstrometryResultData(submissionData.getStarMapId(), (String) sessionKey, subId, null, null);
                            return data;
                        }
                    }, Akka.system().dispatcher());
                }
            }, Akka.system().dispatcher());

            Patterns.pipe(astData, Akka.system().dispatcher()).to(astrometryJobStatusActor);

        } else {
            unhandled(message);
        }

    }

    /**
     *
     * @param image
     * @param sessionKey
     * @return A Future holding the Astrometry submission ID for the given image.
     */
    public Future<String> submitImage(File image, String sessionKey) {
        // Create empty promise, which will hold the upload status. We will return its future.
        final Promise<String> promise = Futures.promise();

        //Prepare Json data required for image submission. Sets submission options.
        JsonNode json = Json.newObject()
                .put("publicly_visible", "n")
                .put("allow_modifications", "d")
                .put("session", sessionKey)
                .put("allow_commercial_use", "d");

        //Manually create multipart/form-data POST request body because current version of Play doesn't support it...
        //Got most information from here: http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.2
        //https://www.ietf.org/rfc/rfc2388.txt
        //String outputFilePath = Play.application().path().getPath() + "/app/utils/tempPostRequestBody.dat";
        File tempPostRequestBodyFile = new File("tempPostRequestBody.dat"); //We'll delete this temp file after we submit post request.

        String boundary = null; //Boundary is used to separate the parts of the multipart request.
        try {
            //Because we're working with multipart/form-data, we need to create an output stream containing both
            //character and binary data. To accomplish this, we use a regular OutputStream as well as a OutputStreamWriter.
            //The OutputStreamWriter allows us to write character streams into [File]OutputStream, which is a byte stream.
            //When we need to write characters/text into the output stream, write into the OutputStreamWriter.
            //When we need to write binary data into the output stream (eg. images), write into the FileOutputStream.
            OutputStream bos = new BufferedOutputStream(new FileOutputStream(tempPostRequestBodyFile));
            Writer writer = new BufferedWriter(new OutputStreamWriter(bos, "UTF8"));

            boundary = "Um1Q5dLk29gluLz3T"; //Boundary is used to separate the parts of the multipart request.
            writer.write("--" + boundary + "\r\n");
            writer.write("Content-Disposition: form-data; name=\"request-json\"\r\n");
            writer.write("Content-Type: application/json\r\n\r\n"); //Note: Change to text/plain if this doesn't work.
            writer.write(json + "\r\n");
            writer.write("--" + boundary + "\r\n");
            writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"" + image.getName() + "\"\r\n");
            writer.write("Content-Type: application/octet-stream\r\n\r\n");
            writer.flush();

            //Write the image data into the output stream.
            InputStream fis = new BufferedInputStream(new FileInputStream(image));
            byte[] buf = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = fis.read(buf)) != -1) { //Read data from input stream into buf. Returns # of bytes read.
                bos.write(buf, 0, bytesRead); //Then write the buf data into output stream.
            }
            bos.flush();
            fis.close();

            writer.flush();
            writer.write("\r\n");
            writer.write("--" + boundary + "--");
            writer.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //With the post request body prepared, we can now send it.
        WS.WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/upload");
        holder.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
        holder.setHeader("Content-Length", String.valueOf(tempPostRequestBodyFile.length())); //Content-Length refers to the size of the post request BODY.

        F.Promise<WS.Response> responsePromise = holder.post(tempPostRequestBodyFile); //Send file contents.

        // Change Promise to Future before transforming... just because I prefer to work with Akka Future rather than
        // Play Promise. Interesting side-note: The Play Promise map() function did not require a dispatcher argument.
        Future<WS.Response> responseFuture = responsePromise.wrapped();

        // No need to create a variable here because we only care about the Promise.
        responseFuture.map(new Mapper<WS.Response, JsonNode>() {
            @Override
            public JsonNode apply(WS.Response response) {
                // Json response will look something like this:
                // {"status":"success","subid":682097,"hash":"6644e79a1023a073d90dbc1f85b19e03296c8d83"}
                JsonNode json = response.asJson();

                if (json.findPath("status").asText().equals("success")) {
                    String submissionId = json.findPath("subid").asText();
                    Logger.debug("[AstrometryActor] - Image submission successful! Response: " + json);
                    promise.success(submissionId);
                } else {
                    Logger.debug("[AstrometryActor] - Image submission failed! Response: " + json);
                    promise.failure(new RuntimeException("Image submission failed."));
                }

                return json;
            }
        }, Akka.system().dispatcher());


        tempPostRequestBodyFile.delete(); //Delete the tempPostRequestBody.dat file after we're done with it.

        return promise.future();
    }

}

