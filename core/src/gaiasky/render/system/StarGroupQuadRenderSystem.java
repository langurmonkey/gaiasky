/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Quaternion;
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
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.DecalUtils;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.Settings.SceneSettings.StarSettings;
import gaiasky.util.color.Colormap;
import gaiasky.util.comp.DistToCameraComparator;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import org.lwjgl.opengl.GL30;

/**
 * Renders star groups using quads.
 */
public class StarGroupQuadRenderSystem extends ImmediateRenderSystem implements IObserver {
    private final double BRIGHTNESS_FACTOR;

    private final Vector3 aux1;
    private int sizeOffset, pmOffset, uvOffset, starPosOffset;
    private float[] pointAlpha;
    private final float[] alphaSizeFovBr;
    private final float[] pointAlphaHl;
    private final Colormap cmap;

    private Texture starTex;
    private Quaternion quaternion;

    // Positions per vertex index
    private Pair<Float, Float>[] vertPos;
    // UV coordinates per vertex index (0,1,2,4)
    private Pair<Float, Float>[] vertUV;

    public StarGroupQuadRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        BRIGHTNESS_FACTOR = 10;
        this.comp = new DistToCameraComparator<>();
        this.alphaSizeFovBr = new float[4];
        this.pointAlphaHl = new float[] { 2, 4 };
        this.aux1 = new Vector3();
        this.quaternion = new Quaternion();
        cmap = new Colormap();
        setStarTexture(Settings.settings.scene.star.getStarTexture());

        vertPos = new Pair[4];
        vertPos[0] = new Pair<>(1f, 1f);
        vertPos[1] = new Pair<>(1f, -1f);
        vertPos[2] = new Pair<>(-1f, -1f);
        vertPos[3] = new Pair<>(-1f, 1f);

        vertUV = new Pair[4];
        vertUV[0] = new Pair<>(1f, 1f);
        vertUV[1] = new Pair<>(1f, 0f);
        vertUV[2] = new Pair<>(0f, 0f);
        vertUV[3] = new Pair<>(0f, 1f);

        EventManager.instance.subscribe(this, Events.STAR_MIN_OPACITY_CMD, Events.DISPOSE_STAR_GROUP_GPU_MESH, Events.STAR_TEXTURE_IDX_CMD);
    }

    public void setStarTexture(String starTexture) {
        starTex = new Texture(Settings.settings.data.dataFileHandle(starTexture), true);
        starTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    @Override
    protected void initShaderProgram() {
        Gdx.gl.glEnable(GL30.GL_POINT_SPRITE);
        Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);

        pointAlpha = new float[] { Settings.settings.scene.star.opacity[0], Settings.settings.scene.star.opacity[1] };

        ExtShaderProgram shaderProgram = getShaderProgram();
        shaderProgram.begin();
        // Uniforms that rarely change
        shaderProgram.setUniformf("u_thAnglePoint", 1e-10f, 1.5e-8f);
        shaderProgram.end();
    }

    @Override
    protected void initVertices() {
        // STARS
        meshes = new Array<>();
    }

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index
     *
     * @param maxVerts   The max number of vertices this mesh data can hold
     * @param maxIndices The maximum number of indices this mesh data can hold
     *
     * @return The index of the new mesh data
     */
    private int addMeshData(int maxVerts, int maxIndices) {
        int mdi = createMeshData();
        curr = meshes.get(mdi);

        VertexAttribute[] attributes = buildVertexAttributes();
        curr.mesh = new IntMesh(false, maxVerts, maxIndices, attributes);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        uvOffset = curr.mesh.getVertexAttribute(Usage.TextureCoordinates) != null ? curr.mesh.getVertexAttribute(Usage.TextureCoordinates).offset / 4 : 0;
        pmOffset = curr.mesh.getVertexAttribute(Usage.Tangent) != null ? curr.mesh.getVertexAttribute(Usage.Tangent).offset / 4 : 0;
        sizeOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
        starPosOffset = curr.mesh.getVertexAttribute(OwnUsage.StarPosition) != null ? curr.mesh.getVertexAttribute(OwnUsage.StarPosition).offset / 4 : 0;

        return mdi;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            ExtShaderProgram shaderProgram = getShaderProgram();
            float starPointSize = StarSettings.getStarPointSize();

            // Billboard quaternion
            DecalUtils.setBillboardRotation(quaternion, camera.getCamera().direction, camera.getCamera().up);

            shaderProgram.begin();
            // Global uniforms
            shaderProgram.setUniformf("u_quaternion", quaternion.x, quaternion.y, quaternion.z, quaternion.w);
            shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
            shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux1));
            shaderProgram.setUniformf("u_camDir", camera.getCamera().direction);
            shaderProgram.setUniformf("u_brPow", Settings.settings.scene.star.power);
            shaderProgram.setUniformf("u_ar", Settings.settings.program.modeStereo.isStereoHalfWidth() ? 2f : 1f);
            addEffectsUniforms(shaderProgram, camera);
            // Update projection if fovMode is 3
            int fovMode = camera.getMode().getGaiaFovMode();
            if (fovMode == 3) {
                // Cam is Fov1 & Fov2
                FovCamera cam = ((CameraManager) camera).fovCamera;
                // Update combined
                PerspectiveCamera[] cams = camera.getFrontCameras();
                shaderProgram.setUniformMatrix("u_projModelView", cams[cam.dirIndex].combined);
            }
            alphaSizeFovBr[2] = (float) (Settings.settings.scene.star.brightness * BRIGHTNESS_FACTOR);
            alphaSizeFovBr[3] = rc.scaleFactor;

            renderables.forEach(r -> {
                final StarGroup starGroup = (StarGroup) r;
                synchronized (starGroup) {
                    if (!starGroup.disposed) {
                        boolean hlCmap = starGroup.isHighlighted() && !starGroup.isHlplain();
                        if (!starGroup.inGpu()) {
                            int n = starGroup.size();
                            starGroup.offset = addMeshData(n * 4, n * 6);
                            curr = meshes.get(starGroup.offset);
                            ensureTempVertsSize(n * 4 * curr.vertexSize);
                            ensureTempIndicesSize(n * 6);
                            int numVerticesAdded = 0;
                            int numStarsAdded = 0;

                            for (int i = 0; i < n; i++) {
                                if (starGroup.filter(i) && starGroup.isVisible(i)) {
                                    IParticleRecord particle = starGroup.data().get(i);
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

                                        // SIZE
                                        double sizeFac = 6e1;
                                        if (starGroup.isHlAllVisible() && starGroup.isHighlighted()) {
                                            tempVerts[curr.vertexIdx + sizeOffset] = Math.max(10f, (float) (particle.size() * sizeFac) * starGroup.highlightedSizeFactor());
                                        } else {
                                            tempVerts[curr.vertexIdx + sizeOffset] = (float) (particle.size() * sizeFac) * starGroup.highlightedSizeFactor();
                                        }

                                        // Vertex POSITION [u]
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
                                    index(curr.numVertices - 4);
                                    index(curr.numVertices - 3);
                                    index(curr.numVertices - 2);

                                    index(curr.numVertices - 2);
                                    index(curr.numVertices - 1);
                                    index(curr.numVertices - 4);
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

                            shaderProgram.setUniform2fv("u_pointAlpha", starGroup.isHighlighted() && starGroup.getCatalogInfo().hlAllVisible ? pointAlphaHl : pointAlpha, 0, 2);

                            alphaSizeFovBr[0] = starGroup.opacity * alphas[starGroup.ct.getFirstOrdinal()];
                            alphaSizeFovBr[1] = ((fovMode == 0 ? (Settings.settings.program.modeStereo.isStereoFullWidth() ? 1f : 2f) : 10f) * starPointSize * rc.scaleFactor * starGroup.highlightedSizeFactor()) / camera.getFovFactor();
                            shaderProgram.setUniform4fv("u_alphaSizeFovBr", alphaSizeFovBr, 0, 4);

                            // Days since epoch
                            // Emulate double with floats, for compatibility
                            double curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), starGroup.getEpoch());
                            float curRt2 = (float) (curRt - (double) ((float) curRt));
                            shaderProgram.setUniformf("u_t", (float) curRt, curRt2);

                            try {
                                curr.mesh.render(shaderProgram, GL20.GL_TRIANGLES);
                            } catch (IllegalArgumentException e) {
                                logger.error(e);
                            }
                        }
                    }
                }

            });
            shaderProgram.end();
        }
    }

    private void index(int idx) {
        tempIndices[curr.indexIdx++] = idx;
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attributes = new Array<>();
        attributes.add(new VertexAttribute(Usage.Position, 2, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ExtShaderProgram.TEXCOORD_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.Tangent, 3, "a_pm"));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.Generic, 1, "a_size"));
        attributes.add(new VertexAttribute(OwnUsage.StarPosition, 3, "a_starPos"));

        VertexAttribute[] array = new VertexAttribute[attributes.size];
        for (int i = 0; i < attributes.size; i++)
            array[i] = attributes.get(i);
        return array;
    }

    @Override
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case STAR_MIN_OPACITY_CMD:
            pointAlpha[0] = (float) data[0];
            break;
        case DISPOSE_STAR_GROUP_GPU_MESH:
            Integer meshIdx = (Integer) data[0];
            clearMeshData(meshIdx);
            break;
        case STAR_TEXTURE_IDX_CMD:
            GaiaSky.postRunnable(() -> setStarTexture(Settings.settings.scene.star.getStarTexture()));
            break;
        default:
            break;
        }
    }

}
