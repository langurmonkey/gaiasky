/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data.octreegen.generator;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Greedy generator where octants in a level are filled up with as many
 * stars as possible before proceeding to lower levels. This approach is
 * outdated and should not be used. Use {@link OctreeGeneratorMag} instead.
 * This uses less memory than the its magnitude counterpart, but does not
 * implement the bijective map f: mag -> level.
 *
 * @author Toni Sagrista
 */
public class OctreeGeneratorPart implements IOctreeGenerator {

    private OctreeGeneratorParams params;

    private IAggregationAlgorithm aggregation;

    public OctreeGeneratorPart(OctreeGeneratorParams params) {
        IAggregationAlgorithm aggr = new BrightestStars(25, params.maxPart, params.maxPart, false);
        this.aggregation = aggr;
        this.params = params;
    }

    public OctreeNode generateOctree(Array<StarBean> catalog) {
        OctreeNode root = IOctreeGenerator.startGeneration(catalog, params);

        Array<OctreeNode>[] octantsPerLevel = new Array[25];
        octantsPerLevel[0] = new Array<OctreeNode>(1);
        octantsPerLevel[0].add(root);

        Map<OctreeNode, Array<StarBean>> inputLists = new HashMap<OctreeNode, Array<StarBean>>();
        inputLists.put(root, catalog);

        treatLevel(inputLists, 0, octantsPerLevel, MathUtils.clamp((float) aggregation.getMaxPart() / (float) catalog.size, 0f, 1f));

        root.updateNumbers();

        return root;
    }

    /**
     * Generate the octree on a per-level basis to have a uniform density in all
     * the nodes of the same level. Breadth-first.
     * 
     * @param inputLists List of star beans per octant
     * @param level The depth
     * @param octantsPerLevel Octants of each level
     * @param percentage Percentage
     */
    private void treatLevel(Map<OctreeNode, Array<StarBean>> inputLists, int level, Array<OctreeNode>[] octantsPerLevel, float percentage) {
        logger.info("Generating level " + level);
        Array<OctreeNode> levelOctants = octantsPerLevel[level];

        octantsPerLevel[level + 1] = new Array<OctreeNode>(levelOctants.size * 8);

        /** CREATE OCTANTS FOR LEVEL+1 **/
        Iterator<OctreeNode> it = levelOctants.iterator();
        while (it.hasNext()) {
            OctreeNode octant = it.next();
            Array<StarBean> list = inputLists.get(octant);

            if (list.size == 0) {
                // Empty node, remove
                it.remove();
                octant.remove();
            } else {
                boolean leaf = aggregation.sample(list, octant, percentage);

                if (!leaf) {
                    // Generate 8 children per each level octant
                    double hsx = octant.size.x / 4d;
                    double hsy = octant.size.y / 4d;
                    double hsz = octant.size.z / 4d;

                    /** CREATE SUB-OCTANTS **/
                    // Front - top - left
                    octantsPerLevel[level + 1].add(new OctreeNode(octant.centre.x - hsx, octant.centre.y + hsy, octant.centre.z - hsz, hsx, hsy, hsz, octant.depth + 1, octant, 0));
                    // Front - top - right
                    octantsPerLevel[level + 1].add(new OctreeNode(octant.centre.x + hsx, octant.centre.y + hsy, octant.centre.z - hsz, hsx, hsy, hsz, octant.depth + 1, octant, 1));
                    // Front - bottom - left
                    octantsPerLevel[level + 1].add(new OctreeNode(octant.centre.x - hsx, octant.centre.y - hsy, octant.centre.z - hsz, hsx, hsy, hsz, octant.depth + 1, octant, 2));
                    // Front - bottom - right
                    octantsPerLevel[level + 1].add(new OctreeNode(octant.centre.x + hsx, octant.centre.y - hsy, octant.centre.z - hsz, hsx, hsy, hsz, octant.depth + 1, octant, 3));
                    // Back - top - left
                    octantsPerLevel[level + 1].add(new OctreeNode(octant.centre.x - hsx, octant.centre.y + hsy, octant.centre.z + hsz, hsx, hsy, hsz, octant.depth + 1, octant, 4));
                    // Back - top - right
                    octantsPerLevel[level + 1].add(new OctreeNode(octant.centre.x + hsx, octant.centre.y + hsy, octant.centre.z + hsz, hsx, hsy, hsz, octant.depth + 1, octant, 5));
                    // Back - bottom - left
                    octantsPerLevel[level + 1].add(new OctreeNode(octant.centre.x - hsx, octant.centre.y - hsy, octant.centre.z + hsz, hsx, hsy, hsz, octant.depth + 1, octant, 6));
                    // Back - bottom - right
                    octantsPerLevel[level + 1].add(new OctreeNode(octant.centre.x + hsx, octant.centre.y - hsy, octant.centre.z + hsz, hsx, hsy, hsz, octant.depth + 1, octant, 7));
                }
            }
        }

        /** IF WE HAVE OCTANTS IN THE NEXT LEVEL, INTERSECT **/
        if (octantsPerLevel[level + 1].size != 0) {

            /** INTERSECT CATALOG WITH OCTANTS, COMPUTE PERCENTAGE **/
            int maxSublevelObjs = 0;
            double maxSublevelMag = Double.MAX_VALUE;
            double minSublevelMag = 0;
            Map<OctreeNode, Array<StarBean>> lists = new HashMap<OctreeNode, Array<StarBean>>();

            for (OctreeNode octant : octantsPerLevel[level + 1]) {
                Array<StarBean> list = intersect(inputLists.get(octant.parent), octant);
                lists.put(octant, list);
                if (list.size > maxSublevelObjs) {
                    maxSublevelObjs = list.size;
                }
                // Adapt levels by magnitude
                for (StarBean sb : list) {
                    if (sb.absmag() < maxSublevelMag) {
                        maxSublevelMag = sb.absmag();
                    }
                    if (sb.absmag() > minSublevelMag) {
                        minSublevelMag = sb.absmag();
                    }
                }
            }
            float sublevelPercentage = MathUtils.clamp((float) aggregation.getMaxPart() / (float) maxSublevelObjs, 0f, 1f);

            /** GO ONE MORE LEVEL DOWN **/
            treatLevel(lists, level + 1, octantsPerLevel, sublevelPercentage);
        }
    }

    /**
     * Returns a new list with all the stars of the incoming list that are
     * inside the box.
     * 
     * @param stars
     * @param box
     * @return
     */
    private Array<StarBean> intersect(Array<StarBean> stars, OctreeNode box) {
        Array<StarBean> result = new Array<StarBean>();
        for (StarBean star : stars) {
            if (star.octant == null && box.box.contains(star.data[StarBean.I_X], star.data[StarBean.I_Y], star.data[StarBean.I_Z])) {
                result.add(star);
            }
        }
        return result;

    }


    @Override
    public int getDiscarded() {
        return aggregation.getDiscarded();
    }

}
