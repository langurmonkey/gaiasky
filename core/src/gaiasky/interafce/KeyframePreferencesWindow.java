/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.desktop.util.camera.CameraKeyframeManager;
import gaiasky.event.EventManager;
import gaiasky.event.Event;
import gaiasky.interafce.beans.ComboBoxBean;
import gaiasky.util.Constants;
import gaiasky.util.I18n;
import gaiasky.util.Settings;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnSelectBox;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.DoubleValidator;

public class KeyframePreferencesWindow extends GenericDialog {

    private OwnSelectBox<ComboBoxBean> posMethod, orientationMethod;
    public OwnTextField camrecFps;
    private final INumberFormat nf3;

    public KeyframePreferencesWindow(Stage stage, Skin skin) {
        super(I18n.txt("gui.keyframes.preferences"), skin, stage);
        setModal(true);
        this.nf3 = NumberFormatFactory.getFormatter("0.000");

        setAcceptText(I18n.txt("gui.saveprefs"));
        setCancelText(I18n.txt("gui.cancel"));

        buildSuper();
    }

    @Override
    protected void build() {

        ComboBoxBean[] interpolation = new ComboBoxBean[] { new ComboBoxBean(I18n.txt("gui.interpolation.linear"), CameraKeyframeManager.PathType.LINEAR.ordinal()), new ComboBoxBean(I18n.txt("gui.interpolation.catmull"), CameraKeyframeManager.PathType.SPLINE.ordinal()) };

        OwnLabel generalTitle = new OwnLabel(I18n.txt("gui.general"), skin, "hud-header");

        // fps
        OwnLabel camfpsLabel = new OwnLabel(I18n.txt("gui.target.fps"), skin);
        camrecFps = new OwnTextField(nf3.format(Settings.settings.camrecorder.targetFps), skin, new DoubleValidator(Constants.MIN_FPS, Constants.MAX_FPS));
        camrecFps.setWidth(240f);

        OwnLabel interpTitle = new OwnLabel(I18n.txt("gui.keyframes.interp"), skin, "hud-header");

        // Camera position
        OwnLabel pos = new OwnLabel(I18n.txt("gui.cam.pos"), skin);
        posMethod = new OwnSelectBox<>(skin);
        posMethod.setItems(interpolation);
        posMethod.setSelectedIndex(Settings.settings.camrecorder.keyframe.position.ordinal());
        posMethod.setItems(interpolation);
        posMethod.setWidth(240f);

        // Camera orientation
        OwnLabel orientation = new OwnLabel(I18n.txt("gui.cam.orientation"), skin);
        orientationMethod = new OwnSelectBox<>(skin);
        orientationMethod.setItems(interpolation);
        orientationMethod.setSelectedIndex(Settings.settings.camrecorder.keyframe.orientation.ordinal());
        orientationMethod.setItems(interpolation);
        orientationMethod.setWidth(240f);

        // Time
        OwnLabel time = new OwnLabel(I18n.txt("gui.time"), skin);
        OwnLabel timeMethod = new OwnLabel(I18n.txt("gui.interpolation.linear"), skin);

        content.add(generalTitle).left().top().colspan(2).padBottom(pad10).row();

        content.add(camfpsLabel).left().padRight(pad10).padBottom(pad10 * 3f);
        content.add(camrecFps).left().padBottom(pad10 * 3f).row();

        content.add(interpTitle).left().top().colspan(2).padBottom(pad10).row();

        content.add(pos).left().padRight(pad10).padBottom(pad10);
        content.add(posMethod).left().padBottom(pad10).row();

        content.add(orientation).left().padRight(pad10).padBottom(pad10);
        content.add(orientationMethod).left().padBottom(pad10).row();

        content.add(time).left().padRight(pad10).padBottom(pad10 * 3f);
        content.add(timeMethod).left().padBottom(pad10 * 3f).row();

    }

    @Override
    protected void accept() {
        EventManager.publish(Event.CAMRECORDER_FPS_CMD, this, Parser.parseDouble(camrecFps.getText()));
        Settings.settings.camrecorder.keyframe.position = CameraKeyframeManager.PathType.values()[posMethod.getSelectedIndex()];
        Settings.settings.camrecorder.keyframe.orientation = CameraKeyframeManager.PathType.values()[orientationMethod.getSelectedIndex()];
    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }
}
