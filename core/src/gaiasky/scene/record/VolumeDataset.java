/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import gaiasky.GaiaSky;
import gaiasky.data.AssetBean;
import gaiasky.render.BlendMode;
import gaiasky.data.util.GlobalResources;
import gaiasky.render.gdx.graphics.VolumeTexture;
import gaiasky.render.gdx.loader.VolumeTextureLoader;

import java.util.Locale;

/**
 * A dataset composed of a 3D volume texture, with one (intensity) or two (intensity, color) channels.
 */
public class VolumeDataset {
    // The source file.
    public String file;
    // Unpacked file path.
    public String fileUnpack;
    // The data.
    public VolumeTexture texture;
    // True when the texture was generated in memory and has no file behind it.
    public boolean generated = false;
    // Type of data.
    public ParticleType type;
    // The intensity factor.
    public float intensity = 1;
    // Whether to allow depth writes when rendering.
    public boolean depthMask = false;
    // The blending mode.
    public BlendMode blending = BlendMode.ADDITIVE;

    public VolumeDataset() {
        super();
    }

    public boolean initialize() {
        if (generated && texture != null) {
            // Already built in memory: nothing to load, nothing to wait for.
            return true;
        }
        if (file != null && !file.isBlank()) {
            fileUnpack = GlobalResources.unpackAssetPath(GaiaSky.settings().data.dataFile(file));
            var param = new VolumeTextureLoader.VolumeTextureParameter();
            AssetBean.addAsset(fileUnpack, VolumeTexture.class, param);
            return true;
        }
        return false;
    }

    /**
     * Hands over a volume that was built in memory instead of loaded from a file, such as the density
     * field the galaxy generator voxelises. Skips the asset loading entirely: there is no file, and no
     * asset manager entry to wait for.
     *
     * @param texture The generated texture.
     */
    public void setTexture(VolumeTexture texture) {
        this.texture = texture;
        this.generated = texture != null;
    }

    public void doneLoading() {
        if (generated) {
            return;
        }
        if (fileUnpack != null && AssetBean.manager().contains(fileUnpack, VolumeTexture.class)) {
            texture = AssetBean.manager().get(fileUnpack, VolumeTexture.class);
        }
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setIntensity(Double intensity) {
        this.intensity = intensity.floatValue();
    }

    public void setType(String type) {
        if (type != null && !type.isBlank()) {
            this.type = ParticleType.valueOf(type.toUpperCase(Locale.ROOT));
        }
    }

    public void setDepthMask(Boolean depthMask) {
        this.depthMask = depthMask;
    }

    public void setDepthmask(Boolean depthMask) {
        setDepthMask(depthMask);
    }

    public void setBlending(String blending) {
        this.blending = BlendMode.valueOf(blending.toUpperCase(Locale.ROOT));
    }

    public enum ParticleType {
        DUST,
        BULGE,
        STAR,
        GAS,
        HII,
        GALAXY,
        POINT,
        OTHER
    }
}
