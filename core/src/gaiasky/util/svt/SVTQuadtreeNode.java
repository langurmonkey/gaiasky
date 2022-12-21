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
public class SVTQuadtreeNode<T> {

    public final int level;
    public final int col;
    public final int row;
    public final T object;

    public SVTQuadtreeNode(final int level, final int col, final int row, final T object) {
        this.level = level;
        this.col = col;
        this.row = row;
        this.object = object;
    }

    @Override
    public String toString() {
        return "Node {" + "L" + level + ", col=" + col + ", row=" + row + ", o=" + object + '}';
    }
}
