package cs455.overlay.node;

import cs455.overlay.constants.EventConstants;
import cs455.overlay.constants.MessageConstants;
import cs455.overlay.dijkstra.ExtractLinkWeights;
import cs455.overlay.dijkstra.RoutingCache;
import cs455.overlay.transport.TCPCommunicationHandler;
import cs455.overlay.utils.HelperUtils;
import cs455.overlay.wireformats.*;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.List;

public class MessagingNode extends AbstractNode {

    private static String registryHost;
    private static int registryPort;

    private int numOfMessagesSend = 0;
    private long sumOfSendMessage = 100;
    private int numOfMessagesReceived = 10;
    private long sumOfReceivedMessages = 10;
    private int numOfMessagesRelayed = 0;
    private boolean linkWeightsProcessed = false;
    ExtractLinkWeights extractLinkWeights = null;
    List<RoutingCache> routingCacheList = null;

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
    public void makeConnectionsOnOverLayNodes(final MessagingNodesList nodesList) {
        /*Walk through the all the messaging node, create connection and listen to the incoming connections.*/
        if(nodesList.getNumNodes()== 0) {
            System.out.println("INFO : The node is NOT instructed to create any Messaging Node Connection");
            return;
        }
        int connectionsMade = 0;
        System.out.println("INFO : Number of Messaging Node Connection initiated " + nodesList.getNumNodes());
        for(final String nodeSpec : nodesList.getMessagingNodeList()) {
            if(nodeSpec.isEmpty() || nodeSpec.equals("")) {
                continue;
            }
            System.out.println("INFO : Creating Connection to Messaging Node " + nodeSpec);
            final String nodeName = nodeSpec.split(MessageConstants.NODE_PORT_SEPARATOR)[0];
            final int portNum = HelperUtils.getInt(nodeSpec.split(MessageConstants.NODE_PORT_SEPARATOR)[1]);
            if(portNum == -1) {
                System.out.println("ERROR : Unable to extract port number from node details  " + nodeSpec);
                System.exit(-1);
            }
            try {
                final Socket nodeConnection = new Socket(nodeName, portNum);
                update(nodeConnection);  //TODO : Either use the return value or pick from the map.
                ++connectionsMade;
            } catch (final IOException ioe) {
                System.out.println("ERROR : IO Exception thrown while trying to establish connection to " + nodeSpec);
                System.exit(-1);
            }
        }
        System.out.println("All connections are established. Number of connections: " + connectionsMade);
    }

    @Override
    public void processLinkWeights(final LinkWeights linkWeights) {
        final String me = ipAddress + MessageConstants.NODE_PORT_SEPARATOR + portNum;
        extractLinkWeights = new ExtractLinkWeights(linkWeights.getLinkWeightList(), me);
        routingCacheList = extractLinkWeights.getRoutingForAllNodes();
        System.out.println("Printing routing cache list for all nodes.");
        for(final RoutingCache routingCache : routingCacheList) {
            System.out.println("Source : " + routingCache.getSource() + " Destination  " + routingCache.getDestination()
                    + " Path " + routingCache.getPath()  + " Next Hop " + routingCache.getNextHop());
        }

        System.out.println("INFO : Link weights are received and processed. Ready to send messages.");  //TODO:: Process Weights
        linkWeightsProcessed = true;
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
        System.out.println("INFO : Init Signal for messaging is not supported on messaging node");
    }

    @Override
    public void startMessaging(final String numRoundsStr) {
        if(!linkWeightsProcessed || extractLinkWeights == null) {
            System.out.println("ERROR : Links weights not send/processed");
            return;
        }

        final List<String > listOfNodes  = extractLinkWeights.getAllNodes() ;
        //routingCacheList

        final int numRounds = Integer.parseInt(numRoundsStr);
        final int MAX_MESSAGES_PER_ROUND = 5;
        System.out.println("Messaging Starts");  //TODO :: Pick node to send message.
        for(int numSend = 0 ; numSend < numRounds; ++numSend) {
            for(int messagesPerRound = 0 ; messagesPerRound < MAX_MESSAGES_PER_ROUND ; messagesPerRound++) {
                int rndMessage = HelperUtils.generateRandomNumber(1, 2147483647);  //TODO handle negative random number.
                int rndNodeToSend = HelperUtils.generateRandomNumber(0, listOfNodes.size() -1);
                final String nodeToSend = listOfNodes.get(rndNodeToSend);
                if(nodeToSend != null) {
                    final TCPCommunicationHandler nextHopNodeConnection  = getNextHop(nodeToSend);
                    if(nextHopNodeConnection != null) {
                        final TransmitMessage message = new TransmitMessage(rndMessage, nodeToSend);
                        try {
                            nextHopNodeConnection.sendData(message.getBytes());
                        } catch (IOException ioe) {
                            System.out.println("Error is transmitting message");
                            continue;
                        }
                    } else {
                        System.out.println("ERROR : This should not have happened");
                    }
                }

                ++numOfMessagesSend;
                sumOfSendMessage += rndMessage;
            }
        }
        updateRegistryOnTaskCompletion();
    }

    @Override
    public void processReceivedMessage(final TransmitMessage transmitMessage) {
        System.out.println("INFO : Received Data transmission");
        final String me = ipAddress + MessageConstants.NODE_PORT_SEPARATOR + portNum;
        if(transmitMessage.getDestination().equals(me)) {  //This is the destination, update counters and drop the packet
            ++numOfMessagesReceived;
            sumOfReceivedMessages += transmitMessage.getMessageContent();
        } else { //Retransmit it!
            ++numOfMessagesRelayed;
            final String nodeToSend = transmitMessage.getDestination();
            final TCPCommunicationHandler communicationHandler = getNextHop(nodeToSend);
            try {
                communicationHandler.sendData(transmitMessage.getBytes());
            } catch (IOException ioe) {
                System.out.println("Failed to send message");
                return;
            }
        }
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
        trafficSummary.setNumOfMessagesRelayed(numOfMessagesRelayed);

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
        numOfMessagesRelayed = 0;
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

    private TCPCommunicationHandler getNextHop(final String node) {
        for (final RoutingCache routingCache : routingCacheList) {
            if (routingCache.getDestination().equals(node)) {
                return communicationHandlerMap.get(routingCache.getNextHop());
            }
        }
        return null;
    }
}
