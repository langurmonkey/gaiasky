package gaiasky.data;

import gaiasky.scene.Scene;
import uk.ac.starlink.util.DataSource;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractSceneLoader implements ISceneLoader{


    // Contains all the files to be loaded by this loader
    protected String[] filePaths;

    protected Scene scene;
    protected Set<String> loggedArchetypes;


    @Override
    public void initialize(String[] files, Scene scene) throws RuntimeException {
        this.scene = scene;
        this.filePaths = files;
        this.loggedArchetypes = new HashSet<>();
    }

    @Override
    public void initialize(DataSource ds, Scene scene) {
        this.scene = scene;
    }

}
