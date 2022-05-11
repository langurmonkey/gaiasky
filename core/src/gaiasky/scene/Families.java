package gaiasky.scene;

import com.badlogic.ashley.core.Family;
import gaiasky.scene.component.*;
import gaiasky.scene.component.tag.TagBackgroundModel;

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
            sphericalGrids;

    public Families() {
        roots = Family.all(GraphRoot.class).get();
        graphNodes = Family.all(Base.class, GraphNode.class).get();
        models = Family.all(Base.class, Body.class, Celestial.class, Model.class, ModelScaffolding.class).get();
        particleSets = Family.one(ParticleSet.class, StarSet.class).get();
        particles = Family.all(Base.class, Celestial.class, ProperMotion.class, RenderType.class, ParticleExtra.class).get();
        orbits = Family.all(Trajectory.class, Verts.class).get();
        locations = Family.all(LocationMark.class).get();
        billboardSets = Family.all(BillboardSet.class).get();
        axes = Family.all(Axis.class, RefSysTransform.class).get();
        raymarchings = Family.all(Raymarching.class).get();
        catalogInfos = Family.all(DatasetDescription.class).get();
        orbitalElementSets = Family.all(OrbitElementsSet.class).get();
        fadeNodes = Family.all(Fade.class).get();
        backgroundModels = Family.all(TagBackgroundModel.class).get();
        sphericalGrids = Family.all(GridUV.class).get();
    }

}
