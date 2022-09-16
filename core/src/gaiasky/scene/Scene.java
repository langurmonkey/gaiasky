package gaiasky.scene;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.ICopy;
import gaiasky.scene.component.IDisposable;
import gaiasky.scene.system.initialize.*;
import gaiasky.scene.system.render.extract.*;
import gaiasky.scene.system.update.*;
import gaiasky.scene.view.FocusView;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3b;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.IOctreeObject;
import gaiasky.util.tree.OctreeNode;

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

    /** Holds all initialization systems. **/
    private Array<AbstractInitSystem> initializers;
    /** Holds all update systems. **/
    private Array<AbstractUpdateSystem> updaters;
    /** Holds all extract systems. **/
    private Array<AbstractExtractSystem> extractors;

    /** Number of actual objects in the scene. **/
    private int numberObjects = -1;

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
        focusView = new FocusView(this);

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

        // Create scene observer
        new SceneObserver(this);
    }

    /**
     * Runs the given entity systems only once with a dummy
     * delta time of 0. Useful for running one-time initialization and
     * loading tasks.
     *
     * @param systems The systems to run.
     */
    public void runOnce(EntitySystem... systems) {
        enable(systems);
        engine.update(0f);
        disable(systems);
    }

    /**
     * Runs the given entity systems only once with a dummy
     * delta time of 0. Useful for running one-time initialization and
     * loading tasks.
     *
     * @param array The systems array to run.
     */
    public void runOnce(Array<? extends EntitySystem> array) {
        enable(array);
        engine.update(0f);
        disable(array);
    }

    /**
     * Enable the given entity systems.
     *
     * @param systems The systems.
     */
    public void enable(EntitySystem... systems) {
        for (EntitySystem system : systems) {
            engine.addSystem(system);
        }
    }

    /**
     * Enable the given entity systems.
     *
     * @param array The systems.
     */
    public void enable(Array<? extends EntitySystem> array) {
        array.forEach((system) -> engine.addSystem(system));
    }

    /**
     * Disable the given entity systems.
     *
     * @param systems The systems.
     */
    public void disable(EntitySystem... systems) {
        for (EntitySystem system : systems) {
            engine.removeSystem(system);
        }
    }

    /**
     * Disable the given entity systems.
     *
     * @param array The systems.
     */
    public void disable(Array<? extends EntitySystem> array) {
        array.forEach((system) -> engine.removeSystem(system));
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

    public void addSystemsToEngine(Array<? extends EntitySystem> systems) {
        if (engine != null) {
            for (EntitySystem system : systems) {
                engine.addSystem(system);
            }
        }
    }

    /**
     * Add a new initializer system to the scene.
     */
    private void addInitializer(AbstractInitSystem system) {
        initializers.add(system);
    }

    /**
     * Add a new updater system to the scene.
     */
    private void addUpdater(AbstractUpdateSystem system) {
        updaters.add(system);
    }

    /**
     * Add a new extractor system to the scene.
     */
    private void addExtractor(AbstractExtractSystem system) {
        extractors.add(system);
    }

    private void initializeEntities(boolean setUp) {
        if (engine != null) {
            int priority = 0;

            // Prepare systems.
            initializers = new Array<>(21);
            addInitializer(new BaseInitializer(this, setUp, families.graphNodes, priority++));
            addInitializer(new FadeNodeInitializer(index, setUp, families.fadeNodes, priority++));
            addInitializer(new ParticleSetInitializer(setUp, families.particleSets, priority++));
            addInitializer(new ParticleInitializer(setUp, families.particles, priority++));
            addInitializer(new ModelInitializer(setUp, families.models, priority++));
            addInitializer(new TrajectoryInitializer(setUp, families.orbits, priority++));
            addInitializer(new VertsInitializer(setUp, families.verts, priority++));
            addInitializer(new LocInitializer(setUp, families.locations, priority++));
            addInitializer(new BillboardSetInitializer(setUp, families.billboardSets, priority++));
            addInitializer(new AxesInitializer(setUp, families.axes, priority++));
            addInitializer(new RaymarchingInitializer(setUp, families.raymarchings, priority++));
            addInitializer(new BackgroundModelInitializer(setUp, families.backgroundModels, priority++));
            addInitializer(new ClusterInitializer(setUp, families.clusters, priority++));
            addInitializer(new ConstellationInitializer(this, setUp, families.constellations, priority++));
            addInitializer(new BoundariesInitializer(setUp, families.boundaries, priority++));
            addInitializer(new ElementsSetInitializer(setUp, families.orbitalElementSets, priority++));
            addInitializer(new MeshInitializer(setUp, families.meshes, priority++));
            addInitializer(new GridRecInitializer(setUp, families.gridRecs, priority++));
            addInitializer(new RulerInitializer(setUp, families.rulers, priority++));
            addInitializer(new TitleInitializer(setUp, families.titles, priority++));
            addInitializer(new KeyframeInitializer(this, setUp, families.keyframes, priority++));
            addInitializer(new ShapeInitializer(setUp, families.shapes, priority++));
            addInitializer(new LocInitializer(setUp, families.locations, priority++));
            addInitializer(new PerimeterInitializer(setUp, families.perimeters, priority++));
            addInitializer(new DatasetDescriptionInitializer(setUp, families.catalogInfos, priority));

            // Run once
            runOnce(initializers);
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

            // Prepare system.
            EntitySystem indexSystem = new IndexInitializer(index, Family.all(Base.class).get(), 0);

            // Run once.
            runOnce(indexSystem);
        }
    }

    /**
     * Constructs the scene graph structure in the {@link GraphNode}
     * components of the current entities.
     */
    public void buildSceneGraph() {
        if (engine != null) {
            // Prepare system.
            EntitySystem sceneGraphBuilderSystem = new SceneGraphBuilderSystem(index, families.graphNodes, 0);

            // Run once.
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
            updaters = new Array<>(18);
            addUpdater(new ConstellationUpdater(families.constellations, priority++));
            GraphUpdater sceneGraphUpdateSystem = new GraphUpdater(families.roots, priority++, GaiaSky.instance.time);
            sceneGraphUpdateSystem.setCamera(GaiaSky.instance.getCameraManager());
            addUpdater(sceneGraphUpdateSystem);

            // Regular update systems.
            addUpdater(new OctreeUpdater(this, families.octrees, priority++));
            addUpdater(new ElementsSetUpdater(families.orbitalElementSets, priority++));
            addUpdater(new ParticleSetUpdater(families.particleSets, priority++));
            addUpdater(new ModelUpdater(families.models, priority++));
            addUpdater(new TrajectoryUpdater(families.orbits, priority++));
            addUpdater(new VertsUpdater(families.verts, priority++));
            addUpdater(new BackgroundUpdater(families.backgroundModels, priority++));
            addUpdater(new ClusterUpdater(families.clusters, priority++));
            addUpdater(new RaymarchingUpdater(families.raymarchings, priority++));
            addUpdater(new BillboardSetUpdater(families.billboardSets, priority++));
            addUpdater(new MeshUpdater(families.meshes, priority++));
            addUpdater(new GridRecUpdater(families.gridRecs, priority++));
            addUpdater(new RulerUpdater(families.rulers, priority++));
            addUpdater(new AxesUpdater(families.axes, priority++));
            addUpdater(new TitleUpdater(families.titles, priority++));
            addUpdater(new KeyframeUpdater(families.keyframes, priority++));
            addUpdater(new ShapeUpdater(families.shapes, priority++));
            addUpdater(new LocUpdater(families.locations, priority++));
            addUpdater(new PerimeterUpdater(families.perimeters, priority++));

            // Extract systems.
            extractors = new Array<>(18);
            addExtractor(newExtractor(OctreeExtractor.class, families.octrees, priority++, sceneRenderer));
            addExtractor(newExtractor(ElementsSetExtractor.class, families.orbitalElementSets, priority++, sceneRenderer));
            addExtractor(newExtractor(ParticleSetExtractor.class, families.particleSets, priority++, sceneRenderer));
            addExtractor(newExtractor(ParticleExtractor.class, families.particles, priority++, sceneRenderer));
            addExtractor(newExtractor(ModelExtractor.class, families.models, priority++, sceneRenderer));
            addExtractor(newExtractor(TrajectoryExtractor.class, families.orbits, priority++, sceneRenderer));
            addExtractor(newExtractor(VertsExtractor.class, families.verts, priority++, sceneRenderer));
            addExtractor(newExtractor(BackgroundExtractor.class, families.backgroundModels, priority++, sceneRenderer));
            addExtractor(newExtractor(ClusterExtractor.class, families.clusters, priority++, sceneRenderer));
            addExtractor(newExtractor(BillboardSetExtractor.class, families.billboardSets, priority++, sceneRenderer));
            addExtractor(newExtractor(ConstellationExtractor.class, families.constellations, priority++, sceneRenderer));
            addExtractor(newExtractor(BoundariesExtractor.class, families.boundaries, priority++, sceneRenderer));
            addExtractor(newExtractor(MeshExtractor.class, families.meshes, priority++, sceneRenderer));
            addExtractor(newExtractor(GridRecExtractor.class, families.gridRecs, priority++, sceneRenderer));
            addExtractor(newExtractor(RulerExtractor.class, families.rulers, priority++, sceneRenderer));
            addExtractor(newExtractor(AxesExtractor.class, families.axes, priority++, sceneRenderer));
            addExtractor(newExtractor(TitleExtractor.class, families.titles, priority++, sceneRenderer));
            addExtractor(newExtractor(KeyframeExtractor.class, families.keyframes, priority++, sceneRenderer));
            addExtractor(newExtractor(ShapeExtractor.class, families.shapes, priority++, sceneRenderer));
            addExtractor(newExtractor(LocExtractor.class, families.locations, priority++, sceneRenderer));
            addExtractor(newExtractor(PerimeterExtractor.class, families.perimeters, priority++, sceneRenderer));
            addExtractor(newExtractor(RaymarchingExtractor.class, families.raymarchings, priority, sceneRenderer));

            // Remove all remaining systems.
            engine.removeAllSystems();

            // Add updater systems.
            addSystemsToEngine(updaters);

            // Add extractors.
            addSystemsToEngine(extractors);
        }
    }

    /**
     * Runs the matching initialization systems on the given entity. The systems
     * are matched using their families.
     *
     * @param entity The entity to initialize.
     */
    public void initializeEntity(Entity entity) {
        if (initializers != null) {
            for (AbstractInitSystem system : initializers) {
                if (system.getFamily().matches(entity)) {
                    system.initializeEntity(entity);
                }
            }
        }
    }

    /**
     * Runs the matching setup systems on the given entity. The systems
     * are matched using their families.
     *
     * @param entity The entity to set up.
     */
    public void setUpEntity(Entity entity) {
        if (initializers != null) {
            for (AbstractInitSystem system : initializers) {
                if (system.getFamily().matches(entity)) {
                    system.setUpEntity(entity);
                }
            }
        }
    }

    /**
     * Runs the matching update systems on the given entity. The systems
     * are matched using their families.
     *
     * @param entity The entity to update.
     */
    public void updateEntity(Entity entity, float deltaTime) {
        if (updaters != null) {
            for (AbstractUpdateSystem system : updaters) {
                if (system.getFamily().matches(entity)) {
                    system.updateEntity(entity, deltaTime);
                }
            }
        }
    }

    /**
     * Updates the entity graph for the given entity with the given parent translation vector. Use
     * with caution, this may have unforeseen consequences, as the process usually updates the
     * whole scene graph starting at the root. This by-passes the usual procedure to update
     * a single entity and its subtree.
     *
     * @param entity            The entity to update.
     * @param time              The time object.
     * @param parentTranslation The parent translation.
     * @param opacity           The opacity value.
     */
    public void updateEntityGraph(Entity entity, ITimeFrameProvider time, Vector3b parentTranslation, float opacity) {
        var updater = findUpdater(GraphUpdater.class);
        if (updater != null) {
            updater.update(entity, time, parentTranslation, opacity);
        }
    }

    private <T extends EntityUpdater> T findUpdater(Class<T> updaterClass) {
        if (updaters != null) {
            for (EntityUpdater updater : updaters) {
                if (updater.getClass().isAssignableFrom(updaterClass)) {
                    return (T) updater;
                }
            }
        }
        return null;
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
    private <T extends AbstractExtractSystem> T newExtractor(Class<T> extractorClass, Family family, int priority, ISceneRenderer sceneRenderer) {
        try {
            Constructor<T> c = extractorClass.getDeclaredConstructor(Family.class, int.class);
            T system = c.newInstance(family, priority);
            system.setRenderer(sceneRenderer);
            return system;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the scene. This causes an update to the engine.
     *
     * @param time The time frame provider object.
     */
    public void update(ITimeFrameProvider time) {
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
        try{
            engine.addEntity(entity);
        } catch (IllegalArgumentException e) {
            logger.debug("Entity " + base.getName() + " already in engine.");
        }
        reportDebugObjects();
    }

    /**
     * Removes the given entity from the scene.
     *
     * @param entity          The entity.
     * @param removeFromIndex Whether to remove it from the index too.
     */
    public void remove(Entity entity, boolean removeFromIndex) {
        if (entity != null && Mapper.graph.get(entity).parent != null) {
            var graph = Mapper.graph.get(entity);
            var parentGraph = Mapper.graph.get(graph.parent);
            parentGraph.removeChild(entity, true);
        } else {
            throw new RuntimeException("Given node is null");
        }
        if (removeFromIndex) {
            index.remove(entity);
        }

        // Remove focus if needed.
        CameraManager cam = GaiaSky.instance.getCameraManager();
        if (cam != null && cam.hasFocus() && ((FocusView) cam.getFocus()).getEntity() == entity) {
            EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
        }

        // Dispose components.
        var components = entity.getComponents();
        for (var component : components) {
            if(component instanceof IDisposable) {
                ((IDisposable) component).dispose(entity);
            }
        }

        // Remove from engine.
        engine.removeEntity(entity);

        // Report update to number of objects.
        reportDebugObjects();
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
            } else if (Mapper.octree.has(entity)) {
                // LOD objects are not in the scene graph structure.
                var octree = Mapper.octree.get(entity);
                Set<Entity> objects = octree.parenthood.keySet();
                objects.forEach((object) -> {
                    if (Mapper.focus.has(object)) {
                        list.add(object);
                    }
                });
            }
        });

        return list;
    }

    public ImmutableArray<Entity> findEntitiesByFamily(Family family) {
        return engine.getEntitiesFor(family);
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

    public Families getFamilies() {
        return families;
    }

    public Entity getLineCopy(Entity entity) {
        var copy = getSimpleCopy(entity);
        var graph = Mapper.graph.get(entity);
        if (graph.parent != null) {
            var parentCopy = getLineCopy(graph.parent);
            var parentCopyGraph = Mapper.graph.get(parentCopy);
            parentCopyGraph.addChild(parentCopy, copy, false, 1);
        }
        return copy;
    }

    public Entity getSimpleCopy(Entity entity) {
        Entity copy = engine.createEntity();
        /*
         * Copy components. Only components that implement the {@link ICopy} interface
         * are actually copied. The rest are created by reflection with their
         * default configurations.
         */
        for (Component component : entity.getComponents()) {
            if (component instanceof ICopy) {
                ICopy iCopy = (ICopy) component;
                Component componentCopy = iCopy.getCopy(engine);
                copy.add(componentCopy);
            } else {
                try {
                    Component componentCopy = component.getClass().getDeclaredConstructor().newInstance();
                    copy.add(componentCopy);
                } catch (Exception e) {
                    logger.error("Could not create copy of component " + component.getClass().getSimpleName());
                }
            }
        }
        return copy;
    }

    public void reportDebugObjects() {
        updateNumberObjects();
        EventManager.publish(Event.DEBUG_OBJECTS, this, numberObjects, numberObjects);
    }

    private void updateNumberObjects() {
        if (engine != null) {
            ImmutableArray<Entity> entities = engine.getEntities();
            numberObjects = 0;
            for (Entity entity : entities) {
                numberObjects += getNumberObjects(entity);
            }
        }
    }

    /**
     * Returns the number of actual objects held by this entity. Most entities count as only
     * one object, but some, such as particle and star sets, orbital element sets or octrees,
     * actually hold and wrap many objects.
     *
     * @param entity The entity.
     *
     * @return The number of objects it holds.
     */
    public int getNumberObjects(Entity entity) {

        if (Mapper.particleSet.has(entity)) {
            return Mapper.particleSet.get(entity).data().size();
        } else if (Mapper.starSet.has(entity)) {
            return Mapper.starSet.get(entity).data().size();
        } else if (Mapper.octree.has(entity)) {
            var octant = Mapper.octant.get(entity);
            return getNumberObjects(octant.octant);
        } else {
            return 1;
        }
    }

    /**
     * Computes the number of objects in the given octree node, recursively.
     *
     * @param node The node.
     *
     * @return The number of objects.
     */
    public int getNumberObjects(OctreeNode node) {
        if (node == null) {
            return 0;
        } else {
            int objects = 0;

            // Add own objects.
            if (node.objects != null && !node.objects.isEmpty()) {
                for (IOctreeObject o : node.objects) {
                    objects += o.getStarCount();
                }
            }

            // Count recursively.
            for (OctreeNode child : node.children) {
                objects += getNumberObjects(child);
            }
            return objects;
        }

    }
}
