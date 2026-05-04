/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ArraySelection;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.gui.window.GenericDialog;
import gaiasky.util.Logger;
import gaiasky.util.SysUtils;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.validator.DirectoryNameValidator;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * A generic file picker that enables choosing a single file or directory.
 */
public class FilePicker extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(FilePicker.class);
    private static final Comparator<FileListItem> dirListComparator = (file1, file2) -> {
        if (Files.isDirectory(file1.file) && !Files.isDirectory(file2.file)) {
            return -1;
        }
        if (Files.isDirectory(file1.file) && Files.isDirectory(file2.file)) {
            return file1.name.compareTo(file2.name);
        }
        if (!Files.isDirectory(file1.file) && !Files.isDirectory(file2.file)) {
            return file1.name.compareTo(file2.name);
        }
        return 1;
    };

    /** Target of this file picker. **/
    final private FilePickerTarget target;
    private final Path baseDir;
    /** Enables directories in the file picker. **/
    private final boolean directoryBrowsingEnabled;
    /** Enables files in the file picker. **/
    private final boolean fileBrowsingEnabled;
    protected String result;
    protected ResultListener resultListener;
    protected EventListener selectionListener;
    private boolean fileNameEnabled;
    private TextField fileNameInput;
    private Label acceptedFiles;
    private OwnTextField location;
    private List<FileListItem> fileList;
    private float scrollPaneWidth;
    private float maxPathLength;
    private ScrollPane scrollPane;
    private CheckBox hidden;
    private Consumer<Boolean> hiddenConsumer;
    private long lastClick = 0L;
    private boolean showHidden = false;

    // Navigation history stack
    private final Array<Path> history = new Array<>();
    private int historyIndex = -1;
    private boolean isNavigatingHistory = false;
    private Path currentDir;

    /** Filters files that appear in the file picker. For internal use only! **/
    private DirectoryStream.Filter<Path> filter;
    /** Allows setting filters on the files which are to be selected. **/
    private PathnameFilter pathnameFilter;

    public FilePicker(String title, final Skin skin, Stage stage, Path baseDir, FilePickerTarget target) {
        this(title, skin, stage, baseDir, target, null);
    }

    public FilePicker(String title, final Skin skin, Stage stage, Path baseDir, FilePickerTarget target, EventListener selectionListener) {
        this(title, skin, stage, baseDir, target, selectionListener, true);
    }

    public FilePicker(String title,
                      final Skin skin,
                      Stage stage,
                      Path baseDir,
                      FilePickerTarget target,
                      EventListener selectionListener,
                      boolean directoryBrowsingEnabled) {
        super(title, skin, stage);
        this.baseDir = baseDir;
        this.selectionListener = selectionListener;
        this.target = target;
        // Browse files if we are picking files
        this.fileBrowsingEnabled = target == FilePickerTarget.FILES;
        // Browse directories
        this.directoryBrowsingEnabled = directoryBrowsingEnabled;

        setCancelText(I18n.msg("gui.close"));
        setAcceptText(I18n.msg("gui.select"));

        buildSuper();
    }

    @Override
    public void build() {
        scrollPaneWidth = 960f;
        float scrollPanelHeight = 720f;
        maxPathLength = 9.5f;

        content.top().left();
        content.defaults().space(8f);
        this.padLeft(16f);
        this.padRight(16f);

        // --- UI Setup ---
        setupComponents();
        Table controlsTable = createControlsTable();
        HorizontalGroup driveButtonsList = createDriveBar();

        // --- Layout Assembly ---
        content.add(acceptedFiles).top().left().row();
        content.add(driveButtonsList).top().left().row();
        content.add(controlsTable).top().left().expandX().fillX().row();
        content.add(scrollPane).size(scrollPaneWidth, scrollPanelHeight).left().fill().expand().row();
        content.add(hidden).top().left().row();

        if (fileNameEnabled) {
            content.add(new Label(I18n.msg("gui.fc.filename") + ":", skin)).fillX().row();
            content.add(fileNameInput).fillX().row();
            stage.setKeyboardFocus(fileNameInput);
        }

        // Initial directory load
        try {
            navigateTo(baseDir);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void setupComponents() {
        acceptedFiles = new Label("", skin, "sc-header");
        acceptedFiles.setAlignment(Align.right);

        location = new OwnTextField("", skin);
        location.setErrorColor(ColorUtils.gRedC);
        location.setAlignment(Align.left);
        location.addListener(event -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                try {
                    Path locPath = Path.of(location.getText());
                    if (Files.exists(locPath) && Files.isDirectory(locPath)) {
                        navigateTo(locPath);
                        GaiaSky.postRunnable(() -> {
                            stage.setKeyboardFocus(location);
                            location.setCursorPosition(location.getText().length());
                        });
                    } else {
                        location.setToErrorColor();
                    }
                } catch (InvalidPathException | IOException ignored) {
                    location.setToErrorColor();
                }
                return true;
            }
            return false;
        });

        fileList = new List<>(skin, "light");
        scrollPane = new OwnScrollPane(fileList, skin, "minimalist");
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setSmoothScrolling(true);
        scrollPane.setFadeScrollBars(false);
        fileList.getSelection().setProgrammaticChangeEvents(false);

        if (selectionListener != null) fileList.addListener(selectionListener);

        // Keyboard Lookup
        fileList.addListener(event -> {
            if (event instanceof InputEvent ie && ie.getType() == Type.keyTyped) {
                char ch = Character.toUpperCase(ie.getCharacter());
                for (FileListItem fli : fileList.getItems()) {
                    if (Character.toUpperCase(fli.name.charAt(0)) == ch) {
                        fileList.setSelected(fli);
                        scrollPane.setScrollY(fileList.getSelectedIndex() * fileList.getItemHeight());
                        break;
                    }
                }
            }
            return false;
        });

        // Click Selection
        fileList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FileListItem selected = fileList.getSelected();
                if (selected != null) {
                    result = selected.name;
                    fileNameInput.setText(result);
                }
                lastClick = 0;
            }
        });

        // Double-click behaviour.
        fileList.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                final FileListItem selected = fileList.getSelected();
                if (selected == null) return;

                if (TimeUtils.millis() - lastClick < 250) {
                    Path sel = selected.file;
                    try {
                        if (directoryBrowsingEnabled && Files.isDirectory(sel)) {
                            navigateTo(sel);
                            lastClick = 0;
                        } else if (target == FilePickerTarget.FILES && (filter == null || filter.accept(sel))) {
                            acceptButton.fire(new ChangeListener.ChangeEvent());
                            lastClick = 0;
                        }
                    } catch (IOException e) {
                        logger.error(e);
                    }
                } else if (event.getType() == Type.touchUp) {
                    lastClick = TimeUtils.millis();
                }
            }
        });

        fileNameInput = new OwnTextField("", skin);
        fileNameInput.setTextFieldListener((textField, c) -> result = textField.getText());

        hidden = new OwnCheckBox(I18n.msg("gui.fc.showhidden"), skin, 8f);
        hidden.addListener(event -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                this.showHidden = hidden.isChecked();
                try {
                    refreshDirectory();
                } catch (IOException e) {
                    logger.error(e);
                }
                if (hiddenConsumer != null) hiddenConsumer.accept(this.showHidden);
                return true;
            }
            return false;
        });

        filter = pathname -> {
            boolean root = (Files.isDirectory(pathname) && directoryBrowsingEnabled) || (Files.isRegularFile(pathname) && fileBrowsingEnabled);
            if (root && pathnameFilter != null && Files.isRegularFile(pathname)) {
                root = pathnameFilter.accept(pathname);
            }
            return root;
        };
        setTargetListener();
    }

    private Table createControlsTable() {
        Table table = new Table(skin);

        OwnTextIconButton home = new OwnTextIconButton("", skin, "home");
        home.addListener(new OwnTextTooltip(I18n.msg("gui.fc.home"), skin));
        home.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    navigateTo(SysUtils.getHomeDir());
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        });

        OwnTextIconButton back = new OwnTextIconButton("", skin, "back");
        back.addListener(new OwnTextTooltip(I18n.msg("gui.fc.back"), skin));
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (historyIndex > 0) {
                    isNavigatingHistory = true;
                    try {
                        navigateTo(history.get(--historyIndex));
                    } catch (IOException e) {
                        logger.error(e);
                    }
                    isNavigatingHistory = false;
                }
            }
        });

        OwnTextIconButton fwd = new OwnTextIconButton("", skin, "forward");
        fwd.addListener(new OwnTextTooltip(I18n.msg("gui.fc.forward"), skin));
        fwd.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (historyIndex < history.size - 1) {
                    isNavigatingHistory = true;
                    try {
                        navigateTo(history.get(++historyIndex));
                    } catch (IOException e) {
                        logger.error(e);
                    }
                    isNavigatingHistory = false;
                }
            }
        });

        OwnTextIconButton parent = new OwnTextIconButton("", skin, "up");
        parent.addListener(new OwnTextTooltip(I18n.msg("gui.fc.parent"), skin));
        parent.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (currentDir.getParent() != null) {
                    try {
                        navigateTo(currentDir.getParent());
                    } catch (IOException e) {
                        logger.error(e);
                    }
                }
            }
        });

        table.add(home).left().padRight(10);
        table.add(back).left().padRight(10);
        table.add(parent).left().padRight(10);
        table.add(fwd).left().padRight(18);
        table.add(location).expandX().fillX().row();

        if (target == FilePickerTarget.DIRECTORIES || target == FilePickerTarget.ALL) {
            OwnTextIconButton newDirectory = new OwnTextIconButton(I18n.msg("gui.fc.newdirectory"), skin, "add");
            newDirectory.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    NewDirectoryDialog diag = new NewDirectoryDialog(skin, stage);
                    diag.setAcceptListener(() -> {
                        if (diag.name.isValid()) {
                            try {
                                Files.createDirectory(currentDir.resolve(diag.getNewDirectoryName()));
                                refreshDirectory();
                            } catch (IOException e) {
                                logger.error(e);
                            }
                        }
                    });
                    diag.show(stage);
                }
            });
            table.add(newDirectory).colspan(5).right().padTop(10).padBottom(10);
        }

        return table;
    }

    private HorizontalGroup createDriveBar() {
        HorizontalGroup group = new HorizontalGroup().left().space(16f);
        group.addActor(new OwnLabel(I18n.msg("gui.fc.drives") + ":", skin));
        for (Path drive : FileSystems.getDefault().getRootDirectories()) {
            TextButton btn = new OwnTextIconButton(drive.toString(), skin, "drive");
            btn.addListener(new OwnTextTooltip(I18n.msg("gui.fc.drive", drive.toString()), skin));
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    try {
                        navigateTo(drive);
                    } catch (IOException e) {
                        logger.error(e);
                    }
                }
            });
            group.addActor(btn);
        }
        return group;
    }

    /**
     * Centralized navigation method that handles the history stack.
     */
    private void navigateTo(Path path) throws IOException {
        path = path.toAbsolutePath().normalize();
        if (!isNavigatingHistory) {
            if (historyIndex < history.size - 1) history.truncate(historyIndex + 1);
            history.add(path);
            historyIndex++;
        }
        updateDirectoryDisplay(path, null);
    }

    private void refreshDirectory() throws IOException {
        updateDirectoryDisplay(currentDir, location.getText());
    }

    private void updateDirectoryDisplay(Path directory, String text) throws IOException {
        Path lastDir = (currentDir != null && !directory.equals(currentDir)) ? currentDir : null;
        currentDir = directory;

        // Path truncation logic
        String path = currentDir.toString();
        maxPathLength = 6.5f;
        while (path.length() * maxPathLength > scrollPaneWidth * 0.9f) {
            path = TextUtils.capString(path, path.length() - 4, true);
        }
        location.setText(text != null && !text.isBlank() ? text : path);
        location.setToRegularColor();

        final Array<FileListItem> items = new Array<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, filter)) {
            for (Path p : stream) {
                if (showHidden || !p.getFileName().toString().startsWith(".")) {
                    if (pathnameFilter == null || pathnameFilter.accept(p) || Files.isDirectory(p)) {
                        items.add(new FileListItem(p));
                    }
                }
            }
        }
        items.sort(dirListComparator);

        if (directory.getParent() != null) {
            items.insert(0, new FileListItem("..", directory.getParent()));
        }

        if (target != FilePickerTarget.DIRECTORIES) acceptButton.setDisabled(true);
        fileList.setItems(items);
        scrollPane.layout();

        // Auto-select logic when going "Up".
        if (lastDir != null && lastDir.startsWith(currentDir)) {
            for (FileListItem fli : fileList.getItems()) {
                if (fli.file.equals(lastDir)) {
                    fileList.setSelected(fli);
                    acceptButton.setDisabled(!isTargetOk(fli.file));
                    scrollPane.setScrollY(fileList.getSelectedIndex() * fileList.getItemHeight());
                    break;
                }
            }
        } else {
            fileList.setSelected(null);
            scrollPane.setScrollY(0);
        }
    }

    private boolean isTargetOk(Path file) {
        return switch (target) {
            case FILES -> Files.isRegularFile(file);
            case DIRECTORIES -> Files.isDirectory(file);
            default -> true;
        };
    }

    private void setTargetListener() {
        setSelectionListener(event1 -> {
            if (event1 instanceof ChangeListener.ChangeEvent) {
                List<FilePicker.FileListItem> list = (List<FilePicker.FileListItem>) event1.getListenerActor();
                if (list != null) {
                    ArraySelection<FileListItem> as = list.getSelection();
                    if (as != null && as.notEmpty()) {
                        FilePicker.FileListItem selected = as.getLastSelected();
                        acceptButton.setDisabled(!isTargetOk(selected.file));
                    }
                }
                return true;
            }
            return false;
        });
    }

    public void setShowHidden(boolean showHidden) {
        hidden.setChecked(showHidden);
    }

    public void setShowHiddenConsumer(Consumer<Boolean> r) {
        this.hiddenConsumer = r;
    }

    public void setAcceptedFiles(String accepted) {
        acceptedFiles.setText(I18n.msg("gui.fc.accepted", accepted));
    }

    public Path getResult() {
        if (result == null || result.isEmpty()) return currentDir;
        // Resolve path safely using NIO
        Path resolved = currentDir.resolve(result).normalize();
        return Files.exists(resolved) ? resolved : currentDir.resolve(result);
    }

    public FilePicker setFilter(DirectoryStream.Filter<Path> filter) {
        this.filter = filter;
        return this;
    }

    public void setFileFilter(PathnameFilter f) {
        this.pathnameFilter = f;
        try {
            refreshDirectory();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void setSelectionListener(EventListener listener) {
        if (listener != null) {
            if (this.selectionListener != null) fileList.removeListener(this.selectionListener);
            this.selectionListener = listener;
            if (!fileList.getListeners().contains(selectionListener, true)) {
                fileList.addListener(selectionListener);
            }
        }
    }

    public FilePicker setFileNameEnabled(boolean fileNameEnabled) {
        this.fileNameEnabled = fileNameEnabled;
        return this;
    }

    public void setResultListener(ResultListener result) {
        this.resultListener = result;
    }

    @Override
    public boolean accept() {
        if (resultListener != null) resultListener.result(true, getResult());
        return true;
    }

    @Override
    public void cancel() {
        if (resultListener != null) resultListener.result(false, getResult());
    }

    @Override
    public void dispose() {
    }

    public enum FilePickerTarget {FILES, DIRECTORIES, ALL}

    public interface ResultListener {
        boolean result(boolean success, Path result);
    }

    public interface PathnameFilter {
        boolean accept(Path pathname);
    }

    public static class FileListItem {
        public Path file;
        public String name;

        public FileListItem(Path file) {
            this.file = file;
            this.name = file.getFileName() == null ? file.toString() : file.getFileName().toString();
        }

        public FileListItem(String name, Path file) {
            this.file = file;
            this.name = name;
        }

        @Override
        public String toString() {
            return " " + name;
        }
    }

    private static class NewDirectoryDialog extends GenericDialog {
        public OwnTextField name;

        public NewDirectoryDialog(Skin skin, Stage ui) {
            super(I18n.msg("gui.fc.newdirectory.title"), skin, ui);
            setAcceptText(I18n.msg("gui.ok"));
            setCancelText(I18n.msg("gui.cancel"));
            buildSuper();
        }

        @Override
        protected void build() {
            content.clear();
            content.add(new OwnLabel(I18n.msg("gui.fc.newdirectory.name"), skin)).left().padRight(18);
            name = new OwnTextField("", skin);
            name.setWidth(350f);
            name.setMessageText(I18n.msg("gui.fc.newdirectory"));
            name.setValidator(new DirectoryNameValidator());
            content.add(name).left();
        }

        public String getNewDirectoryName() {
            return (name != null && !name.getText().isEmpty()) ? name.getText() : I18n.msg("gui.fc.newdirectory");
        }

        @Override
        public void setKeyboardFocus() {
            stage.setKeyboardFocus(name);
        }

        @Override
        protected boolean accept() {
            return true;
        }

        @Override
        protected void cancel() {
        }

        @Override
        public void dispose() {
        }
    }
}