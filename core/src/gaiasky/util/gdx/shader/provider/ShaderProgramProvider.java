/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.provider;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AssetLoader;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.RenderAssets;
import gaiasky.util.Settings;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gdx.shader.loader.ShaderTemplatingLoader;

/**
 * {@link AssetLoader} for {@link ExtShaderProgram} instances loaded from text files. If the file suffix is ".vert", it is assumed
 * to be a vertex shader, and a fragment shader is found using the same file name with a ".frag" suffix. And vice versa if the
 * file suffix is ".frag". These default suffixes can be changed in the ShaderProgramLoader constructor.
 * <p>
 * For all other file suffixes, the same file is used for both (and therefore should internally distinguish between the programs
 * using preprocessor directives and {@link ExtShaderProgram#prependVertexCode} and {@link ExtShaderProgram#prependFragmentCode}).
 * <p>
 * The above default behavior for finding the files can be overridden by explicitly setting the file names in a
 * {@link ShaderProgramParameter}. The parameter can also be used to prepend code to the programs.
 *
 * @author cypherdare
 */
public class ShaderProgramProvider extends AsynchronousAssetLoader<ExtShaderProgram, ShaderProgramProvider.ShaderProgramParameter> {

    private String vertexFileSuffix = ".vertex.glsl";
    private String fragmentFileSuffix = ".fragment.glsl";

    public ShaderProgramProvider(FileHandleResolver resolver) {
        super(resolver);
    }

    public ShaderProgramProvider(FileHandleResolver resolver, String vertexFileSuffix, String fragmentFileSuffix) {
        super(resolver);
        this.vertexFileSuffix = vertexFileSuffix;
        this.fragmentFileSuffix = fragmentFileSuffix;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, ShaderProgramParameter parameter) {
        return null;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, ShaderProgramParameter parameter) {
    }

    @Override
    public ExtShaderProgram loadSync(AssetManager manager, String fileName, FileHandle file, ShaderProgramParameter parameter) {
        String vertFileName = null, fragFileName = null;
        if (parameter != null) {
            if (parameter.vertexFile != null)
                vertFileName = parameter.vertexFile;
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
        FileHandle fragmentFile = fragFileName == null ? file : resolve(fragFileName);
        String vertexCode = ShaderTemplatingLoader.load(vertexFile);
        String fragmentCode = vertexFile.equals(fragmentFile) ? vertexCode : ShaderTemplatingLoader.load(fragmentFile);
        if (parameter != null) {
            if (parameter.prependVertexCode != null)
                vertexCode = getShaderCode(parameter.prependVertexCode, vertexCode);
            if (parameter.prependFragmentCode != null)
                fragmentCode = getShaderCode(parameter.prependFragmentCode, fragmentCode);
        }

        // Lazy-load deactivated shaders (relativistic and gravitational waves) and off-by-default modes (motion blur and SSR).
        boolean lazyLoad = parameter != null && parameter.name != null
                && (parameter.name.contains(RenderAssets.SUFFIX_REL)
                || parameter.name.contains(RenderAssets.SUFFIX_GRAV)
                || (parameter.name.contains(RenderAssets.SUFFIX_VELBUFF) && !Settings.settings.postprocess.motionBlur.active)
                || (parameter.name.contains(RenderAssets.SUFFIX_SSR) && !Settings.settings.postprocess.ssr.active)
        );

        ExtShaderProgram shaderProgram = new ExtShaderProgram(parameter != null ? parameter.name : null, vertFileName, fragFileName, vertexCode, fragmentCode, lazyLoad);
        if ((parameter == null || parameter.logOnCompileFailure) && !shaderProgram.isCompiled()) {
            manager.getLogger().error("ExtShaderProgram " + fileName + " failed to compile:\n" + shaderProgram.getLog());
        }

        return shaderProgram;
    }

    static public String getShaderCode(String prefix, String code) {
        code = code.trim();
        if (code.startsWith("#version") && !prefix.isEmpty()) {
            int firstLineEnd = code.indexOf('\n') + 1;
            String versionStr = code.substring(0, firstLineEnd);
            return versionStr + prefix + code.substring(firstLineEnd);
        } else {
            return prefix + code;
        }
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
         * Code that is always added to the fragment shader code. This is added as-is, and you should include a newline (`\n`) if
         * needed. {@linkplain ExtShaderProgram#prependFragmentCode} is placed before this code.
         */
        public String prependFragmentCode;
    }
}
