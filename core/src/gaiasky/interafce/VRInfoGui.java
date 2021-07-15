package gaiasky.interafce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.scene2d.OwnLabel;

public class VRInfoGui extends AbstractGui {
    protected Container<Table> container;
    protected Table contents, infoFocus, infoFree;
    protected Cell<Table> infoCell;

    public VRInfoGui(final Skin skin, final Lwjgl3Graphics graphics, final Float unitsPerPixel) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
        EventManager.instance.subscribe(this, Events.CAMERA_MODE_CMD);
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        // User interface
        float h = GlobalConf.screen.BACKBUFFER_HEIGHT;
        float w = GlobalConf.screen.BACKBUFFER_WIDTH;
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        ui = new Stage(vp, sb);

        container = new Container<>();
        container.setFillParent(true);
        container.bottom().right();
        container.padRight((w / 3f) - hoffset);
        container.padBottom(h / 3f);

        contents = new Table();
        contents.setFillParent(false);

        // FOUCS INFO
        contents.add(new FocusInfoInterface(skin, true)).left().padBottom(15f).row();
        infoCell = contents.add();

        infoFocus = new Table(skin);
        infoFocus.setBackground("table-bg");
        infoFocus.pad(5f);
        OwnLabel focusLabel = new OwnLabel("You are in focus mode", skin, "msg-21");
        focusLabel.setColor(1, 1, 0, 1);
        infoFocus.add(focusLabel).left().row();
        infoFocus.add(new OwnLabel("Push the joystick to get back", skin, "msg-21")).left().row();
        infoFocus.add(new OwnLabel("to free mode", skin, "msg-21")).left().row();

        infoFree = new Table(skin);
        infoFree.setBackground("table-bg");
        infoFree.pad(5f);
        OwnLabel freeLabel = new OwnLabel("You are in free mode", skin, "msg-21");
        freeLabel.setColor(1, 1, 0, 1);
        infoFree.add(freeLabel).left().row();
        infoFree.add(new OwnLabel("You can select an object by", skin, "msg-21")).left().row();
        infoFree.add(new OwnLabel("pointing at it and pressing", skin, "msg-21")).left().row();
        infoFree.add(new OwnLabel("the trigger", skin, "msg-21")).left().row();

        if (GaiaSky.instance.cam.mode.isFocus()) {
            infoCell.setActor(infoFocus);
        } else if (GaiaSky.instance.cam.mode.isFree()) {
            infoCell.setActor(infoFree);
        }

        container.setActor(contents);

        rebuildGui();
    }

    @Override
    public void doneLoading(AssetManager assetManager) {

    }

    @Override
    protected void rebuildGui() {
        if (ui != null) {
            ui.clear();
            if (container != null)
                ui.addActor(container);
        }
    }

    @Override
    public boolean mustDraw(){
        return GlobalConf.runtime.DISPLAY_VR_GUI;
    }

    @Override
    public void notify(final Events event, final Object... data) {

        switch (event) {
            case CAMERA_MODE_CMD:
                CameraMode cm = (CameraMode) data[0];
                if (cm.isFocus()) {
                    infoCell.setActor(infoFocus);
                } else if (cm.isFree()) {
                    infoCell.setActor(infoFree);
                } else {
                    infoCell.clearActor();
                }
                break;
            default:
                break;
        }
    }

}
