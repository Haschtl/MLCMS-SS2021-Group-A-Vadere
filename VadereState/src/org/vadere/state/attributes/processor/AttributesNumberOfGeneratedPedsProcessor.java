package org.vadere.state.attributes.processor;

public class AttributesNumberOfGeneratedPedsProcessor extends AttributesProcessor {

    private double startTime = 10.0;
    private double endTime = -1; // -1 until simulation finished


    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }
}
