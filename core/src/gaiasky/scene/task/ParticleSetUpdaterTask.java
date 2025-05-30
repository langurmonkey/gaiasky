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
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.TopNBuffer;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.ds.GaiaSkyExecutorService;
import gaiasky.util.math.Vector3D;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

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
    private final TopNBuffer buffer;
    private final Vector3D D31 = new Vector3D();
    private final Vector3D D32 = new Vector3D();
    private final Vector3D D34 = new Vector3D();

    private final GaiaSkyExecutorService executor;

    enum UpdateStage {
        /** Compute metadata. **/
        METADATA,
        /** First part of the sort. **/
        SORT1,
        /** Seconds part of the sort. **/
        SORT2,
        /** Working. **/
        BUSY
    }

    /** Contains the stage that needs to be run next for this updater. **/
    private final ThreadLocal<UpdateStage> stage;

    /** Nested class holding the brightness lookup table. **/
    private static class StarBrightness {
        private static final float MIN_MAG = -10.0f;
        private static final float STEP = 0.01f;
        public static final float[] BRIGHTNESS_TABLE = new float[3001];

        static {
            for (int i = 0; i < BRIGHTNESS_TABLE.length; i++) {
                float mag = MIN_MAG + i * STEP;
                BRIGHTNESS_TABLE[i] = (float) Math.pow(10.0, -0.4 * mag);
            }
        }
    }

    public ParticleSetUpdaterTask(Entity entity,
                                  ParticleSet particleSet) {
        this.base = Mapper.base.get(entity);
        this.particleSet = particleSet;
        this.datasetDescription = Mapper.datasetDescription.get(entity);
        this.utils = new ParticleUtils();
        this.buffer = new TopNBuffer(this.particleSet.indices.length);
        this.stage = new ThreadLocal<>();
        this.stage.set(SORT1);
        this.executor = GaiaSky.instance.getExecutorService();

        if (particleSet.isStars) {
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
        if (pointData != null && !pointData.isEmpty() && pointData.getFirst()
                .names() != null) {
            switch (stage.get()) {
                case METADATA -> {
                    if (base.opacity > 0
                            && ((particleSet.lastSortCameraPos.dst2D(camera.getPos()) > CAM_DX_TH_SQ)
                            || (GaiaSky.instance.time.getWarpFactor() > 1.0e12))) {
                        executor.execute(this);
                    }
                }
                case SORT1, SORT2 -> {
                    executor.execute(this);
                }
            }
        }
    }

    @Override
    public void run() {
        var time = GaiaSky.instance.time;
        var camera = GaiaSky.instance.getICamera();
        switch (stage.get()) {
            case METADATA -> {
                // Compute metadata -- brightness proxy for every star.
                stage.set(BUSY);
                updateMetadataConsumer.accept(time, camera);
                stage.set(SORT1);
            }
            case SORT1 -> {
                // Get K-brightest star indices -- first half.
                stage.set(BUSY);

                var totalCount = particleSet.pointData.size();
                var metadata = particleSet.metadata;

                // Clear queue.
                this.buffer.clear();

                // Offer first half of particles.
                int n = totalCount / 2;
                for (int i = 0; i < n; i++) {
                    buffer.add(i, metadata[i]);
                }

                stage.set(SORT2);
            }
            case SORT2 -> {
                // Get K-brightest star indices -- second half.
                stage.set(BUSY);

                var totalCount = particleSet.pointData.size();
                var metadata = particleSet.metadata;

                int n = totalCount / 2 + 1;
                for (int i = n; i < totalCount; i++) {
                    buffer.add(i, metadata[i]);
                }
                // Sort it.
                buffer.sort();

                // Now move top indices to array.
                int[] topIndices = this.buffer.indexArray();
                var targetIndices = particleSet.indices;
                System.arraycopy(topIndices, 0, targetIndices, 0, topIndices.length);

                // Synchronously with the render thread, update indices, lastSortTime and updating state.
                GaiaSky.postRunnable(() -> {
                    particleSet.lastSortCameraPos.set(camera.getPos());
                    particleSet.lastSortTime = GaiaSky.instance.getT();
                    stage.set(METADATA);
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
        Vector3D camPos = camera.getPos()
                .tov3d(D34);
        int n = particleSet.pointData.size();
        for (int i = 0; i < n; i++) {
            IParticleRecord d = particleSet.pointData.get(i);
            Vector3D x = D31.set(d.x(), d.y(), d.z());
            particleSet.metadata[i] = utils.filter(i, particleSet, datasetDescription) ? camPos.dst2(x) : Double.MAX_VALUE;
        }
    }

    /**
     * Computes a proxy to the star brightness given its absolute magnitude and distance squared.
     * Uses a pre-computed table to avoid {@link FastMath#pow(double, double)} calls.
     *
     * @param absMag          The absolute magnitude.
     * @param distanceSquared The distance^2 to the star.
     *
     * @return A proxy to the brightness, useful for comparing stars.
     */
    public static double brightnessProxy(float absMag, double distanceSquared) {
        int index = (int) Math.floor((absMag - StarBrightness.MIN_MAG) / StarBrightness.STEP);
        if (index < 0 || index >= StarBrightness.BRIGHTNESS_TABLE.length) return 0f;
        return -StarBrightness.BRIGHTNESS_TABLE[index] / distanceSquared;
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
        Vector3D camPos = camera.getPos()
                .tov3d(D34);
        double deltaYears = AstroUtils.getMsSince(time.getTime(), particleSet.epochJd) * Nature.MS_TO_Y;
        if (particleSet.pointData != null) {
            int n = particleSet.pointData.size();
            for (int i = 0; i < n; i++) {
                IParticleRecord d = particleSet.pointData.get(i);

                // Pm
                Vector3D dx = D32.set(d.vx(), d.vy(), d.vz())
                        .scl(deltaYears);
                // Pos
                Vector3D pos = D31.set(d.x(), d.y(), d.z())
                        .add(dx);

                // Use magnitude.
                particleSet.metadata[i] = utils.filter(i, particleSet, datasetDescription) ?
                        brightnessProxy(d.absMag(), camPos.dst2(pos)) :
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
