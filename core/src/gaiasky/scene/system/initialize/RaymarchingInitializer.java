package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.entity.FocusActive;
import gaiasky.util.Settings;

import java.time.Instant;

/**
 * Initializes invisible and raymarching container entities.
 */
public class RaymarchingInitializer extends AbstractInitSystem {

    public RaymarchingInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var focus = Mapper.focus.get(entity);

        // Focus active
        focus.activeFunction = FocusActive::isFocusActiveTrue;

        if (graph.parentName == null) {
            graph.parentName = Scene.ROOT_NAME;
        }
        if (body.size == 0) {
            body.setSizeM(500.0);
        } else {
            // Size is given as a radius in km.
            body.setRadiusKm((double) body.size);
        }
        if (base.ct == null || base.ct.allSetLike(new ComponentTypes(ComponentType.Others))) {
            base.ct = new ComponentTypes(ComponentType.Invisible);
        }

    }

    @Override
    public void setUpEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var raymarching = Mapper.raymarching.get(entity);

        if (raymarching != null) {
            if (raymarching.raymarchingShader != null && !raymarching.raymarchingShader.isBlank() && !Settings.settings.program.safeMode)
                EventManager.publish(Event.RAYMARCHING_CMD, this, base.getName(), false, entity, raymarching.raymarchingShader, new float[] { 1f, 0f, 0f, 0f });
            else
                raymarching.raymarchingShader = null;
        }
    }
}
