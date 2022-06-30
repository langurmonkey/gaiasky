package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.ComponentTypes;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.ParticleExtra;
import gaiasky.scene.component.StarSet;

import java.util.Locale;

/**
 * An entity view that implements the {@link IRenderable} methods.
 */
public class RenderView extends BaseView implements IRenderable {

    /** Particle component, maybe. **/
    protected ParticleExtra extra;
    /** Star set component **/
    protected StarSet set;

    public RenderView() {
    }

    public RenderView(Entity entity) {
        super(entity);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.extra = Mapper.extra.get(entity);
        this.set = Mapper.starSet.get(entity);
    }

    @Override
    public ComponentTypes getComponentType() {
        return base.ct;
    }

    @Override
    public double getDistToCamera() {
        return body.distToCamera;
    }

    @Override
    public float getOpacity() {
        return base.opacity;
    }

    public double getRadius() {
        return extra == null ? body.size / 2.0 : extra.radius;
    }

    /** Text color for single objects **/
    public float[] textColour() {
        return body.labelColor;
    }

    /** Text color for the star with the given name in a star set. **/
    public float[] textColour(String name) {
        assert set != null : "Called the wrong method!";
        name = name.toLowerCase(Locale.ROOT).trim();
        if (set.index.containsKey(name)) {
            int idx = set.index.get(name);
            if (set.labelColors.containsKey(idx)) {
                return set.labelColors.get(idx);
            }
        }
        return body.labelColor;
    }
}
