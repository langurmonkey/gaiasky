/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.SysUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnTextTooltip;

public class DesktopMusicActors {

    public Actor[] getActors(Skin skin) {
        ImageButton musicTooltip = new OwnImageButton(skin, "tooltip");
        musicTooltip.addListener(new OwnTextTooltip(
                I18n.msg("gui.tooltip.music", SysUtils.getDefaultMusicDir()), skin));

        ImageButton reloadMusic = new OwnImageButton(skin, "reload");
        reloadMusic.setName("reload music");
        reloadMusic.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.MUSIC_RELOAD_CMD, this);
                return true;
            }
            return false;
        });
        reloadMusic.addListener(new OwnTextTooltip(I18n.msg("gui.music.reload"), skin));

        return new Actor[] { musicTooltip, reloadMusic };
    }

}
