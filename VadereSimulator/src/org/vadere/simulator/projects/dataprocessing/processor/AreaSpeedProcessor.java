package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesAreaSpeedProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.geometry.shapes.VPoint;

import java.util.Map;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;

/**
 * @author Mario Teixeira Parente
 *
 */
@DataProcessorClass(label = "AreaSpeedProcessor")
public class AreaSpeedProcessor extends AreaDataProcessor<Double> {
    private PedestrianPositionProcessor pedPosProc;
    private PedestrianVelocityProcessor pedVelProc;

    public AreaSpeedProcessor() {
        super("areaSpeed");
        setAttributes(new AttributesAreaSpeedProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        int step = state.getStep();

        this.pedPosProc.update(state);
        this.pedVelProc.update(state);

        Map<PedestrianIdKey, VPoint> positionMap = this.pedPosProc.getPositions(new TimestepKey(step));

        int pedCount = 0;
        double sumVelocities = 0.0;

        for (Map.Entry<PedestrianIdKey, VPoint> entry : positionMap.entrySet()) {
            final int pedId = entry.getKey().getPedestrianId();
            final VPoint pos = entry.getValue();

            if (getMeasurementArea().contains(pos)) {
                sumVelocities += this.pedVelProc.getValue(new TimestepPedestrianIdKey(step, pedId));
                pedCount++;
            }
        }

        this.putValue(new TimestepKey(step), sumVelocities / pedCount);
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesAreaSpeedProcessor att = (AttributesAreaSpeedProcessor) this.getAttributes();
        this.pedPosProc = (PedestrianPositionProcessor) manager.getProcessor(att.getPedestrianPositionProcessorId());
        this.pedVelProc = (PedestrianVelocityProcessor) manager.getProcessor(att.getPedestrianVelocityProcessorId());

    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesAreaSpeedProcessor());
        }

        return super.getAttributes();
    }
}
