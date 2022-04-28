/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.I3DTextRenderable;
import gaiasky.render.IRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.RenderingContext.CubemapSide;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.octreewrapper.AbstractOctreeWrapper;
import gaiasky.util.*;
import gaiasky.util.coord.IBodyCoordinates;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.*;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.IPosition;
import gaiasky.util.tree.OctreeNode;
import net.jafama.FastMath;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An object in the scene graph. Serves as a top class which provides the basic functionality.
 */
public class SceneGraphNode implements IStarContainer, IPosition, IVisibilitySwitch {
    public static final String ROOT_NAME = "Universe";

    protected static TLV3D D31 = new TLV3D(), D32 = new TLV3D(), D33 = new TLV3D(), D34 = new TLV3D();
    protected static TLV3B B31 = new TLV3B(), B32 = new TLV3B(), B33 = new TLV3B(), B34 = new TLV3B();
    protected static TLV3 F31 = new TLV3(), F32 = new TLV3(), F33 = new TLV3(), F34 = new TLV3();

    /**
     * Inserts the given node into the default scene graph, if it exists.
     *
     * @param node       The node to insert
     * @param addToIndex Whether to add to the index
     *
     * @return True if it was inserted, false otherwise
     */
    public static boolean insert(SceneGraphNode node, boolean addToIndex) {
        if (GaiaSky.instance.sceneGraph != null) {
            GaiaSky.instance.sceneGraph.insert(node, addToIndex);
            return true;
        }
        return false;
    }

    /**
     * The internal identifier
     **/
    public long id = -1;

    /**
     * The parent entity.
     */
    public SceneGraphNode parent;

    /**
     * List of children entities.
     */
    public Array<SceneGraphNode> children;

    /**
     * Cumulative translation object. In contrast with the position, which contains
     * the position relative to the parent, this contains the absolute position in the
     * internal reference system.
     */
    public Vector3b translation;

    /**
     * Local transform matrix. Contains the transform matrix and the
     * transformations that will be applied to this object and not to its
     * children.
     */
    public Matrix4 localTransform;

    /**
     * This transform stores only the orientation of the object. For example in
     * planets, it stores their orientation with respect to their equatorial
     * plane, but not other transformations applied to the object such as the
     * size or the rotation angle at the time.
     */
    public Matrix4d orientation;

    /**
     * The name(s) of the node, if any.
     */
    public String[] names;

    /**
     * The index of the localized name in the {@link #names} array.
     */
    public int localizedNameIndex = 0;

    /**
     * The first name of the parent object.
     */
    public String parentName = null;

    /**
     * The total number of descendants under this node.
     */
    public int numChildren;

    /**
     * Flag indicating whether the object has been computed in this step.
     */
    public boolean computed = true;

    /**
     * Is this node visible?
     */
    protected boolean visible = true;

    /**
     * Force to render the label of this entity,
     * bypassing the solid angle check
     */
    protected boolean forceLabel = false;

    /**
     * Time of last visibility change in milliseconds
     */
    protected long lastStateChangeTimeMs = 0;

    /**
     * The ownOpacity value (alpha)
     */
    public float opacity = 1f;

    /**
     * Component types, for managing visibility
     */
    public ComponentTypes ct;

    /**
     * Position of this entity in the local reference system. The units are
     * {@link gaiasky.util.Constants#U_TO_KM} by default.
     */
    public Vector3b pos;

    /**
     * Coordinates provider. Provides position coordinates depending on time.
     **/
    protected IBodyCoordinates coordinates;

    /**
     * Position in the equatorial system; ra, dec.
     */
    public Vector2d posSph;

    /**
     * Size factor in internal units.
     */
    public float size;

    /**
     * The distance to the camera from the focus center.
     */
    public double distToCamera;

    /**
     * The view angle, in radians.
     */
    public double viewAngle;

    /**
     * The view angle corrected with the field of view angle, in radians.
     */
    public double viewAngleApparent;

    /**
     * Base RGB color
     */
    public float[] cc;
    public float[] labelcolor = Settings.settings.program.ui.isUINightMode() ? new float[] { 1, 0, 0, 1 } : new float[] { 1, 1, 1, 1 };

    /**
     * Is this just a copy?
     */
    public boolean copy = false;

    /**
     * The id of the octant it belongs to, if any
     **/
    public Long octantId;

    /**
     * Its page
     **/
    public OctreeNode octant;

    public SceneGraphNode() {
        // Identity
        this.translation = new Vector3b();
        pos = new Vector3b();
        posSph = new Vector2d();
    }

    public SceneGraphNode(int id) {
        this();
        this.id = id;
    }

    public SceneGraphNode(ComponentType ct) {
        super();
        this.ct = new ComponentTypes(ct);
        pos = new Vector3b();
        posSph = new Vector2d();
    }

    public SceneGraphNode(String[] names, SceneGraphNode parent) {
        this();
        this.names = names;
        this.parent = parent;
    }

    public SceneGraphNode(String name, SceneGraphNode parent) {
        this(new String[] { name }, parent);
    }

    public SceneGraphNode(String name) {
        this(name, null);
    }

    public SceneGraphNode(SceneGraphNode parent) {
        this((String[]) null, parent);
    }

    /**
     * Adds the given SceneGraphNode list as children to this node.
     *
     * @param children The children nodes to add.
     */
    public final void add(SceneGraphNode... children) {
        if (this.children == null) {
            initChildren(this.parent == null || this instanceof AbstractOctreeWrapper ? 300000 : children.length * 5, this.parent == null ? 1000 : children.length);
        }
        for (SceneGraphNode child : children) {
            this.children.add(child);
            child.parent = this;
        }
        numChildren += children.length;
    }

    /**
     * Adds a child to the given node and updates the number of children in this
     * node and in all ancestors.
     *
     * @param child               The child node to add.
     * @param updateAncestorCount Whether to update the ancestors number of children.
     */
    public final void addChild(SceneGraphNode child, boolean updateAncestorCount) {
        if (this.children == null) {
            initChildren(this.parent == null ? 200 : 5, this.parent == null ? 100 : 1);
        }
        this.children.add(child);
        child.parent = this;
        numChildren++;

        if (updateAncestorCount) {
            // Update num children in ancestors
            SceneGraphNode ancestor = this.parent;
            while (ancestor != null) {
                ancestor.numChildren++;
                ancestor = ancestor.parent;
            }
        }
    }

    /**
     * Removes the given child from this node, if it exists.
     *
     * @param child               The child node to remove.
     * @param updateAncestorCount Whether to update the ancestors number of children.
     */
    public final void removeChild(SceneGraphNode child, boolean updateAncestorCount) {
        if (this.children.contains(child, true)) {
            this.children.removeValue(child, true);
            child.parent = null;
            numChildren--;
            if (updateAncestorCount) {
                // Update num children in ancestors
                SceneGraphNode ancestor = this.parent;
                while (ancestor != null) {
                    ancestor.numChildren--;
                    ancestor = ancestor.parent;
                }
            }
        }
    }

    /**
     * Adds a child to the given node and updates the number of children in this
     * node and in all ancestors.
     *
     * @param child               The child node to add.
     * @param updateAncestorCount Whether to update the ancestors number of children.
     * @param numChildren         The number of children this will hold.
     */
    public final void addChild(SceneGraphNode child, boolean updateAncestorCount, int numChildren) {
        if (this.children == null) {
            initChildren(numChildren, 1);
        }
        this.children.add(child);
        child.parent = this;
        numChildren++;

        if (updateAncestorCount) {
            // Update num children in ancestors
            SceneGraphNode ancestor = this.parent;
            while (ancestor != null) {
                ancestor.numChildren++;
                ancestor = ancestor.parent;
            }
        }
    }

    /**
     * Adds the given list of children as child nodes.
     *
     * @param children The children nodes to add.
     */
    public void add(List<? extends SceneGraphNode> children) {
        add(children.toArray(new SceneGraphNode[0]));
    }

    private void initChildren(int size, int grow) {
        children = new Array<>(false, size);
    }

    public SceneGraphNode getChildByNameAndType(String name, Class<? extends SceneGraphNode> clazz) {
        int size = children.size;
        for (int i = 0; i < size; i++) {
            SceneGraphNode child = children.get(i);
            if (child.getName().equalsIgnoreCase(name) && clazz.isInstance(child)) {
                return child;
            }
        }
        return null;
    }

    public SceneGraphNode getChildByName(String name) {
        int size = children.size;
        for (int i = 0; i < size; i++) {
            SceneGraphNode child = children.get(i);
            if (child.getName().equalsIgnoreCase(name)) {
                return child;
            }
        }
        return null;
    }

    public Array<SceneGraphNode> getChildrenByType(Class<? extends SceneGraphNode> clazz, Array<SceneGraphNode> list) {
        if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                if (clazz.isInstance(child))
                    list.add(child);

                child.getChildrenByType(clazz, list);
            }
        }
        return list;
    }

    public Array<SceneGraphNode> getChildrenByComponentType(ComponentType ct, Array<SceneGraphNode> list) {
        if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                ComponentTypes cct = child.getComponentType();
                if (cct != null && cct.isEnabled(ct))
                    list.add(child);

                child.getChildrenByComponentType(ct, list);
            }
        }
        return list;
    }

    public SceneGraphNode getNode(String name) {
        if (this.names != null && this.names[0].equals(name)) {
            return this;
        } else if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                SceneGraphNode n = child.getNode(name);
                if (n != null) {
                    return n;
                }
            }
        }
        return null;
    }

    public SceneGraphNode getNode(int id) {
        if (this.id >= 0 && this.id == id) {
            return this;
        } else if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                SceneGraphNode n = child.getNode(id);
                if (n != null) {
                    return n;
                }
            }
        }
        return null;
    }

    public void update(ITimeFrameProvider time, final Vector3b parentTransform, ICamera camera) {
        update(time, parentTransform, camera, 1f);
    }

    public void update(ITimeFrameProvider time, final Vector3b parentTransform, ICamera camera, float opacity) {
        this.opacity = opacity;
        translation.set(parentTransform);

        // Update with translation/rotation/etc
        updateLocal(time, camera);

        if (children != null) {
            for (int i = 0; i < children.size; i++) {
                children.get(i).update(time, translation, camera, opacity);
            }
        }
    }

    /**
     * Updates the transform matrix with the transformations that will apply to
     * the children and the local transform matrix with the transformations that
     * will apply only to this object.
     */
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        updateLocalValues(time, camera);

        this.translation.add(pos);
        this.opacity *= this.getVisibilityOpacityFactor();

        this.distToCamera = translation.lend();
        this.viewAngle = FastMath.atan(size / distToCamera);
        this.viewAngleApparent = this.viewAngle / camera.getFovFactor();
        if (!copy) {
            addToRenderLists(camera);
        }
    }

    /**
     * Adds this entity to the necessary render lists after the distance to the
     * camera and the view angle have been determined.
     */
    protected void addToRenderLists(ICamera camera) {
    }

    /**
     * This function updates all the local values before the localTransform is
     * updated. Position, rotations and scale must be updated in here.
     */
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    public void initialize() {
        if (ct == null)
            ct = new ComponentTypes(ComponentType.Others.ordinal());
    }

    public void doneLoading(AssetManager manager) {
        if (coordinates != null)
            coordinates.doneLoading(GaiaSky.instance.sceneGraph, this);
    }

    public Vector3b getPos() {
        return pos;
    }

    public boolean isCopy() {
        return copy;
    }

    /**
     * Returns the position of this entity in the internal reference system.
     *
     * @param aux The vector where the result will be put.
     *
     * @return The aux vector with the position.
     */
    public Vector3d getPosition(Vector3d aux) {
        return aux.set(pos);
    }

    public void setNames(String... names) {
        this.names = names;
        updateLocalizedName();
    }

    public void setName(String name) {
        if (names != null)
            names[0] = name;
        else
            names = new String[] { name };
        updateLocalizedName();
    }

    public void updateLocalizedName() {
        if (names != null && names.length > 0) {
            String base = names[0].toLowerCase(Locale.ROOT).replace(' ', '_');
            if (I18n.hasObject(base)) {
                String localizedName = I18n.obj(base);
                if (!localizedName.equalsIgnoreCase(names[localizedNameIndex])) {
                    if (localizedNameIndex == 0) {
                        // Add localized name to list
                        int idx = addName(localizedName);
                        localizedNameIndex = idx;
                    } else {
                        // Update it
                        setName(localizedName, localizedNameIndex);
                    }
                }
            }
        }
    }

    /**
     * Recursively updates the localized names for this node
     * and its children using the current Locale setting.
     */
    public void updateLocalizedNameRecursive() {
        updateLocalizedName();
        if (children != null) {
            for (SceneGraphNode child : children) {
                if (child != null) {
                    child.updateLocalizedNameRecursive();
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void setDescription(String description) {
    }

    /**
     * Adds a name to the list of names.
     *
     * @param name The name.
     *
     * @return The index of the added name.
     */
    public int addName(String name) {
        if (!hasName(name)) {
            if (names != null) {
                // Extend array
                String[] newNames = new String[names.length + 1];
                System.arraycopy(names, 0, newNames, 0, names.length);
                newNames[names.length] = name;
                int idx = names.length;
                names = newNames;
                return idx;
            } else {
                names = new String[] { name };
                return 0;
            }
        } else {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equalsIgnoreCase(name))
                    return i;
            }
            return -1;
        }
    }

    /**
     * Sets the given index in the names list to the given name.
     * If the index is out of bounds, nothing happens.
     *
     * @param name  The new name.
     * @param index The index in the names list.
     */
    public void setName(String name, int index) {
        if (names != null && index >= 0 && index < names.length) {
            names[index] = name;
        }
    }

    public String[] getNames() {
        return names;
    }

    public String getName() {
        return names != null ? names[0] : null;
    }

    public String getLocalizedName() {
        if (localizedNameIndex >= 0 && names.length > localizedNameIndex) {
            return names[localizedNameIndex];
        } else {
            return getName();
        }
    }

    public String namesConcat() {
        return TextUtils.concatenate(Constants.nameSeparator, names);
    }

    public boolean hasName(String candidate) {
        return hasName(candidate, false);
    }

    public boolean hasName(String candidate, boolean matchCase) {
        if (names == null) {
            return false;
        } else {
            for (String name : names) {
                if (matchCase) {
                    if (name.equals(candidate))
                        return true;
                } else {
                    if (name.equalsIgnoreCase(candidate))
                        return true;
                }
            }
        }
        return false;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setParent(String parentName) {
        this.parentName = parentName;
    }

    public void dispose() {
        if (children != null) {
            for (SceneGraphNode child : children) {
                child.dispose();
            }
        }
    }

    /**
     * Adds all the children that are focusable objects to the list.
     */
    public void addFocusableObjects(Array<IFocus> list) {
        if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                child.addFocusableObjects(list);
            }
        }
    }

    public void addNodes(Array<SceneGraphNode> nodes) {
        nodes.add(this);
        if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                child.addNodes(nodes);
            }
        }
    }

    public void setUp(ISceneGraph sceneGraph) {
    }

    public void setCt(String ct) {
        this.ct = new ComponentTypes();
        if (!ct.isEmpty())
            this.ct.set(ComponentType.valueOf(ct).ordinal());
    }

    public void setCt(String[] cts) {
        this.ct = new ComponentTypes();
        for (String s : cts) {
            if (!s.isEmpty()) {
                this.ct.set(ComponentType.valueOf(s).ordinal());
            }
        }
    }

    public ComponentTypes getCt() {
        return ct;
    }

    public ComponentTypes getComponentType() {
        return ct;
    }

    public boolean hasCt(ComponentType ct) {
        return ct != null && this.ct.isEnabled(ct);
    }

    /**
     * Gets the number of nodes contained in this node, including itself.
     *
     * @return The number of children of this node and its descendants.
     */
    public int getAggregatedChildren() {
        return numChildren + 1;
    }

    public <T extends SceneGraphNode> T getLineCopy() {
        if (this.parent != null) {
            T parentCopy = parent.getLineCopy();
            T me = getSimpleCopy();
            parentCopy.addChild(me, false, 1);
            return me;
        } else {
            return getSimpleCopy();
        }

    }

    /**
     * Gets a copy of this object but does not copy its parent or children.
     *
     * @return The copied object.
     */
    @SuppressWarnings("unchecked")
    public <T extends SceneGraphNode> T getSimpleCopy() {
        T copy = null;
        try {
            copy = (T) this.getClass().getConstructor().newInstance();
            copy.parentName = this.parentName;
            copy.copy = true;
            copy.pos.set(this.pos);
            copy.size = this.size;
            copy.distToCamera = this.distToCamera;
            copy.viewAngle = this.viewAngle;
            copy.translation.set(this.translation);
            copy.ct = this.ct;
            copy.coordinates = this.coordinates;
            if (this.localTransform != null)
                copy.localTransform.set(this.localTransform);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Logger.getLogger(this.getClass()).error(e);
        }
        return copy;
    }

    public SceneGraphNode getRoot() {
        if (this.parent == null) {
            return this;
        } else {
            return this.parent.getRoot();
        }
    }

    @Override
    public String toString() {
        if (names != null)
            return names[0];
        return super.toString();
    }

    public void returnToPool() {
        // if (this.children != null) {
        // for (SceneGraphNode child : children)
        // child.returnToPool();
        // this.children.clear();
        // }
        // Class clazz = this.getClass();
        // MyPools.get(clazz).free(this);
    }

    /**
     * Sets the computed flag of the list of nodes and their children to the
     * given value.
     *
     * @param nodes    List of nodes to set the flag to. May be null.
     * @param computed The computed value.
     */
    public void setComputedFlag(Array<SceneGraphNode> nodes, boolean computed) {
        if (nodes != null) {
            int size = nodes.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode node = nodes.get(i);
                node.computed = computed;
                setComputedFlag(node.children, computed);
            }
        }
    }

    /**
     * Adds the given renderable to the given render group list.
     *
     * @param renderable The renderable to add.
     * @param rg         The render group that identifies the renderable list.
     *
     * @return True if added, false otherwise.
     */
    protected boolean addToRender(IRenderable renderable, RenderGroup rg) {
        try {
            GaiaSky.instance.sgr.renderListsBack().get(rg.ordinal()).add(renderable);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Removes the given renderable from the given render group list.
     *
     * @param renderable The renderable to remove.
     * @param rg         The render group to remove from.
     *
     * @return True if removed, false otherwise.
     */
    protected boolean removeFromRender(IRenderable renderable, RenderGroup rg) {
        return GaiaSky.instance.sgr.renderListsBack().get(rg.ordinal()).removeValue(renderable, true);
    }

    protected boolean isInRender(IRenderable renderable, RenderGroup rg) {
        return GaiaSky.instance.sgr.renderListsBack().get(rg.ordinal()).contains(renderable, true);
    }

    protected boolean isInRender(IRenderable renderable, RenderGroup... rgs) {
        boolean is = false;
        for (RenderGroup rg : rgs)
            is = is || GaiaSky.instance.sgr.renderListsBack().get(rg.ordinal()).contains(renderable, true);
        return is;
    }

    /**
     * Gets the first ancestor of this node that is of type {@link Star}.
     *
     * @return The first ancestor of type {@link Star}.
     */
    public SceneGraphNode getFirstStarAncestor() {
        if (this instanceof Star) {
            return this;
        } else if (parent != null) {
            return parent.getFirstStarAncestor();
        } else {
            return null;
        }
    }

    @Override
    public int getStarCount() {
        return 0;
    }

    @Override
    public Vector3d getVelocity() {
        return null;
    }

    public Matrix4d getOrientation() {
        return orientation;
    }

    public boolean isVisibilityOn() {
        return GaiaSky.instance.isOn(ct);
    }

    public float getOpacity() {
        return opacity;
    }

    public int getSceneGraphDepth() {
        if (this.parent == null) {
            return 0;
        } else {
            return this.parent.getSceneGraphDepth() + 1;
        }
    }

    /**
     * Special actions to be taken for this object when adding to the index.
     *
     * @param map The index.
     */
    protected void addToIndex(Map<String, SceneGraphNode> map) {
    }

    /**
     * Special actions to be taken for this object when removing from the index. Must implement if addToIndex is implemented.
     *
     * @param map The index.
     */
    protected void removeFromIndex(Map<String, SceneGraphNode> map) {
    }

    /**
     * Whether to add this node to the index.
     *
     * @return True if the node needs to be added to the index.
     */
    public boolean mustAddToIndex() {
        return true;
    }

    /**
     * Returns whether the current position is valid (usually, when there is no
     * coordinates overflow).
     */
    public boolean isValidPosition() {
        return true;
    }

    /**
     * Gets a copy of this entity which mimics its state in the next time step with position,
     * orientation, etc.
     *
     * @return A copy of this entity in the next time step
     */
    public IFocus getNext(ITimeFrameProvider time, ICamera camera, boolean force) {
        if (!mustUpdatePosition(time) && !force) {
            return (IFocus) this;
        } else {
            // Get copy of focus and update it to know where it will be in the
            // next step
            SceneGraphNode fc = this;
            SceneGraphNode fccopy = fc.getLineCopy();
            SceneGraphNode root = fccopy.getRoot();
            root.translation.set(camera.getInversePos());
            root.update(time, root.translation, camera);

            return (IFocus) fccopy;
        }
    }

    /**
     * Gets the position of this entity in the next time step in the
     * internal reference system using the given time provider and the given
     * camera.
     *
     * @param out    The out vector where the result will be stored.
     * @param time   The time frame provider.
     * @param camera The camera.
     * @param force  Whether to force the computation if time is off.
     *
     * @return The aux vector for chaining.
     */
    public Vector3b getPredictedPosition(Vector3b out, ITimeFrameProvider time, ICamera camera, boolean force) {
        if (!mustUpdatePosition(time) && !force) {
            return getAbsolutePosition(out);
        } else {
            // Get copy of focus and update it to know where it will be in the
            // next step
            SceneGraphNode fc = this;
            SceneGraphNode fccopy = fc.getLineCopy();
            SceneGraphNode root = fccopy.getRoot();
            root.translation.set(camera.getInversePos());
            root.update(time, root.translation, camera);

            fccopy.getAbsolutePosition(out);

            // Return to pool
            SceneGraphNode ape = fccopy;
            do {
                ape.returnToPool();
                ape = ape.parent;
            } while (ape != null);

            return out;
        }
    }

    /**
     * Whether position must be recomputed for this entity. By default, only
     * when time is on
     *
     * @param time The current time
     *
     * @return True if position should be recomputed for this entity
     */
    protected boolean mustUpdatePosition(ITimeFrameProvider time) {
        return time.getHdiff() != 0;
    }

    /**
     * Returns the absolute position of this entity in the native coordinates
     * (equatorial system) and internal units
     *
     * @param out Auxiliary vector to put the result in
     *
     * @return The vector with the position
     */
    public Vector3b getAbsolutePosition(Vector3b out) {
        out.set(pos);
        SceneGraphNode entity = this;
        while (entity.parent != null) {
            entity = entity.parent;
            out.add(entity.pos);
        }
        return out;
    }

    public Vector3b getAbsolutePosition(String name, Vector3b aux) {
        return this.hasName(name) ? getAbsolutePosition(aux) : null;
    }

    public Matrix4d getAbsoluteOrientation(Matrix4d aux) {
        aux.set(orientation);
        SceneGraphNode entity = this;
        while (entity.parent != null) {
            entity = entity.parent;
            if (entity.orientation != null)
                aux.mul(entity.orientation);
        }
        return aux;
    }

    /**
     * Returns the radius in internal units
     *
     * @return The radius of the object, in internal units
     */
    public double getRadius() {
        return size / 2d;
    }

    public double getHeight(Vector3b camPos) {
        return getRadius();
    }

    public double getHeight(Vector3b camPos, boolean useFuturePosition) {
        return getRadius();
    }

    public double getHeight(Vector3b camPos, Vector3b nextPos) {
        return getRadius();
    }

    public double getHeightScale() {
        return 0;
    }

    /**
     * Returns the size (diameter) of this entity in internal units.
     *
     * @return The size in internal units.
     */
    public double getSize() {
        return size;
    }

    /**
     * Sets the absolute size (diameter) of this entity
     *
     * @param size The diameter in internal units
     */
    public void setSize(Double size) {
        this.size = size.floatValue();
    }

    /**
     * Sets the absolute size (diameter) of this entity
     *
     * @param size The diameter in internal units
     */
    public void setSize(Long size) {
        this.size = (float) size;
    }

    public Vector2d getPosSph() {
        return posSph;
    }

    public double getAlpha() {
        return posSph.x;
    }

    public double getDelta() {
        return posSph.y;
    }

    /**
     * Sets the object color, as an RGBA double array.
     *
     * @param color The color.
     */
    public void setColor(double[] color) {
        this.cc = GlobalResources.toFloatArray(color);
    }

    /**
     * Sets the object color, as an RGBA float array.
     *
     * @param color The color.
     */
    public void setColor(float[] color) {
        this.cc = color;
    }

    public float[] getColor() {
        return cc;
    }

    /**
     * Sets the label color, as an RGBA double array.
     *
     * @param color The label color.
     */
    public void setLabelcolor(double[] color) {
        this.labelcolor = GlobalResources.toFloatArray(color);
    }

    /**
     * Sets the label color, as an RGBA float array.
     *
     * @param color The label color.
     */
    public void setLabelcolor(float[] color) {
        this.labelcolor = color;
    }

    public void setLabelcolor(float[] color, String name) {
        this.setLabelcolor(color);
    }

    public float[] getLabelcolor() {
        return labelcolor;
    }

    public OctreeNode getOctant() {
        return octant;
    }

    public Vector3d computeFuturePosition() {
        return null;
    }

    /**
     * Returns the current distance to the camera in internal units.
     *
     * @return The current distance to the camera, in internal units.
     */
    public double getDistToCamera() {
        return distToCamera;
    }

    /**
     * Returns the current view angle of this entity, in radians.
     *
     * @return The view angle in radians.
     */
    public double getViewAngle() {
        return viewAngle;
    }

    /**
     * Returns the current apparent view angle (view angle corrected with the
     * field of view) of this entity, in radians.
     *
     * @return The apparent view angle in radians.
     */
    public double getViewAngleApparent() {
        return viewAngleApparent;
    }

    protected void render2DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, Vector3d pos3d) {
        Vector3 p = F31.get();
        pos3d.setVector3(p);

        camera.getCamera().project(p);
        p.x += 15;
        p.y -= 15;

        shader.setUniformf("scale", 1f);
        DecalUtils.drawFont2D(font, batch, label, p);
    }

    protected void render2DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, float x, float y) {
        render2DLabel(batch, shader, rc, font, camera, label, x, y, 1f);
    }

    protected void render2DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, float x, float y, float scale) {
        render2DLabel(batch, shader, rc, font, camera, label, x, y, scale, -1);
    }

    protected void render2DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, float x, float y, float scale, int align) {
        shader.setUniformf("u_scale", scale);
        DecalUtils.drawFont2D(font, batch, rc, label, x, y, scale, align);
    }

    protected void render3DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, BitmapFont font, ICamera camera, RenderingContext rc, String label, Vector3d pos, double distToCamera, float scale, double size, boolean forceLabel) {
        render3DLabel(batch, shader, font, camera, rc, label, pos, distToCamera, scale, size, -1, -1, forceLabel);
    }

    protected void render3DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, BitmapFont font, ICamera camera, RenderingContext rc, String label, Vector3d pos, double distToCamera, float scale, double size, float minSizeDegrees, float maxSizeDegrees, boolean forceLabel) {
        // The smoothing scale must be set according to the distance
        shader.setUniformf("u_scale", Settings.settings.scene.label.size * scale / camera.getFovFactor());

        double r = getRadius();
        if (forceLabel || r == 0 || distToCamera > r * 2d) {

            size *= Settings.settings.scene.label.size;

            float rot = 0;
            if (rc.cubemapSide == CubemapSide.SIDE_UP || rc.cubemapSide == CubemapSide.SIDE_DOWN) {
                Vector3 v1 = F31.get();
                Vector3 v2 = F32.get();
                camera.getCamera().project(v1.set((float) pos.x, (float) pos.y, (float) pos.z));
                v1.z = 0f;
                v2.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0f);
                rot = GlobalResources.angle2d(v1, v2) + (rc.cubemapSide == CubemapSide.SIDE_UP ? 90f : -90f);
            }

            shader.setUniformf("u_pos", pos.put(F31.get()));

            // Enable or disable blending
            ((I3DTextRenderable) this).textDepthBuffer();

            DecalUtils.drawFont3D(font, batch, label, (float) pos.x, (float) pos.y, (float) pos.z, size, rot, camera, !rc.isCubemap(), minSizeDegrees, maxSizeDegrees);
        }
    }

    public void setCoordinates(IBodyCoordinates coord) {
        coordinates = coord;
    }

    public IBodyCoordinates getCoordinates() {
        return coordinates;
    }

    public void setPos(double[] pos) {
        this.pos.set(pos);
    }

    @Override
    public Vector3b getPosition() {
        return pos;
    }

    public Vector3d getUnrotatedPos() {
        return null;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        this.lastStateChangeTimeMs = (long) (GaiaSky.instance.getT() * 1000f);
    }

    public boolean isVisible() {
        return this.visible || msSinceStateChange() <= Settings.settings.scene.fadeMs;
    }

    public boolean isVisible(boolean attributeValue) {
        if (attributeValue)
            return this.visible;
        else
            return this.isVisible();
    }

    public void setVisible(boolean visible, String name) {
        this.setVisible(visible);
    }

    public boolean isVisible(String name) {
        return this.isVisible();
    }

    public boolean isVisibleGroup() {
        return isVisibleGroup(false);
    }

    public boolean isVisibleGroup(boolean attributeValue) {
        return isVisible(attributeValue);
    }

    public void setVisibleGroup(boolean visibility) {
        setVisible(visibility);
    }

    protected long msSinceStateChange() {
        return (long) (GaiaSky.instance.getT() * 1000f) - this.lastStateChangeTimeMs;
    }

    protected float getVisibilityOpacityFactor() {
        long msSinceStateChange = msSinceStateChange();

        // Fast track
        if (msSinceStateChange > Settings.settings.scene.fadeMs)
            return this.visible ? 1 : 0;

        // Fading
        float visop = MathUtilsd.lint(msSinceStateChange, 0, Settings.settings.scene.fadeMs, 0, 1);
        if (!this.visible) {
            visop = 1 - visop;
        }
        return visop;
    }

    protected boolean shouldRender() {
        return GaiaSky.instance.isOn(ct) && opacity > 0 && (this.visible || msSinceStateChange() < Settings.settings.scene.fadeMs);
    }

    public void setForceLabel(Boolean forceLabel) {
        this.setForcelabel(forceLabel);
    }

    public void setForcelabel(Boolean forceLabel) {
        this.forceLabel = forceLabel;
    }

    public void setForceLabel(Boolean forceLabel, String name) {
        this.setForcelabel(forceLabel);
    }

    public boolean isForceLabel() {
        return this.forceLabel;
    }

    public boolean isForceLabel(String name) {
        return this.forceLabel;
    }
}
