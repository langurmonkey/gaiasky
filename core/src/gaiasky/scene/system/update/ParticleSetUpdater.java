package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.DatasetDescription;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.component.StarSet;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;

/**
 * Updates particle and star sets.
 */
public class ParticleSetUpdater extends AbstractUpdateSystem {

    private final ParticleUtils utils;

    public ParticleSetUpdater(Family family, int priority) {
        super(family, priority);
        this.utils = new ParticleUtils();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var camera = GaiaSky.instance.cameraManager;
        if (Mapper.starSet.has(entity)) {
            updateStarSet(camera, Mapper.starSet.get(entity), Mapper.datasetDescription.get(entity));
        } else if (Mapper.particleSet.has(entity)) {
            updateParticleSet(camera, Mapper.particleSet.get(entity));
        }
    }

    private void updateParticleSet(ICamera camera, ParticleSet particleSet) {
        if (particleSet.pointData != null) {
            particleSet.cPosD.set(camera.getPos());

            if (particleSet.focusIndex >= 0) {
                particleSet.updateFocus(camera);
            }
        }
    }

    private void updateStarSet(ICamera camera, StarSet starSet, DatasetDescription datasetDesc) {
        // Fade node visibility
        if (starSet.active.length > 0) {
            starSet.cPosD.set(camera.getPos());
            // Delta years
            starSet.currDeltaYears = AstroUtils.getMsSince(GaiaSky.instance.time.getTime(), starSet.epochJd) * Nature.MS_TO_Y;

            updateParticleSet(camera, starSet);

            // Update close stars
            int j = 0;
            for (int i = 0; i < Math.min(starSet.proximity.updating.length, starSet.pointData.size()); i++) {
                if (utils.filter(starSet.active[i], starSet, datasetDesc)
                        && starSet.isVisible(starSet.active[i])) {
                    IParticleRecord closeStar = starSet.pointData.get(starSet.active[i]);
                    starSet.proximity.set(j, starSet.active[i], closeStar, camera, starSet.currDeltaYears);
                    camera.checkClosestParticle(starSet.proximity.updating[j]);

                    // Model distance
                    if (j == 0) {
                        starSet.modelDist = 172.4643429 * closeStar.radius();
                    }
                    j++;
                }
            }
        }
    }
}
