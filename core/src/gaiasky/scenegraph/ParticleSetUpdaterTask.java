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
import gaiasky.scene.component.StarSet;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
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

    /** Reference to the star set component. **/
    private final StarSet starSet;

    /** Reference to the dataset description component. **/
    private final DatasetDescription datasetDescription;

    private final ParticleUtils utils;

    private final Comparator<Integer> comp;

    private final Vector3d D31 = new Vector3d();
    private final Vector3d D32 = new Vector3d();
    private final Vector3d D34 = new Vector3d();

    /**
     * User order in metadata arrays to compare indices in this particle set.
     */
    private class ParticleSetComparator implements Comparator<Integer> {
        private ParticleSet set;

        public ParticleSetComparator(ParticleSet set) {
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
        this.starSet = null;
        this.datasetDescription = null;
        this.utils = null;
        this.comp = null;
    }

    public ParticleSetUpdaterTask(Entity entity, ParticleSet particleSet, StarSet starSet) {
        this.entity = entity;
        this.particleSet = particleSet;
        this.starSet = starSet;
        this.datasetDescription = Mapper.datasetDescription.get(entity);
        this.utils = new ParticleUtils();
        this.comp = new ParticleSetComparator(this.particleSet);

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

    private long last = -1;

    private void updateSorter(ITimeFrameProvider time, ICamera camera) {
        // Prepare metadata to sort
        updateMetadata(time, camera);

        // Sort background list of indices
        Arrays.sort(particleSet.background, comp);

        // Synchronously with the render thread, update indices, lastSortTime and updating state
        GaiaSky.postRunnable(() -> {
            swapBuffers();
            particleSet.lastSortTime = TimeUtils.millis();
            particleSet.updating = false;
        });
    }

    private void swapBuffers() {
        if (particleSet.active == particleSet.indices1) {
            particleSet.active = particleSet.indices2;
            particleSet.background = particleSet.indices1;
        } else {
            particleSet.active = particleSet.indices1;
            particleSet.background = particleSet.indices2;
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
        if(starSet != null) {
            // Stars, propagate proper motion, weigh with size.
            Vector3d camPos = camera.getPos().tov3d(D34);
            double deltaYears = AstroUtils.getMsSince(time.getTime(), starSet.epochJd) * Nature.MS_TO_Y;
            if (starSet.pointData != null) {
                int n = starSet.pointData.size();
                for (int i = 0; i < n; i++) {
                    IParticleRecord d = starSet.pointData.get(i);

                    // Pm
                    Vector3d dx = D32.set(d.pmx(), d.pmy(), d.pmz()).scl(deltaYears);
                    // Pos
                    Vector3d x = D31.set(d.x(), d.y(), d.z()).add(dx);

                    starSet.metadata[i] = utils.filter(i, particleSet, datasetDescription) ? (-(((d.size() * Constants.STAR_SIZE_FACTOR) / camPos.dst(x)) / camera.getFovFactor()) * Settings.settings.scene.star.brightness) : Double.MAX_VALUE;
                }
            }
        } else {
            // Particles, only distance.
            Vector3b camPos = camera.getPos();
            int n = particleSet.pointData.size();
            for (int i = 0; i < n; i++) {
                IParticleRecord d = particleSet.pointData.get(i);
                particleSet.metadata[i] = utils.filter(i, particleSet, datasetDescription) ? camPos.dst2d(d.x(), d.y(), d.z()) : Double.MAX_VALUE;
            }
        }
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
                FocusView view = (FocusView) data[0];
                particleSet.focusIndex = (view.getSet() == particleSet) ? particleSet.focusIndex : -1;
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
                    particleSet.updating = GaiaSky.instance.getExecutorService().execute(this);
                }
            }
            break;
        default:
            break;
        }
    }
}
