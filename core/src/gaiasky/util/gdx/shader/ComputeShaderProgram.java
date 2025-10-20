/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import gaiasky.util.Logger;
import gaiasky.util.SysUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL42C.glMemoryBarrier;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT;
import static org.lwjgl.opengl.GL43C.glDispatchCompute;

/**
 * A compute shader program.
 * <p>Here is a usage example:</p>
 * <p>
 * <pre>
 * ParticleBuffer particleBuffer = new ParticleBuffer(100_000);
 *
 * // Bind to shader (layout(binding = 0))
 * particleBuffer.bind(0);
 *
 * computeShader.bind();
 * computeShader.setUniform("seed", 1234);
 * computeShader.setUniform("count", 100_000);
 * computeShader.setUniform("radius", 1.0f);
 *
 * int groups = (100_000 + 255) / 256;
 * glDispatchCompute(groups, 1, 1);
 * glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
 * </pre>
 * </p>
 */
public class ComputeShaderProgram {
    private static final Logger.Log logger = Logger.getLogger(ComputeShaderProgram.class);

    private final String name;
    private final String shaderCode;
    private int programId;
    private boolean isCompiled = false;

    public int localSizeX = 256;
    private final Map<String, Integer> uniforms = new HashMap<>();

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
            String uniformName = GL20.glGetActiveUniform(programId, i, sizeBuf, typeBuf);
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

    public void begin() {
        glUseProgram(programId);
    }

    public void end(int numElements) {
        // Compute number of groups
        int groups = (numElements + localSizeX - 1) / localSizeX;

        // Dispatch compute shader
        glDispatchCompute(groups, 1, 1);

        // Ensure writes are visible
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    }

    /** Activate the compute shader program for dispatch */
    public void bind() {
        glUseProgram(programId);
    }

    /** Stop using any compute shader program */
    public static void unbind() {
        glUseProgram(0);
    }

    /** Returns the program ID (for SSBO binding etc.) */
    public int getProgramId() {
        return programId;
    }
}
