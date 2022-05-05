package gaiasky.scene;

import com.badlogic.ashley.core.ComponentMapper;
import gaiasky.scene.component.*;

/**
 * Centralized repository of component mappers.
 */
public class Mapper {
    public static final ComponentMapper<Base> base = ComponentMapper.getFor(Base.class);
    public static final ComponentMapper<Id> id = ComponentMapper.getFor(Id.class);
    public static final ComponentMapper<Hip> hip = ComponentMapper.getFor(Hip.class);
    public static final ComponentMapper<Body> body = ComponentMapper.getFor(Body.class);
    public static final ComponentMapper<Celestial> celestial = ComponentMapper.getFor(Celestial.class);
    public static final ComponentMapper<Coordinates> coordinates = ComponentMapper.getFor(Coordinates.class);
    public static final ComponentMapper<Label> label = ComponentMapper.getFor(Label.class);
    public static final ComponentMapper<RefSysTransform> transform = ComponentMapper.getFor(RefSysTransform.class);
    public static final ComponentMapper<AffineTransformations> affine = ComponentMapper.getFor(AffineTransformations.class);
    public static final ComponentMapper<GraphNode> graph = ComponentMapper.getFor(GraphNode.class);
    public static final ComponentMapper<Octant> octant = ComponentMapper.getFor(Octant.class);
    public static final ComponentMapper<Fade> fade = ComponentMapper.getFor(Fade.class);
    public static final ComponentMapper<Rotation> rotation = ComponentMapper.getFor(Rotation.class);
    public static final ComponentMapper<DatasetDescription> datasetDescription = ComponentMapper.getFor(DatasetDescription.class);
    public static final ComponentMapper<ParticleSet> particleSet = ComponentMapper.getFor(ParticleSet.class);
    public static final ComponentMapper<StarSet> starSet = ComponentMapper.getFor(StarSet.class);
    public static final ComponentMapper<Magnitude> magnitude = ComponentMapper.getFor(Magnitude.class);
    public static final ComponentMapper<ParticleExtra> extra = ComponentMapper.getFor(ParticleExtra.class);
    public static final ComponentMapper<Distance> distance = ComponentMapper.getFor(Distance.class);
    public static final ComponentMapper<ProperMotion> pm = ComponentMapper.getFor(ProperMotion.class);
    public static final ComponentMapper<Model> model = ComponentMapper.getFor(Model.class);
    public static final ComponentMapper<SolidAngle> sa = ComponentMapper.getFor(SolidAngle.class);
    public static final ComponentMapper<Text> text = ComponentMapper.getFor(Text.class);
    public static final ComponentMapper<Atmosphere> atmosphere = ComponentMapper.getFor(Atmosphere.class);
    public static final ComponentMapper<Cloud> cloud = ComponentMapper.getFor(Cloud.class);
    public static final ComponentMapper<ModelScaffolding> modelScaffolding = ComponentMapper.getFor(ModelScaffolding.class);
    public static final ComponentMapper<Attitude> attitude = ComponentMapper.getFor(Attitude.class);
    public static final ComponentMapper<MotorEngine> engine = ComponentMapper.getFor(MotorEngine.class);
    public static final ComponentMapper<RenderType> render = ComponentMapper.getFor(RenderType.class);
    public static final ComponentMapper<LocationMark> loc = ComponentMapper.getFor(LocationMark.class);
    public static final ComponentMapper<BillboardSet> billboardSet = ComponentMapper.getFor(BillboardSet.class);
    public static final ComponentMapper<Axis> axis = ComponentMapper.getFor(Axis.class);
    public static final ComponentMapper<Cluster> cluster = ComponentMapper.getFor(Cluster.class);
    public static final ComponentMapper<SingleMatrix> matrix = ComponentMapper.getFor(SingleMatrix.class);
    public static final ComponentMapper<SingleTexture> texture = ComponentMapper.getFor(SingleTexture.class);
    public static final ComponentMapper<Trajectory> trajectory = ComponentMapper.getFor(Trajectory.class);
    public static final ComponentMapper<Verts> verts = ComponentMapper.getFor(Verts.class);
    public static final ComponentMapper<OrbitElementsSet> orbitElementsSet = ComponentMapper.getFor(OrbitElementsSet.class);
    public static final ComponentMapper<ParentOrientation> parentOrientation = ComponentMapper.getFor(ParentOrientation.class);
    public static final ComponentMapper<Raymarching> raymarching = ComponentMapper.getFor(Raymarching.class);

}
