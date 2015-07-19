package actors;

import actors.messages.*;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import play.Logger;
import play.libs.Akka;
import scala.concurrent.Future;

/**
 * Starting root actor for all Starmap image submissions.
 */
public class StarmapSubmissionActor extends UntypedActor {

    private final ActorRef databaseActor;
    private final ActorRef awsUploadActor;
    private final ActorRef astrometryActor;

    public StarmapSubmissionActor() {
        Logger.info("Starting StarmapSubmissionActor.");
        this.databaseActor = getContext().actorOf(Props.create(DatabaseActor.class), "databaseActor");
        this.awsUploadActor = getContext().actorOf(Props.create(AwsUploadActor.class, databaseActor), "awsUploadActor");
        this.astrometryActor = getContext().actorOf(Props.create(AstrometryActor.class, databaseActor), "astrometryActor");
    }

    @Override
    public void onReceive(Object message) {

        if (message instanceof StarmapSubmission) {
            final StarmapSubmission submissionData = (StarmapSubmission) message;

            // Message to be sent to databaseActor.
            final StarmapCreate starmapCreate = new StarmapCreate(submissionData.getUser());

            // Create new StarMap database entry using databaseActor child.
            final Future<Object> starMapId = Patterns.ask(databaseActor, starmapCreate, 100000);
            // Once the starMapId Future completes, we will use its returned StarMap ID value to concurrently create two
            // other Futures. One to create an AwsUpload message for the awsUploadActor, and another to create the
            // AstSubmission message for the AstSubmissionActor.
            // The AwsUploadActor and AstSubmissionActor will be running concurrently.


            // Transform the id to AwsUpload. Note that id is an up-casted Long. This is because the Patterns.ask()
            // method forcibly returns an Object.
            final Future<AwsUpload> awsUploadMsg = starMapId.map(new Mapper<Object, AwsUpload>() {
                @Override
                public AwsUpload apply(Object id) {
                    return new AwsUpload(submissionData.getFile(), submissionData.getFileName(), (Long) id);
                }
            }, Akka.system().dispatcher());

            // Pipe() is equivalent to registering an onComplete callback on the future and sending its result to an actor.
            // Note that the starMapId variable name is a bit misleading now, as it has been transformed to
            // an AwsUpload object at this point.
            Patterns.pipe(awsUploadMsg, Akka.system().dispatcher()).to(awsUploadActor);


            // Create another Future, transforming the starMapId to AstSubmission, which we send as a message
            // to the Astrometry actor to get the submission process rolling.
            final Future<AstSubmission> astrometryMessage = starMapId.map(new Mapper<Object, AstSubmission>() {
                @Override
                public AstSubmission apply(Object id) {
                    AstLoginKey loginKey = new AstLoginKey(submissionData.getLoginKey());
                    return new AstSubmission(submissionData.getFile(), loginKey, (Long) id);
                }
            }, Akka.system().dispatcher());

            Patterns.pipe(astrometryMessage, Akka.system().dispatcher()).to(astrometryActor);


//            // Alternatively to the map transform and piping we do above, we could do something like this with
//            // the onSuccess() callback. But I think the above is the better way to do things.
//            starMapId.onSuccess(new OnSuccess<Object>() {
//                public void onSuccess(Object id) {
//                    AwsUpload awsUploadMessage = new AwsUpload(submissionData.getFile(), (Long) id);
//                    awsUploadActor.tell(awsUploadMessage, getSelf()); // Is getSelf() safe here? Race conditions?
//                    Logger.debug("[StarmapSubmissionActor] StarMap creation callback result: " + id); // id is a long.
//                }
//            }, Akka.system().dispatcher());

        } else {
            unhandled(message);
        }
    }
}
