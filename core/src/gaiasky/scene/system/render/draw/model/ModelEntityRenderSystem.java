/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw.model;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.GaiaSky;
import gaiasky.render.BlendMode;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.ShadowMapImpl;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.record.AtmosphereComponent;
import gaiasky.scene.record.ModelComponent;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Settings;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.CascadeShadowMapAttribute;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.FloatAttribute;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3b;

import java.util.Objects;

public class ModelEntityRenderSystem {

    private final ParticleUtils utils;
    private final SceneRenderer sceneRenderer;
    private final Vector3b aux3b2 = new Vector3b();

    public ModelEntityRenderSystem(SceneRenderer sr) {
        this.sceneRenderer = sr;
        this.utils = new ParticleUtils();
    }

    /**
     * Renders a single entity as a model. The entity is assumed to have a {@link gaiasky.scene.component.Model}
     * component.
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
    public void render(Entity entity,
                       IntModelBatch batch,
                       ICamera camera,
                       float alpha,
                       double t,
                       RenderingContext rc,
                       RenderGroup renderGroup,
                       boolean shadow) {
        var model = Mapper.model.get(entity);
        if (model != null) {
            if (model.renderConsumer != null) {
                boolean relativistic = !(Mapper.engine.has(entity) && camera.getMode().isSpacecraft());

                // Just run consumer.
                model.renderConsumer.apply(this, entity, model, batch, alpha, t, rc, renderGroup, shadow, relativistic);
            }
        }
    }

    /**
     * Model opaque rendering for the light glow pass.
     *
     * @param entity     The entity.
     * @param modelBatch The model batch.
     * @param alpha      The alpha value.
     * @param shadow     Shadow environment.
     */
    public void renderOpaque(Entity entity,
                             IntModelBatch modelBatch,
                             float alpha,
                             boolean shadow) {
        var scaffolding = Mapper.modelScaffolding.get(entity);
        var model = Mapper.model.get(entity);

        ModelComponent mc = model.model;
        if (mc != null && mc.instance != null && mc.isModelInitialised()) {
            if (scaffolding != null) {
                if (shadow) {
                    prepareShadowEnvironment(model, scaffolding);
                }
                mc.update(alpha * scaffolding.fadeOpacity);
                modelBatch.render(mc.instance, mc.env);
            } else {
                mc.setTransparency(alpha);
                modelBatch.render(mc.instance, mc.env);

            }
        }
    }

    public void renderVRDeviceModel(Entity entity,
                                    Model model,
                                    IntModelBatch batch,
                                    float alpha,
                                    double t,
                                    RenderingContext rc,
                                    RenderGroup renderGroup,
                                    boolean shadow,
                                    boolean relativistic) {
        model.model.setTransparency(alpha);
        batch.render(model.model.instance, model.model.env);

        var vr = Mapper.vr.get(entity);
        var intersectionModel = vr.intersectionModel;
        // Intersection
        if (vr.intersection != null && vr.interacting && vr.hitUI) {
            batch.render(intersectionModel, model.model.env);
        }
    }

    /**
     * Renders a generic model.
     *
     * @param entity       The entity.
     * @param model        The model component.
     * @param batch        The batch.
     * @param alpha        The alpha value.
     * @param t            The time.
     * @param rc           The rendering context.
     * @param renderGroup  The render group.
     * @param shadow       Whether to prepare the shadow environment.
     * @param relativistic Whether to apply relativistic effects.
     */
    public void renderGenericModel(Entity entity,
                                   Model model,
                                   IntModelBatch batch,
                                   float alpha,
                                   double t,
                                   RenderingContext rc,
                                   RenderGroup renderGroup,
                                   boolean shadow,
                                   boolean relativistic) {
        var scaffolding = Mapper.modelScaffolding.get(entity);

        ModelComponent mc = model.model;
        if (mc != null && mc.instance != null && mc.isModelInitialised()) {
            var base = Mapper.base.get(entity);
            var body = Mapper.body.get(entity);
            if (scaffolding != null && shadow) {
                prepareShadowEnvironment(model, scaffolding);
            }

            float alphaFactor;
            if (scaffolding != null) {
                alphaFactor = Mapper.fade.has(entity) ? base.opacity : scaffolding.fadeOpacity * base.opacity;
            } else {
                alphaFactor = base.opacity;
            }

            if (Mapper.grid.has(entity)) {
                // Line width and fov factor.
                ICamera cam = GaiaSky.instance.getICamera();
                mc.setFloatExtAttribute(FloatAttribute.Generic1, Settings.settings.scene.renderer.line.width);
                mc.setFloatExtAttribute(FloatAttribute.Generic2, cam.getFovFactor());
            }


            float colorAlpha = Mapper.tagBillboard.has(entity) || Mapper.tagBillboardGalaxy.has(entity) ? body.color[3] : 1.0f;
            mc.update(alpha * alphaFactor * colorAlpha, relativistic);
            model.model.setSize(body.size);
            batch.render(mc.instance, mc.env);
        }
    }
    /**
     * Renders an aurora.
     *
     * @param entity       The entity.
     * @param model        The model component.
     * @param batch        The batch.
     * @param alpha        The alpha value.
     * @param t            The time.
     * @param rc           The rendering context.
     * @param renderGroup  The render group.
     * @param shadow       Whether to prepare the shadow environment.
     * @param relativistic Whether to apply relativistic effects.
     */
    public void renderAurora(Entity entity,
                                   Model model,
                                   IntModelBatch batch,
                                   float alpha,
                                   double t,
                                   RenderingContext rc,
                                   RenderGroup renderGroup,
                                   boolean shadow,
                                   boolean relativistic) {
        var scaffolding = Mapper.modelScaffolding.get(entity);

        ModelComponent mc = model.model;
        if (mc != null && mc.instance != null && mc.isModelInitialised()) {
            var base = Mapper.base.get(entity);
            var body = Mapper.body.get(entity);

            float alphaFactor;
            if (scaffolding != null) {
                alphaFactor = Mapper.fade.has(entity) ? base.opacity : scaffolding.fadeOpacity * base.opacity;
            } else {
                alphaFactor = base.opacity;
            }

            mc.updateTime(t);
            mc.update(alpha * alphaFactor, relativistic);
            mc.setBlendMode(BlendMode.ADDITIVE);
            mc.updateDepthTest();
            model.model.setSize(body.size);
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
     * @param shadow       Whether to prepare the shadow environment.
     * @param relativistic Whether to apply relativistic effects.
     */
    public void renderShape(Entity entity,
                            Model model,
                            IntModelBatch batch,
                            float alpha,
                            double t,
                            RenderingContext rc,
                            RenderGroup renderGroup,
                            boolean shadow,
                            boolean relativistic) {
        var mc = model.model;
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);

        mc.update(graph.localTransform, alpha * base.opacity * body.color[3]);
        mc.updateDepthTest();
        Gdx.gl20.glLineWidth(1.5f + Settings.settings.scene.renderer.line.glWidthBias);
        batch.render(mc.instance, mc.env);

        model.model.update(alpha * base.opacity, relativistic);
        batch.render(model.model.instance, model.model.env);
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
     * @param shadow       Whether to prepare the shadow environment.
     * @param relativistic Whether to apply relativistic effects.
     */
    public void renderRecursiveGridModel(Entity entity,
                                         Model model,
                                         IntModelBatch modelBatch,
                                         float alpha,
                                         double t,
                                         RenderingContext rc,
                                         RenderGroup renderGroup,
                                         boolean shadow,
                                         boolean relativistic) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var gr = Mapper.gridRec.get(entity);

        ModelComponent mc = model.model;

        mc.update(alpha * body.color[3] * base.opacity, relativistic);
        if (gr.regime == 1) {
            mc.depthTestReadOnly();
        } else {
            mc.depthTestDisable();
        }
        // Distance in u_tessQuality.
        mc.setFloatExtAttribute(FloatAttribute.TessQuality, gr.scalingFading.getFirst().floatValue());
        // Fading in u_heightScale.
        mc.setFloatExtAttribute(FloatAttribute.HeightScale, gr.scalingFading.getSecond().floatValue());
        // Grid style in u_elevationMultiplier.
        mc.setFloatExtAttribute(FloatAttribute.ElevationMultiplier, (float) Settings.settings.program.recursiveGrid.style.ordinal());
        // Line width.
        mc.setFloatExtAttribute(FloatAttribute.Ts, Settings.settings.scene.renderer.line.width);

        // Render.
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
     * @param shadow       Whether to prepare the shadow environment.
     * @param relativistic Whether to apply relativistic effects.
     */
    public void renderMeshModel(Entity entity,
                                Model model,
                                IntModelBatch batch,
                                float alpha,
                                double t,
                                RenderingContext rc,
                                RenderGroup renderGroup,
                                boolean shadow,
                                boolean relativistic) {
        if (model.model != null) {
            var graph = Mapper.graph.get(entity);
            var base = Mapper.base.get(entity);
            var mesh = Mapper.mesh.get(entity);

            var mc = model.model;

            if (mesh.shading == Mesh.MeshShading.ADDITIVE) {
                mc.update(relativistic, graph.localTransform, alpha * base.opacity, GL20.GL_ONE, GL20.GL_ONE, true);
                // Depth reads, no depth writes
                mc.depthTestReadOnly();
            } else {
                mc.update(relativistic, graph.localTransform, alpha * base.opacity);
                // Depth reads and writes
                mc.depthTestReadWrite();
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
     * @param shadow       Whether to prepare the shadow environment.
     * @param relativistic Whether to apply relativistic effects.
     */
    public void renderStarClusterModel(Entity entity,
                                       Model model,
                                       IntModelBatch modelBatch,
                                       float alpha,
                                       double t,
                                       RenderingContext rc,
                                       RenderGroup renderGroup,
                                       boolean shadow,
                                       boolean relativistic) {
        ModelComponent mc = model.model;
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);
        var cluster = Mapper.cluster.get(entity);

        mc.update(relativistic, null, alpha * base.opacity * cluster.fadeAlpha);
        mc.updateDepthTest();
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
     * @param shadow       Whether to prepare the shadow environment.
     * @param relativistic Whether to apply relativistic effects.
     */
    public void renderParticleStarSetModel(Entity entity,
                                           Model model,
                                           IntModelBatch batch,
                                           float alpha,
                                           double t,
                                           RenderingContext rc,
                                           RenderGroup renderGroup,
                                           boolean shadow,
                                           boolean relativistic) {
        var set = Mapper.starSet.get(entity);

        var mc = model.model;

        if (mc != null && mc.isModelInitialised()) {
            mc.touch();
            if (set.proximity.updating[0] != null) {
                float opacity = (float) MathUtilsDouble.lint(set.proximity.updating[0].distToCamera, set.modelDist / 50f, set.modelDist, 1f, 0f);
                if (alpha * opacity > 0) {
                    mc.setTransparency(alpha * opacity);
                    mc.updateDepthTest();
                    float[] col = set.proximity.updating[0].col;
                    ((ColorAttribute) Objects.requireNonNull(mc.env.get(ColorAttribute.AmbientLight))).color.set(col[0], col[1], col[2], 1f);
                    ((FloatAttribute) Objects.requireNonNull(mc.env.get(FloatAttribute.Time))).value = (float) t;
                    // Local transform
                    double variableScaling = utils.getVariableSizeScaling(set, set.proximity.updating[0].index);
                    int idx = set.proximity.updating[0].index;
                    // We need to fetch the position again, for the camera position is different in stereoscopic mode.
                    var pos = set.fetchPosition(set.get(idx), GaiaSky.instance.getICamera().getPos(), aux3b2, set.currDeltaYears);
                    mc.instance.transform.idt().translate((float) pos.x.doubleValue(), (float) pos.y.doubleValue(), (float) pos.z.doubleValue()).scl(
                            (float) (set.getRadius(set.active[0]) * 2d * variableScaling));
                    if (relativistic) {
                        mc.updateRelativisticEffects(GaiaSky.instance.getICamera());
                    }
                    batch.render(mc.instance, mc.env);
                }
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
     * @param shadow       Whether to prepare the shadow environment.
     * @param relativistic Whether to apply relativistic effects.
     */
    public void renderParticleStarModel(Entity entity,
                                        Model model,
                                        IntModelBatch batch,
                                        float alpha,
                                        double t,
                                        RenderingContext rc,
                                        RenderGroup renderGroup,
                                        boolean shadow,
                                        boolean relativistic) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var extra = Mapper.extra.get(entity);
        var dist = Mapper.distance.get(entity);
        var scaffolding = Mapper.modelScaffolding.get(entity);

        ModelComponent mc = model.model;
        var cc = body.color;

        double thresholdDistance = dist.distance * scaffolding.sizeScaleFactor;
        float opacity = (float) MathUtilsDouble.flint(body.distToCamera, thresholdDistance / 50f, thresholdDistance, 1f, 0f);
        ((ColorAttribute) Objects.requireNonNull(mc.env.get(ColorAttribute.AmbientLight))).color.set(cc[0], cc[1], cc[2], 1f);
        ((FloatAttribute) Objects.requireNonNull(mc.env.get(FloatAttribute.Time))).value = (float) t;
        mc.updateDepthTest();
        mc.update(alpha * opacity, relativistic);
        // Local transform
        graph.translation.setToTranslation(mc.instance.transform).scl((float) (extra.radius * 2d) * scaffolding.sizeScaleFactor);
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
     * @param shadow       Whether to prepare the shadow environment.
     * @param relativistic Whether to apply relativistic effects.
     */
    public void renderSpacecraft(Entity entity,
                                 Model model,
                                 IntModelBatch batch,
                                 float alpha,
                                 double t,
                                 RenderingContext rc,
                                 RenderGroup renderGroup,
                                 boolean shadow,
                                 boolean relativistic) {
        var scaffolding = Mapper.modelScaffolding.get(entity);
        var graph = Mapper.graph.get(entity);

        ICamera cam = GaiaSky.instance.getICamera();
        ModelComponent mc = model.model;
        if (mc.isModelInitialised()) {
            // Good, render
            if (shadow) {
                prepareShadowEnvironment(model, scaffolding);
            }
            mc.setTransparency(alpha * scaffolding.fadeOpacity);
            if (cam.getMode().isSpacecraft())
                // In SPACECRAFT_MODE mode, we are not affected by relativistic aberration or Doppler shift
                mc.updateRelativisticEffects(cam, 0);
            else
                mc.updateRelativisticEffects(cam);
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
     * @param shadow       Whether to prepare the shadow environment.
     * @param relativistic Whether to apply relativistic effects.
     */
    public void renderPlanet(Entity entity,
                             Model model,
                             IntModelBatch batch,
                             float alpha,
                             double t,
                             RenderingContext rc,
                             RenderGroup renderGroup,
                             boolean shadow,
                             boolean relativistic) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var scaffolding = Mapper.modelScaffolding.get(entity);
        var atmosphere = Mapper.atmosphere.get(entity);
        var cloud = Mapper.cloud.get(entity);
        if (renderGroup == RenderGroup.MODEL_ATM) {
            // Atmosphere
            renderAtmosphere(entity, body, model, scaffolding, batch, atmosphere, sceneRenderer.alphas[ComponentType.Atmospheres.ordinal()], rc);
        } else if (renderGroup == RenderGroup.MODEL_CLOUD) {
            // Clouds
            renderClouds(entity, base, model, cloud, batch, sceneRenderer.alphas[ComponentType.Clouds.ordinal()], t);
        } else {
            // If atmosphere ground params are present, set them
            if (atmosphere.atmosphere != null) {
                float atmOpacity = (float) MathUtilsDouble.flint(body.solidAngle, 0.00745329f, 0.02490659f, 0f, 1f);
                if (Settings.settings.scene.visibility.get(ComponentType.Atmospheres.toString()) && atmOpacity > 0 && rc != null) {
                    var graph = Mapper.graph.get(entity);
                    var rotation = Mapper.orientation.get(entity).rotationComponent;
                    atmosphere.atmosphere.updateAtmosphericScatteringParams(model.model.instance.materials.first(), alpha * atmOpacity, true, graph,
                            rotation, scaffolding, rc.vrOffset);
                } else {
                    atmosphere.atmosphere.removeAtmosphericScattering(model.model.instance.materials.first());
                }
            }
            // Regular planet, render model normally
            if (shadow) {
                prepareShadowEnvironment(model, scaffolding);
            }
            model.model.updateEclipsingBodyUniforms(entity);
            model.model.update(alpha * base.opacity, relativistic);
            model.model.setSize(body.size);
            batch.render(model.model.instance, model.model.env);
        }
    }

    /**
     * Renders the atmosphere of a planet.
     */
    public void renderAtmosphere(Entity entity,
                                 Body body,
                                 Model model,
                                 ModelScaffolding scaffolding,
                                 IntModelBatch batch,
                                 Atmosphere atmosphere,
                                 float alpha,
                                 RenderingContext rc) {

        // Atmosphere fades in between 1 and 2 degrees of view angle apparent
        ICamera cam = GaiaSky.instance.getICamera();
        float atmOpacity = (float) MathUtilsDouble.flint(body.solidAngle, 0.00745329f, 0.02490659f, 0f, 1f);
        if (atmOpacity > 0) {
            var graph = Mapper.graph.get(entity);
            var orientation = Mapper.orientation.get(entity);
            AtmosphereComponent ac = atmosphere.atmosphere;
            ac.updateAtmosphericScatteringParams(ac.mc.instance.materials.first(), alpha * atmOpacity, false, graph, orientation.rotationComponent, scaffolding,
                    rc.vrOffset);
            ac.mc.updateRelativisticEffects(cam);
            ac.mc.updateEclipsingBodyUniforms(entity);
            batch.render(ac.mc.instance, model.model.env);
        }
    }

    /**
     * Renders the cloud layer of a planet.
     */
    public void renderClouds(Entity entity,
                             Base base,
                             Model model,
                             Cloud cloud,
                             IntModelBatch batch,
                             float alpha,
                             double t) {
        // Update cull face depending on distance.
        cloud.cloud.updateCullFace(Mapper.body.get(entity).distToCamera);
        cloud.cloud.touch(model);
        ICamera cam = GaiaSky.instance.getICamera();
        cloud.cloud.mc.updateRelativisticEffects(cam);
        cloud.cloud.mc.setTransparency(alpha * base.opacity);
        cloud.cloud.mc.updateEclipsingBodyUniforms(entity);
        batch.render(cloud.cloud.mc.instance, model.model.env);
    }

    /**
     * Prepares the shadow environment for shadow mapping.
     */
    protected void prepareShadowEnvironment(Model model,
                                            ModelScaffolding scaffolding) {
        if (!model.model.env.has(CascadeShadowMapAttribute.Type)) {
            var env = model.model.env;
            // Only for regular shadow maps (no CSM!).
            if (Settings.settings.scene.renderer.shadow.active && scaffolding.isSelfShadow()) {
                if (scaffolding.shadow > 0
                        && scaffolding.shadowMapFb != null
                        && scaffolding.shadowMapCombined != null) {
                    boolean global = scaffolding.shadowMapFbGlobal != null;
                    Matrix4 combined = scaffolding.shadowMapCombined;
                    Texture tex = scaffolding.shadowMapFb.getColorBufferTexture();
                    Matrix4 combinedGlobal = global ? scaffolding.shadowMapCombinedGlobal : null;
                    Texture texGlobal = global ? scaffolding.shadowMapFbGlobal.getColorBufferTexture() : null;

                    // Gather info.
                    if (scaffolding.shadowMap == null) {
                        scaffolding.shadowMap = new ShadowMapImpl(
                                combined,
                                tex,
                                combinedGlobal,
                                texGlobal);
                    } else {
                        scaffolding.shadowMap.setProjViewTrans(combined);
                        scaffolding.shadowMap.setDepthMap(tex);
                        if (global) {
                            scaffolding.shadowMap.setProjViewTransGlobal(combinedGlobal);
                            scaffolding.shadowMap.setDepthMapGlobal(texGlobal);
                        }
                    }

                    // Set to environment.
                    env.shadowMap = scaffolding.shadowMap;

                    scaffolding.shadow--;
                } else {
                    env.shadowMap = null;
                }
            } else {
                model.model.env.shadowMap = null;
            }
        }
    }
}
