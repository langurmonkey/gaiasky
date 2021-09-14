/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
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
import gaiasky.util.Settings.SceneSettings.StarSettings;
import gaiasky.util.color.Colormap;
import gaiasky.util.comp.DistToCameraComparator;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import org.lwjgl.opengl.GL30;

public class VariableGroupRenderSystem extends ImmediateRenderSystem implements IObserver {
    private final double BRIGHTNESS_FACTOR;

    private final Vector3 aux1;
    private int nSizesOffset, sizesOffset, pmOffset;
    private float[] pointAlpha;
    private final float[] alphaSizeFovBr;
    private final float[] pointAlphaHl;
    private final Colormap cmap;

    private Texture starTex;

    public VariableGroupRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        BRIGHTNESS_FACTOR = 10;
        this.comp = new DistToCameraComparator<>();
        this.alphaSizeFovBr = new float[4];
        this.pointAlphaHl = new float[] { 2, 4 };
        this.aux1 = new Vector3();
        cmap = new Colormap();
        setStarTexture(Settings.settings.scene.star.getStarTexture());

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
     * @param nVertices The max number of vertices this mesh data can hold
     *
     * @return The index of the new mesh data
     */
    private int addMeshData(int nVertices) {
        int mdi = createMeshData();
        curr = meshes.get(mdi);

        VertexAttribute[] attributes = buildVertexAttributes();
        curr.mesh = new IntMesh(false, nVertices, 0, attributes);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        pmOffset = curr.mesh.getVertexAttribute(Usage.Tangent) != null ? curr.mesh.getVertexAttribute(Usage.Tangent).offset / 4 : 0;
        nSizesOffset = curr.mesh.getVertexAttribute(Usage.BiNormal) != null ? curr.mesh.getVertexAttribute(Usage.BiNormal).offset / 4 : 0;
        sizesOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
        return mdi;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            ExtShaderProgram shaderProgram = getShaderProgram();
            float starPointSize = StarSettings.getStarPointSize();

            shaderProgram.begin();
            // Global uniforms
            shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
            shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux1));
            shaderProgram.setUniformf("u_camDir", camera.getCamera().direction);
            shaderProgram.setUniformi("u_cubemap", Settings.settings.program.modeCubemap.active ? 1 : 0);
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
                final StarGroup variableGroup = (StarGroup) r;
                synchronized (variableGroup) {
                    if (!variableGroup.disposed) {
                        boolean hlCmap = variableGroup.isHighlighted() && !variableGroup.isHlplain();
                        if (!variableGroup.inGpu()) {
                            int n = variableGroup.size();
                            variableGroup.offset = addMeshData(n);
                            curr = meshes.get(variableGroup.offset);
                            ensureTempVertsSize(n * curr.vertexSize);
                            int numAdded = 0;
                            for (int i = 0; i < n; i++) {
                                if (variableGroup.filter(i) && variableGroup.isVisible(i)) {
                                    VariableRecord vr = (VariableRecord) variableGroup.data().get(i);
                                    if (!Double.isFinite(vr.size())) {
                                        logger.debug("Star " + vr.id() + " has a non-finite size");
                                        continue;
                                    }
                                    // COLOR
                                    if (hlCmap) {
                                        // Color map
                                        double[] color = cmap.colormap(variableGroup.getHlcmi(), variableGroup.getHlcma().get(vr), variableGroup.getHlcmmin(), variableGroup.getHlcmmax());
                                        tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits((float) color[0], (float) color[1], (float) color[2], 1.0f);
                                    } else {
                                        // Plain
                                        tempVerts[curr.vertexIdx + curr.colorOffset] = variableGroup.getColor(i);
                                    }

                                    // SIZE
                                    tempVerts[curr.vertexIdx + nSizesOffset] = vr.nMagnitudes;
                                    for (int k = 0; k < vr.nMagnitudes; k++) {
                                        if (variableGroup.isHlAllVisible() && variableGroup.isHighlighted()) {
                                            tempVerts[curr.vertexIdx + sizesOffset + k] = Math.max(10f, (float) (vr.sizes(k) * Constants.STAR_SIZE_FACTOR) * variableGroup.highlightedSizeFactor());
                                        } else {
                                            tempVerts[curr.vertexIdx + sizesOffset + k] = (float) (vr.sizes(k) * Constants.STAR_SIZE_FACTOR) * variableGroup.highlightedSizeFactor();
                                        }
                                    }

                                    // POSITION [u]
                                    tempVerts[curr.vertexIdx] = (float) vr.x();
                                    tempVerts[curr.vertexIdx + 1] = (float) vr.y();
                                    tempVerts[curr.vertexIdx + 2] = (float) vr.z();

                                    // PROPER MOTION [u/yr]
                                    tempVerts[curr.vertexIdx + pmOffset] = (float) vr.pmx();
                                    tempVerts[curr.vertexIdx + pmOffset + 1] = (float) vr.pmy();
                                    tempVerts[curr.vertexIdx + pmOffset + 2] = (float) vr.pmz();

                                    curr.vertexIdx += curr.vertexSize;
                                    numAdded++;
                                }
                            }
                            variableGroup.count = numAdded * curr.vertexSize;
                            curr.mesh.setVertices(tempVerts, 0, variableGroup.count);

                            variableGroup.inGpu(true);

                        }

                        /*
                         * RENDER
                         */
                        curr = meshes.get(variableGroup.offset);
                        if (curr != null) {

                            if (starTex != null) {
                                starTex.bind(0);
                                shaderProgram.setUniformi("u_starTex", 0);
                            }

                            shaderProgram.setUniform2fv("u_pointAlpha", variableGroup.isHighlighted() && variableGroup.getCatalogInfo().hlAllVisible ? pointAlphaHl : pointAlpha, 0, 2);

                            alphaSizeFovBr[0] = variableGroup.opacity * alphas[variableGroup.ct.getFirstOrdinal()];
                            alphaSizeFovBr[1] = ((fovMode == 0 ? (Settings.settings.program.modeStereo.isStereoFullWidth() ? 1f : 2f) : 10f) * starPointSize * rc.scaleFactor * variableGroup.highlightedSizeFactor()) / camera.getFovFactor();
                            shaderProgram.setUniform4fv("u_alphaSizeFovBr", alphaSizeFovBr, 0, 4);

                            // Days since epoch
                            // Emulate double with floats, for compatibility
                            double curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), variableGroup.getEpoch());
                            float curRt2 = (float) (curRt - (double) ((float) curRt));
                            shaderProgram.setUniformf("u_t", (float) curRt, curRt2);

                            double seconds = (curRt - ((int) curRt)) * 86400.0;
                            shaderProgram.setUniformf("u_s", (float) (seconds % 16d));
                            logger.info("u_s=" + ((float) (seconds % 16d)));

                            try {
                                curr.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                            } catch (IllegalArgumentException e) {
                                logger.error("Render exception");
                            }
                        }
                    }
                }

            });
            shaderProgram.end();
        }
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attributes = new Array<>();
        attributes.add(new VertexAttribute(Usage.Position, 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.Tangent, 3, "a_pm"));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.BiNormal, 1, "a_nsizes"));
        attributes.add(new VertexAttribute(Usage.Generic, 4, "a_sizes1"));
        attributes.add(new VertexAttribute(Usage.Normal, 4, "a_sizes2"));
        attributes.add(new VertexAttribute(Usage.BoneWeight, 4, "a_sizes3"));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 4, "a_sizes4"));

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