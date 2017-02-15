package cs455.overlay.wireformats;

import cs455.overlay.constants.EventType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegisterAcknowledgementEvent extends AbstractEvent {
    private byte code;
    private String additionalInfo;

    public byte getCode() {
        return code;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public RegisterAcknowledgementEvent(final byte code, final String additionalInfo) {
        super(EventType.REGISTER_RESPONSE.getValue());
        this.code = code;
        this.additionalInfo = additionalInfo;
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));
        dout.writeInt(type);
        dout.write(code);
        writeStringAsByte(dout, additionalInfo);
        dout.flush();
        byte[] marshalledBytes = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

    public RegisterAcknowledgementEvent(byte[] marshalledBytes) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(marshalledBytes);
        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(byteArrayInputStream));
        type = dataInputStream.readInt();
        code = dataInputStream.readByte();
        additionalInfo = readStringFromBytes(dataInputStream);
        byteArrayInputStream.close();
        dataInputStream.close();
    }

}
