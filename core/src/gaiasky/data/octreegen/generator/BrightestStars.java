/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen.generator;

import com.badlogic.gdx.math.MathUtils;
import gaiasky.data.octreegen.StarBrightnessComparator;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.tree.OctreeNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BrightestStars implements IAggregationAlgorithm {
    /** Maximum depth of the octree **/
    private final int MAX_DEPTH;
    /** Maximum number of objects in the densest node of a level **/
    private final int MAX_PART;
    /**
     * Minimum number of objects under which we do not further break the octree
     **/
    private final int MIN_PART;

    /** Whether to discard stars due to density or not **/
    private boolean DISCARD = false;

    Comparator<IParticleRecord> comp;

    int discarded = 0;

    /**
     * Constructor using fields
     * 
     * @param maxDepth
     *            Maximum depth of the octree
     * @param maxPart
     *            Number of objects in the densest node of this level
     * @param minPart
     *            Number of objects below which we do not further break the
     *            octree
     * @param discard
     *            Whether to discard stars due to density or not
     */
    public BrightestStars(int maxDepth, int maxPart, int minPart, boolean discard) {
        comp = new StarBrightnessComparator();
        this.MAX_DEPTH = maxDepth;
        this.MAX_PART = maxPart;
        this.MIN_PART = minPart;
        this.DISCARD = discard;
    }

    @Override
    public boolean sample(List<IParticleRecord> inputStars, OctreeNode octant, float percentage) {
        // Calculate nObjects for this octant based on maxObjs and the MAX_PART
        int nInput = inputStars.size();
        int nObjects = MathUtils.clamp(Math.round(nInput * percentage), 1, Integer.MAX_VALUE);

        StarGroup sg = new StarGroup();
        List<IParticleRecord> data = new ArrayList<>();

        if (nInput <= MIN_PART || octant.depth >= MAX_DEPTH) {
            if (!DISCARD) {
                // Never discard any
                for (IParticleRecord s : inputStars) {

                    if (s.octant() == null) {
                        data.add(s);
                        s.setOctant(octant);
                    }
                }
            } else {
                if (nInput <= MIN_PART) {
                    // Downright use all stars that have not been assigned
                    for (IParticleRecord s : inputStars) {
                        if (s.octant() == null) {
                            data.add(s);
                            s.setOctant(octant);
                        }
                    }
                } else {
                    // Select sample, discard the rest
                    inputStars.sort(comp);
                    for (int i = 0; i < nObjects; i++) {
                        IParticleRecord s = inputStars.get(i);
                        if (s.octant() == null) {
                            data.add(s);
                            s.setOctant(octant);
                        }
                    }

                    discarded += nInput - nObjects;
                }
            }
            sg.setData(data, false);
            octant.add(sg);
            sg.octant = octant;
            return true;
        } else {
            // Extract sample
            inputStars.sort(comp);
            int added = 0;
            int i = 0;
            while (added < nObjects && i < inputStars.size()) {
                IParticleRecord s = inputStars.get(i);
                if (s.octant() == null) {
                    // Add star
                    data.add(s);
                    s.setOctant(octant);
                    added++;
                }
                i++;
            }
            if (added > 0) {
                sg.setData(data, false);
                octant.add(sg);
                sg.octant = octant;
            }
            // It is leaf if we added all the stars
            return added == inputStars.size();
        }

    }

    public int getMaxPart() {
        return MAX_PART;
    }

    public int getDiscarded() {
        return discarded;
    }

    @Override
    public int getMaxDepth() {
        return MAX_DEPTH;
    }
}
