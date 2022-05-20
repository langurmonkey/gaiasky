package gaiasky.scene.render.draw.text;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.RenderingContext;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.render.draw.TextRenderer;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;

/**
 * Renders object labels.
 */
public class LabelRenderSystem {

    /** Implements the old <code>isLabel()</code> method from {@link gaiasky.render.api.I3DTextRenderable}. **/
    public boolean isLabel(Entity entity) {
        if (Mapper.label.has(entity)) {
            return Mapper.label.get(entity).label;
        } else if (Mapper.loc.has(entity) || Mapper.title.has(entity)) {
            return false;
        }
        return true;
    }

    /** Computes the text opacity for the given entity. **/
    public float getTextOpacity(Entity entity) {
        var base = Mapper.base.get(entity);
        if (Mapper.tagQuatOrientation.has(entity)) {
            // Billboard labels should go with the model opacity.
            return Math.min(base.opacity, Mapper.modelScaffolding.get(entity).fadeOpacity);
        } else {
            return base.opacity;
        }
    }

    /**
     * Renders the text for the given entity.
     *
     * @param entity The entity.
     * @param batch  The sprite batch.
     * @param shader The shader.
     * @param sys    The text renderer.
     * @param rc     The render context.
     * @param camera The camera.
     */
    public void render(Entity entity, ExtSpriteBatch batch, ExtShaderProgram shader, TextRenderer sys, RenderingContext rc, ICamera camera){

    }
}
