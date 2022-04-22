package gaiasky.scene;

import com.artemis.*;
import gaiasky.scene.component.*;
import gaiasky.scene.system.HelloWorldSystem;
import gaiasky.scenegraph.*;
import gaiasky.util.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a scene, contains and manages the world. The world contains
 * and manages all entities and systems.
 */
public class Scene {
    private static final Logger.Log logger = Logger.getLogger(Scene.class);

    // The world
    public World world;

    // Archetypes map, links old scene graph model objects to artemis archetypes
    protected Map<String, Archetype> archetypes;

    // Maps old attributes to components
    protected Map<String, Class<? extends Component>> attributeMap;

    public Map<String, Archetype> archetypes() { return archetypes; }
    public Map<String, Class<? extends Component>> attributeMap() { return attributeMap; }

    public Scene(){

    }

    public void initialize(){
        WorldConfiguration setup = new WorldConfigurationBuilder()
                .with(new HelloWorldSystem())
                .build();

        world = new World(setup);

        initializeArchetypes();
        initializeAttributes();
    }

    protected void initializeArchetypes() {
        if (this.world != null) {
            this.archetypes = new HashMap<>();

            // SceneGraphNode
            addArchetype(SceneGraphNode.class.getName(),
                    Base.class, Flags.class, Body.class, GraphNode.class, Octant.class);

            // Celestial
            addArchetype(CelestialBody.class.getName(), SceneGraphNode.class.getName(),
                    Celestial.class, Magnitude.class, Coordinates.class,
                    ProperMotion.class, Rotation.class);

            // ModelBody
            addArchetype(ModelBody.class.getName(), CelestialBody.class.getName(),
                    Model.class, ModelScaffolding.class);

            // Planet
            addArchetype(Planet.class.getName(), ModelBody.class.getName(),
                    Atmosphere.class, Cloud.class);

            // Star
            addArchetype(Star.class.getName(), CelestialBody.class.getName(),
                    ProperMotion.class);

            // Satellite
            addArchetype(Satellite.class.getName(), ModelBody.class.getName(),
                    ParentOrientation.class);

            // Gaia
            addArchetype(Gaia.class.getName(), Satellite.class.getName(),
                    Attitude.class);

            // GenericSpacecraft
            addArchetype(GenericSpacecraft.class.getName(), Satellite.class.getName(),
                    RenderFlags.class);

            // Spacecraft
            addArchetype(Spacecraft.class.getName(), GenericSpacecraft.class.getName(),
                    Machine.class);

            // VertsObject
            addArchetype(VertsObject.class.getName(), SceneGraphNode.class.getName(),
                    Verts.class);

            // Polyline
            addArchetype(Polyline.class.getName(), VertsObject.class.getName(),
                    Arrow.class);

            // Orbit
            addArchetype(Orbit.class.getName(), Polyline.class.getName(),
                    Trajectory.class, Transform.class);

            // HeliotropicOrbit
            addArchetype(HeliotropicOrbit.class.getName(), Orbit.class.getName(),
                    Heliotropic.class);

            // FadeNode
            addArchetype(FadeNode.class.getName(), SceneGraphNode.class.getName(),
                    Fade.class, Label.class, DatasetDescription.class, Highlight.class);

            // BackgroundModel
            addArchetype(BackgroundModel.class.getName(), FadeNode.class.getName(),
                    Transform.class, Model.class, Label.class, Coordinates.class,
                    RenderType.class);

            // SphericalGrid
            addArchetype(SphericalGrid.class.getName(), BackgroundModel.class.getName(),
                    GridUV.class);

            // RecursiveGrid
            addArchetype(RecursiveGrid.class.getName(), SceneGraphNode.class.getName(),
                    GridRecursive.class, Fade.class, Transform.class, Model.class,
                    Label.class, RenderType.class);

            // BillboardGroup
            addArchetype(BillboardGroup.class.getName(), SceneGraphNode.class.getName(),
                    BillboardDatasets.class, Transform.class, Label.class, Fade.class,
                    Coordinates.class);

            // Text2D
            addArchetype(Text2D.class.getName(), SceneGraphNode.class.getName(),
                    Fade.class, Title.class);

            // Axes
            addArchetype(Axes.class.getName(), SceneGraphNode.class.getName(),
                    Axis.class, Transform.class);

            // Loc
            addArchetype(Loc.class.getName(), SceneGraphNode.class.getName(),
                    LocationMark.class);

            // Area
            addArchetype(Area.class.getName(), SceneGraphNode.class.getName(),
                    Perimeter.class, AuxVec.class);

            // Constellation
            addArchetype(Constellation.class.getName(), SceneGraphNode.class.getName(),
                    Constel.class);

        } else {
            logger.error("World is null, can't initialize archetypes.");
        }
    }

    private void addArchetype(String archetypeName, String parentArchetypeName, Class<? extends Component>... classes) {
        ArchetypeBuilder builder;
        if(parentArchetypeName != null && this.archetypes.containsKey(parentArchetypeName)) {
            builder = new ArchetypeBuilder(this.archetypes.get(parentArchetypeName));
        } else {
            builder = new ArchetypeBuilder();
        }
        for (Class<? extends Component> c : classes) {
            builder.add(c);
        }
        this.archetypes.put(archetypeName, builder.build(world));
    }

    protected void addArchetype(String archetypeName, Class<? extends Component>... classes) {
        addArchetype(archetypeName, null, classes);
    }

    protected void initializeAttributes() {
        if (this.world != null) {
            this.attributeMap = new HashMap<>();

            // Base
            putAll(Base.class,
                    "id",
                    "name",
                    "names",
                    "opacity",
                    "ct");

            // Body
            putAll(Body.class,
                    "position",
                    "size",
                    "color",
                    "labelcolor");

            // GraphNode
            putAll(GraphNode.class,
                    "parent");

            // Coordinates
            putAll(Coordinates.class,
                    "coordinates");

            // Rotation
            putAll(Rotation.class,
                    "rotation");

            // Celestial
            putAll(Celestial.class,
                    "wikiname",
                    "colorbv");

            // Magnitude
            putAll(Magnitude.class,
                    "appmag",
                    "absmag");

            // ModelScaffolding
            putAll(ModelScaffolding.class,
                    "refplane",
                    "transformations",
                    "randomize",
                    "seed",
                    "sizescalefactor",
                    "locvamultiplier",
                    "locthoverfactor",
                    "shadowvalues");

            // Model
            putAll(Model.class,
                    "model");

            // Atmosphere
            putAll(Atmosphere.class,
                    "atmosphere");
            // Cloud
            putAll(Cloud.class,
                    "cloud");

            // RenderFlags
            putAll(RenderFlags.class,
                    "renderquad");

            // Machine
            putAll(Machine.class,
                    "machines");

            // Trajectory
            putAll(Trajectory.class,
                    "provider",
                    "orbit",
                    "model:Orbit",
                    "trail",
                    "newmethod");

            // Transform
            putAll(Transform.class,
                    "transformName",
                    "transformFunction",
                    "transformValues");

            // Fade
            putAll(Fade.class,
                    "fadein",
                    "fadeout",
                    "positionobjectname");

            // Label
            putAll(Label.class,
                    "label",
                    "label2d",
                    "labelposition");

            // RenderType
            putAll(RenderType.class,
                    "rendergroup");

            // BillboardDataset
            putAll(BillboardDatasets.class,
                    "data:BillboardGroup");

            // Title
            putAll(Title.class,
                    "scale:Text2D",
                    "lines:Text2D",
                    "align:Text2D");

            // Axis
            putAll(Axis.class,
                    "axesColors");

            // LocationMark
            putAll(LocationMark.class,
                    "location",
                    "distFactor");

            // Constel
            putAll(Constel.class,
                    "ids");
        } else {
            logger.error("World is null, can't initialize attributes.");
        }
    }

    protected void putAll(Class<? extends Component> clazz, String... attributes) {
        for (String attribute : attributes) {
            if(attributeMap.containsKey(attribute)) {
                logger.warn("Attribute already defined: " + attribute);
                throw new RuntimeException("Attribute already defined: " + attribute);
            } else {
                attributeMap.put(attribute, clazz);
            }
        }
    }
}
