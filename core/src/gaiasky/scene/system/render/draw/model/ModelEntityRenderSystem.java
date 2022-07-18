package gaiasky.scene.system.render.draw.model;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.ShadowMapImpl;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.AtmosphereComponent;
import gaiasky.scenegraph.component.CloudComponent;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Settings;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.FloatAttribute;
import gaiasky.util.math.MathUtilsd;

/**
 * Contains the logic to render model entities, the ones that
 * have a {@link Model} component.
 */
public class ModelEntityRenderSystem {

    private final ParticleUtils utils;

    public ModelEntityRenderSystem() {
        this.utils = new ParticleUtils();
    }

    /**
     * Renders a single entity as a model. The entity is assumed to have a {@link gaiasky.scene.component.Model} component.
     *
     * @param entity      The entity to render.
     * @param batch       The model batch to use.
     * @param camera      The camera.
     * @param alpha       The opacity value in [0,1].
     * @param t           The time, in seconds, since the start of the session.
     * @param rc          The rendering context object.
     * @param renderGroup The render group.
     * @param shadow      Whether to prepare the shadow environment, to render the shadow map.
     */
    public void render(Entity entity, IntModelBatch batch, ICamera camera, float alpha, double t, RenderingContext rc, RenderGroup renderGroup, boolean shadow) {
        var model = Mapper.model.get(entity);
        if (model != null) {
            if (model.renderConsumer != null) {
                boolean relativistic = !(Mapper.engine.has(entity) && camera.getMode().isSpacecraft());

                // Just run consumer.
                model.renderConsumer.apply(this, entity, model, batch, alpha, t, rc, renderGroup, relativistic, shadow);
            }
        }
    }

    /**
     * Renders a generic model.
     *
     * @param model        The model component.
     * @param batch        The batch.
     * @param alpha        The alpha value.
     * @param t            The time.
     * @param rc           The rendering context.
     * @param renderGroup  The render group.
     * @param relativistic Whether to apply relativistic effects.
     * @param shadow       Whether to prepare the shadow environment.
     */
    public void renderGenericModel(Entity entity, Model model, IntModelBatch batch, float alpha, double t, RenderingContext rc, RenderGroup renderGroup, boolean relativistic, boolean shadow) {
        var scaffolding = Mapper.modelScaffolding.get(entity);

        ModelComponent mc = model.model;
        if (mc != null && mc.isModelInitialised()) {
            var base = Mapper.base.get(entity);
            if (scaffolding != null && shadow) {
                prepareShadowEnvironment(entity, model, scaffolding);
            }

            float alphaFactor;
            if (scaffolding != null) {
                alphaFactor = Mapper.fade.has(entity) ? base.opacity : scaffolding.fadeOpacity;
            } else {
                var body = Mapper.body.get(entity);
                alphaFactor = body.color[3] * base.opacity;
            }

            mc.update(alpha * alphaFactor, relativistic);
            batch.render(mc.instance, mc.env);
        }
    }

    /**
     * Renders a wireframe/shape model.
     *
     * @param entity       The entity.
     * @param model        The model component.
     * @param batch        The batch.
     * @param alpha        The alpha value.
     * @param t            The time.
     * @param rc           The rendering context.
     * @param renderGroup  The render group.
     * @param relativistic Whether to apply relativistic effects.
     * @param shadow       Whether to prepare the shadow environment.
     */
    public void renderShape(Entity entity, Model model, IntModelBatch batch, float alpha, double t, RenderingContext rc, RenderGroup renderGroup, boolean relativistic, boolean shadow) {
        var mc = model.model;
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);

        mc.update(null, alpha * base.opacity * body.color[3], GL20.GL_ONE, GL20.GL_ONE);
        // Depth reads, no depth writes
        mc.setDepthTest(GL20.GL_LEQUAL, false);
        Gdx.gl20.glLineWidth(1.5f);
        mc.instance.transform.set(graph.localTransform);
        batch.render(mc.instance, mc.env);
    }

    /**
     * Renders the recursive grid.
     *
     * @param entity       The entity.
     * @param model        The model component.
     * @param modelBatch   The batch.
     * @param alpha        The alpha value.
     * @param t            The time.
     * @param rc           The rendering context.
     * @param renderGroup  The render group.
     * @param relativistic Whether to apply relativistic effects.
     * @param shadow       Whether to prepare the shadow environment.
     */
    public void renderRecursiveGridModel(Entity entity, Model model, IntModelBatch modelBatch, float alpha, double t, RenderingContext rc, RenderGroup renderGroup, boolean shadow, boolean relativistic) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var gr = Mapper.gridRec.get(entity);

        ModelComponent mc = model.model;

        mc.update(alpha * body.color[3] * base.opacity, relativistic);
        if (gr.regime == 1)
            mc.setDepthTest(GL20.GL_LEQUAL, false);
        else
            mc.setDepthTest(0, false);
        mc.setFloatExtAttribute(FloatAttribute.TessQuality, gr.scalingFading.getFirst().floatValue());
        // Fading in u_heightScale
        mc.setFloatExtAttribute(FloatAttribute.HeightScale, gr.scalingFading.getSecond().floatValue());
        // FovFactor
        mc.setFloatExtAttribute(FloatAttribute.Ts, gr.fovFactor * 0.5f * Settings.settings.scene.lineWidth);
        modelBatch.render(mc.instance, mc.env);
    }

    /**
     * Renders a mesh, typically an iso-surface.
     *
     * @param entity       The entity.
     * @param model        The model component.
     * @param batch        The batch.
     * @param alpha        The alpha value.
     * @param t            The time.
     * @param rc           The rendering context.
     * @param renderGroup  The render group.
     * @param relativistic Whether to apply relativistic effects.
     * @param shadow       Whether to prepare the shadow environment.
     */
    public void renderMeshModel(Entity entity, Model model, IntModelBatch batch, float alpha, double t, RenderingContext rc, RenderGroup renderGroup, boolean relativistic, boolean shadow) {
        if (model.model != null) {
            var graph = Mapper.graph.get(entity);
            var base = Mapper.base.get(entity);
            var mesh = Mapper.mesh.get(entity);

            var mc = model.model;

            if (mesh.shading == Mesh.MeshShading.ADDITIVE) {
                mc.update(relativistic, graph.localTransform, alpha * base.opacity, GL20.GL_ONE, GL20.GL_ONE);
                // Depth reads, no depth writes
                mc.setDepthTest(GL20.GL_LEQUAL, false);
            } else {
                mc.update(relativistic, graph.localTransform, alpha * base.opacity);
                // Depth reads and writes
                mc.setDepthTest(GL20.GL_LEQUAL, true);
            }
            // Render
            if (mc.instance != null)
                batch.render(mc.instance, mc.env);
        }
    }

    /**
     * Renders a star cluster entity as a model.
     *
     * @param entity       The entity.
     * @param model        The model component.
     * @param modelBatch   The model batch.
     * @param alpha        The model alpha.
     * @param t            The time.
     * @param rc           The rendering context.
     * @param renderGroup  The render group.
     * @param relativistic Whether to apply relativistic effects.
     * @param shadow       Whether to prepare the shadow environment.
     */
    public void renderStarClusterModel(Entity entity, Model model, IntModelBatch modelBatch, float alpha, double t, RenderingContext rc, RenderGroup renderGroup, boolean shadow, boolean relativistic) {
        ModelComponent mc = model.model;
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);
        var cluster = Mapper.cluster.get(entity);

        mc.update(relativistic, null, alpha * base.opacity * cluster.fadeAlpha, GL20.GL_ONE, GL20.GL_ONE);
        // Depth reads, no depth writes
        mc.setDepthTest(GL20.GL_LEQUAL, false);
        mc.instance.transform.set(graph.localTransform);
        modelBatch.render(mc.instance, mc.env);
    }

    /**
     * Renders the model of a single star or particle.
     *
     * @param entity       The entity.
     * @param model        The model component.
     * @param batch        The model batch.
     * @param alpha        The model alpha.
     * @param t            The time.
     * @param rc           The rendering context.
     * @param renderGroup  The render group.
     * @param relativistic Whether to apply relativistic effects.
     * @param shadow       Whether to prepare the shadow environment.
     */
    public void renderParticleStarSetModel(Entity entity, Model model, IntModelBatch batch, float alpha, double t, RenderingContext rc, RenderGroup renderGroup, boolean shadow, boolean relativistic) {
        var set = Mapper.starSet.get(entity);

        var mc = model.model;

        if (mc != null && mc.isModelInitialised()) {
            mc.touch();
            float opacity = (float) MathUtilsd.lint(set.proximity.updating[0].distToCamera, set.modelDist / 50f, set.modelDist, 1f, 0f);
            if (alpha * opacity > 0) {
                mc.setTransparency(alpha * opacity);
                float[] col = set.proximity.updating[0].col;
                ((ColorAttribute) mc.env.get(ColorAttribute.AmbientLight)).color.set(col[0], col[1], col[2], 1f);
                ((FloatAttribute) mc.env.get(FloatAttribute.Time)).value = (float) t;
                // Local transform
                double variableScaling = utils.getVariableSizeScaling(set, set.proximity.updating[0].index);
                mc.instance.transform.idt()
                        .translate(
                                (float) set.proximity.updating[0].pos.x,
                                (float) set.proximity.updating[0].pos.y,
                                (float) set.proximity.updating[0].pos.z)
                        .scl((float) (set.getRadius(set.active[0]) * 2d * variableScaling));
                if (relativistic) {
                    mc.updateRelativisticEffects(GaiaSky.instance.getICamera());
                }
                mc.updateVelocityBufferUniforms(GaiaSky.instance.getICamera());
                batch.render(mc.instance, mc.env);
            }
        }
    }

    /**
     * Renders the model of a single star or particle.
     *
     * @param entity       The entity.
     * @param model        The model component.
     * @param batch        The model batch.
     * @param alpha        The model alpha.
     * @param t            The time.
     * @param rc           The rendering context.
     * @param renderGroup  The render group.
     * @param relativistic Whether to apply relativistic effects.
     * @param shadow       Whether to prepare the shadow environment.
     */
    public void renderParticleStarModel(Entity entity, Model model, IntModelBatch batch, float alpha, double t, RenderingContext rc, RenderGroup renderGroup, boolean shadow, boolean relativistic) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var extra = Mapper.extra.get(entity);
        var dist = Mapper.distance.get(entity);

        ModelComponent mc = model.model;
        var cc = body.color;

        float opacity = (float) MathUtilsd.lint(body.distToCamera, dist.distance / 50f, dist.distance, 1f, 0f);
        ((ColorAttribute) mc.env.get(ColorAttribute.AmbientLight)).color.set(cc[0], cc[1], cc[2], 1f);
        ((FloatAttribute) mc.env.get(FloatAttribute.Time)).value = (float) t;
        mc.update(alpha * opacity, relativistic);
        // Local transform
        graph.translation.setToTranslation(mc.instance.transform).scl((float) (extra.radius * 2d));
        batch.render(mc.instance, mc.env);
    }

    /**
     * Renders a spacecraft. Not used right now.
     *
     * @param entity       The entity.
     * @param model        The model component.
     * @param batch        The model batch.
     * @param alpha        The model alpha.
     * @param t            The time.
     * @param rc           The rendering context.
     * @param renderGroup  The render group.
     * @param relativistic Whether to apply relativistic effects.
     * @param shadow       Whether to prepare the shadow environment.
     */
    public void renderSpacecraft(Entity entity, Model model, IntModelBatch batch, float alpha, double t, RenderingContext rc, RenderGroup renderGroup, boolean shadow, boolean relativistic) {
        var scaffolding = Mapper.modelScaffolding.get(entity);
        var graph = Mapper.graph.get(entity);

        ICamera cam = GaiaSky.instance.getICamera();
        ModelComponent mc = model.model;
        if (mc.isModelInitialised()) {
            // Good, render
            if (shadow) {
                prepareShadowEnvironment(entity, model, scaffolding);
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
     * @param entity       The entity.
     * @param model        The model component.
     * @param batch        The batch.
     * @param alpha        The alpha value.
     * @param t            The time, in seconds, since the session start.
     * @param rc           The rendering context.
     * @param renderGroup  The render group.
     * @param relativistic Whether to apply relativistic effects.
     * @param shadow       Whether to prepare the shadow environment.
     */
    public void renderPlanet(Entity entity, Model model, IntModelBatch batch, float alpha, double t, RenderingContext rc, RenderGroup renderGroup, boolean shadow, boolean relativistic) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var scaffolding = Mapper.modelScaffolding.get(entity);
        var atmosphere = Mapper.atmosphere.get(entity);
        var cloud = Mapper.cloud.get(entity);
        if (renderGroup == RenderGroup.MODEL_ATM) {
            // Atmosphere
            renderAtmosphere(entity, body, model, scaffolding, batch, atmosphere, GaiaSky.instance.sgr.alphas[ComponentType.Atmospheres.ordinal()], rc);
        } else if (renderGroup == RenderGroup.MODEL_CLOUD) {
            // Clouds
            renderClouds(entity, base, model, cloud, batch, GaiaSky.instance.sgr.alphas[ComponentType.Clouds.ordinal()], t);
        } else {
            // If atmosphere ground params are present, set them
            if (atmosphere.atmosphere != null) {
                float atmOpacity = (float) MathUtilsd.lint(body.solidAngle, 0.00745329f, 0.02490659f, 0f, 1f);
                if (Settings.settings.scene.visibility.get(ComponentType.Atmospheres.toString()) && atmOpacity > 0) {
                    var graph = Mapper.graph.get(entity);
                    var rotation = Mapper.rotation.get(entity);
                    atmosphere.atmosphere.updateAtmosphericScatteringParams(model.model.instance.materials.first(), alpha * atmOpacity, true, graph, rotation, scaffolding, rc.vrOffset);
                } else {
                    atmosphere.atmosphere.removeAtmosphericScattering(model.model.instance.materials.first());
                }
            }
            // Regular planet, render model normally
            if (shadow) {
                prepareShadowEnvironment(entity, model, scaffolding);
            }
            model.model.update(alpha * base.opacity, relativistic);
            batch.render(model.model.instance, model.model.env);
        }
    }

    /**
     * Renders the atmosphere of a planet.
     */
    public void renderAtmosphere(
            Entity entity,
            Body body,
            Model model,
            ModelScaffolding scaffolding,
            IntModelBatch batch,
            Atmosphere atmosphere,
            float alpha,
            RenderingContext rc) {

        // Atmosphere fades in between 1 and 2 degrees of view angle apparent
        ICamera cam = GaiaSky.instance.getICamera();
        float atmOpacity = (float) MathUtilsd.lint(body.solidAngle, 0.00745329f, 0.02490659f, 0f, 1f);
        if (atmOpacity > 0) {
            var graph = Mapper.graph.get(entity);
            var rotation = Mapper.rotation.get(entity);
            AtmosphereComponent ac = atmosphere.atmosphere;
            ac.updateAtmosphericScatteringParams(ac.mc.instance.materials.first(), alpha * atmOpacity, false, graph, rotation, scaffolding, rc.vrOffset);
            ac.mc.updateRelativisticEffects(cam);
            batch.render(ac.mc.instance, model.model.env);
        }
    }

    /**
     * Renders the cloud layer of a planet.
     */
    public void renderClouds(Entity entity, Base base, Model model, Cloud cloud, IntModelBatch batch, float alpha, double t) {
        CloudComponent clc = cloud.cloud;
        clc.touch();
        ICamera cam = GaiaSky.instance.getICamera();
        clc.mc.updateRelativisticEffects(cam);
        clc.mc.updateVelocityBufferUniforms(cam);
        clc.mc.setTransparency(alpha * base.opacity, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR);
        batch.render(clc.mc.instance, model.model.env);
    }

    /**
     * Prepares the shadow environment for shadow mapping.
     */
    protected void prepareShadowEnvironment(Entity entity, Model model, ModelScaffolding scaffolding) {
        if (Settings.settings.scene.renderer.shadow.active) {
            Environment env = model.model.env;
            SceneRenderer sceneRenderer = GaiaSky.instance.sceneRenderer;
            if (scaffolding.shadow > 0 && sceneRenderer.smTexMap.containsKey(entity)) {
                Matrix4 combined = sceneRenderer.smCombinedMap.get(entity);
                Texture tex = sceneRenderer.smTexMap.get(entity);
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
