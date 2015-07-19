package actors.messages;

/**
 * Created by David on 7/11/15.
 */
public class StarmapUpdateAWS {
    private final Long starMapId;
    private final String AwsUrl;

    public StarmapUpdateAWS(Long starMapId, String awsUrl) {
        this.starMapId = starMapId;
        AwsUrl = awsUrl;
    }

    public Long getStarMapId() {
        return starMapId;
    }

    public String getAwsUrl() {
        return AwsUrl;
    }
}
