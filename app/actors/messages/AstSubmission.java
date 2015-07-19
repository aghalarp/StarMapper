package actors.messages;

import java.io.File;

/**
 * Created by David on 7/14/15.
 */
public class AstSubmission {
    private final File file;
    private final AstLoginKey loginKey;
    private final Long starMapId;

    public AstSubmission(File file, AstLoginKey loginKey, Long starMapId) {
        this.file = file;
        this.loginKey = loginKey;
        this.starMapId = starMapId;
    }

    public File getFile() {
        return file;
    }

    public AstLoginKey getLoginKey() {
        return loginKey;
    }

    public Long getStarMapId() {
        return starMapId;
    }
}
