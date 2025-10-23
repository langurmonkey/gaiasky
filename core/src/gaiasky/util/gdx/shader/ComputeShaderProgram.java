/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.render.GaiaSkyShaderCompileException;
import gaiasky.util.Logger;
import gaiasky.util.SysUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL43;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * A compute shader program helper.
 * <p>
 * Usage pattern:
 * <pre>{@code
 * ParticleBuffer particleBuffer = new ParticleBuffer(100_000);
 * ComputeShaderProgram computeShader = new ComputeShaderProgram("galaxy", shaderCode);
 *
 * computeShader.begin();
 * computeShader.setUniform("seed", 1234);
 * computeShader.setUniform("count", 100_000);
 * computeShader.setUniform("radius", 1.0f);
 *
 * particleBuffer.bind(0); // matches layout(binding = 0) in GLSL
 * computeShader.end(100_000);
 * }</pre>
 * <p>
 * Notes:
 * - Make sure your shader's `local_size_x` matches LOCAL_SIZE_X constant.
 * - After dispatch, use the particle buffer in rendering. Memory barrier is applied automatically.
 */
public class ComputeShaderProgram implements Disposable {
    private static final Logger.Log logger = Logger.getLogger(ComputeShaderProgram.class);

    private final String name;
    private final String shaderFile;
    private final String shaderCode;
    private String log;
    private int program;
    private boolean isCompiled = false;
    private final Map<String, Integer> uniforms = new HashMap<>();
    private final Map<String, Integer> uniformTypes = new HashMap<>(); // Track uniform types
    private boolean using = false;

    /** The local size X dimension. Must match the compute shader layout. **/
    private final static int localSizeX = 256;


    public ComputeShaderProgram(String name, String shaderFile, String shaderCode) throws IOException {
        this.name = name;
        this.shaderFile = shaderFile;
        this.shaderCode = shaderCode;

        if (!SysUtils.isComputeShaderSupported()) {
            logger.warn("Compute shaders require OpenGL 4.3+ or ARB_compute_shader extension");
        } else {
            compile();
            if (isCompiled) {
                fetchUniforms();
            } else {
                throw new GaiaSkyShaderCompileException(this);
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getShaderFileName() {
        return shaderFile;
    }

    public String getShaderSource() {
        return shaderCode;
    }

    public String getLog() {
        return log;
    }

    public boolean isCompiled() {
        return isCompiled;
    }

    /** Compile shader and detect uniforms */
    public void compile() {
        if (!isCompiled) {
            var cache = ShaderCache.instance();
            var comp = cache.compileShaders(name, shaderCode);
            program = comp[1];

            isCompiled = comp[0] == GL_TRUE;
            log = cache.getLog();
        }
    }

    /** Detects active uniforms in the shader and caches their locations and types */
    private void fetchUniforms() {
        int uniformCount = glGetProgrami(program, GL_ACTIVE_UNIFORMS);
        IntBuffer sizeBuf = BufferUtils.createIntBuffer(1);
        IntBuffer typeBuf = BufferUtils.createIntBuffer(1);

        for (int i = 0; i < uniformCount; i++) {
            String uniformName = glGetActiveUniform(program, i, sizeBuf, typeBuf);
            int location = glGetUniformLocation(program, uniformName);
            int type = typeBuf.get(0);

            uniforms.put(uniformName, location);
            uniformTypes.put(uniformName, type);

            logger.debug("Uniform detected: " + uniformName + " (location: " + location + ", type: " + type + ")");
        }
    }

    protected int fetchUniformLocation(String name) {
        return uniforms.get(name);
    }

    /** Set uniform by name with type checking */
    public void setUniform(String name, int value) {
        if (!checkUniformExists(name)) return;

        Integer type = uniformTypes.get(name);
        Integer loc = uniforms.get(name);

        if (type == GL_INT) {
            glUniform1i(loc, value);
        } else if (type == GL_UNSIGNED_INT) {
            GL43.glUniform1ui(loc, value); // Use uint for unsigned int
        } else {
            logger.warn("Uniform '" + name + "' type mismatch. Expected int/uint, got type: " + type);
            // Try to set anyway as int (might work for some cases)
            glUniform1i(loc, value);
        }

        checkGLError("setUniform(" + name + ", int)");
    }

    public void setUniform(String name, float value) {
        if (!checkUniformExists(name)) return;

        Integer type = uniformTypes.get(name);
        Integer loc = uniforms.get(name);

        if (type == GL_FLOAT) {
            glUniform1f(loc, value);
        } else {
            logger.warn("Uniform '" + name + "' type mismatch. Expected float, got type: " + type);
            // Try to set anyway as float
            glUniform1f(loc, value);
        }

        checkGLError("setUniform(" + name + ", float)");
    }

    public void setUniform(String name, Vector3 vec) {
        setUniform(name, vec.x, vec.y, vec.z);
    }

    public void setUniform(String name, float x, float y, float z) {
        if (!checkUniformExists(name)) return;

        Integer type = uniformTypes.get(name);
        Integer loc = uniforms.get(name);

        if (type == GL_FLOAT_VEC3) {
            glUniform3f(loc, x, y, z);
        } else {
            logger.warn("Uniform '" + name + "' type mismatch. Expected vec3, got type: " + type);
            glUniform3f(loc, x, y, z);
        }

        checkGLError("setUniform(" + name + ", vec3)");
    }

    public void setUniform(String name, float x, float y, float z, float w) {
        if (!checkUniformExists(name)) return;

        Integer type = uniformTypes.get(name);
        Integer loc = uniforms.get(name);

        if (type == GL_FLOAT_VEC4) {
            glUniform4f(loc, x, y, z, w);
        } else {
            logger.warn("Uniform '" + name + "' type mismatch. Expected vec4, got type: " + type);
            glUniform4f(loc, x, y, z, w);
        }

        checkGLError("setUniform(" + name + ", vec4)");
    }

    /**
     * Sets the uniform matrix with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param matrix the matrix
     */
    public void setUniformMatrix(String name,
                                 Matrix4 matrix) {
        setUniformMatrix(name, matrix, false);
    }

    /**
     * Sets the uniform matrix with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name      the name of the uniform
     * @param matrix    the matrix
     * @param transpose whether the matrix should be transposed
     */
    public void setUniformMatrix(String name,
                                 Matrix4 matrix,
                                 boolean transpose) {
        setUniformMatrix(fetchUniformLocation(name), matrix, transpose);
    }

    public void setUniformMatrix(int location,
                                 Matrix4 matrix) {
        setUniformMatrix(location, matrix, false);
    }

    public void setUniformMatrix(int location,
                                 Matrix4 matrix,
                                 boolean transpose) {
        GL20 gl = Gdx.gl20;
        gl.glUniformMatrix4fv(location, 1, transpose, matrix.val, 0);
    }

    /**
     * Sets the uniform matrix with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param matrix the matrix
     */
    public void setUniformMatrix(String name,
                                 Matrix3 matrix) {
        setUniformMatrix(name, matrix, false);
    }

    /**
     * Sets the uniform matrix with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name      the name of the uniform
     * @param matrix    the matrix
     * @param transpose whether the uniform matrix should be transposed
     */
    public void setUniformMatrix(String name,
                                 Matrix3 matrix,
                                 boolean transpose) {
        setUniformMatrix(fetchUniformLocation(name), matrix, transpose);
    }

    public void setUniformMatrix(int location,
                                 Matrix3 matrix) {
        setUniformMatrix(location, matrix, false);
    }

    public void setUniformMatrix(int location,
                                 Matrix3 matrix,
                                 boolean transpose) {
        GL20 gl = Gdx.gl20;
        gl.glUniformMatrix3fv(location, 1, transpose, matrix.val, 0);
    }

    /**
     * Sets an array of uniform matrices with the given name. The {@link ExtShaderProgram} must be bound for this to
     * work.
     *
     * @param name      the name of the uniform
     * @param buffer    buffer containing the matrix data
     * @param transpose whether the uniform matrix should be transposed
     */
    public void setUniformMatrix3fv(String name,
                                    FloatBuffer buffer,
                                    int count,
                                    boolean transpose) {
        GL20 gl = Gdx.gl20;
        buffer.position(0);
        int location = fetchUniformLocation(name);
        gl.glUniformMatrix3fv(location, count, transpose, buffer);
    }

    /**
     * Sets an array of uniform matrices with the given name. The {@link ExtShaderProgram} must be bound for this to
     * work.
     *
     * @param name      the name of the uniform
     * @param buffer    buffer containing the matrix data
     * @param transpose whether the uniform matrix should be transposed
     */
    public void setUniformMatrix4fv(String name,
                                    FloatBuffer buffer,
                                    int count,
                                    boolean transpose) {
        GL20 gl = Gdx.gl20;
        buffer.position(0);
        int location = fetchUniformLocation(name);
        gl.glUniformMatrix4fv(location, count, transpose, buffer);
    }

    public void setUniformMatrix4fv(int location,
                                    float[] values,
                                    int offset,
                                    int length) {
        GL20 gl = Gdx.gl20;
        gl.glUniformMatrix4fv(location, length / 16, false, values, offset);
    }

    public void setUniformMatrix4fv(String name,
                                    float[] values,
                                    int offset,
                                    int length) {
        setUniformMatrix4fv(fetchUniformLocation(name), values, offset, length);
    }

    /** Additional method for unsigned int (common in compute shaders) */
    public void setUniformUint(String name, int value) {
        if (!checkUniformExists(name)) return;

        Integer type = uniformTypes.get(name);
        Integer loc = uniforms.get(name);

        if (type == GL_UNSIGNED_INT) {
            GL43.glUniform1ui(loc, value);
        } else if (type == GL_INT) {
            glUniform1i(loc, value); // Fallback to signed int
        } else {
            logger.warn("Uniform '" + name + "' type mismatch. Expected uint, got type: " + type);
            GL43.glUniform1ui(loc, value); // Try anyway
        }

        checkGLError("setUniformUint(" + name + ")");
    }

    /** Check if uniform exists and we're in a valid state */
    private boolean checkUniformExists(String name) {
        if (!using) {
            logger.error("Cannot set uniform '" + name + "' - shader not active. Call begin() first.");
            return false;
        }

        if (!uniforms.containsKey(name)) {
            logger.error("Uniform '" + name + "' not found in shader program.");
            return false;
        }

        Integer loc = uniforms.get(name);
        if (loc == null || loc < 0) {
            logger.error("Uniform '" + name + "' has invalid location: " + loc);
            return false;
        }

        return true;
    }

    /** Check for OpenGL errors after uniform operations */
    private void checkGLError(String operation) {
        int error = GL43.glGetError();
        if (error != GL43.GL_NO_ERROR) {
            logger.error("OpenGL error in " + operation + ": " + error);
        }
    }

    /** Bind the shader program for dispatch */
    public void begin() {
        if (!using) {
            // Clear any existing errors before beginning
            while (GL43.glGetError() != GL43.GL_NO_ERROR) ;

            glUseProgram(program);
            using = true;

            int error = GL43.glGetError();
            if (error != GL43.GL_NO_ERROR) {
                logger.error("OpenGL error after begin(): " + error);
            }
        } else {
            logger.error("The last call to begin() was not met with a call to end()!");
        }
    }

    /**
     * Dispatch the compute shader for numElements, inserting a memory barrier.
     *
     * @param numElements number of elements (particles) to process
     */
    public void end(int numElements) {
        if (using) {
            int groups = (numElements + localSizeX - 1) / localSizeX;

            // Clear errors before dispatch
            while (GL43.glGetError() != GL43.GL_NO_ERROR) ;

            glDispatchCompute(groups, 1, 1);
            int error = GL43.glGetError();
            if (error != GL43.GL_NO_ERROR) {
                logger.error("OpenGL error after dispatch: " + error);
            }

            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
            unbind();
        } else {
            logger.error("You need to call begin() first, otherwise the shader is not active.");
        }
    }

    /** Stop this or any other program. */
    private void unbind() {
        glUseProgram(0);
        using = false;
    }

    /** Returns the OpenGL program ID (for advanced usage, e.g., SSBO binding) */
    public int getProgram() {
        return program;
    }

    /** Get uniform type for debugging */
    public int getUniformType(String name) {
        return uniformTypes.getOrDefault(name, -1);
    }

    /** Get uniform location for advanced usage */
    public int getUniformLocation(String name) {
        return uniforms.getOrDefault(name, -1);
    }

    /** Deletes the program */
    public void cleanup() {
        glDeleteProgram(program);
    }

    @Override
    public void dispose() {
        if (isCompiled)
            cleanup();
    }
}
