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
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener.FocusEvent;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.util.*;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextField;

import java.util.HashMap;
import java.util.Map;

/**
 * Dialog to configure a controller interactively
 */
public class ControllerConfigWindow extends GenericDialog implements IObserver {
    private static Logger.Log logger = Logger.getLogger(ControllerConfigWindow.class);

    private Texture controller, stick, stickH, stickV, dpadU, dpadD, dpadL, dpadR, startSelect, a, b, x, y, lt, rt, lb, rb;
    // For each button/axis we have the texture, the location in pixels and the name
    private Map<Gamepad, Trio<Texture, float[], String>> inputInfo;
    private Map<Gamepad, OwnTextField> inputFields;

    private Gamepad currGamepad;
    private OwnTextField currTextField;

    // The cell with the active element
    private Cell<Image> elementCell;

    private enum Gamepad {
        A(true),
        B(true),
        X(true),
        Y(true),
        LSTICK_V(false),
        LSTICK_H(false),
        LSTICK(true),
        RSTICK_V(false),
        RSTICK_H(false),
        RSTICK(true),
        DPAD_UP(true),
        DPAD_DOWN(true),
        DPAD_RIGHT(true),
        DPAD_LEFT(true),
        START(true),
        SELECT(true),
        RB(true),
        RT(false),
        LB(true),
        LT(false);

        public boolean button;
        Gamepad(boolean button){
            this.button = button;
        }
    }


    public ControllerConfigWindow(String controllerName, Stage stage, Skin skin) {
        super("Configure controller: " + controllerName, skin, stage);

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
        inputInfo.put(Gamepad.DPAD_RIGHT, new Trio<>(dpadR, new float[]{-120, 49}, "Dpad right"));
        inputInfo.put(Gamepad.DPAD_LEFT, new Trio<>(dpadL, new float[]{-194, 49}, "Dpad left"));
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
        Cell controllerCell = content.add().padRight(pad * 2);

        Table controllerTable = new Table(skin);
        controllerTable.setBackground(new SpriteDrawable(new Sprite(controller)));
        controllerTable.setSize(controller.getWidth(), controller.getHeight());
        controllerCell.setActor(controllerTable);

        elementCell = controllerTable.add();

        // Table with inputs and mappings
        Table inputTable = new Table(skin);
        Gamepad[] gpds = Gamepad.values();
        for (Gamepad gpd : gpds) {
            Trio<Texture, float[], String> t = inputInfo.get(gpd);
            inputTable.add(new OwnLabel(t.getThird() + ": ", skin)).left().padBottom(pad5).padRight(pad);
            OwnTextField inputField = new OwnTextField("none", skin);
            Color origCol = inputField.getColor().cpy();
            String lastText = inputField.getText();
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

        // Select first
        stage.setKeyboardFocus(inputFields.get(gpds[0]));

        content.pack();
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

    @Override
    protected void accept() {
    }

    @Override
    protected void cancel() {
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
        long minDelayT = 300;
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
            logger.info("Button: " + buttonCode);
            if (currGamepad != null && currTextField != null && currGamepad.button && System.currentTimeMillis() - lastT > minDelayT) {
                currTextField.setText("Button " + buttonCode);
                currTextField.next(false);
                lastT = System.currentTimeMillis();
            }
            return true;
        }

        @Override
        public boolean buttonUp(Controller controller, int buttonCode) {
            return false;
        }

        @Override
        public boolean axisMoved(Controller controller, int axisCode, float value) {
            logger.info("Axis: " + axisCode);
            if (currGamepad != null && currTextField != null && !currGamepad.button && (System.currentTimeMillis() - lastT > minDelayT || capturingAxis)) {
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

                        currTextField.setText("Axis " + maxAxis);
                        currTextField.next(false);
                        capturingAxis = false;
                        lastT = System.currentTimeMillis();
                    }
                }
            }
            return false;
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
