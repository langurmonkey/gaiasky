package gaiasky.scene;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.PooledEngine;
import gaiasky.scene.component.*;
import gaiasky.scenegraph.*;
import gaiasky.util.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a scene, contains and manages the engine. The engine contains
 * and manages all entities and systems in the world.
 */
public class Scene {
    private static final Logger.Log logger = Logger.getLogger(Scene.class);

    // The world
    public Engine engine;

    // Archetypes map, links old scene graph model objects to artemis archetypes
    protected Map<String, Archetype> archetypes;

    // Systems that load data -- run a the very beginning
    protected Set<EntitySystem> loadingSystems;
    // Systems that initialize entities -- run after loading systems
    protected Set<EntitySystem> initSystems;
    // Systems that contain update logic -- run every cycle
    protected Set<EntitySystem> updateSystems;
    // Systems that contain render logic -- run every cycle
    protected Set<EntitySystem> renderSystems;
    // Systems that dispose resources -- run at the end
    protected Set<EntitySystem> disposeSystems;
    // All systems container
    protected Set<Set<EntitySystem>> allSystems;

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
    }

    /**
     * Enables the given groups of systems.
     *
     * @param systemGroups An array with the system groups to enable.
     */
    public void enableSystems(Set<EntitySystem>... systemGroups) {
        setEnabled(true, systemGroups);
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
            addArchetype(CelestialBody.class.getName(), SceneGraphNode.class.getName(), Celestial.class, Magnitude.class, Coordinates.class, ProperMotion.class, Rotation.class);

            // ModelBody
            addArchetype(ModelBody.class.getName(), CelestialBody.class.getName(), Model.class, ModelScaffolding.class);

            // Planet
            addArchetype(Planet.class.getName(), ModelBody.class.getName(), Atmosphere.class, Cloud.class);

            // Star
            addArchetype(Star.class.getName(), CelestialBody.class.getName(), ProperMotion.class);

            // Satellite
            addArchetype(Satellite.class.getName(), ModelBody.class.getName(), ParentOrientation.class);

            // Gaia
            addArchetype(Gaia.class.getName(), Satellite.class.getName(), Attitude.class);

            // GenericSpacecraft
            addArchetype(GenericSpacecraft.class.getName(), Satellite.class.getName(), RenderFlags.class);

            // Spacecraft
            addArchetype(Spacecraft.class.getName(), GenericSpacecraft.class.getName(), Machine.class);

            // VertsObject
            addArchetype(VertsObject.class.getName(), SceneGraphNode.class.getName(), Verts.class);

            // Polyline
            addArchetype(Polyline.class.getName(), VertsObject.class.getName(), Arrow.class);

            // Orbit
            addArchetype(Orbit.class.getName(), Polyline.class.getName(), Trajectory.class, Transform.class);

            // HeliotropicOrbit
            addArchetype(HeliotropicOrbit.class.getName(), Orbit.class.getName(), Heliotropic.class);

            // FadeNode
            addArchetype(FadeNode.class.getName(), SceneGraphNode.class.getName(), Fade.class, Label.class, DatasetDescription.class, Highlight.class);

            // BackgroundModel
            addArchetype(BackgroundModel.class.getName(), FadeNode.class.getName(), Transform.class, Model.class, Label.class, Coordinates.class, RenderType.class);

            // SphericalGrid
            addArchetype(SphericalGrid.class.getName(), BackgroundModel.class.getName(), GridUV.class);

            // RecursiveGrid
            addArchetype(RecursiveGrid.class.getName(), SceneGraphNode.class.getName(), GridRecursive.class, Fade.class, Transform.class, Model.class, Label.class, RenderType.class);

            // BillboardGroup
            addArchetype(BillboardGroup.class.getName(), SceneGraphNode.class.getName(), BillboardDatasets.class, Transform.class, Label.class, Fade.class, Coordinates.class);

            // Text2D
            addArchetype(Text2D.class.getName(), SceneGraphNode.class.getName(), Fade.class, Title.class);

            // Axes
            addArchetype(Axes.class.getName(), SceneGraphNode.class.getName(), Axis.class, Transform.class);

            // Loc
            addArchetype(Loc.class.getName(), SceneGraphNode.class.getName(), LocationMark.class);

            // Area
            addArchetype(Area.class.getName(), SceneGraphNode.class.getName(), Perimeter.class, AuxVec.class);

            // Constellation
            addArchetype(Constellation.class.getName(), SceneGraphNode.class.getName(), Constel.class);

            // Constellation
            addArchetype(ConstellationBoundaries.class.getName(), SceneGraphNode.class.getName(), Boundaries.class);

            // ParticleGroup
            addArchetype(ParticleGroup.class.getName(), FadeNode.class.getName(), ParticleSet.class);

            // StarGroup
            addArchetype(StarGroup.class.getName(), FadeNode.class.getName(), StarSet.class);

        } else {
            logger.error("World is null, can't initialize archetypes.");
        }
    }

    private void addArchetype(String archetypeName, String parentArchetypeName, Class<? extends Component>... classes) {
        Archetype parent = null;
        if (parentArchetypeName != null && this.archetypes.containsKey(parentArchetypeName)) {
            parent = this.archetypes.get(parentArchetypeName);
        }
        this.archetypes.put(archetypeName, new Archetype(engine, parent, classes));
    }

    protected void addArchetype(String archetypeName, Class<? extends Component>... classes) {
        addArchetype(archetypeName, null, classes);
    }

    protected void initializeAttributes() {
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

            // ModelScaffolding
            putAll(ModelScaffolding.class, "refplane", "transformations", "randomize", "seed", "sizescalefactor", "locvamultiplier", "locthoverfactor", "shadowvalues");

            // Model
            putAll(Model.class, "model");

            // Atmosphere
            putAll(Atmosphere.class, "atmosphere");
            // Cloud
            putAll(Cloud.class, "cloud");

            // RenderFlags
            putAll(RenderFlags.class, "renderquad");

            // Machine
            putAll(Machine.class, "machines");

            // Trajectory
            putAll(Trajectory.class, "provider", "orbit", "model:Orbit", "trail", "newmethod");

            // Transform
            putAll(Transform.class, "transformName", "transformFunction", "transformValues");

            // Fade
            putAll(Fade.class, "fadein", "fadeout", "positionobjectname");

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
        } else {
            logger.error("World is null, can't initialize attributes.");
        }
    }

    protected void putAll(Class<? extends Component> clazz, String... attributes) {
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
