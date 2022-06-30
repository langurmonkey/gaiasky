package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.render.RenderingContext;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.Consumers.Consumer6;
import gaiasky.util.Consumers.Consumer7;
import gaiasky.util.Functions.Function6;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Vector3b;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class Label implements Component {

    public float labelFactor;
    public float labelMax;
    /** Scale parameter for distance field fonts. **/
    public float textScale = -1;
    /** Power to apply to the view angle for labels. **/
    public float viewAnglePow = 1;

    /**
     * Position of label.
     */
    public Vector3b labelPosition;

    /**
     * Whether to draw 2D and 3D labels.
     */
    public boolean label, label2d;

    /** The label rendering code. **/
    public Consumer7<LabelEntityRenderSystem, LabelView , ExtSpriteBatch, ExtShaderProgram , FontRenderSystem, RenderingContext , ICamera> renderConsumer;

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