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
public class NewSurferPage extends BasePage {

  /**
   * Create the LoginPage.
   * 
   * @param webDriver The driver.
   * @param port The port.
   */
  public NewSurferPage(WebDriver webDriver, int port) {
    super(webDriver, "http://localhost", port, "/new");
  }

  @Override
  public void isAt() {
    assertThat(title()).isEqualTo("Surferpedia - NewSurfer");
  }
  
  public void fillSurferForm(String slug, String name, String home, String awards, String carouselImgUrl, String bioImgUrl,
      String biography, String surferType, String footstyleType, String country) {
    
    fill("#slug").with(slug);
    fill("#name").with(name);
    fill("#home").with(home);
    fill("#awards").with(awards);
    fill("#carouselImgUrl").with(carouselImgUrl);
    fill("#bioImgUrl").with(bioImgUrl);
    fill("#biography").with(biography);
    find("select", withId("surferType")).find("option", withText().equalTo(surferType)).click();
    find("div", withId("footstyleTypes")).findFirst("input", withId(footstyleType)).click();
    fill("#country").with(country);
    //System.out.println(findFirst("form"));
    //findFirst("form").submit();
    submit("#submitSurfer");
  }
  
  public void login(String email, String pass) {
    // Fill the input field with id "email" with the passed name string.
    fill("#email").with(email);
    // Fill the input field with id "password" with the passed pass string.
    fill("#password").with(pass);
    
    // Submit the form whose id is "submit"
    findFirst("form").submit();
  }
}
