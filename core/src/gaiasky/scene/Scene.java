package gaiasky.scene;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.GaiaSky;
import gaiasky.scene.component.*;
import gaiasky.scene.system.initialize.*;
import gaiasky.scene.system.update.SceneGraphUpdateSystem;
import gaiasky.scene.view.PositionEntity;
import gaiasky.scenegraph.*;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.IPosition;

import java.util.*;

/**
 * Represents a scene, contains and manages the engine. The engine contains
 * and manages all entities and systems in the world.
 */
public class Scene {
    private static final Logger.Log logger = Logger.getLogger(Scene.class);

    public static final String ROOT_NAME = "Universe";

    /** The engine, containing all entities, components and systems **/
    public Engine engine;

    /** Quick lookup map. Name to node. **/
    protected Map<String, Entity> index;
    /**
     * Map from integer to position with all Hipparcos stars, for the
     * constellations
     **/
    protected Map<Integer, IPosition> hipMap;
    // Archetypes map, links old scene graph model objects to artemis archetypes
    protected Map<String, Archetype> archetypes;

    // Maps old attributes to components
    protected Map<String, Class<? extends Component>> attributeMap;

    public Map<String, Archetype> archetypes() {
        return archetypes;
    }

    public Map<String, Class<? extends Component>> attributeMap() {
        return attributeMap;
    }

    public Scene() {
    }

    public void initialize() {
        engine = new PooledEngine();

        initializeArchetypes();
        initializeAttributes();

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
            EntitySystem baseInit = new BaseInitializer(this, setUp, Family.all(Base.class, GraphNode.class).get(), priority++);
            EntitySystem particleSetInit = new ParticleSetInitializer(setUp, Family.one(ParticleSet.class, StarSet.class).get(), priority++);
            EntitySystem particleInit = new ParticleInitializer(setUp, Family.all(Base.class, Celestial.class, ProperMotion.class, RenderType.class, ParticleExtra.class).get(), priority++);
            EntitySystem trajectoryInit = new TrajectoryInitializer(setUp, Family.all(Trajectory.class, Verts.class).get(), priority++);
            EntitySystem modelInit = new ModelInitializer(setUp, Family.all(Base.class, Body.class, Celestial.class, Model.class, ModelScaffolding.class).get(), priority++);
            EntitySystem locInit = new LocInitializer(setUp, Family.all(LocationMark.class).get(), priority++);
            EntitySystem billboardInit = new BillboardSetInitializer(setUp, Family.all(BillboardSet.class).get(), priority++);
            EntitySystem axesInit = new AxesInitializer(setUp, Family.all(Axis.class, RefSysTransform.class).get(), priority++);

            // Run once
            runOnce(baseInit, particleSetInit, particleInit, trajectoryInit, modelInit, locInit, billboardInit, axesInit);
        }
    }

    /**
     * Initializes the name and id index using the current entities.
     */
    public void initializeIndex() {
        if (engine != null) {
            int numEntities = engine.getEntities().size();
            // String-to-node map. The number of objects is a first approximation, as
            // some nodes actually contain multiple objects.
            index = new HashMap<>((int) (numEntities * 1.25));
            // HIP map with 121k * 1.25
            hipMap = new HashMap<>(151250);

            // Prepare system
            EntitySystem indexSystem = new IndexInitializer(this, Family.all(Base.class).get(), 0);

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
            EntitySystem sceneGraphBuilderSystem = new SceneGraphBuilderSystem(this.index, Family.all(GraphNode.class).get(), 0);

            // Run once
            runOnce(sceneGraphBuilderSystem);

            GraphNode rootGraph = Mapper.graph.get(getNode("Universe"));
            logger.info("Initialized " + (rootGraph.numChildren + 1) + " into the scene graph.");
        }
    }

    /**
     * Prepares the engine to start running update cycles. This method
     * initializes the engine with all the necessary update systems.
     */
    public void prepareUpdateSystems() {
        if (engine != null) {
            SceneGraphUpdateSystem sceneGraphUpdateSystem = new SceneGraphUpdateSystem(Family.all(GraphRoot.class).get(), 0, GaiaSky.instance.time);
            sceneGraphUpdateSystem.setCamera(GaiaSky.instance.getCameraManager());

            // Remove all remaining systems
            engine.removeAllSystems();

            // Add first updater: scene graph update system
            engine.addSystem(sceneGraphUpdateSystem);
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
     * Returns the entity identified with the given name, or null if
     * it is not found.
     *
     * @param name The name of the entity.
     *
     * @return The entity, or null if it does not exist.
     */
    public Entity getNode(String name) {
        synchronized (index) {
            name = name.toLowerCase().strip();
            Entity entity = index.get(name);
            return entity;
        }
    }

    /**
     * Adds the given node to the index. Returns false if it was not added due to a naming conflict (name already exists)
     * with the same object (same class and same names).
     *
     * @param entity The entity to add.
     *
     * @return False if the object already exists.
     */
    public boolean addToIndex(Entity entity) {
        boolean ok = true;
        Base base;
        if ((base = Mapper.base.get(entity)) != null) {
            synchronized (index) {
                if (base.names != null) {
                    if (mustAddToIndex(entity)) {
                        for (String name : base.names) {
                            String nameLowerCase = name.toLowerCase().trim();
                            if (!index.containsKey(nameLowerCase)) {
                                index.put(nameLowerCase, entity);
                            } else if (!nameLowerCase.isEmpty()) {
                                Entity conflict = index.get(nameLowerCase);
                                Base conflictBase = Mapper.base.get(conflict);
                                Archetype entityArchetype = findArchetype(entity);
                                Archetype conflictArchetype = findArchetype(conflict);
                                logger.debug(I18n.msg("error.name.conflict", name + " (" + entityArchetype.getName().toLowerCase() + ")", conflictBase.getName() + " (" + conflictArchetype.getName().toLowerCase() + ")"));
                                String[] names1 = base.names;
                                String[] names2 = conflictBase.names;
                                boolean same = names1.length == names2.length;
                                if (same) {
                                    for (int i = 0; i < names1.length; i++) {
                                        same = same && names1[i].equals(names2[i]);
                                    }
                                }
                                if (same) {
                                    same = entityArchetype == conflictArchetype;
                                }
                                ok = !same;
                            }
                        }

                        // Id
                        Id id = Mapper.id.get(entity);
                        if (id != null && id.id > 0) {
                            String idString = String.valueOf(id.id);
                            index.put(idString, entity);
                        }
                    }

                    // Special cases

                    // HIP stars add "HIP + hipID"
                    Archetype starArchetype = archetypes.get(Star.class.getName());
                    if (starArchetype.matches(entity)) {
                        // Hip
                        Hip hip = Mapper.hip.get(entity);
                        if (hip.hip > 0) {
                            String hipid = "hip " + hip.hip;
                            index.put(hipid, entity);
                        }
                    }

                    // Particle sets add names of each particle
                    ParticleSet particleSet = Mapper.particleSet.get(entity);
                    if (particleSet != null) {
                        if (particleSet.index != null) {
                            Set<String> keys = particleSet.index.keySet();
                            for (String key : keys) {
                                index.put(key, entity);
                            }
                        }
                    }

                }
            }
        }
        if (!ok) {
            logger.warn(I18n.msg("error.object.exists", base.getName() + "(" + findArchetype(entity).getName() + ")"));
        }
        return ok;
    }

    public void addToHipMap(Entity entity) {
        if (Mapper.octant.has(entity)) {
            // TODO add octree stars to hip map
        } else {
            synchronized (hipMap) {
                Archetype starArchetype = archetypes.get(Star.class.getName());
                if (starArchetype.matches(entity)) {
                    Hip hip = Mapper.hip.get(entity);
                    if (hip.hip > 0) {
                        if (hipMap.containsKey(hip.hip)) {
                            logger.debug(I18n.msg("error.id.hip.duplicate", hip.hip));
                        } else {
                            hipMap.put(hip.hip, new PositionEntity(entity));
                        }
                    }
                } else if (Mapper.starSet.has(entity)) {
                    StarSet starSet = Mapper.starSet.get(entity);
                    List<IParticleRecord> stars = starSet.data();
                    for (IParticleRecord pb : stars) {
                        if (pb.hip() > 0) {
                            hipMap.put(pb.hip(), new Position(pb.x(), pb.y(), pb.z(), pb.pmx(), pb.pmy(), pb.pmz()));
                        }
                    }
                }
            }
        }
    }

    private boolean mustAddToIndex(Entity entity) {
        // All entities except the ones who have perimeter, location mark and particle or star set
        return entity.getComponent(Perimeter.class) == null && entity.getComponent(LocationMark.class) == null && entity.getComponent(ParticleSet.class) == null && entity.getComponent(StarSet.class) == null;
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
     * Initializes the archetypes map with an entry for each model object.
     */
    protected void initializeArchetypes() {
        this.archetypes = (new ArchetypeInitializer(engine)).initializeArchetypes();
    }

    /**
     * Initializes the attributes map.
     */
    private void initializeAttributes() {
        this.attributeMap = (new AttributeInitializer(engine)).initializeAttributes();
    }

    /**
     * Find a matching archetype given an entity.
     *
     * @param entity The entity.
     *
     * @return The matching archetype if it exists, or null if it does not.
     */
    private Archetype findArchetype(Entity entity) {
        Collection<Archetype> archetypes = this.archetypes.values();
        for (Archetype archetype : archetypes) {
            if (archetype.matches(entity)) {
                return archetype;
            }
        }
        return null;
    }

}
