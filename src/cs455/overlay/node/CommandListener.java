package cs455.overlay.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandListener implements Runnable {
    private final Node node;
    private final String promptString;

    CommandListener(final Node node) {
        this.node = node;
        this.promptString = "Enter the command :  # ";  //TODO :: Deal with prompt later
    }

    @Override
    public void run() {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String command;
        while (true) {
            System.out.print(promptString);
            try {
                command = bufferedReader.readLine();
                if(command == null || command.trim().isEmpty()) {
                    continue;
                }
                node.processCommand(command);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
