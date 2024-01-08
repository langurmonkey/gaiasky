/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.BookmarksManager.BookmarkNode;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.view.FocusView;
import gaiasky.util.TextUtils;

public class BookmarkButton extends OwnTextIconButton {
    public BookmarkNode bookmark;
    private final FocusView view;

    public BookmarkButton(BookmarkNode bookmark, Skin skin) {
        super(TextUtils.capString(bookmark.name, 16), skin, bookmark.folder ? "bookmarks-folder" : "bookmarks-bookmark");
        this.bookmark = bookmark;
        this.view = new FocusView();

        this.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent ce) {
                var scene = GaiaSky.instance.scene;
                if (bookmark.position == null) {
                    // Object bookmark.
                    String name = bookmark.name;
                    if (scene.index().containsEntity(name)) {
                        Entity node = scene.getEntity(name);
                        if (Mapper.focus.has(node)) {
                            view.setEntity(node);
                            view.getFocus(name);
                            IFocus focus = view;
                            boolean timeOverflow = focus.isCoordinatesTimeOverflow();
                            boolean ctOn = GaiaSky.instance.isOn(focus.getCt());
                            if (!timeOverflow && ctOn) {
                                GaiaSky.postRunnable(() -> {
                                    EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE, true);
                                    EventManager.publish(Event.FOCUS_CHANGE_CMD, this, focus, true);
                                });
                            }
                        }
                    }
                } else {
                    // Position bookmark.
                    GaiaSky.postRunnable(() -> {
                        var p = bookmark.position;
                        var d = bookmark.direction;
                        var u = bookmark.up;
                        EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FREE_MODE, true);
                        EventManager.publish(Event.CAMERA_POS_CMD, this, (Object) new double[]{p.x, p.y, p.z});
                        EventManager.publish(Event.CAMERA_DIR_CMD, this, (Object) new double[]{d.x, d.y, d.z});
                        EventManager.publish(Event.CAMERA_UP_CMD, this, (Object) new double[]{u.x, u.y, u.z});
                        EventManager.publish(Event.TIME_CHANGE_CMD, this, bookmark.time);
                    });

                }
            }
            return false;
        });
    }
}
