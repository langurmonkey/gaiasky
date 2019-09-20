/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.FovCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Nature;
import gaia.cu9.ari.gaiaorbit.util.comp.DistToCameraComparator;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.ExtShaderProgram;
import org.lwjgl.opengl.GL30;

public class StarGroupRenderSystem extends ImmediateRenderSystem implements IObserver {
    private final double BRIGHTNESS_FACTOR;

    private Vector3 aux1;
    private int sizeOffset, pmOffset;
    private float[] pointAlpha, alphaSizeFovBr, pointAlphaHl;

    public StarGroupRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        BRIGHTNESS_FACTOR = 10;
        this.comp = new DistToCameraComparator<>();
        this.alphaSizeFovBr = new float[4];
        this.pointAlphaHl = new float[]{2, 4};
        this.aux1 = new Vector3();

        EventManager.instance.subscribe(this, Events.STAR_MIN_OPACITY_CMD, Events.DISPOSE_STAR_GROUP_GPU_MESH);
    }

    @Override
    protected void initShaderProgram() {
        Gdx.gl.glEnable(GL30.GL_POINT_SPRITE);
        Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);

        pointAlpha = new float[] { GlobalConf.scene.POINT_ALPHA_MIN, GlobalConf.scene.POINT_ALPHA_MIN + GlobalConf.scene.POINT_ALPHA_MAX };
    }

    @Override
    protected void initVertices() {
        /** STARS **/
        meshes = new Array<>();
    }

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index
     *
     * @param nVertices The max number of vertices this mesh data can hold
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
        sizeOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
        return mdi;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        //renderables.sort(comp);
        if (renderables.size > 0) {

            ExtShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();
            for (IRenderable renderable : renderables) {
                StarGroup starGroup = (StarGroup) renderable;
                synchronized (starGroup) {
                    if (!starGroup.disposed) {
                        /*
                         * ADD PARTICLES
                         */
                        if (!starGroup.inGpu) {
                            int n = starGroup.size();
                            starGroup.offset = addMeshData(n);
                            curr = meshes.get(starGroup.offset);
                            ensureTempVertsSize(n * curr.vertexSize);
                            for (int i = 0; i < n; i++) {
                                StarBean p = starGroup.data().get(i);
                                // COLOR
                                tempVerts[curr.vertexIdx + curr.colorOffset] = starGroup.getColor(i);

                                // SIZE and APPMAG
                                tempVerts[curr.vertexIdx + sizeOffset] = (float) (p.size() * Constants.STAR_SIZE_FACTOR) * starGroup.highlightedSizeFactor();
                                tempVerts[curr.vertexIdx + sizeOffset + 1] = (float) p.appmag();

                                // POSITION [u]
                                tempVerts[curr.vertexIdx] = (float) p.x();
                                tempVerts[curr.vertexIdx + 1] = (float) p.y();
                                tempVerts[curr.vertexIdx + 2] = (float) p.z();

                                // PROPER MOTION [u/yr]
                                tempVerts[curr.vertexIdx + pmOffset] = (float) p.pmx();
                                tempVerts[curr.vertexIdx + pmOffset + 1] = (float) p.pmy();
                                tempVerts[curr.vertexIdx + pmOffset + 2] = (float) p.pmz();

                                curr.vertexIdx += curr.vertexSize;
                            }
                            starGroup.count = starGroup.size() * curr.vertexSize;
                            curr.mesh.setVertices(tempVerts, 0, starGroup.count);

                            starGroup.inGpu = true;

                        }

                        /*
                         * RENDER
                         */
                        curr = meshes.get(starGroup.offset);
                        if (curr != null) {
                            int fovMode = camera.getMode().getGaiaFovMode();

                            shaderProgram.setUniform2fv("u_pointAlpha", starGroup.isHighlighted() ? pointAlphaHl : pointAlpha, 0, 2);
                            shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
                            shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux1));
                            shaderProgram.setUniformf("u_camDir", camera.getCurrent().getCamera().direction);
                            shaderProgram.setUniformi("u_cubemap", GlobalConf.program.CUBEMAP360_MODE ? 1 : 0);
                            shaderProgram.setUniformf("u_magLimit", GlobalConf.runtime.LIMIT_MAG_RUNTIME);

                            // Rel, grav, z-buffer, etc.
                            addEffectsUniforms(shaderProgram, camera);

                            alphaSizeFovBr[0] = starGroup.opacity * alphas[starGroup.ct.getFirstOrdinal()];
                            alphaSizeFovBr[1] = (fovMode == 0 ? (GlobalConf.program.isStereoFullWidth() ? 1f : 2f) : 10f) * GlobalConf.scene.STAR_POINT_SIZE * rc.scaleFactor * starGroup.highlightedSizeFactor();
                            alphaSizeFovBr[2] = camera.getFovFactor();
                            alphaSizeFovBr[3] = (float) (GlobalConf.scene.STAR_BRIGHTNESS * BRIGHTNESS_FACTOR);
                            shaderProgram.setUniform4fv("u_alphaSizeFovBr", alphaSizeFovBr, 0, 4);

                            // Days since epoch
                            shaderProgram.setUniformi("u_t", (int) (AstroUtils.getMsSince(GaiaSky.instance.time.getTime(), starGroup.getEpoch()) * Nature.MS_TO_D));
                            shaderProgram.setUniformf("u_ar", GlobalConf.program.isStereoHalfWidth() ? 2f : 1f);
                            shaderProgram.setUniformf("u_thAnglePoint", (float) 1e-8);

                            // Update projection if fovmode is 3
                            if (fovMode == 3) {
                                // Cam is Fov1 & Fov2
                                FovCamera cam = ((CameraManager) camera).fovCamera;
                                // Update combined
                                PerspectiveCamera[] cams = camera.getFrontCameras();
                                shaderProgram.setUniformMatrix("u_projModelView", cams[cam.dirindex].combined);
                            }
                            try {
                                curr.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                            } catch (IllegalArgumentException e) {
                                logger.error("Render exception");
                            }
                        }
                    }
                }
            }
            shaderProgram.end();
        }
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attributes = new Array<>();
        attributes.add(new VertexAttribute(Usage.Position, 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.Tangent, 3, "a_pm"));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.Generic, 2, "a_sizeMag"));

        VertexAttribute[] array = new VertexAttribute[attributes.size];
        for (int i = 0; i < attributes.size; i++)
            array[i] = attributes.get(i);
        return array;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case STAR_MIN_OPACITY_CMD:
            pointAlpha[0] = (float) data[0];
            pointAlpha[1] = (float) data[0] + GlobalConf.scene.POINT_ALPHA_MAX;
            for (ExtShaderProgram p : programs) {
                if (p != null && p.isCompiled()) {
                    Gdx.app.postRunnable(() -> {
                        p.begin();
                        p.setUniform2fv("u_pointAlpha", pointAlpha, 0, 2);
                        p.end();
                    });
                }
            }
            break;
        case DISPOSE_STAR_GROUP_GPU_MESH:
            Integer meshIdx = (Integer) data[0];
            clearMeshData(meshIdx);
            break;
        default:
            break;
        }
    }

}
