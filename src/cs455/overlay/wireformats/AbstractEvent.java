// (C) Copyright 2015 Hewlett Packard Enterprise Development LP
package cs455.overlay.wireformats;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AbstractEvent implements Event {
    public int type;

    AbstractEvent() {
    }

    AbstractEvent(final int type) {
        this.type = type;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return new byte[0];
    }

    @Override
    public int getType() {
        return type;
    }

    protected void writeStringAsByte(final DataOutputStream dataOutputStream, final String textToWrite) throws IOException {
        byte[] identifierBytes = textToWrite.getBytes();
        int elementLength = identifierBytes.length;
        dataOutputStream.writeInt(elementLength);
        dataOutputStream.write(identifierBytes);
    }

    protected String readStringFromBytes (final DataInputStream dataInputStream) throws IOException {
        int identifierLength = dataInputStream.readInt();
        byte[] identifierBytes = new byte[identifierLength];
        dataInputStream.readFully(identifierBytes);
        return new String (identifierBytes);

    }
}
