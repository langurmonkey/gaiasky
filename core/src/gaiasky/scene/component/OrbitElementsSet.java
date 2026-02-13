/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.TextureArray;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.Logger;
import gaiasky.util.math.MathUtilsDouble;

import java.util.List;
import java.util.Locale;

/**
 * Aggregates children entities (typically orbits) so that they are treated as one,
 * especially in terms of GPU draw calls.
 */
public class OrbitElementsSet implements Component {
    public Array<Entity> alwaysUpdate;
    public boolean initialUpdate = true;

    /**
     * Particle data.
     */
    public List<IParticleRecord> data;

    /**
     * Profile decay of the particles in the shader, when using quads and plain {@link ParticleSet.ShadingType}.
     */
    public float profileDecay = 6.5f;

    /**
     * Noise factor for the color in [0,1].
     */
    public float colorNoise = 0;

    /**
     * Fully qualified name of data provider class.
     */
    public String provider;

    /**
     * Pointer to the data file.
     */
    public String datafile;

    /**
     * The texture attribute is an attribute in the original table that points to the texture to use. Ideally, it should
     * be an integer value from 1 to n, where n is the number of textures.
     */
    public String textureAttribute;

    /**
     * Texture files to use for rendering the particles, at random. Applies only to quads.
     **/
    public String[] textureFiles = null;

    /**
     * Reference to the texture array containing the textures for this set, if any. Applies only to quads.
     **/
    public TextureArray textureArray;

    /** The shading type, for lighting the particles. **/
    public ParticleSet.ShadingType shadingType = ParticleSet.ShadingType.PLAIN;
    /** Controls how sharply the sphere edges darken (higher values = more pronounced sphere appearance with darker edges). **/
    public float sphericalPower = 3f;

    public void markForUpdate(Render render) {
        EventManager.publish(Event.GPU_DISPOSE_ORBITAL_ELEMENTS, render);
    }

    /**
     * Returns the list of particles.
     */
    public List<IParticleRecord> data() {
        return data;
    }

    public void setDataFile(String dataFile) {
        this.datafile = dataFile;
    }

    public void setData(List<IParticleRecord> pointData) {
        this.data = pointData;
    }

    public void setProfileDecay(Double profileDecay) {
        this.profileDecay = profileDecay.floatValue();
    }

    /**
     * @deprecated Use {@link #setProfileDecay(Double)}.
     */
    @Deprecated
    public void setProfiledecay(Double profiledecay) {
        setProfileDecay(profiledecay);
    }

    public void setColorNoise(Double colorNoise) {
        this.colorNoise = colorNoise.floatValue();
    }

    public void setColornoise(Double colorNoise) {
        this.setColorNoise(colorNoise);
    }

    public void setTextureAttribute(String textureAttribute) {
        this.textureAttribute = textureAttribute;
    }

    public void setTexture(String texture) {
        this.textureFiles = new String[]{texture};
    }

    public void setTextures(String[] textures) {
        this.textureFiles = textures;
    }

    public void setShadingType(int type) {
        type = MathUtils.clamp(type, 0, ParticleSet.ShadingType.values().length - 1);
        this.shadingType = ParticleSet.ShadingType.values()[type];
    }

    public void setShadingType(String type) {
        try {
            this.shadingType = ParticleSet.ShadingType.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Logger.getLogger(this.getClass()
                                     .getSimpleName())
                    .error("Error setting shading type: " + type, e);
        }
    }

    public void setSphericalPower(Double power) {
        this.sphericalPower = (float) MathUtilsDouble.clamp(power, 0.1, 20.0);
    }
}
