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
    public String location;
    public String locationUnpacked;
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
        this.tileSize = size;
    }

    public void setTileSize(Long size) {
        this.tileSize = Math.toIntExact(size);
    }

    @Override
    public void dispose() {

    }
}
