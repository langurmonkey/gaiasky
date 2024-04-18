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
import gaiasky.util.gdx.shader.TessellationShaderProgram;
import gaiasky.util.gdx.shader.loader.ShaderTemplatingLoader;

public class TessellationShaderProgramProvider extends AsynchronousAssetLoader<TessellationShaderProgram, TessellationShaderProgramProvider.ShaderProgramParameter> {

    private String vertexFileSuffix = ".vertex.glsl";
    private String controlFileSuffix = ".control.glsl";
    private String evaluationFileSuffix = ".eval.glsl";
    private String fragmentFileSuffix = ".fragment.glsl";

    public TessellationShaderProgramProvider(FileHandleResolver resolver) {
        super(resolver);
    }

    public TessellationShaderProgramProvider(FileHandleResolver resolver,
                                             String vertexFileSuffix,
                                             String controlFileSuffix,
                                             String evaluationFileSuffix,
                                             String fragmentFileSuffix) {
        super(resolver);
        this.vertexFileSuffix = vertexFileSuffix;
        this.controlFileSuffix = controlFileSuffix;
        this.evaluationFileSuffix = evaluationFileSuffix;
        this.fragmentFileSuffix = fragmentFileSuffix;
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
    public TessellationShaderProgram loadSync(AssetManager manager,
                                     String fileName,
                                     FileHandle file,
                                     ShaderProgramParameter parameter) {
        String vertFileName = null, controlFileName = null, evaluationFileName = null, fragFileName = null;
        if (parameter != null) {
            if (parameter.vertexFile != null)
                vertFileName = parameter.vertexFile;
            if (parameter.controlFile != null)
                controlFileName = parameter.controlFile;
            if (parameter.evaluationFile != null)
                evaluationFileName = parameter.evaluationFile;
            if (parameter.fragmentFile != null)
                fragFileName = parameter.fragmentFile;
        }
        if (vertFileName == null && fileName.endsWith(fragmentFileSuffix)) {
            vertFileName = fileName.substring(0, fileName.length() - fragmentFileSuffix.length()) + vertexFileSuffix;
        }
        if (fragFileName == null && fileName.endsWith(vertexFileSuffix)) {
            fragFileName = fileName.substring(0, fileName.length() - vertexFileSuffix.length()) + fragmentFileSuffix;
        }
        FileHandle vertexFile = vertFileName == null ? file : resolve(vertFileName);
        FileHandle controlFile = controlFileName == null ? null : resolve(controlFileName);
        FileHandle evaluationFile = evaluationFileName == null ? null : resolve(evaluationFileName);
        FileHandle fragmentFile = fragFileName == null ? file : resolve(fragFileName);
        String vertexCode = ShaderTemplatingLoader.load(vertexFile);
        String controlCode = ShaderTemplatingLoader.load(controlFile);
        String evaluationCode = ShaderTemplatingLoader.load(evaluationFile);
        String fragmentCode = ShaderTemplatingLoader.load(fragmentFile);
        if (parameter != null) {
            if (parameter.prependVertexCode != null)
                vertexCode = getShaderCode(parameter.prependVertexCode, vertexCode);
            if (parameter.prependControlCode != null && controlCode != null)
                controlCode = getShaderCode(parameter.prependControlCode, controlCode);
            if (parameter.prependEvaluationCode != null && evaluationCode != null)
                evaluationCode = getShaderCode(parameter.prependEvaluationCode, evaluationCode);
            if (parameter.prependFragmentCode != null)
                fragmentCode = getShaderCode(parameter.prependFragmentCode, fragmentCode);
        }

        // Lazy-load deactivated shaders (relativistic and gravitational waves) and off-by-default modes (motion blur and SSR).
        boolean lazyLoad = parameter != null && parameter.name != null
                && (parameter.name.contains(RenderAssets.SUFFIX_REL)
                || parameter.name.contains(RenderAssets.SUFFIX_GRAV)
                || (parameter.name.contains(RenderAssets.SUFFIX_SSR) && !Settings.settings.postprocess.ssr.active)
        );

        TessellationShaderProgram shaderProgram = new TessellationShaderProgram(
                parameter != null ? parameter.name : null,
                vertFileName,
                controlFileName,
                evaluationFileName,
                fragFileName,
                vertexCode,
                controlCode,
                evaluationCode,
                fragmentCode,
                lazyLoad);

        if ((parameter == null || parameter.logOnCompileFailure) && !shaderProgram.isCompiled()) {
            manager.getLogger().error("TessellationShaderProgram " + fileName + " failed to compile:\n" + shaderProgram.getLog());
        }

        return shaderProgram;
    }

    static public class ShaderProgramParameter extends AssetLoaderParameters<TessellationShaderProgram> {
        /** Name of the shader. Optional. **/
        public String name;
        /**
         * File name to be used for the vertex program instead of the default determined by the file name used to submit this asset
         * to AssetManager.
         */
        public String vertexFile;
        /**
         * File name to be used for the tessellation control shader.
         */
        public String controlFile;
        /**
         * File name to be used for the tessellation evaluation shader.
         */
        public String evaluationFile;
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
         * Code that is always added to the tessellation control shader code. This is added as-is, and you should include a newline (`\n`) if
         * needed. {@linkplain gaiasky.util.gdx.shader.TessellationShaderProgram#prependControlCode} is placed before this code.
         */
        public String prependControlCode;
        /**
         * Code that is always added to the tessellation evaluation shader code. This is added as-is, and you should include a newline (`\n`) if
         * needed. {@linkplain gaiasky.util.gdx.shader.TessellationShaderProgram#prependEvaluationCode} is placed before this code.
         */
        public String prependEvaluationCode;
        /**
         * Code that is always added to the fragment shader code. This is added as-is, and you should include a newline (`\n`) if
         * needed. {@linkplain ExtShaderProgram#prependFragmentCode} is placed before this code.
         */
        public String prependFragmentCode;
    }
}
