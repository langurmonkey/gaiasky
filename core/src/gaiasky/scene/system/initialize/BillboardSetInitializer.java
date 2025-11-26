/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.TextureArray;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.GaiaSky;
import gaiasky.data.AssetBean;
import gaiasky.data.group.PointDataProvider;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.entity.FocusHit;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.GalaxyGenerator;
import gaiasky.scene.record.ParticleVector;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.SysUtils;
import gaiasky.util.gdx.TextureArrayLoader;
import gaiasky.util.math.StdRandom;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;

import java.util.List;

public class BillboardSetInitializer extends AbstractInitSystem {

    private final Vector3D D31;
    private final ObjectMap<Entity, BillboardDataset[]> generatedDatasets = new ObjectMap<>();

    public BillboardSetInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        D31 = new Vector3D();
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var label = Mapper.label.get(entity);
        var focus = Mapper.focus.get(entity);
        var bb = Mapper.billboardSet.get(entity);

        // Label.
        label.label = true;
        label.textScale = 3;
        label.labelMax = (float) (2e-3 / Constants.DISTANCE_SCALE_FACTOR);
        label.labelFactor = 0.6f;
        label.renderConsumer = LabelEntityRenderSystem::renderBillboardSet;
        label.renderFunction = LabelView::renderTextBase;
        label.depthBufferConsumer = LabelView::defaultTextDepthBuffer;

        // Focus.
        focus.hitCoordinatesConsumer = FocusHit::addHitBillboardSet;

        reloadData(entity);

        // Generate seed if needed.
        if (!bb.hasSeed()) {
            bb.seed = StdRandom.uniform(999999);
        }

        // Textures.
        var billboards = Mapper.billboardSet.get(entity);
        AssetManager manager = AssetBean.manager();
        billboards.textureArrayName = base.getName() + " TextureArray";
        if (billboards.textureFiles.length == 1) {
            // Single-directory arrays can be reutilized by using the directory name directly!
            billboards.textureArrayName = billboards.textureFiles[0] + " TextureArray";
        }
        var actualFilePaths = SysUtils.gatherFilesExtension(billboards.textureFiles, new String[]{"png", "jpeg", "jpg", "png"});
        // Send to load.
        if (!actualFilePaths.isEmpty()) {
            manager.load(billboards.textureArrayName, TextureArray.class, new TextureArrayLoader.TextureArrayParameter(actualFilePaths));
        }

    }

    @Override
    public void setUpEntity(Entity entity) {
        var billboard = Mapper.billboardSet.get(entity);
        var render = Mapper.render.get(entity);

        // Textures.
        AssetManager manager = AssetBean.manager();
        if (manager.contains(billboard.textureArrayName)) {
            billboard.textureArray = manager.get(billboard.textureArrayName, false);
        }

        // Generate.
        // We need to take care because generation happens only once, but there are potentially two entities: one for
        // the full-resolution buffer and one for the half-resolution buffer. Typically, the full resolution entity
        // is the parent, and has the half-resolution entity as child.
        // We save the generated datasets after generation.
        if (billboard.procedural && billboard.morphology != null) {
            if (generatedDatasets.containsKey(entity)) {
                var datasets = generatedDatasets.get(entity);
                billboard.datasets = datasets;
            } else {
                var graph = Mapper.graph.get(entity);
                Entity full, half;
                if (render.halfResolutionBuffer) {
                    // We are the child.
                    half = entity;
                    full = graph.parent;
                } else {
                    // We are the parent.
                    full = entity;
                    half = graph.getFirstChildOfType(GaiaSky.instance.scene.archetypes().get("BillboardGroup"));
                }
                // Generate two components (full- and half-res). We assume full-res is always the parent.
                GalaxyGenerator gg = new GalaxyGenerator();
                var components = gg.generateGalaxy(billboard.morphology, billboard.seed);
                if (full != null) {
                    var bbFull = Mapper.billboardSet.get(full);
                    bbFull.datasets = components.getFirst();
                    generatedDatasets.put(full, bbFull.datasets);
                }
                if (half != null) {
                    var bbHalf = Mapper.billboardSet.get(half);
                    bbHalf.datasets = components.getSecond();
                    generatedDatasets.put(half, bbHalf.datasets);
                }
            }
        }

        // Transform.
        transformData(entity);
    }

    public void reloadData(Entity entity) {
        try {
            var billboard = Mapper.billboardSet.get(entity);
            if (!billboard.procedural) {
                var provider = new PointDataProvider();
                boolean reload = false;
                for (BillboardDataset dataset : billboard.datasets) {
                    boolean reloadNeeded = dataset.initialize(provider, reload);
                    reload = reload || reloadNeeded;
                }
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass())
                    .error(e);
        }
    }

    public void transformData(Entity entity) {
        var body = Mapper.body.get(entity);
        var coord = Mapper.coordinates.get(entity);
        var transform = Mapper.transform.get(entity);
        var set = Mapper.billboardSet.get(entity);

        // Set static coordinates to position
        coord.coordinates.getEquatorialCartesianCoordinates(null, body.pos);

        // Model
        Vector3D aux = D31;
        Vector3Q pos3b = body.pos;

        // Transform all
        if (set.datasets != null)
            for (BillboardDataset bd : set.datasets) {
                List<IParticleRecord> a = bd.data;
                if (a != null) {
                    for (IParticleRecord iParticleRecord : a) {
                        var pr = (ParticleVector) iParticleRecord;
                        aux.set((float) pr.x(), (float) pr.z(), (float) pr.y());
                        aux.scl(body.size / 2.0)
                                .rotate(-90, 0, 1, 0);
                        if (transform.matrix != null)
                            aux.mul(transform.matrix);
                        aux.add(pos3b);

                        // We can modify the data vector because it is an array.
                        var dat = pr.data();
                        dat[0] = aux.x;
                        dat[1] = aux.y;
                        dat[2] = aux.z;
                    }
                }
            }
    }
}
