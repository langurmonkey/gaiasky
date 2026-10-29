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
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.gdx.graphics.GalaxyVolumeBuilder;
import gaiasky.render.gdx.graphics.GalaxyVolumeGenerator;
import gaiasky.scene.entity.GalaxyVolumeEntity;
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
import gaiasky.render.gdx.TextureArrayLoader;
import gaiasky.util.math.StdRandom;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;

import java.util.List;

/**
 * Initializes billboard set entities, including their procedural generation and texture management.
 */
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
        if (billboard.textureArrayName != null && manager.contains(billboard.textureArrayName)) {
            // Entities created at runtime are set up right after their array is queued, so wait for it here.
            billboard.textureArray = manager.finishLoadingAsset(billboard.textureArrayName);
            if (billboard.textureArray == null) {
                logger.error("Could not load texture array: " + billboard.textureArrayName);
            }
        }

        // Generate.
        // We need to take care because generation happens only once, but there are potentially two entities: one for
        // the full-resolution buffer and one for the half-resolution buffer. Typically, the full resolution entity
        // is the parent, and has the half-resolution entity as child.
        // We save the generated datasets after generation.
        if (billboard.procedural && billboard.morphology != null) {
            if (generatedDatasets.containsKey(entity)) {
                billboard.datasets = generatedDatasets.get(entity);
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
                var gg = new GalaxyGenerator();
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

        // Volume path: the channels with an analytic field behind them (gas, bulge, dust) are voxelised
        // into 3D textures and raymarched by a child entity, instead of being splatted into billboards.
        // The datasets that go to a volume are dropped from this set so that they are not drawn twice.
        if (billboard.procedural && billboard.datasets != null) {
            buildVolumeChild(entity, billboard);
        }

        // Transform.
        transformData(entity);
    }

    /**
     * Generates the galaxy's volume textures, if any of its datasets asks for them, and attaches the
     * child entity that renders them.
     * <p>
     * Only the entity that owns the eligible datasets generates: that is normally the half-resolution
     * one, but a descriptor is free to put gas in the full-resolution entity. The volume is parented to
     * the galaxy's full-resolution entity either way, so it inherits the transform and the fade of a
     * single object.
     *
     * @param entity    The entity whose datasets are being set up.
     * @param billboard Its billboard set.
     */
    private void buildVolumeChild(Entity entity, gaiasky.scene.component.BillboardSet billboard) {
        boolean anyVolume = false;
        for (var dataset : billboard.datasets) {
            if (dataset != null && dataset.isVolume()) {
                anyVolume = true;
                break;
            }
        }
        if (!anyVolume) {
            return;
        }
        if (!GalaxyVolumeGenerator.isSupported()) {
            // No compute shaders: leave the billboards alone rather than draw nothing at all.
            logger.warn("Volume generation needs compute shaders, falling back to billboards for " + Mapper.base.get(entity).getName());
            return;
        }

        var graph = Mapper.graph.get(entity);
        var render = Mapper.render.get(entity);
        Entity owner = render.halfResolutionBuffer && graph.parent != null ? graph.parent : entity;
        var ownerName = Mapper.base.get(owner).getName();
        if (hasVolumeChild(owner, ownerName)) {
            // Already built: reloading a scene must not stack volumes on top of each other.
            return;
        }

        double sizePc = Mapper.body.get(entity).size * Constants.U_TO_PC;
        var textures = GalaxyVolumeBuilder.generate(billboard.datasets, sizePc, GaiaSky.settings().graphics.quality);
        if (textures[GalaxyVolumeBuilder.SLOT_EMISSIVE] == null && textures[GalaxyVolumeBuilder.SLOT_OCCLUSIVE] == null) {
            logger.warn("Volume generation produced no textures for " + ownerName + ", keeping billboards");
            return;
        }

        var volume = GalaxyVolumeEntity.create(GaiaSky.instance.scene, owner,
                                               textures[GalaxyVolumeBuilder.SLOT_EMISSIVE],
                                               textures[GalaxyVolumeBuilder.SLOT_OCCLUSIVE]);
        if (volume == null) {
            return;
        }

        GaiaSky.instance.scene.initializeEntity(volume);
        GaiaSky.instance.scene.setUpEntity(volume);
        EventManager.publish(Event.SCENE_ADD_OBJECT_NO_POST_CMD, this, volume, false);

        // The billboards of this set would otherwise be drawn on top of the volume they became.
        var remaining = new java.util.ArrayList<BillboardDataset>(billboard.datasets.length);
        for (var dataset : billboard.datasets) {
            if (dataset == null || !dataset.isVolume()) {
                remaining.add(dataset);
            }
        }
        billboard.datasets = remaining.toArray(new BillboardDataset[0]);
    }

    /** Whether the galaxy already has a volume child, by name. **/
    private boolean hasVolumeChild(Entity owner, String ownerName) {
        var children = Mapper.graph.get(owner).children;
        if (children == null) {
            return false;
        }
        String volumeName = ownerName + GalaxyVolumeEntity.NAME_SUFFIX;
        for (var child : children) {
            if (child != null && Mapper.base.has(child) && volumeName.equals(Mapper.base.get(child).getName())) {
                return true;
            }
        }
        return false;
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
