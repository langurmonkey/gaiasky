package gaiasky.scene;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import gaiasky.scene.component.*;
import gaiasky.scenegraph.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Initializes the archetypes map.
 */
public class ArchetypeInitializer {

    private final Engine engine;
    private final Map<String, Archetype> archetypes;

    public ArchetypeInitializer(final Engine engine) {
        this.engine = engine;
        this.archetypes = new HashMap<>();
    }

    public Map<String, Archetype> initializeArchetypes() {

        if (this.engine != null) {
            // SceneGraphNode
            addArchetype(SceneGraphNode.class.getName(), Base.class, Body.class, GraphNode.class, Octant.class);

            // Universe
            addArchetype(Scene.ROOT_NAME, Base.class, Body.class, GraphNode.class, GraphRoot.class);

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

            // StarCluster
            addArchetype(StarCluster.class.getName(), SceneGraphNode.class.getName(), Model.class, Cluster.class, ProperMotion.class);

            // Billboard
            addArchetype(Billboard.class.getName(), ModelBody.class.getName(), Fade.class);

            // BillboardGalaxy
            addArchetype(BillboardGalaxy.class.getName(), Billboard.class.getName());

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
            addArchetype(BillboardGroup.class.getName(), SceneGraphNode.class.getName(), BillboardSet.class, RefSysTransform.class, Label.class, Fade.class, Coordinates.class);

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

            // CosmicRuler
            addArchetype(CosmicRuler.class.getName(), SceneGraphNode.class.getName(), Ruler.class);

            // GenericCatalog
            addArchetype(GenericCatalog.class.getName(), FadeNode.class.getName());


            return archetypes;
        } else {
            throw new RuntimeException("Can't create archetypes: the engine is null!");
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
}
