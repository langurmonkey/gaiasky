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
import gaiasky.util.*;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

import java.util.Arrays;
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
    protected static final double UPDATE_INTERVAL_S = 0.5;
    protected static final double UPDATE_INTERVAL_S_2 = UPDATE_INTERVAL_S * 2.0;
    // Camera dx threshold
    protected static final double CAM_DX_TH = 100 * Constants.PC_TO_U;
    protected static final double CAM_DX_TH_SQ = CAM_DX_TH * CAM_DX_TH;
    /** Base component. **/
    private final Base base;
    /** Reference to the particle set component. **/
    private final ParticleSet particleSet;
    /** Consumer method that updates the metadata. **/
    private final BiConsumer<ITimeFrameProvider, ICamera> updateMetadataConsumer;
    /** Reference to the dataset description component. **/
    private final DatasetDescription datasetDescription;
    private final ParticleUtils utils;
    private final IntDoublePriorityQueue reusableQueue;
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

    /**
     * This number should be larger than both {@link Settings.SceneSettings.ParticleSettings#numLabels} and {@link gaiasky.util.Settings.SceneSettings.StarSettings.GroupSettings#numLabels}.
     */
    private final int K;

    public ParticleSetUpdaterTask(Entity entity,
                                  ParticleSet particleSet,
                                  StarSet starSet) {
        this.base = Mapper.base.get(entity);
        if (starSet != null) {
            this.particleSet = starSet;
        } else {
            this.particleSet = particleSet;
        }
        this.datasetDescription = Mapper.datasetDescription.get(entity);
        this.utils = new ParticleUtils();
        this.K = this.particleSet.numLabels;
        this.reusableQueue = new IntDoublePriorityQueue(K);
        this.stage = SORT;

        if (starSet != null) {
            updateMetadataConsumer = this::updateMetadataParticlesExt;
        } else {
            updateMetadataConsumer = this.particleSet.isExtended ? this::updateMetadataParticlesExt : this::updateMetadataParticles;
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
        if (pointData != null && !pointData.isEmpty() && pointData.get(0)
                .names() != null) {
            double t = GaiaSky.instance.getT() - particleSet.lastSortTime;
            if (stage != BUSY
                    && base.opacity > 0
                    && (t > UPDATE_INTERVAL_S_2
                    || (particleSet.lastSortCameraPos.dst2d(camera.getPos()) > CAM_DX_TH_SQ && t > UPDATE_INTERVAL_S)
                    || (GaiaSky.instance.time.getWarpFactor() > 1.0e12 && t > UPDATE_INTERVAL_S))) {
                GaiaSky.instance.getExecutorService()
                        .execute(this);
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
                updateMetadataConsumer.accept(time, camera);
                stage = SORT;
            }
            case SORT -> {
                stage = BUSY;

                var totalCount = particleSet.pointData.size();
                var metadata = particleSet.metadata;

                // Clear queue
                this.reusableQueue.clear();

                boolean bg = false;
                for (int i = 0; i < totalCount; i++) {
                    var value = metadata[i];
                    if (reusableQueue.size() < K) {
                        reusableQueue.add(i, value);
                    } else if (value < reusableQueue.peekLastValue()) {
                        reusableQueue.removeLast();
                        reusableQueue.add(i, value);
                    }
                }

                // Now move topK to array and sort it (optional)
                int[] topIndices = this.reusableQueue.indexArray();
                var targetIndices = particleSet.indices;
                System.arraycopy(topIndices, 0, targetIndices, 0, topIndices.length);

                // Synchronously with the render thread, update indices, lastSortTime and updating state.
                GaiaSky.postRunnable(() -> {
                    particleSet.lastSortCameraPos.set(camera.getPos());
                    particleSet.lastSortTime = GaiaSky.instance.getT();
                    stage = METADATA;
                });
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
        Vector3d camPos = camera.getPos()
                .tov3d(D34);
        int n = particleSet.pointData.size();
        for (int i = 0; i < n; i++) {
            IParticleRecord d = particleSet.pointData.get(i);
            Vector3d x = D31.set(d.x(), d.y(), d.z());
            particleSet.metadata[i] = utils.filter(i, particleSet, datasetDescription) ? camPos.dst2(x) : Double.MAX_VALUE;
        }
    }

    /**
     * Computes the current apparent magnitude given the absolute magnitude and distance squared.
     *
     * @param absMag          The absolute magnitude.
     * @param distanceSquared The distance^2 to the star.
     *
     * @return The apparent magnitude at the given distance.
     */
    public static double apparentMag(float absMag, double distanceSquared) {
        return 2.5 * FastMath.log10(distanceSquared) - 5.0 + absMag;
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
        // Extended particles and stars, propagate proper motion, weigh with pseudo-size.
        Vector3d camPos = camera.getPos()
                .tov3d(D34);
        double deltaYears = AstroUtils.getMsSince(time.getTime(), particleSet.epochJd) * Nature.MS_TO_Y;
        if (particleSet.pointData != null) {
            int n = particleSet.pointData.size();
            for (int i = 0; i < n; i++) {
                IParticleRecord d = particleSet.pointData.get(i);

                // Pm
                Vector3d dx = D32.set(d.vx(), d.vy(), d.vz())
                        .scl(deltaYears);
                // Pos
                Vector3d pos = D31.set(d.x(), d.y(), d.z())
                        .add(dx);

                // Use magnitude.
                particleSet.metadata[i] = utils.filter(i, particleSet, datasetDescription) ?
                        apparentMag(d.absMag(), camPos.dst2(pos)) :
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

}
