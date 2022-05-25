/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen.generator;

import com.badlogic.gdx.utils.LongMap;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import gaiasky.util.tree.OctreeNode;

import java.util.*;

/**
 * Implements a f: mag -> level bijective map, where octree nodes in a level are filled with
 * magnitude-sorted stars until one of them is saturated before proceeding to lower
 * levels. This uses more memory than the outdated {@link OctreeGeneratorPart} but
 * it generally produces an artifact-free octree. The technique is called MS-LOD.
 */
public class OctreeGeneratorMag implements IOctreeGenerator {

    private final OctreeGeneratorParams params;
    private OctreeNode root;
    private Vector3d min = new Vector3d();
    private Vector3d max = new Vector3d();

    public OctreeGeneratorMag(OctreeGeneratorParams params) {
        this.params = params;
    }

    @Override
    public OctreeNode generateOctree(List<IParticleRecord> catalog) {
        root = IOctreeGenerator.startGeneration(catalog, params);

        // Holds all octree nodes indexed by id
        LongMap<OctreeNode> idMap = new LongMap<>();
        idMap.put(root.pageId, root);

        // Contains the list of objects for each node
        Map<OctreeNode, List<IParticleRecord>> objMap = new HashMap<>();

        int catalogSize = catalog.size();
        int catalogIndex = 0;
        for (int level = 0; level <= 19; level++) {
            logger.info("Generating level " + level + " (" + (catalog.size() - catalogIndex) + " stars left)");
            while (catalogIndex < catalogSize) {
                // Add stars to nodes until we reach max part
                IParticleRecord sb = catalog.get(catalogIndex++);
                double x = sb.x();
                double y = sb.y();
                double z = sb.z();

                Long nodeId = getPositionOctantId(x, y, z, level);
                if (!idMap.containsKey(nodeId)) {
                    // Create octant and parents if necessary
                    OctreeNode octant = createOctant(nodeId, x, y, z, level);
                    // Add to idMap
                    idMap.put(octant.pageId, octant);
                }
                // Add star to node
                OctreeNode octant = idMap.get(nodeId);
                int addedNum = addStarToNode(sb, octant, objMap);

                if (addedNum >= params.maxPart) {
                    // On to next level!
                    break;
                }
            }

            if (catalogIndex >= catalog.size()) {
                // All stars added -> FINISHED
                break;
            }
        }

        if (params.postprocess) {
            logger.info("Post-processing octree: childcount=" + params.childCount + ", parentcount=" + params.parentCount);
            long mergedNodes = 0;
            long mergedObjects = 0;
            // We merge low-count nodes (<= childcount) with parents, if parents' count is <= parentcount
            Object[] nodes = objMap.keySet().toArray();
            // Sort by descending depth
            Arrays.sort(nodes, (node1, node2) -> {
                OctreeNode n1 = (OctreeNode) node1;
                OctreeNode n2 = (OctreeNode) node2;
                return Integer.compare(n2.depth, n1.depth);
            });

            int n = objMap.size();
            for (int i = n - 1; i >= 0; i--) {
                OctreeNode current = (OctreeNode) nodes[i];
                if (current.numChildren() ==0 && current.parent != null && objMap.containsKey(current) && objMap.containsKey(current.parent)) {
                    List<IParticleRecord> childArr = objMap.get(current);
                    List<IParticleRecord> parentArr = objMap.get(current.parent);
                    if (childArr.size() <= params.childCount && parentArr.size() <= params.parentCount) {
                        // Merge children nodes with parent nodes, remove children
                        parentArr.addAll(childArr);
                        objMap.remove(current);
                        current.remove();
                        mergedNodes++;
                        mergedObjects += childArr.size();
                    }
                }
            }

            logger.info("POSTPROCESS STATS:");
            logger.info("    Merged nodes:    " + mergedNodes);
            logger.info("    Merged objects:  " + mergedObjects);
        }

        // Tree is ready, create star groups
        Set<OctreeNode> nodes = objMap.keySet();
        for (OctreeNode node : nodes) {
            List<IParticleRecord> list = objMap.get(node);
            StarGroup sg = new StarGroup();
            sg.setData(list, false);
            node.add(sg);
            sg.octant = node;
        }

        root.updateCounts();
        return root;
    }

    private OctreeNode createOctant(Long id, double x, double y, double z, int level) {
        min.setZero();
        OctreeNode current = root;
        // From root down to level
        for (int l = 1; l <= level; l++) {
            double hs = current.size.x / 2d;
            int idx;
            if (x <= current.min.x + hs) {
                if (y <= current.min.y + hs) {
                    if (z <= current.min.z + hs) {
                        idx = 0;
                        min.set(current.min);
                    } else {
                        idx = 1;
                        min.set(current.min.x, current.min.y, current.min.z + hs);
                    }
                } else {
                    if (z <= current.min.z + hs) {
                        idx = 2;
                        min.set(current.min.x, current.min.y + hs, current.min.z);
                    } else {
                        idx = 3;
                        min.set(current.min.x, current.min.y + hs, current.min.z + hs);
                    }
                }
            } else {
                if (y <= current.min.y + hs) {
                    if (z <= current.min.z + hs) {
                        idx = 4;
                        min.set(current.min.x + hs, current.min.y, current.min.z);
                    } else {
                        idx = 5;
                        min.set(current.min.x + hs, current.min.y, current.min.z + hs);
                    }
                } else {
                    if (z <= current.min.z + hs) {
                        idx = 6;
                        min.set(current.min.x + hs, current.min.y + hs, current.min.z);
                    } else {
                        idx = 7;
                        min.set(current.min.x + hs, current.min.y + hs, current.min.z + hs);
                    }
                }
            }
            if (current.children[idx] == null) {
                // Create kid
                double nhs = hs / 2d;
                new OctreeNode(min.x + nhs, min.y + nhs, min.z + nhs, nhs, nhs, nhs, l, current, idx);
            }
            current = current.children[idx];
        }

        if (current.pageId != id)
            throw new RuntimeException("Given id and newly created node id do not match: " + id + " vs " + current.pageId);

        return current;
    }

    private int addStarToNode(IParticleRecord sb, OctreeNode node, Map<OctreeNode, List<IParticleRecord>> map) {
        List<IParticleRecord> array = map.get(node);
        if (array == null) {
            array = new ArrayList<>(25);
            map.put(node, array);
        }
        array.add(sb);
        return array.size();
    }

    @Override
    public int getDiscarded() {
        return 0;
    }


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
            // Root is always id=0
            return 0l;
        }
        min.set(root.min);
        // Half side
        double hs = root.size.x / 2d;
        StringBuilder id = new StringBuilder();

        for (int l = 1; l <= level; l++) {
            if (x <= min.x + hs) {
                if (y <= min.y + hs) {
                    if (z <= min.z + hs) {
                        // Min stays the same!
                        id.append("1");
                    } else {
                        min.set(min.x, min.y, min.z + hs);
                        id.append("2");
                    }
                } else {
                    if (z <= min.z + hs) {
                        min.set(min.x, min.y + hs, min.z);
                        id.append("3");
                    } else {
                        min.set(min.x, min.y + hs, min.z + hs);
                        id.append("4");
                    }
                }
            } else {
                if (y <= min.y + hs) {
                    if (z <= min.z + hs) {
                        min.set(min.x + hs, min.y, min.z);
                        id.append("5");
                    } else {
                        min.set(min.x + hs, min.y, min.z + hs);
                        id.append("6");
                    }
                } else {
                    if (z <= min.z + hs) {
                        min.set(min.x + hs, min.y + hs, min.z);
                        id.append("7");
                    } else {
                        min.set(min.x + hs, min.y + hs, min.z + hs);
                        id.append("8");
                    }

                }
            }
            hs = hs / 2d;
        }
        return Parser.parseLong(id.toString());
    }

}
