package gaia.cu9.ari.gaiaorbit.data.octreegen.generator;

import java.util.Iterator;

import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.math.BoundingBoxd;
import gaia.cu9.ari.gaiaorbit.util.math.Longref;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

public interface IOctreeGenerator {
    public static final Log logger = Logger.getLogger(IOctreeGenerator.class);

    public OctreeNode generateOctree(Array<StarBean> catalog);

    public int getDiscarded();

    static Longref pageid = new Longref(0l);

    static long nextPageId() {
        return pageid.num++;
    }

    static OctreeNode startGeneration(Array<StarBean> catalog, Class<?> clazz, OctreeGeneratorParams params) {
        
        logger.info("Starting generation of octree");

        /** Maximum distance allowed **/
        double maxdist = Double.MIN_VALUE;

        /** Furthest star from origin **/
        StarBean furthest = null;

        // Aux vectors
        Vector3d pos0 = new Vector3d();
        Vector3d pos1 = new Vector3d();

        Iterator<StarBean> it = catalog.iterator();
        while (it.hasNext()) {
            StarBean s = it.next();

            double dist = pos(s.data, pos0).len();
            if (dist * Constants.U_TO_PC > params.maxDistanceCap) {
                // Remove star
                it.remove();
            } else if (dist > maxdist) {
                furthest = s;
                maxdist = dist;
            }
        }

        OctreeNode root = null;
        if (params.sunCentre) {
            /** THE CENTRE OF THE OCTREE IS THE SUN **/
            pos(furthest.data, pos0);
            double halfSize = Math.max(Math.max(pos0.x, pos0.y), pos0.z);
            root = new OctreeNode(0, 0, 0, halfSize, halfSize, halfSize, 0);
        } else {
            /** THE CENTRE OF THE OCTREE MAY BE ANYWHERE **/
            double volume = Double.MIN_VALUE;
            BoundingBoxd aux = new BoundingBoxd();
            BoundingBoxd box = new BoundingBoxd();
            // Lets try to maximize the volume: from furthest star to star where axis-aligned bounding box volume is maximum
            pos(furthest.data, pos1);
            for (StarBean s : catalog) {
                pos(s.data, pos0);
                aux.set(pos1, pos0);
                double vol = aux.getVolume();
                if (vol > volume) {
                    volume = vol;
                    box.set(aux);
                }
            }
            double halfSize = Math.max(Math.max(box.getDepth(), box.getHeight()), box.getWidth()) / 2d;
            root = new OctreeNode(box.getCenterX(), box.getCenterY(), box.getCenterZ(), halfSize, halfSize, halfSize, 0);
        }
        return root;
    }

    static Vector3d pos(double[] s, Vector3d p) {
        return p.set(s[StarBean.I_X], s[StarBean.I_Y], s[StarBean.I_Z]);
    }
}
