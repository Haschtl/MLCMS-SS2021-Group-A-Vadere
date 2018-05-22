package org.vadere.util.geometry.shapes;

import com.google.common.collect.ImmutableList;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.ShapeType;
import org.vadere.util.geometry.Vector2D;

public class VCircle implements VShape, ICircleSector {

	private final VPoint center;
	private final double radius;

	public VCircle(double x, double y, double radius) {
		this(new VPoint(x, y), radius);
	}

	public VCircle(VPoint center, double radius) {

		if (radius <= 0) {
			throw new IllegalArgumentException("Radius must be positive.");
		}

		this.center = center;
		this.radius = radius;
	}

	/**
	 * A circle at 0,0.
	 */
	public VCircle(double radius) {
		this(0, 0, radius);
	}

	public double getRadius() {
		return this.radius;
	}

	/**
	 * The distance to the boundary of the circle.
	 */
	@Override
	public double distance(VPoint pos) {
		return Math.abs(this.center.distance(pos) - this.radius);
	}

	public VPoint getCenter() {
		return this.center;
	}

	@Override
	public VPoint closestPoint(VPoint point) {
		Vector2D direction = new Vector2D(point.x - center.x, point.y
				- center.y);
		VPoint vector = direction.normalize(radius);
		return new VPoint(vector.x + center.x, vector.y + center.y);
	}

	/**
	 * Returns zero, one or two points which are the intersection of this circle and the line defined
	 * by p = (x11, y11) and q = (x22, y22).
	 *
	 * @param x11
	 * @param y11
	 * @param x22
	 * @param y22
	 * @return
	 */
	public ImmutableList<VPoint> getIntersectionPoints(final double x11, final double y11, final double x22, final double y22) {

		double x1 = x11 - center.x;
		double y1 = y11 - center.y;
		double x2 = x22 - center.x;
		double y2 = y22 - center.y;

		double dx = x2 - x1;
		double dy = y2 - y1;
		double dr = Math.sqrt(dx * dx + dy * dy);
		double disc = x1 * y2 - x2 * y1;
		double D = radius * radius * dr * dr - disc*disc;
		double sign = dy < 0 ? -1 : 1;

		if(D == 0) {
			x1 = (disc * dy + sign * dx * Math.sqrt(D)) / (dr * dr);
			y1 = (-disc * dx + Math.abs(dy) * Math.sqrt(D)) / (dr * dr);
			return ImmutableList.of(new VPoint(x1 + this.getCenter().x, y1 + this.getCenter().y));
		}
		else if(D < 0) {
			return ImmutableList.of();
		}
		else {
			x1 = (disc * dy + sign * dx * Math.sqrt(D)) / (dr * dr);
			y1 = (-disc * dx + Math.abs(dy) * Math.sqrt(D)) / (dr * dr);
			x2 = (disc * dy - sign * dx * Math.sqrt(D)) / (dr * dr);
			y2 = (-disc * dx - Math.abs(dy) * Math.sqrt(D)) / (dr * dr);

			return ImmutableList.of(new VPoint(x1 + this.getCenter().x, y1 + this.getCenter().y), new VPoint(x2 + this.getCenter().x, y2 + this.getCenter().y));
		}
	}

	/**
	 * Returns, the closest of all intersection points of the intersection of this circle and the line (p, q).
	 *
	 * @param p defining the line
	 * @param q defining the line
	 * @param r the point of measurement
	 * @return the closest of all intersection points of the intersection of this circle and the line (p, q). There might be no such point!
	 */
	public Optional<VPoint> getClosestIntersectionPoint(@NotNull final VPoint p, @NotNull final VPoint q, @NotNull final VPoint r) {
		ImmutableList<VPoint> intersectionPoints = getIntersectionPoints(p, q);
		return intersectionPoints.stream().min(Comparator.comparingDouble(point -> point.distance(r)));
	}

	/**
	 * Returns, the closest of all intersection points of the intersection of this circle and the line (p=(x1, y1), q = (x2, y2)) with respect to r = (x3, y3).
	 *
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param x3
	 * @param y3
	 * @return
	 */
	public Optional<VPoint> getClosestIntersectionPoint(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3) {
		ImmutableList<VPoint> intersectionPoints = getIntersectionPoints(x1, y1, x2, y2);
		final VPoint r = new VPoint(x3, y3);
		return intersectionPoints.stream().min(Comparator.comparingDouble(point -> point.distance(r)));
	}

	@Override
	public boolean intersectsLine(double x1, double y1, double x2, double y2) {
		return intersects(new VLine(x1, y1, x2, y2));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof VCircle))
			return false;

		VCircle other = (VCircle) obj;

		if (this.radius != other.radius)
			return false;
		if (!this.center.equals(other.center))
			return false;

		return true;
	}

	@Override
	public Rectangle getBounds() {
		int diameter = (int) Math.ceil(2 * radius);
		return new Rectangle((int) Math.floor(center.x - radius),
				(int) Math.floor(center.y - radius), diameter, diameter);
	}

	@Override
	public Rectangle2D getBounds2D() {
		return new Rectangle2D.Double(center.x - radius, center.y - radius,
				2 * radius, 2 * radius);
	}

	@Override
	public boolean contains(double x, double y) {
		return Math.sqrt(Math.pow(center.x - x, 2) + Math.pow(center.y - y, 2)) <= radius;
	}

	@Override
	public boolean contains(Point2D p) {
		return center.distance(p) <= radius;
	}

	@Override
	public boolean contains(VPoint p) {
		return p.distance(center) <= radius;
	}

	@Override
	public boolean intersects(double x, double y, double w, double h) {
		VRectangle rect = new VRectangle(x, y, w, h);

		if (rect.distance(center) <= radius || rect.contains(center)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean intersects(Rectangle2D r) {
		return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	@Override
	public boolean contains(double x, double y, double w, double h) {
		return (contains(x, y) && contains(x + w, y) && contains(x, y + h) && contains(
				x + w, y + h));
	}

	@Override
	public boolean contains(Rectangle2D r) {
		return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/**
	 * Dummy implementation using Ellipse2D.Double.
	 */
	@Override
	public PathIterator getPathIterator(AffineTransform at) {
		return new Ellipse2D.Double(center.x - radius, center.y - radius,
				radius * 2, radius * 2).getPathIterator(at);
	}

	/**
	 * Dummy implementation using Ellipse2D.Double.
	 */
	@Override
	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return new Ellipse2D.Double(center.x - radius, center.y - radius,
				radius * 2, radius * 2).getPathIterator(at, flatness);
	}

	@Override
	public VShape translate(final VPoint vector) {
		return new VCircle(getCenter().add(vector), getRadius());
	}

	@Override
	public VShape translatePrecise(final VPoint vector) {
		return new VCircle(getCenter().addPrecise(vector), getRadius());
	}

	@Override
	public VShape scale(final double scalar) {
		return new VCircle(getCenter().scalarMultiply(scalar), getRadius() * scalar);
	}

	@Override
	public boolean intersects(VLine intersectingLine) {
		if (intersectingLine.ptSegDist(this.getCenter()) <= this.getRadius())
			return true;
		return false;
	}

	@Override
	public VPoint getCentroid() {
		return center;
	}

	@Override
	public ShapeType getType() {
		return ShapeType.CIRCLE;
	}
}
