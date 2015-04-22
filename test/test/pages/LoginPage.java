package test.pages;

import java.util.ArrayList;
import java.util.List;
import org.fluentlenium.core.FluentPage;
import org.openqa.selenium.WebDriver;
// Although Eclipse marks the following two methods as deprecated, 
// the no-arg versions of the methods used here are not deprecated.  (as of May, 2013).
import static org.fluentlenium.core.filter.FilterConstructor.withText;
import static org.fluentlenium.core.filter.FilterConstructor.withId;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Illustration of the Page Object Pattern in Fluentlenium.
 * 
 * @author Philip Johnson
 * @author Kevin
 */
@SuppressWarnings("unused")
public class LoginPage extends BasePage {

  /**
   * Create the LoginPage.
   * 
   * @param webDriver The driver.
   * @param port The port.
   */
  public LoginPage(WebDriver webDriver, int port) {
    super(webDriver, "http://localhost", port, "/login");
  }

  @Override
  public void isAt() {
    assertThat(title()).isEqualTo("Surferpedia - Login");
  }

  public void login(String email, String pass) {
    // Fill the input field with id "email" with the passed name string.
    fill("#email").with(email);
    // Fill the input field with id "password" with the passed pass string.
    fill("#password").with(pass);
    
    // Submit the form whose id is "submit"
    submit("input[type=submit]");
  }
}
