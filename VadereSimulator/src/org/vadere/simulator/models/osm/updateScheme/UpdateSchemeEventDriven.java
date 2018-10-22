package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.geometry.shapes.VPoint;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * @author Benedikt Zoennchen
 */
public class UpdateSchemeEventDriven implements UpdateSchemeOSM {

	private final Topography topography;
	protected PriorityQueue<PedestrianOSM> pedestrianEventsQueue;

	public UpdateSchemeEventDriven(@NotNull final Topography topography) {
		this.topography = topography;
		this.pedestrianEventsQueue = new PriorityQueue<>(100, new ComparatorPedestrianOSM());
		this.pedestrianEventsQueue.addAll(topography.getElements(PedestrianOSM.class));
	}

	@Override
	public void update(final double timeStepInSec, final double currentTimeInSec) {
		for(PedestrianOSM pedestrianOSM : topography.getElements(PedestrianOSM.class)) {
			pedestrianOSM.clearStrides();
		}

		if(!pedestrianEventsQueue.isEmpty()) {
			// event driven update ignores time credits!
			while (pedestrianEventsQueue.peek().getTimeOfNextStep() < currentTimeInSec) {
				PedestrianOSM ped = pedestrianEventsQueue.poll();
				update(ped, currentTimeInSec);
				//System.out.println(ped.getId());
				pedestrianEventsQueue.add(ped);
			}
		}
	}

	protected void update(@NotNull final PedestrianOSM pedestrian, final double currentTimeInSec) {
		VPoint oldPosition = pedestrian.getPosition();

		// for the first step after creation, timeOfNextStep has to be initialized
		if (pedestrian.getTimeOfNextStep() == 0) {
			pedestrian.setTimeOfNextStep(currentTimeInSec);
		}

		// this can cause problems if the pedestrian desired speed is 0 (see speed adjuster)
		pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());
		pedestrian.updateNextPosition();
		makeStep(topography, pedestrian, pedestrian.getDurationNextStep());

		pedestrian.setTimeOfNextStep(pedestrian.getTimeOfNextStep() + pedestrian.getDurationNextStep());
		topography.moveElement(pedestrian, oldPosition);
	}

	@Override
	public void elementRemoved(@NotNull final Pedestrian element) {
		pedestrianEventsQueue.remove(element);
	}

	@Override
	public void elementAdded(final Pedestrian element) {
		pedestrianEventsQueue.add((PedestrianOSM) element);
	}

	/**
	 * Compares the time of the next possible move.
	 */
	private class ComparatorPedestrianOSM implements Comparator<PedestrianOSM> {
		@Override
		public int compare(PedestrianOSM ped1, PedestrianOSM ped2) {
			int timeCompare = Double.compare(ped1.getTimeOfNextStep(), ped2.getTimeOfNextStep());
			if(timeCompare != 0) {
				return timeCompare;
			}
			else {
				if(ped1.getId() < ped2.getId()) {
					return -1;
				}
				else {
					return 1;
				}
			}
		}
	}
}
