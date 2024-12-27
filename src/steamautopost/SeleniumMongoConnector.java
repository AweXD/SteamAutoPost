package steamautopost;

import com.mongodb.client.MongoCursor;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.bson.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;


/**
 * This starts Selenium or Firefox.
 * Loads your cookie (SQL) to bypass steam 2FA.
 * Connects to your Mongo database.
 * Cycle through the database and finds expired forum posts.
 * Posts forum if expired.
 * Closes everything after done.
 * Shutdown hooks triggered to free resources.
 * 
 * @author Awe
 * @version 2.0
 * 
 * 2024-12-26: Initial release.
 */
public class SeleniumMongoConnector {
    
    private String _FireFoxCookie; // Your cookies.sqlite path.
    private String _GeckoDriver; // Your pathway to gecko driver.
    private String _FireFox; // Your pathwy to firefox.
    private MyMongoDB _Mongo = new MyMongoDB();
    
    /**
     * Constructor method.
     * Instantiates Properties file and reads it.
     * Defines these data from property file.
     * _FireFoxCookie - Your cookies.sqlite path.
     * _GeckoDriver - Your pathway to gecko driver.
     */
    public SeleniumMongoConnector(){
         Properties tProperties = new Properties();
        
        try {
            // Load the properties file
            FileInputStream tFis = new FileInputStream("config.properties");
            tProperties.load(tFis);
            
            // Retrieve values using keys
            String tPathToCookie = tProperties.getProperty("firefoxCookie");
            String tPathToGecko = tProperties.getProperty("geckoDriverPath");
            String tDataBaseName = tProperties.getProperty("databaseName"); // TODO: Remove or Hide DB name
            String tFireFox = tProperties.getProperty("firefox");
            _FireFoxCookie = tPathToCookie;
            _GeckoDriver = tPathToGecko;
            _FireFox = tFireFox;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load FireFox cookies from steamcommunity.com.
     *
     * @return List<Cookie> - A list of Cookie Objects.
     */
    public List<Cookie> fetchCookiesFromFirefox() {
        List<Cookie> tCookies = new ArrayList<>();

        try ( Connection tConnection = DriverManager.getConnection("jdbc:sqlite:" + _FireFoxCookie)) {
            System.out.println("Connected to gecko driver successfully.");

            String tSQL = "SELECT name, value FROM moz_cookies WHERE host LIKE '%.steamcommunity.com' OR host LIKE 'steamcommunity.com'";

            try ( Statement stmt = tConnection.createStatement();  ResultSet rs = stmt.executeQuery(tSQL)) {
                while (rs.next()) {
                    String tName = rs.getString("name");
                    String tValue = rs.getString("value");

                    // Add the cookie to the list
                    tCookies.add(new Cookie(tName, tValue));
                }
            }
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            e.printStackTrace();
        }
        return tCookies;
    }
    
    /**
     * Post a discussion on current URL from driver. 
     * @param aWebDriver - Selenium driver
     * @param aTitle - Title for forum post
     * @param aBody - Main message or body of post.
     */
    public void postDiscussion(WebDriver aWebDriver, String aTitle, String aBody) {
        
        // Wait for page to load and locate the "Start a New Discussion" button
        aWebDriver.findElement(By.linkText("Start a New Discussion")).click();
        
        // Wait
        aWebDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        
        // Fill out the form
        aWebDriver.findElement(By.name("topic")).sendKeys(aTitle);
        aWebDriver.findElement(By.name("text")).sendKeys(aBody);

        // Submit the discussion
        aWebDriver.findElement(By.cssSelector("button[type='submit']")).click();
        
        // Wait
        aWebDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
    }
    
    /** 
     * Checks the first page of a forum page to see if user posted.
     * 
     * @param aDriver - the gecko driver
     * @param aURL - The URL of forum board.
     * @return boolean - if user was found or not to be found.
     */
    public boolean checkIfPostExists(WebDriver aDriver, String aURL) {
        aDriver.get(aURL);
        aDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));

        // Find elements containing the post titles (update selector as needed)
        WebElement tForumTopic = aDriver.findElement(By.xpath("//div[starts-with(@id, 'forum_') and contains(@id, '_topics')]"));

        List<WebElement> tDiscussion = tForumTopic.findElements(By.className("forum_topic")); // Replace with actual class name for discussions

        for (WebElement tDiscusions : tDiscussion) {
            String tOriginalPoster =  tDiscusions.findElement(By.className("forum_topic_op")).getText(); // Assuming OP names are in <span>
            
            if (tOriginalPoster.equals("Awe"))
            {
                return true; // Found user post.
            }
        }
        return false; // Post not found
    }
    
    /**
     * Starts Selenium (firefox)
     * This will also start the process of cycling through games on MongoDB.
     * 
     * @throws InterruptedException 
     */
    public void driveSelenium() {
        // Set the path to geckodriver
        System.setProperty("webdriver.gecko.driver", _GeckoDriver);

        // Set the path to Firefox
        FirefoxOptions tOptions = new FirefoxOptions();
        tOptions.setBinary(_FireFox);
        
        // Uncomment below to run selenium in background (no firefox gui)
        tOptions.addArguments("--headless");  // Run Firefox in headless mode

        tOptions.addPreference("general.useragent.override", "Mozilla/5.0 x64");
        
        // Initialize WebDriver with specified options
        WebDriver tDriver = new FirefoxDriver(tOptions);
        
        
        // Register the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (tDriver != null) {
                System.out.println("Gecko driver closed successfully.");
                tDriver.quit();
            }
        }));


        try {            
            // Navigate to the Steam Community Login Page
            tDriver.navigate().refresh();
            tDriver.get("https://steamcommunity.com/login/home/");
            
            // Assuming you've collected cookies from Firefox and stored them in a List
            List<Cookie> tCookies = fetchCookiesFromFirefox(); // Implement this method to get cookies
            
            // Add each cookie to the WebDriver session
            for (Cookie tForEachCookie : tCookies) {
                String tName = tForEachCookie.getName();
                String tValue = tForEachCookie.getValue();
                String tDomain = "steamcommunity.com"; // Use the appropriate domain
                String tPath = "/"; // Use the appropriate path

                // Add the cookie to the WebDriver
                tDriver.manage().addCookie(new Cookie(tName, tValue, tDomain, tPath, null, false, true));
            }

            // Refresh the page to ensure cookies are set
            tDriver.get(tDriver.getCurrentUrl());

            try ( MongoCursor<Document> tCursor = _Mongo.getURLDoc()) { // Automatically closes the cursor
                while (tCursor.hasNext()) {
                    Document tDocument = tCursor.next();
                    String tURL = tDocument.getString("url"); // Extract the 'url' field
                     // Fetch the exact match document from the database
                        Document tDoc = _Mongo.findExactMatch("url", tURL);
                        String tTitle = tDoc.getString("title");
                        String tMsg = tDoc.getString("msg");
                        String tGame = tDoc.getString("game");

                    // Check if post exists
                    if (!checkIfPostExists(tDriver, tURL)) {
                        if (tDoc != null) {
                            // Post the discussion thread
                            postDiscussion(tDriver, tTitle, tMsg);
                            System.out.println(tGame + " No Current Post. \nCreating...");
                            System.out.println(tURL);
                        } else {
                            System.out.println("No details found for game: " + tGame);
                        }
                    } else {
                        System.out.println(tGame + " Post Exists");
                        System.out.println(tURL);
                    }
                }
            } catch (Exception e) {
                System.err.println("An error occurred while processing URLs:");
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}