/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.i18n.I18n;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL41;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;

/**
 * Implements shader caching to disk.
 */
public class ShaderCache {
    private static final Logger.Log logger = Logger.getLogger(ShaderCache.class);

    public synchronized static ShaderCache instance() {
        if (instance == null) {
            instance = new ShaderCache();
        }
        return instance;
    }

    private static ShaderCache instance;
    private final static String DEFAULT_SHADER_NAME = "default";

    private final boolean cacheEnabled;

    /**
     * Int buffer for types.
     **/
    protected IntBuffer type = BufferUtils.newIntBuffer(1);
    /**
     * Int for shader length.
     */
    protected IntBuffer len = BufferUtils.newIntBuffer(1);
    /**
     * Int for shader binary format.
     */
    protected IntBuffer format = BufferUtils.newIntBuffer(1);
    /**
     * Aux integer buffer.
     */
    protected IntBuffer intBuffer = BufferUtils.newIntBuffer(1);
    /**
     * Byte buffer to get binary shaders.
     */
    protected ByteBuffer byteBuffer = ByteBuffer.allocateDirect(56000);

    protected int program = -1;
    /**
     * Output log.
     */
    protected String log;
    /**
     * Compiled flag.
     */
    boolean isCompiled = false;

    public ShaderCache() {
        // Cache is enabled if we use OpenGL 4.1+, for we need to access the binary format of shaders.
        cacheEnabled = Gdx.graphics.getGLVersion().isVersionEqualToOrHigher(4, 1) &&
                Settings.settings.program.shaderCache &&
                !Settings.settings.program.safeMode;
    }

    public boolean isCompiled() {
        return isCompiled;
    }

    public String getLog() {
        return log;
    }

    public int getProgram() {
        return program;
    }


    public void clear() {
        isCompiled = false;
        log = null;
        program = -1;

        type.rewind();
        len.rewind();
        format.rewind();
        intBuffer.rewind();
        byteBuffer.rewind();
    }

    /**
     * Checks the shader cache with the given name and code.
     * @param name The name of the shader.
     * @param code Concatenation of the code for all the shader stages.
     * @return The program handle (>= 0) if the cache was hit successfully, -1 otherwise.
     */
    private int checkCache(String name,
                           String code) {
        if (!cacheEnabled) {
            return -1;
        }

        int program = -1;

        var hash = code.hashCode();
        var cacheLocation = SysUtils.getShaderCacheDir();

        try {
            Files.createDirectories(cacheLocation);
        } catch (IOException e) {
            logger.error(e);
        }
        GL41.glGetIntegerv(GL41.GL_NUM_PROGRAM_BINARY_FORMATS, intBuffer);

        String pattern = name + "_" + hash + "_*.bin";
        var fullPattern = cacheLocation.resolve(pattern);
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + fullPattern);
        try (var paths = Files.find(cacheLocation, 1, (path, f) -> pathMatcher.matches(path))) {
            var l = paths.toList();
            for (var cacheFile : l) {
                if (Files.exists(cacheFile) && Files.isReadable(cacheFile)) {
                    // Hit.
                    try {
                        var fileName = cacheFile.getFileName().toString();
                        var formatInt = Integer.parseInt(fileName.substring(fileName.lastIndexOf("_") + 1, fileName.lastIndexOf(".")));
                        format.rewind().put(formatInt).rewind();

                        // Load data.
                        var bytes = Files.readAllBytes(cacheFile);
                        var buffer = ByteBuffer.allocateDirect(bytes.length);
                        buffer.put(bytes);
                        var s = new String(bytes, StandardCharsets.UTF_8);
                        buffer.rewind();

                        program = createProgram();
                        GL41.glProgramBinary(program, formatInt, buffer);

                        GL33.glGetProgramiv(program, GL33.GL_LINK_STATUS, intBuffer);
                        int linked = intBuffer.get(0);
                        if (linked == 0) {
                            log = GL33.glGetProgramInfoLog(program);
                            program = -1;
                        } else {
                            isCompiled = true;
                        }
                        return program;

                    } catch (IOException e) {
                        throw new ShaderCacheException(e);
                    }
                }
            }
        } catch (IOException e) {
            throw new ShaderCacheException(e);
        }
        return program;
    }

    /**
     * Puts the binary data of the given format in the disk cache (saves a file).
     * @param program The program handle.
     * @param name The name of the shader.
     * @param code Concatenation of the code for all the shader stages.
     * @return True if the program was put in the cache successfully, false otherwise.
     */
    private boolean putInCache(int program,
                               String name,
                               String code) {
        if (!cacheEnabled) {
            return false;
        }

        var hash = code.hashCode();
        var cacheLocation = SysUtils.getShaderCacheDir();

        len.rewind();
        GL33.glGetProgramiv(program, GL41.GL_PROGRAM_BINARY_LENGTH, len);
        int binaryLength = len.get();
        len.rewind();

        // Get binary shader.
        if (byteBuffer.capacity() < binaryLength) {
            byteBuffer = ByteBuffer.allocateDirect(binaryLength);
        } else {
            byteBuffer.clear();
        }
        GL41.glGetProgramBinary(program, len, format, byteBuffer);

        int err = GL41.glGetError();
        if (err == GL41.GL_NO_ERROR) {
            // Save to disk.
            int formatInt = format.get();

            var cacheFileName = name + "_" + hash + "_" + formatInt + ".bin";
            var cacheFile = cacheLocation.resolve(cacheFileName);
            int remaining = len.get();
            try (FileOutputStream fos = new FileOutputStream(cacheFile.toFile())) {
                while (byteBuffer.hasRemaining() && remaining > 0) {
                    fos.write(byteBuffer.get());
                    remaining--;
                }
            } catch (IOException e) {
                logger.error(e);
                return false;
            }
            return true;
        } else {
            String log = GL33.glGetProgramInfoLog(program);
            logger.error(log);
            return false;
        }
    }

    /**
     * Loads and compiles the shaders, creates a new program and links the shaders.
     *
     * @param name           The name of the shader.
     * @param vertexShader   The vertex shader code.
     * @param tessControlShader The tessellation control shader code.
     * @param tessEvalShader The tessellation evaluation shader code.
     * @param fragmentShader The fragment shader code.
     * @return Integer array with the program handle, and the handle of every shader stage.
     *
     */
    public int[] compileShaders(String name,
                                String vertexShader,
                                String tessControlShader,
                                String tessEvalShader,
                                String fragmentShader) {

        clear();

        boolean logCompile = true;
        if (name == null) {
            name = DEFAULT_SHADER_NAME;
            logCompile = false;
        }

        program = checkCache(name, vertexShader + tessControlShader + tessEvalShader + fragmentShader);

        // Load and compile shaders in the usual way.
        if (program == -1) {
            if (logCompile)
                logger.info(I18n.msg("notif.shader.compile", name));

            int vertexShaderHandle = loadShader(GL20.GL_VERTEX_SHADER, vertexShader);
            int controlShaderHandle = loadShader(GL41.GL_TESS_CONTROL_SHADER, tessControlShader);
            int evaluationShaderHandle = loadShader(GL41.GL_TESS_EVALUATION_SHADER, tessEvalShader);
            int fragmentShaderHandle = loadShader(GL20.GL_FRAGMENT_SHADER, fragmentShader);
            logger.debug(I18n.msg("notif.shader.load.handle", vertexShaderHandle, fragmentShaderHandle));

            if (vertexShaderHandle == -1 || controlShaderHandle == -1 || evaluationShaderHandle == -1 || fragmentShaderHandle == -1) {
                isCompiled = false;
                return new int[]{program, vertexShaderHandle, controlShaderHandle, evaluationShaderHandle, fragmentShaderHandle};
            }

            program = linkProgram(createProgram(), vertexShaderHandle, controlShaderHandle, evaluationShaderHandle, fragmentShaderHandle);
            if (program == -1) {
                isCompiled = false;
                return new int[]{program, vertexShaderHandle, controlShaderHandle, evaluationShaderHandle, fragmentShaderHandle};
            } else {
                // Cache.
                if (putInCache(program, name, vertexShader + controlShaderHandle + evaluationShaderHandle + fragmentShader)) {
                    logger.debug("Shader " + name + " saved to cache");
                }
            }

            isCompiled = true;
            return new int[]{program, vertexShaderHandle, controlShaderHandle, evaluationShaderHandle, fragmentShaderHandle};
        } else {
            logger.debug(I18n.msg("notif.shader.cache", name));
        }
        return new int[]{program, 0, 0, 0, 0};

    }

    /**
     * Loads and compiles the shaders, creates a new program and links the shaders.
     *
     * @param name           The name of the shader.
     * @param vertexShader   The vertex shader code.
     * @param geometryShader The geometry shader code.
     * @param fragmentShader The fragment shader code.
     * @return Integer array with the program handle, and the handle of every shader stage.
     */
    public int[] compileShaders(String name,
                                String vertexShader,
                                String geometryShader,
                                String fragmentShader) {

        clear();

        boolean logCompile = true;
        if (name == null) {
            name = DEFAULT_SHADER_NAME;
            logCompile = false;
        }

        program = checkCache(name, vertexShader + geometryShader + fragmentShader);

        // Load and compile shaders in the usual way.
        if (program == -1) {
            if (logCompile)
                logger.info(I18n.msg("notif.shader.compile", name));

            int vertexShaderHandle = loadShader(GL20.GL_VERTEX_SHADER, vertexShader);
            int geometryShaderHandle = loadShader(GL32.GL_GEOMETRY_SHADER, geometryShader);
            int fragmentShaderHandle = loadShader(GL20.GL_FRAGMENT_SHADER, fragmentShader);
            logger.debug(I18n.msg("notif.shader.load.handle", vertexShaderHandle, fragmentShaderHandle));

            if (vertexShaderHandle == -1 || geometryShaderHandle == -1 || fragmentShaderHandle == -1) {
                isCompiled = false;
                return new int[]{program, vertexShaderHandle, geometryShaderHandle, fragmentShaderHandle};
            }

            program = linkProgram(createProgram(), vertexShaderHandle, geometryShaderHandle, fragmentShaderHandle);
            if (program == -1) {
                isCompiled = false;
                return new int[]{program, vertexShaderHandle, geometryShaderHandle, fragmentShaderHandle};
            } else {
                // Cache.
                if (putInCache(program, name, vertexShader + geometryShader + fragmentShader)) {
                    logger.debug("Shader " + name + " saved to cache");
                }
            }

            isCompiled = true;
            return new int[]{program, vertexShaderHandle, geometryShaderHandle, fragmentShaderHandle};
        } else {
            logger.debug(I18n.msg("notif.shader.cache", name));
        }
        return new int[]{program, 0, 0, 0};
    }

    /**
     * Loads and compiles the shaders, creates a new program and links the shaders.
     *
     * @param name           The name of the shader.
     * @param vertexShader   The vertex shader code.
     * @param fragmentShader The fragment shader code.
     * @return Integer array with the program handle, and the handle of every shader stage.
     */
    public int[] compileShaders(String name,
                                String vertexShader,
                                String fragmentShader) {

        clear();

        boolean logCompile = true;
        if (name == null) {
            name = DEFAULT_SHADER_NAME;
            logCompile = false;
        }

        program = checkCache(name, vertexShader + fragmentShader);

        // Load and compile shaders in the usual way.
        if (program == -1) {
            if (logCompile)
                logger.info(I18n.msg("notif.shader.compile", name));

            int vertexShaderHandle = loadShader(GL20.GL_VERTEX_SHADER, vertexShader);
            int fragmentShaderHandle = loadShader(GL20.GL_FRAGMENT_SHADER, fragmentShader);
            logger.debug(I18n.msg("notif.shader.load.handle", vertexShaderHandle, fragmentShaderHandle));

            if (vertexShaderHandle == -1 || fragmentShaderHandle == -1) {
                isCompiled = false;
                return new int[]{program, vertexShaderHandle, fragmentShaderHandle};
            }

            program = linkProgram(createProgram(), vertexShaderHandle, fragmentShaderHandle);
            if (program == -1) {
                isCompiled = false;
                return new int[]{program, vertexShaderHandle, fragmentShaderHandle};
            } else {
                // Cache.
                try {
                    if (putInCache(program, name, vertexShader + fragmentShader)) {
                        logger.debug("Shader " + name + " saved to cache");
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            }

            isCompiled = true;
            return new int[]{program, vertexShaderHandle, fragmentShaderHandle};
        } else {
            logger.debug(I18n.msg("notif.shader.cache", name));
        }
        return new int[]{program, 0, 0};
    }

    protected int createProgram() {

        int program = GL33.glCreateProgram();
        return program != 0 ? program : -1;
    }

    private int loadShader(int type,
                           String source) {

        int shader = GL41.glCreateShader(type);
        if (shader == 0) return -1;

        intBuffer.rewind();

        GL41.glShaderSource(shader, source);
        GL41.glCompileShader(shader);
        GL41.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, intBuffer);

        int compiled = intBuffer.get(0);
        if (compiled == 0) {
            String infoLog = GL41.glGetShaderInfoLog(shader);
            switch (type) {
                case GL41.GL_VERTEX_SHADER -> log += "Vertex shader\n";
                case GL41.GL_FRAGMENT_SHADER -> log += "Fragment shader\n";
                case GL41.GL_GEOMETRY_SHADER -> log += "Geometry shader\n";
                case GL41.GL_TESS_CONTROL_SHADER -> log += "Tessellation control shader\n";
                case GL41.GL_TESS_EVALUATION_SHADER -> log += "Tessellation evaluation shader\n";
            }
            log += infoLog;
            // }
            return -1;
        }

        return shader;
    }

    private int linkProgram(int program,
                            int vertexShaderHandle,
                            int fragmentShaderHandle) {

        if (program == -1)
            return -1;

        GL33.glAttachShader(program, vertexShaderHandle);
        GL33.glAttachShader(program, fragmentShaderHandle);
        GL33.glLinkProgram(program);
        GL33.glDetachShader(program, vertexShaderHandle);
        GL33.glDetachShader(program, fragmentShaderHandle);

        GL33.glGetProgramiv(program, GL33.GL_LINK_STATUS, intBuffer);
        int linked = intBuffer.get(0);
        if (linked == 0) {
            log = GL33.glGetProgramInfoLog(program);
            return -1;
        }

        return program;
    }

    private int linkProgram(int program,
                            int vertexShaderHandle,
                            int geometryShaderHandle,
                            int fragmentShaderHandle) {
        if (program == -1)
            return -1;

        GL33.glAttachShader(program, vertexShaderHandle);
        GL33.glAttachShader(program, geometryShaderHandle);
        GL33.glAttachShader(program, fragmentShaderHandle);
        GL33.glLinkProgram(program);
        GL33.glDetachShader(program, vertexShaderHandle);
        GL33.glDetachShader(program, geometryShaderHandle);
        GL33.glDetachShader(program, fragmentShaderHandle);

        GL33.glGetProgramiv(program, GL33.GL_LINK_STATUS, intBuffer);
        int linked = intBuffer.get(0);
        if (linked == 0) {
            log = GL33.glGetProgramInfoLog(program);
            return -1;
        }

        return program;
    }

    private int linkProgram(int program,
                            int vertexShaderHandle,
                            int controlShaderHandle,
                            int evaluationShaderHandle,
                            int fragmentShaderHandle) {

        if (program == -1) return -1;

        GL41.glAttachShader(program, vertexShaderHandle);
        GL41.glAttachShader(program, controlShaderHandle);
        GL41.glAttachShader(program, evaluationShaderHandle);
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

    public static class ShaderCacheException extends RuntimeException {

        public ShaderCacheException(Throwable e) {
            super(e);
        }

        public ShaderCacheException(String e) {
            super(e);
        }

    }

}
