package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scenegraph.ParticleGroup;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.camera.Proximity;
import gaiasky.util.math.Vector3d;

import java.util.*;

public class ParticleSet implements Component {

    public static long idSeq = 0;

    /**
     * List that contains the point data. It contains only [x y z]
     */
    public List<IParticleRecord> pointData;

    /**
     * Fully qualified name of data provider class
     */
    public String provider;

    /**
     * Path of data file
     */
    public String datafile;

    // Parameters for the data provider
    public Map<String, Object> providerParams;

    /**
     * Profile decay of the particles in the shader
     */
    public float profileDecay = 0.2f;

    /**
     * Noise factor for the color in [0,1]
     */
    public float colorNoise = 0;

    /**
     * Particle size limits
     */
    public double[] particleSizeLimitsPoint = new double[] { 2d, 50d };
    /**
     * Particle size limits for the GL_TRIANGLES renderer. This will be multiplied by
     * the distance to the particle in the shader, so that <code>size = tan(angle) * dist</code>
     */
    public double[] particleSizeLimits = new double[] { Math.tan(Math.toRadians(0.1)), Math.tan(Math.toRadians(6.0)) };

    /**
     * This flag indicates whether the mean position is already given by the
     * JSON injector
     */
    public boolean fixedMeanPosition = false;

    /**
     * Factor to apply to the data points, usually to normalise distances
     */
    public Double factor = null;


    /**
     * Mapping colors
     */
    public float[] ccMin = null, ccMax = null;

    /**
     * Stores the time when the last sort operation finished, in ms
     */
    public long lastSortTime;

    /**
     * The mean distance from the origin of all points in this group.
     * Gives a sense of the scale.
     */
    public double meanDistance;
    public double maxDistance, minDistance;

    /**
     * Geometric centre at epoch, for render sorting
     */
    public static Vector3d geomCentre;

    /**
     * Reference to the current focus
     */
    public IParticleRecord focus;

    /**
     * Proximity particles
     */
    public Proximity proximity;

    // Has been disposed
    public boolean disposed = false;

    // Name index
    public Map<String, Integer> index;

    // Minimum amount of time [ms] between two update calls
    public static final double UPDATE_INTERVAL_MS = 1500;
    public static final double UPDATE_INTERVAL_MS_2 = UPDATE_INTERVAL_MS * 2;

    // Metadata, for sorting - holds distances from each particle to the camera, squared
    public double[] metadata;

    // Comparator
    private  Comparator<Integer> comp;

    // Indices list buffer 1
    public Integer[] indices1;
    // Indices list buffer 2
    public Integer[] indices2;
    // Active indices list
    public Integer[] active;
    // Background indices list (the one we sort)
    public Integer[] background;

    // Visibility array with 1 (visible) or 0 (hidden) for each particle
    public byte[] visibilityArray;

    // Is it updating?
    public volatile boolean updating = false;

    // Updater task
    public ParticleGroup.UpdaterTask updaterTask;

    // Camera dx threshold
    public static final double CAM_DX_TH = 100 * Constants.PC_TO_U;
    // Last sort position
    public Vector3d lastSortCameraPos, cPosD;


    /**
     * Returns the list of particles.
     */
    public List<IParticleRecord> data() {
        return pointData;
    }

    public void setData(List<IParticleRecord> pointData) {
        setData(pointData, true);
    }

    public void setData(List<IParticleRecord> pointData, boolean regenerateIndex) {
        this.pointData = pointData;

        // Regenerate index
        if (regenerateIndex)
            regenerateIndex();
        // Initialize visibility - all visible
        this.visibilityArray = new byte[pointData.size()];
        for (int i = 0; i < pointData.size(); i++) {
            this.visibilityArray[i] = (byte) 1;
        }
    }

    /**
     * Regenerates the name index
     */
    public void regenerateIndex() {
        index = generateIndex(data());
    }

    /**
     * Generates the index (maps name to array index)
     * and computes the geometric center of this group
     *
     * @param pointData The data
     *
     * @return An map{string,int} mapping names to indices
     */
    public Map<String, Integer> generateIndex(List<IParticleRecord> pointData) {
        Map<String, Integer> index = new HashMap<>((int) (pointData.size() * 1.25));
        int n = pointData.size();
        for (int i = 0; i < n; i++) {
            IParticleRecord pb = pointData.get(i);
            if (pb.names() != null) {
                final int idx = i;
                Arrays.stream(pb.names()).forEach(name -> index.put(name.toLowerCase(), idx));
            }
        }
        return index;
    }

    public void setDatafile(String datafile) {
        this.datafile = datafile;
    }

    public void setProviderparams(Map<String, Object> params) {
        this.providerParams = params;
    }


    public void setFactor(Double factor) {
        this.factor = factor * Constants.DISTANCE_SCALE_FACTOR;
    }

    public void setProfiledecay(Double profiledecay) {
        this.profileDecay = profiledecay.floatValue();
    }

    public void setColornoise(Double colorNoise) {
        this.colorNoise = colorNoise.floatValue();
    }

    public void setParticlesizelimits(double[] sizeLimits) {
        if (sizeLimits[0] > sizeLimits[1])
            sizeLimits[0] = sizeLimits[1];
        this.particleSizeLimits = sizeLimits;
    }

}
