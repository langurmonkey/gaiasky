package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.gdx.contrib.postprocess.utils.FullscreenQuad;

/** The base class for any single-pass filter. */
public abstract class Filter<T> {

    protected static final FullscreenQuad quad = new FullscreenQuad();
    protected static final int u_texture0 = 0;
    protected static final int u_texture1 = 1;
    protected static final int u_texture2 = 2;
    protected static final int u_texture3 = 3;
    protected Texture inputTexture = null;
    protected FrameBuffer inputBuffer = null;
    protected FrameBuffer outputBuffer = null;
    protected ShaderProgram program;
    private boolean programBegan = false;
    public Filter(ShaderProgram program) {
        this.program = program;
    }

    public T setInput(Texture input) {
        this.inputTexture = input;
        return (T) this; // assumes T extends Filter
    }

    public T setInput(FrameBuffer input) {
        this.inputBuffer = input;
        if (input != null)
            return setInput(input.getColorBufferTexture());
        else
            return (T) this;
    }

    public T setOutput(FrameBuffer output) {
        this.outputBuffer = output;
        return (T) this;
    }

    /**
     * Caution, disposes of the current program and updates it with the new one. Run synchronously after render().
     *
     * @param program The new shader program.
     */
    public void updateProgram(ShaderProgram program) {
        if (this.program != null) {
            this.program.dispose();
        }
        this.program = program;
        rebind();
    }

    public void dispose() {
        program.dispose();
    }

    public abstract void rebind();

    // int
    protected void setParam(Parameter param, int value) {
        program.bind();
        program.setUniformi(param.mnemonic(), value);
    }

    /*
     * Sets the parameter to the specified value for this filter. This is for
     * one-off operations since the shader is being bound and unbound once per
     * call: for a batch-ready version of this function see and use setParams
     * instead.
     */

    // float
    protected void setParam(Parameter param, float value) {
        program.bind();
        program.setUniformf(param.mnemonic(), value);
    }

    // vec2
    protected void setParam(Parameter param, Vector2 value) {
        program.bind();
        program.setUniformf(param.mnemonic(), value);
    }

    // vec3
    protected void setParam(Parameter param, Vector3 value) {
        program.bind();
        program.setUniformf(param.mnemonic(), value);
    }

    // mat3
    protected T setParam(Parameter param, Matrix3 value) {
        program.bind();
        program.setUniformMatrix(param.mnemonic(), value);
        return (T) this;
    }

    // mat4
    protected T setParam(Parameter param, Matrix4 value) {
        program.bind();
        program.setUniformMatrix(param.mnemonic(), value);
        return (T) this;
    }

    // float[], vec2[], vec3[], vec4[]
    protected T setParamv(Parameter param, float[] values, int offset, int length) {
        program.bind();

        switch (param.arrayElementSize()) {
        case 4 -> program.setUniform4fv(param.mnemonic(), values, offset, length);
        case 3 -> program.setUniform3fv(param.mnemonic(), values, offset, length);
        case 2 -> program.setUniform2fv(param.mnemonic(), values, offset, length);
        case 1 -> program.setUniform1fv(param.mnemonic(), values, offset, length);
        }

        return (T) this;
    }

    /**
     * Sets the parameter to the specified value for this filter. When you are
     * finished building the batch you shall signal it by invoking endParams().
     */

    // float
    protected T setParams(Parameter param, float value) {
        if (!programBegan) {
            programBegan = true;
            program.bind();
        }
        program.setUniformf(param.mnemonic(), value);
        return (T) this;
    }

    // int version
    protected T setParams(Parameter param, int value) {
        if (!programBegan) {
            programBegan = true;
            program.bind();
        }
        program.setUniformi(param.mnemonic(), value);
        return (T) this;
    }

    // vec2 version
    protected T setParams(Parameter param, Vector2 value) {
        if (!programBegan) {
            programBegan = true;
            program.bind();
        }
        program.setUniformf(param.mnemonic(), value);
        return (T) this;
    }

    // vec3 version
    protected T setParams(Parameter param, Vector3 value) {
        if (!programBegan) {
            programBegan = true;
            program.bind();
        }
        program.setUniformf(param.mnemonic(), value);
        return (T) this;
    }

    // mat3
    protected T setParams(Parameter param, Matrix3 value) {
        if (!programBegan) {
            programBegan = true;
            program.bind();
        }
        program.setUniformMatrix(param.mnemonic(), value);
        return (T) this;
    }

    // mat4
    protected T setParams(Parameter param, Matrix4 value) {
        if (!programBegan) {
            programBegan = true;
            program.bind();
        }
        program.setUniformMatrix(param.mnemonic(), value);
        return (T) this;
    }

    // float[], vec2[], vec3[], vec4[]
    protected T setParamsv(Parameter param, float[] values, int offset, int length) {
        if (!programBegan) {
            programBegan = true;
            program.bind();
        }

        switch (param.arrayElementSize()) {
        case 4 -> program.setUniform4fv(param.mnemonic(), values, offset, length);
        case 3 -> program.setUniform3fv(param.mnemonic(), values, offset, length);
        case 2 -> program.setUniform2fv(param.mnemonic(), values, offset, length);
        case 1 -> program.setUniform1fv(param.mnemonic(), values, offset, length);
        }

        return (T) this;
    }

    /** Should be called after any one or more setParams method calls. */
    protected void endParams() {
        if (programBegan) {
            programBegan = false;
        }
    }

    /** This method will get called just before a rendering operation occurs. */
    protected abstract void onBeforeRender();

    public final void render() {
        if (outputBuffer != null) {
            outputBuffer.begin();
            realRender();
            outputBuffer.end();
        } else {
            realRender();
        }
    }

    protected void realRender() {
        // gives a chance to filters to perform needed operations just before the rendering operation take place.
        onBeforeRender();

        program.bind();
        quad.render(program);
    }

    public interface Parameter {
        String mnemonic();

        int arrayElementSize();
    }
}
