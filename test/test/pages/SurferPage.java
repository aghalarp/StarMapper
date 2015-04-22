package test.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
 * @author David
 */
@SuppressWarnings("unused")
public class SurferPage extends BasePage {

  /**
   * Create the IndexPage.
   * 
   * @param webDriver The driver.
   * @param port The port.
   * @param slug The slug of the surfer.
   */
  public SurferPage(WebDriver webDriver, int port, String slug) {
    super(webDriver, "http://localhost", port, "/surfer/" + slug);
  }

  @Override
  public void isAt() {
    assertThat(title()).isEqualTo("Surferpedia - ShowSurfer");
  }
  
  /**
   * Adds the surfer to favorites.
   */
  public void addToFavorites() {
    find("#add-to-favorites").click();
  }
  
  /**
   * Removes the surfer from favorites.
   */
  public void removeFromFavorites() {
    find("#remove-from-favorites").click();
  }
  
  /**
   * Retrieves the surfer's name.
   * @return the surfer's name.
   */
  public String getSurferName() {
    return findFirst("h1").getText();
  }
  
  /**
   * Clicks the edit surfer button.
   */
  public void goToEditSurfer() {
    find("#editLink").click();
  }
  
  /**
   * Clicks the delete surfer button.
   */
  public void ClickDeleteSurfer() {
    find("#deleteLink").click();
  }
}
