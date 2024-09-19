/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;

public class Body implements Component, ICopy {
    /**
     * Position of this entity in the local reference system and internal units.
     * This holds the position of the entity in the reference system of its parent.
     * It is not the absolute position. If the entity has a {@link ProperMotion} component,
     * the proper motion is applied for the current time. The position at epoch is
     * kept in {@link Body#posEpoch}.
     * The absolute position is held at {@link GraphNode#translation}.
     */
    public Vector3b pos = new Vector3b();
    /**
     * A copy of the original position, if any.
     * If the entity has a {@link ProperMotion} component, this is the
     * position at epoch {@link ProperMotion#epochJd}.
     */
    public Vector3b posEpoch = new Vector3b();

    /**
     * Position in the equatorial system; ra, dec.
     */
    public Vector2d posSph = new Vector2d();

    /**
     * Body size in internal units.
     */
    public float size;
    /**
     * This flag indicates whether the size has already been set using the correct units.
     * This is for backward compatibility with the data files.
     */
    public boolean sizeInUnitsFlag = false;
    /**
     * This flag enables backwards-compatibility. When true, the body size is assumed to be a radius,
     * and will be doubled in the initialization. Setting the size using any of the
     * setRadius() methods sets this to false.
     */
    public boolean sizeIsRadiusFlag = true;

    /**
     * The distance to the camera from the focus center.
     */
    public double distToCamera;

    /**
     * The view angle, in radians.
     */
    public double solidAngle;

    /**
     * The view angle corrected with the field of view angle, in radians.
     */
    public double solidAngleApparent;

    /**
     * Base RGB color
     */
    public float[] color;
    public float[] labelColor = Settings.settings != null && Settings.settings.program.ui.isUINightMode() ? new float[] { 1, 0, 0, 1 } : new float[] { 1, 1, 1, 1 };

    /**
     * Flag to control whether the position of this object has been set in a script.
     */
    public boolean positionSetInScript = false;

    public void setPos(Vector3b pos) {
        this.pos.set(pos);
        updatePosEpoch();
    }

    public void setPos(double[] pos) {
        setPosition(pos);
    }

    public void setPosition(double[] pos) {
        this.pos.set(pos[0] * Constants.DISTANCE_SCALE_FACTOR, pos[1] * Constants.DISTANCE_SCALE_FACTOR, pos[2] * Constants.DISTANCE_SCALE_FACTOR);
        updatePosEpoch();
    }

    public void setPosKm(double[] pos) {
        setPositionKm(pos);
    }

    public void setPositionKm(double[] pos) {
        this.pos.set(pos[0] * Constants.KM_TO_U, pos[1] * Constants.KM_TO_U, pos[2] * Constants.KM_TO_U);
        updatePosEpoch();
    }

    public void setPosPc(double[] pos) {
        setPositionPc(pos);
    }

    public void setPositionPc(double[] pos) {
        this.pos.set(pos[0] * Constants.PC_TO_U, pos[1] * Constants.PC_TO_U, pos[2] * Constants.PC_TO_U);
        updatePosEpoch();
    }

    public void setPosition(int[] pos) {
        setPosition(new double[] { pos[0] * Constants.DISTANCE_SCALE_FACTOR, pos[1] * Constants.DISTANCE_SCALE_FACTOR, pos[2] * Constants.DISTANCE_SCALE_FACTOR });
    }

    /**
     * Sets the position at epoch as a copy of the position. This is usually
     * done at initialization only.
     */
    public void updatePosEpoch() {
        this.posEpoch.set(this.pos);
    }

    public void setSize(Double size) {
        this.size = size.floatValue();
    }

    public void setSizeKm(Double sizeKm) {
        this.size = (float) (sizeKm * Constants.KM_TO_U);
        this.sizeInUnitsFlag = true;
    }

    public void setSizepc(Double sizePc) {
        setSizePc(sizePc);
    }

    public void setSizePc(Double sizePc) {
        this.size = (float) (sizePc * Constants.PC_TO_U);
        this.sizeInUnitsFlag = true;
    }

    public void setSizeM(Double sizeM) {
        this.size = (float) (sizeM * Constants.M_TO_U);
        this.sizeInUnitsFlag = true;
    }

    public void setSizeAU(Double sizeAU) {
        this.size = (float) (sizeAU * Constants.AU_TO_U);
        this.sizeInUnitsFlag = true;
    }

    public void setRadius(Double radius) {
        setSize(radius * 2.0);
        this.sizeIsRadiusFlag = false;
    }

    public void setRadiusKm(Double radiusKm) {
        setSizeKm(radiusKm * 2.0);
        this.sizeIsRadiusFlag = false;
    }

    public void setRadiusPc(Double radiusPc) {
        setSizePc(radiusPc * 2.0);
        this.sizeIsRadiusFlag = false;
    }

    public void setDiameter(Double diameter) {
        setSize(diameter);
        this.sizeIsRadiusFlag = false;
    }

    public void setDiameterKm(Double diameterKm) {
        setDiameter(diameterKm * Constants.KM_TO_U);
    }

    public void setDiameterPc(Double diameterPc) {
        setDiameter(diameterPc * Constants.PC_TO_U);
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

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.solidAngle = solidAngle;
        copy.solidAngleApparent = solidAngleApparent;
        copy.size = size;
        copy.distToCamera = distToCamera;
        copy.pos.set(pos);
        copy.posEpoch.set(posEpoch);
        return copy;
    }
}
