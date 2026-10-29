package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.render.gdx.graphics.VolumeTexture;
import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.util.Logger;
import gaiasky.util.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stores information for volumetric rendering of bodies.
 */
public class Volume implements Component, IDisposable {
    public static final Logger.Log logger = Logger.getLogger(Volume.class);

    /** Slot of the emissive volume texture: what the galaxy emits. **/
    public static final int SLOT_EMISSIVE = 0;
    /** Slot of the occlusive volume texture: what absorbs light through it. **/
    public static final int SLOT_OCCLUSIVE = 1;

    public String vertexShader = "assets/shader/pbr.vertex.glsl";
    public String fragmentShader = "assets/shader/volume.fragment.glsl";
    public int key;

    /**
     * Procedural generation fields of this volume, or null/empty for a volume loaded from a file.
     * <p>
     * These are the same shape parameters a {@link gaiasky.scene.record.BillboardDataset} carries,
     * describing the analytic density model of {@code assets/shader/lib/galaxy.glsl}. A volume is
     * generated from its own fields and from nothing else - no billboard set is read, written or
     * emptied to produce a volume. A volume without fields is a plain texture volume, which is
     * what the Milky Way is.
     * <p>
     * The routing is by {@link GalaxyAttributes#type}: occluding channels (dust) go to the
     * occlusive slot, everything else to the emissive one. There is no slot index in the
     * descriptor - the raymarcher has exactly two slots and always will.
     */
    public GalaxyAttributes[] fields;

    /** Volume texture generated from {@link #emissiveFields()}, or null. Owned by this component. **/
    private VolumeTexture generatedEmissive;
    /** Volume texture generated from {@link #occlusiveFields()}, or null. Owned by this component. **/
    private VolumeTexture generatedOcclusive;
    /** Set when the component is disposed, see {@link #isDisposed()}. **/
    private boolean disposed;

    /**
     * Setter used by the scene JSON loader, mirroring {@link gaiasky.scene.component.BillboardSet#setData(Object[])}.
     *
     * @param fields The field objects, each of them a {@link GalaxyAttributes}.
     */
    public void setFields(Object[] fields) {
        if (fields == null) {
            this.fields = null;
            return;
        }
        this.fields = new GalaxyAttributes[fields.length];
        for (int i = 0; i < fields.length; i++) {
            this.fields[i] = (GalaxyAttributes) fields[i];
        }
    }

    /** @return Whether this volume is generated from {@link #fields} rather than from a file. **/
    public boolean isProcedural() {
        return fields != null && fields.length > 0;
    }

    /**
     * @return The fields that go into the emissive slot, in descriptor order. Empty if none.
     */
    public List<GalaxyAttributes> emissiveFields() {
        return fieldsOfSlot(false);
    }

    /**
     * @return The fields that go into the occlusive slot, in descriptor order. Empty if none.
     */
    public List<GalaxyAttributes> occlusiveFields() {
        return fieldsOfSlot(true);
    }

    private List<GalaxyAttributes> fieldsOfSlot(boolean occlusive) {
        var list = new ArrayList<GalaxyAttributes>();
        if (fields != null) {
            for (var field : fields) {
                if (field.isOcclusive() == occlusive) {
                    list.add(field);
                }
            }
        }
        return list;
    }

    /**
     * The texture generated for the given slot, or null if the slot is empty or was not generated.
     * The texture is owned by this component, not by the asset manager, so it has to be disposed
     * here - see {@link #dispose(Entity)}.
     */
    public VolumeTexture getGeneratedVolume(int slot) {
        return switch (slot) {
            case SLOT_EMISSIVE -> generatedEmissive;
            case SLOT_OCCLUSIVE -> generatedOcclusive;
            default -> null;
        };
    }

    /**
     * Stores the texture generated for the given slot, disposing whatever the slot held before.
     * Passing null clears the slot.
     *
     * @param slot    {@link #SLOT_EMISSIVE} or {@link #SLOT_OCCLUSIVE}.
     * @param texture The generated volume, owned by this component from now on.
     */
    public void setGeneratedVolume(int slot,
                                   VolumeTexture texture) {
        switch (slot) {
            case SLOT_EMISSIVE -> {
                disposeVolume(generatedEmissive, texture);
                generatedEmissive = texture;
            }
            case SLOT_OCCLUSIVE -> {
                disposeVolume(generatedOcclusive, texture);
                generatedOcclusive = texture;
            }
            default -> logger.warn("No such volume slot: " + slot);
        }
    }

    private static void disposeVolume(VolumeTexture previous,
                                      VolumeTexture replacement) {
        if (previous != null && previous != replacement && previous.texture() != null) {
            previous.texture().dispose();
        }
    }

    /**
     * Releases the generated textures. Called by the scene when the entity is removed, which is the
     * only place these textures are freed: they are not registered with the asset manager, so
     * nothing else will.
     */
    @Override
    public void dispose(Entity e) {
        disposed = true;
        setGeneratedVolume(SLOT_EMISSIVE, null);
        setGeneratedVolume(SLOT_OCCLUSIVE, null);
    }

    /**
     * @return Whether this component has been disposed. Generation may complete after the entity is
     * gone when lazy texture initialisation is on, and a texture bound to a disposed component would
     * leak, so the deferred generation checks this first.
     */
    public boolean isDisposed() {
        return disposed;
    }

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
