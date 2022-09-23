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

    /** The engine reference. **/
    private Engine engine;

    /** Archetypes map, links old scene graph model objects to artemis archetypes. **/
    protected Map<String, Archetype> archetypes;

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
        Collection<Archetype> archetypes = this.archetypes.values();
        for (Archetype archetype : archetypes) {
            if (archetype.matches(entity)) {
                return archetype;
            }
        }
        return null;
    }

    private String modelName(String className) {
        return "gaiasky.scenegraph." + className;
    }

    public Map<String, Archetype> initializeArchetypes() {
        if (engine != null) {
            this.archetypes = new HashMap<>();

            // SceneGraphNode
            addArchetype(modelName("SceneGraphNode"), Base.class, Body.class, GraphNode.class, Octant.class, Render.class);

            // Universe
            addArchetype(Scene.ROOT_NAME, Base.class, Body.class, GraphNode.class, GraphRoot.class);

            // Celestial
            addArchetype(modelName("CelestialBody"), modelName("SceneGraphNode"), Celestial.class, Magnitude.class,
                    Coordinates.class, Rotation.class, Label.class, SolidAngle.class, Focus.class, Billboard.class);

            // ModelBody
            addArchetype(modelName("ModelBody"), modelName("CelestialBody"), Model.class, ModelScaffolding.class, AffineTransformations.class);

            // Planet
            addArchetype(modelName("Planet"), modelName("ModelBody"), Atmosphere.class, Cloud.class);

            // Particle
            addArchetype(modelName("Particle"), modelName("CelestialBody"), ProperMotion.class, RenderType.class, ParticleExtra.class);

            // Star
            addArchetype(modelName("Star"), modelName("Particle"), Hip.class, Distance.class, Model.class, ModelScaffolding.class);

            // Satellite
            addArchetype(modelName("Satellite"), modelName("ModelBody"), ParentOrientation.class);

            // HeliotropicSatellite
            addArchetype(modelName("HeliotropicSatellite"), modelName("Satellite"), Attitude.class, TagHeliotropic.class);

            // GenericSpacecraft
            addArchetype(modelName("GenericSpacecraft"), modelName("Satellite"), RenderFlags.class);

            // Spacecraft
            addArchetype(modelName("Spacecraft"), modelName("GenericSpacecraft"), MotorEngine.class);

            // StarCluster
            addArchetype(modelName("StarCluster"), modelName("SceneGraphNode"), Model.class, Cluster.class, ProperMotion.class, Label.class, Focus.class, Billboard.class);

            // Billboard
            addArchetype(modelName("Billboard"), modelName("ModelBody"), TagQuaternionOrientation.class, Fade.class);

            // BillboardGalaxy
            addArchetype(modelName("BillboardGalaxy"), modelName("Billboard"), TagBillboardGalaxy.class);

            // VertsObject
            addArchetype(modelName("VertsObject"), modelName("SceneGraphNode"), Verts.class);

            // Polyline
            addArchetype(modelName("Polyline"), modelName("VertsObject"), Arrow.class, Line.class);

            // Orbit
            addArchetype(modelName("Orbit"), modelName("Polyline"), Trajectory.class, RefSysTransform.class);

            // HeliotropicOrbit
            addArchetype(modelName("HeliotropicOrbit"), modelName("Orbit"), TagHeliotropic.class);

            // FadeNode
            addArchetype(modelName("FadeNode"), modelName("SceneGraphNode"), Fade.class, Label.class);

            // GenericCatalog
            addArchetype(modelName("GenericCatalog"), modelName("FadeNode"), DatasetDescription.class, Highlight.class);

            // MeshObject
            addArchetype(modelName("MeshObject"), modelName("FadeNode"), Mesh.class, Model.class, DatasetDescription.class, RefSysTransform.class, AffineTransformations.class);

            // BackgroundModel
            addArchetype(modelName("BackgroundModel"), modelName("FadeNode"), TagBackgroundModel.class, RefSysTransform.class, Model.class, Label.class, Coordinates.class, RenderType.class);

            // SphericalGrid
            addArchetype(modelName("SphericalGrid"), modelName("BackgroundModel"), GridUV.class);

            // RecursiveGrid
            addArchetype(modelName("RecursiveGrid"), modelName("SceneGraphNode"), GridRecursive.class, Fade.class, RefSysTransform.class, Model.class, Label.class, Line.class, RenderType.class);

            // BillboardGroup
            addArchetype(modelName("BillboardGroup"), modelName("SceneGraphNode"), BillboardSet.class, RefSysTransform.class, Label.class, Fade.class, Coordinates.class);

            // Text2D
            addArchetype(modelName("Text2D"), modelName("SceneGraphNode"), Fade.class, Title.class, Label.class);

            // Axes
            addArchetype(modelName("Axes"), modelName("SceneGraphNode"), Axis.class, RefSysTransform.class, Line.class);

            // Loc
            addArchetype(modelName("Loc"), modelName("SceneGraphNode"), LocationMark.class, Label.class);

            // Area
            addArchetype(modelName("Area"), modelName("SceneGraphNode"), Perimeter.class, Line.class, TagNoProcessGraph.class);

            // ParticleGroup
            addArchetype(modelName("ParticleGroup"), modelName("GenericCatalog"), ParticleSet.class, TagNoProcessChildren.class, Focus.class);

            // StarGroup
            addArchetype(modelName("StarGroup"), modelName("GenericCatalog"), StarSet.class, Model.class, Label.class, Line.class, Focus.class, Billboard.class);

            // Constellation
            addArchetype(modelName("Constellation"), modelName("SceneGraphNode"), Constel.class, Line.class, Label.class, TagNoProcessGraph.class);

            // ConstellationBoundaries
            addArchetype(modelName("ConstellationBoundaries"), modelName("SceneGraphNode"), Boundaries.class, Line.class);

            // CosmicRuler
            addArchetype(modelName("CosmicRuler"), modelName("SceneGraphNode"), Ruler.class, Line.class, Label.class);

            // OrbitalElementsGroup
            addArchetype(modelName("OrbitalElementsGroup"), modelName("GenericCatalog"), OrbitElementsSet.class, TagNoProcessChildren.class);

            // Invisible
            addArchetype(modelName("Invisible"), modelName("CelestialBody"), Raymarching.class, TagInvisible.class);

            // OctreeWrapper
            addArchetype(modelName("octreewrapper.OctreeWrapper"), modelName("SceneGraphNode"), Fade.class, DatasetDescription.class, Highlight.class, Octree.class, Octant.class, TagNoProcessChildren.class);

            // ShapeObject
            addArchetype(modelName("ShapeObject"), modelName("SceneGraphNode"), Model.class, Shape.class, Label.class, Line.class);

            // KeyframesPathObject
            addArchetype(modelName("KeyframesPathObject"), modelName("VertsObject"), Keyframes.class, Label.class);

            // VRDeviceModel
            addArchetype(modelName("VRDeviceModel"), modelName("SceneGraphNode"), VRDevice.class, Model.class, Line.class);

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
    private void addArchetype(String archetypeName, Class<? extends Component>... classes) {
        addArchetype(archetypeName, null, classes);
    }
}
