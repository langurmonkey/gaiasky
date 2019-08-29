/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data.octreegen;

import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.Pair;
import gaia.cu9.ari.gaiaorbit.util.tree.LoadStatus;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes and reads the metadata to/from binary. The format is as follows:
 * 
 * - 32 bits (int) with the number of nodes, nNodes repeat the following nNodes times (for each node)
 * - 64 bits (long)
 * - pageId - The page id
 * - 64 bits (double) - centreX - The x component of the centre
 * - 64 bits (double) - centreY - The y component of the centre
 * - 64 bits (double) - centreZ - The z component of the centre
 * - 64 bits (double) - sx - The size in x
 * - 64 bits (double) - sy - The size in y
 * - 64 bits (double) - sz - The size in z
 * - 64 bits * 8 (long) - childrenIds - 8 longs with the ids of the children. If no child in the given position, the id is negative.
 * - 32 bits (int) - depth - The depth of the node
 * - 32 bits (int) - nObjects - The number of objects of this node and its descendants
 * - 32 bits (int) - ownObjects - The number of objects of this node
 * - 32 bits (int) - childCount - The number of children nodes
 * 
 * @author Toni Sagrista
 *
 */
public class MetadataBinaryIO {
    private static final Log logger = Logger.getLogger(MetadataBinaryIO.class);
    
    public Map<Long, Pair<OctreeNode, long[]>> nodesMap;

    /**
     * Reads the metadata into an octree node
     * 
     * @param in
     * @return The octree node
     */
    public OctreeNode readMetadata(InputStream in) {
        return readMetadata(in, null);
    }

    /**
     * Reads the metadata into an octree node
     * 
     * @param in
     *            Input stream
     * @return The octree node
     */
    public OctreeNode readMetadata(InputStream in, LoadStatus status) {
        nodesMap = new HashMap<Long, Pair<OctreeNode, long[]>>();

        DataInputStream data_in = new DataInputStream(in);
        try {
            OctreeNode root = null;
            // Read size of stars
            int size = data_in.readInt();
            int maxDepth = 0;

            for (int idx = 0; idx < size; idx++) {
                try {
                    // name_length, name, appmag, absmag, colorbv, ra, dec, dist
                    long pageId = data_in.readInt();
                    float x = (float) (data_in.readFloat() * Constants.DISTANCE_SCALE_FACTOR);
                    float y = (float) (data_in.readFloat() * Constants.DISTANCE_SCALE_FACTOR);
                    float z = (float) (data_in.readFloat() * Constants.DISTANCE_SCALE_FACTOR);
                    float hsx = (float) ((data_in.readFloat() / 2f) * Constants.DISTANCE_SCALE_FACTOR);
                    float hsy = (float) ((data_in.readFloat() / 2f) * Constants.DISTANCE_SCALE_FACTOR);
                    float hsz = (float) ((data_in.readFloat() / 2f) * Constants.DISTANCE_SCALE_FACTOR);
                    long[] childrenIds = new long[8];
                    for (int i = 0; i < 8; i++) {
                        childrenIds[i] = data_in.readInt();
                    }
                    int depth = data_in.readInt();
                    int nObjects = data_in.readInt();
                    int ownObjects = data_in.readInt();
                    int childrenCount = data_in.readInt();

                    maxDepth = Math.max(maxDepth, depth);

                    OctreeNode node = new OctreeNode(pageId, x, y, z, hsx, hsy, hsz, childrenCount, nObjects, ownObjects, depth);
                    nodesMap.put(pageId, new Pair<OctreeNode, long[]>(node, childrenIds));
                    if (status != null)
                        node.setStatus(status);

                    if (depth == 0) {
                        root = node;
                    }

                } catch (EOFException eof) {
                    logger.error(eof);
                }
            }

            OctreeNode.maxDepth = maxDepth;
            // All data has arrived
            if (root != null) {
                root.resolveChildren(nodesMap);
            } else {
                logger.error(new RuntimeException("No root node in visualization-metadata"));
            }

            return root;

        } catch (IOException e) {
            logger.error(e);
        }
        return null;
    }

    public OctreeNode readMetadataMapped(String file) {
        return readMetadataMapped(file, null);
    }

    public OctreeNode readMetadataMapped(String file, LoadStatus status) {
        nodesMap = new HashMap<Long, Pair<OctreeNode, long[]>>();

        try {
            FileChannel fc = new RandomAccessFile(GlobalConf.data.dataFile(file), "r").getChannel();

            MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            OctreeNode root = null;
            // Read size of stars
            int size = mem.getInt();
            int maxDepth = 0;

            for (int idx = 0; idx < size; idx++) {
                try {
                    // name_length, name, appmag, absmag, colorbv, ra, dec, dist
                    long pageId = mem.getInt();
                    float x = (float) (mem.getFloat() * Constants.DISTANCE_SCALE_FACTOR);
                    float y = (float) (mem.getFloat() * Constants.DISTANCE_SCALE_FACTOR);
                    float z = (float) (mem.getFloat() * Constants.DISTANCE_SCALE_FACTOR);
                    float hsx = (float) ((mem.getFloat() / 2f) * Constants.DISTANCE_SCALE_FACTOR);
                    //float hsy = mem.getFloat() / 2f;
                    mem.position(mem.position() + 4); // skip hsy
                    float hsy = hsx;
                    //float hsz = mem.getFloat() / 2f;
                    mem.position(mem.position() + 4); // skip hsz
                    float hsz = hsx;
                    long[] childrenIds = new long[8];
                    for (int i = 0; i < 8; i++) {
                        childrenIds[i] = mem.getInt();
                    }
                    int depth = mem.getInt();
                    int nObjects = mem.getInt();
                    int ownObjects = mem.getInt();
                    int childrenCount = mem.getInt();

                    maxDepth = Math.max(maxDepth, depth);

                    OctreeNode node = new OctreeNode(pageId, x, y, z, hsx, hsy, hsz, childrenCount, nObjects, ownObjects, depth);
                    nodesMap.put(pageId, new Pair<OctreeNode, long[]>(node, childrenIds));
                    if (status != null)
                        node.setStatus(status);

                    if (depth == 0) {
                        root = node;
                    }

                } catch (BufferUnderflowException bue) {
                    logger.error(bue);
                }
            }

            OctreeNode.maxDepth = maxDepth;
            // All data has arrived
            if (root != null) {
                root.resolveChildren(nodesMap);
            } else {
                logger.error(new RuntimeException("No root node in visualization-metadata"));
            }

            fc.close();

            return root;

        } catch (Exception e) {
            logger.error(e);
        }
        return null;

    }

    /**
     * Writes the metadata of the given octree node and its descendants to the
     * given output stream in binary.
     * 
     * @param root
     * @param out
     */
    public void writeMetadata(OctreeNode root, OutputStream out) {
        List<OctreeNode> nodes = new ArrayList<OctreeNode>();
        toList(root, nodes);

        // Wrap the FileOutputStream with a DataOutputStream
        DataOutputStream data_out = new DataOutputStream(out);

        try {
            // Number of nodes
            data_out.writeInt(nodes.size());

            for (OctreeNode node : nodes) {
                data_out.writeInt((int) node.pageId);
                data_out.writeFloat((float) node.centre.x);
                data_out.writeFloat((float) node.centre.y);
                data_out.writeFloat((float) node.centre.z);
                data_out.writeFloat((float) node.size.x);
                data_out.writeFloat((float) node.size.y);
                data_out.writeFloat((float) node.size.z);
                for (int i = 0; i < 8; i++) {
                    data_out.writeInt((int) (node.children[i] != null ? node.children[i].pageId : -1));
                }
                data_out.writeInt(node.depth);
                data_out.writeInt(node.nObjects);
                data_out.writeInt(node.ownObjects);
                data_out.writeInt(node.childrenCount);
            }

            data_out.close();
            out.close();

        } catch (IOException e) {
            logger.error(e);
        }

    }

    public void toList(OctreeNode node, List<OctreeNode> nodes) {
        nodes.add(node);
        for (OctreeNode child : node.children) {
            if (child != null) {
                toList(child, nodes);
            }
        }
    }

}
