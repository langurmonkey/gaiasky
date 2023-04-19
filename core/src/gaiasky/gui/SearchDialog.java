/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.view.FilterView;
import gaiasky.scene.view.FocusView;
import gaiasky.util.CatalogInfo;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextField;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

public class SearchDialog extends GenericDialog {
    private static final Log logger = Logger.getLogger(SearchDialog.class);
    private final Scene scene;
    // Matching nodes
    private final SortedSet<String> matching;
    private final Array<OwnLabel> matchingLabels;
    private final FocusView view;
    private final FilterView filterView;
    private final Vector2 aux;
    private final boolean suggestions;
    private final Array<Task> tasks;
    private OwnTextField searchInput;
    private String currentInputText = "";
    private Cell<?> infoCell;
    private OwnLabel infoMessage;
    private Table candidates;
    private int cIdx = -1;

    public SearchDialog(Skin skin, Stage ui, final Scene scene, boolean suggestions) {
        super(I18n.msg("gui.objects.search"), skin, ui);
        this.scene = scene;
        this.aux = new Vector2();
        this.matching = new TreeSet<>();
        this.matchingLabels = new Array<>(10);
        this.tasks = new Array<>(20);
        this.view = new FocusView();
        this.filterView = new FilterView();

        setModal(false);
        setAcceptText(I18n.msg("gui.close"));

        this.addListener(new InputListener() {

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                removeCandidates();
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                removeCandidates();
                super.touchDragged(event, x, y, pointer);
            }
        });

        // Build
        buildSuper();

        // Pack
        pack();

        this.suggestions = suggestions;
    }

    public void build() {
        candidates = new Table(skin);
        candidates.setBackground("table-bg");
        candidates.setFillParent(false);

        // Info message
        searchInput = new OwnTextField("", skin);
        searchInput.setWidth(480f);
        searchInput.setMessageText(I18n.msg("gui.objects.search"));
        searchInput.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                int matchingSize = matching.size();
                int code = ie.getKeyCode();
                if (ie.getType() == Type.keyUp) {
                    if (code == Keys.ESCAPE || code == Keys.ENTER) {
                        if (cIdx >= 0) {
                            checkString(searchInput.getText(), scene);
                        }
                        removeCandidates();
                        me.remove();
                        return true;
                    } else if (code == Keys.TAB && Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) && matchingSize > 0) {
                        cIdx = cIdx - 1 < 0 ? matchingSize - 1 : cIdx - 1;
                        selectMatch();
                        return true;
                    } else if (code == Keys.TAB && !Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) && matchingSize > 0) {
                        cIdx = (cIdx + 1) % matchingSize;
                        selectMatch();
                        return true;
                    } else if (!searchInput.getText().equals(currentInputText) && !searchInput.getText().isBlank()) {
                        // Process only if text changed
                        if (suggestions) {
                            currentInputText = searchInput.getText();
                            String name = currentInputText.toLowerCase().trim();

                            // New task
                            Task task = new Task() {
                                public void run() {
                                    synchronized (matching) {
                                        matchingNodes(name, scene);

                                        if (!matching.isEmpty()) {
                                            cIdx = -1;
                                            candidates.clear();
                                            matching.forEach(match -> {
                                                OwnLabel m = new OwnLabel(match, skin);
                                                m.addListener((evt) -> {
                                                    if (evt instanceof InputEvent) {
                                                        InputEvent iEvt = (InputEvent) evt;
                                                        if (iEvt.getType() == Type.touchDown) {
                                                            checkString(match, scene);
                                                            searchInput.setText(match);
                                                            accept();
                                                            return true;
                                                        }
                                                    }
                                                    return false;
                                                });
                                                matchingLabels.add(m);
                                                m.setWidth(searchInput.getWidth());
                                                Cell<?> c = candidates.add(m).left().padBottom(pad10);
                                                c.row();
                                            });
                                            candidates.pack();
                                            searchInput.localToStageCoordinates(aux.set(0, 0));
                                            candidates.setPosition(aux.x, aux.y, Align.topLeft);
                                            stage.addActor(candidates);
                                        } else {
                                            removeCandidates();
                                        }
                                    }
                                }
                            };
                            // Cancel others
                            cancelTasks();
                            tasks.add(task);
                            // Schedule with delay
                            Timer.schedule(task, 0.5f);

                            // Actually check and select
                            if (!checkString(name, scene)) {
                                if (name.matches("[0-9]+")) {
                                    // Check with 'HIP '
                                    if (checkString("hip " + name, scene)) {
                                        cancelTasks();
                                        removeCandidates();
                                    }
                                } else if (name.matches("hip [0-9]+") || name.matches("HIP [0-9]+")) {
                                    // Check without 'HIP '
                                    if (checkString(name.substring(4), scene)) {
                                        cancelTasks();
                                        removeCandidates();
                                    }
                                }
                            } else {
                                cancelTasks();
                                removeCandidates();
                            }
                            return true;
                        }
                    } else {
                        removeCandidates();
                        return true;
                    }
                }
            }
            return false;
        });

        // Info message
        infoMessage = new OwnLabel("", skin, "default-blue");

        content.add(searchInput).top().left().expand().row();
        infoCell = content.add();
        infoCell.top().left().padTop(pad10).expand().row();
    }

    @Override
    public boolean accept() {
        cancelTasks();
        removeCandidates();
        info(null);
        return true;
    }

    @Override
    public void cancel() {
        // Unused
    }

    private void cancelTasks() {
        // Cancel tasks
        if (!tasks.isEmpty()) {
            for (Task t : tasks) {
                t.cancel();
            }
        }
        tasks.clear();
    }

    private void removeCandidates() {
        if (candidates != null) {
            candidates.clear();
            candidates.remove();
        }
        cIdx = -1;
    }

    private void selectMatch() {
        for (int i = 0; i < matchingLabels.size; i++) {
            OwnLabel l = matchingLabels.get(i);
            if (i == cIdx) {
                l.setColor(ColorUtils.gYellowC);
                searchInput.setText(l.getText().toString());
            } else {
                l.setColor(ColorUtils.gWhiteC);
            }
        }
    }

    private void matchingNodes(String text, Scene scene) {
        matching.clear();
        matchingLabels.clear();
        scene.matchingFocusableNodes(text, matching, 10, null);
    }

    private boolean checkString(String text, Scene scene) {
        try {
            if (scene.index().containsEntity(text)) {
                Entity entity = scene.getEntity(text);
                if (Mapper.focus.has(entity)) {
                    view.setEntity(entity);
                    view.getFocus(text);
                    filterView.setEntity(entity);

                    boolean timeOverflow = view.isCoordinatesTimeOverflow();
                    boolean canSelect = view.getSet() == null || view.getSet().canSelect(filterView);
                    boolean ctOn = GaiaSky.instance.isOn(view.getCt());
                    Optional<CatalogInfo> ci = GaiaSky.instance.getCatalogInfoFromEntity(entity);
                    boolean datasetVisible = ci.isEmpty() || ci.get().isVisible(true);
                    if (!timeOverflow && canSelect && ctOn && datasetVisible) {
                        GaiaSky.postRunnable(() -> {
                            EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE, true);
                            EventManager.publish(Event.FOCUS_CHANGE_CMD, this, entity, true);
                        });
                        info(null);
                    } else if (timeOverflow) {
                        info(I18n.msg("gui.objects.search.timerange", text));
                    } else if (!canSelect) {
                        info(I18n.msg("gui.objects.search.filter", text));
                    } else if (!datasetVisible) {
                        info(I18n.msg("gui.objects.search.dataset.invisible", text, ci.get().name));
                    } else {
                        info(I18n.msg("gui.objects.search.invisible", text, Mapper.base.get(entity).ct.toString()));
                    }
                    return true;
                }
            } else {
                info(null);
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return false;
    }

    private void info(String info) {
        if (info == null) {
            infoMessage.setText("");
            info(false);
        } else {
            infoMessage.setText(info);
            info(true);
        }
    }

    private void info(boolean visible) {
        if (visible) {
            infoCell.setActor(infoMessage);
        } else {
            infoCell.setActor(null);
        }
        pack();
    }

    public void clearText() {
        removeCandidates();
        searchInput.setText("");
    }

    @Override
    public GenericDialog show(Stage stage, Action action) {
        GenericDialog gd = super.show(stage, action);
        // FOCUS_MODE to input
        stage.setKeyboardFocus(searchInput);
        return gd;
    }

    @Override
    public void dispose() {
    }

}
