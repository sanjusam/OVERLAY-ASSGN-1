package cs455.overlay.transport;

import java.net.Socket;

public interface ConnectionObservable {
    void registerListeners(final ConnectionObserver observers);
    void removeListeners(final ConnectionObserver observers);
    void notifyListeners(final Socket socket);
}
