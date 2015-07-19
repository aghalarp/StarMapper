package actors;

import actors.messages.AwsUpload;
import actors.messages.StarmapUpdateAWS;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.dispatch.OnComplete;
import play.Logger;
import play.libs.Akka;
import scala.concurrent.Future;
import utils.AwsS3Utils;

import java.io.File;

/**
 * Actor responsible for uploading images to Amazon AWS S3.
 */
public class AwsUploadActor extends UntypedActor {

    private final ActorRef databaseActor;

    public AwsUploadActor(ActorRef databaseActor) {
        Logger.info("Starting AwsUploadActor.");
        this.databaseActor = databaseActor;
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof AwsUpload) {
            final AwsUpload submission = (AwsUpload) message;

            // Rename file with StarMap Id prepended to file name. Lazy way to ensure uniqueness.
            File renamedFile = AwsS3Utils.renameFile(submission.getFile(), submission.getStarMapId() + "-" + submission.getFileName());

            // Upload to S3. Eventually get back url (or exception).
            Future<String> urlResult = AwsS3Utils.uploadFileAsync(renamedFile);

            urlResult.onComplete(new OnComplete<String>() {
                public void onComplete(Throwable failure, String url) {
                    if (failure != null) {
                        Logger.debug("[AwsUploadActor] - " + renamedFile.getName() + " upload to S3 has failed.");
                        // Handle failure
                    } else {
                        Logger.debug("[AwsUploadActor] - " + renamedFile.getName() + " has been successfully uploaded to S3.");
                        // Send message to database actor with url.
                        StarmapUpdateAWS awsResult = new StarmapUpdateAWS(submission.getStarMapId(), url);
                        databaseActor.tell(awsResult, getSelf());
                    }
                }
            }, Akka.system().dispatcher());
        } else {
            unhandled(message);
        }

    }
}
