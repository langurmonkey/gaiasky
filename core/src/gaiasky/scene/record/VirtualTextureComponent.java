/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.gdx.graphics.FloatTextureDataExt;
import gaiasky.util.gdx.graphics.TextureExt;
import gaiasky.util.gdx.shader.attribute.FloatAttribute;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;
import gaiasky.util.gdx.shader.attribute.Vector2Attribute;
import gaiasky.util.svt.SVTQuadtree;
import gaiasky.util.svt.SVTQuadtreeBuilder;
import net.jafama.FastMath;

import java.nio.file.Path;

public final class VirtualTextureComponent extends NamedComponent {

    /**
     * The indirection buffer texture. {@link TextureExt} enables drawing to
     * any mipmap level.
     */
    public TextureExt indirectionBuffer;

    public int id;
    /**
     * The location of the tiles in the file system.
     * This directory should contain a list of directories, one for each level,
     * following the naming convention "level[LEVEL_NUMBER]", starting at "level0".
     * The files should be named "tx_[COL]_[ROW].ext".
     **/
    public String location;
    public String locationUnpacked;
    /** A power of two with a maximum of 1024. **/
    public int tileSize;

    public SVTQuadtree<Path> tree;

    private IMaterialProvider materialProvider;

    /** Texture attribute ID for the indirection texture **/
    private int indirectionAttributeId;

    public VirtualTextureComponent() {
    }

    public void initialize(String name, IMaterialProvider materialProvider, int indirectionAttributeId) {
        super.initialize(name);
        this.materialProvider = materialProvider;
        this.indirectionAttributeId = indirectionAttributeId;
        buildTree();
    }

    public void doneLoading(AssetManager manager) {
        buildIndirectionBuffer();
    }

    public void buildIndirectionBuffer() {
        if (indirectionBuffer == null) {
            // Initialize indirection buffer.
            var indirectionSize = (int) FastMath.round(FastMath.pow(2.0, tree.depth));
            // We use RGBA with 32-bit floating point numbers per channel for the indirection buffer.
            // The indirection buffer only needs to store XY coordinates in the cache (which are usually small), and the level, so we could
            // use a 16-bit float per channel. However, java does not have a half-precision float, so we go with RGBA32F.
            var indirectionData = new FloatTextureDataExt(indirectionSize * tree.root.length, indirectionSize, GL30.GL_RGBA32F, GL30.GL_RGBA, GL30.GL_FLOAT, true, false);
            indirectionBuffer = new TextureExt(indirectionData);
            // Important to set the minification filter to use mipmaps.
            indirectionBuffer.setFilter(TextureFilter.MipMapNearestNearest, TextureFilter.Nearest);
        }
    }

    public void buildTree() {
        var builder = new SVTQuadtreeBuilder();
        locationUnpacked = Settings.settings.data.dataFile(location);
        tree = builder.build(name, Path.of(locationUnpacked), tileSize);
        // In our implementation, we keep a reference to the component in the auxiliary data of the tree.
        tree.aux = this;

        int maxResolution = (int) FastMath.round(tree.tileSize * FastMath.pow(2.0, tree.depth));
        Logger.getLogger(VirtualTextureComponent.class).info("SVT initialized with " + tree.root.length + " roots, " + tree.numTiles + " tiles (" + tree.tileSize + "x" + tree.tileSize + "), depth " + tree.depth + " and maximum resolution of " + (maxResolution * tree.root.length) + "x" + maxResolution);
    }

    /**
     * Sets the SVT cache and indirection buffers to the material for this VT.
     *
     * @param cacheBufferTexture The cache buffer, which is global.
     */
    public void setSVTAttributes(Texture cacheBufferTexture) {
        if (materialProvider != null) {
            var material = materialProvider.getMaterial();
            if (material != null) {
                material.set(new TextureAttribute(TextureAttribute.SvtCache, cacheBufferTexture));
                if (indirectionBuffer != null && !material.has(indirectionAttributeId)) {
                    materialProvider.getMaterial().set(new TextureAttribute(indirectionAttributeId, indirectionBuffer));
                    // Height textures need more information!
                    if (indirectionAttributeId == TextureAttribute.SvtIndirectionHeight) {
                        if (materialProvider != null && materialProvider instanceof MaterialComponent mc) {
                            int[] resolution = tree.getResolution();
                            mc.heightSize.set(resolution[0], resolution[1]);
                            material.set(new FloatAttribute(FloatAttribute.HeightScale, mc.heightScale * (float) Settings.settings.scene.renderer.elevation.multiplier));
                            material.set(new Vector2Attribute(Vector2Attribute.HeightSize, mc.heightSize));
                            material.set(new FloatAttribute(FloatAttribute.TessQuality, (float) Settings.settings.scene.renderer.elevation.quality));
                        }
                    }
                }
            }
        }
    }

    public boolean svtAttributesSet() {
        if (materialProvider != null) {
            var material = materialProvider.getMaterial();
            if (material != null) {
                return material.has(indirectionAttributeId) && material.has(TextureAttribute.SvtCache);
            }
        }
        return false;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setTileSize(Integer size) {
        assert validTileSizeCheck(size) : "Tile size must be a power of two, with a maximum of 1024.";
        this.tileSize = size;
    }

    private boolean validTileSizeCheck(int x) {
        return (x == 4 || x == 8 || x == 16 || x == 32 || x == 64 || x == 128 || x == 256 || x == 512 || x == 1024);
    }

    public void setTileSize(Long size) {
        setTileSize(FastMath.toIntExact(size));
    }

    @Override
    public void dispose() {

    }
}
