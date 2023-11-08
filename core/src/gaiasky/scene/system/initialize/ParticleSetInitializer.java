/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.TextureArray;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.data.AssetBean;
import gaiasky.data.api.IParticleGroupDataProvider;
import gaiasky.data.api.IStarGroupDataProvider;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.FocusHit;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.record.ParticleRecord.ParticleRecordType;
import gaiasky.scene.record.VariableRecord;
import gaiasky.scene.system.render.draw.billboard.BillboardEntityRenderSystem;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.task.ParticleSetUpdaterTask;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.camera.Proximity;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.TextureArrayLoader.TextureArrayParameter;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ParticleSetInitializer extends AbstractInitSystem {
    private static final Log logger = Logger.getLogger(ParticleSetInitializer.class);

    private final ParticleUtils utils;

    public ParticleSetInitializer(boolean setUp,
                                  Family family,
                                  int priority) {
        super(setUp, family, priority);
        this.utils = new ParticleUtils();
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var particleSet = Mapper.particleSet.get(entity);
        var starSet = Mapper.starSet.get(entity);
        var focus = Mapper.focus.get(entity);
        var transform = Mapper.transform.get(entity);
        var label = Mapper.label.get(entity);

        // Focus hits.
        focus.hitCoordinatesConsumer = FocusHit::addHitCoordinateParticleSet;
        focus.hitRayConsumer = FocusHit::addHitRayParticleSet;

        // Initialize particle set
        if (starSet == null) {
            initializeCommon(base, particleSet, label);
            initializeParticleSet(entity, particleSet, label, transform);
        } else {
            initializeCommon(base, starSet, label);
            initializeStarSet(entity, starSet, label, transform);
        }
    }

    @Override
    public void setUpEntity(Entity entity) {
        // Textures.
        var base = Mapper.base.get(entity);
        var particleSet = Mapper.particleSet.get(entity);
        AssetManager manager = AssetBean.manager();
        if (manager.contains(base.getName() + " Textures")) {
            particleSet.textureArray = manager.get(base.getName() + " Textures");
        }

        var starSet = Mapper.starSet.get(entity);

        if (starSet != null) {
            // Stars.
            // Is it a catalog of variable stars?
            starSet.variableStars = !starSet.pointData.isEmpty() && starSet.pointData.get(0) instanceof VariableRecord;
            if (starSet.numLabels > 0) {
                initSortingData(entity, starSet, starSet);
            }

            var model = Mapper.model.get(entity);
            // Star set.
            model.renderConsumer = ModelEntityRenderSystem::renderParticleStarSetModel;

            // Load model in main thread
            GaiaSky.postRunnable(() -> utils.initModel(AssetBean.manager(), model));
        } else {
            // Particles.
            if (particleSet.numLabels > 0) {
                initSortingData(entity, particleSet, null);
            }

            // Model.
            if (particleSet.modelFile != null && manager.isLoaded(Settings.settings.data.dataFile(particleSet.modelFile))) {
                // Model comes from file (probably .obj or .g3db)
                particleSet.model = manager.get(Settings.settings.data.dataFile(particleSet.modelFile), IntModel.class);
            }
        }

    }

    /**
     * Initializes the common parts of particles and star sets.
     *
     * @param base  The base component.
     * @param set   The set.
     * @param label The label component.
     */
    private void initializeCommon(Base base,
                                  ParticleSet set,
                                  Label label) {
        if (base.id < 0) {
            base.id = ParticleSet.idSeq++;
        }

        if (set.factor == null)
            set.factor = 1d;
        set.lastSortTime = -1;
        set.cPosD = new Vector3b();
        set.lastSortCameraPos = new Vector3b(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        set.proximity = new Proximity(Constants.N_DIR_LIGHTS);
        set.focusPosition = new Vector3b();
        set.focusPositionSph = new Vector2d();

        // Default epochs, if not set
        if (set.epochJd <= 0) {
            set.epochJd = AstroUtils.JD_J2015_5;
        }

        // Maps.
        set.forceLabel = new HashSet<>();
        set.labelColors = new HashMap<>();

        // Labels.
        label.label = true;
        label.textScale = 0.5f;
        label.renderFunction = LabelView::renderTextBase;

        // Textures.
        AssetManager manager = AssetBean.manager();
        if (set.textureFiles != null) {
            // Convert to a list of actual file paths.
            Array<String> actualFilePaths = new Array<>();
            for (String textureFile : set.textureFiles) {
                String unpackedFile = Settings.settings.data.dataFile(textureFile);
                Path galLocationPath = Path.of(unpackedFile);
                if (Files.exists(galLocationPath)) {
                    if (Files.isDirectory(galLocationPath)) {
                        // Directory.
                        Collection<File> galaxyFiles = FileUtils.listFiles(galLocationPath.toFile(), new String[]{"png", "jpeg", "jpg"}, true);
                        if (!galaxyFiles.isEmpty()) {
                            for (File f : galaxyFiles) {
                                actualFilePaths.add(f.getAbsolutePath());
                            }
                        }
                    } else {
                        // File.
                        actualFilePaths.add(galLocationPath.toAbsolutePath().toString());
                    }
                }
            }
            // Send to load.
            if (!actualFilePaths.isEmpty()) {
                manager.load(base.getName() + " Textures", TextureArray.class, new TextureArrayParameter(actualFilePaths));
            }
        }
    }

    /**
     * Initializes a particle set. It loads the data from the provider
     *
     * @param entity    The entity.
     * @param set       The particle set.
     * @param label     The label component.
     * @param transform The transform.
     */
    private void initializeParticleSet(Entity entity,
                                       ParticleSet set,
                                       Label label,
                                       RefSysTransform transform) {
        set.isStars = false;
        boolean initializeData = set.pointData == null;

        if (initializeData && set.provider != null) {
            // Load data
            try {
                Class<?> clazz = Class.forName(set.provider);
                IParticleGroupDataProvider provider = (IParticleGroupDataProvider) clazz.getConstructor().newInstance();
                if (set.providerParams != null) {
                    provider.setProviderParams(set.providerParams);
                } else {
                    var params = new HashMap<String, Object>();
                    params.put("type", "PARTICLES");
                    provider.setProviderParams(params);
                }
                provider.setTransformMatrix(transform.matrix);
                set.setData(provider.loadData(set.datafile, set.factor));
                set.isExtended = !set.data().isEmpty() && set.data().get(0).getType() == ParticleRecordType.PARTICLE_EXT;
            } catch (Exception e) {
                Logger.getLogger(this.getClass()).error(e);
                set.pointData = null;
            }
        }

        computeMinMeanMaxDistances(set);
        computeMeanPosition(entity, set);
        setLabelPosition(entity);

        // Labels.
        label.labelMax = 1;
        label.labelFactor = 1e-3f;
        label.renderConsumer = LabelEntityRenderSystem::renderParticleSet;
        set.numLabels = set.numLabels >= 0 ? set.numLabels : Settings.settings.scene.particleGroups.numLabels;

        // Model.
        if (set.modelFile != null && !set.modelFile.isBlank()) {
            AssetBean.addAsset(Settings.settings.data.dataFile(set.modelFile), IntModel.class);
        }
    }

    /**
     * Initializes a star set. Loads the data from the provider and computes mean and
     * label positions.
     *
     * @param entity    The entity.
     * @param set       The star set.
     * @param label     The label component.
     * @param transform The transform.
     */
    public void initializeStarSet(Entity entity,
                                  StarSet set,
                                  Label label,
                                  RefSysTransform transform) {
        set.isStars = true;
        set.isExtended = false;
        boolean initializeData = set.pointData == null;

        if (initializeData && set.provider != null) {
            // Load data
            try {
                Class<?> clazz = Class.forName(set.provider);
                IStarGroupDataProvider provider = (IStarGroupDataProvider) clazz.getConstructor().newInstance();
                provider.setProviderParams(set.providerParams);
                provider.setTransformMatrix(transform.matrix);

                // Set data, generate index
                List<IParticleRecord> l = provider.loadData(set.datafile, set.factor);
                set.setData(l);

            } catch (Exception e) {
                Logger.getLogger(this.getClass()).error(e);
                set.pointData = null;
            }
        }

        computeMeanPosition(entity, set);
        setLabelPosition(entity);

        // Default variability epoch, if not set
        if (set.variabilityEpochJd <= 0) {
            set.variabilityEpochJd = AstroUtils.JD_J2010;
        }

        // Labels.
        label.renderConsumer = LabelEntityRenderSystem::renderStarSet;
        label.renderFunction = LabelView::renderTextBase;
        set.numLabels = set.numLabels >= 0 ? set.numLabels : Settings.settings.scene.star.group.numLabels;

        // Lines.
        var line = Mapper.line.get(entity);
        line.lineWidth = 1f;
        line.renderConsumer = LineEntityRenderSystem::renderStarSet;

        // Billboard.
        var bb = Mapper.billboard.get(entity);
        bb.renderConsumer = BillboardEntityRenderSystem::renderBillboardStarSet;

    }

    public void computeMinMeanMaxDistances(ParticleSet set) {
        set.meanDistance = 0;
        set.maxDistance = Double.MIN_VALUE;
        set.minDistance = Double.MAX_VALUE;
        List<Double> distances = new ArrayList<>();
        for (IParticleRecord point : set.data()) {
            // Add sample to mean distance
            double dist = len(point.x(), point.y(), point.z());
            if (Double.isFinite(dist)) {
                distances.add(dist);
                set.maxDistance = Math.max(set.maxDistance, dist);
                set.minDistance = Math.min(set.minDistance, dist);
            }
        }
        // Mean is computed as half of the 90th percentile to avoid outliers
        distances.sort(Double::compare);
        int idx = (int) Math.ceil((90d / 100d) * (double) distances.size());
        set.meanDistance = distances.get(idx - 1) / 2d;
    }

    public void computeMeanPosition(Entity entity,
                                    ParticleSet set) {
        var body = Mapper.body.get(entity);
        if (set.meanPosition != null) {
            // Use given mean position.
            body.pos.set(set.meanPosition);
        } else if (set.data() == null || set.data().isEmpty()) {
            // Mean position is 0.
            body.pos.set(0, 0, 0);
        } else {
            // Compute mean position from particles.
            for (IParticleRecord point : set.data()) {
                body.pos.add(point.x(), point.y(), point.z());
            }
            body.pos.scl(1d / set.data().size());
        }
    }

    public void setLabelPosition(Entity entity) {
        // Label position
        if (Mapper.label.has(entity) && Mapper.body.has(entity)) {
            Label label = Mapper.label.get(entity);
            Body body = Mapper.body.get(entity);
            if (label.labelPosition == null) {
                label.labelPosition = new Vector3b(body.pos);
            }
        } else {
            Base base = entity.getComponent(Base.class);
            logger.warn("Particle set entity does not have label or body (or both): " + base.getName());
        }
    }

    private double len(double x,
                       double y,
                       double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    private void initSortingData(Entity entity,
                                 ParticleSet particleSet,
                                 StarSet starSet) {
        var pointData = particleSet.pointData;

        // Metadata
        particleSet.metadata = new double[pointData.size()];

        // Initialise indices list with natural order
        particleSet.indices1 = new Integer[pointData.size()];
        particleSet.indices2 = new Integer[pointData.size()];
        for (int i = 0; i < pointData.size(); i++) {
            particleSet.indices1[i] = i;
            particleSet.indices2[i] = i;
        }
        particleSet.active = particleSet.indices1;
        particleSet.background = particleSet.indices2;

        // Initialize updater task
        particleSet.updaterTask = new ParticleSetUpdaterTask(entity, particleSet, starSet);
    }
}
