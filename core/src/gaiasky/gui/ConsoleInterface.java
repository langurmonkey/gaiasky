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
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.time.Instant;

public class ConsoleInterface extends TableGuiInterface {

    private final Table inputTable;
    private final OwnTextField input;
    private final Table output;
    private final OwnScrollPane outputScroll;
    private final OwnTextIconButton close;
    private final OwnLabel prompt;
    private final IScriptingInterface scr;
    private final static Array<Message> console = new Array<>();
    private final static Array<String> cmdHistory = new Array<>();
    int historyIndex = -1;
    float pad = 10f;

    record Message(String msg, MsgType type, Instant time) {

    }

    private enum MsgType {
        INFO(ColorUtils.gYellowC),
        ERROR(ColorUtils.gRedC),
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
        scr = GaiaSky.instance.scripting();

        close = new OwnTextIconButton("", skin, "quit");
        close.setSize(33, 30);
        close.addListener(event -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                closeConsole();
                ;
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
                            if (console.isEmpty()) break;
                            // History up.
                            if (historyIndex == -1) {
                                historyIndex = Math.max(0, console.size - 1);
                            } else {
                                historyIndex = Math.max(0, historyIndex - 1);
                            }
                            input.setProgrammaticChangeEvents(false);
                            input.setText(console.get(historyIndex).msg);
                            input.setProgrammaticChangeEvents(true);
                        }
                        case GSKeys.DOWN -> {
                            if (console.isEmpty()) break;
                            // History down.
                            historyIndex = Math.min(console.size - 1, historyIndex + 1);
                            input.setProgrammaticChangeEvents(false);
                            input.setText(console.get(historyIndex).msg);
                            input.setProgrammaticChangeEvents(true);
                        }
                    }
                }
            }
            return false;
        });
        inputTable = new Table(skin);
        inputTable.add(prompt).left().padRight(pad);
        inputTable.add(input).left();

        output = new Table(skin);
        output.top().left();
        outputScroll = new OwnScrollPane(output, skin, "default-nobg");
        outputScroll.setHeight(400f);
        outputScroll.setForceScroll(false, true);
        outputScroll.setSmoothScrolling(true);
        outputScroll.setFadeScrollBars(false);

        rebuildMainTable();
        pack();

        if(console.isEmpty()) {
            addOutputInfo(I18n.msg("gui.console.welcome"));
        } else {
            restoreConsoleMessages();
        }

    }

    private void rebuildMainTable() {
        var upp = GaiaSky.instance.getUnitsPerPixel();
        input.setWidth((Gdx.graphics.getWidth() - pad - prompt.getWidth()) * upp - 500f);
        outputScroll.setWidth((Gdx.graphics.getWidth() - pad - prompt.getWidth()) * upp - 500f);
        output.setWidth(outputScroll.getWidth());
        Table mainTable = new Table(getSkin());
        mainTable.pad(pad);
        mainTable.setBackground("bg-pane-border-dark");
        mainTable.center();
        mainTable.add(new OwnLabel("  " + I18n.msg("gui.console.title"), getSkin(), "header-s")).left().padBottom(pad);
        mainTable.add(close).right().padBottom(pad).row();
        mainTable.add(outputScroll).colspan(2).left().padLeft(pad).fillX().row();
        mainTable.add(inputTable).colspan(2).left().padLeft(pad).fillX().pad(0);
        mainTable.pack();
        mainTable.debug();

        // Add to parent.
        clearChildren();
        add(mainTable);
    }

    public void showConsole() {
        rebuildMainTable();
        input.getStage().setKeyboardFocus(input);
        this.addAction(
                Actions.sequence(
                        Actions.alpha(0f),
                        Actions.fadeIn(Settings.settings.program.ui.getAnimationSeconds())));
    }

    public void closeConsole() {
        this.addAction(
                Actions.sequence(
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
        for(var msg : console) {
            addMessageWidget(msg);
        }
    }

    private void addOutputInfo(String msg) {
        addOutput(msg, MsgType.INFO);
    }

    private void addOutputOk(String msg) {
        addOutput(msg, MsgType.OK);
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
        // Add to command history.
        cmdHistory.add(cmd);

        // Process.
        if (cmd.startsWith("focus ")) {
            var objectName = cmd.substring("focus ".length());
            GaiaSky.instance.getExecutorService().execute(() -> scr.setCameraFocus(objectName));
            addOutputOk(cmd);
        } else if (cmd.startsWith("goto ")) {
            var objectName = cmd.substring("goto ".length());
            GaiaSky.instance.getExecutorService().execute(() -> scr.goToObject(objectName));
            addOutputOk(cmd);
        } else {
            addOutputError(I18n.msg("gui.console.cmd.notfound", cmd));
        }
    }


    public void update() {

    }

    @Override
    public void dispose() {
    }
}
