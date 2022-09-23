package gaiasky.scene;

import com.badlogic.ashley.core.Entity;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;

import java.util.Locale;

/**
 * Deals with all events related to the scene.
 */
public class SceneObserver implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(SceneObserver.class);

    private final FocusView view;

    public SceneObserver() {
        this.view = new FocusView();

        EventManager.instance.subscribe(this, Event.PER_OBJECT_VISIBILITY_CMD, Event.FORCE_OBJECT_LABEL_CMD, Event.LABEL_COLOR_CMD);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        switch (event) {
        case PER_OBJECT_VISIBILITY_CMD -> {
            if (data[0] instanceof FocusView) {
                final var entity = (FocusView) data[0];
                final String name = (String) data[1];
                final boolean state = (boolean) data[2];

                entity.setVisible(state, name.toLowerCase());
                logger.info(I18n.msg("notif.visibility.object.set", entity.getName(), I18n.msg("gui." + state)));
            }
        }
        case FORCE_OBJECT_LABEL_CMD -> {
            if (data[0] instanceof FocusView) {
                final var entity = (FocusView) data[0];
                final String name = (String) data[1];
                final boolean state = (boolean) data[2];

                entity.setForceLabel(state, name.toLowerCase(Locale.ROOT));
                logger.info(I18n.msg("notif.object.flag", "forceLabel", entity.getName(), I18n.msg("gui." + state)));
            }
        }
        case LABEL_COLOR_CMD -> {
            final Entity entity = (Entity) data[0];
            String name = (String) data[1];
            float[] labelColor = (float[]) data[2];

            synchronized (view) {
                view.setEntity(entity);
                view.setLabelColor(labelColor, name);
            }
        }
        }

    }
}
