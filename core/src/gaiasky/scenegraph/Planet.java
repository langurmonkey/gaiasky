/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.ILineRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.SceneGraphRenderer;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.AtmosphereComponent;
import gaiasky.scenegraph.component.CloudComponent;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.camera.CameraUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

public class Planet extends ModelBody implements ILineRenderable {
    private static final double TH_ANGLE_NONE = ModelBody.TH_ANGLE_POINT / 1e6;
    private static final double TH_ANGLE_POINT = ModelBody.TH_ANGLE_POINT / 3e4;
    private static final double TH_ANGLE_QUAD = ModelBody.TH_ANGLE_POINT / 2f;

    private final Vector3d endLine = new Vector3d();

    @Override
    public double THRESHOLD_NONE() {
        return TH_ANGLE_NONE;
    }

    @Override
    public double THRESHOLD_POINT() {
        return TH_ANGLE_POINT;
    }

    @Override
    public double THRESHOLD_QUAD() {
        return TH_ANGLE_QUAD;
    }

    ICamera camera;

    /** ATMOSPHERE **/
    AtmosphereComponent ac;

    /** CLOUDS **/
    CloudComponent clc;

    public Planet() {
        super();
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isRandomizeCloud()) {
            // Ignore current cloud component (if any) and create a random one
            clc = new CloudComponent();
            clc.randomizeAll(getSeed("cloud"), size);
        }
        if (isRandomizeAtmosphere()) {
            // Ignore current atmosphere component (if any) and create a random one
            ac = new AtmosphereComponent();
            ac.randomizeAll(getSeed("atmosphere"), size);
        }
        if (clc != null) {
            clc.initialize(this.getName(), this.getId(), false);
        }
    }

    protected void setColor2Data() {
        final float plus = .6f;
        ccPale = new float[] { Math.min(1, cc[0] + plus), Math.min(1, cc[1] + plus), Math.min(1, cc[2] + plus) };
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);

        // INITIALIZE ATMOSPHERE
        if (ac != null) {
            // Initialize atmosphere model
            ac.doneLoading(mc.instance.materials.first(), this.size);
        }

        // INITIALIZE CLOUDS
        if (clc != null) {
            clc.doneLoading(manager);
        }

    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);
        this.camera = camera;
    }

    @Override
    protected void updateLocalTransform() {
        super.updateLocalTransform();
        if (ac != null) {
            ac.update(translation);
        }
        if (clc != null) {
            clc.update(translation);
            setToLocalTransform(clc.size, 1, clc.localTransform, true);
        }

    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        forceUpdateLocalValues(time);
    }

    protected void forceUpdateLocalValues(ITimeFrameProvider time) {
        if (time.getHdiff() == 0) {
            return;
        }
        Vector3d aux3 = aux3d1.get();
        // Load this object's equatorial cartesian coordinates into pos
        coordinatesTimeOverflow = coordinates.getEquatorialCartesianCoordinates(time.getTime(), pos) == null;

        // Convert to cartesian coordinates and put them in aux3 vector
        Coordinates.cartesianToSpherical(pos, aux3);
        posSph.set((float) (Nature.TO_DEG * aux3.x), (float) (Nature.TO_DEG * aux3.y));
        // Update angle
        if (rc != null)
            rc.update(time);
    }

    /**
     * Renders model
     */
    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc, RenderGroup group) {

        if (group == RenderGroup.MODEL_ATM) {
            // Atmosphere
            renderAtmosphere(modelBatch, SceneGraphRenderer.alphas[ComponentType.Atmospheres.ordinal()], rc);
        } else if (group == RenderGroup.MODEL_CLOUD) {
            // Clouds
            renderClouds(modelBatch, SceneGraphRenderer.alphas[ComponentType.Clouds.ordinal()], t);
        } else {
            // If atmosphere ground params are present, set them
            if (ac != null) {
                float atmOpacity = (float) MathUtilsd.lint(viewAngle, 0.00745329f, 0.02490659f, 0f, 1f);
                if (Settings.settings.scene.visibility.get(ComponentType.Atmospheres.toString()) && atmOpacity > 0) {
                    ac.updateAtmosphericScatteringParams(mc.instance.materials.first(), alpha * atmOpacity, true, this, rc.vrOffset);
                } else {
                    ac.removeAtmosphericScattering(mc.instance.materials.first());
                }
            }
            // Regular planet, render model normally
            compalpha = alpha;
            prepareShadowEnvironment();
            mc.update(alpha * opacity);
            modelBatch.render(mc.instance, mc.env);
        }
    }

    /**
     * Renders the atmosphere
     */
    public void renderAtmosphere(IntModelBatch modelBatch, float alpha, RenderingContext rc) {
        // Atmosphere fades in between 1 and 2 degrees of view angle apparent
        ICamera cam = GaiaSky.instance.getICamera();
        float atmOpacity = (float) MathUtilsd.lint(viewAngle, 0.00745329f, 0.02490659f, 0f, 1f);
        if (atmOpacity > 0) {
            ac.updateAtmosphericScatteringParams(ac.mc.instance.materials.first(), alpha * atmOpacity, false, this, rc.vrOffset);
            ac.mc.updateRelativisticEffects(cam);
            modelBatch.render(ac.mc.instance, mc.env);
        }
    }

    /**
     * Renders the clouds
     */
    public void renderClouds(IntModelBatch modelBatch, float alpha, double t) {
        clc.touch();
        ICamera cam = GaiaSky.instance.getICamera();
        clc.mc.updateRelativisticEffects(cam);
        clc.mc.updateVelocityBufferUniforms(cam);
        clc.mc.setTransparency(alpha * opacity, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR);
        modelBatch.render(clc.mc.instance, mc.env);
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        super.addToRenderLists(camera);
        // Add atmosphere to default render group if necessary
        if (ac != null && isInRender(this, RenderGroup.MODEL_PIX, RenderGroup.MODEL_PIX_TESS) && !coordinatesTimeOverflow) {
            addToRender(this, RenderGroup.MODEL_ATM);
        }
        // Cloud
        if (clc != null && isInRender(this, RenderGroup.MODEL_PIX, RenderGroup.MODEL_PIX_TESS) && !coordinatesTimeOverflow) {
            addToRender(this, RenderGroup.MODEL_CLOUD);
        }
    }

    @Override
    public boolean hasAtmosphere() {
        return ac != null;
    }

    public void setAtmosphere(AtmosphereComponent ac) {
        this.ac = ac;
    }

    public void setCloud(CloudComponent clc) {
        this.clc = clc;
    }

    @Override
    protected float labelFactor() {
        return (float) (1.5e1 * Constants.DISTANCE_SCALE_FACTOR);
    }

    public void dispose() {

    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        renderer.addLine(this, translation.x.doubleValue(), translation.y.doubleValue(), translation.z.doubleValue(), endLine.x, endLine.y, endLine.z, 1, 0, 0, 1);
    }

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINE_STRIP;
    }

    @Override
    protected boolean checkClickDistance(int screenX, int screenY, Vector3 pos, NaturalCamera camera, PerspectiveCamera pcamera, double pixelSize) {
        Vector3 aux1 = aux3f1.get();
        Vector3 aux2 = aux3f2.get();
        Vector3 aux3 = aux3f3.get();
        Vector3 aux4 = aux3f4.get();
        return super.checkClickDistance(screenX, screenY, pos, camera, pcamera, pixelSize) || CameraUtils.intersectScreenSphere(this, camera, screenX, screenY, aux1, aux2, aux3, aux4);
    }

    @Override
    public float getLineWidth() {
        return 1;
    }

}
