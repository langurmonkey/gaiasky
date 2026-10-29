/*
 * Copyright (c) 2025-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.GaiaSky;
import gaiasky.render.ShaderCompilationException;
import gaiasky.util.Logger;
import gaiasky.util.SysUtils;
import gaiasky.render.gdx.shader.loader.ShaderTemplatingLoader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL43;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.*;

public class ComputeShaderProgram implements Disposable {
    private static final Logger.Log logger = Logger.getLogger(ComputeShaderProgram.class);

    private final String name;
    private final String shaderFile;
    private String shaderCode;
    private String log;
    private int program;
    private boolean isCompiled;
    private final Map<String, Integer> uniforms = new HashMap<>();
    private final Map<String, Integer> uniformTypes = new HashMap<>(); // Track uniform types
    private boolean using;

    /** The local size X dimension. Must match the compute shader layout. **/
    private final static int localSizeX = 256;


    public ComputeShaderProgram(String name, String shaderFile, String shaderCode) throws IOException {
        this.name = name;
        this.shaderFile = shaderFile;
        this.shaderCode = shaderCode;

        if (!GaiaSky.settings().runtime.compute) {
            logger.warn("Compute shaders require OpenGL 4.3+ or ARB_compute_shader extension");
        } else {
            compile();
        }
    }

    /**
     * Reloads and recompiles the program.
     */
    public void reload() {
        // Reload code from disk.
        var shaderFile = Gdx.files.internal(this.shaderFile);
        shaderCode = ShaderTemplatingLoader.load(shaderFile);
        // Mark compiled false.
        isCompiled = false;
        // Recompile.
        if (!GaiaSky.settings().runtime.compute) {
            logger.warn("Compute shaders require OpenGL 4.3+ or ARB_compute_shader extension");
        } else {
            compile();
        }
    }

    public String getName() {
        return name;
    }

    public String getShaderFileName() {
        return Path.of(shaderFile).getFileName().toString();
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

        if (isCompiled) {
            fetchUniforms();
        } else {
            throw new ShaderCompilationException(this);
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

        var type = uniformTypes.get(name);
        var loc = uniforms.get(name);

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

        var type = uniformTypes.get(name);
        var loc = uniforms.get(name);

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

    public void setUniform(String name, float x, float y) {
        if (!checkUniformExists(name)) return;

        var type = uniformTypes.get(name);
        var loc = uniforms.get(name);

        if (type == GL_FLOAT_VEC2) {
            glUniform2f(loc, x, y);
        } else {
            logger.warn("Uniform '" + name + "' type mismatch. Expected vec2, got type: " + type);
            glUniform2f(loc, x, y);
        }

        checkGLError("setUniform(" + name + ", vec3)");
    }

    public void setUniform(String name, float x, float y, float z) {
        if (!checkUniformExists(name)) return;

        var type = uniformTypes.get(name);
        var loc = uniforms.get(name);

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

        var type = uniformTypes.get(name);
        var loc = uniforms.get(name);

        if (type == GL_FLOAT_VEC4) {
            glUniform4f(loc, x, y, z, w);
        } else {
            logger.warn("Uniform '" + name + "' type mismatch. Expected vec4, got type: " + type);
            glUniform4f(loc, x, y, z, w);
        }

        checkGLError("setUniform(" + name + ", vec4)");
    }

    public void setUniform2fv(String name, float[] values) {
        if (!checkUniformExists(name)) return;

        var loc = uniforms.get(name);
        var buffer = BufferUtils.createFloatBuffer(values.length);
        buffer.put(values).flip();
        glUniform2fv(loc, buffer);

        checkGLError("setUniform(" + name + ", float[" + values.length + "])");
    }

    public void setUniform3fv(String name, float[] values) {
        if (!checkUniformExists(name)) return;

        var loc = uniforms.get(name);
        var buffer = BufferUtils.createFloatBuffer(values.length);
        buffer.put(values).flip();
        glUniform3fv(loc, buffer);

        checkGLError("setUniform(" + name + ", float[" + values.length + "])");
    }

    /**
     * Sets a float array or a single vector uniform from an array.
     * <p>
     * A scalar array and a single vec3 are both "a few floats from Java", but GL is strict about
     * which call accepts which: {@code glUniform1fv} on a vec3 is {@code GL_INVALID_OPERATION} and
     * leaves the uniform untouched. So the type decides the call, and a vector is one element.
     */
    public void setUniform(String name, float[] values) {
        if (!checkUniformExists(name)) return;

        Integer type = uniformTypes.get(name);
        Integer loc = uniforms.get(name);
        int vecSize = switch (type) {
            case GL_FLOAT_VEC4 -> 4;
            case GL_FLOAT_VEC3 -> 3;
            case GL_FLOAT_VEC2 -> 2;
            default -> 1;
        };
        if (vecSize > 1 && values.length < vecSize) {
            logger.warn("Uniform '" + name + "' needs " + vecSize + " floats, got " + values.length);
            return;
        }
        if (vecSize == 1 && type != GL_FLOAT) {
            logger.warn("Uniform '" + name + "' type mismatch. Expected float, got type: " + type);
        }

        switch (vecSize) {
            case 4 -> glUniform4fv(loc, values);
            case 3 -> glUniform3fv(loc, values);
            case 2 -> glUniform2fv(loc, values);
            default -> glUniform1fv(loc, values);
        }

        checkGLError("setUniform(" + name + ", float[" + values.length + "])");
    }

    /** Sets an ivec3 uniform. **/
    public void setUniform(String name, int x, int y, int z) {
        if (!checkUniformExists(name)) return;

        Integer type = uniformTypes.get(name);
        Integer loc = uniforms.get(name);

        if (type != GL_INT_VEC3) {
            logger.warn("Uniform '" + name + "' type mismatch. Expected ivec3, got type: " + type);
        }
        GL43.glUniform3i(loc, x, y, z);

        checkGLError("setUniform(" + name + ", ivec3)");
    }

    /** Sets an int array or a single integer vector uniform from an array. **/
    public void setUniform(String name, int[] values) {
        if (!checkUniformExists(name)) return;

        Integer type = uniformTypes.get(name);
        Integer loc = uniforms.get(name);
        int vecSize = switch (type) {
            case GL_INT_VEC4 -> 4;
            case GL_INT_VEC3 -> 3;
            case GL_INT_VEC2 -> 2;
            default -> 1;
        };
        if (vecSize > 1 && values.length < vecSize) {
            logger.warn("Uniform '" + name + "' needs " + vecSize + " ints, got " + values.length);
            return;
        }
        if (vecSize == 1 && type != GL_INT) {
            logger.warn("Uniform '" + name + "' type mismatch. Expected int, got type: " + type);
        }

        switch (vecSize) {
            case 4 -> GL43.glUniform4iv(loc, values);
            case 3 -> GL43.glUniform3iv(loc, values);
            case 2 -> GL43.glUniform2iv(loc, values);
            default -> GL43.glUniform1iv(loc, values);
        }

        checkGLError("setUniform(" + name + ", int[" + values.length + "])");
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

    /**
     * Dispatch the compute shader over a 3D grid, inserting a full memory barrier.
     * <p>
     * The workgroup is 256x1x1, so x is the only axis that is tiled: the group count on x is
     * rounded up (a shader whose work is a voxel grid must mask the overshooting invocations
     * itself), while y and z dispatch exactly their voxel count.
     * <p>
     * The barrier is {@link GL43#GL_ALL_BARRIER_BITS} rather than the storage buffer bit used by
     * {@link #end(int)}: these dispatches write 3D images and the next pass may both read what they
     * wrote and sample the texture it produced.
     *
     * @param nx Number of invocations on x.
     * @param ny Number of invocations on y.
     * @param nz Number of invocations on z.
     */
    public void end3D(int nx, int ny, int nz) {
        if (using) {
            int groupsX = (nx + localSizeX - 1) / localSizeX;

            while (GL43.glGetError() != GL43.GL_NO_ERROR) ;

            glDispatchCompute(groupsX, Math.max(ny, 1), Math.max(nz, 1));
            int error = GL43.glGetError();
            if (error != GL43.GL_NO_ERROR) {
                logger.error("OpenGL error after dispatch: " + error);
            }

            GL43.glMemoryBarrier(GL43.GL_ALL_BARRIER_BITS);
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
