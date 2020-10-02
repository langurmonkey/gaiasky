package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.SceneGraphRenderer;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.*;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnSliderPlus;
import gaiasky.util.scene2d.OwnTextButton;
import gaiasky.util.scene2d.OwnTextIconButton;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GUI that is operated with a game controller and optimized for that purpose.
 */
public class ControllerGui extends AbstractGui {

    private final Table content, menu;
    private Table camT, timeT, optT, typesT, sysT;
    private Cell contentCell;
    private OwnTextButton cameraButton, timeButton, optionsButton, typesButton, systemButton;
    // Contains a matrix (column major) of actors for each tab
    private List<Actor[][]> model;
    private OwnTextButton cameraFocus, cameraFree, cameraCinematic;
    private OwnTextButton timeStartStop, timeUp, timeDown, timeReset, quit, motionBlurButton, flareButton, starGlowButton;
    private OwnSliderPlus fovSlider, camSpeedSlider, camRotSlider, camTurnSlider, bloomSlider;

    private List<OwnTextButton> tabButtons;
    private List<ScrollPane> tabContents;

    private Actor[][] currentModel;

    private EventManager em;
    private GUIControllerListener guiControllerListener;
    private float pad5, pad10, pad20, pad30;

    private int selectedTab = 0;
    private int fi = 0, fj = 0;

    public ControllerGui() {
        super();
        this.skin = GlobalResources.skin;
        this.em = EventManager.instance;
        model = new ArrayList<>();
        content = new Table(skin);
        menu = new Table(skin);
        guiControllerListener = new GUIControllerListener();
        tabButtons = new ArrayList<>();
        tabContents = new ArrayList<>();
        pad5 = 5f * GlobalConf.UI_SCALE_FACTOR;
        pad10 = 10f * GlobalConf.UI_SCALE_FACTOR;
        pad20 = 20f * GlobalConf.UI_SCALE_FACTOR;
        pad30 = 30f * GlobalConf.UI_SCALE_FACTOR;
    }

    @Override
    protected void rebuildGui() {

        // Clean up
        content.clear();
        menu.clear();
        tabButtons.clear();
        tabContents.clear();
        model.clear();

        float w = 900f * GlobalConf.UI_SCALE_FACTOR;
        float h = 500f * GlobalConf.UI_SCALE_FACTOR;
        // Widget width
        float ww = 250f * GlobalConf.UI_SCALE_FACTOR;
        float sw = ww;
        float sh = 60f * GlobalConf.UI_SCALE_FACTOR;

        // Create contents

        // CAMERA
        Actor[][] cameraModel = new Actor[2][4];
        model.add(cameraModel);

        camT = new Table(skin);
        camT.setSize(w, h);
        CameraManager cm = GaiaSky.instance.getCameraManager();

        // Focus
        cameraFocus = new OwnTextButton(CameraManager.CameraMode.FOCUS_MODE.toStringI18n(), skin, "toggle-big");
        cameraModel[0][0] = cameraFocus;
        cameraFocus.setWidth(ww);
        cameraFocus.setChecked(cm.getMode().isFocus());
        cameraFocus.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (cameraFocus.isChecked()) {
                    em.post(Events.CAMERA_MODE_CMD, CameraManager.CameraMode.FOCUS_MODE);
                    cameraFree.setProgrammaticChangeEvents(false);
                    cameraFree.setChecked(false);
                    cameraFree.setProgrammaticChangeEvents(true);
                }
                return true;
            }
            return false;
        });

        // Free
        cameraFree = new OwnTextButton(CameraManager.CameraMode.FREE_MODE.toStringI18n(), skin, "toggle-big");
        cameraModel[0][1] = cameraFree;
        cameraFree.setWidth(ww);
        cameraFree.setChecked(cm.getMode().isFree());
        cameraFree.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (cameraFree.isChecked()) {
                    em.post(Events.CAMERA_MODE_CMD, CameraManager.CameraMode.FREE_MODE);
                    cameraFocus.setProgrammaticChangeEvents(false);
                    cameraFocus.setChecked(false);
                    cameraFocus.setProgrammaticChangeEvents(true);
                }
                return true;
            }
            return false;
        });

        // Cinematic
        cameraCinematic = new OwnTextButton(I18n.txt("gui.camera.cinematic"), skin, "toggle-big");
        cameraModel[0][2] = cameraCinematic;
        cameraCinematic.setWidth(ww);
        cameraCinematic.setChecked(GlobalConf.scene.CINEMATIC_CAMERA);
        cameraCinematic.addListener(event -> {
            if (event instanceof ChangeEvent) {
                em.post(Events.CAMERA_CINEMATIC_CMD, cameraCinematic.isChecked(), false);
                return true;
            }
            return false;
        });

        // FOV
        fovSlider = new OwnSliderPlus(I18n.txt("gui.camera.fov"), Constants.MIN_FOV, Constants.MAX_FOV, Constants.SLIDER_STEP_SMALL, false, skin);
        cameraModel[1][0] = fovSlider;
        fovSlider.setValueSuffix("°");
        fovSlider.setName("field of view");
        fovSlider.setWidth(sw);
        fovSlider.setHeight(sh);
        fovSlider.setValue(GlobalConf.scene.CAMERA_FOV);
        fovSlider.setDisabled(GlobalConf.program.isFixedFov());
        fovSlider.addListener(event -> {
            if (event instanceof ChangeEvent && !SlaveManager.projectionActive() && !GlobalConf.program.isFixedFov()) {
                float value = fovSlider.getMappedValue();
                EventManager.instance.post(Events.FOV_CHANGED_CMD, value);
                return true;
            }
            return false;
        });


        // Speed
        camSpeedSlider = new OwnSliderPlus(I18n.txt("gui.camera.speed"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_CAM_SPEED, Constants.MAX_CAM_SPEED, skin);
        cameraModel[1][1] = camSpeedSlider;
        camSpeedSlider.setName("camera speed");
        camSpeedSlider.setWidth(sw);
        camSpeedSlider.setHeight(sh);
        camSpeedSlider.setMappedValue(GlobalConf.scene.CAMERA_SPEED);
        camSpeedSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.CAMERA_SPEED_CMD, camSpeedSlider.getMappedValue(), false);
                return true;
            }
            return false;
        });

        // Rot
        camRotSlider = new OwnSliderPlus(I18n.txt("gui.rotation.speed"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED, skin);
        cameraModel[1][2] = camRotSlider;
        camRotSlider.setName("rotate speed");
        camRotSlider.setWidth(sw);
        camRotSlider.setHeight(sh);
        camRotSlider.setMappedValue(GlobalConf.scene.ROTATION_SPEED);
        camRotSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.ROTATION_SPEED_CMD, camRotSlider.getMappedValue(), false);
                return true;
            }
            return false;
        });

        // Turn
        camTurnSlider = new OwnSliderPlus(I18n.txt("gui.turn.speed"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED, skin);
        cameraModel[1][3] = camTurnSlider;
        camTurnSlider.setName("turn speed");
        camTurnSlider.setWidth(sw);
        camTurnSlider.setHeight(sh);
        camTurnSlider.setMappedValue(GlobalConf.scene.TURNING_SPEED);
        camTurnSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.TURNING_SPEED_CMD, camTurnSlider.getMappedValue(), false);
                return true;
            }
            return false;
        });

        camT.add(cameraFocus).padBottom(pad10).padRight(pad30);
        camT.add(fovSlider).padBottom(pad10).row();

        camT.add(cameraFree).padBottom(pad10).padRight(pad30);
        camT.add(camSpeedSlider).padBottom(pad10).row();

        camT.add(cameraCinematic).padBottom(pad10).padRight(pad30);
        camT.add(camRotSlider).padBottom(pad10).row();

        camT.add().padBottom(pad10).padRight(pad30);
        camT.add(camTurnSlider).padBottom(pad10).row();

        tabContents.add(container(camT, w, h));
        updatePads(camT);

        // TIME
        Actor[][] timeModel = new Actor[3][2];
        model.add(timeModel);

        timeT = new Table(skin);

        boolean timeOn = GlobalConf.runtime.TIME_ON;
        timeStartStop = new OwnTextButton(timeOn ? "Stop time" : "Start time", skin, "toggle-big");
        timeModel[1][0] = timeStartStop;
        timeStartStop.setWidth(ww);
        timeStartStop.setChecked(timeOn);
        timeStartStop.addListener(event -> {
            if (event instanceof ChangeEvent) {
                em.post(Events.TIME_STATE_CMD, timeStartStop.isChecked(), false);
                timeStartStop.setText(timeStartStop.isChecked() ? "Stop time" : "Start time");
                return true;
            }
            return false;
        });
        timeUp = new OwnTextIconButton("Speed up", Align.right, skin, "fwd");
        timeModel[2][0] = timeUp;
        timeUp.setWidth(ww);
        timeUp.addListener(event -> {
            if (event instanceof ChangeEvent) {
                em.post(Events.TIME_WARP_INCREASE_CMD);
                return true;
            }
            return false;
        });
        timeDown = new OwnTextIconButton("Slow down", skin, "bwd");
        timeModel[0][0] = timeDown;
        timeDown.setWidth(ww);
        timeDown.addListener(event -> {
            if (event instanceof ChangeEvent) {
                em.post(Events.TIME_WARP_DECREASE_CMD);
                return true;
            }
            return false;
        });
        timeReset = new OwnTextIconButton("Reset time", Align.center, skin, "reload");
        timeModel[1][1] = timeReset;
        timeReset.setWidth(ww);
        timeReset.addListener(event -> {
            if (event instanceof ChangeEvent) {
                em.post(Events.TIME_CHANGE_CMD, Instant.now());
                return true;
            }
            return false;
        });

        timeT.add(timeDown).padBottom(pad30).padRight(pad10);
        timeT.add(timeStartStop).padBottom(pad30).padRight(pad10);
        timeT.add(timeUp).padBottom(pad30).row();
        timeT.add(timeReset).padRight(pad10).padLeft(pad10).colspan(3).padBottom(pad10).row();
        tabContents.add(container(timeT, w, h));
        updatePads(timeT);

        // TYPES
        int visTableCols = 6;
        Actor[][] typesModel = new Actor[visTableCols][7];
        model.add(typesModel);

        typesT = new Table(skin);

        float buttonPadHor = (GlobalConf.isHiDPI() ? 6f : 4f) * GlobalConf.UI_SCALE_FACTOR;
        float buttonPadVert = (GlobalConf.isHiDPI() ? 2.5f : 2.2f) * GlobalConf.UI_SCALE_FACTOR;
        Set<Button> buttons = new HashSet<>();
        ComponentType[] visibilityEntities = ComponentType.values();
        boolean[] visible = new boolean[visibilityEntities.length];
        for (int i = 0; i < visibilityEntities.length; i++)
            visible[i] = SceneGraphRenderer.visible.get(visibilityEntities[i].ordinal());

        if (visibilityEntities != null) {
            int di = 0, dj = 0;
            for (int i = 0; i < visibilityEntities.length; i++) {
                final ComponentType ct = visibilityEntities[i];
                final String name = ct.getName();
                if (name != null) {
                    Button button;
                    if (ct.style != null) {
                        Image icon = new Image(skin.getDrawable(ct.style));
                        button = new OwnTextIconButton("", icon, skin, "toggle");
                    } else {
                        button = new OwnTextButton(name, skin, "toggle");
                    }
                    // Name is the key
                    button.setName(ct.key);
                    typesModel[di][dj] = button;

                    button.setChecked(visible[i]);
                    button.addListener(event -> {
                        if (event instanceof ChangeEvent) {
                            EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, ct.key, true, ((Button) event.getListenerActor()).isChecked());
                            return true;
                        }
                        return false;
                    });
                    Cell c = typesT.add(button).padBottom(buttonPadVert).left();

                    if ((i + 1) % visTableCols == 0) {
                        typesT.row();
                        di = 0;
                        dj++;
                    } else {
                        c.padRight(buttonPadHor);
                        di++;

                    }
                    buttons.add(button);
                }
            }
        }

        typesT.setSize(w, h);
        tabContents.add(container(typesT, w, h));
        updatePads(typesT);

        // OPTIONS
        Actor[][] optionsModel = new Actor[1][4];
        model.add(optionsModel);

        optT = new Table(skin);

        // Slider
        bloomSlider = new OwnSliderPlus("Bloom", Constants.MIN_SLIDER, Constants.MAX_SLIDER * 0.2f, 1f, false, skin, "ui-15");
        bloomSlider.setWidth(sw);
        bloomSlider.setHeight(sh);
        bloomSlider.setValue(GlobalConf.postprocess.POSTPROCESS_BLOOM_INTENSITY * 10f);
        bloomSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.BLOOM_CMD, bloomSlider.getValue() / 10f, false);
                return true;
            }
            return false;
        });
        optionsModel[0][0] = bloomSlider;
        optT.add(bloomSlider).padBottom(pad10).row();

        // Lens flare
        flareButton = new OwnTextButton("Lens flare", skin, "toggle-big");
        optionsModel[0][1] = flareButton;
        flareButton.setWidth(ww);
        flareButton.setChecked(GlobalConf.postprocess.POSTPROCESS_LENS_FLARE);
        flareButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.LENS_FLARE_CMD, flareButton.isChecked(), false);
                return true;
            }
            return false;
        });
        optT.add(flareButton).padBottom(pad10).row();

        // Star glow
        starGlowButton = new OwnTextButton("Star glow", skin, "toggle-big");
        optionsModel[0][2] = starGlowButton;
        starGlowButton.setWidth(ww);
        starGlowButton.setChecked(GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING);
        starGlowButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.LIGHT_SCATTERING_CMD, starGlowButton.isChecked(), false);
                return true;
            }
            return false;
        });
        optT.add(starGlowButton).padBottom(pad10).row();

        // Motion blur
        motionBlurButton = new OwnTextButton("Motion blur", skin, "toggle-big");
        optionsModel[0][3] = motionBlurButton;
        motionBlurButton.setWidth(ww);
        motionBlurButton.setChecked(GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR);
        motionBlurButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.MOTION_BLUR_CMD, motionBlurButton.isChecked(), false);
                return true;
            }
            return false;
        });
        optT.add(motionBlurButton);

        tabContents.add(container(optT, w, h));
        updatePads(optT);

        // SYSTEM
        Actor[][] systemModel = new Actor[1][1];
        model.add(systemModel);

        sysT = new Table(skin);

        quit = new OwnTextIconButton("Exit", Align.center, skin, "quit");
        systemModel[0][0] = quit;
        quit.setWidth(ww);
        quit.addListener(event -> {
            if (event instanceof ChangeEvent) {
                GaiaSky.postRunnable(() -> Gdx.app.exit());
                return true;
            }
            return false;
        });
        sysT.add(quit);

        tabContents.add(container(sysT, w, h));
        updatePads(sysT);

        // Create tab buttons
        cameraButton = new OwnTextButton("Camera", skin, "toggle-huge");
        tabButtons.add(cameraButton);

        timeButton = new OwnTextButton("Time", skin, "toggle-huge");
        tabButtons.add(timeButton);

        typesButton = new OwnTextButton("Types", skin, "toggle-huge");
        tabButtons.add(typesButton);

        optionsButton = new OwnTextButton("Options", skin, "toggle-huge");
        tabButtons.add(optionsButton);

        systemButton = new OwnTextButton("System", skin, "toggle-huge");
        tabButtons.add(systemButton);

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
        menu.add(typesButton).center().padBottom(pad10);
        menu.add(optionsButton).center().padBottom(pad10);
        menu.add(systemButton).center().padBottom(pad10);
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
        updateFocused(true);
    }

    @Override
    public void initialize(AssetManager assetManager) {
        // User interface
        Viewport vp = new ScreenViewport();
        ui = new Stage(vp, GlobalResources.spriteBatch);

        // Comment to hide this whole dialog and functionality
        EventManager.instance.subscribe(this, Events.SHOW_CONTROLLER_GUI_ACTION, Events.TIME_STATE_CMD);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        rebuildGui();
        ui.setKeyboardFocus(null);
    }

    @Override
    public void update(double dt) {
        super.update(dt);
        this.guiControllerListener.update();
    }

    private ScrollPane container(Table t, float w, float h) {
        OwnScrollPane c = new OwnScrollPane(t, skin, "minimalist-nobg");
        t.top();
        c.setFadeScrollBars(true);
        c.setForceScroll(false, false);
        c.setSize(w, h);
        return c;
    }

    private void updatePads(Table t) {
        Array<Cell> cells = t.getCells();
        for (Cell c : cells) {
            if (c.getActor() instanceof Button) {
                ((Button) c.getActor()).pad(pad20);
            }
        }
    }

    public void updateTabs() {
        for (OwnTextButton tb : tabButtons) {
            tb.setChecked(false);
        }
        tabButtons.get(selectedTab).setChecked(true);
        contentCell.setActor(null);
        currentModel = model.get(selectedTab);
        contentCell.setActor(tabContents.get(selectedTab));
        selectFirst();
        updateFocused();
    }

    /**
     * Selects the given object. If it is null, it scans the row in the given direction until
     * all elements have been scanned.
     *
     * @param i     The column
     * @param j     The row
     * @param right Whehter scan right or left
     * @return True if the element was selected, false otherwise
     */
    public boolean selectRow(int i, int j, boolean right) {
        fi = i;
        fj = j;
        if (currentModel != null && currentModel.length > 0) {
            while (currentModel[fi][fj] == null) {
                // Move to next column
                fi = (fi + (right ? 1 : -1)) % currentModel.length;
                if (fi == i) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Selects the given object. If it is null, it scans the column in the given direction until
     * all elements have been scanned.
     *
     * @param i    The column
     * @param j    The row
     * @param down Whehter scan up or down
     * @return True if the element was selected, false otherwise
     */
    public boolean selectCol(int i, int j, boolean down) {
        fi = i;
        fj = j;
        if (currentModel != null && currentModel.length > 0) {
            while (currentModel[fi][fj] == null) {
                // Move to the next row
                fj = (fj + (down ? 1 : -1)) % currentModel[fi].length;
                if (fj == j) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void selectFirst() {
        selectRow(0, 0, true);
    }

    public void updateFocused() {
        updateFocused(false);
    }

    public void updateFocused(boolean force) {
        if ((force || content.getParent() != null) && currentModel != null && currentModel.length != 0) {
            Actor actor = currentModel[fi][fj];
            if (actor instanceof Button || actor instanceof Slider) {
                ui.setKeyboardFocus(actor);
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
        selectCol(fi, update(fj, -1, currentModel[fi].length), false);
        updateFocused();
    }

    public void down() {
        selectCol(fi, update(fj, 1, currentModel[fi].length), true);
        updateFocused();
    }

    public void left() {
        selectRow(update(fi, -1, currentModel.length), fj, false);
        updateFocused();
    }

    public void right() {
        selectRow(update(fi, 1, currentModel.length), fj, false);
        updateFocused();
    }

    public void sliderUp(float percent) {
        sliderMove(true, percent);
    }

    public void sliderDown(float percent) {
        sliderMove(false, percent);
    }

    public void sliderMove(boolean up, float percent) {
        if (currentModel != null && currentModel[fi][fj] != null && currentModel[fi][fj] instanceof OwnSliderPlus) {
            OwnSliderPlus s = (OwnSliderPlus) currentModel[fi][fj];
            float max = s.getMaxValue();
            float min = s.getMinValue();
            float val = s.getValue();
            float inc = (max - min) * percent;
            s.setValue(MathUtils.clamp(val + (up ? inc : -inc), min, max));
        }
    }

    private int update(int val, int inc, int len) {
        if (len <= 0)
            return val;
        if (inc >= 0) {
            return (val + inc) % len;
        } else {
            if (val + inc < 0) {
                return inc + len;
            } else {
                return val + inc;
            }
        }
    }

    public void touchDown() {
        if (currentModel != null) {
            Actor actor = currentModel[fi][fj];
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
        if (currentModel != null) {
            Actor actor = currentModel[fi][fj];
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
        updateFocused();
        ui.setKeyboardFocus(null);
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
                ui.setKeyboardFocus(null);

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
        case TIME_STATE_CMD:
            boolean on = (Boolean) data[0];
            timeStartStop.setProgrammaticChangeEvents(false);

            timeStartStop.setChecked(on);
            timeStartStop.setText(on ? "Stop time" : "Start time");

            timeStartStop.setProgrammaticChangeEvents(true);
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
        private static final double AXIS_TH = 0.3;
        private static final long AXIS_EVT_DELAY = 250;
        private static final long AXIS_POLL_DELAY = 50;

        // Left and right stick values
        private float lStickX = 0, lStickY = 0, rStickX = 0, rStickY = 0;
        private long lastAxisEvtTime = 0, lastAxisPollTime = 0;
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
            if (Math.abs(value) > AXIS_TH && System.currentTimeMillis() - lastAxisEvtTime > AXIS_EVT_DELAY) {
                // Event-based
                if (axisCode == mappings.getAxisLstickH()) {
                    // LEFT STICK horizontal - move horizontally
                    lStickX = value;
                    if (value > 0) {
                        right();
                    } else {
                        left();
                    }
                } else if (axisCode == mappings.getAxisLstickV()) {
                    // LEFT STICK vertical - move vertically
                    lStickY = value;
                    if (value > 0) {
                        down();
                    } else {
                        up();
                    }
                }
                lastAxisEvtTime = System.currentTimeMillis();
            }
            // Poll
            if (axisCode == mappings.getAxisRstickH()) {
                // RIGHT STICK horizontal - slider up/down
                rStickX = value;
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
            // Right stick moves slider
            boolean update = System.currentTimeMillis() - lastAxisPollTime > AXIS_POLL_DELAY;
            if (Math.abs(rStickX) > AXIS_TH && update) {
                if (rStickX > 0) {
                    sliderUp(0.05f);
                } else {
                    sliderDown(0.05f);
                }
                lastAxisPollTime = System.currentTimeMillis();
            }
        }

        @Override
        public void activate() {

        }

        @Override
        public void deactivate() {

        }

    }
}
