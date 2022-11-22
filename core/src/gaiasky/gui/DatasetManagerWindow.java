/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.Timer;
import gaiasky.GaiaSky;
import gaiasky.desktop.GaiaSkyDesktop;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.input.AbstractGamepadListener;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.datadesc.DataDescriptor;
import gaiasky.util.datadesc.DataDescriptorUtils;
import gaiasky.util.datadesc.DatasetDesc;
import gaiasky.util.datadesc.DatasetType;
import gaiasky.util.i18n.I18n;
import gaiasky.util.io.FileInfoInputStream;
import gaiasky.util.scene2d.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dataset manager. It gets a descriptor file from the server containing all
 * available datasets, detects them in the current system and offers and manages
 * their downloads.
 */
public class DatasetManagerWindow extends GenericDialog {
    private static final Log logger = Logger.getLogger(DatasetManagerWindow.class);

    private static final Map<String, String> iconMap;

    static {
        iconMap = new HashMap<>();
        iconMap.put("other", "icon-elem-others");
        iconMap.put("data-pack", "icon-elem-others");
        iconMap.put("catalog-lod", "icon-elem-stars");
        iconMap.put("catalog-gaia", "icon-elem-stars");
        iconMap.put("catalog-star", "icon-elem-stars");
        iconMap.put("catalog-gal", "icon-elem-galaxies");
        iconMap.put("catalog-cluster", "icon-elem-clusters");
        iconMap.put("catalog-sso", "icon-elem-asteroids");
        iconMap.put("catalog-other", "icon-elem-others");
        iconMap.put("mesh", "icon-elem-meshes");
        iconMap.put("spacecraft", "icon-elem-satellites");
        iconMap.put("system", "iconic-target");
        iconMap.put("texture-pack", "icon-elem-moons");
    }

    public static int getTypeWeight(String type) {
        return switch (type) {
            case "data-pack" -> -2;
            case "texture-pack" -> -1;
            case "catalog-lod" -> 0;
            case "catalog-gaia" -> 1;
            case "catalog-star" -> 2;
            case "catalog-gal" -> 3;
            case "catalog-cluster" -> 4;
            case "catalog-sso" -> 5;
            case "system" -> 6;
            case "spacecraft" -> 7;
            case "mesh" -> 8;
            case "other" -> 9;
            case "catalog-other" -> 10;
            default -> 11;
        };
    }

    public static String getIcon(String type) {
        if (type != null && iconMap.containsKey(type))
            return iconMap.get(type);
        return "icon-elem-others";
    }

    private DataDescriptor serverDd, localDd;
    private DatasetMode currentMode;
    private Cell<?> left, right;
    private OwnScrollPane leftScroll;
    private final DatasetDesc[] selectedDataset;
    private final float[][] scroll;

    private final Map<String, Pair<DatasetDesc, Net.HttpRequest>> currentDownloads;

    private final Map<String, Button>[] buttonMap;
    private final List<Pair<DatasetDesc, Actor>> selectionOrder;
    private int selectedIndex = 0;

    private final Color highlight;

    // Whether to show the data location chooser
    private final boolean dataLocation;

    private final DecimalFormat nf;

    private final Set<DatasetWatcher> watchers;
    private DatasetWatcher rightPaneWatcher;

    private final AtomicBoolean initialized;

    public DatasetManagerWindow(Stage stage, Skin skin, DataDescriptor serverDd) {
        this(stage, skin, serverDd, true, I18n.msg("gui.close"));
    }

    public DatasetManagerWindow(Stage stage, Skin skin, DataDescriptor serverDd, boolean dataLocation, String acceptText) {
        super(I18n.msg("gui.download.title") + (serverDd != null && serverDd.updatesAvailable ? " - " + I18n.msg("gui.download.updates", serverDd.numUpdates) : ""), skin, stage);
        this.nf = new DecimalFormat("##0.0");
        this.serverDd = serverDd;
        this.highlight = ColorUtils.gYellowC;
        this.watchers = new HashSet<>();
        this.scroll = new float[][] { { 0f, 0f }, { 0f, 0f } };
        this.selectedDataset = new DatasetDesc[2];
        this.initialized = new AtomicBoolean(false);
        this.buttonMap = new HashMap[2];
        this.buttonMap[0] = new HashMap<>();
        this.buttonMap[1] = new HashMap<>();
        this.currentDownloads = Collections.synchronizedMap(new HashMap<>());
        this.selectionOrder = new ArrayList<>();

        this.gamepadListener = new DatasetManagerGamepadListener(Settings.settings.controls.gamepad.mappingsFile);
        this.dataLocation = dataLocation;

        setAcceptText(acceptText);

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        initialized.set(false);
        float tabWidth = 500f;
        float width = 1800f;

        try {
            Files.createDirectories(SysUtils.getLocalDataDir());
        } catch (FileAlreadyExistsException e) {
            // Good
        } catch (IOException e) {
            logger.error(e);
            return;
        }

        // Tabs
        HorizontalGroup tabGroup = new HorizontalGroup();
        tabGroup.align(Align.center);

        String tabInstalledText;
        if (serverDd != null && serverDd.updatesAvailable) {
            tabInstalledText = I18n.msg("gui.download.tab.installed.updates", serverDd.numUpdates);
        } else {
            tabInstalledText = I18n.msg("gui.download.tab.installed");
        }

        tabs = new Array<>();

        final OwnTextButton tabAvail = new OwnTextButton(I18n.msg("gui.download.tab.available"), skin, "toggle-big");
        tabAvail.pad(pad5);
        tabAvail.setWidth(tabWidth);
        final OwnTextButton tabInstalled = new OwnTextButton(tabInstalledText, skin, "toggle-big");
        tabInstalled.pad(pad5);
        tabInstalled.setWidth(tabWidth);

        tabs.add(tabAvail);
        tabs.add(tabInstalled);

        tabGroup.addActor(tabAvail);
        tabGroup.addActor(tabInstalled);

        content.add(tabGroup).center().expandX().row();

        // Create the tab content. Just using images here for simplicity.
        Stack tabContent = new Stack();

        // Content
        final Table contentAvail = new Table(skin);
        contentAvail.align(Align.top);
        contentAvail.pad(pad10);

        final Table contentInstalled = new Table(skin);
        contentInstalled.align(Align.top);
        contentInstalled.pad(pad10);

        /* ADD ALL CONTENT */
        tabContent.addActor(contentAvail);
        tabContent.addActor(contentInstalled);

        content.add(tabContent).expand().fill().padBottom(pad20).row();

        // Listen to changes in the tab button checked states
        // Set visibility of the tab content to match the checked state
        ChangeListener tabListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (tabAvail.isChecked()) {
                    if (initialized.get())
                        selectedTab = 0;
                    reloadAvailable(contentAvail, width);
                }
                if (tabInstalled.isChecked()) {
                    if (initialized.get())
                        selectedTab = 1;
                    reloadInstalled(contentInstalled, width);
                }
                contentAvail.setVisible(tabAvail.isChecked());
                contentInstalled.setVisible(tabInstalled.isChecked());

                content.pack();
                setKeyboardFocus();
            }
        };
        tabAvail.addListener(tabListener);
        tabInstalled.addListener(tabListener);

        // Let only one tab button be checked at a time
        ButtonGroup<Button> tabs = new ButtonGroup<>();
        tabs.setMinCheckCount(1);
        tabs.setMaxCheckCount(1);
        tabs.add(tabAvail);
        tabs.add(tabInstalled);

        // Check
        if (serverDd != null && serverDd.updatesAvailable)
            selectedTab = 1;
        tabs.setChecked(selectedTab == 0 ? tabAvail.getText().toString() : tabInstalled.getText().toString());

        // Data location
        if (dataLocation)
            addDataLocation(content);

        // Add manual download
        Link manualDownload = new Link(I18n.msg("gui.download.manual"), skin, "link", Settings.settings.program.url.dataMirror);
        content.add(manualDownload).center();

        addGamepadListener();
        initialized.set(true);
    }

    private void addDataLocation(Table content) {
        float buttonPad = 1.6f;
        String catLoc = Settings.settings.data.location;

        Table dataLocTable = new Table(skin);

        OwnLabel catalogsLocLabel = new OwnLabel(I18n.msg("gui.download.location"), skin);
        OwnImageButton catalogsLocTooltip = new OwnImageButton(skin, "tooltip");
        catalogsLocTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.download.location.info"), skin));
        HorizontalGroup catalogsLocGroup = new HorizontalGroup();
        catalogsLocGroup.space(pad10);
        catalogsLocGroup.addActor(catalogsLocLabel);
        catalogsLocGroup.addActor(catalogsLocTooltip);

        String dataLocationString = Path.of(catLoc).toString();
        OwnTextButton dataLocationButton = new OwnTextButton(TextUtils.capString(dataLocationString, 85), skin);
        dataLocationButton.addListener(new OwnTextTooltip(dataLocationString, skin));
        dataLocationButton.pad(buttonPad * 4f);
        dataLocationButton.setMinWidth(1000f);

        dataLocTable.add(catalogsLocGroup).left().padBottom(pad10);
        dataLocTable.add(dataLocationButton).left().padLeft(pad5).padBottom(pad10).row();
        Cell<Actor> notice = dataLocTable.add((Actor) null).colspan(2).padBottom(pad10);
        notice.row();

        content.add(dataLocTable).center().row();

        dataLocationButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                FileChooser fc = new FileChooser(I18n.msg("gui.download.pickloc"), skin, stage, Path.of(Settings.settings.data.location), FileChooser.FileChooserTarget.DIRECTORIES);
                fc.setShowHidden(Settings.settings.program.fileChooser.showHidden);
                fc.setShowHiddenConsumer((showHidden) -> Settings.settings.program.fileChooser.showHidden = showHidden);
                fc.setResultListener((success, result) -> {
                    if (success) {
                        if (Files.isReadable(result) && Files.isWritable(result)) {
                            // Set data location
                            dataLocationButton.setText(result.toAbsolutePath().toString());
                            // Change data location
                            Settings.settings.data.location = result.toAbsolutePath().toString().replaceAll("\\\\", "/");
                            // Create temp dir
                            SysUtils.mkdir(SysUtils.getTempDir(Settings.settings.data.location));
                            me.pack();
                            GaiaSky.postRunnable(() -> {
                                // Reset datasets
                                Settings.settings.data.dataFiles.clear();
                                reloadAll();
                                GaiaSky.instance.getGlobalResources().reloadDataFiles();
                            });
                        } else {
                            Label warn = new OwnLabel(I18n.msg("gui.download.pickloc.permissions"), skin);
                            warn.setColor(ColorUtils.gRedC);
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
    }

    private void reloadAvailable(Table content, float width) {
        reloadBothPanes(content, width, serverDd, currentMode = DatasetMode.AVAILABLE);
    }

    private void reloadInstalled(Table content, float width) {
        this.localDd = DataDescriptorUtils.instance().buildLocalDatasets(this.serverDd);
        reloadBothPanes(content, width, localDd, currentMode = DatasetMode.INSTALLED);
    }

    private enum DatasetMode {
        AVAILABLE,
        INSTALLED
    }

    private void reloadBothPanes(Table content, float width, DataDescriptor dataDescriptor, DatasetMode mode) {
        content.clear();
        if (dataDescriptor == null || dataDescriptor.datasets.isEmpty()) {
            if (mode == DatasetMode.AVAILABLE) {
                content.add(new OwnLabel(I18n.msg("gui.download.noconnection.title"), skin)).center().padTop(pad20 * 2f).padBottom(pad10).row();
            }
            content.add(new OwnLabel(I18n.msg("gui.dschooser.nodatasets"), skin)).center().padTop(mode == DatasetMode.AVAILABLE ? 0 : pad20 * 2f).row();
        } else {
            left = content.add().top().left().padRight(pad20);
            right = content.add().top().left();

            int datasets = reloadLeftPane(left, right, dataDescriptor, mode, width);
            if (datasets > 0) {
                reloadRightPane(right, selectedDataset[mode.ordinal()], mode);
            } else {
                content.clear();
                content.add(new OwnLabel(I18n.msg("gui.dschooser.nodatasets"), skin)).center().padTop(mode == DatasetMode.AVAILABLE ? 0 : pad20 * 2f).row();
            }

        }
        me.pack();
    }

    private int reloadLeftPane(Cell<?> left, Cell<?> right, DataDescriptor dataDescriptor, DatasetMode mode, float width) {
        Table leftTable = new Table(skin);
        leftTable.align(Align.topRight);
        leftScroll = new OwnScrollPane(leftTable, skin, "minimalist-nobg");
        leftScroll.setScrollingDisabled(true, false);
        leftScroll.setForceScroll(false, true);
        leftScroll.setSmoothScrolling(false);
        leftScroll.setFadeScrollBars(false);
        leftScroll.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.scrolled) {
                    // Save scroll position
                    scroll[mode.ordinal()][0] = leftScroll.getScrollX();
                    scroll[mode.ordinal()][1] = leftScroll.getScrollY();
                }
            }
            return false;
        });

        // Current selected datasets
        List<String> currentSetting = Settings.settings.data.dataFiles;

        selectionOrder.clear();
        selectedIndex = 0;

        int added = 0;
        for (DatasetType type : dataDescriptor.types) {
            List<DatasetDesc> datasets = type.datasets;
            List<DatasetDesc> filtered = datasets.stream().filter(d -> mode != DatasetMode.AVAILABLE || !d.exists).collect(Collectors.toList());
            if (!filtered.isEmpty()) {
                OwnLabel dsType = new OwnLabel(I18n.msg("gui.download.type." + type.typeStr), skin, "hud-header");
                leftTable.add(dsType).left().padTop(pad20 * 2f).padBottom(pad10).row();

                for (DatasetDesc dataset : filtered) {
                    Table t = new Table(skin);
                    t.pad(pad10, pad10, 0, pad10);

                    String tooltipText = dataset.key;

                    // Type icon
                    Image typeImage = new OwnImage(skin.getDrawable(getIcon(dataset.type)));
                    float scl = 0.7f;
                    float iw = typeImage.getWidth();
                    float ih = typeImage.getHeight();
                    typeImage.setSize(iw * scl, ih * scl);
                    typeImage.addListener(new OwnTextTooltip(dataset.type, skin, 10));

                    // Title
                    String titleString = dataset.name;
                    OwnLabel title = new OwnLabel(TextUtils.capString(titleString, 60), skin, "ui-23");
                    title.setWidth(width * 0.41f);
                    if (dataset.outdated) {
                        title.setColor(highlight);
                        tooltipText = I18n.msg("gui.download.version.new", Integer.toString(dataset.serverVersion), Integer.toString(dataset.myVersion));
                    }

                    // Install, update or enable/disable
                    Actor installOrSelect;
                    float installOrSelectSize = 40f;
                    if (mode == DatasetMode.AVAILABLE || dataset.outdated) {
                        OwnTextIconButton install = new OwnTextIconButton("", skin, "install");
                        install.setContentAlign(Align.center);
                        install.addListener(new OwnTextTooltip(I18n.msg(dataset.outdated ? "gui.download.update" : "gui.download.install"), skin));
                        install.addListener((event) -> {
                            if (event instanceof ChangeEvent) {
                                if (dataset.outdated) {
                                    actionUpdateDataset(dataset);
                                } else {
                                    actionDownloadDataset(dataset);
                                }
                            }
                            return false;
                        });
                        install.setSize(installOrSelectSize, installOrSelectSize);
                        if (currentDownloads.containsKey(dataset.key)) {
                            install.setDisabled(true);
                        }
                        installOrSelect = install;
                    } else {
                        OwnCheckBox select = new OwnCheckBox("", skin, 0f);
                        installOrSelect = select;
                        if (dataset.baseData || dataset.type.equals("texture-pack")) {
                            select.setChecked(true);
                            select.setDisabled(true);
                        } else if (dataset.minGsVersion > GaiaSkyDesktop.SOURCE_VERSION) {
                            select.setChecked(false);
                            select.setDisabled(true);
                            title.setColor(ColorUtils.gRedC);
                            tooltipText = I18n.msg("gui.download.version.gs.mismatch", Integer.toString(GaiaSkyDesktop.SOURCE_VERSION), Integer.toString(dataset.minGsVersion));
                            select.getStyle().disabledFontColor = ColorUtils.gRedC;

                            // Remove from selected, if it is
                            String filePath = dataset.catalogFile.path();
                            if (Settings.settings.data.dataFiles.contains(filePath)) {
                                Settings.settings.data.dataFiles.remove(filePath);
                                logger.info(I18n.msg("gui.download.disabled.version", dataset.name, Integer.toString(dataset.minGsVersion), Integer.toString(GaiaSkyDesktop.SOURCE_VERSION)));
                            }
                        } else {
                            select.setChecked(isPathIn(Settings.settings.data.dataFile(dataset.checkStr), currentSetting));
                            select.addListener(new OwnTextTooltip(dataset.checkPath.toString(), skin));
                        }
                        select.setSize(installOrSelectSize, installOrSelectSize);
                        select.setHeight(40f);
                        select.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                if (select.isChecked()) {
                                    actionEnableDataset(dataset);
                                } else {
                                    actionDisableDataset(dataset);
                                }
                                GaiaSky.postRunnable(() -> reloadRightPane(right, dataset, mode));
                            }
                        });
                    }

                    // Size
                    OwnLabel size = new OwnLabel(dataset.size, skin, "grey-large");
                    size.addListener(new OwnTextTooltip(I18n.msg("gui.download.size.tooltip"), skin, 10));
                    size.setWidth(88f);

                    // Version
                    OwnLabel version = null;
                    if (mode == DatasetMode.AVAILABLE) {
                        version = new OwnLabel(I18n.msg("gui.download.version.server", dataset.serverVersion), skin, "grey-large");
                    } else if (mode == DatasetMode.INSTALLED) {
                        if (dataset.outdated) {
                            version = new OwnLabel(I18n.msg("gui.download.version.new", Integer.toString(dataset.serverVersion), Integer.toString(dataset.myVersion)), skin, "grey-large");
                            version.setColor(highlight);
                        } else {
                            version = new OwnLabel(I18n.msg("gui.download.version.local", dataset.myVersion), skin, "grey-large");
                        }
                    }
                    HorizontalGroup versionSize = new HorizontalGroup();
                    versionSize.space(pad20 * 2f);
                    versionSize.addActor(version);
                    versionSize.addActor(size);

                    // Progress
                    OwnProgressBar progress = new OwnProgressBar(0f, 100f, 0.1f, false, skin, "tiny-horizontal");
                    progress.setPrefWidth(850f);
                    progress.setValue(60f);
                    progress.setVisible(false);

                    t.add(typeImage).left().padRight(pad10);
                    t.add(title).left().padRight(pad10);
                    t.add(installOrSelect).right().row();
                    t.add();
                    t.add(versionSize).colspan(2).left().padRight(pad10).padBottom(pad5).row();
                    t.add(progress).colspan(3).expandX();
                    t.pack();
                    OwnButton button = new OwnButton(t, skin, "dataset", false);
                    button.setWidth(width * 0.52f);
                    title.addListener(new OwnTextTooltip(tooltipText, skin, 10));
                    buttonMap[mode.ordinal()].put(dataset.key, button);

                    // Clicks
                    button.addListener(new InputListener() {
                        @Override
                        public boolean handle(com.badlogic.gdx.scenes.scene2d.Event event) {
                            if (event instanceof InputEvent) {
                                InputEvent ie = (InputEvent) event;
                                InputEvent.Type type = ie.getType();
                                if (type == InputEvent.Type.touchDown) {
                                    if (ie.getButton() == Input.Buttons.LEFT) {
                                        GaiaSky.postRunnable(() -> reloadRightPane(right, dataset, mode));
                                        selectedDataset[mode.ordinal()] = dataset;
                                    } else if (ie.getButton() == Input.Buttons.RIGHT) {
                                        GaiaSky.postRunnable(() -> {
                                            // Context menu
                                            ContextMenu datasetContext = new ContextMenu(skin, "default");
                                            if (mode == DatasetMode.INSTALLED) {
                                                if (dataset.outdated) {
                                                    // Update
                                                    MenuItem update = new MenuItem(I18n.msg("gui.download.update"), skin, skin.getDrawable("iconic-arrow-circle-bottom"));
                                                    update.addListener(new ChangeListener() {
                                                        @Override
                                                        public void changed(ChangeEvent event, Actor actor) {
                                                            actionUpdateDataset(dataset);
                                                        }
                                                    });
                                                    datasetContext.addItem(update);
                                                }
                                                if (!dataset.baseData && !dataset.type.equals("texture-pack") && dataset.minGsVersion <= GaiaSkyDesktop.SOURCE_VERSION) {
                                                    boolean enabled = isPathIn(dataset.catalogFile.path(), currentSetting);
                                                    if (enabled) {
                                                        // Disable
                                                        MenuItem disable = new MenuItem(I18n.msg("gui.download.disable"), skin, skin.getDrawable("check-off-disabled"));
                                                        disable.addListener(new ChangeListener() {
                                                            @Override
                                                            public void changed(ChangeEvent event, Actor actor) {
                                                                actionDisableDataset(dataset);
                                                                if (installOrSelect instanceof OwnCheckBox) {
                                                                    OwnCheckBox cb = (OwnCheckBox) installOrSelect;
                                                                    cb.setProgrammaticChangeEvents(false);
                                                                    cb.setChecked(false);
                                                                    cb.setProgrammaticChangeEvents(true);
                                                                }
                                                                GaiaSky.postRunnable(() -> reloadRightPane(right, dataset, mode));
                                                            }
                                                        });
                                                        datasetContext.addItem(disable);
                                                    } else {
                                                        // Enable
                                                        MenuItem enable = new MenuItem(I18n.msg("gui.download.enable"), skin, skin.getDrawable("check-on"));
                                                        enable.addListener(new ChangeListener() {
                                                            @Override
                                                            public void changed(ChangeEvent event, Actor actor) {
                                                                actionEnableDataset(dataset);
                                                                if (installOrSelect instanceof OwnCheckBox) {
                                                                    OwnCheckBox cb = (OwnCheckBox) installOrSelect;
                                                                    cb.setProgrammaticChangeEvents(false);
                                                                    cb.setChecked(true);
                                                                    cb.setProgrammaticChangeEvents(true);
                                                                }
                                                                GaiaSky.postRunnable(() -> reloadRightPane(right, dataset, mode));
                                                            }
                                                        });
                                                        datasetContext.addItem(enable);
                                                    }
                                                    datasetContext.addSeparator();
                                                }
                                                // Delete
                                                MenuItem delete = new MenuItem(I18n.msg("gui.download.delete"), skin, skin.getDrawable("iconic-trash"));
                                                delete.addListener(new ClickListener() {
                                                    @Override
                                                    public void clicked(InputEvent event, float x, float y) {
                                                        actionDeleteDataset(dataset);
                                                        super.clicked(event, x, y);
                                                    }
                                                });
                                                datasetContext.addItem(delete);
                                            } else if (mode == DatasetMode.AVAILABLE) {
                                                // Install
                                                MenuItem install = new MenuItem(I18n.msg("gui.download.install"), skin, skin.getDrawable("iconic-cloud-download"));
                                                install.addListener(new ChangeListener() {
                                                    @Override
                                                    public void changed(ChangeEvent event, Actor actor) {
                                                        actionDownloadDataset(dataset);
                                                    }
                                                });
                                                datasetContext.addItem(install);
                                            }
                                            datasetContext.showMenu(stage, Gdx.input.getX(ie.getPointer()) / Settings.settings.program.ui.scale, stage.getHeight() - Gdx.input.getY(ie.getPointer()) / Settings.settings.program.ui.scale);
                                        });
                                    }
                                }
                            }
                            return super.handle(event);
                        }
                    });

                    leftTable.add(button).left().row();

                    // Add check.
                    selectionOrder.add(new Pair<>(dataset, installOrSelect));

                    if (selectedDataset[mode.ordinal()] == null) {
                        selectedDataset[mode.ordinal()] = dataset;
                        selectedIndex = selectionOrder.size() - 1;
                    }

                    // Create watcher
                    watchers.add(new DatasetWatcher(dataset, progress, installOrSelect instanceof OwnTextIconButton ? (OwnTextIconButton) installOrSelect : null, null, null));
                    added++;
                }
            }
        }
        leftScroll.setWidth(width * 0.52f);
        leftScroll.setHeight(Math.min(stage.getHeight() * 0.5f, 1500f));
        leftScroll.layout();
        leftScroll.setScrollX(scroll[mode.ordinal()][0]);
        leftScroll.setScrollY(scroll[mode.ordinal()][1]);
        left.setActor(leftScroll);

        return added;
    }

    private void reloadRightPane(Cell<?> cell, DatasetDesc dataset, DatasetMode mode) {
        if (rightPaneWatcher != null) {
            rightPaneWatcher.dispose();
            watchers.remove(rightPaneWatcher);
            rightPaneWatcher = null;
        }
        cell.clearActor();
        Table t = new Table(skin);

        if (dataset == null) {
            OwnLabel l = new OwnLabel(I18n.msg("gui.download.noselected"), skin);
            t.add(l).center().padTop(pad20 * 3);
        } else {
            // Type icon
            String dType = dataset.type != null ? dataset.type : "other";
            Image typeImage = new OwnImage(skin.getDrawable(getIcon(dType)));
            float scl = 0.7f;
            float iw = typeImage.getWidth();
            float ih = typeImage.getHeight();
            typeImage.setSize(iw * scl, ih * scl);
            typeImage.addListener(new OwnTextTooltip(dType, skin, 10));

            // Title
            String titleString = dataset.name;
            OwnLabel title = new OwnLabel(TextUtils.breakCharacters(titleString, 45), skin, "hud-header");
            title.addListener(new OwnTextTooltip(dataset.description, skin, 10));

            // Title group
            HorizontalGroup titleGroup = new HorizontalGroup();
            titleGroup.space(pad10);
            titleGroup.addActor(typeImage);
            titleGroup.addActor(title);

            // Status
            OwnLabel status = null;
            if (mode == DatasetMode.AVAILABLE) {
                status = new OwnLabel(I18n.msg("gui.download.available"), skin, "mono");
            } else if (mode == DatasetMode.INSTALLED) {
                if (dataset.baseData || dType.equals("texture-pack")) {
                    // Always enabled
                    status = new OwnLabel(I18n.msg("gui.download.enabled"), skin, "mono");
                } else if (dataset.minGsVersion > GaiaSkyDesktop.SOURCE_VERSION) {
                    // Notify version mismatch
                    status = new OwnLabel(I18n.msg("gui.download.version.gs.mismatch.short", Integer.toString(GaiaSkyDesktop.SOURCE_VERSION), Integer.toString(dataset.minGsVersion)), skin, "mono");
                    status.setColor(ColorUtils.gRedC);
                } else {
                    // Notify status
                    List<String> currentSetting = Settings.settings.data.dataFiles;
                    boolean enabled = isPathIn(dataset.catalogFile.path(), currentSetting);
                    status = new OwnLabel(I18n.msg(enabled ? "gui.download.enabled" : "gui.download.disabled"), skin, "mono");
                }
            }

            // Type
            String typeString;
            if (I18n.hasMessage("gui.download.type." + dType)) {
                typeString = I18n.msg("gui.download.type." + dType);
            } else {
                typeString = dType;
            }
            OwnLabel type = new OwnLabel(I18n.msg("gui.download.type", typeString), skin, "grey-large");
            type.addListener(new OwnTextTooltip(dType, skin, 10));

            // Version
            OwnLabel version = null;
            if (mode == DatasetMode.AVAILABLE) {
                version = new OwnLabel(I18n.msg("gui.download.version.server", dataset.serverVersion), skin, "grey-large");
            } else if (mode == DatasetMode.INSTALLED) {
                if (dataset.outdated) {
                    version = new OwnLabel(I18n.msg("gui.download.version.new", Integer.toString(dataset.serverVersion), Integer.toString(dataset.myVersion)), skin, "grey-large");
                    version.setColor(highlight);
                } else {
                    version = new OwnLabel(I18n.msg("gui.download.version.local", dataset.myVersion), skin, "grey-large");
                }
            }

            // Key
            OwnLabel key = new OwnLabel(I18n.msg("gui.download.name", dataset.key), skin, "grey-large");

            // Size
            OwnLabel size = new OwnLabel(I18n.msg("gui.download.size", dataset.size), skin, "grey-large");
            size.addListener(new OwnTextTooltip(I18n.msg("gui.download.size.tooltip"), skin, 10));

            // Num objects
            String nObjStr = dataset.nObjects > 0 ? I18n.msg("gui.dataset.nobjects", (int) dataset.nObjects) : I18n.msg("gui.dataset.nobjects.none");
            OwnLabel nObjects = new OwnLabel(nObjStr, skin, "grey-large");
            nObjects.addListener(new OwnTextTooltip(I18n.msg("gui.download.nobjects.tooltip") + ": " + dataset.nObjectsStr, skin, 10));

            // Link
            Link link = null;
            if (dataset.link != null && !dataset.link.isBlank()) {
                String linkStr = dataset.link.replace("@mirror-url@", Settings.settings.program.url.dataMirror);
                link = new Link(TextUtils.breakCharacters(linkStr, 100, true), skin, dataset.link);
            }

            Table infoTable = new Table(skin);
            infoTable.align(Align.topLeft);

            // Description
            String descriptionString = dataset.description;
            OwnLabel desc = new OwnLabel(TextUtils.breakCharacters(descriptionString, 80), skin);
            desc.setWidth(1000f);

            // Release notes
            String releaseNotesString = dataset.releaseNotes;
            if (releaseNotesString == null || releaseNotesString.isBlank()) {
                releaseNotesString = "-";
            }
            releaseNotesString = TextUtils.breakCharacters(releaseNotesString, 80);
            OwnLabel releaseNotes = new OwnLabel(I18n.msg("gui.download.releasenotes", releaseNotesString), skin);
            releaseNotes.setWidth(1000f);

            // Files
            String filesString;
            if (dataset.files == null || dataset.files.length == 0) {
                filesString = "-";
            } else {
                filesString = TextUtils.arrayToStr(dataset.files, "", "", "\n");
            }
            OwnLabel files = new OwnLabel(I18n.msg("gui.download.files", filesString), skin, "grey-large");

            infoTable.add(desc).top().left().padBottom(pad20).row();
            infoTable.add(releaseNotes).top().left().padBottom(pad20).row();
            infoTable.add(files).top().left();

            // Scroll
            OwnScrollPane infoScroll = new OwnScrollPane(infoTable, skin, "minimalist-nobg");
            infoScroll.setScrollingDisabled(true, false);
            infoScroll.setExpand(true);
            infoScroll.setSmoothScrolling(false);
            infoScroll.setFadeScrollBars(false);
            infoScroll.setWidth(1050f);

            OwnTextIconButton cancelDownloadButton = null;
            if (currentDownloads.containsKey(dataset.key)) {
                Pair<DatasetDesc, Net.HttpRequest> pair = currentDownloads.get(dataset.key);
                HttpRequest request = pair.getSecond();
                cancelDownloadButton = new OwnTextIconButton(I18n.msg("gui.download.cancel"), skin, "quit");
                cancelDownloadButton.pad(14.4f);
                cancelDownloadButton.getLabel().setColor(1, 0, 0, 1);
                cancelDownloadButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (request != null) {
                            GaiaSky.postRunnable(() -> Gdx.net.cancelHttpRequest(request));
                        }
                    }
                });
            }

            t.add(titleGroup).top().left().padBottom(pad5).padTop(pad20).row();
            t.add(status).top().left().padLeft(pad10 * 3f).padBottom(pad20).row();
            t.add(type).top().left().padBottom(pad5).row();
            t.add(version).top().left().padBottom(pad5).row();
            t.add(key).top().left().padBottom(pad5).row();
            t.add(size).top().left().padBottom(pad5).row();
            t.add(nObjects).top().left().padBottom(pad10).row();
            t.add(link).top().left().padBottom(pad20 * 2f).row();
            t.add(infoScroll).top().left().padBottom(pad20).row();
            t.add(cancelDownloadButton).padTop(pad20).center();

            // Scroll
            OwnScrollPane scrollPane = new OwnScrollPane(t, skin, "minimalist-nobg");
            scrollPane.setScrollingDisabled(true, false);
            scrollPane.setSmoothScrolling(false);
            scrollPane.setFadeScrollBars(false);
            scrollPane.setWidth(1050f);
            scrollPane.setHeight(Math.min(stage.getHeight() * 0.5f, 1500f));

            cell.setActor(t);
            cell.top().left();
            cell.getTable().pack();

            // Create watcher
            rightPaneWatcher = new DatasetWatcher(dataset, null, null, status, t);
            watchers.add(rightPaneWatcher);
        }
    }

    private boolean isPathIn(String path, List<String> setting) {
        for (String candidate : setting) {
            var candidatePath = Settings.settings.data.dataPath(candidate);
            try {
                if (Path.of(path).toRealPath().equals(candidatePath.toRealPath())) {
                    return true;
                }
            } catch (IOException e) {
                logger.error(e);
            }
        }
        return false;
    }

    public void refresh() {
        content.clear();
        build();
    }

    private void downloadDataset(DatasetDesc dataset) {
        downloadDataset(dataset, null);
    }

    private void downloadDataset(DatasetDesc dataset, Runnable successRunnable) {

        String name = dataset.name;
        String url = dataset.file.replace("@mirror-url@", Settings.settings.program.url.dataMirror);

        String filename = FilenameUtils.getName(url);
        FileHandle tempDownload = Gdx.files.absolute(SysUtils.getTempDir(Settings.settings.data.location) + "/" + filename + ".part");

        ProgressRunnable progressDownload = (read, total, progress, speed) -> {
            try {
                double readMb = (double) read / 1e6d;
                double totalMb = (double) total / 1e6d;
                final String progressString = progress >= 100 ? I18n.msg("gui.done") : I18n.msg("gui.download.downloading", nf.format(progress));
                double mbPerSecond = speed / 1000d;
                final String speedString = nf.format(readMb) + "/" + nf.format(totalMb) + " MB (" + nf.format(mbPerSecond) + " MB/s)";
                // Since we are downloading on a background thread, post a runnable to touch UI
                GaiaSky.postRunnable(() -> {
                    EventManager.publish(Event.DATASET_DOWNLOAD_PROGRESS_INFO, this, dataset.key, (float) progress, progressString, speedString);
                });
            } catch (Exception e) {
                logger.warn(I18n.msg("gui.download.error.progress"));
            }
        };
        ProgressRunnable progressHashResume = (read, total, progress, speed) -> {
            double readMb = (double) read / 1e6d;
            double totalMb = (double) total / 1e6d;
            final String progressString = progress >= 100 ? I18n.msg("gui.done") : I18n.msg("gui.download.checksumming", nf.format(progress));
            double mbPerSecond = speed / 1000d;
            final String speedString = nf.format(readMb) + "/" + nf.format(totalMb) + " MB (" + nf.format(mbPerSecond) + " MB/s)";
            // Since we are downloading on a background thread, post a runnable to touch UI
            GaiaSky.postRunnable(() -> {
                EventManager.publish(Event.DATASET_DOWNLOAD_PROGRESS_INFO, this, dataset.key, (float) progress, progressString, speedString);
            });
        };

        ChecksumRunnable finish = (digest) -> {
            String errorMsg = null;
            // Unpack
            int errors = 0;
            logger.info("Extracting: " + tempDownload.path());
            String dataLocation = Settings.settings.data.location + File.separatorChar;
            // Checksum
            if (digest != null && dataset.sha256 != null) {
                String serverDigest = dataset.sha256;
                try {
                    boolean ok = serverDigest.equals(digest);
                    if (ok) {
                        logger.info("SHA256 ok: " + name);
                    } else {
                        logger.error("SHA256 check failed: " + name);
                        errorMsg = "(SHA256 check failed)";
                        errors++;
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, "Error checking SHA256: " + name, 10f);
                    }
                } catch (Exception e) {
                    logger.info("Error checking SHA256: " + name);
                    errorMsg = "(SHA256 check failed)";
                    errors++;
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, "Error checking SHA256: " + name, 10f);
                }
            } else {
                logger.info("No digest found for dataset: " + name);
                EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, "No digest found for dataset: " + name, 10f);
            }

            if (errors == 0) {
                try {
                    // Extract
                    decompress(tempDownload.path(), new File(dataLocation), dataset);
                    // Remove archive
                    cleanupTempFile(tempDownload.path());
                } catch (Exception e) {
                    logger.error(e, "Error decompressing: " + name);
                    errorMsg = "(decompressing error)";
                    errors++;
                }
            }

            final String errorMessage = errorMsg;
            final int numErrors = errors;
            // Done
            GaiaSky.postRunnable(() -> {
                currentDownloads.remove(dataset.key);

                if (numErrors == 0) {
                    // Ok message
                    EventManager.publish(Event.DATASET_DOWNLOAD_FINISH_INFO, this, dataset.key, 0);
                    dataset.exists = true;
                    actionEnableDataset(dataset);
                    if (successRunnable != null) {
                        successRunnable.run();
                    }
                    resetSelectedDataset();
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.finished", name), 10f);
                    Timer.schedule(new Timer.Task() {
                        @Override
                        public void run() {
                            reloadAll();
                        }
                    }, 0.5f);
                } else {
                    logger.error(I18n.msg("gui.download.failed", name + " - " + url));
                    tempDownload.delete();
                    setStatusError(dataset, errorMessage);
                    currentDownloads.remove(dataset.key);
                    resetSelectedDataset();
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.failed", name), 10f);
                    Timer.schedule(new Timer.Task() {
                        @Override
                        public void run() {
                            reloadAll();
                        }
                    }, 1.5f);
                }
            });

        };

        Runnable fail = () -> {
            logger.error(I18n.msg("gui.download.failed", name + " - " + url));
            tempDownload.delete();
            setStatusError(dataset);
            currentDownloads.remove(dataset.key);
            resetSelectedDataset();
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.failed", name), 10f);
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    reloadAll();
                }
            }, 1.5f);
        };

        Runnable cancel = () -> {
            logger.error(I18n.msg("gui.download.cancelled", name + " - " + url));
            setStatusCancelled(dataset);
            currentDownloads.remove(dataset.key);
            resetSelectedDataset();
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.cancelled", name), 10f);
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    reloadAll();
                }
            }, 0.5f);
        };

        // Download
        final Net.HttpRequest request = DownloadHelper.downloadFile(url, tempDownload, Settings.settings.program.offlineMode, progressDownload, progressHashResume, finish, fail, cancel);
        GaiaSky.postRunnable(() -> EventManager.publish(Event.DATASET_DOWNLOAD_START_INFO, this, dataset.key, request));
        currentDownloads.put(dataset.key, new Pair<>(dataset, request));

    }

    private void decompress(String in, File out, DatasetDesc dataset) throws Exception {
        FileInfoInputStream fIs = new FileInfoInputStream(in);
        GzipCompressorInputStream gzIs = new GzipCompressorInputStream(fIs);
        TarArchiveInputStream tarIs = new TarArchiveInputStream(gzIs);
        double sizeKb = fileSize(in) / 1000d;
        String sizeKbStr = nf.format(sizeKb);
        TarArchiveEntry entry;
        long last = 0;
        while ((entry = tarIs.getNextTarEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }
            File curFile = new File(out, entry.getName());
            File parent = curFile.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    logger.info("Parent directory not created, already exists: " + parent.toPath());
                }
            }

            IOUtils.copy(tarIs, new FileOutputStream(curFile));

            // Every 250 ms we update the view
            long current = System.currentTimeMillis();
            long elapsed = current - last;
            if (elapsed > 250) {
                GaiaSky.postRunnable(() -> {
                    float val = (float) ((fIs.getBytesRead() / 1000d) / sizeKb) * 100f;
                    String progressString = I18n.msg("gui.download.extracting", nf.format(fIs.getBytesRead() / 1000d) + "/" + sizeKbStr + " Kb");
                    EventManager.publish(Event.DATASET_DOWNLOAD_PROGRESS_INFO, this, dataset.key, val, progressString, null);
                });
                last = current;
            }

        }
    }

    /**
     * Returns the file size
     *
     * @param inputFilePath A file
     *
     * @return The size in bytes
     */
    private long fileSize(String inputFilePath) {
        return new File(inputFilePath).length();
    }

    /**
     * Returns the GZ uncompressed size
     *
     * @param inputFilePath A gzipped file
     *
     * @return The uncompressed size in bytes
     *
     * @throws IOException If the file failed to read
     */
    private long fileSizeGZUncompressed(String inputFilePath) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(inputFilePath, "r");
        raf.seek(raf.length() - 4);
        byte[] bytes = new byte[4];
        raf.read(bytes);
        long fileSize = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).getLong();
        if (fileSize < 0)
            fileSize += (1L << 32);
        raf.close();
        return fileSize;
    }

    /**
     * We should never need to call this, as the main {@link GaiaSky#dispose()} method
     * already cleans up the temp directory.
     * This way, we allow download resumes within the same session.
     */
    private void cleanupTempFiles() {
        cleanupTempFiles(true, false);
    }

    private void cleanupTempFile(String file) {
        deleteFile(Path.of(file));
    }

    private void cleanupTempFiles(final boolean dataDownloads, final boolean dataDescriptor) {
        if (dataDownloads) {
            final Path tempDir = SysUtils.getTempDir(Settings.settings.data.location);
            // Clean up partial downloads
            try (final Stream<Path> stream = Files.find(tempDir, 2, (path, basicFileAttributes) -> {
                final File file = path.toFile();
                return !file.isDirectory() && file.getName().endsWith("tar.gz.part");
            })) {
                stream.forEach(this::deleteFile);
            } catch (IOException e) {
                logger.error(e);
            }
        }

        if (dataDescriptor) {
            // Clean up data descriptor
            Path gsDownload = SysUtils.getTempDir(Settings.settings.data.location).resolve("gaiasky-data.json");
            deleteFile(gsDownload);
        }
    }

    private void deleteFile(Path p) {
        if (java.nio.file.Files.exists(p)) {
            try {
                java.nio.file.Files.delete(p);
            } catch (IOException e) {
                logger.error(e, "Failed cleaning up file: " + p.toString());
            }
        }
    }

    private void setStatusError(DatasetDesc ds) {
        setStatusError(ds, null);
    }

    private void setStatusError(DatasetDesc ds, String message) {
        if (message != null && !message.isEmpty()) {
            EventManager.publish(Event.DATASET_DOWNLOAD_FINISH_INFO, this, ds.key, 1, message);
        } else {
            EventManager.publish(Event.DATASET_DOWNLOAD_FINISH_INFO, this, ds.key, 1);
        }
    }

    private void setStatusCancelled(DatasetDesc ds) {
        EventManager.publish(Event.DATASET_DOWNLOAD_FINISH_INFO, this, ds.key, 2);
    }

    @Override
    protected boolean accept() {
        final GenericDialog myself = me;
        // Create a copy
        Map<String, Pair<DatasetDesc, HttpRequest>> copy = new HashMap<>(currentDownloads);

        if (!copy.isEmpty()) {
            GenericDialog question = new GenericDialog(I18n.msg("gui.download.close.title"), skin, stage) {

                @Override
                protected void build() {
                    content.clear();
                    content.add(new OwnLabel(I18n.msg("gui.download.close", currentDownloads.size()), skin)).left().padBottom(pad20).row();
                    content.add(new OwnLabel(I18n.msg("gui.download.close.current"), skin)).left().padBottom(pad10).row();
                    for (String key : copy.keySet()) {
                        DatasetDesc dd = copy.get(key).getFirst();
                        content.add(new OwnLabel(dd.name, skin, "warp")).center().padBottom(pad5).row();
                    }
                }

                @Override
                protected boolean accept() {
                    // Cancel all requests
                    Set<String> keys = copy.keySet();
                    for (String key : keys) {
                        Net.HttpRequest request = copy.get(key).getSecond();
                        Gdx.net.cancelHttpRequest(request);
                    }
                    if (this.acceptRunnable != null) {
                        this.acceptRunnable.run();
                    }
                    return true;
                }

                @Override
                protected void cancel() {
                    // Nothing
                }

                @Override
                public void dispose() {
                    // Nothing
                }
            };
            question.setAcceptText(I18n.msg("gui.yes"));
            question.setCancelText(I18n.msg("gui.no"));
            question.buildSuper();
            question.show(stage);
        } else {
            if (this.acceptRunnable != null) {
                this.acceptRunnable.run();
            }
            myself.hide(); // Close.
        }
        removeGamepadListener();
        // Do not close dialog, we close it.
        return false;
    }

    @Override
    protected void cancel() {
        removeGamepadListener();
    }

    @Override
    public void dispose() {
        removeGamepadListener();

    }

    private void backupScrollValues() {
        if (leftScroll != null) {
            this.scroll[0][0] = leftScroll.getScrollX();
            this.scroll[0][1] = leftScroll.getScrollY();
        }
    }

    private void restoreScrollValues() {
        if (leftScroll != null) {
            GaiaSky.postRunnable(() -> {
                leftScroll.setScrollX(scroll[0][0]);
                leftScroll.setScrollY(scroll[0][1]);
            });
        }
    }

    @Override
    public void setKeyboardFocus() {
        if (stage != null && selectedDataset[selectedTab] != null) {
            Button button = buttonMap[selectedTab].get(selectedDataset[selectedTab].key);
            if (button != null) {
                stage.setKeyboardFocus(button);
            }
        }
    }

    private void cleanWatchers() {
        for (DatasetWatcher watcher : watchers) {
            watcher.dispose();
        }
        watchers.clear();
        rightPaneWatcher = null;
    }

    /**
     * Drops the current view and regenerates all window content
     */
    private void reloadAll() {
        cleanWatchers();

        serverDd = DataDescriptorUtils.instance().buildServerDatasets(null);
        backupScrollValues();
        leftScroll = null;
        content.clear();
        build();
        pack();
        restoreScrollValues();
    }

    private void actionDownloadDataset(DatasetDesc dataset) {
        downloadDataset(dataset);
    }

    private void actionUpdateDataset(DatasetDesc dataset) {
        downloadDataset(dataset, () -> {
            // Success!
            dataset.outdated = false;
            if (dataset.server != null) {
                dataset.server.outdated = false;
                dataset.server.myVersion = dataset.server.serverVersion;
                dataset.myVersion = dataset.server.serverVersion;
                if (serverDd != null) {
                    serverDd.numUpdates = Math.max(serverDd.numUpdates - 1, 0);
                    serverDd.updatesAvailable = serverDd.numUpdates > 0;
                }
            } else {
                dataset.myVersion = dataset.serverVersion;
            }
        });
    }

    private void actionEnableDataset(DatasetDesc dataset) {
        // Texture packs can't be enabled
        if (dataset.type.equals("texture-pack"))
            return;
        String filePath = null;
        if (dataset.checkStr != null) {
            filePath = TextUtils.ensureStartsWith(dataset.checkStr, Constants.DATA_LOCATION_TOKEN);
        }
        if (filePath != null && !filePath.isBlank()) {
            if (!Settings.settings.data.dataFiles.contains(filePath)) {
                Settings.settings.data.dataFiles.add(filePath);
            }
        }
    }

    private void actionDisableDataset(DatasetDesc dataset) {
        // Base data can't be disabled
        if (!dataset.baseData) {
            String filePath = null;
            if (dataset.checkStr != null) {
                filePath = TextUtils.ensureStartsWith(dataset.checkStr, Constants.DATA_LOCATION_TOKEN);
            }
            if (filePath != null && !filePath.isBlank()) {
                Settings.settings.data.dataFiles.remove(filePath);
            }
        }
    }

    private void actionDeleteDataset(DatasetDesc dataset) {
        GenericDialog question = new GenericDialog(I18n.msg("gui.download.delete.title"), skin, stage) {

            @Override
            protected void build() {
                content.clear();
                String title = dataset.name;
                content.add(new OwnLabel(I18n.msg("gui.download.delete.text"), skin)).left().padBottom(pad10 * 2f).row();
                content.add(new OwnLabel(title, skin, "warp")).center().padBottom(pad10 * 2f).row();
            }

            @Override
            protected boolean accept() {
                DatasetDesc serverDataset = serverDd != null ? serverDd.findDatasetByKey(dataset.key) : null;
                boolean deleted = false;
                // Delete
                if (dataset.files != null) {
                    for (String fileToDelete : dataset.files) {
                        try {
                            if (fileToDelete.endsWith("/")) {
                                fileToDelete = fileToDelete.substring(0, fileToDelete.length() - 1);
                            }
                            // Separate parent from file.
                            String baseParent = "";
                            String baseName = fileToDelete;
                            if (fileToDelete.contains("/")) {
                                baseParent = fileToDelete.substring(0, fileToDelete.lastIndexOf('/'));
                                baseName = fileToDelete.substring(fileToDelete.lastIndexOf('/') + 1);
                            }
                            // Add data location if necessary.
                            Path dataPath;
                            Path basePath = Path.of(baseParent);
                            if (!basePath.isAbsolute()) {
                                dataPath = Paths.get(Settings.settings.data.location).resolve(baseParent);
                            } else {
                                dataPath = basePath;
                            }
                            File directory = dataPath.toRealPath().toFile();
                            // Expand possible wildcards.
                            Collection<File> files = FileUtils.listFilesAndDirs(directory, new WildcardFileFilter(baseName), new WildcardFileFilter(baseName));
                            for (File file : files) {
                                if (!file.equals(directory) && file.exists()) {
                                    FileUtils.forceDelete(file);
                                }
                            }
                            deleted = true;
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }
                } else if (dataset.checkPath != null) {
                    // Only remove "check"
                    try {
                        FileUtils.forceDelete(dataset.checkPath.toFile());
                        deleted = true;
                    } catch (IOException e) {
                        logger.error(e);
                    }
                }
                // Update server dataset status and selected
                if (deleted && serverDataset != null) {
                    serverDataset.exists = false;
                    actionDisableDataset(dataset);
                    resetSelectedDataset();

                }
                // RELOAD DATASETS VIEW
                GaiaSky.postRunnable(() -> reloadAll());
                return true;
            }

            @Override
            protected void cancel() {
                // Nothing
            }

            @Override
            public void dispose() {
                // Nothing
            }
        };
        question.setAcceptText(I18n.msg("gui.yes"));
        question.setCancelText(I18n.msg("gui.no"));
        question.buildSuper();
        question.show(stage);
    }

    private void resetSelectedDataset() {
        selectedDataset[0] = null;
        selectedDataset[1] = null;
        scroll[0][0] = 0f;
        scroll[0][1] = 0f;
        scroll[1][0] = 0f;
        scroll[1][1] = 0f;
    }

    public void fire() {
        Actor target = stage.getKeyboardFocus();
        if (target == null) {
            if (selectionOrder != null) {
                target = selectionOrder.get(selectedIndex).getSecond();
            }
        }

        if (target != null) {
            if (target instanceof CheckBox) {
                // Check or uncheck
                CheckBox cb = (CheckBox) target;
                if(!cb.isDisabled()) {
                    cb.setChecked(!cb.isChecked());
                }
            } else {
                // Fire change event
                ChangeEvent event = Pools.obtain(ChangeEvent.class);
                event.setTarget(target);
                target.fire(event);
                Pools.free(event);
            }
        }
    }

    public void cycleDialogButtons() {
        Actor target = stage.getKeyboardFocus();
        if (target == acceptButton && cancelButton != null) {
            stage.setKeyboardFocus(cancelButton);
        } else {
            stage.setKeyboardFocus(acceptButton);
        }
    }

    private void up() {
        selectedIndex = selectedIndex - 1;
        if (selectedIndex < 0) {
            selectedIndex = selectionOrder.size() - 1;
        }
        updateSelection();
    }

    private void down() {
        selectedIndex = (selectedIndex + 1) % selectionOrder.size();
        updateSelection();
    }

    private void updateSelection() {
        if (selectedIndex >= 0 && selectedIndex < selectionOrder.size()) {
            Pair<DatasetDesc, Actor> selection = selectionOrder.get(selectedIndex);
            Actor target = selection.getSecond();
            stage.setKeyboardFocus(target);
            // Move scroll
            target = target.getParent();
            int si = selectedIndex;
            leftScroll.setScrollY(si * target.getHeight());
            // Update right pane
            GaiaSky.postRunnable(() -> reloadRightPane(right, selection.getFirst(), currentMode));
            selectedDataset[currentMode.ordinal()] = selection.getFirst();
        }
    }

    private class DatasetManagerGamepadListener extends AbstractGamepadListener {

        public DatasetManagerGamepadListener(String mappingsFile) {
            super(mappingsFile);
        }

        @Override
        public void connected(Controller controller) {

        }

        @Override
        public void disconnected(Controller controller) {

        }

        @Override
        public boolean buttonDown(Controller controller, int buttonCode) {
            return true;
        }

        @Override
        public boolean buttonUp(Controller controller, int buttonCode) {
            if (buttonCode == mappings.getButtonStart()) {
                accept();
            } else if (buttonCode == mappings.getButtonA()) {
                fire();
            } else if (buttonCode == mappings.getButtonB() || buttonCode == mappings.getButtonSelect()) {
                cycleDialogButtons();
            } else if (buttonCode == mappings.getButtonDpadUp()) {
                up();
            } else if (buttonCode == mappings.getButtonDpadDown()) {
                down();
            } else if (buttonCode == mappings.getButtonRB()) {
                tabRight();
            } else if (buttonCode == mappings.getButtonLB()) {
                tabLeft();
            }
            return true;
        }

        @Override
        public boolean axisMoved(Controller controller, int axisCode, float value) {
            if (Math.abs(value) > AXIS_TH && System.currentTimeMillis() - lastAxisEvtTime > AXIS_EVT_DELAY) {
                // Event-based
                if (axisCode == mappings.getAxisLstickV()) {
                    // LEFT STICK vertical - move vertically
                    if (value > 0) {
                        down();
                    } else {
                        up();
                    }
                }
                lastAxisEvtTime = System.currentTimeMillis();
            }
            return true;
        }

        @Override
        public void update() {

        }

        @Override
        public void activate() {

        }

        @Override
        public void deactivate() {

        }

    }
}
