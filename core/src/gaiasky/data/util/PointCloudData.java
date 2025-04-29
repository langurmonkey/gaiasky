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
import net.jafama.FastMath;

import java.time.Instant;
import java.util.ArrayList;

public class PointCloudData implements Cloneable {
    private final Vector3d v0;
    private final Vector3d v1;

    /**
     * A sample in the point cloud.
     *
     * @param x       The X component.
     * @param y       The Y component.
     * @param z       The Z component.
     * @param seconds The amount of seconds since {@link Instant#EPOCH}. If this is equal to {@link Long#MIN_VALUE}, time is ignored.
     * @param nanos   The nanoseconds in the second.
     */
    public record PointSample(double x, double y, double z, long seconds, int nanos) {

        public PointSample(double x, double y, double z) {
            this(x, y, z, Long.MIN_VALUE, 0);
        }

        public PointSample(Vector3d v) {
            this(v.x, v.y, v.z, Long.MIN_VALUE, 0);
        }

        public PointSample(double x, double y, double z, Instant t) {
            this(x, y, z, t.getEpochSecond(), t.getNano());
        }

        public PointSample(Vector3d v, Instant t) {
            this(v.x, v.y, v.z, t.getEpochSecond(), t.getNano());
        }

        public void put(Vector3 v) {
            v.set((float) x, (float) y, (float) z);
        }

        public void put(Vector3d v) {
            v.set(x, y, z);
        }

        public void put(Vector3b v) {
            v.set(x, y, z);
        }

        public long toEpochMilli() {
            if (this.seconds < 0L && this.nanos > 0) {
                long millis = FastMath.multiplyExact(this.seconds + 1L, 1000);
                long adjustment = this.nanos / 1000000 - 1000;
                return FastMath.addExact(millis, adjustment);
            } else {
                long millis = FastMath.multiplyExact(this.seconds, 1000);
                return FastMath.addExact(millis, this.nanos / 1000000);
            }
        }

        public Instant toInstant() {
            return Instant.ofEpochSecond(this.seconds, this.nanos);
        }
    }

    /** The samples of this point cloud. **/
    public ArrayList<PointSample> samples;
    /** Period in days. **/
    public double period = -1;
    private Instant start, end;


    public PointCloudData() {
        this(16);
    }

    public PointCloudData(int capacity) {
        samples = new ArrayList<>(capacity);

        v0 = new Vector3d();
        v1 = new Vector3d();
    }

    /**
     * Clears all data
     **/
    public void clear() {
        samples.clear();
        v0.setZero();
        v1.setZero();
    }

    public boolean isEmpty() {
        return samples.isEmpty();
    }

    public boolean hasTime() {
        return samples != null && !samples.isEmpty() && samples.get(0).seconds > Long.MIN_VALUE;
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
                var sample = new PointSample(points[i * 3], points[i * 3 + 1], points[i * 3 + 2], Long.MIN_VALUE, 0);
                samples.add(sample);
            }
        }
    }

    public void addPoint(Vector3d point, Instant t) {
        samples.add(new PointSample(point, t));
    }

    public void addPoint(Vector3d point) {
        samples.add(new PointSample(point));
    }

    public void addPoint(double x, double y, double z, long seconds, int nano) {
        samples.add(new PointSample(x, y, z, seconds, nano));
    }

    public void addPoint(double x, double y, double z, Instant t) {
        samples.add(new PointSample(x, y, z, t.getEpochSecond(), t.getNano()));
    }

    public void addPoint(double x, double y, double z) {
        samples.add(new PointSample(x, y, z));
    }

    /**
     * Loads the data point at the index in the vector in the Orbit reference
     * system.
     *
     * @param v     The vector to load the data into.
     * @param index The data index.
     */
    public void loadPoint(Vector3d v, int index) {
        samples.get(index)
                .put(v);
    }

    public void loadPoint(Vector3b v, int index) {
        samples.get(index)
                .put(v);
    }

    public int getNumPoints() {
        return samples.size();
    }

    public double getX(int index) {
        return samples.get(index).x;
    }

    public double getY(int index) {
        return samples.get(index).y;
    }

    public double getZ(int index) {
        return samples.get(index).z;
    }

    public void setPoint(Vector3d v, int index) {
        samples.set(index, new PointSample(v));
    }

    public Instant getDate(int index) {
        return samples.get(index)
                .toInstant();
    }

    public Instant getStart() {
        if (start == null) {
            start = samples.get(0)
                    .toInstant();
        }
        return start;
    }

    public long getStartMs() {
        return getStart().toEpochMilli();
    }

    public Instant getEnd() {
        if (end == null) {
            end = samples.get(samples.size() - 1)
                    .toInstant();
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
        samples.get(index)
                .put(v);
    }

    /**
     * Returns a vector with the data point at the given time. It uses linear
     * interpolation. The time instant must be within the bounds of this point cloud's times
     *
     * @param v       The vector to store the result.
     * @param instant The time as an instant.
     *
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
     *
     * @return Whether the operation completes successfully
     */
    public boolean loadPoint(Vector3d v, long timeMs) {
        // Data is sorted
        int idx = binarySearch(samples, timeMs);

        if (idx < 0 || idx >= samples.size()) {
            // No data for this time
            return false;
        }


        if (samples.get(idx)
                .toEpochMilli() == timeMs) {
            samples.get(idx)
                    .put(v);
        } else {
            // Interpolate
            loadPoint(v0, idx);
            loadPoint(v1, idx + 1);
            long t0 = samples.get(idx)
                    .toEpochMilli();
            long t1 = samples.get(idx + 1)
                    .toEpochMilli();

            double scl = (double) (timeMs - t0) / (t1 - t0);
            v.set(v1.sub(v0)
                          .scl(scl)
                          .add(v0));
        }
        return true;
    }

    public boolean loadPoint(Vector3b v, long timeMs) {
        // Data is sorted
        int idx = binarySearch(samples, timeMs);

        if (idx < 0 || idx >= samples.size()) {
            // No data for this time
            return false;
        }

        if (samples.get(idx)
                .toEpochMilli() == timeMs) {
            samples.get(idx)
                    .put(v);
        } else {
            // Interpolate
            loadPoint(v0, idx);
            loadPoint(v1, idx + 1);
            long t0 = samples.get(idx)
                    .toEpochMilli();
            long t1 = samples.get(idx + 1)
                    .toEpochMilli();

            double scl = (double) (timeMs - t0) / (t1 - t0);
            v.set(v1.sub(v0)
                          .scl(scl)
                          .add(v0));
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
     *
     * @return The two indices
     */
    public int getIndex(Instant instant) {
        return binarySearch(samples, getWrapTimeMs(instant));
    }

    public int getIndex(long wrappedTimeMs) {
        return binarySearch(samples, wrappedTimeMs);
    }

    private int binarySearch(ArrayList<PointSample> samples, Instant elem) {
        return binarySearch(samples, elem.toEpochMilli());
    }

    private int binarySearch(ArrayList<PointSample> samples, long time) {
        if (time >= samples.get(0)
                .toEpochMilli() && time <= samples.get(samples.size() - 1)
                .toEpochMilli()) {
            return binarySearch(samples, time, 0, samples.size() - 1);
        } else {
            return -1;
        }
    }

    private int binarySearch(ArrayList<PointSample> samples, long time, int i0, int i1) {
        if (i0 > i1) {
            return -1;
        } else if (i0 == i1) {
            if (samples.get(i0)
                    .toEpochMilli() > time) {
                return i0 - 1;
            } else {
                return i0;
            }
        }

        int mid = (i0 + i1) / 2;
        if (samples.get(mid)
                .toEpochMilli() == time) {
            return mid;
        } else if (samples.get(mid)
                .toEpochMilli() < time) {
            return binarySearch(samples, time, mid + 1, i1);
        } else {
            return binarySearch(samples, time, i0, mid);
        }
    }

    @Override
    public PointCloudData clone() {
        try {
            PointCloudData clone = (PointCloudData) super.clone();
            clone.samples = new ArrayList<>(this.samples);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
