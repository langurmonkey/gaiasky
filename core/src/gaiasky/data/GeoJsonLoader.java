package gaiasky.data;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.scene.Archetype;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.Perimeter;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;

import java.io.FileNotFoundException;
import java.util.Map;

/**
 * Loads GeoJson files into ECS entities.
 */
public class GeoJsonLoader extends AbstractSceneLoader {
    private static final Logger.Log logger = Logger.getLogger(GeoJsonLoader.class);

    @Override
    public Array<Entity> loadData() throws FileNotFoundException {
        Array<Entity> loadedEntities = new Array<>();
        try {
            JsonReader json = new JsonReader();
            for (String filePath : filePaths) {
                FileHandle file = Settings.settings.data.dataFileHandle(filePath);
                JsonValue model = json.parse(file.read());
                JsonValue child = model.get("features").child;
                int size = 0;
                while (child != null) {
                    size++;

                    Entity loadedEntity = loadJsonObject(child);
                    if (loadedEntity != null) {
                        loadedEntities.add(loadedEntity);
                    }

                    child = child.next;
                }
                Logger.getLogger(this.getClass()).info(I18n.msg("notif.nodeloader", size, filePath));
            }

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }
        return loadedEntities;
    }

    private Entity loadJsonObject(JsonValue json) {
        String className = "gaiasky.scenegraph.Area";
        if (!scene.archetypes().contains(className)) {
            // Do not know what to do
            if (!loggedArchetypes.contains(className)) {
                logger.warn("Skipping " + className + ": no suitable archetype found.");
                loggedArchetypes.add(className);
            }
        } else {
            // Create entity and fill it up
            Archetype archetype = scene.archetypes().get(className);
            Entity entity = archetype.createEntity();

            // Components
            Base base = entity.getComponent(Base.class);
            GraphNode graphNode = entity.getComponent(GraphNode.class);
            Perimeter perimeter = entity.getComponent(Perimeter.class);

            // Set base info
            base.setName(json.get("properties").getString("name"));
            base.setCt("Countries");
            graphNode.setParent("Earth");

            JsonValue jsonArray = json.get("geometry").get("coordinates");

            JsonValue firstelem;
            int size;
            int d;

            int depth = depth(jsonArray);

            if (depth == 4) {
                firstelem = jsonArray.child;
                size = jsonArray.size;
                d = 1;
            } else {
                firstelem = jsonArray.child;
                size = jsonArray.size;
                d = 2;
            }

            // Set to component
            perimeter.setPerimeter(convertToDoubleArray(firstelem, size, d));

            return entity;
        }
        return null;
    }

    public double[][][] convertToDoubleArray(JsonValue json, int size, int d) {
        double[][][] result = new double[size][][];
        int i = 0;
        JsonValue current = json;
        if (d > 1)
            current = json.child;
        do {
            double[][] l1 = new double[current.size][];
            // Fill in last level

            JsonValue child = current.child;
            int j = 0;
            do {
                double[] l2 = child.asDoubleArray();
                l1[j] = l2;

                child = child.next();
                j++;
            } while (child != null);

            result[i] = l1;

            if (d == 1) {
                current = current.next();
            } else {
                current = json.next() != null ? json.next().child : null;
                json = json.next();
            }
            i++;
        } while (current != null);

        return result;
    }

    private int depth(JsonValue v) {
        if (v.isArray()) {
            return depth(v.child) + 1;
        } else {
            return 1;
        }
    }

    @Override
    public void setName(String name) {

    }

    @Override
    public void setDescription(String description) {

    }

    @Override
    public void setParams(Map<String, Object> params) {

    }
}
