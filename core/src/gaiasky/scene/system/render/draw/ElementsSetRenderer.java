/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
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
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Highlight;
import gaiasky.scene.component.Render;
import gaiasky.scene.record.OrbitComponent;
import gaiasky.scene.record.ParticleKepler;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.CatalogInfo;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.color.Colormap;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Matrix4D;
import net.jafama.FastMath;
import org.lwjgl.opengl.GL41;

import java.util.List;
import java.util.Random;

public class ElementsSetRenderer extends InstancedRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(ElementsSetRenderer.class);

    private final Matrix4 aux, refSysTransformF;
    private final double[] defaultSizeLimits = new double[]{Math.tan(Math.toRadians(0.05)), FastMath.tan(Math.toRadians(9.6))};
    private final Colormap cmap;
    private final Random rand;

    public ElementsSetRenderer(SceneRenderer sceneRenderer,
                               RenderGroup rg,
                               float[] alphas,
                               ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        cmap = new Colormap();
        aux = new Matrix4();
        rand = new Random(123L);
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
            var desc = Mapper.datasetDescription.get(render.entity);
            var hl = Mapper.highlight.get(render.entity);
            var set = Mapper.particleSet.get(render.entity);
            var eSet = Mapper.orbitElementsSet.get(render.entity);


            CatalogInfo ci = desc.catalogInfo;
            if (!inGpu(render)) {
                // Fetch parameters from particle or elements set.
                var pointData = set != null ? set.pointData : eSet.pointData;
                var colorNoise = set != null ? set.colorNoise : eSet.colorNoise;
                var sizeNoise = set != null ? set.sizeNoise : eSet.sizeNoise;
                var textureArray = set != null ? set.textureArray : eSet.textureArray;
                var textureAttribute = set != null ? set.textureAttribute : eSet.textureAttribute;

                rand.setSeed(123L);
                // Check children nodes.
                var body = Mapper.body.get(render.entity);
                float[] colorNoiseContainer = new float[3];
                float[] baseColor = utils.getColor(body, hl);

                int numParticlesAdded = 0;
                if (eSet != null && graph.children != null && graph.children.size > 0) {
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


                    Array<Entity> children = graph.children;
                    for (var child : children) {
                        if (Mapper.trajectory.has(child)) {
                            var base = Mapper.base.get(child);
                            var trajectory = Mapper.trajectory.get(child);

                            // Respect body representation in trajectory.
                            if (trajectory.bodyRepresentation.isBody()) {

                                OrbitComponent oc = trajectory.oc;
                                // COLOR
                                if (hl.isHighlighted()) {
                                    // Do not apply noise to base color.
                                    model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits(baseColor[0],
                                                                                                                      baseColor[1],
                                                                                                                      baseColor[2],
                                                                                                                      baseColor[3]);
                                } else {
                                    var c = trajectory.bodyColor;
                                    generateChannelNoise(base.id, colorNoise, colorNoiseContainer);
                                    model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits(
                                            MathUtils.clamp(c[0] + colorNoiseContainer[0], 0, 1),
                                            MathUtils.clamp(c[1] + colorNoiseContainer[1], 0, 1),
                                            MathUtils.clamp(c[2] + colorNoiseContainer[2], 0, 1),
                                            MathUtils.clamp(c[3], 0, 1));
                                }

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
                                var size = (body.size + (float) (rand.nextGaussian() * body.size * sizeNoise));
                                model.instanceAttributes[curr.instanceIdx + model.sizeOffset] = trajectory.pointSize * (hl.isHighlighted() && ci != null ? ci.hlSizeFactor : size);

                                // TEXTURE INDEX
                                float textureIndex = computeTextureIndex(null, rand, textureArray, textureAttribute);
                                model.instanceAttributes[curr.instanceIdx + model.textureIndexOffset] = textureIndex;

                                curr.instanceIdx += curr.instanceSize;
                                numParticlesAdded++;
                            }
                        }
                    }
                }
                // Check own list.
                if (pointData != null && !pointData.isEmpty()) {
                    int n = pointData.size();
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

                    for (int i = 0; i < n; i++) {
                        if (set == null || utils.filter(i, set, desc) && set.isVisible(i)) {
                            var p = pointData.get(i);
                            var k = (ParticleKepler) p;
                            // COLOR
                            setParticleColorAttributes(k, hl, baseColor, colorNoise, colorNoiseContainer, model);

                            // ORBIT ELEMENTS 01
                            model.instanceAttributes[curr.instanceIdx + model.elems01Offset] = (float) k.period();
                            model.instanceAttributes[curr.instanceIdx + model.elems01Offset + 1] = (float) k.epoch();
                            model.instanceAttributes[curr.instanceIdx + model.elems01Offset + 2] = (float) k.semiMajorAxis();
                            model.instanceAttributes[curr.instanceIdx + model.elems01Offset + 3] = (float) k.eccentricity();

                            // ORBIT ELEMENTS 02
                            model.instanceAttributes[curr.instanceIdx + model.elems02Offset] = (float) (k.inclination() * MathUtilsDouble.degRad);
                            model.instanceAttributes[curr.instanceIdx + model.elems02Offset + 1] = (float) (k.ascendingNode() * MathUtilsDouble.degRad);
                            model.instanceAttributes[curr.instanceIdx + model.elems02Offset + 2] = (float) (k.argOfPericenter() * MathUtilsDouble.degRad);
                            model.instanceAttributes[curr.instanceIdx + model.elems02Offset + 3] = (float) (k.meanAnomaly() * MathUtilsDouble.degRad);

                            // SIZE
                            var size = Math.max((body.size + (float) (rand.nextGaussian() * body.size * sizeNoise)), 0.1f);
                            model.instanceAttributes[curr.instanceIdx + model.sizeOffset] = (hl.isHighlighted() && ci != null ? ci.hlSizeFactor : size);

                            // TEXTURE INDEX
                            float textureIndex = computeTextureIndex(k, rand, textureArray, textureAttribute);
                            model.instanceAttributes[curr.instanceIdx + model.textureIndexOffset] = textureIndex;

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

            if (set != null || eSet != null) {
                // Fetch parameters from particle or elements set.
                var textureArray = set != null ? set.textureArray : eSet.textureArray;
                var sphericalPower = set != null ? set.sphericalPower : eSet.sphericalPower;
                var profileDecay = set != null ? set.profileDecay : eSet.profileDecay;
                var shadingTyp = set != null ? set.shadingType : eSet.shadingType;

                var offset = getOffset(render);
                if (offset < 0) return;
                curr = meshes.get(offset);
                if (curr != null) {
                    if (textureArray != null) {
                        textureArray.bind(0);
                    }


                    ExtShaderProgram shaderProgram = getShaderProgram();
                    shaderProgram.begin();

                    shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
                    shaderProgram.setUniformf("u_camPos", camera.getPos());
                    addCameraUpCubemapMode(shaderProgram, camera);

                    int shadingType = preShadingType(hl, shadingTyp);
                    float sizeFactor = utils.getDatasetSizeFactor(render.entity, hl, desc);
                    var sizeLimits = set != null ? set.particleSizeLimits : defaultSizeLimits;

                    shaderProgram.setUniformf("u_alpha", alphas[renderable.getComponentType().getFirstOrdinal()] * renderable.getOpacity());
                    shaderProgram.setUniformf("u_falloff", getProfileDecay(shadingType, profileDecay));
                    shaderProgram.setUniformf("u_sizeLimits", (float) (sizeLimits[0] * sizeFactor),
                                              (float) (sizeLimits[1] * sizeFactor));
                    shaderProgram.setUniformf("u_sizeFactor", (float) (sizeFactor / Constants.DISTANCE_SCALE_FACTOR));

                    // Shading type.
                    setShadingTypeUniforms(shaderProgram, camera, shadingType, sphericalPower, 0f);

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

                    postShadingType();
                }


            }
        }
    }

    /**
     * Computes and sets the color of the given particle to the instanced attributes.
     *
     * @param k                   The particle.
     * @param hl                  The {@link Highlight} component.
     * @param baseColor           The base color for the particle.
     * @param colorNoiseContainer The color noise value.
     * @param model               The instanced model.
     */
    private void setParticleColorAttributes(IParticleRecord k,
                                            Highlight hl,
                                            float[] baseColor,
                                            float colorNoise,
                                            float[] colorNoiseContainer,
                                            InstancedModel model) {
        if (hl.isHighlighted()) {
            setHighlightColorAttributes(k, hl, baseColor, model);
        } else {
            generateChannelNoise(k.id(), colorNoise, colorNoiseContainer);
            model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits(
                    MathUtils.clamp(baseColor[0] + colorNoiseContainer[0], 0, 1),
                    MathUtils.clamp(baseColor[1] + colorNoiseContainer[1], 0, 1),
                    MathUtils.clamp(baseColor[2] + colorNoiseContainer[2], 0, 1),
                    MathUtils.clamp(baseColor[3], 0, 1));
        }
    }

    /**
     * Sets the color in highlight mode.
     *
     * @param k         The particle.
     * @param hl        The {@link Highlight} component.
     * @param baseColor The base color for the particle.
     * @param model     The instanced model.
     */
    private void setHighlightColorAttributes(IParticleRecord k,
                                             Highlight hl,
                                             float[] baseColor,
                                             InstancedModel model) {
        if (!hl.isHlplain()) {
            // Color map.
            double[] color = cmap.colormap(hl.getHlcmi(), hl.getHlcma()
                    .getNumber(k), hl.getHlcmmin(), hl.getHlcmmax());
            model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits(
                    (float) color[0], (float) color[1], (float) color[2],
                    hl.getHlcmAlpha());
        } else {
            // Plain highlight color.
            model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits(baseColor[0], baseColor[1],
                                                                                              baseColor[2], baseColor[3]);
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
