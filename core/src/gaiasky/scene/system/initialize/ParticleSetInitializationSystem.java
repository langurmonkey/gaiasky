package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.data.group.IParticleGroupDataProvider;
import gaiasky.data.group.IStarGroupDataProvider;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.CatalogInfo;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.math.Vector3b;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Initializes old ParticleGroup and StarGroup objects.
 */
public class ParticleSetInitializationSystem extends IteratingSystem {
    private static final Log logger = Logger.getLogger(ParticleSetInitializationSystem.class);

    public ParticleSetInitializationSystem(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ParticleSet particleSet = Mapper.particleSet.get(entity);
        StarSet starSet = Mapper.starSet.get(entity);

        // Initialize particle set
        if (starSet == null) {
            initializeParticleSet(entity, particleSet);
        } else {
            initializeStarSet(entity, starSet);
        }
    }

    private void initializeParticleSet(Entity entity, ParticleSet set) {
        initializeParticleSet(entity, set, true, true);
    }

    /**
     * Initializes a particle set. It loads the data from the provider
     * @param entity The entity.
     * @param set The particle set.
     * @param dataLoad Whether to load the data.
     * @param createCatalogInfo Whether to initialize the catalog info.
     */
    private void initializeParticleSet(Entity entity, ParticleSet set, boolean dataLoad, boolean createCatalogInfo) {
        // Load data
        try {
            if (set.factor == null)
                set.factor = 1d;

            set.lastSortTime = -1;

            if (dataLoad) {
                Class<?> clazz = Class.forName(set.provider);
                IParticleGroupDataProvider provider = (IParticleGroupDataProvider) clazz.getConstructor().newInstance();
                provider.setProviderParams(set.providerParams);

                set.setData(provider.loadData(set.datafile, set.factor));
            }

            computeMinMeanMaxDistances(entity, set);
            computeMeanPosition(entity, set);
            setLabelPosition(entity);

            if (createCatalogInfo && Mapper.datasetDescription.has(entity)) {
                Base base = Mapper.base.get(entity);
                DatasetDescription desc = Mapper.datasetDescription.get(entity);
                // Create catalog info and broadcast
                CatalogInfo ci = new CatalogInfo(base.names[0], base.names[0], null, CatalogInfoSource.INTERNAL, 1f, entity);
                ci.nParticles = set.pointData != null ? set.pointData.size() : -1;
                Path df = Path.of(Settings.settings.data.dataFile(set.datafile));
                ci.sizeBytes = Files.exists(df) && Files.isRegularFile(df) ? df.toFile().length() : -1;

                // Insert
                EventManager.publish(Event.CATALOG_ADD, this, ci, false);
            }

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
            set.pointData = null;
        }
    }

    /**
     * Initializes a star set. Loads the data from the provider and computes mean and
     * label positions.
     * @param entity The entity.
     * @param set The star set.
     */
    public void initializeStarSet(Entity entity, StarSet set) {
        // Load data
        try {
            if (set.factor == null)
                set.factor = 1d;

            Class<?> clazz = Class.forName(set.provider);
            IStarGroupDataProvider provider = (IStarGroupDataProvider) clazz.getConstructor().newInstance();
            provider.setProviderParams(set.providerParams);

            // Set data, generate index
            List<IParticleRecord> l = provider.loadData(set.datafile, set.factor);
            set.setData(l);

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
            set.pointData = null;
        }

        computeMeanPosition(entity, set);
        setLabelPosition(entity);
    }

    public void computeMinMeanMaxDistances(Entity entity, ParticleSet set) {
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

    public void computeMeanPosition(Entity entity, ParticleSet set) {
        if (!set.fixedMeanPosition && Mapper.body.has(entity)) {
            Body body = Mapper.body.get(entity);
            body.pos = new Vector3b();
            // Mean position
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
            Base base = Mapper.base.get(entity);
            logger.warn("Particle set entity does not have label or body (or both): " + base.getName());
        }
    }

    private double len(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

}
