package gaiasky.scene.record;

import gaiasky.util.Settings;
import gaiasky.util.svt.SVTQuadtree;
import gaiasky.util.svt.SVTQuadtreeBuilder;

import java.nio.file.Path;

public class VirtualTextureComponent extends NamedComponent {

    public String location;
    public String locationUnpacked;
    public int size;

    public SVTQuadtree<Path> tree;

    public void initialize(String name) {
        super.initialize(name);
        buildTree();
    }

    public void buildTree() {
        var builder = new SVTQuadtreeBuilder();
        locationUnpacked = Settings.settings.data.dataFile(location);
        tree = builder.build(Path.of(locationUnpacked), size);
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public void setSize(Long size) {
        this.size = Math.toIntExact(size);
    }

    @Override
    public void dispose() {

    }
}
