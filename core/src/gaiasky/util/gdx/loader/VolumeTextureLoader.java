/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.loader;

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
import gaiasky.util.gdx.graphics.VolumeTexture;
import gaiasky.util.gdx.graphics.VolumeType;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Utility class for loading .raw volume files into LibGDX {@link Texture3D} objects, wrapped into {@link VolumeTexture}s.
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
     * Auto-detect volume type and dimensions from file
     */
    private static VolumeDetectionResult detectVolumeTypeAndDimensions(byte[] data) {
        // Try as density first (single channel)
        int densityVoxels = data.length;
        int densityDimension = (int) Math.round(Math.cbrt(densityVoxels));

        if (densityDimension * densityDimension * densityDimension == densityVoxels) {
            return new VolumeDetectionResult(VolumeType.DENSITY,
                                             new Trio<>(densityDimension, densityDimension, densityDimension));
        }

        // Try as color (three channels)
        int colorVoxels = data.length / 3;
        int colorDimension = (int) Math.round(Math.cbrt(colorVoxels));

        if (colorDimension * colorDimension * colorDimension == colorVoxels) {
            return new VolumeDetectionResult(VolumeType.COLOR,
                                             new Trio<>(colorDimension, colorDimension, colorDimension));
        }

        throw new GdxRuntimeException("Cannot detect volume format. File size: " + data.length +
                                              " bytes. Must be a perfect cube for density (n³) or color (3*n³)");
    }

    /**
     * Load bounds from metadata file
     */
    private static Vector3[] loadBoundsFromMetadata(FileHandle volumeFile) {
        // Remove _density.raw or _color.raw suffix
        var volumePath = volumeFile.path();
        String basePath = volumePath
                .replace("_density.raw", "")
                .replace("_color.raw", "")
                .replace(".raw", ""); // Fallback

        FileHandle metadataFile = new FileHandle(basePath + "_metadata.txt");

        if (!metadataFile.exists()) {
            // Try without replacing .raw in case it's already removed
            metadataFile = new FileHandle(volumeFile.path() + "_metadata.txt");
        }

        if (!metadataFile.exists()) {
            logger.warn("No metadata file found for: " + volumeFile.name() + ". Using default bounds.");
            return getDefaultBounds();
        }

        try {
            String metadata = metadataFile.readString();
            Vector3 boundsMin = new Vector3();
            Vector3 boundsMax = new Vector3();

            // Parse metadata - look for lines like:
            // World bounds: [-3.359, -3.375, -0.345] to [3.365, 3.359, 0.329]
            String[] lines = metadata.split("\n");
            for (String line : lines) {
                if (line.contains("World bounds:") || line.contains("Volume bounds:")) {
                    // Extract the bounds string
                    String boundsStr = line.substring(line.indexOf('['));
                    String[] parts = boundsStr.split(" to ");

                    if (parts.length == 2) {
                        // Parse min bounds
                        String minStr = parts[0].replace("[", "").replace("]", "").trim();
                        String[] minCoords = minStr.split(",");
                        boundsMin.set(
                                Float.parseFloat(minCoords[0].trim()),
                                Float.parseFloat(minCoords[1].trim()),
                                Float.parseFloat(minCoords[2].trim())
                        );

                        // Parse max bounds
                        String maxStr = parts[1].replace("[", "").replace("]", "").trim();
                        String[] maxCoords = maxStr.split(",");
                        boundsMax.set(
                                Float.parseFloat(maxCoords[0].trim()),
                                Float.parseFloat(maxCoords[1].trim()),
                                Float.parseFloat(maxCoords[2].trim())
                        );

                        logger.info("Loaded bounds from metadata: " + boundsMin + " to " + boundsMax);
                        return new Vector3[]{boundsMin, boundsMax};
                    }
                }
            }

            logger.warn("Could not parse bounds from metadata file: " + metadataFile.name());
            return getDefaultBounds();

        } catch (Exception e) {
            logger.error("Error reading metadata file: " + metadataFile.name(), e);
            return getDefaultBounds();
        }
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
        byte[] fileData = file.readBytes();

        // Auto-detect type and dimensions
        var detection = detectVolumeTypeAndDimensions(fileData);
        VolumeType type = detection.type;
        Trio<Integer, Integer, Integer> dimensions = detection.dimensions;
        int width = dimensions.getFirst();
        int height = dimensions.getSecond();
        int depth = dimensions.getThird();

        logger.info("Auto-detected volume: " + type + " " + width + "x" + height + "x" + depth);

        // Validate size
        int channels = (type == VolumeType.COLOR) ? 3 : 1;
        int expectedSize = width * height * depth * channels;
        if (fileData.length != expectedSize) {
            throw new IOException(String.format(
                    "Volume file size mismatch. Expected %d bytes (%dx%dx%dx%d) but got %d bytes",
                    expectedSize, width, height, depth, channels, fileData.length
            ));
        }

        // Load bounds from metadata
        Vector3[] bounds = loadBoundsFromMetadata(file);
        Vector3 boundsMin = bounds[0];
        Vector3 boundsMax = bounds[1];

        // Create Texture3DData
        CustomTexture3DData textureData = new CustomTexture3DData(
                width, height, depth,
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
    private record VolumeDetectionResult(VolumeType type, Trio<Integer, Integer, Integer> dimensions) {
    }

    private record VolumeLoadResult(CustomTexture3DData data, VolumeType type, Vector3 boundsMin, Vector3 boundsMax) {
    }

    static public class VolumeTextureParameter extends AssetLoaderParameters<VolumeTexture> {
        /** the format of the final Texture. Uses the source images format if null **/
        public Pixmap.Format format = null;
        /** The {@link Texture3DData}, optional. **/
        public Texture3DData data = null;
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
