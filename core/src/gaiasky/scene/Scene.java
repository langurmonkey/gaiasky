package gaiasky.scene;

import com.badlogic.ashley.core.*;
import gaiasky.scene.component.*;
import gaiasky.scene.component.MotorEngine;
import gaiasky.scene.system.initialize.*;
import gaiasky.scene.view.PositionEntity;
import gaiasky.scenegraph.*;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;
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
    public com.badlogic.ashley.core.Engine engine;

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
        Archetype sgn = archetypes.get(SceneGraphNode.class.getName());
        Entity root = sgn.createEntity();
        Base base = root.getComponent(Base.class);
        base.setName(ROOT_NAME);
        engine.addEntity(root);

    }

    /**
     * Runs the given entity systems only once with a dummy
     * delta time of 0. Useful for running one-time initialization and
     * loading tasks.
     * @param systems The systems.
     */
    public void runOnce(EntitySystem... systems) {
        enable(systems);
        engine.update(0f);
        disable(systems);
    }

    /**
     * Enables the given entity systems.
     * @param systems The systems.
     */
    public void enable(EntitySystem... systems) {
        for(EntitySystem system : systems){
            engine.addSystem(system);
        }
    }

    /**
     * Disables the given entity systems.
     * @param systems The systems.
     */
    public void disable(EntitySystem... systems) {
        for(EntitySystem system : systems){
            engine.removeSystem(system);
        }
    }

    /**
     * Sets up the initializing systems and runs them. These systems perform the
     * initial entity initialization.
     */
    public void initializeEntities() {
        if (engine != null) {
            // Prepare systems
            EntitySystem baseInit = new BaseInitializationSystem(Family.all(Base.class).get(), 0);
            EntitySystem particleSetInit = new ParticleSetInitializationSystem(Family.one(ParticleSet.class, StarSet.class).get(), 1);
            EntitySystem particleInit = new ParticleInitializationSystem(Family.all(Base.class, Celestial.class, ProperMotion.class, RenderType.class, ParticleExtra.class).get(), 2);
            EntitySystem modelInit = new ModelInitializationSystem(Family.all(Base.class, Body.class, Celestial.class, Model.class, ModelScaffolding.class).get(), 3);

            // Run once
            runOnce(baseInit, particleSetInit, particleInit, modelInit);
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
            IndexInitializationSystem indexSystem = new IndexInitializationSystem(Family.all(Base.class).get(), 0, this);

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

        }
    }

    /**
     * Adds the given node to the index. Returns false if it was not added due to a naming conflict (name already exists)
     * with the same object (same class and same names).
     *
     * @param entity The entity to add.
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

    protected void initializeArchetypes() {
        if (this.engine != null) {
            this.archetypes = new HashMap<>();

            // SceneGraphNode
            addArchetype(SceneGraphNode.class.getName(), Base.class, Flags.class, Body.class, GraphNode.class, Octant.class);

            // Celestial
            addArchetype(CelestialBody.class.getName(), SceneGraphNode.class.getName(), Celestial.class, Magnitude.class,
                    Coordinates.class, Rotation.class, Text.class, SolidAngle.class);

            // ModelBody
            addArchetype(ModelBody.class.getName(), CelestialBody.class.getName(), Model.class, ModelScaffolding.class, AffineTransformations.class);

            // Planet
            addArchetype(Planet.class.getName(), ModelBody.class.getName(), Atmosphere.class, Cloud.class);

            // Particle
            addArchetype(Particle.class.getName(), CelestialBody.class.getName(), ProperMotion.class, RenderType.class, ParticleExtra.class);

            // Star
            addArchetype(Star.class.getName(), Particle.class.getName(), Hip.class, Distance.class);

            // Satellite
            addArchetype(Satellite.class.getName(), ModelBody.class.getName(), ParentOrientation.class);

            // HeliotropicSatellite
            addArchetype(HeliotropicSatellite.class.getName(), Satellite.class.getName(), Attitude.class);

            // GenericSpacecraft
            addArchetype(GenericSpacecraft.class.getName(), Satellite.class.getName(), RenderFlags.class);

            // Spacecraft
            addArchetype(Spacecraft.class.getName(), GenericSpacecraft.class.getName(), MotorEngine.class);

            // Billboard
            addArchetype(Billboard.class.getName(), ModelBody.class.getName(), Fade.class);

            // VertsObject
            addArchetype(VertsObject.class.getName(), SceneGraphNode.class.getName(), Verts.class);

            // Polyline
            addArchetype(Polyline.class.getName(), VertsObject.class.getName(), Arrow.class);

            // Orbit
            addArchetype(Orbit.class.getName(), Polyline.class.getName(), Trajectory.class, RefSysTransform.class);

            // HeliotropicOrbit
            addArchetype(HeliotropicOrbit.class.getName(), Orbit.class.getName(), Heliotropic.class);

            // FadeNode
            addArchetype(FadeNode.class.getName(), SceneGraphNode.class.getName(), Fade.class, Label.class, DatasetDescription.class, Highlight.class);

            // BackgroundModel
            addArchetype(BackgroundModel.class.getName(), FadeNode.class.getName(), RefSysTransform.class, Model.class, Label.class, Coordinates.class, RenderType.class);

            // SphericalGrid
            addArchetype(SphericalGrid.class.getName(), BackgroundModel.class.getName(), GridUV.class);

            // RecursiveGrid
            addArchetype(RecursiveGrid.class.getName(), SceneGraphNode.class.getName(), GridRecursive.class, Fade.class, RefSysTransform.class, Model.class, Label.class, RenderType.class);

            // BillboardGroup
            addArchetype(BillboardGroup.class.getName(), SceneGraphNode.class.getName(), BillboardDatasets.class, RefSysTransform.class, Label.class, Fade.class, Coordinates.class);

            // Text2D
            addArchetype(Text2D.class.getName(), SceneGraphNode.class.getName(), Fade.class, Title.class);

            // Axes
            addArchetype(Axes.class.getName(), SceneGraphNode.class.getName(), Axis.class, RefSysTransform.class);

            // Loc
            addArchetype(Loc.class.getName(), SceneGraphNode.class.getName(), LocationMark.class);

            // Area
            addArchetype(Area.class.getName(), SceneGraphNode.class.getName(), Perimeter.class, AuxVec.class);

            // ParticleGroup
            addArchetype(ParticleGroup.class.getName(), FadeNode.class.getName(), ParticleSet.class);

            // StarGroup
            addArchetype(StarGroup.class.getName(), FadeNode.class.getName(), StarSet.class);

            // Constellation
            addArchetype(Constellation.class.getName(), SceneGraphNode.class.getName(), Constel.class);

            // ConstellationBoundaries
            addArchetype(ConstellationBoundaries.class.getName(), SceneGraphNode.class.getName(), Boundaries.class);

        } else {
            logger.error("World is null, can't initialize archetypes.");
        }
    }

    private void addArchetype(String archetypeName, String parentArchetypeName, Class<? extends Component>... classes) {
        Archetype parent = null;
        if (parentArchetypeName != null && this.archetypes.containsKey(parentArchetypeName)) {
            parent = this.archetypes.get(parentArchetypeName);
        }
        this.archetypes.put(archetypeName, new Archetype(engine, parent, archetypeName, classes));
    }

    private void addArchetype(String archetypeName, Class<? extends Component>... classes) {
        addArchetype(archetypeName, null, classes);
    }

    /**
     * Find a matching archetype given an entity.
     *
     * @param entity The entity.
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

    private void initializeAttributes() {
        if (this.engine != null) {
            this.attributeMap = new HashMap<>();

            // Base
            putAll(Base.class, "id", "name", "names", "opacity", "ct");

            // Body
            putAll(Body.class, "position", "size", "color", "labelcolor");

            // GraphNode
            putAll(GraphNode.class, "parent");

            // Coordinates
            putAll(Coordinates.class, "coordinates");

            // Rotation
            putAll(Rotation.class, "rotation");

            // Celestial
            putAll(Celestial.class, "wikiname", "colorbv");

            // Magnitude
            putAll(Magnitude.class, "appmag", "absmag");

            // SolidAngleThresholds
            putAll(SolidAngle.class, "thresholdNone", "thresholdPoint", "thresholdQuad");

            // Text
            putAll(Text.class, "labelFactor", "labelMax", "textScale");

            // ModelScaffolding
            putAll(ModelScaffolding.class, "refplane", "randomize", "seed", "sizescalefactor", "locvamultiplier", "locthoverfactor", "shadowvalues");

            // Model
            putAll(Model.class, "model");

            // Atmosphere
            putAll(Atmosphere.class, "atmosphere");

            // Cloud
            putAll(Cloud.class, "cloud");

            // RenderFlags
            putAll(RenderFlags.class, "renderquad");

            // Machine
            putAll(MotorEngine.class, "machines");

            // Trajectory
            putAll(Trajectory.class, "provider", "orbit", "model:Orbit", "trail", "newmethod");

            // RefSysTransform
            putAll(RefSysTransform.class, "transformName", "transformFunction", "transformValues");

            // AffineTransformations
            putAll(AffineTransformations.class, "transformations");

            // Fade
            putAll(Fade.class, "fadein", "fadeout", "fade", "fadepc", "positionobjectname");

            // DatasetDescription
            putAll(DatasetDescription.class, "catalogInfo", "cataloginfo");

            // Label
            putAll(Label.class, "label", "label2d", "labelposition");

            // RenderType
            putAll(RenderType.class, "rendergroup");

            // BillboardDataset
            putAll(BillboardDatasets.class, "data:BillboardGroup");

            // Title
            putAll(Title.class, "scale:Text2D", "lines:Text2D", "align:Text2D");

            // Axis
            putAll(Axis.class, "axesColors");

            // LocationMark
            putAll(LocationMark.class, "location", "distFactor");

            // Constel
            putAll(Constel.class, "ids");

            // Boundaries
            putAll(Boundaries.class, "boundaries");

            // ParticleSet
            putAll(ParticleSet.class, "provider:ParticleGroup", "datafile", "providerparams", "factor", "profiledecay", "colornoise", "particlesizelimits");

            // StarSet
            putAll(StarSet.class, "provider:StarGroup", "datafile:StarGroup", "providerparams:StarGroup", "factor:StarGroup", "profiledecay:StarGroup", "colornoise:StarGroup", "particlesizelimits:StarGroup");

            // Attitude
            putAll(Attitude.class, "provider:HeliotropicSatellite", "attitudeLocation");
        } else {
            logger.error("World is null, can't initialize attributes.");
        }
    }

    private void putAll(Class<? extends Component> clazz, String... attributes) {
        for (String attribute : attributes) {
            if (attributeMap.containsKey(attribute)) {
                logger.warn("Attribute already defined: " + attribute);
                throw new RuntimeException("Attribute already defined: " + attribute);
            } else {
                attributeMap.put(attribute, clazz);
            }
        }
    }
}
