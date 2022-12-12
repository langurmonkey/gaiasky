package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.util.Constants;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;

public class Fade implements Component {
    /**
     * Fade in low and high limits
     */
    public Vector2d fadeIn, fadeInMap;

    /**
     * Fade out low and high limits
     */
    public Vector2d fadeOut, fadeOutMap;

    /**
     * The current distance at each cycle, in internal units
     */
    public double currentDistance;

    /**
     * If set, the fading distance is the distance between the current object and this object.
     * This has precedence over the fade position.
     */
    public Entity fadePositionObject;

    /**
     * The name of the position object
     */
    public String fadePositionObjectName;

    /**
     * The position to use in order to compute the fading distance.
     */
    public Vector3b fadePosition;

    public void setFadein(double[] fadeIn) {
        setFadeIn(fadeIn);
    }

    public void setFadeIn(double[] fadeIn) {
        if (fadeIn != null)
            this.fadeIn = new Vector2d(fadeIn[0] * Constants.PC_TO_U, fadeIn[1] * Constants.PC_TO_U);
        else
            this.fadeIn = null;
    }

    /**
     * Sets the values to which the fade in distances are mapped. If not set, they are initialized
     * automatically with [0,1].
     *
     * @param fadeInMap The values.
     */
    public void setFadeInMap(double[] fadeInMap) {
        if (fadeInMap != null)
            this.fadeInMap = new Vector2d(fadeInMap[0], fadeInMap[1]);
        else
            this.fadeInMap = null;
    }

    public void setFadeout(double[] fadeOut) {
        setFadeOut(fadeOut);
    }

    public void setFadeOut(double[] fadeOut) {
        if (fadeOut != null)
            this.fadeOut = new Vector2d(fadeOut[0] * Constants.PC_TO_U, fadeOut[1] * Constants.PC_TO_U);
        else
            this.fadeOut = null;
    }

    /**
     * Sets the values to which the fade out distances are mapped. If not set, they are initialized
     * automatically with [1,0].
     *
     * @param fadeOutMap The values.
     */
    public void setFadeOutMap(double[] fadeOutMap) {
        if (fadeOutMap != null)
            this.fadeOutMap = new Vector2d(fadeOutMap[0], fadeOutMap[1]);
        else
            this.fadeOutMap = null;
    }

    public void setFade(double[] fade) {
        setFadepc(fade);
    }

    public void setFadepc(double[] fade) {
        setFadePc(fade);
    }

    public void setFadePc(double[] fade) {
        if (fadeIn == null) {
            fadeIn = new Vector2d();
        }
        fadeIn.set(fade).scl(Constants.PC_TO_U);
    }

    public void setPositionobjectname(String name) {
        setFadeObjectName(name);
    }

    public void setFadeObjectName(String name) {
        this.fadePositionObjectName = name;
    }

    public void setFadePosition(Double fadePosition) {
        this.fadePosition = new Vector3b(fadePosition, fadePosition, fadePosition);
    }

    public void setFadePosition(double[] fadePosition) {
        this.fadePosition = new Vector3b(fadePosition[0], fadePosition[1], fadePosition[2]);
    }
}
