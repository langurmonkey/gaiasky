/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.svt;

import gaiasky.scene.record.VirtualTextureComponent;

public class SVTQuadtreeNode<T> implements Comparable<SVTQuadtreeNode> {

    /** The tree this node belongs to. **/
    public final SVTQuadtree<T> tree;
    /** The parent of this tile, if it is not at the root level. **/
    public final SVTQuadtreeNode<T> parent;
    public final int level;
    public final int col;
    public final int row;
    public final T object;
    // Node state.
    public int state = 0;
    // Node last access time.
    public long accessed = 0;

    public SVTQuadtreeNode(final SVTQuadtree<T> tree, final SVTQuadtreeNode<T> parent, final int level, final int col, final int row, final T object) {
        this.tree = tree;
        this.parent = parent;
        this.level = level;
        this.col = col;
        this.row = row;
        this.object = object;
    }

    /**
     * Get the UV coordinates in [0,1] of the top-left position of this tile. The UV coordinates start at the bottom-left.
     *
     * @return The UV coordinates of this tile.
     */
    public double[] getUV() {
        var numCols = (double) tree.getUTileCount(level);
        var numRows = (double) tree.getVTileCount(level);

        return new double[] { (double) col / numCols, 1.0 - (double) row / numRows };
    }

    /**
     * Computes the OpenGL mip level for this tile.
     *
     * @return The OpenGL mip level for this tile.
     */
    public int mipLevel() {
        return tree.depth - level;
    }

    @Override
    public String toString() {
        return "Node {" + "L" + level + ", col=" + col + ", row=" + row + ", o=" + object + '}';
    }

    public String toStringShort() {
        return "id" + ((VirtualTextureComponent) tree.aux).id + "-L" + level + "[" + col + "," + row + "]";
    }

    @Override
    public int compareTo(SVTQuadtreeNode o) {
        // The natural order of nodes depends only on their depth.
        return Integer.compare(this.level, o.level);
    }

    /**
     * Gets a unique 64-bit integer key for this tile, including the level, the column and the row.
     *
     * @return The unique key.
     */
    public long getKey() {
        return tree.getKey(level, col, row);
    }
}
