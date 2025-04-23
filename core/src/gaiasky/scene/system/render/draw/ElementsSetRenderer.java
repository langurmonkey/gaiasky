/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
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
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.record.OrbitComponent;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.CatalogInfo;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Matrix4d;
import net.jafama.FastMath;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ElementsSetRenderer extends PointCloudTriRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(ElementsSetRenderer.class);

    private final Vector3 aux1;
    private final Matrix4 aux, refSysTransformF;
    private int posOffset;
    private int uvOffset;
    private int elems01Offset;
    private int elems02Offset;
    private int sizeOffset;
    private int textureIndexOffset;
    private final double[] particleSizeLimits = new double[]{Math.tan(Math.toRadians(0.075)), FastMath.tan(Math.toRadians(0.9))};

    public ElementsSetRenderer(SceneRenderer sceneRenderer,
                               RenderGroup rg,
                               float[] alphas,
                               ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        aux1 = new Vector3();
        aux = new Matrix4();
        refSysTransformF = new Matrix4();
        EventManager.instance.subscribe(this, Event.GPU_DISPOSE_ORBITAL_ELEMENTS);
    }

    @Override
    protected void addVertexAttributes(Array<VertexAttribute> attributes) {
        attributes.add(new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.OrbitElems1, 4, "a_orbitelems01"));
        attributes.add(new VertexAttribute(OwnUsage.OrbitElems2, 4, "a_orbitelems02"));
        attributes.add(new VertexAttribute(OwnUsage.Size, 1, "a_size"));
        attributes.add(new VertexAttribute(OwnUsage.TextureIndex, 1, "a_textureIndex"));
    }

    @Override
    protected void offsets(MeshData curr) {
        posOffset = curr.mesh.getVertexAttribute(Usage.Position) != null ? curr.mesh.getVertexAttribute(Usage.Position).offset / 4 : 0;
        uvOffset = curr.mesh.getVertexAttribute(Usage.TextureCoordinates) != null ? curr.mesh.getVertexAttribute(Usage.TextureCoordinates).offset / 4 : 0;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        elems01Offset = curr.mesh.getVertexAttribute(OwnUsage.OrbitElems1) != null ? curr.mesh.getVertexAttribute(OwnUsage.OrbitElems1).offset / 4 : 0;
        elems02Offset = curr.mesh.getVertexAttribute(OwnUsage.OrbitElems2) != null ? curr.mesh.getVertexAttribute(OwnUsage.OrbitElems2).offset / 4 : 0;
        sizeOffset = curr.mesh.getVertexAttribute(OwnUsage.Size) != null ? curr.mesh.getVertexAttribute(OwnUsage.Size).offset / 4 : 0;
        textureIndexOffset = curr.mesh.getVertexAttribute(OwnUsage.TextureIndex) != null ? curr.mesh.getVertexAttribute(OwnUsage.TextureIndex).offset / 4 : 0;
    }

    @Override
    public void renderStud(List<IRenderable> renderables,
                           ICamera camera,
                           double t) {
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

                        // Respect body representation in trajectory.
                        if (trajectory.bodyRepresentation.isBody()) {

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
                                float[] c = hl.isHighlighted() && ci != null ? ci.getHlColor() : trajectory.bodyColor;
                                tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(c[0], c[1], c[2], c[3]);

                                // ORBIT ELEMENTS 01
                                tempVerts[curr.vertexIdx + elems01Offset] = (float) oc.period;
                                tempVerts[curr.vertexIdx + elems01Offset + 1] = (float) oc.epoch;
                                tempVerts[curr.vertexIdx + elems01Offset + 2] = (float) (oc.semiMajorAxis);
                                tempVerts[curr.vertexIdx + elems01Offset + 3] = (float) oc.e;

                                // ORBIT ELEMENTS 02
                                tempVerts[curr.vertexIdx + elems02Offset] = (float) (oc.i * MathUtilsDouble.degRad);
                                tempVerts[curr.vertexIdx + elems02Offset + 1] = (float) (oc.ascendingNode * MathUtilsDouble.degRad);
                                tempVerts[curr.vertexIdx + elems02Offset + 2] = (float) (oc.argOfPericenter * MathUtilsDouble.degRad);
                                tempVerts[curr.vertexIdx + elems02Offset + 3] = (float) (oc.meanAnomaly * MathUtilsDouble.degRad);

                                // SIZE
                                tempVerts[curr.vertexIdx + sizeOffset] = trajectory.pointSize * (hl.isHighlighted() && ci != null ? ci.hlSizeFactor : 1);

                                // TEXTURE INDEX
                                tempVerts[curr.vertexIdx + textureIndexOffset] = -1f;

                                curr.vertexIdx += curr.vertexSize;
                                curr.numVertices++;
                                numVerticesAdded.incrementAndGet();
                            }
                            // Indices
                            quadIndices(curr);
                            numParticlesAdded.incrementAndGet();

                            setInGpu(render, true);
                        }
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
                var hl = Mapper.highlight.get(render.entity);

                ExtShaderProgram shaderProgram = getShaderProgram();

                shaderProgram.begin();
                shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
                shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux1));
                addCameraUpCubemapMode(shaderProgram, camera);
                shaderProgram.setUniformf("u_alpha", alphas[renderable.getComponentType().getFirstOrdinal()] * renderable.getOpacity());
                shaderProgram.setUniformf("u_falloff", 2.5f);
                shaderProgram.setUniformf("u_sizeFactor", Settings.settings.scene.star.pointSize * 0.1f * hl.pointscaling);
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
                if (graph.children != null && graph.children.size > 0) {
                    if (Mapper.transform.has(graph.children.get(0))) {
                        refSysTransform = Mapper.transform.get(graph.children.get(0)).matrix;
                    }
                }
                if (refSysTransform != null
                        && graph.children != null && graph.children.size > 0
                        && Mapper.trajectory.has(graph.children.get(0))
                        && Mapper.trajectory.get(graph.children.get(0)).model.isExtrasolar()) {
                    refSysTransform.putIn(aux).inv();
                    refSysTransformF.setToRotation(0, 1, 0, -90).mul(aux);
                } else if (refSysTransform != null) {
                    refSysTransform.putIn(refSysTransformF).inv();
                } else {
                    refSysTransformF.idt();
                }
                shaderProgram.setUniformMatrix("u_refSysTransform", refSysTransformF);


                // Affine transformations.
                addAffineTransformUniforms(shaderProgram, Mapper.affine.get(render.entity));

                // Relativistic effects.
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
    public void notify(final Event event,
                       Object source,
                       final Object... data) {
        if (event.equals(Event.GPU_DISPOSE_ORBITAL_ELEMENTS)) {
            if (source instanceof Render) {
                var render = (Render) source;
                int offset = getOffset(render);
                if (offset >= 0) {
                    clearMeshData(offset);
                }
                setOffset(render, -1);
                setInGpu(render, false);
            }
        }
    }
}
