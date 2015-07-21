package utils;

import akka.dispatch.Futures;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import play.Logger;
import play.Play;
import play.mvc.WebSocket;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class AwsS3Utils {

    /**
     * Utility method to upload file to S3. Note that this method blocks until the upload has completed or failed.
     *
     * @param file The file to upload.
     * @return
     */
    public static String uploadFile(final File file) {
        String awsAccessKey = Play.application().configuration().getString("aws.access.key");
        String awsSecretKey = Play.application().configuration().getString("aws.secret.key");
        final String s3BucketName = "starmapper-app";

        AWSCredentials awsCred = new BasicAWSCredentials(awsAccessKey, awsSecretKey);

        TransferManager tm = new TransferManager(awsCred);

        //final Upload upload = tm.upload(s3BucketName, filename, file);
        final Upload upload = tm.upload(s3BucketName, file.getName(), file);

        // Create and set anonymous ProgressListener
        upload.addProgressListener(new ProgressListener() {
            @Override
            public void progressChanged(ProgressEvent progressEvent) {
                Logger.debug("\rS3 Upload Progress (" + file.getName() + "): " + upload.getProgress().getPercentTransferred() + "%");

                switch(progressEvent.getEventType()) {
                    case TRANSFER_COMPLETED_EVENT:
                        Logger.debug("\n" + file.getName() + " has been successfully uploaded to S3.");
                        break;
                    case TRANSFER_FAILED_EVENT:
                        Logger.debug("\n" + file.getName() + " upload failed.");
                        break;
                }
            }
        });

        try {
            upload.waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        tm.shutdownNow();

        if (upload.isDone()) {
            return "https://s3.amazonaws.com/" + s3BucketName + "/" + file.getName();
        } else {
            return null;
        }
    }

    /**
     * Utility method to upload file to S3 with given file name and contentType metadata.
     * Note that this method blocks until upload has completed or failed.
     *
     * @param file The file to upload.
     * @param filename The real file name, which is also used as the S3 object key name.
     * @param contentType The contentType of the file to set on S3.
     * @return
     */
    public static String uploadFile(final File file, final String filename, String contentType) {
        String awsAccessKey = Play.application().configuration().getString("aws.access.key");
        String awsSecretKey = Play.application().configuration().getString("aws.secret.key");
        final String s3BucketName = "starmapper-app";

        AWSCredentials awsCred = new BasicAWSCredentials(awsAccessKey, awsSecretKey);

        TransferManager tm = new TransferManager(awsCred);

        final Upload upload = tm.upload(s3BucketName, filename, file);

        // Create and set anonymous ProgressListener
        upload.addProgressListener(new ProgressListener() {
            @Override
            public void progressChanged(ProgressEvent progressEvent) {
                System.out.print("\rS3 Upload Progress (" + filename + "): " + upload.getProgress().getPercentTransferred() + "%");

                switch(progressEvent.getEventType()) {
                    case TRANSFER_COMPLETED_EVENT:
                        System.out.println("\n" + filename + " has been successfully uploaded to S3.");
                        break;
                    case TRANSFER_FAILED_EVENT:
                        System.out.println("\n" + filename + " upload failed.");

                }
            }
        });

        try {
            upload.waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        tm.shutdownNow();

        if (upload.isDone()) {
            // Set proper content-type metadata (file is currently octet-stream).
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            setMetadata(metadata, s3BucketName, filename);

            return "https://s3.amazonaws.com/" + s3BucketName + "/" + filename;
        } else {
            return null;
        }
    }

    /**
     * Utility method to upload file to S3. Method is non-blocking and returns a Future.
     *
     * @param file The file to upload.
     * @return A Future holding the resulting s3 url for the uploaded file, or an AmazonClientException on failure.
     */
    public static Future<String> uploadFileAsync(final File file) {
        // Create empty promise, which will hold the upload status. We will return its future.
        final Promise<String> promise = Futures.promise();

        final String awsAccessKey = Play.application().configuration().getString("aws.access.key");
        final String awsSecretKey = Play.application().configuration().getString("aws.secret.key");
        final String s3BucketName = "starmapper-app";

        final AWSCredentials awsCred = new BasicAWSCredentials(awsAccessKey, awsSecretKey);

        final TransferManager tm = new TransferManager(awsCred);

        //final Upload upload = tm.upload(s3BucketName, filename, file);
        final Upload upload = tm.upload(s3BucketName, file.getName(), file);


        // Create and set anonymous ProgressListener
        upload.addProgressListener(new ProgressListener() {
            @Override
            public void progressChanged(ProgressEvent progressEvent) {

                //System.out.print("S3 Upload Progress (" + file.getName() + "): " + upload.getProgress().getPercentTransferred() + "%");

                switch(progressEvent.getEventType()) {
                    case TRANSFER_COMPLETED_EVENT:
                        // Complete the promise with the newly created s3 url for the file.
                        //Logger.debug(file.getName() + " has been successfully uploaded to S3.");
                        promise.success("https://s3.amazonaws.com/" + s3BucketName + "/" + file.getName());
                        break;
                    case TRANSFER_FAILED_EVENT:
                        //Logger.debug(file.getName() + " upload failed.");

                        try {
                            // Shouldn't block because transfer already finished at this point.
                            AmazonClientException ex = upload.waitForException();
                            // Complete the promise with the exception.
                            promise.failure(ex);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        break;
                }
            }
        });

        // Return the future associated with the promise.
        return promise.future();
    }

    public static void setMetadata(ObjectMetadata metadata, String bucket, String key) {
        String awsAccessKey = Play.application().configuration().getString("aws.access.key");
        String awsSecretKey = Play.application().configuration().getString("aws.secret.key");

        AWSCredentials awsCred = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        TransferManager tm = new TransferManager(awsCred);

        // While strange, we are setting the new meta-data by remaking the file.
        final CopyObjectRequest request = new CopyObjectRequest(bucket, key, bucket, key)
                .withSourceBucketName(bucket)
                .withSourceKey(key)
                .withNewObjectMetadata(metadata);

        Copy copy = tm.copy(request);

        try {
            copy.waitForCopyResult();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        tm.shutdownNow();

        if (copy.isDone()) {
            System.out.println(key + " metadata has been set to " + metadata.getContentType());
        }
    }

    /**
     * Utility function to rename any given File. Implementation uses the Path class, which requires JDK 7.
     *
     * @param file The file to rename.
     * @param newFileName The new file name.
     * @return The renamed File object, or null if errors occurred.
     */
    public static File renameFile(File file, String newFileName) {
        // Get Path representation of File.
        Path path = file.toPath();
        System.out.println(path);
        Path newPath = Paths.get(newFileName);
        System.out.println(newPath);

        // Check to see that the new file name has not already been set.
        if (!file.getName().equals(newFileName)) {
            try {
                // Renaming is done via platform dependent move command.
                // We opt for Files.move() rather than File.renameTo() because the former seems to be more reliable on
                // different platforms.
                // Note: Changed to copy() command so that we can hold on to original file.
                newPath = Files.copy(path, newPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println(newPath);
                File newFile = newPath.toFile();

                newFile.deleteOnExit();

                return newFile;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
