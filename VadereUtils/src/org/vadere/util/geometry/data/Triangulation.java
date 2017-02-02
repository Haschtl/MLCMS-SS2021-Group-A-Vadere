package org.vadere.util.geometry.data;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.Set;
import java.util.stream.Stream;

public interface Triangulation<P extends IPoint> {
	Face<P> locate(final double x, final double y);
	Face<P> locate(final IPoint point);
	Stream<Face<P>> streamFaces();
	Set<Face<P>> getFaces();
	void insert(final P point);
	void remove(final P point);
}
