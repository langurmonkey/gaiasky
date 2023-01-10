package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.ParticleExtra;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.component.StarSet;

import java.util.Locale;

/**
 * An entity view that implements the {@link IRenderable} methods.
 */
public class RenderView extends BaseView implements IRenderable {

    /** Particle component, maybe. **/
    public ParticleExtra extra;
    /** Particle set component **/
    public ParticleSet particleSet;
    /** Star set component **/
    public StarSet starSet;

    public RenderView() {
    }

    public RenderView(Entity entity) {
        super(entity);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.extra = Mapper.extra.get(entity);
        this.particleSet = Mapper.particleSet.get(entity);
        this.starSet = Mapper.starSet.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.extra = null;
        this.particleSet = null;
        this.starSet = null;
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
        assert starSet != null : "Called the wrong method!";
        name = name.toLowerCase(Locale.ROOT).trim();
        if (starSet.index.containsKey(name)) {
            int idx = starSet.index.get(name);
            if (starSet.labelColors.containsKey(idx)) {
                return starSet.labelColors.get(idx);
            }
        }
        return body.labelColor;
    }
}
