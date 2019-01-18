package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaia.cu9.ari.gaiaorbit.desktop.util.camera.CameraKeyframeManager;
import gaia.cu9.ari.gaiaorbit.interfce.beans.ComboBoxBean;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnSelectBox;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextField;
import gaia.cu9.ari.gaiaorbit.util.validator.IntValidator;

public class KeyframePreferencesWindow extends GenericDialog {

    OwnSelectBox<ComboBoxBean> posMethod, orientationMethod;
    OwnTextField camrecFps;

    public KeyframePreferencesWindow(Stage stage, Skin skin) {
        super("Keyframe preferences", skin, stage);
        setModal(true);

        setAcceptText(txt("gui.saveprefs"));
        setCancelText(txt("gui.cancel"));

        buildSuper();
    }

    @Override
    protected void build() {

        ComboBoxBean[] interpolation = new ComboBoxBean[] { new ComboBoxBean("Linear", CameraKeyframeManager.PathType.LINEAR.ordinal()), new ComboBoxBean("Catmull-Rom Spline", CameraKeyframeManager.PathType.SPLINE.ordinal()) };

        OwnLabel generalTitle = new OwnLabel("General", skin, "hud-header");

        // fps
        OwnLabel camfpsLabel = new OwnLabel("Target frames per second", skin);
        camrecFps = new OwnTextField(Integer.toString(GlobalConf.frame.CAMERA_REC_TARGET_FPS), skin, new IntValidator(1, 200));
        camrecFps.setWidth(60 * GlobalConf.SCALE_FACTOR * 3f);

        OwnLabel interpTitle = new OwnLabel("Interpolation method", skin, "hud-header");

        // Camera position
        OwnLabel pos = new OwnLabel("Camera position", skin);
        posMethod = new OwnSelectBox<>(skin);
        posMethod.setItems(interpolation);
        posMethod.setSelectedIndex(GlobalConf.frame.KF_PATH_TYPE_POSITION.ordinal());
        posMethod.setItems(interpolation);

        // Camera orientation
        OwnLabel orientation = new OwnLabel("Camera orientation", skin);
        orientationMethod = new OwnSelectBox<>(skin);
        orientationMethod.setItems(interpolation);
        orientationMethod.setSelectedIndex(GlobalConf.frame.KF_PATH_TYPE_ORIENTATION.ordinal());
        orientationMethod.setItems(interpolation);

        // Time
        OwnLabel time = new OwnLabel("Time", skin);
        OwnLabel timeMethod = new OwnLabel("Linear", skin);

        content.add(generalTitle).left().top().colspan(2).padBottom(pad).row();

        content.add(camfpsLabel).left().padRight(pad).padBottom(pad5);
        content.add(camrecFps).left().padBottom(pad5).padBottom(pad).row();

        content.add(interpTitle).left().top().colspan(2).padBottom(pad).row();

        content.add(pos).left().padRight(pad).padBottom(pad5);
        content.add(posMethod).left().padBottom(pad5).row();

        content.add(orientation).left().padRight(pad).padBottom(pad5);
        content.add(orientationMethod).left().padBottom(pad5).row();

        content.add(time).left().padRight(pad).padBottom(pad5);
        content.add(timeMethod).left().padBottom(pad5).row();

    }

    @Override
    protected void accept() {
        GlobalConf.frame.CAMERA_REC_TARGET_FPS = Integer.parseInt(camrecFps.getText());
        GlobalConf.frame.KF_PATH_TYPE_POSITION = CameraKeyframeManager.PathType.values()[posMethod.getSelectedIndex()];
        GlobalConf.frame.KF_PATH_TYPE_ORIENTATION = CameraKeyframeManager.PathType.values()[orientationMethod.getSelectedIndex()];
    }

    @Override
    protected void cancel() {

    }
}
