package cs455.overlay.wireformats;

import cs455.overlay.constants.EventType;
import cs455.overlay.constants.MessageConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Formatter;

public class TrafficSummary extends AbstractEvent {
    private String ipAddress;
    private int portNum;
    private int numOfMessagesSend;
    private long sumOfSendMessage;

    private int numOfMessagesReceived;
    private long sumOfReceivedMessages;

    private int numOfMessagesRelayed;

    public TrafficSummary() {
        super(EventType.TRAFFIC_SUMMARY.getValue());
    }


    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));
        dout.writeInt(type);
        dout.writeInt(portNum);
        dout.writeInt(numOfMessagesSend);
        dout.writeInt(numOfMessagesReceived);
        dout.writeInt(numOfMessagesRelayed);
        writeStringAsByte(dout, ipAddress);
        dout.writeLong(sumOfSendMessage);
        dout.writeLong(sumOfReceivedMessages);
        dout.flush();
        byte[] marshalledBytes = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        dout.close();
        return marshalledBytes;
    }


    public TrafficSummary(byte[] marshalledBytes) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(marshalledBytes);
        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(byteArrayInputStream));
        type = dataInputStream.readInt();
        portNum = dataInputStream.readInt();
        numOfMessagesSend = dataInputStream.readInt();
        numOfMessagesReceived = dataInputStream.readInt();
        numOfMessagesRelayed = dataInputStream.readInt();
        ipAddress = readStringFromBytes(dataInputStream);
        sumOfSendMessage = dataInputStream.readLong();
        sumOfReceivedMessages = dataInputStream.readLong();
        byteArrayInputStream.close();
        dataInputStream.close();
    }


    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPortNum() {
        return portNum;
    }

    public void setPortNum(int portNum) {
        this.portNum = portNum;
    }

    public int getNumOfMessagesSend() {
        return numOfMessagesSend;
    }

    public void setNumOfMessagesSend(int numOfMessagesSend) {
        this.numOfMessagesSend = numOfMessagesSend;
    }

    public long getSumOfSendMessage() {
        return sumOfSendMessage;
    }

    public void setSumOfSendMessage(long sumOfSendMessage) {
        this.sumOfSendMessage = sumOfSendMessage;
    }

    public int getNumOfMessagesReceived() {
        return numOfMessagesReceived;
    }

    public void setNumOfMessagesReceived(int numOfMessagesReceived) {
        this.numOfMessagesReceived = numOfMessagesReceived;
    }

    public long getSumOfReceivedMessages() {
        return sumOfReceivedMessages;
    }

    public void setSumOfReceivedMessages(long sumOfReceivedMessage) {
        this.sumOfReceivedMessages = sumOfReceivedMessage;
    }
    public int getNumOfMessagesRelayed() {
        return numOfMessagesRelayed;
    }

    public void setNumOfMessagesRelayed(int numOfMessagesRelayed) {
        this.numOfMessagesRelayed = numOfMessagesRelayed;
    }



    public String summaryFormatted () {
        Formatter formatter = new Formatter();
        formatter.format("%-32s %-10d %-20d %-10d  %-20d  %-20d", ipAddress + ":" + portNum, numOfMessagesSend,
                sumOfSendMessage, numOfMessagesReceived, sumOfReceivedMessages, numOfMessagesRelayed);
        return formatter.toString();

    }
}
