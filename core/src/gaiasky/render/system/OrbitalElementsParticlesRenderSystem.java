/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.IRenderable;
import gaiasky.scenegraph.Orbit;
import gaiasky.scenegraph.SceneGraphNode.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.OrbitComponent;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsd;
import org.lwjgl.opengl.GL30;

import java.util.List;

public class OrbitalElementsParticlesRenderSystem extends ImmediateRenderSystem implements IObserver {
    private Vector3 aux1;
    private Matrix4 maux;
    private int elems01Offset, elems02Offset, sizeOffset, count;

    public OrbitalElementsParticlesRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        aux1 = new Vector3();
        maux = new Matrix4();
    }

    @Override
    protected void initShaderProgram() {
        Gdx.gl.glEnable(GL30.GL_POINT_SPRITE);
        Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);
    }

    @Override
    protected void initVertices() {
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
        elems01Offset = curr.mesh.getVertexAttribute(Usage.Tangent) != null ? curr.mesh.getVertexAttribute(Usage.Tangent).offset / 4 : 0;
        elems02Offset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
        sizeOffset = curr.mesh.getVertexAttribute(Usage.Normal) != null ? curr.mesh.getVertexAttribute(Usage.Normal).offset / 4 : 0;
        return mdi;
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        int n = renderables.size();
        if (n > 0 && renderables.get(0).getOpacity() > 0) {
            Orbit first = (Orbit) renderables.get(0);
            if (!first.elemsInGpu) {
                curr = meshes.get(addMeshData(n));

                ensureTempVertsSize(n * curr.vertexSize);
                renderables.forEach(renderable ->{
                    Orbit orbitElems = (Orbit) renderable;

                    if (!orbitElems.elemsInGpu) {

                        OrbitComponent oc = orbitElems.oc;

                        // COLOR
                        tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(orbitElems.pointColor[0], orbitElems.pointColor[1], orbitElems.pointColor[2], orbitElems.pointColor[3]);

                        // ORBIT ELEMS 01
                        tempVerts[curr.vertexIdx + elems01Offset + 0] = (float) Math.sqrt(oc.mu / Math.pow(oc.semimajoraxis * 1000d, 3d));
                        tempVerts[curr.vertexIdx + elems01Offset + 1] = (float) oc.epoch;
                        tempVerts[curr.vertexIdx + elems01Offset + 2] = (float) (oc.semimajoraxis * 1000d); // In metres
                        tempVerts[curr.vertexIdx + elems01Offset + 3] = (float) oc.e;

                        // ORBIT ELEMS 02
                        tempVerts[curr.vertexIdx + elems02Offset + 0] = (float) (oc.i * MathUtilsd.degRad);
                        tempVerts[curr.vertexIdx + elems02Offset + 1] = (float) (oc.ascendingnode * MathUtilsd.degRad);
                        tempVerts[curr.vertexIdx + elems02Offset + 2] = (float) (oc.argofpericenter * MathUtilsd.degRad);
                        tempVerts[curr.vertexIdx + elems02Offset + 3] = (float) (oc.meananomaly * MathUtilsd.degRad);

                        // SIZE
                        tempVerts[curr.vertexIdx + sizeOffset] = orbitElems.pointSize;

                        curr.vertexIdx += curr.vertexSize;

                        orbitElems.elemsInGpu = true;
                    }
                });
                count = n * curr.vertexSize;
                curr.mesh.setVertices(tempVerts, 0, count);
            }

            if (curr != null) {
                ExtShaderProgram shaderProgram = getShaderProgram();

                boolean stereoHw = GlobalConf.program.isStereoHalfWidth();

                shaderProgram.begin();
                shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
                shaderProgram.setUniformf("u_alpha", alphas[first.ct.getFirstOrdinal()] * first.getOpacity());
                shaderProgram.setUniformf("u_ar", stereoHw ? 2f : 1f);
                shaderProgram.setUniformf("u_falloff", 2.5f);
                shaderProgram.setUniformf("u_scaleFactor", rc.scaleFactor * 1.5f * (stereoHw ? 2f : 1f));
                shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux1));
                shaderProgram.setUniformf("u_camDir", camera.getCurrent().getCamera().direction);
                shaderProgram.setUniformi("u_cubemap", GlobalConf.program.CUBEMAP_MODE ? 1 : 0);

                shaderProgram.setUniformf("u_size", rc.scaleFactor);
                // VR scale
                shaderProgram.setUniformf("u_vrScale", (float) Constants.DISTANCE_SCALE_FACTOR);
                // Emulate double, for compatibility
                double curRt = AstroUtils.getJulianDate(GaiaSky.instance.time.getTime());
                float curRt1 = (float) curRt;
                float curRt2 = (float) (curRt - (double) curRt1);
                shaderProgram.setUniformf("u_t", curRt1, curRt2);
                shaderProgram.setUniformMatrix("u_eclToEq", maux.setToRotation(0, 1, 0, -90).mul(Coordinates.equatorialToEclipticF()));

                // Relativistic effects
                addEffectsUniforms(shaderProgram, camera);

                curr.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                shaderProgram.end();
            }
        }
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<>();
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Tangent, 4, "a_orbitelems01"));
        attribs.add(new VertexAttribute(Usage.Generic, 4, "a_orbitelems02"));
        attribs.add(new VertexAttribute(Usage.Normal, 1, "a_size"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void notify(final Events event, final Object... data) {
    }
}
