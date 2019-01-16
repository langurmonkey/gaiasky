package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.desktop.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.desktop.util.camera.CameraKeyframeManager;
import gaia.cu9.ari.gaiaorbit.desktop.util.camera.Keyframe;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.scene2d.*;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.validator.FloatValidator;

import java.io.File;
import java.io.FileFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class KeyframesWindow extends GenericDialog implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(KeyframesWindow.class);

    private static float pad = 10f * GlobalConf.SCALE_FACTOR;
    private static float pad5 = 5f * GlobalConf.SCALE_FACTOR;
    private static float buttonSize = 15 * GlobalConf.SCALE_FACTOR;

    private INumberFormat secondsFormatter;

    /** Seconds **/
    private OwnTextField secondsInput;
    /** Name **/
    private OwnTextField nameInput;
    /** Current keyframes **/
    private Array<Keyframe> keyframes;
    /** Right and left tables **/
    private Table left, right;
    /** Keyframes table **/
    private Table keyframesTable;
    /** Notice cell **/
    private Cell notice;
    /** Scroll for keyframes **/
    private OwnScrollPane rightScroll;
    /** Frames per second **/
    private long framerate;
    /** Current camera params **/
    private Object lock = new Object();
    private Vector3d pos, dir, up;
    private ITimeFrameProvider t;
    /** Cumulative number of frames in the sequence of keyframes **/
    long nframes = 0;
    /** Date format **/
    private DateFormat df;
    /** Last loaded keyframe file name **/
    private String lastKeyframeFileName = null;

    public KeyframesWindow(Stage stage, Skin skin) {
        super("Camera recorder - Keyframes", skin, stage);

        this.keyframes = new Array<Keyframe>();
        this.framerate = GlobalConf.frame.CAMERA_REC_TARGET_FPS;
        this.secondsFormatter = NumberFormatFactory.getFormatter("000.00");
        this.df = new SimpleDateFormat("yyyyMMdd_HH-mm-ss-SSS");
        setModal(false);

        setCancelText(txt("gui.close"));

        // Build UI
        buildSuper();

        EventManager.instance.subscribe(this, Events.UPDATE_CAM_RECORDER);
    }

    @Override
    protected void build() {
        left = new Table(skin);
        right = new Table(skin);

        /** LEFT - CONTROLS **/

        // ADD
        Image addImg = new Image(skin.getDrawable("sc-engine-power-up"));
        OwnTextIconButton addKeyframe = new OwnTextIconButton("Add keyframe", addImg, skin);
        addKeyframe.pad(pad5);
        left.add(addKeyframe).left().colspan(2).padBottom(pad5).row();
        addKeyframe.addListener(event -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                try {
                    if (secondsInput.isValid()) {
                        // Create new instances for the current keyframe
                        Vector3d cPos = new Vector3d();
                        Vector3d cDir = new Vector3d();
                        Vector3d cUp = new Vector3d();
                        long cTime;
                        // Freeze the camera info
                        synchronized (lock) {
                            cTime = t.getTime().toEpochMilli();
                            cPos.set(pos);
                            cDir.set(dir);
                            cUp.set(up);
                        }

                        // Frame number
                        long frameNumber = nframes;
                        // First keyframe is always at time 0.0 (or frame number 0)
                        if (!keyframes.isEmpty()) {
                            frameNumber += Math.round(Double.parseDouble(secondsInput.getText()) * framerate);
                        }

                        // Name
                        String name;
                        String ni = nameInput.getText();
                        if (ni != null && !ni.isEmpty()) {
                            name = ni;
                        } else {
                            name = "Keyframe " + (keyframes.size + 1);
                        }

                        Keyframe kf = new Keyframe(name, cPos, cDir, cUp, cTime, frameNumber);
                        keyframes.add(kf);
                        nframes = frameNumber;

                        addKeyframeToTable(kf, keyframes.size - 1, keyframesTable);
                    } else {
                        logger.info("Keyframe not added - Invalid value for 'seconds' input: " + secondsInput.getText());
                    }
                } catch (Exception e) {
                    logger.error("Keyframe not added - error in input", e);
                    return false;
                }
                return true;
            }
            return false;
        });

        // SECONDS
        FloatValidator secondsValidator = new FloatValidator(0.0001f, 9999.0f);
        secondsValidator.setIsValidCallback(() -> {
            // Enable add button
            addKeyframe.setDisabled(false);
        });
        secondsValidator.setIsInvalidCallback(() -> {
            // Disable add button
            addKeyframe.setDisabled(true);
        });
        secondsInput = new OwnTextField("1.0", skin, secondsValidator);
        secondsInput.setWidth(60 * GlobalConf.SCALE_FACTOR);
        OwnLabel secondsLabel = new OwnLabel("Seconds after previous:", skin);
        left.add(secondsLabel).bottom().left().padRight(pad5).padBottom(pad);
        left.add(secondsInput).bottom().left().padBottom(pad).row();

        // NAME
        nameInput = new OwnTextField("", skin);
        nameInput.setWidth(60 * GlobalConf.SCALE_FACTOR);
        OwnLabel nameLabel = new OwnLabel("Name (optional):", skin);
        left.add(nameLabel).bottom().left().padRight(pad5).padBottom(pad);
        left.add(nameInput).bottom().left().padBottom(pad).row();

        /** RIGHT - KEYFRAMES **/
        OwnLabel keyframesTitle = new OwnLabel("Keyframes list", skin, "hud-header");

        // KEYFRAMES TABLE
        keyframesTable = buildKeyframesTable();

        // ADD SCROLL
        rightScroll = new OwnScrollPane(keyframesTable, skin, "minimalist-nobg");
        rightScroll.setHeight(100 * GlobalConf.SCALE_FACTOR);
        rightScroll.setWidth(250 * GlobalConf.SCALE_FACTOR);
        rightScroll.setFadeScrollBars(true);

        right.add(keyframesTitle).top().left().padBottom(pad).row();
        right.add(rightScroll).top().left();

        /** ACTION BUTTONS **/
        HorizontalGroup buttons = new HorizontalGroup();
        buttons.space(pad);

        // Open keyframes
        Image openImg = new Image(skin.getDrawable("open"));
        OwnTextIconButton open = new OwnTextIconButton("Load keyframes file", openImg, skin);
        open.pad(pad5);
        open.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                FileChooser fc = FileChooser.createPickDialog(txt("gui.download.pickloc"), skin, new FileHandle(SysUtils.getDefaultCameraDir()));
                fc.setResultListener(new FileChooser.ResultListener() {
                    @Override
                    public boolean result(boolean success, FileHandle result) {
                        if (success) {
                            if (result.file().exists() && result.file().isFile()) {
                                // Load selected file
                                try {
                                    Array<Keyframe> kfs = CameraKeyframeManager.instance.loadKeyframesFile(result.file());
                                    // Update current instance
                                    reinitialiseKeyframes(kfs);
                                    lastKeyframeFileName = result.file().getName();
                                    logger.info(keyframes.size + " keyframes loaded successfully from " + result.file().getName());
                                } catch (RuntimeException e) {
                                    logger.error("Error loading keyframes file: " + result.file().getName(), e);
                                    Label warn = new OwnLabel("Error loading file: wrong format", skin);
                                    warn.setColor(1f, .4f, .4f, 1f);
                                    notice.setActor(warn);
                                    return false;
                                }

                            } else {
                                logger.error("Chosen file does not exist or is not a file: " + result.file().getName());
                                Label warn = new OwnLabel("Chosen file does not exist or is not a file", skin);
                                warn.setColor(1f, .4f, .4f, 1f);
                                notice.setActor(warn);
                                return false;
                            }
                        }
                        notice.clearActor();
                        return true;
                    }
                });

                fc.setFilter(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile();
                    }
                });
                fc.show(stage);
                return true;
            }
            return false;
        });

        // Save keyframes
        Image saveImg = new Image(skin.getDrawable("save"));
        OwnTextIconButton save = new OwnTextIconButton("Save keyframes to file", saveImg, skin);
        save.pad(pad5);
        save.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                String suggestedName = lastKeyframeFileName == null ? df.format(new Date()) + "_keyframes.gkf" : lastKeyframeFileName;
                FileNameWindow fnw = new FileNameWindow(suggestedName, stage, skin);
                OwnTextField textField = fnw.getFileNameField();
                fnw.setAcceptRunnable(() -> {
                    if (textField.isValid()) {
                        EventManager.instance.post(Events.KEYFRAMES_FILE_SAVE, keyframes, textField.getText());
                        notice.clearActor();
                    } else {
                        Label warn = new OwnLabel("File name chosen is not valid", skin);
                        warn.setColor(1f, .4f, .4f, 1f);
                        notice.setActor(warn);
                    }
                });
                fnw.show(stage);
                return true;
            }
            return false;
        });

        // Export to camera path
        Image exportImg = new Image(skin.getDrawable("export"));
        OwnTextIconButton export = new OwnTextIconButton("Export to camera path", exportImg, skin);
        export.pad(pad5);
        export.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                String suggestedName = df.format(new Date()) + "_gscamera.dat";
                FileNameWindow fnw = new FileNameWindow(suggestedName, stage, skin);
                OwnTextField textField = fnw.getFileNameField();
                fnw.setAcceptRunnable(() -> {
                    if (textField.isValid()) {
                        EventManager.instance.post(Events.KEYFRAMES_EXPORT, keyframes, textField.getText());
                        notice.clearActor();
                    } else {
                        Label warn = new OwnLabel("File name chosen is not valid", skin);
                        warn.setColor(1f, .4f, .4f, 1f);
                        notice.setActor(warn);
                    }
                });
                fnw.show(stage);
                return true;
            }
            return false;
        });

        buttons.addActor(open);
        buttons.addActor(save);
        buttons.addActor(export);

        /** FINAL LAYOUT **/
        content.add(left).top().left().padRight(pad * 2f).padBottom(pad * 3f);
        content.add(right).top().left().row();
        notice = content.add();
        notice.padBottom(pad * 2f).center().colspan(2).row();
        content.add(buttons).colspan(2).right();
    }

    private Table buildKeyframesTable() {
        Table table = new Table(skin);
        table.align(Align.top | Align.left);

        int i = 0;
        for (Keyframe kf : keyframes) {
            addKeyframeToTable(kf, i, table);
            i++;
        }
        return table;
    }

    private void addKeyframeToTable(Keyframe kf, int index, Table table) {
        // Time
        double t = (double) kf.frame / (double) framerate;
        table.add(new OwnLabel(secondsFormatter.format(t), skin, "hud-header")).left().padRight(pad).padBottom(pad5);

        // Frame number
        table.add(new OwnLabel((index + 1) + ": " + kf.name, skin)).left().padRight(pad).padBottom(pad5);

        // Go to
        Image gotoimg = new Image(skin.getDrawable("go-to"));
        OwnTextIconButton goTo = new OwnTextIconButton("", gotoimg, skin);
        goTo.setSize(buttonSize, buttonSize);
        goTo.addListener(new TextTooltip("Go to keyframe", skin));
        goTo.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                // Go to keyframe
                EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraManager.CameraMode.Free_Camera);
                Gdx.app.postRunnable(()->{
                    EventManager.instance.post(Events.CAMERA_POS_CMD, kf.pos.values());
                    EventManager.instance.post(Events.CAMERA_DIR_CMD, kf.dir.values());
                    EventManager.instance.post(Events.CAMERA_UP_CMD, kf.up.values());
                    EventManager.instance.post(Events.TIME_CHANGE_CMD, Instant.ofEpochMilli(kf.time));
                });
                return true;
            }
            return false;
        });
        table.add(goTo).left().padRight(pad5).padBottom(pad5);

        // Rubbish
        Image rubbishimg = new Image(skin.getDrawable("bin-icon"));
        OwnTextIconButton rubbish = new OwnTextIconButton("", rubbishimg, skin);
        rubbish.setSize(buttonSize, buttonSize);
        rubbish.addListener(new TextTooltip("Remove keyframe", skin));
        rubbish.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                // Remove keyframe
                Array<Keyframe> newKfs = new Array<Keyframe>(keyframes.size - 1);
                for (Keyframe k : keyframes)
                    if (k != kf)
                        newKfs.add(k);
                reinitialiseKeyframes(newKfs);
                logger.info("Removed keyframe: " + kf.name);
                return true;
            }
            return false;
        });
        table.add(rubbish).left().padBottom(pad5).row();
        table.pack();
        if (rightScroll != null) {
            Gdx.app.postRunnable(() -> {
                rightScroll.setScrollPercentY(110f);
            });
        }

        this.pack();

    }

    private void reinitialiseKeyframes(Array<Keyframe> kfs) {
        clean();
        keyframes.addAll(kfs);
        for (int i = 0; i < keyframes.size; i++) {
            addKeyframeToTable(keyframes.get(i), i, keyframesTable);
        }
        nframes = keyframes.isEmpty() ? 0 : keyframes.get(keyframes.size - 1).frame;
    }

    private void clean() {
        keyframes.clear();
        keyframesTable.clearChildren();
        nframes = 0;
        nameInput.setText("");
        secondsInput.setText("1.0");
        lastKeyframeFileName = null;
    }

    @Override
    protected void accept() {
        // Convert to camera path file?
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    protected void cancel() {
        clean();
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case UPDATE_CAM_RECORDER:
            synchronized (lock) {
                t = (ITimeFrameProvider) data[0];
                pos = (Vector3d) data[1];
                dir = (Vector3d) data[2];
                up = (Vector3d) data[3];
            }
            break;
        default:
            break;
        }
    }
}
