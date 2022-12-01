package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.component.Base;

/**
 * This class contains the implementation of {@link IFocus#isFocusActive()}.
 */
public class FocusActive {

    public boolean isFocusActiveTrue(Entity entity, Base base) {
        return true;
    }

    public boolean isFocusActiveFalse(Entity entity, Base base) {
        return true;
    }

    public boolean isFocusActiveCtOpacity(Entity entity, Base base) {
        return GaiaSky.instance.isOn(base.ct) && base.opacity > 0;
    }

    public boolean isFocusActiveGroup(Entity entity, Base base) {
        if (Mapper.starSet.has(entity)) {
            return Mapper.starSet.get(entity).focusIndex >= 0;
        } else if (Mapper.particleSet.has(entity)) {
            return Mapper.particleSet.get(entity).focusIndex >= 0;
        } else {
            return false;
        }
    }
}
