package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;

/**
 * Initializes objects with a {@link gaiasky.scene.component.Fade} component.
 */
public class FadeNodeInitializer extends InitSystem{

    public FadeNodeInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
    }

    @Override
    public void setUpEntity(Entity entity) {
        var fade = Mapper.fade.get(entity);
        if (fade.positionObjectName != null) {
            fade.positionObject = GaiaSky.instance.scene.getNode(fade.positionObjectName);
        }
    }
}
