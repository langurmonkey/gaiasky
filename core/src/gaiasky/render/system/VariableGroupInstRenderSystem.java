/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

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
import gaiasky.render.api.IRenderable;
import gaiasky.render.RenderGroup;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.VariableRecord;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.color.Colormap;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.shader.ExtShaderProgram;

/**
 * Renders variable star groups using instancing via billboards with geometry (quads as two triangles).
 */
public class VariableGroupInstRenderSystem extends InstancedRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(VariableGroupInstRenderSystem.class);

    // Maximum number of data points in the light curves
    public static final int MAX_VARI = 20;

    private final Vector3 aux1;
    private int nVariOffset, variMagsOffset, variTimesOffset, pmOffset, starPosOffset;
    private final Colormap cmap;

    private StarGroupTriComponent triComponent;

    public VariableGroupInstRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        this.aux1 = new Vector3();
        cmap = new Colormap();
        triComponent.setStarTexture(Settings.settings.scene.star.getStarTexture());

        EventManager.instance.subscribe(this, Event.STAR_BRIGHTNESS_CMD, Event.STAR_BRIGHTNESS_POW_CMD, Event.STAR_POINT_SIZE_CMD, Event.STAR_MIN_OPACITY_CMD, Event.GPU_DISPOSE_VARIABLE_GROUP, Event.BILLBOARD_TEXTURE_IDX_CMD);
    }

    @Override
    protected void addAttributesDivisor0(Array<VertexAttribute> attributes) {
        // Vertex position and texture coordinates are global
        attributes.add(new VertexAttribute(Usage.Position, 2, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ExtShaderProgram.TEXCOORD_ATTRIBUTE));
    }

    @Override
    protected void addAttributesDivisor1(Array<VertexAttribute> attributes) {
        // Color, object position, proper motion and time series are per instance
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.ProperMotion, 3, "a_pm"));
        attributes.add(new VertexAttribute(OwnUsage.ObjectPosition, 3, "a_starPos"));

        attributes.add(new VertexAttribute(OwnUsage.NumVariablePoints, 1, "a_nVari"));
        attributes.add(new VertexAttribute(OwnUsage.VariableMagnitudes, 4, "a_vmags1"));
        attributes.add(new VertexAttribute(OwnUsage.VariableMagnitudes + 1, 4, "a_vmags2"));
        attributes.add(new VertexAttribute(OwnUsage.VariableMagnitudes + 2, 4, "a_vmags3"));
        attributes.add(new VertexAttribute(OwnUsage.VariableMagnitudes + 3, 4, "a_vmags4"));
        attributes.add(new VertexAttribute(OwnUsage.VariableMagnitudes + 4, 4, "a_vmags5"));
        attributes.add(new VertexAttribute(OwnUsage.VariableTimes, 4, "a_vtimes1"));
        attributes.add(new VertexAttribute(OwnUsage.VariableTimes + 1, 4, "a_vtimes2"));
        attributes.add(new VertexAttribute(OwnUsage.VariableTimes + 2, 4, "a_vtimes3"));
        attributes.add(new VertexAttribute(OwnUsage.VariableTimes + 3, 4, "a_vtimes4"));
        attributes.add(new VertexAttribute(OwnUsage.VariableTimes + 4, 4, "a_vtimes5"));
    }

    @Override
    protected void offsets0(MeshData curr) {
        // Not needed
    }

    @Override
    protected void offsets1(MeshData curr) {
        curr.colorOffset = curr.mesh.getInstancedAttribute(Usage.ColorPacked) != null ? curr.mesh.getInstancedAttribute(Usage.ColorPacked).offset / 4 : 0;
        pmOffset = curr.mesh.getInstancedAttribute(OwnUsage.ProperMotion) != null ? curr.mesh.getInstancedAttribute(OwnUsage.ProperMotion).offset / 4 : 0;
        starPosOffset = curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition) != null ? curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition).offset / 4 : 0;
        nVariOffset = curr.mesh.getInstancedAttribute(OwnUsage.NumVariablePoints) != null ? curr.mesh.getInstancedAttribute(OwnUsage.NumVariablePoints).offset / 4 : 0;
        variMagsOffset = curr.mesh.getInstancedAttribute(OwnUsage.VariableMagnitudes) != null ? curr.mesh.getInstancedAttribute(OwnUsage.VariableMagnitudes).offset / 4 : 0;
        variTimesOffset = curr.mesh.getInstancedAttribute(OwnUsage.VariableTimes) != null ? curr.mesh.getInstancedAttribute(OwnUsage.VariableTimes).offset / 4 : 0;
    }

    @Override
    protected void initShaderProgram() {
        this.triComponent = new StarGroupTriComponent();
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
        final StarGroup starGroup = (StarGroup) renderable;
        synchronized (starGroup) {
            if (!starGroup.disposed) {
                boolean hlCmap = starGroup.isHighlighted() && !starGroup.isHlplain();
                int n = starGroup.size();
                if (!inGpu(starGroup)) {
                    int offset = addMeshData(6, n);
                    setOffset(starGroup, offset);
                    curr = meshes.get(offset);
                    ensureInstanceAttribsSize(n * curr.instanceSize);
                    int numStarsAdded = 0;

                    for (int i = 0; i < n; i++) {
                        if (starGroup.filter(i) && starGroup.isVisible(i)) {
                            VariableRecord particle = (VariableRecord) starGroup.data().get(i);
                            if (!Double.isFinite(particle.size())) {
                                logger.debug("Star " + particle.id() + " has a non-finite size");
                                continue;
                            }

                            // COLOR
                            if (hlCmap) {
                                // Color map
                                double[] color = cmap.colormap(starGroup.getHlcmi(), starGroup.getHlcma().get(particle), starGroup.getHlcmmin(), starGroup.getHlcmmax());
                                tempInstanceAttribs[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits((float) color[0], (float) color[1], (float) color[2], 1.0f);
                            } else {
                                // Plain
                                tempInstanceAttribs[curr.instanceIdx + curr.colorOffset] = starGroup.getColor(i);
                            }

                            // VARIABLE STARS (magnitudes and times)
                            tempInstanceAttribs[curr.instanceIdx + nVariOffset] = particle.nVari;
                            for (int k = 0; k < particle.nVari; k++) {
                                if (starGroup.isHlAllVisible() && starGroup.isHighlighted()) {
                                    tempInstanceAttribs[curr.instanceIdx + variMagsOffset + k] = Math.max(10f, (float) (particle.variMag(k) * Constants.STAR_SIZE_FACTOR) * starGroup.highlightedSizeFactor());
                                } else {
                                    tempInstanceAttribs[curr.instanceIdx + variMagsOffset + k] = (float) (particle.variMag(k) * Constants.STAR_SIZE_FACTOR) * starGroup.highlightedSizeFactor();
                                }
                                tempInstanceAttribs[curr.instanceIdx + variTimesOffset + k] = (float) particle.variTime(k);
                            }

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
                    setCount(starGroup, count);
                    curr.mesh.setInstanceAttribs(tempInstanceAttribs, 0, count);

                    setInGpu(starGroup, true);
                }

                /*
                 * RENDER
                 */
                curr = meshes.get(getOffset(starGroup));
                if (curr != null) {
                    if (triComponent.starTex != null) {
                        triComponent.starTex.bind(0);
                        shaderProgram.setUniformi("u_starTex", 0);
                    }

                    triComponent.alphaSizeBr[0] = starGroup.opacity * alphas[starGroup.ct.getFirstOrdinal()];
                    triComponent.alphaSizeBr[1] = triComponent.starPointSize * 1e6f * starGroup.highlightedSizeFactor();
                    shaderProgram.setUniform3fv("u_alphaSizeBr", triComponent.alphaSizeBr, 0, 3);

                    // Days since epoch
                    // Emulate double with floats, for compatibility
                    double curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), starGroup.getEpoch());
                    float curRt2 = (float) (curRt - (double) ((float) curRt));
                    shaderProgram.setUniformf("u_t", (float) curRt, curRt2);

                    curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), starGroup.getVariabilityEpoch());
                    shaderProgram.setUniformf("u_s", (float) curRt);

                    try {
                        curr.mesh.render(shaderProgram, GL20.GL_TRIANGLES, 0, 6, n);
                    } catch (IllegalArgumentException e) {
                        logger.error(e, "Render exception");
                    }
                }
            }
        }
    }

    protected void setInGpu(IRenderable renderable, boolean state) {
        if(inGpu != null) {
            if(inGpu.contains(renderable) && !state) {
                EventManager.publish(Event.GPU_DISPOSE_VARIABLE_GROUP, renderable);
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
        case GPU_DISPOSE_VARIABLE_GROUP -> {
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
