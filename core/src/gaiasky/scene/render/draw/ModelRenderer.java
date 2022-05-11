/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.render.draw;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.*;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.api.IModelRenderable;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.render.SceneRenderer;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.AtmosphereComponent;
import gaiasky.scenegraph.component.CloudComponent;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Settings;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.math.MathUtilsd;

/**
 * Renders simple models using a model batch.
 */
public class ModelRenderer extends AbstractRenderSystem {

    public enum ModelRenderType {
        NORMAL,
        ATMOSPHERE,
        CLOUD
    }

    protected ComponentTypes ctAtm, ctClouds;

    protected IntModelBatch batch;
    protected ModelRenderType type;

    private ModelRenderObject renderObject;

    /**
     * Creates a new model batch render component.
     *
     * @param rg     The render group.
     * @param alphas The alphas list.
     * @param batch  The model batch.
     */
    public ModelRenderer(RenderGroup rg, float[] alphas, IntModelBatch batch) {
        super(rg, alphas, null);
        this.batch = batch;
        this.renderObject = new ModelRenderObject();
        this.ctAtm = new ComponentTypes(ComponentType.Atmospheres);
        this.ctClouds = new ComponentTypes(ComponentType.Clouds);
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (mustRender()) {
            batch.begin(camera.getCamera());
            renderables.forEach(r -> {
                Render render = (Render) r;
                renderObject.render(render.entity, batch, camera, getAlpha(render.entity), t, rc, getRenderGroup(), !Mapper.tagQuatOrientation.has(render.entity));
            });
            batch.end();
        }
    }

    public static class ModelRenderObject {

        public ModelRenderObject() {
        }

        /**
         * Renders a single entity as a model. The entity is assumed to have a {@link gaiasky.scene.component.Model} component.
         *
         * @param entity            The entity to render.
         * @param batch             The model batch to use.
         * @param camera            The camera.
         * @param alpha             The opacity value in [0,1].
         * @param t                 The time, in seconds, since the start of the session.
         * @param rc                The rendering context object.
         * @param renderGroup       The render group.
         * @param shadowEnvironment Whether to prepare the shadow environment, to render the shadow map.
         */
        public void render(Entity entity, IntModelBatch batch, ICamera camera, float alpha, double t, RenderingContext rc, RenderGroup renderGroup, boolean shadowEnvironment) {
            var model = Mapper.model.get(entity);
            var scaffolding = Mapper.modelScaffolding.get(entity);
            if (Mapper.atmosphere.has(entity)) {
                // Planet
                renderPlanet(entity, model, scaffolding, batch, alpha, t, rc, renderGroup);
            } else {
                boolean relativistic = !(Mapper.engine.has(entity) && camera.getMode().isSpacecraft());
                // Generic model
                renderGenericModel(model, scaffolding, batch, alpha, relativistic, shadowEnvironment);
            }
        }

        /**
         * Renders a generic model.
         *
         * @param model             The model component.
         * @param scaffolding       The scaffolding component.
         * @param batch             The batch.
         * @param alpha             The alpha value.
         * @param relativistic      Whether to apply relativistic effects.
         * @param shadowEnvironment Whether to prepare the shadow environment.
         */
        private void renderGenericModel(Model model, ModelScaffolding scaffolding, IntModelBatch batch, float alpha, boolean relativistic, boolean shadowEnvironment) {
            if (model.model.isModelInitialised()) {
                if (shadowEnvironment) {
                    prepareShadowEnvironment(model, scaffolding);
                }
                model.model.update(alpha * scaffolding.fadeOpacity, relativistic);
                batch.render(model.model.instance, model.model.env);
            }
        }

        /**
         * Renders a spacecraft. Not used right now.
         *
         * @param model             The model component.
         * @param scaffolding       The scaffolding component.
         * @param graph             The graph component.
         * @param batch             The model batch.
         * @param alpha             The alpha value.
         * @param shadowEnvironment Whether to prepare the shadow environment.
         */
        private void renderSpacecraft(Model model, ModelScaffolding scaffolding, GraphNode graph, IntModelBatch batch, float alpha, boolean shadowEnvironment) {
            ICamera cam = GaiaSky.instance.getICamera();
            ModelComponent mc = model.model;
            if (mc.isModelInitialised()) {
                // Good, render
                if (shadowEnvironment) {
                    prepareShadowEnvironment(model, scaffolding);
                }
                mc.setTransparency(alpha * scaffolding.fadeOpacity);
                if (cam.getMode().isSpacecraft())
                    // In SPACECRAFT_MODE mode, we are not affected by relativistic aberration or Doppler shift
                    mc.updateRelativisticEffects(cam, 0);
                else
                    mc.updateRelativisticEffects(cam);
                mc.updateVelocityBufferUniforms(cam);
                batch.render(mc.instance, mc.env);
            } else {
                // Keep loading
                mc.load(graph.localTransform);
            }
        }

        /**
         * Renders a planet.
         *
         * @param entity      The entity.
         * @param model       The model component.
         * @param scaffolding The scaffolding component.
         * @param batch       The batch.
         * @param alpha       The alpha value.
         * @param t           The time, in seconds, since the session start.
         * @param rc          The rendering context.
         * @param renderGroup The render group.
         */
        private void renderPlanet(Entity entity, Model model, ModelScaffolding scaffolding, IntModelBatch batch, float alpha, double t, RenderingContext rc, RenderGroup renderGroup) {
            var base = Mapper.base.get(entity);
            var body = Mapper.body.get(entity);
            var atmosphere = Mapper.atmosphere.get(entity);
            var cloud = Mapper.cloud.get(entity);
            if (renderGroup == RenderGroup.MODEL_ATM) {
                // Atmosphere
                renderAtmosphere(batch, entity, body, model, scaffolding, atmosphere, GaiaSky.instance.sgr.alphas[ComponentType.Atmospheres.ordinal()], rc);
            } else if (renderGroup == RenderGroup.MODEL_CLOUD) {
                // Clouds
                renderClouds(batch, entity, base, model, cloud, GaiaSky.instance.sgr.alphas[ComponentType.Clouds.ordinal()], t);
            } else {
                // If atmosphere ground params are present, set them
                if (atmosphere.atmosphere != null) {
                    float atmOpacity = (float) MathUtilsd.lint(body.viewAngle, 0.00745329f, 0.02490659f, 0f, 1f);
                    if (Settings.settings.scene.visibility.get(ComponentType.Atmospheres.toString()) && atmOpacity > 0) {
                        var graph = Mapper.graph.get(entity);
                        var rotation = Mapper.rotation.get(entity);
                        atmosphere.atmosphere.updateAtmosphericScatteringParams(model.model.instance.materials.first(), alpha * atmOpacity, true, graph, rotation, scaffolding, rc.vrOffset);
                    } else {
                        atmosphere.atmosphere.removeAtmosphericScattering(model.model.instance.materials.first());
                    }
                }
                // Regular planet, render model normally
                prepareShadowEnvironment(model, scaffolding);
                model.model.update(alpha * base.opacity);
                batch.render(model.model.instance, model.model.env);
            }
        }

        /**
         * Renders the atmosphere of a planet.
         */
        public void renderAtmosphere(
                IntModelBatch modelBatch,
                Entity entity,
                Body body,
                Model model,
                ModelScaffolding scaffolding,
                Atmosphere atmosphere,
                float alpha,
                RenderingContext rc) {

            // Atmosphere fades in between 1 and 2 degrees of view angle apparent
            ICamera cam = GaiaSky.instance.getICamera();
            float atmOpacity = (float) MathUtilsd.lint(body.viewAngle, 0.00745329f, 0.02490659f, 0f, 1f);
            if (atmOpacity > 0) {
                var graph = Mapper.graph.get(entity);
                var rotation = Mapper.rotation.get(entity);
                AtmosphereComponent ac = atmosphere.atmosphere;
                ac.updateAtmosphericScatteringParams(ac.mc.instance.materials.first(), alpha * atmOpacity, false, graph, rotation, scaffolding, rc.vrOffset);
                ac.mc.updateRelativisticEffects(cam);
                modelBatch.render(ac.mc.instance, model.model.env);
            }
        }

        /**
         * Renders the cloud layer of a planet.
         */
        public void renderClouds(IntModelBatch modelBatch, Entity entity, Base base, Model model, Cloud cloud, float alpha, double t) {
            CloudComponent clc = cloud.cloud;
            clc.touch();
            ICamera cam = GaiaSky.instance.getICamera();
            clc.mc.updateRelativisticEffects(cam);
            clc.mc.updateVelocityBufferUniforms(cam);
            clc.mc.setTransparency(alpha * base.opacity, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR);
            modelBatch.render(clc.mc.instance, model.model.env);
        }

        /**
         * Prepares the shadow environment for shadow mapping.
         */
        protected void prepareShadowEnvironment(Model model, ModelScaffolding scaffolding) {
            if (Settings.settings.scene.renderer.shadow.active) {
                Environment env = model.model.env;
                SceneRenderer sceneRenderer = GaiaSky.instance.sceneRenderer;
                if (scaffolding.shadow > 0 && sceneRenderer.smTexMap.containsKey(this)) {
                    Matrix4 combined = sceneRenderer.smCombinedMap.get(this);
                    Texture tex = sceneRenderer.smTexMap.get(this);
                    if (env.shadowMap == null) {
                        if (scaffolding.shadowMap == null)
                            scaffolding.shadowMap = new ShadowMapImpl(combined, tex);
                        env.shadowMap = scaffolding.shadowMap;
                    }
                    scaffolding.shadowMap.setProjViewTrans(combined);
                    scaffolding.shadowMap.setDepthMap(tex);

                    scaffolding.shadow--;
                } else {
                    env.shadowMap = null;
                }
            } else {
                model.model.env.shadowMap = null;
            }
        }
    }

    protected boolean mustRender() {
        return true;
    }

}
