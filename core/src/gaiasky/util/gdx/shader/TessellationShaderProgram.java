/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL41;

import java.nio.*;

public class TessellationShaderProgram extends ExtShaderProgram {
    /** default name for position attributes **/
    public static final String POSITION_ATTRIBUTE = "a_position";
    /** default name for color attributes **/
    public static final String COLOR_ATTRIBUTE = "a_color";
    /** default name for texture coordinates attributes, append texture unit number **/
    public static final String TEXCOORD_ATTRIBUTE = "a_texCoord";
    /** the list of currently available shaders **/
    private final static ObjectMap<Application, Array<TessellationShaderProgram>> shaders = new ObjectMap<>();
    /** flag indicating whether attributes & uniforms must be present at all times **/
    public static boolean pedantic = true;
    /**
     * code that is always added to the vertex shader code, typically used to inject a #version line. Note that this is added
     * as-is, you should include a newline (`\n`) if needed.
     */
    public static String prependVertexCode = "";
    public static String prependTessControlCode = "";
    public static String prependTessEvaluationCode = "";
    /**
     * code that is always added to every fragment shader code, typically used to inject a #version line. Note that this is added
     * as-is, you should include a newline (`\n`) if needed.
     */
    public static String prependFragmentCode = "";
    /** uniform lookup **/
    private final ObjectIntMap<String> uniforms = new ObjectIntMap<>();
    /** uniform types **/
    private final ObjectIntMap<String> uniformTypes = new ObjectIntMap<>();
    /** uniform sizes **/
    private final ObjectIntMap<String> uniformSizes = new ObjectIntMap<>();
    /** attribute lookup **/
    private final ObjectIntMap<String> attributes = new ObjectIntMap<>();
    /** attribute types **/
    private final ObjectIntMap<String> attributeTypes = new ObjectIntMap<>();
    /** attribute sizes **/
    private final ObjectIntMap<String> attributeSizes = new ObjectIntMap<>();
    /** vertex shader source **/
    private final String vertexShaderSource;
    /** tess control source **/
    private final String tessellationControlShaderSource;
    /** tess evaluation source **/
    private final String tessellationEvaluationShaderSource;
    /** fragment shader source **/
    private final String fragmentShaderSource;
    /** reference count **/
    private final int refCount = 0;
    IntBuffer params = BufferUtils.newIntBuffer(1);
    IntBuffer type = BufferUtils.newIntBuffer(1);
    /** the log **/
    private String log = "";
    /** whether this program compiled successfully **/
    private boolean isCompiled;
    /** uniform names **/
    private String[] uniformNames;
    /** attribute names **/
    private String[] attributeNames;
    /** program handle **/
    private int program;
    private int vertexShaderHandle, fragmentShaderHandle;
    private int tesselationControlShaderHandle, tessellationEvaluationShaderHandle;
    /** whether this shader was invalidated **/
    private boolean invalidated;

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

    /** @return the number of managed shader programs currently loaded */
    public static int getNumManagedShaderPrograms() {
        return shaders.get(Gdx.app).size;
    }

    /**
     * Loads and compiles the shaders, creates a new program and links the shaders.
     */
    private void compileShaders(String vShader, String tcShader, String teShader, String fShader) {
        vertexShaderHandle = loadShader(GL30.GL_VERTEX_SHADER, vShader);
        tesselationControlShaderHandle = loadShader(GL41.GL_TESS_CONTROL_SHADER, tcShader);
        tessellationEvaluationShaderHandle = loadShader(GL41.GL_TESS_EVALUATION_SHADER, teShader);
        fragmentShaderHandle = loadShader(GL30.GL_FRAGMENT_SHADER, fShader);

        if (vertexShaderHandle == -1 || tesselationControlShaderHandle == -1 || tessellationEvaluationShaderHandle == -1 || fragmentShaderHandle == -1) {
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
        IntBuffer intbuf = BufferUtils.newIntBuffer(1);

        int shader = GL41.glCreateShader(type);
        if (shader == 0)
            return -1;

        GL41.glShaderSource(shader, source);
        GL41.glCompileShader(shader);
        GL41.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, intbuf);

        int compiled = intbuf.get(0);
        if (compiled == 0) {
            String infoLog = GL41.glGetShaderInfoLog(shader);
            switch (type) {
            case GL41.GL_VERTEX_SHADER:
                log += "Vertex shader\n";
                break;
            case GL41.GL_FRAGMENT_SHADER:
                log += "Fragment shader\n";
                break;
            case GL41.GL_TESS_CONTROL_SHADER:
                log += "Tessellation control shader\n";
                break;
            case GL41.GL_TESS_EVALUATION_SHADER:
                log += "Tessellation evaluation shader\n";
                break;
            }
            log += infoLog;
            // }
            return -1;
        }

        return shader;
    }

    protected int createProgram() {
        int program = GL41.glCreateProgram();
        return program != 0 ? program : -1;
    }

    private int linkProgram(int program) {
        if (program == -1)
            return -1;

        GL41.glAttachShader(program, vertexShaderHandle);
        GL41.glAttachShader(program, tesselationControlShaderHandle);
        GL41.glAttachShader(program, tessellationEvaluationShaderHandle);
        GL41.glAttachShader(program, fragmentShaderHandle);
        GL41.glLinkProgram(program);

        ByteBuffer tmp = ByteBuffer.allocateDirect(4);
        tmp.order(ByteOrder.nativeOrder());
        IntBuffer intbuf = tmp.asIntBuffer();

        GL41.glGetProgramiv(program, GL20.GL_LINK_STATUS, intbuf);
        int linked = intbuf.get(0);
        if (linked == 0) {
            log = GL41.glGetProgramInfoLog(program);
            // }
            return -1;
        }

        return program;
    }

    /**
     * @return the log info for the shader compilation and program linking stage. The shader needs to be bound for this method to
     * have an effect.
     */
    public String getLog() {
        if (isCompiled) {
            log = Gdx.gl.glGetProgramInfoLog(program);
        }
        return log;
    }

    /** @return whether this ShaderProgram compiled successfully. */
    public boolean isCompiled() {
        return isCompiled;
    }

    private int fetchAttributeLocation(String name) {
        GL20 gl = Gdx.gl;
        // -2 == not yet cached
        // -1 == cached but not found
        int location;
        if ((location = attributes.get(name, -2)) == -2) {
            location = gl.glGetAttribLocation(program, name);
            attributes.put(name, location);
        }
        return location;
    }

    private int fetchUniformLocation(String name) {
        return fetchUniformLocation(name, pedantic);
    }

    public int fetchUniformLocation(String name, boolean pedantic) {
        GL20 gl = Gdx.gl;
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
     * Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name  the name of the uniform
     * @param value the value
     */
    public void setUniformi(String name, int value) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform1i(location, value);
    }

    public void setUniformi(int location, int value) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniform1i(location, value);
    }

    /**
     * Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     */
    public void setUniformi(String name, int value1, int value2) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform2i(location, value1, value2);
    }

    public void setUniformi(int location, int value1, int value2) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniform2i(location, value1, value2);
    }

    /**
     * Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     */
    public void setUniformi(String name, int value1, int value2, int value3) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform3i(location, value1, value2, value3);
    }

    public void setUniformi(int location, int value1, int value2, int value3) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniform3i(location, value1, value2, value3);
    }

    /**
     * Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     * @param value4 the fourth value
     */
    public void setUniformi(String name, int value1, int value2, int value3, int value4) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform4i(location, value1, value2, value3, value4);
    }

    public void setUniformi(int location, int value1, int value2, int value3, int value4) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniform4i(location, value1, value2, value3, value4);
    }

    /**
     * Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name  the name of the uniform
     * @param value the value
     */
    public void setUniformf(String name, float value) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform1f(location, value);
    }

    public void setUniformf(int location, float value) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniform1f(location, value);
    }

    /**
     * Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     */
    public void setUniformf(String name, float value1, float value2) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform2f(location, value1, value2);
    }

    public void setUniformf(int location, float value1, float value2) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniform2f(location, value1, value2);
    }

    /**
     * Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     */
    public void setUniformf(String name, float value1, float value2, float value3) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform3f(location, value1, value2, value3);
    }

    public void setUniformf(int location, float value1, float value2, float value3) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniform3f(location, value1, value2, value3);
    }

    /**
     * Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     * @param value4 the fourth value
     */
    public void setUniformf(String name, float value1, float value2, float value3, float value4) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform4f(location, value1, value2, value3, value4);
    }

    public void setUniformf(int location, float value1, float value2, float value3, float value4) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniform4f(location, value1, value2, value3, value4);
    }

    public void setUniform1fv(String name, float[] values, int offset, int length) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform1fv(location, length, values, offset);
    }

    public void setUniform1fv(int location, float[] values, int offset, int length) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniform1fv(location, length, values, offset);
    }

    public void setUniform2fv(String name, float[] values, int offset, int length) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform2fv(location, length / 2, values, offset);
    }

    public void setUniform2fv(int location, float[] values, int offset, int length) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniform2fv(location, length / 2, values, offset);
    }

    public void setUniform3fv(String name, float[] values, int offset, int length) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform3fv(location, length / 3, values, offset);
    }

    public void setUniform3fv(int location, float[] values, int offset, int length) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniform3fv(location, length / 3, values, offset);
    }

    public void setUniform4fv(String name, float[] values, int offset, int length) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchUniformLocation(name);
        gl.glUniform4fv(location, length / 4, values, offset);
    }

    public void setUniform4fv(int location, float[] values, int offset, int length) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniform4fv(location, length / 4, values, offset);
    }

    /**
     * Sets the uniform matrix with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param matrix the matrix
     */
    public void setUniformMatrix(String name, Matrix4 matrix) {
        setUniformMatrix(name, matrix, false);
    }

    /**
     * Sets the uniform matrix with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name      the name of the uniform
     * @param matrix    the matrix
     * @param transpose whether the matrix should be transposed
     */
    public void setUniformMatrix(String name, Matrix4 matrix, boolean transpose) {
        setUniformMatrix(fetchUniformLocation(name), matrix, transpose);
    }

    public void setUniformMatrix(int location, Matrix4 matrix) {
        setUniformMatrix(location, matrix, false);
    }

    public void setUniformMatrix(int location, Matrix4 matrix, boolean transpose) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniformMatrix4fv(location, 1, transpose, matrix.val, 0);
    }

    /**
     * Sets the uniform matrix with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param matrix the matrix
     */
    public void setUniformMatrix(String name, Matrix3 matrix) {
        setUniformMatrix(name, matrix, false);
    }

    /**
     * Sets the uniform matrix with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name      the name of the uniform
     * @param matrix    the matrix
     * @param transpose whether the uniform matrix should be transposed
     */
    public void setUniformMatrix(String name, Matrix3 matrix, boolean transpose) {
        setUniformMatrix(fetchUniformLocation(name), matrix, transpose);
    }

    public void setUniformMatrix(int location, Matrix3 matrix) {
        setUniformMatrix(location, matrix, false);
    }

    public void setUniformMatrix(int location, Matrix3 matrix, boolean transpose) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniformMatrix3fv(location, 1, transpose, matrix.val, 0);
    }

    /**
     * Sets an array of uniform matrices with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name      the name of the uniform
     * @param buffer    buffer containing the matrix data
     * @param transpose whether the uniform matrix should be transposed
     */
    public void setUniformMatrix3fv(String name, FloatBuffer buffer, int count, boolean transpose) {
        GL20 gl = Gdx.gl;
        checkManaged();
        buffer.position(0);
        int location = fetchUniformLocation(name);
        gl.glUniformMatrix3fv(location, count, transpose, buffer);
    }

    /**
     * Sets an array of uniform matrices with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name      the name of the uniform
     * @param buffer    buffer containing the matrix data
     * @param transpose whether the uniform matrix should be transposed
     */
    public void setUniformMatrix4fv(String name, FloatBuffer buffer, int count, boolean transpose) {
        GL20 gl = Gdx.gl;
        checkManaged();
        buffer.position(0);
        int location = fetchUniformLocation(name);
        gl.glUniformMatrix4fv(location, count, transpose, buffer);
    }

    public void setUniformMatrix4fv(int location, float[] values, int offset, int length) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUniformMatrix4fv(location, length / 16, false, values, offset);
    }

    public void setUniformMatrix4fv(String name, float[] values, int offset, int length) {
        setUniformMatrix4fv(fetchUniformLocation(name), values, offset, length);
    }

    /**
     * Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param values x and y as the first and second values respectively
     */
    public void setUniformf(String name, Vector2 values) {
        setUniformf(name, values.x, values.y);
    }

    public void setUniformf(int location, Vector2 values) {
        setUniformf(location, values.x, values.y);
    }

    /**
     * Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param values x, y and z as the first, second and third values respectively
     */
    public void setUniformf(String name, Vector3 values) {
        setUniformf(name, values.x, values.y, values.z);
    }

    public void setUniformf(int location, Vector3 values) {
        setUniformf(location, values.x, values.y, values.z);
    }

    /**
     * Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name   the name of the uniform
     * @param values r, g, b and a as the first through fourth values respectively
     */
    public void setUniformf(String name, Color values) {
        setUniformf(name, values.r, values.g, values.b, values.a);
    }

    public void setUniformf(int location, Color values) {
        setUniformf(location, values.r, values.g, values.b, values.a);
    }

    /**
     * Sets the vertex attribute with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name      the attribute name
     * @param size      the number of components, must be >= 1 and <= 4
     * @param type      the type, must be one of GL20.GL_BYTE, GL20.GL_UNSIGNED_BYTE, GL20.GL_SHORT,
     *                  GL20.GL_UNSIGNED_SHORT,GL20.GL_FIXED, or GL20.GL_FLOAT. GL_FIXED will not work on the desktop
     * @param normalize whether fixed point data should be normalized. Will not work on the desktop
     * @param stride    the stride in bytes between successive attributes
     * @param buffer    the buffer containing the vertex attributes.
     */
    public void setVertexAttribute(String name, int size, int type, boolean normalize, int stride, Buffer buffer) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchAttributeLocation(name);
        if (location == -1)
            return;
        gl.glVertexAttribPointer(location, size, type, normalize, stride, buffer);
    }

    public void setVertexAttribute(int location, int size, int type, boolean normalize, int stride, Buffer buffer) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glVertexAttribPointer(location, size, type, normalize, stride, buffer);
    }

    /**
     * Sets the vertex attribute with the given name. The {@link ShaderProgram} must be bound for this to work.
     *
     * @param name      the attribute name
     * @param size      the number of components, must be >= 1 and <= 4
     * @param type      the type, must be one of GL20.GL_BYTE, GL20.GL_UNSIGNED_BYTE, GL20.GL_SHORT,
     *                  GL20.GL_UNSIGNED_SHORT,GL20.GL_FIXED, or GL20.GL_FLOAT. GL_FIXED will not work on the desktop
     * @param normalize whether fixed point data should be normalized. Will not work on the desktop
     * @param stride    the stride in bytes between successive attributes
     * @param offset    byte offset into the vertex buffer object bound to GL20.GL_ARRAY_BUFFER.
     */
    public void setVertexAttribute(String name, int size, int type, boolean normalize, int stride, int offset) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchAttributeLocation(name);
        if (location == -1)
            return;
        gl.glVertexAttribPointer(location, size, type, normalize, stride, offset);
    }

    public void setVertexAttribute(int location, int size, int type, boolean normalize, int stride, int offset) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glVertexAttribPointer(location, size, type, normalize, stride, offset);
    }

    /**
     * Makes OpenGL ES 2.0 use this vertex and fragment shader pair. When you are done with this shader you have to call
     * {@link ShaderProgram#end()}.
     */
    public void begin() {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glUseProgram(program);
    }

    /**
     * Disables this shader. Must be called when one is done with the shader. Don't mix it with dispose, that will release the
     * shader resources.
     */
    public void end() {
        GL20 gl = Gdx.gl;
        gl.glUseProgram(0);
    }

    /**
     * Disables the vertex attribute with the given name
     *
     * @param name the vertex attribute name
     */
    public void disableVertexAttribute(String name) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchAttributeLocation(name);
        if (location == -1)
            return;
        gl.glDisableVertexAttribArray(location);
    }

    public void disableVertexAttribute(int location) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glDisableVertexAttribArray(location);
    }

    /**
     * Enables the vertex attribute with the given name
     *
     * @param name the vertex attribute name
     */
    public void enableVertexAttribute(String name) {
        GL20 gl = Gdx.gl;
        checkManaged();
        int location = fetchAttributeLocation(name);
        if (location == -1)
            return;
        gl.glEnableVertexAttribArray(location);
    }

    public void enableVertexAttribute(int location) {
        GL20 gl = Gdx.gl;
        checkManaged();
        gl.glEnableVertexAttribArray(location);
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

    /**
     * Sets the given attribute
     *
     * @param name   the name of the attribute
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     * @param value4 the fourth value
     */
    public void setAttributef(String name, float value1, float value2, float value3, float value4) {
        GL20 gl = Gdx.gl;
        int location = fetchAttributeLocation(name);
        gl.glVertexAttrib4f(location, value1, value2, value3, value4);
    }

    private void fetchUniforms() {
        params.clear();
        Gdx.gl.glGetProgramiv(program, GL20.GL_ACTIVE_UNIFORMS, params);
        int numUniforms = params.get(0);

        uniformNames = new String[numUniforms];

        for (int i = 0; i < numUniforms; i++) {
            params.clear();
            params.put(0, 1);
            type.clear();
            String name = Gdx.gl.glGetActiveUniform(program, i, params, type);
            int location = Gdx.gl.glGetUniformLocation(program, name);
            uniforms.put(name, location);
            uniformTypes.put(name, type.get(0));
            uniformSizes.put(name, params.get(0));
            uniformNames[i] = name;
        }
    }

    private void fetchAttributes() {
        params.clear();
        Gdx.gl.glGetProgramiv(program, GL20.GL_ACTIVE_ATTRIBUTES, params);
        int numAttributes = params.get(0);

        attributeNames = new String[numAttributes];

        for (int i = 0; i < numAttributes; i++) {
            params.clear();
            params.put(0, 1);
            type.clear();
            String name = Gdx.gl.glGetActiveAttrib(program, i, params, type);
            int location = Gdx.gl.glGetAttribLocation(program, name);
            attributes.put(name, location);
            attributeTypes.put(name, type.get(0));
            attributeSizes.put(name, params.get(0));
            attributeNames[i] = name;
        }
    }

    /**
     * @param name the name of the attribute
     *
     * @return whether the attribute is available in the shader
     */
    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    /**
     * @param name the name of the attribute
     *
     * @return the type of the attribute, one of {@link GL20#GL_FLOAT}, {@link GL20#GL_FLOAT_VEC2} etc.
     */
    public int getAttributeType(String name) {
        return attributeTypes.get(name, 0);
    }

    /**
     * @param name the name of the attribute
     *
     * @return the location of the attribute or -1.
     */
    public int getAttributeLocation(String name) {
        return attributes.get(name, -1);
    }

    /**
     * @param name the name of the attribute
     *
     * @return the size of the attribute or 0.
     */
    public int getAttributeSize(String name) {
        return attributeSizes.get(name, 0);
    }

    /**
     * @param name the name of the uniform
     *
     * @return whether the uniform is available in the shader
     */
    public boolean hasUniform(String name) {
        return uniforms.containsKey(name);
    }

    /**
     * @param name the name of the uniform
     *
     * @return the type of the uniform, one of {@link GL20#GL_FLOAT}, {@link GL20#GL_FLOAT_VEC2} etc.
     */
    public int getUniformType(String name) {
        return uniformTypes.get(name, 0);
    }

    /**
     * @param name the name of the uniform
     *
     * @return the location of the uniform or -1.
     */
    public int getUniformLocation(String name) {
        return uniforms.get(name, -1);
    }

    /**
     * @param name the name of the uniform
     *
     * @return the size of the uniform or 0.
     */
    public int getUniformSize(String name) {
        return uniformSizes.get(name, 0);
    }

    /** @return the attributes */
    public String[] getAttributes() {
        return attributeNames;
    }

    /** @return the uniforms */
    public String[] getUniforms() {
        return uniformNames;
    }

    /** @return the source of the vertex shader */
    public String getVertexShaderSource() {
        return vertexShaderSource;
    }

    /** @return the source of the fragment shader */
    public String getFragmentShaderSource() {
        return fragmentShaderSource;
    }

    @Override
    public void dispose() {
        GL41.glUseProgram(0);
        GL41.glDeleteShader(vertexShaderHandle);
        GL41.glDeleteShader(tesselationControlShaderHandle);
        GL41.glDeleteShader(tessellationEvaluationShaderHandle);
        GL41.glDeleteShader(fragmentShaderHandle);
        GL41.glDeleteProgram(program);
        if (shaders.get(Gdx.app) != null)
            shaders.get(Gdx.app).removeValue(this, true);

    }
}
