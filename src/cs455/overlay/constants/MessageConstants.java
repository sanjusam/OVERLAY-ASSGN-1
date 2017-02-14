package cs455.overlay.constants;

public class MessageConstants {
    public static final String SUCCESSFUL_REGISTRATION = "Registration request successful. " +
            "The number of messaging nodes currently constituting the overlay is (%d)";
    public static final String SUCCESSFUL_DEREGISTRATION = "De-registration request successful. " +
            "The number of messaging nodes currently constituting the overlay is (%d)";

    public static final String NODE_ALREADY_REGISTERED = "Node Registration Failed : Already registered. ";
    public static final String IP_MISMATCH_REGISTRATION = "Node Registration Failed : Ip mismatch ";

    public static final String NODE_NOT_REGISTERED = "Node De Registration Failed : Not registered.";
    public static final String IP_MISMATCH_DEREGISTRATION = "Node Registration Failed : Ip mismatch ";

    public static final String INVALID_PORT_START_FAILURE = "Cannot Start registry - Invalid port number for registry";
    public static final String NODE_PORT_SEPARATOR = ":";
    public static final String NODE_PATH_SEPARATOR = " -- ";
    public static final String markerMain =  "\t\t\t=======================================================================================================";
    public static final String markerSub =   "\t\t\t-------------------------------------------------------------------------------------------------------";
    public static int MAX_MESSAGES_PER_ROUND = 5;
}
