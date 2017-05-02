package org.vadere.util.geometry.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.Collection;
import java.util.Optional;

/**
 * @author Benedikt Zoennchen
 *
 * @param <P>
 * @param <E>
 * @param <F>
 */
public interface IPointLocator<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> extends ITriEventListener<P, E, F> {
	Collection<F> locatePoint(final P point, final boolean insertion);

	Optional<F> locate(final P point);

	enum Type {
		DELAUNAY_TREE,
		DELAUNAY_HIERARCHY,
		BASE
	}
}
