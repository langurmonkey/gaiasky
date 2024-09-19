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
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.view.LabelView;
import gaiasky.util.DecalUtils;
import gaiasky.util.gdx.g2d.Sprite;
import gaiasky.util.math.Vector3d;

public class SpriteEntityRenderSystem {
    private final LabelView view;

    private final Vector3d D31 = new Vector3d();

    private Sprite spriteMain;

    public SpriteEntityRenderSystem() {
        this.view = new LabelView();
        initSprites();
    }

    private void initSprites() {
        Texture mainTex = new Texture(Gdx.files.internal("img/loc-marker.png"));
        mainTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        spriteMain = new Sprite(mainTex);
    }

    public void render(Entity entity, SpriteBatch batch, ICamera camera) {
        view.setEntity(entity);

        if (view.isLocation()
                && view.renderTextLocation()) {

            Vector3d pos = D31;
            view.textPosition(camera, pos);

            spriteMain.setColor(0.7f, 0.6f, 0.0f, 0.4f);
            DecalUtils.drawSprite(spriteMain,
                    batch,
                    (float) pos.x,
                    (float) pos.y,
                    (float) pos.z,
                    0.0001d,
                    1f,
                    camera,
                    true,
                    0.02f,
                    0.04f);
        }
    }

}
