package actors;

import actors.messages.AstrometryResultData;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import scala.Function1;
import scala.concurrent.Future;
import scala.runtime.BoxedUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * Actor responsible for retrieving image annotation data from Astrometry.
 */
public class AstrometryAnnotationsActor extends UntypedActor {
    private final ActorRef databaseActor;

    public AstrometryAnnotationsActor(ActorRef databaseActor) {
        Logger.info("Starting AstrometryAnnotationsActor.");
        this.databaseActor = databaseActor;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        ActorRef self = getSelf();

        if (message instanceof AstrometryResultData) {
            AstrometryResultData data = (AstrometryResultData) message;
            Future<JsonNode> annotations = fetchImageAnnotations(data.getAstSessionKey(), data.getAstJobId());

            annotations.onComplete(new OnComplete<JsonNode>() {
                @Override
                public void onComplete(Throwable failure, JsonNode annotations) throws Throwable {
                    if (failure != null) {
                        // Fail
                    } else {
                        AstrometryResultData newData = new AstrometryResultData(data.getStarMapId(), data.getAstSessionKey(), data.getAstSubmissionId(), data.getAstJobId(), annotations);
                        databaseActor.tell(newData, self);
                    }
                }
            }, Akka.system().dispatcher());


        } else {
            unhandled(message);
        }
    }

    public Future<JsonNode> fetchImageAnnotations(String sessionKey, String jobId) {

        //Create Json object required for post request. {"session":"given key"}
        JsonNode sessionKeyJson = Json.newObject().put("session", sessionKey);

        // Astrometry's "image annotations" api call url.
        WS.WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/jobs/" + jobId + "/annotations").setContentType("application/x-www-form-urlencoded");

        F.Promise<WS.Response> responsePromise = holder.post("request-json=" + sessionKeyJson);

        Future<WS.Response> responseFuture = responsePromise.wrapped();

        Future<JsonNode> annotations = responseFuture.map(new Mapper<WS.Response, JsonNode>() {
            @Override
            public JsonNode apply(WS.Response response) {
                // Response looks like this:
                // {"annotations":[{"radius":0.0,"type":"hd","names":["HD 82394"],"pixelx":355.53258525719673,"pixely":119.62747191355685},
                // {"radius":0.0,"type":"hd","names":["HD 82105"],"pixelx":488.98188022535305,"pixely":216.6216337848115},
                // {"radius":0.0,"type":"hd","names":["HD 82088"],"pixelx":448.10278807832856,"pixely":296.253274626243},
                // {"radius":8.390488697471927,"type":"ngc","names":["NGC 2916"],"pixelx":113.30092930498583,"pixely":45.48019939200901},
                // {"radius":42.92057660440606,"type":"ngc","names":["NGC 2905"],"pixelx":290.990404953905,"pixely":233.83414758667215},
                // {"radius":40.66159812910518,"type":"ngc","names":["NGC 2903"],"pixelx":290.21802543427566,"pixely":240.86889522204743}]}
                JsonNode json = response.asJson();
                Logger.debug("[AstrometryAnnotationsActor] - Image annotations retrieved for Job " + jobId);
                Logger.debug(json.toString());

                return json;
            }
        }, Akka.system().dispatcher());

        return annotations;
    }

}
