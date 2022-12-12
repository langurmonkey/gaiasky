package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.render.RenderingContext;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.Consumers.Consumer7;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Vector3b;

import java.util.function.Consumer;
import java.util.function.Function;

public class Label implements Component {

    public float labelFactor;
    public float labelMax;
    /** Scale parameter for distance field fonts. **/
    public float textScale = -1;
    /** Power to apply to the view angle for labels. **/
    public float solidAnglePow = 1;

    /**
     * Position of label.
     */
    public Vector3b labelPosition;

    /**
     * Is it a label or another kind of text?
     */
    public boolean label, label2d;

    /** Function that checks whether the label must be rendered or not. **/
    public Function<LabelView, Boolean> renderFunction;

    /** The function to apply to set up the depth buffer for text rendering. **/
    public Consumer<LabelView> depthBufferConsumer = LabelView::defaultTextDepthBuffer;

    /** The label rendering code. **/
    public Consumer7<LabelEntityRenderSystem, LabelView, ExtSpriteBatch, ExtShaderProgram, FontRenderSystem, RenderingContext, ICamera> renderConsumer;

    /**
     * Sets the position of the label, in parsecs and in the internal reference
     * frame.
     *
     * @param labelPosition The position of the label in internal cartesian coordinates.
     */
    public void setLabelPosition(double[] labelPosition) {
        if (labelPosition != null) {
            this.labelPosition = new Vector3b(labelPosition[0] * Constants.PC_TO_U, labelPosition[1] * Constants.PC_TO_U, labelPosition[2] * Constants.PC_TO_U);
        }
    }

    public void setLabelposition(double[] labelPosition) {
        setLabelPosition(labelPosition);
    }

    public void setLabelFactor(Double labelFactor) {
        this.labelFactor = labelFactor.floatValue();
    }

    public void setLabelMax(Double labelMax) {
        this.labelMax = labelMax.floatValue();
    }

    public void setTextScale(Double textScale) {
        this.textScale = textScale.floatValue();
    }

}