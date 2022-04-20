/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.octreewrapper;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scenegraph.FadeNode;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Logger;
import gaiasky.util.MyPools;
import gaiasky.util.Settings;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.math.Vector3b;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.OctreeNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static gaiasky.render.SceneGraphRenderer.RenderGroup.LINE;

/**
 * Abstract Octree wrapper with the common parts of the regular Octree wrapper
 * and the concurrent one
 */
public abstract class AbstractOctreeWrapper extends FadeNode implements Iterable<OctreeNode> {

    public OctreeNode root;
    /** Roulette list with the objects to process **/
    protected List<SceneGraphNode> roulette;
    public Map<SceneGraphNode, OctreeNode> parenthood;
    /** The number of objects added to render in the last frame **/
    protected int lastNumberObjects = 0;
    /**
     * Is this just a copy?
     */
    protected boolean copy = false;

    protected AbstractOctreeWrapper() {
        super("Octree", null);
    }

    protected AbstractOctreeWrapper(String parentName, OctreeNode root) {
        this();
        this.ct = new ComponentTypes(ComponentType.Others);
        this.root = root;
        this.parentName = parentName;
        this.parenthood = new HashMap<>();
    }

    /**
     * An octree wrapper has as 'scene graph children' all the elements
     * contained in the octree, even though it acts as a hub that decides which
     * are processed and which are not.
     */
    @Override
    public void initialize() {
        super.initialize();
    }

    public boolean containsObject(SceneGraphNode object) {
        return root.containsObject(object);
    }

    /**
     * Adds all the objects of the octree (recursively) to the root list.
     *
     * @param octant
     * @param root
     */
    private void addObjectsDeep(OctreeNode octant, SceneGraphNode root) {
        if (octant.objects != null) {
            root.add(octant.objects);
            for (SceneGraphNode sgn : octant.objects) {
                parenthood.put(sgn, octant);
            }
        }

        for (int i = 0; i < 8; i++) {
            OctreeNode child = octant.children[i];
            if (child != null) {
                addObjectsDeep(child, root);
            }
        }
    }

    public void add(List<SceneGraphNode> children, OctreeNode octant) {
        super.add(children);
        for (SceneGraphNode sgn : children) {
            parenthood.put(sgn, octant);
        }
    }

    public void add(SceneGraphNode child, OctreeNode octant) {
        super.add(child);
        parenthood.put(child, octant);
    }

    public void removeParenthood(SceneGraphNode child) {
        parenthood.remove(child);
    }

    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);

        // Fade node visibility applies here
        if (this.isVisible()) {
            // Update octants
            if (!copy) {

                // Compute observed octants and fill roulette list
                OctreeNode.nOctantsObserved = 0;
                OctreeNode.nObjectsObserved = 0;

                root.update(translation, camera, roulette, opacity);

                if (OctreeNode.nObjectsObserved != lastNumberObjects) {
                    // Need to update the points in renderer
                    EventManager.publish(Event.STAR_POINT_UPDATE_FLAG, this, true);
                    lastNumberObjects = OctreeNode.nObjectsObserved;
                }

                // Call the update method of all entities in the roulette list. This
                // is implemented in the subclass.
                updateOctreeObjects(time, translation, camera);

                addToRenderLists(camera, root);

                // Reset mask
                roulette.clear();

                // Update focus, just in case
                IFocus focus = camera.getFocus();
                if (focus != null) {
                    SceneGraphNode star = focus.getFirstStarAncestor();
                    OctreeNode parent = parenthood.get(star);
                    if (parent != null && !parent.isObserved()) {
                        star.update(time, star.parent.translation, camera);
                    }
                }
            } else {
                // Just update children
                for (SceneGraphNode node : children) {
                    node.update(time, translation, camera);
                }
            }
        }

    }

    /**
     * Runs the update on all the observed and selected octree objects.
     */
    protected abstract void updateOctreeObjects(ITimeFrameProvider time, final Vector3b parentTransform, ICamera camera);

    public void addToRenderLists(ICamera camera, OctreeNode octant) {
        if (this.shouldRender() && Settings.settings.runtime.drawOctree && octant.observed) {
            boolean added = addToRender(octant, LINE);

            if (added)
                for (int i = 0; i < 8; i++) {
                    OctreeNode child = octant.children[i];
                    if (child != null) {
                        addToRenderLists(camera, child);
                    }
                }
        }
    }

    @Override
    /* Not implemented */ public Iterator<OctreeNode> iterator() {
        return null;
    }

    @Override
    public int getStarCount() {
        return root.numObjectsRec;
    }

    @Override
    public void highlight(boolean hl, float[] color, boolean allVisible) {
        super.highlight(hl, color, allVisible);
        Array<SceneGraphNode> l = new Array<>();
        getChildrenByType(StarGroup.class, l);
        for (SceneGraphNode n : l) {
            ((StarGroup) n).highlight(hl, color, allVisible);
        }
    }

    @Override
    public void highlight(boolean hl, int cmi, IAttribute cma, double cmmin, double cmmax, boolean allVisible) {
        super.highlight(hl, cmi, cma, cmmin, cmmax, allVisible);
        Array<SceneGraphNode> l = new Array<>();
        getChildrenByType(StarGroup.class, l);
        for (SceneGraphNode n : l) {
            ((StarGroup) n).highlight(hl, cmi, cma, cmmin, cmmax, allVisible);
        }
    }

    @Override
    public void setPointscaling(float pointscaling) {
        this.pointscaling = pointscaling;
    }

    /**
     * Gets a copy of this object but does not copy its parent or children
     *
     * @return The copied object
     */
    @Override
    public <T extends SceneGraphNode> T getSimpleCopy() {
        Class<? extends AbstractOctreeWrapper> clazz = this.getClass();
        Pool<? extends AbstractOctreeWrapper> pool = MyPools.get(clazz);
        try {
            AbstractOctreeWrapper instance = pool.obtain();
            instance.copy = true;
            instance.names = this.names;
            instance.translation.set(this.translation);
            instance.ct = this.ct;
            if (this.localTransform != null)
                instance.localTransform.set(this.localTransform);

            return (T) instance;
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }
        return null;
    }

    @Override
    public void dispose() {
        GaiaSky.instance.sceneGraph.remove(this, true);
        root.dispose();
        parenthood.clear();
        roulette.clear();
        root = null;
        OctreeNode.maxDepth = 0;
        OctreeNode.nObjectsObserved = 0;
        OctreeNode.nOctantsObserved = 0;
        EventManager.publish(Event.DEBUG_OBJECTS, this, 0, 0);
        EventManager.publish(Event.OCTREE_DISPOSED, this);
    }

}
