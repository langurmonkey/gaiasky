package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
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
import gaia.cu9.ari.gaiaorbit.scenegraph.KeyframesPathObject;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.math.Interpolationd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.scene2d.*;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.validator.FloatValidator;
import gaia.cu9.ari.gaiaorbit.util.validator.LengthValidator;
import gaia.cu9.ari.gaiaorbit.util.validator.RegexpValidator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class KeyframesWindow extends GenericDialog implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(KeyframesWindow.class);

    private INumberFormat secondsFormatter;
    private IDateFormat dateFormat;

    /**
     * Seconds
     **/
    private OwnTextField secondsInput;
    /**
     * Name
     **/
    private OwnTextField nameInput;
    /**
     * Current keyframes
     **/
    private Array<Keyframe> keyframes;
    /**
     * Right and left tables
     **/
    private Table left, right;
    /**
     * Keyframes table
     **/
    private Table keyframesTable;
    /**
     * Notice cell
     **/
    private Cell notice;
    /**
     * Seconds cells
     **/
    private Map<Keyframe, Cell> secondsCells;
    /**
     * Names cells
     **/
    private Map<Keyframe, Cell> namesCells;
    /**
     * Keyframe cells
     */
    private Map<Keyframe, OwnLabel> keyframeNames;
    /**
     * Scroll for keyframes
     **/
    private OwnScrollPane rightScroll;
    /**
     * Current camera params
     **/
    private Object lock = new Object();
    private Vector3d pos, dir, up;
    private ITimeFrameProvider t;
    /**
     * Date format
     **/
    private DateFormat df;
    /**
     * Last loaded keyframe file name
     **/
    private String lastKeyframeFileName = null;

    /**
     * Model object to represent the path
     **/
    private KeyframesPathObject keyframesPathObject;

    private float buttonSize, buttonSizeL;

    /**
     * Contains info on field currently being edited
     */
    private class Editing {
        private int type = -1;
        private Keyframe kf;
        private int index;
        private OwnTextField tf;
        private Map<String, Object> map;

        public Editing() {
            map = new HashMap<>();
        }

        public boolean notEmpty() {
            return tf != null;
        }

        public boolean isEmpty() {
            return tf == null;
        }

        public void revert() {
            if (isName()) {
                addFrameName(kf, index, keyframesTable);
            } else if (isSeconds()) {
                addFrameSeconds(kf, (Double) map.get("prevT"), index, keyframesTable);
            }
        }

        public void setParam(String key, Object value) {
            map.put(key, value);
        }

        public boolean isName() {
            return !isEmpty() && type == 1;
        }

        public boolean isSeconds() {
            return !isEmpty() && type == 0;
        }

        public void set(Keyframe kf, int idx, OwnTextField tf) {
            this.kf = kf;
            this.index = idx;
            this.tf = tf;
        }

        public void setName(Keyframe kf, int idx, OwnTextField tf) {
            type = 1;
            set(kf, idx, tf);
        }

        public void setSeconds(Keyframe kf, int idx, OwnTextField tf, double prevT) {
            type = 0;
            setParam("prevT", prevT);
            set(kf, idx, tf);
        }

        public void unset() {
            type = -1;
            kf = null;
            index = -1;
            tf = null;
            map.clear();
        }

        public Keyframe kf() {
            return kf;
        }

        public int index() {
            return index;
        }

        public OwnTextField tf() {
            return tf;
        }

    }

    private Editing editing;

    public KeyframesWindow(Stage stage, Skin skin) {
        super(I18n.txt("gui.keyframes.title"), skin, stage);

        buttonSize = 15 * GlobalConf.SCALE_FACTOR;
        buttonSizeL = 17 * GlobalConf.SCALE_FACTOR;

        this.editing = new Editing();
        this.keyframes = new Array<>();
        this.secondsCells = new HashMap<>();
        this.namesCells = new HashMap<>();
        this.keyframeNames = new HashMap<>();
        this.secondsFormatter = NumberFormatFactory.getFormatter("000.00");
        this.df = new SimpleDateFormat("yyyyMMdd_HH-mm-ss-SSS");
        this.dateFormat = DateFormatFactory.getFormatter(I18n.locale, DateFormatFactory.DateType.DATETIME);
        setModal(false);

        setCancelText(I18n.txt("gui.close"));

        // Build UI
        buildSuper();

        // Add {@link gaia.cu9.ari.gaiaorbit.scenegraph.KeyframesPathObject} to model
        keyframesPathObject = new KeyframesPathObject();
        keyframesPathObject.setCt("Others");
        keyframesPathObject.setParent("Universe");
        keyframesPathObject.setName("Keyframe path");
        keyframesPathObject.initialize();
        keyframesPathObject.doneLoading(null);
        keyframesPathObject.setKeyframes(keyframes);

        EventManager.instance.post(Events.SCENE_GRAPH_ADD_OBJECT_CMD, keyframesPathObject, false);

        // Resizable
        setResizable(false, true);
    }

    @Override
    protected void build() {
        left = new Table(skin);
        left.align(Align.top | Align.left);
        right = new Table(skin);
        right.align(Align.top | Align.left);

        /** LEFT - CONTROLS **/

        // ADD
        OwnTextIconButton addKeyframe = new OwnTextIconButton(I18n.txt("gui.keyframes.add.end"), skin, "add");
        addKeyframe.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.add.end"), skin));
        addKeyframe.pad(pad5);
        left.add(addKeyframe).left().colspan(2).padBottom(pad5).row();
        addKeyframe.addListener(event -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                // Add at end
                return addKeyframe(-1);
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
        OwnLabel secondsLabel = new OwnLabel(I18n.txt("gui.keyframes.secsafter") + ":", skin);
        left.add(secondsLabel).center().left().padRight(pad).padBottom(pad);
        left.add(secondsInput).center().left().padBottom(pad).row();

        // NAME
        LengthValidator lengthValidator = new LengthValidator(0, 15);
        RegexpValidator nameValidator = new RegexpValidator(lengthValidator, "^[^*&%\\s\\+\\=\\\\\\/@#\\$&\\*()~]*$");
        nameInput = new OwnTextField("", skin, nameValidator);
        nameInput.setWidth(60 * GlobalConf.SCALE_FACTOR);
        OwnLabel nameLabel = new OwnLabel(I18n.txt("gui.keyframes.name") + ":", skin);
        left.add(nameLabel).center().left().padRight(pad).padBottom(pad);
        left.add(nameInput).center().left().padBottom(pad).row();

        left.pack();

        /** RIGHT - KEYFRAMES **/
        OwnLabel keyframesTitle = new OwnLabel(I18n.txt("gui.keyframes.list"), skin, "hud-header");

        // KEYFRAMES TABLE
        keyframesTable = buildKeyframesTable();

        // ADD SCROLL
        rightScroll = new OwnScrollPane(keyframesTable, skin, "minimalist-nobg");
        rightScroll.setExpand(true);
        rightScroll.setScrollingDisabled(true, false);
        rightScroll.setHeight((GlobalConf.SCALE_FACTOR > 1.5f ? 100 : 110) * GlobalConf.SCALE_FACTOR);
        rightScroll.setWidth((GlobalConf.SCALE_FACTOR > 1.5f ? 360 : 390) * GlobalConf.SCALE_FACTOR);
        rightScroll.setFadeScrollBars(true);

        right.add(keyframesTitle).top().left().padBottom(pad).row();
        right.add(rightScroll).center().left();

        right.pack();

        /** ACTION BUTTONS **/
        HorizontalGroup buttons = new HorizontalGroup();
        buttons.space(pad);

        // Open keyframes
        OwnTextIconButton open = new OwnTextIconButton(I18n.txt("gui.keyframes.load"), skin, "open");
        open.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.load"), skin));
        open.pad(pad5);
        open.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                FileChooser fc = FileChooser.createPickDialog(I18n.txt("gui.download.pickloc"), skin, new FileHandle(SysUtils.getDefaultCameraDir()));
                fc.setTarget(FileChooser.FileChooserTarget.FILES);
                fc.setFileFilter(pathname -> pathname.getName().endsWith(".gkf"));
                fc.setAcceptedFiles("*.gkf");
                fc.setResultListener((success, result) -> {
                    if (success) {
                        if (result.file().exists() && result.file().isFile()) {
                            // Load selected file
                            try {
                                Array<Keyframe> kfs = CameraKeyframeManager.instance.loadKeyframesFile(result.file());
                                // Update current instance
                                reinitialiseKeyframes(kfs, null);
                                keyframesPathObject.unselect();
                                lastKeyframeFileName = result.file().getName();
                                logger.info(I18n.txt("gui.keyframes.load.success", keyframes.size, result.file().getName()));
                            } catch (RuntimeException e) {
                                logger.error(I18n.txt("gui.keyframes.load.error", result.file().getName()), e);
                                Label warn = new OwnLabel(I18n.txt("error.loading.format", result.file().getName()), skin);
                                warn.setColor(1f, .4f, .4f, 1f);
                                notice.setActor(warn);
                                return false;
                            }

                        } else {
                            logger.error(I18n.txt("error.loading.notexistent", result.file().getName()));
                            Label warn = new OwnLabel(I18n.txt("error.loading.notexistent", result.file().getName()), skin);
                            warn.setColor(1f, .4f, .4f, 1f);
                            notice.setActor(warn);
                            return false;
                        }
                    }
                    notice.clearActor();
                    return true;
                });

                fc.show(stage);
                return true;
            }
            return false;
        });

        // Save keyframes
        OwnTextIconButton save = new OwnTextIconButton(I18n.txt("gui.keyframes.save"), skin, "save");
        save.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.save"), skin));
        save.pad(pad5);
        save.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                String suggestedName = lastKeyframeFileName == null ? df.format(new Date()) + "_keyframes.gkf" : lastKeyframeFileName;
                FileNameWindow fnw = new FileNameWindow(suggestedName, stage, skin);
                OwnTextField textField = fnw.getFileNameField();
                fnw.setAcceptRunnable(() -> {
                    if (textField.isValid()) {
                        EventManager.instance.post(Events.KEYFRAMES_FILE_SAVE, keyframes, textField.getText());
                        lastKeyframeFileName = textField.getText();
                        notice.clearActor();
                    } else {
                        Label warn = new OwnLabel(I18n.txt("error.file.name.notvalid", textField.getText()), skin);
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
        OwnTextIconButton export = new OwnTextIconButton(I18n.txt("gui.keyframes.export"), skin, "export");
        export.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.export"), skin));
        export.pad(pad5);
        export.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                String suggestedName = df.format(new Date()) + ".gsc";
                FileNameWindow fnw = new FileNameWindow(suggestedName, stage, skin);
                OwnTextField textField = fnw.getFileNameField();
                fnw.setAcceptRunnable(() -> {
                    if (textField.isValid()) {
                        EventManager.instance.post(Events.KEYFRAMES_EXPORT, keyframes, textField.getText());
                        notice.clearActor();
                    } else {
                        Label warn = new OwnLabel(I18n.txt("error.file.name.notvalid", textField.getText()), skin);
                        warn.setColor(1f, .4f, .4f, 1f);
                        notice.setActor(warn);
                    }
                });
                fnw.show(stage);
                return true;
            }
            return false;
        });

        // Keyframe preferences
        Button preferences = new OwnTextIconButton(I18n.txt("gui.preferences"), skin, "preferences");
        preferences.setName("keyframe preferences");
        preferences.padTop(pad5 / 2.5f);
        preferences.padBottom(pad5 / 2.5f);
        preferences.padRight(pad5);
        preferences.padLeft(pad5);
        preferences.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.editprefs"), skin));
        preferences.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                KeyframePreferencesWindow kpw = new KeyframePreferencesWindow(stage, skin);
                kpw.setAcceptRunnable(() -> {
                    // Resample
                    Gdx.app.postRunnable(() -> {
                        keyframesPathObject.resamplePath();
                    });
                });
                kpw.show(stage, me.getWidth(), 0);
                return true;
            }
            return false;
        });

        buttons.addActor(open);
        buttons.addActor(save);
        buttons.addActor(export);
        buttons.addActor(preferences);

        /** FINAL LAYOUT **/
        content.add(left).top().left().padRight(pad * 2f).padBottom(pad * 3f);
        content.add(right).width(370 * GlobalConf.SCALE_FACTOR).top().left().padBottom(pad).row();
        notice = content.add();
        notice.padBottom(pad).expandY().center().colspan(2).row();
        content.add(buttons).colspan(2).bottom().right().row();

        // CLEAR
        OwnTextButton clear = new OwnTextButton(I18n.txt("gui.clear"), skin);
        clear.setName("clear");
        clear.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                Gdx.app.postRunnable(() -> {
                    clean();
                });
                return true;
            }
            return false;
        });
        buttonGroup.addActorAt(0, clear);

        // HIDE
        OwnTextButton hide = new OwnTextButton(I18n.txt("gui.hide"), skin);
        hide.setName("hide");
        hide.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                hide();
                return true;
            }
            return false;
        });
        buttonGroup.addActorAt(1, hide);


        recalculateButtonSize();

    }

    /**
     * Adds a new keyframe at the given index position using the given camera position, orientation and time
     *
     * @param index The position of the keyframe, negative to add at the end
     * @param cPos  The position
     * @param cDir  The direction
     * @param cUp   The up
     * @param cTime The time
     * @return True if the keyframe was added, false otherwise
     */
    private boolean addKeyframe(int index, Vector3d cPos, Vector3d cDir, Vector3d cUp, long cTime) {

        try {
            boolean secOk = secondsInput.isValid();
            boolean nameOk = nameInput.isValid();
            if (secOk && nameOk) {
                // Seconds after - first keyframe at zero
                double secsAfter = keyframes.size == 0 ? 0 : Double.parseDouble(secondsInput.getText());

                // Name
                String name;
                String ni = nameInput.getText();
                if (ni != null && !ni.isEmpty()) {
                    name = ni;
                } else {
                    name = "Keyframe " + (keyframes.size + 1);
                }

                Keyframe kf = new Keyframe(name, cPos, cDir, cUp, cTime, secsAfter, false);
                final boolean insert = index >= 0 && index != keyframes.size;
                if (!insert) {
                    keyframes.add(kf);
                    double prevT = 0;
                    for (Keyframe kfr : keyframes) {
                        if (kfr == kf)
                            break;
                        prevT += kfr.seconds;
                    }

                    addKeyframeToTable(kf, prevT, keyframes.size - 1, keyframesTable, true);
                } else {
                    keyframes.insert(index, kf);
                }
                Gdx.app.postRunnable(() -> {
                    if (insert)
                        reinitialiseKeyframes(keyframes, kf);
                    scrollToKeyframe(kf);
                });

            } else {
                logger.info(I18n.txt("gui.keyframes.notadded") + "-" + I18n.txt("gui.keyframes.error.values", secOk ? I18n.txt("gui.ok") : I18n.txt("gui.wrong"), nameOk ? I18n.txt("gui.ok") : I18n.txt("gui.wrong")));
            }
        } catch (Exception e) {
            logger.error(I18n.txt("gui.keyframes.notadded") + " - " + I18n.txt("gui.keyframes.error.input"), e);
            return false;
        }
        return true;
    }

    /**
     * Adds a new keyframe at the given index position using the current camera state and time
     *
     * @param index The position of the keyframe, negative to add at the end
     * @return True if the keyframe was added, false otherwise
     */
    private boolean addKeyframe(int index) {
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
        return addKeyframe(index, cPos, cDir, cUp, cTime);
    }

    private Table buildKeyframesTable() {
        Table table = new Table(skin);
        table.align(Align.top | Align.left);

        addKeyframesToTable(keyframes, table);

        return table;
    }

    private void addKeyframesToTable(Array<Keyframe> keyframes, Table table) {
        int i = 0;
        double prevT = 0;
        for (Keyframe kf : keyframes) {

            // Add to UI table
            addKeyframeToTable(kf, prevT, i, table);

            prevT += kf.seconds;
            i++;
        }
        if (keyframesPathObject != null) {
            Gdx.app.postRunnable(() -> {
                keyframesPathObject.setKeyframes(keyframes);
                keyframesPathObject.refreshData();
            });
        }
    }

    private void addKeyframeToTable(Keyframe kf, double prevT, int index, Table table) {
        addKeyframeToTable(kf, prevT, index, table, false);
    }

    private long lastMs = 0l;

    private Cell addFrameSeconds(Keyframe kf, double prevT, int index, Table table) {
        // Seconds
        OwnLabel secondsL = new OwnLabel(secondsFormatter.format(prevT + kf.seconds), skin, "hud-header");
        secondsL.setWidth((GlobalConf.SCALE_FACTOR > 1.5f ? 60f : 75f) * GlobalConf.SCALE_FACTOR);
        Cell secondsCell;
        if (secondsCells.containsKey(kf))
            secondsCell = secondsCells.get(kf);
        else {
            secondsCell = table.add();
            secondsCells.put(kf, secondsCell);
        }
        secondsCell.setActor(secondsL).left().padRight(pad / 2f).padBottom(pad5);
        secondsL.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.seconds", kf.seconds, GlobalConf.frame.RENDER_TARGET_FPS), skin));
        // Can't modify time of first keyframe; it's always zero
        if (index > 0)
            secondsL.addListener((event) -> {
                if (event instanceof InputEvent) {
                    InputEvent ie = (InputEvent) event;
                    if (ie.getType().equals(InputEvent.Type.touchDown)) {
                        if (editing.notEmpty()) {
                            // Remove current
                            editing.revert();
                            editing.unset();
                        }
                        String valText = secondsL.getText().toString();
                        secondsL.clear();
                        secondsCells.get(kf).clearActor();
                        OwnTextField secondsInput = new OwnTextField(valText, skin, new FloatValidator(0.0001f, 500f));
                        secondsInput.setWidth((GlobalConf.SCALE_FACTOR > 1.5f ? 55f : 75f) * GlobalConf.SCALE_FACTOR);
                        secondsInput.selectAll();
                        stage.setKeyboardFocus(secondsInput);
                        editing.setSeconds(kf, index, secondsInput, prevT);
                        secondsInput.addListener((evt) -> {
                            if (secondsInput.isValid() && evt instanceof InputEvent && System.currentTimeMillis() - lastMs > 1500) {
                                InputEvent ievt = (InputEvent) evt;
                                if (ievt.getType() == InputEvent.Type.keyDown && (ievt.getKeyCode() == Input.Keys.ENTER || ievt.getKeyCode() == Input.Keys.ESCAPE)) {
                                    double val = Double.parseDouble(secondsInput.getText());
                                    double t = 0;
                                    for (Keyframe k : keyframes) {
                                        if (k == kf)
                                            break;
                                        t += k.seconds;
                                    }
                                    if (val > t) {
                                        kf.seconds = val - t;
                                        Gdx.app.postRunnable(() -> {
                                            secondsCells.get(kf).clearActor();
                                            secondsInput.clear();
                                            // Rebuild
                                            reinitialiseKeyframes(keyframes, null);
                                        });
                                    }
                                    editing.unset();
                                }
                            }
                            evt.setBubbles(false);
                            return true;
                        });
                        secondsCells.get(kf).setActor(secondsInput);
                        lastMs = System.currentTimeMillis();
                    }
                }
                return true;
            });
        addHighlightListener(secondsL, kf);
        return secondsCell;
    }

    private Cell addFrameName(Keyframe kf, int index, Table table) {
        // Seconds
        OwnLabel nameL = new OwnLabel((index + 1) + ": " + kf.name, skin);
        nameL.setWidth((GlobalConf.SCALE_FACTOR > 1.5f ? 100f : 130f) * GlobalConf.SCALE_FACTOR);
        Cell nameCell;
        if (namesCells.containsKey(kf))
            nameCell = namesCells.get(kf);
        else {
            nameCell = table.add();
            namesCells.put(kf, nameCell);
        }
        nameCell.clearActor();
        nameCell.setActor(nameL).left().padRight(pad / 2f).padBottom(pad5);
        keyframeNames.put(kf, nameL);
        nameL.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.name"), skin));
        nameL.addListener((event) -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == InputEvent.Type.touchDown) {
                    if (editing.notEmpty()) {
                        // Remove current
                        editing.revert();
                        editing.unset();
                    }
                    String valText = nameL.getText().toString();
                    valText = valText.substring(valText.indexOf(":") + 2);
                    nameL.clear();
                    keyframeNames.remove(kf);
                    namesCells.get(kf).clearActor();
                    LengthValidator lengthValidator = new LengthValidator(0, 15);
                    RegexpValidator nameValidator = new RegexpValidator(lengthValidator, "^[^*&%\\+\\=\\\\\\/@#\\$&\\*()~]*$");
                    OwnTextField nameInput = new OwnTextField(valText, skin, nameValidator);
                    nameInput.setWidth((GlobalConf.SCALE_FACTOR > 1.5f ? 100f : 130f) * GlobalConf.SCALE_FACTOR);
                    nameInput.selectAll();
                    stage.setKeyboardFocus(nameInput);
                    editing.setName(kf, index, nameInput);
                    nameInput.addListener((evt) -> {
                        if (nameInput.isValid() && evt instanceof InputEvent && System.currentTimeMillis() - lastMs > 1500) {
                            InputEvent ievt = (InputEvent) evt;
                            if (ievt.getType() == InputEvent.Type.keyDown && (ievt.getKeyCode() == Input.Keys.ENTER || ievt.getKeyCode() == Input.Keys.ESCAPE)) {
                                kf.name = nameInput.getText();
                                addFrameName(kf, index, table);
                                editing.unset();
                            }
                        }
                        evt.setBubbles(false);
                        return true;
                    });
                    namesCells.get(kf).setActor(nameInput);
                    lastMs = System.currentTimeMillis();
                }
            }
            return true;
        });
        addHighlightListener(nameL, kf);
        return nameCell;
    }


    private void addKeyframeToTable(Keyframe kf, double prevT, int index, Table table, boolean addToModel) {

        // Seconds
        addFrameSeconds(kf, prevT, index, table);

        // Frame number
        double t = 0;
        for (int i = 0; ; i++) {
            if (keyframes.get(i).equals(kf))
                break;
            t += keyframes.get(i).seconds;
        }
        long frame = (long) ((t + kf.seconds) * GlobalConf.frame.RENDER_TARGET_FPS);

        OwnLabel framesL = new OwnLabel("(" + frame + ")", skin);
        framesL.setWidth(40 * GlobalConf.SCALE_FACTOR);
        framesL.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.frames", frame, (1d / GlobalConf.frame.RENDER_TARGET_FPS)), skin));
        addHighlightListener(framesL, kf);
        table.add(framesL).left().padRight(pad).padBottom(pad5);

        // Clock - time
        Image clockimg = new Image(skin.getDrawable("clock"));
        clockimg.addListener(new TextTooltip(dateFormat.format(Instant.ofEpochMilli(kf.time)), skin));
        clockimg.setScale(0.7f);
        clockimg.setOrigin(Align.center);
        addHighlightListener(clockimg, kf);
        table.add(clockimg).width(clockimg.getWidth()).left().padRight(pad).padBottom(pad5);

        // Frame name
        addFrameName(kf, index, table);

        // Go to
        OwnTextIconButton goTo = new OwnTextIconButton("", skin, "go-to");
        goTo.setSize(buttonSize, buttonSize);
        goTo.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.goto"), skin));
        goTo.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                // Go to keyframe
                EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraManager.CameraMode.Free_Camera);
                Gdx.app.postRunnable(() -> {
                    EventManager.instance.post(Events.CAMERA_POS_CMD, kf.pos.values());
                    EventManager.instance.post(Events.CAMERA_DIR_CMD, kf.dir.values());
                    EventManager.instance.post(Events.CAMERA_UP_CMD, kf.up.values());
                    EventManager.instance.post(Events.TIME_CHANGE_CMD, Instant.ofEpochMilli(kf.time));
                });
                return true;
            }
            return false;
        });
        addHighlightListener(goTo, kf);
        table.add(goTo).left().padRight(pad5).padBottom(pad5);

        // Seam
        OwnTextIconButton seam = new OwnTextIconButton("", skin, "seam", "toggle");
        seam.setSize(buttonSize, buttonSize);
        seam.setChecked(kf.seam);
        seam.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.seam"), skin));
        seam.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                // Make seam
                kf.seam = seam.isChecked();
                Gdx.app.postRunnable(() -> {
                    keyframesPathObject.refreshData();
                    if (keyframesPathObject.selected == kf) {
                        if (seam.isChecked())
                            keyframesPathObject.selectedKnot.setColor(GlobalResources.gRed);
                        else
                            keyframesPathObject.selectedKnot.setColor(GlobalResources.gWhite);

                    }
                });
                return true;
            }
            return false;
        });
        addHighlightListener(seam, kf);
        table.add(seam).left().padRight(pad5).padBottom(pad5);

        // Add after
        OwnTextIconButton addKeyframe = new OwnTextIconButton("", skin, "add");
        addKeyframe.setSize(buttonSizeL, buttonSize);
        addKeyframe.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.add.after"), skin));
        addKeyframe.addListener(event -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                // Work out keyframe properties
                Keyframe k0, k1;
                Vector3d pos, dir, up;
                long time;
                if (index < keyframes.size - 1) {
                    // We can interpolate
                    k0 = keyframes.get(index);
                    k1 = keyframes.get(index + 1);
                    pos = new Vector3d().set(k0.pos).interpolate(k1.pos, 0.5, Interpolationd.linear);
                    dir = new Vector3d().set(k0.dir).interpolate(k1.dir, 0.5, Interpolationd.linear);
                    up = new Vector3d().set(k0.up).interpolate(k1.up, 0.5, Interpolationd.linear);
                    time = k0.time + (long) ((k1.time - k0.time) / 2d);
                } else {
                    // Last keyframe
                    k0 = keyframes.get(index - 1);
                    k1 = keyframes.get(index);
                    pos = new Vector3d().set(k0.pos).interpolate(k1.pos, 1.5, Interpolationd.linear);
                    dir = new Vector3d().set(k0.dir).interpolate(k1.dir, 1.5, Interpolationd.linear);
                    up = new Vector3d().set(k0.up).interpolate(k1.up, 1.5, Interpolationd.linear);
                    time = k1.time + (long) ((k1.time - k0.time) / 2d);
                }
                // Add at end
                boolean ret = addKeyframe(index + 1, pos, dir, up, time);
                return ret;
            }
            return false;
        });
        addHighlightListener(addKeyframe, kf);
        table.add(addKeyframe).left().padRight(pad5).padBottom(pad5);

        // Rubbish
        OwnTextIconButton rubbish = new OwnTextIconButton("", skin, "rubbish");
        rubbish.setSize(buttonSizeL, buttonSize);
        rubbish.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.remove"), skin));
        rubbish.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                // Remove keyframe
                Array<Keyframe> newKfs = new Array<>(keyframes.size - 1);
                for (Keyframe k : keyframes) {
                    if (k != kf)
                        newKfs.add(k);
                }

                // In case we removed the first
                if (!newKfs.isEmpty())
                    newKfs.get(0).seconds = 0;

                reinitialiseKeyframes(newKfs, null);
                logger.info(I18n.txt("gui.keyframes.removed", kf.name));
                return true;
            }
            return false;
        });
        addHighlightListener(rubbish, kf);
        Cell rub = table.add(rubbish).left().padBottom(pad5);
        rub.row();
        table.pack();

        if (addToModel && keyframesPathObject != null) {
            Gdx.app.postRunnable(() -> {
                // Update model data
                keyframesPathObject.addKnot(kf.pos, kf.dir, kf.up, kf.seam);
                keyframesPathObject.segments.addPoint(kf.pos);
                if (keyframes.size > 1)
                    keyframesPathObject.resamplePath();
            });
        }

    }

    private void addHighlightListener(Actor a, Keyframe kf){
        a.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == InputEvent.Type.enter) {
                    keyframesPathObject.highlight(kf);
                } else if (ie.getType() == InputEvent.Type.exit) {
                    keyframesPathObject.unhighlight(kf);
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public GenericDialog show(Stage stage, Action action) {
        // Subscriptions
        EventManager.instance.subscribe(this, Events.UPDATE_CAM_RECORDER, Events.KEYFRAMES_REFRESH, Events.KEYFRAME_SELECT, Events.KEYFRAME_UNSELECT, Events.KEYFRAME_ADD);
        // Re-add if necessary
        if (keyframesPathObject.getParent() == null) {
            EventManager.instance.post(Events.SCENE_GRAPH_ADD_OBJECT_CMD, keyframesPathObject, false);
        }
        return super.show(stage, action);
    }

    private void reinitialiseKeyframes(Array<Keyframe> kfs, Keyframe moveTo) {
        // Clean
        clean(kfs != keyframes, false);

        // Update list
        if (kfs != keyframes)
            keyframes.addAll(kfs);

        // Add to table
        addKeyframesToTable(keyframes, keyframesTable);

        if (moveTo != null) {
            scrollToKeyframe(moveTo);
        } else {
            scrollToSelected();
        }
    }

    private void clean() {
        clean(true, true);
    }

    private void clean(boolean cleanKeyframesList, boolean cleanModel) {
        if (cleanKeyframesList)
            keyframes.clear();

        notice.clearActor();
        namesCells.clear();
        secondsCells.clear();
        keyframesTable.clearChildren();
        nameInput.setText("");
        secondsInput.setText("1.0");
        if (cleanModel)
            Gdx.app.postRunnable(() -> {
                keyframesPathObject.clear();
            });
    }

    private void scrollToSelected() {
        scrollToKeyframe(keyframesPathObject.selected);
    }

    private void scrollToKeyframe(Keyframe kf) {
        // Scroll to keyframe
        if (rightScroll != null && kf != null) {
            int i = keyframes.indexOf(kf, true);
            int n = keyframes.size;
            if (i >= 0) {
                rightScroll.setScrollPercentY((float) i / (n - 5f));
            }
        }
    }

    @Override
    protected void accept() {
        // Accept not present
    }

    @Override
    protected void cancel() {
        clean();
        EventManager.instance.removeAllSubscriptions(this);
    }

    private Color colorBak;

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
            case KEYFRAME_ADD:
                addKeyframe(-1);
                break;
            case UPDATE_CAM_RECORDER:
                synchronized (lock) {
                    t = (ITimeFrameProvider) data[0];
                    pos = (Vector3d) data[1];
                    dir = (Vector3d) data[2];
                    up = (Vector3d) data[3];
                }
                break;
            case KEYFRAMES_REFRESH:
                reinitialiseKeyframes(keyframes, null);
                break;
            case KEYFRAME_SELECT:
                Keyframe kf = (Keyframe) data[0];
                OwnLabel nl = keyframeNames.get(kf);
                if (nl != null) {
                    colorBak = nl.getColor().cpy();
                    nl.setColor(skin.getColor("theme"));
                    scrollToKeyframe(kf);
                }
                break;
            case KEYFRAME_UNSELECT:
                kf = (Keyframe) data[0];
                nl = keyframeNames.get(kf);
                if (nl != null && colorBak != null) {
                    nl.setColor(colorBak);
                }
                break;
            default:
                break;
        }
    }

    public void dispose() {
        // UI
        clear();

        // Model
        if (keyframesPathObject != null && keyframesPathObject.getParent() != null) {
            Gdx.app.postRunnable(() -> {
                keyframesPathObject.getParent().removeChild(keyframesPathObject, false);
                keyframesPathObject.clear();
                keyframesPathObject = null;
            });
        }

    }
}
