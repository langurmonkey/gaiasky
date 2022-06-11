/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.render.draw;

import com.badlogic.gdx.utils.Array;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IModelRenderable;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Render;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.IntModelBatch;
import org.lwjgl.opengl.GL41;

/**
 * Renders model objects with tessellation shaders.
 */
public class TessellationRenderer extends AbstractRenderSystem {
    private final IntModelBatch batch;
    private ModelEntityRender renderObject;

    /**
     * Creates a new model batch render component.
     *
     * @param rg     The render group.
     * @param alphas The alphas list.
     * @param batch  The model batch.
     */
    public TessellationRenderer(RenderGroup rg, float[] alphas, IntModelBatch batch) {
        super(rg, alphas, null);
        this.batch = batch;
        this.renderObject = new ModelEntityRender();
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (mustRender()) {
            // Triangles for tessellation
            GL41.glPatchParameteri(GL41.GL_PATCH_VERTICES, 3);
            batch.begin(camera.getCamera());
            renderables.forEach(r ->{
                Render render = (Render) r;
                renderObject.render(render.entity, batch, camera, getAlpha(render.entity), t, rc, getRenderGroup(), !Mapper.tagQuatOrientation.has(render.entity));
            });
            batch.end();
        }
    }

    protected boolean mustRender() {
        return true;
    }

}