/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Settings;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.math.Vector3d;

public class StarSet extends ParticleSet {
    /**
     * Epoch for the times in the light curves in julian days.
     */
    public double variabilityEpochJd;
    public double modelDist;
    /**
     * Does this contain variable stars?
     **/
    public boolean variableStars = false;
    /**
     * Number of particles to render as a billboard for this group.
     **/
    public int numBillboards = -1;

    /**
     * Updates the parameters of the focus, if the focus is active in this group.
     * This version is special for star sets, and uses the double-precision version of fetchPosition()
     * for speed.
     *
     * @param camera The current camera
     */
    public void updateFocus(ICamera camera) {
        IParticleRecord focus = pointData.get(focusIndex);
        Vector3d aux = this.fetchPositionDouble(focus, cPosD, D31, currDeltaYears);
        this.focusPosition.set(aux).add(cPosD);
        this.focusDistToCamera = aux.len();
        this.focusSize = getFocusSize();
        this.focusSolidAngle = (float) ((getRadius() / this.focusDistToCamera) / camera.getFovFactor());
        this.focusSolidAngleApparent = this.focusSolidAngle * Settings.settings.scene.star.brightness;
    }

    /**
     * Sets the light curve epoch to use for the stars in this group.
     *
     * @param epochJd The light curve epoch in julian days (days since January 1, 4713 BCE).
     */
    public void setVariabilityEpoch(Double epochJd) {
        setVariabilityEpochJd(epochJd);
    }

    /**
     * Sets the light curve epoch to use for the stars in this group.
     *
     * @param epochJd The light curve epoch in julian days (days since January 1, 4713 BCE).
     */
    public void setVariabilityEpochJd(Double epochJd) {
        this.variabilityEpochJd = epochJd;
    }


    public int getHip() {
        if (focus != null && focus.hip() > 0)
            return focus.hip();
        return -1;
    }

    public long getId() {
        if (focus != null)
            return focus.id();
        else
            return -1;
    }

    public double getCandidateSolidAngleApparent() {
        return getSolidAngleApparent(candidateFocusIndex);
    }

    /**
     * Returns the apparent solid angle of the star with the given index.
     *
     * @param index The index in the star list.
     * @return The apparent solid angle.
     */
    public double getSolidAngleApparent(int index) {
        if (index >= 0) {
            IParticleRecord candidate = pointData.get(index);
            Vector3d aux = candidate.pos(D31);
            ICamera camera = GaiaSky.instance.getICamera();
            double va = (float) ((candidate.radius() / aux.sub(camera.getPos()).len()) / camera.getFovFactor());
            return va * Settings.settings.scene.star.brightness;
        } else {
            return -1;
        }

    }

    public double getClosestDistToCamera() {
        return this.proximity.updating[0].distToCamera;
    }

    public double getClosestSize() {
        return this.proximity.updating[0].size;
    }

    public Vector3d getClosestPos(Vector3d out) {
        return out.set(this.proximity.updating[0].pos);
    }

    public Vector3Q getClosestAbsolutePos(Vector3Q out) {
        return out.set(this.proximity.updating[0].absolutePos);
    }

    public float[] getClosestCol() {
        return this.proximity.updating[0].col;
    }

    public void markForUpdate(Render render) {
        if (variableStars) {
            GaiaSky.postRunnable(() -> EventManager.publish(Event.GPU_DISPOSE_VARIABLE_GROUP, render));
        } else {
            GaiaSky.postRunnable(() -> EventManager.publish(Event.GPU_DISPOSE_STAR_GROUP, render));
        }
    }
}
