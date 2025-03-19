/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.task;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.DatasetDescription;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.component.StarSet;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiConsumer;

import static gaiasky.scene.task.ParticleSetUpdaterTask.UpdateStage.*;


/**
 * The task to update particle sets. A full update operation happens in two steps:
 * <ul>
 *     <li>Update metadata: particle metadata like positions and proper motions are updated.</li>
 *     <li>Sort particles: particles are sorted w.r.t. their distance to the camera and brightness.</li>
 * </ul>
 */
public class ParticleSetUpdaterTask implements Runnable, IObserver {

    // Minimum amount of time [s] between two update calls
    protected static final double UPDATE_INTERVAL_S = 0.3;
    protected static final double UPDATE_INTERVAL_S_2 = UPDATE_INTERVAL_S * 2.0;
    // Camera dx threshold
    protected static final double CAM_DX_TH = 100 * Constants.PC_TO_U;
    protected static final double CAM_DX_TH_SQ = CAM_DX_TH * CAM_DX_TH;
    /** Base component. **/
    private final Base base;
    /** Reference to the particle set component. **/
    private final ParticleSet particleSet;
    /** Reference to the star set component. **/
    private final StarSet starSet;
    /** Consumer method that updates the metadata. **/
    private final BiConsumer<ITimeFrameProvider, ICamera> updateConsumer;
    /** Reference to the dataset description component. **/
    private final DatasetDescription datasetDescription;
    private final ParticleUtils utils;
    private final Comparator<Integer> comp;
    private final Vector3d D31 = new Vector3d();
    private final Vector3d D32 = new Vector3d();
    private final Vector3d D34 = new Vector3d();

    enum UpdateStage {
        // The Sort stage has finished, and the next stage is metadata.
        METADATA,
        // The metadata stage has finished, and the next stage is sort.
        SORT,
        // The updater is busy right now.
        BUSY
    }

    /** Contains the stage that needs to be run next for this updater. **/
    private UpdateStage stage;

    public ParticleSetUpdaterTask(Entity entity,
                                  ParticleSet particleSet,
                                  StarSet starSet) {
        this.base = Mapper.base.get(entity);
        this.particleSet = particleSet;
        this.starSet = starSet;
        this.datasetDescription = Mapper.datasetDescription.get(entity);
        this.utils = new ParticleUtils();
        this.comp = new ParticleSetComparator(this.particleSet);
        this.stage = SORT;

        if (this.starSet != null) {
            updateConsumer = this::updateMetadataStars;
        } else {
            updateConsumer = this.particleSet.isExtended ? this::updateMetadataParticlesExt : this::updateMetadataParticles;
        }

        EventManager.instance.subscribe(this, Event.FOCUS_CHANGED);
    }

    /**
     * Triggers the update task if the requirements for it are met:
     * <ul>
     *     <li>Set is not updating.</li>
     *     <li>Opacity is not 0.</li>
     *     <li>Time since last update is not larger than updateInterval*2.</li>
     *     <li>The camera has moved a lot.</li>
     *     <li>Time is on and very fast.</li>
     * </ul>
     *
     * @param camera The camera.
     */
    public void update(ICamera camera) {
        var pointData = particleSet.pointData;
        if (pointData != null && !pointData.isEmpty() && pointData.get(0).names() != null) {
            double t = GaiaSky.instance.getT() - particleSet.lastSortTime;
            if (stage != BUSY
                    && base.opacity > 0
                    && (t > UPDATE_INTERVAL_S_2
                    || (particleSet.lastSortCameraPos.dst2d(camera.getPos()) > CAM_DX_TH_SQ && t > UPDATE_INTERVAL_S)
                    || (GaiaSky.instance.time.getWarpFactor() > 1.0e12 && t > UPDATE_INTERVAL_S))) {
                GaiaSky.instance.getExecutorService().execute(this);
            }
        }
    }

    @Override
    public void run() {
        if (particleSet != null) {
            updateSorter(GaiaSky.instance.time, GaiaSky.instance.getICamera());
        }
    }

    private void updateSorter(ITimeFrameProvider time,
                              ICamera camera) {
        switch (stage) {
            case METADATA -> {
                // Prepare metadata to sort.
                stage = BUSY;
                updateConsumer.accept(time, camera);
                stage = SORT;
            }
            case SORT -> {
                stage = BUSY;
                // Sort background list of indices.
                Arrays.parallelSort(particleSet.background, comp);

                // Synchronously with the render thread, update indices, lastSortTime and updating state.
                GaiaSky.postRunnable(() -> {
                    swapBuffers();
                    particleSet.lastSortCameraPos.set(camera.getPos());
                    particleSet.lastSortTime = GaiaSky.instance.getT();
                    stage = METADATA;
                });
            }
        }

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
     * Updates the star metadata information, used mainly for sorting. For stars, we propagate the positions
     * with the proper motion and weigh the number using the pseudo-size, which is a proxy to the magnitude.
     *
     * @param time   The time frame provider.
     * @param camera The camera.
     */
    private void updateMetadataStars(ITimeFrameProvider time,
                                     ICamera camera) {
        // Stars, propagate proper motion, weigh with pseudo-size.
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

                starSet.metadata[i] = utils.filter(i, starSet, datasetDescription) ?
                        -((d.size() / camPos.dst2(x)) / camera.getFovFactor()) :
                        Double.MAX_VALUE;
            }
        }
    }

    /**
     * Updates the particle metadata information, used for sorting. In this case only the position (distance
     * from camera) is important.
     *
     * @param time   The time frame provider.
     * @param camera The camera.
     */
    private void updateMetadataParticles(ITimeFrameProvider time,
                                         ICamera camera) {
        // Particles, only distance.
        Vector3d camPos = camera.getPos().tov3d(D34);
        int n = particleSet.pointData.size();
        for (int i = 0; i < n; i++) {
            IParticleRecord d = particleSet.pointData.get(i);
            Vector3d x = D31.set(d.x(), d.y(), d.z());
            particleSet.metadata[i] = utils.filter(i, particleSet, datasetDescription) ? camPos.dst2(x) : Double.MAX_VALUE;
        }
    }

    /**
     * Updates the extended particle metadata information, used for sorting. In this case, the position (distance
     * from camera), the proper motion, and the size are important.
     *
     * @param time   The time frame provider.
     * @param camera The camera.
     */
    private void updateMetadataParticlesExt(ITimeFrameProvider time,
                                            ICamera camera) {
        // Particles, only distance.
        Vector3d camPos = camera.getPos().tov3d(D34);
        double deltaYears = AstroUtils.getMsSince(time.getTime(), particleSet.epochJd) * Nature.MS_TO_Y;
        if (particleSet.pointData != null) {
            int n = particleSet.pointData.size();
            for (int i = 0; i < n; i++) {
                IParticleRecord d = particleSet.pointData.get(i);

                // Pm
                Vector3d dx = D32.set(d.pmx(), d.pmy(), d.pmz()).scl(deltaYears);
                // Pos
                Vector3d pos = D31.set(d.x(), d.y(), d.z()).add(dx);

                particleSet.metadata[i] = utils.filter(i, particleSet, datasetDescription) ?
                        (-(d.size() / camPos.dst2(pos)) / camera.getFovFactor()) :
                        Double.MAX_VALUE;
            }
        }
    }

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        if (Objects.requireNonNull(event) == Event.FOCUS_CHANGED) {
            if (data[0] instanceof String) {
                particleSet.focusIndex = data[0].equals(base.getName()) ? particleSet.focusIndex : -1;
            } else {
                FocusView view = (FocusView) data[0];
                particleSet.focusIndex = (view.getSet() == particleSet) ? particleSet.focusIndex : -1;
            }
            utils.updateFocusDataPos(particleSet);
        }
    }

    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    /**
     * User order in metadata arrays to compare indices in this particle set.
     */
    private record ParticleSetComparator(ParticleSet set) implements Comparator<Integer> {

        @Override
        public int compare(Integer i1,
                           Integer i2) {
            return Double.compare(set.metadata[i1], set.metadata[i2]);
        }
    }
}
