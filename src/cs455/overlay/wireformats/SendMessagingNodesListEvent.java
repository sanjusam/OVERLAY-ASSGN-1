package cs455.overlay.wireformats;

import cs455.overlay.constants.EventType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SendMessagingNodesListEvent extends AbstractEvent {
    private final String nodeDelimiter = "DELIM";

    private int numNodes;
    private final List<String> messagingNodeList = new ArrayList<>();  //List of Strings pattern : Node_Name:myPortNum

    public SendMessagingNodesListEvent(final int numNodes) {
        super(EventType.MESSAGING_NODES_LIST.getValue());
        this.numNodes = numNodes;
    }

    public int addNodesToList(final String nodeDetails) {
        messagingNodeList.add(nodeDetails);
        numNodes = messagingNodeList.size();
        return numNodes;
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));
        dout.writeInt(type);
        dout.writeInt(numNodes);
        String longListOfNodeDetails = "";
        for(String messagingNode : messagingNodeList) {
            longListOfNodeDetails = longListOfNodeDetails + messagingNode + nodeDelimiter;
        }
        writeStringAsByte(dout, longListOfNodeDetails);
        dout.flush();
        byte[] marshalledBytes = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

    public SendMessagingNodesListEvent(byte[] marshalledBytes) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(marshalledBytes);
        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(byteArrayInputStream));
        type = dataInputStream.readInt();
        numNodes = dataInputStream.readInt();
        String longListOfNodeDetails = readStringFromBytes(dataInputStream);
        for(final String messagingNode : longListOfNodeDetails.split(nodeDelimiter)) {
            messagingNodeList.add(messagingNode);
        }
        byteArrayInputStream.close();
        dataInputStream.close();
    }

    public int getNumNodes() {
        return numNodes;
    }

    public List<String> getMessagingNodeList() {
        return messagingNodeList;
    }

}
