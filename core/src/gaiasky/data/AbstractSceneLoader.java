package gaiasky.data;

import gaiasky.scene.Scene;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import uk.ac.starlink.util.DataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
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

    public Object interceptDataFilePath(Class<?> valueClass, Object val) {
        // Intercept file paths.
        if (valueClass == String.class && ((String) val).startsWith(Constants.DATA_LOCATION_TOKEN)) {
            // Path is in data directory, just remove leading '$data/' and prepend data location
            String resolvedPathStr = (String) val;
            String pathFromDataStr = resolvedPathStr.replace(Constants.DATA_LOCATION_TOKEN, "");
            Path pathFromData = Path.of(pathFromDataStr);
            Path resolvedPath = Path.of(Settings.settings.data.location).resolve(pathFromDataStr);
            // We inject the location if:
            // - the current resolved path does not exist, and
            // - the dataset location is not null or empty, and
            // - the injected dataset location is not already in the path.
            if (!Files.exists(resolvedPath) && datasetDirectory != null && !datasetDirectory.isEmpty() && !pathFromData.getName(0).toString().equals(datasetDirectory)) {
                // Use dsLocation
                return Path.of(Constants.DATA_LOCATION_TOKEN).resolve(datasetDirectory).resolve(pathFromDataStr).toString();
            }
        }
        return val;
    }

    public String[] interceptDataFilePaths(String[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = (String) interceptDataFilePath(String.class, values[i]);
        }
        return values;
    }
}
