/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.*;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnSelectBox;
import gaiasky.util.scene2d.OwnTextButton;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.FloatValidator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlaveConfigWindow extends GenericDialog {
    private static Logger.Log logger = Logger.getLogger(SlaveConfigWindow.class);

    private static Map<String, Map<String, String>> parametersMap;
    static {
        parametersMap = new HashMap();
    }

    private OwnTextField yaw, pitch, roll, fov;
    private OwnSelectBox<String> slaveSelect;


    public SlaveConfigWindow(Stage stage, Skin skin) {
        super(I18n.txt("gui.slave.config.title"), skin, stage);


        setModal(false);
        setCancelText(I18n.txt("gui.close"));

        // Build UI
        buildSuper();
    }

    @Override
    protected void build() {

        FloatValidator angleVal = new FloatValidator(-1000f, 1000f);
        FloatValidator fovVal = new FloatValidator(Constants.MIN_FOV, 170f);

        float tw = 60 * GlobalConf.UI_SCALE_FACTOR;

        // Send button (does not close dialog)
        OwnTextButton sendButton = new OwnTextButton(I18n.txt("gui.send"), skin, "default");
        sendButton.setName("send");
        sendButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                accept();
                return true;
            }
            return false;

        });
        buttonGroup.addActor(sendButton);
        recalculateButtonSize();

        // Instance
        List<String> slaves = MasterManager.instance.getSlaves();
        Array<String> slaveList = new Array<>();
        for (String slave : slaves) {
            if (MasterManager.instance.isSlaveConnected(slave))
                slaveList.add(slave);
        }

        slaveSelect = new OwnSelectBox<>(skin);
        slaveSelect.setWidth(tw * 4f);
        slaveSelect.setItems(slaveList);
        slaveSelect.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                String newSlave = slaveSelect.getSelected();
                pullParameters(newSlave);
                return true;
            }
            return false;
        });
        OwnLabel slaveLabel = new OwnLabel(I18n.txt("gui.slave.config.instance") + ":", skin);
        content.add(slaveLabel).center().left().padRight(pad).padBottom(pad * 2f);
        content.add(slaveSelect).center().left().padBottom(pad * 2f).row();

        // Yaw
        yaw = new OwnTextField("", skin, angleVal);
        yaw.setWidth(tw);
        OwnLabel yawLabel = new OwnLabel(I18n.txt("gui.slave.config.yaw") + ":", skin);
        content.add(yawLabel).center().left().padRight(pad).padBottom(pad);
        content.add(yaw).center().left().padBottom(pad).row();
        // Pitch
        pitch = new OwnTextField("", skin, angleVal);
        pitch.setWidth(tw);
        OwnLabel pitchLabel = new OwnLabel(I18n.txt("gui.slave.config.pitch") + ":", skin);
        content.add(pitchLabel).center().left().padRight(pad).padBottom(pad);
        content.add(pitch).center().left().padBottom(pad).row();
        // Roll
        roll = new OwnTextField("", skin, angleVal);
        roll.setWidth(tw);
        OwnLabel rollLabel = new OwnLabel(I18n.txt("gui.slave.config.roll") + ":", skin);
        content.add(rollLabel).center().left().padRight(pad).padBottom(pad);
        content.add(roll).center().left().padBottom(pad).row();
        // FOV
        fov = new OwnTextField("", skin, fovVal);
        fov.setWidth(tw);
        OwnLabel fovLabel = new OwnLabel(I18n.txt("gui.slave.config.fov") + ":", skin);
        content.add(fovLabel).center().left().padRight(pad).padBottom(pad);
        content.add(fov).center().left().padBottom(pad).row();

        if (slaveSelect.getSelected() != null)
            pullParameters(slaveSelect.getSelected());
    }

    private void pullParameters(String slave) {
        pullParameter(slave, "yaw", yaw);
        pullParameter(slave, "pitch", pitch);
        pullParameter(slave, "roll", roll);
        pullParameter(slave, "fov", fov);
    }

    private void pullParameter(String slave, String param, OwnTextField field) {
        if (parametersMap.containsKey(slave)) {
            Map<String, String> params = parametersMap.get(slave);
            if (params.containsKey(param)) {
                field.setText(params.get(param));
            }
        }
    }

    private void pushParameters(String slave) {
        pushParameter(slave, "yaw", yaw.getText());
        pushParameter(slave, "pitch", pitch.getText());
        pushParameter(slave, "roll", roll.getText());
        pushParameter(slave, "fov", fov.getText());
    }

    private void pushParameter(String slave, String param, String value) {
        Map<String, String> params;
        if (parametersMap.containsKey(slave)) {
            params = parametersMap.get(slave);
        } else {
            params = new HashMap<>();
        }
        params.put(param, value);
        parametersMap.put(slave, params);
    }

    @Override
    protected void accept() {

        String slave = slaveSelect.getSelected();
        String logStr = "";
        if (MasterManager.instance.isSlaveConnected(slave)) {
            if (yaw.isValid()) {
                float val = Parser.parseFloat(yaw.getText());
                MasterManager.instance.setSlaveYaw(slave, val);
                logStr += "yaw=" + val + ";";
                pushParameter(slave, "yaw", yaw.getText());
            }
            if (pitch.isValid()) {
                float val = Parser.parseFloat(pitch.getText());
                MasterManager.instance.setSlavePitch(slave, val);
                logStr += "pitch=" + val + ";";
                pushParameter(slave, "pitch", pitch.getText());
            }
            if (roll.isValid()) {
                float val = Parser.parseFloat(roll.getText());
                MasterManager.instance.setSlaveRoll(slave, val);
                logStr += "roll=" + val + ";";
                pushParameter(slave, "roll", roll.getText());
            }
            if (fov.isValid()) {
                float val = Parser.parseFloat(fov.getText());
                MasterManager.instance.setSlaveFov(slave, val);
                logStr += "fov=" + val + ";";
                pushParameter(slave, "fov", fov.getText());
            }
        }

        logger.info("New configuration sent to slave successfully: " + logStr);
    }

    @Override
    protected void cancel() {

    }
}
