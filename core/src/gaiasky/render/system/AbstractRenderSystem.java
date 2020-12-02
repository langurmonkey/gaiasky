/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.math.Vector3;
import gaiasky.desktop.util.SysUtils;
import gaiasky.render.ComponentTypes;
import gaiasky.render.IRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.SceneGraphRenderer;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractRenderSystem implements IRenderSystem {
    /**
     * When this is true, new point information is available, so new data is
     * streamed to the GPU
     **/
    public static boolean POINT_UPDATE_FLAG = true;

    protected ExtShaderProgram[] programs;
    private final RenderGroup group;
    protected float[] alphas;
    /** Comparator of renderables, in case of need **/
    protected Comparator<IRenderable> comp;
    public RenderingContext rc;
    protected Vector3 aux;
    protected Vector3d auxd;

    private boolean vrScaleFlag = false, depthBufferFlag = false;

    protected List<RenderSystemRunnable> preRunnables, postRunnables;

    protected AbstractRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] programs) {
        super();
        this.group = rg;
        this.alphas = alphas;
        this.programs = programs;
        this.aux = new Vector3();
        this.auxd = new Vector3d();
        this.preRunnables = new ArrayList<>(3);
        this.postRunnables = new ArrayList<>(3);
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
        preRunnables.addAll(Arrays.asList(r));
    }

    public void addPostRunnables(RenderSystemRunnable... r) {
        postRunnables.addAll(Arrays.asList(r));
    }

    protected void run(List<RenderSystemRunnable> runnables, List<IRenderable> renderables, ICamera camera) {
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
     * @return The alpha value as the product of all the alphas of its component
     * types.
     */
    public float getAlpha(IRenderable renderable) {
        return getAlpha(renderable.getComponentType());
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

    public interface RenderSystemRunnable {
        void run(AbstractRenderSystem renderSystem, List<IRenderable> renderables, ICamera camera);
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
        if (GlobalConf.runtime.RELATIVISTIC_ABERRATION) {
            RelativisticEffectsManager rem = RelativisticEffectsManager.getInstance();
            shaderProgram.setUniformf("u_velDir", rem.velDir);
            shaderProgram.setUniformf("u_vc", rem.vc);
        }
    }

    protected void addGravWaveUniforms(ExtShaderProgram shaderProgram) {
        if (GlobalConf.runtime.GRAVITATIONAL_WAVES) {
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
     * Uniforms needed to compute the logarithmic depth buffer. They never change, so only add if not present
     *
     * @param shaderProgram The program
     * @param camera        The camera
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
        if(GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR) {
            shaderProgram.setUniformf("u_prevCamPos", camera.getPreviousPos().put(aux));
            shaderProgram.setUniformf("u_dCamPos", auxd.set(camera.getPreviousPos()).sub(camera.getPos()).put(aux));
            shaderProgram.setUniformMatrix("u_prevProjView", camera.getPreviousProjView());
        }
    }

    protected ExtShaderProgram getShaderProgram() {
        try {
            if (!GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR && !GlobalConf.runtime.RELATIVISTIC_ABERRATION && !GlobalConf.runtime.GRAVITATIONAL_WAVES)
                //return programs[0];
                // TODO this is a hack till I narrow down the bug, for the moment, velocity map always computed
                return programs[SysUtils.isMac() ? 1 : 0];
            if (GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR && !GlobalConf.runtime.RELATIVISTIC_ABERRATION && !GlobalConf.runtime.GRAVITATIONAL_WAVES)
                return programs[1];
            else if (!GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR && GlobalConf.runtime.RELATIVISTIC_ABERRATION && !GlobalConf.runtime.GRAVITATIONAL_WAVES)
                return programs[2];
            else if (!GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR && !GlobalConf.runtime.RELATIVISTIC_ABERRATION && GlobalConf.runtime.GRAVITATIONAL_WAVES)
                return programs[3];
            else if (GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR && GlobalConf.runtime.RELATIVISTIC_ABERRATION && !GlobalConf.runtime.GRAVITATIONAL_WAVES)
                return programs[4];
            else if (GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR && !GlobalConf.runtime.RELATIVISTIC_ABERRATION && GlobalConf.runtime.GRAVITATIONAL_WAVES)
                return programs[5];
            else if (!GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR && GlobalConf.runtime.RELATIVISTIC_ABERRATION && GlobalConf.runtime.GRAVITATIONAL_WAVES)
                return programs[6];
            else if (GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR && GlobalConf.runtime.RELATIVISTIC_ABERRATION && GlobalConf.runtime.GRAVITATIONAL_WAVES)
                return programs[7];
        } catch (Exception e) {
        }
        return programs[1];
    }

}
