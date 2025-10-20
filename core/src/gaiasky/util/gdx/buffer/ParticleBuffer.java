/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.buffer;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL43.*;

/**
 * A particle buffer to hold particles generated with a {@link gaiasky.util.gdx.shader.ComputeShaderProgram}.
 * <p>Here is a usage example:</p>
 * <p>
 * <pre>{@code
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
 * }</pre>
 * </p>
 */
public class ParticleBuffer {
    private final int ssbo;
    private final int numParticles;

    public ParticleBuffer(int numParticles) {
        this.numParticles = numParticles;
        int stride = 8 * Float.BYTES; // vec4 position + vec4 color

        ssbo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
        glBufferData(GL_SHADER_STORAGE_BUFFER, (long) numParticles * stride, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    /** Bind the buffer to a given binding point (matching GLSL layout(binding=X)) */
    public void bind(int binding) {
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, binding, ssbo);
    }

    /** Read back the buffer data (optional, slower) */
    public FloatBuffer getData() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
        FloatBuffer buf = glMapBuffer(GL_SHADER_STORAGE_BUFFER, GL_READ_ONLY).asFloatBuffer();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        return buf;
    }

    /** Unmap buffer after read */
    public void unmap() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
        glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    public int getId() {
        return ssbo;
    }

    public void cleanup() {
        glDeleteBuffers(ssbo);
    }
}
