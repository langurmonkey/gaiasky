/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.RenderingContext;
import gaiasky.render.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.*;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.DepthTestAttribute;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

public class Billboard extends ModelBody {

    protected boolean hidden = false;
    protected double[] fade;

    protected Quaternion q;

    public Billboard() {
        super();
        q = new Quaternion();

        double thPoint = this.thresholdPoint;
        this.thresholdNone = 0.002;
        this.thresholdPoint = thPoint / 1e9;
        this.thresholdQuad = thPoint / 8;

        this.labelFactor = 1e1f;
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        if(mc != null && mc.instance != null){
            // Disable depth test
            Array<Material> mats = mc.instance.materials;
            for(Material mat : mats){
                mat.set(new DepthTestAttribute(false));
            }
        }
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        forceUpdatePosition(time, false);
        if (fade != null) {
            fadeOpacity = (float) MathUtilsd.lint(distToCamera, fade[0], fade[1], 1, 0.5);
        } else {
            fadeOpacity = 1f;
        }
    }

    /**
     * Default implementation, only sets the result of the coordinates call to
     * pos
     *
     * @param time  Time to get the coordinates
     * @param force Whether to force the update
     */
    protected void forceUpdatePosition(ITimeFrameProvider time, boolean force) {
        if (time.getHdiff() != 0 || force) {
            coordinatesTimeOverflow = coordinates.getEquatorialCartesianCoordinates(time.getTime(), pos) == null;
        }
    }

    @Override
    protected void updateLocalTransform() {
        setToLocalTransform(localTransform, true);
    }

    /**
     * Sets the local transform of this satellite
     */
    public void setToLocalTransform(Matrix4 localTransform, boolean forceUpdate) {
        if (forceUpdate) {
            // Convert to cartesian coordinates and put them in aux3 vector
            Vector3d aux3 = D31.get();
            Coordinates.cartesianToSpherical(pos, aux3);
            posSph.set((float) (Nature.TO_DEG * aux3.x), (float) (Nature.TO_DEG * aux3.y));
            DecalUtils.setBillboardRotation(q, pos.put(D32.get()).nor(), new Vector3d(0, 1, 0));

            translation.getMatrix(localTransform).scl(size).rotate(q);
        } else {
            localTransform.set(this.localTransform);
        }
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender()) {
            if (viewAngleApparent >= thresholdNone) {
                addToRender(this, RenderGroup.MODEL_DIFFUSE);
                if (renderText()) {
                    addToRender(this, RenderGroup.FONT_LABEL);
                }
            }
        }
    }

    /** Model rendering **/
    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc, RenderGroup group) {
        render(modelBatch, group, alpha, t, false);
    }

    @Override
    public boolean renderText() {
        return !hidden && super.renderText();
    }

    @Override
    public float getTextOpacity() {
        return Math.min(getOpacity(), fadeOpacity);
    }


    @Override
    public float labelSizeConcrete() {
        return size * .5e-2f;
    }

    protected float getViewAnglePow() {
        return 1f;
    }

    protected float getThOverFactorScl() {
        return 1f;
    }

    @Override
    public float textScale() {
        return 0.3f;
    }

    public float getFuzzyRenderSize(ICamera camera) {
        float computedSize = this.size;
        computedSize *= Settings.settings.scene.star.brightness * .6e-3;

        return computedSize;
    }

    public void setHidden(String hidden) {
        try {
            this.hidden = Boolean.parseBoolean(hidden);
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }
    }

    /**
     * Sets the size of this entity in parsecs
     *
     * @param sizePc The size in parsecs
     */
    public void setSizepc(Double sizePc) {
        this.size = (float) (sizePc * 2 * Constants.PC_TO_U);
    }

    public void setFade(double[] fadein) {
        fade = fadein;
        fade[0] *= Constants.PC_TO_U;
        fade[1] *= Constants.PC_TO_U;
    }
}
