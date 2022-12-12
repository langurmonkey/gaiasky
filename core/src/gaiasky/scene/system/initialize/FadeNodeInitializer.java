package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Index;
import gaiasky.scene.Mapper;
import gaiasky.util.math.Vector2d;

/**
 * Initializes objects with a {@link gaiasky.scene.component.Fade} component.
 */
public class FadeNodeInitializer extends AbstractInitSystem {

    /** The index reference. **/
    private final Index index;

    public FadeNodeInitializer(final Index index, boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        this.index = index;
    }

    @Override
    public void initializeEntity(Entity entity) {
        var fade = Mapper.fade.get(entity);

        // Initialize default mappings, if no mappings are set
        if (fade.fadeIn != null && fade.fadeInMap == null) {
            fade.fadeInMap = new Vector2d(0, 1);
        }
        if (fade.fadeOut != null && fade.fadeOutMap == null) {
            fade.fadeOutMap = new Vector2d(1, 0);
        }

    }

    @Override
    public void setUpEntity(Entity entity) {
        var fade = Mapper.fade.get(entity);
        if (fade.fadePositionObjectName != null) {
            fade.fadePositionObject = index.getEntity(fade.fadePositionObjectName);
        }
    }
}
