package gaiasky.scene.component;

import gaiasky.GaiaSky;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.scenegraph.particle.VariableRecord;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.util.Map;
import java.util.Set;

public class StarSet extends ParticleSet {
    /**
     * Epoch for positions/proper motions in julian days.
     **/
    public double epochJd;

    /**
     * Epoch for the times in the light curves in julian days.
     */
    public double variabilityEpochJd;

    /**
     * Current computed epoch time.
     **/
    public double currDeltaYears = 0;

    public double modelDist;

    /** Does this contain variable stars? **/
    public boolean variableStars = false;

    /** Stars for which forceLabel is enabled. **/
    public Set<Integer> forceLabelStars;

    /** Stars with special label colors. **/
    public Map<Integer, float[]> labelColors;

    public final Vector3d D32 = new Vector3d();
    public final Vector3d D33 = new Vector3d();

    public Vector3b getAbsolutePosition(String name, Vector3b aux) {
        Vector3d vec = getAbsolutePosition(name, D31);
        aux.set(vec);
        return aux;
    }

    public Vector3d getAbsolutePosition(String name, Vector3d aux) {
        if (index.containsKey(name)) {
            int idx = index.get(name);
            IParticleRecord sb = pointData.get(idx);
            fetchPosition(sb, null, aux, currDeltaYears);
            return aux;
        } else {
            return null;
        }
    }

    /**
     * Updates the parameters of the focus, if the focus is active in this group
     *
     * @param camera The current camera
     */
    public void updateFocus(ICamera camera) {
        IParticleRecord focus = pointData.get(focusIndex);
        Vector3d aux = this.fetchPosition(focus, cPosD, D31, currDeltaYears);

        this.focusPosition.set(aux).add(camera.getPos());
        this.focusDistToCamera = aux.len();
        this.focusSize = getFocusSize();
        this.focusViewAngle = (float) ((getRadius() / this.focusDistToCamera) / camera.getFovFactor());
        this.focusViewAngleApparent = this.focusViewAngle * Settings.settings.scene.star.brightness;
    }

    public Vector3d fetchPosition(IParticleRecord pb, Vector3d campos, Vector3d out, double deltaYears) {
        Vector3d pm = D32.set(pb.pmx(), pb.pmy(), pb.pmz()).scl(deltaYears);
        Vector3d dest = D33.set(pb.x(), pb.y(), pb.z());
        if (campos != null && !campos.hasNaN())
            dest.sub(campos).add(pm);
        else
            dest.add(pm);

        return out.set(dest);
    }

    /**
     * Sets the epoch to use for the stars in this set.
     *
     * @param epochJd The epoch in julian days (days since January 1, 4713 BCE).
     */
    public void setEpoch(Double epochJd) {
        setEpochJd(epochJd);
    }

    /**
     * Sets the epoch to use for the stars in this set.
     *
     * @param epochJd The epoch in julian days (days since January 1, 4713 BCE).
     */
    public void setEpochJd(Double epochJd) {
        this.epochJd = epochJd;
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


}
