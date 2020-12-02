/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.ISceneGraph;
import gaiasky.scenegraph.ParticleGroup;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextField;

public class SearchDialog extends GenericDialog {
    private static final Log logger = Logger.getLogger(SearchDialog.class);

    private OwnTextField searchInput;
    private String currentInputText = "";
    private Cell<OwnLabel> infoCell;
    private OwnLabel infoMessage;
    private final ISceneGraph sg;

    public SearchDialog(Skin skin, Stage ui,  final ISceneGraph sg) {
        super(I18n.txt("gui.objects.search"), skin, ui);
        this.sg = sg;

        setAcceptText(I18n.txt("gui.close"));

        // Build
        buildSuper();

        // Pack
        pack();
    }
    public void build() {
        // Info message
        searchInput = new OwnTextField("", skin);
        searchInput.setWidth(300 * GlobalConf.UI_SCALE_FACTOR);
        searchInput.setMessageText(I18n.txt("gui.objects.search"));
        searchInput.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.keyUp) {
                    if (ie.getKeyCode() == Keys.ESCAPE || ie.getKeyCode() == Keys.ENTER) {
                        me.remove();
                        return true;
                    } else if (!searchInput.getText().equals(currentInputText) && !searchInput.getText().isBlank()) {
                        // Process only if text changed
                        currentInputText = searchInput.getText();
                        String name = currentInputText.toLowerCase().trim();
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

                    if(GaiaSky.instance.getICamera() instanceof NaturalCamera)
                        ((NaturalCamera)GaiaSky.instance.getICamera()).getCurrentMouseKbdListener().removePressedKey(ie.getKeyCode());
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
    public void accept(){
        stage.unfocusAll();
        info(null);
    }
    @Override
    public void cancel(){
        stage.unfocusAll();
        info(null);
    }

    public boolean checkString(String text, ISceneGraph sg) {
        try {
            if (sg.containsNode(text)) {
                SceneGraphNode node = sg.getNode(text);
                if (node instanceof IFocus) {
                    IFocus focus = ((IFocus) node).getFocus(text);
                    boolean timeOverflow = focus.isCoordinatesTimeOverflow();
                    boolean canSelect = !(focus instanceof ParticleGroup) || ((ParticleGroup) focus).canSelect();
                    boolean ctOn = GaiaSky.instance.isOn(focus.getCt());
                    if (!timeOverflow && canSelect && ctOn) {
                        GaiaSky.postRunnable(() -> {
                            EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE, true);
                            EventManager.instance.post(Events.FOCUS_CHANGE_CMD, focus, true);
                        });
                        info(null);
                    } else if (timeOverflow) {
                        info(I18n.txt("gui.objects.search.timerange", text));
                    } else if(!canSelect) {
                        info(I18n.txt("gui.objects.search.filter", text));
                    } else {
                        info(I18n.txt("gui.objects.search.invisible", text, focus.getCt().toString()));
                    }
                    return true;
                }
            } else {
                info(null);
            }
        }catch(Exception e){
            logger.error(e);
        }
        return false;
    }

    private void info(String info){
        if(info == null){
            infoMessage.setText("");
            info(false);
        } else {
            infoMessage.setText(info);
            info(true);
        }
    }

    private void info(boolean visible){
        if(visible){
            infoCell.setActor(infoMessage);
        } else {
            infoCell.setActor(null);
        }
        pack();
    }

    public void clearText() {
        searchInput.setText("");
    }

    @Override
    public GenericDialog show(Stage stage, Action action) {
        GenericDialog gd = super.show(stage, action);
        // FOCUS_MODE to input
        stage.setKeyboardFocus(searchInput);
        return gd;
    }

}
