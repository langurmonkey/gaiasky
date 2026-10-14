package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

/**
 * Stores information for volumetric rendering of bodies.
 */
public class Volume implements Component {

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
}
