package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class SolidAngle implements Component {
    /**
     * radius/distance limit for rendering at all. If angle is smaller than this
     * quantity, no rendering happens.
     */
    public double thresholdNone;

    /**
     * radius/distance limit for rendering as shader. If angle is any bigger, we
     * render as a model.
     */
    public double thresholdQuad;

    /**
     * radius/distance limit for rendering as point. If angle is any bigger, we
     * render with shader.
     */
    public  double thresholdPoint;

    /**
     * The old TH_OVER_FACTOR value.
     */
    public double thresholdFactor;
}
