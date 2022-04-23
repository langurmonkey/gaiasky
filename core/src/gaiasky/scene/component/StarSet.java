package gaiasky.scene.component;

import java.util.Map;
import java.util.Set;

public class StarSet extends ParticleSet {
    /**
     * Epoch for positions/proper motions in julian days
     **/
    public double epochJd;

    /**
     * Epoch for the times in the light curves in julian days
     */
    public double variabilityEpochJd;
    /**
     * Current computed epoch time
     **/
    public double currDeltaYears = 0;

    public double modelDist;

    /** Does this contain variable stars? **/
    public boolean variableStars = false;

    /** Stars for which forceLabel is enabled **/
    public Set<Integer> forceLabelStars;
    /** Stars with special label colors **/
    public Map<Integer, float[]> labelColors;
}
