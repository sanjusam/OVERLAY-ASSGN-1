package cs455.overlay.node;

import cs455.overlay.constants.MessageConstants;
import cs455.overlay.transport.ConnectionObserver;
import cs455.overlay.transport.TCPCommunicationHandler;
import cs455.overlay.transport.TCPServerThread;
import cs455.overlay.utils.HelperUtils;
import cs455.overlay.wireformats.*;
import cs455.overlay.constants.EventType;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractNode implements Node, ConnectionObserver {
    public static int portNum;
    public static String ipAddress;

    final Map<String, TCPCommunicationHandler> communicationHandlerMap = new HashMap<>();
    TCPCommunicationHandler registerCommHandler;
    protected boolean overlayConfigured = false;
    protected boolean requestedToExitOverlay = false;

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

    @Override  //Something that comes to the Node
    public void onEvent(final Event event, final Socket socket) {
        final int eventTypeReceived = event.getType();
        if(event.getType() == EventType.REGISTER_REQUEST.getValue()) {
            registerNode((RegisterRequest) event, socket);
        } else if(event.getType() == EventType.DEREGISTER_REQUEST.getValue()) {
            deRegisterNode((DeregisterRequest) event, socket);
        } else if(eventTypeReceived == EventType.REGISTER_RESPONSE.getValue()) {
            registerNodeAcknowledgement((RegisterAcknowledgement) event);
        } else if (eventTypeReceived == EventType.MESSAGING_NODES_LIST.getValue()) {
            makeConnectionsOnOverLayNodes((MessagingNodesList) event);
        } else if(eventTypeReceived == EventType.Link_Weights.getValue()) {
            processLinkWeights((LinkWeights) event);
        } else if(eventTypeReceived == EventType.SIGNAL_TO_START_MSG.getValue()) {
            initiateMessagingSignalForNodes(((TaskInitiate) event).getNumRoundsAsString());
        }  else if(eventTypeReceived == EventType.TASK_INITIATE.getValue()) {
            startMessaging(((TaskInitiate) event).getNumRoundsAsString());
        } else if(eventTypeReceived == EventType.TASK_COMPLETE.getValue()) {
            final TaskComplete taskComplete = (TaskComplete) event;
            acknowledgeTaskComplete(taskComplete.getNodeIpAddress(), taskComplete.getPortNum());
        } else if (eventTypeReceived == EventType.PULL_TRAFFIC_SUMMARY.getValue()) {
            pullTrafficSummary();
        } else if (eventTypeReceived == EventType.TRAFFIC_SUMMARY.getValue()) {
            printTrafficSummary((TrafficSummary) event);
        } else if(eventTypeReceived == EventType.MESSAGE_TRANSMIT.getValue()) {
            processReceivedMessage((TransmitMessage) event);
        }

    }

    @Override //Something that would be send out - reading from the command line
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
            listEdgeWeight();
        } else if (eventType == EventType.SIGNAL_TO_START_MSG) {
            initiateMessagingSignalForNodes(command);
        } else if (eventType == EventType.TASK_INITIATE) {
            startMessaging(command);
        } else if (eventType == EventType.DEREGISTER_REQUEST) {
            requestDeRegisterNode();
        } else if (eventType == EventType.EXIT_OVERLAY) {
            requestDeRegisterNode(); //TODO :: WHAT??
        }
    }


    private void requestDeRegisterNode() {
        final DeregisterRequest deregisterRequest = new DeregisterRequest(ipAddress, portNum);
        try {
            sendMessageToRegistry(deregisterRequest.getBytes());
        } catch (IOException ioe ) {
            System.out.println("Unable to send the traffic stats to the registry");
        }
    }

    private void requestExitOverlay() {
        requestedToExitOverlay = true;
        requestDeRegisterNode();
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

    protected boolean sendMessageToRegistry(byte [] bytesToSend) {
        return registerCommHandler.sendData(bytesToSend);
    }


}
