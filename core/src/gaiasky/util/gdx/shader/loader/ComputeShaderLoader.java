/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.loader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.Logger;
import gaiasky.util.SysUtils;
import gaiasky.util.gdx.shader.ComputeShaderProgram;

import java.io.IOException;

/**
 * Loads compute shaders asynchronously.
 *
 * @param <T> The parameters class.
 */
public class ComputeShaderLoader<T extends ComputeShaderLoader.ComputeShaderParameter> extends AsynchronousAssetLoader<ComputeShaderProgram, T> {
    private static final Logger.Log logger = Logger.getLogger(ComputeShaderProgram.class);

    private String shaderCode;
    public ComputeShaderProgram computeShaderProgram;

    public ComputeShaderLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, T parameter) {
        var shaderFile = Gdx.files.internal(parameter.computeShaderFile);
        shaderCode = shaderFile.readString();
    }

    @Override
    public ComputeShaderProgram loadSync(AssetManager manager, String fileName, FileHandle file, T parameter) {
        try {
            if (!SysUtils.isComputeShaderSupported()) {
               throw new RuntimeException("Compute shaders not supported on this platform: OpenGL 4.3+ or ARB_compute_shader extension required");
            }
            computeShaderProgram = new ComputeShaderProgram(parameter.name, shaderCode);
            return computeShaderProgram;
        } catch (IOException e) {
            logger.error("Error creating compute shader: " + parameter.name, e);
            return null;
        }
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, T parameter) {
        return null;
    }

    public static class ComputeShaderParameter extends AssetLoaderParameters<ComputeShaderProgram> {
        public String name;
        public String computeShaderFile;

        public ComputeShaderParameter(String name, String computeShaderFile) {
            super();
            this.name = name;
            this.computeShaderFile = computeShaderFile;
        }
    }

}
