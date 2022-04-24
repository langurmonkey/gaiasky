package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.coord.IBodyCoordinates;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;

public class Body implements Component {
    /**
     * Position of this entity in the local reference system. The units are
     * {@link gaiasky.util.Constants#U_TO_KM} by default.
     */
    public Vector3b pos;

    /**
     * Position in the equatorial system; ra, dec.
     */
    public Vector2d posSph;

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
    public float[] cc;
    public float[] labelcolor = Settings.settings.program.ui.isUINightMode() ? new float[] { 1, 0, 0, 1 } : new float[] { 1, 1, 1, 1 };


    public void setPosition(double[] pos) {
        this.pos = new Vector3b();
        this.pos.set(pos[0], pos[1], pos[2]);
    }

    public void setPositionKm(double[] pos) {
        this.pos = new Vector3b();
        this.pos.set(pos[0] * Constants.KM_TO_U, pos[1] * Constants.KM_TO_U, pos[2] * Constants.KM_TO_U);
    }

    public void setPositionPc(double[] pos) {
        this.pos = new Vector3b();
        this.pos.set(pos[0] * Constants.PC_TO_U, pos[1] * Constants.PC_TO_U, pos[2] * Constants.PC_TO_U);
    }

    public void setPosition(int[] pos) {
        setPosition(new double[] { pos[0], pos[1], pos[2] });
    }

    public void setSize(Double size) {
        this.size = size.floatValue();
    }

    public void setSizeKm(Double sizeKm) {
        this.size = (float) (size * Constants.KM_TO_U);
    }

    public void setSizePc(Double sizePc) {
        this.size = (float) (size * Constants.PC_TO_U);
    }

    public void setSizeM(Double sizeM) {
        this.size = (float) (size * Constants.M_TO_U);
    }

    public void setSizeAU(Double sizeAU) {
        this.size = (float) (size * Constants.AU_TO_U);
    }

    /**
     * Sets the object color, as an RGBA double array.
     *
     * @param color The color.
     */
    public void setColor(double[] color) {
        this.cc = GlobalResources.toFloatArray(color);
    }

    /**
     * Sets the object color, as an RGBA float array.
     *
     * @param color The color.
     */
    public void setColor(float[] color) {
        this.cc = color;
    }
    
    /**
     * Sets the label color, as an RGBA double array.
     *
     * @param color The label color.
     */
    public void setLabelcolor(double[] color) {
        this.labelcolor = GlobalResources.toFloatArray(color);
    }

    /**
     * Sets the label color, as an RGBA float array.
     *
     * @param color The label color.
     */
    public void setLabelcolor(float[] color) {
        this.labelcolor = color;
    }
}
