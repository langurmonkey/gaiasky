package gaia.cu9.ari.gaiaorbit.util.scene2d;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ArraySelection;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;

public class FileChooser extends Dialog {

    public interface ResultListener {
        boolean result(boolean success, FileHandle result);
    }

    /**
     * The type of files that can be choosen with this file chooser
     */
    public enum FileChooserTarget {FILES, DIRECTORIES, ALL}

    /** Target of this file chooser **/
    private FileChooserTarget target = FileChooserTarget.ALL;
    private boolean fileNameEnabled;
    private final TextField fileNameInput;
    private final Label fileNameLabel, acceptedFiles;
    private final FileHandle baseDir;
    private final Label fileListLabel;
    private final List<FileListItem> fileList;
    private final HorizontalGroup driveButtonsList;
    private final Array<TextButton> driveButtons;
    private final ScrollPane scrollPane;
    private final CheckBox hidden;

    private FileHandle currentDir;
    protected String result;

    private boolean showHidden = false;

    protected ResultListener resultListener;
    protected EventListener selectionListener;

    private final TextButton ok;
    private final TextButton cancel;

    private static final Comparator<FileListItem> dirListComparator = (file1, file2) -> {
        if (file1.file.isDirectory() && !file2.file.isDirectory()) {
            return -1;
        }
        if (file1.file.isDirectory() && file2.file.isDirectory()) {
            return file1.name.compareTo(file2.name);
        }
        if (!file1.file.isDirectory() && !file2.file.isDirectory()) {
            return file1.name.compareTo(file2.name);
        }
        return 1;
    };
    /** Filters files that appear in the file chooser. For internal use only! **/
    private FileFilter filter;
    /** Enables directories in the file chooser **/
    private boolean directoryBrowsingEnabled = true;
    /** Enables files in the file chooser **/
    private boolean fileBrowsingEnabled = true;
    /** Allows setting filters on the files which are to be selected **/
    private PathnameFilter pathnameFilter;

    public FileChooser(String title, final Skin skin, FileHandle baseDir) {
        this(title, skin, baseDir, null);
    }

    public FileChooser(String title, final Skin skin, FileHandle baseDir, EventListener selectionListener) {
        super(title, skin);
        this.baseDir = baseDir;
        this.selectionListener = selectionListener;

        final Table content = getContentTable();
        content.top().left();
        content.defaults().space(5 * GlobalConf.SCALE_FACTOR);
        this.padLeft(10 * GlobalConf.SCALE_FACTOR);
        this.padRight(10 * GlobalConf.SCALE_FACTOR);

        // In windows, we need to be able to change drives
        driveButtonsList = new HorizontalGroup();
        driveButtonsList.left().space(10 * GlobalConf.SCALE_FACTOR);
        File[] drives = File.listRoots();
        driveButtons = new Array<>(drives.length);
        for (File drive : drives) {
            TextButton driveButton = new OwnTextIconButton(drive.toString(), skin, "drive");
            driveButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    changeDirectory(new FileHandle(drive));
                    lastClick = 0;
                }
            });
            driveButtons.add(driveButton);
            driveButtonsList.addActor(driveButton);
        }

        fileListLabel = new Label("", skin);
        fileListLabel.setAlignment(Align.left);

        acceptedFiles = new Label("", skin);
        acceptedFiles.setAlignment(Align.right);

        fileList = new List<>(skin, "light");
        scrollPane = new ScrollPane(fileList, skin);
        fileList.getSelection().setProgrammaticChangeEvents(false);
        if (selectionListener != null)
            fileList.addListener(selectionListener);
        fileList.addListener(event -> {
            if(event instanceof InputEvent){
                InputEvent ie = (InputEvent) event;
                if(ie.getType() == Type.keyTyped){
                    char ch = ie.getCharacter();
                    Array<FileListItem> l = fileList.getItems();
                    FileListItem toSelect = null;
                    for(FileListItem fli : l){
                        if(Character.toUpperCase(fli.name.charAt(0)) == Character.toUpperCase(ch)){
                            toSelect = fli;
                            break;
                        }
                    }
                    if(toSelect != null){
                        fileList.setSelected(toSelect);
                        int si = fileList.getSelectedIndex();
                        float px = si * fileList.getItemHeight();
                        scrollPane.setScrollY(px);
                    }
                }
            }
            return false;
        });


        fileNameInput = new TextField("", skin);
        fileNameLabel = new Label("File name:", skin);
        fileNameInput.setTextFieldListener((textField, c) -> result = textField.getText());

        hidden = new OwnCheckBox("Show hidden", skin, 5 * GlobalConf.SCALE_FACTOR);
        hidden.setChecked(false);
        hidden.addListener(event -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                this.showHidden = hidden.isChecked();
                changeDirectory(currentDir);
                return true;
            }
            return false;
        });

        getButtonTable().pad(10 * GlobalConf.SCALE_FACTOR);

        ok = new OwnTextButton(I18n.bundle.get("gui.select"), skin);
        button(ok, true);
        ok.setWidth(150 * GlobalConf.SCALE_FACTOR);

        cancel = new OwnTextButton(I18n.bundle.get("gui.cancel"), skin);
        button(cancel, false);
        cancel.setWidth(150 * GlobalConf.SCALE_FACTOR);

        key(Keys.ENTER, true);
        key(Keys.ESCAPE, false);

        fileList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                final FileListItem selected = fileList.getSelected();
                result = selected.name;
                fileNameInput.setText(result);
            }
        });

        filter = pathname -> {
            boolean root = (pathname.isDirectory() && directoryBrowsingEnabled) || (pathname.isFile() && fileBrowsingEnabled);
            if (root && pathnameFilter != null && pathname.isFile()) {
                root = pathnameFilter.accept(pathname);
            }
            return root;
        };
        setTargetListener();
    }

    private void changeDirectory(FileHandle directory) {

        currentDir = directory;
        fileListLabel.setText(currentDir.path());

        final Array<FileListItem> items = new Array<FileListItem>();

        final FileHandle[] list = directory.list(filter);
        for (final FileHandle handle : list) {
            if (showHidden || !handle.name().startsWith(".")) {
                FileListItem fli = new FileListItem(handle);
                items.add(fli);
            }
        }

        items.sort(dirListComparator);

        if (directory.file().getParentFile() != null) {
            items.insert(0, new FileListItem("..", directory.parent()));
        }

        fileList.setSelected(null);
        ok.setDisabled(true);
        fileList.setItems(items);
        scrollPane.setScrollY(0);
    }

    public void setTarget(FileChooserTarget t) {
        if (t != null) {
            this.target = t;
            setTargetListener();
        }
    }

    private boolean isTargetOk(File file) {
        switch (target) {
        case ALL:
            return true;
        case FILES:
            return file.isFile();
        case DIRECTORIES:
            return file.isDirectory();
        default:
            return true;
        }
    }

    private void setTargetListener() {
        setSelectionListener(event1 -> {
            if (event1 instanceof ChangeListener.ChangeEvent) {
                List<FileChooser.FileListItem> list = (List<FileChooser.FileListItem>) event1.getListenerActor();
                if (list != null) {
                    ArraySelection<FileListItem> as = list.getSelection();
                    if (as != null && as.hasItems()) {
                        FileChooser.FileListItem selected = as.getLastSelected();
                        ok.setDisabled(!isTargetOk(selected.file.file()));
                    }
                }
                return true;
            }
            return false;
        });
    }

    public void setAcceptedFiles(String accepted) {
        acceptedFiles.setText("Accepted: " + accepted);
    }

    public FileHandle getResult() {
        String path = currentDir.path() + "/";
        if (result != null && result.length() > 0) {
            String folder = currentDir.file().getName();
            if (folder.equals(result)) {
                if ((new FileHandle(path + result)).exists()) {
                    path += result;
                } else {
                    // Nothing
                }
            } else {
                path += result;
            }
        }
        return new FileHandle(path);
    }

    /**
     * Overrides the default filter. If you use this, the attributes {@link FileChooser#directoryBrowsingEnabled} and
     * {@file FileChooser#fileBrowsingEnabled} won't have effect anymore. To set additional filters on the
     * path names, use {@link FileChooser#setFileFilter(PathnameFilter)} instead.
     *
     * @param filter The new file filter
     * @return This file chooser
     */
    public FileChooser setFilter(FileFilter filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets the file filter. This filter will be used to check whether file pathnames are accepted or not. It works
     * in conjunction with {@file FileChooser#fileBrowsingEnabled},
     * so you do not need to check whether the pathname is a file.
     *
     * @param f The file filter
     * @return This file chooser
     */
    public FileChooser setFileFilter(PathnameFilter f) {
        this.pathnameFilter = f;
        return this;
    }

    public FileChooser setOkButtonText(String text) {
        this.ok.setText(text);
        return this;
    }

    /**
     * Sets a listener which runs when an entry is selected. Useful to show
     * some text, disable items, etc.
     *
     * @param listener The listener
     * @return This file chooser
     */
    private FileChooser setSelectionListener(EventListener listener) {
        if (listener != null) {
            if (this.selectionListener != null)
                fileList.removeListener(this.selectionListener);

            this.selectionListener = listener;
            if (!fileList.getListeners().contains(selectionListener, true)) {
                fileList.addListener(selectionListener);
            }
        }
        return this;
    }

    public FileChooser setCancelButtonText(String text) {
        this.cancel.setText(text);
        return this;
    }

    public FileChooser setFileNameEnabled(boolean fileNameEnabled) {
        this.fileNameEnabled = fileNameEnabled;
        return this;
    }

    public FileChooser setResultListener(ResultListener result) {
        this.resultListener = result;
        return this;
    }

    public FileChooser setDirectoryBrowsingEnabled(boolean directoryBrowsingEnabled) {
        this.directoryBrowsingEnabled = directoryBrowsingEnabled;
        return this;
    }

    public FileChooser setFileBrowsingEnabled(boolean fileBrowsingEnabled) {
        this.fileBrowsingEnabled = fileBrowsingEnabled;
        return this;
    }

    long lastClick = 0l;

    @Override
    public Dialog show(Stage stage, Action action) {
        final Table content = getContentTable();
        content.add(acceptedFiles).top().left().row();
        content.add(driveButtonsList).top().left().expandX().fillX().row();
        content.add(fileListLabel).top().left().expandX().fillX().row();
        content.add(scrollPane).size(330 * GlobalConf.SCALE_FACTOR, 200 * GlobalConf.SCALE_FACTOR).left().fill().expand().row();
        content.add(hidden).top().left().row();
        if (fileNameEnabled) {
            content.add(fileNameLabel).fillX().expandX().row();
            content.add(fileNameInput).fillX().expandX().row();
            stage.setKeyboardFocus(fileNameInput);
        }

        if (directoryBrowsingEnabled) {
            fileList.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    final FileListItem selected = fileList.getSelected();
                    if (selected.file.isDirectory() && TimeUtils.millis() - lastClick < 500) {
                        changeDirectory(selected.file);
                        lastClick = 0;
                    } else if (event.getType() == Type.touchUp) {
                        lastClick = TimeUtils.millis();
                    }
                }
            });
        }

        changeDirectory(baseDir);
        return super.show(stage, action);
    }

    public static FileChooser createSaveDialog(String title, final Skin skin, final FileHandle path) {
        final FileChooser save = new FileChooser(title, skin, path) {
            @Override
            protected void result(Object object) {

                if (resultListener == null) {
                    return;
                }

                final boolean success = (Boolean) object;
                if (!resultListener.result(success, getResult())) {
                    this.cancel();
                }
            }
        }.setFileNameEnabled(true).setOkButtonText("Save");

        return save;

    }

    public static FileChooser createLoadDialog(String title, final Skin skin, final FileHandle path) {
        final FileChooser load = new FileChooser(title, skin, path) {
            @Override
            protected void result(Object object) {

                if (resultListener == null) {
                    return;
                }

                final boolean success = (Boolean) object;
                resultListener.result(success, getResult());
            }
        }.setFileNameEnabled(false).setOkButtonText("Load");

        return load;

    }

    public static FileChooser createPickDialog(String title, final Skin skin, final FileHandle path) {
        final FileChooser pick = new FileChooser(title, skin, path) {
            @Override
            protected void result(Object object) {

                if (resultListener == null) {
                    return;
                }

                final boolean success = (Boolean) object;
                resultListener.result(success, getResult());
            }
        }.setOkButtonText("Select");

        return pick;
    }

    public class FileListItem {

        public FileHandle file;
        public String name;

        public FileListItem(FileHandle file) {
            this.file = file;
            this.name = file.name();
        }

        public FileListItem(String name, FileHandle file) {
            this.file = file;
            this.name = name;
        }

        public String toString() {
            return name;
        }

    }

    public interface PathnameFilter {
        boolean accept(File pathname);
    }

}