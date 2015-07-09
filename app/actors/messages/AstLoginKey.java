package actors.messages;

/**
 * POJO actor message to hold Astrometry login key.
 *
 */
public class AstLoginKey {
    private final String loginKey;

    public AstLoginKey(String key) {
        this.loginKey = key;
    }

    public String getLoginKey() {
        return this.loginKey;
    }
}
