// (C) Copyright 2015 Hewlett Packard Enterprise Development LP
package cs455.overlay.wireformats;

import cs455.overlay.constants.EventType;

public class Default extends AbstractEvent {
    public Default() {
        super(EventType.DEFAULT.getValue());
    }

}
