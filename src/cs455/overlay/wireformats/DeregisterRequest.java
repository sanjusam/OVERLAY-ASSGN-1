// (C) Copyright 2015 Hewlett Packard Enterprise Development LP
package cs455.overlay.wireformats;

import cs455.overlay.constants.EventType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DeregisterRequest extends AbstractEvent {

    private final String nodeIpAddress;
    private final int portNum;

    DeregisterRequest(final String nodeIpAddress, final int portNum) {
        super(EventType.DEREGISTER_REQUEST.getValue());
        this.nodeIpAddress = nodeIpAddress;
        this.portNum = portNum;
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));
        dout.writeInt(type);
        writeStringAsByte(dout, nodeIpAddress);
        dout.writeInt(portNum);
        dout.flush();
        byte[] marshalledBytes = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

    public DeregisterRequest(byte[] marshalledBytes) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(marshalledBytes);
        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(byteArrayInputStream));
        type = dataInputStream.readInt();
        nodeIpAddress = readStringFromBytes(dataInputStream);
        portNum = dataInputStream.readInt();
        byteArrayInputStream.close();
        dataInputStream.close();
    }

    public String getNodeIpAddress() {
        return nodeIpAddress;
    }

    public int getPortNum() {
        return portNum;
    }}
