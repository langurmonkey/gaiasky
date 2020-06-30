/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Vector2;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.CatalogInfo;
import gaiasky.util.CatalogInfo.CatalogInfoType;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.Map;

/**
 * Node that offers fade-in and fade-out capabilities.
 *
 * @author tsagrista
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
    protected float[] labelcolor;

    /**
     * The current distance at each cycle, in internal units
     */
    private double currentDistance;

    /**
     * If set, the fade distance is the distance between the current fade node and this object.
     * Otherwise, it is the length of the current object's position.
     */
    private AbstractPositionEntity position;

    /**
     * The name of the position object
     */
    private String positionObjectName;

    /**
     * Is this fade node visible?
     */
    private boolean visible = true;

    /**
     * Is the node already in the scene graph?
     */
    public boolean inSceneGraph = false;

    /**
     * Time of last visibility change in milliseconds
     */
    private long lastStateChangeTimeMs = 0;

    /**
     * Information on the catalog this fade node represents (particle group, octree, etc.)
     */
    protected CatalogInfo catalogInfo = null;

    // Initial update flag
    private boolean initialUpdate = true;

    /**
     * Is it highlighted?
     */
    protected boolean highlighted = false;
    // Plain color for highlighting
    protected boolean hlplain = false;
    // Highlight color
    protected float[] hlc;
    // Highlight colormap index
    protected int hlcmi;
    // Hightlight colormap attribute
    protected IAttribute hlcma;
    // Highlight colormap min
    protected double hlcmmin;
    // Highlight colormap max
    protected double hlcmmax;

    public FadeNode() {
        super();
        this.hlc = new float[4];
    }

    public FadeNode(String name, SceneGraphNode parent) {
        super(name, parent);
        this.hlc = new float[4];
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        if (positionObjectName != null) {
            this.position = (AbstractPositionEntity) sg.getNode(positionObjectName);
        }
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
        this.opacity = opacity;
        translation.set(parentTransform);
        Vector3d aux = aux3d1.get();

        if (this.position == null) {
            this.currentDistance = aux.set(this.pos).sub(camera.getPos()).len() * camera.getFovFactor();
        } else {
            this.currentDistance = this.position.distToCamera;
        }

        // Update with translation/rotation/etc
        updateLocal(time, camera);

        if (children != null && (initialUpdate || GaiaSky.instance.isOn(ct))) {
            for (int i = 0; i < children.size; i++) {
                SceneGraphNode child = children.get(i);
                child.update(time, translation, camera, this.opacity);
            }
            initialUpdate = false;
        }
    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        this.distToCamera = this.position == null ? (float) pos.dst(camera.getPos()) : this.position.distToCamera;

        // Update alpha
        //this.opacity = getBaseOpacity();
        if (fadeIn != null)
            this.opacity *= MathUtilsd.lint((float) this.currentDistance, fadeIn.x, fadeIn.y, 0, 1);
        if (fadeOut != null)
            this.opacity *= MathUtilsd.lint((float) this.currentDistance, fadeOut.x, fadeOut.y, 1, 0);

        // Visibility
        float visop = MathUtilsd.lint(msSinceStateChange(), 0, GlobalConf.scene.OBJECT_FADE_MS, 0, 1);
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
        if (fadein != null)
            fadeIn = new Vector2((float) (fadein[0] * Constants.PC_TO_U), (float) (fadein[1] * Constants.PC_TO_U));
    }

    public void setFadeout(double[] fadeout) {
        if (fadeout != null)
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
        if (labelposition != null)
            this.labelPosition = new Vector3d(labelposition[0] * Constants.PC_TO_U, labelposition[1] * Constants.PC_TO_U, labelposition[2] * Constants.PC_TO_U);
    }

    /**
     * Sets the label color
     *
     * @param labelcolor
     */
    @Override
    public void setLabelcolor(double[] labelcolor) {
        this.labelcolor = GlobalResources.toFloatArray(labelcolor);
    }

    @Override
    public void setLabelcolor(float[] labelcolor) {
        this.labelcolor = labelcolor;
    }

    public void setPositionobjectname(String po) {
        this.positionObjectName = po;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        this.lastStateChangeTimeMs = (long) (GaiaSky.instance.getT() * 1000f);
    }

    public boolean isVisible() {
        return this.visible || msSinceStateChange() <= GlobalConf.scene.OBJECT_FADE_MS;
    }

    private long msSinceStateChange() {
        return (long) (GaiaSky.instance.getT() * 1000f) - this.lastStateChangeTimeMs;
    }

    public void setCatalogInfoBare(CatalogInfo info) {
        this.catalogInfo = info;
    }

    public void setCatalogInfo(CatalogInfo info) {
        this.catalogInfo = info;
        this.catalogInfo.object = this;
    }

    public CatalogInfo getCatalogInfo() {
        return this.catalogInfo;
    }

    public void setCataloginfo(Map<String, String> map) {
        String name = map.get("name");
        String desc = map.get("description");
        String source = map.get("source");
        CatalogInfoType type = map.get("type") != null ? CatalogInfoType.valueOf(map.get("type")) : CatalogInfoType.INTERNAL;
        float size = map.get("size") != null ? Parser.parseFloat(map.get("size")) : 1;
        this.catalogInfo = new CatalogInfo(name, desc, source, type, size, this);
        EventManager.instance.post(Events.CATALOG_ADD, this.catalogInfo, false);
    }

    /**
     * Highlight using a plain color
     *
     * @param hl    Whether to highlight
     * @param color The plain color
     */
    public void highlight(boolean hl, float[] color) {
        this.highlighted = hl;
        if (hl) {
            this.hlplain = true;
            System.arraycopy(color, 0, this.hlc, 0, color.length);
        }
    }

    /**
     * Highlight using a colormap
     *
     * @param hl    Whether to highlight
     * @param cmi   Color map index
     * @param cma   Attribute
     * @param cmmin Min mapping value
     * @param cmmax Max mapping value
     */
    public void highlight(boolean hl, int cmi, IAttribute cma, double cmmin, double cmmax) {
        this.highlighted = hl;
        if (hl) {
            this.hlplain = false;
            this.hlcmi = cmi;
            this.hlcma = cma;
            this.hlcmmin = cmmin;
            this.hlcmmax = cmmax;
        }
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    @Override
    public void setUp() {
        super.setUp();
        inSceneGraph = true;
    }

    @Override
    public void setSize(Long size) {
        this.size = (long) (size * Constants.DISTANCE_SCALE_FACTOR);
    }

    @Override
    public void setSize(Double size) {
        this.size = (float) (size * Constants.DISTANCE_SCALE_FACTOR);
    }

    public boolean isHlplain() {
        return hlplain;
    }

    public int getHlcmi() {
        return hlcmi;
    }

    public IAttribute getHlcma() {
        return hlcma;
    }

    public double getHlcmmin() {
        return hlcmmin;
    }

    public double getHlcmmax() {
        return hlcmmax;
    }
}
