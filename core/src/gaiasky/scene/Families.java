package gaiasky.scene;

import com.badlogic.ashley.core.Family;
import gaiasky.scene.component.*;
import gaiasky.scene.component.tag.TagBackgroundModel;
import gaiasky.scene.component.tag.TagBillboardGalaxy;
import gaiasky.scene.component.tag.TagOctreeObject;
import gaiasky.scene.component.tag.TagSetElement;

/**
 * A simple utility object that collects the most-used families.
 */
public class Families {

    public final Family roots,
            graphNodes,
            models,
            meshes,
            particleSets,
            particles,
            orbits,
            locations,
            billboardSets,
            billboardGalaxies,
            axes,
            raymarchings,
            catalogInfos,
            gridRecs,
            rulers,
            orbitalElementSets,
            fadeNodes,
            backgroundModels,
            sphericalGrids,
            clusters,
            octrees,
            constellations,
            boundaries,
            titles;

    public Families() {
        roots = Family.all(GraphRoot.class).get();
        graphNodes = Family.all(Base.class, GraphNode.class).exclude(TagOctreeObject.class, TagSetElement.class).get();
        models = Family.all(Base.class, Body.class, Celestial.class, Model.class, ModelScaffolding.class).get();
        meshes = Family.all(Base.class, Body.class, Mesh.class, Model.class).get();
        particleSets = Family.one(ParticleSet.class, StarSet.class).exclude(TagOctreeObject.class).get();
        particles = Family.all(Base.class, Celestial.class, ProperMotion.class, RenderType.class, ParticleExtra.class).get();
        orbits = Family.all(Trajectory.class, Verts.class).exclude(TagSetElement.class).get();
        locations = Family.all(LocationMark.class).get();
        billboardSets = Family.all(BillboardSet.class).get();
        billboardGalaxies = Family.all(TagBillboardGalaxy.class).get();
        axes = Family.all(Axis.class).get();
        raymarchings = Family.all(Raymarching.class).get();
        catalogInfos = Family.all(DatasetDescription.class).get();
        gridRecs = Family.all(GridRecursive.class).get();
        rulers = Family.all(Ruler.class).get();
        orbitalElementSets = Family.all(OrbitElementsSet.class).get();
        fadeNodes = Family.all(Fade.class).exclude(TagOctreeObject.class).get();
        backgroundModels = Family.all(TagBackgroundModel.class).get();
        sphericalGrids = Family.all(GridUV.class).get();
        clusters = Family.all(Cluster.class).get();
        octrees = Family.all(Octree.class).get();
        constellations = Family.all(Constel.class).get();
        boundaries = Family.all(Boundaries.class).get();
        titles = Family.all(Title.class).get();
    }

}
