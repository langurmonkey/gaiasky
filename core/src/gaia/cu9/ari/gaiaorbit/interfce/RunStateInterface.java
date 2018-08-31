package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.script.ScriptingFactory;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextIconButton;

/**
 * Contains elements which depend on the current state of the program, such as
 * the running scripts, the buttons to pause the camera subsystem, etc.
 * 
 * @author tsagrista
 *
 */
public class RunStateInterface extends Table implements IObserver, IGuiInterface {

    private Cell<?> keyboardImgCell, stopScriptCell, stopCameraCell, pauseBgCell, frameoutputImgCell;
    private Image keyboardImg, frameoutputImg;
    private TextButton cancelScript, cancelCamera, bgLoading;
    private boolean loadingPaused = false;

    public RunStateInterface(Skin skin) {
        this(skin, false);
    }

    public RunStateInterface(Skin skin, boolean horizontal) {
        super(skin);

        float pad = 2 * GlobalConf.SCALE_FACTOR;

        keyboardImg = new Image(skin.getDrawable("no-input"));
        keyboardImg.addListener(new TextTooltip(txt("gui.tooltip.noinput"), skin));
        frameoutputImg = new Image(skin.getDrawable("frameoutput"));
        frameoutputImg.addListener(new TextTooltip(txt("gui.tooltip.frameoutputon"), skin));

        Image dataloadPauseImg = new Image(skin.getDrawable("dataload-pause"));
        bgLoading = new OwnTextIconButton("", dataloadPauseImg, skin, "toggle");
        TextTooltip pauseBgTT = new TextTooltip(txt("gui.tooltip.pausebg"), skin);
        bgLoading.addListener(pauseBgTT);
        bgLoading.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (loadingPaused) {
                    EventManager.instance.post(Events.RESUME_BACKGROUND_LOADING);
                    loadingPaused = false;
                    pauseBgTT.getActor().setText(txt("gui.tooltip.pausebg"));
                } else {
                    EventManager.instance.post(Events.PAUSE_BACKGROUND_LOADING);
                    loadingPaused = true;
                    pauseBgTT.getActor().setText(txt("gui.tooltip.resumebg"));
                }
            }
            return false;
        });

        int num = ScriptingFactory.getInstance().getNumRunningScripts();
        Image scriptStopImg = new Image(skin.getDrawable("script-stop"));
        cancelScript = new OwnTextIconButton("", scriptStopImg, skin);
        cancelScript.addListener(new TextTooltip(I18n.bundle.format("gui.script.stop", num), skin));
        cancelScript.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.CANCEL_SCRIPT_CMD);
            }
            return false;
        });

        Image cameraStopImg = new Image(skin.getDrawable("camera-stop"));
        cancelCamera = new OwnTextIconButton("", cameraStopImg, skin);
        cancelCamera.addListener(new TextTooltip(I18n.bundle.get("gui.stop"), skin));
        cancelCamera.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.STOP_CAMERA_PLAY);
            }
            return false;
        });

        if (horizontal) {
            // Horizontal cells, centered
            pauseBgCell = this.add().bottom();
            stopScriptCell = this.add().bottom().padLeft(pad);
            stopCameraCell = this.add().bottom().padLeft(pad);
            keyboardImgCell = this.add().bottom().padLeft(pad);
            frameoutputImgCell = this.add().bottom().padLeft(pad);
        } else {
            // Vertical cells, aligned right
            pauseBgCell = this.add().right().padTop(pad);
            pauseBgCell.row();
            stopScriptCell = this.add().right().padTop(pad);
            stopScriptCell.row();
            stopCameraCell = this.add().right().padTop(pad);
            stopCameraCell.row();
            keyboardImgCell = this.add().right().padTop(pad);
            keyboardImgCell.row();
            frameoutputImgCell = this.add().right().padTop(pad);
            frameoutputImgCell.row();
        }

        EventManager.instance.subscribe(this, Events.INPUT_ENABLED_CMD, Events.NUM_RUNNING_SCRIPTS, Events.CAMERA_PLAY_INFO, Events.BACKGROUND_LOADING_INFO, Events.FRAME_OUTPUT_CMD, Events.OCTREE_DISPOSED);
    }

    private void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case INPUT_ENABLED_CMD:
            Gdx.app.postRunnable(() -> {
                boolean visible = !(boolean) data[0];
                if (visible) {
                    if (keyboardImgCell.getActor() == null)
                        keyboardImgCell.setActor(keyboardImg);
                } else {
                    keyboardImgCell.setActor(null);
                }
            });
            break;
        case FRAME_OUTPUT_CMD:
            Gdx.app.postRunnable(() -> {
                boolean visible = (Boolean) data[0];
                if (visible) {
                    if (frameoutputImgCell.getActor() == null)
                        frameoutputImgCell.setActor(frameoutputImg);
                } else {
                    frameoutputImgCell.setActor(null);
                }
            });
            break;
        case CAMERA_PLAY_INFO:
            Gdx.app.postRunnable(() -> {
                boolean visible = (boolean) data[0];
                if (visible) {
                    if (stopCameraCell.getActor() == null)
                        stopCameraCell.setActor(cancelCamera);
                } else {
                    stopCameraCell.setActor(null);
                }
            });

            break;
        case NUM_RUNNING_SCRIPTS:
            Gdx.app.postRunnable(() -> {
                boolean visible = (Integer) data[0] > 0;
                if (visible) {
                    if (stopScriptCell.getActor() == null)
                        stopScriptCell.setActor(cancelScript);
                } else {
                    stopScriptCell.setActor(null);
                }
            });
            break;
        case BACKGROUND_LOADING_INFO:
            Gdx.app.postRunnable(() -> {
                if (pauseBgCell.getActor() == null)
                    pauseBgCell.setActor(bgLoading);
            });
            break;
        case OCTREE_DISPOSED:
            Gdx.app.postRunnable(() -> {
                if (pauseBgCell.getActor() != null) {
                    pauseBgCell.clearActor();
                }
            });
            break;
        default:
            break;
        }
    }

    private String txt(String key) {
        return I18n.bundle.get(key);
    }

    @Override
    public void dispose() {
        unsubscribe();
    }

}
