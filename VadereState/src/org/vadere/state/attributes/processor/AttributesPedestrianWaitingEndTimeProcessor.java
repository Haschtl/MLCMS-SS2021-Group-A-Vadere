package org.vadere.state.attributes.processor;

import org.vadere.geometry.shapes.VRectangle;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AttributesPedestrianWaitingEndTimeProcessor extends AttributesProcessor {
    private VRectangle waitingArea = new VRectangle(0, 0, 1, 1);

    public VRectangle getWaitingArea() {
        return waitingArea;
    }

    public void setWaitingArea(VRectangle waitingArea) {
        checkSealed();
        this.waitingArea = waitingArea;
    }
}
