package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;

public class Body implements Component {
    /**
     * Position of this entity in the local reference system. The units are
     * {@link gaiasky.util.Constants#U_TO_KM} by default.
     */
    public Vector3b pos = new Vector3b();

    /**
     * Position in the equatorial system; ra, dec.
     */
    public Vector2d posSph = new Vector2d();

    /**
     * Size factor in internal units.
     */
    public float size;

    /**
     * The distance to the camera from the focus center.
     */
    public double distToCamera;

    /**
     * The view angle, in radians.
     */
    public double viewAngle;

    /**
     * The view angle corrected with the field of view angle, in radians.
     */
    public double viewAngleApparent;

    /**
     * Base RGB color
     */
    public float[] color;
    public float[] labelColor = Settings.settings.program.ui.isUINightMode() ? new float[] { 1, 0, 0, 1 } : new float[] { 1, 1, 1, 1 };


    public void setPos(double[] pos) {
        setPosition(pos);
    }

    public void setPosition(double[] pos) {
        this.pos.set(pos[0], pos[1], pos[2]);
    }

    public void setPosKm(double[] pos) {
        setPositionKm(pos);
    }

    public void setPositionKm(double[] pos) {
        this.pos.set(pos[0] * Constants.KM_TO_U, pos[1] * Constants.KM_TO_U, pos[2] * Constants.KM_TO_U);
    }

    public void setPosPc(double[] pos) {
        setPositionPc(pos);
    }

    public void setPositionPc(double[] pos) {
        this.pos.set(pos[0] * Constants.PC_TO_U, pos[1] * Constants.PC_TO_U, pos[2] * Constants.PC_TO_U);
    }

    public void setPosition(int[] pos) {
        setPosition(new double[] { pos[0], pos[1], pos[2] });
    }

    public void setSize(Double size) {
        this.size = size.floatValue();
    }

    public void setSizeKm(Double sizeKm) {
        this.size = (float) (sizeKm * Constants.KM_TO_U);
    }

    public void setSizepc(Double sizePc) {
        setSizePc(sizePc);
    }

    public void setSizePc(Double sizePc) {
        this.size = (float) (sizePc * Constants.PC_TO_U);
    }

    public void setSizeM(Double sizeM) {
        this.size = (float) (sizeM * Constants.M_TO_U);
    }

    public void setSizeAU(Double sizeAU) {
        this.size = (float) (sizeAU * Constants.AU_TO_U);
    }

    public void setRadius(Double radius) {
        setSize(radius * 2.0);
    }
    public void setRadiusKm(Double radiusKm) {
        setRadius(radiusKm * Constants.KM_TO_U);
    }
    public void setDiameter(Double diameter) {
        setSize(diameter);
    }
    public void setDiameterKm(Double diameterKm) {
        setDiameter(diameterKm * Constants.KM_TO_U);
    }

    /**
     * Sets the object color, as an RGBA double array.
     *
     * @param color The color.
     */
    public void setColor(double[] color) {
        this.color = GlobalResources.toFloatArray(color);
    }

    /**
     * Sets the object color, as an RGBA float array.
     *
     * @param color The color.
     */
    public void setColor(float[] color) {
        this.color = color;
    }
    
    /**
     * Sets the label color, as an RGBA double array.
     *
     * @param color The label color.
     */
    public void setLabelColor(double[] color) {
        this.labelColor = GlobalResources.toFloatArray(color);
    }

    @Deprecated
    public void setLabelcolor(double[] color) {
        setLabelColor(color);
    }

    /**
     * Sets the label color, as an RGBA float array.
     *
     * @param color The label color.
     */
    public void setLabelColor(float[] color) {
        this.labelColor = color;
    }

    @Deprecated
    public void setLabelcolor(float[] color) {
        setLabelColor(color);
    }
}
