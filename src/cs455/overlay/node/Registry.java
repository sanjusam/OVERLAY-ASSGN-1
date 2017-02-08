package cs455.overlay.node;

import cs455.overlay.constants.MessageConstants;
import cs455.overlay.transport.TCPCommunicationHandler;
import cs455.overlay.transport.TCPSender;
import cs455.overlay.utils.HelperUtils;
import cs455.overlay.wireformats.DeregisterRequest;
import cs455.overlay.wireformats.Event;
import cs455.overlay.constants.EventConstants;
import cs455.overlay.constants.EventType;
import cs455.overlay.wireformats.LinkWeights;
import cs455.overlay.wireformats.MessagingNodesList;
import cs455.overlay.wireformats.RegisterRequest;
import cs455.overlay.wireformats.RegisterAcknowledgement;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class Registry extends AbstractNode implements Node {
    final List<NodeDetails> nodeDetailsList = new ArrayList<>();
    private LinkWeights linkWeightsEvent = null;

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
    public void onEvent(final Event event, final Socket socket) {
        if(event.getType() == EventType.REGISTER_REQUEST.getValue()) {
            System.out.println("Received a registration request from node");
            registerNode((RegisterRequest)event, socket);
        }else if(event.getType() == EventType.DEREGISTER_REQUEST.getValue()) {
            System.out.println("Received a registration request from node");
            deRegisterNode((DeregisterRequest)event, socket);
        }
    }

    @Override
    public void setupOverlay(final String command)throws IOException {
        if(overlayConfigured) {
            System.out.println("Overlay-setup already done - re-run the program to setup the overlay again.");
            return;
        }

        if(!validFirstArgument(command, 2, true)) {  //Error check on the argumnet.
            return;
        }

        final int connectionRequirement = HelperUtils.getInt(command.split(" ")[1]);

        if(nodeDetailsList.size() <= connectionRequirement) {
            System.out.println("Unable to create overlay : Number of Messaging Nodes Registered " + nodeDetailsList.size() + " Num Overlay requested " + connectionRequirement );
            return;
        }
        final Map<NodeDetails, MessagingNodesList>  allOverlays = buildOverlayNodes(connectionRequirement);
        for(final NodeDetails nodeDetails : allOverlays.keySet()) {
            final MessagingNodesList messagingNodesList = allOverlays.get(nodeDetails);
            TCPCommunicationHandler communicationHandler = getConnectionFromPool(nodeDetails.getFormattedString());  /*Check if a connection is already created to the node, if not create on.*/
            if(communicationHandler == null) {
                //TODO :: Create connection.
                Socket socket= new Socket(nodeDetails.getNodeName(), nodeDetails.getPortNum());
                communicationHandler = update(socket);
                System.out.println("Sending Messaging Node List");
                communicationHandler.sendData(messagingNodesList.getBytes());
            }
        }
        overlayConfigured = true;
    }

    @Override
    public void sendLinkWeight() {
        final LinkWeights linkWeights = generateLinkWeights();
        if(!overlayConfigured) {
            System.out.println("Cannot send the links weights as the overlay is not configured yet");
            return;
        }

        for(final NodeDetails nodeDetails : nodeDetailsList) {
            TCPCommunicationHandler communicationHandler = getConnectionFromPool(nodeDetails.getFormattedString());  /*Check if a connection is already created to the node, if not create on.*/
            try {
                if(communicationHandler == null) {
                    System.out.println("Error : This case should not have happened");
                    Socket socket= new Socket(nodeDetails.getNodeName(), nodeDetails.getPortNum());
                    communicationHandler = update(socket);
                }
                communicationHandler.sendData(linkWeights.getBytes());
            } catch (IOException ioe) {
                System.out.println("ERROR : Error in sending link weights to node " + nodeDetails.getFormattedString());
            }
        }
    }

    @Override
    public void processLinkWeights() {
        System.out.println(("Error : Processing received link weights is not supported on Registry"));
        return;
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
    public void ListEdgeWeight() {
        final LinkWeights linkWeights = generateLinkWeights();
        if(linkWeights == null) {
            return;
        } else {
            for(final String weights : linkWeights.getLinkWeightList()) {
                System.out.println(weights);
            }
        }
    }


    private LinkWeights generateLinkWeights() {
        if(!overlayConfigured) {
            System.out.println("Error : Network Overlay not configured, Link weights cannot be assigned.");
            return null;
        }
        if(linkWeightsEvent != null) {
            return  linkWeightsEvent;
        }
        linkWeightsEvent = new LinkWeights(0);
        for(final NodeDetails nodeDetails : nodeDetailsList) {
            for(final NodeDetails connectedNode : nodeDetails.getConnections()) {
                int weight = HelperUtils.generateRandomNumber(1, 10);
                String weightString = nodeDetails.getFormattedString() + " " + connectedNode.getFormattedString() +" " + weight;
                linkWeightsEvent.addLinkWeights(weightString);
            }
        }
        return linkWeightsEvent;
    }

    private Map<NodeDetails, MessagingNodesList> buildOverlayNodes(final int connectionRequirement) {
        final Map<NodeDetails, MessagingNodesList> overlayList = new HashMap<>();
        buildPeerMessagingNodeOnAllNodes();  /*First builds overlay connection all nodes - to avoid network partitions. */
        buildPeerMessagingNodesOnEachNode(connectionRequirement);  /*Build connections on the nodes */
        for(final NodeDetails nodeDetails : nodeDetailsList) {
            System.out.println("All Connections for node      " + nodeDetails.getFormattedString() + " " + nodeDetails.getAllConnections());
            System.out.println("Created Connections for node  " + nodeDetails.getFormattedString() + " " + nodeDetails.getConnections().size());
            MessagingNodesList temp = buildMessagingNodeList(nodeDetails);  /* Build the message to be send to each node*/
            overlayList.put(nodeDetails, temp);
        }
        return overlayList;
    }

    private MessagingNodesList buildMessagingNodeList(final NodeDetails nodeDetails) {
        final MessagingNodesList overlayList = new MessagingNodesList(0);
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
        final List<NodeDetails> tmpNodeDetailsList  = nodeDetailsList;
        ListIterator<NodeDetails> nodeListIterator = tmpNodeDetailsList.listIterator();
        while (nodeListIterator.hasNext()) {
            NodeDetails currentNode = nodeListIterator.next();
            while (currentNode.moreConnectionsAllowed(connectionRequirement)) {
                int randomNum = HelperUtils.generateRandomNumber(0, tmpNodeDetailsList.size()-1);
                NodeDetails connectingNode = nodeDetailsList.get(randomNum);
                if(isNodeContactable(currentNode, connectingNode, connectionRequirement)) {
                    currentNode.addConnections(connectingNode);
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

    private void deRegisterNode(final DeregisterRequest deregisterRequest, final Socket socket) {
        /* Checks
           1. The requested IP, is the same as the source of the request.
           2. Checks if already registered.
         */
        RegisterAcknowledgement registerAck;
        final boolean alreadyRegistered = registered(deregisterRequest.getNodeIpAddress(), deregisterRequest.getPortNum());
        final boolean ipAddressSame = matchIpForMessageAndSocket(socket, deregisterRequest.getNodeIpAddress());

        if(!alreadyRegistered || !ipAddressSame) {
            if(!alreadyRegistered) { /*Branch for the Failure types*/
                registerAck = new RegisterAcknowledgement(EventConstants.REGISTER_OR_DEREGISTER_FAILURE, MessageConstants.NODE_NOT_REGISTERED);
            } else {
                registerAck = new RegisterAcknowledgement(EventConstants.REGISTER_OR_DEREGISTER_FAILURE, MessageConstants.IP_MISMATCH_DEREGISTRATION
                        + socket.getRemoteSocketAddress().toString() + " " + deregisterRequest.getNodeIpAddress());
            }
        } else {
            registerAck = new RegisterAcknowledgement(EventConstants.REGISTER_OR_DEREGISTER_SUCCESS,
                    MessageConstants.SUCCESSFUL_DEREGISTRATION.replaceAll("(%d)", Integer.toString(nodeDetailsList.size())));
            removeNodeFromList(deregisterRequest.getNodeIpAddress(), deregisterRequest.getPortNum());
        }
        sendEvent(registerAck, socket);
    }

    private void registerNode(final RegisterRequest registerRequestEvent, final Socket socket) {
        /* Checks
           1. The requested IP, is the same as the source of the request.
           2. Checks if already registered.
         */

        final boolean alreadyRegistered = registered(registerRequestEvent.getNodeIpAddress(), registerRequestEvent.getPortNum());
        final boolean ipAddressSame = matchIpForMessageAndSocket(socket, registerRequestEvent.getNodeIpAddress());
        RegisterAcknowledgement registerAck;
        if(alreadyRegistered || !ipAddressSame) {
            if(alreadyRegistered) { /*Branch for the Failure types*/
                registerAck = new RegisterAcknowledgement(EventConstants.REGISTER_OR_DEREGISTER_FAILURE, MessageConstants.NODE_ALREADY_REGISTERED);
            } else {
                registerAck = new RegisterAcknowledgement(EventConstants.REGISTER_OR_DEREGISTER_FAILURE, MessageConstants.IP_MISMATCH_REGISTRATION
                        + socket.getRemoteSocketAddress().toString() + " " + registerRequestEvent.getNodeIpAddress());
            }
            System.out.println("Unable to register the node, already exist");
        } else {
            nodeDetailsList.add(new NodeDetails(registerRequestEvent.getNodeIpAddress(), registerRequestEvent.getPortNum()));
            registerAck = new RegisterAcknowledgement(EventConstants.REGISTER_OR_DEREGISTER_SUCCESS,
                    MessageConstants.SUCCESSFUL_REGISTRATION.replaceAll("(%d)", Integer.toString(nodeDetailsList.size())));
            System.out.println("Node Successfully Registered " + socket.getInetAddress() + ":" + socket.getPort());

        }
        final boolean success = sendEvent(registerAck, socket);
        if(!success) {  /*If the message send failed */
            removeNodeFromList(registerRequestEvent.getNodeIpAddress(), registerRequestEvent.getPortNum());
        }
    }

    private void removeNodeFromList(final String nodeIpAddres, final int portNum) {
        int indexForRemoval = -1;
        for (final NodeDetails nodeDetails : nodeDetailsList)
            if (portNum == nodeDetails.getPortNum() || nodeIpAddres.equals(nodeDetails.getNodeName())) {
                indexForRemoval = nodeDetailsList.indexOf(nodeDetails);
            }
        if (indexForRemoval != -1) {
            nodeDetailsList.remove(indexForRemoval);
        }
    }

    private boolean sendEvent(final Event eventToSend, final Socket socket) {
        try {
            final TCPSender sender = new TCPSender(socket);
            byte [] bytesToSend = eventToSend.getBytes();
//            System.out.println("ACK Data Send : " + Arrays.toString(bytesToSend));
            sender.sendData(bytesToSend);
        } catch (IOException ioe) {
            System.out.println("Error : Unable to Send registration acknowledgement ");
            ioe.printStackTrace();
            return false;
        }
        System.out.println("Send registration acknowledgement " + socket.getInetAddress() + "     " + socket.getLocalPort() + "  " + socket.getPort()  );
        return true;
    }

/*
    private Socket getSocketToSendEvent(final String hostName, final int portNum) {
        return communicationPreferences.get(hostName + portNum);
    }

    //Copied from the Messaging Node.
    private void handleSend(final String commandLine) {
        String [] parts = commandLine.split(" ");
        final String message =  parts[3];
        final Socket socket = communicationPreferences.get(parts[1]+ HelperUtils.getInt(parts[2]));
        if(socket == null) {
            System.out.println("Unable to get the socket for communication - Node might not be connected !!!");
            return;
        }
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (final IOException ioe) {
            ioe.printStackTrace();
            System.exit(-1);
        }
    }

*/
    private boolean registered (final String nodeName, final int portNum) {
        for(final NodeDetails nodeDetails : nodeDetailsList) {
            if ((nodeDetails.getNodeName().equals(nodeName)) && (nodeDetails.getPortNum()== portNum)) {
                return true;
            }
        }
        return false;
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

}
