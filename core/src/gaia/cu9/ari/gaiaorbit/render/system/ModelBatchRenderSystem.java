package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.render.*;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.comp.ModelComparator;

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

    private ModelBatch batch;
    private boolean sort = true;
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
    public ModelBatchRenderSystem(RenderGroup rg, float[] alphas, ModelBatch batch, ModelRenderType type) {
        super(rg, alphas, null);
        this.batch = batch;
        this.type = type;
        comp = new ModelComparator<IRenderable>();

        this.ctAtm = new ComponentTypes(ComponentType.Atmospheres);
        this.ctClouds = new ComponentTypes(ComponentType.Clouds);
    }

    public ModelBatchRenderSystem(RenderGroup rg, float[] alphas, ModelBatch batch, ModelRenderType type, boolean sort) {
        this(rg, alphas, batch, type);
        this.sort = sort;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (sort)
            renderables.sort(comp);
        Gdx.gl.glLineWidth(1.2f * GlobalConf.SCALE_FACTOR);
        if (mustRender()) {
            batch.begin(camera.getCamera());

            int size = renderables.size;
            for (int i = 0; i < size; i++) {
                IModelRenderable s = (IModelRenderable) renderables.get(i);
                // Route to correct interface
                switch (type) {
                case NORMAL:
                    s.render(batch, getAlpha(s), t);
                    break;
                case ATMOSPHERE:
                    ((IAtmosphereRenderable) s).renderAtmosphere(batch, getAlpha(ctAtm), t);
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
