package gaiasky.scene.record;

public class VirtualTextureComponent extends NamedComponent {

    public String location;
    public int size;

    public void initialize(String name) {
        super.initialize(name);
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
