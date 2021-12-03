package gaiasky.scenegraph.component;

import java.util.Locale;

public class NamedComponent implements IComponent {
    protected String name;
    protected Long id;

    @Override
    public void initialize(String name, Long id) {
        if (name != null)
            this.name = name.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
        this.id = id;
    }
}
