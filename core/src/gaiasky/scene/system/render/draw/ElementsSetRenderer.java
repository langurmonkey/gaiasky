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
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.InstancedRenderSystem;
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
import gaiasky.util.math.Matrix4D;
import net.jafama.FastMath;
import org.lwjgl.opengl.GL41;

import java.util.List;

public class ElementsSetRenderer extends InstancedRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(ElementsSetRenderer.class);

    private final Matrix4 aux, refSysTransformF;
    private final double[] particleSizeLimits = new double[]{Math.tan(Math.toRadians(0.075)), FastMath.tan(Math.toRadians(0.9))};

    public ElementsSetRenderer(SceneRenderer sceneRenderer,
                               RenderGroup rg,
                               float[] alphas,
                               ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        aux = new Matrix4();
        refSysTransformF = new Matrix4();
        EventManager.instance.subscribe(this, Event.GPU_DISPOSE_ORBITAL_ELEMENTS);
    }

    @Override
    protected void addAttributesDivisor1(Array<VertexAttribute> attributes, int primitive) {
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.OrbitElems1, 4, "a_orbitelems01"));
        attributes.add(new VertexAttribute(OwnUsage.OrbitElems2, 4, "a_orbitelems02"));
        attributes.add(new VertexAttribute(OwnUsage.Size, 1, "a_size"));
        attributes.add(new VertexAttribute(OwnUsage.TextureIndex, 1, "a_textureIndex"));
    }

    @Override
    protected void offsets0(MeshData curr, InstancedModel model) {
        // Unused
    }

    @Override
    protected void offsets1(MeshData curr, InstancedModel model) {
        curr.colorOffset = curr.mesh.getInstancedAttribute(Usage.ColorPacked) != null ?
                curr.mesh.getInstancedAttribute(Usage.ColorPacked).offset / 4 : 0;
        model.elems01Offset = curr.mesh.getInstancedAttribute(OwnUsage.OrbitElems1) != null ?
                curr.mesh.getInstancedAttribute(OwnUsage.OrbitElems1).offset / 4 : 0;
        model.elems02Offset = curr.mesh.getInstancedAttribute(OwnUsage.OrbitElems2) != null ?
                curr.mesh.getInstancedAttribute(OwnUsage.OrbitElems2).offset / 4 : 0;
        model.sizeOffset = curr.mesh.getInstancedAttribute(OwnUsage.Size) != null ?
                curr.mesh.getInstancedAttribute(OwnUsage.Size).offset / 4 : 0;
        model.textureIndexOffset = curr.mesh.getInstancedAttribute(OwnUsage.TextureIndex) != null ?
                curr.mesh.getInstancedAttribute(OwnUsage.TextureIndex).offset / 4 : 0;
    }


    @Override
    public void renderStud(List<IRenderable> renderables,
                           ICamera camera,
                           double t) {
        final var primitive = GL41.GL_TRIANGLES;
        for (IRenderable renderable : renderables) {
            Render render = (Render) renderable;
            var graph = Mapper.graph.get(render.entity);

            var model = getModelQuad(primitive,
                                     getOffset(render));
            if (!inGpu(render) && graph.children != null && graph.children.size > 0) {
                int n = graph.children.size;
                int offset = addMeshData(model,
                                         model.numVertices,
                                         n,
                                         model.numIndices,
                                         "quad",
                                         primitive);
                setModel(offset, model);
                setOffset(render, offset);
                curr = meshes.get(offset);
                model.ensureInstanceAttribsSize(n * curr.instanceSize);

                int numParticlesAdded = 0;

                var desc = Mapper.datasetDescription.get(render.entity);
                var hl = Mapper.highlight.get(render.entity);

                CatalogInfo ci = desc.catalogInfo;
                Array<Entity> children = graph.children;
                for (var child : children) {
                    if (Mapper.trajectory.has(child)) {
                        var trajectory = Mapper.trajectory.get(child);

                        // Respect body representation in trajectory.
                        if (trajectory.bodyRepresentation.isBody()) {

                            OrbitComponent oc = trajectory.oc;
                            // COLOR
                            float[] c = hl.isHighlighted() && ci != null ? ci.getHlColor() : trajectory.bodyColor;
                            model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits(c[0], c[1], c[2], c[3]);

                            // ORBIT ELEMENTS 01
                            model.instanceAttributes[curr.instanceIdx + model.elems01Offset] = (float) oc.period;
                            model.instanceAttributes[curr.instanceIdx + model.elems01Offset + 1] = (float) oc.epoch;
                            model.instanceAttributes[curr.instanceIdx + model.elems01Offset + 2] = (float) (oc.semiMajorAxis);
                            model.instanceAttributes[curr.instanceIdx + model.elems01Offset + 3] = (float) oc.e;

                            // ORBIT ELEMENTS 02
                            model.instanceAttributes[curr.instanceIdx + model.elems02Offset] = (float) (oc.i * MathUtilsDouble.degRad);
                            model.instanceAttributes[curr.instanceIdx + model.elems02Offset + 1] = (float) (oc.ascendingNode * MathUtilsDouble.degRad);
                            model.instanceAttributes[curr.instanceIdx + model.elems02Offset + 2] = (float) (oc.argOfPericenter * MathUtilsDouble.degRad);
                            model.instanceAttributes[curr.instanceIdx + model.elems02Offset + 3] = (float) (oc.meanAnomaly * MathUtilsDouble.degRad);

                            // SIZE
                            model.instanceAttributes[curr.instanceIdx + model.sizeOffset] = trajectory.pointSize * (hl.isHighlighted() && ci != null ? ci.hlSizeFactor : 1);

                            // TEXTURE INDEX
                            model.instanceAttributes[curr.instanceIdx + model.textureIndexOffset] = -1f;

                            curr.instanceIdx += curr.instanceSize;
                            numParticlesAdded++;
                        }
                    }
                }
                // Global (divisor=0) vertices (position, uv?) plus optional indices
                curr.mesh.setVertices(model.vertices, 0, model.numVertices * model.modelVertexSize);
                if (model.numIndices > 0) {
                    curr.mesh.setIndices(model.indices, 0, model.numIndices);
                }
                // Per instance (divisor=1) vertices
                int count = numParticlesAdded * curr.instanceSize;
                setCount(render, numParticlesAdded);
                curr.mesh.setInstanceAttribs(model.instanceAttributes, 0, count);
                model.instanceAttributes = null;

                setInGpu(render, true);
            }

            /*
             * RENDER
             */
            curr = meshes.get(getOffset(render));
            if (curr != null) {
                var hl = Mapper.highlight.get(render.entity);

                ExtShaderProgram shaderProgram = getShaderProgram();
                shaderProgram.begin();

                shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
                shaderProgram.setUniformf("u_camPos", camera.getPos());
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
                Matrix4D refSysTransform = null;
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
                    int count = curr.mesh.getNumIndices() > 0 ? curr.mesh.getNumIndices() : curr.mesh.getNumVertices();
                    curr.mesh.render(shaderProgram, primitive, 0, count, getCount(render));
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
            if (source instanceof Render render) {
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
