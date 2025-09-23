/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.DatasetDescription;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.component.StarSet;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import net.jafama.FastMath;

import java.nio.file.Files;
import java.util.Locale;

public class ParticleSetUpdater extends AbstractUpdateSystem {

    private final ParticleUtils utils;

    public ParticleSetUpdater(Family family,
                              int priority) {
        super(family, priority);
        this.utils = new ParticleUtils();
    }

    @Override
    protected void processEntity(Entity entity,
                                 float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity,
                             float deltaTime) {
        var camera = GaiaSky.instance.cameraManager;
        var set = Mapper.particleSet.has(entity) ? Mapper.particleSet.get(entity) : Mapper.starSet.get(entity);
        if (set != null) {
            updateCommon(camera, set);
            if (set instanceof StarSet ss) {
                updateStarSet(camera, ss, Mapper.datasetDescription.get(entity));
            } else {
                updateParticleSet(camera, set);
            }
        }
    }

    private void updateCommon(ICamera camera, ParticleSet set) {
        // Update proximity loading.
        if (set.proximityLoadingFlag) {
            int idxNearest = set.indices[0];
            IParticleRecord bean;
            if (idxNearest >= 0 && (bean = set.pointData.get(idxNearest)) != null) {
                var sa = set.getSolidAngleApparent(idxNearest);
                var beanSelected = camera.getMode()
                        .isFocus()
                        && camera.getFocus()
                        .isValid()
                        && ((FocusView) camera.getFocus()).getSet() == set
                        && camera.getFocus()
                        .hasName(bean.names()[0]);
                if (!set.proximityLoaded.contains(idxNearest)) {
                    // About 4 degrees.
                    if (sa > set.proximityThreshold) {
                        // Load descriptor file, if it exists.
                        // Check all names.
                        var found = false;
                        for (var name : bean.names()) {
                            var path = set.proximityDescriptorsPath.resolve(name + ".json");
                            if (Files.exists(path)) {
                                // Remove current bean from index.
                                for (var key : bean.names()) {
                                    var k = key.toLowerCase(Locale.ROOT)
                                            .trim();
                                    GaiaSky.instance.scene.index()
                                            .remove(k);
                                }
                                // Load descriptor.
                                // Only re-focus (select) if our current focus is the object in question.
                                GaiaSky.postRunnable(() -> GaiaSky.instance.scripting()
                                        .loadJsonDataset(name, path.toString(), beanSelected, true));
                                set.proximityLoaded.add(idxNearest);
                                found = true;
                            }
                        }
                        if (!found) {
                            set.proximityLoaded.add(idxNearest);
                            set.proximityMissing.add(idxNearest);
                        }
                    }
                } else if (!set.proximityMissing.contains(idxNearest) && beanSelected) {
                    // Already loaded. Do focus transition.
                    GaiaSky.instance.scripting()
                            .setCameraFocus(bean.names()[0]);
                }
            }
        }
    }

    private void updateParticleSet(ICamera camera,
                                   ParticleSet particleSet) {
        // Delta years
        particleSet.currDeltaYears = AstroUtils.getMsSince(GaiaSky.instance.time.getTime(), particleSet.epochJd) * Nature.MS_TO_Y;

        if (particleSet.pointData != null) {
            particleSet.cPosD.set(camera.getPos());

            if (particleSet.focusIndex >= 0) {
                particleSet.updateFocus(camera);
            }

            // Touch task.
            if (particleSet.updaterTask != null) {
                particleSet.updaterTask.update(camera);
            }

        }
    }

    private void updateStarSet(ICamera camera,
                               StarSet set,
                               DatasetDescription datasetDesc) {
        // Fade node visibility
        if (set.indices != null && set.indices.length > 0 && set.pointData != null) {
            updateParticleSet(camera, set);

            // Update close stars in camera proximity.
            int j = 0;
            for (int i = 0; i < FastMath.min(set.proximity.updating.length, set.indices.length); i++) {
                if (set.indices[i] >= 0
                        && utils.filter(set.indices[i], set, datasetDesc)
                        && set.isVisible(set.indices[i])) {
                    IParticleRecord closeStar = set.pointData.get(set.indices[i]);
                    set.proximity.set(j, set.indices[i], closeStar, camera, set.currDeltaYears);
                    camera.checkClosestParticle(set.proximity.updating[j]);

                    // Model distance
                    if (j == 0) {
                        set.modelDist = 172.4643429 * closeStar.radius();
                    }
                    j++;
                }
            }
        }
    }
}
