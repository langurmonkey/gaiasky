package gaiasky.util.svt;

import java.nio.file.Path;

/**
 * A quadtree node that represents a single tile with a specific level of detail (LOD)
 * in the Sparse Virtual Texture (SVT).
 * Contains the level of detail ({@link #lod}), the column within the level ({@link #u}), the
 * row within the level ({@link #v}) and the path to the actual texture file.
 * Each node is subdivided into four tiles like so:
 * .----.----.
 * | TL | TR |
 * :----+----:
 * | BL | BR |
 * '----'----'
 */
public class SVTQuadtreeNode<T> {

    public final int lod;
    public final int u;
    public final int v;
    public final T object;

    public SVTQuadtreeNode(final int lod, final int u, final int v, final T object) {
        this.lod = lod;
        this.u = u;
        this.v = v;
        this.object = object;
    }
}
