package models;

import com.avaje.ebean.annotation.CreatedTimestamp;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * StarMap class.
 */
@Entity
public class StarMap extends Model {
    @Id
    private long id;

    @Column(name="created_date")
    @CreatedTimestamp
    private Date createdDate;

    private String s3imageUrl; // Stored S3 image url
    private String submissionId; //Astrometry API subid
    private String jobId; //Astrometry API jobid
    private String jobStatus; //Success, Solving, or Failure.

    @Lob
    private String imageAnnotations; //Astrometry API image data


    @ManyToOne
    private UserInfo user;

    @ManyToMany
    private List<Star> stars;

    @OneToMany(mappedBy="starMap", cascade = CascadeType.ALL)
    private List<Coordinate> coordinates;



    /**
     * The EBean ORM finder method for database queries on ID.
     *
     * @return The finder method for StarMaps.
     */
    public static Finder<Long, StarMap> find() {
        return new Finder<Long, StarMap>(Long.class, StarMap.class);
    }

    /**
     * Constructor to create new StarMap instance for a given user.
     * @param user
     */
    public StarMap(UserInfo user) {
        this.user = user;
    }

    /**
     * Convenience constructor to be used for testing.
     *
     * @param s3imageUrl
     * @param submissionId
     * @param jobId
     */
    public StarMap(String s3imageUrl, String submissionId, String jobId, File image) {
        this.s3imageUrl = s3imageUrl;
        this.submissionId = submissionId;
        this.jobId = jobId;
    }

    public void addStar(Star star) {

        if (!this.stars.contains(star)) {
            stars.add(star);
        }

    }

    /**
     * Converts File to byte array.
     *
     * @param file The file to convert.
     * @return The byte array representation of the input file.
     */
    public byte[] fileToBytes(File file) throws IOException {
        InputStream fis = new BufferedInputStream(new FileInputStream(file));

        ByteArrayOutputStream byteOS = new ByteArrayOutputStream(); //Byte stream
        OutputStream bufOS = new BufferedOutputStream(byteOS); //Buffer the above byte stream for efficiency.

        byte[] buf = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = fis.read(buf)) != -1) { //Read data from input stream into buf. Returns # of bytes read.
            bufOS.write(buf, 0, bytesRead); //Then write the buf data into output stream.
        }

        fis.close();
        bufOS.close();

        return byteOS.toByteArray();
    }

    public static StarMap getStarmap(Long id) {
        return StarMap.find().where().eq("id", id).findUnique();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getS3imageUrl() {
        return s3imageUrl;
    }

    public void setS3imageUrl(String s3imageUrl) {
        this.s3imageUrl = s3imageUrl;
    }

    public String getImageAnnotations() {
        return imageAnnotations;
    }

    public void setImageAnnotations(String imageAnnotations) {
        this.imageAnnotations = imageAnnotations;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public List<Star> getStars() {
        return stars;
    }

    public void setStars(List<Star> stars) {
        this.stars = stars;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Coordinate> coordinates) {
        this.coordinates = coordinates;
    }
}
