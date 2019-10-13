/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.interfce.IMusicActors;
import gaiasky.util.I18n;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnTextTooltip;

public class DesktopMusicActors implements IMusicActors {

    @Override
    public Actor[] getActors(Skin skin) {
	ImageButton musicTooltip = new OwnImageButton(skin, "tooltip");
	musicTooltip.addListener(new OwnTextTooltip(
		I18n.bundle.format("gui.tooltip.music", SysUtils.getDefaultMusicDir()), skin));

	ImageButton reloadMusic = new OwnImageButton(skin, "reload");
	reloadMusic.setName("reload music");
	reloadMusic.addListener(new EventListener() {
	    @Override
	    public boolean handle(Event event) {
		if (event instanceof ChangeEvent) {
		    EventManager.instance.post(Events.MUSIC_RELOAD_CMD);
		    return true;
		}
		return false;
	    }
	});
	reloadMusic.addListener(new OwnTextTooltip(I18n.bundle.get("gui.music.reload"), skin));

	return new Actor[] { musicTooltip, reloadMusic };
    }

}
