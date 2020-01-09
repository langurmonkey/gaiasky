/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.*;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnSelectBox;
import gaiasky.util.scene2d.OwnTextButton;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.FloatValidator;

import java.util.List;

public class SlaveConfigWindow extends GenericDialog {
    private static Logger.Log logger = Logger.getLogger(SlaveConfigWindow.class);

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
            if (event instanceof ChangeListener.ChangeEvent) {
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
            }
            if (pitch.isValid()) {
                float val = Parser.parseFloat(pitch.getText());
                MasterManager.instance.setSlavePitch(slave, val);
                logStr += "pitch=" + val + ";";
            }
            if (roll.isValid()) {
                float val = Parser.parseFloat(roll.getText());
                MasterManager.instance.setSlaveRoll(slave, val);
                logStr += "roll=" + val + ";";
            }
            if (fov.isValid()) {
                float val = Parser.parseFloat(fov.getText());
                MasterManager.instance.setSlaveFov(slave, val);
                logStr += "fov=" + val + ";";
            }
        }

        logger.info("New configuration sent to slave successfully: " + logStr);
    }

    @Override
    protected void cancel() {

    }
}
