package cs455.overlay.transport;

import java.net.Socket;

public interface ConnectionObserver {
    TCPCommunicationHandler update(final Socket socket);
}
