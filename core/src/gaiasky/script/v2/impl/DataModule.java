/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.GaiaSky;
import gaiasky.data.SceneJsonLoader;
import gaiasky.data.StarClusterLoader;
import gaiasky.data.group.DatasetOptions;
import gaiasky.data.group.STILDataProvider;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.window.ColormapPicker;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.entity.SetUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.script.v2.api.DataAPI;
import gaiasky.util.CatalogInfo;
import gaiasky.util.Constants;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.filter.attrib.AttributeUCD;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.i18n.I18n;
import gaiasky.util.ucd.UCD;
import net.jafama.FastMath;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The data module provides calls and methods to handle datasets and catalogs.
 */
public class DataModule extends APIModule implements IObserver, DataAPI {
    /** Reference to scene. **/
    private Scene scene;
    /** Focus view. **/
    private final FocusView focusView;

    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public DataModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
        this.focusView = new FocusView();

        em.subscribe(this, Event.SCENE_LOADED);
    }

    @Override
    public String get_datasets_directory() {
        return Settings.settings.data.location;
    }

    @Override
    public boolean load_dataset(String dsName, String absolutePath) {
        return load_dataset(dsName, absolutePath, CatalogInfo.CatalogInfoSource.SCRIPT, true);
    }

    @Override
    public boolean load_dataset(String dsName, String path, boolean sync) {
        return load_dataset(dsName, path, CatalogInfo.CatalogInfoSource.SCRIPT, sync);
    }

    public boolean load_dataset(String dsName, String path, CatalogInfo.CatalogInfoSource type, boolean sync) {
        if (sync) {
            return loadDatasetImmediate(dsName, path, type, true);
        } else {
            Thread t = new Thread(() -> loadDatasetImmediate(dsName, path, type, false));
            t.start();
            return true;
        }
    }

    public boolean load_dataset(String dsName, String path, CatalogInfo.CatalogInfoSource type, DatasetOptions datasetOptions, boolean sync) {
        if (sync) {
            return loadDatasetImmediate(dsName, path, type, datasetOptions, true);
        } else {
            Thread t = new Thread(() -> loadDatasetImmediate(dsName, path, type, datasetOptions, false));
            t.start();
            return true;
        }
    }

    public boolean load_dataset(String dsName, DataSource ds, CatalogInfo.CatalogInfoSource type, DatasetOptions datasetOptions, boolean sync) {
        if (sync) {
            return loadDatasetImmediate(dsName, ds, type, datasetOptions, true);
        } else {
            Thread t = new Thread(() -> loadDatasetImmediate(dsName, ds, type, datasetOptions, false));
            t.start();
            return true;
        }
    }

    @Override
    public boolean load_star_dataset(String dsName, String path, boolean sync) {
        return load_star_dataset(dsName, path, CatalogInfo.CatalogInfoSource.SCRIPT, 1, new double[]{0, 0, 0, 0}, null, null, sync);
    }

    @Override
    public boolean load_star_dataset(String dsName, String path, double magnitudeScale, boolean sync) {
        return load_star_dataset(dsName, path, CatalogInfo.CatalogInfoSource.SCRIPT, magnitudeScale, new double[]{0, 0, 0, 0}, null, null, sync);
    }

    @Override
    public boolean load_star_dataset(String dsName, String path, double magnitudeScale, double[] labelColor, boolean sync) {
        return load_star_dataset(dsName, path, CatalogInfo.CatalogInfoSource.SCRIPT, magnitudeScale, labelColor, null, null, sync);
    }

    public boolean load_star_dataset(String dsName, String path, double magnitudeScale, final List<?> labelColor, boolean sync) {
        return load_star_dataset(dsName, path, magnitudeScale, api.dArray(labelColor), sync);
    }

    @Override
    public boolean load_star_dataset(String dsName,
                                     String path,
                                     double magnitudeScale,
                                     double[] labelColor,
                                     double[] fadeIn,
                                     double[] fadeOut,
                                     boolean sync) {
        return load_star_dataset(dsName, path, CatalogInfo.CatalogInfoSource.SCRIPT, magnitudeScale, labelColor, fadeIn, fadeOut, sync);
    }

    public boolean load_star_dataset(String dsName,
                                     String path,
                                     double magnitudeScale,
                                     final List<?> labelColor,
                                     final List<?> fadeIn,
                                     final List<?> fadeOut,
                                     boolean sync) {
        return load_star_dataset(dsName, path, magnitudeScale, api.dArray(labelColor), api.dArray(fadeIn), api.dArray(fadeOut), sync);
    }

    public boolean load_star_dataset(String dsName,
                                     String path,
                                     CatalogInfo.CatalogInfoSource type,
                                     double magnitudeScale,
                                     double[] labelColor,
                                     double[] fadeIn,
                                     double[] fadeOut,
                                     boolean sync) {
        DatasetOptions datasetOptions = DatasetOptions.getStarDatasetOptions(dsName, magnitudeScale, labelColor, fadeIn, fadeOut);
        return load_dataset(dsName, path, type, datasetOptions, sync);
    }

    @Override
    public boolean load_particle_dataset(String dsName,
                                         String path,
                                         double profileDecay,
                                         double[] particleColor,
                                         double colorNoise,
                                         double[] labelColor,
                                         double particleSize,
                                         String ct,
                                         boolean sync) {
        return load_particle_dataset(dsName,
                                     path,
                                     profileDecay,
                                     particleColor,
                                     colorNoise,
                                     labelColor,
                                     particleSize,
                                     new double[]{1.5d, 100d},
                                     ct,
                                     null,
                                     null,
                                     sync);
    }

    public boolean load_particle_dataset(String dsName,
                                         String path,
                                         double profileDecay,
                                         List<?> particleColor,
                                         double colorNoise,
                                         List<?> labelColor,
                                         double particleSize,
                                         String ct,
                                         boolean sync) {
        return load_particle_dataset(dsName,
                                     path,
                                     profileDecay,
                                     api.dArray(particleColor),
                                     colorNoise,
                                     api.dArray(labelColor),
                                     particleSize,
                                     ct,
                                     null,
                                     null,
                                     sync);
    }

    @Override
    public boolean load_particle_dataset(String dsName,
                                         String path,
                                         double profileDecay,
                                         double[] particleColor,
                                         double colorNoise,
                                         double[] labelColor,
                                         double particleSize,
                                         String ct,
                                         double[] fadeIn,
                                         double[] fadeOut,
                                         boolean sync) {
        return load_particle_dataset(dsName,
                                     path,
                                     profileDecay,
                                     particleColor,
                                     colorNoise,
                                     labelColor,
                                     particleSize,
                                     new double[]{Math.tan(Math.toRadians(0.1)), FastMath.tan(Math.toRadians(6.0))},
                                     ct,
                                     fadeIn,
                                     fadeOut,
                                     sync);
    }

    public boolean load_particle_dataset(String dsName,
                                         String path,
                                         double profileDecay,
                                         final List<?> particleColor,
                                         double colorNoise,
                                         final List<?> labelColor,
                                         double particleSize,
                                         String ct,
                                         final List<?> fadeIn,
                                         final List<?> fadeOut,
                                         boolean sync) {
        return load_particle_dataset(dsName,
                                     path,
                                     profileDecay,
                                     api.dArray(particleColor),
                                     colorNoise,
                                     api.dArray(labelColor),
                                     particleSize,
                                     ct,
                                     api.dArray(fadeIn),
                                     api.dArray(fadeOut),
                                     sync);
    }

    @Override
    public boolean load_particle_dataset(String dsName,
                                         String path,
                                         double profileDecay,
                                         double[] particleColor,
                                         double colorNoise,
                                         double[] labelColor,
                                         double particleSize,
                                         double[] sizeLimits,
                                         String ct,
                                         double[] fadeIn,
                                         double[] fadeOut,
                                         boolean sync) {
        ComponentTypes.ComponentType compType = ComponentTypes.ComponentType.valueOf(ct);
        return load_particle_dataset(dsName,
                                     path,
                                     profileDecay,
                                     particleColor,
                                     colorNoise,
                                     labelColor,
                                     particleSize,
                                     sizeLimits,
                                     compType,
                                     fadeIn,
                                     fadeOut,
                                     sync);
    }

    public boolean load_particle_dataset(String dsName,
                                         String path,
                                         double profileDecay,
                                         final List<?> particleColor,
                                         double colorNoise,
                                         final List<?> labelColor,
                                         double particleSize,
                                         List<?> sizeLimits,
                                         String ct,
                                         final List<?> fadeIn,
                                         final List<?> fadeOut,
                                         boolean sync) {
        return load_particle_dataset(dsName,
                                     path,
                                     profileDecay,
                                     api.dArray(particleColor),
                                     colorNoise,
                                     api.dArray(labelColor),
                                     particleSize,
                                     api.dArray(sizeLimits),
                                     ct,
                                     api.dArray(fadeIn),
                                     api.dArray(fadeOut),
                                     sync);
    }

    public boolean load_particle_dataset(String dsName,
                                         String path,
                                         double profileDecay,
                                         double[] particleColor,
                                         double colorNoise,
                                         double[] labelColor,
                                         double particleSize,
                                         double[] sizeLimits,
                                         ComponentTypes.ComponentType ct,
                                         double[] fadeIn,
                                         double[] fadeOut,
                                         boolean sync) {
        return load_particle_dataset(dsName,
                                     path,
                                     CatalogInfo.CatalogInfoSource.SCRIPT,
                                     profileDecay,
                                     particleColor,
                                     colorNoise,
                                     labelColor,
                                     particleSize,
                                     sizeLimits,
                                     ct,
                                     fadeIn,
                                     fadeOut,
                                     sync);
    }

    public boolean load_particle_dataset(String dsName,
                                         String path,
                                         CatalogInfo.CatalogInfoSource type,
                                         double profileDecay,
                                         double[] particleColor,
                                         double colorNoise,
                                         double[] labelColor,
                                         double particleSize,
                                         double[] sizeLimits,
                                         ComponentTypes.ComponentType ct,
                                         double[] fadeIn,
                                         double[] fadeOut,
                                         boolean sync) {
        DatasetOptions datasetOptions = DatasetOptions.getParticleDatasetOptions(dsName,
                                                                       profileDecay,
                                                                       particleColor,
                                                                       colorNoise,
                                                                       labelColor,
                                                                       particleSize,
                                                                       sizeLimits,
                                                                       ct,
                                                                       fadeIn,
                                                                       fadeOut);
        return load_dataset(dsName, path, type, datasetOptions, sync);
    }

    @Override
    public boolean load_star_cluster_dataset(String dsName, String path, double[] particleColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        return load_star_cluster_dataset(dsName, path, particleColor, ComponentTypes.ComponentType.Clusters.toString(), fadeIn, fadeOut, sync);
    }

    public boolean load_star_cluster_dataset(String dsName, String path, List<?> particleColor, List<?> fadeIn, List<?> fadeOut, boolean sync) {
        return load_star_cluster_dataset(dsName, path, api.dArray(particleColor), api.dArray(fadeIn), api.dArray(fadeOut), sync);
    }

    @Override
    public boolean load_star_cluster_dataset(String dsName,
                                             String path,
                                             double[] particleColor,
                                             double[] labelColor,
                                             double[] fadeIn,
                                             double[] fadeOut,
                                             boolean sync) {
        return load_star_cluster_dataset(dsName,
                                         path,
                                         particleColor,
                                         labelColor,
                                         ComponentTypes.ComponentType.Clusters.toString(),
                                         fadeIn,
                                         fadeOut,
                                         sync);
    }

    public boolean load_star_cluster_dataset(String dsName,
                                             String path,
                                             List<?> particleColor,
                                             List<?> labelColor,
                                             List<?> fadeIn,
                                             List<?> fadeOut,
                                             boolean sync) {
        return load_star_cluster_dataset(dsName, path, api.dArray(particleColor), api.dArray(labelColor), api.dArray(fadeIn), api.dArray(fadeOut), sync);
    }

    @Override
    public boolean load_star_cluster_dataset(String dsName,
                                             String path,
                                             double[] particleColor,
                                             String ct,
                                             double[] fadeIn,
                                             double[] fadeOut,
                                             boolean sync) {
        ComponentTypes.ComponentType compType = ComponentTypes.ComponentType.valueOf(ct);
        DatasetOptions datasetOptions = DatasetOptions.getStarClusterDatasetOptions(dsName, particleColor, particleColor.clone(), compType, fadeIn, fadeOut);
        return load_dataset(dsName, path, CatalogInfo.CatalogInfoSource.SCRIPT, datasetOptions, sync);
    }

    public boolean load_star_cluster_dataset(String dsName,
                                             String path,
                                             List<?> particleColor,
                                             String ct,
                                             List<?> fadeIn,
                                             List<?> fadeOut,
                                             boolean sync) {
        return load_star_cluster_dataset(dsName, path, api.dArray(particleColor), ct, api.dArray(fadeIn), api.dArray(fadeOut), sync);
    }

    @Override
    public boolean load_star_cluster_dataset(String dsName,
                                             String path,
                                             double[] particleColor,
                                             double[] labelColor,
                                             String ct,
                                             double[] fadeIn,
                                             double[] fadeOut,
                                             boolean sync) {
        ComponentTypes.ComponentType compType = ComponentTypes.ComponentType.valueOf(ct);
        DatasetOptions datasetOptions = DatasetOptions.getStarClusterDatasetOptions(dsName, particleColor, labelColor, compType, fadeIn, fadeOut);
        return load_dataset(dsName, path, CatalogInfo.CatalogInfoSource.SCRIPT, datasetOptions, sync);
    }

    @Override
    public boolean load_variable_star_dataset(String dsName,
                                              String path,
                                              double magnitudeScale,
                                              double[] labelColor,
                                              double[] fadeIn,
                                              double[] fadeOut,
                                              boolean sync) {
        return load_variable_star_dataset(dsName, path, CatalogInfo.CatalogInfoSource.SCRIPT, magnitudeScale, labelColor, fadeIn, fadeOut, sync);
    }

    public boolean load_variable_star_dataset(String dsName,
                                              String path,
                                              CatalogInfo.CatalogInfoSource type,
                                              double magnitudeScale,
                                              double[] labelColor,
                                              double[] fadeIn,
                                              double[] fadeOut,
                                              boolean sync) {
        DatasetOptions datasetOptions = DatasetOptions.getVariableStarDatasetOptions(dsName,
                                                                           magnitudeScale,
                                                                           labelColor,
                                                                           ComponentTypes.ComponentType.Stars,
                                                                           fadeIn,
                                                                           fadeOut);
        return load_dataset(dsName, path, type, datasetOptions, sync);
    }

    public boolean load_star_cluster_dataset(String dsName,
                                             String path,
                                             List<?> particleColor,
                                             List<?> labelColor,
                                             String ct,
                                             List<?> fadeIn,
                                             List<?> fadeOut,
                                             boolean sync) {
        return load_star_cluster_dataset(dsName,
                                         path,
                                         api.dArray(particleColor),
                                         api.dArray(labelColor),
                                         ct,
                                         api.dArray(fadeIn),
                                         api.dArray(fadeOut),
                                         sync);
    }

    private boolean loadDatasetImmediate(String dsName, String path, CatalogInfo.CatalogInfoSource type, boolean sync) {
        return loadDatasetImmediate(dsName, path, type, null, sync);
    }

    private boolean loadDatasetImmediate(String dsName,
                                         String path,
                                         CatalogInfo.CatalogInfoSource type,
                                         DatasetOptions datasetOptions,
                                         boolean sync) {
        Path p = Paths.get(path);
        if (Files.exists(p) && Files.isReadable(p)) {
            try {
                return loadDatasetImmediate(dsName, new FileDataSource(p.toFile()), type, datasetOptions, sync);
            } catch (Exception e) {
                logger.error("Error loading file: " + p, e);
            }
        } else {
            logger.error("Can't read file: " + path);
        }
        return false;
    }

    private List<IParticleRecord> loadParticleBeans(DataSource ds, DatasetOptions datasetOptions, STILDataProvider provider) {
        provider.setDatasetOptions(datasetOptions);
        String catalogName = datasetOptions != null && datasetOptions.catalogName != null ? datasetOptions.catalogName : ds.getName();
        return provider.loadData(ds, 1.0f, () -> {
            // Create
            EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, catalogName, 0.01f);
        }, (current, count) -> {
            EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, catalogName, (float) current / (float) count);
            if (current % 250000 == 0) {
                logger.info(current + " objects loaded...");
            }
        }, () -> {
            // Force remove
            EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, catalogName, 2f);
        });
    }

    @Override
    public boolean load_json_dataset(String dsName, String path) {
        return load_json_dataset(dsName, path, true);
    }

    public boolean load_json_dataset(String dsName, String pathString, boolean sync) {
        return load_json_dataset(dsName, pathString, false, sync);
    }

    public boolean load_json_dataset(String dsName, String pathString, boolean select, boolean sync) {
        // Load internal JSON dataset file.
        try {
            logger.info(I18n.msg("notif.catalog.loading", pathString));
            final Array<Entity> objects = SceneJsonLoader.loadJsonFile(Gdx.files.absolute(pathString), scene);
            int i = 0;
            for (Entity e : objects) {
                if (e == null) {
                    logger.error("Entity is null: " + i);
                }
                i++;
            }
            logger.info(I18n.msg("notif.catalog.loaded", objects.size, I18n.msg("gui.objects")));
            if (objects.size > 0) {
                api.base.post_runnable(() -> {
                    objects.forEach(scene.engine::addEntity);
                    objects.forEach(scene::initializeEntity);
                    objects.forEach(scene::addToIndex);

                    // Wait for entity in new task.
                    GaiaSky.instance.getExecutorService().execute(() -> {
                        while (!GaiaSky.instance.assetManager.isFinished()) {
                            // Active wait
                            api.base.sleep_frames(1);
                        }

                        // Finish initialization and touch scene graph.
                        api.base.post_runnable(() -> {
                            objects.forEach((entity) -> EventManager.publish(Event.SCENE_ADD_OBJECT_NO_POST_CMD, this, entity, false));
                            objects.forEach(scene::setUpEntity);
                            GaiaSky.instance.touchSceneGraph();

                            if (select) {
                                focusView.setEntity(objects.get(0));
                                api.camera.focus_mode(focusView.getName());
                            }
                        });
                    });
                });
            }

        } catch (Exception e) {
            notifyErrorPopup(I18n.msg("error.loading.format", pathString), e);
            return false;
        }
        return true;
    }

    private boolean loadDatasetImmediate(String dsName,
                                         DataSource ds,
                                         CatalogInfo.CatalogInfoSource type,
                                         DatasetOptions datasetOptions,
                                         boolean sync) {
        try {
            logger.info(I18n.msg("notif.catalog.loading", dsName));

            // Create star/particle group or star clusters
            if (api.validator.checkString(dsName, "datasetName") && !api.catalogManager.contains(dsName)) {
                // Only local files checked.
                Path path = null;
                if (ds instanceof FileDataSource) {
                    var file = ((FileDataSource) ds).getFile();
                    path = file.toPath();
                    String pathString = file.getAbsolutePath();
                    if (!Files.exists(file.toPath())) {
                        notifyErrorPopup(I18n.msg("error.loading.notexistent", pathString));
                        return false;
                    }
                    if (!Files.isReadable(path)) {
                        notifyErrorPopup(I18n.msg("error.file.read", pathString));
                        return false;
                    }
                    if (Files.isDirectory(path)) {
                        notifyErrorPopup(I18n.msg("error.file.isdir", pathString));
                        return false;
                    }
                }

                if (path != null && path.getFileName().toString().endsWith(".json")) {
                    // Only local files allowed for JSON.
                    load_json_dataset(dsName, path.toString(), sync);
                } else if (datasetOptions == null || datasetOptions.type == DatasetOptions.DatasetLoadType.STARS || datasetOptions.type == DatasetOptions.DatasetLoadType.VARIABLES) {
                    var provider = new STILDataProvider();
                    List<IParticleRecord> data = loadParticleBeans(ds, datasetOptions, provider);
                    if (data != null && !data.isEmpty()) {
                        // STAR GROUP
                        AtomicReference<Entity> starGroup = new AtomicReference<>();
                        api.base.post_runnable(() -> {
                            if (datasetOptions != null) datasetOptions.initializeCatalogInfo = false;
                            starGroup.set(SetUtils.createStarSet(scene,
                                                                 dsName,
                                                                 ds.getName(),
                                                                 data,
                                                                 provider.getColumnInfoList(),
                                                                 datasetOptions,
                                                                 false));

                            // Catalog info.
                            new CatalogInfo(dsName, ds.getName(), null, type, 1.5f, starGroup.get());
                            // Add to scene.
                            EventManager.publish(Event.SCENE_ADD_OBJECT_CMD, this, starGroup.get(), true);
                            // Add to catalog manager -> setUp.
                            scene.initializeEntity(starGroup.get());
                            scene.setUpEntity(starGroup.get());

                            String typeStr = datasetOptions == null || datasetOptions.type == DatasetOptions.DatasetLoadType.STARS ? I18n.msg(
                                    "gui.dsload.stars.name") : I18n.msg(
                                    "gui.dsload.variablestars.name");
                            logger.info(I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                                 this,
                                                 dsName + ": " + I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                        });
                        // Sync waiting until the node is in the scene graph
                        while (sync && (starGroup.get() == null || Mapper.graph.get(starGroup.get()).parent != null)) {
                            api.base.sleep_frames(1);
                        }
                    }
                } else if (datasetOptions.type == DatasetOptions.DatasetLoadType.PARTICLES) {
                    // PARTICLE GROUP
                    var provider = new STILDataProvider();
                    List<IParticleRecord> data = loadParticleBeans(ds, datasetOptions, provider);
                    if (data != null && !data.isEmpty()) {
                        AtomicReference<Entity> particleGroup = new AtomicReference<>();
                        api.base.post_runnable(() -> {
                            datasetOptions.initializeCatalogInfo = false;
                            particleGroup.set(SetUtils.createParticleSet(scene,
                                                                         dsName,
                                                                         ds.getName(),
                                                                         data,
                                                                         provider.getColumnInfoList(),
                                                                         datasetOptions,
                                                                         false));

                            // Catalog info
                            CatalogInfo ci = new CatalogInfo(dsName, ds.getName(), ds.getURL().toString(), type, 1.5f, particleGroup.get());
                            // Add to scene.
                            EventManager.publish(Event.SCENE_ADD_OBJECT_CMD, this, ci.entity, true);
                            // Add to catalog manager -> setUp
                            scene.setUpEntity(particleGroup.get());

                            String typeStr = I18n.msg("gui.dsload.objects.name");
                            logger.info(I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                                 this,
                                                 dsName + ": " + I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                        });
                        // Sync waiting until the node is in the scene graph
                        while (sync && (particleGroup.get() == null || Mapper.graph.get(particleGroup.get()).parent != null)) {
                            api.base.sleep_frames(1);
                        }
                    }
                } else if (datasetOptions.type == DatasetOptions.DatasetLoadType.PARTICLES_EXT) {
                    // PARTICLE GROUP EXTENDED
                    var provider = new STILDataProvider();
                    List<IParticleRecord> data = loadParticleBeans(ds, datasetOptions, provider);
                    if (data != null && !data.isEmpty()) {
                        AtomicReference<Entity> particleGroup = new AtomicReference<>();
                        api.base.post_runnable(() -> {
                            datasetOptions.initializeCatalogInfo = false;
                            particleGroup.set(SetUtils.createParticleSet(scene,
                                                                         dsName,
                                                                         ds.getName(),
                                                                         data,
                                                                         provider.getColumnInfoList(),
                                                                         datasetOptions,
                                                                         false));

                            // Catalog info
                            CatalogInfo ci = new CatalogInfo(dsName, ds.getName(), ds.getURL().toString(), type, 1.5f, particleGroup.get());
                            // Add to scene.
                            EventManager.publish(Event.SCENE_ADD_OBJECT_CMD, this, ci.entity, true);
                            // Add to catalog manager -> setUp
                            scene.setUpEntity(particleGroup.get());

                            String typeStr = I18n.msg("gui.dsload.objects.name");
                            logger.info(I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                                 this,
                                                 dsName + ": " + I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                        });
                        // Sync waiting until the node is in the scene graph
                        while (sync && (particleGroup.get() == null || Mapper.graph.get(particleGroup.get()).parent != null)) {
                            api.base.sleep_frames(1);
                        }
                    }
                } else if (datasetOptions.type == DatasetOptions.DatasetLoadType.CLUSTERS) {
                    // STAR CLUSTERS
                    var archetype = scene.archetypes().get("GenericCatalog");
                    var entity = archetype.createEntity();

                    var base = Mapper.base.get(entity);
                    base.setName(dsName);
                    base.setCt(datasetOptions.ct.toString());

                    var body = Mapper.body.get(entity);
                    body.setColor(datasetOptions.particleColor);
                    body.setLabelColor(datasetOptions.labelColor);
                    body.setPosition(new double[]{0, 0, 0});

                    var fade = Mapper.fade.get(entity);
                    fade.setFadeIn(datasetOptions.fadeIn);
                    fade.setFadeOut(datasetOptions.fadeOut);

                    var graph = Mapper.graph.get(entity);
                    graph.setParent(Scene.ROOT_NAME);
                    AtomicInteger numLoaded = new AtomicInteger(-5);

                    api.base.post_runnable(() -> {
                        // Load data
                        StarClusterLoader scl = new StarClusterLoader();
                        scl.initialize(ds, scene);
                        scl.setParentName(dsName);
                        scl.loadData();
                        Array<Entity> clusters = scl.getClusters();
                        numLoaded.set(clusters.size);

                        if (!clusters.isEmpty()) {
                            // Initialize.
                            scene.initializeEntity(entity);
                            for (Entity cluster : clusters) {
                                scene.initializeEntity(cluster);
                                var cBody = Mapper.body.get(cluster);
                                cBody.setColor(datasetOptions.particleColor);
                                cBody.setLabelColor(datasetOptions.labelColor);
                            }

                            // Insert
                            scene.insert(entity, true);
                            for (Entity cluster : clusters) {
                                scene.insert(cluster, true);
                            }

                            // Finalize
                            scene.setUpEntity(entity);
                            for (Entity cluster : clusters) {
                                scene.setUpEntity(cluster);
                            }

                            String typeStr = I18n.msg("gui.dsload.clusters.name");
                            logger.info(I18n.msg("notif.catalog.loaded", graph.children.size, typeStr));
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                                 this,
                                                 dsName + ": " + I18n.msg("notif.catalog.loaded", graph.children.size, typeStr));
                        }
                    });
                    // Sync waiting until the node is in the scene graph
                    while (sync && graph.parent == null) {
                        int loaded = numLoaded.get();
                        if (loaded == 0) {
                            // Stop waiting, no objects loaded.
                            break;
                        }
                        api.base.sleep_frames(1);
                    }
                }
                // One extra flush frame
                api.base.sleep_frames(1);
                return true;
            } else {
                logger.error(dsName + ": invalid or already existing dataset name");
                return false;
            }
        } catch (Exception e) {
            logger.error(e);
            return false;
        }

    }

    @Override
    public boolean dataset_exists(String dsName) {
        return api.validator.checkString(dsName, "datasetName") && api.validator.checkDatasetName(dsName);
    }

    @Override
    public boolean set_dataset_transform_matrix(String dsName, double[] matrix) {
        if (api.validator.checkString(dsName, "datasetName") && api.validator.checkDatasetName(dsName) && api.validator.checkNotNull(matrix,
                                                                                                                                     "matrix")) {
            var ci = api.catalogManager.get(dsName);
            if (ci != null && ci.entity != null) {
                var affine = Mapper.affine.get(ci.entity);
                if (affine != null) {
                    affine.clear();
                    affine.setMatrix(matrix);
                }
                return true;
            }
        }
        return false;
    }

    public boolean clear_dataset_transform_matrix(String dsName) {
        if (api.validator.checkString(dsName, "datasetName") && api.validator.checkDatasetName(dsName)) {
            var ci = api.catalogManager.get(dsName);
            if (ci != null && ci.entity != null) {
                var affine = Mapper.affine.get(ci.entity);
                if (affine != null) {
                    affine.clear();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean remove_dataset(String dsName) {
        if (api.validator.checkString(dsName, "datasetName") && api.validator.checkDatasetName(dsName)) {
            api.base.post_runnable(() -> EventManager.publish(Event.CATALOG_REMOVE, this, dsName));
            return true;
        }
        return false;
    }

    @Override
    public boolean hide_dataset(String dsName) {
        if (api.validator.checkString(dsName, "datasetName") && api.validator.checkDatasetName(dsName)) {
            api.base.post_runnable(() -> EventManager.publish(Event.CATALOG_VISIBLE, this, dsName, false));
            return true;
        }
        return false;
    }

    @Override
    public boolean show_dataset(String dsName) {
        if (api.validator.checkString(dsName, "datasetName") && api.validator.checkDatasetName(dsName)) {
            api.base.post_runnable(() -> EventManager.publish(Event.CATALOG_VISIBLE, this, dsName, true));
            return true;
        }
        return false;
    }

    @Override
    public boolean highlight_dataset(String dsName, boolean highlight) {
        if (api.validator.checkString(dsName, "datasetName") && api.validator.checkDatasetName(dsName)) {
            CatalogInfo ci = api.catalogManager.get(dsName);
            api.base.post_runnable(() -> EventManager.publish(Event.CATALOG_HIGHLIGHT, this, ci, highlight));
            return true;
        }
        return false;
    }

    @Override
    public boolean highlight_dataset(String dsName, int colorIndex, boolean highlight) {
        float[] color = ColorUtils.getColorFromIndex(colorIndex);
        return highlight_dataset(dsName, color[0], color[1], color[2], color[3], highlight);
    }

    @Override
    public boolean highlight_dataset(String dsName, float r, float g, float b, float a, boolean highlight) {
        if (api.validator.checkString(dsName, "datasetName") && api.validator.checkDatasetName(dsName)) {
            CatalogInfo ci = api.catalogManager.get(dsName);
            ci.plainColor = true;
            ci.hlColor[0] = r;
            ci.hlColor[1] = g;
            ci.hlColor[2] = b;
            ci.hlColor[3] = a;
            api.base.post_runnable(() -> EventManager.publish(Event.CATALOG_HIGHLIGHT, this, ci, highlight));
            return true;
        }
        return false;
    }

    @Override
    public boolean highlight_dataset(String dsName, String attributeName, String colorMap, double minMap, double maxMap, boolean highlight) {
        if (api.validator.checkString(dsName, "datasetName") && api.validator.checkDatasetName(dsName)) {
            CatalogInfo ci = api.catalogManager.get(dsName);
            IAttribute attribute = getAttributeByName(attributeName, ci);
            int cmapIndex = getCmapIndexByName(colorMap);
            if (attribute != null && cmapIndex >= 0) {
                ci.plainColor = false;
                ci.hlCmapIndex = cmapIndex;
                ci.hlCmapMin = minMap;
                ci.hlCmapMax = maxMap;
                ci.hlCmapAttribute = attribute;
                api.base.post_runnable(() -> EventManager.publish(Event.CATALOG_HIGHLIGHT, this, ci, highlight));
            } else {
                if (attribute == null) logger.error("Could not find attribute with name '" + attributeName + "'");
                if (cmapIndex < 0) logger.error("Could not find color map with name '" + colorMap + "'");
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean set_dataset_highlight_size_factor(String dsName, float sizeFactor) {
        if (api.validator.checkString(dsName, "datasetName")
                && api.validator.checkNum(sizeFactor,
                                          Constants.MIN_DATASET_SIZE_FACTOR,
                                          Constants.MAX_DATASET_SIZE_FACTOR,
                                          "sizeFactor")) {

            boolean exists = api.catalogManager.contains(dsName);
            if (exists) {
                CatalogInfo ci = api.catalogManager.get(dsName);
                ci.setHlSizeFactor(sizeFactor);
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    @Override
    public boolean set_dataset_highlight_all_visible(String dsName, boolean allVisible) {
        if (api.validator.checkString(dsName, "datasetName")) {

            boolean exists = api.catalogManager.contains(dsName);
            if (exists) {
                CatalogInfo ci = api.catalogManager.get(dsName);
                ci.setHlAllVisible(allVisible);
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    @Override
    public void set_dataset_point_size_factor(String dsName, double multiplier) {
        if (api.validator.checkString(dsName, "datasetName")) {
            boolean exists = api.catalogManager.contains(dsName);
            if (exists) {
                em.post(Event.CATALOG_POINT_SIZE_SCALING_CMD, this, dsName, multiplier);
            } else {
                logger.warn("Catalog does not exist: " + dsName);
            }
        }
    }

    private void notifyErrorPopup(String message) {
        notifyErrorPopup(message, null);
    }

    private void notifyErrorPopup(String message, Exception e) {
        if (e != null) {
            logger.error(message, e);
        } else {
            logger.error(message);
        }
        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, message);
    }

    @Override
    public List<String> list_datasets() {
        Set<String> names = api.catalogManager.getDatasetNames();
        if (names != null) return new ArrayList<>(names);
        else return new ArrayList<>();
    }

    private int getCmapIndexByName(String name) {
        for (Pair<String, Integer> cmap : ColormapPicker.cmapList) {
            if (name.equalsIgnoreCase(cmap.getFirst())) return cmap.getSecond();
        }
        return -1;
    }

    private IAttribute getAttributeByName(String name, CatalogInfo ci) {
        try {
            // One of the default attributes
            Class<?> clazz = Class.forName("gaiasky.util.filter.attrib.Attribute" + name);
            Constructor<?> constructor = clazz.getConstructor();
            return (IAttribute) constructor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // Try extra attributes

            // New
            {
                if (ci.entity != null) {
                    var entity = ci.entity;
                    synchronized (focusView) {
                        focusView.setEntity(entity);
                        if (focusView.isSet()) {
                            ObjectMap.Keys<UCD> ucds = focusView.getSet().data().getFirst().extraKeys();
                            for (UCD ucd : ucds)
                                if (ucd.colName.equalsIgnoreCase(name)) return new AttributeUCD(ucd);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (Objects.requireNonNull(event) == Event.SCENE_LOADED) {
            this.scene = (Scene) data[0];
            this.focusView.setScene(this.scene);
        }
    }
}
