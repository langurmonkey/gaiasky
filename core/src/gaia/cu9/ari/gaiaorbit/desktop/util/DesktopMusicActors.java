package gaia.cu9.ari.gaiaorbit.desktop.util;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.interfce.IMusicActors;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;

public class DesktopMusicActors implements IMusicActors {

    @Override
    public Actor[] getActors(Skin skin) {
	ImageButton musicTooltip = new OwnImageButton(skin, "tooltip");
	musicTooltip.addListener(new TextTooltip(
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
	reloadMusic.addListener(new TextTooltip(I18n.bundle.get("gui.music.reload"), skin));

	return new Actor[] { musicTooltip, reloadMusic };
    }

}
