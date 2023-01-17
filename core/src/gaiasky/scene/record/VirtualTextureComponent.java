package gaiasky.scene.record;

import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.gdx.graphics.FloatTextureDataExt;
import gaiasky.util.gdx.graphics.TextureExt;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;
import gaiasky.util.svt.SVTQuadtree;
import gaiasky.util.svt.SVTQuadtreeBuilder;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class VirtualTextureComponent extends NamedComponent {

    private static int sequenceId = 1;
    private static final Map<Integer, VirtualTextureComponent> index = new HashMap<>();

    /**
     * The indirection buffer texture. {@link TextureExt} enables drawing to
     * any mipmap level.
     */
    public TextureExt indirectionBuffer;

    public static VirtualTextureComponent getSVT(int id) {
        return index.get(id);
    }

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

    private MaterialComponent materialComponent;

    public VirtualTextureComponent() {
        this.id = sequenceId++;
        index.put(this.id, this);
    }

    public void initialize(String name, MaterialComponent materialComponent) {
        super.initialize(name);
        this.materialComponent = materialComponent;
        buildTree();
        buildIndirectionBuffer();
    }

    public void buildIndirectionBuffer() {
        if (indirectionBuffer == null) {
            // Initialize indirection buffer.
            var indirectionSize = (int) Math.pow(2.0, tree.depth);
            // We use RGBA with 32-bit floating point numbers per channel for the indirection buffer.
            var indirectionData = new FloatTextureDataExt(indirectionSize * tree.root.length, indirectionSize, GL30.GL_RGBA32F, GL30.GL_RGBA, GL30.GL_FLOAT, true, false);
            indirectionBuffer = new TextureExt(indirectionData);
            // Important to set the minification filter to use mipmaps.
            indirectionBuffer.setFilter(TextureFilter.MipMapNearestNearest, TextureFilter.Nearest);
        }
    }

    public void buildTree() {
        var builder = new SVTQuadtreeBuilder();
        locationUnpacked = Settings.settings.data.dataFile(location);
        tree = builder.build(Path.of(locationUnpacked), tileSize);
        // In our implementation, we keep a reference to the component in the auxiliary data of the tree.
        tree.aux = this;

        int maxResolution = (int) (tree.tileSize * Math.pow(2, tree.depth));
        Logger.getLogger(VirtualTextureComponent.class).info("SVT (id " + id + ") initialized with " + tree.root.length + " roots, " + tree.numTiles + " tiles (" + tree.tileSize + "x" + tree.tileSize + "), depth " + tree.depth + " and maximum resolution of " + (maxResolution * tree.root.length) + "x" + maxResolution);
    }

    /**
     * Sets the SVT cache and indirection buffers to the material for this VT.
     *
     * @param cacheBufferTexture The cache buffer, which is global.
     */
    public void setSVTAttributes(Texture cacheBufferTexture) {
        if (materialComponent != null) {
            var material = materialComponent.getMaterial();
            if (material != null) {
                material.set(new TextureAttribute(TextureAttribute.SvtCache, cacheBufferTexture));
                if (indirectionBuffer != null && !material.has(TextureAttribute.SvtIndirectionDiffuse)) {
                    materialComponent.getMaterial().set(new TextureAttribute(TextureAttribute.SvtIndirectionDiffuse, indirectionBuffer));
                }
            }
        }
    }

    public boolean svtAttributesSet() {
        if (materialComponent != null) {
            var material = materialComponent.getMaterial();
            if (material != null) {
                return material.has(TextureAttribute.SvtIndirectionDiffuse) && material.has(TextureAttribute.SvtCache);
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
        setTileSize(Math.toIntExact(size));
    }

    @Override
    public void dispose() {

    }
}
