package gaiasky.interafce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.scene2d.OwnTextButton;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI that is operated with a game controller and optimized for that purpose.
 */
public class ControllerGui extends AbstractGui {

    private final Table content, menu, camT, timeT, optT, datT, visT;
    private OwnTextButton cameraButton, timeButton, optionsButton, datasetsButton, visualsButton;
    private List<OwnTextButton> tabButtons;

    private GUIControllerListener guiControllerListener;

    private Button[][] focus;
    private int row = 0, col = 0;

    public ControllerGui() {
        super();
        this.skin = GlobalResources.skin;
        content = new Table(skin);
        menu = new Table(skin);
        camT = new Table(skin);
        timeT = new Table(skin);
        optT = new Table(skin);
        datT = new Table(skin);
        visT = new Table(skin);
        guiControllerListener = new GUIControllerListener();
        tabButtons = new ArrayList<>();
        focus = new Button[1][5];
    }

    @Override
    protected void rebuildGui() {
        content.clear();
        menu.clear();
        camT.clear();
        timeT.clear();
        datT.clear();
        optT.clear();
        visT.clear();

        float padBig = 30f * GlobalConf.UI_SCALE_FACTOR;
        cameraButton = new OwnTextButton("Camera", skin, "toggle-huge");
        focus[0][0] = cameraButton;
        tabButtons.add(cameraButton);

        timeButton = new OwnTextButton("Time", skin, "toggle-huge");
        focus[0][1] = timeButton;
        tabButtons.add(timeButton);

        datasetsButton = new OwnTextButton("Datasets", skin, "toggle-huge");
        focus[0][2] = datasetsButton;
        tabButtons.add(datasetsButton);

        optionsButton = new OwnTextButton("Options", skin, "toggle-huge");
        focus[0][3] = optionsButton;
        tabButtons.add(optionsButton);

        visualsButton = new OwnTextButton("Visuals", skin, "toggle-huge");
        focus[0][4] = visualsButton;
        tabButtons.add(visualsButton);

        for (OwnTextButton b : tabButtons) {
            b.pad(10f * GlobalConf.UI_SCALE_FACTOR);
            b.setMinWidth(200f * GlobalConf.UI_SCALE_FACTOR);
        }

        menu.add(cameraButton).center();
        menu.add(timeButton).center();
        menu.add(datasetsButton).center();
        menu.add(optionsButton).center();
        menu.add(visualsButton).center();

        Table padTable = new Table(skin);
        padTable.pad(padBig);
        padTable.setBackground("table-border");
        menu.pack();
        padTable.add(menu).left();

        content.add(padTable);

        content.setFillParent(true);
        content.center();
        content.pack();

        updateFocused();

    }

    @Override
    public void initialize(AssetManager assetManager) {
        // User interface
        Viewport vp = new ScreenViewport();
        ui = new Stage(vp, GlobalResources.spriteBatch);

        //EventManager.instance.subscribe(this, Events.SHOW_CONTROLLER_GUI_ACTION);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        rebuildGui();
    }

    public void updateFocused() {
        for (int i = 0; i < focus.length; i++) {
            for (int j = 0; j < focus[i].length; j++) {
                if (row == i && col == j) {
                    ui.setKeyboardFocus(focus[i][j]);
                }
            }
        }
    }

    public void up() {
        row = (row - 1) % focus.length;
        updateFocused();
    }

    public void down() {
        row = (row + 1) % focus.length;
        updateFocused();
    }

    public void left() {
        if(col - 1 < 0){
            col = focus[row].length - 1;
        } else {
            col = (col - 1) % focus[row].length;
        }
        updateFocused();
    }

    public void right() {
        col = (col + 1) % focus[row].length;
        updateFocused();
    }

    public void select() {
        focus[row][col].setChecked(true);
    }

    public void back() {
        EventManager.instance.post(Events.SHOW_CONTROLLER_GUI_ACTION, GaiaSky.instance.cam.naturalCamera);
        ui.setKeyboardFocus(null);
        updateFocused();
    }

    @Override
    public void notify(final Events event, final Object... data) {
        // Empty by default
        switch (event) {
        case SHOW_CONTROLLER_GUI_ACTION:
            NaturalCamera cam = (NaturalCamera) data[0];
            if (content.isVisible() && content.getParent() != null) {
                // Hide and remove
                content.setVisible(false);
                content.remove();

                // Remove GUI listener, add natural listener
                cam.addControllerListener();
                removeControllerListener();
            } else {
                // Show
                // Add and show
                ui.addActor(content);
                content.setVisible(true);
                updateFocused();

                // Remove natural listener, add GUI listener
                cam.removeControllerListener();
                addControllerListener(cam, cam.getControllerListener().getMappings());
            }

            break;
        default:
            break;
        }
    }

    public boolean removeControllerGui(NaturalCamera cam) {
        if (content.isVisible() && content.getParent() != null) {
            // Hide and remove
            content.setVisible(false);
            content.remove();

            // Remove GUI listener, add natural listener
            cam.addControllerListener();
            removeControllerListener();
            return true;
        }
        return false;
    }

    private void addControllerListener(NaturalCamera cam, IControllerMappings mappings) {
        guiControllerListener.setCamera(cam);
        guiControllerListener.setMappings(mappings);
        GlobalConf.controls.addControllerListener(guiControllerListener);
        guiControllerListener.activate();
    }

    private void removeControllerListener() {
        GlobalConf.controls.removeControllerListener(guiControllerListener);
        guiControllerListener.deactivate();
    }

    private class GUIControllerListener implements ControllerListener, IInputListener {
        private static final double AXIS_TH = 0.25;
        private static final long AXIS_DELAY = 150;

        private long lastAxisTime = 0;
        private EventManager em;
        private NaturalCamera cam;
        private IControllerMappings mappings;

        public GUIControllerListener() {
            super();
            this.em = EventManager.instance;
        }

        public void setCamera(NaturalCamera cam) {
            this.cam = cam;
        }

        public void setMappings(IControllerMappings mappings) {
            this.mappings = mappings;
        }

        @Override
        public void connected(Controller controller) {

        }

        @Override
        public void disconnected(Controller controller) {

        }

        @Override
        public boolean buttonDown(Controller controller, int buttonCode) {
            return false;
        }

        @Override
        public boolean buttonUp(Controller controller, int buttonCode) {
            if (buttonCode == mappings.getButtonStart()) {
                em.post(Events.SHOW_CONTROLLER_GUI_ACTION, cam);
            } else if (buttonCode == mappings.getButtonB()) {
                back();
            } else if (buttonCode == mappings.getButtonA()) {
                select();
            } else if (buttonCode == mappings.getButtonDpadUp()) {
                up();
            } else if (buttonCode == mappings.getButtonDpadDown()) {
                down();
            } else if (buttonCode == mappings.getButtonDpadLeft()) {
                left();
            } else if (buttonCode == mappings.getButtonDpadRight()) {
                right();
            }

            return true;
        }

        @Override
        public boolean axisMoved(Controller controller, int axisCode, float value) {
            if (Math.abs(value) > AXIS_TH) {
                if (System.currentTimeMillis() - lastAxisTime > AXIS_DELAY) {
                    if (axisCode == mappings.getAxisLstickH()) {
                        // right/left
                        if (value > 0) {
                            right();
                        } else {
                            left();
                        }
                    } else if (axisCode == mappings.getAxisLstickV()) {
                        // up/down
                        if (value > 0) {
                            up();
                        } else {
                            down();
                        }
                    }
                    lastAxisTime = System.currentTimeMillis();
                }
            }
            return true;
        }

        @Override
        public boolean povMoved(Controller controller, int povCode, PovDirection value) {
            return false;
        }

        @Override
        public boolean xSliderMoved(Controller controller, int sliderCode, boolean value) {
            return false;
        }

        @Override
        public boolean ySliderMoved(Controller controller, int sliderCode, boolean value) {
            return false;
        }

        @Override
        public boolean accelerometerMoved(Controller controller, int accelerometerCode, Vector3 value) {
            return false;
        }

        @Override
        public void update() {

        }

        @Override
        public void activate() {

        }

        @Override
        public void deactivate() {

        }

    }
}
