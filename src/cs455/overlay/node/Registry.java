package cs455.overlay.node;

import cs455.overlay.constants.MessageConstants;
import cs455.overlay.transport.TCPCommunicationHandler;
import cs455.overlay.transport.TCPSender;
import cs455.overlay.utils.HelperUtils;
import cs455.overlay.wireformats.*;
import cs455.overlay.constants.EventConstants;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class Registry extends AbstractNode implements Node {
    private final List<NodeDetails> nodeDetailsList = new ArrayList<>();
    private SendLinkWeightsEvent sendLinkWeightsEventEvent = null;
    private int taskCompleted = 0;
    private int receivedTrafficStatsFromAllNodes = 0;
    private int numAllMessagesSent = 0;
    private int numAllMessagesReceived = 0;
    private long sumAllMessagesSent = 0 ;
    private long sumAllMessagesReceived = 0;
    private boolean printOnce = false;
    public static Object LOCK_REGISTRY = new Object();


    public static void main(String args[]) throws Exception {
        int portNum = HelperUtils.getInt(args[0]);

        if(portNum <= 0) {
            System.out.println(MessageConstants.INVALID_PORT_START_FAILURE);
            System.exit(-1);
        }
        Registry registry = new Registry();
        registry.startTCPServerThread(portNum);
        registry.startCommandListener();
    }

    @Override
    public void setupOverlay(final String command)throws IOException {
        if(overlayConfigured) {
            System.out.println("Overlay-setup already done - re-run the program to setup the overlay again.");
            return;
        }

        if(!validArgument(command, 2, true, 1)) {  //Error check on the argument.
            return;
        }

        final int connectionRequirement = HelperUtils.getInt(command.split(" ")[1]);

        if(nodeDetailsList.size() <= connectionRequirement) {
            System.out.println("Unable to create overlay : Number of Messaging Nodes Registered " + nodeDetailsList.size() + " Num Overlay requested " + connectionRequirement );
            return;
        }
        final Map<NodeDetails, SendMessagingNodesListEvent>  allOverlays = buildOverlayNodes(connectionRequirement);
        for(final NodeDetails nodeDetails : allOverlays.keySet()) {
            final SendMessagingNodesListEvent sendMessagingNodesListEvent = allOverlays.get(nodeDetails);
            TCPCommunicationHandler communicationHandler = getConnectionFromPool(nodeDetails.getFormattedString());  /*Check if a connection is already created to the node, if not create on.*/
            if(communicationHandler == null) {
                Socket socket= new Socket(nodeDetails.getNodeName(), nodeDetails.getPortNum());
                communicationHandler = update(socket);
                communicationHandler.sendData(sendMessagingNodesListEvent.getBytes());
            }
        }
        overlayConfigured = true;
    }

    @Override
    public void forceExit() {
        final ForceExitEvent forceExitEvent = new ForceExitEvent();
        System.out.println("INFO : Sending all nodes to exit the application");
        try {
            broadcastMessageToAllNodes(forceExitEvent.getBytes());
        } catch (IOException  ioe) {
            System.out.println("ERROR : Unable to send force exit message" );
        }
        System.out.println("INFO : Exiting the application");
        System.exit(0);
    }

    @Override
    public void sendLinkWeight() {
        final SendLinkWeightsEvent sendLinkWeightsEvent = generateLinkWeights();
        if(sendLinkWeightsEvent == null) {
            return;
        }
        for(final NodeDetails nodeDetails : nodeDetailsList) {
            TCPCommunicationHandler communicationHandler = getConnectionFromPool(nodeDetails.getFormattedString());  /*Check if a connection is already created to the node, if not create on.*/
            try {
                if(communicationHandler == null) {
                    System.out.println("ERROR : This case should not have happened");
                    Socket socket= new Socket(nodeDetails.getNodeName(), nodeDetails.getPortNum());
                    communicationHandler = update(socket);
                }
                communicationHandler.sendData(sendLinkWeightsEvent.getBytes());
            } catch (IOException ioe) {
                System.out.println("ERROR : Error in sending link weights to node " + nodeDetails.getFormattedString());
            }
        }
    }

    @Override
    public void listMessagingNodes() {
        if(nodeDetailsList.size() == 0) {
            System.out.println("Messaging Nodes are not configured yet");
            return;
        }
        for(final NodeDetails nodeDetails  : nodeDetailsList ) {
            System.out.println("Messaging node : " + nodeDetails.getFormattedString());
        }
    }

    @Override
    public void listEdgeWeight() {
        final SendLinkWeightsEvent sendLinkWeightsEvent = generateLinkWeights();
        if(sendLinkWeightsEvent != null) {
            for(final String weights : sendLinkWeightsEvent.getLinkWeightList()) {
                System.out.println(weights);
            }
        }
    }

    @Override
    public void initiateMessagingSignalForNodes(final String numRoundsStr) {
        if(!overlayConfigured || sendLinkWeightsEventEvent == null) {
            System.out.println("Either the overlay is not setup, or the link-weights are not assigned");
            System.out.println("Overlay setup " + overlayConfigured);
            System.out.println("Link Weights  " + ((sendLinkWeightsEventEvent == null)? "Null": "Not Null"));
            return;
        }
        if(!validArgument(numRoundsStr, 2, true, 1)) {  //Error check on the argument.
            return;
        }
        final int numRounds = HelperUtils.getInt(numRoundsStr.split(" ")[1]);
        final TaskInitiateEvent taskInitiateEvent = new TaskInitiateEvent(numRounds);
        try {
            broadcastMessageToAllNodes(taskInitiateEvent.getBytes());
        } catch (IOException ioe) {
            System.out.println("ERROR : Failed to send data to all nodes " );
        }
    }

    @Override
    public void acknowledgeTaskComplete(final String node, final int port) {
        synchronized (LOCK_REGISTRY) {
            ++taskCompleted;
        }

        if(taskCompleted == nodeDetailsList.size()) {
            System.out.println("INFO :: Received task complete from all nodes. - Pulling Traffic summary in 15 seconds");
            taskCompleted = 0;
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                System.out.println("Received a Thread interrupted exception.");
            }
            pullTrafficSummary();
        } else {
            System.out.println("INFO :: Received Task Complete from " + taskCompleted +"/" + nodeDetailsList.size());
        }
    }

    @Override
    public void pullTrafficSummary() {
        final PullTrafficSummaryEvent pullTrafficSummaryEvent = new PullTrafficSummaryEvent();
        try {
            broadcastMessageToAllNodes(pullTrafficSummaryEvent.getBytes());
        } catch (IOException ioe) {
            System.out.println("ERROR : Unable to send pull-traffic summary to all nodes");
        }
    }

    @Override
    public synchronized void  printTrafficSummary(final TrafficSummaryEvent trafficSummaryEvent) {
        ++receivedTrafficStatsFromAllNodes;
        if(!printOnce) {
            printHeader();
            printOnce = true;
        }

        numAllMessagesSent +=  trafficSummaryEvent.getNumOfMessagesSend();
        numAllMessagesReceived += trafficSummaryEvent.getNumOfMessagesReceived();
        sumAllMessagesSent += trafficSummaryEvent.getSumOfSendMessage();
        sumAllMessagesReceived += trafficSummaryEvent.getSumOfReceivedMessages();

        printData(trafficSummaryEvent);
        if(receivedTrafficStatsFromAllNodes == nodeDetailsList.size()) {
            Formatter formatter = new Formatter();
            formatter.format("%-32s %-15d %-15d %-25d %-25d  ",
                    "Sum", numAllMessagesSent, numAllMessagesReceived, sumAllMessagesSent, sumAllMessagesReceived);
            System.out.println(MessageConstants.markerMain);
            System.out.println(formatter.toString());
            System.out.println(MessageConstants.markerMain);
            numAllMessagesSent = 0;
            numAllMessagesReceived = 0;
            sumAllMessagesSent = 0;
            sumAllMessagesReceived = 0;
            printOnce = false;
            receivedTrafficStatsFromAllNodes = 0;
        }
    }

    private void printHeader() {
        final String header1 =   "\t    \t\t\tNumber          Number of                                                              Number of " ;
        final String header2  =  "\t    \t\t\tof messages     messages       Summation of sent           Summation of received       messages" ;
        final String header3 =   "\tNODE\t\t\tsent            received           messages                        messages            relayed";

        System.out.println(header1);
        System.out.println(header2);
        System.out.println(header3);
        System.out.println(MessageConstants.markerMain);
        System.out.println(MessageConstants.markerSub);
        System.out.flush();
    }

    private void printData(final TrafficSummaryEvent trafficSummaryEvent) {
        System.out.println(trafficSummaryEvent.summaryFormatted());
        System.out.println(MessageConstants.markerSub);
        System.out.flush();
    }

    private SendLinkWeightsEvent generateLinkWeights() {
        if(!overlayConfigured) {
            System.out.println("Error : Network Overlay not configured, Link weights cannot be assigned.");
            return null;
        }
        if(sendLinkWeightsEventEvent != null) {
            return sendLinkWeightsEventEvent;
        }
        sendLinkWeightsEventEvent = new SendLinkWeightsEvent(0);
        for(final NodeDetails nodeDetails : nodeDetailsList) {
            for(final NodeDetails connectedNode : nodeDetails.getConnections()) {
                int weight = HelperUtils.generateRandomNumber(1, 10);
                String weightString = nodeDetails.getFormattedString() + " " + connectedNode.getFormattedString() +" " + weight;
                sendLinkWeightsEventEvent.addLinkWeights(weightString);
            }
        }
        return sendLinkWeightsEventEvent;
    }

    private Map<NodeDetails, SendMessagingNodesListEvent> buildOverlayNodes(final int connectionRequirement) {
        final Map<NodeDetails, SendMessagingNodesListEvent> overlayList = new HashMap<>();
        buildPeerMessagingNodeOnAllNodes();  /*First builds overlay connection all nodes - to avoid network partitions. */
        buildPeerMessagingNodesOnEachNode(connectionRequirement);  /*Build connections on the nodes */
        for(final NodeDetails nodeDetails : nodeDetailsList) {
            SendMessagingNodesListEvent temp = buildMessagingNodeList(nodeDetails);  /* Build the message to be send to each node*/
            overlayList.put(nodeDetails, temp);
        }
        return overlayList;
    }

    private SendMessagingNodesListEvent buildMessagingNodeList(final NodeDetails nodeDetails) {  /* Build the messaing node list for ach node.*/
        final SendMessagingNodesListEvent overlayList = new SendMessagingNodesListEvent(0);
        if ((nodeDetails.getConnections().isEmpty()) || (nodeDetails.getConnections().size() <= 0)) {
            return overlayList;
        }
        for(final NodeDetails connectedNodes : nodeDetails.getConnections()) {
            overlayList.addNodesToList(connectedNodes.getFormattedString());
        }
        return overlayList;
    }

    private void buildPeerMessagingNodeOnAllNodes() {
        /*This is to avoid network partition.*/
        final List<NodeDetails> tmpNodeDetailsList  = nodeDetailsList;
        ListIterator<NodeDetails> nodeListIterator = tmpNodeDetailsList.listIterator();
        while (nodeListIterator.hasNext()) {
            NodeDetails currentNode = nodeListIterator.next();
            NodeDetails nextNode ;
            if(nodeListIterator.hasNext()) {
                nextNode = tmpNodeDetailsList.get(nodeListIterator.nextIndex());
                currentNode.addConnections(nextNode);
            }
        }
    }

    private void buildPeerMessagingNodesOnEachNode(final int connectionRequirement) {
        /*Check capacity of each nodes and creates links for each node.*/
        final List <NodeDetails> shuffledNodeDetails = new ArrayList<>(nodeDetailsList);
        ListIterator<NodeDetails> nodeListIterator = nodeDetailsList.listIterator();
        Collections.shuffle(shuffledNodeDetails);
        while (nodeListIterator.hasNext()) {
            NodeDetails currentNode = nodeListIterator.next();
            int idx = 0;
            while (currentNode.moreConnectionsAllowed(connectionRequirement)) {
                NodeDetails connectingNode = shuffledNodeDetails.get(idx);
                if(isNodeContactable(currentNode, connectingNode, connectionRequirement)) {
                    currentNode.addConnections(connectingNode);
                }
                idx++;
                if(idx >= shuffledNodeDetails.size()) {
                    break;
                }
            }
        }
    }

    private boolean isNodeContactable(final NodeDetails source, final NodeDetails destination, final int connectionRequirement) {
        if (!destination.moreConnectionsAllowed(connectionRequirement)) {  /*Check Maximum connections reached.*/
            return false;
        }
        if(source.getFormattedString().equals(destination.getFormattedString())) {  /*Connection to itself is not supported.*/
            return false;
        }
        return !source.nodeAlreadyConnected(destination);  /*Connect if not already connected.*/
    }

    @Override
    public void deRegisterNode(final DeregisterRequestEvent deregisterRequestEvent, final Socket socket) {
        /* Checks
           1. The requested IP, is the same as the source of the request.
           2. Checks if already registered.
         */
        RegisterAcknowledgementEvent registerAck;
        final boolean alreadyRegistered = registered(deregisterRequestEvent.getNodeIpAddress(), deregisterRequestEvent.getPortNum());
        final boolean ipAddressSame = matchIpForMessageAndSocket(socket, deregisterRequestEvent.getNodeIpAddress());

        if(!alreadyRegistered || !ipAddressSame) {
            if(!ipAddressSame) { /*Branch for the Failure types*/
                registerAck = new RegisterAcknowledgementEvent(EventConstants.REGISTER_OR_DEREGISTER_FAILURE, MessageConstants.IP_MISMATCH_DEREGISTRATION
                        + socket.getRemoteSocketAddress().toString() + " " + deregisterRequestEvent.getNodeIpAddress());
            } else {
                registerAck = new RegisterAcknowledgementEvent(EventConstants.REGISTER_OR_DEREGISTER_FAILURE, MessageConstants.NODE_NOT_REGISTERED);
            }
        } else {
            removeNodeFromList(deregisterRequestEvent.getNodeIpAddress(), deregisterRequestEvent.getPortNum());
            registerAck = new RegisterAcknowledgementEvent(EventConstants.REGISTER_OR_DEREGISTER_SUCCESS,
                    MessageConstants.SUCCESSFUL_DEREGISTRATION.replaceAll("(%d)", Integer.toString(nodeDetailsList.size())));
        }
        sendEvent(registerAck, socket);
    }

    @Override
    public void registerNode(final RegisterRequestEvent registerRequestEventEvent, final Socket socket) {
        /* Checks
           1. The requested IP, is the same as the source of the request.
           2. Checks if already registered.
         */
        final boolean alreadyRegistered = registered(registerRequestEventEvent.getNodeIpAddress(), registerRequestEventEvent.getPortNum());
        final boolean ipAddressSame = matchIpForMessageAndSocket(socket, registerRequestEventEvent.getNodeIpAddress());
        RegisterAcknowledgementEvent registerAck;
        if(alreadyRegistered || !ipAddressSame) {
            if(alreadyRegistered) { /*Branch for the Failure types*/
                registerAck = new RegisterAcknowledgementEvent(EventConstants.REGISTER_OR_DEREGISTER_FAILURE, MessageConstants.NODE_ALREADY_REGISTERED);
            } else {
                registerAck = new RegisterAcknowledgementEvent(EventConstants.REGISTER_OR_DEREGISTER_FAILURE, MessageConstants.IP_MISMATCH_REGISTRATION
                        + socket.getRemoteSocketAddress().toString() + " " + registerRequestEventEvent.getNodeIpAddress());
            }
            System.out.println("Unable to register the node, already exist");
        } else {
            nodeDetailsList.add(new NodeDetails(registerRequestEventEvent.getNodeIpAddress(), registerRequestEventEvent.getPortNum()));
            registerAck = new RegisterAcknowledgementEvent(EventConstants.REGISTER_OR_DEREGISTER_SUCCESS,
                    MessageConstants.SUCCESSFUL_REGISTRATION.replaceAll("(%d)", Integer.toString(nodeDetailsList.size())));
            System.out.println("Node Successfully Registered " + socket.getInetAddress() + ":" + socket.getPort());

        }
        final boolean success = sendEvent(registerAck, socket);
        if(!success) {  /*If the message send failed */
            removeNodeFromList(registerRequestEventEvent.getNodeIpAddress(), registerRequestEventEvent.getPortNum());
        }
    }

    private void removeNodeFromList(final String nodeIpAddres, final int portNum) {
        int indexForRemoval = -1;
        for (final NodeDetails nodeDetails : nodeDetailsList) {
            if (portNum == nodeDetails.getPortNum() && nodeIpAddres.equals(nodeDetails.getNodeName())) {
                indexForRemoval = nodeDetailsList.indexOf(nodeDetails);
                break;
            }
        }
        if (indexForRemoval != -1) {
            nodeDetailsList.remove(indexForRemoval);
        }
    }

    private boolean sendEvent(final Event eventToSend, final Socket socket) {
        try {
            final TCPSender sender = new TCPSender(socket);
            byte [] bytesToSend = eventToSend.getBytes();
            sender.sendData(bytesToSend);
        } catch (IOException ioe) {
            System.out.println("ERROR : Unable to Send registration acknowledgement ");
            ioe.printStackTrace();
            return false;
        }
        System.out.println("INFO : Send registration acknowledgement " + socket.getInetAddress() + ":" + socket.getPort() );
        return true;
    }

    private boolean registered (final String nodeName, final int portNum) {
        for(final NodeDetails nodeDetails : nodeDetailsList) {
            if ((nodeDetails.getNodeName().equals(nodeName)) && (nodeDetails.getPortNum() == portNum)) {
                return true;
            }
        }
        return false;
    }

    private void broadcastMessageToAllNodes(final byte[] byteStreamToSend) throws IOException {
        for(final NodeDetails nodeDetails : nodeDetailsList) {
            TCPCommunicationHandler communicationHandler = getConnectionFromPool(nodeDetails.getFormattedString());  /*Check if a connection is already created to the node, if not create on.*/
            if(communicationHandler == null) {
                Socket socket= new Socket(nodeDetails.getNodeName(), nodeDetails.getPortNum());
                communicationHandler = update(socket);
            }
            communicationHandler.sendData(byteStreamToSend);
        }
    }

    private boolean matchIpForMessageAndSocket(final Socket socket, final String nodeIpAddress) {
        String ipAddressInSocket = socket.getRemoteSocketAddress().toString();
        if(ipAddressInSocket.contains("localhost") || ipAddressInSocket.contains("127.0.0.1")) {
            ipAddressInSocket = HelperUtils.getLocalHostIpAddress();
        }
        /*Strip off the leading / if any */
        if(ipAddressInSocket.contains("/")) {
            ipAddressInSocket = ipAddressInSocket.substring(ipAddressInSocket.indexOf("/") + 1);
            System.out.println("Stripped / : Current Value is : " + ipAddressInSocket);
        }
        /*Strip off the port number in ipddress if any : Usually the format is /120.11.11.11:3232 */
        if(ipAddressInSocket.contains(":")) {
            ipAddressInSocket = ipAddressInSocket.substring(0, ipAddressInSocket.indexOf(":"));
            System.out.println("Stripped  : : Current Value is : " + ipAddressInSocket);
        }
        System.out.println("Remote IP Address  : " + ipAddressInSocket + " Ipaddress in message " + nodeIpAddress);
        return ipAddressInSocket.equals(nodeIpAddress);
    }

    @Override
    public void registerNodeAcknowledgement(final RegisterAcknowledgementEvent acknowledgement) {
        System.out.println("INFO : Register node acknowledgement is not supported on Registry");
    }


    @Override
    public void processReceivedMessage(final TransmitMessageEvent transmitMessageEvent) {
        System.out.println("INFO : Received Random message is not supported on Registry");
    }

    @Override
    public void updateConnectionInfo(final SendListeningPortEvent sendListeningPortEvent, final Socket socket) {
        System.out.println("INFO : Update connection info is not supported on registry");
    }

    @Override
    public void initiateNodeRegistration(final String myHostName, final int myPortNum) {
        System.out.println("INFO : Initiate node registration is not supported on registry.");
    }

    @Override
    public void requestDeRegister(final String myHostName, final int myPortNum) {
        System.out.println("INFO : Request de-registration is not supported on registry.");
    }

    @Override
    public void startMessaging(final String numRoundsStr) {
        System.out.println("INFO : Start Messaging is not supported on Registry");
    }

    @Override
    public void printShortestPath() {
        System.out.println("INFO : Print shortest path is not supported on Registry");
    }

    @Override
    public void exitOverlay() {
        System.out.println("INFO : Exit Overlay is not supported on Registry");
    }

    @Override
    public void processLinkWeights(final SendLinkWeightsEvent sendLinkWeightsEvent) {
        System.out.println(("INFO : Processing received link weights is not supported on Registry"));
    }

    @Override
    public void makeConnectionsOnOverLayNodes(final SendMessagingNodesListEvent sendMessagingNodesListEvent) {
        System.out.println(("INFO : Make Overlay connections is not supported on Registry"));
    }
}
