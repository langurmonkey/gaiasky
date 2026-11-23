/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import gaiasky.scene.record.OrbitComponent;
import gaiasky.scene.record.ParticleKepler;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;

import java.util.stream.IntStream;

/**
 * Computes the positions of an orbital elements group in double precision and writes them into the
 * float buffer the vertex shader reads, advancing the eccentric anomaly from the previous frame.
 */
public class ElementPositions {

    /** Doubles per element in the constants array. **/
    private static final int STRIDE = 11;
    /** Above this number of elements, the work is spread over the common fork-join pool. **/
    private static final int PARALLEL_THRESHOLD = 4096;
    /** Elements per parallel task. **/
    private static final int CHUNK_SIZE = 512;
    /** Frame interval between two full solutions of Kepler's equation. **/
    private static final int RESYNC_INTERVAL = 64;
    /** Mean anomaly change above which the element is solved from scratch, in radians. **/
    private static final double MAX_STEP = 0.05;
    /** Tolerance of the Newton iteration, in radians. **/
    private static final double TOLERANCE = 1e-12;
    /** Residual above which the incremental update is discarded in favour of a full solve. **/
    private static final double MAX_RESIDUAL = 1e-10;
    private static final double PI2 = Math.PI * 2.0;
    private static final double INV_24 = 1d / 24d, INV_6 = 1d / 6d, INV_120 = 1d / 120d;

    /** Orbital elements, converted to radians and folded with the perifocal rotation. **/
    private double[] constants;
    /** Mean anomaly and eccentric anomaly of each element on the previous frame. **/
    private double[] meanAnomaly, eccAnomaly;
    /** Cosine and sine of the eccentric anomaly on the previous frame. **/
    private double[] cosE, sinE;
    /** Dataset-frame positions, three doubles per element. **/
    private double[] orbitPos;
    /** Number of elements. **/
    private int count;
    /** Time of the previous computation, in julian days. **/
    private double lastTjd = Double.NaN;
    /** Frames since the last full solution. **/
    private int frame;
    /** Combined reference-system and affine transform, or null if it is the identity, see {@link #setTransform(Matrix4D, Matrix4D)}. **/
    private double[] mtx;
    /** Dataset origin, camera-relative. **/
    private double dx, dy, dz;

    /**
     * Prepares the given elements for fast evaluation. Must be called before
     * {@link #compute(double, float[], int, int, int)}.
     */
    public void compile(Object[] src,
                        int count) {
        if (constants == null || constants.length < count * STRIDE) {
            constants = new double[count * STRIDE];
            meanAnomaly = new double[count];
            eccAnomaly = new double[count];
            cosE = new double[count];
            sinE = new double[count];
            orbitPos = new double[count * 3];
        }
        this.count = count;
        this.lastTjd = Double.NaN;
        this.frame = 0;

        if (count >= PARALLEL_THRESHOLD) {
            int chunks = (count + CHUNK_SIZE - 1) / CHUNK_SIZE;
            IntStream.range(0, chunks).parallel().forEach(chunk -> {
                int from = chunk * CHUNK_SIZE;
                compileRange(src, from, Math.min(count, from + CHUNK_SIZE));
            });
        } else {
            compileRange(src, 0, count);
        }
    }

    /**
     * Computes the position of every element at the given time, in internal units, and writes the
     * camera-relative float position into the vertex (or instance) buffer in the same pass.
     */
    public void compute(double tjd,
                        float[] buf,
                        int stride,
                        int offset,
                        int repeat,
                        Matrix4D refSys,
                        Vector3D dataPos,
                        Matrix4D affine) {
        if (count == 0 || constants == null) {
            return;
        }

        // Time going backwards or a first call: no previous state to advance from.
        final boolean resync = Double.isNaN(lastTjd) || (frame++ % RESYNC_INTERVAL) == 0;
        final double dt = resync ? 0d : (tjd - lastTjd);
        lastTjd = tjd;

        setTransform(refSys, affine);
        dx = dataPos != null ? dataPos.x : 0d;
        dy = dataPos != null ? dataPos.y : 0d;
        dz = dataPos != null ? dataPos.z : 0d;

        final boolean full = resync || dt < 0d;
        if (count >= PARALLEL_THRESHOLD) {
            int chunks = (count + CHUNK_SIZE - 1) / CHUNK_SIZE;
            IntStream.range(0, chunks).parallel().forEach(chunk -> {
                int from = chunk * CHUNK_SIZE;
                computeRange(from, Math.min(count, from + CHUNK_SIZE), tjd, dt, full, buf, stride, offset, repeat);
            });
        } else {
            computeRange(0, count, tjd, dt, full, buf, stride, offset, repeat);
        }
    }

    /**
     * Rewrites the float positions from the dataset-frame positions found by the last call to
     * {@link #compute(double, float[], int, int, int)}. Used when only the transforms have changed.
     */
    public void project(float[] buf,
                        int stride,
                        int offset,
                        int repeat,
                        Matrix4D refSys,
                        Vector3D dataPos,
                        Matrix4D affine) {
        if (count == 0 || orbitPos == null) {
            return;
        }
        setTransform(refSys, affine);
        dx = dataPos != null ? dataPos.x : 0d;
        dy = dataPos != null ? dataPos.y : 0d;
        dz = dataPos != null ? dataPos.z : 0d;
        projectRange(0, count, buf, stride, offset, repeat);
    }

    /**
     * Builds the combined transform as the elements vertex shader applies it (the ref sys as a row
     * vector, the affine as a column one), folded into a single 3x4 array.
     */
    private void setTransform(Matrix4D refSys,
                              Matrix4D affine) {
        if (refSys == null && affine == null) {
            mtx = null;
            return;
        }
        if (mtx == null) {
            mtx = new double[12];
        }
        if (affine == null) {
            // A row-vector multiply reads the rows of the transposed matrix, which is the layout of
            // the values, so no rearrangement is needed and the translation is not applied.
            System.arraycopy(refSys.val, 0, mtx, 0, 12);
            return;
        }
        final double[] a = affine.val;
        final double[] r = refSys != null ? refSys.val : null;
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                if (r == null) {
                    // Identity reference system: the affine transformation alone.
                    mtx[i * 4 + k] = a[k * 4 + i];
                } else {
                    double v = 0d;
                    for (int j = 0; j < 3; j++) {
                        v += a[j * 4 + i] * r[j * 4 + k];
                    }
                    mtx[i * 4 + k] = v;
                }
            }
            // The reference system contributes no offset, so this is the affine's.
            mtx[i * 4 + 3] = a[12 + i];
        }
    }

    private void compileRange(Object[] src,
                              int from,
                              int to) {
        final double kmToU = Constants.KM_TO_U;
        for (int i = from; i < to; i++) {
            int o = i * STRIDE;
            double period, epoch, meanAnomalyDeg, semiMajorAxis, e, ascendingNode, argOfPericenter, inclination;
            var obj = src[i];
            if (obj instanceof ParticleKepler k) {
                period = k.period();
                epoch = k.epoch();
                meanAnomalyDeg = k.meanAnomaly();
                semiMajorAxis = k.semiMajorAxis();
                e = k.eccentricity();
                ascendingNode = k.ascendingNode();
                argOfPericenter = k.argOfPericenter();
                inclination = k.inclination();
            } else if (obj instanceof OrbitComponent oc) {
                period = oc.period;
                if (period <= 0 && oc.mu > 0 && oc.semiMajorAxis > 0) {
                    // No period given: derive it from the standard gravitational parameter, which
                    // is the same information the shader used to compute the mean motion from.
                    double a = oc.semiMajorAxis;
                    period = 2.0 * Math.PI * Math.sqrt(a * a * a / oc.mu) / Nature.D_TO_S;
                }
                epoch = oc.epoch;
                meanAnomalyDeg = oc.meanAnomaly;
                semiMajorAxis = oc.semiMajorAxis;
                e = oc.e;
                ascendingNode = oc.ascendingNode;
                argOfPericenter = oc.argOfPericenter;
                inclination = oc.i;
            } else {
                // Not an orbit: pin it to the origin.
                zeroConstants(o);
                continue;
            }

            if (period <= 0 || semiMajorAxis <= 0) {
                // No usable orbit: pin it to the origin.
                zeroConstants(o);
                continue;
            }

            double inc = FastMath.toRadians(inclination);
            double ascNode = FastMath.toRadians(ascendingNode);
            double argP = FastMath.toRadians(argOfPericenter);
            double cosO = FastMath.cos(ascNode), sinO = FastMath.sin(ascNode);
            double cosI = FastMath.cos(inc), sinI = FastMath.sin(inc);
            double cosW = FastMath.cos(argP), sinW = FastMath.sin(argP);

            double n = PI2 / period;
            double a = semiMajorAxis * kmToU;

            // Mean anomaly is M0 + n * (tjd - epoch), evaluated as (M0 - n * epoch) + n * tjd.
            constants[o] = n;
            constants[o + 1] = FastMath.toRadians(meanAnomalyDeg) - n * epoch;
            constants[o + 2] = e;
            constants[o + 3] = a;
            constants[o + 4] = a * Math.sqrt((1d - e) * (1d + e));
            // Perifocal to equatorial rotation, already permuted to Gaia Sky axes (y, z, x).
            constants[o + 5] = sinO * cosW + cosO * sinW * cosI;
            constants[o + 6] = -sinO * sinW + cosO * cosW * cosI;
            constants[o + 7] = sinW * sinI;
            constants[o + 8] = cosW * sinI;
            constants[o + 9] = cosO * cosW - sinO * sinW * cosI;
            constants[o + 10] = -cosO * sinW - sinO * cosW * cosI;

            cosE[i] = 0d;
            sinE[i] = 0d;
            eccAnomaly[i] = 0d;
            meanAnomaly[i] = 0d;
        }
    }

    private void zeroConstants(int o) {
        for (int k = 0; k < STRIDE; k++) {
            constants[o + k] = 0d;
        }
    }

    /**
     * Starting point for a cold solve of Kepler's equation: the mean anomaly for low eccentricity,
     * near the apoapsis for high.
     */
    private static double initialGuess(double m,
                                       double e) {
        return e < 0.8d ? m : Math.PI;
    }

    /**
     * Solves Kepler's equation, M = E - e sin E, starting from the given seed.
     */
    private static double solveKepler(double m,
                                      double e,
                                      double seed) {
        double ecc = seed;
        for (int it = 0; it < 24; it++) {
            double dE = -(ecc - e * FastMath.sin(ecc) - m) / (1d - e * FastMath.cos(ecc));
            ecc += dE;
            if (FastMath.abs(dE) < TOLERANCE) {
                break;
            }
        }
        return ecc;
    }

    private void computeRange(int from,
                              int to,
                              double tjd,
                              double dt,
                              boolean fullSolve,
                              float[] buf,
                              int stride,
                              int offset,
                              int repeat) {
        final double[] c = constants;
        final double[] mtx = this.mtx;
        final double dx = this.dx, dy = this.dy, dz = this.dz;
        final boolean single = repeat == 1;

        for (int i = from; i < to; i++) {
            int o = i * STRIDE;
            final double n = c[o];
            final double e = c[o + 2];
            if (n == 0d && e == 0d) {
                // Element without a usable orbit.
                writeZero(i, buf, stride, offset, repeat);
                continue;
            }

            double cos, sin;
            if (fullSolve || cosE[i] == 0d && sinE[i] == 0d || FastMath.abs(n * dt) > MAX_STEP) {
                // Full solution of Kepler's equation.
                double m = (c[o + 1] + n * tjd) % PI2;
                if (m < 0d) {
                    m += PI2;
                }
                double ecc = solveKepler(m, e, initialGuess(m, e));
                cos = FastMath.cos(ecc);
                sin = FastMath.sin(ecc);
                meanAnomaly[i] = m;
                eccAnomaly[i] = ecc;
                cosE[i] = cos;
                sinE[i] = sin;
            } else {
                // Both anomalies wrap at 2 pi at the same time, so a single wrap keeps the residual a
                // small angle rather than a whole revolution.
                double m = meanAnomaly[i] + n * dt;
                double ecc = eccAnomaly[i];
                if (m >= PI2) {
                    m -= PI2;
                    ecc -= PI2;
                } else if (m < 0d) {
                    m += PI2;
                    ecc += PI2;
                }
                cos = cosE[i];
                sin = sinE[i];
                for (int it = 0; it < 3; it++) {
                    double dE = -(ecc - e * sin - m) / (1d - e * cos);
                    // Rotate by dE with its sine and cosine series truncated at the fifth order: the
                    // error is below 1e-19 for the largest step accepted, so no renormalization.
                    double d2 = dE * dE;
                    double cosD = 1d - 0.5 * d2 + d2 * d2 * INV_24;
                    double sinD = dE * (1d - d2 * INV_6 + d2 * d2 * INV_120);
                    double cosNew = cos * cosD - sin * sinD;
                    sin = sin * cosD + cos * sinD;
                    cos = cosNew;
                    ecc += dE;
                    if (FastMath.abs(dE) < TOLERANCE) {
                        break;
                    }
                }
                if (FastMath.abs(ecc - e * sin - m) > MAX_RESIDUAL) {
                    // The step was too large for the rotation to follow: solve from scratch.
                    ecc = solveKepler(m, e, ecc);
                    cos = FastMath.cos(ecc);
                    sin = FastMath.sin(ecc);
                }
                meanAnomaly[i] = m;
                eccAnomaly[i] = ecc;
                cosE[i] = cos;
                sinE[i] = sin;
            }

            // Parametric ellipse in the perifocal plane: x = a (cos E - e), y = a sqrt(1 - e^2) sin E.
            double xPf = c[o + 3] * (cos - e);
            double yPf = c[o + 4] * sin;
            double x = xPf * c[o + 5] + yPf * c[o + 6];
            double y = xPf * c[o + 7] + yPf * c[o + 8];
            double z = xPf * c[o + 9] + yPf * c[o + 10];

            orbitPos[i * 3] = x;
            orbitPos[i * 3 + 1] = y;
            orbitPos[i * 3 + 2] = z;

            if (mtx != null) {
                double nx = x * mtx[0] + y * mtx[1] + z * mtx[2] + mtx[3];
                double ny = x * mtx[4] + y * mtx[5] + z * mtx[6] + mtx[7];
                double nz = x * mtx[8] + y * mtx[9] + z * mtx[10] + mtx[11];
                x = nx;
                y = ny;
                z = nz;
            }

            float fx = (float) (x + dx);
            float fy = (float) (y + dy);
            float fz = (float) (z + dz);
            if (single) {
                int j = i * stride + offset;
                buf[j] = fx;
                buf[j + 1] = fy;
                buf[j + 2] = fz;
            } else {
                int first = i * repeat;
                for (int v = 0; v < repeat; v++) {
                    int j = (first + v) * stride + offset;
                    buf[j] = fx;
                    buf[j + 1] = fy;
                    buf[j + 2] = fz;
                }
            }
        }
    }

    private void projectRange(int from,
                              int to,
                              float[] buf,
                              int stride,
                              int offset,
                              int repeat) {
        final double[] mtx = this.mtx;
        final double dx = this.dx, dy = this.dy, dz = this.dz;
        final boolean single = repeat == 1;
        for (int i = from; i < to; i++) {
            double x = orbitPos[i * 3];
            double y = orbitPos[i * 3 + 1];
            double z = orbitPos[i * 3 + 2];
            if (mtx != null) {
                double nx = x * mtx[0] + y * mtx[1] + z * mtx[2] + mtx[3];
                double ny = x * mtx[4] + y * mtx[5] + z * mtx[6] + mtx[7];
                double nz = x * mtx[8] + y * mtx[9] + z * mtx[10] + mtx[11];
                x = nx;
                y = ny;
                z = nz;
            }
            float fx = (float) (x + dx);
            float fy = (float) (y + dy);
            float fz = (float) (z + dz);
            if (single) {
                int j = i * stride + offset;
                buf[j] = fx;
                buf[j + 1] = fy;
                buf[j + 2] = fz;
            } else {
                int first = i * repeat;
                for (int v = 0; v < repeat; v++) {
                    int j = (first + v) * stride + offset;
                    buf[j] = fx;
                    buf[j + 1] = fy;
                    buf[j + 2] = fz;
                }
            }
        }
    }

    private static void writeZero(int i,
                                  float[] buf,
                                  int stride,
                                  int offset,
                                  int repeat) {
        int first = i * repeat;
        for (int v = 0; v < repeat; v++) {
            int j = (first + v) * stride + offset;
            buf[j] = 0f;
            buf[j + 1] = 0f;
            buf[j + 2] = 0f;
        }
    }

    /**
     * Releases the state of this instance.
     */
    public void reset() {
        constants = null;
        meanAnomaly = null;
        eccAnomaly = null;
        cosE = null;
        sinE = null;
        orbitPos = null;
        count = 0;
        lastTjd = Double.NaN;
        frame = 0;
    }
}
