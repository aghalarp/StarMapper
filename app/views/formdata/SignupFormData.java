package views.formdata;

import java.util.ArrayList;
import java.util.List;
import play.data.validation.ValidationError;

/**
 * Backing class for the login form.
 */
public class SignupFormData {

  /**
   * Response text with invalid validation.
   */
  public static final String ERROR_TEXT = "Email and password are required.";
  /**
   * Response text when signup succeeds.
   */
  public static final String SUCCESS_TEXT = "Signup successful.";

  /** The submitted email. */
  public String email = "";
  /** The submitted password. */
  public String password = "";

  /** Required for form instantiation. */
  public SignupFormData() {
  }

  /**
   * Validates Form<LoginFormData>. Called automatically in the controller by bindFromRequest(). Checks to see that
   * email and password are valid credentials.
   * 
   * @return Null if valid, or a List[ValidationError] if problems found.
   */
  public List<ValidationError> validate() {

    List<ValidationError> errors = new ArrayList<>();

    if (email == null || email.length() == 0) {
      errors.add(new ValidationError("email", ""));
    }

    if (password == null || password.length() == 0) {
      errors.add(new ValidationError("password", ""));
    }

    return (errors.size() > 0) ? errors : null;
  }

}
