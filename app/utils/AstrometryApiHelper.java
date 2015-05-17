package utils;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.routes;
import play.Play;
import play.libs.Json;
import play.libs.WS;
import play.libs.WS.*;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Result;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by David on 12/25/14.
 */
public class AstrometryApiHelper {
    private String apiKey;
    private JsonNode loginJsonResponse;
    private String sessionKey; //After logging in with apiKey, must use this session key for subsequent api calls.

    private JsonNode imageSubmissionJsonResponse;
    private String submissionId;

    private JsonNode submissionIdJsonResponse;
    private String jobId;

    private JsonNode jobstatusJsonResponse;
    private String jobStatus;

    private JsonNode imageAnnotationsJsonResponse;
    private String imageAnnotations;
    private List<JsonNode> imageJsonAnnotations;


    public Promise<JsonNode> apiKeyPromise;
    public JsonNode apiKeyResponse;
    public String stringtest = "init";

    /**
     * Constructor which creates new Astrometry API session.
     * @param apiKey The Astrometry API key.
     */
    public AstrometryApiHelper(String apiKey) {
        this.apiKey = apiKey;
        //Pass given apiKey to login method.
        apiKeyLogin(this.apiKey);

        //Example of static api login.
        //this.apiKey = staticApiKeyLogin(apiKey);

    }

    /**
     * Logs into the Astrometry web service with given user api key, which responds with a session key that we
     * must use with all subsequent api calls.
     *
     * @param apiKey The Astrometry user API key.
     */
    public void apiKeyLogin(String apiKey) {
        //Create simple json object to hold user api key. {"apikey" : "given key"}
        JsonNode apiKeyJson = Json.newObject().put("apikey", apiKey);

        WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/login").setContentType("application/x-www-form-urlencoded");

        Promise<WS.Response> responsePromise = holder.post("request-json=" + apiKeyJson);

        Promise<JsonNode> jsonPromise = responsePromise.map(
                new Function<WS.Response, JsonNode>() {
                    public JsonNode apply(WS.Response response) {
                        JsonNode json = response.asJson();
                        //Store the json response to instance variable.
                        AstrometryApiHelper.this.loginJsonResponse = json;

                        if (json.findPath("status").asText().equals("success")) {
                            //Store session key to instance variable.
                            AstrometryApiHelper.this.sessionKey = json.findPath("session").asText();
                            System.out.println("Login Successful! Response: " + json);
                        } else {
                            System.out.println("Login Failed! Response: " + json);
                        }

                        return json;
                    }
                }
        );

        //Wait up to 10 seconds for the promise to get the (json) result.
        //This is kind of bad, as it goes against the asynchronous nature of Promises.
        //But we need to get this response before we can do other api calls.
        jsonPromise.get(15000);
    }

    /**
     * Static version of apiKeyLogin. Useful if need to generate a "temporary" Astrometry session key to be used for
     * further API calls.
     *
     * @param apiKey The Astrometry user API key.
     * @return The generated session key, or null if login failed.
     */
    public static String staticApiKeyLogin(String apiKey) {
        //Create simple json object to hold user api key. {"apikey" : "given key"}
        JsonNode apiKeyJson = Json.newObject().put("apikey", apiKey);

        WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/login").setContentType("application/x-www-form-urlencoded");

        Promise<WS.Response> responsePromise = holder.post("request-json=" + apiKeyJson);

        Promise<JsonNode> jsonPromise = responsePromise.map(
                new Function<WS.Response, JsonNode>() {
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

        //Wait up to 10 seconds for the promise to get the (json) result.
        //This is kind of bad, as it goes against the asynchronous nature of Promises.
        //But we need to get this response before we can do other api calls.
        JsonNode jsonResponse = jsonPromise.get(15000);

        // If login was successful, return generated session key. Otherwise, return null indicating failure.
        return jsonResponse.findPath("status").asText().equals("success") ? jsonResponse.findPath("session").asText() : null;
    }

    public void submitImage(File image) throws IOException {
        //Prepare Json data required for image submission. Sets submission options.
        JsonNode json = Json.newObject()
                .put("publicly_visible", "n")
                .put("allow_modifications", "d")
                .put("session", this.sessionKey)
                .put("allow_commercial_use", "d");

        //Manually create multipart/form-data POST request body because current version of Play doesn't support it...
        //Got most information from here: http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.2
        //https://www.ietf.org/rfc/rfc2388.txt
        //String outputFilePath = Play.application().path().getPath() + "/app/utils/tempPostRequestBody.dat";
        File tempPostRequestBodyFile = new File("tempPostRequestBody.dat"); //We'll delete this temp file after we submit post request.

        //Because we're working with multipart/form-data, we need to create an output stream containing both
        //character and binary data. To accomplish this, we use a regular OutputStream as well as a OutputStreamWriter.
        //The OutputStreamWriter allows us to write character streams into [File]OutputStream, which is a byte stream.
        //When we need to write characters/text into the output stream, write into the OutputStreamWriter.
        //When we need to write binary data into the output stream (eg. images), write into the FileOutputStream.
        OutputStream bos = new BufferedOutputStream(new FileOutputStream(tempPostRequestBodyFile));
        Writer writer = new BufferedWriter(new OutputStreamWriter(bos, "UTF8"));

        String boundary = "Um1Q5dLk29gluLz3T"; //Boundary is used to separate the parts of the multipart request.
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


        //With the post request body prepared, we can now send it.
        WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/upload");
        holder.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
        holder.setHeader("Content-Length", String.valueOf(tempPostRequestBodyFile.length())); //Content-Length refers to the size of the post request BODY.

        Promise<WS.Response> responsePromise = holder.post(tempPostRequestBodyFile); //Send file contents.

        Promise<JsonNode> jsonPromise = responsePromise.map(
                new Function<WS.Response, JsonNode>() {
                    public JsonNode apply(WS.Response response) {
                        JsonNode json = response.asJson();
                        // Store Json response to instance variable.
                        AstrometryApiHelper.this.imageSubmissionJsonResponse = json;

                        if (json.findPath("status").asText().equals("success")) {
                            //Store submission ID to instance variable.
                            AstrometryApiHelper.this.submissionId = json.findPath("subid").asText();
                            System.out.println("Image submission successful! Response: " + json);
                        } else {
                            System.out.println("Image submission failed! Response: " + json);
                        }

                        return json;
                    }
                }
        );

        //Wait up to 10 seconds for the promise to get the (json) result.
        //This is kind of bad, as it goes against the asynchronous nature of Promises.
        //But we need to get this response before we can do other api calls.
        jsonPromise.get(15000);

        tempPostRequestBodyFile.delete(); //Delete the tempPostRequestBody.dat file after we're done with it.
    }


    public void fetchJobId() {
        //Create Json object required for post request. {"session":"given key"}
        JsonNode sessionKeyJson = Json.newObject().put("session", this.sessionKey);

        // Astrometry's "submission status" api call url.
        WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/submissions/" + this.submissionId).setContentType("application/x-www-form-urlencoded");


        // Remember that we are calling a third party API (Astrometry) - our image submission might take a while to process.
        // As such, we need to call the submission status API multiple times until the JobID value has been created, which we use later.
        while (this.jobId == null) {

            Promise<WS.Response> responsePromise = holder.post("request-json=" + sessionKeyJson);

            Promise<JsonNode> jsonPromise = responsePromise.map(
                    new Function<WS.Response, JsonNode>() {
                        public JsonNode apply(WS.Response response) {
                            JsonNode json = response.asJson();
                            //Store the json response to instance variable.
                            AstrometryApiHelper.this.submissionIdJsonResponse = json;
                            System.out.println("Response: " + json);

                            // The "jobs" key/value pair in the Json response holds a 1-element array containing the Job ID.
                            // Initially will be null, or (for some reason) the actual string "null", until the
                            // API has processed the image submission and gives us an actual Job ID value, which we grab.
                            JsonNode jobsArray = json.findValue("jobs");

                            // Initially, {..., "jobs":[], ...}, OR {..., "jobs":[null], ...}. So we need to check for that.
                            if (jobsArray.has(0) && !jobsArray.get(0).isNull()) {
                                // Save Job ID.
                                AstrometryApiHelper.this.jobId = json.findPath("jobs").get(0).asText();
                                System.out.println("JobId check successful!");
                            } else {
                                System.out.println("Submission still being processed. Checking again...");
                            }

                            return json;
                        }
                    }
            );

            //Wait up to 10 seconds for the promise to get the (json) result.
            //This is kind of bad, as it goes against the asynchronous nature of Promises.
            //But we need to get this response before we can do other api calls.
            jsonPromise.get(15000);

            // Wait 5 seconds before the next API call so we don't spam their servers.
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void checkJobStatusUntilDone() {
        // Job status can only be: success, failure, or solving. We keep looping until job is done or has failed.
        while (this.jobStatus == null || this.jobStatus.isEmpty() || this.jobStatus.equals("solving")) {

            String status = checkJobStatus(this.jobId, this.sessionKey);

            switch (status) {
                case "success":
                    // Save Job Status.
                    this.jobStatus = "success";
                    System.out.println("Image has been successfully processed!");
                    break;
                case "solving":
                    this.jobStatus = "solving";
                    System.out.println("Image is still being processed. Checking again...!");
                    break;
                case "failure":
                    this.jobStatus = "failure";
                    System.out.println("Image was unable to be processed.");
                    break;
                default:
                    System.out.println("Unknown job status. This shouldn't happen.");
                    break;
            }

            // Wait 5 seconds before the next API call so we don't spam their servers.
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Calls Astrometry API once to get the current job status.
     *
     * @param jobId The Astrometry job ID to check.
     * @return The current job status - either "success", "failure", or "solving".
     */
    public static String checkJobStatus(String jobId, String sessionKey) {
        //Create Json object required for post request. {"session":"given key"}
        JsonNode sessionKeyJson = Json.newObject().put("session", sessionKey);

        // Astrometry's "job status" api call url.
        WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/jobs/" + jobId).setContentType("application/x-www-form-urlencoded");

        Promise<WS.Response> responsePromise = holder.post("request-json=" + sessionKeyJson);

        Promise<JsonNode> jsonPromise = responsePromise.map(
                new Function<WS.Response, JsonNode>() {
                    public JsonNode apply(WS.Response response) {
                        JsonNode json = response.asJson();
                        System.out.println("Job Status response: " + json);

                        return json;
                    }
                }
        );

        //Wait up to 10 seconds for the promise to get the (json) result.
        //This is kind of bad, as it goes against the asynchronous nature of Promises.
        //But we need to get this response before we can do other api calls.
        JsonNode jsonResponse = jsonPromise.get(15000);

        return jsonResponse.findPath("status").asText();
    }

    public void fetchImageAnnotations() {

        //Create Json object required for post request. {"session":"given key"}
        JsonNode sessionKeyJson = Json.newObject().put("session", this.sessionKey);

        // Astrometry's "image annotations" api call url.
        WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/jobs/" + this.jobId + "/annotations").setContentType("application/x-www-form-urlencoded");

        Promise<WS.Response> responsePromise = holder.post("request-json=" + sessionKeyJson);

        Promise<JsonNode> jsonPromise = responsePromise.map(
                new Function<WS.Response, JsonNode>() {
                    public JsonNode apply(WS.Response response) {
//                        System.out.println(response.getBody() + "\n\n\n\n");
//                        //while (response.getStatus() == 404) {
//                            //System.out.println("404 Not Found.");
//                            try {
//                                Thread.sleep(5000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        //}
//                        System.out.println(response.getBody() + "\n\n\n\n");
                        JsonNode json = response.asJson();
                        //Store the json response to instance variable.
                        AstrometryApiHelper.this.imageAnnotationsJsonResponse = json;
                        AstrometryApiHelper.this.imageAnnotations = json.toString();
                        AstrometryApiHelper.this.imageJsonAnnotations = parseJsonImageAnnotations(json);
                        System.out.println("Image Annotations Response: " + json);

                        return json;
                    }
                }
        );

        //Wait up to 10 seconds for the promise to get the (json) result.
        //This is kind of bad, as it goes against the asynchronous nature of Promises.
        //But we need to get this response before we can do other api calls.
        jsonPromise.get(15000);

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



    public JsonNode getJson(Promise<JsonNode> jsonPromise) {
//        Promise<JsonNode> js = jsonPromise.map(
//                new Function<JsonNode, JsonNode>() {
//                    public JsonNode apply(JsonNode json) {
//                        //AstrometryApiHelper.this.apiKeyResponse = json;
//                        //System.out.println("Internal: " + AstrometryApiHelper.this.apiKeyResponse);
//                        return json;
//                    }
//                }
//        );

        return jsonPromise.get(10000);
        //return js.get(10000); //Wait to get result. 10 seconds to timeout. This might cause a deadlock...
    }

    public Promise<JsonNode> getApiKeyResponse() {
        //JsonNode apiKeyJson = Json.newObject().put("apikey", "fvyuvruptdpybabg");
        WSRequestHolder holder = WS.url("http://nova.astrometry.net/api/login").setContentType("application/x-www-form-urlencoded");

        Promise<WS.Response> responsePromise = holder.post("request-json=" + this.apiKey);

        Promise<JsonNode> jsonPromise = responsePromise.map(
            new Function<WS.Response, JsonNode>() {
                public JsonNode apply(WS.Response response) {
                    JsonNode json = response.asJson();
                    //AstrometryApiHelper.this.apiKeyResponse = json;
                    return json;
                }
            }
        );

        return jsonPromise;
    }


    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public JsonNode getSubmissionIdJsonResponse() {
        return submissionIdJsonResponse;
    }

    public void setSubmissionIdJsonResponse(JsonNode submissionIdJsonResponse) {
        this.submissionIdJsonResponse = submissionIdJsonResponse;
    }

    public JsonNode getImageSubmissionJsonResponse() {
        return imageSubmissionJsonResponse;
    }

    public void setImageSubmissionJsonResponse(JsonNode imageSubmissionJsonResponse) {
        this.imageSubmissionJsonResponse = imageSubmissionJsonResponse;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public JsonNode getLoginJsonResponse() {
        return loginJsonResponse;
    }

    public void setLoginJsonResponse(JsonNode loginJsonResponse) {
        this.loginJsonResponse = loginJsonResponse;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public JsonNode getJobstatusJsonResponse() {
        return jobstatusJsonResponse;
    }

    public void setJobstatusJsonResponse(JsonNode jobstatusJsonResponse) {
        this.jobstatusJsonResponse = jobstatusJsonResponse;
    }

    public JsonNode getImageAnnotationsJsonResponse() {
        return imageAnnotationsJsonResponse;
    }

    public void setImageAnnotationsJsonResponse(JsonNode imageAnnotationsJsonResponse) {
        this.imageAnnotationsJsonResponse = imageAnnotationsJsonResponse;
    }

    public String getImageAnnotations() {
        return imageAnnotations;
    }

    public void setImageAnnotations(String imageAnnotations) {
        this.imageAnnotations = imageAnnotations;
    }

    public List<JsonNode> getImageJsonAnnotations() {
        return imageJsonAnnotations;
    }

    public void setImageJsonAnnotations(List<JsonNode> imageJsonAnnotations) {
        this.imageJsonAnnotations = imageJsonAnnotations;
    }
}
