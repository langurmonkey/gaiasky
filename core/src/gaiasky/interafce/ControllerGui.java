package gaiasky.interafce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextButton;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI that is operated with a game controller and optimized for that purpose.
 */
public class ControllerGui extends AbstractGui {

    private final Table content, menu;
    private Table camT, timeT, optT, datT, visT;
    private Cell contentCell;
    private OwnTextButton cameraButton, timeButton, optionsButton, datasetsButton, visualsButton;

    private List<OwnTextButton> tabButtons;
    private List<Table> tabContents;
    private Table currentContent;

    private GUIControllerListener guiControllerListener;
    private float pad5, pad10, pad30;

    private int selectedTab = 0;
    private int focusedElem = 0;

    public ControllerGui() {
        super();
        this.skin = GlobalResources.skin;
        content = new Table(skin);
        menu = new Table(skin);
        guiControllerListener = new GUIControllerListener();
        tabButtons = new ArrayList<>();
        tabContents = new ArrayList<>();
        pad5 = 5f * GlobalConf.UI_SCALE_FACTOR;
        pad10 = 10f * GlobalConf.UI_SCALE_FACTOR;
        pad30 = 30f * GlobalConf.UI_SCALE_FACTOR;
    }

    @Override
    protected void rebuildGui() {

        // Clean up
        content.clear();
        menu.clear();
        tabButtons.clear();
        tabContents.clear();

        float w = 500f * GlobalConf.UI_SCALE_FACTOR;
        float h = 500f * GlobalConf.UI_SCALE_FACTOR;

        // Create contents
        camT = new Table(skin);
        camT.setSize(w, h);
        camT.add(new OwnTextButton("Button cam 1", skin, "toggle-big")).padBottom(pad10).row();
        camT.add(new OwnTextButton("Button cam 2", skin, "big")).padBottom(pad10).row();
        camT.add(new OwnTextButton("Button cam 3", skin, "big")).padBottom(pad10).row();
        tabContents.add(camT);
        updatePads(camT);

        timeT = new Table(skin);
        timeT.setSize(w, h);
        timeT.add(new OwnTextButton("Button time 1", skin, "toggle-big")).padBottom(pad10).row();
        timeT.add(new OwnTextButton("Button time 2", skin, "toggle-big")).padBottom(pad10).row();
        timeT.add(new OwnTextButton("Button time 3", skin, "toggle-big")).padBottom(pad10).row();
        timeT.add(new OwnTextButton("Button time 4", skin, "toggle-big")).padBottom(pad10).row();
        timeT.add(new OwnTextButton("Button time 5", skin, "toggle-big")).padBottom(pad10).row();
        tabContents.add(timeT);
        updatePads(timeT);

        datT = new Table(skin);
        datT.setSize(w, h);
        datT.add(new OwnLabel("This is datasets", skin, "default"));
        tabContents.add(datT);
        updatePads(datT);

        optT = new Table(skin);
        optT.setSize(w, h);
        optT.add(new OwnLabel("This is options", skin, "default"));
        tabContents.add(optT);
        updatePads(optT);

        visT = new Table(skin);
        visT.setSize(w, h);
        visT.add(new OwnLabel("This is visuals", skin, "default"));
        tabContents.add(visT);
        updatePads(visT);

        // Create tab buttons
        cameraButton = new OwnTextButton("Camera", skin, "toggle-huge");
        tabButtons.add(cameraButton);

        timeButton = new OwnTextButton("Time", skin, "toggle-huge");
        tabButtons.add(timeButton);

        datasetsButton = new OwnTextButton("Datasets", skin, "toggle-huge");
        tabButtons.add(datasetsButton);

        optionsButton = new OwnTextButton("Options", skin, "toggle-huge");
        tabButtons.add(optionsButton);

        visualsButton = new OwnTextButton("Visuals", skin, "toggle-huge");
        tabButtons.add(visualsButton);

        for (OwnTextButton b : tabButtons) {
            b.pad(pad10);
            b.setMinWidth(200f * GlobalConf.UI_SCALE_FACTOR);
        }

        OwnTextButton lb, rb;
        lb = new OwnTextButton("RB >", skin, "key-big");
        rb = new OwnTextButton("< LB", skin, "key-big");
        lb.pad(pad10);
        rb.pad(pad10);
        menu.add(rb).center().padBottom(pad10).padRight(pad30);
        menu.add(cameraButton).center().padBottom(pad10);
        menu.add(timeButton).center().padBottom(pad10);
        menu.add(datasetsButton).center().padBottom(pad10);
        menu.add(optionsButton).center().padBottom(pad10);
        menu.add(visualsButton).center().padBottom(pad10);
        menu.add(lb).center().padBottom(pad10).padLeft(pad30).row();

        contentCell = menu.add().colspan(7);

        Table padTable = new Table(skin);
        padTable.pad(pad30);
        padTable.setBackground("table-border");
        menu.pack();
        padTable.add(menu).center();

        content.add(padTable);

        content.setFillParent(true);
        content.center();
        content.pack();

        updateTabs();
        updateFocused();

    }

    @Override
    public void initialize(AssetManager assetManager) {
        // User interface
        Viewport vp = new ScreenViewport();
        ui = new Stage(vp, GlobalResources.spriteBatch);

        // Comment to hide this whole dialog and functionality
        //EventManager.instance.subscribe(this, Events.SHOW_CONTROLLER_GUI_ACTION);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        rebuildGui();
    }

    private void updatePads(Table t) {
        Array<Cell> cells = t.getCells();
        for (Cell c : cells) {
            if (c.getActor() instanceof Button) {
                ((Button) c.getActor()).pad(pad30);
            }
        }
    }

    public void updateTabs() {
        for (OwnTextButton tb : tabButtons) {
            tb.setChecked(false);
        }
        tabButtons.get(selectedTab).setChecked(true);
        contentCell.setActor(null);
        currentContent = tabContents.get(selectedTab);
        contentCell.setActor(currentContent);
        focusedElem = 0;
        updateFocused();
    }

    public void updateFocused() {
        // Use current content table
        if (currentContent != null) {
            Array<Cell> cells = currentContent.getCells();
            int focused = focusedElem % cells.size;
            Cell cell = cells.get(focused);
            if (cell.getActor() != null) {
                Actor actor = cell.getActor();
                if (actor instanceof Button) {
                    ui.setKeyboardFocus(actor);
                }
            }
        }

    }

    public void tabLeft() {
        if (selectedTab - 1 < 0) {
            selectedTab = tabButtons.size() - 1;
        } else {
            selectedTab--;
        }
        updateTabs();
    }

    public void tabRight() {
        selectedTab = (selectedTab + 1) % tabButtons.size();
        updateTabs();
    }

    public void up() {
        left();
    }

    public void down() {
        right();
    }

    public void left() {
        focusedElem = focusedElem - 1;
        if (focusedElem < 0) {
            focusedElem = currentContent.getCells().size - 1;
        }
        updateFocused();
    }

    public void right() {
        focusedElem = (focusedElem + 1) % currentContent.getCells().size;
        updateFocused();
    }

    public void touchDown() {
        if (currentContent != null) {
            Array<Cell> cells = currentContent.getCells();
            int focused = focusedElem % cells.size;
            Actor actor = cells.get(focused).getActor();
            if (actor != null && actor instanceof Button) {
                final Button b = (Button) actor;

                InputEvent inputEvent = Pools.obtain(InputEvent.class);
                inputEvent.setType(InputEvent.Type.touchDown);
                b.fire(inputEvent);
                Pools.free(inputEvent);
            }
        }

    }

    public void touchUp() {
        if (currentContent != null) {
            Array<Cell> cells = currentContent.getCells();
            int focused = focusedElem % cells.size;
            Actor actor = cells.get(focused).getActor();
            if (actor != null && actor instanceof Button) {
                final Button b = (Button) actor;

                InputEvent inputEvent = Pools.obtain(InputEvent.class);
                inputEvent.setType(InputEvent.Type.touchUp);
                b.fire(inputEvent);
                Pools.free(inputEvent);
            }

        }
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
        private static final double AXIS_TH = 0.5;
        private static final long AXIS_DELAY = 250;

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
            if (buttonCode == mappings.getButtonA()) {
                touchDown();
            }
            return true;
        }

        @Override
        public boolean buttonUp(Controller controller, int buttonCode) {
            if (buttonCode == mappings.getButtonStart()) {
                em.post(Events.SHOW_CONTROLLER_GUI_ACTION, cam);
            } else if (buttonCode == mappings.getButtonB()) {
                back();
            } else if (buttonCode == mappings.getButtonA()) {
                touchUp();
            } else if (buttonCode == mappings.getButtonDpadUp()) {
                up();
            } else if (buttonCode == mappings.getButtonDpadDown()) {
                down();
            } else if (buttonCode == mappings.getButtonDpadLeft()) {
                left();
            } else if (buttonCode == mappings.getButtonDpadRight()) {
                right();
            } else if (buttonCode == mappings.getButtonRB()) {
                tabRight();
            } else if (buttonCode == mappings.getButtonLB()) {
                tabLeft();
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
                            down();
                        } else {
                            up();
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
