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

public class LinkWeights extends AbstractEvent {
    private final String nodeDelimiter = "DELIM"; //TODO :: Find a better delimiter.
    private int numLinks;
    private List<String> linkWeightList = new ArrayList<>();  // hostnameA:portnumA hostnameB:portnumB weight

    public LinkWeights(final int numLinks) {
        super(EventType.Link_Weights.getValue());
        this.numLinks = numLinks;
    }

    public int addLinkWeights(final String nodeDetails) {
        linkWeightList.add(nodeDetails);
        numLinks = linkWeightList.size();
        return numLinks;
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));
        dout.writeInt(type);
        dout.writeInt(numLinks);
        String longListOfNodeDetails = "";
        for(String linkWeight : linkWeightList) {
            longListOfNodeDetails = longListOfNodeDetails + linkWeight + nodeDelimiter;
        }
        writeStringAsByte(dout, longListOfNodeDetails);
        dout.flush();
        byte[] marshalledBytes = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

    public LinkWeights(byte[] marshalledBytes) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(marshalledBytes);
        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(byteArrayInputStream));
        type = dataInputStream.readInt();
        numLinks = dataInputStream.readInt();
        String longListOfNodeDetails = readStringFromBytes(dataInputStream);
        for(final String nodeLink : longListOfNodeDetails.split(nodeDelimiter)) {
            linkWeightList.add(nodeLink);
        }
        byteArrayInputStream.close();
        dataInputStream.close();
    }

    public int getNumLinks() {
        return numLinks;
    }

    public List<String> getLinkWeightList() {
        return linkWeightList;
    }
}
