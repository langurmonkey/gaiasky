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
import gaiasky.render.system.InstancedRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Render;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.camera.FovCamera;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.color.Colormap;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.shader.ExtShaderProgram;

/**
 * Renders star sets using instancing via billboards with geometry (quads as two triangles).
 */
public class StarSetInstancedRenderer extends InstancedRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(StarSetInstancedRenderer.class);

    private final Vector3 aux1;
    private int sizeOffset, pmOffset, starPosOffset;
    private final Colormap cmap;

    private StarSetQuadComponent triComponent;
    private final ParticleUtils utils;

    public StarSetInstancedRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        utils = new ParticleUtils();
        cmap = new Colormap();

        aux1 = new Vector3();
        triComponent.setStarTexture(Settings.settings.scene.star.getStarTexture());

        EventManager.instance.subscribe(this, Event.STAR_BRIGHTNESS_CMD, Event.STAR_BRIGHTNESS_POW_CMD, Event.STAR_POINT_SIZE_CMD, Event.STAR_MIN_OPACITY_CMD, Event.GPU_DISPOSE_STAR_GROUP, Event.BILLBOARD_TEXTURE_IDX_CMD);
    }

    @Override
    protected void addAttributesDivisor0(Array<VertexAttribute> attributes) {
        // Vertex position and texture coordinates are global
        attributes.add(new VertexAttribute(Usage.Position, 2, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ExtShaderProgram.TEXCOORD_ATTRIBUTE));
    }

    @Override
    protected void addAttributesDivisor1(Array<VertexAttribute> attributes) {
        // Color, object position, proper motion and size are per instance
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.ObjectPosition, 3, "a_starPos"));
        attributes.add(new VertexAttribute(OwnUsage.ProperMotion, 3, "a_pm"));
        attributes.add(new VertexAttribute(OwnUsage.Size, 1, "a_size"));
    }

    @Override
    protected void offsets0(MeshData curr) {
        // Not needed
    }

    @Override
    protected void offsets1(MeshData curr) {
        curr.colorOffset = curr.mesh.getInstancedAttribute(Usage.ColorPacked) != null ? curr.mesh.getInstancedAttribute(Usage.ColorPacked).offset / 4 : 0;
        pmOffset = curr.mesh.getInstancedAttribute(OwnUsage.ProperMotion) != null ? curr.mesh.getInstancedAttribute(OwnUsage.ProperMotion).offset / 4 : 0;
        sizeOffset = curr.mesh.getInstancedAttribute(OwnUsage.Size) != null ? curr.mesh.getInstancedAttribute(OwnUsage.Size).offset / 4 : 0;
        starPosOffset = curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition) != null ? curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition).offset / 4 : 0;
    }

    @Override
    protected void initShaderProgram() {
        this.triComponent = new StarSetQuadComponent();
        this.triComponent.initShaderProgram(getShaderProgram());
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

        synchronized (render) {
            if (!set.disposed) {
                boolean hlCmap = hl.isHighlighted() && !hl.isHlplain();
                int n = set.data().size();
                if (!inGpu(render)) {
                    int offset = addMeshData(6, n);
                    setOffset(render, offset);
                    curr = meshes.get(offset);
                    ensureInstanceAttribsSize(n * curr.instanceSize);
                    int numStarsAdded = 0;

                    for (int i = 0; i < n; i++) {
                        if (utils.filter(i, set, desc) && set.isVisible(i)) {
                            IParticleRecord particle = set.get(i);
                            if (!Double.isFinite(particle.size())) {
                                logger.debug("Star " + particle.id() + " has a non-finite size");
                                continue;
                            }

                            // COLOR
                            if (hlCmap) {
                                // Color map
                                double[] color = cmap.colormap(hl.getHlcmi(), hl.getHlcma().get(particle), hl.getHlcmmin(), hl.getHlcmmax());
                                tempInstanceAttribs[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits((float) color[0], (float) color[1], (float) color[2], 1.0f);
                            } else {
                                // Plain
                                tempInstanceAttribs[curr.instanceIdx + curr.colorOffset] = utils.getColor(i, set, hl);
                            }

                            // SIZE
                            tempInstanceAttribs[curr.instanceIdx + sizeOffset] = (float) (particle.size() * Constants.STAR_SIZE_FACTOR) * sizeFactor;

                            // PROPER MOTION [u/yr]
                            tempInstanceAttribs[curr.instanceIdx + pmOffset] = (float) particle.pmx();
                            tempInstanceAttribs[curr.instanceIdx + pmOffset + 1] = (float) particle.pmy();
                            tempInstanceAttribs[curr.instanceIdx + pmOffset + 2] = (float) particle.pmz();

                            // STAR POSITION [u]
                            tempInstanceAttribs[curr.instanceIdx + starPosOffset] = (float) particle.x();
                            tempInstanceAttribs[curr.instanceIdx + starPosOffset + 1] = (float) particle.y();
                            tempInstanceAttribs[curr.instanceIdx + starPosOffset + 2] = (float) particle.z();

                            curr.instanceIdx += curr.instanceSize;
                            curr.numVertices++;
                            numStarsAdded++;
                        }
                    }
                    // Global (divisor=0) vertices (position, uv)
                    curr.mesh.setVertices(tempVerts, 0, 24);
                    // Per instance (divisor=1) vertices
                    int count = numStarsAdded * curr.instanceSize;
                    setCount(render, numStarsAdded);
                    curr.mesh.setInstanceAttribs(tempInstanceAttribs, 0, count);

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
                        curr.mesh.render(shaderProgram, GL20.GL_TRIANGLES, 0, 6, getCount(render));
                    } catch (IllegalArgumentException e) {
                        logger.error(e, "Render exception");
                    }
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
        case STAR_MIN_OPACITY_CMD -> {
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
