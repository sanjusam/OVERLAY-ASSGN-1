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
    private long sumOfSendMessage = 0;
    private int numOfMessagesReceived = 0;
    private long sumOfReceivedMessages = 0;
    private int numOfMessagesRelayed = 0;
    private boolean linkWeightsProcessed = false;
    private int potentialNumPacketsSend = 0;
    private boolean pullSummaryRequestReceived = false;
    private ExtractLinkWeights extractLinkWeights = null;
    private List<RoutingCache> routingCacheList = null;

    public static void main(String args[]) throws Exception {
        registryHost = args[0];
        registryPort = HelperUtils.getInt(args[1]);
        myIpAddress = Inet4Address.getLocalHost().getHostAddress();

        MessagingNode messagingNode = new MessagingNode();
        myPortNum = messagingNode.startTCPServerThread(-1);
        messagingNode.initiateNodeRegistration(myIpAddress, myPortNum);
        messagingNode.startCommandListener();
    }

    @Override
    public void initiateNodeRegistration(final String myHostName, final int myPortNum) {
        try {
            System.out.println("INFO : Send registration request to the registry!");
            Socket connectionToRegistry = new Socket(registryHost, registryPort);
            registerCommHandler = update(connectionToRegistry);
            final Event registerEvent = new RegisterRequestEvent(myHostName, myPortNum);
            registerCommHandler.sendData(registerEvent.getBytes());
        } catch (IOException ioe) {
            System.out.println("ERROR : Exiting Unable to initiate connection to registry.");
            System.exit(-1);
        }
    }

    @Override
    public void requestDeRegister(final String hostName, final int portNum) {
        final DeregisterRequestEvent deregisterRequestEvent = new DeregisterRequestEvent(hostName, portNum);
        try {
            sendMessageToRegistry(deregisterRequestEvent.getBytes());
        } catch (IOException ioe ) {
            System.out.println("ERROR : Unable to request de-register");
        }
    }


    @Override
    public void makeConnectionsOnOverLayNodes(final SendMessagingNodesListEvent nodesList) {
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
                TCPCommunicationHandler tcpCommunicationHandler = update(nodeConnection);
                ++connectionsMade;
                final SendListeningPortEvent listeningPortMsg = new SendListeningPortEvent(myPortNum);
                tcpCommunicationHandler.sendData(listeningPortMsg.getBytes());
            } catch (final IOException ioe) {
                System.out.println("ERROR : IO Exception thrown while trying to establish connection to " + nodeSpec);
                System.exit(-1);
            }
        }
        System.out.println("All connections are established. Number of connections: " + connectionsMade);
    }

    @Override
    public void updateConnectionInfo(final SendListeningPortEvent sendListeningPortEvent, final Socket socket) {
        updateConnectionInformation(socket, sendListeningPortEvent.getListeningPort());
    }


    @Override
    public void processLinkWeights(final SendLinkWeightsEvent sendLinkWeightsEvent) {
        final String me = myIpAddress + MessageConstants.NODE_PORT_SEPARATOR + myPortNum;
        extractLinkWeights = new ExtractLinkWeights(sendLinkWeightsEvent.getLinkWeightList(), me);
        routingCacheList = extractLinkWeights.getRoutingForAllNodes();
        System.out.println("INFO : Link weights are received and processed. Ready to send messages.");
        linkWeightsProcessed = true;
    }

    @Override
    public void registerNodeAcknowledgement(final RegisterAcknowledgementEvent acknowledgement) {
        if (acknowledgement.getCode() == EventConstants.REGISTER_OR_DEREGISTER_FAILURE) {
            System.out.println("ERROR : Node Failed to Register");
        } else {
                System.out.println("INFO : Node Registration Successful");
        }
        System.out.println("Additional INFO : " + acknowledgement.getAdditionalInfo());
        if(requestedToExitOverlay) {
            requestedToExitOverlay = false;
            System.out.print("INFO : Exiting the overlay");
            for(String communicationKey : communicationHandlerMap.keySet()) {
                try {
                    communicationHandlerMap.get(communicationKey).getSocket().close();
                } catch (IOException e) {
                    System.out.println("ERROR : Failed to close socket " + communicationKey) ;
                }
            }
            System.exit(0);
        }
    }


    @Override
    public void startMessaging(final String numRoundsStr) {
        if(!linkWeightsProcessed || extractLinkWeights == null) {
            System.out.println("ERROR : Links weights not send/processed");
            return;
        }

        final List<String > listOfNodes  = extractLinkWeights.getAllNodesExceptMe() ;
        potentialNumPacketsSend  = numOfMessagesSend * MessageConstants.MAX_MESSAGES_PER_ROUND ;
        final int numRounds = Integer.parseInt(numRoundsStr);
        System.out.println("Messaging Starts");
        for(int numSend = 0 ; numSend < numRounds; ++numSend) {
            for(int messagesPerRound = 0 ; messagesPerRound < MessageConstants.MAX_MESSAGES_PER_ROUND ; messagesPerRound++) {
                int rndMessage = HelperUtils.generateRandomNumber(1, 2147483647);
                int factor = HelperUtils.generateRandomNumber(1, 5);
                if(factor%2 == 0) {  // Just to get a negative number randomly.
                    rndMessage = rndMessage * -1 ;
                }
                int rndNodeToSend = HelperUtils.generateRandomNumber(0, listOfNodes.size() -1);
                final String nodeToSend = listOfNodes.get(rndNodeToSend);
                if(nodeToSend != null) {
                    final TCPCommunicationHandler nextHopNodeConnection  = getConnectionHandlerForNextHop(nodeToSend);
                    if(nextHopNodeConnection != null) {
                        final TransmitMessageEvent message = new TransmitMessageEvent(rndMessage, nodeToSend);
                        try {
                            nextHopNodeConnection.sendData(message.getBytes());
                        } catch (IOException ioe) {
                            System.out.println("Error is transmitting message");
                            continue;
                        }
                    } else {
                        System.out.println("ERROR : Missing connection information - All connections should have been established");
                    }
                }
                ++numOfMessagesSend;
                sumOfSendMessage += rndMessage;
            }
        }
        updateRegistryOnTaskCompletion();
    }

    @Override
    public synchronized void processReceivedMessage(final TransmitMessageEvent transmitMessageEvent) {
        final String me = myIpAddress + MessageConstants.NODE_PORT_SEPARATOR + myPortNum;
        if(transmitMessageEvent.getDestination().equals(me)) {  //This is the destination, update counters and drop the packet
            ++numOfMessagesReceived;
            sumOfReceivedMessages += transmitMessageEvent.getMessageContent();
        } else { //Retransmit the package!
            ++numOfMessagesRelayed;
            final String nodeToSend = transmitMessageEvent.getDestination();
            final TCPCommunicationHandler connectionHandlerForNextHop = getConnectionHandlerForNextHop(nodeToSend);
            try {
                connectionHandlerForNextHop.sendData(transmitMessageEvent.getBytes());
            } catch (IOException ioe) {
                System.out.println("INFO : Failed to send message");
            }
        }
        if(numOfMessagesReceived < potentialNumPacketsSend  && pullSummaryRequestReceived) {
            System.out.println("WARNING !!!! : Pull Traffic Summary Request received before all the packets are received " + numOfMessagesReceived +"/" + potentialNumPacketsSend );
        }
    }

    @Override
    public void forceExit() {
        final ForceExitEvent forceExitEvent = new ForceExitEvent();
        try {
            sendMessageToRegistry(forceExitEvent.getBytes());
        } catch (IOException ioe) {
            System.out.println("ERROR : Unable to send force exit message to registry");
        }
        System.out.println("INFO : Exiting the application");
        System.exit(0);
    }

    @Override
    public void printShortestPath() {
        if(routingCacheList == null) {
            System.out.println("ERROR : Link weights are not assigned to calculate the shortest path.");
            return;
        }
        System.out.println("Printing routing cache list for all nodes.");
        for(final RoutingCache routingCache : routingCacheList) {
            System.out.println("Shortest Path : " + routingCache.getPath());
        }
    }

    @Override
    public void exitOverlay() {
        requestedToExitOverlay = true;
        DeregisterRequestEvent deregisterRequestEvent = new DeregisterRequestEvent(myIpAddress, myPortNum);
        try {
            sendMessageToRegistry(deregisterRequestEvent.getBytes());
        } catch (IOException ioe ) {
            System.out.println("Unable to send the traffic stats to the registry");
        }
    }

    @Override
    public void pullTrafficSummary() {
        pullSummaryRequestReceived = true;
        final TrafficSummaryEvent trafficSummaryEvent = new TrafficSummaryEvent();
        trafficSummaryEvent.setIpAddress(myIpAddress);
        trafficSummaryEvent.setPortNum(myPortNum);
        trafficSummaryEvent.setNumOfMessagesSend(numOfMessagesSend);
        trafficSummaryEvent.setNumOfMessagesReceived(numOfMessagesReceived);
        trafficSummaryEvent.setSumOfReceivedMessages(sumOfReceivedMessages);
        trafficSummaryEvent.setSumOfSendMessage(sumOfSendMessage);
        trafficSummaryEvent.setNumOfMessagesRelayed(numOfMessagesRelayed);

        try {
            if(sendMessageToRegistry(trafficSummaryEvent.getBytes())) {
                clearMessageCounters();
            }
        } catch (IOException ioe ) {
            System.out.println("Unable to send the traffic stats to the registry");
        }
    }

    private void updateRegistryOnTaskCompletion() {
        System.out.println("INFO : Sending Task Completed message to registry ");
        final TaskCompleteEvent taskCompleteEvent = new TaskCompleteEvent(myIpAddress, myPortNum);
        try {
            registerCommHandler.sendData(taskCompleteEvent.getBytes());
        } catch (IOException ioe) {
            System.out.println("ERROR : Unable to send TASK COMPLETION event to the registry.");
        }
    }

    private TCPCommunicationHandler getConnectionHandlerForNextHop(final String node) {
        for (final RoutingCache routingCache : routingCacheList) {
            if (routingCache.getDestination().equals(node)) {
                return communicationHandlerMap.get(routingCache.getNextHop());
            }
        }
        return null;
    }

    private void clearMessageCounters() {
        numOfMessagesSend = 0;
        numOfMessagesReceived = 0;
        sumOfReceivedMessages = 0;
        sumOfSendMessage = 0;
        numOfMessagesRelayed = 0;
        pullSummaryRequestReceived = false;
        potentialNumPacketsSend = 0;
    }


    @Override
    public void acknowledgeTaskComplete(final String node, final int port) {
        System.out.println("INFO : Task Completed Acknowledge is not supported on messaging node");
    }

    @Override
    public void printTrafficSummary(final TrafficSummaryEvent trafficSummaryEvent) {
        System.out.println(("INFO : Print Traffic summary is not supported on messaging node"));
    }

    @Override
    public void initiateMessagingSignalForNodes(final String numRoundsStr) {
        System.out.println("INFO : Init Signal for messaging is not supported on messaging node");
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
    public void registerNode(final RegisterRequestEvent registerRequestEventEvent, final Socket socket) {
        System.out.println("INFO : Register node is not supported on a messaging node.");
    }

    @Override
    public void deRegisterNode(final DeregisterRequestEvent deregisterRequestEvent, final Socket socket) {
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
}
