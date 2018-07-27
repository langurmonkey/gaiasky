package gaia.cu9.ari.gaiaorbit.util.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.StreamingOctreeLoader;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.ILineRenderable;
import gaia.cu9.ari.gaiaorbit.render.system.AbstractRenderSystem;
import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.scenegraph.Transform;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Pair;
import gaia.cu9.ari.gaiaorbit.util.math.BoundingBoxd;
import gaia.cu9.ari.gaiaorbit.util.math.Frustumd;
import gaia.cu9.ari.gaiaorbit.util.math.Intersectord;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Rayd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Octree node implementation which contains a list of {@link IPosition} objects
 * and possibly 8 subnodes.
 * 
 * @author Toni Sagrista
 */
public class OctreeNode implements ILineRenderable {
    public static int nOctantsObserved = 0;
    public static int nObjectsObserved = 0;
    /** Max depth of the structure this node belongs to **/
    public static int maxDepth;
    /** Is dynamic loading active? **/
    public static boolean LOAD_ACTIVE;

    /**
     * Since OctreeNode is not to be parallelised, these can be static.
     **/
    private static BoundingBoxd boxcopy = new BoundingBoxd(new Vector3d(), new Vector3d());
    private static Vector3d auxD1 = new Vector3d(), auxD3 = new Vector3d(), auxD4 = new Vector3d();
    private static Rayd ray = new Rayd(new Vector3d(), new Vector3d());

    private Vector3d aux3d1;

    /** The load status of this node **/
    private LoadStatus status;
    /** The unique page identifier **/
    public long pageId;
    /** Contains the bottom-left-front position of the octant **/
    public final Vector3d blf;
    /** Contains the top-right-back position of the octant **/
    public final Vector3d trb;
    /** The centre of this octant **/
    public final Vector3d centre;
    /** The bounding box **/
    public final BoundingBoxd box;
    /** Octant size in x, y and z **/
    public final Vector3d size;
    /** Contains the depth level **/
    public final int depth;
    /** Number of objects contained in this node and its descendants **/
    public int nObjects;
    /** Number of objects contained in this node **/
    public int ownObjects;
    /** Number of children nodes of this node **/
    public int childrenCount;
    /** The parent, if any **/
    public OctreeNode parent;
    /** Children nodes **/
    public OctreeNode[] children = new OctreeNode[8];
    /** List of objects **/
    public Array<AbstractPositionEntity> objects;

    private double radius;
    /** If observed, the view angle in radians of this octant **/
    public double viewAngle;
    /** The distance to the camera in units of the center of this octant **/
    public double distToCamera;
    /** Is this octant observed in this frame? **/
    public boolean observed;
    /** Camera transform to render **/
    Vector3d transform;
    /** The opacity of this node **/
    public float opacity;

    /**
     * Constructs an octree node
     * 
     * @param x
     * @param y
     * @param z
     * @param hsx
     * @param hsy
     * @param hsz
     * @param depth
     */
    public OctreeNode(double x, double y, double z, double hsx, double hsy, double hsz, int depth) {
        this.blf = new Vector3d(x - hsx, y - hsy, z - hsz);
        this.trb = new Vector3d(x + hsx, y + hsy, z + hsz);
        this.centre = new Vector3d(x, y, z);
        this.size = new Vector3d(hsx * 2, hsy * 2, hsz * 2);
        this.box = new BoundingBoxd(blf, trb);
        this.aux3d1 = new Vector3d();
        this.depth = depth;
        this.transform = new Vector3d();
        this.observed = false;
        this.status = LoadStatus.NOT_LOADED;
        this.radius = Math.sqrt(hsx * hsx + hsy * hsy + hsz * hsz);
    }

    /**
     * Constructs an octree node
     * @param pageId
     * @param x
     * @param y
     * @param z
     * @param hsx
     * @param hsy
     * @param hsz
     * @param depth
     */
    public OctreeNode(long pageId, double x, double y, double z, double hsx, double hsy, double hsz, int depth) {
        this.pageId = pageId;
        this.blf = new Vector3d(x - hsx, y - hsy, z - hsz);
        this.trb = new Vector3d(x + hsx, y + hsy, z + hsz);
        this.centre = new Vector3d(x, y, z);
        this.size = new Vector3d(hsx * 2, hsy * 2, hsz * 2);
        this.box = new BoundingBoxd(blf, trb);
        this.aux3d1 = new Vector3d();
        this.depth = depth;
        this.transform = new Vector3d();
        this.observed = false;
        this.status = LoadStatus.NOT_LOADED;
        this.radius = Math.sqrt(hsx * hsx + hsy * hsy + hsz * hsz);
    }

    /**
     * Constructs an octree node
     * 
     * @param x
     * @param y
     * @param z
     * @param hsx
     * @param hsy
     * @param hsz
     * @param depth
     * @param parent
     *            The parent of this octant
     * @param i
     *            The index in the parent's children
     */
    public OctreeNode(double x, double y, double z, double hsx, double hsy, double hsz, int depth, OctreeNode parent, int i) {
        this(x, y, z, hsx, hsy, hsz, depth);
        this.parent = parent;
        parent.children[i] = this;
        this.pageId = computePageId();
    }

    /**
     * Constructs an octree node.
     * 
     * @param x
     *            The x coordinate of the center.
     * @param y
     *            The y coordinate of the center.
     * @param z
     *            The z coordinate of the center.
     * @param hsx
     *            The half-size in x.
     * @param hsy
     *            The half-size in y.
     * @param hsz
     *            The half-size in z.
     * @param childrenCount
     *            Number of children nodes. Same as non null positions in
     *            children vector.
     * @param nObjects
     *            Number of objects contained in this node and its descendants.
     * @param ownObjects
     *            Number of objects contained in this node. Same as
     *            objects.size().
     */
    public OctreeNode(double x, double y, double z, double hsx, double hsy, double hsz, int childrenCount, int nObjects, int ownObjects, int depth) {
        this(x, y, z, hsx, hsy, hsz, depth);
        this.childrenCount = childrenCount;
        this.nObjects = nObjects;
        this.ownObjects = ownObjects;
    }

    /**
     * Constructs an octree node.
     * @param pageId
     *            The octant id 
     * @param x
     *            The x coordinate of the center.
     * @param y
     *            The y coordinate of the center.
     * @param z
     *            The z coordinate of the center.
     * @param hsx
     *            The half-size in x.
     * @param hsy
     *            The half-size in y.
     * @param hsz
     *            The half-size in z.
     * @param childrenCount
     *            Number of children nodes. Same as non null positions in
     *            children vector.
     * @param nObjects
     *            Number of objects contained in this node and its descendants.
     * @param ownObjects
     *            Number of objects contained in this node. Same as
     *            objects.size().
     */
    public OctreeNode(long pageid, double x, double y, double z, double hsx, double hsy, double hsz, int childrenCount, int nObjects, int ownObjects, int depth) {
        this(pageid, x, y, z, hsx, hsy, hsz, depth);
        this.childrenCount = childrenCount;
        this.nObjects = nObjects;
        this.ownObjects = ownObjects;
    }

    public long computePageId() {
        int[] hashv = new int[25];
        hashv[0] = depth;
        computePageIdRec(hashv);
        return (long) Arrays.hashCode(hashv);
    }

    protected void computePageIdRec(int[] hashv) {
        if (depth == 0)
            return;
        hashv[depth] = getParentIndex();
        if (parent != null) {
            parent.computePageIdRec(hashv);
        }
    }

    /** Gets the index of this node in the parent's list **/
    protected int getParentIndex() {
        if (parent != null) {
            for (int i = 0; i < 8; i++) {
                if (parent.children[i] == this)
                    return i;
            }
            return 0;
        } else {
            return 0;
        }
    }

    /**
     * Resolves and adds the children of this node using the map. It runs
     * recursively once the children have been added.
     * 
     * @param map
     */
    public void resolveChildren(Map<Long, Pair<OctreeNode, long[]>> map) {
        Pair<OctreeNode, long[]> me = map.get(pageId);
        if (me == null) {
            throw new RuntimeException("OctreeNode with page ID " + pageId + " not found in map");
        }

        long[] childrenIds = me.getSecond();
        int i = 0;
        for (long childId : childrenIds) {
            if (childId != -1) {
                // Child exists
                OctreeNode child = map.get(childId).getFirst();
                children[i] = child;
                child.parent = this;
            } else {
                // No node in this position
            }
            i++;
        }

        // Recursive running
        for (int j = 0; j < children.length; j++) {
            OctreeNode child = children[j];
            if (child != null) {
                child.resolveChildren(map);
            }
        }
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean add(AbstractPositionEntity e) {
        if (objects == null)
            objects = new Array<AbstractPositionEntity>(false, 1);
        objects.add(e);
        ownObjects = e instanceof ParticleGroup ? objects.size - 1 + ((ParticleGroup) e).size() : objects.size;
        return true;
    }

    public boolean addAll(Array<AbstractPositionEntity> l) {
        if (objects == null)
            objects = new Array<AbstractPositionEntity>(false, l.size);
        objects.addAll(l);
        ownObjects = objects.size;
        return true;
    }

    public void setObjects(Array<AbstractPositionEntity> l) {
        this.objects = l;
        ownObjects = objects.size;
    }

    public boolean insert(AbstractPositionEntity e, int level) {
        int node = 0;
        if (e.getPosition().y > blf.y + ((trb.y - blf.y) / 2))
            node += 4;
        if (e.getPosition().z > blf.z + ((trb.z - blf.z) / 2))
            node += 2;
        if (e.getPosition().x > blf.x + ((trb.x - blf.x) / 2))
            node += 1;
        if (level == this.depth + 1) {
            return children[node].add(e);
        } else {
            return children[node].insert(e, level);
        }
    }

    public void toTree(TreeSet<AbstractPositionEntity> tree) {
        for (AbstractPositionEntity i : objects) {
            tree.add(i);
        }
        if (children != null) {
            for (int i = 0; i < 8; i++) {
                children[i].toTree(tree);
            }
        }
    }

    /**
     * Adds all the children of this node and its descendants to the given list.
     * 
     * @param tree
     */
    public void addChildrenToList(ArrayList<OctreeNode> tree) {
        if (children != null) {
            for (int i = 0; i < 8; i++) {
                if (children[i] != null) {
                    tree.add(children[i]);
                    children[i].addChildrenToList(tree);
                }
            }
        }
    }

    /**
     * Adds all the particles of this node and its descendants to the given
     * list.
     * 
     * @param particles
     */
    public void addParticlesTo(Array<AbstractPositionEntity> particles) {
        if (this.objects != null) {
            for (AbstractPositionEntity elem : this.objects)
                particles.add(elem);
        }
        for (int i = 0; i < 8; i++) {
            if (children[i] != null) {
                children[i].addParticlesTo(particles);
            }
        }
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean rec) {
        StringBuffer str = new StringBuffer(depth);
        if (rec)
            for (int i = 0; i < depth; i++) {
                str.append("   ");
            }

        str.append(pageId).append("(L").append(depth).append(")");
        if (parent != null) {
            str.append(" [i: ").append(Arrays.asList(parent.children).indexOf(this)).append(", ownobj: ");
        } else {
            str.append("[ownobj: ");
        }
        str.append(objects != null ? objects.size : "0").append("/").append(ownObjects).append(", recobj: ").append(nObjects).append(", nchld: ").append(childrenCount).append("] ").append(status).append("\n");

        if (childrenCount > 0 && rec) {
            for (OctreeNode child : children) {
                if (child != null) {
                    str.append(child.toString(rec));
                }
            }
        }
        return str.toString();
    }

    /**
     * Gets some per-level stats on the octree node
     * @return A [DEPTH,2] matrix with number of octants [i,0] and objects [i,1] per level
     */
    public int[][] stats() {
        int[][] result = new int[getMaxDepth()][2];
        statsRec(result);
        return result;
    }

    private void statsRec(int[][] mat) {
        mat[this.depth][0] += 1;
        mat[this.depth][1] += this.ownObjects;

        for (OctreeNode child : children) {
            if (child != null) {
                child.statsRec(mat);
            }
        }
    }

    /**
     * Removes this octant from the octree
     */
    public void remove() {
        if (this.parent != null)
            this.parent.removeChild(this);
    }

    /**
     * Removes the child from this octant's descendants
     * 
     * @param child
     */
    public void removeChild(OctreeNode child) {
        if (children != null)
            for (int i = 0; i < children.length; i++) {
                if (children[i] != null && children[i] == child) {
                    child.parent = null;
                    children[i] = null;
                }
            }
    }

    /**
     * Counts the number of nodes recursively
     * 
     * @return The number of nodes
     */
    public int numNodes() {
        int numNodes = 1;
        for (int i = 0; i < 8; i++) {
            if (children[i] != null) {
                numNodes += children[i].numNodes();
            }
        }
        return numNodes;
    }

    ComponentTypes ct = new ComponentTypes(ComponentType.Others);

    @Override
    public ComponentTypes getComponentType() {
        return ct;
    }

    @Override
    public double getDistToCamera() {
        return 0;
    }

    /**
     * Returns the deepest octant that contains the position
     * 
     * @param position
     *            The position
     * @return The best octant
     */
    public OctreeNode getBestOctant(Vector3d position) {
        if (!this.box.contains(position)) {
            return null;
        } else {
            OctreeNode candidate = null;
            for (int i = 0; i < 8; i++) {
                OctreeNode child = children[i];
                if (child != null) {
                    candidate = child.getBestOctant(position);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
            // We could not found a candidate in our children, we use this node.
            return this;
        }
    }

    /**
     * Gets the depth of this subtree, that is, the number of levels of the
     * longest parent-child path starting at this node. If run on the root node,
     * this gives the maximum octree depth.
     * 
     * @return The maximum depth of this node.
     */
    public int getMaxDepth() {
        int maxChildrenDepth = 0;
        if (children != null) {
            for (OctreeNode child : children) {
                if (child != null) {
                    int d = child.getMaxDepth();
                    if (d > maxChildrenDepth) {
                        maxChildrenDepth = d;
                    }
                }
            }
        }
        return maxChildrenDepth + 1;
    }

    /**
     * Computes the observed value and the transform of each observed node.
     * 
     * @param parentTransform
     *            The parent transform.
     * @param cam
     *            The current camera.
     * @param roulette
     *            List where the nodes to be processed are to be added.
     * @param opacity
     *            The opacity to set.
     */
    public void update(Transform parentTransform, ICamera cam, Array<SceneGraphNode> roulette, float opacity) {
        parentTransform.getTranslation(transform);
        this.opacity = opacity;
        this.observed = true;

        if (observed) {
            // Compute distance and view angle
            distToCamera = auxD1.set(centre).add(cam.getInversePos()).len();
            // View angle is normalized to 40 degrees when the octant is exactly the size of the screen height, regardless of the camera fov
            viewAngle = Math.atan(radius / distToCamera) * 2;

            float th0 = GlobalConf.scene.OCTANT_THRESHOLD_0;
            float th1 = GlobalConf.scene.OCTANT_THRESHOLD_1;

            if (viewAngle < th0) {
                // Not observed
                this.observed = false;
                setChildrenObserved(false);
            } else {
                nOctantsObserved++;
                //int L_DEPTH = 5;
                /**
                 * Load lists of pages
                 */
                if (status == LoadStatus.NOT_LOADED && LOAD_ACTIVE /*&& depth == L_DEPTH*/) {
                    // Add to load and go on
                    StreamingOctreeLoader.queue(this);
                } else if (status == LoadStatus.LOADED) {
                    // Visited last!
                    StreamingOctreeLoader.touch(this);

                    // Break down tree, fade in until th2
                    double alpha = 1;
                    if (GlobalConf.scene.OCTREE_PARTICLE_FADE && viewAngle < th1) {
                        AbstractRenderSystem.POINT_UPDATE_FLAG = true;
                        alpha = MathUtilsd.clamp(MathUtilsd.lint(viewAngle, th0, th1, 0d, 1d), 0f, 1f);
                    }
                    this.opacity *= alpha;

                    // Add objects
                    //if (depth == L_DEPTH)
                    addObjectsTo(roulette);
                } else if (status == LoadStatus.QUEUED) {
                    // What do? Move first in queue?
                }

                // Update children
                for (int i = 0; i < 8; i++) {
                    OctreeNode child = children[i];
                    if (child != null /*&& child.depth <= L_DEPTH*/) {
                        child.update(parentTransform, cam, roulette, this.opacity);
                    }
                }

            }
        }
    }

    private void addObjectsTo(Array<SceneGraphNode> roulette) {
        if (objects != null) {
            roulette.addAll(objects);
            for (SceneGraphNode obj : objects) {
                nObjectsObserved += obj.getStarCount();
            }
        }
    }

    private void setChildrenObserved(boolean observed) {
        for (int i = 0; i < 8; i++) {
            OctreeNode child = children[i];
            if (child != null) {
                child.observed = observed;
            }
        }
    }

    public boolean isObserved() {
        return observed && (parent == null ? true : parent.isObserved());
    }

    /**
     * Checks whether the given frustum intersects with the current octant.
     * 
     * @param parentTransform
     * @param cam
     */
    private boolean computeObserved1(Transform parentTransform, Frustumd frustum) {
        boxcopy.set(box);
        // boxcopy.mul(boxtransf.idt().translate(parentTransform.getTranslation()));

        observed = GlobalConf.program.CUBEMAP360_MODE || frustum.pointInFrustum(boxcopy.getCenter(auxD1)) || frustum.pointInFrustum(boxcopy.getCorner000(auxD1)) || frustum.pointInFrustum(boxcopy.getCorner001(auxD1)) || frustum.pointInFrustum(boxcopy.getCorner010(auxD1)) || frustum.pointInFrustum(boxcopy.getCorner011(auxD1)) || frustum.pointInFrustum(boxcopy.getCorner100(auxD1)) || frustum.pointInFrustum(boxcopy.getCorner101(auxD1)) || frustum.pointInFrustum(boxcopy.getCorner110(auxD1))
                || frustum.pointInFrustum(boxcopy.getCorner111(auxD1));

        for (int i = 0; i < 4; i++) {
            if (!observed) {
                // 0-4
                ray.origin.set(frustum.planePoints[i]);
                ray.direction.set(frustum.planePoints[i + 4]).sub(ray.origin);
                observed = Intersectord.intersectRayBoundsFast(ray, boxcopy.getCenter(auxD3), boxcopy.getDimensions(auxD4));
            } else {
                break;
            }
        }

        return observed;
    }

    public LoadStatus getStatus() {
        return status;
    }

    public void setStatus(LoadStatus status) {
        synchronized (status) {
            this.status = status;
        }
    }

    /**
     * Sets the status to this node and its descendants recursively to the given
     * depth level.
     * 
     * @param status
     *            The new status.
     * @param depth
     *            The depth.
     */
    public void setStatus(LoadStatus status, int depth) {
        if (depth >= this.depth) {
            setStatus(status);
            for (int i = 0; i < 8; i++) {
                OctreeNode child = children[i];
                if (child != null) {
                    child.setStatus(status, depth);
                }
            }
        }
    }

    /**
     * Updates the number of objects, own objects and children. This operation
     * runs recursively in depth.
     */
    public void updateNumbers() {
        // Number of own objects
        if (objects == null) {
            this.ownObjects = 0;
        } else {
            this.ownObjects = 0;
            for (AbstractPositionEntity ape : objects) {
                this.ownObjects += ape instanceof ParticleGroup ? ((ParticleGroup) ape).size() : 1;
            }

        }

        // Number of recursive objects
        this.nObjects = this.ownObjects;

        // Children count
        this.childrenCount = 0;
        for (int i = 0; i < 8; i++) {
            if (children[i] != null) {
                this.childrenCount++;
                // Recursive call
                children[i].updateNumbers();
                nObjects += children[i].nObjects;
            }
        }

    }

    public int countObjects() {
        int n = 0;
        if (objects != null) {
            for (AbstractPositionEntity obj : objects) {
                n += obj.getStarCount();
            }
        }

        if (children != null)
            for (OctreeNode child : children) {
                if (child != null)
                    n += child.countObjects();
            }

        return n;
    }

    public OctreeNode findOctant(long id) {
        if (this.pageId == id)
            return this;
        else {
            if (this.children != null) {
                OctreeNode target = null;
                for (OctreeNode child : children) {
                    if (child != null) {
                        target = child.findOctant(id);
                        if (target != null)
                            return target;
                    }
                }
            }
        }
        return null;
    }

    com.badlogic.gdx.graphics.Color col = new com.badlogic.gdx.graphics.Color();

    @Override
    public void render(LineRenderSystem sr, ICamera camera, float alpha) {
        if (this.observed) {
            this.col.set(Color.GREEN);
            this.col.a = alpha * opacity;
        } else {
            this.col.set(Color.RED);
            this.col.a = alpha * opacity;
        }

        if (this.col.a > 0) {
            // Camera correction
            Vector3d loc = aux3d1;
            loc.set(this.blf).add(camera.getInversePos());

            /*
             * .·------· .' | .'| +---+--·' | | | | | | ,+--+---· |.' | .'
             * +------+'
             */
            line(sr, loc.x, loc.y, loc.z, loc.x + size.x, loc.y, loc.z, this.col);
            line(sr, loc.x, loc.y, loc.z, loc.x, loc.y + size.y, loc.z, this.col);
            line(sr, loc.x, loc.y, loc.z, loc.x, loc.y, loc.z + size.z, this.col);

            /*
             * .·------· .' | .'| ·---+--+' | | | | | | ,·--+---+ |.' | .'
             * ·------+'
             */
            line(sr, loc.x + size.x, loc.y, loc.z, loc.x + size.x, loc.y + size.y, loc.z, this.col);
            line(sr, loc.x + size.x, loc.y, loc.z, loc.x + size.x, loc.y, loc.z + size.z, this.col);

            /*
             * .·------+ .' | .'| ·---+--·' | | | | | | ,+--+---+ |.' | .'
             * ·------·'
             */
            line(sr, loc.x + size.x, loc.y, loc.z + size.z, loc.x, loc.y, loc.z + size.z, this.col);
            line(sr, loc.x + size.x, loc.y, loc.z + size.z, loc.x + size.x, loc.y + size.y, loc.z + size.z, this.col);

            /*
             * .+------· .' | .'| ·---+--·' | | | | | | ,+--+---· |.' | .'
             * ·------·'
             */
            line(sr, loc.x, loc.y, loc.z + size.z, loc.x, loc.y + size.y, loc.z + size.z, this.col);

            /*
             * .+------+ .' | .'| +---+--+' | | | | | | ,·--+---· |.' | .'
             * ·------·'
             */
            line(sr, loc.x, loc.y + size.y, loc.z, loc.x + size.x, loc.y + size.y, loc.z, this.col);
            line(sr, loc.x, loc.y + size.y, loc.z, loc.x, loc.y + size.y, loc.z + size.z, this.col);
            line(sr, loc.x, loc.y + size.y, loc.z + size.z, loc.x + size.x, loc.y + size.y, loc.z + size.z, this.col);
            line(sr, loc.x + size.x, loc.y + size.y, loc.z, loc.x + size.x, loc.y + size.y, loc.z + size.z, this.col);
        }
    }

    /** Draws a line **/
    private void line(LineRenderSystem sr, double x1, double y1, double z1, double x2, double y2, double z2, com.badlogic.gdx.graphics.Color col) {
        sr.addLine(this, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, col);
    }

    public static long hash(double x, double y, double z) {
        long result = 3;
        result = result * 31 + hash(x);
        result = result * 31 + hash(y);
        result = result * 31 + hash(z);

        return result;
    }

    /**
     * Returns an integer hash code representing the given double value.
     * 
     * @param value the value to be hashed
     * @return the hash code
     */
    public static long hash(double value) {
        long bits = Double.doubleToLongBits(value);
        return (bits ^ (bits >>> 32));
    }

    /**
     * Disposes this octree node (and all children nodes recursively)
     */
    public void dispose() {
        if (objects != null)
            for (SceneGraphNode object : objects) {
                if (object != null)
                    object.dispose();
            }
        if (children != null)
            for (OctreeNode child : children) {
                if (child != null)
                    child.dispose();
            }
    }

    @Override
    public float getLineWidth() {
        return 1;
    }

}
