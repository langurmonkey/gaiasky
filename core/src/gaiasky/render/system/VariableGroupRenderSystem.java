/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.VariableRecord;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.color.Colormap;
import gaiasky.util.comp.DistToCameraComparator;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.shader.ExtShaderProgram;

/**
 * Renders variable stars which have periodical light curve data
 */
public class VariableGroupRenderSystem extends PointCloudTriRenderSystem implements IObserver {
    // Maximum number of data points in the light curves
    public static final int MAX_VARI = 20;

    private final Vector3 aux1;
    private int nVariOffset, variMagsOffset, variTimesOffset, pmOffset, uvOffset, starPosOffset;
    private final float[] alphaSizeBr;
    private final Colormap cmap;

    private float starPointSize;
    private float[] solidAngleLimits;
    private int fovMode;
    private Texture starTex;

    public VariableGroupRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        this.comp = new DistToCameraComparator<>();
        this.alphaSizeBr = new float[3];
        this.aux1 = new Vector3();
        cmap = new Colormap();
        setStarTexture(Settings.settings.scene.star.getStarTexture());
        solidAngleLimits = new float[]{(float) Math.tan(Math.toRadians((Settings.settings.scene.star.opacity[0]) * 0.3f)), (float) Math.tan(Math.toRadians((Settings.settings.scene.star.opacity[1]) * 50f))};

        EventManager.instance.subscribe(this, Events.STAR_MIN_OPACITY_CMD, Events.DISPOSE_STAR_GROUP_GPU_MESH, Events.STAR_TEXTURE_IDX_CMD);
    }

    public void setStarTexture(String starTexture) {
        starTex = new Texture(Settings.settings.data.dataFileHandle(starTexture), true);
        starTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    @Override
    protected void initShaderProgram() {
        ExtShaderProgram shaderProgram = getShaderProgram();
        shaderProgram.begin();
        // Uniforms that rarely change
        shaderProgram.setUniformf("u_thAnglePoint", 1e-10f, 1.5e-8f);
        shaderProgram.end();
    }

    protected void addVertexAttributes(Array<VertexAttribute> attributes) {
        attributes.add(new VertexAttribute(Usage.Position, 2, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ExtShaderProgram.TEXCOORD_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.ProperMotion, 3, "a_pm"));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
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

    protected void offsets(MeshData curr) {
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        uvOffset = curr.mesh.getVertexAttribute(Usage.TextureCoordinates) != null ? curr.mesh.getVertexAttribute(Usage.TextureCoordinates).offset / 4 : 0;
        pmOffset = curr.mesh.getVertexAttribute(OwnUsage.ProperMotion) != null ? curr.mesh.getVertexAttribute(OwnUsage.ProperMotion).offset / 4 : 0;
        starPosOffset = curr.mesh.getVertexAttribute(OwnUsage.ObjectPosition) != null ? curr.mesh.getVertexAttribute(OwnUsage.ObjectPosition).offset / 4 : 0;
        nVariOffset = curr.mesh.getVertexAttribute(OwnUsage.NumVariablePoints) != null ? curr.mesh.getVertexAttribute(OwnUsage.NumVariablePoints).offset / 4 : 0;
        variMagsOffset = curr.mesh.getVertexAttribute(OwnUsage.VariableMagnitudes) != null ? curr.mesh.getVertexAttribute(OwnUsage.VariableMagnitudes).offset / 4 : 0;
        variTimesOffset = curr.mesh.getVertexAttribute(OwnUsage.VariableTimes) != null ? curr.mesh.getVertexAttribute(OwnUsage.VariableTimes).offset / 4 : 0;
    }

    protected void globalUniforms(ExtShaderProgram shaderProgram, ICamera camera) {

        starPointSize = Settings.settings.scene.star.pointSize * 0.2f;

        shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
        shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux1));
        shaderProgram.setUniformf("u_camUp", camera.getUp().put(aux1));
        shaderProgram.setUniformf("u_ar", Settings.settings.program.modeStereo.isStereoHalfWidth() ? 2f : 1f);
        shaderProgram.setUniform2fv("u_solidAngleLimits", solidAngleLimits, 0, 2);
        addEffectsUniforms(shaderProgram, camera);
        // Update projection if fovMode is 3
        fovMode = camera.getMode().getGaiaFovMode();
        if (fovMode == 3) {
            // Cam is Fov1 & Fov2
            FovCamera cam = ((CameraManager) camera).fovCamera;
            // Update combined
            PerspectiveCamera[] cams = camera.getFrontCameras();
            shaderProgram.setUniformMatrix("u_projView", cams[cam.dirIndex].combined);
        }
        alphaSizeBr[2] = Settings.settings.scene.star.brightness;
        shaderProgram.setUniformf("u_brightnessPower", ((Settings.settings.scene.star.power / 1.1f) - 0.1f) * 2.0f - 1.0f);
    }

    protected void renderObject(ExtShaderProgram shaderProgram, IRenderable r) {
        final StarGroup starGroup = (StarGroup) r;
        synchronized (starGroup) {
            if (!starGroup.disposed) {
                boolean hlCmap = starGroup.isHighlighted() && !starGroup.isHlplain();
                if (!starGroup.inGpu()) {
                    int n = starGroup.size();
                    if (starGroup.offset < 0) {
                        starGroup.offset = addMeshData(n * 4, n * 6);
                    }
                    curr = meshes.get(starGroup.offset);
                    ensureTempVertsSize(n * 4 * curr.vertexSize);
                    ensureTempIndicesSize(n * 6);
                    int numVerticesAdded = 0;
                    int numStarsAdded = 0;

                    for (int i = 0; i < n; i++) {
                        if (starGroup.filter(i) && starGroup.isVisible(i)) {
                            VariableRecord particle = (VariableRecord) starGroup.data().get(i);
                            if (!Double.isFinite(particle.size())) {
                                logger.debug("Star " + particle.id() + " has a non-finite size");
                                continue;
                            }
                            // 4 vertices per star
                            for (int vert = 0; vert < 4; vert++) {
                                // COLOR
                                if (hlCmap) {
                                    // Color map
                                    double[] color = cmap.colormap(starGroup.getHlcmi(), starGroup.getHlcma().get(particle), starGroup.getHlcmmin(), starGroup.getHlcmmax());
                                    tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits((float) color[0], (float) color[1], (float) color[2], 1.0f);
                                } else {
                                    // Plain
                                    tempVerts[curr.vertexIdx + curr.colorOffset] = starGroup.getColor(i);
                                }

                                // VARIABLE STARS (magnitudes and times)
                                tempVerts[curr.vertexIdx + nVariOffset] = particle.nVari;
                                for (int k = 0; k < particle.nVari; k++) {
                                    if (starGroup.isHlAllVisible() && starGroup.isHighlighted()) {
                                        tempVerts[curr.vertexIdx + variMagsOffset + k] = Math.max(10f, (float) (particle.variMag(k) * Constants.STAR_QUAD_SIZE_FACTOR) * starGroup.highlightedSizeFactor());
                                    } else {
                                        tempVerts[curr.vertexIdx + variMagsOffset + k] = (float) (particle.variMag(k) * Constants.STAR_QUAD_SIZE_FACTOR) * starGroup.highlightedSizeFactor();
                                    }
                                    tempVerts[curr.vertexIdx + variTimesOffset + k] = (float) particle.variTime(k);
                                }

                                // Vertex POSITION
                                tempVerts[curr.vertexIdx] = vertPos[vert].getFirst();
                                tempVerts[curr.vertexIdx + 1] = vertPos[vert].getSecond();

                                // UV coordinates
                                tempVerts[curr.vertexIdx + uvOffset] = vertUV[vert].getFirst();
                                tempVerts[curr.vertexIdx + uvOffset + 1] = vertUV[vert].getSecond();

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
                    starGroup.count = numVerticesAdded * curr.vertexSize;
                    curr.mesh.setVertices(tempVerts, 0, starGroup.count);
                    curr.mesh.setIndices(tempIndices, 0, numStarsAdded * 6);

                    starGroup.inGpu(true);
                }

                /*
                 * RENDER
                 */
                curr = meshes.get(starGroup.offset);
                if (curr != null) {
                    if (starTex != null) {
                        starTex.bind(0);
                        shaderProgram.setUniformi("u_starTex", 0);
                    }

                    alphaSizeBr[0] = starGroup.opacity * alphas[starGroup.ct.getFirstOrdinal()];
                    alphaSizeBr[1] = ((fovMode == 0 ? (Settings.settings.program.modeStereo.isStereoFullWidth() ? 1f : 2f) : 10f) * starPointSize * rc.scaleFactor * starGroup.highlightedSizeFactor()) * 0.3f;
                    shaderProgram.setUniform3fv("u_alphaSizeBr", alphaSizeBr, 0, 3);

                    // Days since epoch
                    // Emulate double with floats, for compatibility
                    double curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), starGroup.getEpoch());
                    float curRt2 = (float) (curRt - (double) ((float) curRt));
                    shaderProgram.setUniformf("u_t", (float) curRt, curRt2);

                    curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), starGroup.getVariabilityepoch());
                    shaderProgram.setUniformf("u_s", (float) curRt);

                    try {
                        curr.mesh.render(shaderProgram, GL20.GL_TRIANGLES);
                    } catch (IllegalArgumentException e) {
                        logger.error(e, "Render exception");
                    }
                }
            }
        }
    }

    @Override
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case STAR_MIN_OPACITY_CMD -> solidAngleLimits[0] = (float) Math.tan(Math.toRadians(((float) data[0]) * 0.3f));
        case DISPOSE_STAR_GROUP_GPU_MESH -> {
            Integer meshIdx = (Integer) data[0];
            clearMeshData(meshIdx);
        }
        case STAR_TEXTURE_IDX_CMD -> GaiaSky.postRunnable(() -> setStarTexture(Settings.settings.scene.star.getStarTexture()));
        default -> {
        }
        }
    }

}
