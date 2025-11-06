/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene;

import com.badlogic.ashley.core.Family;
import gaiasky.scene.component.*;
import gaiasky.scene.component.tag.*;

/**
 * ECS families.
 */
public class Families {

    public final Family roots,
            graphNodes,
            models,
            satellites,
            meshes,
            datasets,
            particleSets,
            particles,
            orbits,
            orbitsTLE,
            locations,
            billboardSets,
            billboardGalaxies,
            proceduralTriggers,
            axes,
            raymarchings,
            invisibles,
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
            keyframes,
            shapes,
            perimeters,
            verts,
            volumes,
            vrdevices;

    public Families() {
        roots = Family.all(GraphRoot.class).exclude(TagNoProcess.class, TagNoProcessGraph.class).get();
        graphNodes = Family.all(Base.class, GraphNode.class).exclude(TagNoProcess.class, TagOctreeObject.class, TagSetElement.class).get();
        models = Family.all(Base.class, Body.class, Celestial.class, Model.class, ModelScaffolding.class)
                .exclude(Hip.class, TagNoProcess.class)
                .get();
        satellites = Family.all(Base.class, Body.class, Celestial.class, Model.class, ModelScaffolding.class, ParentOrientation.class)
                .exclude(Hip.class, TagNoProcess.class)
                .get();
        datasets = Family.all(Base.class, Body.class, DatasetDescription.class, Highlight.class).exclude(TagNoProcess.class).get();
        meshes = Family.all(Base.class, Body.class, Mesh.class, Model.class).exclude(TagNoProcess.class).get();
        particleSets = Family.one(ParticleSet.class, StarSet.class).exclude(TagNoProcess.class, TagOctreeObject.class).get();
        particles = Family.all(Base.class, Celestial.class, ProperMotion.class, ParticleExtra.class)
                .exclude(TagNoProcess.class)
                .get();
        orbits = Family.all(Trajectory.class, Verts.class).exclude(TagNoProcess.class, TagSetElement.class).get();
        orbitsTLE = Family.all(TLESource.class).exclude(TagNoProcess.class, TagSetElement.class).get();
        locations = Family.all(LocationMark.class).exclude(TagNoProcess.class).get();
        billboardSets = Family.all(BillboardSet.class).exclude(TagNoProcess.class).get();
        billboardGalaxies = Family.all(TagBillboardGalaxy.class).exclude(TagNoProcess.class).get();
        proceduralTriggers = Family.all(ProceduralTrigger.class).exclude(TagNoProcess.class).get();
        axes = Family.all(Axis.class).exclude(TagNoProcess.class).get();
        raymarchings = Family.all(Raymarching.class).exclude(TagNoProcess.class).get();
        invisibles = Family.all(TagInvisible.class).exclude(Raymarching.class, TagNoProcess.class).get();
        catalogInfos = Family.all(DatasetDescription.class).exclude(TagNoProcess.class).get();
        gridRecs = Family.all(GridRecursive.class).exclude(TagNoProcess.class).get();
        rulers = Family.all(Ruler.class).exclude(TagNoProcess.class).get();
        orbitalElementSets = Family.all(OrbitElementsSet.class).exclude(TagNoProcess.class).get();
        fadeNodes = Family.all(Fade.class).exclude(TagNoProcess.class, TagOctreeObject.class).get();
        backgroundModels = Family.all(TagBackgroundModel.class).exclude(TagNoProcess.class).get();
        sphericalGrids = Family.all(GridUV.class).exclude(TagNoProcess.class).get();
        clusters = Family.all(Cluster.class).exclude(TagNoProcess.class).get();
        octrees = Family.all(Octree.class).exclude(TagNoProcess.class).get();
        constellations = Family.all(Constel.class).exclude(TagNoProcess.class).get();
        boundaries = Family.all(Boundaries.class).exclude(TagNoProcess.class).get();
        keyframes = Family.all(Keyframes.class).exclude(TagNoProcess.class).get();
        shapes = Family.all(Shape.class).exclude(TagNoProcess.class).get();
        perimeters = Family.all(Perimeter.class).exclude(TagNoProcess.class).get();
        verts = Family.all(Verts.class).exclude(Keyframes.class, Trajectory.class, TagNoProcess.class).get();
        volumes = Family.all(Volume.class).exclude(TagNoProcess.class).get();
        vrdevices = Family.one(VRDevice.class, TagVRUI.class).exclude(TagNoProcess.class).get();
    }

}
