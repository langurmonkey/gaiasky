/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.ParticleGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.Settings.SceneSettings.StarSettings;
import gaiasky.util.color.Colormap;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.StdRandom;

import java.util.Random;

/**
 * Renders particle groups using regular arrays via billboards with geometry (quads as two triangles).
 */
public class ParticleGroupRenderSystem extends PointCloudTriRenderSystem implements IObserver {
    private final Vector3 aux1;
    private int posOffset, sizeOffset, particlePosOffset, uvOffset;
    private final Random rand;
    private final Colormap cmap;

    public ParticleGroupRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        rand = new Random(123);
        aux1 = new Vector3();
        cmap = new Colormap();
        EventManager.instance.subscribe(this, Event.DISPOSE_PARTICLE_GROUP_GPU_MESH);
    }

    @Override
    protected void initShaderProgram() {
        // Empty
    }

    protected void addVertexAttributes(Array<VertexAttribute> attributes) {
        attributes.add(new VertexAttribute(Usage.Position, 2, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ExtShaderProgram.TEXCOORD_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.ObjectPosition, 3, "a_particlePos"));
        attributes.add(new VertexAttribute(OwnUsage.Size, 1, "a_size"));
    }

    protected void offsets(MeshData curr) {
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        posOffset = curr.mesh.getVertexAttribute(Usage.Position) != null ? curr.mesh.getVertexAttribute(Usage.Position).offset / 4 : 0;
        uvOffset = curr.mesh.getVertexAttribute(Usage.TextureCoordinates) != null ? curr.mesh.getVertexAttribute(Usage.TextureCoordinates).offset / 4 : 0;
        sizeOffset = curr.mesh.getVertexAttribute(OwnUsage.Size) != null ? curr.mesh.getVertexAttribute(OwnUsage.Size).offset / 4 : 0;
        particlePosOffset = curr.mesh.getVertexAttribute(OwnUsage.ObjectPosition) != null ? curr.mesh.getVertexAttribute(OwnUsage.ObjectPosition).offset / 4 : 0;
    }

    protected void preRenderObjects(ExtShaderProgram shaderProgram, ICamera camera) {
        shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
        shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux1));
        addEffectsUniforms(shaderProgram, camera);
    }

    @Override
    protected void renderObject(ExtShaderProgram shaderProgram, IRenderable renderable) {
        final ParticleGroup particleGroup = (ParticleGroup) renderable;
        synchronized (particleGroup) {
            if (!particleGroup.disposed) {
                boolean hlCmap = particleGroup.isHighlighted() && !particleGroup.isHlplain();
                if (!particleGroup.inGpu()) {
                    int n = particleGroup.size();
                    particleGroup.offset = addMeshData(n * 4, n * 6);
                    curr = meshes.get(particleGroup.offset);
                    ensureTempVertsSize(n * 4 * curr.vertexSize);
                    ensureTempIndicesSize(n * 6);

                    float[] c = particleGroup.getColor();
                    float[] colorMin = particleGroup.getColorMin();
                    float[] colorMax = particleGroup.getColorMax();
                    double minDistance = particleGroup.getMinDistance();
                    double maxDistance = particleGroup.getMaxDistance();

                    int numVerticesAdded = 0;
                    int numParticlesAdded = 0;
                    for (int i = 0; i < n; i++) {
                        if (particleGroup.filter(i) && particleGroup.isVisible(i)) {
                            IParticleRecord particle = particleGroup.get(i);
                            double[] p = particle.rawDoubleData();
                            // 4 vertices per particle
                            for (int vert = 0; vert < 4; vert++) {
                                // Vertex POSITION
                                tempVerts[curr.vertexIdx + posOffset] = vertPos[vert].getFirst();
                                tempVerts[curr.vertexIdx + posOffset + 1] = vertPos[vert].getSecond();

                                // UV coordinates
                                tempVerts[curr.vertexIdx + uvOffset] = vertUV[vert].getFirst();
                                tempVerts[curr.vertexIdx + uvOffset + 1] = vertUV[vert].getSecond();

                                // COLOR
                                if (particleGroup.isHighlighted()) {
                                    if (hlCmap) {
                                        // Color map
                                        double[] color = cmap.colormap(particleGroup.getHlcmi(), particleGroup.getHlcma().get(particle), particleGroup.getHlcmmin(), particleGroup.getHlcmmax());
                                        tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits((float) color[0], (float) color[1], (float) color[2], 1.0f);
                                    } else {
                                        // Plain
                                        tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(c[0], c[1], c[2], c[3]);
                                    }
                                } else {
                                    if (colorMin != null && colorMax != null) {
                                        double dist = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
                                        // fac = 0 -> colorMin,  fac = 1 -> colorMax
                                        double fac = (dist - minDistance) / (maxDistance - minDistance);
                                        interpolateColor(colorMin, colorMax, c, fac);
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
                                tempVerts[curr.vertexIdx + sizeOffset] = (particleGroup.size + (float) (rand.nextGaussian() * particleGroup.size / 5d)) * particleGroup.highlightedSizeFactor();

                                // PARTICLE POSITION
                                tempVerts[curr.vertexIdx + particlePosOffset] = (float) p[0];
                                tempVerts[curr.vertexIdx + particlePosOffset + 1] = (float) p[1];
                                tempVerts[curr.vertexIdx + particlePosOffset + 2] = (float) p[2];

                                curr.vertexIdx += curr.vertexSize;
                                curr.numVertices++;
                                numVerticesAdded++;
                            }
                            // Indices
                            quadIndices(curr);
                            numParticlesAdded++;
                        }
                    }
                    particleGroup.count = numVerticesAdded * curr.vertexSize;
                    curr.mesh.setVertices(tempVerts, 0, particleGroup.count);
                    curr.mesh.setIndices(tempIndices, 0, numParticlesAdded * 6);

                    particleGroup.inGpu(true);
                }

                /*
                 * RENDER
                 */
                curr = meshes.get(particleGroup.offset);
                if (curr != null) {
                    float meanDist = (float) (particleGroup.getMeanDistance());

                    double s = .3e-4f;
                    shaderProgram.setUniformf("u_alpha", alphas[particleGroup.ct.getFirstOrdinal()] * particleGroup.getOpacity());
                    shaderProgram.setUniformf("u_falloff", particleGroup.profileDecay);
                    shaderProgram.setUniformf("u_sizeFactor", (float) (((StarSettings.getStarPointSize() * s)) * particleGroup.highlightedSizeFactor() * meanDist / Constants.DISTANCE_SCALE_FACTOR));
                    shaderProgram.setUniformf("u_sizeLimits", (float) (particleGroup.particleSizeLimits[0] * particleGroup.highlightedSizeFactor()), (float) (particleGroup.particleSizeLimits[1] * particleGroup.highlightedSizeFactor()));

                    try {
                        curr.mesh.render(shaderProgram, GL20.GL_TRIANGLES);
                    } catch (IllegalArgumentException e) {
                        logger.error(e, "Render exception");
                    }
                }
            }
        }
    }

    private void interpolateColor(float[] c0, float[] c1, float[] result, double factor) {
        float f = (float) factor;
        result[0] = (1 - f) * c0[0] + f * c1[0];
        result[1] = (1 - f) * c0[1] + f * c1[1];
        result[2] = (1 - f) * c0[2] + f * c1[2];
        result[3] = (1 - f) * c0[3] + f * c1[3];
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.DISPOSE_PARTICLE_GROUP_GPU_MESH) {
            Integer meshIdx = (Integer) data[0];
            clearMeshData(meshIdx);
        }
    }

}
