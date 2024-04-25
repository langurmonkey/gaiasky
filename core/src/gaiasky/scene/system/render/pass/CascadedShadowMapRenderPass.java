/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.pass;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
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
import gaiasky.util.math.IntersectorDouble;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.util.List;

import static gaiasky.render.RenderGroup.MODEL_PIX;
import static gaiasky.render.RenderGroup.MODEL_PIX_TESS;

public class CascadedShadowMapRenderPass implements Disposable {
    /** Number of *shadow casting* lights supported. */
    private final static int NUM_LEVELS = 4;
    private final Array<FrameBuffer> frameBuffers;
    /** The scene renderer object. **/
    private final SceneRenderer sceneRenderer;
    /** Contains the code to render models. **/
    private final ModelEntityRenderSystem modelRenderer;
    // Camera at light position, with same direction. For shadow mapping
    private Camera cameraLight, cameraLight2;

    // Are the textures displaying in the UI already?
    private boolean uiViewCreated = true;

    private Array<Vector3d> frustumCorners;
    private Matrix4d maux;
    private Vector3 aux1;
    private Vector3d aux1d, aux2d, aux3d;
    private Vector3b aux1b, aux2b;

    public CascadedShadowMapRenderPass(final SceneRenderer sceneRenderer) {
        this.sceneRenderer = sceneRenderer;
        this.modelRenderer = new ModelEntityRenderSystem(sceneRenderer);
        this.frameBuffers = new Array<>(NUM_LEVELS);
    }

    public void initialize() {
        // Shadow map camera.
        cameraLight = new PerspectiveCamera(0.5f, Settings.settings.scene.renderer.shadow.resolution, Settings.settings.scene.renderer.shadow.resolution);
        cameraLight2 = new OrthographicCamera(Settings.settings.scene.renderer.shadow.resolution, Settings.settings.scene.renderer.shadow.resolution);

        // 4 frustum corners.
        frustumCorners = new Array<>();
        frustumCorners.add(new Vector3d());
        frustumCorners.add(new Vector3d());
        frustumCorners.add(new Vector3d());
        frustumCorners.add(new Vector3d());

        // Aux matrix.
        maux = new Matrix4d();

        // Aux vectors.
        aux1 = new Vector3();
        for (int i = 0; i < 8; i++)
            aux1d = new Vector3d();

        // Build frame buffers and arrays
        buildShadowMapData();
    }

    /**
     * Builds the shadow map data.
     */
    public void buildShadowMapData() {
        for (int i = 0; i < NUM_LEVELS; i++) {
            frameBuffers.add(newShadowMapFrameBuffer());
        }
    }

    private FrameBuffer newShadowMapFrameBuffer() {
        // Create frame buffer.
        return new FrameBuffer(Pixmap.Format.RGBA8888,
                Settings.settings.scene.renderer.shadow.resolution,
                Settings.settings.scene.renderer.shadow.resolution,
                true);
    }

    private final double[] values = new double[]{0, 0, 0};

    public void render(ICamera camera) {
        /*
         * Cascaded shadow mapping:
         * <ul>
         * <li>Partition view frustum into NUM_LEVELS subfrusta.</li>
         * <li>Gather all models.</li>
         * <li>Compute orthographic projection for each subfrustum.</li>
         * <li>Render shadow map for each subfrustum.</li>
         * </ul>
         */
        List<IRenderable> models = sceneRenderer.getRenderLists().get(MODEL_PIX.ordinal());
        List<IRenderable> modelsTess = sceneRenderer.getRenderLists().get(MODEL_PIX_TESS.ordinal());

        // Frustum. Camera position is (0 0 0) always.
        // Get frustum corners in world space (assuming camera is at 0).
        maux.set(camera.getCamera().combined).inv();
        int i = 0;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    values[0] = 2.0 * x - 1.0;
                    values[1] = 2.0 * y - 1.0;
                    values[2] = 2.0 * z - 1.0;
                    Matrix4d.prj(maux.val, values);
                    frustumCorners.get(i).set(values);
                    i++;
                }
            }
        }

    }

    public void disposeCachedFrameBuffers() {
        if (!frameBuffers.isEmpty()) {
            frameBuffers.forEach(GLFrameBuffer::dispose);
            frameBuffers.clear();
        }
    }

    public void dispose() {
        disposeCachedFrameBuffers();
    }
}
