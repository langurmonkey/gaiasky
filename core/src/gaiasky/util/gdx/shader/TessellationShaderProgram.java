/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.render.GaiaSkyShaderCompileException;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;
import org.lwjgl.opengl.GL41;

public class TessellationShaderProgram extends ExtShaderProgram {
    private static final Logger.Log logger = Logger.getLogger(ExtShaderProgram.class);

    /**
     * the list of currently available shaders
     **/
    private final static ObjectMap<Application, Array<TessellationShaderProgram>> shaders = new ObjectMap<>();
    /**
     * Code that is always added to the tessellation shaders, typically used to inject a #version line. Note that this
     * is added
     * as-is, you should include a newline (`\n`) if needed.
     */
    public static String prependControlCode = "";
    public static String prependEvaluationCode = "";
    /**
     * Tessellation control source.
     **/
    private final String controlShaderSource;
    /**
     * Tessellation evaluation source.
     **/
    private final String evaluationShaderSource;

    protected String controlShaderFile, evaluationShaderFile;

    private int controlShaderHandle, evaluationShaderHandle;

    public TessellationShaderProgram(String vertexShaderCode, String controlShaderCode, String evaluationShaderCode, String fragmentShaderCode) {
        this(null, null, null, null, null, vertexShaderCode, controlShaderCode, evaluationShaderCode, fragmentShaderCode, false);
    }

    /**
     * Constructs a new shader program and immediately compiles it, if it is not lazy.
     *
     * @param name                 The shader name, if any.
     * @param vertexFile           The vertex shader file.
     * @param tessControlFile      The tessellation control shader file.
     * @param tessEvaluationFile   The tessellation evaluation shader file.
     * @param fragmentFile         The fragment shader file.
     * @param vertexShaderCode     The vertex shader code.
     * @param controlShaderCode    The tessellation control shader code.
     * @param evaluationShaderCode The tessellation evaluation shader code.
     * @param fragmentShaderCode   The fragment shader code.
     * @param lazyLoading          Whether to use lazy loading, only preparing the data without actually compiling the
     *                             shaders.
     */
    public TessellationShaderProgram(String name, String vertexFile, String tessControlFile, String tessEvaluationFile, String fragmentFile, String vertexShaderCode, String controlShaderCode, String evaluationShaderCode, String fragmentShaderCode, boolean lazyLoading) {
        if (vertexShaderCode == null) throw new IllegalArgumentException("vertex shader must not be null");
        if (controlShaderCode == null) throw new IllegalArgumentException("tess control shader must not be null");
        if (evaluationShaderCode == null)
            throw new IllegalArgumentException("tess evaluation shader must not be null");
        if (fragmentShaderCode == null) throw new IllegalArgumentException("fragment shader must not be null");

        if (prependVertexCode != null && !prependVertexCode.isEmpty())
            vertexShaderCode = prependVertexCode + vertexShaderCode;
        if (prependControlCode != null && !prependControlCode.isEmpty())
            controlShaderCode = prependControlCode + controlShaderCode;
        if (prependEvaluationCode != null && !prependEvaluationCode.isEmpty())
            evaluationShaderCode = prependEvaluationCode + evaluationShaderCode;
        if (prependFragmentCode != null && !prependFragmentCode.isEmpty())
            fragmentShaderCode = prependFragmentCode + fragmentShaderCode;


        // Sources.
        this.vertexShaderSource = vertexShaderCode;
        this.controlShaderSource = controlShaderCode;
        this.evaluationShaderSource = evaluationShaderCode;
        this.fragmentShaderSource = fragmentShaderCode;

        // Files.
        this.vertexShaderFile = vertexFile;
        this.fragmentShaderFile = fragmentFile;
        this.controlShaderFile = fragmentFile;
        this.evaluationShaderFile = fragmentFile;

        if (!lazyLoading) {
            compile();
        }
    }

    public void compile() {
        if (!isCompiled) {
            initializeLocalAssets();

            if (name != null) {
                logger.info(I18n.msg("notif.shader.compile", name));
            }

            if (vertexShaderFile != null || controlShaderFile != null || evaluationShaderFile != null || fragmentShaderFile != null) {
                logger.debug(I18n.msg("notif.shader.load.tess", vertexShaderFile, controlShaderFile, evaluationShaderFile, fragmentShaderFile));
            }

            compileShaders(vertexShaderSource, controlShaderSource, evaluationShaderSource, fragmentShaderSource);
            if (isCompiled()) {
                fetchAttributes();
                fetchUniforms();
                addManagedShader(Gdx.app, this);
            } else {
                throw new GaiaSkyShaderCompileException(this);
            }

        }
    }

    /**
     * Invalidates all shaders so the next time they are used new handles are generated
     */
    public static void invalidateAllShaderPrograms(Application app) {
        if (Gdx.gl == null) return;

        Array<TessellationShaderProgram> shaderArray = shaders.get(app);
        if (shaderArray == null) return;

        for (int i = 0; i < shaderArray.size; i++) {
            shaderArray.get(i).invalidated = true;
            shaderArray.get(i).checkManaged();
        }
    }

    public static void clearAllShaderPrograms(Application app) {
        shaders.remove(app);
    }

    public static String getManagedStatus() {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        builder.append("Managed shaders/app: { ");
        for (Application app : shaders.keys()) {
            builder.append(shaders.get(app).size);
            builder.append(" ");
        }
        builder.append("}");
        return builder.toString();
    }

    /**
     * @return the number of managed shader programs currently loaded
     */
    public static int getNumManagedShaderPrograms() {
        return shaders.get(Gdx.app).size;
    }

    /**
     * Loads and compiles the shaders, creates a new program and links the shaders.
     */
    private void compileShaders(String vShader, String tcShader, String teShader, String fShader) {
        var cache = ShaderCache.instance();
        int[] handles = cache.compileShaders(name, vShader, tcShader, teShader, fShader);
        program = handles[0];
        vertexShaderHandle = handles[1];
        controlShaderHandle = handles[2];
        evaluationShaderHandle = handles[3];
        fragmentShaderHandle = handles[4];

        isCompiled = cache.isCompiled();
        log = cache.getLog();
    }


    private void checkManaged() {
        if (invalidated) {
            compileShaders(vertexShaderSource, controlShaderSource, evaluationShaderSource, fragmentShaderSource);
            invalidated = false;
        }
    }

    private void addManagedShader(Application app, TessellationShaderProgram shaderProgram) {
        Array<TessellationShaderProgram> managedResources = shaders.get(app);
        if (managedResources == null) managedResources = new Array<>();
        managedResources.add(shaderProgram);
        shaders.put(app, managedResources);
    }

    public String getControlShaderSource() {
        return controlShaderSource;
    }

    public String getControlShaderFileName() {
        return controlShaderFile;
    }

    public String getEvaluationShaderSource() {
        return evaluationShaderSource;
    }

    public String getEvaluationShaderFileName() {
        return evaluationShaderFile;
    }

    @Override
    public void dispose() {
        if (isCompiled && !isDisposed) {
            GL41.glUseProgram(0);
            if (vertexShaderHandle != 0) {
                GL41.glDeleteShader(vertexShaderHandle);
            }
            if (controlShaderHandle != 0) {
                GL41.glDeleteShader(controlShaderHandle);
            }
            if (evaluationShaderHandle != 0) {
                GL41.glDeleteShader(evaluationShaderHandle);
            }
            if (fragmentShaderHandle != 0) {
                GL41.glDeleteShader(fragmentShaderHandle);
            }
            GL41.glDeleteProgram(program);
            if (shaders.get(Gdx.app) != null) shaders.get(Gdx.app).removeValue(this, true);

            isDisposed = true;
        }
    }

    @Override
    public void writeShaders() {
        super.writeShaders();
        writeShader(getControlShaderFileName(), getControlShaderSource());
        writeShader(getEvaluationShaderFileName(), getEvaluationShaderSource());
    }
}
