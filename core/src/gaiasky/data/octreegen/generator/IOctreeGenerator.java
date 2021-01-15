/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen.generator;

import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.BoundingBoxd;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.OctreeNode;

import java.util.Iterator;
import java.util.List;

public interface IOctreeGenerator {
    Log logger = Logger.getLogger(IOctreeGenerator.class);

    OctreeNode generateOctree(List<IParticleRecord> catalog);

    int getDiscarded();

    static OctreeNode startGeneration(List<IParticleRecord> catalog, OctreeGeneratorParams params) {
        
        logger.info("Starting generation of octree");

        /** Maximum distance allowed **/
        double maxdist = Double.MIN_VALUE;

        /** Furthest star from origin **/
        IParticleRecord furthest = null;

        // Aux vectors
        Vector3d pos0 = new Vector3d();
        Vector3d pos1 = new Vector3d();

        Iterator<IParticleRecord> it = catalog.iterator();
        while (it.hasNext()) {
            IParticleRecord s = it.next();

            double dist = pos(s, pos0).len();
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
            pos(furthest, pos0);
            double halfSize = Math.max(Math.max(pos0.x, pos0.y), pos0.z);
            root = new OctreeNode(0, 0, 0, halfSize, halfSize, halfSize, 0);
        } else {
            /** THE CENTRE OF THE OCTREE MAY BE ANYWHERE **/
            double volume = Double.MIN_VALUE;
            BoundingBoxd aux = new BoundingBoxd();
            BoundingBoxd box = new BoundingBoxd();
            // Lets try to maximize the volume: from furthest star to star where axis-aligned bounding box volume is maximum
            pos(furthest, pos1);
            for (IParticleRecord s : catalog) {
                pos(s, pos0);
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

    static Vector3d pos(IParticleRecord s, Vector3d p) {
        return p.set(s.x(), s.y(), s.z());
    }
}
