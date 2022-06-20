/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.MusicManager;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextTooltip;

import java.text.DecimalFormat;

public class MusicComponent extends GuiComponent implements IObserver {

    protected ImageButton prev, play, next;
    protected OwnLabel vol, track, position;
    protected DecimalFormat nf;
    protected DecimalFormat intf;

    private String currentTrack;
    private int si = 0;
    private int sp = 1;
    private final int w = getTrackWindowSize();

    public MusicComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Event.MUSIC_PLAYPAUSE_CMD, Event.MUSIC_VOLUME_CMD, Event.MUSIC_NEXT_CMD, Event.MUSIC_PREVIOUS_CMD, Event.MUSIC_TRACK_INFO);
    }

    @Override
    public void initialize() {
        float componentWidth = 264f;
        nf = new DecimalFormat("##0");
        intf = new DecimalFormat("#00");

        /* Previous track */
        prev = new OwnImageButton(skin, "audio-bwd");
        prev.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.MUSIC_PREVIOUS_CMD, prev);
                return true;
            }
            return false;
        });
        prev.addListener(new OwnTextTooltip(I18n.msg("gui.music.previous"), skin));

        /* Play/pause */
        play = new OwnImageButton(skin, "audio-playpause");
        play.setChecked(false);
        play.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.MUSIC_PLAYPAUSE_CMD, play);
                return true;
            }
            return false;
        });
        play.addListener(new OwnTextTooltip(I18n.msg("gui.music.playpause"), skin));

        /* Next track */
        next = new OwnImageButton(skin, "audio-fwd");
        next.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.MUSIC_NEXT_CMD, next);
                return true;
            }
            return false;
        });
        next.addListener(new OwnTextTooltip(I18n.msg("gui.music.next"), skin));

        /* Volume */
        vol = new OwnLabel(I18n.msg("gui.music.volume.short")+ ": " + nf.format(getVolumePercentage()) + "%", skin, "mono-pink");
        vol.addListener(new OwnTextTooltip(I18n.msg("gui.music.volume"), skin));
        vol.receiveScrollEvents();
        vol.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType().equals(InputEvent.Type.scrolled)) {
                    float scroll = -ie.getScrollAmountY() * 0.1f;
                    float currentVol = getVolume();
                    float newVol = Math.max(0f, Math.min(1f, currentVol + scroll));
                    EventManager.publish(Event.MUSIC_VOLUME_CMD, vol, newVol);
                    vol.setText(I18n.msg("gui.music.volume.short") + ": " + nf.format(getVolumePercentage()) + "%");
                    return true;
                }
            }
            return false;
        });

        /* Position mm:ss */
        position = new OwnLabel(toMinutesSeconds(0f), skin, "mono");

        /* Track name */
        track = new OwnLabel("", skin, "mono");

        float space3 = 4.8f;
        VerticalGroup musicGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(space3);

        HorizontalGroup playGroup = new HorizontalGroup();
        playGroup.setWidth(componentWidth);
        playGroup.space(18f);
        prev.align(Align.left);
        play.align(Align.left);
        next.align(Align.left);
        playGroup.addActor(prev);
        playGroup.addActor(play);
        playGroup.addActor(next);
        playGroup.addActor(vol);

        HorizontalGroup trackGroup = new HorizontalGroup();
        trackGroup.space(16f);
        trackGroup.addActor(position);
        trackGroup.addActor(track);

        musicGroup.addActor(playGroup);
        musicGroup.addActor(trackGroup);

        component = musicGroup;

        Task musicUpdater = new Task() {
            @Override
            public void run() {
                position.setText(toMinutesSeconds(MusicManager.instance.getPosition()));
                slideTrackName();
            }
        };
        Timer.schedule(musicUpdater, 1, 1);

    }

    private String toMinutesSeconds(float seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return intf.format(minutes) + ":" + intf.format(secs);
    }

    private void slideTrackName() {
        if (currentTrack != null && w < currentTrack.length()) {
            int l = currentTrack.length();

            if (si + w == l && sp > 0) {
                sp *= -1;
            } else if (si <= 0 && sp < 0) {
                sp *= -1;
            }
            si += sp;

            track.setText(capStr(currentTrack, si, si + w));
        }
    }

    private String capStr(String in, int start, int end) {
        if (in.length() <= end - start) {
            return in;
        } else {
            return in.substring(start, end);
        }
    }

    private int getTrackWindowSize() {
        return 20;
    }

    private float getVolume(){
        if (MusicManager.initialized())
            return MusicManager.instance.getVolume();
        else
            return 0f;
    }

    private float getVolumePercentage() {
        if (MusicManager.initialized())
            return MusicManager.instance.getVolume() * 100f;
        else
            return 0f;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.MUSIC_TRACK_INFO) {// We changed the music track
            currentTrack = (String) data[0];
            si = 0;
            sp = 1;
            track.setText(capStr(currentTrack, 0, w));
        }
    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
