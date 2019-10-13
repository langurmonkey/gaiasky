/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce.components;

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
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.MusicManager;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextTooltip;

public class MusicComponent extends GuiComponent implements IObserver {

    protected ImageButton prev, play, next;
    protected OwnLabel vol, track, position;
    protected INumberFormat nf;
    protected INumberFormat intf;

    private String currentTrack;
    private int si = 0;
    private int sp = 1;
    private int w = getTrackWindowSize();

    public MusicComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Events.MUSIC_PLAYPAUSE_CMD, Events.MUSIC_VOLUME_CMD, Events.MUSIC_NEXT_CMD, Events.MUSIC_PREVIOUS_CMD, Events.MUSIC_TRACK_INFO);
    }

    @Override
    public void initialize() {
        float componentWidth = 165 * GlobalConf.UI_SCALE_FACTOR;
        nf = NumberFormatFactory.getFormatter("##0");
        intf = NumberFormatFactory.getFormatter("#00");

        /** Previous track **/
        prev = new OwnImageButton(skin, "audio-bwd");
        prev.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.MUSIC_PREVIOUS_CMD);
                return true;
            }
            return false;
        });
        prev.addListener(new OwnTextTooltip(I18n.txt("gui.music.previous"), skin));

        /** Play/pause **/
        play = new OwnImageButton(skin, "audio-playpause");
        play.setChecked(false);
        play.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.MUSIC_PLAYPAUSE_CMD);
                return true;
            }
            return false;
        });
        play.addListener(new OwnTextTooltip(I18n.txt("gui.music.playpause"), skin));

        /** Next track **/
        next = new OwnImageButton(skin, "audio-fwd");
        next.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.MUSIC_NEXT_CMD);
                return true;
            }
            return false;
        });
        next.addListener(new OwnTextTooltip(I18n.txt("gui.music.next"), skin));

        /** Volume **/
        vol = new OwnLabel("VOL: " + nf.format(getVolumePercentage()) + "%", skin, "mono");
        vol.receiveScrollEvents();
        vol.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType().equals(InputEvent.Type.scrolled)) {
                    float scroll = -ie.getScrollAmount() * 0.1f;
                    float currentVol = MusicManager.instance.getVolume();
                    float newVol = Math.max(0f, Math.min(1f, currentVol + scroll));
                    EventManager.instance.post(Events.MUSIC_VOLUME_CMD, newVol);
                    vol.setText("VOL: " + nf.format(getVolumePercentage()) + "%");
                    return true;
                }
            }
            return false;
        });

        /** Position mm:ss **/
        position = new OwnLabel(toMMSS(0f), skin, "mono");

        /** Track name **/
        track = new OwnLabel("", skin, "mono");

        float space3 = 3 * GlobalConf.UI_SCALE_FACTOR;
        VerticalGroup musicGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(space3);

        HorizontalGroup playGroup = new HorizontalGroup();
        playGroup.setWidth(componentWidth);
        playGroup.space(15 * GlobalConf.UI_SCALE_FACTOR);
        prev.align(Align.left);
        play.align(Align.left);
        next.align(Align.left);
        playGroup.addActor(prev);
        playGroup.addActor(play);
        playGroup.addActor(next);
        playGroup.addActor(vol);

        HorizontalGroup trackGroup = new HorizontalGroup();
        trackGroup.space(10 * GlobalConf.UI_SCALE_FACTOR);
        trackGroup.addActor(position);
        trackGroup.addActor(track);

        musicGroup.addActor(playGroup);
        musicGroup.addActor(trackGroup);

        component = musicGroup;

        Task musicUpdater = new Task() {
            @Override
            public void run() {
                position.setText(toMMSS(MusicManager.instance.getPosition()));
                slideTrackName();
            }
        };
        Timer.schedule(musicUpdater, 1, 1);

    }

    private String toMMSS(float seconds) {
        int mins = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return intf.format(mins) + ":" + intf.format(secs);
    }

    private void slideTrackName() {
        if (currentTrack != null && w < currentTrack.length()) {
            int l = currentTrack.length();

            if(si + w == l && sp > 0) {
                sp *= -1;
            } else if (si <= 0 && sp < 0) {
                sp *= -1;
            }
            si += sp;

            track.setText(capStr(currentTrack, si, si + w));
        }
    }

    private String capStr(String in, int start, int end){
        if(in.length() <= end - start){
            return in;
        }else{
            return in.substring(start, end);
        }
    }

    private int getTrackWindowSize() {
        return GlobalConf.UI_SCALE_FACTOR > 1.5f ? 20 : 14;
    }

    private float getVolumePercentage() {
        return MusicManager.instance.getVolume() * 100f;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case MUSIC_TRACK_INFO:
            // We changed the music track
            currentTrack = (String) data[0];
            si = 0;
            sp = 1;
            track.setText(capStr(currentTrack, 0, w));
            break;
        case MUSIC_PLAYPAUSE_CMD:
            break;
        case MUSIC_NEXT_CMD:
            break;
        case MUSIC_PREVIOUS_CMD:
            break;
        default:
            break;
        }
    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
