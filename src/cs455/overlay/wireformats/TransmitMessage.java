package cs455.overlay.wireformats;


import cs455.overlay.constants.EventType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class TransmitMessage extends AbstractEvent {
    private final int messageContent;
    private final String destination;

    public TransmitMessage(final int messageContent, final String destination) {
        super(EventType.MESSAGE_TRANSMIT.getValue());
        this.messageContent =  messageContent;
        this.destination = destination;
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));
        dout.writeInt(type);
        dout.writeInt(messageContent);
        writeStringAsByte(dout, destination);
        dout.flush();
        byte[] marshalledBytes = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

    public TransmitMessage(byte[] marshalledBytes) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(marshalledBytes);
        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(byteArrayInputStream));
        type = dataInputStream.readInt();
        messageContent = dataInputStream.readInt();
        destination = readStringFromBytes(dataInputStream);
        byteArrayInputStream.close();
        dataInputStream.close();
    }

    public String getDestination () {
        return destination;
    }

    public int getMessageContent() {
        return messageContent;
    }


}
