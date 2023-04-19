/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.ComponentTypes;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Vector3d;

import java.util.Comparator;
import java.util.List;

public abstract class AbstractRenderSystem implements IRenderSystem {

    protected final SceneRenderer sceneRenderer;
    private final RenderGroup group;
    private final Settings settings;
    public RenderingContext rc;
    protected ExtShaderProgram[] programs;
    protected float[] alphas;
    /** Comparator of renderables, in case of need **/
    protected Comparator<IRenderable> comp;
    protected Vector3 auxf;
    protected Vector3d auxd;
    protected Array<RenderSystemRunnable> preRunnables, postRunnables;
    private boolean vrScaleFlag = false, depthBufferFlag = false;

    protected AbstractRenderSystem(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] programs) {
        super();
        this.sceneRenderer = sceneRenderer;
        this.settings = Settings.settings;
        this.group = rg;
        this.alphas = alphas;
        this.programs = programs;
        this.auxf = new Vector3();
        this.auxd = new Vector3d();
        this.preRunnables = new Array<>(false, 1);
        this.postRunnables = new Array<>(false, 1);
    }

    @Override
    public RenderGroup getRenderGroup() {
        return group;
    }

    @Override
    public void render(List<IRenderable> renderables, ICamera camera, double t, RenderingContext rc) {
        if (renderables != null && renderables.size() != 0) {
            this.rc = rc;
            run(preRunnables, renderables, camera);
            renderStud(renderables, camera, t);
            run(postRunnables, renderables, camera);
        }
    }

    public abstract void renderStud(List<IRenderable> renderables, ICamera camera, double t);

    public void addPreRunnables(RenderSystemRunnable... r) {
        preRunnables.addAll(r);
    }

    public void addPostRunnables(RenderSystemRunnable... r) {
        postRunnables.addAll(r);
    }

    protected void run(Array<RenderSystemRunnable> runnables, List<IRenderable> renderables, ICamera camera) {
        if (runnables != null) {
            for (RenderSystemRunnable runnable : runnables)
                runnable.run(this, renderables, camera);
        }
    }

    /**
     * Computes the alpha opacity value of a given renderable using its
     * component types
     *
     * @param renderable The renderable
     *
     * @return The alpha value as the product of all the alphas of its component
     * types.
     */
    public float getAlpha(IRenderable renderable) {
        return getAlpha(renderable.getComponentType());
    }

    public float getAlpha(Entity entity) {
        return getAlpha(Mapper.base.get(entity).ct);
    }

    public float getAlpha(ComponentTypes ct) {
        int idx = -1;
        float alpha = 1f;
        while ((idx = ct.nextSetBit(idx + 1)) >= 0) {
            alpha *= alphas[idx];
        }
        return alpha;
    }

    @Override
    public void resize(int w, int h) {
        // Empty, to override in subclasses if needed
    }

    @Override
    public void updateBatchSize(int w, int h) {
        // Empty by default
    }

    protected void addEffectsUniforms(ExtShaderProgram shaderProgram, ICamera camera) {
        addRelativisticUniforms(shaderProgram, camera);
        addGravWaveUniforms(shaderProgram);
        addDepthBufferUniforms(shaderProgram, camera);
        addPreviousFrameUniforms(shaderProgram, camera);
        addVRScale(shaderProgram);
    }

    protected void addVRScale(ExtShaderProgram shaderProgram) {
        if (!vrScaleFlag) {
            shaderProgram.setUniformf("u_vrScale", (float) Constants.DISTANCE_SCALE_FACTOR);
            vrScaleFlag = true;
        }
    }

    protected void addRelativisticUniforms(ExtShaderProgram shaderProgram, ICamera camera) {
        if (settings.runtime.relativisticAberration) {
            RelativisticEffectsManager rem = RelativisticEffectsManager.getInstance();
            shaderProgram.setUniformf("u_velDir", rem.velDir);
            shaderProgram.setUniformf("u_vc", rem.vc);
        }
    }

    protected void addGravWaveUniforms(ExtShaderProgram shaderProgram) {
        if (settings.runtime.gravitationalWaves) {
            RelativisticEffectsManager rem = RelativisticEffectsManager.getInstance();
            // Time in seconds - use simulation time
            shaderProgram.setUniformf("u_ts", rem.gwtime);
            // Wave frequency
            shaderProgram.setUniformf("u_omgw", rem.omgw);
            // Coordinates of wave (cartesian)
            shaderProgram.setUniformf("u_gw", rem.gw);
            // Transformation matrix
            shaderProgram.setUniformMatrix("u_gwmat3", rem.gwmat3);
            // H terms - hpluscos, hplussin, htimescos, htimessin
            shaderProgram.setUniform4fv("u_hterms", rem.hterms, 0, 4);
        }
    }

    /**
     * Uniforms needed to compute the logarithmic depth buffer. They never change, so only add if not present.
     *
     * @param shaderProgram The program.
     * @param camera        The camera.
     */
    protected void addDepthBufferUniforms(ExtShaderProgram shaderProgram, ICamera camera) {
        if (!depthBufferFlag) {
            shaderProgram.setUniformf("u_zfar", (float) camera.getFar());
            shaderProgram.setUniformf("u_k", Constants.getCameraK());
            depthBufferFlag = true;
        }
    }

    /**
     * Uniforms needed for the velocity buffer
     *
     * @param shaderProgram The program
     * @param camera        The camera
     */
    protected void addPreviousFrameUniforms(ExtShaderProgram shaderProgram, ICamera camera) {
        // Velocity buffer
        if (settings.postprocess.motionBlur.active) {
            shaderProgram.setUniformf("u_prevCamPos", camera.getPreviousPos().put(auxf));
            shaderProgram.setUniformf("u_dCamPos", auxd.set(camera.getPreviousPos()).sub(camera.getPos()).put(auxf));
            shaderProgram.setUniformMatrix("u_prevProjView", camera.getPreviousProjView());
        }
    }

    protected ExtShaderProgram getShaderProgram() {
        boolean gw = settings.runtime.gravitationalWaves;
        boolean ra = settings.runtime.relativisticAberration;
        boolean vb = settings.postprocess.motionBlur.active;
        boolean ssr = settings.postprocess.ssr.active;
        int num = (gw ? 8 : 0) + (ra ? 4 : 0) + (vb ? 2 : 0) + (ssr ? 1 : 0);
        if (SysUtils.isMac() && num == 0) {
            // TODO this is a hack till I narrow down the bug, for the moment, velocity map always computed
            num = 2;
        }
        var program = programs[num];
        if (!program.isCompiled()) {
            // Compile shader
            program.compile();
            // Initialize
            initShaderProgram();

        }
        return program;
    }

    /**
     * Initializes metadata or essential uniforms in the shader program.
     */
    protected void initShaderProgram() {
        // Empty by default, override if needed.
    }

    public void dispose() {
        preRunnables.clear();
        preRunnables = null;
        postRunnables.clear();
        postRunnables = null;
    }

    public void resetFlags() {
        vrScaleFlag = false;
        depthBufferFlag = false;
    }

    public interface RenderSystemRunnable {
        void run(AbstractRenderSystem renderSystem, List<IRenderable> renderables, ICamera camera);
    }
}
