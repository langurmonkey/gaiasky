package gaia.cu9.ari.gaiaorbit.util;

import java.io.File;
import java.io.FilenameFilter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

public class MusicManager implements IObserver {
    private static final Log logger = Logger.getLogger(MusicManager.class);
    
    public static MusicManager instance;
    private static FileHandle[] folders;

    public static void initialize(FileHandle... folders) {
        MusicManager.folders = folders;
        instance = new MusicManager(folders);
    }

    public static boolean initialized() {
        return instance != null;
    }

    private Array<FileHandle> musicFiles;
    private int i = 0;
    private Music currentMusic;
    private float volume = 0.05f;

    public MusicManager(FileHandle[] dirs) {
        super();
        initFiles(dirs);

        EventManager.instance.subscribe(this, Events.MUSIC_NEXT_CMD, Events.MUSIC_PLAYPAUSE_CMD, Events.MUSIC_PREVIOUS_CMD, Events.MUSIC_VOLUME_CMD, Events.MUSIC_RELOAD_CMD);
    }

    private void initFiles(FileHandle[] folders) {
        if (folders != null) {
            musicFiles = new Array<FileHandle>();

            for (FileHandle folder : folders) {
                GlobalResources.listRec(folder, musicFiles, new MusicFileFilter());
            }
            logger.info(I18n.bundle.format("gui.music.load", musicFiles.size));
        } else {
            musicFiles = new Array<FileHandle>();
        }
        i = 0;
    }

    public void start() {
        if (musicFiles.size > 0) {
            playNextMusic();
        }
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
        FileHandle f = musicFiles.get(i);

        if (currentMusic != null) {
            if (currentMusic.isPlaying())
                currentMusic.stop();
            currentMusic.dispose();
        }
        try {
            currentMusic = Gdx.audio.newMusic(f);
            currentMusic.setVolume(volume);
            currentMusic.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(Music music) {
                    playNextMusic();
                }
            });
            currentMusic.play();
            logger.info(I18n.bundle.format("gui.music.playing", musicFiles.get(i).name()));
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * Sets the volume of this music manager. The volume must be given in the range [0,1] with 0 being silent and 1 being the maximum volume.
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

    private class MusicFileFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg");
        }

    }

    private void disposeInstance() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic.dispose();
        }
        if (EventManager.instance != null)
            EventManager.instance.unsubscribe(this, Events.MUSIC_NEXT_CMD, Events.MUSIC_PLAYPAUSE_CMD, Events.MUSIC_PREVIOUS_CMD, Events.MUSIC_VOLUME_CMD, Events.MUSIC_RELOAD_CMD);
    }

    public static void dispose() {
        if (instance != null) {
            instance.disposeInstance();
            instance = null;
        }
    }

    @Override
    public void notify(Events event, Object... data) {
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
