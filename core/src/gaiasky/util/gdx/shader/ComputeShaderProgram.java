/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.utils.Disposable;
import gaiasky.util.Logger;
import gaiasky.util.SysUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
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
    private final String shaderCode;
    private int programId;
    private boolean isCompiled = false;
    private final Map<String, Integer> uniforms = new HashMap<>();
    private final static int localSizeX = 256; // Must match compute shader layout.

    public ComputeShaderProgram(String name, String shaderCode) throws IOException {
        this.name = name;
        this.shaderCode = shaderCode;

        if (!SysUtils.isComputeShaderSupported()) {
            logger.warn("Compute shaders require OpenGL 4.3+ or ARB_compute_shader extension");
        } else {
            compile();
        }
    }

    public boolean isCompiled() {
        return isCompiled;
    }

    /** Compile shader and detect uniforms */
    public void compile() {
        if (!isCompiled) {
            var cache = ShaderCache.instance();
            var comp = cache.compileShaders(name, shaderCode);

            this.isCompiled = comp[0] == GL_TRUE;
            this.programId = comp[1];

            if (isCompiled) detectUniforms();
        }
    }

    /** Detects active uniforms in the shader and caches their locations */
    private void detectUniforms() {
        int uniformCount = glGetProgrami(programId, GL_ACTIVE_UNIFORMS);
        IntBuffer sizeBuf = BufferUtils.createIntBuffer(1);
        IntBuffer typeBuf = BufferUtils.createIntBuffer(1);

        for (int i = 0; i < uniformCount; i++) {
            String uniformName = glGetActiveUniform(programId, i, sizeBuf, typeBuf);
            int location = glGetUniformLocation(programId, uniformName);
            uniforms.put(uniformName, location);
        }
    }

    /** Set uniform by name */
    public void setUniform(String name, int value) {
        Integer loc = uniforms.get(name);
        if (loc != null && loc >= 0) glUniform1i(loc, value);
    }

    public void setUniform(String name, float value) {
        Integer loc = uniforms.get(name);
        if (loc != null && loc >= 0) glUniform1f(loc, value);
    }

    public void setUniform(String name, float x, float y, float z) {
        Integer loc = uniforms.get(name);
        if (loc != null && loc >= 0) glUniform3f(loc, x, y, z);
    }

    public void setUniform(String name, float x, float y, float z, float w) {
        Integer loc = uniforms.get(name);
        if (loc != null && loc >= 0) glUniform4f(loc, x, y, z, w);
    }

    /** Bind the shader program for dispatch */
    public void begin() {
        glUseProgram(programId);
    }

    /**
     * Dispatch the compute shader for numElements, inserting a memory barrier.
     *
     * @param numElements number of elements (particles) to process
     */
    public void end(int numElements) {
        int groups = (numElements + localSizeX - 1) / localSizeX;
        glDispatchCompute(groups, 1, 1);
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            System.out.println("OpenGL error after dispatch: " + error);
        }
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    }

    /** Stop using any compute shader program */
    public void unbind() {
        glUseProgram(0);
    }

    /** Returns the OpenGL program ID (for advanced usage, e.g., SSBO binding) */
    public int getProgramId() {
        return programId;
    }

    /** Deletes the program */
    public void cleanup() {
        glDeleteProgram(programId);
    }

    @Override
    public void dispose() {
        if (isCompiled)
            cleanup();
    }
}
