/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.input.WindowGamepadListener;
import gaiasky.util.Logger;
import gaiasky.util.SysUtils;
import gaiasky.util.Trio;
import gaiasky.util.i18n.I18n;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.LengthValidator;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Dialog to configure a gamepad interactively.
 */
public class GamepadConfigWindow extends GenericDialog implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(GamepadConfigWindow.class);

    private final String none;
    private final String button;
    private final String axis;

    private Texture controller;
    // For each button/axis we have the texture, the location in pixels and the name
    private Map<GamepadInput, Trio<Texture, float[], String>> inputInfo;
    private Map<GamepadInput, OwnTextField> inputFields;

    private final String controllerName;
    private GamepadMappings mappings;

    private GamepadInput currGamepadInput;
    private OwnTextField currTextField, filename;
    private OwnLabel currentInput;
    private OwnSlider lsx, lsy, rsx, rsy, lts, rts, axisPower;

    // The cell with the active element
    private Cell<Image> elementCell;
    // Saved file, at the end, if any
    public Path savedFile = null;

    private static final int TYPE_BUTTON = 0;
    private static final int TYPE_AXIS = 1;
    private static final int TYPE_EITHER = 2;

    private enum GamepadInput {
        A(TYPE_BUTTON),
        B(TYPE_BUTTON),
        X(TYPE_BUTTON),
        Y(TYPE_BUTTON),
        LSTICK_V(TYPE_AXIS),
        LSTICK_H(TYPE_AXIS),
        LSTICK(TYPE_BUTTON),
        RSTICK_V(TYPE_AXIS),
        RSTICK_H(TYPE_AXIS),
        RSTICK(TYPE_BUTTON),
        DPAD_UP(TYPE_EITHER),
        DPAD_DOWN(TYPE_EITHER),
        DPAD_LEFT(TYPE_EITHER),
        DPAD_RIGHT(TYPE_EITHER),
        START(TYPE_BUTTON),
        SELECT(TYPE_BUTTON),
        RB(TYPE_BUTTON),
        RT(TYPE_EITHER),
        LB(TYPE_BUTTON),
        LT(TYPE_EITHER);

        public final int type;

        /**
         * @param type 0 = button, 1 = axis, 2 = either
         */
        GamepadInput(int type) {
            this.type = type;
        }

        public boolean isButton() {
            return type == TYPE_BUTTON || type == TYPE_EITHER;
        }

        public boolean isAxis() {
            return type == TYPE_AXIS || type == TYPE_EITHER;
        }

        public boolean isEither() {
            return type == TYPE_EITHER;
        }
    }

    public GamepadConfigWindow(String controllerName, GamepadMappings mappings, Stage stage, Skin skin) {
        super("Configure controller: " + controllerName, skin, stage);
        this.controllerName = controllerName;
        this.mappings = mappings;
        if (this.mappings == null) {
            this.mappings = new GamepadMappings(this.controllerName);
        }

        none = "-" + I18n.msg("gui.none").toLowerCase() + "-";
        button = I18n.msg("gui.controller.button");
        axis = I18n.msg("gui.controller.axis");

        setModal(true);
        setAcceptText(I18n.msg("gui.save"));
        setCancelText(I18n.msg("gui.cancel"));

        // Initialize textures
        Texture rb;
        Texture lb;
        Texture rt;
        Texture lt;
        Texture y;
        Texture x;
        Texture b;
        Texture a;
        Texture startSelect;
        Texture dPadR;
        Texture dPadL;
        Texture dPadD;
        Texture dPadU;
        Texture stickV;
        Texture stickH;
        Texture stick;
        try {
            controller = new Texture(Gdx.files.internal("img/controller/illustration/controller-nocontrast.png"));
            stick = new Texture(Gdx.files.internal("img/controller/illustration/button-stick.png"));
            stickH = new Texture(Gdx.files.internal("img/controller/illustration/axis-stick-h.png"));
            stickV = new Texture(Gdx.files.internal("img/controller/illustration/axis-stick-v.png"));
            dPadU = new Texture(Gdx.files.internal("img/controller/illustration/dpad-u.png"));
            dPadD = new Texture(Gdx.files.internal("img/controller/illustration/dpad-d.png"));
            dPadL = new Texture(Gdx.files.internal("img/controller/illustration/dpad-l.png"));
            dPadR = new Texture(Gdx.files.internal("img/controller/illustration/dpad-r.png"));
            startSelect = new Texture(Gdx.files.internal("img/controller/illustration/start-select.png"));
            a = new Texture(Gdx.files.internal("img/controller/illustration/button-a.png"));
            b = new Texture(Gdx.files.internal("img/controller/illustration/button-b.png"));
            x = new Texture(Gdx.files.internal("img/controller/illustration/button-x.png"));
            y = new Texture(Gdx.files.internal("img/controller/illustration/button-y.png"));
            lb = new Texture(Gdx.files.internal("img/controller/illustration/lb.png"));
            rb = new Texture(Gdx.files.internal("img/controller/illustration/rb.png"));
            lt = new Texture(Gdx.files.internal("img/controller/illustration/lt.png"));
            rt = new Texture(Gdx.files.internal("img/controller/illustration/rt.png"));
        } catch (Exception e) {
            logger.error(e);
            return;
        }

        inputFields = new HashMap<>();
        inputInfo = new HashMap<>();
        // Buttons
        inputInfo.put(GamepadInput.A, new Trio<>(a, new float[] { 310, -50 }, I18n.msg("gui.controller.action.primary")));
        inputInfo.put(GamepadInput.B, new Trio<>(b, new float[] { 397, -120 }, I18n.msg("gui.controller.action.back")));
        inputInfo.put(GamepadInput.X, new Trio<>(x, new float[] { 227, -120 }, I18n.msg("gui.controller.action.secondary")));
        inputInfo.put(GamepadInput.Y, new Trio<>(y, new float[] { 310, -190 }, I18n.msg("gui.controller.action.tertiary")));
        // Left stick
        inputInfo.put(GamepadInput.LSTICK, new Trio<>(stick, new float[] { -322, -122 }, I18n.msg("gui.controller.lstick.click")));
        inputInfo.put(GamepadInput.LSTICK_H, new Trio<>(stickH, new float[] { -322, -122 }, I18n.msg("gui.controller.lstick.horizontal")));
        inputInfo.put(GamepadInput.LSTICK_V, new Trio<>(stickV, new float[] { -322, -122 }, I18n.msg("gui.controller.lstick.vertical")));
        // Right stick
        inputInfo.put(GamepadInput.RSTICK, new Trio<>(stick, new float[] { 160, 50 }, I18n.msg("gui.controller.rstick.click")));
        inputInfo.put(GamepadInput.RSTICK_H, new Trio<>(stickH, new float[] { 160, 50 }, I18n.msg("gui.controller.rstick.horizontal")));
        inputInfo.put(GamepadInput.RSTICK_V, new Trio<>(stickV, new float[] { 160, 50 }, I18n.msg("gui.controller.rstick.vertical")));
        // Dpad
        inputInfo.put(GamepadInput.DPAD_UP, new Trio<>(dPadU, new float[] { -155, 10 }, I18n.msg("gui.controller.dpad.up")));
        inputInfo.put(GamepadInput.DPAD_DOWN, new Trio<>(dPadD, new float[] { -155, 85 }, I18n.msg("gui.controller.dpad.down")));
        inputInfo.put(GamepadInput.DPAD_LEFT, new Trio<>(dPadL, new float[] { -194, 49 }, I18n.msg("gui.controller.dpad.left")));
        inputInfo.put(GamepadInput.DPAD_RIGHT, new Trio<>(dPadR, new float[] { -120, 49 }, I18n.msg("gui.controller.dpad.right")));
        // Start/select
        inputInfo.put(GamepadInput.START, new Trio<>(startSelect, new float[] { 75, -170 }, I18n.msg("gui.controller.start")));
        inputInfo.put(GamepadInput.SELECT, new Trio<>(startSelect, new float[] { -75, -170 }, I18n.msg("gui.controller.select")));
        // Bumpers
        inputInfo.put(GamepadInput.LB, new Trio<>(lb, new float[] { -322, -282 }, I18n.msg("gui.controller.lb")));
        inputInfo.put(GamepadInput.RB, new Trio<>(rb, new float[] { 322, -282 }, I18n.msg("gui.controller.rb")));
        // Triggers
        inputInfo.put(GamepadInput.LT, new Trio<>(lt, new float[] { -354, -265 }, I18n.msg("gui.controller.lt")));
        inputInfo.put(GamepadInput.RT, new Trio<>(rt, new float[] { 354, -265 }, I18n.msg("gui.controller.rt")));

        // Park our own listener.
        defaultGamepadListener = false;
        gamepadListener = new GamepadConfigListener(mappings);

        // Build UI
        buildSuper();

    }

    @Override
    protected void build() {
        float lw = 224f;
        float iw = 176f;

        // Main tips
        OwnLabel tip = new OwnLabel(I18n.msg("gui.controller.tip.config"), skin);
        content.add(tip).colspan(2).padBottom(pad18 * 2f).row();

        // Controller
        Cell<?> controllerCell = content.add().padRight(pad18 * 2);

        Table controllerTable = new Table(skin);
        controllerTable.setBackground(new SpriteDrawable(new Sprite(controller)));
        controllerTable.setSize(controller.getWidth(), controller.getHeight());
        controllerCell.setActor(controllerTable);
        elementCell = controllerTable.add((Image) null);

        // Last input
        OwnLabel currentInputLabel = new OwnLabel(I18n.msg("gui.controller.lastinput") + ":", skin, "header");
        currentInput = new OwnLabel(none, skin, "default-blue");

        HorizontalGroup lastInputGroup = new HorizontalGroup();
        lastInputGroup.space(pad18);
        lastInputGroup.addActor(currentInputLabel);
        lastInputGroup.addActor(currentInput);

        // File name
        OwnLabel fileLabel = new OwnLabel(I18n.msg("gui.controller.filename") + ":", skin, "header");
        LengthValidator lv = new LengthValidator(3, 100);
        filename = new OwnTextField(this.controllerName.replaceAll("\\s+", "_"), skin, lv);
        filename.setWidth(384f);
        OwnImageButton filenameTooltip = new OwnImageButton(skin, "tooltip");
        filenameTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.controller.filename.tooltip", SysUtils.getDefaultMappingsDir().toAbsolutePath()), skin));

        HorizontalGroup filenameGroup = new HorizontalGroup();
        filenameGroup.space(pad34);
        filenameGroup.addActor(fileLabel);
        filenameGroup.addActor(filename);
        filenameGroup.addActor(filenameTooltip);

        // Table with inputs and mappings
        Table inputTable = new Table(skin);
        GamepadInput[] gpds = GamepadInput.values();
        for (GamepadInput gpd : gpds) {
            Trio<Texture, float[], String> t = inputInfo.get(gpd);
            inputTable.add(new OwnLabel(t.getThird() + ": ", skin, lw)).left().padBottom(pad10).padRight(pad18);

            OwnTextField inputField = new OwnTextField(getMappingsValue(gpd, mappings), skin);
            inputField.setWidth(iw);
            Color origCol = inputField.getColor().cpy();
            inputFields.put(gpd, inputField);
            inputField.addListener(event -> {
                if (event instanceof FocusListener.FocusEvent) {
                    FocusListener.FocusEvent fe = (FocusListener.FocusEvent) event;
                    if (fe.isFocused()) {
                        inputField.setColor(0.4f, 0.4f, 1f, 1f);
                        displayElement(gpd);
                        makeCurrent(gpd, inputField);
                    } else {
                        inputField.setColor(origCol);
                        displayElement(null);
                        makeCurrent(null, null);
                    }
                    return true;
                }
                return false;
            });
            inputTable.add(inputField).left().padBottom(pad10).row();
        }

        // Sensitivity
        lsx = new OwnSlider(0.1f, 10f, 0.1f, skin);
        lsx.setValue((float) this.mappings.AXIS_LSTICK_H_SENS);
        lsx.setWidth(iw);
        lsy = new OwnSlider(0.1f, 10f, 0.1f, skin);
        lsy.setValue((float) this.mappings.AXIS_LSTICK_V_SENS);
        lsy.setWidth(iw);
        rsx = new OwnSlider(0.1f, 10f, 0.1f, skin);
        rsx.setValue((float) this.mappings.AXIS_RSTICK_H_SENS);
        rsx.setWidth(iw);
        rsy = new OwnSlider(0.1f, 10f, 0.1f, skin);
        rsy.setValue((float) this.mappings.AXIS_RSTICK_V_SENS);
        rsy.setWidth(iw);
        lts = new OwnSlider(0.1f, 10f, 0.1f, skin);
        lts.setValue((float) this.mappings.AXIS_LT_SENS);
        lts.setWidth(iw);
        rts = new OwnSlider(0.1f, 10f, 0.1f, skin);
        rts.setValue((float) this.mappings.AXIS_RT_SENS);
        rts.setWidth(iw);
        axisPower = new OwnSlider(0.1f, 10f, 0.1f, skin);
        axisPower.setColor(1f, 0.5f, 0.5f, 1f);
        axisPower.setValue((float) this.mappings.AXIS_VALUE_POW);
        axisPower.setWidth(iw);

        Table sensitivityTable01 = new Table(skin);
        Table sensitivityTable02 = new Table(skin);

        OwnLabel titleSensitivity = new OwnLabel(I18n.msg("gui.controller.sensitivity"), skin, "header-s");

        sensitivityTable01.add(new OwnLabel(I18n.msg("gui.controller.lstick") + " X:", skin, lw)).left().padRight(pad18).padBottom(pad10);
        sensitivityTable01.add(lsx).left().padBottom(pad10).row();
        sensitivityTable01.add(new OwnLabel(I18n.msg("gui.controller.lstick") + " Y:", skin, lw)).left().padRight(pad18).padBottom(pad10);
        sensitivityTable01.add(lsy).left().padBottom(pad10).row();
        sensitivityTable01.add(new OwnLabel(I18n.msg("gui.controller.rstick") + " X:", skin, lw)).left().padRight(pad18).padBottom(pad10);
        sensitivityTable01.add(rsx).left().padBottom(pad10).row();
        sensitivityTable01.add(new OwnLabel(I18n.msg("gui.controller.rstick") + " Y:", skin, lw)).left().padRight(pad18).padBottom(pad10);
        sensitivityTable01.add(rsy).left().padBottom(pad10).row();

        sensitivityTable02.add(new OwnLabel(I18n.msg("gui.controller.lt") + ":", skin, lw)).left().padRight(pad18).padBottom(pad10);
        sensitivityTable02.add(lts).left().padBottom(pad10).row();
        sensitivityTable02.add(new OwnLabel(I18n.msg("gui.controller.rt") + ":", skin, lw)).left().padRight(pad18).padBottom(pad34);
        sensitivityTable02.add(rts).left().padBottom(pad34).row();
        sensitivityTable02.add(new OwnLabel(I18n.msg("gui.controller.axis.pow") + ":", skin, lw)).left().padRight(pad18).padBottom(pad10);
        sensitivityTable02.add(axisPower).left();

        // Add inputs and the rest
        content.add(inputTable).left();
        content.row();
        content.add(lastInputGroup).padBottom(pad18);
        content.row();
        content.add(titleSensitivity).left().colspan(2).padBottom(pad18);
        content.add();
        content.row();
        content.add(sensitivityTable01).left().top();
        content.add(sensitivityTable02).left().top();
        content.row();
        content.add(filenameGroup).colspan(2).padTop(pad34);

        // Select first
        GaiaSky.postRunnable(() -> stage.setKeyboardFocus(inputFields.get(gpds[0])));

        content.pack();
    }

    private String getMappingsValue(GamepadInput gpd, GamepadMappings m) {
        if (m == null)
            return none;

        String b = button + " ";
        String a = axis + " ";
        String out;
        switch (gpd) {
        case A:
            out = m.BUTTON_A >= 0 ? b + m.BUTTON_A : none;
            break;
        case B:
            out = m.BUTTON_B >= 0 ? b + m.BUTTON_B : none;
            break;
        case X:
            out = m.BUTTON_X >= 0 ? b + m.BUTTON_X : none;
            break;
        case Y:
            out = m.BUTTON_Y >= 0 ? b + m.BUTTON_Y : none;
            break;
        case START:
            out = m.BUTTON_START >= 0 ? b + m.BUTTON_START : none;
            break;
        case SELECT:
            out = m.BUTTON_SELECT >= 0 ? b + m.BUTTON_SELECT : none;
            break;
        case DPAD_UP:
            if (m.BUTTON_DPAD_UP >= 0) {
                // Button
                out = b + m.BUTTON_DPAD_UP;
            } else if (m.AXIS_DPAD_V >= 0) {
                // Axis
                out = a + m.AXIS_DPAD_V;
            } else {
                // None
                out = none;
            }
            break;
        case DPAD_DOWN:
            if (m.BUTTON_DPAD_DOWN >= 0) {
                // Button
                out = b + m.BUTTON_DPAD_DOWN;
            } else if (m.AXIS_DPAD_V >= 0) {
                // Axis
                out = a + m.AXIS_DPAD_V;
            } else {
                // None
                out = none;
            }
            break;
        case DPAD_LEFT:
            if (m.BUTTON_DPAD_LEFT >= 0) {
                // Button
                out = b + m.BUTTON_DPAD_LEFT;
            } else if (m.AXIS_DPAD_H >= 0) {
                // Axis
                out = a + m.AXIS_DPAD_H;
            } else {
                // None
                out = none;
            }
            break;
        case DPAD_RIGHT:
            if (m.BUTTON_DPAD_RIGHT >= 0) {
                // Button
                out = b + m.BUTTON_DPAD_RIGHT;
            } else if (m.AXIS_DPAD_H >= 0) {
                // Axis
                out = a + m.AXIS_DPAD_H;
            } else {
                // None
                out = none;
            }
            break;
        case RB:
            out = m.BUTTON_RB >= 0 ? b + m.BUTTON_RB : none;
            break;
        case RT:
            if (m.BUTTON_RT >= 0) {
                // Button
                out = b + m.BUTTON_RT;
            } else if (m.AXIS_RT >= 0) {
                // Axis
                out = a + m.AXIS_RT;
            } else {
                // None
                out = none;
            }
            break;
        case LB:
            out = m.BUTTON_LB >= 0 ? b + m.BUTTON_LB : none;
            break;
        case LT:
            if (m.BUTTON_LT >= 0) {
                // Button
                out = b + m.BUTTON_LT;
            } else if (m.AXIS_LT >= 0) {
                // Axis
                out = a + m.AXIS_LT;
            } else {
                // None
                out = none;
            }
            break;
        case LSTICK_H:
            out = m.AXIS_LSTICK_H >= 0 ? a + m.AXIS_LSTICK_H : none;
            break;
        case LSTICK_V:
            out = m.AXIS_LSTICK_V >= 0 ? a + m.AXIS_LSTICK_V : none;
            break;
        case LSTICK:
            out = m.BUTTON_LSTICK >= 0 ? b + m.BUTTON_LSTICK : none;
            break;
        case RSTICK_H:
            out = m.AXIS_RSTICK_H >= 0 ? a + m.AXIS_RSTICK_H : none;
            break;
        case RSTICK_V:
            out = m.AXIS_RSTICK_V >= 0 ? a + m.AXIS_RSTICK_V : none;
            break;
        case RSTICK:
            out = m.BUTTON_RSTICK >= 0 ? b + m.BUTTON_RSTICK : none;
            break;

        default:
            out = none;
            break;
        }
        return out;
    }

    private void makeCurrent(GamepadInput gp, OwnTextField tf) {
        this.currGamepadInput = gp;
        this.currTextField = tf;
    }

    private void displayElement(GamepadInput input) {
        if (input != null) {
            Trio<Texture, float[], String> data = inputInfo.get(input);
            Image img = new Image(data.getFirst());

            elementCell.clearActor();
            elementCell.setActor(img).padLeft(data.getSecond()[0]).padTop(data.getSecond()[1]);
        } else {
            elementCell.clearActor();
        }
    }

    /**
     * Returns an integer array with [0] the code and [1] the type.
     *
     * @param gp The gamepad input.
     *
     * @return The array with the configuration.
     */
    protected int[] getInput(GamepadInput gp) {
        OwnTextField i = inputFields.get(gp);
        String text = i.getText();
        if (text.equalsIgnoreCase(none)) {
            return new int[] { -1, -1 };
        } else {
            String[] tokens = text.split("\\s+");
            if (tokens.length != 2) {
                logger.error("Failed to parse " + gp);
                return new int[] { -1, -1 };
            } else {
                try {
                    int code = Parser.parseIntException(tokens[1]);
                    int type = gp.type;
                    if (type == TYPE_EITHER) {
                        if (tokens[0].toLowerCase().equals(button.toLowerCase())) {
                            type = TYPE_BUTTON;
                        } else {
                            type = TYPE_AXIS;
                        }
                    }
                    return new int[] { code, type };
                } catch (Exception e) {
                    logger.error("Failed to parse " + gp);
                    return new int[] { -1, -1 };
                }
            }
        }
    }

    @Override
    protected boolean accept() {
        // Generate and save mappings file
        Path mappings = SysUtils.getDefaultMappingsDir();
        Path file = mappings.resolve(filename.getText() + ".controller");

        GamepadMappings cm = this.mappings;

        // Power value
        cm.AXIS_VALUE_POW = axisPower.getValue();

        // Sticks
        cm.AXIS_LSTICK_H = getInput(GamepadInput.LSTICK_H)[0];
        cm.AXIS_LSTICK_H_SENS = lsx.getValue();
        cm.AXIS_LSTICK_V = getInput(GamepadInput.LSTICK_V)[0];
        cm.AXIS_LSTICK_V_SENS = lsy.getValue();
        cm.BUTTON_LSTICK = getInput(GamepadInput.LSTICK)[0];
        cm.AXIS_RSTICK_H = getInput(GamepadInput.RSTICK_H)[0];
        cm.AXIS_RSTICK_H_SENS = rsx.getValue();
        cm.AXIS_RSTICK_V = getInput(GamepadInput.RSTICK_V)[0];
        cm.AXIS_RSTICK_V_SENS = rsy.getValue();
        cm.BUTTON_RSTICK = getInput(GamepadInput.RSTICK)[0];

        // Buttons
        cm.BUTTON_A = getInput(GamepadInput.A)[0];
        cm.BUTTON_B = getInput(GamepadInput.B)[0];
        cm.BUTTON_X = getInput(GamepadInput.X)[0];
        cm.BUTTON_Y = getInput(GamepadInput.Y)[0];
        cm.BUTTON_START = getInput(GamepadInput.START)[0];
        cm.BUTTON_SELECT = getInput(GamepadInput.SELECT)[0];

        // Dpad
        int[] dpu = getInput(GamepadInput.DPAD_UP);
        if (dpu[1] == TYPE_BUTTON)
            cm.BUTTON_DPAD_UP = dpu[0];
        else
            cm.AXIS_DPAD_V = dpu[0];
        int[] dpd = getInput(GamepadInput.DPAD_DOWN);
        if (dpd[1] == TYPE_BUTTON)
            cm.BUTTON_DPAD_DOWN = dpd[0];
        else
            cm.AXIS_DPAD_V = dpd[0];
        int[] dpl = getInput(GamepadInput.DPAD_LEFT);
        if (dpl[1] == TYPE_BUTTON)
            cm.BUTTON_DPAD_LEFT = dpl[0];
        else
            cm.AXIS_DPAD_H = dpl[0];
        int[] dpr = getInput(GamepadInput.DPAD_RIGHT);
        if (dpr[1] == TYPE_BUTTON)
            cm.BUTTON_DPAD_RIGHT = dpr[0];
        else
            cm.AXIS_DPAD_H = dpr[0];

        // Shoulder
        cm.BUTTON_RB = getInput(GamepadInput.RB)[0];
        cm.BUTTON_LB = getInput(GamepadInput.LB)[0];

        int[] rt = getInput(GamepadInput.RT);
        if (rt[1] == TYPE_BUTTON)
            cm.BUTTON_RT = rt[0];
        else {
            cm.AXIS_RT = rt[0];
        }
        cm.AXIS_RT_SENS = rts.getValue();

        int[] lt = getInput(GamepadInput.LT);
        if (lt[1] == TYPE_BUTTON)
            cm.BUTTON_LT = lt[0];
        else {
            cm.AXIS_LT = lt[0];
        }
        cm.AXIS_LT_SENS = lts.getValue();

        if (cm.persist(file)) {
            savedFile = file;
        }
        EventManager.publish(Event.RELOAD_CONTROLLER_MAPPINGS, this, file.toAbsolutePath().toString());
        return true;
    }

    @Override
    protected void cancel() {
    }

    @Override
    public void dispose() {

    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
    }

    /**
     * Listens to gamepad input events in order to configure the axes and buttons.
     */
    private class GamepadConfigListener extends WindowGamepadListener {
        boolean capturingAxis = false;
        long lastT = System.currentTimeMillis();
        long lastAxisT = System.currentTimeMillis();
        long minDelayT = 500;
        long minAxisT = 300;
        double[] axes = new double[40];

        public GamepadConfigListener(String mappingsFile) {
            super(mappingsFile, me.stage, me);
        }

        public GamepadConfigListener(IGamepadMappings mappings) {
            super(mappings, me.stage, me);
        }

        @Override
        // Prevent axis polling.
        public boolean pollAxis() {
            return false;
        }

        @Override
        // Prevent button polling.
        public boolean pollButtons() {
            return false;
        }

        @Override
        public boolean buttonDown(Controller controller, int buttonCode) {
            if (active.get()) {
                addPressedKey(buttonCode);
                if (currTextField == null) {
                    // Not capturing.
                    if (buttonCode == mappings.getButtonA()) {
                        actionDown();
                    } else if (buttonCode == mappings.getButtonB()) {
                        back();
                    } else if (buttonCode == mappings.getButtonSelect()) {
                        select();
                    }
                } else {
                    // Capturing.
                    if (currGamepadInput != null && currGamepadInput.isButton() && System.currentTimeMillis() - lastT > minDelayT) {
                        currTextField.setText(button + " " + buttonCode);
                        jumpToNext();
                        lastT = System.currentTimeMillis();
                    }
                }
                currentInput.setText(button + " " + buttonCode);
            }
            return true;
        }

        @Override
        public boolean buttonUp(Controller controller, int buttonCode) {
            if (active.get()) {
                boolean b = super.buttonUp(controller, buttonCode);
                currentInput.setText(button + " " + buttonCode);
                return b;
            }
            return false;
        }

        @Override
        public boolean axisMoved(Controller controller, int axisCode, float value) {
            if (active.get()) {
                value = (float) applyZeroPoint(value);
                if (value != 0 && currTextField != null) {
                    // Capturing.
                    long now = TimeUtils.millis();
                    if (currGamepadInput != null && currGamepadInput.isAxis() && (now - lastT > minDelayT || capturingAxis)) {
                        if (!capturingAxis) {
                            // Start capturing
                            capturingAxis = true;
                            lastAxisT = now;
                            axes[axisCode] += Math.abs(value);
                        } else {
                            if (now - lastAxisT < minAxisT) {
                                // Just note the new values
                                axes[axisCode] += Math.abs(value);
                            } else {
                                // Finish
                                axes[axisCode] += Math.abs(value);

                                // Look for largest
                                double max = 0;
                                int maxAxis = -1;
                                for (int i = 0; i < axes.length; i++) {
                                    if (axes[i] > max) {
                                        max = axes[i];
                                        maxAxis = i;
                                    }
                                }
                                axes = new double[40];

                                currTextField.setText(axis + " " + maxAxis);
                                jumpToNext();
                                capturingAxis = false;
                                lastT = now;
                            }
                        }
                    }
                    currentInput.setText(axis + " " + axisCode);
                }
            }
            return false;
        }

        private void jumpToNext() {
            if (currTextField != inputFields.get(GamepadInput.values()[GamepadInput.values().length - 1])) {
                currTextField.next(false);
            } else {
                stage.setKeyboardFocus(acceptButton);
                currTextField = null;
            }
        }
    }
}
