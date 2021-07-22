package gaiasky.desktop.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gaiasky.desktop.GaiaSkyDesktop;
import gaiasky.util.*;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Initializes and stores the YAML configuration file for Gaia Sky.
 */
public class SettingsManager {
    private static final Logger.Log logger = Logger.getLogger(SettingsManager.class);

    public static SettingsManager instance;
    public static void initialize(boolean vr) throws Exception {
        SettingsManager.instance = new SettingsManager(vr);
        instance.initSettings();
    }

    private Settings settings;
    private Properties vp;

    public SettingsManager(boolean vr) {
        super();
        try {
            String propsFileProperty = System.getProperty("properties.file");
            propsFileProperty = "/home/tsagrista/.config/gaiasky/config.yaml";
            if (propsFileProperty == null || propsFileProperty.isEmpty()) {
                propsFileProperty = initConfigFile(vr);
            }

            File confFile = new File(propsFileProperty);
            InputStream fis = new FileInputStream(confFile);

            // This should work for the normal execution
            InputStream vis = GaiaSkyDesktop.class.getResourceAsStream("/version");
            if (vis == null) {
                // In case of running in 'developer' mode
                vis = new FileInputStream(GlobalConf.ASSETS_LOC + File.separator + "dummyversion");
            }
            vp = new Properties();
            vp.load(vis);

            ObjectMapper om = new ObjectMapper(new YAMLFactory());
            om.findAndRegisterModules();
            settings = om.readValue(fis, Settings.class);

        } catch (Exception e) {
            logger.error(e);
        }
    }

    public SettingsManager(InputStream fis, InputStream vis) {
        super();
        try {
            vp = new Properties();
            vp.load(vis);

            ObjectMapper om = new ObjectMapper(new YAMLFactory());
            om.findAndRegisterModules();
            settings = om.readValue(fis, Settings.class);

        } catch (Exception e) {
            logger.error(e);
        }
    }

    public void initSettings(){
        String ARCH = System.getProperty("sun.arch.data.model");



    }

    public void persistSettings(final File settingsFile) {

    }

    private String initConfigFile(boolean vr) throws IOException {
        // Use user folder
        Path userFolder = SysUtils.getConfigDir();
        Path userFolderConfFile = userFolder.resolve(getConfigFileName(vr));

        if (!Files.exists(userFolderConfFile)) {
            // Copy file
            GlobalResources.copyFile(Path.of("conf", getConfigFileName(vr)), userFolderConfFile, false);
        }
        String props = userFolderConfFile.toAbsolutePath().toString();
        System.setProperty("properties.file", props);
        return props;
    }

    public static String getConfigFileName(boolean vr) {
        if (vr)
            return "config.vr.yaml";
        else
            return "config.yaml";
    }

}
