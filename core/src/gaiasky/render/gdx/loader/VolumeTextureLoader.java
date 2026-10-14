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

            return new VolumeTexture(texture, parameter.data.getWidth(), parameter.data.getHeight(),
                                     parameter.data.getDepth(), parameter.type, parameter.boundsMin, parameter.boundsMax);
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

        return new VolumeLoadResult(textureData, type, boundsMin, boundsMax);
    }

    // Helper classes
    record VolumeDetectionResult(VolumeType type, Trio<Integer, Integer, Integer> dimensions) {
    }

    private record VolumeLoadResult(CustomTexture3DData data, VolumeType type, Vector3 boundsMin, Vector3 boundsMax) {
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
        public Texture.TextureFilter magFilter = Texture.TextureFilter.Linear;
        public Texture.TextureFilter minFilter = Texture.TextureFilter.Linear;
        public Texture.TextureWrap wrapU = Texture.TextureWrap.ClampToEdge;
        public Texture.TextureWrap wrapV = Texture.TextureWrap.ClampToEdge;
        public Texture.TextureWrap wrapR = Texture.TextureWrap.ClampToEdge;
    }
}
