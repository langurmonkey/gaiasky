/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.IRenderable;
import gaiasky.scenegraph.ParticleGroup;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.SceneGraphNode.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.comp.DistToCameraComparator;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.StdRandom;
import org.lwjgl.opengl.GL30;

import java.util.Random;

public class ParticleGroupRenderSystem extends ImmediateRenderSystem implements IObserver {
    Vector3 aux1;
    int additionalOffset, pmOffset;
    Random rand;

    public ParticleGroupRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        comp = new DistToCameraComparator<>();
        rand = new Random(123);
        aux1 = new Vector3();
        EventManager.instance.subscribe(this, Events.DISPOSE_PARTICLE_GROUP_GPU_MESH);
    }

    @Override
    protected void initShaderProgram() {
        Gdx.gl.glEnable(GL30.GL_POINT_SPRITE);
        Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);
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

        VertexAttribute[] attribs = buildVertexAttributes();
        curr.mesh = new IntMesh(false, nVertices, 0, attribs);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        pmOffset = curr.mesh.getVertexAttribute(Usage.Tangent) != null ? curr.mesh.getVertexAttribute(Usage.Tangent).offset / 4 : 0;
        additionalOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
        return mdi;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            ExtShaderProgram shaderProgram = getShaderProgram();
            shaderProgram.begin();
            for (IRenderable renderable : renderables) {
                ParticleGroup particleGroup = (ParticleGroup) renderable;
                /**
                 * GROUP RENDER
                 */
                if (!particleGroup.inGpu()) {
                    particleGroup.offset = addMeshData(particleGroup.size());
                    curr = meshes.get(particleGroup.offset);

                    float[] c = particleGroup.getColor();
                    float[] cmin = particleGroup.getColorMin();
                    float[] cmax = particleGroup.getColorMax();
                    double dmin = particleGroup.getMinDistance();
                    double dmax = particleGroup.getMaxDistance();

                    ensureTempVertsSize(particleGroup.size() * curr.vertexSize);
                    int n = particleGroup.data().size;
                    int nadded = 0;
                    for (int i = 0; i < n; i++) {
                        if(particleGroup.filter(i)) {
                            ParticleBean pb = particleGroup.get(i);
                            double[] p = pb.data;
                            // COLOR
                            if(particleGroup.isHighlighted()){
                                tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(c[0], c[1], c[2], c[3]);
                            }else {
                                if (cmin != null && cmax != null) {
                                    double dist = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
                                    // fac = 0 -> cmin,  fac = 1 -> cmax
                                    double fac = (dist - dmin) / (dmax - dmin);
                                    interpolateColor(cmin, cmax, c, fac);
                                }
                                float r = 0, g = 0, b = 0;
                                if (particleGroup.colorNoise != 0) {
                                    r = (float) ((StdRandom.uniform() - 0.5) * 2.0 * particleGroup.colorNoise);
                                    g = (float) ((StdRandom.uniform() - 0.5) * 2.0 * particleGroup.colorNoise);
                                    b = (float) ((StdRandom.uniform() - 0.5) * 2.0 * particleGroup.colorNoise);
                                }
                                tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(MathUtils.clamp(c[0] + r, 0, 1), MathUtils.clamp(c[1] + g, 0, 1), MathUtils.clamp(c[2] + b, 0, 1), MathUtils.clamp(c[3], 0, 1));
                            }

                            // SIZE
                            tempVerts[curr.vertexIdx + additionalOffset] = (particleGroup.size + (float) (rand.nextGaussian() * particleGroup.size / 4d)) * particleGroup.highlightedSizeFactor();

                            // POSITION
                            final int idx = curr.vertexIdx;
                            tempVerts[idx] = (float) p[0];
                            tempVerts[idx + 1] = (float) p[1];
                            tempVerts[idx + 2] = (float) p[2];

                            curr.vertexIdx += curr.vertexSize;
                            nadded++;
                        }
                    }
                    particleGroup.count = nadded * curr.vertexSize;
                    curr.mesh.setVertices(tempVerts, 0, particleGroup.count);

                    particleGroup.inGpu(true);

                }

                curr = meshes.get(particleGroup.offset);
                if (curr != null) {
                    boolean stereoHalfWidth = GlobalConf.program.isStereoHalfWidth();
                    float meanDist = (float) (particleGroup.getMeanDistance());

                    shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
                    shaderProgram.setUniformf("u_alpha", alphas[particleGroup.ct.getFirstOrdinal()] * particleGroup.getOpacity());
                    shaderProgram.setUniformf("u_ar", stereoHalfWidth ? 2f : 1f);
                    shaderProgram.setUniformf("u_falloff", particleGroup.profileDecay);
                    shaderProgram.setUniformf("u_sizeFactor", (float) ((((stereoHalfWidth ? 2.0 : 1.0) * rc.scaleFactor * GlobalConf.scene.STAR_POINT_SIZE)) * particleGroup.highlightedSizeFactor() * meanDist * 0.15 / (camera.getFovFactor() * Constants.DISTANCE_SCALE_FACTOR)));
                    shaderProgram.setUniformf("u_minSize", (float) (3.0 / (camera.getFovFactor() * Constants.DISTANCE_SCALE_FACTOR)));
                    shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux1));
                    shaderProgram.setUniformf("u_camDir", camera.getCurrent().getCamera().direction);
                    shaderProgram.setUniformi("u_cubemap", GlobalConf.program.CUBEMAP360_MODE ? 1 : 0);

                    // Rel, grav, z-buffer
                    addEffectsUniforms(shaderProgram, camera);

                    curr.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                }
            }
            shaderProgram.end();
        }
    }

    private void interpolateColor(float[] c0, float[] c1, float[] result, double factor) {
        float f = (float) factor;
        result[0] = (1 - f) * c0[0] + f * c1[0];
        result[1] = (1 - f) * c0[1] + f * c1[1];
        result[2] = (1 - f) * c0[2] + f * c1[2];
        result[3] = (1 - f) * c0[3] + f * c1[3];
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 1, "a_size"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case DISPOSE_PARTICLE_GROUP_GPU_MESH:
            Integer meshIdx = (Integer) data[0];
            clearMeshData(meshIdx);
            break;
        default:
            break;
        }
    }

}
