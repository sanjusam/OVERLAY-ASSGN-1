package cs455.overlay.transport;


import java.io.IOException;
import java.net.ServerSocket;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class TCPServerThread implements ConnectionObservable, Runnable {
    private final int portNum;
    ServerSocket serverSocket;
    final List<ConnectionObserver> incomingConnectionObserverList = new ArrayList<>();

    public TCPServerThread(final int portNum) throws IOException {
        this.portNum = portNum;
        serverSocket = new ServerSocket(portNum);
        System.out.println("Starting Registry Server on port " + portNum);
    }

    public TCPServerThread() throws IOException {
        serverSocket = new ServerSocket(0);
        portNum = serverSocket.getLocalPort();
        System.out.println("Starting Messaging Server on port " + portNum);
    }

    public int getPortNum() {
        return portNum;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                notifyListeners(socket);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    @Override
    public void registerListeners(final ConnectionObserver observer) {
        if(!incomingConnectionObserverList.contains(observer)) {
            incomingConnectionObserverList.add(observer);
        }
    }

    @Override
    public void removeListeners(final ConnectionObserver observer) {
        if(incomingConnectionObserverList.contains(observer)) {
            incomingConnectionObserverList.remove(incomingConnectionObserverList.indexOf(observer));
        }
    }

    @Override
    public void notifyListeners(final Socket socket) {  // Updates all listens that a new connection has come in
        for(final ConnectionObserver connectionObserver : incomingConnectionObserverList) {
            connectionObserver.update(socket);
        }
    }
}

