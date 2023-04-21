/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.camera.FovCamera;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.view.RenderView;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.shader.ExtShaderProgram;

import java.util.List;

public class SinglePointRenderer extends PointCloudQuadRenderer implements IObserver {
    protected static final Log logger = Logger.getLogger(SinglePointRenderer.class);

    private final RenderView view;
    private final Vector3 aux1 = new Vector3();
    private final ComponentType ct;
    boolean initializing;
    private int sizeOffset, pmOffset, uvOffset, starPosOffset;
    private StarSetQuadComponent triComponent;

    public SinglePointRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders, ComponentType ct) {
        super(sceneRenderer, rg, alphas, shaders);
        this.view = new RenderView();
        this.ct = ct;
        this.triComponent.setStarTexture(Settings.settings.scene.star.getStarTexture());
        initializing = true;

        // Create mesh data.
        addMeshData(500 * 4, 500 * 6);

        EventManager.instance.subscribe(this, Event.STAR_BRIGHTNESS_CMD, Event.STAR_BRIGHTNESS_POW_CMD, Event.STAR_POINT_SIZE_CMD, Event.STAR_BASE_LEVEL_CMD, Event.BILLBOARD_TEXTURE_IDX_CMD);
    }

    protected void addVertexAttributes(Array<VertexAttribute> attributes) {
        attributes.add(new VertexAttribute(Usage.Position, 2, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ExtShaderProgram.TEXCOORD_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.ObjectPosition, 3, "a_starPos"));
        attributes.add(new VertexAttribute(OwnUsage.ProperMotion, 3, "a_pm"));
        attributes.add(new VertexAttribute(OwnUsage.Size, 1, "a_size"));
    }

    protected void offsets(MeshData curr) {
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        uvOffset = curr.mesh.getVertexAttribute(Usage.TextureCoordinates) != null ? curr.mesh.getVertexAttribute(Usage.TextureCoordinates).offset / 4 : 0;
        pmOffset = curr.mesh.getVertexAttribute(OwnUsage.ProperMotion) != null ? curr.mesh.getVertexAttribute(OwnUsage.ProperMotion).offset / 4 : 0;
        sizeOffset = curr.mesh.getVertexAttribute(OwnUsage.Size) != null ? curr.mesh.getVertexAttribute(OwnUsage.Size).offset / 4 : 0;
        starPosOffset = curr.mesh.getVertexAttribute(OwnUsage.ObjectPosition) != null ? curr.mesh.getVertexAttribute(OwnUsage.ObjectPosition).offset / 4 : 0;
    }

    @Override
    protected void initShaderProgram() {
        triComponent = new StarSetQuadComponent();
        triComponent.initShaderProgram(getShaderProgram());
    }

    protected void preRenderObjects(ExtShaderProgram shaderProgram, ICamera camera) {
        shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
        shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux1));
        addEffectsUniforms(shaderProgram, camera);
        // Update projection if fovMode is 3
        triComponent.fovMode = camera.getMode().getGaiaFovMode();
        if (triComponent.fovMode == 3) {
            // Cam is Fov1 & Fov2
            FovCamera cam = ((CameraManager) camera).fovCamera;
            // Update combined
            PerspectiveCamera[] cams = camera.getFrontCameras();
            shaderProgram.setUniformMatrix("u_projView", cams[cam.dirIndex].combined);
        }
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size() > 0) {
            ExtShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();
            // Pre-render
            preRenderObjects(shaderProgram, camera);
            // Render
            renderObjects(shaderProgram, renderables);
            // Post-render
            postRenderObjects(shaderProgram, camera);
            shaderProgram.end();
        }
    }

    private void renderObjects(ExtShaderProgram shaderProgram, List<IRenderable> renderables) {

        int n = renderables.size();
        ensureTempVertsSize(n * 4 * curr.vertexSize);
        ensureTempIndicesSize(n * 6);
        int numVerticesAdded = 0;
        int numStarsAdded = 0;
        curr.clear();

        for (int i = 0; i < n; i++) {
            Entity entity = ((Render) renderables.get(i)).entity;
            view.setEntity(entity);
            var base = Mapper.base.get(entity);
            var body = Mapper.body.get(entity);
            float[] col = body.color;
            // 4 vertices per star
            for (int vert = 0; vert < 4; vert++) {
                // Vertex POSITION
                tempVerts[curr.vertexIdx] = vertPos[vert].getFirst();
                tempVerts[curr.vertexIdx + 1] = vertPos[vert].getSecond();

                // UV coordinates
                tempVerts[curr.vertexIdx + uvOffset] = vertUV[vert].getFirst();
                tempVerts[curr.vertexIdx + uvOffset + 1] = vertUV[vert].getSecond();

                // COLOR
                tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(col[0], col[1], col[2], base.opacity);

                // SIZE
                tempVerts[curr.vertexIdx + sizeOffset] = (float) (view.getRadius() * 5.0);

                // POSITION
                aux1.set(body.pos.x.floatValue(), body.pos.y.floatValue(), body.pos.z.floatValue());
                tempVerts[curr.vertexIdx + starPosOffset] = aux1.x;
                tempVerts[curr.vertexIdx + starPosOffset + 1] = aux1.y;
                tempVerts[curr.vertexIdx + starPosOffset + 2] = aux1.z;

                // PROPER MOTION (body position already has the proper motion for single stars)
                tempVerts[curr.vertexIdx + pmOffset] = 0;
                tempVerts[curr.vertexIdx + pmOffset + 1] = 0;
                tempVerts[curr.vertexIdx + pmOffset + 2] = 0;

                curr.vertexIdx += curr.vertexSize;
                curr.numVertices++;
                numVerticesAdded++;
            }
            // Indices
            quadIndices(curr);
            numStarsAdded++;
        }
        int count = numVerticesAdded * curr.vertexSize;
        curr.mesh.setVertices(tempVerts, 0, count);
        curr.mesh.setIndices(tempIndices, 0, numStarsAdded * 6);

        /*
         * RENDER
         */
        if (curr != null) {
            if (triComponent.starTex != null) {
                triComponent.starTex.bind(0);
                shaderProgram.setUniformi("u_starTex", 0);
            }

            triComponent.alphaSizeBr[0] = alphas[ct.ordinal()];
            triComponent.alphaSizeBr[1] = triComponent.starPointSize * 1e6f;
            shaderProgram.setUniform3fv("u_alphaSizeBr", triComponent.alphaSizeBr, 0, 3);

            // Fixed size
            shaderProgram.setUniformf("u_fixedAngularSize", -1f);

            // Days since epoch
            // Emulate double with floats, for compatibility
            double curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), AstroUtils.JD_J2015);
            float curRt2 = (float) (curRt - (double) ((float) curRt));
            shaderProgram.setUniformf("u_t", (float) curRt, curRt2);

            // Opacity limits
            triComponent.setOpacityLimitsUniform(shaderProgram, null);

            try {
                curr.mesh.render(shaderProgram, GL20.GL_TRIANGLES);
            } catch (IllegalArgumentException e) {
                logger.error(e, "Render exception");
            }
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case STAR_BASE_LEVEL_CMD -> {
            triComponent.updateStarOpacityLimits((float) data[0], Settings.settings.scene.star.opacity[1]);
            triComponent.touchStarParameters(getShaderProgram());
        }
        case STAR_BRIGHTNESS_CMD -> {
            triComponent.updateStarBrightness((float) data[0]);
            triComponent.touchStarParameters(getShaderProgram());
        }
        case STAR_BRIGHTNESS_POW_CMD -> {
            triComponent.updateBrightnessPower((float) data[0]);
            triComponent.touchStarParameters(getShaderProgram());
        }
        case STAR_POINT_SIZE_CMD -> {
            triComponent.updateStarPointSize((float) data[0]);
            triComponent.touchStarParameters(getShaderProgram());
        }
        case BILLBOARD_TEXTURE_IDX_CMD -> GaiaSky.postRunnable(() -> triComponent.setStarTexture(Settings.settings.scene.star.getStarTexture()));
        default -> {
        }
        }
    }
}
