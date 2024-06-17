/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import net.jafama.FastMath;

public class IntersectorDouble {

    private static final Vector3d auxd1 = new Vector3d();
    private static final Vector3d auxd2 = new Vector3d();
    private static final Vector3d auxd3 = new Vector3d();

    /**
     * Quick check whether the given {@link Ray} and {@link BoundingBoxDouble}
     * intersect.
     *
     * @param ray        The ray
     * @param center     The center of the bounding box
     * @param dimensions The dimensions (width, height and depth) of the bounding box
     *
     * @return Whether the ray and the bounding box intersect.
     */
    public static boolean intersectRayBoundsFast(RayDouble ray,
                                                 Vector3d center,
                                                 Vector3d dimensions) {
        final double divX = 1f / ray.direction.x;
        final double divY = 1f / ray.direction.y;
        final double divZ = 1f / ray.direction.z;

        double minx = ((center.x - dimensions.x * .5f) - ray.origin.x) * divX;
        double maxx = ((center.x + dimensions.x * .5f) - ray.origin.x) * divX;
        if (minx > maxx) {
            final double t = minx;
            minx = maxx;
            maxx = t;
        }

        double miny = ((center.y - dimensions.y * .5f) - ray.origin.y) * divY;
        double maxy = ((center.y + dimensions.y * .5f) - ray.origin.y) * divY;
        if (miny > maxy) {
            final double t = miny;
            miny = maxy;
            maxy = t;
        }

        double minz = ((center.z - dimensions.z * .5f) - ray.origin.z) * divZ;
        double maxz = ((center.z + dimensions.z * .5f) - ray.origin.z) * divZ;
        if (minz > maxz) {
            final double t = minz;
            minz = maxz;
            maxz = t;
        }

        double min = FastMath.max(Math.max(minx, miny), minz);
        double max = FastMath.min(Math.min(maxx, maxy), maxz);

        return max >= 0 && max >= min;
    }

    public static boolean checkIntersectRaySpehre(Vector3d linePoint0,
                                                  Vector3d linePoint1,
                                                  Vector3d sphereCenter,
                                                  double sphereRadius) {
        return checkIntersectRaySpehre(linePoint0.x, linePoint0.y, linePoint0.z, linePoint1.x, linePoint1.y, linePoint1.z, sphereCenter.x, sphereCenter.y, sphereCenter.z,
                                       sphereRadius);
    }

    public static boolean checkIntersectRaySpehre(Vector3 linePoint0,
                                                  Vector3 linePoint1,
                                                  Vector3d sphereCenter,
                                                  double sphereRadius) {
        return checkIntersectRaySpehre(linePoint0.x, linePoint0.y, linePoint0.z, linePoint1.x, linePoint1.y, linePoint1.z, sphereCenter.x, sphereCenter.y, sphereCenter.z,
                                       sphereRadius);
    }

    public static boolean checkIntersectRaySpehre(double px,
                                                  double py,
                                                  double pz,
                                                  double vx,
                                                  double vy,
                                                  double vz,
                                                  double cx,
                                                  double cy,
                                                  double cz,
                                                  double sphereRadius) {
        double A = vx * vx + vy * vy + vz * vz;
        double B = 2.0 * (px * vx + py * vy + pz * vz - vx * cx - vy * cy - vz * cz);
        double C = px * px - 2 * px * cx + cx * cx + py * py - 2 * py * cy + cy * cy + pz * pz - 2 * pz * cz + cz * cz - sphereRadius * sphereRadius;

        // discriminant
        double D = B * B - 4 * A * C;

        return D >= 0;
    }

    public synchronized static Array<Vector3d> intersectRaySphere(Vector3d linePoint0,
                                                                  Vector3d linePoint1,
                                                                  Vector3d sphereCenter,
                                                                  double sphereRadius) {
        // http://www.codeproject.com/Articles/19799/Simple-Ray-Tracing-in-C-Part-II-Triangles-Intersec

        double cx = sphereCenter.x;
        double cy = sphereCenter.y;
        double cz = sphereCenter.z;

        double px = linePoint0.x;
        double py = linePoint0.y;
        double pz = linePoint0.z;

        double vx = linePoint1.x - px;
        double vy = linePoint1.y - py;
        double vz = linePoint1.z - pz;

        double A = vx * vx + vy * vy + vz * vz;
        double B = 2.0 * (px * vx + py * vy + pz * vz - vx * cx - vy * cy - vz * cz);
        double C = px * px - 2 * px * cx + cx * cx + py * py - 2 * py * cy + cy * cy + pz * pz - 2 * pz * cz + cz * cz - sphereRadius * sphereRadius;

        // discriminant
        double D = B * B - 4 * A * C;

        Array<Vector3d> result = new Array<>(false, 2);

        if (D < 0) {
            return result;
        }

        double t1 = (-B - FastMath.sqrt(D)) / (2.0 * A);

        Vector3d solution1 = auxd1.set(linePoint0.x * (1 - t1) + t1 * linePoint1.x, linePoint0.y * (1 - t1) + t1 * linePoint1.y,
                                       linePoint0.z * (1 - t1) + t1 * linePoint1.z);
        if (D == 0) {
            result.add(solution1);
            return result;
        }

        double t2 = (-B + FastMath.sqrt(D)) / (2.0 * A);
        Vector3d solution2 = auxd2.set(linePoint0.x * (1 - t2) + t2 * linePoint1.x, linePoint0.y * (1 - t2) + t2 * linePoint1.y,
                                       linePoint0.z * (1 - t2) + t2 * linePoint1.z);

        // prefer a solution that's on the line segment itself

        if (Math.abs(t1 - 0.5) < FastMath.abs(t2 - 0.5)) {
            result.add(solution1);
            result.add(solution2);
            return result;
        }

        result.add(solution2);
        result.add(solution1);
        return result;
    }

    public static boolean checkIntersectSegmentSphere(Vector3d linePoint0,
                                                      Vector3d linePoint1,
                                                      Vector3d sphereCenter,
                                                      double sphereRadius) {
        Array<Vector3d> solutions = intersectRaySphere(linePoint0, linePoint1, sphereCenter, sphereRadius);
        // Test each point
        int n = solutions.size;
        for (int i = 0; i < n; i++) {
            if (isBetween(linePoint0, linePoint1, solutions.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if c is between a and b. Assumes c is co-linear with a and b.
     *
     * @param a Point A
     * @param b Point B
     * @param c Point to check
     */
    private static boolean isBetween(Vector3d a,
                                     Vector3d b,
                                     Vector3d c) {
        // -epsilon < (distance(a, c) + distance(c, b) - distance(a, b)) < epsilon
        double ab = a.dst(b);
        double ac = a.dst(c);
        double cb = c.dst(b);

        // ab * 1e-6 is our target precision
        double epsilon = ab * 1e-6 / 2;

        double value = ac + cb - ab;

        return -epsilon < value && value < epsilon;

    }

    /**
     * Returns the shortest distance between the line defined by x1 and x2 and
     * the point x0. See <a href=
     * "http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html">here</a>.
     *
     * @param x1 Segment first point
     * @param x2 Segment second point
     * @param x0 Point to test
     *
     * @return The minimum distance between the line and the point
     */
    public synchronized static double distanceLinePoint(Vector3d x1,
                                                        Vector3d x2,
                                                        Vector3d x0) {
        Vector3d aux1 = auxd2.set(x0).sub(x2);
        double nominator = auxd1.set(x0).sub(x1).crs(aux1).len();
        double denominator = aux1.set(x2).sub(x1).len();
        return nominator / denominator;
    }

    /**
     * Calculates the Euclidean distance from a point to a line segment.
     *
     * @param v the point
     * @param a start of line segment
     * @param b end of line segment
     *
     * @return distance from v to line segment [a,b]
     */
    public synchronized static double distanceSegmentPoint(final Vector3d a,
                                                           final Vector3d b,
                                                           final Vector3d v) {
        final Vector3d ab = auxd1.set(b).sub(a);
        double ablen = ab.len();
        final Vector3d av = auxd2.set(v).sub(a);
        double avlen = av.len();

        if (av.dot(ab) <= 0.0) // Point is lagging behind start of the segment, so perpendicular distance is not viable.
            return avlen; // Use distance to start of segment instead.

        final Vector3d bv = auxd3.set(v).sub(b);
        double bvlen = bv.len();

        if (bv.dot(ab) >= 0.0) // Point is advanced past the end of the segment, so perpendicular distance is not viable.
            return bvlen; // Use distance to end of the segment instead.

        return (ab.crs(av)).len() / ablen; // Perpendicular distance of point to segment.
    }

    /**
     * Determines the point of intersection between a plane defined by a point and a normal vector and a line defined by a point and a direction vector.
     *
     * @param planePoint    A point on the plane.
     * @param planeNormal   The normal vector of the plane.
     * @param linePoint     A point on the line.
     * @param lineDirection The direction vector of the line.
     */
    public static void lineIntersection(Vector3d planePoint,
                                        Vector3d planeNormal,
                                        Vector3d linePoint,
                                        Vector3d lineDirection,
                                        Vector3d out) {
        if (planeNormal.dot(lineDirection.nor()) == 0) {
            return;
        }

        double t = (planeNormal.dot(planePoint) - planeNormal.dot(linePoint)) / planeNormal.dot(lineDirection.nor());
        out.set(linePoint).add(lineDirection.nor().scl(t));
    }

    /**
     * Determine the signed distance of the given point to the plane determined by the given plane normal and point.
     *
     * @param point       The point to test.
     * @param planeNormal The normal vector of the plane.
     * @param planePoint  A point in the plane.
     *
     * @return the distance between the point and the plane.
     */
    public static double distancePointPlane(Vector3d point,
                                            Vector3d planeNormal,
                                            Vector3d planePoint) {
        double a = planeNormal.x;
        double b = planeNormal.y;
        double c = planeNormal.z;
        double d = -a * planePoint.x - b * planePoint.y - c * planePoint.z;
        return distancePointPlane(point.x, point.y, point.z, a, b, c, d);
    }

    /**
     * Determine the signed distance of the given point <code>(pointX, pointY, pointZ)</code> to the plane specified via its general plane equation
     * <i>a*x + b*y + c*z + d = 0</i>. From JOML (MIT license).
     *
     * @param pointX the x coordinate of the point
     * @param pointY the y coordinate of the point
     * @param pointZ the z coordinate of the point
     * @param a      the x factor in the plane equation
     * @param b      the y factor in the plane equation
     * @param c      the z factor in the plane equation
     * @param d      the constant in the plane equation
     *
     * @return the distance between the point and the plane
     */
    public static double distancePointPlane(double pointX,
                                            double pointY,
                                            double pointZ,
                                            double a,
                                            double b,
                                            double c,
                                            double d) {
        double denom = FastMath.sqrt(a * a + b * b + c * c);
        return (a * pointX + b * pointY + c * pointZ + d) / denom;
    }

    /**
     * Determine whether the line segment with the end points <code>(p0X, p0Y, p0Z)</code> and <code>(p1X, p1Y, p1Z)</code>
     * intersects the plane given as the normal and point,
     * and return the point of intersection.
     *
     * @param p0                the line segment's first end point.
     * @param p1                the line segment's second end point.
     * @param planeNormal       the plane normal.
     * @param planePoint        the plane point.
     * @param intersectionPoint the point of intersection
     *
     * @return <code>true</code> if the given line segment intersects the plane; <code>false</code> otherwise
     */
    public static boolean intersectLineSegmentPlane(Vector3d p0,
                                                    Vector3d p1,
                                                    Vector3d planeNormal,
                                                    Vector3d planePoint,
                                                    Vector3d intersectionPoint) {
        double a = planeNormal.x;
        double b = planeNormal.y;
        double c = planeNormal.z;
        double d = -a * planePoint.x - b * planePoint.y - c * planePoint.z;
        return intersectLineSegmentPlane(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, a, b, c, d, intersectionPoint);
    }

    /**
     * Determine whether the line segment with the end points <code>(p0X, p0Y, p0Z)</code> and <code>(p1X, p1Y, p1Z)</code>
     * intersects the plane given as the general plane equation <i>a*x + b*y + c*z + d = 0</i>,
     * and return the point of intersection. This code is from JOML (MIT license).
     *
     * @param p0X               the x coordinate of the line segment's first end point
     * @param p0Y               the y coordinate of the line segment's first end point
     * @param p0Z               the z coordinate of the line segment's first end point
     * @param p1X               the x coordinate of the line segment's second end point
     * @param p1Y               the y coordinate of the line segment's second end point
     * @param p1Z               the z coordinate of the line segment's second end point
     * @param a                 the x factor in the plane equation
     * @param b                 the y factor in the plane equation
     * @param c                 the z factor in the plane equation
     * @param d                 the constant in the plane equation
     * @param intersectionPoint the point of intersection
     *
     * @return <code>true</code> if the given line segment intersects the plane; <code>false</code> otherwise
     */
    public static boolean intersectLineSegmentPlane(double p0X,
                                                    double p0Y,
                                                    double p0Z,
                                                    double p1X,
                                                    double p1Y,
                                                    double p1Z,
                                                    double a,
                                                    double b,
                                                    double c,
                                                    double d,
                                                    Vector3d intersectionPoint) {
        double dirX = p1X - p0X;
        double dirY = p1Y - p0Y;
        double dirZ = p1Z - p0Z;
        double denominator = a * dirX + b * dirY + c * dirZ;
        double t = -(a * p0X + b * p0Y + c * p0Z + d) / denominator;
        if (t >= 0.0 && t <= 1.0) {
            intersectionPoint.x = p0X + t * dirX;
            intersectionPoint.y = p0Y + t * dirY;
            intersectionPoint.z = p0Z + t * dirZ;
            return true;
        }
        return false;
    }

}
