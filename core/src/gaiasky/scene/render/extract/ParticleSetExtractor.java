package gaiasky.scene.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Render;
import gaiasky.scene.component.StarSet;
import gaiasky.util.Settings;

/**
 * Extracts particle and star set data to feed to the render stages.
 */
public class ParticleSetExtractor extends AbstractExtractSystem {
    public ParticleSetExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        extractEntity(entity);
    }

    public void extractEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        if (shouldRender(base)) {
            var render = Mapper.render.get(entity);
            if (Mapper.starSet.has(entity)) {
                addToRenderLists(render, Mapper.starSet.get(entity));
            } else {
                addToRenderLists(render);
            }
        }
    }

    /** For star sets. **/
    private void addToRenderLists(Render render, StarSet starSet) {
        if (starSet.variableStars) {
            addToRender(render, RenderGroup.VARIABLE_GROUP);
        } else {
            addToRender(render, RenderGroup.STAR_GROUP);
        }
        addToRender(render, RenderGroup.MODEL_VERT_STAR);
        if (Settings.settings.scene.star.group.billboard) {
            addToRender(render, RenderGroup.BILLBOARD_STAR);
        }
        if (GaiaSky.instance.sgr.isOn(ComponentTypes.ComponentType.VelocityVectors)) {
            addToRender(render, RenderGroup.LINE);
        }
        if (renderText()) {
            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }

    /** For particle sets. **/
    private void addToRenderLists(Render render) {
        addToRender(render, RenderGroup.PARTICLE_GROUP);
        if (renderText()) {
            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }

    private boolean renderText() {
        return GaiaSky.instance.isOn(ComponentType.Labels);
    }
}
