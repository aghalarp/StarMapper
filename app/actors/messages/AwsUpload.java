package actors.messages;

import java.io.File;

/**
 * Created by David on 7/13/15.
 */
public class AwsUpload {
    private final File file;
    private final String fileName;
    private final Long starMapId;

    public AwsUpload(File file, String fileName, Long starMapId) {
        this.file = file;
        this.fileName = fileName;
        this.starMapId = starMapId;
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getStarMapId() {
        return starMapId;
    }
}
