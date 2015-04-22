package models;


/**
 * Provides an in-memory repository for UserInfo.
 * 
 * @author Dave
 */
public class UserInfoDB {

  public static final String ADMIN = "Administrator";
  public static final String STANDARD = "Standard";

  /**
   * Adds the specified user to the UserInfoDB.
   * 
   * @param type User type.
   * @param email Their email.
   * @param password Their password.
   */
  public static void addUserInfo(String type, String email, String password) {
    UserInfo userInfo = getUser(email);
    if (userInfo != null) {
      userInfo.setType(type);
      userInfo.setPassword(password);
    }
    else {
      userInfo = new UserInfo(type, email, password);
    }
    userInfo.save();
  }

  /**
   * Returns true if the email represents a known user.
   * 
   * @param email The email.
   * @return True if known user.
   */
  public static boolean isUser(String email) {
    UserInfo userInfo = UserInfo.find().where().eq("email", email).findUnique();
    return userInfo != null;
  }

  /**
   * Returns the UserInfo associated with the email, or null if not found.
   * 
   * @param email The email.
   * @return The UserInfo.
   */
  public static UserInfo getUser(String email) {
    return UserInfo.find().where().eq("email", email).findUnique();
  }

  /**
   * Returns true if email and password are valid credentials.
   * 
   * @param email The email.
   * @param password The password.
   * @return True if email is a valid user email and password is valid for that email.
   */
  public static boolean isValid(String email, String password) {
    return (UserInfo.find().where().eq("email", email).where().eq("password", password).findUnique() != null);
  }


}
