package actors.messages;

import models.UserInfo;

import java.io.File;

/**
 * Actor message to hold all Starmap submission data (Image, User, etc.)
 */
public class StarmapSubmission {
    private final UserInfo user;
    private final String loginKey;
    private final File file;
    private final String fileName;

    public StarmapSubmission(UserInfo user, String loginKey, File file, String fileName) {
        this.user = user;
        this.loginKey = loginKey;
        this.file = file;
        this.fileName = fileName;
    }

    public UserInfo getUser() {
        return user;
    }

    public String getLoginKey() {
        return loginKey;
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }
}
