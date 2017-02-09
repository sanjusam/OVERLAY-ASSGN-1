package cs455.overlay.constants;

import java.util.HashMap;
import java.util.Map;

public enum EventType {
    DEFAULT (0, "default", "default"),
    REGISTER_REQUEST(1,"register", "reg"),
    REGISTER_RESPONSE (2,"response","res"),
    DEREGISTER_REQUEST(3,"deregister", "dereg"),
    MESSAGING_NODES_LIST(4,"setup-overlay", "setup-overlay"),
    Link_Weights(5,"link-weights", "weights"),
    SIGNAL_TO_START_MSG(6, "start", "start"),
    LIST_MSG_NODES(7, "list-messaging nodes", "ls node"),
    LIST_WEIGHTS(8, "list-weights", "ls wt"),
    SEND_LINK_WEIGHTS(9, "send-overlay-link-weights", "send wt"),
    TASK_COMPLETE(10, "Task Complete", "task complete"),
    PRINT_SHORT_PATH(11, "print-shortest-path","print-shortest-path"),
    EXIT_OVERLAY(12, "exit-overlay","exit-overlay"),
    PULL_TRAFFIC_SUMMARY(13, "pull-traffic-summary","pull-sum"),
    TRAFFIC_SUMMARY(14, "pull-traffic-summary","pull-sum"),
    TASK_INITIATE(15, "task-initiate","task-init");

    private final int value;
    private final String longCommand;
    private final String shortCmd;
    EventType(final int value, final String longCommnd, final String shortCmd) {
        this.value = value;
        this.longCommand = longCommnd;
        this.shortCmd = shortCmd;
    }

    private static final Map<Integer, EventType> commandMap = new HashMap<Integer, EventType>(EventType.values().length);

    static {
        for (final EventType eventType : EventType.values()) {
            commandMap.put(eventType.getValue(), eventType);
        }
    }

    public static EventType getEvent(final int value) {
        return commandMap.get(value);
    }


    public static EventType getEventTypeFromCommand(final String command) {
        for(Integer current : commandMap.keySet()) {
            final EventType eventType = commandMap.get(current);
            if(eventType.getLongCommand().toLowerCase().equals(command.toLowerCase()) ||
                    eventType.getShortCommand().toLowerCase().equals(command.toLowerCase()))  {
                return eventType;
            } else if (eventType.getLongCommand().toLowerCase().startsWith(command.toLowerCase())) {
                return eventType;
            }
        }
        return null;
    }

    public int getValue() {return value; }
    public String getLongCommand() { return longCommand; }
    public String getShortCommand() {return shortCmd; }





}
