package controllers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import models.*;
import play.Play;
import play.data.Form;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import utils.AstrometryApiHelper;
import utils.AwsS3Utils;
import views.formdata.*;
import views.html.*;

import play.libs.WS;
import static play.libs.F.Function;
import static play.libs.F.Promise;


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

    return ok(Dashboard.render("User Dashboard", Secured.isLoggedIn(ctx()), Secured.isAdmin(ctx()),
            Secured.getUserInfo(ctx()), submissions));
  }

  /**
   * Returns the Starmap page.
   *
   * @return Starmap page.
   */
  public static Result getStarmap(Long starmapID) {
    StarMap starMap = StarMap.getStarmap(starmapID);

    return ok(ShowStarmap.render("ShowStarmap", Secured.isLoggedIn(ctx()), Secured.isAdmin(ctx()),
            Secured.getUserInfo(ctx()), starMap));
  }


  public static Promise<Result> testWS() {
    final Promise<Result> resultPromise = WS.url("http://www.mocky.io/v2/5185415ba171ea3a00704eed").get().map(
            new Function<WS.Response, Result>() {
              public Result apply(WS.Response response) {
                return ok("Feed title:" + response.asJson().findPath("hello"));
              }
            }
    );
    return resultPromise;
  }

  public static Promise<Result> testWS2() {


//    ObjectMapper mapper = new ObjectMapper();
//    JsonNode apiKeyNode = mapper.createObjectNode(); // will be of type ObjectNode
//    ((ObjectNode) apiKeyNode).put("apikey", "fvyuvruptdpybabg");
//
//    System.out.println(apiKeyNode);

    String awsAccessKey = Play.application().configuration().getString("aws.access.key");
    JsonNode apiKeyJson = Json.newObject().put("apikey", awsAccessKey);


    //final AstrometryApiHelper apiResponse = new AstrometryApiHelper("fvyuvruptdpybabg");


    final Promise<Result> resultPromise = WS.url("http://nova.astrometry.net/api/login").setContentType("application/x-www-form-urlencoded").post("request-json=" + apiKeyJson).map(
      new Function<WS.Response, Result>() {
        Function<WS.Response, Result> thisFunc = this; //Create reference to this instance
        public Result apply(WS.Response response) {
          //apiResponse.setApiKeyResponse(response.asJson());
          //System.out.println(apiResponse.getApiKeyResponse());
         // return ok(apiResponse.getApiKeyResponse());
          return ok("From Result:" + response.asJson());
        }
      }
    );

    //System.out.println(apiResponse.getApiKeyResponse());
    //System.out.println(apiResponse.getApiKeyResponse().get(1000));
    return resultPromise;
  }

  public static Result testWS3() {
    String awsAccessKey = Play.application().configuration().getString("aws.access.key");
    AstrometryApiHelper astmSession = new AstrometryApiHelper(awsAccessKey);

    System.out.println("LoginJsonResponse: " + astmSession.getLoginJsonResponse());
    System.out.println("Session Key: " + astmSession.getSessionKey());

    File image = Play.application().getFile("/public/images/night-sky.jpg");

    try {
      astmSession.submitImage(image);
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("ImageSubmissionResponse: " + astmSession.getImageSubmissionJsonResponse());
    System.out.println("Submission ID: " + astmSession.getSubmissionId());

    astmSession.fetchJobId();

    System.out.println("SubmissionIdResponse: " + astmSession.getSubmissionIdJsonResponse());
    System.out.println("Job ID: " + astmSession.getJobId());



    return ok("Job ID: " + astmSession.getJobId());
  }

  public static Promise<Result> testWS4() {
    Promise<String> promiseOfInt = Promise.promise(
            new F.Function0<String>() {
              public String apply() {

                String awsAccessKey = Play.application().configuration().getString("aws.access.key");
                AstrometryApiHelper astmSession = new AstrometryApiHelper(awsAccessKey);

                System.out.println("LoginJsonResponse: " + astmSession.getLoginJsonResponse());
                System.out.println("Session Key: " + astmSession.getSessionKey());

                File image = Play.application().getFile("/public/images/night-sky.jpg");
                try {
                  astmSession.submitImage(image);
                } catch (IOException e) {
                  e.printStackTrace();
                }

                System.out.println("ImageSubmissionResponse: " + astmSession.getImageSubmissionJsonResponse());
                System.out.println("Submission ID: " + astmSession.getSubmissionId());

                astmSession.fetchJobId();

                System.out.println("SubmissionIdResponse: " + astmSession.getSubmissionIdJsonResponse());
                System.out.println("Job ID: " + astmSession.getJobId());

                return astmSession.getJobId();
              }
            }
    );
    return promiseOfInt.map(
            new Function<String, Result>() {
              public Result apply(String jobId) {
                return ok("Job ID: " + jobId);
              }
            }
    );

  }

  public static Result testWS5() {
    Promise<String> promiseOfInt = Promise.promise(
            new F.Function0<String>() {
              public String apply() {

                String awsAccessKey = Play.application().configuration().getString("aws.access.key");
                AstrometryApiHelper astmSession = new AstrometryApiHelper(awsAccessKey);

                System.out.println("LoginJsonResponse: " + astmSession.getLoginJsonResponse());
                System.out.println("Session Key: " + astmSession.getSessionKey());

                File image = Play.application().getFile("/public/images/night-sky.jpg");
                try {
                  astmSession.submitImage(image);
                } catch (IOException e) {
                  e.printStackTrace();
                }

                System.out.println("ImageSubmissionResponse: " + astmSession.getImageSubmissionJsonResponse());
                System.out.println("Submission ID: " + astmSession.getSubmissionId());

                astmSession.fetchJobId();

                System.out.println("SubmissionIdResponse: " + astmSession.getSubmissionIdJsonResponse());
                System.out.println("Job ID: " + astmSession.getJobId());

                return astmSession.getJobId();
              }
            }
    );

    return ok("Hi");
  }

  public static Result uploadImage() {
    Http.MultipartFormData body = request().body().asMultipartFormData();
    Http.MultipartFormData.FilePart image = body.getFile("uploadedFile"); //"uploadedFile" refers to the form field name.

    if (image != null) {
      String fileName = image.getFilename();
      String contentType = image.getContentType();
      File file = image.getFile(); // Uses ugly file name, i.e. multipartBody5737560692539269893asTemporaryFile

      // Create new empty StarMap, generating an ID, which we prepend to the submitted image file name.
      StarMap starMap = new StarMap(Secured.getUserInfo(ctx()));
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
      String awsAccessKey = Play.application().configuration().getString("aws.access.key");
      AstrometryApiHelper astmSession = new AstrometryApiHelper(awsAccessKey);
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

      return ok("File was uploaded to: " + starMap.getS3imageUrl() + "\n" +
                "Astrometry Submission ID: " + starMap.getSubmissionId() + "\n" +
                "Astrometry Job ID: " + starMap.getJobId() + "\n" +
                "Astrometry Image Annotations: " + starMap.getImageAnnotations());
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
}
