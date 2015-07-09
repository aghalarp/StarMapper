import actors.AstrometryActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import models.UserInfoDB;
import controllers.Application;
import play.GlobalSettings;
import play.Play;
import play.libs.Akka;

/**
 * Provides initialization code for the DiveTableApp application.
 * 
 * @author David A.
 * 
 */
public class Global extends GlobalSettings {

  /**
   * Initialize the system with some sample contacts.
   * 
   * @param app The application.
   */
  public void onStart(play.Application app) {

    Application.adminEmail = Play.application().configuration().getString("admin.email");
    Application.adminPassword = Play.application().configuration().getString("admin.password");

    if (Application.adminEmail == null) {
      System.err.println("The admin email environmental variable was not set correctly.");
    }
    if (Application.adminPassword == null) {
      System.err.println("The admin password environmental variable was not set correctly.");
    }

    UserInfoDB.addUserInfo(UserInfoDB.ADMIN, Application.adminEmail, Application.adminPassword);

    //Create Anonymous User for anonymous image submissions.
    UserInfoDB.addUserInfo(UserInfoDB.STANDARD, "anonymous", "anonymous");

    // Create Astrometry actor to handle all submissions.
    ActorRef astrometryActor = Akka.system().actorOf(Props.create(AstrometryActor.class), "astrometryActor");


  }
}
