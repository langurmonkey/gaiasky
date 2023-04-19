/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.pass;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.BlendMode;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.view.ModelView;
import gaiasky.util.Settings;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static gaiasky.render.RenderGroup.MODEL_PIX;
import static gaiasky.render.RenderGroup.MODEL_PIX_TESS;

public class SVTRenderPass {
    /**
     * The tile detection buffer is smaller than the main window by this factor.
     * Should match the constant with the same name in svt.detection.fragment.glsl
     * and tess.svt.detection.fragment.glsl.
     **/
    public static float SVT_TILE_DETECTION_REDUCTION_FACTOR = (float) Settings.settings.scene.renderer.virtualTextures.detectionBufferFactor;

    /** The scene renderer object. **/
    private final SceneRenderer sceneRenderer;

    /** The model view object. **/
    private final ModelView view;

    private final Array<IRenderable> candidates, candidatesTess, candidatesCloud;

    /** The frame buffer to render the SVT tile detection. Format should be at least RGBAF16. **/
    private FrameBuffer frameBuffer;
    private FloatBuffer pixels;

    private boolean uiViewCreated = true;

    public SVTRenderPass(final SceneRenderer sceneRenderer) {
        this.sceneRenderer = sceneRenderer;
        this.view = new ModelView();
        this.candidates = new Array<>();
        this.candidatesTess = new Array<>();
        this.candidatesCloud = new Array<>();
    }

    public void initialize() {
        // Initialize frame buffer with 16 bits per channel.
        int w = (int) (Gdx.graphics.getWidth() / SVT_TILE_DETECTION_REDUCTION_FACTOR);
        int h = (int) (Gdx.graphics.getHeight() / SVT_TILE_DETECTION_REDUCTION_FACTOR);
        FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(w, h);
        frameBufferBuilder.addFloatAttachment(GL30.GL_RGBA32F, GL30.GL_RGBA, GL30.GL_FLOAT, false);
        frameBufferBuilder.addDepthRenderBuffer(GL20.GL_DEPTH_COMPONENT16);
        frameBuffer = new GaiaSkyFrameBuffer(frameBufferBuilder, 0);

        // Pixels for readout: w * h * 4 (RGBA).
        pixels = BufferUtils.createFloatBuffer(w * h * 4);
    }

    /**
     * Collects the candidate entities in the given render group that have a non-cloud SVT.
     *
     * @param renderGroup The render group.
     * @param candidates  The entities in the given render group with at least one non-cloud SVT.
     */
    private void fetchCandidates(RenderGroup renderGroup, Array<IRenderable> candidates) {
        List<IRenderable> models = sceneRenderer.getRenderLists().get(renderGroup.ordinal());
        candidates.clear();
        // Collect SVT-enabled models.
        models.forEach(e -> {
            view.setEntity(((Render) e).getEntity());
            if (view.hasSVTNoCloud()) {
                candidates.add(e);
            }
        });
    }

    /**
     * Collects the candidate entities in the given render groups that have no non-cloud SVT and
     * a cloud SVT.
     *
     * @param renderGroups The render groups.
     * @param candidates   The candidates with only cloud SVT.
     */
    private void fetchCandidatesCloud(RenderGroup[] renderGroups, Array<IRenderable> candidates) {
        List<IRenderable> models = new ArrayList<>();
        for (var rg : renderGroups) {
            models.addAll(sceneRenderer.getRenderLists().get(rg.ordinal()));
        }
        candidates.clear();
        // Collect SVT-enabled models with only clouds.
        models.forEach(e -> {
            view.setEntity(((Render) e).getEntity());
            if (!view.hasSVTNoCloud() && view.hasSVTCloud()) {
                candidates.add(e);
            }
        });
    }

    private final RenderGroup[] renderGroups = new RenderGroup[] { MODEL_PIX, MODEL_PIX_TESS };

    public void render(ICamera camera) {
        // Costly operation, every 4th frame.
        if (GaiaSky.instance.frames % 4 == 0) {
            fetchCandidates(renderGroups[0], candidates);
            fetchCandidates(renderGroups[1], candidatesTess);
            fetchCandidatesCloud(renderGroups, candidatesCloud);

            var renderAssets = sceneRenderer.getRenderAssets();

            int rendered = 0;

            // Render SVT tile detection to frame buffer.
            if(!candidates.isEmpty() || !candidatesTess.isEmpty() || !candidatesCloud.isEmpty()) {
                frameBuffer.begin();
                Gdx.gl.glEnable(GL30.GL_DEPTH_TEST);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

                // Non-tessellated models.
                if (!candidates.isEmpty() || !candidatesCloud.isEmpty()) {
                    renderAssets.mbPixelLightingSvtDetection.begin(camera.getCamera());
                    for (var candidate : candidates) {
                        pushBlend(candidate);
                        sceneRenderer.renderModel(candidate, renderAssets.mbPixelLightingSvtDetection);
                        popBlend(candidate);
                        rendered++;
                    }
                    // Models with only cloud SVT.
                    for (var candidate : candidatesCloud) {
                        var e = ((Render) candidate).getEntity();
                        var m = Mapper.model.get(e);
                        var c = Mapper.cloud.get(e);
                        if (c.cloud.hasSVT()) {
                            sceneRenderer.getModelRenderSystem().renderClouds(e, Mapper.base.get(e), m, c, renderAssets.mbPixelLightingSvtDetection, 1f, 0);
                            rendered++;
                        }
                    }
                    renderAssets.mbPixelLightingSvtDetection.end();
                }

                // Tessellated models.
                if (!candidatesTess.isEmpty()) {
                    renderAssets.mbPixelLightingSvtDetectionTessellation.begin(camera.getCamera());
                    for (var candidate : candidatesTess) {
                        pushBlend(candidate);
                        sceneRenderer.renderModel(candidate, renderAssets.mbPixelLightingSvtDetectionTessellation);
                        popBlend(candidate);
                        rendered++;
                    }
                    renderAssets.mbPixelLightingSvtDetectionTessellation.end();
                }

                frameBuffer.end();
            }

            if (rendered > 0) {
                // Read out pixels to float buffer.
                frameBuffer.getColorBufferTexture().bind();
                GL30.glGetTexImage(frameBuffer.getColorBufferTexture().glTarget, 0, GL30.GL_RGBA, GL30.GL_FLOAT, pixels);

                // Send message informing a new tile detection buffer is ready.
                EventManager.publish(Event.SVT_TILE_DETECTION_READY, this, pixels);

                if (!uiViewCreated) {
                    GaiaSky.postRunnable(() -> {
                        // Create UI view
                        EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "SVT tile detection", frameBuffer);
                    });
                    uiViewCreated = true;
                }
            }
        }
    }

    BlendMode blendBak = null;

    private void pushBlend(IRenderable candidate) {
        var modelComponent = Mapper.model.get(((Render) candidate).entity);
        var model = modelComponent.model;
        blendBak = model.getBlendMode();
        model.setBlendMode(BlendMode.NONE);
    }

    private void popBlend(IRenderable candidate) {
        var modelComponent = Mapper.model.get(((Render) candidate).entity);
        var model = modelComponent.model;
        model.setBlendMode(blendBak);
    }
}
