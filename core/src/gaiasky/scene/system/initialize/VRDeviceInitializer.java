/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.util.Bits;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.ModelCache;
import gaiasky.util.Pair;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.math.Vector3d;

import java.util.HashMap;
import java.util.Map;

public class VRDeviceInitializer extends AbstractInitSystem {
    private static final Log logger = Logger.getLogger(VRDeviceInitializer.class);

    public VRDeviceInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        if (Mapper.vr.has(entity)) {
            initializeVRDevice(entity);
        }

        if (Mapper.tagVRUI.has(entity)) {
            initializeVRUI(entity);
        }

    }

    private void initializeVRUI(Entity entity) {

    }

    private void initializeVRDevice(Entity entity) {
        // VR device.
        var vr = Mapper.vr.get(entity);
        vr.beamP0 = new Vector3d();
        vr.beamP1 = new Vector3d();
        vr.intersection = new Vector3d();

        // Base.
        var base = Mapper.base.get(entity);
        base.setComponentType(ComponentType.Others);

        // Body.
        var body = Mapper.body.get(entity);
        body.color = new float[] { 1f, 0f, 0f };

        // Line.
        var line = Mapper.line.get(entity);
        line.lineWidth = 2f;
        line.renderConsumer = LineEntityRenderSystem::renderVRDevice;

        // Model renderer.
        var model = Mapper.model.get(entity);
        if (vr.device != null) {
            model.model.instance = vr.device.getModelInstance();
            Map<String, Object> params = new HashMap<>();
            params.put("diameter", 1.0);
            params.put("quality", 20L);
            params.put("flip", false);
            Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Bits.indexes(Usage.Position, Usage.Normal), GL20.GL_TRIANGLES);
            IntModel sphere = pair.getFirst();

            // Create models
            vr.intersectionModel = new IntModelInstance(sphere, new Matrix4());
            vr.intersectionModel.materials.get(0).set(new ColorAttribute(ColorAttribute.Diffuse, ColorUtils.gRedC));
            vr.intersectionModel.materials.get(0).set(new BlendingAttribute(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.7f));
        } else {
            logger.error("VR device model has no attached device!");
        }
        if (model.renderConsumer == null) {
            model.renderConsumer = ModelEntityRenderSystem::renderVRDeviceModel;
        }

        EventManager.publish(Event.VR_CONTROLLER_INFO, this, vr);

    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
