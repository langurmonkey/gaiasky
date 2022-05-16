package gaiasky.scenegraph;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.DatasetDescription;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.CatalogInfo;
import gaiasky.util.Constants;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Implements index update process of particle sets.
 */
public class ParticleSetUpdaterTask implements Runnable, IObserver {

    /** Reference to old OOP model object. **/
    private final ParticleGroup pg;

    /** Reference to the entity. **/
    private final Entity entity;

    /** Reference to the particle set component. **/
    private final ParticleSet particleSet;

    /** Reference to the dataset description component. **/
    private final DatasetDescription datasetDescription;

    private final ParticleUtils utils;

    private final Comparator<Integer> comp;
    private final Vector3d D31 = new Vector3d();

    /**
     * User order in metadata arrays to compare indices
     */
    private class ParticleGroupComparator implements Comparator<Integer> {
        private ParticleSet set;

        public ParticleGroupComparator(ParticleSet set) {
            this.set = set;
        }

        @Override
        public int compare(Integer i1, Integer i2) {
            return Double.compare(set.metadata[i1], set.metadata[i2]);
        }
    }

    public ParticleSetUpdaterTask(ParticleGroup pg) {
        this.pg = pg;

        this.entity = null;
        this.particleSet = null;
        this.datasetDescription = null;
        this.utils = null;
        this.comp = null;
    }

    public ParticleSetUpdaterTask(Entity entity) {
        this.entity = entity;
        this.particleSet = Mapper.particleSet.get(entity);
        this.datasetDescription = Mapper.datasetDescription.get(entity);
        this.utils = new ParticleUtils();
        this.comp = new ParticleGroupComparator(this.particleSet);

        this.pg = null;

        EventManager.instance.subscribe(this, Event.FOCUS_CHANGED, Event.CAMERA_MOTION_UPDATE);
    }

    @Override
    public void run() {
        if (pg != null) {
            pg.updateSorter(GaiaSky.instance.time, GaiaSky.instance.getICamera());
        } else if (particleSet != null) {
            updateSorter(GaiaSky.instance.time, GaiaSky.instance.getICamera());
        }
    }

    private void updateSorter(ITimeFrameProvider time, ICamera camera) {
        var background = particleSet.background;

        // Prepare metadata to sort
        updateMetadata(time, camera);

        // Sort background list of indices
        Arrays.sort(background, comp);

        // Synchronously with the render thread, update indices, lastSortTime and updating state
        GaiaSky.postRunnable(() -> {
            swapBuffers();
            particleSet.lastSortTime = TimeUtils.millis();
            particleSet.updating = false;
        });
    }

    private void swapBuffers() {
        var indices1 = particleSet.indices1;
        var indices2 = particleSet.indices2;

        if (particleSet.active == indices1) {
            particleSet.active = indices2;
            particleSet.background = indices1;
        } else {
            particleSet.active = indices1;
            particleSet.background = indices2;
        }
    }

    /**
     * Updates the metadata information, to use for sorting. For particles, only the position (distance
     * from camera) is important.
     *
     * @param time   The time frame provider.
     * @param camera The camera.
     */
    private void updateMetadata(ITimeFrameProvider time, ICamera camera) {
        Vector3b camPos = camera.getPos();
        int n = particleSet.pointData.size();
        for (int i = 0; i < n; i++) {
            IParticleRecord d = particleSet.pointData.get(i);
            // Pos
            Vector3d x = D31.set(d.x(), d.y(), d.z());
            particleSet.metadata[i] = filter(i) ? camPos.dst2d(x) : Double.MAX_VALUE;
        }
    }

    /**
     * Evaluates the filter of this dataset (if any) for the given particle index
     *
     * @param index The index to filter
     *
     * @return The result of the filter evaluation, true if the particle passed the filtering, false otherwise
     */
    public boolean filter(int index) {
        final CatalogInfo catalogInfo = datasetDescription.catalogInfo;
        if (catalogInfo != null && catalogInfo.filter != null) {
            return catalogInfo.filter.evaluate(particleSet.get(index));
        }
        return true;
    }

    // Minimum amount of time [ms] between two update calls
    protected static final double UPDATE_INTERVAL_MS = 1500;
    protected static final double UPDATE_INTERVAL_MS_2 = UPDATE_INTERVAL_MS * 2;

    // Camera dx threshold
    protected static final double CAM_DX_TH = 100 * Constants.PC_TO_U;

    @Override
    public void notify(Event event, Object source, Object... data) {
        var base = Mapper.base.get(entity);
        switch (event) {
        case FOCUS_CHANGED:
            if (data[0] instanceof String) {
                particleSet.focusIndex = data[0].equals(base.getName()) ? particleSet.focusIndex : -1;
            } else {
                particleSet.focusIndex = data[0] == this ? particleSet.focusIndex : -1;
            }
            utils.updateFocusDataPos(particleSet);
            break;
        case CAMERA_MOTION_UPDATE:
            // Check that the particles have names
            var pointData = particleSet.pointData;
            if (particleSet.updaterTask != null && pointData.size() > 0 && pointData.get(0).names() != null) {
                final Vector3b currentCameraPos = (Vector3b) data[0];
                long t = TimeUtils.millis() - particleSet.lastSortTime;
                if (!particleSet.updating && base.opacity > 0 && (t > UPDATE_INTERVAL_MS_2 || (particleSet.lastSortCameraPos.dst(currentCameraPos) > CAM_DX_TH && t > UPDATE_INTERVAL_MS))) {
                    particleSet.updating = GaiaSky.instance.getExecutorService().execute(particleSet.updaterTask);
                }
            }
            break;
        default:
            break;
        }
    }
}
