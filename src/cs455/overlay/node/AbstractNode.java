package cs455.overlay.node;

import cs455.overlay.transport.ConnectionObserver;
import cs455.overlay.transport.TCPCommunicationHandler;
import cs455.overlay.transport.TCPServerThread;
import cs455.overlay.utils.HelperUtils;
import cs455.overlay.wireformats.Event;
import cs455.overlay.constants.EventConstants;
import cs455.overlay.constants.EventType;
import cs455.overlay.wireformats.MessagingNodesList;
import cs455.overlay.wireformats.RegisterAcknowledgement;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class AbstractNode implements Node, ConnectionObserver {
    public static int portNum;
    public static String ipAddress;

    final Map<String, TCPCommunicationHandler> communicationHandlerMap = new HashMap<>();

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
        addConnectionToPool(hostName + socket.getPort(), communicationHandler); //TODO :: Check
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

        }
    }

    private void makeConnectionsOnOverLayNodes(final MessagingNodesList nodesList) {
        //TODO :: SANJU

    }

    @Override
    public void processCommand(final String command) {
        final EventType eventType = EventType.getEventTypeFromCommand(command.split(" ")[0]);
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

    @Override
    public void setupOverlay(final int numConnections) throws IOException {
        System.out.println("Setup Overlay not supported on a messaging node.");
        return;
    }

    protected void startCommandListener() {
        final CommandListener commandListener = new CommandListener(this);
        final Thread commandListenerThread = new Thread(commandListener);
        commandListenerThread.start();
    }
}
