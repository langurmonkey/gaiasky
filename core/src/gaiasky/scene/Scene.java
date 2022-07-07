package gaiasky.scene;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.system.render.extract.*;
import gaiasky.scene.system.initialize.*;
import gaiasky.scene.system.update.*;
import gaiasky.scene.view.FocusView;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3b;
import gaiasky.util.time.ITimeFrameProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /** The focus view. **/
    private FocusView focusView;

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
        focusView = new FocusView();

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
            EntitySystem raymarchingInit = new RaymarchingInitializer(setUp, families.raymarchings, priority++);
            EntitySystem datasetDescInit = new DatasetDescriptionInitializer(setUp, families.catalogInfos, priority++);
            EntitySystem backgroundInit = new BackgroundModelInitializer(setUp, families.backgroundModels, priority++);
            EntitySystem clusterInit = new ClusterInitializer(setUp, families.clusters, priority++);
            EntitySystem constellationInit = new ConstellationInitializer(setUp, families.constellations, priority++);
            EntitySystem boundariesInit = new BoundariesInitializer(setUp, families.boundaries, priority++);
            EntitySystem elementsSetInit = new ElementsSetInitializer(setUp, families.orbitalElementSets, priority++);
            EntitySystem meshInit = new MeshInitializer(setUp, families.meshes, priority++);
            EntitySystem recGridInit = new GridRecInitializer(setUp, families.gridRecs, priority++);
            EntitySystem rulerInit = new RulerInitializer(setUp, families.rulers, priority++);
            EntitySystem titleInit = new TitleInitializer(setUp, families.titles, priority++);
            EntitySystem keyframeInit = new KeyframeInitializer(this, setUp, families.keyframes, priority++);
            EntitySystem shapeInit = new ShapeInitializer(setUp, families.shapes, priority++);

            // Run once
            runOnce(baseInit, particleSetInit, particleInit,
                    trajectoryInit, modelInit, locInit, billboardSetInit,
                    axesInit, raymarchingInit, fadeInit, datasetDescInit,
                    backgroundInit, clusterInit, constellationInit, boundariesInit,
                    elementsSetInit, meshInit, recGridInit, rulerInit,
                    titleInit, keyframeInit, shapeInit);
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

            GraphNode rootGraph = Mapper.graph.get(index.getEntity("Universe"));
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
            ConstellationUpdater constellationUpdateSystem = new ConstellationUpdater(families.constellations, priority++);
            GraphUpdater sceneGraphUpdateSystem = new GraphUpdater(families.roots, priority++, GaiaSky.instance.time);
            sceneGraphUpdateSystem.setCamera(GaiaSky.instance.getCameraManager());

            // Regular update systems.
            OctreeUpdater octreeUpdateSystem = new OctreeUpdater(families.octrees, priority++);
            ElementsSetUpdater elementsSetUpdater = new ElementsSetUpdater(families.orbitalElementSets, priority++);
            ParticleSetUpdater particleSetUpdateSystem = new ParticleSetUpdater(families.particleSets, priority++);
            ModelUpdater modelUpdateSystem = new ModelUpdater(families.models, priority++);
            TrajectoryUpdater trajectoryUpdateSystem = new TrajectoryUpdater(families.orbits, priority++);
            BackgroundUpdater backgroundUpdateSystem = new BackgroundUpdater(families.backgroundModels, priority++);
            ClusterUpdater clusterUpdateSystem = new ClusterUpdater(families.clusters, priority++);
            RaymarchingUpdater raymarchingUpdater = new RaymarchingUpdater(families.raymarchings, priority++);
            BillboardSetUpdater billboardSetUpdater = new BillboardSetUpdater(families.billboardSets, priority++);
            MeshUpdater meshUpdater = new MeshUpdater(families.meshes, priority++);
            GridRecUpdater gridRecUpdater = new GridRecUpdater(families.gridRecs, priority++);
            RulerUpdater rulerUpdater = new RulerUpdater(families.rulers, priority++);
            AxesUpdater axesUpdater = new AxesUpdater(families.axes, priority++);
            TitleUpdater titleUpdater = new TitleUpdater(families.titles, priority++);
            KeyframeUpdater keyframeUpdater = new KeyframeUpdater(families.keyframes, priority++);
            ShapeUpdater shapeUpdater = new ShapeUpdater(families.shapes, priority++);

            // Extract systems.
            AbstractExtractSystem octreeExtractor = newExtractor(OctreeExtractor.class, families.octrees, priority++, sceneRenderer);
            AbstractExtractSystem elementsSetExtractor = newExtractor(ElementsSetExtractor.class, families.orbitalElementSets, priority++, sceneRenderer);
            AbstractExtractSystem particleSetExtractor = newExtractor(ParticleSetExtractor.class, families.particleSets, priority++, sceneRenderer);
            AbstractExtractSystem particleExtractor = newExtractor(ParticleExtractor.class, families.particles, priority++, sceneRenderer);
            AbstractExtractSystem modelExtractor = newExtractor(ModelExtractor.class, families.models, priority++, sceneRenderer);
            AbstractExtractSystem trajectoryExtractor = newExtractor(TrajectoryExtractor.class, families.orbits, priority++, sceneRenderer);
            AbstractExtractSystem backgroundExtractor = newExtractor(BackgroundExtractor.class, families.backgroundModels, priority++, sceneRenderer);
            AbstractExtractSystem clusterExtractor = newExtractor(ClusterExtractor.class, families.clusters, priority++, sceneRenderer);
            AbstractExtractSystem billboardSetExtractor = newExtractor(BillboardSetExtractor.class, families.billboardSets, priority++, sceneRenderer);
            AbstractExtractSystem constellationExtractor = newExtractor(ConstellationExtractor.class, families.constellations, priority++, sceneRenderer);
            AbstractExtractSystem boundariesExtractor = newExtractor(BoundariesExtractor.class, families.boundaries, priority++, sceneRenderer);
            AbstractExtractSystem meshExtractor = newExtractor(MeshExtractor.class, families.meshes, priority++, sceneRenderer);
            AbstractExtractSystem gridRecExtractor = newExtractor(GridRecExtractor.class, families.gridRecs, priority++, sceneRenderer);
            AbstractExtractSystem rulerExtractor = newExtractor(RulerExtractor.class, families.rulers, priority++, sceneRenderer);
            AbstractExtractSystem axesExtractor = newExtractor(AxesExtractor.class, families.axes, priority++, sceneRenderer);
            AbstractExtractSystem titleExtractor = newExtractor(TitleExtractor.class, families.titles, priority++, sceneRenderer);
            AbstractExtractSystem keyframeExtractor = newExtractor(KeyframeExtractor.class, families.keyframes, priority++, sceneRenderer);
            AbstractExtractSystem shapeExtractor = newExtractor(ShapeExtractor.class, families.shapes, priority++, sceneRenderer);

            // Remove all remaining systems.
            engine.removeAllSystems();

            // 1. First updater: scene graph and octree update systems.
            engine.addSystem(constellationUpdateSystem);
            engine.addSystem(sceneGraphUpdateSystem);
            engine.addSystem(octreeUpdateSystem);
            engine.addSystem(elementsSetUpdater);

            // 2. Update --- these can run in parallel.
            engine.addSystem(particleSetUpdateSystem);
            engine.addSystem(modelUpdateSystem);
            engine.addSystem(trajectoryUpdateSystem);
            engine.addSystem(backgroundUpdateSystem);
            engine.addSystem(clusterUpdateSystem);
            engine.addSystem(raymarchingUpdater);
            engine.addSystem(billboardSetUpdater);
            engine.addSystem(meshUpdater);
            engine.addSystem(gridRecUpdater);
            engine.addSystem(rulerUpdater);
            engine.addSystem(axesUpdater);
            engine.addSystem(titleUpdater);
            engine.addSystem(keyframeUpdater);
            engine.addSystem(shapeUpdater);

            // 3. Extract --- these can also run in parallel.
            engine.addSystem(octreeExtractor);
            engine.addSystem(elementsSetExtractor);
            engine.addSystem(particleSetExtractor);
            engine.addSystem(particleExtractor);
            engine.addSystem(modelExtractor);
            engine.addSystem(trajectoryExtractor);
            engine.addSystem(backgroundExtractor);
            engine.addSystem(clusterExtractor);
            engine.addSystem(billboardSetExtractor);
            engine.addSystem(constellationExtractor);
            engine.addSystem(boundariesExtractor);
            engine.addSystem(meshExtractor);
            engine.addSystem(gridRecExtractor);
            engine.addSystem(rulerExtractor);
            engine.addSystem(axesExtractor);
            engine.addSystem(titleExtractor);
            engine.addSystem(keyframeExtractor);
            engine.addSystem(shapeExtractor);
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

    public void insert(Entity entity, boolean addToIndex) {
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);
        Entity parent = getEntity(graph.parentName);
        boolean ok = true;
        if (addToIndex) {
            ok = index.addToIndex(entity);
        }
        if (!ok) {
            logger.warn(I18n.msg("error.object.exists", base.getName() + "(" + archetypes.findArchetype(entity).getName() + ")"));
        } else {
            if (parent != null) {
                var parentGraph = Mapper.graph.get(parent);
                parentGraph.addChild(parent, entity, true, 1);
            } else {
                throw new RuntimeException(I18n.msg("error.parent.notfound", base.getName(), graph.parentName));
            }
        }
        // Add to engine
        engine.addEntity(entity);
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

    /**
     * Updates the localized names of all entities in the scene.
     */
    public void updateLocalizedNames() {
        engine.getEntities().forEach((entity) -> {
            var base = Mapper.base.get(entity);
            base.updateLocalizedName();
        });
    }

    private Vector3b aux3b = new Vector3b();

    /**
     * Returns the entity with the given name, or null if it does not exist.
     *
     * @param name The name of the entity to retrieve.
     *
     * @return The entity.
     */
    public Entity getEntity(String name) {
        return index.getEntity(name);
    }

    /**
     * Returns the focus entity with the given name, if it exists.
     *
     * @param name The name.
     *
     * @return The entity.
     */
    public Entity findFocus(String name) {
        Entity entity = getEntity(name);
        if (Mapper.focus.has(entity))
            return entity;
        else
            return null;
    }

    /**
     * Returns focus entities matching the given string by name, to a maximum
     * of 10 results.
     *
     * @param name    The name.
     * @param results The set where the results are to be stored.
     */
    public void findMatchingFocusEntity(String name, SortedSet<String> results) {
        index.matchingFocusableNodes(name, results, 10, null);
    }

    /**
     * Returns focus entities matching the given string by name, to a maximum
     * of <code>maxResults</code>.
     *
     * @param name       The name.
     * @param results    The set where the results are to be stored.
     * @param maxResults The maximum number of results.
     * @param abort      To enable abortion mid-computation.
     */
    public void matchingFocusableNodes(String name, SortedSet<String> results, int maxResults, AtomicBoolean abort) {
        index.matchingFocusableNodes(name, results, maxResults, abort);
    }

    /**
     * Returns a list with all the entities which are focusable.
     *
     * @return A list with all focusable entities in this scene.
     */
    public Array<Entity> findFocusableEntities() {
        Array<Entity> list = new Array<>();

        engine.getEntities().forEach((entity) -> {
            if (Mapper.focus.has(entity)) {
                list.add(entity);
            }
        });

        return list;
    }

    /**
     * Gets the current position of the object identified by the given name.
     * The given position is in the internal reference system and corrects stars
     * for proper motions and other objects for their specific motions as well.
     *
     * @param name The name of the object
     * @param out  The out double array
     *
     * @return The out double array if the object exists, has a position and out has 3 or more
     * slots. Null otherwise.
     */
    public double[] getObjectPosition(String name, double[] out) {
        if (out.length >= 3 && name != null) {
            name = name.toLowerCase().trim();
            if (index.containsEntity(name)) {
                Entity entity = index.getEntity(name);
                focusView.setEntity(entity);
                focusView.getAbsolutePosition(name, aux3b);
                out[0] = aux3b.x.doubleValue();
                out[1] = aux3b.y.doubleValue();
                out[2] = aux3b.z.doubleValue();
                return out;
            }
        }
        return null;
    }

}
