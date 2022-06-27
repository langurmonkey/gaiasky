/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.PointCloudTriRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Render;
import gaiasky.scenegraph.Orbit;
import gaiasky.scenegraph.OrbitalElementsGroup;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.OrbitComponent;
import gaiasky.util.CatalogInfo;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Matrix4d;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Renders orbital elements groups as a whole.
 */
public class ElementsSetRenderer extends PointCloudTriRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(ElementsSetRenderer.class);

    private final Vector3 aux1;
    private final Matrix4 aux, refSysTransformF;
    private int posOffset;
    private int uvOffset;
    private int elems01Offset;
    private int elems02Offset;
    private int sizeOffset;
    private double[] particleSizeLimits = new double[] { Math.tan(Math.toRadians(0.025)), Math.tan(Math.toRadians(0.9)) };

    public ElementsSetRenderer(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        aux1 = new Vector3();
        aux = new Matrix4();
        refSysTransformF = new Matrix4();
        EventManager.instance.subscribe(this, Event.GPU_DISPOSE_ORBITAL_ELEMENTS);
    }

    @Override
    protected void initShaderProgram() {
    }

    @Override
    protected void addVertexAttributes(Array<VertexAttribute> attributes) {
        attributes.add(new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.OrbitElems1, 4, "a_orbitelems01"));
        attributes.add(new VertexAttribute(OwnUsage.OrbitElems2, 4, "a_orbitelems02"));
        attributes.add(new VertexAttribute(OwnUsage.Size, 1, "a_size"));
    }

    @Override
    protected void offsets(MeshData curr) {
        posOffset = curr.mesh.getVertexAttribute(Usage.Position) != null ? curr.mesh.getVertexAttribute(Usage.Position).offset / 4 : 0;
        uvOffset = curr.mesh.getVertexAttribute(Usage.TextureCoordinates) != null ? curr.mesh.getVertexAttribute(Usage.TextureCoordinates).offset / 4 : 0;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        elems01Offset = curr.mesh.getVertexAttribute(OwnUsage.OrbitElems1) != null ? curr.mesh.getVertexAttribute(OwnUsage.OrbitElems1).offset / 4 : 0;
        elems02Offset = curr.mesh.getVertexAttribute(OwnUsage.OrbitElems2) != null ? curr.mesh.getVertexAttribute(OwnUsage.OrbitElems2).offset / 4 : 0;
        sizeOffset = curr.mesh.getVertexAttribute(OwnUsage.Size) != null ? curr.mesh.getVertexAttribute(OwnUsage.Size).offset / 4 : 0;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        for (IRenderable renderable : renderables) {
            Render render = (Render) renderable;
            var graph = Mapper.graph.get(render.entity);

            if (!inGpu(render) && graph.children != null && graph.children.size > 0) {
                int n = graph.children.size;
                int offset = addMeshData(n * 4, n * 6);
                setOffset(render, offset);
                curr = meshes.get(offset);

                ensureTempVertsSize(n * 4 * curr.vertexSize);
                ensureTempIndicesSize(n * 6);

                AtomicInteger numVerticesAdded = new AtomicInteger(0);
                AtomicInteger numParticlesAdded = new AtomicInteger(0);

                var desc = Mapper.datasetDescription.get(render.entity);
                var hl = Mapper.highlight.get(render.entity);

                CatalogInfo ci = desc.catalogInfo;
                Array<Entity> children = graph.children;
                children.forEach(child -> {
                    if (Mapper.trajectory.has(child)) {
                        var trajectory = Mapper.trajectory.get(child);

                        OrbitComponent oc = trajectory.oc;

                        // 4 vertices per particle
                        for (int vert = 0; vert < 4; vert++) {
                            // Vertex POSITION
                            tempVerts[curr.vertexIdx + posOffset] = vertPos[vert].getFirst();
                            tempVerts[curr.vertexIdx + posOffset + 1] = vertPos[vert].getSecond();

                            // UV coordinates
                            tempVerts[curr.vertexIdx + uvOffset] = vertUV[vert].getFirst();
                            tempVerts[curr.vertexIdx + uvOffset + 1] = vertUV[vert].getSecond();

                            // COLOR
                            float[] c = hl.isHighlighted() && ci != null ? ci.getHlColor() : trajectory.pointColor;
                            tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(c[0], c[1], c[2], c[3]);

                            // ORBIT ELEMENTS 01
                            tempVerts[curr.vertexIdx + elems01Offset] = (float) Math.sqrt(oc.mu / Math.pow(oc.semimajoraxis * 1000d, 3d));
                            tempVerts[curr.vertexIdx + elems01Offset + 1] = (float) oc.epoch;
                            tempVerts[curr.vertexIdx + elems01Offset + 2] = (float) (oc.semimajoraxis * 1000d); // In metres
                            tempVerts[curr.vertexIdx + elems01Offset + 3] = (float) oc.e;

                            // ORBIT ELEMENTS 02
                            tempVerts[curr.vertexIdx + elems02Offset] = (float) (oc.i * MathUtilsd.degRad);
                            tempVerts[curr.vertexIdx + elems02Offset + 1] = (float) (oc.ascendingnode * MathUtilsd.degRad);
                            tempVerts[curr.vertexIdx + elems02Offset + 2] = (float) (oc.argofpericenter * MathUtilsd.degRad);
                            tempVerts[curr.vertexIdx + elems02Offset + 3] = (float) (oc.meananomaly * MathUtilsd.degRad);

                            // SIZE
                            tempVerts[curr.vertexIdx + sizeOffset] = trajectory.pointSize * (hl.isHighlighted() && ci != null ? ci.hlSizeFactor : 1);

                            curr.vertexIdx += curr.vertexSize;
                            curr.numVertices++;
                            numVerticesAdded.incrementAndGet();
                        }
                        // Indices
                        quadIndices(curr);
                        numParticlesAdded.incrementAndGet();

                        setInGpu(render, true);
                    }
                });
                int count = numVerticesAdded.get() * curr.vertexSize;
                setCount(render, count);
                curr.mesh.setVertices(tempVerts, 0, count);
                curr.mesh.setIndices(tempIndices, 0, numParticlesAdded.get() * 6);

                setInGpu(render, true);
            }

            curr = meshes.get(getOffset(renderable));
            if (curr != null) {
                var set = Mapper.orbitElementsSet.get(render.entity);
                var hl = Mapper.highlight.get(render.entity);

                ExtShaderProgram shaderProgram = getShaderProgram();

                shaderProgram.begin();
                shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
                shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux1));
                shaderProgram.setUniformf("u_alpha", alphas[renderable.getComponentType().getFirstOrdinal()] * renderable.getOpacity());
                shaderProgram.setUniformf("u_falloff", 2.5f);
                shaderProgram.setUniformf("u_sizeFactor", Settings.settings.scene.star.pointSize * 0.08f * hl.pointscaling);
                shaderProgram.setUniformf("u_sizeLimits", (float) (particleSizeLimits[0]), (float) (particleSizeLimits[1]));

                // VR scale
                shaderProgram.setUniformf("u_vrScale", (float) Constants.DISTANCE_SCALE_FACTOR);
                // Emulate double, for compatibility
                double curRt = AstroUtils.getJulianDate(GaiaSky.instance.time.getTime());
                float curRt1 = (float) curRt;
                float curRt2 = (float) (curRt - (double) curRt1);
                shaderProgram.setUniformf("u_t", curRt1, curRt2);

                // Reference system transform
                Matrix4d refSysTransform = null;
                if(graph.children != null && graph.children.size > 0) {
                   refSysTransform = Mapper.transform.get(graph.children.get(0)).matrix;
                }
                if(Mapper.trajectory.get(graph.children.get(0)).model.isExtrasolar()) {
                    refSysTransform.putIn(aux).inv();
                    refSysTransformF.setToRotation(0,1,0,-90).mul(aux);
                } else {
                    refSysTransform.putIn(refSysTransformF).inv();
                }
                shaderProgram.setUniformMatrix("u_refSysTransform", refSysTransformF);

                // Relativistic effects
                addEffectsUniforms(shaderProgram, camera);

                try {
                    curr.mesh.render(shaderProgram, GL20.GL_TRIANGLES);
                } catch (IllegalArgumentException e) {
                    logger.error(e, "Render exception");
                }
                shaderProgram.end();
            }
        }
    }

    public void reset() {
        clearMeshes();
        curr = null;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event.equals(Event.GPU_DISPOSE_ORBITAL_ELEMENTS)) {
            if (source instanceof OrbitalElementsGroup) {
                OrbitalElementsGroup oeg = (OrbitalElementsGroup) source;
                int offset = getOffset(oeg);
                if (offset >= 0) {
                    clearMeshData(offset);
                }
                setOffset(oeg, -1);
                setInGpu(oeg, false);
            }
        }
    }
}
