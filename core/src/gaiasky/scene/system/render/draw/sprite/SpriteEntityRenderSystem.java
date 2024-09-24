/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw.sprite;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.view.LabelView;
import gaiasky.util.DecalUtils;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.gdx.g2d.Sprite;
import gaiasky.util.math.Vector3d;

import java.util.HashMap;
import java.util.Map;

public class SpriteEntityRenderSystem {
    protected static final Logger.Log logger = Logger.getLogger(SpriteEntityRenderSystem.class);

    private final LabelView view;

    private final Vector3d D31 = new Vector3d();

    private Map<String, Sprite> spriteMap;

    public SpriteEntityRenderSystem() {
        this.view = new LabelView();
        initSprites();
    }

    private void initSprites() {
        this.spriteMap = new HashMap<>();

        var spriteDefault = new Sprite(GaiaSky.instance.getGlobalResources().getTexture("loc-marker-default"));
        var spriteFlag = new Sprite(GaiaSky.instance.getGlobalResources().getTexture("loc-marker-flag"));
        var spriteCity = new Sprite(GaiaSky.instance.getGlobalResources().getTexture("loc-marker-city"));
        this.spriteMap.put("default", spriteDefault);
        this.spriteMap.put("loc-marker-default", spriteDefault);
        this.spriteMap.put("flag", spriteFlag);
        this.spriteMap.put("loc-marker-flag", spriteFlag);
        this.spriteMap.put("city", spriteCity);
        this.spriteMap.put("loc-marker-city", spriteCity);
    }

    private Sprite getSprite(String name) {
        if (name.equalsIgnoreCase("none")) {
            return null;
        }
        var s = spriteMap.get(name);
        if (s == null) {
            try {
                Texture t = new Texture(Settings.settings.data.dataFile(name));
                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                s = new Sprite(t);
                spriteMap.put(name, s);
            } catch (Exception e) {
                logger.error(e);
            }
        }
        return s;
    }


    Vector3 F31 = new Vector3();

    public void render(Entity entity, SpriteBatch batch, ICamera camera) {
        view.setEntity(entity);

        if (view.isLocation()
                && view.renderTextLocation()) {

            Vector3d pos = D31;
            view.textPosition(camera, pos);

            var sprite = getSprite(view.loc.locationMarkerTexture);
            if (sprite != null) {
                var body = view.body;

                float[] color = body.color != null ? body.color : body.labelColor;
                if (color != null) {
                    sprite.setColor(color[0], color[1], color[2], color.length > 3 ? color[3] : 0.4f);
                } else {
                    // Default.
                    sprite.setColor(0.7f, 0.6f, 0.0f, 0.4f);
                }
                DecalUtils.drawSprite(sprite,
                        batch,
                        (float) pos.x,
                        (float) pos.y,
                        (float) pos.z,
                        0.0001d,
                        1f,
                        camera,
                        true,
                        0.017f,
                        0.035f);

                // Check mouse collision.
                var loc = Mapper.loc.get(entity);
                if (loc != null && loc.tooltipText != null) {
                    pos.put(F31);
                    camera.getCamera().project(F31);
                    var x = Gdx.input.getX();
                    var y_o = Gdx.input.getY();
                    var y = Gdx.graphics.getHeight() - y_o;

                    var s = 8;
                    if (x > F31.x - s && x < F31.x + s && y > F31.y - s && y < F31.y + s) {
                        // Collision!
                        EventManager.publish(Event.LOCATION_HOVER_INFO, this, x, y_o, loc);
                    }
                }
            }
        }
    }

}
