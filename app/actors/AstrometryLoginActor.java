package actors;

import actors.messages.AstLoginKey;
import akka.actor.ActorRef;
import akka.actor.Status;
import akka.actor.UntypedActor;
import akka.dispatch.Mapper;
import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import scala.concurrent.Future;


/**
 * Created by David on 7/6/15.
 */
public class AstrometryLoginActor extends UntypedActor {

    public AstrometryLoginActor() {
        Logger.info("Starting AstrometryLoginActor");
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof AstLoginKey) {
            // This is important! We must save the reference to the sender and use this reference in the Future mapping.
            // If we use getSender() directly within the Future, we might run into race conditions that will screw everything up!
            final ActorRef sender = getSender();
            // Self reference here might not be needed, but I do it just in case. Need to look into this further.
            final ActorRef self = getSelf();

            final AstLoginKey loginKey = (AstLoginKey) message;
            //Future<JsonNode> jsonResult = login(loginKey);
            //getSender().tell(jsonResult, getSelf());

            final Future<WS.Response> wsResponse = login2(loginKey);

            wsResponse.map(new Mapper<WS.Response, JsonNode>() {
                @Override
                public JsonNode apply(WS.Response response) {
                    // Json response looks something like this:
                    // {"status":"success","message":"authenticated user: ","session":"elowv64c6mh69ru6ryhwu4y1xs2hdsx0"}
                    JsonNode json = response.asJson();


                    if (json.findPath("status").asText().equals("success")) {
                        String sessionKey = json.findPath("session").asText();
                        Logger.debug("[AstrometryLoginActor] - Login Successful! Response: " + json);
                        sender.tell(sessionKey, self);
                    } else {
                        Logger.debug("[AstrometryLoginActor] - Login Failed! Response: " + json);
                        Exception ex = new RuntimeException("Login failed.");
                        sender.tell(new Status.Failure(ex), self);
                        // Do we need to actually throw ex in this case?
                    }

                    // This return doesn't matter. Maybe change Mapper to something else...
                    return json;
                }
            }, Akka.system().dispatcher());

        }
    }

    private Future<WS.Response> login2(AstLoginKey loginKey) {
        String apiKey = loginKey.getLoginKey();

        //Create simple json object to hold user api key. {"apikey" : "given key"}
        JsonNode apiKeyJson = Json.newObject().put("apikey", apiKey);

        WS.WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/login").setContentType("application/x-www-form-urlencoded");

        F.Promise<WS.Response> responsePromise = holder.post("request-json=" + apiKeyJson);


        // Wrapped() will get the original Future that was wrapped by Promise. Play Promises are just a wrapper around Akka Futures.
        return responsePromise.wrapped();
    }

    private Future<JsonNode> login(AstLoginKey loginKey) {
        String apiKey = loginKey.getLoginKey();

        //Create simple json object to hold user api key. {"apikey" : "given key"}
        JsonNode apiKeyJson = Json.newObject().put("apikey", apiKey);

        WS.WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/login").setContentType("application/x-www-form-urlencoded");

        F.Promise<WS.Response> responsePromise = holder.post("request-json=" + apiKeyJson);

        F.Promise<JsonNode> jsonPromise = responsePromise.map(
                new F.Function<WS.Response, JsonNode>() {
                    public JsonNode apply(WS.Response response) {
                        JsonNode json = response.asJson();

                        if (json.findPath("status").asText().equals("success")) {
                            System.out.println("Login Successful! Response: " + json);
                        } else {
                            System.out.println("Login Failed! Response: " + json);
                        }

                        return json;
                    }
                }
        );

        // Get the original Future that was wrapped by Promise. Play Promises are just a wrapper around Akka Futures.
        Future<JsonNode> jsonFuture = jsonPromise.wrapped();

        // Note to self: The dispatcher is what binds a set of actors to a thread pool. Thread pool == execution context.
        // Send the result of future to another actor.
        //akka.pattern.Patterns.pipe(jsonFuture, Akka.system().dispatcher()).to(someActor);

        return jsonFuture;


    }
}
