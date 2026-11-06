/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.render.gdx.graphics.CPUGalaxyVolumeGenerator;
import gaiasky.render.gdx.graphics.GalaxyDensityField;
import gaiasky.render.gdx.graphics.GalaxyShape;
import gaiasky.render.gdx.graphics.VolumeCalibration;
import gaiasky.render.gdx.graphics.VolumeLayout;
import gaiasky.render.gdx.graphics.VoxelGrid;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.util.Settings.GraphicsQuality;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * The CPU voxeliser, which is the only part of the volume generation that can be checked without a
 * GL context: it evaluates the density field in Java and writes the voxels itself.
 * <p>
 * What is tested here is that it agrees with the shader's <em>conventions</em> - the packed layout,
 * the voxel order, the calibration - and not that its noise matches: it does not, and is not meant
 * to (see {@link GalaxyDensityField}).
 */
public class CPUGalaxyVolumeGeneratorTest {

    private static BillboardDataset gas() {
        var gas = new BillboardDataset();
        gas.type = GalaxyAttributes.ChannelType.GAS;
        gas.distribution = GalaxyAttributes.Distribution.SPIRAL;
        gas.baseRadius = 1.0f;
        gas.minRadius = 0.25f;
        gas.numArms = 4;
        gas.armSigma = 0.1872369f;
        gas.baseAngle = 500f;
        gas.heightScale = 0.02f;
        gas.intensity = 1f;
        for (int i = 0; i < 12; i++) {
            gas.baseColors[i] = 1f;
        }
        return gas;
    }

    private static BillboardDataset dust() {
        var dust = new BillboardDataset();
        dust.type = GalaxyAttributes.ChannelType.DUST;
        dust.distribution = GalaxyAttributes.Distribution.SPIRAL;
        dust.baseRadius = 1.0f;
        dust.minRadius = 0.25f;
        dust.numArms = 4;
        dust.armSigma = 0.1872369f;
        dust.baseAngle = 500f;
        dust.heightScale = 0.02f;
        dust.intensity = 1f;
        dust.occlusionThreshold = 0.05f;
        for (int i = 0; i < 12; i++) {
            dust.baseColors[i] = 1f;
        }
        return dust;
    }

    private static VoxelGrid smallGrid() {
        return VoxelGrid.forSpan(2_000, 0.1, GraphicsQuality.LOW).shrinkTo(64 * 16 * 64);
    }

    @Test
    public void packedLayoutRoundTrips() {
        // Every member of the shape has to come back out of the packed array where it went in: this
        // is the Java reader of the layout, and the shader's gxUnpack reads the same offsets.
        var field = gas();
        field.baseRadius = 1.5f;
        field.minRadius = 0.125f;
        field.numArms = 3;
        field.armSigma = 0.25f;
        field.baseAngle = 640f;
        field.eccentricity[0] = 0.1f;
        field.eccentricity[1] = 0.2f;
        field.aspect = 0.3f;
        field.warpStrength = 0.04f;
        field.heightScale = 0.015f;
        field.heightProfile = GalaxyAttributes.HeightProfile.SMOOTH_DEC;
        field.colorNoise = 0.4f;
        field.intensity = 2.5f;
        field.baseColors[0] = 0.11f;
        field.baseColors[11] = 0.99f;
        field.laneWarp = 0.06f;
        field.noiseMask = 0.7f;
        field.laneNoiseScale = 1.5f;
        field.filamentContrast = 0.7f;
        field.filamentScale = 14f;
        field.filamentThreshold = 0.35f;
        field.noiseMaskFreq = 12f;
        field.noiseMaskOctaves = 5;

        float[] packed = VolumeLayout.packFields(List.of(field), 1);
        var shape = GalaxyShape.from(packed, 0);

        Assert.assertEquals(field.distribution.ordinal(), shape.distribution);
        Assert.assertEquals(field.baseRadius, shape.baseRadius, 0f);
        Assert.assertEquals(field.minRadius, shape.minRadius, 0f);
        Assert.assertEquals(field.numArms, shape.numArms);
        Assert.assertEquals(field.armSigma, shape.armSigma, 0f);
        Assert.assertEquals(field.baseAngle, shape.baseAngle, 0f);
        Assert.assertEquals(field.eccentricity[0], shape.eccentricityX, 0f);
        Assert.assertEquals(field.eccentricity[1], shape.eccentricityY, 0f);
        Assert.assertEquals(field.aspect, shape.aspect, 0f);
        Assert.assertEquals(field.warpStrength, shape.warpStrength, 0f);
        Assert.assertEquals(field.heightScale, shape.heightScale, 0f);
        Assert.assertEquals(field.heightProfile.ordinal(), shape.heightProfile);
        Assert.assertEquals(field.colorNoise, shape.colorNoise, 0f);
        Assert.assertEquals(field.intensity, shape.weight, 0f);
        Assert.assertEquals(field.baseColors[0], shape.baseColors[0], 0f);
        Assert.assertEquals(field.baseColors[11], shape.baseColors[11], 0f);
        Assert.assertEquals(field.type.ordinal(), shape.channel);
        Assert.assertEquals(field.laneWarp, shape.laneWarp, 0f);
        Assert.assertEquals(field.noiseMask, shape.noiseMask, 0f);
        Assert.assertEquals(field.laneNoiseScale, shape.laneNoiseScale, 0f);
        Assert.assertEquals(field.filamentContrast, shape.filamentContrast, 0f);
        Assert.assertEquals(field.filamentScale, shape.filamentScale, 0f);
        Assert.assertEquals(field.filamentThreshold, shape.filamentThreshold, 0f);
        Assert.assertEquals(field.noiseMaskFreq, shape.noiseMaskFreq, 0f);
        Assert.assertEquals(field.noiseMaskOctaves, shape.noiseMaskOctaves);
    }

    @Test
    public void sphereIsDenseInsideAndEmptyOutside() {
        var sphere = gas();
        sphere.distribution = GalaxyAttributes.Distribution.SPHERE;
        sphere.baseRadius = 1f;
        sphere.heightScale = 1f;
        // No inner hole: a spheroid with minRadius set has an empty core by design.
        sphere.minRadius = 0f;
        var shape = GalaxyShape.from(VolumeLayout.packFields(List.of(sphere), 1), 0);
        var out = new GalaxyDensityField.Sample();

        GalaxyDensityField.sample(shape, 0f, 0f, 0f, 0.01f, out);
        Assert.assertTrue("the core must have density", out.density > 0f);

        GalaxyDensityField.sample(shape, 5f, 0f, 0f, 0.01f, out);
        Assert.assertEquals("beyond 1.2 radii the field is empty", 0f, out.density, 0f);

        // And the core is denser than the rim.
        GalaxyDensityField.sample(shape, 0.9f, 0f, 0f, 0.01f, out);
        float rim = out.density;
        GalaxyDensityField.sample(shape, 0f, 0f, 0f, 0.01f, out);
        Assert.assertTrue(out.density > rim);
    }

    @Test
    public void flatFieldsAreCutByTheVerticalTail() {
        var disk = gas();
        disk.distribution = GalaxyAttributes.Distribution.DISK;
        disk.heightScale = 0.05f;
        var shape = GalaxyShape.from(VolumeLayout.packFields(List.of(disk), 1), 0);
        var out = new GalaxyDensityField.Sample();

        GalaxyDensityField.sample(shape, 0.5f, 0f, 0f, 0.01f, out);
        Assert.assertTrue(out.density > 0f);

        // Three scale heights and a bit up, the slab has been cut.
        GalaxyDensityField.sample(shape, 0.5f, GalaxyDensityField.VERTICAL_TAIL * disk.heightScale * 1.5f, 0f, 0.01f, out);
        Assert.assertEquals(0f, out.density, 0f);
    }

    @Test
    public void noiseIsInRangeAndDeterministic() {
        for (int i = 0; i < 1_000; i++) {
            float v = GalaxyDensityField.noise(i * 0.37f, i * 0.11f, i * 0.73f);
            Assert.assertTrue("noise out of range: " + v, v >= 0f && v <= 1f);
        }
        Assert.assertEquals(GalaxyDensityField.noise(1.3f, 2.7f, -0.4f),
                            GalaxyDensityField.noise(1.3f, 2.7f, -0.4f), 0f);
        // Not constant, or every clump would be the same clump.
        Assert.assertNotEquals(GalaxyDensityField.noise(0.1f, 0.1f, 0.1f),
                               GalaxyDensityField.noise(0.1f, 0.6f, 0.1f), 0f);
    }

    @Test
    public void dustConcentratesMoreThanGas() {
        float gasRatio = meanToPeak(gas());
        float dustRatio = meanToPeak(dust());
        Assert.assertTrue("dust should be lane-like: mean/peak " + dustRatio + " against gas " + gasRatio,
                          dustRatio < gasRatio);
    }

    /** Mean-to-peak ratio of the field over a small grid, without any calibration applied. **/
    private static float meanToPeak(BillboardDataset field) {
        var grid = smallGrid();
        var shape = GalaxyShape.from(VolumeLayout.packFields(List.of(field), 1), 0);
        float lateral = (float) field.volumeLateralHalfExtent();
        float vertical = (float) field.volumeVerticalHalfExtent();
        float[] origin = VolumeLayout.gridOrigin(lateral, vertical, grid);
        float[] cell = VolumeLayout.gridCell(lateral, vertical, grid);
        var out = new GalaxyDensityField.Sample();
        double sum = 0.0;
        float peak = 0f;
        for (int z = 0; z < grid.nz; z++) {
            for (int y = 0; y < grid.ny; y++) {
                for (int x = 0; x < grid.nx; x++) {
                    GalaxyDensityField.sample(shape,
                                              origin[0] + x * cell[0],
                                              origin[1] + y * cell[1],
                                              origin[2] + z * cell[2],
                                              cell[0], out);
                    sum += out.density;
                    peak = Math.max(peak, out.density);
                }
            }
        }
        Assert.assertTrue("the field has no density at all", peak > 0f);
        return (float) (sum / grid.voxelCount()) / peak;
    }

    @Test
    public void voxelsAreTheGridSizeInXFastestOrder() {
        var grid = smallGrid();
        ByteBuffer bytes = CPUGalaxyVolumeGenerator.voxelise(List.of(dust()), grid, new float[14]);
        Assert.assertEquals(grid.nx * grid.ny * grid.nz * 4, bytes.remaining());
    }

    @Test
    public void occlusiveSlotStoresTheCalibratedPeak() {
        // The point of the per-field normalisation: the densest voxel of a field stores
        // OCCLUSIVE_TARGET_PEAK times its intensity, whatever its shape.
        var grid = smallGrid();
        var stats = new float[14];
        ByteBuffer bytes = CPUGalaxyVolumeGenerator.voxelise(List.of(dust()), grid, stats);

        int peak = 0;
        for (int i = 3; i < bytes.capacity(); i += 4) {
            peak = Math.max(peak, bytes.get(i) & 0xff);
        }
        int expected = Math.round((float) VolumeCalibration.OCCLUSIVE_TARGET_PEAK * 255f);
        Assert.assertEquals("stored peak", expected, peak, 1);
        Assert.assertTrue("the reduce pass found a peak", stats[6] > 0f);
    }

    @Test
    public void intensityStaysALinearKnobOnTheOcclusiveSlot() {
        var half = dust();
        half.intensity = 0.5f;
        int halfPeak = peakAlpha(CPUGalaxyVolumeGenerator.voxelise(List.of(half), smallGrid(), new float[14]));
        int fullPeak = peakAlpha(CPUGalaxyVolumeGenerator.voxelise(List.of(dust()), smallGrid(), new float[14]));
        // Only one byte of quantisation is allowed: 0.5 * 0.98 * 255 = 125 against 250.
        Assert.assertEquals(expectedPeak(0.5f), halfPeak, 1);
        Assert.assertEquals(expectedPeak(1f), fullPeak, 1);
    }

    private static int expectedPeak(float intensity) {
        return Math.round(Math.min(1f, (float) VolumeCalibration.OCCLUSIVE_TARGET_PEAK * intensity) * 255f);
    }

    private static int peakAlpha(ByteBuffer bytes) {
        int peak = 0;
        for (int i = 3; i < bytes.capacity(); i += 4) {
            peak = Math.max(peak, bytes.get(i) & 0xff);
        }
        return peak;
    }

    @Test
    public void writtenAlphaIsTheFieldNormalisedAsTheShaderDoesIt() {
        // Recompute one voxel's alpha independently - same field, same calibration, same packed
        // layout - and compare it with the byte the writer produced. This is what pins the voxel
        // order (x fastest), the per-field normalisation and the target peak together.
        var grid = smallGrid();
        var field = dust();
        var stats = new float[14];
        ByteBuffer bytes = CPUGalaxyVolumeGenerator.voxelise(List.of(field), grid, stats);

        float[] packed = VolumeLayout.packFields(List.of(field), 1);
        var shape = GalaxyShape.from(packed, 0);
        float lateral = (float) field.volumeLateralHalfExtent();
        float vertical = (float) field.volumeVerticalHalfExtent();
        float[] origin = VolumeLayout.gridOrigin(lateral, vertical, grid);
        float[] cell = VolumeLayout.gridCell(lateral, vertical, grid);

        var sum = new GalaxyDensityField.FieldSum();
        var one = new GalaxyDensityField.Sample();
        float targetPeak = (float) VolumeCalibration.OCCLUSIVE_TARGET_PEAK;

        int checked = 0;
        for (int z = 0; z < grid.nz; z += 3) {
            for (int y = 0; y < grid.ny; y += 3) {
                for (int x = 0; x < grid.nx; x += 3) {
                    GalaxyDensityField.sampleFields(new GalaxyShape[]{shape}, 1,
                                                    origin[0] + x * cell[0],
                                                    origin[1] + y * cell[1],
                                                    origin[2] + z * cell[2],
                                                    cell[0], sum, one);
                    float alpha = 0f;
                    for (int i = 0; i < 1; i++) {
                        if (targetPeak > 0f && stats[6 + i] > 0f) {
                            alpha += sum.fieldDensity[i] * targetPeak / stats[6 + i];
                        } else {
                            alpha += sum.fieldDensity[i];
                        }
                    }
                    int expected = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
                    int offset = ((z * grid.ny + y) * grid.nx + x) * 4 + 3;
                    Assert.assertEquals("voxel " + x + "," + y + "," + z,
                                        expected, bytes.get(offset) & 0xff);
                    checked++;
                }
            }
        }
        Assert.assertTrue("nothing was checked", checked > 100);
    }

    @Test
    public void emissiveSlotIsBrightestAtTheCore() {
        var grid = smallGrid();
        var field = gas();
        // No inner hole: minRadius is the field's inner edge, so a field that has one has an empty
        // core and the brightest voxel sits on the edge instead.
        field.minRadius = 0f;
        ByteBuffer bytes = CPUGalaxyVolumeGenerator.voxelise(List.of(field), grid, new float[14]);
        // The galaxy is centred in the box, so the middle voxel of the grid is the core.
        int cx = grid.nx / 2, cy = grid.ny / 2, cz = grid.nz / 2;
        int core = bytes.get(((cz * grid.ny + cy) * grid.nx + cx) * 4 + 3) & 0xff;
        int corner = bytes.get(3) & 0xff;
        Assert.assertTrue("the core (" + core + ") should be brighter than a corner (" + corner + ")",
                          core > corner);
    }

    @Test
    public void innerRadiusEmptiesTheCore() {
        var grid = smallGrid();
        var field = gas();
        Assert.assertTrue(field.minRadius > 0f);
        ByteBuffer bytes = CPUGalaxyVolumeGenerator.voxelise(List.of(field), grid, new float[14]);
        int cx = grid.nx / 2, cy = grid.ny / 2, cz = grid.nz / 2;
        int core = bytes.get(((cz * grid.ny + cy) * grid.nx + cx) * 4 + 3) & 0xff;
        Assert.assertEquals("minRadius leaves the middle of the box empty", 0, core);
    }

    @Test
    public void emptySlotProducesNothing() {
        Assert.assertEquals(0, CPUGalaxyVolumeGenerator.voxelise(List.of(), smallGrid(), null).remaining());
    }
}
