package steamautopost;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;


/**
 * MangoDB class used to connect to database.
 * This class will retrieve name or key values.
 * 
 * @author Awe
 * @version 2.0
 * 
 * 2024-12-26: Initial release.
 */
public class MyMongoDB {
    
    private MongoDatabase _database;
    private MongoCollection<Document> _Collection;
    
    /**
     * Constructor method.
     * Will grab config.properties file for secret pathways.
     */
    public MyMongoDB() {
       
        // Retrieve the Mongo URI from environment variables
        String tConnectionString = System.getenv("MONGO_URI");
        
        // Create MongoClient
        MongoClient tMongoClient = MongoClients.create(tConnectionString);
        
       
        // Add a shutdown hook to close the MongoClient
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            if (tMongoClient != null) {
                tMongoClient.close();
                System.out.println("MongoClient closed successfully.");
            }
        }));
        
        try {
            // Connect to the database
            _database = tMongoClient.getDatabase("SteamPost");
            
            // Retrieve Collection from database.
            MongoCollection<Document> tCollection = _database.getCollection("SteamGames");
            _Collection = tCollection;
            
            System.out.println("Connected to the Mongo database successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Print method used for development.
     * No functionality use.
     */
    private void printURLs() {
        MongoCursor<Document> tCursor = _Collection.find()
                    .projection(new Document("game", 1)) // Include 'url', exclude '_id'
                    .iterator();
        
        System.out.println("Games in the collection:");
            while (tCursor.hasNext()) {
                Document tDocu = tCursor.next();
                String tURL = tDocu.getString("game"); // Extract the 'url' field
                System.out.println(tURL);
            }
            tCursor.close();
    }
    
    /**
     * Public method that grabs URL for games.
     * @return MangoCursor
     */
    public MongoCursor getURLDoc() {
        MongoCursor<Document> tCursor = _Collection.find()
                .projection(new Document("url", 1).append("_id", 0)) // ".append("_id", 0)" means to exclude the _id field (0 = exclude). The _id field is included by default, so this explicitly removes it.
                .iterator();
        return tCursor;
    }
    
    /**
     * Method uses parameters and filters through the deatabase and grabs the first result.
     * @param aFieldName - Name of game
     * @param aValue - URL
     * @return Document - A document class to print from.
     */
    public Document findExactMatch(String aFieldName, String aValue){
   
        // Find the document where fieldName equals fieldValue
        Document tFilter = new Document(aFieldName, aValue);
        Document tResults = _Collection.find(tFilter).first(); // Retrieves the first match

        if (tResults != null) {
            return tResults;
         } else {
             System.out.println("No exact match found for " + aFieldName + " = " + aValue);
    }
         return null;
    }
}
    