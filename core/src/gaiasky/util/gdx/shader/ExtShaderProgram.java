/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.*;
import gaiasky.render.GaiaSkyShaderCompileException;
import gaiasky.util.ChecksumRunnable;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL42;

import java.lang.StringBuilder;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ExtShaderProgram implements Disposable {
    private static final Log logger = Logger.getLogger(ExtShaderProgram.class);

    /**
     * Default name for position attributes.
     **/
    public static final String POSITION_ATTRIBUTE = "a_position";
    /**
     * Default name for color attributes.
     **/
    public static final String COLOR_ATTRIBUTE = "a_color";
    /**
     * Default name for normal attribute.
     **/
    public static final String NORMAL_ATTRIBUTE = "a_normal";
    /**
     * Default name for texture coordinates attributes, append texture unit number.
     **/
    public static final String TEXCOORD_ATTRIBUTE = "a_texCoord";

    /**
     * The list of currently available shaders.
     **/
    private final static ObjectMap<Application, Array<ExtShaderProgram>> shaders = new ObjectMap<>();
    /**
     * Flag indicating whether attributes & uniforms must be present at all times.
     **/
    public static boolean pedantic = true;
    /**
     * Code that is always added to the vertex shader code, typically used to inject a #version line. Note that this is added
     * as-is, you should include a newline (`\n`) if needed.
     */
    public static String prependVertexCode = "";
    /**
     * Code that is always added to the geometry shader code, typically used to inject a #version line. Note that this is added
     * as-is, you should include a newline (`\n`) if needed.
     */
    public static String prependGeometryCode = "";
    /**
     * Code that is always added to every fragment shader code, typically used to inject a #version line. Note that this is added
     * as-is, you should include a newline (`\n`) if needed.
     */
    public static String prependFragmentCode = "";

    /**
     * Int buffer for parameters.
     **/
    protected IntBuffer params = BufferUtils.newIntBuffer(1);
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
    protected ByteBuffer byteBuffer = ByteBuffer.allocateDirect(50000);
    /**
     * The log.
     **/
    protected String log = "";
    /**
     * The shader name, if any.
     **/
    protected String name;
    /**
     * Whether this program compiled successfully.
     **/
    protected boolean isCompiled;
    /**
     * Whether lazy loading is activated for this shader.
     **/
    protected boolean isLazy;
    /**
     * Whether this program has been disposed.
     **/
    protected boolean isDisposed;
    /**
     * Uniform lookup.
     **/
    protected ObjectIntMap<String> uniforms;
    /**
     * Uniform types.
     **/
    protected ObjectIntMap<String> uniformTypes;
    /**
     * Uniform sizes.
     **/
    protected ObjectIntMap<String> uniformSizes;
    /**
     * Uniform names.
     **/
    protected String[] uniformNames;
    /**
     * Attribute lookup.
     **/
    protected ObjectIntMap<String> attributes;
    /**
     * Attribute types.
     **/
    protected ObjectIntMap<String> attributeTypes;
    /**
     * Attribute sizes.
     **/
    protected ObjectIntMap<String> attributeSizes;
    /**
     * Attribute names.
     **/
    protected String[] attributeNames;
    /**
     * Program handle.
     **/
    protected int program;
    /**
     * Vertex shader handle.
     **/
    protected int vertexShaderHandle;
    /**
     * Geometry shader handle.
     **/
    private int geometryShaderHandle;
    /**
     * Fragment shader handle.
     **/
    protected int fragmentShaderHandle;
    /**
     * Vertex shader source.
     **/
    protected String vertexShaderSource;
    /**
     * Geometry shader source.
     **/
    protected String geometryShaderSource;
    /**
     * Fragment shader source.
     **/
    protected String fragmentShaderSource;

    protected String vertexShaderFile, geometryShaderFile, fragmentShaderFile;
    /**
     * Whether this shader was invalidated.
     **/
    protected boolean invalidated;

    public ExtShaderProgram() {
    }

    public ExtShaderProgram(String vertexFile,
                            String fragmentFile,
                            String vertexShaderCode,
                            String fragmentShaderCode) {
        this(null, vertexFile, fragmentFile, vertexShaderCode, fragmentShaderCode, false);
    }

    public ExtShaderProgram(String vertexFile,
                            String geometryFile,
                            String fragmentFile,
                            String vertexShaderCode,
                            String geometryShaderCode,
                            String fragmentShaderCode) {
        this(null, vertexFile, geometryFile, fragmentFile, vertexShaderCode, geometryShaderCode, fragmentShaderCode, false);
    }

    public ExtShaderProgram(String name,
                            String vertexFile,
                            String geometryFile,
                            String fragmentFile,
                            String vertexShaderCode,
                            String geometryShaderCode,
                            String fragmentShaderCode) {
        this(name, vertexFile, geometryFile, fragmentFile, vertexShaderCode, geometryShaderCode, fragmentShaderCode, false);
    }

    public ExtShaderProgram(String name,
                            String vertexFile,
                            String fragmentFile,
                            String vertexShaderCode,
                            String fragmentShaderCode) {
        this(name, vertexFile, fragmentFile, vertexShaderCode, fragmentShaderCode, false);
    }

    /**
     * Constructs a new ShaderProgram and immediately compiles it.
     *
     * @param name               The shader name, if any.
     * @param vertexFile         The vertex shader file.
     * @param fragmentFile       The fragment shader file.
     * @param vertexShaderCode   The vertex shader code.
     * @param fragmentShaderCode The fragment shader code.
     * @param lazyLoading        Whether to use lazy loading, only preparing the data without actually compiling the shaders.
     */
    public ExtShaderProgram(String name,
                            String vertexFile,
                            String fragmentFile,
                            String vertexShaderCode,
                            String fragmentShaderCode,
                            boolean lazyLoading) {
        this(name, vertexFile, null, fragmentFile, vertexShaderCode, null, fragmentShaderCode, lazyLoading);
    }

    /**
     * Constructs a new shader program and immediately compiles it, if it is not lazy.
     *
     * @param name               The shader name, if any.
     * @param vertexFile         The vertex shader file.
     * @param geometryFile       The geometry shader file.
     * @param fragmentFile       The fragment shader file.
     * @param vertexShaderCode   The vertex shader code.
     * @param geometryShaderCode The geometry shader code.
     * @param fragmentShaderCode The fragment shader code.
     * @param lazyLoading        Whether to use lazy loading, only preparing the data without actually compiling the shaders.
     */
    public ExtShaderProgram(String name,
                            String vertexFile,
                            String geometryFile,
                            String fragmentFile,
                            String vertexShaderCode,
                            String geometryShaderCode,
                            String fragmentShaderCode,
                            boolean lazyLoading) {
        if (vertexShaderCode == null)
            throw new IllegalArgumentException("vertex shader must not be null");
        if (fragmentShaderCode == null)
            throw new IllegalArgumentException("fragment shader must not be null");

        if (prependVertexCode != null && !prependVertexCode.isEmpty())
            vertexShaderCode = prependVertexCode + vertexShaderCode;
        if (geometryShaderCode != null && prependGeometryCode != null && !prependGeometryCode.isEmpty())
            geometryShaderCode = prependGeometryCode + geometryShaderCode;
        if (prependFragmentCode != null && !prependFragmentCode.isEmpty())
            fragmentShaderCode = prependFragmentCode + fragmentShaderCode;

        this.isLazy = lazyLoading;
        this.name = name;

        // Sources.
        this.vertexShaderSource = vertexShaderCode;
        this.geometryShaderSource = geometryShaderCode;
        this.fragmentShaderSource = fragmentShaderCode;

        // Files.
        this.vertexShaderFile = vertexFile;
        this.geometryShaderFile = geometryFile;
        this.fragmentShaderFile = fragmentFile;

        if (!lazyLoading) {
            compile(name);
        }
    }

    /**
     * Constructs a new ShaderProgram and immediately compiles it.
     *
     * @param vertexShader   The vertex shader code.
     * @param fragmentShader The fragment shader code.
     */
    public ExtShaderProgram(String vertexShader,
                            String fragmentShader) {
        this(null, null, vertexShader, fragmentShader);
    }

    public ExtShaderProgram(FileHandle vertexShader,
                            FileHandle fragmentShader) {
        this(vertexShader.readString(), fragmentShader.readString());
    }

    public ExtShaderProgram(String name,
                            FileHandle vertexShader,
                            FileHandle fragmentShader) {
        this(name, null, null, vertexShader.readString(), fragmentShader.readString());
    }

    /**
     * Invalidates all shaders so the next time they are used new handles are generated
     *
     * @param app The application.
     */
    public static void invalidateAllShaderPrograms(Application app) {
        if (Gdx.gl20 == null)
            return;

        Array<ExtShaderProgram> shaderArray = shaders.get(app);
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

    protected void initializeLocalAssets() {
        uniforms = new ObjectIntMap<>();
        uniformTypes = new ObjectIntMap<>();
        uniformSizes = new ObjectIntMap<>();
        attributes = new ObjectIntMap<>();
        attributeTypes = new ObjectIntMap<>();
        attributeSizes = new ObjectIntMap<>();
    }

    public String getName() {
        return name;
    }

    public void compile(String name) {
        if (!isCompiled) {
            initializeLocalAssets();

            if (vertexShaderFile != null || fragmentShaderFile != null) {
                if (geometryShaderFile != null) {
                    logger.debug(I18n.msg("notif.shader.load.geom", vertexShaderFile, geometryShaderFile, fragmentShaderFile));
                } else {
                    logger.debug(I18n.msg("notif.shader.load", vertexShaderFile, fragmentShaderFile));
                }
            }

            if (geometryShaderSource != null) {
                compileShaders(name, vertexShaderSource, geometryShaderSource, fragmentShaderSource);
            } else {
                compileShaders(name, vertexShaderSource, fragmentShaderSource);
            }
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
     * Loads and compiles the shaders, creates a new program and links the shaders.
     *
     * @param name           The name of the shader.
     * @param vertexShader   The vertex shader code.
     * @param geometryShader The geometry shader code.
     * @param fragmentShader The fragment shader code.
     */
    private void compileShaders(String name,
                                String vertexShader,
                                String geometryShader,
                                String fragmentShader) {

        var cache = ShaderCache.instance();
        int[] handles = cache.compileShaders(name, vertexShader, geometryShader, fragmentShader);
        program = handles[0];
        vertexShaderHandle = handles[1];
        geometryShaderHandle = handles[2];
        fragmentShaderHandle = handles[3];

        isCompiled = cache.isCompiled();
        log = cache.getLog();
    }

    /**
     * Loads and compiles the shaders, creates a new program and links the shaders.
     *
     * @param name           The name of the shader.
     * @param vertexShader   The vertex shader code.
     * @param fragmentShader The fragment shader code.
     */
    private void compileShaders(String name,
                                String vertexShader,
                                String fragmentShader) {

        var cache = ShaderCache.instance();
        int[] handles = cache.compileShaders(name, vertexShader, fragmentShader);
        program = handles[0];
        vertexShaderHandle = handles[1];
        fragmentShaderHandle = handles[2];

        isCompiled = cache.isCompiled();
        log = cache.getLog();
    }

    /**
     * @return the log info for the shader compilation and program linking stage. The shader needs to be bound for this method to
     * have an effect.
     */
    public String getLog() {
        if (isCompiled) {
            log = Gdx.gl20.glGetProgramInfoLog(program);
        }
        return log;
    }

    /**
     * @return whether this program compiled successfully.
     */
    public boolean isCompiled() {
        return isCompiled;
    }

    /**
     * @return whether this program has lazy loading activated.
     */
    public boolean isLazy() {
        return isLazy;
    }

    protected int fetchAttributeLocation(String name) {
        GL20 gl = Gdx.gl20;
        // -2 == not yet cached
        // -1 == cached but not found
        int location;
        if ((location = attributes.get(name, -2)) == -2) {
            location = gl.glGetAttribLocation(program, name);
            attributes.put(name, location);
        }
        return location;
    }

    protected int fetchUniformLocation(String name) {
        return fetchUniformLocation(name, pedantic);
    }

    public int fetchUniformLocation(String name,
                                    boolean pedantic) {
        GL20 gl = Gdx.gl20;
        // -2 == not yet cached
        // -1 == cached but not found
        int location;
        if ((location = uniforms.get(name, -2)) == -2) {
            location = gl.glGetUniformLocation(program, name);
            if (location == -1 && pedantic)
                throw new IllegalArgumentException("no uniform with name '" + name + "' in shader");
            uniforms.put(name, location);
        }
        return location;
    }

    /**
     * Sets the uniform with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name  the name of the uniform
     * @param value the value
     */
    public void setUniformi(String name,
                            int value) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform1i(location, value);
    }

    public void setUniformi(int location,
                            int value) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniform1i(location, value);
    }

    /**
     * Sets the uniform with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     */
    public void setUniformi(String name,
                            int value1,
                            int value2) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform2i(location, value1, value2);
    }

    public void setUniformi(int location,
                            int value1,
                            int value2) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniform2i(location, value1, value2);
    }

    /**
     * Sets the uniform with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     */
    public void setUniformi(String name,
                            int value1,
                            int value2,
                            int value3) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform3i(location, value1, value2, value3);
    }

    public void setUniformi(int location,
                            int value1,
                            int value2,
                            int value3) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniform3i(location, value1, value2, value3);
    }

    /**
     * Sets the uniform with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     * @param value4 the fourth value
     */
    public void setUniformi(String name,
                            int value1,
                            int value2,
                            int value3,
                            int value4) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform4i(location, value1, value2, value3, value4);
    }

    public void setUniformi(int location,
                            int value1,
                            int value2,
                            int value3,
                            int value4) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniform4i(location, value1, value2, value3, value4);
    }

    /**
     * Sets the uniform with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name  the name of the uniform
     * @param value the value
     */
    public void setUniformf(String name,
                            float value) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform1f(location, value);
    }

    public void setUniformf(int location,
                            float value) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniform1f(location, value);
    }

    /**
     * Sets the uniform with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     */
    public void setUniformf(String name,
                            float value1,
                            float value2) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform2f(location, value1, value2);
    }

    public void setUniformf(int location,
                            float value1,
                            float value2) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniform2f(location, value1, value2);
    }

    /**
     * Sets the uniform with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     */
    public void setUniformf(String name,
                            float value1,
                            float value2,
                            float value3) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform3f(location, value1, value2, value3);
    }

    public void setUniformf(int location,
                            float value1,
                            float value2,
                            float value3) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniform3f(location, value1, value2, value3);
    }

    /**
     * Sets the uniform with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     * @param value4 the fourth value
     */
    public void setUniformf(String name,
                            float value1,
                            float value2,
                            float value3,
                            float value4) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform4f(location, value1, value2, value3, value4);
    }

    public void setUniformf(int location,
                            float value1,
                            float value2,
                            float value3,
                            float value4) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniform4f(location, value1, value2, value3, value4);
    }

    public void setUniform1fv(String name,
                              float[] values,
                              int offset,
                              int length) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform1fv(location, length, values, offset);
    }

    public void setUniform1fv(int location,
                              float[] values,
                              int offset,
                              int length) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniform1fv(location, length, values, offset);
    }

    public void setUniform2fv(String name,
                              float[] values,
                              int offset,
                              int length) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform2fv(location, length / 2, values, offset);
    }

    public void setUniform2fv(int location,
                              float[] values,
                              int offset,
                              int length) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniform2fv(location, length / 2, values, offset);
    }

    public void setUniform3fv(String name,
                              float[] values,
                              int offset,
                              int length) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform3fv(location, length / 3, values, offset);
    }

    public void setUniform3fv(int location,
                              float[] values,
                              int offset,
                              int length) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniform3fv(location, length / 3, values, offset);
    }

    public void setUniform4fv(String name,
                              float[] values,
                              int offset,
                              int length) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform4fv(location, length / 4, values, offset);
    }

    public void setUniform4fv(int location,
                              float[] values,
                              int offset,
                              int length) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniform4fv(location, length / 4, values, offset);
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
        checkManaged();
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
        checkManaged();
        gl.glUniformMatrix3fv(location, 1, transpose, matrix.val, 0);
    }

    /**
     * Sets an array of uniform matrices with the given name. The {@link ExtShaderProgram} must be bound for this to work.
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
        checkManaged();
        buffer.position(0);
        int location = fetchUniformLocation(name);
        gl.glUniformMatrix3fv(location, count, transpose, buffer);
    }

    /**
     * Sets an array of uniform matrices with the given name. The {@link ExtShaderProgram} must be bound for this to work.
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
        checkManaged();
        buffer.position(0);
        int location = fetchUniformLocation(name);
        gl.glUniformMatrix4fv(location, count, transpose, buffer);
    }

    public void setUniformMatrix4fv(int location,
                                    float[] values,
                                    int offset,
                                    int length) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUniformMatrix4fv(location, length / 16, false, values, offset);
    }

    public void setUniformMatrix4fv(String name,
                                    float[] values,
                                    int offset,
                                    int length) {
        setUniformMatrix4fv(fetchUniformLocation(name), values, offset, length);
    }

    /**
     * Sets the uniform with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param values x and y as the first and second values respectively
     */
    public void setUniformf(String name,
                            Vector2 values) {
        setUniformf(name, values.x, values.y);
    }

    public void setUniformf(int location,
                            Vector2 values) {
        setUniformf(location, values.x, values.y);
    }

    /**
     * Sets the uniform with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param values x, y and z as the first, second and third values respectively
     */
    public void setUniformf(String name,
                            Vector3 values) {
        setUniformf(name, values.x, values.y, values.z);
    }

    public void setUniformf(int location,
                            Vector3 values) {
        setUniformf(location, values.x, values.y, values.z);
    }

    /**
     * Sets the uniform with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param values r, g, b and a as the first through fourth values respectively
     */
    public void setUniformf(String name,
                            Color values) {
        setUniformf(name, values.r, values.g, values.b, values.a);
    }

    public void setUniformf(int location,
                            Color values) {
        setUniformf(location, values.r, values.g, values.b, values.a);
    }

    /**
     * Sets the vertex attribute with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name      the attribute name
     * @param size      the number of components, must be >= 1 and <= 4
     * @param type      the type, must be one of GL20.GL_BYTE, GL20.GL_UNSIGNED_BYTE, GL20.GL_SHORT,
     *                  GL20.GL_UNSIGNED_SHORT,GL20.GL_FIXED, or GL20.GL_FLOAT. GL_FIXED will not work on the desktop
     * @param normalize whether fixed point data should be normalized. Will not work on the desktop
     * @param stride    the stride in bytes between successive attributes
     * @param buffer    the buffer containing the vertex attributes.
     */
    public void setVertexAttribute(String name,
                                   int size,
                                   int type,
                                   boolean normalize,
                                   int stride,
                                   Buffer buffer) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchAttributeLocation(name);
        if (location == -1)
            return;
        gl.glVertexAttribPointer(location, size, type, normalize, stride, buffer);
    }

    public void setVertexAttribute(int location,
                                   int size,
                                   int type,
                                   boolean normalize,
                                   int stride,
                                   Buffer buffer) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glVertexAttribPointer(location, size, type, normalize, stride, buffer);
    }

    /**
     * Sets the vertex attribute with the given name. The {@link ExtShaderProgram} must be bound for this to work.
     *
     * @param name      the attribute name
     * @param size      the number of components, must be >= 1 and <= 4
     * @param type      the type, must be one of GL20.GL_BYTE, GL20.GL_UNSIGNED_BYTE, GL20.GL_SHORT,
     *                  GL20.GL_UNSIGNED_SHORT,GL20.GL_FIXED, or GL20.GL_FLOAT. GL_FIXED will not work on the desktop
     * @param normalize whether fixed point data should be normalized. Will not work on the desktop
     * @param stride    the stride in bytes between successive attributes
     * @param offset    byte offset into the vertex buffer object bound to GL20.GL_ARRAY_BUFFER.
     */
    public void setVertexAttribute(String name,
                                   int size,
                                   int type,
                                   boolean normalize,
                                   int stride,
                                   int offset) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchAttributeLocation(name);
        if (location == -1)
            return;
        gl.glVertexAttribPointer(location, size, type, normalize, stride, offset);
    }

    public void setVertexAttribute(int location,
                                   int size,
                                   int type,
                                   boolean normalize,
                                   int stride,
                                   int offset) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glVertexAttribPointer(location, size, type, normalize, stride, offset);
    }

    /**
     * Makes OpenGL ES 2.0 use this vertex and fragment shader pair. When you are done with this shader you have to call
     * {@link ExtShaderProgram#end()}.
     */
    public void begin() {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glUseProgram(program);
    }

    /**
     * Disables this shader. Must be called when one is done with the shader. Don't mix it with dispose, that will release the
     * shader resources.
     */
    public void end() {
        GL20 gl = Gdx.gl20;
        gl.glUseProgram(0);
    }

    /**
     * Disables the vertex attribute with the given name
     *
     * @param name the vertex attribute name
     */
    public void disableVertexAttribute(String name) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchAttributeLocation(name);
        if (location == -1)
            return;
        gl.glDisableVertexAttribArray(location);
    }

    public void disableVertexAttribute(int location) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glDisableVertexAttribArray(location);
    }

    /**
     * Enables the vertex attribute with the given name
     *
     * @param name the vertex attribute name
     */
    public void enableVertexAttribute(String name) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        int location = fetchAttributeLocation(name);
        if (location == -1)
            return;
        gl.glEnableVertexAttribArray(location);
    }

    public void enableVertexAttribute(int location) {
        GL20 gl = Gdx.gl20;
        checkManaged();
        gl.glEnableVertexAttribArray(location);
    }

    private void checkManaged() {
        if (invalidated) {
            if (geometryShaderSource != null) {
                compileShaders(getName(), vertexShaderSource, geometryShaderSource, fragmentShaderSource);
            } else {
                compileShaders(getName(), vertexShaderSource, fragmentShaderSource);
            }
            invalidated = false;
        }
    }

    private void addManagedShader(Application app,
                                  ExtShaderProgram shaderProgram) {
        Array<ExtShaderProgram> managedResources = shaders.get(app);
        if (managedResources == null)
            managedResources = new Array<>();
        managedResources.add(shaderProgram);
        shaders.put(app, managedResources);
    }

    /**
     * Sets the given attribute
     *
     * @param name   the name of the attribute
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     * @param value4 the fourth value
     */
    public void setAttributef(String name,
                              float value1,
                              float value2,
                              float value3,
                              float value4) {
        GL20 gl = Gdx.gl20;
        int location = fetchAttributeLocation(name);
        gl.glVertexAttrib4f(location, value1, value2, value3, value4);
    }

    protected void fetchUniforms() {
        params.clear();
        Gdx.gl20.glGetProgramiv(program, GL20.GL_ACTIVE_UNIFORMS, params);
        int numUniforms = params.get(0);

        uniformNames = new String[numUniforms];

        for (int i = 0; i < numUniforms; i++) {
            params.clear();
            params.put(0, 1);
            type.clear();
            String name = Gdx.gl20.glGetActiveUniform(program, i, params, type);
            int location = Gdx.gl20.glGetUniformLocation(program, name);
            uniforms.put(name, location);
            uniformTypes.put(name, type.get(0));
            uniformSizes.put(name, params.get(0));
            uniformNames[i] = name;
        }
    }

    protected void fetchAttributes() {
        params.clear();
        Gdx.gl20.glGetProgramiv(program, GL20.GL_ACTIVE_ATTRIBUTES, params);
        int numAttributes = params.get(0);

        attributeNames = new String[numAttributes];

        for (int i = 0; i < numAttributes; i++) {
            params.clear();
            params.put(0, 1);
            type.clear();
            String name = Gdx.gl20.glGetActiveAttrib(program, i, params, type);
            int location = Gdx.gl20.glGetAttribLocation(program, name);
            attributes.put(name, location);
            attributeTypes.put(name, type.get(0));
            attributeSizes.put(name, params.get(0));
            attributeNames[i] = name;
        }
    }

    /**
     * @param name the name of the attribute
     * @return whether the attribute is available in the shader
     */
    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    /**
     * @param name the name of the attribute
     * @return the type of the attribute, one of {@link GL20#GL_FLOAT}, {@link GL20#GL_FLOAT_VEC2} etc.
     */
    public int getAttributeType(String name) {
        return attributeTypes.get(name, 0);
    }

    /**
     * @param name the name of the attribute
     * @return the location of the attribute or -1.
     */
    public int getAttributeLocation(String name) {
        return attributes.get(name, -1);
    }

    /**
     * @param name the name of the attribute
     * @return the size of the attribute or 0.
     */
    public int getAttributeSize(String name) {
        return attributeSizes.get(name, 0);
    }

    /**
     * @param name the name of the uniform
     * @return whether the uniform is available in the shader
     */
    public boolean hasUniform(String name) {
        return uniforms.containsKey(name);
    }

    /**
     * @param name the name of the uniform
     * @return the type of the uniform, one of {@link GL20#GL_FLOAT}, {@link GL20#GL_FLOAT_VEC2} etc.
     */
    public int getUniformType(String name) {
        return uniformTypes.get(name, 0);
    }

    /**
     * @param name the name of the uniform
     * @return the location of the uniform or -1.
     */
    public int getUniformLocation(String name) {
        return uniforms.get(name, -1);
    }

    /**
     * @param name the name of the uniform
     * @return the size of the uniform or 0.
     */
    public int getUniformSize(String name) {
        return uniformSizes.get(name, 0);
    }

    /**
     * @return the attributes
     */
    public String[] getAttributes() {
        return attributeNames;
    }

    /**
     * @return the uniforms
     */
    public String[] getUniforms() {
        return uniformNames;
    }

    /**
     * @return the source of the vertex shader
     */
    public String getVertexShaderSource() {
        return vertexShaderSource;
    }

    public String getVertexShaderFileName() {
        return vertexShaderFile;
    }

    /**
     * @return the source of the vertex shader
     */
    public String getGeometryShaderSource() {
        return geometryShaderSource;
    }

    public String getGeometryShaderFileName() {
        return geometryShaderFile;
    }

    /**
     * @return the source of the fragment shader
     */
    public String getFragmentShaderSource() {
        return fragmentShaderSource;
    }

    public String getFragmentShaderFileName() {
        return fragmentShaderFile;
    }

    /**
     * Disposes all resources associated with this shader. Must be called when the shader is no longer used.
     */
    public void dispose() {
        if (isCompiled && !isDisposed) {
            GL33.glUseProgram(0);
            if (vertexShaderHandle != 0) {
                GL33.glDeleteShader(vertexShaderHandle);
            }
            if (geometryShaderHandle != 0) {
                GL33.glDeleteShader(geometryShaderHandle);
            }
            if (fragmentShaderHandle != 0) {
                GL33.glDeleteShader(fragmentShaderHandle);
            }
            GL33.glDeleteProgram(program);
            if (shaders.get(Gdx.app) != null)
                shaders.get(Gdx.app).removeValue(this, true);

            isDisposed = true;
        }
    }
}
