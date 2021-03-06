package controllers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import actors.Pinger;
import actors.messages.AstLoginKey;
import actors.messages.StarmapSubmission;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import models.*;
import play.Logger;
import play.Play;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Akka;
import play.libs.F;
import play.mvc.*;
import scala.concurrent.duration.Duration;
import utils.AstrometryApiHelper;
import utils.AwsS3Utils;
import views.formdata.*;
import views.html.*;


import javax.imageio.ImageIO;


/**
 * Implements the controllers for this application.
 */
public class Application extends Controller {

  public static String adminEmail;
  public static String adminPassword;

  /**
   * Returns the home page.
   * 
   * @return The resulting home page.
   */
  public static Result index() {
    // Redirect logged in users to Dashboard page.
    if (Secured.isLoggedIn(ctx())) {
      return redirect(routes.Application.getDashboard());
    }

    return ok(Index.render("Home", Secured.isLoggedIn(ctx()), Secured.isAdmin(ctx()), Secured.getUserInfo(ctx())));
  }


  /**
   * Provides the Login page (only to unauthenticated users).
   * 
   * @return The Login page.
   */
  public static Result login() {
    Form<LoginFormData> formData = Form.form(LoginFormData.class);

    return ok(Login.render("Login", Secured.isLoggedIn(ctx()), Secured.isAdmin(ctx()), Secured.getUserInfo(ctx()),
        formData));
  }

  /**
   * Processes a login form submission from an unauthenticated user. First we bind the HTTP POST data to an instance of
   * LoginFormData. The binding process will invoke the LoginFormData.validate() method. If errors are found, re-render
   * the page, displaying the error data. If errors not found, render the page with the good data.
   * 
   * @return The index page with the results of validation.
   */
  public static Result postLogin() {
    // Get the submitted form data from the request object, and run validation.
    Form<LoginFormData> formData = Form.form(LoginFormData.class).bindFromRequest();

    if (formData.hasErrors()) {
      flash("error", LoginFormData.ERROR_TEXT);

      return badRequest(Login.render("Login", Secured.isLoggedIn(ctx()), Secured.isAdmin(ctx()),
          Secured.getUserInfo(ctx()), formData));
    }
    else {
      // email/password OK, so now we set the session variable and only go to authenticated pages.
      session().clear();
      session("email", formData.get().email);
      return redirect(routes.Application.index());
    }
  }

  /**
   * Logs out (only for authenticated users) and returns them to the Index page.
   * 
   * @return A redirect to the Index page.
   */
  @Security.Authenticated(Secured.class)
  public static Result logout() {
    session().clear();
    return redirect(routes.Application.index());
  }


  /**
   * Displays the signup page.
   * 
   * @return Signup page.
   */
  public static Result signup() {
    Form<SignupFormData> formData = Form.form(SignupFormData.class);

    return ok(Signup.render("Signup", Secured.isLoggedIn(ctx()), Secured.isAdmin(ctx()), Secured.getUserInfo(ctx()),
        formData));
  }

  /**
   * Processes the signup page.
   * 
   * @return Signup page on fail, Login page on success.
   */
  public static Result postSignup() {
    Form<SignupFormData> formData = Form.form(SignupFormData.class).bindFromRequest();

    if (formData.hasErrors()) {
      flash("error", SignupFormData.ERROR_TEXT);

      return badRequest(Signup.render("Signup", Secured.isLoggedIn(ctx()), Secured.isAdmin(ctx()),
          Secured.getUserInfo(ctx()), formData));
    }
    else {
      // email/password OK, so now we set the session variable and only go to authenticated pages.
      UserInfoDB.addUserInfo(UserInfoDB.STANDARD, formData.get().email, formData.get().password);
      flash("success", SignupFormData.SUCCESS_TEXT);
      return redirect(routes.Application.login());
    }
  }


  /**
   * Returns the Dashboard page.
   *
   * @return Dashboard page.
   */
  public static Result getDashboard() {
    UserInfo user = Secured.getUserInfo(ctx());

    List<StarMap> submissions = user.getStarmaps();

    Comparator<StarMap> descByIdComparator = new Comparator<StarMap>() {
      @Override
      public int compare(StarMap o1, StarMap o2) {
        Long comp = o2.getId() - o1.getId();
        return comp.intValue(); // Can cause overflow here if the Long happens to be bigger than max int.
      }
    };

    Collections.sort(submissions, descByIdComparator);

    return ok(Dashboard.render("User Dashboard", Secured.isLoggedIn(ctx()), Secured.isAdmin(ctx()),
            Secured.getUserInfo(ctx()), submissions));
  }

  /**
   * Returns the Public Submissions page.
   *
   * @return Dashboard page.
   */
  public static Result getPublicSubmissions() {
    UserInfo user = Secured.getUserInfo(ctx());

    List<StarMap> submissions = StarMap.find().all();

    Comparator<StarMap> descByIdComparator = new Comparator<StarMap>() {
      @Override
      public int compare(StarMap o1, StarMap o2) {
        Long comp = o2.getId() - o1.getId();
        return comp.intValue(); // Can cause overflow here if the Long happens to be bigger than max int.
      }
    };

    Collections.sort(submissions, descByIdComparator);

    return ok(PublicSubmissions.render("Public Submissions", Secured.isLoggedIn(ctx()), Secured.isAdmin(ctx()),
            Secured.getUserInfo(ctx()), submissions));
  }

  /**
   * Returns the Starmap page.
   *
   * @return Starmap page.
   */
  public static Result getStarmap(Long starmapID) {
    StarMap starMap = StarMap.getStarmap(starmapID);

    //long epoch = System.currentTimeMillis()/1000;

    return ok(ShowStarmap.render("ShowStarmap", Secured.isLoggedIn(ctx()), Secured.isAdmin(ctx()),
            Secured.getUserInfo(ctx()), starMap));
  }

  public static Result uploadImage() {
    // Get image file
    Http.MultipartFormData body = request().body().asMultipartFormData();
    Http.MultipartFormData.FilePart image = body.getFile("uploadedFile"); //"uploadedFile" refers to the form field name.

    // Get form image url
    DynamicForm requestData = Form.form().bindFromRequest();
    String formImageUrl = requestData.get("image_url");

    String fileName = "";
    File file = null;

    // First check if user submitted URL.
    if (!formImageUrl.isEmpty()) {
      try {
        URL url = new URL(formImageUrl);
        fileName = url.getFile();
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1).split("\\?")[0].split("#")[0]; // Gets actual filename instead of entire file path (from getFile()).

        // Change from Java user-agent to a browser user agent. Otherwise will not be able to get image properly.
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");

        BufferedImage urlImage = ImageIO.read(connection.getInputStream());
        file = new File(fileName);
        String fileExtension = formImageUrl.substring(formImageUrl.lastIndexOf(".") + 1);
        ImageIO.write(urlImage, fileExtension, file);
        file.deleteOnExit();

        // Test that file is a valid image. (DO THIS FOR browsed images too)
        if (ImageIO.read(file) == null) {
          flash("error", "URL not a valid image. Please try again.");
          return redirect(routes.Application.index());
        }

        System.out.println("FileName string: " + fileName);
      } catch (MalformedURLException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    // Otherwise check if user submitted local image.
    else if (image != null) {
      fileName = image.getFilename();
      file = image.getFile(); // Uses ugly file name, i.e. multipartBody5737560692539269893asTemporaryFile
    }

    // If user did indeed submit either URL or local image, we continue the submission process.
    if (image != null || !formImageUrl.isEmpty()) {
      // Create new empty StarMap, generating an ID, which we prepend to the submitted image file name.
      // If user is not logged in, we create StarMap with the anonymous account (created in Global).
      StarMap starMap = (Secured.isLoggedIn(ctx())) ? new StarMap(Secured.getUserInfo(ctx())) : new StarMap(UserInfoDB.getUser("anonymous"));
      starMap.save();

      // Rename file to make life easier later on.
      File renamedFile = AwsS3Utils.renameFile(file, starMap.getId() + "-" + fileName);

      // Upload to S3.
      String s3Url = AwsS3Utils.uploadFile(renamedFile);

      // Update StarMap fields.
      starMap.setS3imageUrl(s3Url);
      starMap.update(starMap.getId());

      // Send to Astrometry API.
      // Login, get API session key.
      String astrometryApiKey = Play.application().configuration().getString("astrometry.api.key");
      AstrometryApiHelper astmSession = new AstrometryApiHelper(astrometryApiKey);
      System.out.println("LoginJsonResponse: " + astmSession.getLoginJsonResponse());
      System.out.println("Session Key: " + astmSession.getSessionKey());

      // Submit image
      try {
        astmSession.submitImage(renamedFile);
      } catch (IOException e) {
        e.printStackTrace();
      }

      System.out.println("ImageSubmissionResponse: " + astmSession.getImageSubmissionJsonResponse());
      System.out.println("Submission ID: " + astmSession.getSubmissionId());

      // Fetch Astrometry API job ID
      astmSession.fetchJobId();
      System.out.println("SubmissionIdResponse: " + astmSession.getSubmissionIdJsonResponse());
      System.out.println("Job ID: " + astmSession.getJobId());

      // Wait for image processing to complete.
      astmSession.checkJobStatusUntilDone();

      // Fetch Astrometry API image annotations
      astmSession.fetchImageAnnotations();

      // Store Astrometry API results to StarMap
      starMap.setSubmissionId(astmSession.getSubmissionId());
      starMap.setJobId(astmSession.getJobId());
      starMap.setImageAnnotations(astmSession.getImageAnnotations());
      starMap.update(starMap.getId());

      //// Create new Stars and Coordinates
      // Get all found stars in Json format.
      List<JsonNode> starsJsonList = astmSession.getImageJsonAnnotations();

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
          Coordinate coord = new Coordinate(starMap, star, x, y, radius);
          coord.save();

          // Add and Save star to StarMap.
          starMap.addStar(star);
          starMap.save();
        }
      }

      return redirect(routes.Application.getStarmap(starMap.getId()));

    } else {
      flash("error", "No file was attached.");
      return redirect(routes.Application.index());
    }
  }

  public static Result SearchStar(String starName) {
    UserInfo user = Secured.getUserInfo(ctx());


    Star star = Star.getStar(starName);

    List<StarMap> starMaps = (star != null) ? star.getStarMaps() : null;


    return ok(StarSearch.render("Search Results: " + starName, Secured.isLoggedIn(ctx()), Secured.isAdmin(ctx()),
            Secured.getUserInfo(ctx()), starMaps, starName));
  }

  public static Result testAkka() {
    String astrometryApiKey = Play.application().configuration().getString("astrometry.api.key");
    AstLoginKey loginKey = new AstLoginKey(astrometryApiKey);

    // This old version of Akka doesn't have an easy way to get an ActorRef from ActorSelection
    // We can still safely tell() to the ActorSelection as long as we only create one actor, but once we upgrade
    // the framework, use the resolveOne() method to get an ActorRef and use that instead.
    ActorSelection actor = Akka.system().actorSelection("/user/astrometryActor");
    actor.tell(loginKey);


    return ok("Done!");
  }

  public static Result starmapSubmissionAkka() {

    // Get image file
    Http.MultipartFormData body = request().body().asMultipartFormData();
    Http.MultipartFormData.FilePart image = body.getFile("uploadedFile"); //"uploadedFile" refers to the form field name.

    // Get form image url
    DynamicForm requestData = Form.form().bindFromRequest();
    String formImageUrl = requestData.get("image_url");

    String fileName = "";
    File file = null;

    // First check if user submitted URL.
    if (!formImageUrl.isEmpty()) {
      try {
        URL url = new URL(formImageUrl);
        fileName = url.getFile();
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1).split("\\?")[0].split("#")[0]; // Gets actual filename instead of entire file path (from getFile()).

        // Change from Java user-agent to a browser user agent. Otherwise will not be able to get image properly.
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");

        BufferedImage urlImage = ImageIO.read(connection.getInputStream());
        file = new File(fileName);
        String fileExtension = formImageUrl.substring(formImageUrl.lastIndexOf(".") + 1);
        ImageIO.write(urlImage, fileExtension, file);
        file.deleteOnExit();

        // Test that file is a valid image. (DO THIS FOR browsed images too)
        if (ImageIO.read(file) == null) {
          flash("error", "URL not a valid image. Please try again.");
          return redirect(routes.Application.index());
        }

        System.out.println("FileName string: " + fileName);
      } catch (MalformedURLException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    // Otherwise check if user submitted local image.
    else if (image != null) {
      fileName = image.getFilename();
      file = image.getFile(); // Uses ugly file name, i.e. multipartBody5737560692539269893asTemporaryFile
    }

    // If user did indeed submit either URL or local image, we continue the submission process.
    if (image != null || !formImageUrl.isEmpty()) {

      UserInfo user = (Secured.isLoggedIn(ctx())) ? Secured.getUserInfo(ctx()) : UserInfoDB.getUser("anonymous");
      String astrometryApiKey = Play.application().configuration().getString("astrometry.api.key");

      StarmapSubmission submission = new StarmapSubmission(user, astrometryApiKey, file, fileName);

      // This old version of Akka doesn't have an easy way to get an ActorRef from ActorSelection
      // We can still safely tell() to the ActorSelection as long as we only create one actor, but once we upgrade
      // the framework, use the resolveOne() method to get an ActorRef and use that instead.
      ActorSelection actor = Akka.system().actorSelection("/user/starmapSubmissionActor");
      actor.tell(submission);

      if (Secured.isLoggedIn(ctx())) {
        return redirect(routes.Application.getDashboard());
      } else {
        return redirect(routes.Application.getPublicSubmissions());
      }

    } else {
      flash("error", "No file was attached.");
      return redirect(routes.Application.index());
    }
  }

  public static WebSocket<String> pingWs() {
    return new WebSocket<String>() {
      public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out) {
        final ActorRef pingActor = Akka.system().actorOf(Props.create(Pinger.class, in, out));
        final Cancellable cancellable = Akka.system().scheduler().schedule(Duration.create(1, TimeUnit.SECONDS),
                Duration.create(1, TimeUnit.SECONDS),
                pingActor,
                "Tick",
                Akka.system().dispatcher(),
                null
        );

        in.onClose(new F.Callback0() {
          @Override
          public void invoke() throws Throwable {
            cancellable.cancel();
            Logger.info("Cancelling WS actor.");
          }
        });
      }

    };
  }


}
