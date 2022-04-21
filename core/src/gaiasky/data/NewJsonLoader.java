package gaiasky.data;

import com.artemis.Archetype;
import com.artemis.ArchetypeBuilder;
import com.artemis.World;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.component.*;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import uk.ac.starlink.util.DataSource;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class NewJsonLoader implements ISceneLoader {
    private static final Log logger = Logger.getLogger(NewJsonLoader.class);

    // Contains all the files to be loaded by this loader
    private String[] filePaths;

    private Map<String, Archetype> archetypes;

    @Override
    public void initialize(String[] files) throws RuntimeException {
        filePaths = files;
    }

    @Override
    public void initialize(DataSource ds) {
        archetypes = new HashMap<>();

        Archetype star = new ArchetypeBuilder()
                .add(Base.class)
                .add(Body.class)
                .add(Celestial.class)
                .add(Magnitude.class)
                .add(GraphNode.class)
                .add(Coordinates.class)
    }
    @Override
    public void loadData(World world) throws FileNotFoundException {

        Array<String> filePaths = new Array<>(this.filePaths);

        // Actually load the files.
        JsonReader json = new JsonReader();
        for (String filePath : filePaths) {
            try {
                FileHandle file = Settings.settings.data.dataFileHandle(filePath);
                JsonValue model = json.parse(file.read());
                // Must have an 'objects' element.
                if (model.has("objects")) {
                    JsonValue child = model.get("objects").child;
                    final int count = model.get("objects").size;
                    int current = 0;
                    while (child != null) {
                        current++;
                        String clazzName = child.getString("impl");

                        @SuppressWarnings("unchecked") Class<Object> clazz = (Class<Object>) ClassReflection.forName(clazzName);

                        // Convert to object and add to list.
                        //@SuppressWarnings("unchecked") T object = (T) convertJsonToObject(child, clazz);


                        child = child.next;
                        EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, file.name(), (float) current / (float) count);
                    }
                    EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, file.name(), 2f);
                    logger.info(I18n.msg("notif.nodeloader", current, filePath));
                }
            } catch (Exception e) {
                logger.error(e);
            }
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
