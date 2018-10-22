package org.vadere.util.potential.timecost;

import org.vadere.geometry.shapes.IPoint;

/**
 * Interface for a generic time cost function in 2D.
 * 
 * 
 */
public interface ITimeCostFunction {

	/**
	 * Computes a generic, double-valued cost at a given point in 2D space.
	 * 
	 * @param p
	 *        a point in 2D space.
	 * @return the double-valued cost at p.
	 */
	double costAt(IPoint p);

	/**
	 * Prepares the dynamic timeCostFunction for the next step.
	 */
	void update();

	/**
	 * Indicates that this ITimeCostFunction is for generating a dynamic
	 * potential field.
	 * 
	 * @return true => this ITimeCostFunction is for generating a dynaic
	 *         potential field, otherwise false
	 */
	boolean needsUpdate();
}
