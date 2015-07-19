package actors;

import actors.messages.StarmapCreate;
import actors.messages.StarmapUpdateAWS;
import actors.messages.AstrometryResultData;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.JsonNode;
import models.Coordinate;
import models.Star;
import models.StarMap;
import models.UserInfo;
import play.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DatabaseActor extends UntypedActor {

    public DatabaseActor() {
        Logger.info("Starting DatabaseActor.");
    }

    @Override
    public void onReceive(Object message) {
        // This is important! We must save the reference to the sender and use this reference in the Future mapping.
        // If we use getSender() directly within the Future, we might run into race conditions that will screw everything up!
        final ActorRef sender = getSender();
        // Self reference here might not be needed, but I do it just in case. Need to look into this further.
        final ActorRef self = getSelf();


        if (message instanceof StarmapCreate) {
            // Get UserInfo from message.
            final StarmapCreate msg = (StarmapCreate) message;
            final UserInfo user = msg.getUser();

            // Create new empty StarMap entry, generating an ID.
            final StarMap sm = new StarMap(user);
            sm.save();

            // Send back StarMap Id to sender.
            final Long id = sm.getId();
            Logger.debug("[DatabaseActor] - StarMap #" + id + " has been created.");
            sender.tell(id, self);
        }
        else if (message instanceof StarmapUpdateAWS) {
            final StarmapUpdateAWS msg = (StarmapUpdateAWS) message;

            // Get StarMap and update its S3 url.
            final StarMap sm = StarMap.getStarmap(msg.getStarMapId());
            sm.setS3imageUrl(msg.getAwsUrl());
            sm.update(msg.getStarMapId());
        }
        else if (message instanceof AstrometryResultData) {
            AstrometryResultData data = (AstrometryResultData) message;

            final StarMap sm = StarMap.getStarmap(data.getStarMapId());
            sm.setSubmissionId(data.getAstSubmissionId());
            sm.setJobId(data.getAstJobId());
            sm.setImageAnnotations(data.getAstImageAnnotations().toString());
            sm.update(data.getStarMapId());

            List<JsonNode> starsJsonList = parseJsonImageAnnotations(data.getAstImageAnnotations());

            // Check if each Star already exists in database. If not, we create it.
            for (JsonNode starJson : starsJsonList) {
                if (starJson.get("names").has(0)) {
                    String starName = starJson.get("names").get(0).asText();
                    String type = starJson.get("type").asText();
                    Double x = starJson.get("pixelx").asDouble();
                    Double y = starJson.get("pixely").asDouble();
                    Double radius = starJson.get("radius").asDouble();

                    // Create new star if doesn't exist. Otherwise grab existing entry.
                    Star star = (!Star.starExists(starName)) ? new Star(starName, type) : Star.getStar(starName);

                    // Save to create new Star instance if new star. If existing star, this will harmlessly update() the object.
                    star.save();

                    // Create and save new Coordinate instance, linking the StarMap and Star.
                    Coordinate coord = new Coordinate(sm, star, x, y, radius);
                    coord.save();

                    // Add and Save star to StarMap.
                    sm.addStar(star);
                    sm.save();
                }
            }
        }
        else {
            unhandled(message);
        }
    }

    /**
     * Parses the JsonNode image annotations response from Astrometry, and returns a List of JsonNode objects representing
     * star data in the following format:
     * {"radius":42.92057660440606,"type":"ngc","names":["NGC 2905"],"pixelx":290.990404953905,"pixely":233.83414758667215}
     *
     * @param json The Astrometry image annotations json
     * @return List of JsonNode star data.
     */
    public static List<JsonNode> parseJsonImageAnnotations(JsonNode json) {
        JsonNode annotations = json.findValue("annotations");

        if (annotations != null) {
            List<JsonNode> jsonStars = new ArrayList<JsonNode>();

            for (int i=0; i < annotations.size(); i++) {
                JsonNode js = annotations.get(i);
                jsonStars.add(js);
                //System.out.println(js);
            }

            return jsonStars;
        }

        return null;
    }
}
