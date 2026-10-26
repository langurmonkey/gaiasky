/*
 * Copyright (c) 2025-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.loader;

import com.badlogic.gdx.math.Vector3;
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

    @Test
    public void parsesWhatTheGeneratorActuallyWrites() {
        // Verbatim from `particles-to-3dtex.py --grid-size 60x15x60`, so that the generator
        // and the loader stay pinned to each other. The '#' lines have no key and must be
        // skipped, as must the extra keys (Galactic grid, Data bounds, ...); the grid is a
        // vector in storage order, which is the order the file is indexed in.
        String generated = """
                # Grid, world size, bounds and voxel size are in storage order (W, H, D)
                # = galactic (Y, Z, X), the order the file is indexed in.
                Channel: galaxy_Gas.dat.gz
                Grid size: 60x15x60
                Galactic grid (x, y, z): 60x60x15
                World size: [7.058, 7.071, 0.704]
                Data bounds: [-3.348, -3.372, -0.343] to [3.373, 3.363, 0.328]
                Volume bounds: [-3.540, -0.359, -3.516] to [3.532, 0.345, 3.541]
                Voxel size: [0.118, 0.118, 0.047]
                Has color: True
                Original particle count: 14864
                Formats: rgba
                Color gain: 21.203079 (mode: p999)
                """;

        var metadata = VolumeTextureLoader.parseMetadata(generated);

        assertGrid(metadata.grid(), 60, 15, 60);
        Assert.assertEquals(-3.540f, metadata.boundsMin().x, 1e-6f);
        Assert.assertEquals(-0.359f, metadata.boundsMin().y, 1e-6f);
        Assert.assertEquals(-3.516f, metadata.boundsMin().z, 1e-6f);
        Assert.assertEquals(3.532f, metadata.boundsMax().x, 1e-6f);
        Assert.assertEquals(0.345f, metadata.boundsMax().y, 1e-6f);
        Assert.assertEquals(3.541f, metadata.boundsMax().z, 1e-6f);

        // And the grid the metadata declares is the one the file length implies.
        var detection = VolumeTextureLoader.detectVolumeTypeAndDimensions(
                60L * 15 * 60 * 4, metadata.grid());
        Assert.assertEquals(VolumeType.RGBA, detection.type());
        assertGrid(detection.dimensions(), 60, 15, 60);

        // A scalar grid, as the cubic runs wrote it before per-axis grids existed, still
        // reads as a cube.
        assertGrid(VolumeTextureLoader.parseMetadata("Grid size: 40\n").grid(), 40, 40, 40);
    }

    @Test
    public void theTexturePutsTheFilesLastAxisFirst() {
        // glTexImage3D varies its first axis fastest, and a .raw file varies its last one
        // fastest, so the extents GL is told about are the file's in reverse. The most common
        // shape of all - a cube - reverses to itself, which is why this went unnoticed.
        assertGrid(VolumeTextureLoader.glGrid(new Trio<>(400, 400, 400)), 400, 400, 400);
        assertGrid(VolumeTextureLoader.glGrid(new Trio<>(60, 15, 60)), 60, 15, 60);
        assertGrid(VolumeTextureLoader.glGrid(new Trio<>(400, 40, 399)), 399, 40, 400);
        assertGrid(VolumeTextureLoader.glGrid(new Trio<>(200, 100, 50)), 50, 100, 200);
    }

    @Test
    public void theBoxIsReversedWithTheGrid() {
        // The box has to be turned into the texture's frame by the same permutation as the grid,
        // or the thin axis of a flat volume is normalised over the wide extent of the texture.
        Vector3 box = VolumeTextureLoader.glBounds(new Vector3(-3.540f, -0.359f, -3.516f));

        Assert.assertEquals(-3.516f, box.x, 1e-6f);
        Assert.assertEquals(-0.359f, box.y, 1e-6f);
        Assert.assertEquals(-3.540f, box.z, 1e-6f);

        // A cubic box is its own reverse, so nothing written before non-cubic grids existed
        // changes: the two calls must be indistinguishable for every cube.
        Vector3 cube = VolumeTextureLoader.glBounds(new Vector3(-3.0718f, -3.0718f, -3.0718f));
        Assert.assertEquals(cube.x, -3.0718f, 1e-6f);
        Assert.assertEquals(cube.y, -3.0718f, 1e-6f);
        Assert.assertEquals(cube.z, -3.0718f, 1e-6f);
    }

    @Test
    public void gridAndBoxStayPairedThroughTheReversal() {
        // The shader normalises each axis over the box's own component, so whichever extent is
        // first in the box must be the extent associated with the grid's first number. Reversing
        // both keeps them paired; reversing one is what stretches the volume.
        var grid = VolumeTextureLoader.glGrid(new Trio<>(400, 40, 399));
        Vector3 min = VolumeTextureLoader.glBounds(new Vector3(-3.540f, -0.359f, -3.516f));
        Vector3 max = VolumeTextureLoader.glBounds(new Vector3(3.532f, 0.345f, 3.541f));

        // Every voxel of a grid generated with a single --voxel-size is a cube, and the pairing
        // is what says so: 7.072/400, 0.704/40 and 7.057/399 all come out at 0.0177.
        float voxelX = (max.x - min.x) / grid.getFirst();
        float voxelY = (max.y - min.y) / grid.getSecond();
        float voxelZ = (max.z - min.z) / grid.getThird();
        Assert.assertEquals(voxelX, voxelY, 1e-4f);
        Assert.assertEquals(voxelY, voxelZ, 1e-4f);
    }

    @Test
    public void theDensityChannelIsWhereTheShaderReadsIt() {
        // volume.fragment.glsl takes the density from .r in a density-only volume and from .a in a
        // colour+density one, and gates every use of a voxel's colour behind a density test. A
        // colour-only volume therefore has no density to measure, and nothing to crop.
        Assert.assertEquals(0, VolumeTextureLoader.densityChannelOffset(VolumeType.DENSITY));
        Assert.assertEquals(3, VolumeTextureLoader.densityChannelOffset(VolumeType.RGBA));
        Assert.assertEquals(-1, VolumeTextureLoader.densityChannelOffset(VolumeType.COLOR));
    }

    @Test
    public void contentBoundsAreTheFilledBoxPaddedByOneVoxel() {
        // A 4x3x5 RGBA grid with material at (1, 1, 2) and (2, 1, 3) only, i.e. inside the grid on
        // both ends of every axis, so every side gets its padding.
        int w = 4, h = 3, d = 5, channels = 4, density = 3;
        byte[] data = new byte[w * h * d * channels];
        data[voxel(w, h, d, channels, density, 1, 1, 2)] = 10;
        data[voxel(w, h, d, channels, density, 2, 1, 3)] = 10;

        Assert.assertArrayEquals(new int[]{0, 3, 0, 2, 1, 4},
                                 VolumeTextureLoader.contentVoxelBounds(data, w, h, d, channels, density));
    }

    @Test
    public void contentBoundsStopAtTheGridEdge() {
        // Material in the first voxel of the grid leaves nothing to pad toward, and the bounds have
        // to stay inside the grid rather than point at voxels that do not exist.
        int w = 4, h = 3, d = 5, channels = 4, density = 3;
        byte[] data = new byte[w * h * d * channels];
        data[voxel(w, h, d, channels, density, 0, 0, 0)] = 1;

        Assert.assertArrayEquals(new int[]{0, 1, 0, 1, 0, 1},
                                 VolumeTextureLoader.contentVoxelBounds(data, w, h, d, channels, density));
    }

    @Test
    public void anEmptyGridHasNoContentBounds() {
        // Which the caller reads as "nothing to crop", not as an empty box: a volume with no
        // material at all is left on the box its metadata declares.
        Assert.assertNull(VolumeTextureLoader.contentVoxelBounds(new byte[4 * 3 * 5 * 4], 4, 3, 5, 4, 3));
        Assert.assertNull(VolumeTextureLoader.contentVoxelBounds(new byte[4 * 3 * 5], 4, 3, 5, 1, 0));
    }

    @Test
    public void onlyTheDensityChannelDecidesWhatIsContent() {
        // A voxel with no density is never sampled for its colour, so a filled colour channel is
        // not material and must not keep the empty shell alive.
        int w = 2, h = 2, d = 2, channels = 4, density = 3;
        byte[] data = new byte[w * h * d * channels];
        for (int i = 0; i < data.length; i += channels) {
            data[i] = (byte) 255;
            data[i + 1] = (byte) 255;
            data[i + 2] = (byte) 255;
        }

        Assert.assertNull(VolumeTextureLoader.contentVoxelBounds(data, w, h, d, channels, density));
    }

    @Test
    public void cropMovesWholeRowsInFileOrder() {
        // The file's fastest axis is contiguous, so a cropped row is a copy of a run of the source;
        // every byte has to land at the offset it had relative to the voxel, or the texture is
        // uploaded with its channels shifted.
        int w = 4, h = 3, d = 5, channels = 4;
        byte[] data = new byte[w * h * d * channels];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i & 0x7f);
        }

        // The box the content-bounds test above produces for this grid.
        int[] bounds = {0, 3, 0, 2, 1, 4};
        byte[] cropped = VolumeTextureLoader.crop(data, w, h, d, channels, bounds);

        Assert.assertEquals(4 * 3 * 4 * channels, cropped.length);
        for (int c = 0; c < channels; c++) {
            int from = voxel(w, h, d, channels, c, 2, 1, 3);
            int to = voxel(4, 3, 4, channels, c, 2, 1, 2);
            Assert.assertEquals(data[from], cropped[to]);
        }
    }

    @Test
    public void theBrickGridRoundsUpPerAxis() {
        // Each axis is ceiled on its own, so a partial brick at the far edge still gets a texel.
        assertGrid(VolumeTextureLoader.brickGrid(400, 40, 399), 100, 10, 100);
        assertGrid(VolumeTextureLoader.brickGrid(601, 60, 602), 151, 15, 151);
    }

    @Test
    public void anEmptyVolumeOccupiesNoBrick() {
        // 8x8x8 is 2x2x2 bricks of four voxels.
        int channels = 4, density = 3;
        byte[] occupancy = VolumeTextureLoader.brickOccupancy(new byte[8 * 8 * 8 * channels], 8, 8, 8, channels, density);

        Assert.assertNotNull(occupancy);
        Assert.assertEquals(8, occupancy.length);
        for (byte b : occupancy) {
            Assert.assertEquals(0, b);
        }
    }

    @Test
    public void aVoxelMarksTheBricksItsTrilinearNeighbourhoodReaches() {
        // The brick grid is not a coarse copy of the voxel grid: a sample one voxel inside a brick
        // still interpolates with the first voxel of the neighbouring brick, so a voxel on a brick
        // boundary has to mark both bricks or the skip will cut a one-voxel hole in the extinction
        // wherever two bricks meet. This is the test that pins that dilation.
        int channels = 4, density = 3, size = 8;
        byte[] data = new byte[size * size * size * channels];

        // Brick (0,0,0), i.e. index (0*2 + 0)*2 + 0, on its own.
        data[voxel(size, size, size, channels, density, 0, 0, 0)] = 1;
        Assert.assertArrayEquals(new byte[]{-1, 0, 0, 0, 0, 0, 0, 0},
                                 VolumeTextureLoader.brickOccupancy(data, size, size, size, channels, density));

        // Voxel 3 is the last of brick 0 and voxel 4 the first of brick 1: every brick is within one
        // voxel of it.
        data = new byte[size * size * size * channels];
        data[voxel(size, size, size, channels, density, 3, 3, 3)] = 1;
        for (byte b : VolumeTextureLoader.brickOccupancy(data, size, size, size, channels, density)) {
            Assert.assertEquals(-1, b);
        }

        // Voxel 7 is the last of brick 1, with nothing beyond it: brick (1,1,1) alone.
        data = new byte[size * size * size * channels];
        data[voxel(size, size, size, channels, density, 7, 7, 7)] = 1;
        Assert.assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, -1},
                                 VolumeTextureLoader.brickOccupancy(data, size, size, size, channels, density));
    }

    @Test
    public void aColourWithoutDensityOccupiesNoBrick() {
        // The shader gates every use of a voxel's colour behind a density test, so a filled colour
        // channel is not material and must not keep the empty shell alive.
        int channels = 4, density = 3, size = 8;
        byte[] data = new byte[size * size * size * channels];
        for (int i = 0; i < data.length; i += channels) {
            data[i] = (byte) 255;
        }
        for (byte b : VolumeTextureLoader.brickOccupancy(data, size, size, size, channels, density)) {
            Assert.assertEquals(0, b);
        }

        // And a format that stores no density has no grid to build at all.
        Assert.assertNull(VolumeTextureLoader.brickOccupancy(data, size, size, size, channels, -1));
    }

    @Test
    public void densityOnlyVolumesAreMeasuredFromTheirOnlyChannel() {
        int channels = 1, density = 0;
        byte[] data = new byte[4 * 4 * 4];
        data[voxel(4, 4, 4, channels, density, 2, 2, 2)] = 7;

        byte[] occupancy = VolumeTextureLoader.brickOccupancy(data, 4, 4, 4, channels, density);

        Assert.assertNotNull(occupancy);
        Assert.assertEquals(1, occupancy.length);
        Assert.assertEquals(-1, occupancy[0]);
    }

    @Test
    public void aBrickEdgeThatDoesNotDivideTheGridMarksTheRightBricks() {
        // The brick grid has ceil(V / brick) bricks, so the brick a position falls in is
        // floor(uvw * bricks), not floor(v / brick) - the two only agree when the brick edge
        // divides V. Here it does not: 182 voxels is 46 bricks of four, and a sample that reads
        // voxel 150 covers uvw [149.5/182, 151.5/182), i.e. bricks 37 to 38 - while floor(150/4)
        // is 37 on its own. Marking from the voxel index rather than through the grid would leave
        // brick 38 unmarked and punch a hole in the volume wherever two such bricks meet.
        int width = 182, channels = 1, density = 0;
        byte[] data = new byte[width];
        data[150] = 1;

        byte[] occupancy = VolumeTextureLoader.brickOccupancy(data, width, 1, 1, channels, density);

        Assert.assertNotNull(occupancy);
        Assert.assertEquals(46, occupancy.length);
        for (int i = 0; i < occupancy.length; i++) {
            Assert.assertEquals("brick " + i, i == 37 || i == 38 ? -1 : 0, occupancy[i]);
        }
    }

    @Test
    public void aVolumeWhoseBoxIsTheDataExtentIsFlagged() {
        // Verbatim from the shipped gal-400 volume: the box is the data extent grown by 5%, so the
        // bounds span 0.9524 of the World size on every axis. The shader takes those bounds as the
        // box, which draws the volume 5% too large and 2.3% of the object's size off-centre, so the
        // loader says so rather than rendering it in silence.
        String metadata = """
                Grid size: 399x40x400
                World size: [0.998040, 0.099562, 1.000000]
                Volume bounds: [-0.473301, -0.048570, -0.475536] to [0.477213, 0.046252, 0.476845]
                Data bounds: [-0.473301, -0.476845, -0.048570] to [0.477213, 0.475536, 0.046252]
                """;

        Assert.assertTrue(VolumeTextureLoader.boxIsNotTheGridExtent(
                VolumeTextureLoader.parseMetadata(metadata)));
    }

    @Test
    public void aVolumeWhoseBoxIsTheGridExtentIsAccepted() {
        // What particles-to-volume.py writes now: 'Volume bounds' is the box itself, so it spans
        // exactly the World size and there is nothing to warn about.
        String metadata = """
                Grid size: 400x40x400
                World size: [0.999524, 0.099483, 1.000000]
                Volume bounds: [-0.498776, -0.050900, -0.498997] to [0.500748, 0.048583, 0.501003]
                Data bounds (galactic, inside the box): [-0.475693, -0.474751, -0.047386] to \
                [0.477517, 0.475373, 0.047376]
                """;

        Assert.assertFalse(VolumeTextureLoader.boxIsNotTheGridExtent(
                VolumeTextureLoader.parseMetadata(metadata)));
    }

    @Test
    public void aRawUnitsVolumeIsFlagged() {
        // The convention before normalization, whose bounds double as a size multiplier of ~7 on
        // the object. It cannot be rendered as it stands either, so it is flagged too.
        String metadata = """
                Grid size: 60x15x60
                World size: [7.058, 7.071, 0.704]
                Volume bounds: [-3.540, -0.359, -3.516] to [3.532, 0.345, 3.541]
                """;

        Assert.assertTrue(VolumeTextureLoader.boxIsNotTheGridExtent(
                VolumeTextureLoader.parseMetadata(metadata)));
    }

    @Test
    public void aMetadataWithoutAWorldSizeCannotBeChecked() {
        var metadata = VolumeTextureLoader.parseMetadata(
                "Grid size: 400x40x400\nVolume bounds: [-0.4, -0.04, -0.4] to [0.4, 0.04, 0.4]\n");

        Assert.assertNull(metadata.boxSize());
        Assert.assertFalse(VolumeTextureLoader.boxIsNotTheGridExtent(metadata));
    }

    /**
     * The byte offset of a channel within a voxel, in the file's C-order (W, H, D, channels).
     */
    private static int voxel(int w, int h, int d, int channels, int channel, int x, int y, int z) {
        return ((x * h + y) * d + z) * channels + channel;
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
