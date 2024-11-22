/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import gaiasky.data.AssetBean;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.entity.FocusActive;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.gdx.loader.OwnTextureLoader;
import gaiasky.util.i18n.I18n;

public class RaymarchingInitializer extends AbstractInitSystem {

    public RaymarchingInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var focus = Mapper.focus.get(entity);
        var rm = Mapper.raymarching.get(entity);

        // Focus active
        focus.activeFunction = FocusActive::isFocusActiveTrue;

        if (graph.parentName == null) {
            graph.parentName = Scene.ROOT_NAME;
        }
        if (body.size == 0) {
            body.setSizeM(500.0);
        } else {
            // Size is given as a radius in km.
            body.setRadiusKm((double) body.size);
        }
        if (base.ct == null || base.ct.allSetLike(new ComponentTypes(ComponentType.Others))) {
            base.ct = new ComponentTypes(ComponentType.Invisible);
        }

        if (rm.additionalTexture != null) {
            var tp = new OwnTextureLoader.OwnTextureParameter();
            tp.genMipMaps = false;
            tp.minFilter = TextureFilter.Linear;
            tp.magFilter = TextureFilter.Linear;
            tp.wrapU = Texture.TextureWrap.Repeat;
            tp.wrapV = Texture.TextureWrap.Repeat;
            rm.additionalTextureUnpacked = addToLoad(rm.additionalTexture, tp);
        } else {
            rm.additionalTextureUnpacked = null;
        }

    }

    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex The texture file to load.
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex,
                             OwnTextureLoader.OwnTextureParameter texParams) {
        if (tex == null)
            return null;

        tex = GlobalResources.unpackAssetPath(tex);
        logger.info(I18n.msg("notif.loading", tex));
        AssetBean.addAsset(tex, Texture.class, texParams);

        return tex;
    }

    @Override
    public void setUpEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var raymarching = Mapper.raymarching.get(entity);

        if (raymarching != null) {
            if (raymarching.raymarchingShader != null && !raymarching.raymarchingShader.isBlank() && !Settings.settings.program.safeMode) {
                if (raymarching.additionalTextureUnpacked != null) {
                    if (AssetBean.manager().isLoaded(raymarching.additionalTextureUnpacked)) {
                        raymarching.additional = AssetBean.manager().get(raymarching.additionalTextureUnpacked);
                        EventManager.publish(Event.RAYMARCHING_CMD, this, base.getName(), false, entity, raymarching.raymarchingShader, new float[]{1f, 0f, 0f, 0f}, raymarching.additional);
                    } else {
                        logger.warn("Could not load texture: " + raymarching.additionalTexture);
                        EventManager.publish(Event.RAYMARCHING_CMD, this, base.getName(), false, entity, raymarching.raymarchingShader, new float[]{1f, 0f, 0f, 0f});
                    }
                } else {
                    EventManager.publish(Event.RAYMARCHING_CMD, this, base.getName(), false, entity, raymarching.raymarchingShader, new float[]{1f, 0f, 0f, 0f});
                }

                // Set up label
                var label = Mapper.label.get(entity);
                var sa = Mapper.sa.get(entity);
                sa.thresholdLabel = (Math.toRadians(1e-6) / Settings.settings.scene.label.number) * 60.0;
                label.textScale = 0.2f;
                label.labelMax = 1f;
                if (label.labelFactor == 0)
                    label.labelFactor = (float) (0.5e-3f * Constants.DISTANCE_SCALE_FACTOR);
                label.renderConsumer = LabelEntityRenderSystem::renderCelestial;
                label.renderFunction = LabelView::renderTextBase;
            } else {
                raymarching.raymarchingShader = null;
            }

        }

    }
}
