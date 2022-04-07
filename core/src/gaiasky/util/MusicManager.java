/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;

import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * This guy is in charge of managing the music playlist and actually
 * playing the sounds
 */
public class MusicManager implements IObserver {
    private static final Log logger = Logger.getLogger(MusicManager.class);

    public static MusicManager instance;
    private static Path[] folders;

    public static void initialize(Path... folders) {
        MusicManager.folders = folders;
        instance = new MusicManager(folders);
    }

    public static boolean initialized() {
        return instance != null;
    }

    private Array<Path> musicFiles;
    private int i = 0;
    private Music currentMusic;
    private float volume = 0.05f;

    public MusicManager(Path[] dirs) {
        super();
        initFiles(dirs);

        EventManager.instance.subscribe(this, Event.MUSIC_NEXT_CMD, Event.MUSIC_PLAYPAUSE_CMD, Event.MUSIC_PREVIOUS_CMD, Event.MUSIC_VOLUME_CMD, Event.MUSIC_RELOAD_CMD);
    }

    private void initFiles(Path[] folders) {
        if (folders != null) {
            musicFiles = new Array<>();

            for (Path folder : folders) {
                GlobalResources.listRecursive(folder, musicFiles, new MusicFileFilter());
            }
            logger.debug(I18n.msg("gui.music.load", musicFiles.size));
        } else {
            musicFiles = new Array<>();
        }
        i = 0;
    }

    public void start() {
        if (musicFiles.size > 0) {
            playNextMusic();
        }
    }

    public boolean isPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }

    private void playNextMusic() {
        i = (i + 1) % musicFiles.size;
        playIndex(i);
    }

    private void playPreviousMusic() {
        i = (((i - 1) % musicFiles.size) + musicFiles.size) % musicFiles.size;
        playIndex(i);
    }

    private void playIndex(int i) {
        Path f = musicFiles.get(i);

        if (currentMusic != null) {
            if (currentMusic.isPlaying()) {
                currentMusic.stop();
            }
            currentMusic.dispose();
        }
        try {
            currentMusic = Gdx.audio.newMusic(Gdx.files.absolute(f.toAbsolutePath().toString()));
            currentMusic.setVolume(volume);
            currentMusic.setOnCompletionListener(music -> playNextMusic());

            currentMusic.play();
            EventManager.publish(Event.MUSIC_TRACK_INFO, this, musicFiles.get(i).getFileName().toString());
            logger.info(I18n.msg("gui.music.playing", musicFiles.get(i).getFileName().toString()));
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * Gets the current play position in seconds
     * @return The play position in seconds
     */
    public float getPosition() {
        if (currentMusic == null)
            return 0;
        return currentMusic.getPosition();
    }

    /**
     * Sets the seeker of this music manager. The seeker must be given in the range [0,1] with 0 being silent and 1 being the maximum seeker.
     *
     * @param volume
     */
    public void setVolume(float volume) {
        this.volume = volume;
        if (currentMusic != null) {
            currentMusic.setVolume(this.volume);
        }
    }

    public float getVolume() {
        return volume;
    }

    public void next() {
        playNextMusic();
    }

    public void previous() {
        playPreviousMusic();
    }

    public void playPauseToggle() {
        if (currentMusic != null) {
            if (currentMusic.isPlaying()) {
                currentMusic.pause();
            } else {
                currentMusic.play();
            }
        } else {
            start();
        }
    }

    public void pause() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    public void play() {
        if (currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
        }
    }

    public void reload() {
        initFiles(folders);
    }

    private class MusicFileFilter implements DirectoryStream.Filter<Path> {
        private final PathMatcher pm = FileSystems.getDefault().getPathMatcher("glob:**.{mp3,wav,ogg,flac}");
        @Override
        public boolean accept(Path dir) {
            return pm.matches(dir);
        }

    }

    private void disposeInstance() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic.dispose();
        }
        if (EventManager.instance != null)
            EventManager.instance.unsubscribe(this, Event.MUSIC_NEXT_CMD, Event.MUSIC_PLAYPAUSE_CMD, Event.MUSIC_PREVIOUS_CMD, Event.MUSIC_VOLUME_CMD, Event.MUSIC_RELOAD_CMD);
    }

    public static void dispose() {
        if (instance != null) {
            instance.disposeInstance();
            instance = null;
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case MUSIC_PREVIOUS_CMD:
            previous();
            break;

        case MUSIC_NEXT_CMD:
            next();
            break;

        case MUSIC_PLAYPAUSE_CMD:
            playPauseToggle();
            break;

        case MUSIC_VOLUME_CMD:
            setVolume((float) data[0]);
            break;

        case MUSIC_RELOAD_CMD:
            reload();
            break;
        default:
            break;
        }

    }

}
