/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.datasets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.window.GenericDialog;
import gaiasky.input.WindowGamepadListener;
import gaiasky.input.WindowKbdListener;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.datadesc.*;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import net.jafama.FastMath;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The dataset manager. Download, enable, disable and remove datasets.
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
        iconMap.put("virtualtex-pack", "iconic-image");
        iconMap.put("volume", "icon-elem-nebulae");
    }

    private final DatasetDesc[] selectedDataset;
    private final float[][] scroll;
    private final Map<String, Pair<DatasetDesc, Net.HttpRequest>> currentDownloads;
    private final Map<String, Button>[] buttonMap;
    private final List<Pair<DatasetDesc, Actor>> selectionOrder;
    private final Color highlight;
    // Whether to show the data location chooser
    private final boolean dataLocation;
    private final DecimalFormat nf;
    private final Set<DatasetWatcher> watchers;
    private final AtomicBoolean initialized;
    private DataDescriptor serverDd;
    private DatasetMode currentMode;
    private Cell<?> right;
    private OwnScrollPane leftScroll;
    private int selectedIndex = 0;
    private DatasetWatcher rightPaneWatcher;

    public DatasetManagerWindow(Stage stage, Skin skin, DataDescriptor serverDd) {
        this(stage, skin, serverDd, true, I18n.msg("gui.close"));
    }

    public DatasetManagerWindow(Stage stage, Skin skin, DataDescriptor serverDd, boolean dataLocation, String acceptText) {
        super(I18n.msg("gui.download.title") + (serverDd != null && serverDd.updatesAvailable ? " - " + I18n.msg("gui.download.updates",
                                                                                                                 serverDd.numUpdates) : ""),
              skin,
              stage);
        this.nf = new DecimalFormat("##0.0");
        this.serverDd = serverDd;
        this.highlight = ColorUtils.gYellowC;
        this.watchers = new HashSet<>();
        this.scroll = new float[][]{{0f, 0f}, {0f, 0f}};
        this.selectedDataset = new DatasetDesc[2];
        this.initialized = new AtomicBoolean(false);
        this.buttonMap = new HashMap[2];
        this.buttonMap[0] = new HashMap<>();
        this.buttonMap[1] = new HashMap<>();
        this.currentDownloads = Collections.synchronizedMap(new HashMap<>());
        this.selectionOrder = new ArrayList<>();
        this.dataLocation = dataLocation;

        // Use our keyboard listener.
        this.defaultMouseKbdListener = false;
        this.mouseKbdListener = new DatasetManagerKbdListener(this);

        // Use our own gamepad listener.
        this.defaultGamepadListener = false;
        this.gamepadListener = new DatasetManagerGamepadListener(Settings.settings.controls.gamepad.mappingsFile);

        setAcceptText(acceptText);

        // Build
        buildSuper();
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
            case "catalog-other" -> 6;
            case "system" -> 7;
            case "spacecraft" -> 8;
            case "mesh" -> 9;
            case "volume" -> 10;
            case "virtualtex-pack" -> 11;
            case "other" -> 12;
            default -> 13;
        };
    }

    public static String getIcon(String type) {
        if (type != null && iconMap.containsKey(type))
            return iconMap.get(type);
        return "icon-elem-others";
    }

    @Override
    protected void build() {
        initialized.set(false);
        float tabWidth = 500f;
        float width = 1800f;

        try {
            Files.createDirectories(SysUtils.getDefaultDatasetsDir());
        } catch (FileAlreadyExistsException e) {
            // Good
        } catch (IOException e) {
            logger.error(e);
            return;
        }

        // Tabs.
        HorizontalGroup tabGroup = new HorizontalGroup();
        tabGroup.align(Align.center);

        String tabInstalledText;
        if (serverDd != null && serverDd.updatesAvailable) {
            tabInstalledText = I18n.msg("gui.download.tab.installed.updates", serverDd.numUpdates);
        } else {
            tabInstalledText = I18n.msg("gui.download.tab.installed");
        }


        final OwnTextButton tabAvail = new OwnTextButton(I18n.msg("gui.download.tab.available"), skin, "toggle-big");
        tabAvail.pad(pad10);
        tabAvail.setWidth(tabWidth);
        final OwnTextButton tabInstalled = new OwnTextButton(tabInstalledText, skin, "toggle-big");
        tabInstalled.pad(pad10);
        tabInstalled.setWidth(tabWidth);

        tabGroup.addActor(tabAvail);
        tabGroup.addActor(tabInstalled);

        content.add(tabGroup).center().expandX().row();

        // Create the tab content. Just using images here for simplicity.
        tabStack = new Stack();

        // Content.
        final Table contentAvail = new Table(skin);
        contentAvail.align(Align.top);
        contentAvail.pad(pad18);

        final Table contentInstalled = new Table(skin);
        contentInstalled.align(Align.top);
        contentInstalled.pad(pad18);

        /* ADD ALL CONTENT */
        addTabContent(contentAvail);
        addTabContent(contentInstalled);

        content.add(tabStack).expand().fill().padBottom(pad34).row();

        // Listen to changes in the tab button checked states.
        // Set visibility of the tab content to match the checked state.
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

        // Let only one tab button be checked at a time.
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
        catalogsLocGroup.space(pad18);
        catalogsLocGroup.addActor(catalogsLocLabel);
        catalogsLocGroup.addActor(catalogsLocTooltip);

        String dataLocationString = Path.of(catLoc).toString();
        OwnTextButton dataLocationButton = new OwnTextButton(TextUtils.capString(dataLocationString, 85), skin);
        dataLocationButton.addListener(new OwnTextTooltip(dataLocationString, skin));
        dataLocationButton.pad(buttonPad * 4f);
        dataLocationButton.setMinWidth(1000f);

        dataLocTable.add(catalogsLocGroup).left().padBottom(pad18);
        dataLocTable.add(dataLocationButton).left().padLeft(pad10).padBottom(pad18).row();
        Cell<Actor> notice = dataLocTable.add((Actor) null).colspan(2).padBottom(pad18);
        notice.row();

        content.add(dataLocTable).center().row();

        dataLocationButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                FileChooser fc = new FileChooser(I18n.msg("gui.download.pickloc"),
                                                 skin,
                                                 stage,
                                                 Path.of(Settings.settings.data.location),
                                                 FileChooser.FileChooserTarget.DIRECTORIES);
                fc.setShowHidden(Settings.settings.program.fileChooser.showHidden);
                fc.setShowHiddenConsumer((showHidden) -> Settings.settings.program.fileChooser.showHidden = showHidden);
                fc.setResultListener((success, result) -> {
                    if (success) {
                        if (Files.isReadable(result) && Files.isWritable(result)) {
                            // Set data location.
                            dataLocationButton.setText(result.toAbsolutePath().toString());
                            // Change data location.
                            Settings.settings.data.location = result.toAbsolutePath().toString().replaceAll("\\\\", "/");
                            // Create temp dir.
                            SysUtils.mkdir(SysUtils.getDataTempDir(Settings.settings.data.location));
                            me.pack();
                            GaiaSky.postRunnable(() -> {
                                // Reset datasets.
                                Settings.settings.data.dataFiles.clear();
                                reloadAll();
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
        var localDd = DataDescriptorUtils.instance().buildLocalDatasets(this.serverDd);
        reloadBothPanes(content, width, localDd, currentMode = DatasetMode.INSTALLED);
    }

    private void reloadBothPanes(Table content, float width, DataDescriptor dataDescriptor, DatasetMode mode) {
        content.clear();
        if (dataDescriptor == null || dataDescriptor.datasets.isEmpty()) {
            if (mode == DatasetMode.AVAILABLE) {
                content.add(new OwnLabel(I18n.msg("gui.download.noconnection.title"), skin)).center().padTop(pad34 * 2f).padBottom(pad18).row();
            }
            content.add(new OwnLabel(I18n.msg("gui.dschooser.nodatasets"), skin))
                    .center()
                    .padTop(mode == DatasetMode.AVAILABLE ? 0 : pad34 * 2f)
                    .row();
        } else {
            var left = content.add().top().left().padRight(pad34);
            right = content.add().top().left();

            int datasets = reloadLeftPane(left, dataDescriptor, mode, width);
            if (datasets > 0) {
                reloadRightPane(right, selectedDataset[mode.ordinal()], mode);
            } else {
                content.clear();
                content.add(new OwnLabel(I18n.msg("gui.dschooser.nodatasets"), skin))
                        .center()
                        .padTop(mode == DatasetMode.AVAILABLE ? 0 : pad34 * 2f)
                        .row();
            }

        }
        me.pack();
    }

    private int reloadLeftPane(Cell<?> left, DataDescriptor dataDescriptor, DatasetMode mode, float width) {
        final var leftContent = new Table(skin);
        final var leftTable = new Table(skin);

        var filter = new OwnTextField("", skin, "big");
        filter.setMessageText(I18n.msg("gui.filter"));
        filter.setWidth(400f);
        filter.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                populateLeftTable(leftTable, mode, dataDescriptor, Settings.settings.data.dataFiles, width, filter.getText());
            }
        });
        leftTable.align(Align.topRight);

        leftScroll = new OwnScrollPane(leftTable, skin, "minimalist-nobg");
        leftScroll.setScrollingDisabled(true, false);
        leftScroll.setForceScroll(false, true);
        leftScroll.setSmoothScrolling(false);
        leftScroll.addListener(event -> {
            if (event instanceof InputEvent ie) {
                if (ie.getType() == Type.scrolled) {
                    // Save scroll position
                    scroll[mode.ordinal()][0] = leftScroll.getScrollX();
                    scroll[mode.ordinal()][1] = leftScroll.getScrollY();
                }
            }
            return false;
        });

        selectionOrder.clear();
        selectedIndex = 0;

        // Add datasets to table.
        var added = populateLeftTable(leftTable, mode, dataDescriptor, Settings.settings.data.dataFiles, width, "");

        final var maxScrollHeight = stage.getHeight() * 0.65f;
        leftScroll.setFadeScrollBars(false);
        leftScroll.setWidth(width * 0.52f);
        leftScroll.setHeight(maxScrollHeight);
        leftScroll.layout();
        leftScroll.setScrollX(scroll[mode.ordinal()][0]);
        leftScroll.setScrollY(scroll[mode.ordinal()][1]);

        leftContent.add(filter).top().center().padBottom(pad18).row();
        leftContent.add(leftScroll).top().left();
        left.setActor(leftContent);

        return added;
    }

    private int populateLeftTable(Table leftTable,
                                  DatasetMode mode,
                                  DataDescriptor dataDescriptor,
                                  List<String> currentSetting,
                                  float width,
                                  String filter) {
        leftTable.clear();
        int added = 0;
        for (DatasetType type : dataDescriptor.types) {
            List<DatasetDesc> datasets = type.datasets;
            List<DatasetDesc> filtered = datasets.stream()
                    .filter(d -> d.filter(filter) && (mode != DatasetMode.AVAILABLE || !d.exists))
                    .collect(Collectors.toList());
            added += addDatasetTypeGroup(leftTable, mode, currentSetting, type, filtered, width, filter);
        }
        leftTable.pack();
        return added;
    }

    private int addDatasetTypeGroup(Table leftTable,
                                    DatasetMode mode,
                                    List<String> currentSetting,
                                    DatasetType type,
                                    List<DatasetDesc> filtered,
                                    float width,
                                    String filter) {
        int added = 0;
        if (!filtered.isEmpty()) {
            final Array<CheckBox> groupCheckBoxes = new Array<>();

            // Create collapsible group pane.
            Table contentTable = new Table(skin);
            Table buttons = null;
            if (mode == DatasetMode.INSTALLED) {
                // Select all.
                Button selectAll = new OwnImageButton(skin, "select-all");
                selectAll.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        for (var checkBox : groupCheckBoxes) {
                            if (!checkBox.isDisabled()) {
                                checkBox.setChecked(true);
                            }
                        }
                        return true;
                    }
                    return false;
                });
                selectAll.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.select.all"), skin));

                // Select none.
                Button selectNone = new OwnImageButton(skin, "select-none");
                selectNone.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        for (var checkBox : groupCheckBoxes) {
                            if (!checkBox.isDisabled()) {
                                checkBox.setChecked(false);
                            }
                        }
                        return true;
                    }
                    return false;
                });
                selectNone.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.select.none"), skin));

                final float buttonSize = 17f;
                buttons = new Table(skin);
                buttons.padRight(pad20);
                buttons.add(selectAll).size(buttonSize, buttonSize).right().bottom().padRight(pad10);
                buttons.add(selectNone).size(buttonSize, buttonSize).right().bottom();
            }

            var paneImage = new OwnImage(skin.getDrawable(getIcon(type.typeStr)));
            paneImage.setSize(45f, 45f);
            String typeString;
            if (I18n.hasMessage("gui.download.type." + type.typeStr)) {
                typeString = I18n.msg("gui.download.type." + type.typeStr);
            } else {
                typeString = TextUtils.trueCapitalise(type.typeStr);
            }
            CollapsiblePane groupPane = new CollapsiblePane(stage, paneImage, typeString,
                                                            contentTable, width * 0.5f, skin, "hud-header", "expand-collapse",
                                                            null, filter != null && !filter.isBlank(), null, buttons);
            leftTable.add(groupPane).left().padTop(pad34 * 2f).row();
            selectionOrder.add(new Pair<>(null, groupPane.getExpandCollapseActor()));

            // Add datasets to content table.
            boolean anySelected = false;
            for (DatasetDesc dataset : filtered) {
                var t = new Table(skin);
                t.pad(pad18, pad18, 0, pad18);

                var tooltipText = dataset.key;

                // Type icon.
                var typeImage = new OwnImage(skin.getDrawable(getIcon(dataset.type)));
                float scl = 0.7f;
                float iw = typeImage.getWidth();
                float ih = typeImage.getHeight();
                typeImage.setSize(iw * scl, ih * scl);
                typeImage.addListener(new OwnTextTooltip(dataset.type, skin, 10));

                // Title.
                var titleString = dataset.name;
                var title = new OwnLabel(TextUtils.capString(titleString, 60), skin, "huge");
                title.setWidth(width * 0.41f);
                if (dataset.outdated) {
                    title.setColor(highlight);
                    tooltipText = I18n.msg("gui.download.version.new", Integer.toString(dataset.serverVersion), Integer.toString(dataset.myVersion));
                }

                // Install, update or enable/disable.
                Actor installOrSelect;
                float installOrSelectSize = 43f;
                if (mode == DatasetMode.AVAILABLE || dataset.outdated) {
                    var install = new OwnTextIconButton("", skin, "install");
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
                    anySelected = true;
                } else {
                    var select = new OwnCheckBox("", skin, 0f);
                    groupCheckBoxes.add(select);
                    installOrSelect = select;
                    if (dataset.baseData || dataset.type.equals("texture-pack")) {
                        select.setChecked(true);
                        select.setDisabled(true);
                    } else if (dataset.minGsVersion > Settings.SOURCE_VERSION) {
                        select.setChecked(false);
                        select.setDisabled(true);
                        title.setColor(ColorUtils.gRedC);
                        tooltipText = I18n.msg("gui.download.version.gs.mismatch",
                                               Integer.toString(Settings.SOURCE_VERSION),
                                               Integer.toString(dataset.minGsVersion));
                        select.getStyle().disabledFontColor = ColorUtils.gRedC;

                        // Remove from selected, if it is.
                        String filePath = dataset.catalogFile.path();
                        if (Settings.settings.data.dataFiles.contains(filePath)) {
                            Settings.settings.data.dataFiles.remove(filePath);
                            logger.info(I18n.msg("gui.download.disabled.version",
                                                 dataset.name,
                                                 Integer.toString(dataset.minGsVersion),
                                                 Integer.toString(Settings.SOURCE_VERSION)));
                        }
                    } else {
                        select.setChecked(DatasetDownloadUtils.isPathIn(Settings.settings.data.dataFile(dataset.checkStr), currentSetting));
                        select.addListener(new OwnTextTooltip(dataset.checkPath.toString(), skin));
                    }
                    select.setSize(installOrSelectSize, installOrSelectSize);
                    select.setHeight(40f);
                    select.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            if (select.isChecked()) {
                                actionEnableDataset(dataset, select);
                            } else {
                                actionDisableDataset(dataset);
                            }
                            GaiaSky.postRunnable(() -> reloadRightPane(right, dataset, mode));
                        }
                    });
                    anySelected = anySelected || select.isChecked();
                }

                // Size.
                var size = new OwnLabel(dataset.size, skin, "grey-large");
                size.addListener(new OwnTextTooltip(I18n.msg("gui.download.size.tooltip"), skin, 10));
                size.setWidth(88f);

                // Version.
                OwnLabel version = null;
                if (mode == DatasetMode.AVAILABLE) {
                    version = new OwnLabel(I18n.msg("gui.download.version.server", dataset.serverVersion), skin, "grey-large");
                } else if (mode == DatasetMode.INSTALLED) {
                    if (dataset.outdated) {
                        version = new OwnLabel(I18n.msg("gui.download.version.new",
                                                        Integer.toString(dataset.serverVersion),
                                                        Integer.toString(dataset.myVersion)), skin, "grey-large");
                        version.setColor(highlight);
                    } else {
                        version = new OwnLabel(I18n.msg("gui.download.version.local", dataset.myVersion), skin, "grey-large");
                    }
                }
                var versionSize = new HorizontalGroup();
                versionSize.space(pad34 * 2f);
                versionSize.addActor(version);
                versionSize.addActor(size);

                // Progress.
                var progress = new OwnProgressBar(0f, 100f, 0.1f, false, skin, "small-horizontal");
                progress.setPrefWidth(850f);
                progress.setValue(60f);
                progress.setVisible(false);

                t.add(typeImage).size(typeImage.getWidth(), typeImage.getHeight()).left().padRight(pad18);
                t.add(title).left().padRight(pad18);
                t.add(installOrSelect).right().row();
                t.add();
                t.add(versionSize).colspan(2).left().padRight(pad18).padBottom(pad10).row();
                t.add(progress).colspan(3).expandX();
                t.pack();
                OwnButton button = new OwnButton(t, skin, "dataset", false);
                button.setWidth(width * 0.52f);
                title.addListener(new OwnTextTooltip(tooltipText, skin, 10));
                buttonMap[mode.ordinal()].put(dataset.key, button);

                // Clicks.
                button.addListener(new InputListener() {
                    @Override
                    public boolean handle(com.badlogic.gdx.scenes.scene2d.Event event) {
                        if (event instanceof InputEvent ie) {
                            InputEvent.Type type = ie.getType();
                            if (type == InputEvent.Type.touchDown) {
                                if (ie.getButton() == Input.Buttons.LEFT) {
                                    GaiaSky.postRunnable(() -> reloadRightPane(right, dataset, mode));
                                    selectedDataset[mode.ordinal()] = dataset;
                                } else if (ie.getButton() == Input.Buttons.RIGHT) {
                                    GaiaSky.postRunnable(() -> {
                                        // Context menu.
                                        ContextMenu datasetContext = new ContextMenu(skin, "default");
                                        if (mode == DatasetMode.INSTALLED) {
                                            if (dataset.outdated) {
                                                // Update.
                                                var update = new MenuItem(I18n.msg("gui.download.update"),
                                                                          skin,
                                                                          skin.getDrawable("iconic-arrow-circle-bottom"));
                                                update.addListener(new ChangeListener() {
                                                    @Override
                                                    public void changed(ChangeEvent event, Actor actor) {
                                                        actionUpdateDataset(dataset);
                                                    }
                                                });
                                                datasetContext.addItem(update);
                                            }
                                            if (!dataset.baseData && !dataset.type.equals("texture-pack") && dataset.minGsVersion <= Settings.SOURCE_VERSION) {
                                                boolean enabled = DatasetDownloadUtils.isPathIn(dataset.catalogFile.path(), currentSetting);
                                                if (enabled) {
                                                    // Disable.
                                                    var disable = new MenuItem(I18n.msg("gui.download.disable"),
                                                                               skin,
                                                                               skin.getDrawable("check-off-disabled"));
                                                    disable.addListener(new ChangeListener() {
                                                        @Override
                                                        public void changed(ChangeEvent event, Actor actor) {
                                                            actionDisableDataset(dataset);
                                                            if (installOrSelect instanceof OwnCheckBox cb) {
                                                                cb.setProgrammaticChangeEvents(false);
                                                                cb.setChecked(false);
                                                                cb.setProgrammaticChangeEvents(true);
                                                            }
                                                            GaiaSky.postRunnable(() -> reloadRightPane(right, dataset, DatasetMode.INSTALLED));
                                                        }
                                                    });
                                                    datasetContext.addItem(disable);
                                                } else {
                                                    // Enable.
                                                    var enable = new MenuItem(I18n.msg("gui.download.enable"), skin, skin.getDrawable("check-on"));
                                                    enable.addListener(new ChangeListener() {
                                                        @Override
                                                        public void changed(ChangeEvent event, Actor actor) {
                                                            actionEnableDataset(dataset, null);
                                                            if (installOrSelect instanceof OwnCheckBox cb) {
                                                                cb.setProgrammaticChangeEvents(false);
                                                                cb.setChecked(true);
                                                                cb.setProgrammaticChangeEvents(true);
                                                            }
                                                            GaiaSky.postRunnable(() -> reloadRightPane(right, dataset, DatasetMode.INSTALLED));
                                                        }
                                                    });
                                                    datasetContext.addItem(enable);
                                                }
                                                datasetContext.addSeparator();
                                            }
                                            // Delete.
                                            var delete = new MenuItem(I18n.msg("gui.download.delete"), skin, skin.getDrawable("iconic-trash"));
                                            delete.addListener(new ClickListener() {
                                                @Override
                                                public void clicked(InputEvent event, float x, float y) {
                                                    actionDeleteDataset(dataset);
                                                    super.clicked(event, x, y);
                                                }
                                            });
                                            datasetContext.addItem(delete);
                                        } else if (mode == DatasetMode.AVAILABLE) {
                                            // Install.
                                            var install = new MenuItem(I18n.msg("gui.download.install"),
                                                                       skin,
                                                                       skin.getDrawable("iconic-cloud-download"));
                                            install.addListener(new ChangeListener() {
                                                @Override
                                                public void changed(ChangeEvent event, Actor actor) {
                                                    actionDownloadDataset(dataset);
                                                }
                                            });
                                            datasetContext.addItem(install);
                                        }
                                        var h = (int) stage.getHeight();
                                        float unitsPerPixel = GaiaSky.instance.getUnitsPerPixel();
                                        float px = Gdx.input.getX(ie.getPointer()) * unitsPerPixel;
                                        float py = h - Gdx.input.getY(ie.getPointer()) * unitsPerPixel - 32f;
                                        datasetContext.showMenu(stage, px, py);
                                    });
                                }
                            }
                        }
                        return super.handle(event);
                    }
                });

                contentTable.add(button).left().row();

                // Add check.
                selectionOrder.add(new Pair<>(dataset, installOrSelect));

                if (selectedDataset[mode.ordinal()] == null) {
                    selectedDataset[mode.ordinal()] = dataset;
                    selectedIndex = selectionOrder.size() - 1;
                }

                // Create watcher.
                watchers.add(new DatasetWatcher(dataset,
                                                progress,
                                                installOrSelect instanceof OwnTextIconButton ? (OwnTextIconButton) installOrSelect : null,
                                                null,
                                                null));
                added++;
            }
            if (anySelected) {
                groupPane.expandPane();
            }

        }
        return added;
    }

    private void reloadRightPane(Cell<?> cell, DatasetDesc dataset, DatasetMode mode) {
        if (rightPaneWatcher != null) {
            rightPaneWatcher.dispose();
            watchers.remove(rightPaneWatcher);
            rightPaneWatcher = null;
        }
        var rightTable = new Table(skin);

        if (dataset == null) {
            var l = new OwnLabel(I18n.msg("gui.download.noselected"), skin);
            rightTable.add(l).center().padTop(pad34 * 3);
        } else {
            // Type icon.
            var dType = dataset.type != null ? dataset.type : "other";
            var typeImage = new OwnImage(skin.getDrawable(getIcon(dType)));
            var scl = 0.7f;
            var iw = typeImage.getWidth();
            var ih = typeImage.getHeight();
            typeImage.setSize(iw * scl, ih * scl);
            typeImage.addListener(new OwnTextTooltip(dType, skin, 10));

            // Title.
            var titleString = dataset.name;
            OwnLabel title = new OwnLabel(TextUtils.breakCharacters(titleString, 45), skin, "hud-header");
            title.addListener(new OwnTextTooltip(dataset.description, skin, 10));

            // Title group.
            var titleGroup = new HorizontalGroup();
            titleGroup.space(pad18);
            titleGroup.addActor(typeImage);
            titleGroup.addActor(title);

            // Status.
            OwnLabel status = null;
            if (mode == DatasetMode.AVAILABLE) {
                status = new OwnLabel(I18n.msg("gui.download.available"), skin, "mono");
            } else if (mode == DatasetMode.INSTALLED) {
                if (dataset.baseData || dType.equals("texture-pack")) {
                    // Always enabled.
                    status = new OwnLabel(I18n.msg("gui.download.enabled"), skin, "mono");
                } else if (dataset.minGsVersion > Settings.SOURCE_VERSION) {
                    // Notify version mismatch.
                    status = new OwnLabel(I18n.msg("gui.download.version.gs.mismatch.short",
                                                   Integer.toString(Settings.SOURCE_VERSION),
                                                   Integer.toString(dataset.minGsVersion)), skin, "mono");
                    status.setColor(ColorUtils.gRedC);
                } else {
                    // Notify status.
                    List<String> currentSetting = Settings.settings.data.dataFiles;
                    boolean enabled = DatasetDownloadUtils.isPathIn(dataset.catalogFile.path(), currentSetting);
                    status = new OwnLabel(I18n.msg(enabled ? "gui.download.enabled" : "gui.download.disabled"), skin, "mono");
                }
            }

            // Type.
            String typeString;
            if (I18n.hasMessage("gui.download.type." + dType)) {
                typeString = I18n.msg("gui.download.type." + dType);
            } else {
                typeString = dType;
            }
            var type = new OwnLabel(I18n.msg("gui.download.type", typeString), skin, "grey-large");
            type.addListener(new OwnTextTooltip(dType, skin, 10));

            // Version.
            OwnLabel version = null;
            if (mode == DatasetMode.AVAILABLE) {
                version = new OwnLabel(I18n.msg("gui.download.version.server", dataset.serverVersion), skin, "grey-large");
            } else if (mode == DatasetMode.INSTALLED) {
                if (dataset.outdated) {
                    version = new OwnLabel(I18n.msg("gui.download.version.new",
                                                    Integer.toString(dataset.serverVersion),
                                                    Integer.toString(dataset.myVersion)), skin, "grey-large");
                    version.setColor(highlight);
                } else {
                    version = new OwnLabel(I18n.msg("gui.download.version.local", dataset.myVersion), skin, "grey-large");
                }
            }

            // Key.
            var key = new OwnLabel(I18n.msg("gui.download.name", dataset.key), skin, "grey-large");

            // Creator
            var creator = new OwnLabel(I18n.msg("gui.download.creator", TextUtils.capString(dataset.creator, 70)), skin, "grey-large");

            // Size.
            var size = new OwnLabel(I18n.msg("gui.download.size", dataset.size), skin, "grey-large");
            size.addListener(new OwnTextTooltip(I18n.msg("gui.download.size.tooltip"), skin, 10));

            // Num objects.
            var nObjStr = dataset.nObjects > 0 ? I18n.msg("gui.dataset.nobjects", (int) dataset.nObjects) : I18n.msg("gui.dataset.nobjects.none");
            var nObjects = new OwnLabel(nObjStr, skin, "grey-large");
            nObjects.addListener(new OwnTextTooltip(I18n.msg("gui.download.nobjects.tooltip") + ": " + dataset.nObjectsStr, skin, 10));

            // Links.
            Table linksGroup = null;
            if (dataset.links != null) {
                linksGroup = new Table(skin);

                int i = 0;
                for (var link : dataset.links) {
                    if (!link.isBlank()) {
                        String linkStr = link.replace("@mirror-url@", Settings.settings.program.url.dataMirror);
                        var linkActor = new Link(TextUtils.breakCharacters(linkStr, 70, true), skin, link);
                        if (i > 0)
                            linksGroup.row();
                        linksGroup.add(linkActor).left();
                        i++;
                    }
                }
            }

            Table infoTable = new Table(skin);
            infoTable.align(Align.topLeft);

            // Description.
            var descriptionString = dataset.description;
            var desc = new OwnLabel(TextUtils.breakCharacters(descriptionString, 70), skin);
            desc.setWidth(1000f);

            // Release notes.
            var releaseNotesString = dataset.releaseNotes;
            if (releaseNotesString == null || releaseNotesString.isBlank()) {
                releaseNotesString = "-";
            }
            releaseNotesString = TextUtils.breakCharacters(releaseNotesString, 70);
            var releaseNotesTitle = new OwnLabel(I18n.msg("gui.download.releasenotes"), skin, "grey-large");
            var releaseNotes = new OwnLabel(releaseNotesString, skin);
            releaseNotes.setWidth(1000f);

            // Credits
            var credits = dataset.credits;
            Table creditsContent = null;
            Label creditsTitle = null;
            if (credits != null && credits.length > 0) {
                creditsContent = new Table(skin);
                creditsTitle = new OwnLabel(I18n.msg("gui.download.credits"), skin, "grey-large");
                for (var c : credits) {
                    if (c != null && !c.isEmpty()) {
                        creditsContent.add("-").left().top().padRight(pad10);
                        creditsContent.add(new OwnLabel(TextUtils.breakCharacters(c, 70), skin)).left().row();
                    }
                }
            }

            // Files.
            var filesTitle = new OwnLabel(I18n.msg("gui.download.files"), skin, "grey-large");
            String filesString;
            if (dataset.files == null || dataset.files.length == 0) {
                filesString = "-";
            } else {
                filesString = TextUtils.arrayToStr(dataset.files, "", "", "\n");
                // Use data location token to keep from overflowing horizontally.
                var dataLocation = Settings.settings.data.location;
                if (!dataLocation.endsWith(File.separator) && !dataLocation.endsWith("/")) {
                    dataLocation += "/";
                }
                filesString = filesString.replace(dataLocation, Constants.DATA_LOCATION_TOKEN);
            }
            var files = new OwnLabel(filesString, skin, "mono");

            // Data location.
            var dataLocationNoteString = Constants.DATA_LOCATION_TOKEN + "  =  " + Settings.settings.data.location;
            var dataLocationNote = new OwnLabel(TextUtils.capString(dataLocationNoteString, 60), skin, "mono-pink");
            dataLocationNote.addListener(new OwnTextTooltip(dataLocationNoteString, skin, 10));

            infoTable.add(desc).top().left().padBottom(pad34).row();
            infoTable.add(releaseNotesTitle).top().left().padBottom(pad18).row();
            infoTable.add(releaseNotes).top().left().padBottom(pad34).row();
            if (creditsContent != null) {
                infoTable.add(creditsTitle).top().left().padBottom(pad18).row();
                infoTable.add(creditsContent).top().left().padBottom(pad34).row();
            }
            infoTable.add(filesTitle).top().left().padBottom(pad18).row();
            infoTable.add(files).top().left().padBottom(pad34).row();
            infoTable.add(dataLocationNote).top().left();

            // Scroll.
            var infoScroll = new OwnScrollPane(infoTable, skin, "minimalist-nobg");
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

            rightTable.add(titleGroup).top().left().padBottom(pad10).padTop(pad34).row();
            rightTable.add(status).top().left().padLeft(pad18 * 3f).padBottom(pad34).row();
            rightTable.add(type).top().left().padBottom(pad10).row();
            rightTable.add(version).top().left().padBottom(pad10).row();
            rightTable.add(key).top().left().padBottom(pad10).row();
            if (dataset.creator != null)
                rightTable.add(creator).top().left().padBottom(pad10).row();
            rightTable.add(size).top().left().padBottom(pad10).row();
            rightTable.add(nObjects).top().left().padBottom(pad18).row();
            rightTable.add(linksGroup).top().left().padBottom(pad34 * 2f).row();
            rightTable.add(infoScroll).top().left().padBottom(pad34).row();
            rightTable.add(cancelDownloadButton).padTop(pad34).center();
            rightTable.pack();

            // Scroll.
            final var maxScrollHeight = stage.getHeight() * 0.65f;
            var scrollPane = new OwnScrollPane(rightTable, skin, "minimalist-nobg");
            scrollPane.setScrollingDisabled(true, false);
            scrollPane.setSmoothScrolling(false);
            scrollPane.setFadeScrollBars(false);
            scrollPane.setWidth(1050f);
            scrollPane.setHeight(Math.min(maxScrollHeight, rightTable.getHeight()));

            cell.setActor(scrollPane);
            cell.top().left();

            // Create watcher.
            rightPaneWatcher = new DatasetWatcher(dataset, null, null, status, rightTable);
            watchers.add(rightPaneWatcher);
        }
    }


    public void refresh() {
        content.clear();
        build();
    }

    private void downloadDataset(DatasetDesc dataset) {
        downloadDataset(dataset, null);
    }

    private void downloadDataset(DatasetDesc dataset, Runnable successRunnable) {
        var tempDir = SysUtils.getDataTempDir(Settings.settings.data.location);

        try {
            var fileStore = Files.getFileStore(tempDir);

            // Check for space. We need enough space for the compressed tar.gz package, plus the
            // extracted data, so we do s + s * 1.5, with a base compression ratio of 0.666.
            if (dataset.sizeBytes > 0 && dataset.sizeBytes + dataset.sizeBytes * 1.5 >= fileStore.getUsableSpace()) {
                var title = I18n.msg("gui.download.space.error.title");
                var msg = I18n.msg("gui.download.space.error", fileStore.toString());
                logger.error(msg);
                GuiUtils.addNotificationWindow(title, msg, skin, stage, null);
                return;
            }
        } catch (IOException e) {
            logger.warn("Error getting file store for temp dir: " + tempDir);
        }

        String name = dataset.name;
        String url = dataset.file.replace("@mirror-url@", Settings.settings.program.url.dataMirror);

        String filename = FilenameUtils.getName(url);
        FileHandle tempDownload = Gdx.files.absolute(tempDir + "/" + filename + ".part");

        ProgressRunnable progressDownload = (read, total, progress, speed) -> {
            try {
                double readMb = (double) read / 1e6d;
                double totalMb = (double) total / 1e6d;
                final String progressString = progress >= 100 ? I18n.msg("gui.done") : I18n.msg("gui.download.downloading", nf.format(progress));
                double mbPerSecond = speed / 1000d;
                final String speedString = nf.format(readMb) + "/" + nf.format(totalMb) + " MB (" + nf.format(mbPerSecond) + " MB/s)";
                // Since we are downloading on a background thread, post a runnable to touch UI.
                GaiaSky.postRunnable(() -> EventManager.publish(Event.DATASET_DOWNLOAD_PROGRESS_INFO,
                                                                this,
                                                                dataset.key,
                                                                (float) progress,
                                                                progressString,
                                                                speedString));
            } catch (Exception e) {
                logger.warn(I18n.msg("gui.download.error.progress"));
            }
        };
        ProgressRunnable progressHashResume = (read, total, progress, speed) -> {
            double readMb = (double) read / 1e6d;
            double totalMb = (double) total / 1e6d;
            final String progressString = progress >= 100 ? I18n.msg("gui.done") : I18n.msg("gui.download.checksum.check", nf.format(progress));
            double mbPerSecond = speed / 1000d;
            final String speedString = nf.format(readMb) + "/" + nf.format(totalMb) + " MB (" + nf.format(mbPerSecond) + " MB/s)";
            // Since we are downloading on a background thread, post a runnable to touch UI.
            GaiaSky.postRunnable(() -> EventManager.publish(Event.DATASET_DOWNLOAD_PROGRESS_INFO,
                                                            this,
                                                            dataset.key,
                                                            (float) progress,
                                                            progressString,
                                                            speedString));
        };

        Consumer<String> finish = (digest) -> {
            String errorMsg = null;
            // Unpack.
            int errors = 0;
            logger.info(I18n.msg("gui.download.extracting", tempDownload.path()));
            String dataLocation = Settings.settings.data.location + File.separatorChar;
            // Checksum.
            if (digest != null && dataset.sha256 != null) {
                String serverDigest = dataset.sha256;
                try {
                    boolean ok = serverDigest.equals(digest);
                    if (ok) {
                        logger.info(I18n.msg("gui.download.checksum.ok", name));
                    } else {
                        logger.error(I18n.msg("gui.download.checksum.fail", name));
                        errorMsg = I18n.msg("gui.download.checksum.fail.msg");
                        errors++;
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.checksum.error", name), -1f);
                    }
                } catch (Exception e) {
                    logger.info(I18n.msg("gui.download.checksum.error", name));
                    errorMsg = I18n.msg("gui.download.checksum.fail.msg");
                    errors++;
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.checksum.error", name), -1f);
                }
            } else {
                logger.info(I18n.msg("gui.download.checksum.notfound", name));
                EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.checksum.notfound", name), -1f);
            }

            if (errors == 0) {
                try {
                    // Extract.
                    DatasetDownloadUtils.decompress(tempDownload.path(), new File(dataLocation), dataset);
                } catch (Exception e) {
                    logger.error(e, I18n.msg("gui.download.decompress.error", name));
                    errorMsg = I18n.msg("gui.download.decompress.error.msg");
                    errors++;
                } finally {
                    // Remove archive.
                    DatasetDownloadUtils.cleanupTempFile(tempDownload.path());
                }
            }

            final String errorMessage = errorMsg;
            final int numErrors = errors;
            // Done.
            GaiaSky.postRunnable(() -> {
                currentDownloads.remove(dataset.key);

                if (numErrors == 0) {
                    // Ok message.
                    EventManager.publish(Event.DATASET_DOWNLOAD_FINISH_INFO, this, dataset.key, 0);
                    dataset.exists = true;
                    actionEnableDataset(dataset, null);
                    if (successRunnable != null) {
                        successRunnable.run();
                    }
                    resetSelectedDataset();
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.finished", name), -1f);
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
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.failed", name), -1f);
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
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.failed", name), -1f);
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

        // Download.
        final Net.HttpRequest request = DownloadHelper.downloadFile(url,
                                                                    tempDownload,
                                                                    Settings.settings.program.offlineMode,
                                                                    progressDownload,
                                                                    progressHashResume,
                                                                    finish,
                                                                    fail,
                                                                    cancel);
        GaiaSky.postRunnable(() -> EventManager.publish(Event.DATASET_DOWNLOAD_START_INFO, this, dataset.key, request));
        currentDownloads.put(dataset.key, new Pair<>(dataset, request));

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
        // Create a copy.
        Map<String, Pair<DatasetDesc, HttpRequest>> copy = new HashMap<>(currentDownloads);

        if (!copy.isEmpty()) {
            GenericDialog question = new GenericDialog(I18n.msg("gui.download.close.title"), skin, stage) {

                @Override
                protected void build() {
                    content.clear();
                    content.add(new OwnLabel(I18n.msg("gui.download.close", currentDownloads.size()), skin)).left().padBottom(pad34).row();
                    content.add(new OwnLabel(I18n.msg("gui.download.close.current"), skin)).left().padBottom(pad18).row();
                    for (String key : copy.keySet()) {
                        DatasetDesc dd = copy.get(key).getFirst();
                        content.add(new OwnLabel(dd.name, skin, "warp")).center().padBottom(pad10).row();
                    }
                }

                @Override
                protected boolean accept() {
                    // Cancel all requests.
                    Set<String> keys = copy.keySet();
                    for (String key : keys) {
                        Net.HttpRequest request = copy.get(key).getSecond();
                        Gdx.net.cancelHttpRequest(request);
                    }
                    if (this.acceptListener != null) {
                        this.acceptListener.run();
                    }
                    return true;
                }

                @Override
                protected void cancel() {
                    // Nothing.
                }

                @Override
                public void dispose() {
                    // Nothing.
                }
            };
            question.setAcceptText(I18n.msg("gui.yes"));
            question.setCancelText(I18n.msg("gui.no"));
            question.buildSuper();
            question.show(stage);
        } else {
            myself.hide(); // Close.
        }
        // Do not close dialog, we close it.
        return false;
    }

    @Override
    protected void cancel() {
    }

    @Override
    public void dispose() {
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
                    serverDd.numUpdates = FastMath.max(serverDd.numUpdates - 1, 0);
                    serverDd.updatesAvailable = serverDd.numUpdates > 0;
                }
            } else {
                dataset.myVersion = dataset.serverVersion;
            }
        });
    }

    /**
     * Enables a given dataset, so that it is loaded when Gaia Sky starts.
     *
     * @param dataset The dataset to enable.
     */
    private void actionEnableDataset(DatasetDesc dataset, OwnCheckBox cb) {
        // Texture packs can't be enabled here.
        if (dataset.type.equals("texture-pack"))
            return;

        String filePath = null;
        if (dataset.checkStr != null) {
            filePath = TextUtils.ensureStartsWith(dataset.checkStr, Constants.DATA_LOCATION_TOKEN);
        }
        if (filePath != null && !filePath.isBlank()) {
            if (!Settings.settings.data.dataFiles.contains(filePath)) {
                var opt = checkDatasetIncompatibilities(dataset);
                if (opt.isEmpty()) {
                    Settings.settings.data.dataFiles.add(filePath);
                } else {
                    if (cb != null) {
                        // Uncheck check box until user takes action.
                        cb.setProgrammaticChangeEvents(false);
                        cb.setChecked(false);
                        cb.setProgrammaticChangeEvents(true);
                    }
                    final var path = filePath;
                    GenericDialog question = new GenericDialog(I18n.msg("gui.download.incompatibility"), skin, stage) {
                        @Override
                        protected void build() {
                            content.clear();
                            var warningIcon = skin.getDrawable("iconic-warning");
                            var warning = new OwnImage(warningIcon);
                            warning.setSize(38, 38);
                            warning.setColor(ColorUtils.gYellowC);
                            content.add(warning).left().padRight(pad20).padBottom(pad34);
                            content.add(new OwnLabel(TextUtils.breakCharacters(opt.get(), 60), skin)).left().padBottom(pad34).row();
                            content.add(new OwnLabel(I18n.msg("gui.download.incompatibility.proceed"), skin))
                                    .left()
                                    .colspan(2)
                                    .padBottom(pad18)
                                    .row();
                        }

                        @Override
                        protected boolean accept() {
                            // Add to selected.
                            Settings.settings.data.dataFiles.add(path);
                            if (cb != null) {
                                cb.setProgrammaticChangeEvents(false);
                                cb.setChecked(true);
                                cb.setProgrammaticChangeEvents(true);
                                GaiaSky.postRunnable(() -> reloadRightPane(right, dataset, currentMode));
                            }
                            return true;
                        }

                        @Override
                        protected void cancel() {
                            // Nothing.
                        }

                        @Override
                        public void dispose() {
                            // Nothing.
                        }
                    };
                    question.setAcceptText(I18n.msg("gui.yes"));
                    question.setCancelText(I18n.msg("gui.no"));
                    question.buildSuper();
                    question.show(stage);
                }
            }
        }
    }

    /**
     * Checks whether the given dataset has incompatibilities with the currently enabled datasets.
     *
     * @param dataset The dataset to check.
     *
     * @return Whether there are incompatibilities with the given dataset and the enabled datasets.
     */
    private Optional<String> checkDatasetIncompatibilities(DatasetDesc dataset) {
        // Check LOD catalogs.
        if (dataset.datasetType.typeStr.equalsIgnoreCase("catalog-lod")) {
            var lodDatasets = dataset.datasetType.datasets;
            for (var lodDataset : lodDatasets) {
                if (lodDataset != dataset && DatasetDownloadUtils.isEnabled(lodDataset)) {
                    return Optional.of(I18n.msg("gui.download.incompatibility.lod"));
                }
            }
        }
        // Check clusters.
        if (dataset.datasetType.typeStr.equalsIgnoreCase("catalog-cluster")) {
            var clusterDatasets = dataset.datasetType.datasets;
            for (var clusterDataset : clusterDatasets) {
                if (clusterDataset != dataset && DatasetDownloadUtils.isEnabled(clusterDataset)) {
                    return Optional.of(I18n.msg("gui.download.incompatibility.cluster"));
                }
            }
        }
        // SDSS.
        if (dataset.datasetType.typeStr.equalsIgnoreCase("catalog-gal") && dataset.key.contains("sdss")) {
            var galDatasets = dataset.datasetType.datasets;
            for (var sdssDataset : galDatasets) {
                if (sdssDataset.key.contains("sdss") && sdssDataset != dataset && DatasetDownloadUtils.isEnabled(sdssDataset)) {
                    return Optional.of(I18n.msg("gui.download.incompatibility.sdss"));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Disable a given dataset, so that it is not loaded during startup.
     *
     * @param dataset The dataset to disable.
     */
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
                content.add(new OwnLabel(I18n.msg("gui.download.delete.text"), skin)).left().padBottom(pad18 * 2f).row();
                content.add(new OwnLabel(title, skin, "warp")).center().padBottom(pad18 * 2f).row();
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
                            Collection<File> files = FileUtils.listFilesAndDirs(directory,
                                                                                WildcardFileFilter.builder().setWildcards(baseName).get(),
                                                                                WildcardFileFilter.builder().setWildcards(baseName).get());
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

    private static final int MAX_REC = 150;

    private boolean up() {
        return up(0);
    }

    private boolean up(int recNum) {
        if (recNum > MAX_REC) {
            return false;
        }
        selectedIndex = selectedIndex - 1;
        if (selectedIndex < 0) {
            selectedIndex = selectionOrder.size() - 1;
        }
        Pair<DatasetDesc, Actor> selection = selectionOrder.get(selectedIndex);
        // Skip collapsed and disabled entries.
        if (!selection.getSecond().isDescendantOf(leftScroll) || (selection.getSecond() instanceof Disableable d) && d.isDisabled()) {
            return up(recNum + 1);
        }
        return updateSelection();
    }

    private boolean down() {
        return down(0);
    }

    private boolean down(int recNum) {
        if (recNum > MAX_REC) {
            return false;
        }
        selectedIndex = (selectedIndex + 1) % selectionOrder.size();
        Pair<DatasetDesc, Actor> selection = selectionOrder.get(selectedIndex);
        // Skip collapsed and disabled entries.
        if (!selection.getSecond().isDescendantOf(leftScroll) || (selection.getSecond() instanceof Disableable d) && d.isDisabled()) {
            return down(recNum + 1);
        }
        return updateSelection();
    }

    private boolean updateSelection() {
        if (selectedIndex >= 0 && selectedIndex < selectionOrder.size()) {
            Pair<DatasetDesc, Actor> selection = selectionOrder.get(selectedIndex);
            Actor target = selection.getSecond();
            stage.setKeyboardFocus(target);
            // Move scroll, select parent container button (dataset widget), and use its position.
            if (selection.getFirst() != null) {
                do {
                    target = target.getParent();
                } while (!(target instanceof Button));
            }
            var coordinates = target.localToAscendantCoordinates(leftScroll.getActor(), new Vector2(target.getX(), target.getY()));
            leftScroll.scrollTo(coordinates.x, coordinates.y, target.getWidth(), FastMath.min(200f, target.getHeight() * 10f));
            if (selection.getFirst() != null) {
                // Update right pane
                GaiaSky.postRunnable(() -> reloadRightPane(right, selection.getFirst(), currentMode));
                selectedDataset[currentMode.ordinal()] = selection.getFirst();
            }
            return true;
        }
        return false;
    }

    private enum DatasetMode {
        AVAILABLE,
        INSTALLED
    }

    private class DatasetManagerKbdListener extends WindowKbdListener {

        public DatasetManagerKbdListener(GenericDialog dialog) {
            super(dialog);
        }

        @Override
        public boolean moveUp() {
            return up();
        }

        @Override
        public boolean moveDown() {
            return down();
        }
    }

    private class DatasetManagerGamepadListener extends WindowGamepadListener {

        public DatasetManagerGamepadListener(String mappingsFile) {
            super(mappingsFile, me);
        }

        @Override
        public void moveUp() {
            up();
        }

        @Override
        public void moveDown() {
            down();
        }
    }
}
