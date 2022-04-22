package gaiasky.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.scene.Scene;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.TextUtils;
import gaiasky.util.coord.IBodyCoordinates;
import gaiasky.util.i18n.I18n;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads JSON files into a scene object, which contains an
 * ECS world.
 */
public class SceneJsonLoader {
    private static final Log logger = Logger.getLogger(SceneJsonLoader.class);

    private static List<String> supportedLoaders = Arrays.asList("gaiasky.data.JsonLoader", "gaiasky.data.GeoJsonLoader");

    public synchronized static void loadScene(FileHandle[] jsonFiles, Scene scene) throws FileNotFoundException, ReflectionException {
        logger.info(I18n.msg("notif.loading", "JSON data descriptor files:"));
        for (FileHandle fh : jsonFiles) {
            logger.info("\t" + fh.path() + " - exists: " + fh.exists());
            if (!fh.exists()) {
                logger.error(I18n.msg("error.loading.notexistent", fh.path()));
            }
        }

        // Load files
        for (FileHandle jsonFile : jsonFiles) {
            loadJsonFile(jsonFile, scene);
        }

        // Initialize nodes, look for octrees and star groups.
        scene.initializeEntities();

        // Initialize index and hip map with names.
        scene.initializeIndex();

        // Construct scene graph in GraphNodes.
        scene.buildSceneGraph();
    }
    public synchronized static void loadJsonFile(FileHandle jsonFile, Scene scene) throws ReflectionException, FileNotFoundException {

        JsonReader jsonReader = new JsonReader();
        JsonValue model = jsonReader.parse(jsonFile.read());

        // Must have a 'data' element
        if (model.has("data")) {
            String name = model.get("name") != null ? model.get("name").asString() : null;
            String desc = model.get("description") != null ? model.get("description").asString() : null;
            Long size = model.get("size") != null ? model.get("size").asLong() : -1;
            Long nObjects = model.get("nobjects") != null ? model.get("nobjects").asLong() : -1;

            Map<String, Object> params = new HashMap<>();
            params.put("size", size);
            params.put("nobjects", nObjects);

            JsonValue child = model.get("data").child;
            while (child != null) {
                String clazzName = child.getString("loader").replace("gaia.cu9.ari.gaiaorbit", "gaiasky");

                if(!supportedLoaders.contains(clazzName)){
                    logger.info("Skipping " + clazzName + ": unsupported");
                    child = child.next;
                    continue;
                }

                // Update name
                if(clazzName.equals("gaiasky.data.JsonLoader")) {
                    clazzName = "gaiasky.data.NewJsonLoader";
                } else if (clazzName.equals("gaiasky.data.GeoJsonLoader")) {
                    clazzName = "gaiasky.data.NewGeoJsonLoader";
                }

                @SuppressWarnings("unchecked") Class<Object> clazz = (Class<Object>) ClassReflection.forName(clazzName);

                JsonValue filesJson = child.get("files");
                if (filesJson != null) {
                    String[] files = filesJson.asStringArray();

                    Constructor c = ClassReflection.getConstructor(clazz);
                    ISceneLoader loader = (ISceneLoader) c.newInstance();

                    if (name != null)
                        loader.setName(name);
                    if (desc != null)
                        loader.setDescription(desc);
                    if (params != null && params.size() > 0)
                        loader.setParams(params);

                    // Init loader
                    loader.initialize(files, scene);

                    JsonValue curr = filesJson;
                    while (curr.next != null) {
                        curr = curr.next;
                        String nameAttr = curr.name;
                        Object val = null;
                        Class valueClass = null;
                        if (curr.isDouble()) {
                            val = curr.asDouble();
                            valueClass = Double.class;
                        } else if (curr.isString()) {
                            val = curr.asString();
                            valueClass = String.class;
                        } else if (curr.isNumber()) {
                            val = curr.asLong();
                            valueClass = Long.class;
                        }
                        if (val != null) {
                            String methodName = "set" + TextUtils.propertyToMethodName(nameAttr);
                            Method m = searchMethod(methodName, valueClass, clazz);
                            if (m != null)
                                m.invoke(loader, val);
                            else
                                logger.error("ERROR: No method " + methodName + "(" + valueClass.getName() + ") in class " + clazz + " or its superclass/interfaces.");
                        }
                    }

                    // Load data
                    loader.loadData();
                }

                child = child.next;
            }
        } else {
            // Use regular JsonLoader
            NewJsonLoader loader = new NewJsonLoader();
            loader.initialize(new String[] { jsonFile.file().getAbsolutePath() }, scene);
            // Load data
            loader.loadData();
        }
    }

    /**
     * Searches for the given method with the given class. If none is found, it looks for fitting methods
     * with the class interfaces and superclasses recursively.
     */
    private static Method searchMethod(String methodName, Class<?> clazz, Class<?> source) {
        Method m = null;
        try {
            m = ClassReflection.getMethod(source, methodName, clazz);
        } catch (ReflectionException e) {
            try {
                if (methodName.contains("setCoordinates")) {
                    // Special case
                    m = ClassReflection.getMethod(source, methodName, IBodyCoordinates.class);
                }
            } catch (ReflectionException e1) {
                logger.error(e1);
            }
        }
        return m;
    }
}
