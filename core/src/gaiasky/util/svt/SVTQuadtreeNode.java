package gaiasky.util.svt;

/**
 * <p>
 * A quadtree node that represents a single tile with a specific level of detail (LOD)
 * in the Sparse Virtual Texture (SVT).
 * Contains the level of detail ({@link #level}), the column within the level ({@link #col}), the
 * row within the level ({@link #row}) and the path to the actual texture file.
 * </p>
 * <p>
 * Each node is subdivided into four tiles like so:
 * </p>
 * <pre>
 *      0     1
 *    .----.----.
 * 0  | TL | TR |
 *    :----+----:
 * 1  | BL | BR |
 *    '----'----'
 * </pre>
 * <p>
 * Columns increase left-to-right, and rows increase top-to-bottom. Note that in UV coordinates, the V
 * coordinate, corresponding to the rows, is inverted.
 * </p>
 */
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
     * Get the UV coordinates in [0,1] of the top left position of this tile.
     * @return The UV coordinates of this tile.
     */
    public double[] getUV(){
        var numCols = (double) tree.getUTileCount(level);
        var numRows = (double) tree.getVTileCount(level);

        return new double[]{ (double) col / numCols, (double) row / numRows };
    }

    /**
     * Computes the OpenGL mip level for this tile.
     * @return The OpenGL mip level for this tile.
     */
    public int mipLevel() {
        return tree.depth - level;
    }

    @Override
    public String toString() {
        return "Node {" + "L" + level + ", col=" + col + ", row=" + row + ", o=" + object + '}';
    }

    @Override
    public int compareTo(SVTQuadtreeNode o) {
        // The natural order of nodes depends only on their depth.
        return Integer.compare(this.level, o.level);
    }
}
