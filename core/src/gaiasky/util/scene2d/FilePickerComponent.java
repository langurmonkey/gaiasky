/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
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
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * A generic file picker component extending {@link Table}.
 */
public class FilePickerComponent extends Table {
    private static final Logger.Log logger = Logger.getLogger(FilePickerComponent.class);
    private static final Comparator<FileListItem> dirListComparator = (file1, file2) -> {
        if (Files.isDirectory(file1.file) && !Files.isDirectory(file2.file)) return -1;
        if (Files.isDirectory(file1.file) && Files.isDirectory(file2.file)) return file1.name.compareTo(file2.name);
        if (!Files.isDirectory(file1.file) && !Files.isDirectory(file2.file)) return file1.name.compareTo(file2.name);
        return 1;
    };

    private final Skin skin;
    private final Stage stage;
    private final FilePickerTarget target;
    private final boolean directoryBrowsingEnabled;
    private final boolean fileBrowsingEnabled;

    private OwnTextField location;
    private List<FileListItem> fileList;
    private ScrollPane scrollPane;
    private CheckBox hidden;
    private TextField fileNameInput;
    private Label acceptedFiles;

    private Path currentDir;
    private String result;
    private final Array<Path> history = new Array<>();
    private int historyIndex = -1;
    private boolean isNavigatingHistory = false;
    private boolean showHidden = false;
    private boolean fileNameEnabled = false;

    private Consumer<Boolean> selectionValidListener;
    private Runnable onDoubleAccept;
    private DirectoryStream.Filter<Path> filter;
    private PathnameFilter pathnameFilter;
    private Consumer<Boolean> hiddenConsumer;

    public FilePickerComponent(Skin skin, Stage stage, Path baseDir, FilePickerTarget target, boolean directoryBrowsingEnabled) {
        this.skin = skin;
        this.stage = stage;
        this.target = target;
        this.directoryBrowsingEnabled = directoryBrowsingEnabled;
        this.fileBrowsingEnabled = target == FilePickerTarget.FILES;

        init();
        try {
            navigateTo(baseDir);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void init() {
        this.top().left();
        this.defaults().space(8f).pad(0, 16, 0, 16);

        setupComponents();

        // Assemble Layout
        add(acceptedFiles).top().left().row();
        add(createDriveBar()).top().left().row();
        add(createControlsTable()).top().left().expandX().fillX().row();
        add(scrollPane).size(960f, 720f).left().fill().expand().row();
        add(hidden).top().left().row();

        // Placeholder for filename input (only shows if enabled)
        updateFileNameVisibility();
    }

    private void setupComponents() {
        acceptedFiles = new Label("", skin, "sc-header");
        acceptedFiles.setAlignment(Align.right);

        location = new OwnTextField("", skin);
        location.setErrorColor(ColorUtils.gRedC);
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
                } catch (Exception ignored) {
                    location.setToErrorColor();
                }
                return true;
            }
            return false;
        });

        fileList = new List<>(skin, "light");
        scrollPane = new OwnScrollPane(fileList, skin, "minimalist");
        scrollPane.setFadeScrollBars(false);
        fileList.getSelection().setProgrammaticChangeEvents(false);

        // Keyboard navigation
        fileList.addListener(event -> {
            if (event instanceof InputEvent ie && ie.getType() == InputEvent.Type.keyTyped) {
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

        // Selection Change
        fileList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FileListItem selected = fileList.getSelected();
                if (selected != null) {
                    result = selected.name;
                    if (fileNameInput != null) fileNameInput.setText(result);
                    if (selectionValidListener != null) selectionValidListener.accept(isTargetOk(selected.file));
                }
            }
        });

        // Double Click
        fileList.addListener(new ClickListener() {
            private long lastClickTime = 0;

            @Override
            public void clicked(InputEvent event, float x, float y) {
                FileListItem selected = fileList.getSelected();
                if (selected == null) return;

                if (TimeUtils.millis() - lastClickTime < 250) {
                    try {
                        if (directoryBrowsingEnabled && Files.isDirectory(selected.file)) {
                            navigateTo(selected.file);
                        } else if (onDoubleAccept != null && isTargetOk(selected.file)) {
                            onDoubleAccept.run();
                        }
                    } catch (IOException e) {
                        logger.error(e);
                    }
                } else if (event.getType() == InputEvent.Type.touchUp) {
                    lastClickTime = TimeUtils.millis();
                }
            }
        });

        fileNameInput = new OwnTextField("", skin);
        fileNameInput.setTextFieldListener((textField, c) -> result = textField.getText());

        hidden = new OwnCheckBox(I18n.msg("gui.fc.showhidden"), skin, 8f);
        hidden.addListener(event -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                showHidden = hidden.isChecked();
                try {
                    refreshDirectory();
                } catch (IOException e) {
                    logger.error(e);
                }
                if (hiddenConsumer != null) hiddenConsumer.accept(showHidden);
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
    }

    private Table createControlsTable() {
        Table table = new Table(skin);
        OwnTextIconButton home = new OwnTextIconButton("", skin, "home");
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

        OwnTextIconButton up = new OwnTextIconButton("", skin, "up");
        up.addListener(new ChangeListener() {
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

        table.add(home).padRight(10);
        table.add(back).padRight(10);
        table.add(up).padRight(10);
        table.add(fwd).padRight(18);
        table.add(location).expandX().fillX().row();

        if (target == FilePickerTarget.DIRECTORIES || target == FilePickerTarget.ALL) {
            OwnTextIconButton newDirBtn = new OwnTextIconButton(I18n.msg("gui.fc.newdirectory"), skin, "add");
            newDirBtn.addListener(new ChangeListener() {
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
            table.add(newDirBtn).colspan(5).right().padTop(10).padBottom(10);
        }
        return table;
    }

    private HorizontalGroup createDriveBar() {
        HorizontalGroup group = new HorizontalGroup().left().space(16f);
        group.addActor(new OwnLabel(I18n.msg("gui.fc.drives") + ":", skin));
        for (Path drive : FileSystems.getDefault().getRootDirectories()) {
            TextButton btn = new OwnTextIconButton(drive.toString(), skin, "drive");
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

        String pathStr = currentDir.toString();
        float maxPL = 6.5f;
        while (pathStr.length() * maxPL > 960f * 0.9f) {
            pathStr = TextUtils.capString(pathStr, pathStr.length() - 4, true);
        }
        location.setText(text != null && !text.isBlank() ? text : pathStr);
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

        if (directory.getParent() != null) items.insert(0, new FileListItem("..", directory.getParent()));

        fileList.setItems(items);
        fileList.setSelected(null);
        if (selectionValidListener != null) selectionValidListener.accept(target == FilePickerTarget.DIRECTORIES);

        // Selection restoration when going back
        if (lastDir != null && lastDir.startsWith(currentDir)) {
            for (FileListItem fli : fileList.getItems()) {
                if (fli.file.equals(lastDir)) {
                    fileList.setSelected(fli);
                    scrollPane.setScrollY(fileList.getSelectedIndex() * fileList.getItemHeight());
                    break;
                }
            }
        }
    }

    private boolean isTargetOk(Path file) {
        return switch (target) {
            case FILES -> Files.isRegularFile(file);
            case DIRECTORIES -> Files.isDirectory(file);
            default -> true;
        };
    }

    private void updateFileNameVisibility() {
        if (fileNameEnabled) {
            row();
            add(new Label(I18n.msg("gui.fc.filename") + ":", skin)).fillX().row();
            add(fileNameInput).fillX().row();
            stage.setKeyboardFocus(fileNameInput);
        }
    }

    public Path getResult() {
        if (result == null || result.isEmpty()) return currentDir;
        Path resolved = currentDir.resolve(result).normalize();
        return Files.exists(resolved) ? resolved : currentDir.resolve(result);
    }

    public void setSelectionValidListener(Consumer<Boolean> listener) {
        this.selectionValidListener = listener;
    }

    public void setOnDoubleAccept(Runnable runnable) {
        this.onDoubleAccept = runnable;
    }

    public void setFileNameEnabled(boolean enabled) {
        this.fileNameEnabled = enabled;
        updateFileNameVisibility();
    }

    public void setShowHidden(boolean show) {
        hidden.setChecked(show);
    }

    public void setShowHiddenConsumer(Consumer<Boolean> c) {
        this.hiddenConsumer = c;
    }

    public void setAcceptedFiles(String text) {
        acceptedFiles.setText(I18n.msg("gui.fc.accepted", text));
    }

    public void setFileFilter(PathnameFilter f) {
        this.pathnameFilter = f;
        try {
            refreshDirectory();
        } catch (IOException e) {
            logger.error(e);
        }
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