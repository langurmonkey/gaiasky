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
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ElementsPath;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.gdx.shader.ExtShaderProgram;
import gaiasky.render.system.InstancedRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.AffineTransformations;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.Highlight;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.component.Render;
import gaiasky.scene.record.ModelType;
import gaiasky.scene.record.OrbitComponent;
import gaiasky.scene.record.ParticleKepler;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Constants;
import gaiasky.util.DatasetCard;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Nature;
import gaiasky.util.color.Colormap;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;
import org.lwjgl.opengl.GL41;

import java.util.List;
import java.util.Random;

public class ElementsSetRenderer extends InstancedRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(ElementsSetRenderer.class);

    /** Print the per-frame cost of the element positions, see {@link #profile(long, long, long, int)}. **/
    private static final boolean PROFILE = Boolean.getBoolean("gaiasky.elements.profile");
    private static long prCompute, prCopy, prFrame, prLast;
    private static int prFrames, prCount;

    private final double[] defaultSizeLimits = new double[]{Math.tan(Math.toRadians(0.05)), FastMath.tan(Math.toRadians(9.6))};
    private final Colormap cmap;
    private final Random rand;
    /** Element position state, indexed by mesh offset. **/
    private final Array<InstancePositions> positions;
    /** Vertex shader variants that solve the orbits in the GPU, or null if the CPU path is active. **/
    private final ExtShaderProgram[] gpuPrograms;
    private final Matrix4D refSysTransformD, auxTransformD, affineTransformD;
    private final Vector3D datasetPosD;

    /**
     * Persistent per-mesh state needed to recompute the element positions every frame.
     */
    private static class InstancePositions {
        /** Camera-relative positions, three floats per instance, in a buffer of their own. **/
        float[] pos;
        /** Number of instances written. **/
        int count;
        /** Sources, in instance order: {@link ParticleKepler} or {@link OrbitComponent}. **/
        Object[] src;
        /** Compiled orbits, with the constants and the state of the Kepler solve. **/
        final ElementPositions elements = new ElementPositions();
        /** Time the positions were computed for. **/
        double tjd = Double.NaN;
        /** Last dataset origin used, to skip the upload when nothing changed. **/
        double dx = Double.NaN, dy = Double.NaN, dz = Double.NaN;
    }

    public ElementsSetRenderer(SceneRenderer sceneRenderer,
                               RenderGroup rg,
                               float[] alphas,
                               ExtShaderProgram[] shaders,
                               ExtShaderProgram[] gpuShaders) {
        super(sceneRenderer, rg, alphas, shaders);
        this.gpuPrograms = gpuShaders;
        cmap = new Colormap();
        rand = new Random(123L);
        positions = new Array<>(20);
        refSysTransformD = new Matrix4D();
        auxTransformD = new Matrix4D();
        affineTransformD = new Matrix4D();
        datasetPosD = new Vector3D();
        EventManager.instance.subscribe(this, Event.GPU_DISPOSE_ORBITAL_ELEMENTS);
        if (ElementsPath.isGpu() && gpuShaders == null) {
            logger.error("The GPU path for the orbital elements is selected but its shaders were not loaded. " +
                                 "The positions will be wrong. Check the shader compilation log.");
        }
        if (PROFILE) {
            System.out.println("[ELEM] profiling enabled, printing once per second, path " + ElementsPath.current());
        }
    }

    @Override
    protected boolean isDynamicInstanceData() {
        return true;
    }

    @Override
    protected ExtShaderProgram getShaderProgram() {
        if (ElementsPath.isGpu() && gpuPrograms != null) {
            return getShaderProgram(gpuPrograms);
        }
        return super.getShaderProgram();
    }

    @Override
    protected void addAttributesDivisor1(Array<VertexAttribute> attributes,
                                         int primitive) {
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        if (ElementsPath.isGpu()) {
            // The elements travel to the shader, which solves Kepler's equation from them.
            attributes.add(new VertexAttribute(OwnUsage.OrbitElems1, 4, "a_orbitelems01"));
            attributes.add(new VertexAttribute(OwnUsage.OrbitElems2, 4, "a_orbitelems02"));
        }
        attributes.add(new VertexAttribute(OwnUsage.Size, 1, "a_size"));
        attributes.add(new VertexAttribute(OwnUsage.TextureIndex, 1, "a_textureIndex"));
    }

    @Override
    protected VertexAttribute[] buildAttributesDynamic() {
        if (ElementsPath.isGpu()) {
            // Nothing to rewrite every frame: the shader computes the position from the elements.
            return null;
        }
        // Camera-relative position, computed on the CPU in double precision and rewritten on every
        // frame. It gets a buffer of its own so that the update does not re-upload colour and size.
        return new VertexAttribute[]{new VertexAttribute(OwnUsage.OrbitPos, 3, "a_pos")};
    }

    @Override
    protected void offsets0(MeshData curr,
                            InstancedModel model) {
        // Unused
    }

    @Override
    protected void offsets1(MeshData curr,
                            InstancedModel model) {
        curr.colorOffset = curr.mesh.getInstancedAttribute(Usage.ColorPacked) != null ?
                curr.mesh.getInstancedAttribute(Usage.ColorPacked).offset / 4 : 0;
        model.elems01Offset = curr.mesh.getInstancedAttribute(OwnUsage.OrbitElems1) != null ?
                curr.mesh.getInstancedAttribute(OwnUsage.OrbitElems1).offset / 4 : 0;
        model.elems02Offset = curr.mesh.getInstancedAttribute(OwnUsage.OrbitElems2) != null ?
                curr.mesh.getInstancedAttribute(OwnUsage.OrbitElems2).offset / 4 : 0;
        model.elemPosOffset = curr.mesh.getDynamicAttribute(OwnUsage.OrbitPos) != null ?
                curr.mesh.getDynamicAttribute(OwnUsage.OrbitPos).offset / 4 : 0;
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
        final boolean gpuPath = ElementsPath.isGpu();
        for (IRenderable renderable : renderables) {
            Render render = (Render) renderable;
            var graph = Mapper.graph.get(render.entity);
            var model = getModelQuad(primitive,
                                     getOffset(render));
            var desc = Mapper.datasetDescription.get(render.entity);
            var hl = Mapper.highlight.get(render.entity);
            var set = Mapper.particleSet.get(render.entity);
            var eSet = Mapper.orbitElementsSet.get(render.entity);


            DatasetCard ci = desc.datasetCard;
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

                int numChildren = eSet != null && graph.children != null ? graph.children.size : 0;
                int numOwn = pointData != null ? pointData.size() : 0;
                // Upper bound of the instances this entity can produce: the children that carry a body
                // representation, plus the particles of its own list. Both go into a single mesh,
                // because they share the model and the draw call. Building one mesh per source left
                // the offset pointing at the first one while the instances were written into the
                // second, so the draw call read a buffer that had never been filled.
                int capacity = numChildren + numOwn;
                int numParticlesAdded = 0;
                // Sources per instance, to recompute the positions every frame.
                var src = capacity > 0 ? new Object[capacity] : null;

                if (capacity > 0) {
                    int offset = addMeshData(model,
                                             model.numVertices,
                                             capacity,
                                             model.numIndices,
                                             ModelType.QUAD,
                                             primitive);
                    setModel(offset, model);
                    setOffset(render, offset);
                    curr = meshes.get(offset);
                    curr.instanceIdx = 0;
                    model.ensureInstanceAttribsSize(capacity * curr.instanceSize);
                }

                if (capacity > 0 && numChildren > 0) {
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

                                // Elements, either into the instance buffer or kept for the per-frame
                                // position computation.
                                if (gpuPath) {
                                    setElementsAttributes(model.instanceAttributes, curr.instanceIdx + model.elems01Offset,
                                                          curr.instanceIdx + model.elems02Offset, oc);
                                } else {
                                    src[numParticlesAdded] = oc;
                                }

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
                if (capacity > 0 && numOwn > 0) {
                    for (int i = 0; i < numOwn; i++) {
                        if (set == null || utils.filter(i, set, desc) && set.isVisible(i)) {
                            var p = pointData.get(i);
                            var k = (ParticleKepler) p;
                            // COLOR
                            setParticleColorAttributes(k, hl, baseColor, colorNoise, colorNoiseContainer, model);

                            // Elements, either into the instance buffer or kept for the per-frame
                            // position computation.
                            if (gpuPath) {
                                setElementsAttributes(model.instanceAttributes, curr.instanceIdx + model.elems01Offset,
                                                      curr.instanceIdx + model.elems02Offset, k);
                            } else {
                                src[numParticlesAdded] = k;
                            }

                            // SIZE
                            var size = Math.max((body.size + (float) (rand.nextGaussian() * body.size * sizeNoise)), 1e-10f);
                            model.instanceAttributes[curr.instanceIdx + model.sizeOffset] = (hl.isHighlighted() && ci != null ? ci.hlSizeFactor : size);

                            // TEXTURE INDEX
                            float textureIndex = computeTextureIndex(k, rand, textureArray, textureAttribute);
                            model.instanceAttributes[curr.instanceIdx + model.textureIndexOffset] = textureIndex;

                            curr.instanceIdx += curr.instanceSize;
                            numParticlesAdded++;
                        }
                    }
                }

                if (capacity > 0) {
                    // Global (divisor=0) vertices (position, uv?) plus optional indices
                    curr.mesh.setVertices(model.vertices, 0, model.numVertices * model.modelVertexSize);
                    if (model.numIndices > 0) {
                        curr.mesh.setIndices(model.indices, 0, model.numIndices);
                    }
                    // Per instance (divisor=1) attributes. Only the instances actually written are
                    // uploaded, and only those are drawn. Anything past them is out of the buffer:
                    // undefined values that some drivers return as zeros and others as stale memory.
                    curr.instanceCount = Math.min(numParticlesAdded, capacity);
                    setCount(render, curr.instanceCount);

                    if (!gpuPath) {
                        // Keep the element sources around: the positions are rewritten from them on
                        // every frame, in double precision, on the CPU.
                        var ip = getPositions(getOffset(render));
                        ip.count = curr.instanceCount;
                        ip.src = src;
                        if (ip.pos == null || ip.pos.length < curr.instanceCount * 3) {
                            ip.pos = new float[curr.instanceCount * 3];
                        }
                        // Prepare the orbital elements once: the conversion to radians and the
                        // perifocal rotation do not depend on time.
                        ip.elements.compile(src, curr.instanceCount);
                        ip.tjd = Double.NaN;
                        ip.dx = ip.dy = ip.dz = Double.NaN;
                    }

                    curr.mesh.setInstanceAttribs(model.instanceAttributes, 0, curr.instanceCount * curr.instanceSize);
                    if (!gpuPath) {
                        curr.mesh.setDynamicAttribs(getPositions(getOffset(render)).pos, 0, curr.instanceCount * 3);
                    }
                } else {
                    // Nothing to buffer. Drop the bookkeeping so this renderable draws nothing.
                    setOffset(render, -1);
                }

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
                var diffuseScattering = set != null ? set.ambientLight : 0f;

                var offset = getOffset(render);
                if (offset < 0 || offset >= meshes.size) {
                    // No mesh for this renderable. Skip it, but not the rest of the group.
                    continue;
                }
                curr = meshes.get(offset);
                if (curr != null && curr.instanceCount > 0) {
                    if (textureArray != null) {
                        textureArray.bind(0);
                    }

                    ExtShaderProgram shaderProgram = getShaderProgram();
                    shaderProgram.begin();

                    shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
                    addCameraUpCubemapMode(shaderProgram, camera);

                    int shadingType = preShadingType(hl, shadingTyp);
                    float sizeFactor = utils.getDatasetSizeFactor(render.entity, hl, desc);
                    var sizeLimits = set != null ? set.particleSizeLimits : defaultSizeLimits;

                    shaderProgram.setUniformf("u_alpha", alphas[renderable.getComponentType().getFirstOrdinal()] * renderable.getOpacity());
                    shaderProgram.setUniformf("u_falloff", getProfileDecay(shadingType, profileDecay));
                    shaderProgram.setUniformf("u_sizeLimits", (float) (sizeLimits[0] * sizeFactor),
                                              (float) (sizeLimits[1] * sizeFactor));
                    // The size is a nominal length in local units and the shader multiplies it by the
                    // distance factor (u_vrScale), so that it scales like the positions and the
                    // angular size stays the same whatever the distance scale factor is.
                    shaderProgram.setUniformf("u_sizeFactor", (float) sizeFactor);

                    // Shading type.
                    setShadingTypeUniforms(shaderProgram, camera, shadingType, sphericalPower, diffuseScattering, graph);

                    // VR scale.
                    shaderProgram.setUniformf("u_vrScale", (float) Constants.DISTANCE_SCALE_FACTOR);

                    // Element positions.
                    double tjd = AstroUtils.getJulianDate(GaiaSky.instance.time.getTime());
                    if (gpuPath) {
                        // The shader solves Kepler's equation itself. Time goes as an emulated
                        // double: a float cannot hold a julian date with sub-day resolution, and the
                        // mean anomaly is accumulated from the epoch.
                        float tHigh = (float) tjd;
                        shaderProgram.setUniformf("u_t", tHigh, (float) (tjd - (double) tHigh));

                        // Inverse reference-system transform.
                        computeRefSysTransform(graph, set, refSysTransformD);
                        refSysTransformD.putIn(refSysTransformF);
                        shaderProgram.setUniformMatrix("u_refSysTransform", refSysTransformF);

                        // Arbitrary affine transformations.
                        addAffineTransformUniforms(shaderProgram, Mapper.affine.get(render.entity));

                        // Dataset origin, which the shader adds to the element position.
                        datasetPosD.set(graph.translation);
                    } else {
                        // Kepler's equation and the reference-system transform, in double precision,
                        // on the CPU, once per element instead of once per vertex.
                        updatePositions(getPositions(offset), curr, graph, set, Mapper.affine.get(render.entity), tjd);
                    }

                    // Dataset origin, camera-relative. Only used for the fake lighting.
                    shaderProgram.setUniformf("u_datasetPos", (float) datasetPosD.x, (float) datasetPosD.y, (float) datasetPosD.z);

                    // Relativistic effects.
                    addEffectsUniforms(shaderProgram, camera);

                    try {
                        int count = curr.mesh.getNumIndices() > 0 ? curr.mesh.getNumIndices() : curr.mesh.getNumVertices();
                        curr.mesh.render(shaderProgram, primitive, 0, count, curr.instanceCount);
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

    /**
     * Returns the position state of the given mesh offset, creating it if needed.
     *
     * @param offset The mesh offset.
     * @return The position state.
     */
    private InstancePositions getPositions(int offset) {
        while (positions.size <= offset) {
            positions.add(new InstancePositions());
        }
        return positions.get(offset);
    }

    /**
     * Recomputes the camera-relative position of every instance of the given mesh and uploads the
     * instance buffer. The Kepler solve only depends on time, so it is skipped while the time does
     * not change; the reference-system transform and the dataset origin are applied every frame,
     * because both move with the camera.
     *
     * @param ip     Position state of the mesh.
     * @param curr   Mesh data.
     * @param graph  Graph node of the dataset.
     * @param set    Particle set, if any.
     * @param affine Affine transformations of the dataset, if any.
     * @param tjd    Current time, in julian days.
     */
    /**
     * Writes the orbital elements of an orbit component into the instance buffer, in the layout and
     * the units the vertex shader expects: period in days, epoch in julian days, semi-major axis in
     * kilometres, and the angles in radians.
     *
     * @param data The instance buffer.
     * @param o1   Offset of the first element quad.
     * @param o2   Offset of the second element quad.
     * @param oc   The orbit component.
     */
    private static void setElementsAttributes(float[] data,
                                              int o1,
                                              int o2,
                                              OrbitComponent oc) {
        double period = oc.period;
        if (period <= 0 && oc.mu > 0 && oc.semiMajorAxis > 0) {
            // No period given: derive it from the standard gravitational parameter.
            double a = oc.semiMajorAxis;
            period = 2.0 * Math.PI * Math.sqrt(a * a * a / oc.mu) / Nature.D_TO_S;
        }
        boolean valid = period > 0 && oc.semiMajorAxis > 0;
        data[o1] = (float) (valid ? period : 1d);
        data[o1 + 1] = (float) oc.epoch;
        // A zero semi-major axis collapses the orbit to the origin, which is what an element
        // without a usable orbit should draw.
        data[o1 + 2] = (float) (valid ? oc.semiMajorAxis : 0d);
        data[o1 + 3] = (float) oc.e;
        data[o2] = (float) (oc.i * MathUtilsDouble.degRad);
        data[o2 + 1] = (float) (oc.ascendingNode * MathUtilsDouble.degRad);
        data[o2 + 2] = (float) (oc.argOfPericenter * MathUtilsDouble.degRad);
        data[o2 + 3] = (float) (oc.meanAnomaly * MathUtilsDouble.degRad);
    }

    /**
     * Writes the orbital elements of a particle record into the instance buffer, in the layout and
     * the units the vertex shader expects. See {@link #setElementsAttributes(float[], int, int, OrbitComponent)}.
     *
     * @param data The instance buffer.
     * @param o1   Offset of the first element quad.
     * @param o2   Offset of the second element quad.
     * @param k    The particle record.
     */
    private static void setElementsAttributes(float[] data,
                                              int o1,
                                              int o2,
                                              ParticleKepler k) {
        double period = k.period();
        double a = k.semiMajorAxis();
        boolean valid = period > 0 && a > 0;
        data[o1] = (float) (valid ? period : 1d);
        data[o1 + 1] = (float) k.epoch();
        data[o1 + 2] = (float) (valid ? a : 0d);
        data[o1 + 3] = (float) k.eccentricity();
        data[o2] = (float) (k.inclination() * MathUtilsDouble.degRad);
        data[o2 + 1] = (float) (k.ascendingNode() * MathUtilsDouble.degRad);
        data[o2 + 2] = (float) (k.argOfPericenter() * MathUtilsDouble.degRad);
        data[o2 + 3] = (float) (k.meanAnomaly() * MathUtilsDouble.degRad);
    }

    private void updatePositions(InstancePositions ip,
                                 MeshData curr,
                                 GraphNode graph,
                                 ParticleSet set,
                                 AffineTransformations affine,
                                 double tjd) {
        if (ip == null || ip.pos == null) {
            return;
        }

        // Dataset origin, camera-relative. Always up to date, it is the shading reference.
        datasetPosD.set(graph.translation);

        if (ip.count == 0) {
            return;
        }

        // Reference-system transform, in double precision.
        boolean refSysIdt = computeRefSysTransform(graph, set, refSysTransformD);

        Matrix4D affineD = null;
        if (affine != null && !affine.isEmpty()) {
            affineD = affine.apply(affineTransformD.idt());
        }
        final Matrix4D refSysD = refSysIdt ? null : refSysTransformD;

        long t0 = PROFILE ? System.nanoTime() : 0L;
        final long frame = PROFILE ? (prLast == 0L ? 0L : t0 - prLast) : 0L;
        if (PROFILE) {
            prLast = t0;
        }

        if (tjd != ip.tjd) {
            // Solve the orbits and write the positions in the same pass. This is the expensive part.
            ip.elements.compute(tjd, ip.pos, 3, 0, 1, refSysD, datasetPosD, affineD);
            ip.tjd = tjd;
            ip.dx = datasetPosD.x;
            ip.dy = datasetPosD.y;
            ip.dz = datasetPosD.z;
        } else {
            boolean sameDatasetPos = datasetPosD.x == ip.dx && datasetPosD.y == ip.dy && datasetPosD.z == ip.dz;
            if (sameDatasetPos && refSysIdt && affineD == null) {
                // Nothing moved since the last frame.
                return;
            }
            // Only the projection changed: reuse the orbits found on the previous frame.
            ip.elements.project(ip.pos, 3, 0, 1, refSysD, datasetPosD, affineD);
            ip.dx = datasetPosD.x;
            ip.dy = datasetPosD.y;
            ip.dz = datasetPosD.z;
        }
        long t1 = PROFILE ? System.nanoTime() : 0L;

        // Upload the positions only: they live in a buffer of their own, so this moves twelve bytes
        // per element instead of the whole interleaved instance record.
        curr.mesh.setDynamicAttribs(ip.pos, 0, ip.count * 3);
        if (PROFILE) {
            profile(frame, t1 - t0, System.nanoTime() - t1, ip.count);
        }
    }

    /**
     * Accumulates and prints the time spent per frame on the element positions. Enabled with
     * {@code -Pgaiasky.elements.profile=true}, prints once a second.
     *
     * @param frame   Nanoseconds since the previous frame, so the total budget can be compared.
     * @param compute Nanoseconds spent solving the orbits and writing the positions.
     * @param upload  Nanoseconds spent copying the position buffer into direct memory. The transfer
     *                to the driver happens later, when the mesh is bound, so it is not included.
     * @param count   Number of elements of this mesh.
     */
    private static void profile(long frame, long compute, long upload, int count) {
        prFrame += frame;
        prCompute += compute;
        prCopy += upload;
        prFrames++;
        prCount = count;
        if (prFrames >= 60) {
            System.out.printf("[ELEM] %s n=%d frames=%d  frame %.2f ms = compute %.2f + upload %.2f + rest %.2f%n",
                              ElementsPath.current(),
                              prCount,
                              prFrames,
                              prFrame / prFrames / 1e6,
                              prCompute / prFrames / 1e6,
                              prCopy / prFrames / 1e6,
                              (prFrame - prCompute - prCopy) / prFrames / 1e6);
            prFrame = prCopy = prCompute = 0L;
            prFrames = 0;
        }
    }

    /**
     * Builds the inverse reference-system transform of the given dataset, in double precision.
     * Mirrors {@link InstancedRenderSystem#setRefSysTransformAndDatasetPosUniforms()}, but stays in
     * doubles all the way: a float matrix cannot hold the translation of an extrasolar system
     * without losing the orbit around it.
     *
     * @param graph The dataset graph node.
     * @param set   The particle set, if any.
     * @param out   The output matrix.
     * @return True if the resulting transform is the identity.
     */
    private boolean computeRefSysTransform(GraphNode graph,
                                           ParticleSet set,
                                           Matrix4D out) {
        if (graph.children != null && graph.children.size > 0) {
            var child = graph.children.get(0);
            Matrix4D refSys = Mapper.transform.has(child) ? Mapper.transform.get(child).matrix : null;
            if (refSys == null) {
                out.idt();
                return true;
            }
            if (Mapper.trajectory.has(child) && Mapper.trajectory.get(child).model.isExtrasolar()) {
                // Extrasolar elements require a phase around Y.
                out.setToRotation(0, 1, 0, -90).mul(auxTransformD.set(refSys).inv());
            } else {
                out.set(refSys).inv();
            }
            return false;
        } else if (set != null && set.pointData != null && !set.pointData.isEmpty() && graph.parent != null) {
            var pOri = Mapper.graph.get(graph.parent);
            if (pOri != null && pOri.orientation != null) {
                out.set(pOri.orientation).inv();
                return false;
            }
        }
        out.idt();
        return true;
    }

    public void reset() {
        // Every mesh goes, and so does every per-renderable offset, so that the next frame builds
        // them again with the layout of the path in use now.
        resetMeshes();
        models.clear();
        positions.clear();
        curr = null;
    }

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        if (event.equals(Event.GPU_DISPOSE_ORBITAL_ELEMENTS)) {
            if (source instanceof Render render) {
                int offset = getOffset(render);
                if (offset >= 0) {
                    clearMeshData(offset);
                    if (offset < models.size) {
                        // Free the model slot too, so the next build reuses this index instead of
                        // shifting the models array away from the meshes.
                        models.set(offset, null);
                    }
                }
                setOffset(render, -1);
                setInGpu(render, false);
                if (offset >= 0 && offset < positions.size) {
                    // The mesh is gone: drop the position state that pointed into it.
                    positions.set(offset, new InstancePositions());
                }
            } else {
                // No source: everything goes. This is how a change of the elements path is applied,
                // since the attribute layout of the mesh depends on it.
                GaiaSky.postRunnable(this::reset);
            }
        }
    }
}
