/*
 * Copyright (c) 2025-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.loader;

import com.badlogic.gdx.utils.GdxRuntimeException;
import gaiasky.render.gdx.graphics.VolumeType;
import gaiasky.util.Trio;
import org.junit.Assert;
import org.junit.Test;

/**
 * The loader used to derive a volume's resolution from the length of its file, by taking the cube
 * root of {@code length / channels}. That can only ever produce a cube, so a non-cubic grid - a
 * flattened galaxy disk, say 400x400x40 - was unloadable even though the metadata next to the file
 * stated exactly what it was. These tests pin the metadata-driven resolution, the parser that reads
 * it, and the fallback for every volume that predates the {@code Grid size:} line.
 * <p>
 * The failure mode this replaces is quiet: a grid read as a cube of the wrong size still produces a
 * texture, and the volume is simply stretched on screen.
 */
public class VolumeTextureLoaderTest {

    private static final String METADATA = """
            Channel: galaxy_Gas.dat.gz
            Grid size: 400x400x40
            World size: 7.071
            Data bounds: [-3.348, -3.372, -0.343] to [3.373, 3.363, 0.328]
            Volume bounds: [-3.523, -3.540, -3.543] to [3.548, 3.532, 3.528]
            Voxel size: 0.017679
            Has color: True
            Formats: rgba
            """;

    @Test
    public void scalarGridSizeIsACube() {
        // The shape every volume written before non-cubic grids existed has.
        assertGrid(VolumeTextureLoader.parseGridSize("400"), 400, 400, 400);
        assertGrid(VolumeTextureLoader.parseGridSize(" 400 "), 400, 400, 400);
    }

    @Test
    public void vectorGridSizeAcceptsTheUsualSeparators() {
        assertGrid(VolumeTextureLoader.parseGridSize("400x400x40"), 400, 400, 40);
        assertGrid(VolumeTextureLoader.parseGridSize("400 x 400 x 40"), 400, 400, 40);
        assertGrid(VolumeTextureLoader.parseGridSize("400 * 400 * 40"), 400, 400, 40);
        assertGrid(VolumeTextureLoader.parseGridSize("400, 400, 40"), 400, 400, 40);
    }

    @Test
    public void malformedGridSizeIsRejected() {
        // A half-understood grid must not be guessed at: it is null, and the caller falls back to
        // the file length or fails, rather than loading a cube of the wrong size.
        Assert.assertNull(VolumeTextureLoader.parseGridSize(null));
        Assert.assertNull(VolumeTextureLoader.parseGridSize(""));
        Assert.assertNull(VolumeTextureLoader.parseGridSize("400x400"));
        Assert.assertNull(VolumeTextureLoader.parseGridSize("400x400x40x4"));
        Assert.assertNull(VolumeTextureLoader.parseGridSize("400³"));
    }

    @Test
    public void metadataGivesGridAndBounds() {
        var metadata = VolumeTextureLoader.parseMetadata(METADATA);

        assertGrid(metadata.grid(), 400, 400, 40);
        Assert.assertEquals(-3.523f, metadata.boundsMin().x, 1e-6f);
        Assert.assertEquals(-3.540f, metadata.boundsMin().y, 1e-6f);
        Assert.assertEquals(-3.543f, metadata.boundsMin().z, 1e-6f);
        Assert.assertEquals(3.548f, metadata.boundsMax().x, 1e-6f);
        Assert.assertEquals(3.532f, metadata.boundsMax().y, 1e-6f);
        Assert.assertEquals(3.528f, metadata.boundsMax().z, 1e-6f);
    }

    @Test
    public void dataBoundsIsNotTheBox() {
        // "Data bounds" is the extent of the particles, "Volume bounds" the extent of the box the
        // texture is sampled over. Confusing the two shrinks the volume onto the data.
        var metadata = VolumeTextureLoader.parseMetadata(
                "Channel: x\nData bounds: [-1.0, -1.0, -1.0] to [1.0, 1.0, 1.0]\n");

        Assert.assertEquals(-0.5f, metadata.boundsMin().x, 1e-6f);
        Assert.assertEquals(0.5f, metadata.boundsMax().x, 1e-6f);
    }

    @Test
    public void oldWorldBoundsSpellingIsStillAccepted() {
        var metadata = VolumeTextureLoader.parseMetadata(
                "World bounds: [-3.359, -3.375, -0.345] to [3.365, 3.359, 0.329]\n");

        Assert.assertEquals(-3.359f, metadata.boundsMin().x, 1e-6f);
        Assert.assertEquals(0.329f, metadata.boundsMax().z, 1e-6f);
    }

    @Test
    public void metadataWithoutAGridLeavesItNull() {
        // Which is what tells the caller to infer the resolution from the file length.
        var metadata = VolumeTextureLoader.parseMetadata(METADATA.replace("Grid size: 400x400x40\n", ""));

        Assert.assertNull(metadata.grid());
        Assert.assertEquals(-3.523f, metadata.boundsMin().x, 1e-6f);
    }

    @Test
    public void metadataWithoutBoundsFallsBackToTheUnitCube() {
        var metadata = VolumeTextureLoader.parseMetadata("Channel: x\nGrid size: 400\n");

        assertGrid(metadata.grid(), 400, 400, 400);
        Assert.assertEquals(-0.5f, metadata.boundsMin().x, 1e-6f);
        Assert.assertEquals(0.5f, metadata.boundsMax().x, 1e-6f);
    }

    @Test
    public void nonCubicGridIsLoadedFromMetadata() {
        long rgbsPerVoxel = 4;
        var detection = VolumeTextureLoader.detectVolumeTypeAndDimensions(
                400L * 400 * 40 * rgbsPerVoxel,
                new Trio<>(400, 400, 40));

        Assert.assertEquals(VolumeType.RGBA, detection.type());
        assertGrid(detection.dimensions(), 400, 400, 40);
    }

    @Test
    public void nonCubicGridIsUnloadableWithoutMetadata() {
        // The regression this change fixes: 400*400*40 is 6,400,000 voxels, and
        // 6,400,000 * 4 bytes has no integral cube root, so the file-length heuristic cannot
        // classify it however it is told to read the channels.
        long bytes = 400L * 400 * 40 * 4;

        assertCannotDetect(bytes);
    }

    @Test
    public void gridThatDoesNotDivideTheFileIsRejected() {
        // A stale or truncated texture must fail the load rather than be reinterpreted: the
        // declared grid decides the channel count, so one extra byte means the metadata and the
        // data are out of step.
        assertFails(() -> VolumeTextureLoader.detectVolumeTypeAndDimensions(
                400L * 400 * 40 * 4 + 1,
                new Trio<>(400, 400, 40)));
    }

    @Test
    public void unsupportedChannelCountIsRejected() {
        // Two bytes per voxel is not a format the shader can bind.
        assertFails(() -> VolumeTextureLoader.detectVolumeTypeAndDimensions(
                100L * 100 * 100 * 2,
                new Trio<>(100, 100, 100)));
    }

    @Test
    public void emptyGridIsRejected() {
        // A zero on any axis is caught before the modulo, which would otherwise divide by zero.
        assertFails(() -> VolumeTextureLoader.detectVolumeTypeAndDimensions(100, new Trio<>(400, 400, 0)));
    }

    @Test
    public void declaredGridDecidesTheChannelCount() {
        long voxels = 200L * 100 * 50;

        Assert.assertEquals(VolumeType.DENSITY,
                            VolumeTextureLoader.detectVolumeTypeAndDimensions(voxels, new Trio<>(200, 100, 50)).type());
        Assert.assertEquals(VolumeType.COLOR,
                            VolumeTextureLoader.detectVolumeTypeAndDimensions(voxels * 3, new Trio<>(200, 100, 50)).type());
        Assert.assertEquals(VolumeType.RGBA,
                            VolumeTextureLoader.detectVolumeTypeAndDimensions(voxels * 4, new Trio<>(200, 100, 50)).type());
    }

    @Test
    public void cubicGridsStillLoadFromTheFileLengthAlone() {
        // Every volume written before the metadata carried a grid size, and every one of them is a
        // cube. 256³ keeps the arithmetic exact in a float.
        long voxels = 256L * 256 * 256;

        var density = VolumeTextureLoader.detectVolumeTypeAndDimensions(voxels, null);
        Assert.assertEquals(VolumeType.DENSITY, density.type());
        assertGrid(density.dimensions(), 256, 256, 256);

        var color = VolumeTextureLoader.detectVolumeTypeAndDimensions(voxels * 3, null);
        Assert.assertEquals(VolumeType.COLOR, color.type());
        assertGrid(color.dimensions(), 256, 256, 256);

        var rgba = VolumeTextureLoader.detectVolumeTypeAndDimensions(voxels * 4, null);
        Assert.assertEquals(VolumeType.RGBA, rgba.type());
        assertGrid(rgba.dimensions(), 256, 256, 256);
    }

    @Test
    public void lengthsThatAreNoVolumeAtAllAreRejected() {
        assertCannotDetect(1001);
        assertCannotDetect(0);
    }

    private static void assertGrid(Trio<Integer, Integer, Integer> grid, int width, int height, int depth) {
        Assert.assertNotNull(grid);
        Assert.assertEquals(width, grid.getFirst().intValue());
        Assert.assertEquals(height, grid.getSecond().intValue());
        Assert.assertEquals(depth, grid.getThird().intValue());
    }

    private static void assertCannotDetect(long fileLength) {
        assertFails(() -> VolumeTextureLoader.detectVolumeTypeAndDimensions(fileLength, null));
    }

    private static void assertFails(Runnable action) {
        try {
            action.run();
            Assert.fail("expected a GdxRuntimeException");
        } catch (GdxRuntimeException expected) {
            Assert.assertFalse(expected.getMessage().isBlank());
        }
    }
}
