package cs455.overlay.wireformats;


import cs455.overlay.constants.EventType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SendListeningPortEvent extends AbstractEvent {
    private final int listeningPort;
    public SendListeningPortEvent(final int listeningPort) {
        super(EventType.SEND_LISTENING_PORT.getValue());
        this.listeningPort = listeningPort;
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));
        dout.writeInt(type);
        dout.writeInt(listeningPort);
        dout.flush();
        byte[] marshalledBytes = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        dout.close();
        return marshalledBytes;
    }


    public SendListeningPortEvent(byte[] marshalledBytes) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(marshalledBytes);
        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(byteArrayInputStream));
        type = dataInputStream.readInt();
        listeningPort= dataInputStream.readInt();
        byteArrayInputStream.close();
        dataInputStream.close();
    }

    public int getListeningPort() {
        return listeningPort;
    }
}
