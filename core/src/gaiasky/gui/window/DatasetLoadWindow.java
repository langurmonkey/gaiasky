/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.data.group.DatasetOptions;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.datasets.DatasetLoadDialog;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.util.DatasetCard;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.datadesc.DatasetDownloadUtils;
import gaiasky.util.datadesc.Dataset;
import gaiasky.util.datadesc.DatasetGroup;
import gaiasky.util.datadesc.DatasetType;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 *
 */
public class DatasetLoadWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(DatasetLoadWindow.class);

    private Path lastOpenLocation;
    private final FocusView view;
    private FilePickerComponent filePicker;
    private Dataset selectedDataset = null;
    private final Table contentExisting, contentFile, datasetsTable;

    public DatasetLoadWindow(Stage stage, Skin skin) {
        super(I18n.msg("gui.dsload.title"), skin, stage);

        this.view = new FocusView();
        this.datasetsTable = new Table(skin);
        this.contentExisting = new Table(skin);
        this.contentFile = new Table(skin);

        setAcceptText(I18n.msg("gui.loadcatalog"));
        setCancelText(I18n.msg("gui.close"));

        // Build
        buildSuper();

    }

    @Override
    protected void build() {
        final float tabWidth = 500f;

        padLeft(pad18);
        padRight(pad18);

        // Create the tab buttons
        var tabGroup = new HorizontalGroup();
        tabGroup.left();

        final var tabExisting = new OwnTextButton(I18n.msg("gui.dsload.existing"), skin, "toggle-big");
        tabExisting.pad(pad10);
        tabExisting.setWidth(tabWidth);
        final var tabFile = new OwnTextButton(I18n.msg("gui.dsload.file"), skin, "toggle-big");
        tabFile.pad(pad10);
        tabFile.setWidth(tabWidth);

        tabGroup.addActor(tabExisting);
        tabGroup.addActor(tabFile);

        tabButtons = new Array<>();
        tabButtons.add(tabExisting);
        tabButtons.add(tabFile);

        content.add(tabGroup).left().padLeft(pad10);
        content.row();
        content.pad(pad18);

        /* CONTENT 1 - EXISTING */
        contentExisting.top();
        addContentExisting(contentExisting);
        contentExisting.pack();

        /* CONTENT 2 - FILE */
        contentFile.top();
        addContentFile(contentFile);
        contentFile.pack();


        /* ADD ALL CONTENT */
        addTabContent(contentExisting);
        addTabContent(contentFile);

        content.add(tabStack).expand().fill();

        // Enable/disable accept button on tab change until new selection is made.
        acceptButton.setDisabled(false);
        tabExisting.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                acceptButton.setDisabled(false);
                super.clicked(event, x, y);
            }
        });
        tabFile.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                acceptButton.setDisabled(true);
                super.clicked(event, x, y);
            }
        });

        // Set tab listeners.
        setUpTabListeners();
    }

    public void reload() {
        addContentExisting(contentExisting);
    }

    private void addContentExisting(Table contentExisting) {
        contentExisting.clear();
        var local = DatasetGroup.localDataDescriptor;
        if (local != null && !local.datasets.isEmpty()) {

            ButtonGroup<Button> g = new ButtonGroup<>();

            var filter = new OwnTextField("", skin);
            filter.setMessageText(I18n.msg("gui.filter"));
            filter.setWidth(700f);
            filter.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    reloadDatasetsTable(local, g, filter.getText());
                }
            });

            reloadDatasetsTable(local, g, null);

            var scroll = new OwnScrollPane(datasetsTable, skin, "minimalist-nobg");
            scroll.setScrollingDisabled(true, false);
            scroll.setForceScroll(false, false);
            scroll.setFadeScrollBars(false);
            scroll.setOverscroll(false, false);
            scroll.setSmoothScrolling(true);

            scroll.setHeight(900f);

            if (g.getButtons().size > 0) {
                contentExisting.add(I18n.msg("gui.dsload.existing.info")).left().padTop(pad20).row();
            }
            contentExisting.add(filter).top().left().padTop(pad20).row();
            contentExisting.add(scroll).padTop(pad10);

        } else {
            contentExisting.add("0 datasets available to load").center();
        }
    }

    private void reloadDatasetsTable(DatasetGroup local, ButtonGroup<Button> g, String filter) {
        datasetsTable.clear();
        datasetsTable.align(Align.topLeft);
        g.clear();

        var height = 60f;
        var width = 700f;
        for (var type : local.types) {
            for (var dataset : type.datasets) {
                // Check if dataset is available for loading:
                // 0. Is installed.
                // 1. Is not default-data or texture-pack.
                // 2. Is compatible with current Gaia Sky version.
                // 3. Is not already in the 'enabled' list, or in the current dataset cards.
                var dataFiles = GaiaSky.settings().data.dataFiles;
                var catalogManager = GaiaSky.instance.gaiaSkyAssets.catalogManager;
                boolean available;
                if (dataset.status != Dataset.DatasetStatus.INSTALLED) {
                    available = false;
                } else if (dataset.baseData || dataset.type.equals("texture-pack") || dataset.type.equals("virtualtex-pack")) {
                    available = false;
                } else if (dataset.minGsVersion > Settings.SOURCE_VERSION) {
                    available = false;
                } else {
                    available = !DatasetDownloadUtils.isPathIn(GaiaSky.settings().data.dataFile(dataset.checkStr), dataFiles)
                            && !catalogManager.containsKey(dataset.key);
                }

                if (available) {
                    // Skip dataset if unmatched by filter.
                    if (filter != null && !filter.isBlank()) {
                        var f = filter.toLowerCase(Locale.ROOT);
                        if (!dataset.name.toLowerCase(Locale.ROOT).contains(f)
                                && !dataset.key.toLowerCase(Locale.ROOT).contains(f)) {
                            continue;
                        }
                    }
                    // Add dataset.
                    var t = new Table(skin);
                    var icon = new OwnImage(skin.getDrawable(DatasetType.getTypeIcon(dataset.type)));
                    icon.setSize(45f, 45f);
                    var name = new OwnLabel(dataset.name, skin, "big");
                    t.add(icon).left().padRight(pad10);
                    t.add(name).left().padRight(pad10);
                    var button = new OwnButton(t, skin, "toggle", true);
                    button.setTooltip(dataset.description);
                    button.align(Align.topLeft);
                    button.setPrefSize(width, height);
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            selectedDataset = dataset;
                            acceptButton.setDisabled(false);
                        }
                    });
                    g.add(button);

                    datasetsTable.add(button).center().top().row();
                }
            }
        }

        if (g.getButtons().size <= 0) {
            datasetsTable.add("0 datasets available to load").center().padTop(pad34);
        }
    }

    private void addContentFile(Table contentFile) {
        contentFile.clear();
        if (lastOpenLocation == null && GaiaSky.settings().program.fileChooser.lastLocation != null
                && !GaiaSky.settings().program.fileChooser.lastLocation.isEmpty()) {
            try {
                lastOpenLocation = Paths.get(GaiaSky.settings().program.fileChooser.lastLocation);
            } catch (Exception e) {
                lastOpenLocation = null;
            }
        }
        if (lastOpenLocation == null) {
            lastOpenLocation = SysUtils.getUserHome();
        } else if (!Files.exists(lastOpenLocation) || !Files.isDirectory(lastOpenLocation)) {
            lastOpenLocation = SysUtils.getHomeDir();
        }
        filePicker = new FilePickerComponent(skin, stage, lastOpenLocation, FilePickerComponent.FilePickerTarget.FILES, true);

        // Link component logic to Dialog buttons
        filePicker.setSelectionValidListener(valid -> acceptButton.setDisabled(!valid));

        filePicker.setShowHidden(GaiaSky.settings().program.fileChooser.showHidden);
        filePicker.setShowHiddenConsumer((showHidden) -> GaiaSky.settings().program.fileChooser.showHidden = showHidden);
        filePicker.setFileFilter(pathname -> pathname.getFileName().toString().endsWith(".vot") || pathname.getFileName()
                .toString()
                .endsWith(".csv")
                || pathname.getFileName().toString().endsWith(".fits") || pathname.getFileName().toString().equals("dataset.json"));
        filePicker.setAcceptedFiles("*.vot, *.csv, *.fits, dataset.json");
        filePicker.setResultListener((success, result) -> {
            if (success) {
                if (Files.exists(result) && Files.exists(result)) {
                    // Load selected file.
                    try {
                        filePicker.setSelectionValidListener(valid -> acceptButton.setDisabled(!valid));

                        String fileName = result.getFileName().toString();
                        if (fileName.endsWith(".json")) {
                            // Load internal JSON catalog file.
                            GaiaSky.instance.getExecutorService().execute(() -> {
                                var loaded = GaiaSky.instance.scripting().loadJsonCatalog(fileName, result.toAbsolutePath().toString());
                                if (!loaded) {
                                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.dsload.fail", result.toAbsolutePath()));
                                } else {
                                    EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                                         this,
                                                         I18n.msg("gui.dsload.success", result.toAbsolutePath()));
                                }
                            });
                        } else {
                            final DatasetLoadDialog dld = new DatasetLoadDialog(I18n.msg("gui.dsload.title") + ": " + fileName,
                                                                                fileName,
                                                                                skin,
                                                                                stage);
                            Runnable doLoad = () -> GaiaSky.instance.getExecutorService().execute(() -> {
                                DatasetOptions datasetOptions = dld.generateDatasetOptions();
                                // Load dataset.
                                GaiaSky.instance.scripting().apiv2().data
                                        .load_dataset(datasetOptions.catalogName,
                                                      result.toAbsolutePath().toString(),
                                                      DatasetCard.DatasetSourceType.UI,
                                                      datasetOptions,
                                                      true);
                                // Select first.
                                var catalogManager = GaiaSky.instance.gaiaSkyAssets.catalogManager;
                                DatasetCard ci = catalogManager.get(datasetOptions.catalogName);
                                if (datasetOptions.type.isSelectable() && ci != null && ci.entity != null) {
                                    view.setEntity(ci.entity);
                                    if (view.isSet()) {
                                        var set = view.getSet();
                                        if (set.data() != null && !set.data().isEmpty() && EntityUtils.isVisibilityOn(ci.entity)) {
                                            EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
                                            EventManager.publish(Event.FOCUS_CHANGE_CMD, this, set.getFirstParticleName());
                                        }
                                    } else if (view.getGraph().children != null && !view.getGraph().children.isEmpty() && EntityUtils.isVisibilityOn(
                                            view.getGraph().children.get(0))) {
                                        EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
                                        EventManager.publish(Event.FOCUS_CHANGE_CMD,
                                                             this,
                                                             EntityUtils.isVisibilityOn(view.getGraph().children.get(0)));
                                    }
                                    // Open UI datasets.
                                    GaiaSky.instance.scripting().expandUIPane("Datasets");
                                    EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                                         this,
                                                         I18n.msg("gui.dsload.success", result.toAbsolutePath()));
                                } else {
                                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.dsload.fail", result.toAbsolutePath()));
                                }
                            });
                            dld.setAcceptListener(doLoad);
                            dld.show(stage);
                        }

                        lastOpenLocation = result.getParent();
                        GaiaSky.settings().program.fileChooser.lastLocation = lastOpenLocation.toAbsolutePath().toString();
                        return true;
                    } catch (Exception e) {
                        logger.error(I18n.msg("notif.error", result.getFileName()), e);
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.dsload.fail", result.toAbsolutePath()));
                        return false;
                    }

                } else {
                    logger.error("Selection must be a file: " + result.toAbsolutePath());
                    return false;
                }
            } else {
                // Still, update last location.
                if (!Files.isDirectory(result)) {
                    lastOpenLocation = result.getParent();
                } else {
                    lastOpenLocation = result;
                }
                GaiaSky.settings().program.fileChooser.lastLocation = lastOpenLocation.toAbsolutePath().toString();
            }
            return false;
        });
        contentFile.add(filePicker).top().left().padTop(pad10);
    }

    @Override
    protected boolean accept() {
        if (selectedTab == 0) {
            // Datasets.
            if (selectedDataset != null) {
                // Load installed dataset.
                GaiaSky.instance.getExecutorService().execute(() -> {
                    var loaded = GaiaSky.instance.scripting()
                            .loadJsonCatalog(selectedDataset.name, selectedDataset.checkPath.toAbsolutePath().toString());
                    if (!loaded) {
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                             this,
                                             I18n.msg("gui.dsload.fail", selectedDataset.checkPath.toAbsolutePath()));
                    } else {
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                             this,
                                             I18n.msg("gui.dsload.success", selectedDataset.checkPath.toAbsolutePath()));
                    }
                });
            } else {
                logger.warn("Nothing to load. Accept button should be disabled!");
            }
        } else {
            // File.
            filePicker.result(true);
        }
        return true;
    }

    @Override
    protected void cancel() {
        if (selectedTab == 0) {
            // Datasets.
        } else {
            // File.
            filePicker.result(false);
        }
    }

    @Override
    public void dispose() {

    }


}
