package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Fade;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

/**
 * Updates entities with a {@link gaiasky.scene.component.Fade} component.
 * This must happen fairly early in the pipeline.
 */
public class FadeUpdater extends IteratingSystem implements EntityUpdater {

    private Vector3d D31;
    private Vector3b B31;

    public FadeUpdater(Family family, int priority) {
        super(family, priority);
        this.D31 = new Vector3d();
        this.B31 = new Vector3b();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var body = Mapper.body.get(entity);
        var fade = Mapper.fade.get(entity);

        var camera = GaiaSky.instance.getICamera();

        if (fade.fadePositionObject != null) {
            fade.currentDistance = Mapper.body.get(fade.fadePositionObject).distToCamera;
        } else if (fade.fadePosition != null) {
            fade.currentDistance = D31.set(fade.fadePosition).sub(camera.getPos()).len() * camera.getFovFactor();
        } else {
            fade.currentDistance = D31.set(body.pos).sub(camera.getPos()).len() * camera.getFovFactor();
        }
        body.distToCamera = fade.fadePositionObject == null ? body.pos.dst(camera.getPos(), B31).doubleValue() : Mapper.body.get(fade.fadePositionObject).distToCamera;
    }
}
