/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.util.scene2d.OwnTextTooltip;

public class RunStateInterface extends TableGuiInterface implements IObserver {

    private final Cell<?> keyboardImgCell;
    private final Cell<?> stopCameraCell;
    private final Cell<?> pauseBgCell;
    private final Cell<?> frameoutputImgCell;
    private final Image keyboardImg;
    private final Image frameoutputImg;
    private final OwnTextIconButton cancelCamera;
    private final OwnTextIconButton bgLoading;
    private boolean loadingPaused = false;

    public RunStateInterface(Skin skin) {
        this(skin, false);
    }

    public RunStateInterface(Skin skin, boolean horizontal) {
        super(skin);

        float pad = 3.2f;

        keyboardImg = new Image(skin.getDrawable("no-input"));
        keyboardImg.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.noinput"), skin));
        frameoutputImg = new Image(skin.getDrawable("frameoutput"));
        frameoutputImg.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.frameoutputon"), skin));

        bgLoading = new OwnTextIconButton("", skin, "dataload-bg", "toggle");
        OwnTextTooltip pauseBgTT = new OwnTextTooltip(I18n.msg("gui.tooltip.pausebg"), skin);
        bgLoading.addListener(pauseBgTT);
        bgLoading.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (loadingPaused) {
                    EventManager.publish(Event.RESUME_BACKGROUND_LOADING, bgLoading);
                    loadingPaused = false;
                    pauseBgTT.getActor().setText(I18n.msg("gui.tooltip.pausebg"));
                } else {
                    EventManager.publish(Event.PAUSE_BACKGROUND_LOADING, bgLoading);
                    loadingPaused = true;
                    pauseBgTT.getActor().setText(I18n.msg("gui.tooltip.resumebg"));
                }
            }
            return false;
        });

        cancelCamera = new OwnTextIconButton("", skin, "camera-stop");
        cancelCamera.addListener(new OwnTextTooltip(I18n.msg("gui.stop"), skin));
        cancelCamera.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.STOP_CAMERA_PLAY, cancelCamera);
            }
            return false;
        });

        if (horizontal) {
            // Horizontal cells, centered
            pauseBgCell = this.add().bottom();
            stopCameraCell = this.add().bottom().padLeft(pad);
            keyboardImgCell = this.add().bottom().padLeft(pad);
            frameoutputImgCell = this.add().bottom().padLeft(pad);
        } else {
            // Vertical cells, aligned right
            pauseBgCell = this.add().right().padTop(pad);
            pauseBgCell.row();
            stopCameraCell = this.add().right().padTop(pad);
            stopCameraCell.row();
            keyboardImgCell = this.add().right().padTop(pad);
            keyboardImgCell.row();
            frameoutputImgCell = this.add().right().padTop(pad);
            frameoutputImgCell.row();
        }

        EventManager.instance.subscribe(this, Event.INPUT_ENABLED_CMD, Event.CAMERA_PLAY_INFO, Event.BACKGROUND_LOADING_INFO, Event.FRAME_OUTPUT_CMD, Event.OCTREE_DISPOSED);
    }

    private void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case INPUT_ENABLED_CMD:
            GaiaSky.postRunnable(() -> {
                boolean disabled = !(boolean) data[0];
                if (disabled) {
                    if (keyboardImgCell.getActor() == null)
                        keyboardImgCell.setActor(keyboardImg);
                } else {
                    keyboardImgCell.setActor(null);
                }
            });
            break;
        case FRAME_OUTPUT_CMD:
            GaiaSky.postRunnable(() -> {
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
            GaiaSky.postRunnable(() -> {
                boolean visible = (boolean) data[0];
                if (visible) {
                    if (stopCameraCell.getActor() == null)
                        stopCameraCell.setActor(cancelCamera);
                } else {
                    stopCameraCell.setActor(null);
                }
            });

            break;
        case BACKGROUND_LOADING_INFO:
            GaiaSky.postRunnable(() -> {
                if (pauseBgCell.getActor() == null)
                    pauseBgCell.setActor(bgLoading);
            });
            break;
        case OCTREE_DISPOSED:
            GaiaSky.postRunnable(() -> {
                if (pauseBgCell.getActor() != null) {
                    pauseBgCell.clearActor();
                }
            });
            break;
        default:
            break;
        }
    }

    @Override
    public void dispose() {
        unsubscribe();
    }

    @Override
    public void update() {

    }

}
