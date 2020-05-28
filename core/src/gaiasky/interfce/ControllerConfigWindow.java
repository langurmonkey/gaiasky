/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener.FocusEvent;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import gaiasky.GaiaSky;
import gaiasky.desktop.util.SysUtils;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Trio;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.scene2d.OwnTextTooltip;
import gaiasky.util.validator.LengthValidator;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Dialog to configure a controller interactively
 */
public class ControllerConfigWindow extends GenericDialog implements IObserver {
    private static Logger.Log logger = Logger.getLogger(ControllerConfigWindow.class);

    private static final String none = "-none-";
    private static final String button = "Button";
    private static final String axis = "Axis";

    private Texture controller, stick, stickH, stickV, dpadU, dpadD, dpadL, dpadR, startSelect, a, b, x, y, lt, rt, lb, rb;
    // For each button/axis we have the texture, the location in pixels and the name
    private Map<Gamepad, Trio<Texture, float[], String>> inputInfo;
    private Map<Gamepad, OwnTextField> inputFields;

    private String controllerName;
    private ControllerMappings mappings;

    private Gamepad currGamepad;
    private OwnTextField currTextField, filename;
    private OwnLabel currentInput;

    // The cell with the active element
    private Cell<Image> elementCell;
    private boolean jumpToNext = true;
    // Saved file, at the end, if any
    public Path savedFile = null;

    private static final int TYPE_BUTTON = 0;
    private static final int TYPE_AXIS = 1;
    private static final int TYPE_EITHER = 2;

    private enum Gamepad {
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

        public int type;

        /**
         * @param type 0 = button, 1 = axis, 2 = either
         */
        Gamepad(int type) {
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


    public ControllerConfigWindow(String controllerName, ControllerMappings mappings, Stage stage, Skin skin) {
        super("Configure controller: " + controllerName, skin, stage);
        this.controllerName = controllerName;
        this.mappings = mappings;
        if (this.mappings == null) {
            this.mappings = new ControllerMappings(this.controllerName);
        }

        setModal(true);
        setAcceptText(I18n.txt("gui.save"));
        setCancelText(I18n.txt("gui.cancel"));

        // Initialize textures
        try {
            controller = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/controller-nocontrast.png"));
            stick = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/button-stick.png"));
            stickH = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/axis-stick-h.png"));
            stickV = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/axis-stick-v.png"));
            dpadU = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/dpad-u.png"));
            dpadD = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/dpad-d.png"));
            dpadL = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/dpad-l.png"));
            dpadR = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/dpad-r.png"));
            startSelect = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/start-select.png"));
            a = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/button-a.png"));
            b = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/button-b.png"));
            x = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/button-x.png"));
            y = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/button-y.png"));
            lb = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/lb.png"));
            rb = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/rb.png"));
            lt = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/lt.png"));
            rt = new Texture(GlobalConf.assetsFileStr("img/controller/illustration/rt.png"));
        } catch (Exception e) {
            logger.error(e);
            return;
        }

        inputFields = new HashMap<>();
        inputInfo = new HashMap<>();
        // Buttons
        inputInfo.put(Gamepad.A, new Trio<>(a, new float[]{310, -50}, "Primary action"));
        inputInfo.put(Gamepad.B, new Trio<>(b, new float[]{397, -120}, "Back action"));
        inputInfo.put(Gamepad.X, new Trio<>(x, new float[]{227, -120}, "Secondary action"));
        inputInfo.put(Gamepad.Y, new Trio<>(y, new float[]{310, -190}, "Tertiary action"));
        // Left stick
        inputInfo.put(Gamepad.LSTICK, new Trio<>(stick, new float[]{-322, -122}, "Left stick click"));
        inputInfo.put(Gamepad.LSTICK_H, new Trio<>(stickH, new float[]{-322, -122}, "Left stick horizontal"));
        inputInfo.put(Gamepad.LSTICK_V, new Trio<>(stickV, new float[]{-322, -122}, "Left stick vertical"));
        // Right stick
        inputInfo.put(Gamepad.RSTICK, new Trio<>(stick, new float[]{160, 50}, "Right stick click"));
        inputInfo.put(Gamepad.RSTICK_H, new Trio<>(stickH, new float[]{160, 50}, "Right stick horizontal"));
        inputInfo.put(Gamepad.RSTICK_V, new Trio<>(stickV, new float[]{160, 50}, "Right stick vertical"));
        // Dpad
        inputInfo.put(Gamepad.DPAD_UP, new Trio<>(dpadU, new float[]{-155, 10}, "Dpad up"));
        inputInfo.put(Gamepad.DPAD_DOWN, new Trio<>(dpadD, new float[]{-155, 85}, "Dpad down"));
        inputInfo.put(Gamepad.DPAD_LEFT, new Trio<>(dpadL, new float[]{-194, 49}, "Dpad left"));
        inputInfo.put(Gamepad.DPAD_RIGHT, new Trio<>(dpadR, new float[]{-120, 49}, "Dpad right"));
        // Start/select
        inputInfo.put(Gamepad.START, new Trio<>(startSelect, new float[]{75, -170}, "Start button"));
        inputInfo.put(Gamepad.SELECT, new Trio<>(startSelect, new float[]{-75, -170}, "Select button"));
        // Bumpers
        inputInfo.put(Gamepad.LB, new Trio<>(lb, new float[]{-322, -282}, "Left bumper"));
        inputInfo.put(Gamepad.RB, new Trio<>(rb, new float[]{322, -282}, "Right bumper"));
        // Triggers
        inputInfo.put(Gamepad.LT, new Trio<>(lt, new float[]{-354, -265}, "Left trigger"));
        inputInfo.put(Gamepad.RT, new Trio<>(rt, new float[]{354, -265}, "Right trigger"));

        // Remove all controller listeners
        GlobalConf.controls.removeAllControllerListeners();

        // Park our own
        ConfigControllerListener ccl = new ConfigControllerListener();
        GlobalConf.controls.addControllerListener(ccl);

        // Build UI
        buildSuper();

        // Main tips
        OwnLabel tip = new OwnLabel("Press the button/axis indicated on the controller image. Click any input on the right\nto edit its value. Click the first input to restart the full sequence.", skin);
        content.add(tip).colspan(2).padBottom(pad * 2f).row();

        // Controller
        Cell controllerCell = content.add().padRight(pad * 2);

        Table controllerTable = new Table(skin);
        controllerTable.setBackground(new SpriteDrawable(new Sprite(controller)));
        controllerTable.setSize(controller.getWidth(), controller.getHeight());
        controllerCell.setActor(controllerTable);
        elementCell = controllerTable.add();

        // Last input
        OwnLabel currentInputLabel = new OwnLabel("Last input:", skin, "help-title");
        currentInput = new OwnLabel(none, skin, "default-blue");

        HorizontalGroup lastInputGroup = new HorizontalGroup();
        lastInputGroup.space(pad);
        lastInputGroup.addActor(currentInputLabel);
        lastInputGroup.addActor(currentInput);

        // File name
        OwnLabel fileLabel = new OwnLabel("Filename:", skin, "help-title");
        LengthValidator lv = new LengthValidator(3, 100);
        filename = new OwnTextField(this.controllerName.replaceAll("\\s+", "_"), skin, lv);
        filename.setWidth(200f * GlobalConf.UI_SCALE_FACTOR);
        OwnImageButton filenameTooltip = new OwnImageButton(skin, "tooltip");
        filenameTooltip.addListener(new OwnTextTooltip("Filename without extension.\nThe controller file will be saved in " + SysUtils.getDefaultMappingsDir().toAbsolutePath(), skin));

        HorizontalGroup filenameGroup = new HorizontalGroup();
        filenameGroup.space(pad5);
        filenameGroup.addActor(fileLabel);
        filenameGroup.addActor(filename);
        filenameGroup.addActor(filenameTooltip);

        // Table with inputs and mappings
        Table inputTable = new Table(skin);
        Gamepad[] gpds = Gamepad.values();
        for (Gamepad gpd : gpds) {
            Trio<Texture, float[], String> t = inputInfo.get(gpd);
            inputTable.add(new OwnLabel(t.getThird() + ": ", skin)).left().padBottom(pad5).padRight(pad);

            OwnTextField inputField = new OwnTextField(getMappingsValue(gpd, mappings), skin);
            Color origCol = inputField.getColor().cpy();
            inputFields.put(gpd, inputField);
            inputField.addListener(event -> {
                if (event instanceof FocusEvent) {
                    FocusEvent fe = (FocusEvent) event;
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
            inputTable.add(inputField).left().padBottom(pad5).row();
        }
        content.add(inputTable);
        content.row();
        content.add(lastInputGroup).padTop(pad);
        content.add(filenameGroup).padTop(pad);

        // Select first
        GaiaSky.postRunnable(() -> {
            stage.setKeyboardFocus(inputFields.get(gpds[0]));
        });

        content.pack();
    }

    private String getMappingsValue(Gamepad gpd, ControllerMappings m) {
        if(m == null)
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

    private void makeCurrent(Gamepad gp, OwnTextField tf) {
        this.currGamepad = gp;
        this.currTextField = tf;
    }

    private void displayElement(Gamepad input) {
        if (input != null) {
            Trio<Texture, float[], String> data = inputInfo.get(input);
            Image img = new Image(data.getFirst());

            elementCell.clearActor();
            elementCell.setActor(img).padLeft(data.getSecond()[0]).padTop(data.getSecond()[1]);
        } else {
            elementCell.clearActor();
        }
    }

    @Override
    protected void build() {
    }

    /**
     * Returns an integer array with [0] the code and [1] the type
     *
     * @param gp The gamepad input
     * @return The array with the configuration
     */
    protected int[] getInput(Gamepad gp) {
        OwnTextField i = inputFields.get(gp);
        String text = i.getText();
        if (text.equalsIgnoreCase(none)) {
            return new int[]{-1, -1};
        } else {
            String[] tokens = text.split("\\s+");
            if (tokens.length != 2) {
                logger.error("Failed to parse " + gp);
                return new int[]{-1, -1};
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
                    return new int[]{code, type};
                } catch (Exception e) {
                    logger.error("Failed to parse " + gp);
                    return new int[]{-1, -1};
                }
            }
        }
    }

    private void restoreControllerListener() {
        GlobalConf.controls.removeAllControllerListeners();
        EventManager.instance.post(Events.CONTROLLER_CONNECTED_INFO, controllerName);
    }

    @Override
    protected void accept() {
        // Generate and save mappings file
        Path mappings = SysUtils.getDefaultMappingsDir();
        Path file = mappings.resolve(filename.getText() + ".controller");

        ControllerMappings cm = this.mappings;

        // Sticks
        cm.AXIS_LSTICK_H = getInput(Gamepad.LSTICK_H)[0];
        cm.AXIS_LSTICK_V = getInput(Gamepad.LSTICK_V)[0];
        cm.BUTTON_LSTICK = getInput(Gamepad.LSTICK)[0];
        cm.AXIS_RSTICK_H = getInput(Gamepad.RSTICK_H)[0];
        cm.AXIS_RSTICK_V = getInput(Gamepad.RSTICK_V)[0];
        cm.BUTTON_RSTICK = getInput(Gamepad.RSTICK)[0];

        // Buttons
        cm.BUTTON_A = getInput(Gamepad.A)[0];
        cm.BUTTON_B = getInput(Gamepad.B)[0];
        cm.BUTTON_X = getInput(Gamepad.X)[0];
        cm.BUTTON_Y = getInput(Gamepad.Y)[0];
        cm.BUTTON_START = getInput(Gamepad.START)[0];
        cm.BUTTON_SELECT = getInput(Gamepad.SELECT)[0];

        // Dpad
        int[] dpu = getInput(Gamepad.DPAD_UP);
        if (dpu[1] == TYPE_BUTTON)
            cm.BUTTON_DPAD_UP = dpu[0];
        else
            cm.AXIS_DPAD_V = dpu[0];
        int[] dpd = getInput(Gamepad.DPAD_DOWN);
        if (dpd[1] == TYPE_BUTTON)
            cm.BUTTON_DPAD_DOWN = dpd[0];
        else
            cm.AXIS_DPAD_V = dpd[0];
        int[] dpl = getInput(Gamepad.DPAD_LEFT);
        if (dpl[1] == TYPE_BUTTON)
            cm.BUTTON_DPAD_LEFT = dpl[0];
        else
            cm.AXIS_DPAD_H = dpl[0];
        int[] dpr = getInput(Gamepad.DPAD_RIGHT);
        if (dpr[1] == TYPE_BUTTON)
            cm.BUTTON_DPAD_RIGHT = dpr[0];
        else
            cm.AXIS_DPAD_H = dpr[0];

        // Shoulder
        cm.BUTTON_RB = getInput(Gamepad.RB)[0];
        cm.BUTTON_LB = getInput(Gamepad.LB)[0];

        int[] rt = getInput(Gamepad.RT);
        if (rt[1] == TYPE_BUTTON)
            cm.BUTTON_RT = rt[0];
        else
            cm.AXIS_RT = rt[0];

        int[] lt = getInput(Gamepad.LT);
        if (lt[1] == TYPE_BUTTON)
            cm.BUTTON_LT = lt[0];
        else
            cm.AXIS_LT = lt[0];

        if (cm.persist(file)) {
            savedFile = file;
        }

        restoreControllerListener();
    }

    @Override
    protected void cancel() {
        restoreControllerListener();
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
            default:
                break;
        }
    }

    private class ConfigControllerListener implements ControllerListener {
        boolean capturingAxis = false;
        long lastT = System.currentTimeMillis();
        long lastAxisT = System.currentTimeMillis();
        long minDelayT = 400;
        long minAxisT = 500;
        double[] axes = new double[40];

        @Override
        public void connected(Controller controller) {

        }

        @Override
        public void disconnected(Controller controller) {

        }

        @Override
        public boolean buttonDown(Controller controller, int buttonCode) {
            if (currGamepad != null && currTextField != null && currGamepad.isButton() && System.currentTimeMillis() - lastT > minDelayT) {
                currTextField.setText(button + " " + buttonCode);
                jumpToNext();
                lastT = System.currentTimeMillis();
            }
            currentInput.setText(button + " " + buttonCode);
            return true;
        }

        @Override
        public boolean buttonUp(Controller controller, int buttonCode) {
            return false;
        }

        @Override
        public boolean axisMoved(Controller controller, int axisCode, float value) {
            if (currGamepad != null && currTextField != null && currGamepad.isAxis() && (System.currentTimeMillis() - lastT > minDelayT || capturingAxis)) {
                if (!capturingAxis) {
                    // Start capturing
                    capturingAxis = true;
                    lastAxisT = System.currentTimeMillis();
                    axes[axisCode] += Math.abs(value);
                } else {
                    if (System.currentTimeMillis() - lastAxisT < minAxisT) {
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
                        lastT = System.currentTimeMillis();
                    }
                }
            }
            currentInput.setText(axis + " " + axisCode);
            return false;
        }

        private void jumpToNext() {
            if (currTextField != inputFields.get(Gamepad.values()[Gamepad.values().length - 1]) && jumpToNext) {
                currTextField.next(false);
            } else {
                stage.setKeyboardFocus(null);
            }
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
    }
}
