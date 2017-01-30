package org.vadere.util.potential.calculators;

import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.timecost.ITimeCostFunction;

import java.awt.*;
import java.util.List;


public class EikonalSolverFMMTriangulation implements EikonalSolver  {

	public EikonalSolverFMMTriangulation(final List<VShape> targetShapes,
	                                     final ITimeCostFunction timeCostFunction) {

	}

	@Override
	public void initialize() {

	}

    @Override
    public CellGrid getPotentialField() {
        return null;
    }

    @Override
	public double getValue(double x, double y) {
		return 0;
	}

	public boolean isValidPoint(Point point) {
		return false;
	}


}
