package gaia.cu9.ari.gaiaorbit.scenegraph;

import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Vector2;

import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.CatalogInfo;
import gaia.cu9.ari.gaiaorbit.util.CatalogInfo.CatalogInfoType;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

/**
 * Node that offers fade-in and fade-out capabilities.
 * 
 * @author tsagrista
 *
 */
public class FadeNode extends AbstractPositionEntity {

    /**
     * Fade in low and high limits
     */
    private Vector2 fadeIn;

    /**
     * Fade out low and high limits
     */
    private Vector2 fadeOut;

    /**
     * Position of label
     */
    protected Vector3d labelPosition;

    /**
     * Colour of label
     */
    protected float[] labelColour;

    /**
     * The current distance at each cycle, in internal units
     */
    private double currentDistance;

    /**
     * If set, the fade distance will be computed against this object.
     * Otherwise, we use the static position in {@link SceneGraphNode.pos}
     */
    private AbstractPositionEntity position;

    /**
     * The name of the position object
     */
    private String positionobjectname;

    /**
     * Is this fade node visible?
     */
    private boolean visible = true;

    /**
     * Elapsed milliseconds since this node visibility was last changed.
     */
    private long msSinceStateChange = Long.MAX_VALUE;

    /**
     * Information on the catalog this fade node represents (particle group, octree, etc.)
     */
    protected CatalogInfo catalogInfo = null;

    public FadeNode() {
        super();
    }

    public FadeNode(String name, SceneGraphNode parent) {
        super(name, parent);
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        if (positionobjectname != null) {
            this.position = (AbstractPositionEntity) sg.getNode(positionobjectname);
        }
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
        this.opacity = opacity * this.opacity;
        translation.set(parentTransform);
        Vector3d aux = aux3d1.get();

        if (this.position == null) {
            this.currentDistance = aux.set(this.pos).sub(camera.getPos()).len() * camera.getFovFactor();
        } else {
            this.currentDistance = this.position.distToCamera;
        }

        // Update with translation/rotation/etc
        updateLocal(time, camera);

        if (children != null && GaiaSky.instance.isOn(ct)) {
            for (int i = 0; i < children.size; i++) {
                SceneGraphNode child = children.get(i);
                child.update(time, translation, camera, this.opacity);
            }
        }
    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        this.distToCamera = this.position == null ? (float) pos.dst(camera.getPos()) : this.position.distToCamera;

        // Update alpha
        this.opacity = getBaseOpacity();
        if (fadeIn != null)
            this.opacity *= MathUtilsd.lint((float) this.currentDistance, fadeIn.x, fadeIn.y, 0, 1);
        if (fadeOut != null)
            this.opacity *= MathUtilsd.lint((float) this.currentDistance, fadeOut.x, fadeOut.y, 1, 0);

        // Visibility
        this.msSinceStateChange += Gdx.graphics.getDeltaTime() * 1000;
        float visop = MathUtilsd.lint(this.msSinceStateChange, 0, GlobalConf.scene.OBJECT_FADE_MS, 0, 1);
        if (!this.visible) {
            visop = 1 - visop;
        }
        this.opacity *= visop;

        if (!this.copy && this.opacity > 0) {
            addToRenderLists(camera);
        }

    }

    protected float getBaseOpacity() {
        return 1;
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    public void setFadein(double[] fadein) {
        fadeIn = new Vector2((float) (fadein[0] * Constants.PC_TO_U), (float) (fadein[1] * Constants.PC_TO_U));
    }

    public void setFadeout(double[] fadeout) {
        fadeOut = new Vector2((float) (fadeout[0] * Constants.PC_TO_U), (float) (fadeout[1] * Constants.PC_TO_U));
    }

    public void setPosition(double[] pos) {
        this.pos.set(pos[0] * Constants.PC_TO_U, pos[1] * Constants.PC_TO_U, pos[2] * Constants.PC_TO_U);
    }

    /**
     * Sets the position of the label, in parsecs and in the internal reference
     * frame
     * 
     * @param labelposition
     */
    public void setLabelposition(double[] labelposition) {
        this.labelPosition = new Vector3d(labelposition[0] * Constants.PC_TO_U, labelposition[1] * Constants.PC_TO_U, labelposition[2] * Constants.PC_TO_U);
    }

    /**
     * Sets the label color
     * 
     * @param labelcolor
     */
    public void setLabelcolor(double[] labelcolor) {
        this.labelColour = GlobalResources.toFloatArray(labelcolor);
    }

    public void setPositionobjectname(String po) {
        this.positionobjectname = po;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        this.msSinceStateChange = 0;
    }

    public boolean isVisible() {
        return this.visible || this.msSinceStateChange <= GlobalConf.scene.OBJECT_FADE_MS;
    }

    public void setCatalogInfo(CatalogInfo info) {
        this.catalogInfo = info;
        this.catalogInfo.object = this;
    }

    public CatalogInfo getCatalogInfo() {
        return this.catalogInfo;
    }

    public void setCataloginfo(Map<String, String> map) {
        this.catalogInfo = new CatalogInfo(map.get("name"), map.get("description"), map.get("source"), CatalogInfoType.valueOf(map.get("type")), this);
    }

}
