/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen.generator;

/**
 * Holds the parameters for the octree generation
 *
 * @author Toni Sagrista
 */
public class OctreeGeneratorParams {

    public int maxPart;
    public boolean postprocess;
    public long childCount;
    public long parentCount;

    public OctreeGeneratorParams(int maxPart, boolean postprocess, long childCount, long parentCount) {
        super();
        this.maxPart = maxPart;
        this.postprocess = postprocess;
        this.childCount = childCount;
        this.parentCount = parentCount;
    }

}
