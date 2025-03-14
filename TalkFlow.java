package TalkFlow_Backend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONObject;

/**
 * Main class for the TalkFlow chatbot application
 */
public class TalkFlow {
    private static Responses responseHandler;
    private static ChatDatabase database;
    
    public static void main(String[] args) {
        System.out.println("Welcome to TalkFlow Chatbot!");
        
        // Initialize components
        responseHandler = new Responses();
        database = new ChatDatabase();
        
        // Simple CLI interaction
        if (args.length > 0 && args[0].equals("--cli")) {
            startCliMode(responseHandler, database);
        } else {
            startServer(responseHandler, database);
        }
    }

    private static void startCliMode(TalkFlow_Backend.Responses responseHandler, ChatDatabase database) {
        System.out.println("Starting CLI mode...");
        CLIHandler cliHandler = new CLIHandler(responseHandler, database);
        cliHandler.start();
    }
    
    private static void startServer(Responses responseHandler, ChatDatabase database) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/", new ApiHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Server started on http://localhost:8080");
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }
    
    /**
     * Handle API requests
     */
    private static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                // Add CORS header in all responses
                addCorsHeaders(exchange);
                
                // Handle OPTIONS (CORS preflight)
                if (handleOptionsRequest(exchange)) {
                    return;
                }
                
                // Handle GET
                if (handleGetRequest(exchange)) {
                    return;
                }

                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                // Only POST allowed from here

                // Process POST body
                JSONObject reqJson = readRequestBody(exchange);
                JSONObject respJson = new JSONObject();
                
                // Route action
                String action = reqJson.optString("action", "");
                switch (action) {
                    case "sendMessage":
                        handleSendMessage(reqJson, respJson);
                        break;
                        
                    case "newChat": 
                        handleNewChat(respJson);
                        break;
                        
                    case "clearHistory":
                        handleClearHistory(respJson);
                        break;
                        
                    case "getChatHistory":
                        handleGetChatHistory(reqJson, respJson);
                        break;
                        
                    default:
                        respJson.put("error", "Invalid action");
                }
                
                sendJsonResponse(exchange, respJson, 200);
                
            } catch (Exception e) {
                handleError(exchange, e);
            }
        }
        
        private boolean handleOptionsRequest(HttpExchange exchange) throws Exception {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
                exchange.sendResponseHeaders(204, -1);
                return true;
            }
            return false;
        }
        
        private boolean handleGetRequest(HttpExchange exchange) throws Exception {
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                String welcome = "<html><body><h1>Welcome to TalkFlow Chatbot Backend</h1>"
                        + "<p>Please use POST requests to interact with the API.</p></body></html>";
                exchange.getResponseHeaders().add("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, welcome.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(welcome.getBytes(StandardCharsets.UTF_8));
                os.close();
                return true;
            }
            return false;
        }
        
        private void handleSendMessage(JSONObject reqJson, JSONObject respJson) {
            String userMessage = reqJson.optString("message", "");
            String chatId = reqJson.optString("chatId", "");
            if (chatId.isEmpty()) {
                chatId = responseHandler.createNewChat();
                database.createNewChat(chatId);
            }
            String botResponse = responseHandler.getResponse(userMessage);
            database.saveMessage(chatId, "user", userMessage);
            database.saveMessage(chatId, "bot", botResponse);
            respJson.put("chatId", chatId);
            respJson.put("response", botResponse);
        }
        
        private void handleNewChat(JSONObject respJson) {
            String newChatId = responseHandler.createNewChat();
            database.createNewChat(newChatId);
            respJson.put("chatId", newChatId);
        }
        
        private void handleClearHistory(JSONObject respJson) {
            database.clearHistory();
            respJson.put("status", "cleared");
        }
        
        private void handleGetChatHistory(JSONObject reqJson, JSONObject respJson) {
            String chatId = reqJson.optString("chatId", "");
            if (!chatId.isEmpty()) {
                // Get chat messages from the database
                java.util.List<String[]> messages = database.getChatHistory(chatId);
                
                // Convert to JSON array
                org.json.JSONArray msgArray = new org.json.JSONArray();
                for (String[] msg : messages) {
                    org.json.JSONObject msgObj = new org.json.JSONObject();
                    msgObj.put("sender", msg[0]);
                    msgObj.put("content", msg[1]);
                    msgArray.put(msgObj);
                }
                
                respJson.put("chatId", chatId);
                respJson.put("messages", msgArray);
            } else {
                respJson.put("error", "Chat ID is required");
            }
        }
        
        private JSONObject readRequestBody(HttpExchange exchange) throws Exception {
            InputStream is = exchange.getRequestBody();
            String reqBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new JSONObject(reqBody);
        }
        
        private void addCorsHeaders(HttpExchange exchange) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        }
        
        private void sendJsonResponse(HttpExchange exchange, JSONObject json, int statusCode) throws Exception {
            byte[] responseBytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
        
        private void handleError(HttpExchange exchange, Exception e) {
            try {
                e.printStackTrace();
                JSONObject errorJson = new JSONObject();
                errorJson.put("error", e.getMessage());
                sendJsonResponse(exchange, errorJson, 500);
            } catch (Exception ex) {
                System.err.println("Error handling request: " + ex.getMessage());
            }
        }
    }
}
