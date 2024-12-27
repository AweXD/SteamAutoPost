package steamautopost;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the driver class starts that starts SeleniumMongoConnector. 
 * 
 * @author Awe
 * @version 2.0
 * 
 * 2024-12-25: Initial release.
 */
public class SteamAutoPost {    
    /**
     * Main method driver.
     * @param args 
     */
    public static void main(String[] args) {
        // TODO code application logic here
        SeleniumMongoConnector tSelenium = new SeleniumMongoConnector();
        try {
            tSelenium.driveSelenium();
        } catch (Exception ex) {
            Logger.getLogger(SteamAutoPost.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
