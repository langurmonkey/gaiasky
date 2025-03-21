/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.provider;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.RenderAssets;
import gaiasky.util.Settings;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gdx.shader.loader.ShaderTemplatingLoader;

public class ShaderProgramProvider extends AsynchronousAssetLoader<ExtShaderProgram, ShaderProgramProvider.ShaderProgramParameter> {

    private String vertexFileSuffix = ".vertex.glsl";
    private String geometryFileSuffix = ".geometry.glsl";
    private String fragmentFileSuffix = ".fragment.glsl";

    public ShaderProgramProvider(FileHandleResolver resolver) {
        super(resolver);
    }

    public ShaderProgramProvider(FileHandleResolver resolver,
                                 String vertexFileSuffix,
                                 String geometryFileSuffix,
                                 String fragmentFileSuffix) {
        super(resolver);
        this.vertexFileSuffix = vertexFileSuffix;
        this.geometryFileSuffix = geometryFileSuffix;
        this.fragmentFileSuffix = fragmentFileSuffix;
    }

    public ShaderProgramProvider(FileHandleResolver resolver,
                                 String vertexFileSuffix,
                                 String fragmentFileSuffix) {
        this(resolver, vertexFileSuffix, null, fragmentFileSuffix);
    }

    static public String getShaderCode(String prefix,
                                       String code) {
        if (code == null) {
            return null;
        }
        if (prefix == null) {
            return code;
        }
        code = code.trim();
        if (code.startsWith("#version") && !prefix.isEmpty()) {
            int firstLineEnd = code.indexOf('\n') + 1;
            String versionStr = code.substring(0, firstLineEnd);
            return versionStr + prefix + code.substring(firstLineEnd);
        } else {
            return prefix + code;
        }
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName,
                                                  FileHandle file,
                                                  ShaderProgramParameter parameter) {
        return null;
    }

    @Override
    public void loadAsync(AssetManager manager,
                          String fileName,
                          FileHandle file,
                          ShaderProgramParameter parameter) {
    }

    @Override
    public ExtShaderProgram loadSync(AssetManager manager,
                                     String fileName,
                                     FileHandle file,
                                     ShaderProgramParameter parameter) {
        String vertFileName = null, geomFileName = null, fragFileName = null;
        if (parameter != null) {
            if (parameter.vertexFile != null)
                vertFileName = parameter.vertexFile;
            if (parameter.geometryFile != null)
                geomFileName = parameter.geometryFile;
            if (parameter.fragmentFile != null)
                fragFileName = parameter.fragmentFile;
        }
        // Try to work out shader file names.
        if (vertFileName == null && fileName.endsWith(fragmentFileSuffix)) {
            vertFileName = fileName.substring(0, fileName.length() - fragmentFileSuffix.length()) + vertexFileSuffix;
        }
        if (fragFileName == null && fileName.endsWith(vertexFileSuffix)) {
            fragFileName = fileName.substring(0, fileName.length() - vertexFileSuffix.length()) + fragmentFileSuffix;
        }
        FileHandle vertexFile = vertFileName == null ? file : resolve(vertFileName);
        FileHandle geometryFile = geomFileName == null ? null : resolve(geomFileName);
        FileHandle fragmentFile = fragFileName == null ? file : resolve(fragFileName);
        String vertexCode = ShaderTemplatingLoader.load(vertexFile);
        String geometryCode = ShaderTemplatingLoader.load(geometryFile);
        String fragmentCode = vertexFile.equals(fragmentFile) ? vertexCode : ShaderTemplatingLoader.load(fragmentFile);
        if (parameter != null) {
            if (parameter.prependVertexCode != null)
                vertexCode = getShaderCode(parameter.prependVertexCode, vertexCode);
            if (parameter.prependGeometryCode != null && geometryCode != null)
                geometryCode = getShaderCode(parameter.prependGeometryCode, geometryCode);
            if (parameter.prependFragmentCode != null)
                fragmentCode = getShaderCode(parameter.prependFragmentCode, fragmentCode);
        }

        // Lazy-load deactivated shaders (relativistic and gravitational waves) and off-by-default modes (motion blur and SSR).
        boolean lazyLoad = parameter != null && parameter.name != null
                && (parameter.name.contains(RenderAssets.SUFFIX_REL)
                || parameter.name.contains(RenderAssets.SUFFIX_GRAV)
                || (parameter.name.contains(RenderAssets.SUFFIX_SSR) && !Settings.settings.postprocess.ssr.active)
        );

        ExtShaderProgram shaderProgram;
        if (geometryCode != null) {
            shaderProgram = new ExtShaderProgram(parameter.name, vertFileName, geomFileName, fragFileName, vertexCode, geometryCode, fragmentCode, lazyLoad);
        } else {
            shaderProgram = new ExtShaderProgram(parameter != null ? parameter.name : null, vertFileName, fragFileName, vertexCode, fragmentCode, lazyLoad);
        }

        if ((parameter == null || parameter.logOnCompileFailure) && !shaderProgram.isCompiled()) {
            manager.getLogger().error("ExtShaderProgram " + fileName + " failed to compile:\n" + shaderProgram.getLog());
        }

        return shaderProgram;
    }

    static public class ShaderProgramParameter extends AssetLoaderParameters<ExtShaderProgram> {
        /** Name of the shader. Optional. **/
        public String name;
        /**
         * File name to be used for the vertex program instead of the default determined by the file name used to submit this asset
         * to AssetManager.
         */
        public String vertexFile;
        /**
         * File name to be used for the geometry shader. If null, no geometry stage is created.
         */
        public String geometryFile;
        /**
         * File name to be used for the fragment program instead of the default determined by the file name used to submit this
         * asset to AssetManager.
         */
        public String fragmentFile;
        /** Whether to log (at the error level) the shader's log if it fails to compile. Default true. */
        public boolean logOnCompileFailure = true;
        /**
         * Code that is always added to the vertex shader code. This is added as-is, and you should include a newline (`\n`) if
         * needed. {@linkplain ExtShaderProgram#prependVertexCode} is placed before this code.
         */
        public String prependVertexCode;
        /**
         * Code that is always added to the geometry shader code. This is added as-is, and you should include a newline (`\n`) if
         * needed. {@linkplain ExtShaderProgram#prependGeometryCode} is placed before this code.
         */
        public String prependGeometryCode;
        /**
         * Code that is always added to the fragment shader code. This is added as-is, and you should include a newline (`\n`) if
         * needed. {@linkplain ExtShaderProgram#prependFragmentCode} is placed before this code.
         */
        public String prependFragmentCode;
    }
}
