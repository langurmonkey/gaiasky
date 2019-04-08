/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data.octreegen.generator;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import gaia.cu9.ari.gaiaorbit.data.octreegen.StarBrightnessComparator;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.math.BoundingBoxd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

import java.util.*;

/**
 * Implements the magnitude to level map, where octants in a level are filled with
 * magnitude-sorted stars until one of them is saturated before proceeding to lower
 * levels. This uses more memory than the outdated {@link OctreeGeneratorPart} but
 * it generally produces artifact-free octrees and properly implements the
 * bijective mapping f: mag -> level.
 *
 * @author Toni Sagrista
 */
public class OctreeGeneratorMag implements IOctreeGenerator {

    private OctreeGeneratorParams params;
    private Comparator<StarBean> comp;
    private OctreeNode root;

    public OctreeGeneratorMag(OctreeGeneratorParams params) {
        this.params = params;
        comp = new StarBrightnessComparator();
    }

    @Override
    public OctreeNode generateOctree(Array<StarBean> catalog) {
        root = IOctreeGenerator.startGeneration(catalog, params);

        // Holds all octree nodes indexed by id
        LongMap<OctreeNode> idMap = new LongMap<OctreeNode>();
        idMap.put(root.pageId, root);

        Map<OctreeNode, Array<StarBean>> sbMap = new HashMap<OctreeNode, Array<StarBean>>();

        logger.info("Sorting source catalog with " + catalog.size + " stars");
        catalog.sort(comp);
        logger.info("Catalog sorting done");

        int catalogIndex = 0;
        for (int level = 0; level < 25; level++) {
            logger.info("Generating level " + level + " (" + (catalog.size - catalogIndex) + " stars left)");
            while (catalogIndex < catalog.size) {
                // Add star beans to octants till we reach max capacity
                StarBean sb = catalog.get(catalogIndex++);
                double x = sb.data[StarBean.I_X];
                double y = sb.data[StarBean.I_Y];
                double z = sb.data[StarBean.I_Z];
                int addedNum;

                Long nodeId = getPositionOctantId(x, y, z, level);
                if (!idMap.containsKey(nodeId)) {
                    // Create octant and parents if necessary
                    OctreeNode octant = createOctant(nodeId, x, y, z, level);
                    // Add to idMap
                    idMap.put(octant.pageId, octant);
                }
                // Add star to node
                OctreeNode octant = idMap.get(nodeId);
                addedNum = addStarToNode(sb, octant, sbMap);

                if (addedNum >= params.maxPart) {
                    // On to next level!
                    break;
                }
            }

            if (catalogIndex >= catalog.size) {
                // All stars added -> FINISHED
                break;
            }
        }

        if (params.postprocess) {
            long mergedNodes = 0;
            long mergedObjects = 0;
            // We merge low-count nodes (<= childcount) with parents, if parents' count is <= parentcount
            Object[] nodes = sbMap.keySet().toArray();
            // Sort by descending depth
            Arrays.sort(nodes, (node1, node2) -> {
                OctreeNode n1 = (OctreeNode) node1;
                OctreeNode n2 = (OctreeNode) node2;
                return Integer.compare(n1.depth, n2.depth);
            });

            int n = sbMap.size();
            for (int i = n - 1; i >= 0; i--) {
                OctreeNode current = (OctreeNode) nodes[i];
                if (current.parent != null && sbMap.containsKey(current) && sbMap.containsKey(current.parent)) {
                    Array<StarBean> childrenArr = sbMap.get(current);
                    Array<StarBean> parentArr = sbMap.get(current.parent);
                    if (childrenArr.size <= params.childCount && parentArr.size <= params.parentCount) {
                        // Merge children nodes with parent nodes, remove children
                        parentArr.addAll(childrenArr);
                        sbMap.remove(current);
                        current.remove();
                        mergedNodes++;
                        mergedObjects += childrenArr.size;
                    }
                }
            }

            logger.info("POSTPROCESS EMPTY STATS:");
            logger.info("    Merged nodes:    " + mergedNodes);
            logger.info("    Merged objects:  " + mergedObjects);
        }

        // Tree is ready, create star groups
        Set<OctreeNode> nodes = sbMap.keySet();
        for (OctreeNode node : nodes) {
            Array<StarBean> list = sbMap.get(node);
            StarGroup sg = new StarGroup();
            sg.setData(list, false);
            node.add(sg);
            sg.octant = node;
            sg.octantId = node.pageId;
        }

        root.updateNumbers();
        return root;
    }

    private OctreeNode createOctant(Long id, double x, double y, double z, int level) {
        Vector3d min = new Vector3d();
        OctreeNode current = root;
        for (int l = 1; l <= level; l++) {
            BoundingBoxd b = current.box;
            double hs = b.getWidth() / 2d;
            int idx;
            if (x <= b.min.x + hs) {
                if (y <= b.min.y + hs) {
                    if (z <= b.min.z + hs) {
                        idx = 0;
                        min.set(b.min);
                    } else {
                        idx = 1;
                        min.set(b.min.x, b.min.y, b.min.z + hs);
                    }
                } else {
                    if (z <= b.min.z + hs) {
                        idx = 2;
                        min.set(b.min.x, b.min.y + hs, b.min.z);
                    } else {
                        idx = 3;
                        min.set(b.min.x, b.min.y + hs, b.min.z + hs);
                    }
                }
            } else {
                if (y <= b.min.y + hs) {
                    if (z <= b.min.z + hs) {
                        idx = 4;
                        min.set(b.min.x + hs, b.min.y, b.min.z);
                    } else {
                        idx = 5;
                        min.set(b.min.x + hs, b.min.y, b.min.z + hs);
                    }
                } else {
                    if (z <= b.min.z + hs) {
                        idx = 6;
                        min.set(b.min.x + hs, b.min.y + hs, b.min.z);
                    } else {
                        idx = 7;
                        min.set(b.min.x + hs, b.min.y + hs, b.min.z + hs);
                    }
                }
            }
            if (current.children[idx] == null) {
                // Create parent
                double nhs = hs / 2d;
                new OctreeNode(min.x + nhs, min.y + nhs, min.z + nhs, nhs, nhs, nhs, l, current, idx);
            }
            current = current.children[idx];
        }

        if (current.pageId != id)
            throw new RuntimeException("Given id and newly created node id do not match: " + id + " vs " + current.pageId);

        return current;
    }

    private int addStarToNode(StarBean sb, OctreeNode node, Map<OctreeNode, Array<StarBean>> map) {
        if (!map.containsKey(node)) {
            // Array of a fraction of max part (four array resizes gives max part)
            map.put(node, new Array<StarBean>((int) Math.round(this.params.maxPart * 0.10662224073)));
        }
        Array<StarBean> array = map.get(node);
        array.add(sb);
        return array.size;
    }

    @Override
    public int getDiscarded() {
        return 0;
    }

    Vector3d min = new Vector3d();
    Vector3d max = new Vector3d();

    /**
     * Gets the id of the node which corresponds to the given xyz position
     * @param x Position in x
     * @param y Position in y
     * @param z Position in z
     * @param level Level
     * @return Id of node which contains the position. The id is a long where the two least significant digits 
     * indicate the level and the rest of digit positions indicate the index in the level of
     * the position.
     */
    public Long getPositionOctantId(double x, double y, double z, int level) {
        if (level == 0) {
            // Level 0 always has only one node only
            return root.pageId;
        }
        min.set(root.box.min);
        max.set(root.box.max);
        // Half side
        double hs = (max.x - min.x) / 2d;
        int[] hashv = new int[25];
        hashv[0] = level;

        for (int l = 1; l <= level; l++) {
            if (x <= min.x + hs) {
                if (y <= min.y + hs) {
                    if (z <= min.z + hs) {
                        // Min stays the same!
                        hashv[l] = 0;
                    } else {
                        min.set(min.x, min.y, min.z + hs);
                        hashv[l] = 1;
                    }
                } else {
                    if (z <= min.z + hs) {
                        min.set(min.x, min.y + hs, min.z);
                        hashv[l] = 2;
                    } else {
                        min.set(min.x, min.y + hs, min.z + hs);
                        hashv[l] = 3;
                    }
                }
            } else {
                if (y <= min.y + hs) {
                    if (z <= min.z + hs) {
                        min.set(min.x + hs, min.y, min.z);
                        hashv[l] = 4;
                    } else {
                        min.set(min.x + hs, min.y, min.z + hs);
                        hashv[l] = 5;
                    }
                } else {
                    if (z <= min.z + hs) {
                        min.set(min.x + hs, min.y + hs, min.z);
                        hashv[l] = 6;
                    } else {
                        min.set(min.x + hs, min.y + hs, min.z + hs);
                        hashv[l] = 7;
                    }

                }
            }
            // Max is always half side away from min
            max.set(min.x + hs, min.y + hs, min.z + hs);
            hs = hs / 2d;
        }

        //return id;
        return (long) Arrays.hashCode(hashv);
    }

}
