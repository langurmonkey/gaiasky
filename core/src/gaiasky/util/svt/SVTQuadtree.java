/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.svt;

import com.badlogic.gdx.utils.LongMap;
import gaiasky.util.math.MathUtilsDouble;

public class SVTQuadtree<T> {
    public final int MAX_LEVEL = 15;

    /**
     * Name of the tree.
     */
    public final String name;
    /**
     * Size in pixels of each tile. Tiles are square, so width and height are equal.
     * The tile size is a power of two, capping at 1024.
     * One of 8, 16, 32, 64, 128, 256, 512 or 1024.
     **/
    public final int tileSize;
    /** The total number of tiles in the tree. **/
    public int numTiles;
    /** Depth of the tree, e.g., the deepest level, in [0,n]. **/
    public int depth = 0;

    /** Root node(s) of the tree. **/
    public SVTQuadtreeNode<T>[] root;

    /** Each tile is identified by its level and its UV. Here we can access tiles directly. **/
    public LongMap<SVTQuadtreeNode<T>>[] levels;

    /** Auxiliary object to store additional data. **/
    public Object aux;

    public SVTQuadtree(String name, int tileSize, int rootPositions) {
        this.name = name;
        this.tileSize = tileSize;
        this.root = new SVTQuadtreeNode[rootPositions];
        this.levels = new LongMap[MAX_LEVEL + 1];
    }

    public void insert(final int level, final int col, final int row, T object) {
        assert level >= 0 && level <= MAX_LEVEL : "Level out of bounds: " + level;
        assert col >= 0 && row >= 0 : "Invalid UV: " + col + ", " + row;

        if (levels[level] == null) {
            // Create map.
            levels[level] = new LongMap<>();
        }

        SVTQuadtreeNode<T> parent = null;
        if (level > 0) {
            // Find parent.
            var uv = getUV(level, col, row);
            parent = getTileFromUV(level - 1, uv[0], uv[1]);
        }

        var tile = new SVTQuadtreeNode<>(this, parent, level, col, row, object);
        levels[level].put(tile.getKey(), tile);
        numTiles++;
    }

    /**
     * Gets a tile in the tree given its level, column and row.
     *
     * @param level The level.
     * @param col   The column.
     * @param row   The row.
     *
     * @return The tile with the given level, column and row, if it exists.
     */
    public SVTQuadtreeNode<T> getTile(int level, int col, int row) {
        assert level >= 0 && level <= MAX_LEVEL : "Level out of bounds: " + level;
        assert col >= 0 && row >= 0 : "Invalid Col/Row: " + col + ", " + row;

        if (level > this.depth) {
            return null;
        }

        if (levels[level] == null) {
            return null;
        }

        return levels[level].get(getKey(level, col, row));
    }

    /**
     * Gets a tile given a level and texture coordinates in UV.
     *
     * @param level The level.
     * @param u     The U texture coordinate in [0,1].
     * @param v     The V texture coordinate in [0,1].
     *
     * @return The tile at the given level and UV.
     */
    public SVTQuadtreeNode<T> getTileFromUV(int level, double u, double v) {
        final var pair = getColRow(level, u, v);
        return getTile(level, pair[0], pair[1]);
    }

    public long getUTileCount(int level) {
        return root.length * (1L << level);
    }

    public long getVTileCount(int level) {
        if (level == 0) {
            return 1;
        } else {
            return 2L << (level - 1);
        }
    }

    public int[] getColRow(int level, double u, double v) {
        long vCount = getVTileCount(level);
        long uCount = vCount * root.length;
        final var col = (int) (u * uCount);
        final var row = (int) (v * vCount);
        return new int[] { col, row };
    }

    public double[] getUV(int level, int col, int row) {
        double vCount = getVTileCount(level);
        double uCount = vCount * root.length;
        final var u = (double) col / uCount;
        final var v = (vCount - row) / vCount;
        return new double[] { u, v };
    }

    public boolean contains(int level, int col, int row) {
        return levels[level] != null && levels[level].containsKey(getKey(level, col, row));
    }

    public long getKey(int level, int col, int row) {
        return (long) (level) << 45 | (long) col << 26 | (long) row;
    }
    public long getKey(SVTQuadtreeNode<T> tile) {
        return tile.getKey();
    }

    /**
     * Get the resolution of this SVT at the most-detailed level.
     *
     * @return The resolution at the most detailed level.
     */
    public int[] getResolution() {
        int tiles = (int) Math.pow(2, depth);
        return new int[] { tileSize * tiles * root.length, tileSize * tiles };
    }
}
