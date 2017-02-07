package cs455.overlay.transport;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

public class TCPSender {
    private final Socket socket;
    private final DataOutputStream dataOutputStream;

    public TCPSender(final Socket socket) throws IOException {
        this.socket = socket;
        dataOutputStream= new DataOutputStream(socket.getOutputStream());
    }

    public synchronized void sendData(final byte[] dataToSend) throws IOException {
        int dataLength = dataToSend.length;
        dataOutputStream.writeInt(dataLength);
        dataOutputStream.write(dataToSend, 0, dataLength);
        dataOutputStream.flush();
    }

    @Deprecated
    public void writeStuffToClient(final String message) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(message);
    }
}
