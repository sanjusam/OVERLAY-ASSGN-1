package cs455.overlay.node;

import cs455.overlay.constants.EventType;
import cs455.overlay.utils.HelperUtils;

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
                generateEventFromInput(command);

//                node.processCommand(command);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void generateEventFromInput(final String command) {
        final EventType eventType = EventType.getEventTypeFromCommand(command.split(" ")[0]);
        if(eventType == null) {
            System.out.println("Unknown command");
            return;
        } else if(eventType == EventType.MESSAGING_NODES_LIST){
            try {
                node.setupOverlay(HelperUtils.getInt(command.split(" ")[1]));
                return;
            } catch (final IOException ioe) {
                System.out.println("Unable to process command " + command);
                return;
            }
        }

    }
}
