package cs455.overlay.node;

import cs455.overlay.constants.EventConstants;
import cs455.overlay.utils.HelperUtils;
import cs455.overlay.wireformats.DeregisterRequest;
import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.RegisterAcknowledgement;
import cs455.overlay.wireformats.RegisterRequest;
import cs455.overlay.wireformats.TaskComplete;
import cs455.overlay.wireformats.TrafficSummary;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;

public class MessagingNode extends AbstractNode {

    private static String registryHost;
    private static int registryPort;

    private int numOfMessagesSend;
    private int sumOfSendMessage = 100;
    private int numOfMessagesReceived = 10;
    private int sumOfReceivedMessages = 10;

    public static void main(String args[]) throws Exception {
        registryHost = args[0];
        registryPort = HelperUtils.getInt(args[1]);
        ipAddress = Inet4Address.getLocalHost().getHostAddress();

        MessagingNode messagingNode = new MessagingNode();
        portNum = messagingNode.startTCPServerThread(-1);
        messagingNode.registerMessagingNode(ipAddress, portNum);
        messagingNode.startCommandListener();
    }

    private void registerMessagingNode(final String myHostName, final int myPortNum) {  // TODO :: Connection could be singleton.
        try {
            Socket connectionToRegistry;connectionToRegistry = new Socket(registryHost, registryPort);
            registerCommHandler = update(connectionToRegistry);  //TODO : Either use the return value or pick from the map. -- CAN this is extracted out??
            final Event registerEvent = new RegisterRequest(myHostName, myPortNum);
            registerCommHandler.sendData(registerEvent.getBytes());
        } catch (IOException ioe) {
            System.out.println("ERROR : Exiting Unable to initiate connection to registry.");
            System.exit(-1);
        }
       System.out.println("INFO : Send registration request to the registry!");
    }

    @Override
    public void registerNode(final RegisterRequest registerRequestEvent, final Socket socket) {
        System.out.println("INFO : Register node is not supported on a messaging node.");
    }

    @Override
    public void deRegisterNode(final DeregisterRequest deregisterRequest, final Socket socket) {
        System.out.println("INFO : Register node is not supported on a messaging node.");
    }

    @Override
    public void setupOverlay(final String command) throws IOException {
        System.out.println("INFO : Setup Overlay not supported on a messaging node.");
    }

    @Override
    public void sendLinkWeight() {
        System.out.println("INFO : Sending Link weights is not supported on Messaging Node.!");
    }

    @Override
    public void processLinkWeights() {
        System.out.println("INFO : Link weights are received and processed. Ready to send messages.");  //TODO:: Process Weights
    }

    @Override
    public void listMessagingNodes() {
        System.out.println("INFO : Listing Messaging node is not supported on Messaging Node.!");
    }

    @Override
    public void listEdgeWeight() {
        System.out.println("INFO : Listing Edge weight is not supported on Messaging Node.!");
    }

    @Override
    public void registerNodeAcknowledgement(final RegisterAcknowledgement acknowledgement) {
        //TODO:: Handle deregister.
        if(requestedToExitOverlay) {
            System.out.println("System requested to exit the overlay");
        } else {
            if (acknowledgement.getCode() == EventConstants.REGISTER_OR_DEREGISTER_FAILURE) {
                System.out.println("ERROR : Node Failed to Register");
            } else {
                System.out.println("INFO : Node Registration Successful");
            }
        }
        System.out.println("Additional INFO : " + acknowledgement.getAdditionalInfo());
    }

    @Override
    public void initiateMessagingSignalForNodes(final String numRoundsStr) {
        System.out.println("INFO : Start Messaging is not supported on messaging node");
    }

    @Override
    public void startMessaging(final String numRoundsStr) {
        final int numRounds = Integer.parseInt(numRoundsStr);
        System.out.println("Messaging Starts");  //TODO :: Pick node to send message.
        for(int numSend = 0 ; numSend < numRounds; ++numSend) {
            ++numOfMessagesSend;
            sumOfSendMessage += numOfMessagesSend;
            System.out.println("Sending message to Node " + numSend);
        }
        updateRegistryOnTaskCompletion();
    }

    @Override
    public void printShortestPath() {
        //TODO :: Print shortest path
    }

    @Override
    public void exitOverlay() {
        DeregisterRequest deregisterRequest = new DeregisterRequest(ipAddress, portNum);
        try {
            sendMessageToRegistry(deregisterRequest.getBytes());
        } catch (IOException ioe ) {
            System.out.println("Unable to send the traffic stats to the registry");
        }
    }

    @Override
    public void acknowledgeTaskComplete(final String node, final int port) {
        System.out.println("INFO : Task Completed Acknowledge is not supported on messaging node");
    }

    @Override
    public void printTrafficSummary(final TrafficSummary trafficSummary) {
        System.out.println(("INFO : Print Traffic summary is not supported on messaging node"));
    }

    @Override
    public void pullTrafficSummary() {

        final TrafficSummary trafficSummary = new TrafficSummary();
        //TODO :: Build the Strings
        trafficSummary.setIpAddress(ipAddress);
        trafficSummary.setPortNum(portNum);
        trafficSummary.setNumOfMessagesSend(numOfMessagesSend);
        trafficSummary.setNumOfMessagesReceived(numOfMessagesReceived);
        trafficSummary.setSumOfReceivedMessages(sumOfReceivedMessages);
        trafficSummary.setSumOfSendMessage(sumOfSendMessage);

        try {
            if(sendMessageToRegistry(trafficSummary.getBytes())) {
                clearMessageCounters();
            }
        } catch (IOException ioe ) {
            System.out.println("Unable to send the traffic stats to the registry");
        }
    }

    private void clearMessageCounters() {
        numOfMessagesSend = 0;
        numOfMessagesReceived = 0;
        sumOfReceivedMessages = 0;
        sumOfSendMessage = 0;
    }

    private void updateRegistryOnTaskCompletion() {
        System.out.println("INFO : Sending Task Completed message to registry ");
        final TaskComplete taskComplete = new TaskComplete(ipAddress, portNum);
        try {
            registerCommHandler.sendData(taskComplete.getBytes());
        } catch (IOException ioe) {
            System.out.println("ERROR : Unable to send TASK COMPLETION event to the registry.");
        }

    }
}
