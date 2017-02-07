package cs455.overlay.node;

import cs455.overlay.constants.MessageConstants;
import cs455.overlay.transport.TCPCommunicationHandler;
import cs455.overlay.transport.TCPSender;
import cs455.overlay.utils.HelperUtils;
import cs455.overlay.wireformats.DeregisterRequest;
import cs455.overlay.wireformats.Event;
import cs455.overlay.constants.EventConstants;
import cs455.overlay.constants.EventType;
import cs455.overlay.wireformats.MessagingNodesList;
import cs455.overlay.wireformats.RegisterRequest;
import cs455.overlay.wireformats.RegisterAcknowledgement;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Registry extends AbstractNode implements Node {
    final List<NodeDetails> nodeDetailsList = new ArrayList<>();

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
    public void setupOverlay(final int numConnections)throws IOException {
        if(nodeDetailsList.size() < numConnections) {
            System.out.println("Unable to create over-lay : Number of Messaging Nodes Registered " + nodeDetailsList.size() + " Num Overlay requested " + numConnections );
            return;
        }
        final Map<NodeDetails, MessagingNodesList>  allOverlays = buildOverlayNodes();
        for(final NodeDetails nodeDetails : allOverlays.keySet()) {
            final MessagingNodesList messagingNodesList = allOverlays.get(nodeDetails);
            TCPCommunicationHandler communicationHandler = getConnectionFromPool(nodeDetails.getFormattedString());
            if(communicationHandler == null) {
                //TODO :: Create connection.
                Socket socket= new Socket(nodeDetails.getNodeName(), nodeDetails.getPortNum());
                communicationHandler = update(socket);
                communicationHandler.sendData(messagingNodesList.getBytes());
            }
        }

    }

    private Map<NodeDetails, MessagingNodesList>  buildOverlayNodes() {
        //TODO : Connecting all nodes.
        final Map<NodeDetails, MessagingNodesList> overLayList = new HashMap<>();
        MessagingNodesList temp;
        boolean allNodeListCreated = false;
        for(final NodeDetails nodeDetails : nodeDetailsList) {
            if(!allNodeListCreated) {
                temp = buildOverlayForAllNodes(nodeDetails);
                allNodeListCreated = true;
                overLayList.put(nodeDetails, temp);
            }
        }
        return overLayList;
    }


    private MessagingNodesList buildOverlayForAllNodes(final NodeDetails sourceNode) {
        final List<NodeDetails> tmpNodeDetailsList  = nodeDetailsList;
        final MessagingNodesList overlayList = new MessagingNodesList(0);
        for(final NodeDetails nodeDetails : tmpNodeDetailsList) {
            if(!sourceNode.getFormattedString().equals(nodeDetails.getFormattedString())) {
                overlayList.addNodesToList(nodeDetails.getFormattedString());
                nodeDetails.incrementConnections();
            }
        }
        return overlayList;
    }

    private MessagingNodesList buildOverlayEachNodeNodes(final NodeDetails sourceNode, final int numConnections) {
        //SANJU TODO :
        return null;
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
