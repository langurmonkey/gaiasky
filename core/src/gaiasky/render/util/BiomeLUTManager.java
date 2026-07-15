/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture3D;
import com.badlogic.gdx.graphics.glutils.CustomTexture3DData;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.util.FastStringObjectMap;
import gaiasky.util.Logger;
import gaiasky.util.Trio;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BiomeLUTManager {
    private static final Logger.Log logger = Logger.getLogger(BiomeLUTManager.class);

    /** Filename pattern: {@code biome_lut_{preset}_{NN}.(jpg|png)}. */
    private static final Pattern LUT_PATTERN = Pattern.compile("biome_lut_(\\w+)_(\\d+)\\.(?:jpg|png)$");

    private final Array<String> names = new Array<>(7);
    /** Cache for 3D textures ({@link Texture3D}). **/
    private final FastStringObjectMap<Texture3D> cache3d = new FastStringObjectMap<>();
    /** Cache for 2D textures ({@link Texture}). **/
    private final FastStringObjectMap<Texture> cache2d = new FastStringObjectMap<>();
    /** Cache for paths ({@link FileHandle}). **/
    private final FastStringObjectMap<FileHandle> cachePath = new FastStringObjectMap<>();
    private final Path basePath;
    private boolean initialized = false;

    /**
     * Create a manager that looks for LUT files under the given path.
     * The path is first resolved as an absolute path, then as an internal path.
     *
     * @param basePath Directory containing the LUT JPG files.
     */
    public BiomeLUTManager(Path basePath) {
        this.basePath = basePath;
    }

    /**
     * Create a manager using the default path {@code tex/lut}.
     */
    public BiomeLUTManager() {
        this(GaiaSky.settings().data.dataPath("default-data/tex/lut"));
    }

    /**
     * Scan the base directory, group files by preset name, and load each
     * preset as a {@link Texture3D}.
     */
    public void loadAll() {
        if (initialized)
            return;

        FileHandle dir = resolveDirectory();
        if (dir == null) {
            logger.error("Biome LUT directory not found: " + basePath);
            return;
        }

        // Group files by preset name.
        Map<String, List<SliceFile>> presets = new LinkedHashMap<>();
        for (FileHandle file : dir.list()) {
            Matcher m = LUT_PATTERN.matcher(file.name());
            if (m.matches()) {
                String preset = m.group(1);
                int sliceIndex = Integer.parseInt(m.group(2));
                presets.computeIfAbsent(preset, k -> new ArrayList<>())
                        .add(new SliceFile(sliceIndex, file));
            }
        }

        if (presets.isEmpty()) {
            logger.warn("No biome LUT files found in: " + dir.path());
            return;
        }

        // Load each preset.
        for (Map.Entry<String, List<SliceFile>> entry : presets.entrySet()) {
            String preset = entry.getKey();
            List<SliceFile> slices = entry.getValue();
            Collections.sort(slices);

            try {
                var pair = loadPreset(preset, slices);
                if (pair != null) {
                    // Add 3D texture.
                    var tex3d = pair.getFirst();
                    if (tex3d != null) {
                        cache3d.put(preset, tex3d);
                    }
                    // Add 2D texture and pixmap.
                    var tex2d = pair.getSecond();
                    if (tex2d != null) {
                        cache2d.put(preset, tex2d);
                    }
                    var path = pair.getThird();
                    if (path != null) {
                        cachePath.put(preset, path);
                    }
                    // Add name.
                    names.add(preset);
                } else {
                    logger.error("Error loading biome LUT preset '" + preset + "': null pair");
                }
            } catch (Exception e) {
                logger.error("Error loading biome LUT preset '" + preset + "': " + e.getMessage());
            }
        }
        // Sort names list.
        names.sort();

        logger.info("Loaded " + cache3d.size() + " biome LUT preset(s): " + names.toString("|"));
        initialized = true;
    }

    /**
     * Get a loaded LUT 3D texture by preset name.
     *
     * @param presetName e.g. {@code "earthlike"}, {@code "desert"}.
     *
     * @return The {@link Texture3D}, or {@code null} if not loaded.
     */
    public Texture3D getLUT3D(String presetName) {
        return cache3d.get(presetName);
    }

    /**
     * Get a loaded LUT 2D texture by preset name.
     *
     * @param presetName e.g. {@code "earthlike"}, {@code "desert"}.
     *
     * @return The {@link Texture}, or {@code null} if not loaded.
     */
    public Texture getLUT2D(String presetName) {
        return cache2d.get(presetName);
    }

    /**
     * Get a loaded LUT token path by preset name.
     *
     * @param presetName e.g. {@code "earthlike"}, {@code "desert"}.
     *
     * @return The {@link FileHandle}, or {@code null} if not loaded.
     */
    public FileHandle getLUTPath(String presetName) {
        return cachePath.get(presetName);
    }

    /**
     * Get all loaded preset names.
     */
    public Array<String> getPresetNames() {
        return names;
    }

    public int getSize() {
        return names.size;
    }

    /**
     * Dispose all loaded textures and clear the cache.
     */
    public void dispose() {
        for (String key : names) {
            var t3d = cache3d.get(key);
            if (t3d != null) {
                t3d.dispose();
            }
            var t2d = cache2d.get(key);
            if (t2d != null) {
                t2d.dispose();
            }
            names.clear();
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Resolve the base directory. Tries absolute first, then internal (asset).
     */
    private FileHandle resolveDirectory() {
        // Try as absolute path.
        FileHandle dir = Gdx.files.absolute(basePath.toAbsolutePath().toString());
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        }
        // Try as internal path (relative to assets root).
        dir = Gdx.files.internal(basePath.toAbsolutePath().toString());
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        }
        return null;
    }

    /**
     * Load a single preset from its sorted list of slice files.
     */
    private Trio<Texture3D, Texture, FileHandle> loadPreset(String preset,
                                                        List<SliceFile> slices) {
        if (slices.isEmpty()) {
            return null;
        }

        int depth = slices.size();

        // Load all pixmaps, verifying consistent dimensions.
        List<Pixmap> pixmaps = new ArrayList<>(depth);
        int width = -1;
        int height = -1;

        // This is the index of the (2D) texture we'll use to display the LUT in the GUI.
        int texIndex = depth / 2;
        FileHandle path = null;
        Texture texture2d = null;

        for (SliceFile sf : slices) {
            Pixmap pix = new Pixmap(sf.file);
            if (width == -1) {
                width = pix.getWidth();
                height = pix.getHeight();
            } else if (pix.getWidth() != width || pix.getHeight() != height) {
                logger.warn("Preset '" + preset + "' has inconsistent slice dimensions: " +
                                    pix.getWidth() + "x" + pix.getHeight() + " != " + width + "x" + height +
                                    " for slice " + sf.index + ", skipping preset");
                for (Pixmap p : pixmaps) p.dispose();
                pix.dispose();
                return null;
            }
            pixmaps.add(pix);

            // Create 2D texture if needed.
            if (sf.index == texIndex) {
                path = sf.file;
                texture2d = new Texture(sf.file);
            }
        }

        // Build the 3D texture data (RGB, unsigned byte).
        // Pixel format is always RGB (3 channels) for biome LUTs.
        int channels = 3;
        CustomTexture3DData textureData = new CustomTexture3DData(
                width, height, depth,
                0, // mipMapLevel
                GL30.GL_RGB,
                GL30.GL_RGB8,
                GL30.GL_UNSIGNED_BYTE
        );

        ByteBuffer dst = textureData.getPixels();
        dst.clear();

        // Copy each slice into the 3D buffer.
        // Pixmap format may be RGB888 or RGBA8888 — handle both.
        for (int z = 0; z < depth; z++) {
            Pixmap pix = pixmaps.get(z);
            ByteBuffer src = pix.getPixels();
            src.rewind();

            int pixelCount = width * height;

            if (pix.getFormat() == Pixmap.Format.RGBA8888) {
                // RGBA → RGB: drop alpha.
                for (int i = 0; i < pixelCount; i++) {
                    byte r = src.get();
                    byte g = src.get();
                    byte b = src.get();
                    src.get(); // skip alpha
                    dst.put(r);
                    dst.put(g);
                    dst.put(b);
                }
            } else {
                // Assume RGB or similar — copy directly.
                int sliceBytes = pixelCount * channels;
                for (int i = 0; i < sliceBytes; i++) {
                    dst.put(src.get());
                }
            }
        }

        dst.flip();

        // Create the 3D texture.
        Texture3D texture3d = new Texture3D(textureData);
        texture3d.setWrap(Texture.TextureWrap.ClampToEdge,
                          Texture.TextureWrap.ClampToEdge,
                          Texture.TextureWrap.ClampToEdge);
        texture3d.setFilter(Texture.TextureFilter.Linear,
                            Texture.TextureFilter.Linear);

        // Free pixmaps.
        for (Pixmap p : pixmaps) {
            p.dispose();
        }

        logger.info("Loaded biome LUT preset '" + preset + "': " + width + "x" + height + "x" + depth + " RGB");
        return new Trio<>(texture3d, texture2d, path);
    }

    /** Simple value holder for a file and its slice index. */
    private static class SliceFile implements Comparable<SliceFile> {
        final int index;
        final FileHandle file;

        SliceFile(int index,
                  FileHandle file) {
            this.index = index;
            this.file = file;
        }

        @Override
        public int compareTo(SliceFile o) {
            return Integer.compare(this.index, o.index);
        }
    }
}