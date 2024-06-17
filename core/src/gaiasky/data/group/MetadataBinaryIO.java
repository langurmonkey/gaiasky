/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.tree.LoadStatus;
import gaiasky.util.tree.OctreeNode;
import net.jafama.FastMath;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataBinaryIO {
    private static final Log logger = Logger.getLogger(MetadataBinaryIO.class);

    public Map<Long, Pair<OctreeNode, long[]>> nodesMap;

    /**
     * Reads the metadata into an octree node
     *
     * @return The octree node
     */
    public OctreeNode readMetadata(InputStream in) {
        return readMetadata(in, null);
    }

    /**
     * Reads the metadata into an octree node
     *
     * @param in Input stream
     *
     * @return The octree node
     */
    public OctreeNode readMetadata(InputStream in, LoadStatus status) {
        nodesMap = new HashMap<>();

        DataInputStream data_in = new DataInputStream(in);
        try {
            OctreeNode root = null;

            // If first integer is negative, read:
            //   <int> token
            //   <int> version
            //   <int> size
            // Else
            //   <int> size

            // Token marks presence of version number
            int token = data_in.readInt();
            // Version
            int version = 0;
            // Size
            int size;
            if (token < 0) {
                version = data_in.readInt();
                size = data_in.readInt();
            } else {
                size = token;
            }
            int maxDepth = 0;

            for (int idx = 0; idx < size; idx++) {
                try {
                    // name_length, name, appmag, absmag, colorbv, ra, dec, dist
                    long pageId = version == 0 ? data_in.readInt() : data_in.readLong();
                    float x = (float) (data_in.readFloat() * Constants.DISTANCE_SCALE_FACTOR);
                    float y = (float) (data_in.readFloat() * Constants.DISTANCE_SCALE_FACTOR);
                    float z = (float) (data_in.readFloat() * Constants.DISTANCE_SCALE_FACTOR);
                    float hsx = (float) ((data_in.readFloat() / 2f) * Constants.DISTANCE_SCALE_FACTOR);
                    float hsy = (float) ((data_in.readFloat() / 2f) * Constants.DISTANCE_SCALE_FACTOR);
                    float hsz = (float) ((data_in.readFloat() / 2f) * Constants.DISTANCE_SCALE_FACTOR);
                    long[] childrenIds = new long[8];
                    for (int i = 0; i < 8; i++) {
                        childrenIds[i] = version == 0 ? data_in.readInt() : data_in.readLong();
                    }
                    int depth = data_in.readInt();
                    int nObjects = data_in.readInt();
                    int ownObjects = data_in.readInt();
                    int childrenCount = data_in.readInt();

                    maxDepth = FastMath.max(maxDepth, depth);

                    OctreeNode node = new OctreeNode(pageId, x, y, z, hsx, hsy, hsz, childrenCount, nObjects, ownObjects, depth);
                    nodesMap.put(pageId, new Pair<>(node, childrenIds));
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
        nodesMap = new HashMap<>();

        try (var f = new RandomAccessFile(Settings.settings.data.dataFile(file), "r")) {
            FileChannel fc = f.getChannel();

            MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            OctreeNode root = null;

            // If first integer is negative, read:
            //   <int> token
            //   <int> version
            //   <int> size
            // Else
            //   <int> size

            // Token marks presence of version number
            int token = mem.getInt();
            // Version
            int version = 0;
            // Size
            int size;
            if (token < 0) {
                version = mem.getInt();
                size = mem.getInt();
            } else {
                size = token;
            }

            int maxDepth = 0;

            for (int idx = 0; idx < size; idx++) {
                try {
                    // name_length, name, appmag, absmag, colorbv, ra, dec, dist
                    long pageId = version == 0 ? mem.getInt() : mem.getLong();
                    float x = (float) (mem.getFloat() * Constants.DISTANCE_SCALE_FACTOR);
                    float y = (float) (mem.getFloat() * Constants.DISTANCE_SCALE_FACTOR);
                    float z = (float) (mem.getFloat() * Constants.DISTANCE_SCALE_FACTOR);
                    float hsx = (float) ((mem.getFloat() / 2f) * Constants.DISTANCE_SCALE_FACTOR);
                    //float hsy = mem.getFloat() / 2f;
                    mem.position(mem.position() + 4); // skip hsy
                    mem.position(mem.position() + 4); // skip hsz
                    long[] childrenIds = new long[8];
                    for (int i = 0; i < 8; i++) {
                        childrenIds[i] = version == 0 ? mem.getInt() : mem.getLong();
                    }
                    int depth = mem.getInt();
                    int nObjects = mem.getInt();
                    int ownObjects = mem.getInt();
                    int childrenCount = mem.getInt();

                    maxDepth = FastMath.max(maxDepth, depth);

                    float hsy = hsx;
                    float hsz = hsx;
                    OctreeNode node = new OctreeNode(pageId, x, y, z, hsx, hsy, hsz, childrenCount, nObjects, ownObjects, depth);
                    nodesMap.put(pageId, new Pair<>(node, childrenIds));
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
     */
    public void writeMetadata(OctreeNode root, OutputStream out) {
        List<OctreeNode> nodes = new ArrayList<>();
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
                data_out.writeInt(node.numObjectsRec);
                data_out.writeInt(node.numObjects);
                data_out.writeInt(node.numChildren);
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
