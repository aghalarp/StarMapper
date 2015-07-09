package actors;

import actors.messages.AstLoginKey;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnComplete;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.libs.Akka;
import scala.concurrent.Future;

/**
 * Master actor to initiate all Astrometry submissions.
 */
public class AstrometryActor extends UntypedActor {;

    // Actor to login and get session key
    private final ActorRef loginActor;


    // Actor to submit image (with session key), get submission id.

    // Actor to get job id (with submission id)

    // Actor to get annotation data (with job id)

    public AstrometryActor() {
        Logger.info("Creating AstrometryActor.");
        this.loginActor = getContext().actorOf(Props.create(AstLoginActor.class), "loginActor");
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof AstLoginKey) {
            AstLoginKey loginKey = (AstLoginKey) message;
            //loginActor.tell(loginKey, getSelf());
            // Alternatively, we can ask() loginActor for key, get a future, and pipe it to the next actor.
            // Pipe is preferred over Await.result() or Await.ready() because the latter two will block.
            Future<Object> loginJsonFuture = Patterns.ask(loginActor, loginKey, 100000); // Must use Future<Object>, cast later if needed.
            //akka.pattern.Patterns.pipe(loginJsonFuture, Akka.system().dispatcher()).to(someActor, getSelf());
            loginJsonFuture.onSuccess(new OnSuccess<Object>() {
                public void onSuccess(Object result) {
                    Logger.debug("From onReceive: " + result); // Object here is an up-casted JsonNode.
                }
            }, Akka.system().dispatcher());

        } else {
            unhandled(message);
        }

    }

}

