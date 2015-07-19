package actors.messages;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by David on 7/11/15.
 */
public class AstrometryResultData {
    private final Long starMapId;
    private final String astSessionKey;
    private final String astSubmissionId;
    private final String astJobId;
    private final JsonNode astImageAnnotations;

    public AstrometryResultData(Long starMapId, String astSessionKey, String astSubmissionId, String astJobId, JsonNode astImageAnnotations) {
        this.starMapId = starMapId;
        this.astSessionKey = astSessionKey;
        this.astSubmissionId = astSubmissionId;
        this.astJobId = astJobId;
        this.astImageAnnotations = astImageAnnotations;
    }

    public Long getStarMapId() {
        return starMapId;
    }

    public String getAstSessionKey() {
        return astSessionKey;
    }

    public String getAstSubmissionId() {
        return astSubmissionId;
    }

    public String getAstJobId() {
        return astJobId;
    }

    public JsonNode getAstImageAnnotations() {
        return astImageAnnotations;
    }
}
