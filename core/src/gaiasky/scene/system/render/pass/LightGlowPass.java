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
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.api.IRenderable;
import gaiasky.render.process.RenderModeOpenXR;
import gaiasky.render.system.LightPositionUpdater;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.system.render.draw.BillboardRenderer;
import gaiasky.util.Settings;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

import static gaiasky.render.RenderGroup.*;

public class LightGlowPass {

    private final SceneRenderer sceneRenderer;
    // Light glow pre-render
    private FrameBuffer occlusionFrameBuffer;
    private final List<IRenderable> stars;
    private final LightPositionUpdater lpu;
    private final Array<Entity> controllers = new Array<>();
    private BillboardRenderer billboardStarsRenderer = null;
    private boolean uiViewCreated = false;

    public LightGlowPass(final SceneRenderer sceneRenderer) {
        this.sceneRenderer = sceneRenderer;
        this.stars = new ArrayList<>();
        this.lpu = new LightPositionUpdater();
    }

    public void buildLightGlowData() {
        if (occlusionFrameBuffer == null) {
            GLFrameBuffer.FrameBufferBuilder fbb = new GLFrameBuffer.FrameBufferBuilder(1920, 1080);
            if (!Settings.settings.program.safeMode && Gdx.graphics.isGL30Available()) {
                // Float color and depth buffers.
                fbb.addFloatAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT, true);
                fbb.addDepthTextureAttachment(GL30.GL_DEPTH_COMPONENT24, GL30.GL_FLOAT);
            } else {
                // Regular buffers.
                fbb.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);
                fbb.addBasicDepthRenderBuffer();
            }
            occlusionFrameBuffer = new GaiaSkyFrameBuffer(fbb, 0, 1);
        }
    }

    public void renderGlowPass(ICamera camera,
                               FrameBuffer frameBuffer) {
        if (frameBuffer == null) {
            frameBuffer = occlusionFrameBuffer;
        }
        if (Settings.settings.postprocess.lightGlow.active && frameBuffer != null) {
            var renderLists = sceneRenderer.getRenderLists();
            var renderAssets = sceneRenderer.getRenderAssets();
            // Get all billboard stars.
            List<IRenderable> billboardStars = renderLists.get(BILLBOARD_STAR.ordinal());

            stars.clear();
            for (IRenderable st : billboardStars) {
                if (st instanceof Render) {
                    if (Mapper.hip.has(((Render) st).getEntity())) {
                        stars.add(st);
                    }
                }
            }

            // Get all models.
            List<IRenderable> models = renderLists.get(MODEL_PIX.ordinal());
            List<IRenderable> modelsTess = renderLists.get(MODEL_PIX_TESS.ordinal());

            // VR controllers.
            if (Settings.settings.runtime.openXr) {
                RenderModeOpenXR sgrVR = sceneRenderer.getRenderModeOpenXR();
                if (sceneRenderer.getVrContext() != null) {
                    for (Entity m : sgrVR.controllerObjects) {
                        var render = Mapper.render.get(m);
                        if (!models.contains(render))
                            controllers.add(m);
                    }
                }
            }


            if (billboardStarsRenderer == null) {
                billboardStarsRenderer = (BillboardRenderer) sceneRenderer.getOrInitializeRenderSystem(BILLBOARD_STAR);
            }

            frameBuffer.begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            // Render models.
            renderAssets.mbPixelLightingOpaque.begin(camera.getCamera());
            for (IRenderable model : models) {
                sceneRenderer.renderModel(model, renderAssets.mbPixelLightingOpaque);
            }
            renderAssets.mbPixelLightingOpaque.end();

            // Render tessellated models.
            if (!modelsTess.isEmpty()) {
                renderAssets.mbPixelLightingOpaqueTessellation.begin(camera.getCamera());
                for (IRenderable model : modelsTess) {
                    sceneRenderer.renderModel(model, renderAssets.mbPixelLightingOpaqueTessellation);
                }
                renderAssets.mbPixelLightingOpaqueTessellation.end();
            }

            // Render billboard stars.
            if (billboardStarsRenderer != null) {
                billboardStarsRenderer.renderStud(stars, camera, 0);
            }

            // Set texture to updater.
            lpu.setOcclusionTexture(frameBuffer.getColorBufferTexture());

            frameBuffer.end();

            //if (!uiViewCreated) {
            //    GaiaSky.postRunnable(() -> {
            //        // Create UI view
            //        EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "SVT tile detection", glowFrameBuffer);
            //    });
            //    uiViewCreated = true;
            //}
        }

    }

    public FrameBuffer getOcclusionFrameBuffer() {
        return occlusionFrameBuffer;
    }

    public LightPositionUpdater getLpu() {
        return lpu;
    }
}
