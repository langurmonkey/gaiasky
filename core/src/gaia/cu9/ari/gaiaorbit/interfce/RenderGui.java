package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;

import java.time.Instant;

/**
 * Only for frame output mode, it displays the current time.
 * 
 * @author Toni Sagrista
 *
 */
public class RenderGui extends AbstractGui {
    protected Label time;
    protected Table mainTable;

    protected MessagesInterface messagesInterface;

    protected IDateFormat df;

    public RenderGui() {
        super();
    }

    @Override
    public void initialize(AssetManager assetManager) {
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
        df = DateFormatFactory.getFormatter("dd/MM/yyyy HH:mm:ss");
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        skin = GlobalResources.skin;

        mainTable = new Table(skin);
        time = new OwnLabel("", skin, "ui-13");
        mainTable.add(time);
        mainTable.setFillParent(true);
        mainTable.right().bottom();
        mainTable.pad(5);

        // MESSAGES INTERFACE - LOW CENTER
        messagesInterface = new MessagesInterface(skin, lock);
        messagesInterface.setFillParent(true);
        messagesInterface.left().bottom();
        messagesInterface.pad(0, 300, 150, 0);

        // Add to GUI
        rebuildGui();

        EventManager.instance.subscribe(this, Events.TIME_CHANGE_INFO);
    }

    protected void rebuildGui() {
        if (ui != null) {
            ui.clear();
            ui.addActor(mainTable);
            ui.addActor(messagesInterface);
        }
    }

    @Override
    public void notify(Events event, Object... data) {
        synchronized (lock) {
            switch (event) {
            case TIME_CHANGE_INFO:
                time.setText(df.format((Instant) data[0]));
                break;
            default:
                break;
            }
        }
    }
}
