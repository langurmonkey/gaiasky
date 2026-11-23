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
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.coord.AstroUtils;
import gaiasky.render.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Render system for orbital elements and other line-based trajectories.
 */
public class ElementsRenderer extends PointCloudTriRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(ElementsRenderer.class);

    private final Vector3 aux1;
    private final Matrix4D refSysTransformD, auxTransformD;
    private int posOffset;
    private int uvOffset;
    private int elemPosOffset;
    private int sizeOffset;
    private int textureIndexOffset;
    private boolean forceAdd = false;
    private final double[] particleSizeLimits = new double[]{Math.tan(Math.toRadians(0.05)), FastMath.tan(Math.toRadians(1.0))};

    /** Elements of the mesh currently in the GPU, in vertex order. **/
    private Object[] src;
    private int srcCount;
    /** Compiled orbits, with the constants and the state of the Kepler solve. **/
    private final ElementPositions elements = new ElementPositions();
    /** Dataset-frame positions, three doubles per element. **/

    /** Copy of the vertex data of the mesh in the GPU, whose positions are rewritten every frame. **/
    private float[] verts;
    private int vertsCount;
    private double lastTjd = Double.NaN;

    public ElementsRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        aux1 = new Vector3();
        refSysTransformD = new Matrix4D();
        auxTransformD = new Matrix4D();
        EventManager.instance.subscribe(this, Event.GPU_DISPOSE_ORBITAL_ELEMENTS);
    }

    @Override
    protected void addVertexAttributes(Array<VertexAttribute> attributes) {
        attributes.add(new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        // Camera-relative position, computed on the CPU in double precision.
        attributes.add(new VertexAttribute(OwnUsage.OrbitPos, 3, "a_pos"));
        attributes.add(new VertexAttribute(OwnUsage.Size, 1, "a_size"));
        attributes.add(new VertexAttribute(OwnUsage.TextureIndex, 1, "a_textureIndex"));
    }

    @Override
    protected void offsets(MeshData curr) {
        posOffset = curr.mesh.getVertexAttribute(Usage.Position) != null ? curr.mesh.getVertexAttribute(Usage.Position).offset / 4 : 0;
        uvOffset = curr.mesh.getVertexAttribute(Usage.TextureCoordinates) != null ? curr.mesh.getVertexAttribute(Usage.TextureCoordinates).offset / 4 : 0;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        elemPosOffset = curr.mesh.getVertexAttribute(OwnUsage.OrbitPos) != null ? curr.mesh.getVertexAttribute(OwnUsage.OrbitPos).offset / 4 : 0;
        sizeOffset = curr.mesh.getVertexAttribute(OwnUsage.Size) != null ? curr.mesh.getVertexAttribute(OwnUsage.Size).offset / 4 : 0;
        textureIndexOffset = curr.mesh.getVertexAttribute(OwnUsage.TextureIndex) != null ? curr.mesh.getVertexAttribute(OwnUsage.TextureIndex).offset / 4 : 0;
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        int n = renderables.size();
        if (n > 0 && renderables.getFirst().getOpacity() > 0) {
            Render first = (Render) renderables.getFirst();
            var base = Mapper.base.get(first.entity);

            if (forceAdd || !inGpu(first)) {
                forceAdd = false;
                curr = meshes.get(addMeshData(n * 4, n * 6));

                ensureTempVertsSize(n * 4 * curr.vertexSize);
                ensureTempIndicesSize(n * 6);

                AtomicInteger numVerticesAdded = new AtomicInteger(0);
                AtomicInteger numParticlesAdded = new AtomicInteger(0);
                Object[] srcArr = new Object[n];
                renderables.forEach(renderable -> {
                    Render render = (Render) renderable;

                    if (!inGpu(render)) {
                        var trajectory = Mapper.trajectory.get(render.entity);

                        // Respect body representation in trajectory.
                        if (trajectory.bodyRepresentation.isBody()) {

                            OrbitComponent oc = trajectory.oc;

                            // Elements, kept for the per-frame position computation.
                            srcArr[numParticlesAdded.get()] = oc;

                            // 4 vertices per particle
                            for (int vert = 0; vert < 4; vert++) {
                                // Vertex POSITION
                                tempVerts[curr.vertexIdx + posOffset] = vertPos[vert].getFirst();
                                tempVerts[curr.vertexIdx + posOffset + 1] = vertPos[vert].getSecond();

                                // UV coordinates
                                tempVerts[curr.vertexIdx + uvOffset] = vertUV[vert].getFirst();
                                tempVerts[curr.vertexIdx + uvOffset + 1] = vertUV[vert].getSecond();

                                // COLOR
                                tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(trajectory.bodyColor[0], trajectory.bodyColor[1], trajectory.bodyColor[2], trajectory.bodyColor[3]);

                                // SIZE
                                tempVerts[curr.vertexIdx + sizeOffset] = trajectory.pointSize;

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
                curr.mesh.setVertices(tempVerts, 0, count);
                curr.mesh.setIndices(tempIndices, 0, numParticlesAdded.get() * 6);

                // Keep everything needed to rewrite the positions every frame.
                src = srcArr;
                srcCount = numParticlesAdded.get();
                // Prepare the orbital elements once: the conversion to radians and the perifocal
                // rotation do not depend on time.
                elements.compile(src, srcCount);
                if (verts == null || verts.length < count) {
                    verts = new float[count];
                }
                System.arraycopy(tempVerts, 0, verts, 0, count);
                vertsCount = count;
                lastTjd = Double.NaN;
            }

            if (curr != null) {
                ExtShaderProgram shaderProgram = getShaderProgram();

                // Element positions, in double precision, on the CPU.
                updatePositions(first.entity, AstroUtils.getJulianDate(GaiaSky.instance.time.getTime()));

                shaderProgram.begin();
                shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
                shaderProgram.setUniformf("u_camPos", camera.getPos());
                addCameraUpCubemapMode(shaderProgram, camera);
                shaderProgram.setUniformf("u_alpha", alphas[base.ct.getFirstOrdinal()] * first.getOpacity());
                shaderProgram.setUniformf("u_falloff", 2.5f);
                shaderProgram.setUniformf("u_sizeFactor", GaiaSky.settings().scene.star.pointSize * 0.08f);
                shaderProgram.setUniformf("u_sizeLimits", (float) (particleSizeLimits[0]), (float) (particleSizeLimits[1]));

                // VR scale
                shaderProgram.setUniformf("u_vrScale", (float) Constants.DISTANCE_SCALE_FACTOR);

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

    /**
     * Recomputes the camera-relative position of every element of the current mesh in double
     * precision and re-uploads the vertex buffer. The Kepler solve is skipped while time stands still.
     */
    private void updatePositions(Entity entity, double tjd) {
        if (verts == null || srcCount == 0) {
            return;
        }

        // Reference system transform, in double precision.
        var trans = Mapper.transform.get(entity);
        Matrix4D refSys = trans != null ? trans.matrix : null;
        boolean idt = refSys == null;
        if (!idt) {
            var trajectory = Mapper.trajectory.get(entity);
            if (trajectory != null && trajectory.model.isExtrasolar()) {
                // Extrasolar elements require a phase around Y.
                refSysTransformD.setToRotation(0, 1, 0, -90).mul(auxTransformD.set(refSys).inv());
            } else {
                refSysTransformD.set(refSys).inv();
            }
        }

        if (tjd != lastTjd) {
            // Solve the orbits and write the positions in the same pass.
            elements.compute(tjd, verts, curr.vertexSize, elemPosOffset, 4, idt ? null : refSysTransformD, null, null);
            lastTjd = tjd;
        } else if (!idt) {
            // Same time, but the transform may have moved with its parent: reuse the orbits.
            elements.project(verts, curr.vertexSize, elemPosOffset, 4, refSysTransformD, null, null);
        } else {
            return;
        }
        curr.mesh.setVertices(verts, 0, vertsCount);
    }

    public void reset() {
        resetMeshes();
        curr = null;
        forceAdd = true;
        src = null;
        srcCount = 0;

        elements.reset();
        verts = null;
        vertsCount = 0;
        lastTjd = Double.NaN;
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event.equals(Event.GPU_DISPOSE_ORBITAL_ELEMENTS)) {
            if (source instanceof Render || source == null)
                GaiaSky.postRunnable(this::reset);
        }
    }
}
