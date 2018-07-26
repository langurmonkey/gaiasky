package gaia.cu9.ari.gaiaorbit.data.orbit;

import java.time.Instant;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

public class PolylineData {
    // Values of x, y, z in world coordinates
    public Array<Double> x, y, z;
    public Array<Instant> time;

    private Vector3d v0, v1;

    public PolylineData() {
        this(16);
    }

    public PolylineData(int capacity) {
        x = new Array<Double>(16);
        y = new Array<Double>(16);
        z = new Array<Double>(16);
        time = new Array<Instant>(16);

        v0 = new Vector3d();
        v1 = new Vector3d();
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
     * @param v
     *            The vector
     * @param date
     *            The date
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
