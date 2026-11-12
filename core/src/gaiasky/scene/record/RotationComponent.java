/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import gaiasky.scene.api.IUpdatable;
import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.coord.OrbitFrames;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.Arrays;

public class RotationComponent implements IUpdatable<RotationComponent> {
    /**
     * Angular velocity [deg/hour] around the rotation axis.
     **/
    public double angularVelocity;
    /**
     * Current angle with respect to the rotationAxis in degrees.
     **/
    public double angle;
    /**
     * The rotation period in hours.
     **/
    public double period;
    /**
     * Angle between equatorial plane and orbital plane in degrees.
     **/
    public double axialTilt;
    /**
     * Angle between orbital plane and the ecliptic in degrees.
     **/
    public double inclination;
    /**
     * The ascending node in degrees, should not be used, as it obviously causes the body to wobble.
     **/
    public double ascendingNode;
    /**
     * The meridian (hour) angle at the epoch J2000.0, in degrees
     **/
    public double meridianAngle;
    /**
     * Pole of the rotation axis, as {right ascension, declination} in degrees in the equatorial frame
     * of J2000. When present it supersedes {@link #ascendingNode}, {@link #inclination} and
     * {@link #axialTilt}, and the body is oriented straight from the published pole. Note that the
     * pole carries the sense of the rotation: a retrograde body has a negative {@link #period} about
     * its north pole, rather than the opposite pole and a positive period.
     **/
    public double[] pole;

    public RotationComponent() {
        this.angle = 0;
        this.angularVelocity = 0;
    }

    /**
     * Sets the rotation period in hours. Overwrites the angular velocity.
     *
     * @param rotationPeriod The rotation period in hours.
     */
    public void setPeriod(Double rotationPeriod) {
        this.period = rotationPeriod;
        angularVelocity = 360.0 / rotationPeriod;
    }

    /**
     * Set rotation angular velocity in degrees/hour. Overwrites the rotation period.
     * @param av The angular velocity in deg/h.
     */
    public void setAngularVelocity(Double av) {
        angularVelocity = av;
        this.period = 360.0 / angularVelocity;
    }

    public void update(ITimeFrameProvider time) {
        var prevAngle = angle;
        long t = time.getTime().toEpochMilli() - AstroUtils.J2000_MS;
        angle = (meridianAngle + angularVelocity * t * Nature.MS_TO_H) % 360.0;
    }

    /**
     * Sets the axial tilt, the angle between the equatorial plane and the
     * orbital plane.
     *
     * @param f Angle in deg.
     */
    public void setAxialTilt(Double f) {
        this.axialTilt = f;
    }

    public void setAxialtilt(Double f) {
        setAxialTilt(f);
    }

    public void setAngle(Double angle) {
        this.angle = angle;
    }

    /**
     * Sets the inclination, the angle between the orbital plane and the
     * reference plane
     *
     * @param i Inclination in deg.
     */
    public void setInclination(Double i) {
        inclination = i;
    }

    /**
     * Sets the inclination, the angle between the orbital plane and the
     * reference plane
     *
     * @param i Inclination in deg.
     */
    public void setInclination(Long i) {
        inclination = i;
    }

    /**
     * Sets the ascending node.
     *
     * @param an Angle in deg.
     */
    public void setAscendingNode(Double an) {
        this.ascendingNode = an;
    }

    public void setAscendingnode(Double an) {
        setAscendingNode(an);
    }

    /**
     * Sets the meridian angle.
     *
     * @param ma Angle in deg.
     */
    public void setMeridianAngle(Double ma) {
        this.meridianAngle = ma;
    }

    public void setMeridianangle(Double ma) {
        setMeridianAngle(ma);
    }

    /**
     * Sets the pole of the rotation axis.
     *
     * @param pole Right ascension and declination of the pole, in degrees, in the equatorial frame of
     *             J2000.
     */
    public void setPole(double[] pole) {
        this.pole = pole != null && pole.length >= 2 ? new double[]{pole[0], pole[1]} : null;
    }

    /**
     * Whether the body declares its rotation pole explicitly, in which case the ascending node, the
     * inclination and the axial tilt are not used.
     */
    public boolean hasExplicitPole() {
        return pole != null && pole.length >= 2;
    }

    /**
     * Builds the orientation of the body, the matrix that takes its local axes into the equatorial
     * frame of the scene. The Y axis of the result is the rotation axis of the body.
     *
     * @param refPlaneTransform Name of the reference-plane transform of the body, only used when no
     *                          explicit pole is given.
     * @param out               The matrix to write the result into.
     *
     * @return The output matrix.
     */
    public Matrix4D getOrientation(String refPlaneTransform, Matrix4D out) {
        if (hasExplicitPole()) {
            // The pole is given in the equatorial frame already, so the reference plane of the body
            // plays no part: it only ever mattered to reach that frame from the orbital one.
            return OrbitFrames.fromPole(pole[0], pole[1], out);
        }
        return out.idt()
                .rotate(0, 1, 0, (float) ascendingNode)
                .mul(Coordinates.getTransformD(refPlaneTransform))
                .rotate(0, 0, 1, (float) (inclination + axialTilt));
    }

    @Override
    public String toString() {
        return "{" + "angVel=" + angularVelocity + ", angle=" + angle + ", period=" + period + ", axialTilt=" + axialTilt + ", inclination=" + inclination + ", ascendingNode=" + ascendingNode + ", meridianAngle=" + meridianAngle + ", pole=" + Arrays.toString(pole) + '}';
    }

    public void copyFrom(RotationComponent other) {
        if (other.pole != null) {
            this.pole = other.pole.clone();
        }
        // Unset fields are zero, so only zero is left alone. Anything else, including the negative
        // period of a retrograde body, is copied.
        if (other.period != 0) {
            this.period = other.period;
        }
        if (other.angularVelocity != 0) {
            this.angularVelocity = other.angularVelocity;
        }
        if (other.axialTilt != 0) {
            this.axialTilt = other.axialTilt;
        }
        if (other.inclination != 0) {
            this.inclination = other.inclination;
        }
        if (other.ascendingNode != 0) {
            this.ascendingNode = other.ascendingNode;
        }
        if (other.meridianAngle != 0) {
            this.meridianAngle = other.meridianAngle;
        }
    }

    @Override
    public void updateWith(RotationComponent object) {
        copyFrom(object);
    }

    public RotationComponent copy() {
        var clone = new RotationComponent();
        clone.angularVelocity = angularVelocity;
        clone.angle = angle;
        clone.axialTilt = axialTilt;
        clone.ascendingNode = ascendingNode;
        clone.inclination = inclination;
        clone.meridianAngle = meridianAngle;
        clone.period = period;
        clone.pole = pole != null ? pole.clone() : null;
        return clone;
    }
}
