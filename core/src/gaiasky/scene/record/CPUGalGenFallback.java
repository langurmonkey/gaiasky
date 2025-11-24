/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.record.BillboardDataset.Distribution;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Pair;
import gaiasky.util.math.MathUtilsDouble;
import net.jafama.FastMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.IntStream;

import static gaiasky.scene.record.BillboardDataset.ChannelType.DUST;
import static gaiasky.scene.record.BillboardDataset.Distribution.IRREGULAR;
import static gaiasky.scene.record.BillboardDataset.Distribution.SPIRAL_LOG;

/**
 * CPU equivalent of the GLSL compute shader for procedural billboard particle generation.
 * Produces List<ParticleVector> where each ParticleVector contains:
 * [x, y, z, size, r, g, b]
 * <p>
 * This is not bit-for-bit identical to the GPU shader, but replicates distributions,
 * RNG style (uint state), gaussian/fbm approximations and the main logic.
 */
public class CPUGalGenFallback {
    protected static final Logger.Log logger = Logger.getLogger(CPUGalGenFallback.class);
    private final ThreadLocal<RNG> rngHolder;
    private final double sizeFactor;

    public CPUGalGenFallback(double bodySize) {
        this.rngHolder = ThreadLocal.withInitial(RNG::new);
        this.sizeFactor = (100 * bodySize / (26000.0 * Constants.PC_TO_U));
    }


    // Simple 4x4 transform creation from dataset transform components (scale, rotation (deg) XYZ, translation)
    double[] makeTransform(BillboardDataset ds) {
        // scale
        double sx = ds.scale.x, sy = ds.scale.y, sz = ds.scale.z;
        // rotation Euler degrees to radians
        double rx = FastMath.toRadians(ds.rotation.x);
        double ry = FastMath.toRadians(ds.rotation.y);
        double rz = FastMath.toRadians(ds.rotation.z);

        // build rotation matrices (Z * Y * X)
        double cx = FastMath.cos(rx), sxr = FastMath.sin(rx);
        double cy = FastMath.cos(ry), syr = FastMath.sin(ry);
        double cz = FastMath.cos(rz), szr = FastMath.sin(rz);

        // Combined rotation
        double r00 = cz * cy;
        double r01 = cz * syr * sxr - szr * cx;
        double r02 = cz * syr * cx + szr * sxr;
        double r10 = szr * cy;
        double r11 = szr * syr * sxr + cz * cx;
        double r12 = szr * syr * cx - cz * sxr;
        double r20 = -syr;
        double r21 = cy * sxr;
        double r22 = cy * cx;

        // Apply scale
        r00 *= sx;
        r01 *= sy;
        r02 *= sz;
        r10 *= sx;
        r11 *= sy;
        r12 *= sz;
        r20 *= sx;
        r21 *= sy;
        r22 *= sz;

        double tx = ds.translation.x, ty = ds.translation.y, tz = ds.translation.z;

        // Row-major 4x4 matrix as array (m00,m01,m02,m03, m10,m11,...)
        return new double[]{
                r00, r01, r02, tx,
                r10, r11, r12, ty,
                r20, r21, r22, tz,
                0.0, 0.0, 0.0, 1.0
        };
    }

    double[] transformPoint(double[] mat4, double x, double y, double z) {
        double nx = mat4[0] * x + mat4[1] * y + mat4[2] * z + mat4[3];
        double ny = mat4[4] * x + mat4[5] * y + mat4[6] * z + mat4[7];
        double nz = mat4[8] * x + mat4[9] * y + mat4[10] * z + mat4[11];
        return new double[]{nx, ny, nz};
    }

    /**
     * Precomputes the transformation matrix and the base colors for a given dataset.
     *
     * @param ds The dataset.
     *
     * @return A pair with the transform matrix and the base colors.
     */
    private Pair<double[], float[][]> prepare(BillboardDataset ds) {
        // precompute transform matrix
        double[] mat = makeTransform(ds);

        // Colors: baseColors is float[] length 12 (4 * 3) expected
        float[][] baseColors = new float[4][3];
        if (ds.baseColors != null) {
            for (int i = 0; i < 4; i++) {
                int off = i * 3;
                if (off + 2 < ds.baseColors.length) {
                    baseColors[i][0] = ds.baseColors[off];
                    baseColors[i][1] = ds.baseColors[off + 1];
                    baseColors[i][2] = ds.baseColors[off + 2];
                } else {
                    baseColors[i][0] = baseColors[i][1] = baseColors[i][2] = 1.0f;
                }
            }
        } else {
            for (int i = 0; i < 4; i++) baseColors[i][0] = baseColors[i][1] = baseColors[i][2] = 1.0f;
        }

        return new Pair<>(mat, baseColors);
    }

    /**
     * Generate particles for a given {@link BillboardDataset}.
     *
     * @param ds         BillboardDataset instance (fields read from your class)
     * @param globalSeed integer seed (u_seed)
     *
     * @return list of {@link IParticleRecord}, each with position, size, and color.
     */
    public List<IParticleRecord> generate(BillboardDataset ds, int globalSeed) {
        logger.info("CPU dataset generation: " + ds.type + "/" + ds.distribution);
        int count = ds.particleCount;
        if (count <= 0) return Collections.emptyList();

        IParticleRecord[] array = new IParticleRecord[count];
        var pair = prepare(ds);
        var mat = pair.getFirst();
        var baseColors = pair.getSecond();

        // For each particle, create RNG seeded similarly to shader:
        // shader: uint state = (i * 747796405u + 2891336453u) * u_seed;
        for (int i = 0; i < count; i++) {
            // compute initial state: mix i and globalSeed
            // Use same constants but in signed ints
            long a = Integer.toUnsignedLong(747796405);
            long b = Integer.toUnsignedLong(289133645);
            long s = ((Integer.toUnsignedLong(i) * a + b) * Integer.toUnsignedLong(globalSeed)) & 0xFFFFFFFFL;
            int seed = (int) s;

            var rng = rngHolder.get();
            rng.setSeed(seed);

            // Choose distribution and compute position in dataset local coordinates.
            Distribution distribution = ds.distribution;
            double[] pos = switch (distribution) {
                case SPIRAL_LOG -> positionLogSpiral(rng, ds);
                case SPIRAL -> positionDensityWave(rng, ds);
                case BAR -> positionBar(rng, ds);
                case ELLIPSOID -> positionEllipsoid(rng, ds);
                case SPHERE, IRREGULAR -> positionSphere(rng, ds);
                case DISK_GAUSS -> positionDiskGauss(rng, ds);
                case SPHERE_GAUSS -> positionSphereGauss(rng, ds);
                case CONE -> positionCone(rng, ds);
                default -> positionDisk(rng, ds);
            }; // double version

            double size = generateSize(rng, pos, ds);
            double[] color = generateColor(rng, ds.colorNoise, baseColors);
            double[] tp = transformPoint(mat, pos[0], pos[1], pos[2]);
            double layer = pickLayer(rng, ds);
            double type = ds.type.ordinal();

            var data = new double[]{
                    tp[0],
                    tp[1],
                    tp[2],
                    color[0],
                    color[1],
                    color[2],
                    size,
                    type,
                    layer
            };
            array[i] = new ParticleVector(data);
        }

        return List.of(array);
    }

    /**
     * Concurrent version of {@link #generate(BillboardDataset, int)} using chunked batches.
     *
     * @param ds         BillboardDataset instance (fields read from your class)
     * @param globalSeed integer seed (u_seed)
     *
     * @return list of {@link IParticleRecord}, each with position, size, and color.
     */
    public List<IParticleRecord> generateConcurrent(BillboardDataset ds, int globalSeed) {
        int count = ds.particleCount;
        if (count <= 0) return Collections.emptyList();

        var pair = prepare(ds);
        var mat = pair.getFirst();
        var baseColors = pair.getSecond();

        AtomicReferenceArray<IParticleRecord> out = new AtomicReferenceArray<>(count);

        // Default chunk size. Possibly needs tuning.
        final int chunkSize = 512;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Void>> tasks = new ArrayList<>();

            for (int start = 0; start < count; start += chunkSize) {
                final int chunkStart = start;
                final int chunkEnd = Math.min(start + chunkSize, count);

                tasks.add(() -> {
                    for (int i = chunkStart; i < chunkEnd; i++) {
                        long a = Integer.toUnsignedLong(747796405);
                        long b = Integer.toUnsignedLong(289133645);
                        long s = ((Integer.toUnsignedLong(i) * a + b) * Integer.toUnsignedLong(globalSeed)) & 0xFFFFFFFFL;
                        int seed = (int) s;

                        var rng = rngHolder.get();
                        rng.setSeed(seed);

                        // Compute particle position
                        double[] pos = switch (ds.distribution) {
                            case SPIRAL_LOG -> positionLogSpiral(rng, ds);
                            case SPIRAL -> positionDensityWave(rng, ds);
                            case BAR -> positionBar(rng, ds);
                            case ELLIPSOID -> positionEllipsoid(rng, ds);
                            case SPHERE, IRREGULAR -> positionSphere(rng, ds);
                            case DISK_GAUSS -> positionDiskGauss(rng, ds);
                            case SPHERE_GAUSS -> positionSphereGauss(rng, ds);
                            case CONE -> positionCone(rng, ds);
                            default -> positionDisk(rng, ds);
                        };

                        double size = generateSize(rng, pos, ds);
                        double[] color = generateColor(rng, ds.colorNoise, baseColors);
                        double[] tp = transformPoint(mat, pos[0], pos[1], pos[2]);
                        double layer = pickLayer(rng, ds);
                        double type = ds.type.ordinal();

                        var data = new double[]{
                                tp[0],
                                tp[1],
                                tp[2],
                                color[0],
                                color[1],
                                color[2],
                                size,
                                type,
                                layer
                        };

                        out.set(i, new ParticleVector(data));
                    }
                    return null;
                });
            }

            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        // Convert AtomicReferenceArray back to List
        return IntStream.range(0, count)
                .mapToObj(out::get)
                .toList();
    }

    // Position generators — approximate the GLSL ones. Use RNG methods to match behaviour.

    private double[] positionSphere(RNG rng, BillboardDataset ds) {
        double r0 = (ds.baseRadius - ds.minRadius);
        double r = ds.minRadius + (r0 + (rng.rand() - 0.5) * 0.3) * FastMath.sqrt(rng.rand());
        double theta = rng.rand() * (2.0 * FastMath.PI);
        double phi = FastMath.acos(rng.rand() * 2.0 - 1.0);
        double x = r * FastMath.sin(phi) * FastMath.cos(theta);
        double y = r * FastMath.sin(phi) * FastMath.sin(theta);
        double z = r * FastMath.cos(phi);
        return new double[]{x, y, z};
    }

    private double[] positionSphereGauss(RNG rng, BillboardDataset ds) {
        double r = ds.minRadius + (ds.baseRadius - ds.minRadius) * FastMath.sqrt(rng.ggaussian(1.0f) * 0.15);
        double theta = rng.rand() * (2.0 * FastMath.PI);
        double phi = FastMath.acos(rng.rand() * 2.0 - 1.0);
        double x = r * FastMath.sin(phi) * FastMath.cos(theta);
        double y = r * FastMath.sin(phi) * FastMath.sin(theta);
        double z = r * FastMath.cos(phi);
        return new double[]{x, y, z};
    }

    private double[] positionCone(RNG rng, BillboardDataset ds) {
        double coneAngle = FastMath.toRadians(ds.baseAngle);
        double cosCone = FastMath.cos(coneAngle);
        double cosPhi = cosCone + (1f - cosCone) * rng.rand();
        double phi = FastMath.acos(cosPhi);
        double theta = rng.rand() * (2.0 * FastMath.PI);
        double dirX = FastMath.sin(phi) * FastMath.cos(theta);
        double dirY = FastMath.cos(phi);
        double dirZ = FastMath.sin(phi) * FastMath.sin(theta);
        double r = ds.minRadius + (ds.baseRadius - ds.minRadius) * FastMath.pow(rng.rand(), 1.0 / 3.0);
        r += (rng.rand() - 0.5) * 0.1 * ds.baseRadius;
        return new double[]{r * dirX, r * dirY - 0.5, r * dirZ};
    }

    private double[] positionDisk(RNG rng, BillboardDataset ds) {
        double theta = rng.rand() * (2.0 * FastMath.PI);
        double r = ds.minRadius + (ds.baseRadius - ds.minRadius + (rng.rand() - 0.5f) * 0.3f) * FastMath.sqrt(rng.rand());
        double z = r * FastMath.cos(theta);
        double x = r * FastMath.sin(theta);
        double y = (rng.rand() - 0.5) * 2.0 * ds.heightScale;
        return new double[]{x, y, z};
    }

    private double[] positionDiskGauss(RNG rng, BillboardDataset ds) {
        double theta = rng.rand() * (2.0 * FastMath.PI);
        double r = ds.minRadius + (ds.baseRadius - ds.minRadius) * FastMath.sqrt(rng.ggaussian(1.0f) * 0.15f);
        double z = r * FastMath.cos(theta);
        double x = r * FastMath.sin(theta);
        double y = (rng.rand() - 0.5) * 2.0 * ds.heightScale;
        return new double[]{x, y, z};
    }

    private double[] positionEllipsoid(RNG rng, BillboardDataset ds) {
        double u1 = rng.rand();
        double u2 = rng.rand();
        double z = 2.0f * u1 - 1.0f;
        double phi = (2.0 * FastMath.PI * u2);
        double rxy = FastMath.sqrt(FastMath.max(0.0, 1.0 - z * z));
        double dirX = rxy * FastMath.cos(phi);
        double dirY = z;
        double dirZ = rxy * FastMath.sin(phi);
        double s = rng.rand();
        double minFrac = FastMath.max(0.0, FastMath.min(1.0, ds.minRadius / ds.baseRadius));
        double centralDensity = rng.rand() + 1.2f;
        double rFrac = minFrac + (1.0 - minFrac) * FastMath.pow(s, centralDensity / 3.0);
        double a = ds.baseRadius;
        double b = ds.baseRadius * FastMath.max(0.0, FastMath.min(1.0, 1.0 - ds.eccentricity[1]));
        double c = ds.baseRadius * FastMath.max(0.0, FastMath.min(1.0, 1.0 - ds.eccentricity[0]));
        double x = a * dirX * rFrac;
        double y = c * dirY * rFrac;
        double zOut = b * dirZ * rFrac;
        return new double[]{x, y, zOut};
    }

    private double[] positionBar(RNG rng, BillboardDataset ds) {
        double x = (rng.rand() * 2.0 - 1.0) * ds.baseRadius * ds.aspect;
        double y = (rng.rand() - 0.5) * 2.0 * ds.heightScale * ds.baseRadius;
        double z = (rng.rand() - 0.5) * 2.0 * ds.heightScale * ds.baseRadius;
        double falloff = FastMath.exp(-0.5 * (x * x + z * z) / (ds.baseRadius * ds.baseRadius));
        x *= falloff;
        z *= falloff;
        return new double[]{x, y, z};
    }

    // density wave -> "spiral" approximation
    private double[] positionDensityWave(RNG rng, BillboardDataset ds) {
        final int numEllipses = 200;
        int ellipseIndex = (int) (rng.rand() * numEllipses);
        double t = (ellipseIndex + 0.5f) / numEllipses;
        double min = ds.minRadius;
        double max = 1.0f - min;
        double ellipse_r = ds.baseRadius * (min + max * t);
        double a = ellipse_r;
        double b = ellipse_r * (1.0 - ds.eccentricity[0]);
        double pitchRad = FastMath.toRadians(ds.baseAngle) * t;
        double cosPitch = FastMath.cos(pitchRad), sinPitch = FastMath.sin(pitchRad);
        double ellipse_angle = rng.rand() * 2.0 * FastMath.PI;
        double x_ellipse = a * FastMath.cos(ellipse_angle);
        double z_ellipse = b * FastMath.sin(ellipse_angle);
        double x = x_ellipse * cosPitch + z_ellipse * sinPitch;
        double z = -x_ellipse * sinPitch + z_ellipse * cosPitch;
        x += ds.spiralDeltaPos[0] * t;
        z += ds.spiralDeltaPos[1] * t;
        x += rng.gaussian() * (ds.baseRadius * 0.015);
        z += rng.gaussian() * (ds.baseRadius * 0.015);
        if (rng.rand() > 0.7f) {
            double[] v = new double[]{x, z};
            double r = FastMath.hypot(v[0], v[1]);
            double nnx = v[0] / (r + 1e-9) * (r + (rng.rand() * 2.0 - 1.0) * 0.2);
            double nnz = v[1] / (r + 1e-9) * (r + (rng.rand() * 2.0 - 1.0) * 0.2);
            x = nnx;
            z = nnz;
        }
        double y = (rng.rand() - 0.5) * 2.0 * ds.heightScale;
        return new double[]{x, y, z};
    }

    private double[] positionDensityWaveOne(RNG rng, BillboardDataset ds) {
        final int numLayers = 200;
        int layerIndex = (int) (rng.rand() * numLayers);
        double t = (layerIndex + 0.5f) / numLayers;
        double min = ds.minRadius;
        double max = 1.0f - min;
        double radius = ds.baseRadius * (min + max * t);
        double angle = rng.rand() * 2.0 * FastMath.PI;
        double armAngle = angle % (2.0943951); // 120 degrees
        double x_arm = radius * FastMath.cos(armAngle);
        double z_arm = radius * FastMath.sin(armAngle);
        double pitchRad = FastMath.toRadians(ds.baseAngle) * t;
        double cosPitch = FastMath.cos(pitchRad), sinPitch = FastMath.sin(pitchRad);
        double x = x_arm * cosPitch + z_arm * sinPitch;
        double z = -x_arm * sinPitch + z_arm * cosPitch;
        x += ds.spiralDeltaPos[0] * t;
        z += ds.spiralDeltaPos[1] * t;
        x += rng.gaussian() * (ds.baseRadius * 0.015);
        z += rng.gaussian() * (ds.baseRadius * 0.015);
        if (rng.rand() > 0.7f) {
            double[] v = new double[]{x, z};
            double r = FastMath.hypot(v[0], v[1]);
            double nnx = v[0] / (r + 1e-9) * (r + (rng.rand() * 2.0 - 1.0) * 0.2);
            double nnz = v[1] / (r + 1e-9) * (r + (rng.rand() * 2.0 - 1.0) * 0.2);
            x = nnx;
            z = nnz;
        }
        double y = (rng.rand() - 0.5) * 2.0 * ds.heightScale;
        return new double[]{x, y, z};
    }

    // Logarithmic spiral (approximate)
    private double[] positionLogSpiral(RNG rng, BillboardDataset ds) {
        double r_min = ds.minRadius;
        double r_max = ds.baseRadius;
        double pitchDeg = MathUtilsDouble.clamp(ds.baseAngle * 0.1, 0.0, 100.0);
        double b = FastMath.tan(FastMath.toRadians(pitchDeg));
        double armWidth = 0.2f * ds.baseRadius;
        final int MAX_ATTEMPTS = 10;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double r = r_min + rng.rand() * (r_max - r_min);
            double radius_factor = 1.0 - 0.3 * (r - r_min) / (r_max - r_min);
            if (rng.rand() > radius_factor) {
                r = r_min + rng.rand() * (r_max - r_min);
            }
            r = r_min + (r_max - r_min) * FastMath.pow(rng.rand(), FastMath.max(0.0, FastMath.min(1.0, (r - r_min) / (r_max - r_min))));
            int armIndex = (int) (rng.rand() * ds.numArms);
            double armOffset = armIndex * 2.0 * FastMath.PI / ds.numArms;
            double spiralAngle = b * FastMath.log(FastMath.max(1e-6, r / (0.1 * ds.baseRadius))) + armOffset;

            double base_pattern = (FastMath.sin((spiralAngle + armIndex * 1.618) * 13.0)
                    + FastMath.cos((spiralAngle + armIndex * 1.618) * 9.0) + 1.0) / 2.0;
            double probability = FastMath.max(0.0, FastMath.min(1.0, base_pattern));
            if (rng.rand() <= probability || attempt == MAX_ATTEMPTS - 1) {
                double radialDirx = FastMath.cos(spiralAngle), radialDiry = FastMath.sin(spiralAngle);
                double tangentX = (-radialDiry - b * radialDirx);
                double tangentY = (radialDirx - b * radialDiry);
                double len = FastMath.hypot(tangentX, tangentY);
                if (len > 1e-9) {
                    tangentX /= len;
                    tangentY /= len;
                }
                double sigma = ds.armSigma;
                double gaussianSpread = rng.gaussian() * sigma;
                double[] armSpread = new double[]{gaussianSpread * armWidth * tangentX, gaussianSpread * armWidth * tangentY};
                double x = r * FastMath.cos(spiralAngle) + armSpread[0];
                double z = r * FastMath.sin(spiralAngle) + armSpread[1];
                double y = (rng.rand() - 0.5) * 2.0 * ds.heightScale;
                return new double[]{x, y, z};
            }
        }
        // fallback
        return positionDisk(rng, ds);
    }

    private int pickLayer(RNG rng, BillboardDataset ds) {
        // If there are valid layers, pick one randomly
        if (ds.layers.length > 0) {
            int randomIndex = (int) (rng.rand() * (float) ds.layers.length);
            int selectedLayer = ds.layers[randomIndex];
            return selectedLayer;
        }
        return 0;
    }

    // Size generator approximating shader generateSize
    private double generateSize(RNG rng, double[] pos, BillboardDataset ds) {
        var radialMask = 1.0;
        var sizeNoise = FastMath.abs(ds.sizeNoise);
        // in shader: if (distrib == D_SPIRAL_LOG && type == TYPE_DUST) ...
        if (ds.distribution == SPIRAL_LOG && ds.type == DUST) {
            double r = FastMath.sqrt(pos[0] * pos[0] + pos[2] * pos[2]);
            radialMask = (MathUtilsDouble.smoothstep(ds.minRadius, ds.baseRadius * 0.3, r)
                    * MathUtilsDouble.smoothstep(ds.baseRadius, ds.baseRadius * 0.8, r));
        } else if (ds.distribution == IRREGULAR) {
            var rand = rng.rand();
            var fac = rng.rand() * FastMath.pow(sizeNoise, 4.0);
            if (rng.rand() > 0.5) {
                return rng.fbm((pos[0] + rand) * fac, (pos[1] + rand) * fac, (pos[2] + rand) * fac) * 2.0 * sizeFactor;
            } else {
                return rng.fbm((pos[2] + rand) * fac, (pos[0] + rand) * fac, (pos[1] + rand) * fac) * 2.0 * sizeFactor;
            }
        }
        // When sizeNoise = 0, the particle size is 1.
        // When sizeNoise = 1, size is random between ~0.1 and ~4.
        // When sizeNoise < 0, we use fbm as a mask.
        if (ds.sizeMask) {
            return rng.fbm(pos[0] * sizeNoise, pos[2] * sizeNoise) * 2.0 * sizeFactor * radialMask;
        } else {
            return (sizeFactor + (rng.rand() * 2.0 - 1.0) * sizeFactor * 2.0 * sizeNoise) * radialMask;
        }
    }

    private double[] generateColor(RNG rng, double colorNoise, float[][] baseColors) {
        int idx = (int) (rng.rand() * 4.0f);
        if (idx < 0) idx = 0;
        if (idx > 3) idx = 3;
        float[] base = baseColors[idx];
        double[] noise = new double[]{
                colorNoise * ((rng.rand() * 2.0f) - 1.0f),
                colorNoise * ((rng.rand() * 2.0f) - 1.0f),
                colorNoise * ((rng.rand() * 2.0f) - 1.0f)
        };
        double[] out = new double[3];
        for (int i = 0; i < 3; i++) {
            double v = base[i] + noise[i];
            out[i] = FastMath.max(0f, FastMath.min(1f, v));
        }
        return out;
    }

    /** Random number generator with some utility functions that mimic the GLSL implementation. **/
    public static class RNG {
        private int state;

        public void setSeed(int seed) {
            this.state = seed;
        }

        // ---------- Basic RNG ----------
        public double rand() {
            state ^= (state << 13);
            state ^= (state >>> 17);
            state ^= (state << 5);
            return (state & 0x00FFFFFF) / 16777216.0f;
        }

        public double gaussian() {
            double u, v, s;
            do {
                u = rand() * 2.0 - 1.0;
                v = rand() * 2.0 - 1.0;
                s = u * u + v * v;
            } while (s >= 1.0 || s == 0.0);
            s = FastMath.sqrt(-2.0 * FastMath.log(s) / s);
            return u * s;
        }

        public double gamma(double x) {
            final double g = 4.7421875f;
            double series = 0.99999999999999709182;
            series += 57.156235665862923517 / (x + 0.0);
            series += -59.597960355475491248 / (x + 1.0);
            series += 14.136097974741747174 / (x + 2.0);
            series += -0.49191381609762019978 / (x + 3.0);
            series += 0.33994649984811888699e-4 / (x + 4.0);
            series += 0.46523628927048575665e-4 / (x + 5.0);
            series += -0.98374475304879564677e-4 / (x + 6.0);
            series += 0.15808870322491248884e-3 / (x + 7.0);
            series += -0.21026444172410488382e-3 / (x + 8.0);
            series += 0.21743961811521264320e-3 / (x + 9.0);
            series += -0.16431810653676389022e-3 / (x + 10.0);
            series += 0.84418223983852743293e-4 / (x + 11.0);
            series += -0.26190838401581408670e-4 / (x + 12.0);
            series += 0.36899182659531622704e-5 / (x + 13.0);
            double t = x + g - 0.5;
            return (FastMath.sqrt(2.0 * FastMath.PI) * FastMath.pow(t, x - 0.5) * FastMath.exp(-t) * series);
        }

        public double ggaussian(double beta) {
            double x = gaussian();
            if (FastMath.abs(beta - 2.0f) < 1e-6f) return x;
            double sign = FastMath.signum(x);
            double absX = FastMath.abs(x);
            double transformed = (sign * FastMath.pow(absX, 2.0 / beta));
            double varianceCorrection = FastMath.sqrt(gamma(3.0f / beta) / gamma(1.0f / beta));
            return transformed * varianceCorrection;
        }

        // ---------- 2D / 3D random functions ----------
        public double random(double x, double y) {
            return fract(FastMath.sin(dot(x, y, 12.9898f, 78.233f)) * 43758.5453123f);
        }

        public double random(double x, double y, double z) {
            return fract(FastMath.sin(dot(x, y, z, 12.9898f, 78.233f, 151.718f)) * 43758.5453123f);
        }

        private double dot(double x, double y, double a, double b) {
            return x * a + y * b;
        }

        private double dot(double x, double y, double z, double a, double b, double c) {
            return x * a + y * b + z * c;
        }

        private double fract(double v) {
            return v - FastMath.floor(v);
        }

        // ---------- Perlin-style noise ----------
        public double noise(double x, double y) {
            double ix = FastMath.floor(x);
            double iy = FastMath.floor(y);
            double fx = x - ix;
            double fy = y - iy;

            double a = random(ix, iy);
            double b = random(ix + 1.0f, iy);
            double c = random(ix, iy + 1.0f);
            double d = random(ix + 1.0f, iy + 1.0f);

            double u = fx * fx * (3.0f - 2.0f * fx);

            return lerp(a, b, u) + (c - a) * fy * (1.0f - u) + (d - b) * u * fy;
        }

        public double noise(double x, double y, double z) {
            double ix = FastMath.floor(x);
            double iy = FastMath.floor(y);
            double iz = FastMath.floor(z);
            double fx = x - ix;
            double fy = y - iy;
            double fz = z - iz;

            double a = random(ix, iy, iz);
            double b = random(ix + 1, iy, iz);
            double c = random(ix, iy + 1, iz);
            double d = random(ix + 1, iy + 1, iz);
            double e = random(ix, iy, iz + 1);
            double f1 = random(ix + 1, iy, iz + 1);
            double g = random(ix, iy + 1, iz + 1);
            double h = random(ix + 1, iy + 1, iz + 1);

            double u = fx * fx * (3 - 2 * fx);
            double v = fy * fy * (3 - 2 * fy);
            double w = fz * fz * (3 - 2 * fz);

            return lerp(lerp(lerp(a, b, u), lerp(c, d, u), v),
                        lerp(lerp(e, f1, u), lerp(g, h, u), v),
                        w);
        }

        private double lerp(double a, double b, double t) {
            return a + t * (b - a);
        }

        // ---------- FBM ----------
        private static final int OCTAVES = 4;

        public double fbm(double x, double y) {
            double value = 0;
            double amplitude = 0.5;
            for (int i = 0; i < OCTAVES; i++) {
                value += amplitude * noise(x, y);
                x *= 2.0;
                y *= 2.0;
                amplitude *= 0.5;
            }
            return value;
        }

        public double fbm(double x, double y, double z) {
            double value = 0f;
            double amplitude = 0.5;
            for (int i = 0; i < OCTAVES; i++) {
                value += amplitude * noise(x, y, z);
                x *= 2.0;
                y *= 2.0;
                z *= 2.0;
                amplitude *= 0.5;
            }
            return value;
        }
    }


}

