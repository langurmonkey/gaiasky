/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene;

import com.badlogic.ashley.core.ComponentMapper;
import gaiasky.scene.component.*;
import gaiasky.scene.component.tag.*;

/**
 * Contains component mappers for many types.
 */
public class Mapper {
    // Data components
    public static final ComponentMapper<Base> base = ComponentMapper.getFor(Base.class);
    public static final ComponentMapper<GraphNode> graph = ComponentMapper.getFor(GraphNode.class);
    public static final ComponentMapper<Render> render = ComponentMapper.getFor(Render.class);
    public static final ComponentMapper<RenderFlags> renderFlags = ComponentMapper.getFor(RenderFlags.class);
    public static final ComponentMapper<Id> id = ComponentMapper.getFor(Id.class);
    public static final ComponentMapper<Hip> hip = ComponentMapper.getFor(Hip.class);
    public static final ComponentMapper<Body> body = ComponentMapper.getFor(Body.class);
    public static final ComponentMapper<Celestial> celestial = ComponentMapper.getFor(Celestial.class);
    public static final ComponentMapper<Coordinates> coordinates = ComponentMapper.getFor(Coordinates.class);
    public static final ComponentMapper<Label> label = ComponentMapper.getFor(Label.class);
    public static final ComponentMapper<Orientation> orientation = ComponentMapper.getFor(Orientation.class);
    public static final ComponentMapper<RefSysTransform> transform = ComponentMapper.getFor(RefSysTransform.class);
    public static final ComponentMapper<AffineTransformations> affine = ComponentMapper.getFor(AffineTransformations.class);
    public static final ComponentMapper<Constel> constel = ComponentMapper.getFor(Constel.class);
    public static final ComponentMapper<Boundaries> bound = ComponentMapper.getFor(Boundaries.class);
    public static final ComponentMapper<Octree> octree = ComponentMapper.getFor(Octree.class);
    public static final ComponentMapper<Octant> octant = ComponentMapper.getFor(Octant.class);
    public static final ComponentMapper<Fade> fade = ComponentMapper.getFor(Fade.class);
    public static final ComponentMapper<Focus> focus = ComponentMapper.getFor(Focus.class);
    public static final ComponentMapper<Highlight> highlight = ComponentMapper.getFor(Highlight.class);
    public static final ComponentMapper<DatasetDescription> datasetDescription = ComponentMapper.getFor(DatasetDescription.class);
    public static final ComponentMapper<ParticleSet> particleSet = ComponentMapper.getFor(ParticleSet.class);
    public static final ComponentMapper<StarSet> starSet = ComponentMapper.getFor(StarSet.class);
    public static final ComponentMapper<Magnitude> magnitude = ComponentMapper.getFor(Magnitude.class);
    public static final ComponentMapper<ParticleExtra> extra = ComponentMapper.getFor(ParticleExtra.class);
    public static final ComponentMapper<Distance> distance = ComponentMapper.getFor(Distance.class);
    public static final ComponentMapper<ProperMotion> pm = ComponentMapper.getFor(ProperMotion.class);
    public static final ComponentMapper<Model> model = ComponentMapper.getFor(Model.class);
    public static final ComponentMapper<Mesh> mesh = ComponentMapper.getFor(Mesh.class);
    public static final ComponentMapper<Shape> shape = ComponentMapper.getFor(Shape.class);
    public static final ComponentMapper<Billboard> billboard = ComponentMapper.getFor(Billboard.class);
    public static final ComponentMapper<BillboardSet> billboardSet = ComponentMapper.getFor(BillboardSet.class);
    public static final ComponentMapper<SolidAngle> sa = ComponentMapper.getFor(SolidAngle.class);
    public static final ComponentMapper<Line> line = ComponentMapper.getFor(Line.class);
    public static final ComponentMapper<Arrow> arrow = ComponentMapper.getFor(Arrow.class);
    public static final ComponentMapper<Atmosphere> atmosphere = ComponentMapper.getFor(Atmosphere.class);
    public static final ComponentMapper<Cloud> cloud = ComponentMapper.getFor(Cloud.class);
    public static final ComponentMapper<ModelScaffolding> modelScaffolding = ComponentMapper.getFor(ModelScaffolding.class);
    public static final ComponentMapper<MotorEngine> engine = ComponentMapper.getFor(MotorEngine.class);
    public static final ComponentMapper<RenderType> renderType = ComponentMapper.getFor(RenderType.class);
    public static final ComponentMapper<LocationMark> loc = ComponentMapper.getFor(LocationMark.class);
    public static final ComponentMapper<Axis> axis = ComponentMapper.getFor(Axis.class);
    public static final ComponentMapper<Cluster> cluster = ComponentMapper.getFor(Cluster.class);
    public static final ComponentMapper<SingleMatrix> matrix = ComponentMapper.getFor(SingleMatrix.class);
    public static final ComponentMapper<SingleTexture> texture = ComponentMapper.getFor(SingleTexture.class);
    public static final ComponentMapper<Trajectory> trajectory = ComponentMapper.getFor(Trajectory.class);
    public static final ComponentMapper<TLESource> tle = ComponentMapper.getFor(TLESource.class);
    public static final ComponentMapper<Verts> verts = ComponentMapper.getFor(Verts.class);
    public static final ComponentMapper<OrbitElementsSet> orbitElementsSet = ComponentMapper.getFor(OrbitElementsSet.class);
    public static final ComponentMapper<ParentOrientation> parentOrientation = ComponentMapper.getFor(ParentOrientation.class);
    public static final ComponentMapper<Raymarching> raymarching = ComponentMapper.getFor(Raymarching.class);
    public static final ComponentMapper<GridUV> grid = ComponentMapper.getFor(GridUV.class);
    public static final ComponentMapper<GridRecursive> gridRec = ComponentMapper.getFor(GridRecursive.class);
    public static final ComponentMapper<Ruler> ruler = ComponentMapper.getFor(Ruler.class);
    public static final ComponentMapper<Keyframes> keyframes = ComponentMapper.getFor(Keyframes.class);
    public static final ComponentMapper<Perimeter> perimeter = ComponentMapper.getFor(Perimeter.class);
    public static final ComponentMapper<VRDevice> vr = ComponentMapper.getFor(VRDevice.class);
    public static final ComponentMapper<Volume> volume = ComponentMapper.getFor(Volume.class);

    // Tags
    public static final ComponentMapper<TagCopy> tagCopy = ComponentMapper.getFor(TagCopy.class);
    public static final ComponentMapper<TagBackgroundModel> tagBackgroundModel = ComponentMapper.getFor(TagBackgroundModel.class);
    public static final ComponentMapper<TagBillboardSimple> tagBillboard = ComponentMapper.getFor(TagBillboardSimple.class);
    public static final ComponentMapper<TagHeliotropic> tagHeliotropic = ComponentMapper.getFor(TagHeliotropic.class);
    public static final ComponentMapper<TagNoProcess> tagNoProcess = ComponentMapper.getFor(TagNoProcess.class);
    public static final ComponentMapper<TagNoProcessChildren> tagNoProcessChildren = ComponentMapper.getFor(TagNoProcessChildren.class);
    public static final ComponentMapper<TagSetElement> tagSetElement = ComponentMapper.getFor(TagSetElement.class);
    public static final ComponentMapper<TagInvisible> tagInvisible = ComponentMapper.getFor(TagInvisible.class);
    public static final ComponentMapper<TagBillboardGalaxy> tagBillboardGalaxy = ComponentMapper.getFor(TagBillboardGalaxy.class);
    public static final ComponentMapper<TagNoClosest> tagNoClosest = ComponentMapper.getFor(TagNoClosest.class);
    public static final ComponentMapper<TagVRUI> tagVRUI = ComponentMapper.getFor(TagVRUI.class);
    public static final ComponentMapper<TagOctreeObject> tagOctreeObject = ComponentMapper.getFor(TagOctreeObject.class);

}
