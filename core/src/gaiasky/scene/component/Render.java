package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.render.ComponentTypes;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;

/**
 * This component marks the entity as renderable, and keeps a reference to it.
 */
public class Render implements Component, IRenderable {

    public Entity entity;

    public Entity getEntity() {
        return entity;
    }

    @Override
    public ComponentTypes getComponentType() {
        return Mapper.base.get(entity).ct;
    }

    @Override
    public double getDistToCamera() {
        return Mapper.body.get(entity).distToCamera;
    }

    @Override
    public float getOpacity() {
        return Mapper.base.get(entity).opacity;
    }
}
