package actors.messages;

/**
 * POJO actor message to hold Astrometry session key.
 *
 */
public class AstSessionKey {
    private final String sessionKey;

    public AstSessionKey(String key) {
        this.sessionKey = key;
    }

    public String getLoginKey() {
        return this.sessionKey;
    }
}
