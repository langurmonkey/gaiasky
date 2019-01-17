package gaia.cu9.ari.gaiaorbit.data.util;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.time.Instant;

public class PointCloudData {
    // Values of x, y, z in world coordinates
    public Array<Double> x, y, z;
    public Array<Instant> time;

    private Vector3d v0, v1;

    public PointCloudData() {
        this(16);
    }

    public PointCloudData(int capacity) {
        x = new Array<>(capacity);
        y = new Array<>(capacity);
        z = new Array<>(capacity);
        time = new Array<>(capacity);

        v0 = new Vector3d();
        v1 = new Vector3d();
    }

    /** Clears all data **/
    public void clear() {
        x.clear();
        y.clear();
        z.clear();
        time.clear();
        v0.setZero();
        v1.setZero();
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
     * Loads the data point at the index in the vector in the Orbit reference
     * system
     *
     * @param v
     * @param index
     */
    public void loadPoint(Vector3d v, int index) {
        v.set(x.get(index), y.get(index), z.get(index));
    }

    public int getNumPoints() {
        return x.size;
    }

    public double getX(int index) {
        return x.get(index);
    }

    public double getY(int index) {
        return y.get(index);
    }

    public double getZ(int index) {
        return z.get(index);
    }

    public Instant getDate(int index) {
        return time.get(index);
    }

    /**
     * Loads the data point at the index in the vector in the world reference
     * system
     *
     * @param v
     * @param index
     */
    public void loadPointF(Vector3 v, int index) {
        v.set(x.get(index).floatValue(), y.get(index).floatValue(), z.get(index).floatValue());
    }

    /**
     * Returns a vector with the data point at the given time. It uses linear
     * interpolation
     *
     * @param v       The vector
     * @param instant The date
     * @return Whether the operation completes successfully
     */
    public boolean loadPoint(Vector3d v, Instant instant) {
        // Data is sorted
        int idx = binarySearch(time, instant);

        if (idx < 0 || idx >= time.size) {
            // No data for this time
            return false;
        }

        if (time.get(idx).equals(instant)) {
            v.set(x.get(idx), y.get(idx), z.get(idx));
        } else {
            // Interpolate
            loadPoint(v0, idx);
            loadPoint(v1, idx + 1);
            Instant t0 = time.get(idx);
            Instant t1 = time.get(idx + 1);

            double scl = (double) (instant.toEpochMilli() - t0.toEpochMilli()) / (t1.toEpochMilli() - t0.toEpochMilli());
            v.set(v1.sub(v0).scl(scl).add(v0));
        }
        return true;
    }

    private int binarySearch(Array<Instant> times, Instant elem) {
        long time = elem.toEpochMilli();
        if (time >= times.get(0).toEpochMilli() && time <= times.get(times.size - 1).toEpochMilli()) {
            return binarySearch(times, time, 0, times.size - 1);
        } else {
            return -1;
        }
    }

    private int binarySearch(Array<Instant> times, long time, int i0, int i1) {
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
