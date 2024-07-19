/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.script.IScriptingInterface;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ConsoleInterface extends TableGuiInterface {
    private static final Logger.Log logger = Logger.getLogger(ConsoleInterface.class.getSimpleName());

    private final Table inputTable;
    private final OwnTextField input;
    private final Table output;
    private final OwnScrollPane outputScroll;
    private final OwnTextIconButton close;
    private final OwnLabel prompt;
    int historyIndex = -1;
    float pad = 10f;

    private Map<String, String> shortcutMap;

    private final static Array<Message> console = new Array<>();
    private final static Array<String> cmdHistory = new Array<>();
    private static Map<String, Array<Method>> methodMap;

    record Message(String msg, MsgType type, Instant time) {
    }

    private enum MsgType {
        INFO(ColorUtils.gYellowC),
        ERROR(ColorUtils.gRedC),
        RETURN(ColorUtils.gBlueC),
        OK(ColorUtils.gGreenC);

        private final Color color;

        MsgType(Color color) {
            this.color = color;
        }

        public String getCodeString() {
            return I18n.msg("gui." + this.toString().toLowerCase() + ".code");
        }

        public Color getColor() {
            return color;
        }

    }

    public ConsoleInterface(final Skin skin) {
        super(skin);
        initializeMethodMap();

        close = new OwnTextIconButton("", skin, "quit");
        close.setSize(33, 30);
        close.addListener(event -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                closeConsole();
            }
            return false;
        });
        close.addListener(new OwnTextTooltip(I18n.msg("gui.close"), skin));

        prompt = new OwnLabel(">", skin, "header-s");

        input = new OwnTextField("", skin, "monospace-txt");

        input.addListener((event) -> {
            if (event instanceof InputEvent ie && this.getParent() != null) {
                if (ie.getType() == InputEvent.Type.keyTyped) {
                    var key = ie.getKeyCode();
                    // Mask TILDE (GRAVE).
                    if (key == GSKeys.GRAVE) {
                        input.setProgrammaticChangeEvents(false);
                        var text = input.getText();
                        text = text.substring(0, text.length() - 1);
                        input.setText(text);
                        input.setProgrammaticChangeEvents(true);
                    } else {
                        historyIndex = -1;
                    }

                } else if (ie.getType() == InputEvent.Type.keyDown) {
                    // ESCAPE, TILDE (GRAVE).
                    switch (ie.getKeyCode()) {
                        case GSKeys.ENTER -> {
                            // Submit.
                            processCommand(input.getText());
                            input.setText("");
                        }
                        case GSKeys.GRAVE, GSKeys.ESC, GSKeys.CAPS_LOCK ->
                            // Close.
                                this.closeConsole();
                        case GSKeys.UP -> {
                            if (cmdHistory.isEmpty()) break;
                            // History up.
                            if (historyIndex == -1) {
                                historyIndex = Math.max(0, cmdHistory.size - 1);
                            } else {
                                historyIndex = Math.max(0, historyIndex - 1);
                            }
                            input.setProgrammaticChangeEvents(false);
                            input.setText(cmdHistory.get(historyIndex));
                            input.setProgrammaticChangeEvents(true);
                        }
                        case GSKeys.DOWN -> {
                            if (cmdHistory.isEmpty()) break;
                            // History down.
                            historyIndex = Math.min(cmdHistory.size - 1, historyIndex + 1);
                            input.setProgrammaticChangeEvents(false);
                            input.setText(cmdHistory.get(historyIndex));
                            input.setProgrammaticChangeEvents(true);
                        }
                    }
                }
            }
            return false;
        });
        inputTable = new Table(skin);
        inputTable.add(prompt).left().padLeft(pad).padRight(pad);
        inputTable.add(input).left();

        output = new Table(skin);
        output.top().left();
        outputScroll = new OwnScrollPane(output, skin, "default-nobg");
        outputScroll.setHeight(400f);
        outputScroll.setForceScroll(false, true);
        outputScroll.setSmoothScrolling(true);
        outputScroll.setFadeScrollBars(false);

        try {
            initShortcuts();
        } catch (NoSuchMethodException e) {
            logger.error("Error initializing console shortcut commands");
        }
        rebuildMainTable();
        pack();

        if (console.isEmpty()) {
            addOutputInfo(I18n.msg("gui.console.welcome"));
        } else {
            restoreConsoleMessages();
        }

    }

    private void rebuildMainTable() {
        var upp = GaiaSky.instance.getUnitsPerPixel();
        float w = (Gdx.graphics.getWidth() - pad - prompt.getWidth()) * upp - 500f;
        input.setWidth(w - pad * 2f);
        outputScroll.setWidth(w);
        Table mainTable = new Table(getSkin());
        mainTable.pad(pad);
        mainTable.setBackground("bg-pane-border-dark");
        mainTable.center();
        mainTable.add(new OwnLabel("  " + I18n.msg("gui.console.title"), getSkin(), "header-s")).left().padBottom(pad);
        mainTable.add(close).right().padBottom(pad).row();
        mainTable.add(outputScroll).colspan(2).width(w).left().padLeft(pad).fillX().row();
        mainTable.add(inputTable).colspan(2).left().width(w).padLeft(pad).fillX().pad(0);
        mainTable.pack();

        // Add to parent.
        clearChildren();
        add(mainTable);
    }

    private void initShortcuts() throws NoSuchMethodException {
        shortcutMap = new HashMap<>();
        shortcutMap.put("goto", "goToObject");
        shortcutMap.put("focus", "setCameraFocus");
        shortcutMap.put("camerafree", "setCameraFree");
        shortcutMap.put("starttime", "startSimulationTime");
        shortcutMap.put("stoptime", "stopSimulationTime");
    }

    public void showConsole() {
        rebuildMainTable();
        input.getStage().setKeyboardFocus(input);
        this.addAction(Actions.sequence(
                Actions.alpha(0f),
                Actions.fadeIn(Settings.settings.program.ui.getAnimationSeconds())));
    }

    public void closeConsole() {
        this.addAction(Actions.sequence(
                Actions.alpha(1f),
                Actions.fadeOut(Settings.settings.program.ui.getAnimationSeconds()),
                Actions.run(this::remove)));
    }

    public boolean remove() {
        input.getStage().setKeyboardFocus(null);
        return super.remove();
    }

    private void restoreConsoleMessages() {
        output.clearChildren(true);
        for (var msg : console) {
            addMessageWidget(msg);
        }
    }

    private void addOutputInfo(String msg) {
        addOutput(msg, MsgType.INFO);
    }

    private void addOutputOk(String msg) {
        addOutput(msg, MsgType.OK);
    }

    private void addOutputReturn(String msg) {
        addOutput(msg, MsgType.RETURN);
    }

    private void addOutputError(String err) {
        addOutput(err, MsgType.ERROR);
    }

    private final Vector2 vec2 = new Vector2();

    private void addOutput(String messageText, final MsgType type) {
        Message msg = new Message(messageText, type, Instant.now());
        addMessageWidget(msg);
        console.add(msg);
    }

    private void addMessageWidget(Message msg) {
        OwnLabel status = new OwnLabel(msg.type.getCodeString(), getSkin(), "mono");
        status.setColor(msg.type.getColor());

        output.add(status).left().top().padRight(pad * 2f);
        output.add(new OwnLabel(TextUtils.breakCharacters(msg.msg, (int) (outputScroll.getWidth() * 0.05)), getSkin(), "mono")).left().top().row();

        var coordinates = status.localToAscendantCoordinates(output, vec2.set(status.getX(), status.getY()));
        outputScroll.scrollTo(coordinates.x, coordinates.y, status.getWidth(), status.getHeight());
    }


    private void processCommand(String cmd) {
        if (cmd == null || cmd.isEmpty()) {
            return;
        }
        cmd = cmd.trim();
        // Add to command history.
        cmdHistory.add(cmd);

        // Split command and parameters.
        String command0;
        String[] parameters = null;
        if (cmd.contains(" ")) {
            int idx = cmd.indexOf(" ");
            command0 = cmd.substring(0, idx);
            parameters = cmd.substring(idx + 1).split("\\s+");
        } else {
            command0 = cmd;
        }
        var numParams = parameters != null ? parameters.length : 0;

        // Convert shortcut.
        String command = command0;
        if (shortcutMap.containsKey(command0)) {
            command = shortcutMap.get(command0);
        }

        // Process command.
        if ("help".equals(command)) {
            addOutputOk(command);
            addOutputInfo("List of available API calls:");

            methodMap.keySet().stream().sorted().forEach(a -> {
                var b = methodMap.get(a);
                b.forEach(m -> {
                    StringBuilder sb = new StringBuilder(m.getName());
                    var params = m.getParameters();
                    Arrays.stream(params).forEach(p -> {
                        sb.append(" ").append(p.getName()).append("[").append(p.getType().getSimpleName()).append("]");
                    });
                    addOutputInfo("  " + sb);
                });
            });
            addOutputInfo("");
            addOutputInfo("List of available shortcuts:");
            shortcutMap.keySet().stream().sorted().forEach(a -> {
                var b = shortcutMap.get(a);
                addOutputInfo("  " + a + " :=: " + b);
            });
        } else if (methodMap.containsKey(command)) {
            var methods = methodMap.get(command);

            var matched = 0;
            // Match parameters.
            for (var method : methods) {
                var params = method.getParameters();

                if (numParams == params.length) {
                    matched++;
                    // Match, prepare arguments.
                    Object[] arguments = new Object[params.length];
                    Class<?>[] types = new Class[params.length];

                    boolean ok = true;
                    for (int i = 0; i < params.length; i++) {
                        Parameter p = params[i];
                        var stringValue = parameters[i];

                        // Set type.
                        types[i] = p.getType();

                        /*
                         * Set argument value, handling depends on type: We test against both primitives
                         * (.TYPE) and objects (.class). You might need to add more in the future if you
                         * trigger exceptions here...
                         */
                        try {
                            if (Integer.TYPE.equals(types[i]) || Integer.class.equals(types[i])) {
                                arguments[i] = Integer.parseInt(stringValue);

                            } else if (Long.TYPE.equals(types[i]) || Long.class.equals(types[i])) {
                                arguments[i] = Long.parseLong(stringValue);

                            } else if (Float.TYPE.equals(types[i]) || Float.class.equals(types[i])) {
                                arguments[i] = Float.parseFloat(stringValue);

                            } else if (Double.TYPE.equals(types[i]) || Double.class.equals(types[i])) {
                                arguments[i] = Double.parseDouble(stringValue);

                            } else if (Boolean.TYPE.equals(types[i]) || Boolean.class.equals(types[i])) {
                                arguments[i] = Boolean.parseBoolean(stringValue);

                            } else if (int[].class.equals(types[i]) || Integer[].class.equals(types[i])) {
                                String[] svec = splitArrayString(stringValue);
                                int[] dvec = new int[svec.length];
                                for (int vi = 0; vi < svec.length; vi++) {
                                    dvec[vi] = Integer.parseInt(svec[vi]);
                                }
                                arguments[i] = dvec;

                            } else if (float[].class.equals(types[i]) || Float[].class.equals(types[i])) {
                                String[] svec = splitArrayString(stringValue);
                                float[] dvec = new float[svec.length];
                                for (int vi = 0; vi < svec.length; vi++) {
                                    dvec[vi] = Float.parseFloat(svec[vi]);
                                }
                                arguments[i] = dvec;

                            } else if (double[].class.equals(types[i]) || Double[].class.equals(types[i])) {
                                String[] svec = splitArrayString(stringValue);
                                double[] dvec = new double[svec.length];
                                for (int vi = 0; vi < svec.length; vi++) {
                                    dvec[vi] = Double.parseDouble(svec[vi]);
                                }
                                arguments[i] = dvec;

                            } else if (String[].class.equals(types[i])) {
                                String[] svec = splitArrayString(stringValue);
                                arguments[i] = svec;

                            } else {
                                // String also if it is some other, will raise exception
                                arguments[i] = stringValue;
                            }

                        } catch (IllegalArgumentException e) {
                            String msg = String.format("Argument failure with parameter '%s'", p.getName());
                            logger.warn(msg);
                            ok = false;
                        }
                    }

                    if (ok) {
                        /* Invoke method */
                        try {
                            // note: invoke may return null explicitly or because is void type
                            Object returnObject = method.invoke(GaiaSky.instance.scripting(), arguments);
                            addOutputOk(cmd);

                            if (returnObject != null) {
                                String returnStr;
                                var clazz = returnObject.getClass();
                                if (int[].class.equals(clazz)) {
                                    var obj = (int[]) returnObject;
                                    returnStr = Arrays.toString(obj);
                                } else if (Integer[].class.equals(clazz)) {
                                    var obj = (Integer[]) returnObject;
                                    returnStr = Arrays.toString(obj);
                                } else if (float[].class.equals(clazz)) {
                                    var obj = (float[]) returnObject;
                                    returnStr = Arrays.toString(obj);
                                } else if (Float[].class.equals(clazz)) {
                                    var obj = (Float[]) returnObject;
                                    returnStr = Arrays.toString(obj);
                                } else if (double[].class.equals(clazz)) {
                                    var obj = (double[]) returnObject;
                                    returnStr = Arrays.toString(obj);
                                } else if (Double[].class.equals(clazz)) {
                                    var obj = (Double[]) returnObject;
                                    returnStr = Arrays.toString(obj);
                                } else if (String[].class.equals(clazz)) {
                                    var obj = (String[]) returnObject;
                                    returnStr = Arrays.toString(obj);
                                } else {
                                    // Base type, rely on toString().
                                    returnStr = returnObject.toString();
                                }
                                addOutputReturn(returnStr);
                            }
                            break;
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }
                }
            }

            if (matched == 0) {
                addOutputError(String.format("%s: could not match command", cmd));
            }


        } else {
            addOutputError(I18n.msg("gui.console.cmd.notfound", cmd));
        }
    }

    /**
     * Converts an array-representing string and returns it as array of strings.
     * This defines how array need to be passed as HTTP request parameters:
     * comma-separated and enclosed in square brackets, e.g. "[var1,var2,var3]"
     */
    private String[] splitArrayString(String arrayString) {
        int len = arrayString.length();
        if (len >= 2 && "[".equals(arrayString.substring(0, 1)) && "]".equals(arrayString.substring(len - 1, len))) {
            return arrayString.substring(1, len - 1).split(",");
        } else {
            // probably an array should never be empty
            logger.warn("splitArrayString: '{}' is parsed as empty array!", arrayString);
            throw new IllegalArgumentException();
            // empty array
            // return new String[0];
        }
    }


    public void update() {

    }

    @Override
    public void dispose() {
    }

    private static void initializeMethodMap() {
        if (methodMap == null) {
            Class<IScriptingInterface> iScriptingInterfaceClass = IScriptingInterface.class;
            Method[] allMethods = iScriptingInterfaceClass.getDeclaredMethods();

            methodMap = new HashMap<>();
            for (Method method : allMethods) {
                Array<Method> matches;
                if (methodMap.containsKey(method.getName())) {
                    matches = methodMap.get(method.getName());
                } else {
                    matches = new Array<>(false, 1);
                }
                if (!matches.contains(method, true))
                    matches.add(method);
                methodMap.put(method.getName(), matches);
            }
        }
    }
}
