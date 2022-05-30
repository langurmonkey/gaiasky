package gaiasky.scene;

import com.badlogic.ashley.core.Family;
import gaiasky.scene.component.*;
import gaiasky.scene.component.tag.TagBackgroundModel;
import gaiasky.scene.component.tag.TagOctreeObject;

/**
 * A simple utility object that collects the most-used families.
 */
public class Families {

    public final Family roots,
            graphNodes,
            models,
            particleSets,
            particles,
            orbits,
            locations,
            billboardSets,
            axes,
            raymarchings,
            catalogInfos,
            orbitalElementSets,
            fadeNodes,
            backgroundModels,
            sphericalGrids,
            octrees;

    public Families() {
        roots = Family.all(GraphRoot.class).get();
        graphNodes = Family.all(Base.class, GraphNode.class).exclude(TagOctreeObject.class).get();
        models = Family.all(Base.class, Body.class, Celestial.class, Model.class, ModelScaffolding.class).get();
        particleSets = Family.one(ParticleSet.class, StarSet.class).exclude(TagOctreeObject.class).get();
        particles = Family.all(Base.class, Celestial.class, ProperMotion.class, RenderType.class, ParticleExtra.class).get();
        orbits = Family.all(Trajectory.class, Verts.class).get();
        locations = Family.all(LocationMark.class).get();
        billboardSets = Family.all(BillboardSet.class).get();
        axes = Family.all(Axis.class, RefSysTransform.class).get();
        raymarchings = Family.all(Raymarching.class).get();
        catalogInfos = Family.all(DatasetDescription.class).get();
        orbitalElementSets = Family.all(OrbitElementsSet.class).get();
        fadeNodes = Family.all(Fade.class).exclude(TagOctreeObject.class).get();
        backgroundModels = Family.all(TagBackgroundModel.class).get();
        sphericalGrids = Family.all(GridUV.class).get();
        octrees = Family.all(Octree.class).get();
    }

}
