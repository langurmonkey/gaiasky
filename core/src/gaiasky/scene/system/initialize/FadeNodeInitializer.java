package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.scene.Index;
import gaiasky.scene.Mapper;

/**
 * Initializes objects with a {@link gaiasky.scene.component.Fade} component.
 */
public class FadeNodeInitializer extends InitSystem {

    /** The index reference. **/
    private final Index index;

    public FadeNodeInitializer(final Index index, boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        this.index = index;
    }

    @Override
    public void initializeEntity(Entity entity) {
    }

    @Override
    public void setUpEntity(Entity entity) {
        var fade = Mapper.fade.get(entity);
        if (fade.positionObjectName != null) {
            fade.positionObject = index.getNode(fade.positionObjectName);
        }
    }
}
