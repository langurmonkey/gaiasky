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

    public void insert(int level, int u, int v, T object) {
        assert level >= 0 && level <= MAX_LEVEL : "Level out of bounds: " + level;
        assert u >= 0 && v >= 0 : "Invalid UV: " + u + ", " + v;

        if (levels[level] == null) {
            // Create map.
            levels[level] = new HashMap<>();
        }

        var tile = new SVTQuadtreeNode<T>(level, u, v, object);
        levels[level].put(getKey(u, v), tile);
        numTiles++;
    }

    public SVTQuadtreeNode<T> getNode(int level, int u, int v) {
        assert level >= 0 && level <= MAX_LEVEL : "Level out of bounds: " + level;
        assert u >= 0 && v >= 0 : "Invalid UV: " + u + ", " + v;

        if (levels[level] == null) {
            return null;
        }

        return levels[level].get(getKey(u, v));
    }

    public boolean contains(int level, int u, int v) {
        return levels[level] != null && levels[level].containsKey(getKey(u, v));
    }

    public long getKey(int u, int v) {
        return (long) u << MAX_LEVEL + (long) v;
    }
}
