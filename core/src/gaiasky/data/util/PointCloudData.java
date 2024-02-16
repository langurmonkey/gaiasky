/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.util;

import com.badlogic.gdx.math.Vector3;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PointCloudData {
    private final Vector3d v0;
    private final Vector3d v1;
    // Values of x, y, z in world coordinates
    public List<Double> x, y, z;
    public List<Instant> time;
    // Period in days
    public double period = -1;
    private Instant start, end;

    public PointCloudData() {
        this(16);
    }

    public PointCloudData(int capacity) {
        x = new ArrayList<>(capacity);
        y = new ArrayList<>(capacity);
        z = new ArrayList<>(capacity);
        time = new ArrayList<>(capacity);

        v0 = new Vector3d();
        v1 = new Vector3d();
    }

    /**
     * Clears all data
     **/
    public void clear() {
        x.clear();
        y.clear();
        z.clear();
        time.clear();
        v0.setZero();
        v1.setZero();
    }

    public boolean isEmpty() {
        return x.isEmpty() && y.isEmpty() && z.isEmpty();
    }

    public boolean hasTime() {
        return time != null && !time.isEmpty();
    }

    /**
     * Adds the given vector to the current points. The vector
     * is of the form [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn].
     * If the size of the vector is not a multiple of 3, no points are added
     *
     * @param points The points to add to this point cloud
     */
    public void addPoints(double[] points) {
        if (points.length % 3 == 0) {
            int nPoints = points.length / 3;
            for (int i = 0; i < nPoints; i++) {
                x.add(points[i * 3]);
                y.add(points[i * 3 + 1]);
                z.add(points[i * 3 + 2]);
            }
        }
    }

    /**
     * Same as {@link PointCloudData#addPoints(double[])} but with an array list
     *
     * @param points The points to add to this point cloud
     */
    public void addPoints(ArrayList points) {
        if (points.size() % 3 == 0) {
            int nPoints = points.size() / 3;
            for (int i = 0; i < nPoints; i++) {
                x.add((double) points.get(i * 3));
                y.add((double) points.get(i * 3 + 1));
                z.add((double) points.get(i * 3 + 2));
            }
        }
    }

    /**
     * Adds a single point o this point cloud
     *
     * @param point The point
     */
    public void addPoint(Vector3d point) {
        x.add(point.x);
        y.add(point.y);
        z.add(point.z);
    }

    /**
     * Adds a single point to the cloud
     *
     * @param x The x component
     * @param y The y component
     * @param z The z component
     */
    public void addPoint(double x, double y, double z) {
        this.x.add(x);
        this.y.add(y);
        this.z.add(z);
    }

    /**
     * Loads the data point at the index in the vector in the Orbit reference
     * system.
     *
     * @param v     The vector to load the data into.
     * @param index The data index.
     */
    public void loadPoint(Vector3d v, int index) {
        v.set(x.get(index), y.get(index), z.get(index));
    }

    public void loadPoint(Vector3b v, int index) {
        v.set(x.get(index), y.get(index), z.get(index));
    }

    public Instant loadTime(int index) {
        return time.get(index);
    }

    public int getNumPoints() {
        return x.size();
    }

    public double getX(int index) {
        return x.get(index);
    }

    public void setX(int index, double value) {
        x.set(index, value);
    }

    public double getY(int index) {
        return y.get(index);
    }

    public void setY(int index, double value) {
        y.set(index, value);
    }

    public double getZ(int index) {
        return z.get(index);
    }

    public void setZ(int index, double value) {
        z.set(index, value);
    }

    public void setPoint(Vector3d v, int index) {
        x.set(index, v.x);
        y.set(index, v.y);
        z.set(index, v.z);
    }

    public void setPoint(Vector3b v, int index) {
        x.set(index, v.x.doubleValue());
        y.set(index, v.y.doubleValue());
        z.set(index, v.z.doubleValue());
    }

    public Instant getDate(int index) {
        return time.get(index);
    }

    public Instant getStart() {
        if (start == null) {
            start = time.get(0);
        }
        return start;
    }

    public long getStartMs() {
        return getStart().toEpochMilli();
    }

    public Instant getEnd() {
        if (end == null) {
            end = time.get(time.size() - 1);
        }
        return end;
    }

    public long getEndMs() {
        return getEnd().toEpochMilli();
    }

    /**
     * Loads the data point at the index in the vector in the world reference
     * system.
     *
     * @param v     The vector to store the result.
     * @param index The index of the point to load.
     */
    public void loadPointF(Vector3 v, int index) {
        v.set(x.get(index).floatValue(), y.get(index).floatValue(), z.get(index).floatValue());
    }

    /**
     * Returns a vector with the data point at the given time. It uses linear
     * interpolation. The time instant must be within the bounds of this point cloud's times
     *
     * @param v       The vector to store the result.
     * @param instant The time as an instant.
     * @return Whether the operation completes successfully
     */
    public boolean loadPoint(Vector3d v, Instant instant) {
        return loadPoint(v, instant.toEpochMilli());
    }

    public boolean loadPoint(Vector3b v, Instant instant) {
        return loadPoint(v, instant.toEpochMilli());
    }

    /**
     * Returns a vector with the data point at the given time. It uses linear
     * interpolation. The time instant must be within the bounds of this point cloud's times
     *
     * @param v      The vector
     * @param timeMs The time in milliseconds
     * @return Whether the operation completes successfully
     */
    public boolean loadPoint(Vector3d v, long timeMs) {
        // Data is sorted
        int idx = binarySearch(time, timeMs);

        if (idx < 0 || idx >= time.size()) {
            // No data for this time
            return false;
        }

        if (time.get(idx).toEpochMilli() == timeMs) {
            v.set(x.get(idx), y.get(idx), z.get(idx));
        } else {
            // Interpolate
            loadPoint(v0, idx);
            loadPoint(v1, idx + 1);
            Instant t0 = time.get(idx);
            Instant t1 = time.get(idx + 1);

            double scl = (double) (timeMs - t0.toEpochMilli()) / (t1.toEpochMilli() - t0.toEpochMilli());
            v.set(v1.sub(v0).scl(scl).add(v0));
        }
        return true;
    }

    public boolean loadPoint(Vector3b v, long timeMs) {
        // Data is sorted
        int idx = binarySearch(time, timeMs);

        if (idx < 0 || idx >= time.size()) {
            // No data for this time
            return false;
        }

        if (time.get(idx).toEpochMilli() == timeMs) {
            v.set(x.get(idx), y.get(idx), z.get(idx));
        } else {
            // Interpolate
            loadPoint(v0, idx);
            loadPoint(v1, idx + 1);
            Instant t0 = time.get(idx);
            Instant t1 = time.get(idx + 1);

            double scl = (double) (timeMs - t0.toEpochMilli()) / (t1.toEpochMilli() - t0.toEpochMilli());
            v.set(v1.sub(v0).scl(scl).add(v0));
        }
        return true;

    }

    public Instant getWrapTime(Instant instant) {
        return Instant.ofEpochMilli(getWrapTimeMs(instant));
    }

    public long getWrapTimeMs(Instant instant) {
        long c = instant.toEpochMilli();
        long s = getStartMs();
        long e = getEndMs();

        long ep = e - s;
        long cp = c - s;
        long wrapCurrentTime = ep > 0 ? ((cp % ep) + ep) % ep : 0;
        return wrapCurrentTime + s;
    }

    /**
     * Gets the bounding indices for the given date. If the date is out of range, it wraps.
     *
     * @param instant The date
     * @return The two indices
     */
    public int getIndex(Instant instant) {
        return binarySearch(time, getWrapTimeMs(instant));
    }

    public int getIndex(long wrappedTimeMs) {
        return binarySearch(time, wrappedTimeMs);
    }

    private int binarySearch(List<Instant> times, Instant elem) {
        return binarySearch(times, elem.toEpochMilli());
    }

    private int binarySearch(List<Instant> times, long time) {
        if (time >= times.get(0).toEpochMilli() && time <= times.get(times.size() - 1).toEpochMilli()) {
            return binarySearch(times, time, 0, times.size() - 1);
        } else {
            return -1;
        }
    }

    private int binarySearch(List<Instant> times, long time, int i0, int i1) {
        if (i0 > i1) {
            return -1;
        } else if (i0 == i1) {
            if (times.get(i0).toEpochMilli() > time) {
                return i0 - 1;
            } else {
                return i0;
            }
        }

        int mid = (i0 + i1) / 2;
        if (times.get(mid).toEpochMilli() == time) {
            return mid;
        } else if (times.get(mid).toEpochMilli() < time) {
            return binarySearch(times, time, mid + 1, i1);
        } else {
            return binarySearch(times, time, i0, mid);
        }
    }

}
