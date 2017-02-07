package cs455.overlay.wireformats;

import java.io.IOException;

public interface Event {
    byte[] getBytes() throws IOException;
    int getType();

}
