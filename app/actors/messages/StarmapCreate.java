package actors.messages;

import models.UserInfo;

/**
 * Created by David on 7/11/15.
 */
public class StarmapCreate {
    private final UserInfo user;

    public StarmapCreate(UserInfo user) {
        this.user = user;
    }

    public UserInfo getUser() {
        return user;
    }
}
