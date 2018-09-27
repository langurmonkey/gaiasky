package gaia.cu9.ari.gaiaorbit.data.octreegen.generator;

import java.util.Comparator;

import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.octreegen.StarBrightnessComparator;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

public class BrightestStarsSimple implements IAggregationAlgorithm {
    /** Maximum number of objects in the densest node of a level **/
    private int MAX_PART;

    Comparator<StarBean> comp;


    /**
     * Constructor using fields
     * 
     * @param maxPart
     *            Number of objects in the densest node of this level
     */
    public BrightestStarsSimple(int maxPart) {
        comp = new StarBrightnessComparator();
        this.MAX_PART = maxPart;
    }

    @Override
    public boolean sample(Array<StarBean> inputStars, OctreeNode octant, float percentage) {
        StarGroup sg = new StarGroup();
        Array<StarBean> data = new Array<StarBean>();

        int nInput = inputStars.size;
        inputStars.sort(comp);

        int added = 0;
        while (added < MAX_PART && added < nInput) {
            StarBean sb = inputStars.get(added);
            if (sb.octant == null) {
                data.add(sb);
                sb.octant = octant;
                added++;
            }
        }
        if (added > 0) {
            sg.setData(data, false);
            octant.add(sg);
            sg.octant = octant;
            sg.octantId = octant.pageId;
        }
        return added == inputStars.size;

    }

    public int getMaxPart() {
        return MAX_PART;
    }

    public int getDiscarded() {
        return 0;
    }

    @Override
    public int getMaxDepth() {
        return 30;
    }

}
