/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.pass;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.util.Settings;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.model.gltf.scene3d.lights.DirectionalShadowLight;
import gaiasky.util.gdx.model.gltf.scene3d.scene.CascadeShadowMap;
import gaiasky.util.math.Vector3b;

import java.util.List;

import static gaiasky.render.RenderGroup.MODEL_PIX;
import static gaiasky.render.RenderGroup.MODEL_PIX_TESS;

public class CascadedShadowMapRenderPass extends RenderPass {
    /** Number of cascade buffers. */
    private final static int CASCADE_COUNT = 5;
    /**
     * Describe how to split scene camera frustum. . With a value of 4, far cascade covers the
     * range: 1/4 to 1/1, next cascade, the range 1/16 to 1/4, and so on. The closest one covers
     * the remaining starting from 0. When used with 2 extra cascades (3 areas), split points are: 0.0, 1/16, 1/4, 1.0.
     */
    private final static int SPLIT_DIVISOR = 14;
    /**
     * Shadow box depth factor, depends on the scene. Must be >= 1. Greater than 1 means more objects cast shadows but
     * less precision. A value of 1 restricts shadow box depth to the frustum (only visible objects by the scene
     * camera).
     */
    private final static double LIGHT_DEPTH_FACTOR = 1.1;
    /** Contains the code to render models. **/
    private final ModelEntityRenderSystem modelRenderer;
    private final CascadeShadowMap cascadeShadowMap;
    private DirectionalShadowLight baseLight;
    private final Vector3b auxb = new Vector3b();
    private final Vector3 aux = new Vector3();
    private final Color color = new Color();

    // Are the textures displaying in the UI already?
    private boolean uiViewCreated = false;
    private final int numUiView = 1;

    public CascadedShadowMapRenderPass(final SceneRenderer sceneRenderer) {
        super(sceneRenderer);
        this.modelRenderer = new ModelEntityRenderSystem(sceneRenderer);
        this.cascadeShadowMap = new CascadeShadowMap(CASCADE_COUNT);
    }

    protected void initializeRenderPass() {
        this.baseLight = new DirectionalShadowLight(
                Settings.settings.scene.renderer.shadow.resolution,
                Settings.settings.scene.renderer.shadow.resolution);
    }

    protected void renderPass(ICamera camera, Object... params) {
        var renderAssets = sceneRenderer.getRenderAssets();

        // Get models.
        List<IRenderable> models = sceneRenderer.getRenderLists().get(MODEL_PIX.ordinal());
        List<IRenderable> modelsTess = sceneRenderer.getRenderLists().get(MODEL_PIX_TESS.ordinal());

        // Prepare base light camera: direction and up.
        IFocus l = camera.getCloseLightSource(0);
        l.getAbsolutePosition(auxb);
        auxb.sub(camera.getPos()).nor().put(aux).scl(-1);
        var cameraLight = baseLight.getCamera();
        baseLight.direction.set(aux);
        cameraLight.direction.set(aux);
        // Shadow camera up is perpendicular to dir.
        if (cameraLight.direction.y != 0 || cameraLight.direction.z != 0)
            aux.set(1, 0, 0);
        else
            aux.set(0, 1, 0);
        cameraLight.up.set(cameraLight.direction).crs(aux);

        // Color
        var col = l.getColor();
        color.set(col[0], col[1], col[2], 1);
        baseLight.baseColor.set(color);
        baseLight.color.set(color);
        // Prepare cascading.
        cascadeShadowMap.setCascades(camera, baseLight, LIGHT_DEPTH_FACTOR, SPLIT_DIVISOR);

        for (DirectionalShadowLight light : cascadeShadowMap.lights) {
            light.begin();
            renderDepth(light, camera, renderAssets.mbPixelLightingDepth, models);
            renderDepth(light, camera, renderAssets.mbPixelLightingDepthTessellation, modelsTess);
            light.end();
        }
        if (!uiViewCreated) {
            GaiaSky.postRunnable(() -> {
                int i = 0;
                for (DirectionalShadowLight light : cascadeShadowMap.lights) {
                    // Create UI view(s)
                    EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "CSM " + i, light.getDepthMap().texture, 0.15f);
                    i++;
                    if (i >= numUiView) {
                        break;
                    }
                }
            });
            uiViewCreated = true;
        }
    }

    /**
     * Render only depth (packed 32 bits) with custom camera.
     * Useful to render shadow maps.
     */
    public void renderDepth(DirectionalShadowLight light, ICamera camera, IntModelBatch batch, List<IRenderable> list) {
        batch.begin(light.getCamera());
        for (var r : list) {
            var entity = ((Render) r).entity;
            var model = Mapper.model.get(entity);
            if (model.model.hasPointLight(0)) {
                modelRenderer.render(entity, batch, camera, 1, 0, null, RenderGroup.MODEL_PIX, false);
                model.model.env.set(cascadeShadowMap.attribute);
            }
        }
        batch.end();
    }

    public void disposeCachedFrameBuffers() {
        cascadeShadowMap.dispose();
    }

    public void dispose() {
        disposeCachedFrameBuffers();
    }
}
