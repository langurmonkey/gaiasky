/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.IFadeObject;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.octreewrapper.OctreeWrapper;
import gaiasky.util.CatalogInfo;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import gaiasky.util.time.ITimeFrameProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Node that offers fade-in and fade-out capabilities.
 */
public class FadeNode extends SceneGraphNode implements IFadeObject {

    /**
     * Fade in low and high limits
     */
    private Vector2d fadeIn;

    /**
     * Fade out low and high limits
     */
    private Vector2d fadeOut;

    /**
     * Position of label
     */
    protected Vector3b labelPosition;

    /**
     * The current distance at each cycle, in internal units
     */
    protected double currentDistance;

    /**
     * If set, the fade distance is the distance between the current fade node and this object.
     * Otherwise, it is the length of the current object's position.
     */
    protected SceneGraphNode fadeObject;

    /**
     * The name of the position object
     */
    private String fadeObjectName;

    private Vector3d fadePosition;

    /**
     * Is the node already in the scene graph?
     */
    public boolean inSceneGraph = false;

    /**
     * Allows specifying the description at object level in JSON.
     */
    protected String description;

    /**
     * Information on the catalog this fade node represents (particle group, octree, etc.)
     */
    protected CatalogInfo catalogInfo = null;

    // Initial update flag
    protected boolean initialUpdate = true;

    /**
     * Is it highlighted?
     */
    protected boolean highlighted = false;
    // Plain color for highlighting
    protected boolean hlplain = false;
    // Highlight color
    protected float[] hlc;
    // Highlight all visible
    protected boolean hlallvisible = true;
    // Highlight colormap index
    protected int hlcmi;
    // Highlight colormap attribute
    protected IAttribute hlcma;
    // Highlight colormap min
    protected double hlcmmin;
    // Highlight colormap max
    protected double hlcmmax;
    // Point size scaling
    protected float pointscaling = 1;

    public FadeNode() {
        super();
        this.hlc = new float[4];
    }

    public FadeNode(String name, SceneGraphNode parent) {
        super(name, parent);
        this.hlc = new float[4];
    }

    public void initialize() {
        super.initialize();
        // Create catalog info
        initializeCatalogInfo(false, getName(), description, -1, null);
    }

    protected void initializeCatalogInfo(boolean create, String name, String desc, int nParticles, String dataFile) {
        if (this.catalogInfo == null && create) {
            // Create catalog info and broadcast
            CatalogInfo ci = new CatalogInfo(name, desc, dataFile, CatalogInfoSource.INTERNAL, 1f, this);
            ci.nParticles = nParticles;
            if (dataFile != null) {
                Path df = Path.of(Settings.settings.data.dataFile(dataFile));
                ci.sizeBytes = Files.exists(df) && Files.isRegularFile(df) ? df.toFile().length() : -1;
            } else {
                ci.sizeBytes = -1;
            }
        }
        if (this.catalogInfo != null) {
            // Insert
            EventManager.publish(Event.CATALOG_ADD, this, this.catalogInfo, false);
        }
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        if (fadeObjectName != null) {
            this.fadeObject = GaiaSky.instance.sceneGraph.getNode(fadeObjectName);
        }
    }

    public void update(ITimeFrameProvider time, final Vector3b parentTransform, ICamera camera, float opacity) {
        this.opacity = opacity;
        translation.set(parentTransform);

        if (this.fadeObject != null) {
            this.currentDistance = this.fadeObject.distToCamera;
        }else if(this.fadePosition != null) {
            this.currentDistance = D31.get().set(this.fadePosition).sub(camera.getPos()).len() * camera.getFovFactor();
        } else {
            this.currentDistance = D31.get().set(this.pos).sub(camera.getPos()).len() * camera.getFovFactor();
        }

        // Update with translation/rotation/etc
        updateLocal(time, camera);

        if (children != null) {
            if (initialUpdate || GaiaSky.instance.isOn(ct)) {
                for (int i = 0; i < children.size; i++) {
                    SceneGraphNode child = children.get(i);
                    child.update(time, translation, camera, this.opacity);
                }
                initialUpdate = false;
            }
        }
    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        this.distToCamera = this.fadeObject == null ? pos.dst(camera.getPos(), B31.get()).doubleValue() : this.fadeObject.distToCamera;

        // Opacity
        updateOpacity();

        // Visibility fading
        this.opacity *= this.getVisibilityOpacityFactor();

        if (!this.copy) {
            addToRenderLists(camera);
        }

    }

    protected void updateOpacity() {
        if (fadeIn != null)
            this.opacity *= MathUtilsd.lint((float) this.currentDistance, fadeIn.x, fadeIn.y, 0, 1);
        if (fadeOut != null)
            this.opacity *= MathUtilsd.lint((float) this.currentDistance, fadeOut.x, fadeOut.y, 1, 0);

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

    public Vector2d getFadeIn() {
        return fadeIn;
    }

    @Override
    public void setFadeIn(double nearPc, double farPc) {
        fadeIn = new Vector2d(nearPc * Constants.PC_TO_U, farPc * Constants.PC_TO_U);
    }

    public void setFadein(double[] fadein) {
        setFadeIn(fadein);
    }

    public void setFadeIn(double[] fadein) {
        if (fadein != null)
            fadeIn = new Vector2d(fadein[0] * Constants.PC_TO_U, fadein[1] * Constants.PC_TO_U);
        else
            fadeIn = null;
    }

    public Vector2d getFadeOut() {
        return fadeOut;
    }

    @Override
    public void setFadeOut(double nearPc, double farPc) {
        fadeOut = new Vector2d(nearPc * Constants.PC_TO_U, farPc * Constants.PC_TO_U);
    }

    public void setFadeout(double[] fadeout) {
        setFadeOut(fadeout);
    }

    public void setFadeOut(double[] fadeout) {
        if (fadeout != null)
            fadeOut = new Vector2d(fadeout[0] * Constants.PC_TO_U, fadeout[1] * Constants.PC_TO_U);
        else
            fadeOut = null;
    }

    public void setPosition(double[] pos) {
        this.pos.set(pos[0] * Constants.PC_TO_U, pos[1] * Constants.PC_TO_U, pos[2] * Constants.PC_TO_U);
    }

    public void setPosition(int[] pos) {
        setPosition(new double[] { pos[0], pos[1], pos[2] });
    }

    /**
     * Sets the position of the label, in parsecs and in the internal reference
     * frame.
     *
     * @param labelposition The position of the label in internal cartesian coordinates.
     */
    public void setLabelposition(double[] labelposition) {
        if (labelposition != null)
            this.labelPosition = new Vector3b(labelposition[0] * Constants.PC_TO_U, labelposition[1] * Constants.PC_TO_U, labelposition[2] * Constants.PC_TO_U);
    }

    public void setPositionobjectname(String name) {
        setFadeObjectName(name);
    }

    public void setFadeObjectName(String name) {
        this.fadeObjectName = name;
    }

    public void setFadePosition(double[] position) {
        this.fadePosition = new Vector3d(position);
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
        CatalogInfoSource type = map.get("type") != null ? CatalogInfoSource.valueOf(map.get("type")) : CatalogInfoSource.INTERNAL;
        float size = map.get("size") != null ? Parser.parseFloat(map.get("size")) : 1;
        long sizeBytes = map.get("sizebytes") != null ? Parser.parseLong(map.get("sizebytes")) : -1;
        long nObjects = map.get("nobjects") != null ? Parser.parseLong(map.get("nobjects")) : -1;
        this.catalogInfo = new CatalogInfo(name, desc, source, type, size, this);
        this.catalogInfo.sizeBytes = sizeBytes;
        this.catalogInfo.nParticles = nObjects;
    }

    /**
     * Highlight using a plain color
     *
     * @param hl         Whether to highlight
     * @param color      The plain color
     * @param allVisible All visible
     */
    public void highlight(boolean hl, float[] color, boolean allVisible) {
        this.highlighted = hl;
        if (hl) {
            this.hlplain = true;
            this.hlallvisible = allVisible;
            System.arraycopy(color, 0, this.hlc, 0, color.length);
        }
    }

    /**
     * Highlight using a colormap
     *
     * @param hl    Whether to highlight
     * @param cmi   Color map index
     * @param cma   Color map attribute
     * @param cmmin Min mapping value
     * @param cmmax Max mapping value
     */
    public void highlight(boolean hl, int cmi, IAttribute cma, double cmmin, double cmmax, boolean allVisible) {
        this.highlighted = hl;
        if (hl) {
            this.hlplain = false;
            this.hlallvisible = allVisible;
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
    public void setUp(ISceneGraph sceneGraph) {
        super.setUp(sceneGraph);
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

    public boolean isHlAllVisible() {
        return hlallvisible;
    }

    public float getPointscaling() {
        if(parent instanceof OctreeWrapper) {
            return ((OctreeWrapper) parent).getPointscaling() * pointscaling;
        }
        return pointscaling;
    }

    public void setPointscaling(float pointscaling) {
        this.pointscaling = pointscaling;
    }
}
