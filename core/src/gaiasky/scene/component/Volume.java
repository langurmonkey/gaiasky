package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.Logger;
import gaiasky.util.Settings;

import java.util.Arrays;

/**
 * Stores information for volumetric rendering of bodies.
 */
public class Volume implements Component {
    public static final Logger.Log logger = Logger.getLogger(Volume.class);

    public String vertexShader = "assets/shader/pbr.vertex.glsl";
    public String fragmentShader = "assets/shader/volume.fragment.glsl";
    public int key;

    /**
     * Whether this volume takes part in the depth-only pre-pass. Enabled by default.
     * <p>
     * A volume can be a real occluder: its box spans a region of space, and the fragment shader
     * writes the depth of the first sample dense enough to hide what is behind it, so anything
     * drawn after the volume pass (labels, grids, stars, planets) is hidden behind it. Without
     * the write, a ray that crosses dust but no gas emits no colour at all - the dust only
     * attenuates - so everything behind the dust shows through it. Set to false to restore the
     * read-only, always-see-through behaviour.
     * <p>
     * Which volumes actually occlude is decided per slot, by
     * {@link gaiasky.scene.record.MaterialComponent#volumeDepthThreshold}: an emissive volume
     * such as gas normally carries no threshold and stays transparent to the depth buffer, while
     * the absorbing one (dust) carries one, so only its dense lanes hide anything. With no
     * threshold set on any slot this flag has no visible effect, and the pre-pass is wasted work.
     */
    public boolean writeDepth = true;

    /**
     * Setter used by the scene JSON loader. Accepts {@link Boolean} so an explicit
     * <code>"writeDepth": false</code> overrides the default.
     */
    public void setWriteDepth(Boolean writeDepth) {
        if (writeDepth != null) {
            this.writeDepth = writeDepth;
        }
    }

    /**
     * Number of iterations for the ray-marching volume shader, per quality preset.
     */
    public int[] iterations = {90, 90, 90, 90};

    /**
     * Sets the iteration count for the ray-marcher on this volume.
     *
     * @param iterations Single value: same iteration count for all graphics qualities. The JSON loader hands over a
     *                   scalar integer as {@link Long}, so this must take a Long for the reflective lookup to find it.
     */
    public void setIterations(Long iterations) {
        if (iterations == null) {
            return;
        }
        if (iterations <= 0) {
            logger.warn("Iterations must be greater than zero: " + iterations);
        }
        int n = Settings.GraphicsQuality.values().length;
        this.iterations = new int[n];
        for (int i = 0; i < n; i++)
            this.iterations[i] = iterations.intValue();
    }

    /**
     * Alias to {@link #setIterations(Long)}.
     */
    public void setRaymarchIterations(Long iterations) {
        setIterations(iterations);
    }

    /**
     * Alias to {@link #setIterations(Long)}.
     */
    public void setIterationCount(Long iterations) {
        setIterations(iterations);
    }

    /**
     * Sets the iteration count for the ray-marcher on this volume.
     *
     * @param iterations Array of integers, one per graphics quality [LOW, NORMAL, HIGH, ULTRA].
     */
    public void setIterations(int[] iterations) {
        int n = Settings.GraphicsQuality.values().length;
        if (iterations.length == n) {
            this.iterations = new int[n];
            for (int i = 0; i < n; i++) {
                if (iterations[i] <= 0) {
                    logger.warn("Iterations must be greater than zero: " + iterations[i]);
                }
                this.iterations[i] = iterations[i];
            }
        } else {
            logger.warn("'iterations' must either be an integer, or an array with "
                                + Settings.GraphicsQuality.values().length + " values: "
                                + Arrays.toString(iterations));
        }
    }

    /**
     * Alias to {@link #setIterations(int[])}.
     */
    public void setRaymarchIterations(int[] iterations) {
        setIterations(iterations);
    }

    /**
     * Alias to {@link #setIterations(int[])}.
     */
    public void setIterationCount(int[] iterations) {
        setIterations(iterations);
    }
}
