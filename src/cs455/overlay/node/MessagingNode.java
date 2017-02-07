package cs455.overlay.node;

import cs455.overlay.transport.TCPCommunicationHandler;
import cs455.overlay.utils.HelperUtils;
import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.RegisterRequest;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;

public class MessagingNode extends AbstractNode {

    private static String registryHost;
    private static int registryPort;
    private static Socket connectionToRegistry;

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
        TCPCommunicationHandler registerCommHandler;
        try {
            connectionToRegistry = new Socket(registryHost, registryPort);
            registerCommHandler = update(connectionToRegistry);  //TODO : Either use the return value or pick from the map.
            final Event registerEvent = new RegisterRequest(myHostName, myPortNum);
            registerCommHandler.sendData(registerEvent.getBytes());
        } catch (IOException ioe) {
            System.out.println("Exiting : Unable to initiate connection to registry.");
            System.exit(-1);
        }
       System.out.println("Send registration request to the registry!");
    }


}
