package TalkFlow_Backend;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for storing chat history
 */
public class ChatDatabase {
    private Connection connection;
//    private static final String DB_FILE = "TalkFlow.db";

    String url = "jdbc:postgresql://localhost:5432/Talkflow_db";
    String user = "postgres";
    String password = "root";
    
    public ChatDatabase() {
        initializeDatabase();
    }
    
    /**
     * Initializes the SQLite database and creates necessary tables if they don't exist
     */
    private void initializeDatabase() {
        try {
            // Establish connection to SQLite database
            connection = DriverManager.getConnection(url,user,password);
            System.out.println("Database connection established");
            
            // Create tables if they don't exist
            createTables();
            
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }
    
    /**
     * Creates the necessary tables in the database
     */
    private void createTables() throws SQLException {
        Statement statement = connection.createStatement();
        
        // Create chats table
        statement.execute(
            "CREATE TABLE IF NOT EXISTS chats (" +
            "id TEXT PRIMARY KEY, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
        
        // Create messages table
        statement.execute(
            "CREATE TABLE IF NOT EXISTS messages (" +
            "id SERIAL PRIMARY KEY , " +
            "chat_id TEXT, " +
            "sender TEXT, " +
            "content TEXT, " +
            "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (chat_id) REFERENCES chats(id)" +
            ")"
        );
        
        statement.close();
    }
    
    /**
     * Saves a message to the database
     */
    public void saveMessage(String chatId, String sender, String content) {
        try {
            String sql = "INSERT INTO messages (chat_id, sender, content) VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, chatId);
            statement.setString(2, sender);
            statement.setString(3, content);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error saving message: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves chat history for a specific chat session
     */
    public List<String[]> getChatHistory(String chatId) {
        List<String[]> messages = new ArrayList<>();
        
        try {
            String sql = "SELECT sender, content FROM messages WHERE chat_id = ? ORDER BY timestamp ASC";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, chatId);
            ResultSet result = statement.executeQuery();
            
            while (result.next()) {
                String sender = result.getString("sender");
                String content = result.getString("content");
                messages.add(new String[]{sender, content});
            }
            
            result.close();
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving chat history: " + e.getMessage());
        }
        
        return messages;
    }
    
    /**
     * Creates a new chat in the database
     */
    public void createNewChat(String chatId) {
        try {
            String sql = "INSERT INTO chats (id) VALUES (?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, chatId);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error creating new chat: " + e.getMessage());
        }
    }
    
    /**
     * Clears chat history from the database
     */
    public void clearHistory() {
        try {
            Statement statement = connection.createStatement();
            statement.execute("DELETE FROM messages");
            statement.execute("DELETE FROM chats");
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error clearing history: " + e.getMessage());
        }
    }
}
