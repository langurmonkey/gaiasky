/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextField;

public class SearchDialog extends Window {
    private final Window me;
    private final IGui gui;
    private final TextField searchInput;
    private String currentInputText = "";

    public SearchDialog(IGui gui, Skin skin, final ISceneGraph sg) {
        super(I18n.bundle.get("gui.objects.search"), skin);
        this.me = this;
        this.gui = gui;
        searchInput = new OwnTextField("", skin);
        searchInput.setWidth(150 * GlobalConf.SCALE_FACTOR);
        searchInput.setMessageText(I18n.bundle.get("gui.objects.search"));
        searchInput.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.keyUp) {
                    if (ie.getKeyCode() == Keys.ESCAPE || ie.getKeyCode() == Keys.ENTER) {
                        me.remove();
                        return true;
                    } else if (!searchInput.getText().equals(currentInputText)) {
                        // Process only if text changed
                        currentInputText = searchInput.getText();
                        String name = currentInputText.toLowerCase();
                        if(!checkString(name, sg)){
                            if(name.matches("[0-9]+")){
                                // Check with 'HIP '
                                checkString("hip " + name, sg);
                            } else if(name.matches("hip [0-9]+") || name.matches("HIP [0-9]+")){
                                // Check without 'HIP '
                                checkString(name.substring(4), sg);
                            }
                        }
                    }

                    NaturalInputListener.pressedKeys.remove(ie.getKeyCode());
                }
            }
            return false;
        });

        HorizontalGroup buttonGroup = new HorizontalGroup();
        TextButton cls = new OwnTextButton(I18n.bundle.get("gui.close"), skin, "default");
        cls.setName("close");
        cls.addListener(event -> {
            if (event instanceof ChangeEvent) {
                me.remove();
                return true;
            }

            return false;
        });
        buttonGroup.addActor(cls);
        cls.setSize(70 * GlobalConf.SCALE_FACTOR, 20 * GlobalConf.SCALE_FACTOR);
        buttonGroup.align(Align.right).space(10 * GlobalConf.SCALE_FACTOR);

        add(searchInput).top().left().expand().row();
        add(buttonGroup).pad(5 * GlobalConf.SCALE_FACTOR, 0, 0, 0).bottom().right().expand();
        getTitleTable().align(Align.left);
        setModal(false);
        pack();

        this.setPosition(Math.round(gui.getGuiStage().getWidth() / 2f - this.getWidth() / 2f), Math.round(gui.getGuiStage().getHeight() / 2f - this.getHeight() / 2f));

    }

    public boolean checkString(String text, ISceneGraph sg) {
        if (sg.containsNode(text)) {
            SceneGraphNode node = sg.getNode(text);
            if (node instanceof IFocus) {
                IFocus focus = ((IFocus) node).getFocus(text);
                if (focus != null && !focus.isCoordinatesTimeOverflow() && GaiaSky.instance.isOn(focus.getCt())) {
                    Gdx.app.postRunnable(() -> {
                        EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.Focus, true);
                        EventManager.instance.post(Events.FOCUS_CHANGE_CMD, focus, true);
                    });
                    return true;
                }
            }
        }
        return false;
    }

    public void clearText() {
        searchInput.setText("");
    }

    public void display() {
        if (!gui.getGuiStage().getActors().contains(me, true))
            gui.getGuiStage().addActor(this);
        gui.getGuiStage().setKeyboardFocus(searchInput);
    }
}
