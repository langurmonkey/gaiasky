package gaiasky.scene;

import com.badlogic.ashley.core.*;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.api.IRenderable;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.render.extract.*;
import gaiasky.scene.system.initialize.*;
import gaiasky.scene.system.update.*;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Logger;
import gaiasky.util.time.ITimeFrameProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * Represents a scene, contains and manages the engine. The engine contains
 * and manages all entities and systems in the world.
 */
public class Scene {
    private static final Logger.Log logger = Logger.getLogger(Scene.class);

    public static final String ROOT_NAME = "Universe";

    /** The engine, containing all entities, components and systems. **/
    public Engine engine;

    /** The index of names to entities. **/
    private Index index;

    /** Repository for families, which are component set definitions. **/
    private Families families;

    /** Archetypes map, links old scene graph model objects to artemis archetypes. **/
    private Archetypes archetypes;

    /** Access to the index. **/
    public Index index() {
        return index;
    }

    /** Access to the archetypes. **/
    public Archetypes archetypes() {
        return archetypes;
    }

    public Scene() {
    }

    public void initialize() {
        engine = new PooledEngine();

        // Initialize families.
        families = new Families();

        // Initialize archetypes.
        archetypes = new Archetypes();
        archetypes.initialize(engine);

        // Add root element
        Archetype rootArchetype = archetypes.get(ROOT_NAME);
        Entity root = rootArchetype.createEntity();
        Base base = root.getComponent(Base.class);
        base.setName(ROOT_NAME);
        engine.addEntity(root);
    }

    /**
     * Runs the given entity systems only once with a dummy
     * delta time of 0. Useful for running one-time initialization and
     * loading tasks.
     *
     * @param systems The systems.
     */
    public void runOnce(EntitySystem... systems) {
        enable(systems);
        engine.update(0f);
        disable(systems);
    }

    /**
     * Enables the given entity systems.
     *
     * @param systems The systems.
     */
    public void enable(EntitySystem... systems) {
        for (EntitySystem system : systems) {
            engine.addSystem(system);
        }
    }

    /**
     * Disables the given entity systems.
     *
     * @param systems The systems.
     */
    public void disable(EntitySystem... systems) {
        for (EntitySystem system : systems) {
            engine.removeSystem(system);
        }
    }

    /**
     * Sets up the initializing systems and runs them. These systems perform the
     * initial entity initialization.
     */
    public void initializeEntities() {
        initializeEntities(false);
    }

    /**
     * Runs the set up initialization stage for all entities. This happens when all
     * asset loading has finished.
     */
    public void setUpEntities() {
        initializeEntities(true);
    }

    private void initializeEntities(boolean setUp) {
        if (engine != null) {
            // Prepare systems
            int priority = 0;
            EntitySystem baseInit = new BaseInitializer(this, setUp, families.graphNodes, priority++);
            EntitySystem fadeInit = new FadeNodeInitializer(index, setUp, families.fadeNodes, priority++);
            EntitySystem particleSetInit = new ParticleSetInitializer(setUp, families.particleSets, priority++);
            EntitySystem particleInit = new ParticleInitializer(setUp, families.particles, priority++);
            EntitySystem modelInit = new ModelInitializer(setUp, families.models, priority++);
            EntitySystem trajectoryInit = new TrajectoryInitializer(setUp, families.orbits, priority++);
            EntitySystem locInit = new LocInitializer(setUp, families.locations, priority++);
            EntitySystem billboardSetInit = new BillboardSetInitializer(setUp, families.billboardSets, priority++);
            EntitySystem axesInit = new AxesInitializer(setUp, families.axes, priority++);
            EntitySystem raymarchingInit = new InvisibleInitializer(setUp, families.raymarchings, priority++);
            EntitySystem datasetDescInit = new DatasetDescriptionInitializer(setUp, families.catalogInfos, priority++);
            EntitySystem backgroundInit = new BackgroundModelInitializer(setUp, families.backgroundModels, priority++);

            // Run once
            runOnce(baseInit, particleSetInit, particleInit, trajectoryInit, modelInit, locInit, billboardSetInit, axesInit, raymarchingInit, fadeInit, datasetDescInit, backgroundInit);
        }
    }

    /**
     * Initializes the name and id index using the current entities.
     */
    public void initializeIndex() {
        if (engine != null) {
            int numEntities = engine.getEntities().size();

            index = new Index(archetypes);
            index.initialize(numEntities);

            // Prepare system
            EntitySystem indexSystem = new IndexInitializer(index, Family.all(Base.class).get(), 0);

            // Run once
            runOnce(indexSystem);
        }
    }

    /**
     * Constructs the scene graph structure in the {@link GraphNode}
     * components of the current entities.
     */
    public void buildSceneGraph() {
        if (engine != null) {
            // Prepare system
            EntitySystem sceneGraphBuilderSystem = new SceneGraphBuilderSystem(index, families.graphNodes, 0);

            // Run once
            runOnce(sceneGraphBuilderSystem);

            GraphNode rootGraph = Mapper.graph.get(index.getNode("Universe"));
            logger.info("Initialized " + (rootGraph.numChildren + 1) + " into the scene graph.");
        }
    }

    /**
     * Prepares the engine to start running update cycles. This method
     * initializes the engine with all the necessary update systems.
     */
    public void prepareUpdateSystems(ISceneRenderer sceneRenderer) {
        if (engine != null) {
            int priority = 0;
            // Scene graph update system needs to run first.
            GraphUpdater sceneGraphUpdateSystem = new GraphUpdater(families.roots, priority++, GaiaSky.instance.time);
            sceneGraphUpdateSystem.setCamera(GaiaSky.instance.getCameraManager());

            // Regular update systems.
            OctreeUpdater octreeUpdateSystem = new OctreeUpdater(families.octrees, priority++);
            FadeUpdater fadeUpdateSystem = new FadeUpdater(families.fadeNodes, priority++);
            ParticleSetUpdater particleSetUpdateSystem = new ParticleSetUpdater(families.particleSets, priority++);
            ModelUpdater modelUpdateSystem = new ModelUpdater(families.models, priority++);
            TrajectoryUpdater trajectoryUpdateSystem = new TrajectoryUpdater(families.orbits, priority++);
            BackgroundUpdater backgroundUpdateSystem = new BackgroundUpdater(families.backgroundModels, priority++);

            // Extract systems.
            AbstractExtractSystem octreeExtractor = newExtractor(OctreeExtractor.class, families.octrees, priority++, sceneRenderer);
            AbstractExtractSystem particleSetExtractor = newExtractor(ParticleSetExtractor.class, families.particleSets, priority++, sceneRenderer);
            AbstractExtractSystem particleExtractor = newExtractor(ParticleExtractor.class, families.particles, priority++, sceneRenderer);
            AbstractExtractSystem modelExtractor = newExtractor(ModelExtractor.class, families.models, priority++, sceneRenderer);
            AbstractExtractSystem trajectoryExtractor = newExtractor(TrajectoryExtractor.class, families.orbits, priority++, sceneRenderer);
            AbstractExtractSystem backgroundExtractor = newExtractor(BackgroundExtractor.class, families.backgroundModels, priority++, sceneRenderer);

            // Remove all remaining systems.
            engine.removeAllSystems();

            // 1. First updater: scene graph and fade update systems.
            engine.addSystem(sceneGraphUpdateSystem);
            engine.addSystem(octreeUpdateSystem);
            engine.addSystem(fadeUpdateSystem);

            // 2. Update --- these can run in parallel.
            engine.addSystem(particleSetUpdateSystem);
            engine.addSystem(modelUpdateSystem);
            engine.addSystem(trajectoryUpdateSystem);
            engine.addSystem(backgroundUpdateSystem);

            // 3. Extract --- these can also run in parallel.
            engine.addSystem(octreeExtractor);
            engine.addSystem(particleSetExtractor);
            engine.addSystem(particleExtractor);
            engine.addSystem(modelExtractor);
            engine.addSystem(trajectoryExtractor);
            engine.addSystem(backgroundExtractor);
        }
    }

    /**
     * Creates a new extractor system with the given class, family and priority.
     *
     * @param extractorClass The extractor class. Must extend {@link AbstractExtractSystem}.
     * @param family         The family.
     * @param priority       The priority of the system (lower means the system gets executed before).
     * @param sceneRenderer  The scene renderer.
     *
     * @return The new system instance.
     */
    private AbstractExtractSystem newExtractor(Class<? extends AbstractExtractSystem> extractorClass, Family family, int priority, ISceneRenderer sceneRenderer) {
        try {
            Constructor c = extractorClass.getDeclaredConstructor(Family.class, int.class);
            AbstractExtractSystem system = (AbstractExtractSystem) c.newInstance(family, priority);
            system.setRenderer(sceneRenderer);
            return system;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the scene. This causes an update to the engine.
     *
     * @param time   The time frame provider object.
     * @param camera The camera object.
     */
    public void update(ITimeFrameProvider time, ICamera camera) {
        engine.update((float) time.getDt());
    }

    /**
     * Enables the given groups of systems.
     *
     * @param systemBags An array with the system bags to enable.
     */
    public void enableSystems(Set<EntitySystem>... systemBags) {
        setEnabled(true, systemBags);
    }

    /**
     * Disables the given groups of systems.
     *
     * @param systemBags An array with the system bags to disable.
     */
    public void disableSystems(Set<EntitySystem>... systemBags) {
        setEnabled(false, systemBags);
    }

    /**
     * Enables or disables a group of system bags.
     *
     * @param enabled    The enabled status.
     * @param systemBags The array of groups of systems to enable or disable.
     */
    public void setEnabled(boolean enabled, Set<EntitySystem>... systemBags) {
        for (Set<EntitySystem> systemBag : systemBags) {
            if (systemBag != null) {
                for (EntitySystem system : systemBag) {
                    if (enabled) {
                        engine.addSystem(system);
                    } else {
                        engine.removeSystem(system);
                    }
                }
            }
        }
    }

    /**
     * Enables or disables a given group of systems.
     *
     * @param enabled The enabled status.
     * @param systems The group of systems to enable or disable.
     */
    public void setEnabled(boolean enabled, Set<EntitySystem> systems) {
        for (EntitySystem system : systems)
            if (enabled) {
                engine.addSystem(system);
            } else {
                engine.removeSystem(system);
            }
    }

    /**
     * Removes the given entity from the scene.
     *
     * @param entity          The entity.
     * @param removeFromIndex Whether to remove it from the index too.
     */
    public void remove(Entity entity, boolean removeFromIndex) {
        var graph = Mapper.graph.get(entity);
        if (entity != null && graph.parent != null) {
            var parentGraph = Mapper.graph.get(graph.parent);
            parentGraph.removeChild(entity, true);
        } else {
            throw new RuntimeException("Given node is null");
        }
        if (removeFromIndex) {
            index.remove(entity);
        }
    }

}
