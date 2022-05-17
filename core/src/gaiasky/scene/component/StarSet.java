package gaiasky.scene.component;

import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Settings;
import gaiasky.util.math.Vector3d;

import java.util.Map;
import java.util.Set;

public class StarSet extends ParticleSet {
    /**
     * Epoch for positions/proper motions in julian days
     **/
    public double epochJd;

    /**
     * Epoch for the times in the light curves in julian days
     */
    public double variabilityEpochJd;
    /**
     * Current computed epoch time
     **/
    public double currDeltaYears = 0;

    public double modelDist;

    /** Does this contain variable stars? **/
    public boolean variableStars = false;

    /** Stars for which forceLabel is enabled **/
    public Set<Integer> forceLabelStars;
    /** Stars with special label colors **/
    public Map<Integer, float[]> labelColors;

    private final Vector3d D32 = new Vector3d();
    private final Vector3d D33 = new Vector3d();


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

    protected Vector3d fetchPosition(IParticleRecord pb, Vector3d campos, Vector3d out, double deltaYears) {
        Vector3d pm = D32.set(pb.pmx(), pb.pmy(), pb.pmz()).scl(deltaYears);
        Vector3d dest = D33.set(pb.x(), pb.y(), pb.z());
        if (campos != null && !campos.hasNaN())
            dest.sub(campos).add(pm);
        else
            dest.add(pm);

        return out.set(dest);
    }
}
