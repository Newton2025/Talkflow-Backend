package TalkFlow_Backend;

import java.util.Scanner;

/**
 * Handles command-line interface interactions for TalkFlow
 */
public class CLIHandler {
    private final Responses responseHandler;
    private final ChatDatabase database;
    private String currentChatId;
    private final Scanner scanner;

    public CLIHandler(Responses responseHandler, ChatDatabase database) {
        this.responseHandler = responseHandler;
        this.database = database;
        this.scanner = new Scanner(System.in);
        this.currentChatId = responseHandler.createNewChat();
        database.createNewChat(this.currentChatId);
    }

    /**
     * Starts the CLI interaction loop
     */
    public void start() {
        // Changed input loop
        while (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
                // Save user message to database
                database.saveMessage(currentChatId, "user", input);

                // Get response and save bot response
                String response = responseHandler.getResponse(input);
                database.saveMessage(currentChatId, "bot", response);
        }
        scanner.close();
    }
}
