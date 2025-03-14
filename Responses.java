package TalkFlow_Backend;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles communication with the Gemini API
 */
public class Responses {
    private static final String API_KEY = "AIzaSyBaXnyJwXysDZ4uu3-hq8-Pfu_ccCnPNf0";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private final HttpClient client;
    private final Map<String, String> chatContexts;
    
    public Responses() {
        this.client = HttpClient.newHttpClient();
        this.chatContexts = new HashMap<>();
    }
    
    /**
     * Gets a response from the Gemini API
     * @param message The user's message
     * @return The AI's response
     */
    public String getResponse(String message) {
        try {

//            String prompt = "Based on Indian festivals, I will provide you with some dates. " +
//                    "Read the date and identify the Indian festival. " +
//                    "Respond in exactly five words. " +
//                    "The date is: ";

            // Check for special queries
            String lowerMessage = message.toLowerCase();
            
            // Check if asking about creator/developer
            if (lowerMessage.contains("who created you") || 
                lowerMessage.contains("who made you") || 
                lowerMessage.contains("who developed you") ||
                lowerMessage.contains("who is your creator") ||
                lowerMessage.contains("your developer") ||
                (lowerMessage.contains("made") && lowerMessage.contains("you")) ||
                (lowerMessage.contains("built") && lowerMessage.contains("you"))) {
                
                return "I am a large language model, trained by Google and created by Ravish Kumar Tiwari. "
                     + "You can visit his LinkedIn profile, GitHub repositories, or Portfolio website for more information.";
            }
            
            // Check if asking specifically for contact information
            if (lowerMessage.contains("linkedin") || lowerMessage.contains("github") || 
                lowerMessage.contains("portfolio") || lowerMessage.contains("contact")) {
                
                return "Here are Ravish Kumar Tiwari's professional profiles:\n\n"
                     + "• LinkedIn: https://www.linkedin.com/in/ravish-kumar-tiwari/\n"
                     + "• GitHub: https://github.com/Newton2025\n"
                     + "• Portfolio: https://rktportfolio.me/";
            }
            
            // Build request body for Gemini API exactly as shown in the curl example
            JSONObject requestBody = new JSONObject();
            
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", message);
            parts.put(part);
            
            content.put("parts", parts);
            contents.put(content);
            
            requestBody.put("contents", contents);
            
            System.out.println("Sending to Gemini API: " + requestBody.toString(2));
            
            // Send request to Gemini API
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "?key=" + API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
                
            HttpResponse<String> response = client.send(request, 
                HttpResponse.BodyHandlers.ofString());
                
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                System.out.println("Received response from Gemini API");
                return extractResponseText(jsonResponse);
            } else {
                System.err.println("API Error: " + response.statusCode() + " - " + response.body());
                return "Sorry, I encountered an error with the Gemini API. Status code: " + 
                       response.statusCode() + "\nResponse: " + response.body();
            }
        } catch (Exception e) {
            System.err.println("Exception calling Gemini API: " + e.getMessage());
            e.printStackTrace();
            return "Sorry, I encountered an error while processing your request: " + e.getMessage();
        }
    }
    
    /**
     * Extract response text from Gemini API JSON response
     */
    private String extractResponseText(JSONObject response) {
        try {
            if (!response.has("candidates") || response.getJSONArray("candidates").length() == 0) {
                System.err.println("No candidates in response");
                return "Sorry, I didn't receive a proper response from the AI.";
            }
            
            JSONObject candidate = response.getJSONArray("candidates").getJSONObject(0);
            if (!candidate.has("content")) {
                System.err.println("No content in candidate");
                return "Sorry, the AI response was empty.";
            }
            
            JSONObject content = candidate.getJSONObject("content");
            if (!content.has("parts") || content.getJSONArray("parts").length() == 0) {
                System.err.println("No parts in content");
                return "Sorry, the AI response had no content parts.";
            }
            
            return content.getJSONArray("parts").getJSONObject(0).getString("text");
        } catch (Exception e) {
            System.err.println("Error parsing response: " + e.getMessage());
            e.printStackTrace();
            return "Sorry, I had trouble understanding the API response: " + e.getMessage();
        }
    }
    
    /**
     * Creates a new chat session
     * @return The session ID
     */
    public String createNewChat() {
        // Generate a unique session ID
        return "session_" + System.currentTimeMillis();
    }
}
