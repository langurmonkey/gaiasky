/*
 * Copyright (c) 2025-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.loader;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture3D;
import com.badlogic.gdx.graphics.Texture3DData;
import com.badlogic.gdx.graphics.glutils.CustomTexture3DData;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import gaiasky.util.Logger;
import gaiasky.util.Trio;
import gaiasky.render.gdx.graphics.VolumeTexture;
import gaiasky.render.gdx.graphics.VolumeType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Loads 3D volume textures.
 * <p>
 * A volume is a density (R8), colour (RGB8) or colour+density (RGBA8) grid, stored raw and
 * gzip-compressed, with a {@code _metadata.txt} sidecar that gives the world-space box and, since
 * non-cubic grids are supported, the {@code Grid size:}. The metadata's grid is authoritative when
 * present, because a byte count alone can only ever yield a cube.
 * <p>
 * Everything the metadata describes - grid, box - is in the file's own (W, H, D) order. The texture
 * GL ends up with indexes those axes in reverse, because {@code glTexImage3D} varies its first axis
 * fastest while a C-order array varies its last one fastest. {@link #glGrid} and {@link #glBounds}
 * apply that permutation, and they have to be applied together. See {@link #glGrid} for why this
 * only ever bites on a non-cubic grid.
 */
public class VolumeTextureLoader extends AsynchronousAssetLoader<VolumeTexture, VolumeTextureLoader.VolumeTextureParameter> {
    private static final Logger.Log logger = Logger.getLogger(VolumeTextureLoader.class);

    public VolumeTextureLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, VolumeTextureParameter parameter) {
        try {
            var result = loadVolumeTexture(file);
            parameter.data = result.data;
            parameter.type = result.type;
            parameter.boundsMin = result.boundsMin;
            parameter.boundsMax = result.boundsMax;
            parameter.occupancy = result.occupancy;
            parameter.occupancyWidth = result.occupancyWidth;
            parameter.occupancyHeight = result.occupancyHeight;
            parameter.occupancyDepth = result.occupancyDepth;
        } catch (IOException e) {
            logger.error("Error loading volume texture: " + file, e);
            parameter.data = null;
        }
    }

    @Override
    public VolumeTexture loadSync(AssetManager manager, String fileName, FileHandle file, VolumeTextureParameter parameter) {
        if (parameter.data != null && parameter.type != null) {
            // Create Texture3D
            Texture3D texture = new Texture3D(parameter.data);

            // Set texture parameters for volume rendering
            texture.setWrap(parameter.wrapU, parameter.wrapV, parameter.wrapR);
            texture.setFilter(parameter.minFilter, parameter.magFilter);

            // The occupancy grid, if the volume has one. Nearest and clamped regardless of what the
            // volume asked for: the shader is asking which brick a position is in, and an
            // interpolated answer to that question is not an answer - it would blend two brick
            // maxima across the boundary and skip a brick that has content in it.
            Texture3D occupancy = null;
            if (parameter.occupancy != null) {
                var occupancyData = new CustomTexture3DData(
                        parameter.occupancyWidth, parameter.occupancyHeight, parameter.occupancyDepth,
                        0, // mipMapLevel
                        VolumeType.DENSITY.glFormat,
                        VolumeType.DENSITY.glInternalFormat,
                        VolumeType.DENSITY.glType);
                ByteBuffer occupancyPixels = occupancyData.getPixels();
                occupancyPixels.put(parameter.occupancy);
                occupancyPixels.flip();

                occupancy = new Texture3D(occupancyData);
                occupancy.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge,
                                 Texture.TextureWrap.ClampToEdge);
                occupancy.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }

            return new VolumeTexture(texture, parameter.data.getWidth(), parameter.data.getHeight(),
                                     parameter.data.getDepth(), parameter.type, parameter.boundsMin, parameter.boundsMax,
                                     occupancy);
        }
        return null;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, VolumeTextureParameter parameter) {
        return null;
    }

    /**
     * Auto-detect the volume format and the grid resolution.
     * <p>
     * When the sidecar metadata declares a {@code Grid size:} the resolution comes from there and
     * only the number of channels is derived from the file length. That is the only way to load a
     * grid that is not a cube: the number of voxels says nothing about how they are split between
     * the axes, so cube-rooting the length can only ever recover cubic grids, and a flattened
     * 400x400x40 volume is simply unclassifiable (its 6.4 M voxels have no integral cube root).
     * Non-cubic volumes are worth having because a galaxy disk is flat - a 400x400x40 grid occupies
     * a tenth of the memory of 400³ (24 MiB against 244 MiB for RGBA) and fits in cache, while
     * keeping the same voxel size.
     * <p>
     * Without a declared grid the resolution is inferred from the length as a cube, which is what
     * every volume written before the grid line existed is.
     * <p>
     * Package-private and free of I/O so that the resolution and channel logic can be tested
     * directly, without a volume file or a GL context.
     *
     * @param fileLength   The length of the decompressed volume data, in bytes.
     * @param declaredGrid The grid resolution declared by the metadata, or null.
     *
     * @return The detected volume type and resolution.
     */
    static VolumeDetectionResult detectVolumeTypeAndDimensions(long fileLength, Trio<Integer, Integer, Integer> declaredGrid) {
        if (fileLength <= 0) {
            throw new GdxRuntimeException("Volume has no data (file length " + fileLength + " bytes).");
        }
        if (declaredGrid != null) {
            long width = declaredGrid.getFirst();
            long height = declaredGrid.getSecond();
            long depth = declaredGrid.getThird();
            if (width <= 0 || height <= 0 || depth <= 0) {
                throw new GdxRuntimeException("Volume metadata declares an empty grid: " +
                                                      width + "x" + height + "x" + depth);
            }
            long voxels = width * height * depth;
            if (fileLength % voxels != 0) {
                throw new GdxRuntimeException("Volume file size does not match the grid declared by its metadata: " +
                                                      "grid " + width + "x" + height + "x" + depth + " is " + voxels +
                                                      " voxels, so the file must be a multiple of " + voxels +
                                                      " bytes, but it is " + fileLength + " bytes.");
            }
            long channels = fileLength / voxels;
            VolumeType type = typeForChannels(channels);
            if (type == null) {
                throw new GdxRuntimeException("Volume metadata declares grid " + width + "x" + height + "x" + depth +
                                                      ", which makes the file " + fileLength + " bytes = " + channels +
                                                      " bytes per voxel. Expected 1 (density), 3 (color) or 4 (color+density).");
            }
            return new VolumeDetectionResult(type, new Trio<>((int) width, (int) height, (int) depth));
        }

        // No declared grid: infer a cube from the file length. Try as density first (single
        // channel), then color (three), then color+density (four). No file length can satisfy two
        // of the three cases at once (n³ = 4k³ and 3m³ = 4k³ both require a rational cube root of
        // 4 or 4/3), so the order of the tests does not matter.
        int densityDimension = (int) Math.round(Math.cbrt(fileLength));
        if ((long) densityDimension * densityDimension * densityDimension == fileLength) {
            return new VolumeDetectionResult(VolumeType.DENSITY,
                                             new Trio<>(densityDimension, densityDimension, densityDimension));
        }

        long colorVoxels = fileLength / 3;
        int colorDimension = (int) Math.round(Math.cbrt(colorVoxels));
        if ((long) colorDimension * colorDimension * colorDimension == colorVoxels) {
            return new VolumeDetectionResult(VolumeType.COLOR,
                                             new Trio<>(colorDimension, colorDimension, colorDimension));
        }

        long rgbaVoxels = fileLength / 4;
        int rgbaDimension = (int) Math.round(Math.cbrt(rgbaVoxels));
        if ((long) rgbaDimension * rgbaDimension * rgbaDimension == rgbaVoxels) {
            return new VolumeDetectionResult(VolumeType.RGBA,
                                             new Trio<>(rgbaDimension, rgbaDimension, rgbaDimension));
        }

        throw new GdxRuntimeException("Cannot detect volume format. File size: " + fileLength +
                                              " bytes. Must be a perfect cube for density (n³), color (3*n³) or " +
                                              "color+density (4*n³), or the metadata must declare a 'Grid size:'.");
    }

    /**
     * Map a byte-per-voxel count to a storage format.
     *
     * @param channels Bytes per voxel.
     *
     * @return The format, or null if the count is not one the loader can bind.
     */
    private static VolumeType typeForChannels(long channels) {
        return switch ((int) channels) {
            case 1 -> VolumeType.DENSITY;
            case 3 -> VolumeType.COLOR;
            case 4 -> VolumeType.RGBA;
            default -> null;
        };
    }

    /**
     * The grid to declare to {@link CustomTexture3DData}, given the grid the metadata describes.
     * <p>
     * This reverses the three numbers, and the reason is the memory layout. A volume file is a
     * C-order dump of an (W, H, D, C) array, so its fastest varying spatial axis is the <em>last</em>
     * one, D. {@code glTexImage3D} reads its client buffer with the <em>first</em> axis, width,
     * varying fastest. Declaring the extents in the file's own order therefore reads the buffer with
     * the wrong stride: GL's x advances one element at a time down a file that only advances a whole
     * D line every W elements, so every row is sheared by one element relative to the last and the
     * volume comes out as a fan of slabs - and it does so silently, because the buffer is exactly the
     * right length.
     * <p>
     * While every grid was a cube this was invisible: with W = D the two orders are the same three
     * numbers, and the shear degenerates into a clean transposition of the data. A transposition of a
     * cube is a cube, so the volume merely rendered with its axes permuted, and an axisymmetric gas
     * disk hides a permutation of its own plane. The first non-cubic grid ever loaded was the first
     * one to be visibly wrong.
     * <p>
     * Declaring the extents reversed makes GL's x the file's last axis, GL's y the middle one and
     * GL's z the first: a clean transposition, which is the read every cubic volume has always had.
     * It is a no-op for a cube (three equal numbers reverse to themselves), so no existing volume
     * changes.
     * <p>
     * Package-private and free of I/O so the rule can be tested directly.
     *
     * @param grid The grid as the metadata describes it, i.e. in the file's (W, H, D) order.
     *
     * @return The grid in texture order, (D, H, W), to hand to {@link CustomTexture3DData}.
     */
    static Trio<Integer, Integer, Integer> glGrid(Trio<Integer, Integer, Integer> grid) {
        return new Trio<>(grid.getThird(), grid.getSecond(), grid.getFirst());
    }

    /**
     * The box the shader compares against, given the box the metadata describes.
     * <p>
     * The shader turns a position into texture coordinates component-wise, so its x normalises over
     * the texture's x extent, which by {@link #glGrid} is the file's <em>last</em> axis. The metadata
     * says that extent first. The box is therefore reversed for the same reason the grid is, and by
     * the same permutation, so that the two stay consistent: reversing one without the other would
     * map the box onto the wrong axes, which on a flat volume stretches the thin axis over the wide
     * one.
     * <p>
     * A no-op for a cubic box, which is what every volume written before non-cubic grids existed
     * has (to within the sub-voxel wobble of one that is not exactly centred on the origin).
     *
     * @param bounds The box as the metadata describes it, in the file's (W, H, D) order.
     *
     * @return The box in texture order, (D, H, W).
     */
    static Vector3 glBounds(Vector3 bounds) {
        return new Vector3(bounds.z, bounds.y, bounds.x);
    }

    /**
     * Grid sizes accepted in the metadata's {@code Grid size:} line: either a single edge, which
     * means a cube, or three axes separated by {@code x}, {@code *} or {@code ,}
     * ({@code 400x400x40}, {@code 400 * 400 * 40}, {@code 400, 400, 40}).
     */
    private static final Pattern GRID_VECTOR = Pattern.compile("^\\s*(\\d+)\\s*[xX*,\\s]\\s*(\\d+)\\s*[xX*,\\s]\\s*(\\d+)\\s*$");
    private static final Pattern GRID_SCALAR = Pattern.compile("^\\s*(\\d+)\\s*$");

    /**
     * Parse a {@code Grid size:} value.
     *
     * @param value The value as written in the metadata, without the key.
     *
     * @return The grid resolution, or null if the value is not understood.
     */
    static Trio<Integer, Integer, Integer> parseGridSize(String value) {
        if (value == null) {
            return null;
        }
        Matcher vector = GRID_VECTOR.matcher(value);
        if (vector.matches()) {
            return new Trio<>(Integer.parseInt(vector.group(1)),
                              Integer.parseInt(vector.group(2)),
                              Integer.parseInt(vector.group(3)));
        }
        Matcher scalar = GRID_SCALAR.matcher(value);
        if (scalar.matches()) {
            int edge = Integer.parseInt(scalar.group(1));
            return new Trio<>(edge, edge, edge);
        }
        return null;
    }

    /**
     * Suffixes appended to the channel basename by particles-to-3dtex.py.
     * Longest first: the first match wins, so ".raw.gz" cannot shadow "_rgba.raw.gz".
     */
    private static final String[] VOLUME_SUFFIXES = {
            "_density.raw.gz", "_color.raw.gz", "_rgba.raw.gz",
            "_density.raw", "_color.raw", "_rgba.raw",
            ".raw.gz", ".raw"
    };

    /**
     * Strip the volume file suffix from a path, recovering the channel basename.
     *
     * Only a suffix that terminates the *file name* is removed, and only one of
     * them. The previous chained String.replace() ran over the whole path, so a
     * directory whose name happened to contain ".raw" or "_color.raw" was mangled
     * even when the file name itself was fine.
     */
    private static String volumeBasename(String path) {
        int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String dir = path.substring(0, lastSeparator + 1);
        String name = path.substring(lastSeparator + 1);

        for (String suffix : VOLUME_SUFFIXES) {
            if (name.length() > suffix.length() && name.endsWith(suffix)) {
                return dir + name.substring(0, name.length() - suffix.length());
            }
        }
        return dir + name;
    }

    /**
     * A volume's sidecar metadata: the grid resolution, when the file declares one, and the
     * world-space box. The box always has a value, defaulting to the unit cube.
     */
    record VolumeMetadata(Trio<Integer, Integer, Integer> grid, Vector3 boundsMin, Vector3 boundsMax) {
    }

    /**
     * Matches the value of a {@code Volume bounds:} / {@code World bounds:} line, i.e.
     * <code>[-3.523, -3.540, -3.543] to [3.548, 3.532, 3.528]</code>. Both the generator and the
     * combiner write this shape.
     */
    private static final Pattern BOUNDS_PATTERN = Pattern.compile(
            "\\[\\s*(-?[0-9.eE+]+)\\s*,\\s*(-?[0-9.eE+]+)\\s*,\\s*(-?[0-9.eE+]+)\\s*]\\s*to\\s*" +
                    "\\[\\s*(-?[0-9.eE+]+)\\s*,\\s*(-?[0-9.eE+]+)\\s*,\\s*(-?[0-9.eE+]+)\\s*]");

    /**
     * Read a volume's sidecar metadata, falling back to defaults when it is missing or unreadable.
     *
     * @param volumeFile The volume file.
     *
     * @return The metadata, never null. The grid is null when the metadata is missing or does not
     * declare one, which means the caller has to infer the resolution from the file length.
     */
    private static VolumeMetadata loadMetadata(FileHandle volumeFile) {
        // Remove the _density/_color/_rgba .raw[.gz] suffix to get the channel basename
        String basePath = volumeBasename(volumeFile.path());

        FileHandle metadataFile = new FileHandle(basePath + "_metadata.txt");

        if (!metadataFile.exists()) {
            // Try without replacing .raw in case it's already removed
            metadataFile = new FileHandle(volumeFile.path() + "_metadata.txt");
        }

        if (!metadataFile.exists()) {
            logger.warn("No metadata file found for: " + volumeFile.name() +
                                ". Using default bounds and inferring the grid from the file size.");
            return new VolumeMetadata(null, getDefaultBounds()[0], getDefaultBounds()[1]);
        }

        try {
            return parseMetadata(metadataFile.readString());
        } catch (Exception e) {
            logger.error("Error reading metadata file: " + metadataFile.name(), e);
            return new VolumeMetadata(null, getDefaultBounds()[0], getDefaultBounds()[1]);
        }
    }

    /**
     * Parse a volume metadata file.
     * <p>
     * Package-private and free of I/O so that the parsing can be tested directly, without a volume
     * file or a GL context.
     *
     * @param metadata The metadata file contents.
     *
     * @return The parsed metadata. The grid is null when no <code>Grid size:</code> line is present
     * or parseable; the bounds fall back to the unit cube.
     */
    static VolumeMetadata parseMetadata(String metadata) {
        Trio<Integer, Integer, Integer> grid = null;
        Vector3[] bounds = getDefaultBounds();

        for (String line : metadata.split("\\R")) {
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();

            switch (key) {
                case "grid size" -> {
                    Trio<Integer, Integer, Integer> parsed = parseGridSize(value);
                    if (parsed != null) {
                        grid = parsed;
                    } else {
                        logger.warn("Could not parse the grid size from metadata: '" + value + "'");
                    }
                }
                // "Data bounds" is the extent of the particles, not of the box, and is ignored.
                case "volume bounds", "world bounds" -> {
                    Matcher matcher = BOUNDS_PATTERN.matcher(value);
                    if (matcher.find()) {
                        bounds[0].set(Float.parseFloat(matcher.group(1)),
                                      Float.parseFloat(matcher.group(2)),
                                      Float.parseFloat(matcher.group(3)));
                        bounds[1].set(Float.parseFloat(matcher.group(4)),
                                      Float.parseFloat(matcher.group(5)),
                                      Float.parseFloat(matcher.group(6)));
                    } else {
                        logger.warn("Could not parse the bounds from metadata: '" + value + "'");
                    }
                }
                default -> {
                }
            }
        }

        return new VolumeMetadata(grid, bounds[0], bounds[1]);
    }

    /**
     * Get default bounds for when no metadata is available
     */
    private static Vector3[] getDefaultBounds() {
        // Default to unit cube centered at origin
        return new Vector3[]{
                new Vector3(-0.5f, -0.5f, -0.5f),
                new Vector3(0.5f, 0.5f, 0.5f)
        };
    }

    /**
     * Load a .raw volume file and create a Texture3D
     */
    private VolumeLoadResult loadVolumeTexture(FileHandle file) throws IOException {
        if (!file.exists()) {
            throw new IOException("Volume file not found: " + file.path());
        }
        byte[] fileData;

        if (file.extension().equalsIgnoreCase("gz")) {
            // Decompress gzipped .raw.gz file
            try (var is = new GZIPInputStream(file.read())) {
                fileData = is.readAllBytes();
                logger.info("Decompressed gzipped volume file: " + file.name() + " (" + fileData.length + " bytes)");
            } catch (IOException e) {
                throw new IOException("Failed to decompress gzipped volume file: " + file.path(), e);
            }
        } else {
            // Regular .raw file
            fileData = file.readBytes();
        }

        // Read the metadata first: it is what declares the grid resolution, and without it the
        // resolution can only be inferred as a cube from the file length.
        VolumeMetadata metadata = loadMetadata(file);

        // Auto-detect type and dimensions
        var detection = detectVolumeTypeAndDimensions(fileData.length, metadata.grid());
        VolumeType type = detection.type;
        Trio<Integer, Integer, Integer> dimensions = detection.dimensions;
        int width = dimensions.getFirst();
        int height = dimensions.getSecond();
        int depth = dimensions.getThird();

        logger.info("Auto-detected volume: " + type + " " + width + "x" + height + "x" + depth
                            + (metadata.grid() != null ? " (grid from metadata)" : " (grid from file size)"));

        // Validate size. Redundant after detection, which derives the channel count from the same
        // numbers, but it is the last guard before the data is handed to GL.
        int channels = switch (type) {
            case DENSITY -> 1;
            case COLOR -> 3;
            case RGBA -> 4;
        };
        long expectedSize = (long) width * height * depth * channels;
        if (fileData.length != expectedSize) {
            throw new IOException(String.format(
                    "Volume file size mismatch. Expected %d bytes (%dx%dx%dx%d) but got %d bytes",
                    expectedSize, width, height, depth, channels, fileData.length
            ));
        }

        // Crop the grid to the box that holds any material at all. A voxel with no density
        // contributes nothing to the image - no emission, and no extinction, so no occlusion either
        // - which means removing the empty shell around a volume cannot change what it draws, while
        // it buys three things at once: the box the shader intersects (and so the span every ray
        // marches) shrinks, the same TARGET_STEP_UVW advances fewer voxels, and the texture costs
        // less memory. A galaxy volume spends most of its grid on that shell, including the margin
        // the generator leaves around the data, none of which has to be traversed. It is also what
        // lets volume.fragment.glsl take a coarser step in its depth pre-pass: what matters there
        // is the step in voxels, and the pre-pass now searches the dust's own box rather than the
        // whole volume's.
        //
        // The one voxel of padding is not cosmetic. A sample between the last non-empty voxel and
        // an empty one interpolates toward zero, and that empty voxel has to still be there for the
        // interpolated value to come out the same; one voxel of reach is all trilinear filtering
        // has. The box moves with the data (see the bounds below), so nothing on screen shifts.
        int densityOffset = densityChannelOffset(type);
        if (densityOffset >= 0) {
            int[] content = contentVoxelBounds(fileData, width, height, depth, channels, densityOffset);
            if (content != null && (content[0] > 0 || content[1] < width - 1
                                            || content[2] > 0 || content[3] < height - 1
                                            || content[4] > 0 || content[5] < depth - 1)) {
                // The box is measured in voxel edges, so voxel i covers [i, i + 1) and the cropped
                // box is the first and last index of the content plus one at the far end.
                float voxelX = (metadata.boundsMax().x - metadata.boundsMin().x) / width;
                float voxelY = (metadata.boundsMax().y - metadata.boundsMin().y) / height;
                float voxelZ = (metadata.boundsMax().z - metadata.boundsMin().z) / depth;

                Vector3 croppedMin = new Vector3(metadata.boundsMin().x + content[0] * voxelX,
                                                 metadata.boundsMin().y + content[2] * voxelY,
                                                 metadata.boundsMin().z + content[4] * voxelZ);
                Vector3 croppedMax = new Vector3(metadata.boundsMin().x + (content[1] + 1) * voxelX,
                                                 metadata.boundsMin().y + (content[3] + 1) * voxelY,
                                                 metadata.boundsMin().z + (content[5] + 1) * voxelZ);
                int croppedWidth = content[1] - content[0] + 1;
                int croppedHeight = content[3] - content[2] + 1;
                int croppedDepth = content[5] - content[4] + 1;

                logger.info("Cropping " + file.name() + " from " + width + "x" + height + "x" + depth
                                    + " to " + croppedWidth + "x" + croppedHeight + "x" + croppedDepth
                                    + ": the rest of the grid has no density.");

                fileData = crop(fileData, width, height, depth, channels, content);
                width = croppedWidth;
                height = croppedHeight;
                depth = croppedDepth;
                dimensions = new Trio<>(croppedWidth, croppedHeight, croppedDepth);
                metadata = new VolumeMetadata(dimensions, croppedMin, croppedMax);
            }
        }

        // The file's own order (W, H, D) is not the order GL reads it in, and the box has to be
        // turned into the same frame as the texture, so the two go together: see glGrid and
        // glBounds. Everything above this line is about the file; everything below is about the
        // texture.
        Trio<Integer, Integer, Integer> glGrid = glGrid(dimensions);
        Vector3 boundsMin = glBounds(metadata.boundsMin());
        Vector3 boundsMax = glBounds(metadata.boundsMax());

        // The box and the grid are independent, so the voxels need not be cubes. Worth saying out
        // loud: an anisotropic grid renders correctly (the shader normalises over the box) but its
        // voxels are stretched, which shows as banding along the stretched axis.
        if (width != height || height != depth) {
            logger.info(String.format("Volume grid is not cubic: voxel size %.6f x %.6f x %.6f world units (texture x, y, z)",
                                      (boundsMax.x - boundsMin.x) / glGrid.getFirst(),
                                      (boundsMax.y - boundsMin.y) / glGrid.getSecond(),
                                      (boundsMax.z - boundsMin.z) / glGrid.getThird()));
        }

        // Create Texture3DData
        CustomTexture3DData textureData = new CustomTexture3DData(
                glGrid.getFirst(), glGrid.getSecond(), glGrid.getThird(),
                0, // mipMapLevel
                type.glFormat,
                type.glInternalFormat,
                type.glType
        );

        // Copy data to texture
        ByteBuffer pixels = textureData.getPixels();
        pixels.put(fileData);
        pixels.flip();

        // The occupancy grid, which the shader marches over to skip the empty parts of the box that
        // survived the crop: see brickOccupancy(). It is built from the file's own (W, H, D) order
        // and declared to GL with the same reversal as the voxels, so the two line up in the
        // shader's uvw space without either being transposed on its own.
        byte[] occupancy = brickOccupancy(fileData, width, height, depth, channels, densityOffset);
        int occupancyWidth = 0, occupancyHeight = 0, occupancyDepth = 0;
        if (occupancy != null) {
            Trio<Integer, Integer, Integer> brickGrid = brickGrid(width, height, depth);
            Trio<Integer, Integer, Integer> glBrickGrid = glGrid(brickGrid);
            occupancyWidth = glBrickGrid.getFirst();
            occupancyHeight = glBrickGrid.getSecond();
            occupancyDepth = glBrickGrid.getThird();
            logger.info("Volume occupancy grid: " + occupancyWidth + "x" + occupancyHeight + "x" + occupancyDepth +
                                " bricks of " + OCCUPANCY_BRICK + " voxels, " + occupancy.length + " bytes.");
        }

        return new VolumeLoadResult(textureData, type, boundsMin, boundsMax,
                                    occupancy, occupancyWidth, occupancyHeight, occupancyDepth);
    }

    /**
     * The byte within a voxel that holds the density, for the formats {@code particles-to-3dtex.py}
     * writes; -1 when the format stores no density.
     * <p>
     * This has to agree with the sampling in {@code volume.fragment.glsl}, which reads the density
     * from {@code .r} in a density-only (R8) volume and from {@code .a} in a colour+density (RGBA)
     * one. A colour-only volume carries no density at all - the shader gates every use of a voxel's
     * colour behind a density test, so its colour alone cannot be raymarched and there is nothing to
     * measure, let alone to crop.
     *
     * @param type The volume format.
     *
     * @return The density channel's byte offset in a voxel, or -1 if there is none.
     */
    static int densityChannelOffset(VolumeType type) {
        return switch (type) {
            case DENSITY -> 0;
            case RGBA -> 3;
            case COLOR -> -1;
        };
    }

    /**
     * The range of voxels that hold any density, per axis, inclusive on both ends, in the file's
     * (W, H, D) order, expanded by one voxel on every side that has room for it.
     * <p>
     * Empty is not the same as invisible: a zero voxel still costs a step and a fetch on every ray
     * that crosses it, and a flat volume is mostly zero voxels. Nothing outside this range can
     * contribute emission, extinction or depth, so the range is the only part of the grid worth
     * marching, sampling or storing.
     * <p>
     * Package-private and free of I/O so the rule can be tested directly.
     *
     * @param data          The volume's voxels, C-order (W, H, D, channels).
     * @param width         The grid's extent on the file's first axis.
     * @param height        The grid's extent on the file's second axis.
     * @param depth         The grid's extent on the file's third (fastest varying) axis.
     * @param channels      Bytes per voxel.
     * @param densityOffset The density byte within a voxel, from {@link #densityChannelOffset}.
     *
     * @return {@code {wMin, wMax, hMin, hMax, dMin, dMax}}, or null if every voxel is empty.
     */
    static int[] contentVoxelBounds(byte[] data, int width, int height, int depth, int channels, int densityOffset) {
        int wMin = width, wMax = -1, hMin = height, hMax = -1, dMin = depth, dMax = -1;

        int index = densityOffset;
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                for (int d = 0; d < depth; d++) {
                    if (data[index] != 0) {
                        if (w < wMin) {
                            wMin = w;
                        }
                        if (w > wMax) {
                            wMax = w;
                        }
                        if (h < hMin) {
                            hMin = h;
                        }
                        if (h > hMax) {
                            hMax = h;
                        }
                        if (d < dMin) {
                            dMin = d;
                        }
                        if (d > dMax) {
                            dMax = d;
                        }
                    }
                    index += channels;
                }
            }
        }

        if (wMax < 0) {
            return null;
        }

        // One voxel of padding, clamped to the grid: see the note where this is used.
        return new int[]{
                Math.max(0, wMin - 1), Math.min(width - 1, wMax + 1),
                Math.max(0, hMin - 1), Math.min(height - 1, hMax + 1),
                Math.max(0, dMin - 1), Math.min(depth - 1, dMax + 1)};
    }

    /**
     * Copy the sub-grid {@code bounds} out of a C-order voxel block, in the same order.
     * <p>
     * The file's fastest axis is contiguous, so a run of it is a single array copy; only the two
     * outer loops are walked. The result is the block a texture is uploaded from, one row at a time.
     * <p>
     * Package-private and free of I/O so the rule can be tested directly.
     *
     * @param data     The volume's voxels, C-order (W, H, D, channels).
     * @param width    The grid's extent on the file's first axis.
     * @param height   The grid's extent on the file's second axis.
     * @param depth    The grid's extent on the file's third (fastest varying) axis.
     * @param channels Bytes per voxel.
     * @param bounds   {@code {wMin, wMax, hMin, hMax, dMin, dMax}}, as {@link #contentVoxelBounds}
     *                 returns it.
     *
     * @return The cropped voxels, C-order, with the same channels per voxel.
     */
    static byte[] crop(byte[] data, int width, int height, int depth, int channels, int[] bounds) {
        int wMin = bounds[0], wMax = bounds[1], hMin = bounds[2], hMax = bounds[3], dMin = bounds[4], dMax = bounds[5];
        int rowLength = (dMax - dMin + 1) * channels;
        byte[] cropped = new byte[(wMax - wMin + 1) * (hMax - hMin + 1) * rowLength];

        int out = 0;
        for (int w = wMin; w <= wMax; w++) {
            for (int h = hMin; h <= hMax; h++) {
                int from = (int) ((((long) w * height + h) * depth + dMin) * channels);
                System.arraycopy(data, from, cropped, out, rowLength);
                out += rowLength;
            }
        }
        return cropped;
    }

    /**
     * The edge, in voxels, of one brick of the occupancy grid - the granularity at which
     * {@link #brickOccupancy} can skip empty space.
     * <p>
     * This is a trade: a smaller brick is a finer measure of where the material is and so skips
     * more, but costs more memory and more texels to fetch through. On a galaxy volume, four is
     * where the two curves cross - 4x4x4 leaves 76% of the dust's bricks empty for 8.8 kB of R8,
     * against 47% for 1.1 kB at 8x8x8, and the step counts follow the emptiness rather than the
     * memory.
     */
    static final int OCCUPANCY_BRICK = 4;

    /**
     * The resolution, in bricks, of the occupancy grid summarising a voxel grid of the given
     * resolution.
     * <p>
     * Rounded up per axis, so the last brick on a partial axis hangs over the edge of the grid.
     * The voxels that are not there read as zero, which is what an empty brick means anyway, so
     * the overhang only ever makes the grid more conservative than it needs to be.
     *
     * @param width  The grid's extent on the file's first axis.
     * @param height The grid's extent on the file's second axis.
     * @param depth  The grid's extent on the file's third (fastest varying) axis.
     *
     * @return The brick grid, in the same (W, H, D) order.
     */
    static Trio<Integer, Integer, Integer> brickGrid(int width, int height, int depth) {
        int b = OCCUPANCY_BRICK;
        return new Trio<>((width + b - 1) / b, (height + b - 1) / b, (depth + b - 1) / b);
    }

    /**
     * The occupancy grid of a volume: one byte per brick, 255 where that brick can be seen to hold
     * density and 0 where it certainly does not.
     * <p>
     * The march through a volume is bounded by the volume's box, and the box is already cropped to
     * the data, but that is all: inside the box every step fetches, whether or not there is
     * anything there to fetch, and a galaxy's dust fills 0.4% of its grid. A brick whose voxels
     * are all zero contributes nothing to the image - no emission, and no extinction, so no
     * occlusion either - so a ray that jumps over it cannot draw anything different. What it saves
     * is every fetch and every step the brick would have cost.
     * <p>
     * The load-bearing part is that a brick is marked when any sample that <em>reads</em> a non-zero
     * voxel can land in it, which is not the same as "a non-zero voxel is inside it". A trilinear
     * fetch at a point samples the eight voxels around it, so a sample reads the voxels within one
     * voxel of itself, and the samples that read voxel v span uvw {@code [(v-0.5)/V, (v+1.5)/V)} -
     * wider than the voxel, and it straddles both neighbouring voxels. Marking every brick that
     * range touches is what makes "this brick's texel is zero" mean "no fetch anywhere in this
     * brick can read a non-zero voxel", whatever the filtering does. Without that reach the dust
     * would acquire a thin halo of missing extinction along every brick boundary.
     * <p>
     * The range is mapped through the brick grid, not through a voxel count: the grid has
     * {@code ceil(V / OCCUPANCY_BRICK)} bricks, and dividing the two is only the same as
     * {@code v / OCCUPANCY_BRICK} when the brick edge divides V. When it does not, the two disagree
     * by a brick at either end, and a marked brick that is one short is a hole in the volume.
     * <p>
     * Returned in the same C-order (W, H, D) as {@code data}, for the caller to hand to GL with
     * the same axis reversal the voxels get: see {@link #glGrid}.
     * <p>
     * Package-private and free of I/O so the rule can be tested directly.
     *
     * @param data          The volume's voxels, C-order (W, H, D, channels).
     * @param width         The grid's extent on the file's first axis.
     * @param height        The grid's extent on the file's second axis.
     * @param depth         The grid's extent on the file's third (fastest varying) axis.
     * @param channels      Bytes per voxel.
     * @param densityOffset The density byte within a voxel, from {@link #densityChannelOffset}.
     *
     * @return The brick grid, {@code brickGrid(width, height, depth)} bytes of it, or null when the
     * format stores no density and so has nothing to skip.
     */
    static byte[] brickOccupancy(byte[] data, int width, int height, int depth, int channels, int densityOffset) {
        if (densityOffset < 0) {
            return null;
        }

        Trio<Integer, Integer, Integer> grid = brickGrid(width, height, depth);
        int bw = grid.getFirst();
        int bh = grid.getSecond();
        int bd = grid.getThird();
        byte[] occupancy = new byte[bw * bh * bd];

        // The brick span of the samples that read each voxel, per axis.
        int[] wFrom = new int[width], wTo = new int[width];
        for (int w = 0; w < width; w++) {
            wFrom[w] = brickFloor(2L * w - 1, 2L * width, bw);
            wTo[w] = brickCeil(2L * w + 3, 2L * width, bw);
        }
        int[] hFrom = new int[height], hTo = new int[height];
        for (int h = 0; h < height; h++) {
            hFrom[h] = brickFloor(2L * h - 1, 2L * height, bh);
            hTo[h] = brickCeil(2L * h + 3, 2L * height, bh);
        }
        int[] dFrom = new int[depth], dTo = new int[depth];
        for (int d = 0; d < depth; d++) {
            dFrom[d] = brickFloor(2L * d - 1, 2L * depth, bd);
            dTo[d] = brickCeil(2L * d + 3, 2L * depth, bd);
        }

        int index = densityOffset;
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                for (int d = 0; d < depth; d++, index += channels) {
                    if (data[index] == 0) {
                        continue;
                    }
                    for (int wb = wFrom[w]; wb <= wTo[w]; wb++) {
                        for (int hb = hFrom[h]; hb <= hTo[h]; hb++) {
                            for (int db = dFrom[d]; db <= dTo[d]; db++) {
                                occupancy[(wb * bh + hb) * bd + db] = (byte) 255;
                            }
                        }
                    }
                }
            }
        }

        return occupancy;
    }

    /**
     * The first brick a sample whose fetch reads the voxel at the given position can land in, i.e.
     * the floor of the low end of that sample's uvw range times the brick count.
     * <p>
     * Both arguments are in half-voxel units, so that the range's ends are exact: a sample reading
     * voxel {@code v} of {@code V} covers uvw {@code [(v - 0.5)/V, (v + 1.5)/V)}.
     */
    private static int brickFloor(long twiceLowEnd, long twiceVoxels, int bricks) {
        long brick = Math.floorDiv(twiceLowEnd * bricks, twiceVoxels);
        return (int) Math.max(0L, Math.min(bricks - 1L, brick));
    }

    /**
     * The last brick such a sample can land in. The uvw range is half-open at the top, so a
     * high end that lands exactly on a brick boundary leaves the brick below it: this is the
     * ceiling of the high end minus one.
     */
    private static int brickCeil(long twiceHighEnd, long twiceVoxels, int bricks) {
        long brick = Math.floorDiv(twiceHighEnd * bricks + twiceVoxels - 1, twiceVoxels) - 1;
        return (int) Math.max(0L, Math.min(bricks - 1L, brick));
    }

    // Helper classes
    record VolumeDetectionResult(VolumeType type, Trio<Integer, Integer, Integer> dimensions) {
    }

    /**
     * A loaded volume and the occupancy grid that summarises it. {@code occupancy} is null when
     * there is none to build, which the texture carries through as a null occupancy texture.
     */
    private record VolumeLoadResult(CustomTexture3DData data, VolumeType type, Vector3 boundsMin, Vector3 boundsMax,
                                    byte[] occupancy, int occupancyWidth, int occupancyHeight, int occupancyDepth) {
    }

    static public class VolumeTextureParameter extends AssetLoaderParameters<VolumeTexture> {
        /** the format of the final Texture. Uses the source images format if null **/
        public Pixmap.Format format;
        /** The {@link Texture3DData}, optional. **/
        public Texture3DData data;
        /** Volume type. **/
        public VolumeType type;
        /** Volume bounds in world space **/
        public Vector3 boundsMin;
        public Vector3 boundsMax;
        /** The brick occupancy grid of the volume, or null if it has none: see {@link #brickOccupancy}. **/
        public byte[] occupancy;
        /** The occupancy grid's resolution, in bricks, in texture order. **/
        public int occupancyWidth;
        public int occupancyHeight;
        public int occupancyDepth;
        public Texture.TextureFilter magFilter = Texture.TextureFilter.Linear;
        public Texture.TextureFilter minFilter = Texture.TextureFilter.Linear;
        public Texture.TextureWrap wrapU = Texture.TextureWrap.ClampToEdge;
        public Texture.TextureWrap wrapV = Texture.TextureWrap.ClampToEdge;
        public Texture.TextureWrap wrapR = Texture.TextureWrap.ClampToEdge;
    }
}
