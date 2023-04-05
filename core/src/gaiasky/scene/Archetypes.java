package gaiasky.scene;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import gaiasky.scene.component.*;
import gaiasky.scene.component.tag.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A container for data and logic concerning {@link Archetype}s.
 */
public class Archetypes {

    /** Archetypes map, links old scene graph model objects to artemis archetypes. **/
    protected Map<String, Archetype> archetypes;
    /** The engine reference. **/
    private Engine engine;

    /**
     * Creates a new archetypes container.
     */
    public Archetypes() {
    }

    /**
     * Initializes the archetypes map with an entry for each model object.
     */
    public void initialize(Engine engine) {
        this.engine = engine;
        this.archetypes = initializeArchetypes();
    }

    public boolean contains(String key) {
        return archetypes.containsKey(key);
    }

    /**
     * Gets an archetype by name (key).
     *
     * @param key The name of the archetype.
     *
     * @return The archetype.
     */
    public Archetype get(String key) {
        return archetypes.get(key);
    }

    /**
     * Gets an archetype by class.
     *
     * @param archetypeClass The class of the archetype.
     *
     * @return The archetype.
     */
    public Archetype get(Class archetypeClass) {
        return archetypes.get(archetypeClass.getName());
    }

    /**
     * Finds a matching archetype given an entity.
     *
     * @param entity The entity.
     *
     * @return The matching archetype if it exists, or null if it does not.
     */
    public Archetype findArchetype(Entity entity) {
        // Get entity archetype if it exists.
        var base = Mapper.base.get(entity);
        if (base != null && base.archetype != null) {
            return base.archetype;
        }
        // Find match by looking at components.
        Collection<Archetype> archetypes = this.archetypes.values();
        for (Archetype archetype : archetypes) {
            if (archetype.matches(entity)) {
                return archetype;
            }
        }
        return null;
    }

    /**
     * Generates a list of archetype names from the given class names. Each
     * archetype is submitted twice, with and without the (legacy) package name.
     *
     * @param classNames The class names.
     *
     * @return A list of archetype names.
     */
    private String[] modelNames(String... classNames) {
        String[] result = new String[classNames.length * 2];
        int i = 0;
        for (String name : classNames) {
            result[i++] = name;
            result[i++] = modelName(name);
        }
        return result;
    }

    private String modelName(String className) {
        return "gaiasky.scenegraph." + className;
    }

    public Map<String, Archetype> initializeArchetypes() {
        if (engine != null) {
            this.archetypes = new HashMap<>();

            // SceneGraphNode
            addArchetype(modelNames("SceneGraphNode"), Base.class, Body.class, GraphNode.class, Octant.class, Render.class);

            // Universe
            addArchetype(Scene.ROOT_NAME, Base.class, Body.class, GraphNode.class, GraphRoot.class);

            // Celestial
            addArchetype(modelNames("CelestialBody"), "SceneGraphNode", Celestial.class, Magnitude.class,
                    Coordinates.class, Rotation.class, Label.class, SolidAngle.class, Focus.class, Billboard.class);

            // ModelBody
            addArchetype(modelNames("ModelBody"), "CelestialBody", Model.class, RenderType.class, ModelScaffolding.class, AffineTransformations.class);

            // Planet
            addArchetype(modelNames("Planet"), "ModelBody", Atmosphere.class, Cloud.class);

            // Particle
            addArchetype(modelNames("Particle"), "CelestialBody", ProperMotion.class, RenderType.class, ParticleExtra.class);

            // Star
            addArchetype(modelNames("Star"), "Particle", Hip.class, Distance.class, Model.class, ModelScaffolding.class);

            // Satellite
            addArchetype(modelNames("Satellite"), "ModelBody", ParentOrientation.class);

            // HeliotropicSatellite
            addArchetype(modelNames("HeliotropicSatellite"), "Satellite", Attitude.class, TagHeliotropic.class);

            // GenericSpacecraft
            addArchetype(modelNames("GenericSpacecraft"), "Satellite", RenderFlags.class);

            // Spacecraft
            addArchetype(modelNames("Spacecraft"), "GenericSpacecraft", MotorEngine.class);

            // StarCluster
            addArchetype(modelNames("StarCluster"), "SceneGraphNode", Model.class, Cluster.class, ProperMotion.class, Label.class, Focus.class, Billboard.class);

            // Billboard
            addArchetype(modelNames("Billboard"), "ModelBody", TagQuaternionOrientation.class, Fade.class);

            // BillboardGalaxy
            addArchetype(modelNames("BillboardGalaxy"), "Billboard", TagBillboardGalaxy.class);

            // VertsObject
            addArchetype(modelNames("VertsObject"), "SceneGraphNode", Verts.class);

            // Polyline
            addArchetype(modelNames("Polyline"), "VertsObject", Arrow.class, Line.class);

            // Orbit
            addArchetype(modelNames("Orbit"), "Polyline", Trajectory.class, RefSysTransform.class);

            // HeliotropicOrbit
            addArchetype(modelNames("HeliotropicOrbit"), "Orbit", TagHeliotropic.class);

            // FadeNode
            addArchetype(modelNames("FadeNode"), "SceneGraphNode", Fade.class, Label.class);

            // GenericCatalog
            addArchetype(modelNames("GenericCatalog"), "FadeNode", DatasetDescription.class, Highlight.class, RefSysTransform.class);

            // MeshObject
            addArchetype(modelNames("MeshObject"), "FadeNode", Mesh.class, Model.class, DatasetDescription.class, RefSysTransform.class, AffineTransformations.class);

            // BackgroundModel
            addArchetype(modelNames("BackgroundModel"), "FadeNode", TagBackgroundModel.class, RefSysTransform.class, Model.class, Label.class, Coordinates.class, RenderType.class);

            // SphericalGrid
            addArchetype(modelNames("SphericalGrid"), "BackgroundModel", GridUV.class);

            // RecursiveGrid
            addArchetype(modelNames("RecursiveGrid"), "SceneGraphNode", GridRecursive.class, Fade.class, RefSysTransform.class, Model.class, Label.class, Line.class, RenderType.class);

            // BillboardGroup
            addArchetype(modelNames("BillboardGroup"), "SceneGraphNode", BillboardSet.class, RefSysTransform.class, Label.class, Fade.class, Coordinates.class);

            // Text2D
            addArchetype(modelNames("Text2D"), "SceneGraphNode", Fade.class, Title.class, Label.class);

            // Axes
            addArchetype(modelNames("Axes"), "SceneGraphNode", Axis.class, RefSysTransform.class, Line.class);

            // Loc
            addArchetype(modelNames("Loc"), "SceneGraphNode", LocationMark.class, Label.class);

            // Area
            addArchetype(modelNames("Area"), "SceneGraphNode", Perimeter.class, Line.class, TagNoProcessGraph.class);

            // ParticleGroup
            addArchetype(modelNames("ParticleGroup"), "GenericCatalog", ParticleSet.class, TagNoProcessChildren.class, Focus.class);

            // StarGroup
            addArchetype(modelNames("StarGroup"), "GenericCatalog", StarSet.class, Model.class, Label.class, Line.class, Focus.class, Billboard.class);

            // Constellation
            addArchetype(modelNames("Constellation"), "SceneGraphNode", Constel.class, Line.class, Label.class, TagNoProcessGraph.class);

            // ConstellationBoundaries
            addArchetype(modelNames("ConstellationBoundaries"), "SceneGraphNode", Boundaries.class, Line.class);

            // CosmicRuler
            addArchetype(modelNames("CosmicRuler"), "SceneGraphNode", Ruler.class, Line.class, Label.class);

            // OrbitalElementsGroup
            addArchetype(modelNames("OrbitalElementsGroup"), "GenericCatalog", OrbitElementsSet.class, TagNoProcessChildren.class);

            // Invisible
            addArchetype(modelNames("Invisible"), "CelestialBody", Raymarching.class, TagInvisible.class);

            // OctreeWrapper
            addArchetype(modelNames("octreewrapper.OctreeWrapper"), "SceneGraphNode", Fade.class, DatasetDescription.class, Highlight.class, Octree.class, Octant.class, TagNoProcessChildren.class);

            // Model - a generic model
            addArchetype(modelNames("Model"), "SceneGraphNode", Model.class, RenderType.class, Coordinates.class, SolidAngle.class, RefSysTransform.class, AffineTransformations.class);

            // ShapeObject
            addArchetype(modelNames("ShapeObject"), "Model", Shape.class, Label.class, Line.class);

            // KeyframesPathObject
            addArchetype(modelNames("KeyframesPathObject"), "VertsObject", Keyframes.class, Label.class);

            // VRDeviceModel
            addArchetype(modelNames("VRDeviceModel"), "SceneGraphNode", VRDevice.class, Model.class, Line.class, TagNoClosest.class);

            return archetypes;
        } else {
            throw new RuntimeException("Can't create archetypes: the engine is null!");
        }
    }

    @SafeVarargs
    private void addArchetype(String archetypeName, String parentArchetypeName, Class<? extends Component>... classes) {
        Archetype parent = null;
        if (parentArchetypeName != null && this.archetypes.containsKey(parentArchetypeName)) {
            parent = this.archetypes.get(parentArchetypeName);
        }
        this.archetypes.put(archetypeName, new Archetype(engine, parent, archetypeName, classes));
    }

    @SafeVarargs
    private void addArchetype(String[] archetypeNames, String parentArchetypeName, Class<? extends Component>... classes) {
        for (String archetypeName : archetypeNames) {
            addArchetype(archetypeName, parentArchetypeName, classes);
        }
    }

    @SafeVarargs
    private void addArchetype(String archetypeName, Class<? extends Component>... classes) {
        addArchetype(archetypeName, null, classes);
    }

    @SafeVarargs
    private void addArchetype(String[] archetypeNames, Class<? extends Component>... classes) {
        for (String archetypeName : archetypeNames) {
            addArchetype(archetypeName, classes);
        }
    }
}
