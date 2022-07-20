package gaiasky.scene;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import gaiasky.scene.component.*;
import gaiasky.scene.component.Billboard;
import gaiasky.scene.component.tag.*;
import gaiasky.scenegraph.*;
import gaiasky.scenegraph.octreewrapper.OctreeWrapper;

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

    public Map<String, Archetype> initializeArchetypes() {
        if (engine != null) {
            this.archetypes = new HashMap<>();

            // SceneGraphNode
            addArchetype(SceneGraphNode.class.getName(), Base.class, Body.class, GraphNode.class, Octant.class, Render.class);

            // Universe
            addArchetype(Scene.ROOT_NAME, Base.class, Body.class, GraphNode.class, GraphRoot.class);

            // Celestial
            addArchetype(CelestialBody.class.getName(), SceneGraphNode.class.getName(), Celestial.class, Magnitude.class,
                    Coordinates.class, Rotation.class, Label.class, SolidAngle.class, Focus.class, Billboard.class);

            // ModelBody
            addArchetype(ModelBody.class.getName(), CelestialBody.class.getName(), Model.class, ModelScaffolding.class, AffineTransformations.class);

            // Planet
            addArchetype(Planet.class.getName(), ModelBody.class.getName(), Atmosphere.class, Cloud.class);

            // Particle
            addArchetype(Particle.class.getName(), CelestialBody.class.getName(), ProperMotion.class, RenderType.class, ParticleExtra.class);

            // Star
            addArchetype(Star.class.getName(), Particle.class.getName(), Hip.class, Distance.class, Model.class);

            // Satellite
            addArchetype(Satellite.class.getName(), ModelBody.class.getName(), ParentOrientation.class);

            // HeliotropicSatellite
            addArchetype(HeliotropicSatellite.class.getName(), Satellite.class.getName(), Attitude.class, TagHeliotropic.class);

            // GenericSpacecraft
            addArchetype(GenericSpacecraft.class.getName(), Satellite.class.getName(), RenderFlags.class);

            // Spacecraft
            addArchetype(Spacecraft.class.getName(), GenericSpacecraft.class.getName(), MotorEngine.class);

            // StarCluster
            addArchetype(StarCluster.class.getName(), SceneGraphNode.class.getName(), Model.class, Cluster.class, ProperMotion.class, Label.class, Focus.class, Billboard.class);

            // Billboard
            addArchetype(gaiasky.scenegraph.Billboard.class.getName(), ModelBody.class.getName(), TagQuaternionOrientation.class, Fade.class);

            // BillboardGalaxy
            addArchetype(BillboardGalaxy.class.getName(), gaiasky.scenegraph.Billboard.class.getName(), TagBillboardGalaxy.class);

            // VertsObject
            addArchetype(VertsObject.class.getName(), SceneGraphNode.class.getName(), Verts.class);

            // Polyline
            addArchetype(Polyline.class.getName(), VertsObject.class.getName(), Arrow.class);

            // Orbit
            addArchetype(Orbit.class.getName(), Polyline.class.getName(), Trajectory.class, RefSysTransform.class, Line.class);

            // HeliotropicOrbit
            addArchetype(HeliotropicOrbit.class.getName(), Orbit.class.getName(), TagHeliotropic.class);

            // FadeNode
            addArchetype(FadeNode.class.getName(), SceneGraphNode.class.getName(), Fade.class, Label.class, DatasetDescription.class, Highlight.class);

            // MeshObject
            addArchetype(MeshObject.class.getName(), SceneGraphNode.class.getName(), Mesh.class, Fade.class, Label.class, Model.class, DatasetDescription.class, RefSysTransform.class, AffineTransformations.class);

            // BackgroundModel
            addArchetype(BackgroundModel.class.getName(), FadeNode.class.getName(), TagBackgroundModel.class, RefSysTransform.class, Model.class, Label.class, Coordinates.class, RenderType.class);

            // SphericalGrid
            addArchetype(SphericalGrid.class.getName(), BackgroundModel.class.getName(), GridUV.class);

            // RecursiveGrid
            addArchetype(RecursiveGrid.class.getName(), SceneGraphNode.class.getName(), GridRecursive.class, Fade.class, RefSysTransform.class, Model.class, Label.class, Line.class, RenderType.class);

            // BillboardGroup
            addArchetype(BillboardGroup.class.getName(), SceneGraphNode.class.getName(), BillboardSet.class, RefSysTransform.class, Label.class, Fade.class, Coordinates.class);

            // Text2D
            addArchetype(Text2D.class.getName(), SceneGraphNode.class.getName(), Fade.class, Title.class, Label.class);

            // Axes
            addArchetype(Axes.class.getName(), SceneGraphNode.class.getName(), Axis.class, RefSysTransform.class, Line.class);

            // Loc
            addArchetype(Loc.class.getName(), SceneGraphNode.class.getName(), LocationMark.class, Label.class);

            // Area
            addArchetype(Area.class.getName(), SceneGraphNode.class.getName(), Perimeter.class);

            // ParticleGroup
            addArchetype(ParticleGroup.class.getName(), FadeNode.class.getName(), ParticleSet.class, TagNoProcessChildren.class, Focus.class);

            // StarGroup
            addArchetype(StarGroup.class.getName(), FadeNode.class.getName(), StarSet.class, Model.class, Label.class, Line.class, Focus.class, Billboard.class);

            // Constellation
            addArchetype(Constellation.class.getName(), SceneGraphNode.class.getName(), Constel.class, Line.class, Label.class);

            // ConstellationBoundaries
            addArchetype(ConstellationBoundaries.class.getName(), SceneGraphNode.class.getName(), Boundaries.class, Line.class);

            // CosmicRuler
            addArchetype(CosmicRuler.class.getName(), SceneGraphNode.class.getName(), Ruler.class, Line.class, Label.class);

            // GenericCatalog
            addArchetype(GenericCatalog.class.getName(), FadeNode.class.getName());

            // OrbitalElementsGroup
            addArchetype(OrbitalElementsGroup.class.getName(), GenericCatalog.class.getName(), OrbitElementsSet.class, TagNoProcessChildren.class);

            // Invisible
            addArchetype(Invisible.class.getName(), CelestialBody.class.getName(), Raymarching.class, TagInvisible.class);

            // AbstractOctreeWrapper
            addArchetype(OctreeWrapper.class.getName(), SceneGraphNode.class.getName(), Fade.class, Octree.class, Octant.class, DatasetDescription.class, TagNoProcessChildren.class);

            // ShapeObject
            addArchetype(ShapeObject.class.getName(), SceneGraphNode.class.getName(), Model.class, Shape.class, Label.class, Line.class);

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
