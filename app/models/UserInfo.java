package models;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;

import play.db.ebean.Model;

/**
 * A simple representation of a user.
 *
 * @author David A.
 */
@Entity
public class UserInfo extends Model {

  /**
   * The EBean ORM finder method for database queries on ID.
   * 
   * @return The finder method for UserInfo.
   */
  public static Finder<Long, UserInfo> find() {
    return new Finder<Long, UserInfo>(Long.class, UserInfo.class);
  }

  @Id
  private long id;

  private String type;
  private String email; // Acts as user name.
  private String password;


  //Represents the many starmaps of the user.
  @OneToMany(mappedBy="user", cascade = CascadeType.ALL)
  private List<StarMap> starmaps = new ArrayList<>();

  /**
   * Creates a new UserInfo instance.
   * 
   * @param type The name.
   * @param email The email.
   * @param password The password.
   */
  public UserInfo(String type, String email, String password) {
    this.type = type;
    this.email = email;
    this.password = password;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  /**
   * @return the name
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the name to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * @param email the email to set
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return this.password;
  }

  /**
   * @param password the password to set
   */
  public void setPassword(String password) {
    this.password = password;
  }

  public List<StarMap> getStarmaps() {
    return starmaps;
  }

  public void setStarmaps(List<StarMap> starmaps) {
    this.starmaps = starmaps;
  }

  /**
   * Returns true if the email represents a known user.
   *
   * @param email The email.
   * @return True if known user.
   */
  public static boolean isUser(String email) {
    UserInfo userInfo = UserInfo.find().where().eq("email", email).findUnique();
    return userInfo != null ? true : false;
  }

}
