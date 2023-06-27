/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ObjectMap;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL41;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class TessellationShaderProgram extends ExtShaderProgram {
    /**
     * the list of currently available shaders
     **/
    private final static ObjectMap<Application, Array<TessellationShaderProgram>> shaders = new ObjectMap<>();
    /**
     * Code that is always added to the tessellation shaders, typically used to inject a #version line. Note that this is added
     * as-is, you should include a newline (`\n`) if needed.
     */
    public static String prependTessControlCode = "";
    public static String prependTessEvaluationCode = "";
    /**
     * Tessellation control source.
     **/
    private final String tessellationControlShaderSource;
    /**
     * Tessellation evaluation source.
     **/
    private final String tessellationEvaluationShaderSource;

    private int tessellationControlShaderHandle, tessellationEvaluationShaderHandle;

    public TessellationShaderProgram(String vertexShader, String tessellationControlShader, String tessellationEvaluationShader, String fragmentShader) {
        if (vertexShader == null)
            throw new IllegalArgumentException("vertex shader must not be null");
        if (tessellationControlShader == null)
            throw new IllegalArgumentException("tess control shader must not be null");
        if (tessellationEvaluationShader == null)
            throw new IllegalArgumentException("tess evaluation shader must not be null");
        if (fragmentShader == null)
            throw new IllegalArgumentException("fragment shader must not be null");

        if (prependVertexCode != null && prependVertexCode.length() > 0)
            vertexShader = prependVertexCode + vertexShader;
        if (prependTessControlCode != null && prependTessControlCode.length() > 0)
            tessellationControlShader = prependTessControlCode + tessellationControlShader;
        if (prependTessEvaluationCode != null && prependTessEvaluationCode.length() > 0)
            tessellationEvaluationShader = prependTessEvaluationCode + tessellationEvaluationShader;
        if (prependFragmentCode != null && prependFragmentCode.length() > 0)
            fragmentShader = prependFragmentCode + fragmentShader;

        this.vertexShaderSource = vertexShader;
        this.tessellationControlShaderSource = tessellationControlShader;
        this.tessellationEvaluationShaderSource = tessellationEvaluationShader;
        this.fragmentShaderSource = fragmentShader;
        initializeLocalAssets();

        compileShaders(vertexShader, tessellationControlShader, tessellationEvaluationShader, fragmentShader);
        if (isCompiled()) {
            fetchAttributes();
            fetchUniforms();
            addManagedShader(Gdx.app, this);
        }
    }

    /**
     * Invalidates all shaders so the next time they are used new handles are generated
     */
    public static void invalidateAllShaderPrograms(Application app) {
        if (Gdx.gl == null)
            return;

        Array<TessellationShaderProgram> shaderArray = shaders.get(app);
        if (shaderArray == null)
            return;

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
        vertexShaderHandle = loadShader(GL30.GL_VERTEX_SHADER, vShader);
        tessellationControlShaderHandle = loadShader(GL41.GL_TESS_CONTROL_SHADER, tcShader);
        tessellationEvaluationShaderHandle = loadShader(GL41.GL_TESS_EVALUATION_SHADER, teShader);
        fragmentShaderHandle = loadShader(GL30.GL_FRAGMENT_SHADER, fShader);

        if (vertexShaderHandle == -1 || tessellationControlShaderHandle == -1 || tessellationEvaluationShaderHandle == -1 || fragmentShaderHandle == -1) {
            isCompiled = false;
            return;
        }

        program = linkProgram(createProgram());
        if (program == -1) {
            isCompiled = false;
            return;
        }

        isCompiled = true;
    }

    private int loadShader(int type, String source) {
        IntBuffer intBuffer = BufferUtils.newIntBuffer(1);

        int shader = GL41.glCreateShader(type);
        if (shader == 0)
            return -1;

        GL41.glShaderSource(shader, source);
        GL41.glCompileShader(shader);
        GL41.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, intBuffer);

        int compiled = intBuffer.get(0);
        if (compiled == 0) {
            String infoLog = GL41.glGetShaderInfoLog(shader);
            switch (type) {
                case GL41.GL_VERTEX_SHADER -> log += "Vertex shader\n";
                case GL41.GL_FRAGMENT_SHADER -> log += "Fragment shader\n";
                case GL41.GL_TESS_CONTROL_SHADER -> log += "Tessellation control shader\n";
                case GL41.GL_TESS_EVALUATION_SHADER -> log += "Tessellation evaluation shader\n";
            }
            log += infoLog;
            // }
            return -1;
        }

        return shader;
    }

    private int linkProgram(int program) {
        if (program == -1)
            return -1;

        GL41.glAttachShader(program, vertexShaderHandle);
        GL41.glAttachShader(program, tessellationControlShaderHandle);
        GL41.glAttachShader(program, tessellationEvaluationShaderHandle);
        GL41.glAttachShader(program, fragmentShaderHandle);
        GL41.glLinkProgram(program);

        ByteBuffer tmp = ByteBuffer.allocateDirect(4);
        tmp.order(ByteOrder.nativeOrder());
        IntBuffer intBuffer = tmp.asIntBuffer();

        GL41.glGetProgramiv(program, GL20.GL_LINK_STATUS, intBuffer);
        int linked = intBuffer.get(0);
        if (linked == 0) {
            log = GL41.glGetProgramInfoLog(program);
            // }
            return -1;
        }

        return program;
    }


    private void checkManaged() {
        if (invalidated) {
            compileShaders(vertexShaderSource, tessellationControlShaderSource, tessellationEvaluationShaderSource, fragmentShaderSource);
            invalidated = false;
        }
    }

    private void addManagedShader(Application app, TessellationShaderProgram shaderProgram) {
        Array<TessellationShaderProgram> managedResources = shaders.get(app);
        if (managedResources == null)
            managedResources = new Array<>();
        managedResources.add(shaderProgram);
        shaders.put(app, managedResources);
    }

    @Override
    public void dispose() {
        if (isCompiled && !isDisposed) {
            GL41.glUseProgram(0);
            if (vertexShaderHandle != 0) {
                GL41.glDeleteShader(vertexShaderHandle);
            }
            if (tessellationControlShaderHandle != 0) {
                GL41.glDeleteShader(tessellationControlShaderHandle);
            }
            if (tessellationEvaluationShaderHandle != 0) {
                GL41.glDeleteShader(tessellationEvaluationShaderHandle);
            }
            if (fragmentShaderHandle != 0) {
                GL41.glDeleteShader(fragmentShaderHandle);
            }
            GL41.glDeleteProgram(program);
            if (shaders.get(Gdx.app) != null)
                shaders.get(Gdx.app).removeValue(this, true);

            isDisposed = true;
        }
    }
}
