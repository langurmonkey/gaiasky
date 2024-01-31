/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Texture;
import gaiasky.util.Settings;

public class Raymarching implements Component {

    public String raymarchingShader;
    public String additionalTexture;
    public String additionalTextureUnpacked;
    public Texture additional;
    public boolean isOn = false;

    public void setShader(String shader) {
        this.setRaymarchingShader(shader);
    }

    public void setRaymarchingShader(String shader) {
        this.raymarchingShader = shader;
    }

    public void setAdditionalTexture(String texture) {
        this.additionalTexture = Settings.settings.data.dataFile(texture);
    }

    public void setRaymarchingTexture(String texture) {
        setAdditionalTexture(texture);
    }

}
