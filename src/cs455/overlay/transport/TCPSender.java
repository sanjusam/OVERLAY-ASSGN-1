package cs455.overlay.transport;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPSender {
    private final DataOutputStream dataOutputStream;

    public TCPSender(final Socket socket) throws IOException {
        dataOutputStream= new DataOutputStream(socket.getOutputStream());
    }

    public synchronized void sendData(final byte[] dataToSend) throws IOException {
        int dataLength = dataToSend.length;
        dataOutputStream.writeInt(dataLength);
        dataOutputStream.write(dataToSend, 0, dataLength);
        dataOutputStream.flush();
    }
}
