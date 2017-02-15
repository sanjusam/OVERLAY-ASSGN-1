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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractNode implements Node, ConnectionObserver {
    static int myPortNum;
    static String myIpAddress;

    final Map<String, TCPCommunicationHandler> communicationHandlerMap = new ConcurrentHashMap<>();
    TCPCommunicationHandler registerCommHandler;
    boolean overlayConfigured = false;
    boolean requestedToExitOverlay = false;

    /*package */ int startTCPServerThread (final int portNum) throws IOException {
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
        TCPCommunicationHandler communicationHandler = new TCPCommunicationHandler(socket, this);
        addConnectionToPool(hostName + MessageConstants.NODE_PORT_SEPARATOR + socket.getPort(), communicationHandler);
        return communicationHandler;
    }

    @Override  //Something that comes to the Node
    public void onEvent(final Event event, final Socket socket) {
        final int eventTypeReceived = event.getType();
        if(event.getType() == EventType.REGISTER_REQUEST.getValue()) {
            registerNode((RegisterRequestEvent) event, socket);
        } else if(event.getType() == EventType.DEREGISTER_REQUEST.getValue()) {
            deRegisterNode((DeregisterRequestEvent) event, socket);
        } else if(eventTypeReceived == EventType.REGISTER_RESPONSE.getValue()) {
            registerNodeAcknowledgement((RegisterAcknowledgementEvent) event);
        } else if (eventTypeReceived == EventType.MESSAGING_NODES_LIST.getValue()) {
            makeConnectionsOnOverLayNodes((SendMessagingNodesListEvent) event);
        } else if(eventTypeReceived == EventType.Link_Weights.getValue()) {
            processLinkWeights((SendLinkWeightsEvent) event);
        } else if(eventTypeReceived == EventType.SIGNAL_TO_START_MSG.getValue()) {
            initiateMessagingSignalForNodes(((TaskInitiateEvent) event).getNumRoundsAsString());
        }  else if(eventTypeReceived == EventType.TASK_INITIATE.getValue()) {
            startMessaging(((TaskInitiateEvent) event).getNumRoundsAsString());
        } else if(eventTypeReceived == EventType.TASK_COMPLETE.getValue()) {
            final TaskCompleteEvent taskCompleteEvent = (TaskCompleteEvent) event;
            acknowledgeTaskComplete(taskCompleteEvent.getNodeIpAddress(), taskCompleteEvent.getPortNum());
        } else if (eventTypeReceived == EventType.PULL_TRAFFIC_SUMMARY.getValue()) {
            pullTrafficSummary();
        } else if (eventTypeReceived == EventType.TRAFFIC_SUMMARY.getValue()) {
            printTrafficSummary((TrafficSummaryEvent) event);
        } else if(eventTypeReceived == EventType.MESSAGE_TRANSMIT.getValue()) {
            processReceivedMessage((TransmitMessageEvent) event);
        } else if(eventTypeReceived == EventType.SEND_LISTENING_PORT.getValue()) {
            updateConnectionInfo((SendListeningPortEvent) event, socket);
        } else if(eventTypeReceived == EventType.FORCE_EXIT_EVERYONE.getValue()) {
            forceExit();
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
            checkCommandOptionsAndSendToProcesses(command, false);
        } else if (eventType == EventType.EXIT_OVERLAY) {
            exitOverlay();
        } else if (eventType == EventType.PRINT_SHORT_PATH) {
            printShortestPath();
        } else if(eventType == EventType.REGISTER_NODE) {
            checkCommandOptionsAndSendToProcesses(command, true);
        } else if(eventType == EventType.FORCE_EXIT_EVERYONE) {
            forceExit();
        }
    }


    void checkCommandOptionsAndSendToProcesses(final String command, final boolean register) {
        String parts[] = command.split(" ");
        String address ;
        int portNum;

        if(parts.length == 3) {
            if (parts[1] != null && parts[2] != null) {
                if (isNumeric(parts[2])) {
                    portNum = HelperUtils.getInt(parts[2]);
                    address = parts[1];
                    if (register) {
                        initiateNodeRegistration(address, portNum);  // Deal with the details passed
                    } else {
                        requestDeRegister(address, portNum);
                    }
                }
            }
        } else {
            if (register) {
                initiateNodeRegistration(myIpAddress, myPortNum);  // deal with my own host/port
            } else {
                requestDeRegister(myIpAddress, myPortNum);
            }
        }
    }

    /*package */ boolean validArgument(final String commandToValidate, final int expectedParts, final boolean isNumeric, final int whichIsNumeric) {
        if(commandToValidate.split(" ").length < expectedParts) {
            System.out.println("The command " + commandToValidate.split(" ")[0] +" is expected to have argument[s]");
            return false;
        }
        if(isNumeric) {  /*If its supposed to be numeric, then convert and check if its numeric*/
            String cmdArg = commandToValidate.split(" ")[whichIsNumeric];
            int firstArgNum = HelperUtils.getInt(cmdArg);
            if (firstArgNum == -1) {
                System.out.println("The command " + commandToValidate.split(" ")[0] + " is expected to have a numeric argument " + cmdArg);
                return false;
            }
        }
        return true;
    }

    private boolean isNumeric(final String checkString) {
        int numArg = HelperUtils.getInt(checkString);
        if (numArg== -1) {
            return false;
        }
        return true;
    }

    private void addConnectionToPool(final String key, final TCPCommunicationHandler communicationHandler) {
        if(communicationHandlerMap.get(key) == null) {
            communicationHandlerMap.put(key, communicationHandler);
        }
    }

    /*package */ TCPCommunicationHandler getConnectionFromPool(final String key) {
        return communicationHandlerMap.get(key);
    }

    void updateConnectionInformation(final Socket socket, final int portNum) {
        boolean updateNeeded = false;
        String keyToUpdate = "";
        TCPCommunicationHandler communicationHandlerToUpdate;
        for (final String key : communicationHandlerMap.keySet()) {
            final TCPCommunicationHandler communicationHandler = communicationHandlerMap.get(key);
            if(socket == communicationHandler.getSocket()) {
                updateNeeded = true;
                keyToUpdate = key;
                break;
            }
        }
        if(updateNeeded) {
            communicationHandlerToUpdate = communicationHandlerMap.get(keyToUpdate);
            communicationHandlerMap.remove(keyToUpdate);
            String newKey = keyToUpdate.split(MessageConstants.NODE_PORT_SEPARATOR)[0];
            newKey += MessageConstants.NODE_PORT_SEPARATOR + portNum;
            communicationHandlerMap.put(newKey, communicationHandlerToUpdate);
        }
    }

    /*package */ void startCommandListener() {
        final CommandListener commandListener = new CommandListener(this);
        final Thread commandListenerThread = new Thread(commandListener);
        commandListenerThread.start();
    }

    /*package */ boolean sendMessageToRegistry(byte [] bytesToSend) {
        return registerCommHandler.sendData(bytesToSend);
    }

}
