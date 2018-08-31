package gaia.cu9.ari.gaiaorbit.scenegraph.component;

import gaia.cu9.ari.gaiaorbit.util.Nature;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

/**
 * Provides the information for the rotation of this body.
 * 
 * @author Toni Sagrista
 *
 */
public class RotationComponent {
    /** Angular velocity [deg/hour] around the rotation axis. **/
    public double angularVelocity;
    /** Current angle with respect to the rotationAxis in degrees. **/
    public double angle;

    /** The rotation period in hours. **/
    public double period;
    /** Angle between equatorial plane and orbital plane in degrees. **/
    public double axialTilt;
    /** Angle between orbital plane and the ecliptic in degrees. **/
    public double inclination;
    /** The ascending node in degrees. **/
    public double ascendingNode;
    /** The meridian (hour) angle at the epoch J2000.0, in degrees **/
    public double meridianAngle;

    public RotationComponent() {
        this.angle = 0;
        this.angularVelocity = 0;
    }

    /**
     * Sets the rotation period.
     * 
     * @param rotationPeriod
     *            The period in hours.
     */
    public void setPeriod(Double rotationPeriod) {
        this.period = rotationPeriod;
        if (rotationPeriod != null) {
            angularVelocity = 360 / rotationPeriod;
        }
    }

    public void update(ITimeFrameProvider time) {
        double t = time.getTime().toEpochMilli() - AstroUtils.J2000_MS;
        angle = (meridianAngle + angularVelocity * t * Nature.MS_TO_H) % 360d;
    }

    /**
     * Sets the axial tilt, the angle between the equatorial plane and the
     * orbital plane.
     * 
     * @param f
     *            Angle in deg.
     */
    public void setAxialtilt(Double f) {
        this.axialTilt = f;
    }

    public void setAngle(Double angle) {
        this.angle = angle;
    }

    /**
     * Sets the inclination, the angle between the orbital plane and the
     * reference plane
     * 
     * @param i
     *            Inclination in deg.
     */
    public void setInclination(Double i) {
        inclination = i;
    }

    /**
     * Sets the inclination, the angle between the orbital plane and the
     * reference plane
     * 
     * @param i
     *            Inclination in deg.
     */
    public void setInclination(Long i) {
        inclination = i;
    }

    /**
     * Sets the ascending node.
     * 
     * @param an
     *            Angle in deg.
     */
    public void setAscendingnode(Double an) {
        this.ascendingNode = an;
    }

    /**
     * Sets the meridian angle.
     * 
     * @param ma
     *            Angle in deg.
     */
    public void setMeridianangle(Double ma) {
        this.meridianAngle = ma;
    }

    public RotationComponent clone() {
        RotationComponent clone = new RotationComponent();
        clone.period = this.period;
        clone.inclination = this.inclination;
        clone.angularVelocity = this.angularVelocity;
        clone.angle = this.angle;
        return clone;
    }
}
