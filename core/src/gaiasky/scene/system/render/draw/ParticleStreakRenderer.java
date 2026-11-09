/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.gdx.mesh.IntMesh;
import gaiasky.render.gdx.shader.ExtShaderProgram;
import gaiasky.render.system.ImmediateModeRenderSystem;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;

import java.util.List;
import java.util.Random;

/**
 * Velocity streaks around the camera: a sense of speed that works over the whole range of distances
 * and speeds of the scene.
 * <p>
 * The effect is stateless. The vertex buffer holds a fixed seed per particle and is uploaded once;
 * the vertex shader turns each seed into a position wrapped in a cell around the camera and stretches
 * it along the camera's velocity into a quad. There is no simulation, no per-frame upload, no spawn
 * and no lifetime, and every coordinate in the shader is camera-relative, so no float has to hold an
 * absolute position.
 * <p>
 * The two quantities that matter are in screen space. A streak is a fraction of the screen height at
 * the reference depth, so it looks the same whether the camera is crawling next to a spacecraft or
 * crossing between galaxies, and the intensity follows how fast the view moves in screen heights per
 * second, which is the only measure of speed that means anything when the scene spans twenty orders
 * of magnitude.
 * <p>
 * <b>The cell size must not vary while the cell is visible.</b> A particle sits at
 * {@code mod(seed * size - pos, size)}, so changing {@code size} moves every particle by an amount
 * that depends on its own seed and not on the motion of the camera: the field shimmers sideways and
 * the streaks, which only know about the velocity, keep pointing where they always did. The cell can
 * therefore only be changed for a layer that is already invisible. That is what the layers are for:
 * {@link #N_LAYERS} independent fields, each with its own cell size, which fade in and out of range
 * as the zoom changes and are re-sized only while their weight is zero. At any moment at least one of
 * them is at full weight, so the effect never blinks and never jumps.
 */
public class ParticleStreakRenderer extends ImmediateModeRenderSystem {

    /**
     * Independent fields, each with its own cell size. Two are enough: one is always in range, the
     * other is either fading in or invisible and free to be re-sized.
     */
    private static final int N_LAYERS = 2;
    /**
     * Particles per layer, per quality step from LOW to ULTRA.
     */
    private static final int PARTICLES_PER_STEP = 600;
    private static final int VERTICES_PER_PARTICLE = 6;

    /**
     * The cell is this many "effective scale units" across, where the effective scale is whichever
     * is larger, the zoom (the speed scaling of the camera) or the speed itself. Below the zoom
     * speed the apparent motion therefore grows with the speed, and above it it stays put instead of
     * blowing up: the field is crossed in about the same time however fast the camera goes.
     */
    private static final double CELL_SCALE_UNITS = 2.0;
    private static final double MIN_CELL = 1.0e-4;
    private static final double MAX_CELL = 1.0e15;

    /**
     * Ratio between a layer's cell and the cell the current speed asks for at which the layer is at
     * full weight, and at which it is invisible and may be re-sized.
     */
    private static final double WEIGHT_FULL_RATIO = 1.7;
    private static final double WEIGHT_ZERO_RATIO = 2.9;

    /**
     * Alpha of a particle by its distance from the camera, as a fraction of the half-cell. The inner
     * fade is what keeps the nearest particles from crossing the screen in a few frames.
     */
    private static final float FADE_INNER_LO = 0.36f;
    private static final float FADE_INNER_HI = 0.58f;
    private static final float FADE_OUTER_LO = 0.85f;
    private static final float FADE_OUTER_HI = 1.00f;

    /**
     * Length of a streak at the reference depth, as a fraction of the screen height, at the lowest
     * and the highest speed.
     */
    private static final float LEN_MIN_FRAC = 0.05f;
    private static final float LEN_MAX_FRAC = 0.15f;
    /**
     * Speed of the view, in screen heights per second at the reference depth, at which the effect
     * starts and at which it is at full strength.
     */
    private static final float RATE_IN = 0.015f;
    private static final float RATE_FULL = 0.6f;
    /**
     * Time constant of the smoothing of the camera velocity, in seconds.
     */
    private static final double SMOOTHING_TAU = 0.12;

    private final ComponentTypes ct;
    /** Camera velocity, in U/s, smoothed over frames. **/
    private final Vector3D velocity = new Vector3D();
    private final Vector3D auxD = new Vector3D();
    private final Vector3D auxD2 = new Vector3D();
    private final Matrix4 viewRot = new Matrix4();
    private final Matrix4 projViewRel = new Matrix4();

    /** Cell size of each layer, in internal units. **/
    private final double[] layerSize = new double[N_LAYERS];
    /** Whether each layer has been given a size yet. **/
    private final boolean[] layerReady = new boolean[N_LAYERS];
    /** Weight of each layer this frame, in [0,1]. **/
    private final float[] layerWeight = new float[N_LAYERS];

    /**
     * Particles per layer, and vertices per layer. Both are computed in {@link #initVertices()}, not
     * in the constructor: the base class calls that method from its own constructor, so any field
     * assigned here is still zero when the mesh is built.
     */
    private int particlesPerLayer;
    private int verticesPerLayer;

    public ParticleStreakRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] programs) {
        super(sceneRenderer, rg, alphas, programs);
        ct = new ComponentTypes(ComponentType.valueOf("Effects"));
    }

    @Override
    protected void initShaderProgram() {
        // Nothing to initialize: every uniform is set per frame.
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
        // This method runs inside the base class constructor, before this class's own fields are
        // initialized, so the counts have to be computed here and not kept in the constructor.
        particlesPerLayer = PARTICLES_PER_STEP * (GaiaSky.settings().graphics.quality.ordinal() + 1);
        verticesPerLayer = particlesPerLayer * VERTICES_PER_PARTICLE;
        int nVertices = N_LAYERS * verticesPerLayer;

        int mdi = createMeshData();
        curr = meshes.get(mdi);

        VertexAttribute[] attributes = buildVertexAttributes();
        curr.mesh = new IntMesh(true, nVertices, 0, attributes);
        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.vertices = new float[nVertices * curr.vertexSize];
        curr.colorOffset = 0;

        fillVertices(curr.vertices, curr.vertexSize);

        // The buffer never changes after this: the seeds are all the state there is.
        curr.mesh.setVertices(curr.vertices, 0, curr.vertices.length);
        curr.numVertices = nVertices;
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attributes = new Array<>();
        // A position in [0,1) that the shader wraps into the cell.
        attributes.add(new VertexAttribute(Usage.Position, 3, "a_seed"));
        // Per-particle variation of width, brightness and colour.
        attributes.add(new VertexAttribute(Usage.Generic, 2, "a_rand"));

        VertexAttribute[] array = new VertexAttribute[attributes.size];
        for (int i = 0; i < attributes.size; i++) {
            array[i] = attributes.get(i);
        }
        return array;
    }

    private void fillVertices(float[] vertices,
                              int stride) {
        // Fixed seed: the field is the same on every run, which keeps the frames comparable.
        var random = new Random(0x5EED1E55L);
        int i = 0;
        for (int p = 0; p < N_LAYERS * particlesPerLayer; p++) {
            float sx = random.nextFloat();
            float sy = random.nextFloat();
            float sz = random.nextFloat();
            float r0 = random.nextFloat();
            float r1 = random.nextFloat();
            for (int v = 0; v < VERTICES_PER_PARTICLE; v++) {
                vertices[i] = sx;
                vertices[i + 1] = sy;
                vertices[i + 2] = sz;
                vertices[i + 3] = r0;
                vertices[i + 4] = r1;
                i += stride;
            }
        }
    }

    @Override
    public void renderStud(List<IRenderable> renderables,
                           ICamera camera,
                           double t) {
        float alpha = getAlpha(ct);
        if (alpha <= 0f || curr == null) {
            return;
        }

        double dt = MathUtilsDouble.clamp(Gdx.graphics.getDeltaTime(), 1.0e-4, 0.5);

        // Velocity of the camera relative to the scene, in U/s. dPos is previous minus current, so
        // the motion is its negative. When the camera is position-locked, the focus moves with it and
        // its own motion is not motion of the view: subtract it, as the motion blur does.
        camera.getDPos().put(auxD);
        var focusDx = camera.getFocusDx();
        if (focusDx != null && camera.getFocus() != null && GaiaSky.settings().scene.camera.focusLock.position) {
            focusDx.put(auxD2);
            auxD.add(auxD2);
        }
        auxD.scl(-1.0 / dt);
        velocity.lerp(auxD, 1.0 - Math.exp(-dt / SMOOTHING_TAU));

        double speed = velocity.len();
        if (!Double.isFinite(speed)) {
            return;
        }

        // The cell the current speed asks for. The effective scale is whichever is larger, the zoom
        // or the speed, and the cell is a fixed number of those: what makes the effect behave the
        // same over the whole range and keeps the crossing time of the field bounded at high speed.
        double scale = Math.max(camera.getCurrent().speedScaling(), speed);
        double target = MathUtilsDouble.clamp(CELL_SCALE_UNITS * scale, MIN_CELL, MAX_CELL);

        updateLayers(target);

        float weightSum = 0f;
        for (int k = 0; k < N_LAYERS; k++) {
            weightSum += layerWeight[k];
        }
        if (weightSum <= 1e-5f) {
            return;
        }

        // Screen metrics.
        int w = Math.max(rc != null ? rc.w() : Gdx.graphics.getWidth(), 1);
        int h = Math.max(rc != null ? rc.h() : Gdx.graphics.getHeight(), 1);
        float pxScale = rc != null ? Math.max(rc.scaleFactor, 0.25f) : 1f;
        float fovY = camera.getCamera().fieldOfView * MathUtils.degreesToRadians;
        float fPx = 0.5f * h / (float) Math.tan(0.5f * fovY);

        // The projection, without the translation of the view: the shader works camera-relative.
        viewRot.set(camera.getCamera().view);
        viewRot.val[Matrix4.M03] = 0f;
        viewRot.val[Matrix4.M13] = 0f;
        viewRot.val[Matrix4.M23] = 0f;
        projViewRel.set(camera.getCamera().projection).mul(viewRot);

        // Offset of each cell in the world, in double: the camera position is far too large to wrap
        // in a float, but the offset itself is smaller than the cell. Computed per layer, since each
        // has its own cell size.
        double posX = camera.getPos().x.doubleValue();
        double posY = camera.getPos().y.doubleValue();
        double posZ = camera.getPos().z.doubleValue();

        ExtShaderProgram shaderProgram = getShaderProgram();
        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projView", projViewRel);
        shaderProgram.setUniformf("u_res", (float) w, (float) h);
        shaderProgram.setUniformf("u_vel", (float) velocity.x, (float) velocity.y, (float) velocity.z);
        shaderProgram.setUniformf("u_minPx", 1.5f * pxScale);
        shaderProgram.setUniformf("u_maxPx", Math.max(32f, 0.55f * h));
        shaderProgram.setUniformf("u_widthPx", 1.15f * pxScale);
        shaderProgram.setUniformf("u_near", (float) Math.max(camera.getNear(), 1e-9));
        shaderProgram.setUniformf("u_fade", FADE_INNER_LO, FADE_INNER_HI, FADE_OUTER_LO, FADE_OUTER_HI);

        // Relativistic aberration and gravitational waves, if they are on.
        addEffectsUniforms(shaderProgram, camera);

        for (int k = 0; k < N_LAYERS; k++) {
            float weight = layerWeight[k];
            if (weight <= 1e-3f || speed <= 0.0) {
                continue;
            }
            double size = layerSize[k];
            double dRef = 0.5 * size;

            // How fast the view moves, in screen heights per second at the reference depth. This is
            // the only speed that is meaningful across the whole range of scales of the scene.
            double heightAtRef = 2.0 * dRef * Math.tan(0.5f * fovY);
            float rate = (float) (speed / Math.max(heightAtRef, 1e-12));
            float full = MathUtils.clamp((rate - RATE_IN) / (RATE_FULL - RATE_IN), 0f, 1f);
            float strength = full * full * (3f - 2f * full);

            float uAlpha = alpha * 0.7f * strength * weight / weightSum;
            if (uAlpha < 0.002f) {
                continue;
            }

            // Length of a streak at the reference depth, in pixels, growing with the speed of the view.
            float lenPx = MathUtils.lerp(LEN_MIN_FRAC, LEN_MAX_FRAC, strength) * h;
            // Travel time that projects to that length at the reference depth.
            float tau = (float) (lenPx * dRef / (fPx * speed));

            shaderProgram.setUniformf("u_wrap", (float) wrap(-posX, size), (float) wrap(-posY, size),
                                      (float) wrap(-posZ, size));
            shaderProgram.setUniformf("u_size", (float) size);
            shaderProgram.setUniformf("u_tau", tau);
            shaderProgram.setUniformf("u_alpha", uAlpha);

            // Never ask for more vertices than the mesh holds. A count past the end of the buffer is
            // not a GL error: the driver reads the vertex data and faults, which is a process crash.
            int count = Math.min(verticesPerLayer, curr.mesh.getNumVertices() - k * verticesPerLayer);
            if (count <= 0) {
                continue;
            }
            curr.mesh.render(shaderProgram, GL20.GL_TRIANGLES, k * verticesPerLayer, count);
        }

        shaderProgram.end();
    }

    /**
     * Gives each layer its weight for this frame, and re-sizes the ones that fall out of range.
     * <p>
     * A layer is at full weight while its cell is close to the one the current speed asks for, fades
     * to nothing at a fixed ratio away from it, and is only allowed to change size once its weight is
     * zero: a visible layer that changed size would drag all of its particles sideways.
     */
    private void updateLayers(double target) {
        for (int k = 0; k < N_LAYERS; k++) {
            if (!layerReady[k]) {
                // Spread the initial sizes so that they do not all fall out of range together.
                layerSize[k] = target * (1.0 + 0.5 * k);
                layerReady[k] = true;
            }
        }

        for (int k = 0; k < N_LAYERS; k++) {
            layerWeight[k] = weight(layerSize[k] / target);
            if (layerWeight[k] <= 0f) {
                // Invisible: free to be re-sized, and the jump it makes cannot be seen.
                layerSize[k] = target;
                layerWeight[k] = 1f;
            }
        }
    }

    /**
     * Weight of a layer whose cell differs from the target by {@code ratio}, from 1 while the two are
     * close to 0 at {@link #WEIGHT_ZERO_RATIO} either way.
     */
    private static float weight(double ratio) {
        double t;
        if (ratio >= 1.0) {
            t = (WEIGHT_ZERO_RATIO - ratio) / (WEIGHT_ZERO_RATIO - WEIGHT_FULL_RATIO);
        } else {
            t = (ratio - 1.0 / WEIGHT_ZERO_RATIO) / (1.0 / WEIGHT_FULL_RATIO - 1.0 / WEIGHT_ZERO_RATIO);
        }
        t = MathUtilsDouble.clamp(t, 0.0, 1.0);
        return (float) (t * t * (3f - 2f * t));
    }

    private static double wrap(double value,
                               double period) {
        return value - Math.floor(value / period) * period;
    }
}
