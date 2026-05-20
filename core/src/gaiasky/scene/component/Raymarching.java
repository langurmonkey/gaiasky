/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Texture;
import gaiasky.GaiaSky;

/**
 * Stores information for raymarching-based rendering of bodies.
 */
public class Raymarching implements Component {

    public String raymarchingShader;
    /**
     * Location of an optional texture to pass in  to the ray-marching
     * shader via the <code>u_additionalTexture</code> uniform.
     */
    public String additionalTexStr;
    /**
     * Unpacked version of {@link #additionalTexStr}.
     */
    public String additionalTexStrUnpacked;
    /**
     * Texture object corresponding to {@link #additionalTexStr}.
      */
    public Texture additionalTexture;
    /**
     * A vector of up to 4 components with additional floating point numbers to
     * pass in as data to the ray-marching shader via the <code>u_additional</code> uniform.
     */
    public float[] additional = new float[4];
    public boolean isOn;

    public void setShader(String shader) {
        this.setRaymarchingShader(shader);
    }

    public void setRaymarchingShader(String shader) {
        this.raymarchingShader = shader;
    }

    public void setAdditionalTexture(String texture) {
        this.additionalTexStr = GaiaSky.settings().data.dataFile(texture);
    }

    public void setRaymarchingTexture(String texture) {
        setAdditionalTexture(texture);
    }

    /**
     * Sets the additional array.
     * @param additional The additional array as an array of doubles.
     */
    public void setAdditional(Double[] additional) {
        if (additional == null) {
            return; // or handle as needed
        }
        int count = Math.min(additional.length, this.additional.length);
        for (int i = 0; i < count; i++) {
            if (additional[i] != null) {
                this.additional[i] = additional[i].floatValue();
            }
        }
    }

    public void setAdditionalData(Double[] additional) {
        setAdditional(additional);
    }

    /**
     * Sets the first value in {@link #additional}.
     * @param additional The value.
     */
    public void setAdditional(Double additional) {
        this.additional[0] = additional.floatValue();
    }

}
