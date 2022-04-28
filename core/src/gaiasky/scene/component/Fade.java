package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.util.Constants;
import gaiasky.util.math.Vector2d;

public class Fade implements Component {
    /**
     * Fade in low and high limits
     */
    private Vector2d fadeIn;

    /**
     * Fade out low and high limits
     */
    private Vector2d fadeOut;

    /**
     * The current distance at each cycle, in internal units
     */
    protected double currentDistance;

    /**
     * If set, the fade distance is the distance between the current fade node and this object.
     * Otherwise, it is the length of the current object's position.
     */
    protected Entity position;

    /**
     * The name of the position object
     */
    private String positionObjectName;

    /**
     * Is the node already in the scene graph?
     */
    public boolean inSceneGraph = false;

    public void setFadein(double[] fadein) {
        if (fadein != null)
            fadeIn = new Vector2d(fadein[0] * Constants.PC_TO_U, fadein[1] * Constants.PC_TO_U);
        else
            fadeIn = null;
    }

    public void setFadeout(double[] fadeout) {
        if (fadeout != null)
            fadeOut = new Vector2d(fadeout[0] * Constants.PC_TO_U, fadeout[1] * Constants.PC_TO_U);
        else
            fadeOut = null;
    }

    public void setFade(double[] fade) {
        setFadepc(fade);
    }

    public void setFadepc(double[] fade) {
        if(fadeIn == null) {
            fadeIn = new Vector2d();
        }
        fadeIn.set(fade).scl(Constants.PC_TO_U);
    }

    public void setPositionobjectname(String po) {
        this.positionObjectName = po;
    }
}
