package views.formdata;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import play.Play;
import play.data.validation.ValidationError;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * FormData class that handles the image upload form.
 */
public class ImageUploadFormData {
    public String imageSourceType = ""; //Will either be "url" or "local".
    public String imageSourceUrl = "";


    /**
     * Required empty constructor.
     */
    public ImageUploadFormData() {

    }

    public String uploadFile(File file) {
        String awsAccessKey = Play.application().configuration().getString("aws.access.key");
        String awsSecretKey = Play.application().configuration().getString("aws.secret.key");
        String s3BucketName = "starmapper-app";

        AWSCredentials awsCred = new BasicAWSCredentials(awsAccessKey, awsSecretKey);

        TransferManager tm = new TransferManager(awsCred);

        Upload upload = tm.upload(s3BucketName, file.getName(), file);

        try {
            upload.waitForCompletion();
            System.out.println("Upload Complete.");
            return "https://s3.amazonaws.com/" + s3BucketName + "/" + file.getName();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Validates Form<ImageUploadFormData>. Called automatically in the controller by bindFromRequest().
     *
     * @return Null if valid, or a List[ValidationError] if problems found.
     */
    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (imageSourceType.equals("url") && (imageSourceUrl == null || imageSourceUrl.length() == 0)) {
            errors.add(new ValidationError("imageSourceUrl", "Please enter a URL."));
        }

        return (errors.size() > 0) ? errors : null;
    }
}
