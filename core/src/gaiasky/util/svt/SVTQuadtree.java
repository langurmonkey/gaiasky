package gaiasky.util.svt;

import com.badlogic.gdx.utils.Array;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * A Sparse Virtual Texture (SVT) quadtree with a certain LOD depth and tile size.
 * Only square tiles are supported. The tree typically contains two root nodes,
 * since the textures that wrap around spherical objects have an aspect ratio of 2:1.
 */
public class SVTQuadtree<T> {
    private final int MAX_LEVEL = 15;

    /** Size in pixels of each tile. Tiles are square, so width and height are equal. **/
    public final long tileSize;

    public SVTQuadtreeNode<T>[] root;

    /** Each tile is identified by its level and its UV. Here we can access tiles directly. **/
    public Map<Long, SVTQuadtreeNode<T>>[] levels;

    /** The total number of tiles in the tree. **/
    public int numTiles;

    public SVTQuadtree(int tileSize, int rootPositions) {
        this.tileSize = tileSize;
        this.root = new SVTQuadtreeNode[rootPositions];
        this.levels = new Map[MAX_LEVEL];
    }

    public void insert(int level, int col, int row, T object) {
        assert level >= 0 && level <= MAX_LEVEL : "Level out of bounds: " + level;
        assert col >= 0 && row >= 0 : "Invalid UV: " + col + ", " + row;

        if (levels[level] == null) {
            // Create map.
            levels[level] = new HashMap<>();
        }

        var tile = new SVTQuadtreeNode<T>(level, col, row, object);
        levels[level].put(getKey(col, row), tile);
        numTiles++;
    }

    public SVTQuadtreeNode<T> getNode(int level, int col, int row) {
        assert level >= 0 && level <= MAX_LEVEL : "Level out of bounds: " + level;
        assert col >= 0 && row >= 0 : "Invalid UV: " + col + ", " + row;

        if (levels[level] == null) {
            return null;
        }

        return levels[level].get(getKey(col, row));
    }

    /**
     * Gets a tile given a level and texture coordinates in UV.
     *
     * @param level The level.
     * @param u     The U texture coordinate in [0,1].
     * @param v     The V texture coordinate in [0,1].
     * @return The tile at the given level and UV.
     */
    public SVTQuadtreeNode<T> getTile(int level, double u, double v) {
        long vCount = getVTileCount(level);
        long uCount = vCount * 2;
        final var col = (int) (u * uCount);
        final var row = (int) ((1.0 - v) * uCount);
        return getNode(level, col, row);
    }

    public long getUTileCount(int level) {
        return 2 * (2L << level);
    }

    public long getVTileCount(int level) {
        return 2L << level;
    }

    public boolean contains(int level, int col, int row) {
        return levels[level] != null && levels[level].containsKey(getKey(col, row));
    }

    public long getKey(int col, int row) {
        return (long) col << MAX_LEVEL + (long) row;
    }
}
