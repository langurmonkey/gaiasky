/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.utils.Array;
import gaiasky.render.*;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scenegraph.SceneGraphNode.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.comp.ModelComparator;
import gaiasky.util.gdx.IntModelBatch;

/**
 * Renders with a given model batch.
 * 
 * @author Toni Sagrista
 *
 */
public class ModelBatchRenderSystem extends AbstractRenderSystem {

    public enum ModelRenderType {
        NORMAL, ATMOSPHERE, CLOUD
    }

    private ComponentTypes ctAtm, ctClouds;

    private IntModelBatch batch;
    private ModelRenderType type;

    /**
     * Creates a new model batch render component.
     * 
     * @param rg
     *            The render group.
     * @param alphas
     *            The alphas list.
     * @param batch
     *            The model batch.
     * @param type
     *            The model render type
     */
    public ModelBatchRenderSystem(RenderGroup rg, float[] alphas, IntModelBatch batch, ModelRenderType type) {
        super(rg, alphas, null);
        this.batch = batch;
        this.type = type;
        comp = new ModelComparator<>();

        this.ctAtm = new ComponentTypes(ComponentType.Atmospheres);
        this.ctClouds = new ComponentTypes(ComponentType.Clouds);
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (mustRender()) {
            batch.begin(camera.getCamera());
            int size = renderables.size;
            for (int i = 0; i < size; i++) {
                IModelRenderable s = (IModelRenderable) renderables.get(i);
                // Route to correct interface
                switch (type) {
                case NORMAL:
                    s.render(batch, getAlpha(s), t, rc);
                    break;
                case ATMOSPHERE:
                    ((IAtmosphereRenderable) s).renderAtmosphere(batch, getAlpha(ctAtm), t, rc.vroffset);
                    break;
                case CLOUD:
                    ((ICloudRenderable) s).renderClouds(batch, getAlpha(ctClouds), t);
                    break;
                }
            }
            batch.end();

        }
    }

    protected boolean mustRender() {
        return true;
    }

}
