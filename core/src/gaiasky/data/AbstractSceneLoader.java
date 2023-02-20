package gaiasky.data;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Scene;
import uk.ac.starlink.util.DataSource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractSceneLoader implements ISceneLoader {

    // Contains all the files to be loaded by this loader.
    protected String[] filePaths;
    // Data source, for STIL loaders.
    protected DataSource dataSource;
    // Dataset location (directory).
    protected String datasetDirectory;
    protected Scene scene;
    protected Set<String> loggedArchetypes;
    protected String parentName;
    protected Map<String, Entity> index;

    @Override
    public void initialize(String[] files, Scene scene) throws RuntimeException {
        initialize(files, null, scene);
    }

    @Override
    public void initialize(String[] files, String dsLocation, Scene scene) throws RuntimeException {
        this.scene = scene;
        this.filePaths = files;
        this.datasetDirectory = dsLocation;
        this.loggedArchetypes = new HashSet<>();
    }

    @Override
    public void initialize(DataSource dataSource, Scene scene) {
        this.scene = scene;
        this.dataSource = dataSource;
        this.loggedArchetypes = new HashSet<>();
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public void setIndex(Map<String, Entity> index) {
        this.index = index;
    }

}
