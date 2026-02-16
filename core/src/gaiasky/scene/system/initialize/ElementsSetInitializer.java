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
import com.badlogic.gdx.utils.Array;
import gaiasky.data.AssetBean;
import gaiasky.data.api.IParticleGroupDataProvider;
import gaiasky.data.group.STILDataProvider;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.OrbitElementsSet;
import gaiasky.scene.component.tag.TagSetElement;
import gaiasky.scene.entity.ElementsSetRadio;
import gaiasky.scene.record.ParticleType;
import gaiasky.util.Logger;
import gaiasky.util.SysUtils;
import gaiasky.util.gdx.TextureArrayLoader;

import java.util.HashMap;

public class ElementsSetInitializer extends AbstractInitSystem {
    public ElementsSetInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {

        var set = Mapper.orbitElementsSet.get(entity);
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        boolean initializeData = set.data == null;

        // Point size.
        if (body.size == 0) {
            body.size = 1f;
        }

        // Data.
        if (initializeData && set.provider != null) {
            // Load data.
            try {
                Class<?> clazz = Class.forName(set.provider);
                IParticleGroupDataProvider provider = (IParticleGroupDataProvider) clazz.getConstructor()
                        .newInstance();
                // By default, do not generate index in particle sets.
                set.setData(provider.loadData(set.datafile, 1));
            } catch (Exception e) {
                Logger.getLogger(this.getClass())
                        .error(e);
                set.data = null;
            }
        }

        // Textures.
        AssetManager manager = AssetBean.manager();
        var actualFilePaths = SysUtils.gatherFilesExtension(set.textureFiles, new String[]{"png", "jpeg", "jpg"});
        // Send to load.
        if (!actualFilePaths.isEmpty()) {
            manager.load(base.getName() + " TextureArray", TextureArray.class, new TextureArrayLoader.TextureArrayParameter(actualFilePaths));
        }

    }

    @Override
    public void setUpEntity(Entity entity) {
        var graph = Mapper.graph.get(entity);
        var set = Mapper.orbitElementsSet.get(entity);
        var base = Mapper.base.get(entity);

        // Textures.
        AssetManager manager = AssetBean.manager();
        if (manager.contains(base.getName() + " TextureArray")) {
            set.textureArray = manager.get(base.getName() + " TextureArray");
        }

        // Check children which need updating every time
        initializeObjectsWithOrbit(graph, set);

        EventManager.instance.subscribe(new ElementsSetRadio(entity, this), Event.GPU_DISPOSE_ORBITAL_ELEMENTS);
    }

    /**
     * Gather the children objects that need to be rendered as an orbit line into a list,
     * for they need to be updated every single frame.
     */
    public void initializeObjectsWithOrbit(GraphNode graph, OrbitElementsSet set) {
        if (set.alwaysUpdate == null) {
            set.alwaysUpdate = new Array<>();
        } else {
            set.alwaysUpdate.clear();
        }
        if (graph.children != null && graph.children.size > 0) {
            for (Entity e : graph.children) {
                // Add tag to identify them as set elements.
                e.add(new TagSetElement());

                // The orbits need to go to the alwaysUpdate list.
                if (Mapper.trajectory.has(e)) {
                    var trajectory = Mapper.trajectory.get(e);
                    if (trajectory.bodyRepresentation.isOrbit()) {
                        set.alwaysUpdate.add(e);
                    }
                } else {
                    // Not an orbit, always add.
                    set.alwaysUpdate.add(e);
                }
            }
        }
    }
}
