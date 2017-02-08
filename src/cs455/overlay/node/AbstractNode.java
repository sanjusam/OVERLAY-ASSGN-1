package cs455.overlay.node;

import cs455.overlay.constants.MessageConstants;
import cs455.overlay.transport.ConnectionObserver;
import cs455.overlay.transport.TCPCommunicationHandler;
import cs455.overlay.transport.TCPServerThread;
import cs455.overlay.utils.HelperUtils;
import cs455.overlay.wireformats.Event;
import cs455.overlay.constants.EventConstants;
import cs455.overlay.constants.EventType;
import cs455.overlay.wireformats.LinkWeights;
import cs455.overlay.wireformats.MessagingNodesList;
import cs455.overlay.wireformats.RegisterAcknowledgement;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractNode implements Node, ConnectionObserver {
    public static int portNum;
    public static String ipAddress;

    final Map<String, TCPCommunicationHandler> communicationHandlerMap = new HashMap<>();
    protected boolean overlayConfigured = false;

    protected int startTCPServerThread (final int portNum) throws IOException {
        /* 1. Starts up the server thread on the node on a random/specified port
           2. Registers this as a listener for all the incoming connection on the server thread.
           3. Returns the port num, useful when trying to start on a random port number.
         */
        TCPServerThread serverThread;
        if(portNum == -1) {
            serverThread  = new TCPServerThread();
        } else {
            serverThread = new TCPServerThread(portNum);
        }
        serverThread.registerListeners(this);
        Thread serverRunnerThread = new Thread(serverThread);
        serverRunnerThread.start();
        return serverThread.getPortNum();
    }

    @Override
    public TCPCommunicationHandler update(Socket socket) {
        final String hostName = HelperUtils.convertLoopBackToValidIpAddress(socket.getInetAddress().toString());
        System.out.println("Adding the new connection....! " + hostName + "  " + socket.getPort());
        TCPCommunicationHandler communicationHandler = new TCPCommunicationHandler(socket, this);
        addConnectionToPool(hostName + MessageConstants.NODE_PORT_SEPARATOR + socket.getPort(), communicationHandler); //TODO :: Check
        return communicationHandler;
    }

    @Override
    public void onEvent(final Event event, final Socket socket) {
        final int eventTypeReceived = event.getType();
        if(eventTypeReceived == EventType.REGISTER_RESPONSE.getValue()) {
            RegisterAcknowledgement acknowledgement = (RegisterAcknowledgement) event;
            if(acknowledgement.getCode() == EventConstants.REGISTER_OR_DEREGISTER_FAILURE) {
                System.out.println("Node Failed to Register");
            } else {
                System.out.println("Node Registration Successful");
            }
            System.out.println("Additional Information : " + ((RegisterAcknowledgement)event).getAdditionalInfo());
        } else if(eventTypeReceived == EventType.MESSAGING_NODES_LIST.getValue()) {
            System.out.println("Request to setup overlay received from Registry");
            makeConnectionsOnOverLayNodes((MessagingNodesList)event);
        } else if(eventTypeReceived == EventType.Link_Weights.getValue()) {
            System.out.println("Received Link Weights");
            processLinkWeights();
        }
    }

    private void makeConnectionsOnOverLayNodes(final MessagingNodesList nodesList) {
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
    public void processCommand(final String command) {
        final EventType eventType = EventType.getEventTypeFromCommand(command.split(" ")[0]);
        if(eventType == null) {
            System.out.println("Unknown command");
            return;
        }
        if(eventType == EventType.MESSAGING_NODES_LIST){
            try {
                setupOverlay(command);
            } catch (IOException ioe) {
                System.out.println("Failed to setup overlay.");
                return;
            }
        }
        if(eventType == EventType.SEND_LINK_WEIGHTS) {
            sendLinkWeight();
        } else if(eventType == EventType.LIST_MSG_NODES) {
            listMessagingNodes();
        } else if (eventType == EventType.LIST_WEIGHTS) {
            ListEdgeWeight();
        } else if (eventType == EventType.SEND_LINK_WEIGHTS) {
            sendLinkWeight();
        }
    }

    protected boolean validFirstArgument(final String commandToValidate, final int expectedParts, final boolean isNumeric) {
        if(commandToValidate.split(" ").length < expectedParts) {
            System.out.println("The command " + commandToValidate.split(" ")[0] +" is expected to have argument[s]");
            return false;
        }
        if(isNumeric) {  /*If its supposed to be numeric, then convert and check if its numeric*/
            String cmdArg = commandToValidate.split(" ")[1];
            int firstArgNum = HelperUtils.getInt(cmdArg);
            if (firstArgNum == -1) {
                System.out.println("The command " + commandToValidate.split(" ")[0] + " is expected to have a numeric argument " + cmdArg);
                return false;
            }
        }
        return true;
    }

    protected void addConnectionToPool(final String key, final TCPCommunicationHandler communicationHandler) {
        if(communicationHandlerMap.get(key) == null) {
            communicationHandlerMap.put(key, communicationHandler);
        } else {
            System.out.println("Connection Already exist, skip adding");
        }
    }

    protected TCPCommunicationHandler getConnectionFromPool(final String key) {
        return communicationHandlerMap.get(key);
    }

    protected void startCommandListener() {
        final CommandListener commandListener = new CommandListener(this);
        final Thread commandListenerThread = new Thread(commandListener);
        commandListenerThread.start();
    }
}
