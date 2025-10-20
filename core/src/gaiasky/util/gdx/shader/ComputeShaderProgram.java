/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import org.lwjgl.opengl.GL43;

import java.io.IOException;

import static org.lwjgl.opengl.GL43.glUseProgram;

/**
 * A compute shader. Requires OpenGL 4.3+ to work.
 */
public class ComputeShaderProgram {
    private final String name;
    private final String shaderCode;
    private int programId;
    private boolean isCompiled = false;

    /**
     * Loads and compiles a compute shader from a file path.
     *
     * @param name       Name of the compute shader.
     * @param shaderCode The code of the compute shader.
     *
     * @throws IOException if file cannot be read.
     */
    public ComputeShaderProgram(String name, String shaderCode) throws IOException {
        this.name = name;
        this.shaderCode = shaderCode;
        compile();
    }

    public void compile() {
        if (!isCompiled) {
            var cache = ShaderCache.instance();
            var comp = cache.compileShaders(name, shaderCode);

            this.isCompiled = comp[0] == GL43.GL_TRUE;
            this.programId = comp[1];
        }
    }

    /** Activate the compute shader program for dispatch */
    public void bind() {
        glUseProgram(programId);
    }

    /** Stop using any compute shader program */
    public static void unbind() {
        glUseProgram(0);
    }

}
