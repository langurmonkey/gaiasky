/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

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
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.camera.FovCamera;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.color.Colormap;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.shader.ExtShaderProgram;

/**
 * Renders star sets using regular arrays via billboards with geometry (quads as two triangles).
 */
public class StarSetRenderer extends PointCloudQuadRenderer implements IObserver {
    protected static final Log logger = Logger.getLogger(StarSetRenderer.class);

    private final Vector3 aux1;
    private int sizeOffset, pmOffset, uvOffset, starPosOffset;
    private final Colormap cmap;

    private final ParticleUtils utils;

    private StarSetQuadComponent triComponent;

    public StarSetRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        this.aux1 = new Vector3();
        this.utils = new ParticleUtils();
        this.cmap = new Colormap();
        this.triComponent.setStarTexture(Settings.settings.scene.star.getStarTexture());

        EventManager.instance.subscribe(this, Event.STAR_BRIGHTNESS_CMD, Event.STAR_BRIGHTNESS_POW_CMD, Event.STAR_POINT_SIZE_CMD, Event.STAR_BASE_LEVEL_CMD, Event.GPU_DISPOSE_STAR_GROUP, Event.BILLBOARD_TEXTURE_IDX_CMD);
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

    protected void renderObject(ExtShaderProgram shaderProgram, IRenderable renderable) {
        final Render render = (Render) renderable;
        var base = Mapper.base.get(render.entity);
        var set = Mapper.starSet.get(render.entity);
        var hl = Mapper.highlight.get(render.entity);
        var desc = Mapper.datasetDescription.get(render.entity);

        float sizeFactor = utils.getDatasetSizeFactor(render.entity, hl, desc);

        if (!set.disposed) {
            boolean hlCmap = hl.isHighlighted() && !hl.isHlplain();
            if (!inGpu(render)) {
                int n = set.data().size();
                int offset = addMeshData(n * 4, n * 6);
                setOffset(render, offset);
                // Get mesh, reset indices
                curr = meshes.get(offset);
                ensureTempVertsSize(n * 4 * curr.vertexSize);
                ensureTempIndicesSize(n * 6);
                int numVerticesAdded = 0;
                int numStarsAdded = 0;

                for (int i = 0; i < n; i++) {
                    if (utils.filter(i, set, desc) && set.isVisible(i)) {
                        IParticleRecord particle = set.data().get(i);
                        if (!Double.isFinite(particle.size())) {
                            logger.debug("Star " + particle.id() + " has a non-finite size");
                            continue;
                        }
                        // 4 vertices per star
                        for (int vert = 0; vert < 4; vert++) {
                            // Vertex POSITION
                            tempVerts[curr.vertexIdx] = vertPos[vert].getFirst();
                            tempVerts[curr.vertexIdx + 1] = vertPos[vert].getSecond();

                            // UV coordinates
                            tempVerts[curr.vertexIdx + uvOffset] = vertUV[vert].getFirst();
                            tempVerts[curr.vertexIdx + uvOffset + 1] = vertUV[vert].getSecond();

                            // COLOR
                            if (hlCmap) {
                                // Color map
                                double[] color = cmap.colormap(hl.getHlcmi(), hl.getHlcma().get(particle), hl.getHlcmmin(), hl.getHlcmmax());
                                tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits((float) color[0], (float) color[1], (float) color[2], 1.0f);
                            } else {
                                // Plain
                                tempVerts[curr.vertexIdx + curr.colorOffset] = utils.getColor(i, set, hl);
                            }

                            // SIZE
                            tempVerts[curr.vertexIdx + sizeOffset] = (float) (particle.size() * Constants.STAR_SIZE_FACTOR) * sizeFactor;

                            // PROPER MOTION [u/yr]
                            tempVerts[curr.vertexIdx + pmOffset] = (float) particle.pmx();
                            tempVerts[curr.vertexIdx + pmOffset + 1] = (float) particle.pmy();
                            tempVerts[curr.vertexIdx + pmOffset + 2] = (float) particle.pmz();

                            // STAR POSITION [u]
                            tempVerts[curr.vertexIdx + starPosOffset] = (float) particle.x();
                            tempVerts[curr.vertexIdx + starPosOffset + 1] = (float) particle.y();
                            tempVerts[curr.vertexIdx + starPosOffset + 2] = (float) particle.z();

                            curr.vertexIdx += curr.vertexSize;
                            curr.numVertices++;
                            numVerticesAdded++;
                        }
                        // Indices
                        quadIndices(curr);
                        numStarsAdded++;
                    }
                }
                int count = numVerticesAdded * curr.vertexSize;
                setCount(render, count);
                curr.mesh.setVertices(tempVerts, 0, count);
                curr.mesh.setIndices(tempIndices, 0, numStarsAdded * 6);

                setInGpu(render, true);
            }

            /*
             * RENDER
             */
            curr = meshes.get(getOffset(render));
            if (curr != null) {
                if (triComponent.starTex != null) {
                    triComponent.starTex.bind(0);
                    shaderProgram.setUniformi("u_starTex", 0);
                }

                triComponent.alphaSizeBr[0] = base.opacity * alphas[base.ct.getFirstOrdinal()];
                triComponent.alphaSizeBr[1] = triComponent.starPointSize * 1e6f * sizeFactor;
                shaderProgram.setUniform3fv("u_alphaSizeBr", triComponent.alphaSizeBr, 0, 3);

                // Days since epoch
                // Emulate double with floats, for compatibility
                double curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), set.epochJd);
                float curRt2 = (float) (curRt - (double) ((float) curRt));
                shaderProgram.setUniformf("u_t", (float) curRt, curRt2);

                // Opacity limits
                triComponent.setOpacityLimitsUniform(shaderProgram, hl);

                try {
                    curr.mesh.render(shaderProgram, GL20.GL_TRIANGLES);
                } catch (IllegalArgumentException e) {
                    logger.error(e, "Render exception");
                }
            }
        }
    }

    protected void setInGpu(IRenderable renderable, boolean state) {
        if (inGpu != null) {
            if (inGpu.contains(renderable) && !state) {
                EventManager.publish(Event.GPU_DISPOSE_STAR_GROUP, renderable);
            }
            if (state) {
                inGpu.add(renderable);
            } else {
                inGpu.remove(renderable);
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
        case GPU_DISPOSE_STAR_GROUP -> {
            IRenderable renderable = (IRenderable) source;
            int offset = getOffset(renderable);
            clearMeshData(offset);
            inGpu.remove(renderable);
        }
        case BILLBOARD_TEXTURE_IDX_CMD -> GaiaSky.postRunnable(() -> triComponent.setStarTexture(Settings.settings.scene.star.getStarTexture()));
        default -> {
        }
        }
    }

}
