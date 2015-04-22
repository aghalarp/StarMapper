package test;

import java.util.List;
import org.junit.Test;
import controllers.Application;
import play.test.TestBrowser;
import play.libs.F.Callback;
import test.pages.EditSurferPage;
import test.pages.IndexPage;
import test.pages.LoginPage;
import test.pages.NewSurferPage;
import test.pages.SignupPage;
import test.pages.SurferPage;
import views.formdata.LoginFormData;
import static play.test.Helpers.FIREFOX;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.testServer;
import static play.test.Helpers.running;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Integration tests running on an instance of the application.
 */
public class IntegrationTest {
  /** The port number for the integration test. */
  private static final int PORT = 3333;

  /**
   * Test that makes sure the Index page is retrievable.
   */
  @Test
  public void testIndex() {
    running(testServer(PORT, fakeApplication(inMemoryDatabase())), FIREFOX, new Callback<TestBrowser>() {
      public void invoke(TestBrowser browser) {
        IndexPage indexPage = new IndexPage(browser.getDriver(), PORT);
        browser.goTo(indexPage);
        assertThat(browser.pageSource()).contains("Surferpedia");
      }
    });
  }
  
  /**
   * Login test that logs in.
   */
  @Test
  public void testLogin() {
    running(testServer(PORT, fakeApplication(inMemoryDatabase())), FIREFOX, new Callback<TestBrowser>() {
      public void invoke(TestBrowser browser) {
        LoginPage loginPage = new LoginPage(browser.getDriver(), PORT);
        browser.goTo(loginPage);
        assertThat(browser.pageSource()).contains("Please log in");
        
        // test incorrect logins.
        loginPage.login("test", "incorrect");
        assertThat(browser.pageSource()).contains(LoginFormData.ERROR_TEXT);
        
        // test correct logins
        loginPage.login(Application.adminEmail, Application.adminPassword);
        assertThat(browser.pageSource()).contains(Application.adminEmail);
      }
    });
  }
  
  /**
   * Test that signup page is retrievable.
   */
  @Test
  public void testRetrieveSignupPage() {
    running(testServer(PORT, fakeApplication(inMemoryDatabase())), FIREFOX, new Callback<TestBrowser>() {
      public void invoke(TestBrowser browser) {
        //Start at homepage.
        IndexPage indexPage = new IndexPage(browser.getDriver(), PORT);
        browser.goTo(indexPage);
        indexPage.isAt();
        indexPage.goToSignup();
        
        //Goto sign up page.
        SignupPage signupPage = new SignupPage(browser.getDriver(), PORT);
        browser.goTo(signupPage);
        signupPage.isAt();
      }
    });
  }
  
  /**
   * Test that user can successfully sign up.
   */
  @Test
  public void testSignup() {
    running(testServer(PORT, fakeApplication(inMemoryDatabase())), FIREFOX, new Callback<TestBrowser>() {
      public void invoke(TestBrowser browser) {
        //Start at homepage.
        IndexPage indexPage = new IndexPage(browser.getDriver(), PORT);
        browser.goTo(indexPage);
        indexPage.isAt();
        indexPage.goToSignup();
        
        //Goto sign up page.
        SignupPage signupPage = new SignupPage(browser.getDriver(), PORT);
        browser.goTo(signupPage);
        signupPage.isAt();
        
        //Fill in signup form and submit
        signupPage.login("test@hawaii.edu", "password123");
        assertThat(browser.pageSource()).contains("Signup successful.");
        
        //Goto login page.
        LoginPage loginPage = new LoginPage(browser.getDriver(), PORT);
        browser.goTo(loginPage);
        loginPage.isAt();
        
        //Sign in with new account.
        loginPage.login("test@hawaii.edu", "password123");
        assertThat(browser.pageSource()).contains("test@hawaii.edu");
      }
    });
  }
  

}
