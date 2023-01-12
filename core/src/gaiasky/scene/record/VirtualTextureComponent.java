package gaiasky.scene.record;

import gaiasky.util.Settings;
import gaiasky.util.svt.SVTQuadtree;
import gaiasky.util.svt.SVTQuadtreeBuilder;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class VirtualTextureComponent extends NamedComponent {

    private static int sequenceId = 1;
    private static Map<Integer, VirtualTextureComponent> index = new HashMap<>();

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

    public VirtualTextureComponent() {
        this.id = sequenceId++;
        index.put(this.id, this);
    }

    public void initialize(String name) {
        super.initialize(name);
        buildTree();
    }

    public void buildTree() {
        var builder = new SVTQuadtreeBuilder();
        locationUnpacked = Settings.settings.data.dataFile(location);
        tree = builder.build(Path.of(locationUnpacked), tileSize);
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setTileSize(Integer size) {
        assert validTileSizeCheck(size) : "Tile size must be a power of two in [4,1024].";
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
