/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.pass;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.math.BoundingBoxDouble;
import gaiasky.util.math.IntersectorDouble;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

import java.util.*;

import static gaiasky.render.RenderGroup.MODEL_PIX;

public class ShadowMapRenderPass extends RenderPass {
    /** Number of *shadow casting* lights supported. */
    private final static int NUM_SHADOW_CASTING_LIGHTS = 1;
    private final static boolean GLOBAL_SM = false;
    /** Contains the code to render models. **/
    private final ModelEntityRenderSystem modelRenderer;
    /** Individual camera light. **/
    private PerspectiveCamera cameraLightIndividual;
    /** Global camera light. **/
    private PerspectiveCamera cameraLightGlobal;
    /** Contains the candidates for regular and tessellated shadow maps. **/
    private List<Entity> shadowCandidates/*, shadowCandidatesTess */;
    /**
     * Cache with all the entities that have used shadow map frame buffers to date, for disposing.
     */
    private Set<Entity> shadowMapEntities;
    private final BoundingBoxDouble box = new BoundingBoxDouble();
    private FrameBuffer globalFrameBuffer;

    // Display the depth textures in UI windows, for debugging.
    private static boolean DEBUG_UI_VIEW_GLOBAL = false;
    private static final boolean DEBUG_UI_VIEW_LOCAL = false;
    private static final IntSet DEBUG_UI_VIEW_LOCAL_SET = new IntSet();

    private Vector3 aux1;
    private Vector3d aux1d, aux2d, aux3d;
    private Vector3Q aux1b, aux2b;

    public ShadowMapRenderPass(final SceneRenderer sceneRenderer) {
        super(sceneRenderer);
        this.modelRenderer = new ModelEntityRenderSystem(sceneRenderer);
    }

    protected void initializeRenderPass() {
        // Shadow map camera
        cameraLightIndividual = new PerspectiveCamera(0.5f, Settings.settings.scene.renderer.shadow.resolution, Settings.settings.scene.renderer.shadow.resolution);
        cameraLightGlobal = new PerspectiveCamera(0.5f, Settings.settings.scene.renderer.shadow.resolution, Settings.settings.scene.renderer.shadow.resolution);
        shadowMapEntities = new HashSet<>();

        // Aux vectors
        aux1 = new Vector3();
        aux1d = new Vector3d();
        aux2d = new Vector3d();
        aux3d = new Vector3d();
        aux1b = new Vector3Q();
        aux2b = new Vector3Q();

        // Build frame buffers and arrays
        buildShadowMapData();
    }

    /**
     * Builds the shadow map data; frame buffers, arrays, etc.
     */
    public void buildShadowMapData() {
        // Dispose current.
        shadowMapEntities.forEach(entity -> {
            var scaffolding = Mapper.modelScaffolding.get(entity);
            if (scaffolding != null && scaffolding.shadowMapFb != null) {
                scaffolding.shadowMapFb.dispose();
                scaffolding.shadowMapFb = null;
            }
        });
        shadowMapEntities.clear();

        if (shadowCandidates == null) {
            shadowCandidates = new ArrayList<>(Settings.settings.scene.renderer.shadow.number);
            //shadowCandidatesTess = new ArrayList<>(Settings.settings.scene.renderer.shadow.number);
        }
        shadowCandidates.clear();
        //shadowCandidatesTess.clear();
    }

    private FrameBuffer newShadowMapFrameBuffer(Entity entity) {
        // Add to cache for disposing.
        if (entity != null) {
            shadowMapEntities.add(entity);
        }
        // Create frame buffer.
        return new FrameBuffer(Pixmap.Format.RGBA8888,
                Settings.settings.scene.renderer.shadow.resolution,
                Settings.settings.scene.renderer.shadow.resolution,
                true);
    }

    int prevCandidates = 0;

    private void renderShadowMapGlobal(List<IRenderable> candidates,
                                       ICamera camera) {
        if (!candidates.isEmpty()) {
            if (prevCandidates != candidates.size()) {
                System.out.println("Candidates: " + candidates.size());
                prevCandidates = candidates.size();
            }
            var renderAssets = sceneRenderer.getRenderAssets();
            var first = ((Render) candidates.get(0)).entity;
            var model = Mapper.model.get(first);

            // Find bounding box in world space.
            box.inf();
            double greatestSpan = 0.0;
            for (var render : candidates) {
                var entity = ((Render) render).entity;
                var entityAbsPos = EntityUtils.getAbsolutePosition(entity, aux1b);
                box.ext(entityAbsPos.put(aux1d));

                // Find the greatest radius amongst all objects.
                double entitySpan = EntityUtils.getModelSpan(entity);
                if (greatestSpan < entitySpan) {
                    greatestSpan = entitySpan;
                }
            }

            // Position, factor of radius
            Vector3d boxCenterAbsPos = box.getCenter(aux1d);
            // Light direction depends on light.
            Vector3 lightDir = aux1;
            if (model.model.hasDirLight(0)) {
                lightDir.set(model.model.dirLight(0).direction);
            } else if (model.model.hasPointLight(0)) {
                lightDir.set(model.model.pointLight(0).position);
                aux2b.set(boxCenterAbsPos).sub(lightDir).nor().put(lightDir);
            }
            // Find distance given fov and box side.
            var boxDimension = candidates.size() == 1 ? box.getGreatestDim() + greatestSpan : box.getGreatestDim();
            var distCamCenter = (boxDimension / FastMath.tan(FastMath.toRadians(cameraLightGlobal.fieldOfView))) * 1.2;

            // Direction is that of the light.
            cameraLightGlobal.direction.set(lightDir);
            // Position is from the box center, a distance dist in the opposite light direction.
            var camLightAbsPos = aux3d.set(lightDir).scl(-distCamCenter).add(boxCenterAbsPos);
            aux2d.set(camLightAbsPos).sub(camera.getPos()).put(cameraLightGlobal.position);
            // Find out distance to closest object.
            double minDist = Double.MAX_VALUE;
            double maxDist = 0;
            for (var render : candidates) {
                var entity = ((Render) render).entity;
                var entityAbsPos = EntityUtils.getAbsolutePosition(entity, aux1b);
                var r = EntityUtils.getRadius(entity) * 2;
                var d = camLightAbsPos.dst(entityAbsPos);
                var dMin = d - r;
                var dMax = d + r;
                if (minDist > dMin) {
                    minDist = dMin;
                }
                if (maxDist < dMax) {
                    maxDist = dMax;
                }
            }

            // Up is perpendicular to dir
            if (cameraLightGlobal.direction.y != 0 || cameraLightGlobal.direction.z != 0)
                aux1.set(1, 0, 0);
            else
                aux1.set(0, 1, 0);
            cameraLightGlobal.up.set(cameraLightGlobal.direction).crs(aux1);

            // Near and far use the box width and the greatest radius.
            //var near = distCamCenter - boxDimension;
            //var far = distCamCenter + boxDimension;
            cameraLightGlobal.near = (float) FastMath.max(100.0 * Constants.M_TO_U, minDist);
            cameraLightGlobal.far = (float) FastMath.min(Constants.AU_TO_U, maxDist);

            // Update cam
            cameraLightGlobal.update(false);

            // Render model depth map to global frame buffer.
            final var fb = globalFrameBuffer == null ? newShadowMapFrameBuffer(null) : globalFrameBuffer;

            fb.begin();
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            // No tessellation
            renderAssets.mbPixelLightingDepth.begin(cameraLightGlobal);
            for (var entity : candidates) {
                modelRenderer.render(((Render) entity).entity, renderAssets.mbPixelLightingDepth, camera, 1, 0, null, RenderGroup.MODEL_PIX, false);
            }
            renderAssets.mbPixelLightingDepth.end();

            fb.end();

            globalFrameBuffer = fb;

            if (!DEBUG_UI_VIEW_GLOBAL) {
                GaiaSky.postRunnable(() -> {
                    // Create UI view
                    EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Shadow map (GLOBAL)", globalFrameBuffer.getColorBufferTexture(), 0.2f);
                });
                DEBUG_UI_VIEW_GLOBAL = false;
            }
        }
    }

    private void renderShadowMapCandidates(List<Entity> candidates,
                                           int shadowNRender,
                                           ICamera camera) {
        var renderAssets = sceneRenderer.getRenderAssets();
        int nShadows = FastMath.min(candidates.size(), Settings.settings.scene.renderer.shadow.number);
        for (int i = 0; i < nShadows; i++) {
            var candidate = candidates.get(i);
            var model = Mapper.model.get(candidate);
            var scaffolding = Mapper.modelScaffolding.get(candidate);

            double entitySpan = EntityUtils.getModelSpan(candidate);
            var distCamCenter = (entitySpan * 2.0 / FastMath.tan(FastMath.toRadians(cameraLightIndividual.fieldOfView)));
            // Position, factor of radius
            Vector3Q objPos = EntityUtils.getAbsolutePosition(candidate, aux1b);
            for (int j = 0; j < NUM_SHADOW_CASTING_LIGHTS; j++) {
                // Light direction depends on light.
                Vector3 lightDir = aux1;
                if (model.model.hasDirLight(j)) {
                    lightDir.set(model.model.dirLight(j).direction);
                } else if (model.model.hasPointLight(j)) {
                    lightDir.set(model.model.pointLight(j).position);
                    aux2b.set(objPos).sub(lightDir).nor().put(lightDir);
                }
                // Direction is that of the light
                cameraLightIndividual.direction.set(lightDir);
                objPos.sub(camera.getPos()).sub(lightDir.nor().scl((float) distCamCenter));
                objPos.put(cameraLightIndividual.position);
                // Up is perpendicular to dir
                if (cameraLightIndividual.direction.y != 0 || cameraLightIndividual.direction.z != 0)
                    aux1.set(1, 0, 0);
                else
                    aux1.set(0, 1, 0);
                cameraLightIndividual.up.set(cameraLightIndividual.direction).crs(aux1);

                // Near is sv[1]*radius before the object
                cameraLightIndividual.near = (float) (distCamCenter - entitySpan);
                // Far is sv[2]*radius after the object
                cameraLightIndividual.far = (float) (distCamCenter + entitySpan);

                // Update cam
                cameraLightIndividual.update(false);

                // Render model depth map to frame buffer.
                final var fb = scaffolding.shadowMapFb == null ? newShadowMapFrameBuffer(candidate) : scaffolding.shadowMapFb;

                scaffolding.shadowMapFb = fb;
                if (GLOBAL_SM) {
                    scaffolding.shadowMapFbGlobal = globalFrameBuffer;
                }
                if (scaffolding.shadowMapCombined == null) {
                    scaffolding.shadowMapCombined = new Matrix4();
                }

                fb.begin();
                Gdx.gl.glClearColor(0, 0, 0, 1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

                // No tessellation
                renderAssets.mbPixelLightingDepth.begin(cameraLightIndividual);
                modelRenderer.render(candidate, renderAssets.mbPixelLightingDepth, camera, 1, 0, null, RenderGroup.MODEL_PIX, false);
                renderAssets.mbPixelLightingDepth.end();

                // Save frame buffer and combined matrix
                scaffolding.shadow = shadowNRender;
                scaffolding.shadowMapCombined.set(cameraLightIndividual.combined);
                if (GLOBAL_SM) {
                    scaffolding.shadowMapCombinedGlobal = cameraLightGlobal.combined;
                }
                fb.end();

                var base = Mapper.base.get(candidate);
                var hash = base.getName().hashCode();
                if (DEBUG_UI_VIEW_LOCAL && !DEBUG_UI_VIEW_LOCAL_SET.contains(hash)) {
                    GaiaSky.postRunnable(() -> {
                        // Create UI view
                        EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Shadow map (LOCAL): " + base.getName(), fb.getColorBufferTexture(), 0.2f);
                    });
                    DEBUG_UI_VIEW_LOCAL_SET.add(hash);
                }
            }
        }
    }

    private void addCandidates(List<IRenderable> models,
                               List<Entity> candidates) {
        if (candidates != null) {
            candidates.clear();
            int num = 0;
            for (IRenderable model : models) {
                Render render = (Render) model;
                var scaffolding = Mapper.modelScaffolding.get(render.entity);
                if (scaffolding != null) {
                    if (scaffolding.isSelfShadow()) {
                        candidates.add(num, render.entity);
                        scaffolding.shadow = 0;
                        num++;
                        if (num == Settings.settings.scene.renderer.shadow.number)
                            break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private void renderShadowMapCandidatesTess(Array<Entity> candidates,
                                               int shadowNRender,
                                               ICamera camera,
                                               RenderingContext rc) {
        int i = 0;
        int j = 0;
        var renderAssets = sceneRenderer.getRenderAssets();
        // Normal bodies
        for (Entity candidate : candidates) {
            var body = Mapper.body.get(candidate);
            var model = Mapper.model.get(candidate);
            var scaffolding = Mapper.modelScaffolding.get(candidate);

            double radius = (body.size / 2.0) * scaffolding.sizeScaleFactor;
            // Only render when camera very close to surface
            if (body.distToCamera < radius * 1.1) {
                scaffolding.shadow = shadowNRender;

                Vector3 shadowCameraDir = aux1.set(model.model.dirLight(0).direction);

                // Shadow camera direction is that of the light
                cameraLightIndividual.direction.set(shadowCameraDir);

                Vector3 shadowCamDir = aux1.set(model.model.dirLight(0).direction);
                // Direction is that of the light
                cameraLightIndividual.direction.set(shadowCamDir);

                double entitySpan = EntityUtils.getModelSpan(candidate);
                var distCamCenter = (entitySpan * 2.0 / FastMath.tan(FastMath.toRadians(cameraLightIndividual.fieldOfView)));
                // Position, factor of radius
                Vector3Q objPos = EntityUtils.getAbsolutePosition(candidate, aux1b);
                Vector3Q camPos = camera.getPos();
                Vector3d camDir = aux3d.set(camera.getDirection()).nor().scl(100 * Constants.KM_TO_U);
                boolean intersect = IntersectorDouble.checkIntersectSegmentSphere(camPos.tov3d(), aux3d.set(camPos).add(camDir), objPos.put(aux1d), radius);
                if (intersect) {
                    // Use height
                    camDir.nor().scl(body.distToCamera - radius);
                }
                Vector3d objCam = aux2d.set(camPos).sub(objPos).nor().scl(-(body.distToCamera - radius)).add(camDir);

                objCam.add(shadowCamDir.nor().scl((float) -distCamCenter));
                objCam.put(cameraLightIndividual.position);

                // Shadow camera up is perpendicular to dir
                if (cameraLightIndividual.direction.y != 0 || cameraLightIndividual.direction.z != 0)
                    aux1.set(1, 0, 0);
                else
                    aux1.set(0, 1, 0);
                cameraLightIndividual.up.set(cameraLightIndividual.direction).crs(aux1);

                // Near is sv[1]*radius before the object
                cameraLightIndividual.near = (float) (distCamCenter - entitySpan);
                // Far is sv[2]*radius after the object
                cameraLightIndividual.far = (float) (distCamCenter + entitySpan);

                // Update cam
                cameraLightIndividual.update(false);

                // Render model depth map to frame buffer
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

                // Tessellation
                renderAssets.mbPixelLightingDepthTessellation.begin(cameraLightIndividual);
                modelRenderer.render(candidate, renderAssets.mbPixelLightingDepthTessellation, camera, 1, 0, rc, RenderGroup.MODEL_PIX, true);
                renderAssets.mbPixelLightingDepthTessellation.end();

                // Save frame buffer and combined matrix
                scaffolding.shadow = shadowNRender;
                i++;
            } else {
                scaffolding.shadow = -1;
            }
        }
    }

    protected void renderPass(ICamera camera, Object... params) {
        /*
         * Shadow mapping here?
         * <ul>
         * <li>Extract model bodies (front)</li>
         * <li>Work out light direction</li>
         * <li>Set orthographic camera at set distance from bodies,
         * direction of light, clip planes</li>
         * <li>Render depth map to frame buffer (fb)</li>
         * <li>Send frame buffer texture in to ModelBatchRenderSystem along
         * with light position, direction, clip planes and light camera
         * combined matrix</li>
         * <li>Compare real distance from light to texture sample, render
         * shadow if different</li>
         * </ul>
         */
        List<IRenderable> models = sceneRenderer.getRenderLists().get(MODEL_PIX.ordinal());
        //List<IRenderable> modelsTess = renderLists.get(MODEL_PIX_TESS.ordinal());
        models.sort(Comparator.comparingDouble(IRenderable::getDistToCamera));

        // Global shadow map.
        if (GLOBAL_SM) {
            renderShadowMapGlobal(models, camera);
        }

        final int shadowNRender = (Settings.settings.program.modeStereo.active || Settings.settings.runtime.openXr) ?
                2 :
                Settings.settings.program.modeCubemap.active ? 6 : 1;

        // Shadow candidates.
        addCandidates(models, shadowCandidates);
        //addCandidates(modelsTess, shadowCandidatesTess);
        renderShadowMapCandidates(shadowCandidates, shadowNRender, camera);
        //renderShadowMapCandidatesTess(shadowCandidatesTess, shadowNRender, camera);
    }

    public void disposeCachedFrameBuffers() {
        if (shadowMapEntities != null) {
            // Dispose current.
            shadowMapEntities.forEach(entity -> {
                var scaffolding = Mapper.modelScaffolding.get(entity);
                if (scaffolding != null && scaffolding.shadowMapFb != null) {
                    scaffolding.shadowMapFb.dispose();
                    scaffolding.shadowMapFb = null;
                }
            });
            shadowMapEntities.clear();
        }
    }

    public void dispose() {
        disposeCachedFrameBuffers();
    }
}
