/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

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
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.ISceneGraph;
import gaiasky.scenegraph.ParticleGroup;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.CatalogInfo;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextField;

import java.util.Optional;

/**
 * The dialog to search objects. It optionally presents the user with a list of suggestions as the
 * user types in the name of the object.
 */
public class SearchDialog extends GenericDialog {
    private static final Log logger = Logger.getLogger(SearchDialog.class);

    private OwnTextField searchInput;
    private String currentInputText = "";
    private Cell<?> infoCell;
    private OwnLabel infoMessage;
    private final ISceneGraph sg;
    // Matching nodes
    private final Array<String> matching;
    private Array<OwnLabel> matchingLabels;
    private Table candidates;
    private int cIdx = -1;
    private Vector2 aux;
    private boolean suggestions;

    private final Array<Task> tasks;

    public SearchDialog(Skin skin, Stage ui, final ISceneGraph sg, boolean suggestions) {
        super(I18n.txt("gui.objects.search"), skin, ui);
        this.sg = sg;
        this.aux = new Vector2();
        this.matching = new Array<>(10);
        this.matchingLabels = new Array<>(10);
        this.tasks = new Array<>(20);

        setModal(false);
        setAcceptText(I18n.txt("gui.close"));

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
        searchInput.setMessageText(I18n.txt("gui.objects.search"));
        searchInput.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                int code = ie.getKeyCode();
                if (ie.getType() == Type.keyUp) {
                    if (code == Keys.ESCAPE || code == Keys.ENTER) {
                        if (cIdx >= 0) {
                            checkString(searchInput.getText(), sg);
                        }
                        removeCandidates();
                        me.remove();
                        return true;
                    } else if (code == Keys.UP) {
                        cIdx = cIdx - 1 < 0 ? matching.size - 1 : cIdx - 1;
                        selectMatch();
                    } else if (code == Keys.DOWN) {
                        cIdx = (cIdx + 1) % matching.size;
                        selectMatch();
                    } else if (!searchInput.getText().equals(currentInputText) && !searchInput.getText().isBlank()) {
                        // Process only if text changed
                        if (suggestions) {
                            currentInputText = searchInput.getText();
                            String name = currentInputText.toLowerCase().trim();

                            // New task
                            Task task = new Task() {
                                public void run() {
                                    synchronized (matching) {
                                        matchingNodes(name, sg);
                                        if (!matching.isEmpty()) {
                                            cIdx = -1;
                                            candidates.clear();
                                            int n = matching.size;
                                            for (int i = n - 1; i >= 0; i--) {
                                                String match = matching.get(i);
                                                OwnLabel m = new OwnLabel(match, skin);
                                                m.addListener((evt) -> {
                                                    if (evt instanceof InputEvent) {
                                                        InputEvent iEvt = (InputEvent) evt;
                                                        if (iEvt.getType() == Type.touchDown) {
                                                            checkString(match, sg);
                                                            searchInput.setText(match);
                                                            accept();
                                                            return true;
                                                        }
                                                    }
                                                    return false;
                                                });
                                                matchingLabels.add(m);
                                                m.setWidth(searchInput.getWidth());
                                                Cell<?> c = candidates.add(m).left().padBottom(pad5);
                                                if (i > 0) {
                                                    c.row();
                                                }
                                            }
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
                            if (!checkString(name, sg)) {
                                if (name.matches("[0-9]+")) {
                                    // Check with 'HIP '
                                    if (checkString("hip " + name, sg)) {
                                        cancelTasks();
                                        removeCandidates();
                                    }
                                } else if (name.matches("hip [0-9]+") || name.matches("HIP [0-9]+")) {
                                    // Check without 'HIP '
                                    if (checkString(name.substring(4), sg)) {
                                        cancelTasks();
                                        removeCandidates();
                                    }
                                }
                            } else {
                                cancelTasks();
                                removeCandidates();
                            }
                        }
                    } else {
                        removeCandidates();
                    }

                    if (GaiaSky.instance.getICamera() instanceof NaturalCamera)
                        ((NaturalCamera) GaiaSky.instance.getICamera()).getCurrentMouseKbdListener().removePressedKey(ie.getKeyCode());
                }
            }
            return false;
        });

        // Info message
        infoMessage = new OwnLabel("", skin, "default-blue");

        content.add(searchInput).top().left().expand().row();
        infoCell = content.add();
        infoCell.top().left().padTop(pad5).expand().row();
    }

    @Override
    public void accept() {
        cancelTasks();
        removeCandidates();
        stage.unfocusAll();
        info(null);
    }

    @Override
    public void cancel() {
        cancelTasks();
        removeCandidates();
        stage.unfocusAll();
        info(null);
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

    private void matchingNodes(String text, ISceneGraph sg) {
        matching.clear();
        matchingLabels.clear();
        sg.matchingFocusableNodes(text, matching, 10, null);
    }

    private boolean checkString(String text, ISceneGraph sg) {
        try {
            if (sg.containsNode(text)) {
                SceneGraphNode node = sg.getNode(text);
                if (node instanceof IFocus) {
                    IFocus focus = ((IFocus) node).getFocus(text);
                    boolean timeOverflow = focus.isCoordinatesTimeOverflow();
                    boolean canSelect = !(focus instanceof ParticleGroup) || ((ParticleGroup) focus).canSelect();
                    boolean ctOn = GaiaSky.instance.isOn(focus.getCt());
                    Optional<CatalogInfo> ci = GaiaSky.instance.getCatalogInfoFromObject(node);
                    boolean datasetVisible = ci.isEmpty() || ci.get().isVisible(true);
                    if (!timeOverflow && canSelect && ctOn && datasetVisible) {
                        GaiaSky.postRunnable(() -> {
                            EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE, true);
                            EventManager.instance.post(Events.FOCUS_CHANGE_CMD, focus, true);
                        });
                        info(null);
                    } else if (timeOverflow) {
                        info(I18n.txt("gui.objects.search.timerange", text));
                    } else if (!canSelect) {
                        info(I18n.txt("gui.objects.search.filter", text));
                    } else if (!datasetVisible) {
                        info(I18n.txt("gui.objects.search.dataset.invisible", text, ci.get().name));
                    } else {
                        info(I18n.txt("gui.objects.search.invisible", text, focus.getCt().toString()));
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
