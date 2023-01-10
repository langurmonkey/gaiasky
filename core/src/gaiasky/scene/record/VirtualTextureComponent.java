package gaiasky.scene.record;

import gaiasky.util.Settings;
import gaiasky.util.svt.SVTQuadtree;
import gaiasky.util.svt.SVTQuadtreeBuilder;

import java.nio.file.Path;

public class VirtualTextureComponent extends NamedComponent {

    private static int sequenceId = 0;

    public int id;
    public String location;
    public String locationUnpacked;
    public int tileSize;

    public SVTQuadtree<Path> tree;

    public VirtualTextureComponent() {
       this.id = sequenceId++;
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
