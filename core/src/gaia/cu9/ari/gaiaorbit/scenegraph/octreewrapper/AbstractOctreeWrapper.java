package gaia.cu9.ari.gaiaorbit.scenegraph.octreewrapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import gaia.cu9.ari.gaiaorbit.data.StreamingOctreeLoader;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.system.AbstractRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.FadeNode;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.MyPools;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

/**
 * Abstract Octree wrapper with the common parts of the regular Octree wrapper
 * and the concurrent one.
 * 
 * @author Toni Sagrista
 *
 */
public abstract class AbstractOctreeWrapper extends FadeNode implements Iterable<OctreeNode> {

    public OctreeNode root;
    /** Roulette list with the objects to process **/
    protected Array<SceneGraphNode> roulette;
    public Map<AbstractPositionEntity, OctreeNode> parenthood;
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
        this.parenthood = new HashMap<AbstractPositionEntity, OctreeNode>();
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

    /**
     * Adds all the objects of the octree (recursively) to the root list.
     * 
     * @param octant
     * @param root
     */
    private void addObjectsDeep(OctreeNode octant, SceneGraphNode root) {
        if (octant.objects != null) {
            root.add(octant.objects.items);
            for (AbstractPositionEntity sgn : octant.objects) {
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

    public void add(List<AbstractPositionEntity> children, OctreeNode octant) {
        super.add(children);
        for (AbstractPositionEntity sgn : children) {
            parenthood.put(sgn, octant);
        }
    }

    public void add(AbstractPositionEntity child, OctreeNode octant) {
        super.add(child);
        parenthood.put(child, octant);
    }

    public void removeParenthood(AbstractPositionEntity child) {
        parenthood.remove(child);
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera) {
        update(time, parentTransform, camera, 1f);
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
        this.opacity = opacity;
        translation.set(parentTransform);

        // Fade node visibility applies here
        if (this.isVisible()) {
            // Update octants
            if (!copy) {

                // Compute observed octants and fill roulette list
                OctreeNode.nOctantsObserved = 0;
                OctreeNode.nObjectsObserved = 0;

                root.updateNumbers();

                root.update(translation, camera, roulette, opacity);

                if (OctreeNode.nObjectsObserved != lastNumberObjects) {
                    // Need to update the points in renderer
                    AbstractRenderSystem.POINT_UPDATE_FLAG = true;
                    lastNumberObjects = OctreeNode.nObjectsObserved;
                }

                updateLocal(time, camera);

                // Broadcast the number of objects that we will try to render
                EventManager.instance.post(Events.DEBUG3, "On display: " + OctreeNode.nObjectsObserved + ", Total loaded: " + StreamingOctreeLoader.getNLoadedStars());

                // Call the update method of all entities in the roulette list. This
                // is implemented in the subclass.
                updateOctreeObjects(time, translation, camera);

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
     * 
     * @param time
     * @param parentTransform
     * @param camera
     */
    protected abstract void updateOctreeObjects(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera);

    /**
     * Adds the octants to the render lists.
     * 
     * @param time
     */
    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        if (!copy) {
            addToRenderLists(camera, root);
        }
    }

    public void addToRenderLists(ICamera camera, OctreeNode octant) {
        if (GlobalConf.runtime.DRAW_OCTREE && octant.observed && this.opacity > 0) {
            boolean added = addToRender(octant, RenderGroup.LINE);

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
    /** Not implemented **/
    public Iterator<OctreeNode> iterator() {
        return null;
    }

    @Override
    public int getStarCount() {
        return root.nObjects;
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
            instance.name = this.name;
            instance.translation.set(this.translation);
            instance.ct = this.ct;
            if (this.localTransform != null)
                instance.localTransform.set(this.localTransform);

            return (T) instance;
        } catch (Exception e) {
            Logger.error(e);
        }
        return null;
    }

    @Override
    public void dispose() {
        sg.remove(this, true);
        root.dispose();
        parenthood.clear();
        roulette.clear();
        root = null;
        OctreeNode.maxDepth = 0;
        OctreeNode.nObjectsObserved = 0;
        OctreeNode.nOctantsObserved = 0;
        EventManager.instance.post(Events.DEBUG3, "On display: " + 0 + ", Total loaded: " + 0);
        EventManager.instance.post(Events.OCTREE_DISPOSED);
    }

}
