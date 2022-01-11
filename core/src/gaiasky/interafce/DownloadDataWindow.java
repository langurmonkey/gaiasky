/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.desktop.util.SysUtils;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.datadesc.DataDescriptor;
import gaiasky.util.datadesc.DataDescriptorUtils;
import gaiasky.util.datadesc.DatasetDesc;
import gaiasky.util.datadesc.DatasetType;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;
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
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Download manager. It gets a descriptor file from the server containing all
 * available datasets, detects them in the current system and offers and manages
 * their downloads.
 */
public class DownloadDataWindow extends GenericDialog {
    private static final Log logger = Logger.getLogger(DownloadDataWindow.class);

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
        iconMap.put("catalog-other", "icon-elem-others");
        iconMap.put("mesh", "icon-elem-meshes");
        iconMap.put("texture-pack", "icon-elem-moons");
    }

    public static int getTypeWeight(String type) {
        return switch (type) {
            case "catalog-lod" -> 0;
            case "catalog-gaia" -> 1;
            case "catalog-star" -> 2;
            case "catalog-gal" -> 3;
            case "catalog-cluster" -> 4;
            case "catalog-other" -> 5;
            case "mesh" -> 6;
            case "other" -> 8;
            default -> 10;
        };
    }

    public static String getIcon(String type) {
        if (type != null && iconMap.containsKey(type))
            return iconMap.get(type);
        return "icon-elem-others";
    }

    private DataDescriptor dd;
    private OwnTextButton downloadButton;
    private OwnProgressBar downloadProgress;
    private OwnLabel currentDownloadFile, downloadSpeed;
    private OwnScrollPane datasetsScroll;
    private Cell<?> cancelCell;
    private float scrollX = 0f, scrollY = 0f;

    private final Color highlight;

    // Whether to show the data location chooser
    private final boolean dataLocation;

    private final INumberFormat nf;
    private final List<Trio<DatasetDesc, OwnCheckBox, OwnLabel>> choiceList;
    private Array<Trio<DatasetDesc, OwnCheckBox, OwnLabel>> toDownload;
    private final Array<OwnImageButton> rubbishes;
    private int current = -1;

    private final Set<DatasetDesc> downloaded;

    public DownloadDataWindow(Stage stage, Skin skin, DataDescriptor dd) {
        this(stage, skin, dd, true, I18n.txt("gui.ok"));
    }

    public DownloadDataWindow(Stage stage, Skin skin, DataDescriptor dd, boolean dataLocation, String acceptText) {
        super(I18n.txt("gui.download.title") + (dd.updatesAvailable ? " - " + I18n.txt("gui.download.updates", dd.numUpdates) : ""), skin, stage);
        this.nf = NumberFormatFactory.getFormatter("##0.0");
        this.dd = dd;
        this.choiceList = new LinkedList<>();
        this.rubbishes = new Array<>();
        this.highlight = ColorUtils.gYellowC;
        this.downloaded = new HashSet<>();

        this.dataLocation = dataLocation;

        setAcceptText(acceptText);

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        me.acceptButton.setDisabled(false);
        float pad = 3.2f;
        float padLarge = 14.4f;
        float minW = 800f;
        float tabWidth = 240f;
        float width = 1800f;

        // Tabs
        HorizontalGroup tabGroup = new HorizontalGroup();
        tabGroup.align(Align.center);

        final Button tabAvail = new OwnTextButton(I18n.txt("gui.download.tab.available"), skin, "toggle-big");
        tabAvail.pad(pad5);
        tabAvail.setWidth(tabWidth);
        final Button tabInstalled = new OwnTextButton(I18n.txt("gui.download.tab.installed"), skin, "toggle-big");
        tabInstalled.pad(pad5);
        tabInstalled.setWidth(tabWidth);

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

        content.add(tabContent).expand().fill().padBottom(pad20 * 3f).row();

        // Listen to changes in the tab button checked states
        // Set visibility of the tab content to match the checked state
        ChangeListener tabListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (tabAvail.isChecked()) {
                    reloadAvailable(contentAvail, width);
                }
                if (tabInstalled.isChecked()) {
                    reloadInstalled(contentInstalled, width);
                }
                contentAvail.setVisible(tabAvail.isChecked());
                contentInstalled.setVisible(tabInstalled.isChecked());

                content.pack();
            }
        };
        tabAvail.addListener(tabListener);
        tabInstalled.addListener(tabListener);

        tabAvail.setChecked(true);

        // Let only one tab button be checked at a time
        ButtonGroup<Button> tabs = new ButtonGroup<>();
        tabs.setMinCheckCount(1);
        tabs.setMaxCheckCount(1);
        tabs.add(tabAvail);
        tabs.add(tabInstalled);

        float buttonPad = 1.6f;
        Cell<Actor> topCell = content.add((Actor) null).left().top();
        topCell.row();

        // Offer downloads
        Table downloadTable = new Table(skin);
        downloadTable.setFillParent(true);

        OwnLabel catalogsLocLabel = new OwnLabel(I18n.txt("gui.download.location"), skin);
        OwnImageButton catalogsLocTooltip = new OwnImageButton(skin, "tooltip");
        catalogsLocTooltip.addListener(new OwnTextTooltip(I18n.txt("gui.download.location.info"), skin));
        HorizontalGroup catalogsLocGroup = new HorizontalGroup();
        catalogsLocGroup.space(pad10);
        catalogsLocGroup.addActor(catalogsLocLabel);
        catalogsLocGroup.addActor(catalogsLocTooltip);

        HorizontalGroup hg = new HorizontalGroup();
        hg.space(18f);
        Image system = new Image(skin.getDrawable("tooltip-icon"));
        OwnLabel downloadInfo = new OwnLabel(I18n.txt("gui.download.info"), skin);
        hg.addActor(system);
        hg.addActor(downloadInfo);

        downloadTable.add(hg).left().colspan(2).padBottom(padLarge).row();

        try {
            Files.createDirectories(SysUtils.getLocalDataDir());
        } catch (FileAlreadyExistsException e) {
            // Good
        } catch (IOException e) {
            logger.error(e);
            return;
        }
        String catLoc = Settings.settings.data.location;

        if (dataLocation) {
            OwnTextButton dataLocationButton = new OwnTextButton(catLoc, skin);
            dataLocationButton.pad(buttonPad * 4f);
            dataLocationButton.setMinWidth(minW);
            downloadTable.add(catalogsLocGroup).left().padBottom(padLarge);
            downloadTable.add(dataLocationButton).left().padLeft(pad).padBottom(padLarge).row();
            Cell<Actor> notice = downloadTable.add((Actor) null).colspan(2).padBottom(padLarge);
            notice.row();

            dataLocationButton.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    FileChooser fc = new FileChooser(I18n.txt("gui.download.pickloc"), skin, stage, Path.of(Settings.settings.data.location), FileChooser.FileChooserTarget.DIRECTORIES);
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
                                    Settings.settings.data.catalogFiles.clear();
                                    reloadAll();
                                    GaiaSky.instance.getGlobalResources().reloadDataFiles();
                                });
                            } else {
                                Label warn = new OwnLabel(I18n.txt("gui.download.pickloc.permissions"), skin);
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

        // Uploads available?
        if (dd.updatesAvailable) {
            OwnLabel updates = new OwnLabel(I18n.txt("gui.download.updates", dd.numUpdates), skin, "hud-big");
            updates.setColor(highlight);
            downloadTable.add(updates).colspan(2).center().padBottom(padLarge).row();
        }

        // Build datasets table
        final Table datasetsTable = new Table(skin);
        datasetsTable.align(Align.topLeft);

        for (DatasetType type : dd.types) {
            List<DatasetDesc> datasets = type.datasets;
            OwnLabel dsType = new OwnLabel(I18n.txt("gui.download.type." + type.typeStr), skin, "hud-header");
            OwnImageButton expandIcon = new OwnImageButton(skin, "expand-collapse");
            expandIcon.setChecked(true);
            HorizontalGroup titleGroup = new HorizontalGroup();
            titleGroup.space(padLarge);
            titleGroup.addActor(expandIcon);
            titleGroup.addActor(dsType);
            datasetsTable.add(titleGroup).top().left().padBottom(pad * 3f).padTop(padLarge * 2f).row();

            Table t = new Table(skin);
            Cell<Table> c = datasetsTable.add(t).colspan(2).left();
            c.row();

            expandIcon.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    if (c.getActor() == null) {
                        c.setActor(t);
                    } else {
                        c.setActor(null);
                    }
                    datasetsTable.pack();
                    me.pack();
                    return true;
                }
                return false;
            });

            boolean haveAll = true;
            for (DatasetDesc dataset : datasets) {
                haveAll = haveAll && dataset.exists && !dataset.outdated;
                // Check if dataset requires a minimum version of Gaia Sky

                // Add dataset to desc table
                OwnCheckBox cb = new OwnCheckBox(TextUtils.capString(dataset.shortDescription, 33), skin, "large", pad * 2f);
                cb.addListener(new OwnTextTooltip(dataset.shortDescription, skin, 10));
                cb.left();
                cb.setMinWidth(420f);
                cb.setChecked(dataset.mustDownload);
                cb.setDisabled(dataset.cbDisabled);
                cb.addListener((event) -> {
                    if (event instanceof ChangeEvent) {
                        updateDatasetsSelected();
                        return true;
                    }
                    return false;
                });
                OwnLabel haveIt = new OwnLabel("", skin);
                haveIt.setWidth(75f);
                if (dataset.exists) {
                    if (dataset.outdated) {
                        setStatusOutdated(dataset, haveIt);
                    } else {
                        setStatusFound(dataset, haveIt);
                    }
                } else {
                    setStatusNotFound(dataset, haveIt);
                }

                // Can't proceed without base data - force download
                if (dataset.baseData && !dataset.exists) {
                    me.acceptButton.setDisabled(true);
                }

                // Description
                HorizontalGroup descGroup = new HorizontalGroup();
                descGroup.space(padLarge);
                OwnLabel desc = new OwnLabel(dataset.name, skin);
                desc.addListener(new OwnTextTooltip(dataset.description, skin, 10));
                desc.setWidth(336f);
                // Info
                OwnImageButton imgTooltip = new OwnImageButton(skin, "tooltip");
                imgTooltip.addListener(new OwnTextTooltip(dataset.description, skin, 10));
                descGroup.addActor(imgTooltip);
                descGroup.addActor(desc);
                // Link
                if (dataset.link != null) {
                    String link = dataset.link.replace("@mirror-url@", Settings.settings.program.url.dataMirror);
                    LinkButton imgLink = new LinkButton(link, skin);
                    descGroup.addActor(imgLink);
                } else {
                    Image emptyImg = new Image(skin, "iconic-empty");
                    descGroup.addActor(emptyImg);

                }

                // Version
                String vstring;
                if (!dataset.exists) {
                    vstring = "-";
                } else if (dataset.outdated) {
                    vstring = dataset.myVersion + " -> v-" + dataset.serverVersion;
                } else {
                    vstring = "v-" + dataset.myVersion;
                }

                OwnLabel vers = new OwnLabel(vstring, skin);
                vers.setWidth(128f);
                if (!dataset.exists) {
                    vers.addListener(new OwnTextTooltip(I18n.txt("gui.download.version.server", Integer.toString(dataset.serverVersion)), skin, 10));
                } else if (dataset.outdated) {
                    // New version!
                    vers.setColor(highlight);
                    vers.addListener(new OwnTextTooltip(I18n.txt("gui.download.version.new", Integer.toString(dataset.serverVersion), Integer.toString(dataset.myVersion)), skin, 10));
                } else {
                    vers.addListener(new OwnTextTooltip(I18n.txt("gui.download.version.ok"), skin, 10));
                }

                // Type icon
                Image typeImage = new OwnImage(skin.getDrawable(getIcon(dataset.type)));
                float scl = 0.7f;
                float iw = typeImage.getWidth();
                float ih = typeImage.getHeight();
                typeImage.setSize(iw * scl, ih * scl);
                typeImage.addListener(new OwnTextTooltip(dataset.type, skin, 10));

                // Size
                OwnLabel size = new OwnLabel(dataset.size, skin);
                size.addListener(new OwnTextTooltip(I18n.txt("gui.download.size.tooltip"), skin, 10));
                size.setWidth(88f);

                // Objects
                OwnLabel nObjects = new OwnLabel(dataset.nObjectsStr, skin);
                nObjects.addListener(new OwnTextTooltip(I18n.txt("gui.download.nobjects.tooltip"), skin, 10));
                nObjects.setWidth(192f);

                // Delete
                OwnImageButton rubbish = null;
                if (dataset.exists) {
                    rubbish = new OwnImageButton(skin, "rubbish-bin");
                    rubbish.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.remove"), skin));
                    rubbish.addListener((event) -> {
                        if (event instanceof ChangeEvent) {
                            // Remove dataset
                            if (dataset.filesToDelete != null) {
                                for (String fileToDelete : dataset.filesToDelete) {
                                    try {
                                        if (fileToDelete.endsWith("/")) {
                                            fileToDelete = fileToDelete.substring(0, fileToDelete.length() - 1);
                                        }
                                        // Expand possible wildcards
                                        String basePath = "";
                                        String baseName = fileToDelete;
                                        if (fileToDelete.contains("/")) {
                                            basePath = fileToDelete.substring(0, fileToDelete.lastIndexOf('/'));
                                            baseName = fileToDelete.substring(fileToDelete.lastIndexOf('/') + 1);
                                        }
                                        Path dataPath = Paths.get(Settings.settings.data.location);
                                        dataPath = dataPath.toRealPath();
                                        File directory = dataPath.resolve(basePath).toFile();
                                        Collection<File> files = FileUtils.listFilesAndDirs(directory, new WildcardFileFilter(baseName), new WildcardFileFilter(baseName));
                                        for (File file : files) {
                                            if (!file.equals(directory) && file.exists()) {
                                                FileUtils.forceDelete(file);
                                            }
                                        }
                                        // Remove from downloaded list, if it is there
                                        downloaded.remove(dataset);
                                    } catch (Exception e) {
                                        logger.error(e);
                                    }
                                }
                            } else {
                                // Only remove "check"
                                try {
                                    FileUtils.forceDelete(dataset.check.toFile());
                                } catch (IOException e) {
                                    logger.error(e);
                                }
                            }
                            // RELOAD DATASETS VIEW
                            GaiaSky.postRunnable(this::reloadAll);

                            return true;
                        }
                        return false;
                    });
                    rubbishes.add(rubbish);
                }
                t.add(cb).left().padLeft(padLarge * 2f).padRight(padLarge).padBottom(pad);
                t.add(descGroup).left().padRight(padLarge).padBottom(pad);
                t.add(vers).center().padRight(padLarge).padBottom(pad);
                t.add(typeImage).center().padRight(padLarge).padBottom(pad);
                t.add(size).left().padRight(padLarge).padBottom(pad);
                t.add(nObjects).left().padRight(padLarge).padBottom(pad);
                t.add(haveIt).left().padBottom(pad);
                if (dataset.exists) {
                    t.add(rubbish).center().padLeft(padLarge * 2.5f);
                }
                t.row();

                choiceList.add(new Trio<>(dataset, cb, haveIt));
            }
            expandIcon.setChecked(!haveAll);

        }

        datasetsScroll = new OwnScrollPane(datasetsTable, skin, "minimalist-nobg");
        datasetsScroll.setScrollingDisabled(true, false);
        datasetsScroll.setForceScroll(false, true);
        datasetsScroll.setSmoothScrolling(false);
        datasetsScroll.setFadeScrollBars(false);

        downloadTable.add(datasetsScroll).top().center().padLeft(pad20).padRight(pad20).padBottom(padLarge).colspan(2).row();

        // Current dataset info
        currentDownloadFile = new OwnLabel("", skin, "hotkey");
        downloadTable.add(currentDownloadFile).center().colspan(2).padBottom(padLarge).row();

        // Download button
        downloadButton = new OwnTextButton(I18n.txt("gui.download.download"), skin, "download");
        downloadButton.pad(buttonPad * 4f);
        downloadButton.setMinWidth(minW);
        downloadButton.setMinHeight(80f);
        downloadTable.add(downloadButton).center().colspan(2).padBottom(0f).row();

        // Progress bar
        downloadProgress = new OwnProgressBar(0, 100, 0.1f, false, skin, "default-horizontal");
        downloadProgress.setValue(0);
        downloadProgress.setVisible(false);
        downloadProgress.setPrefWidth(minW);
        downloadTable.add(downloadProgress).center().colspan(2).padBottom(padLarge).row();

        // Download info and cancel
        Table infoCancel = new Table(skin);
        downloadSpeed = new OwnLabel("", skin);
        downloadSpeed.setVisible(false);
        downloadSpeed.setWidth(300f);
        infoCancel.add(downloadSpeed).center().padRight(padLarge * 2f);
        cancelCell = infoCancel.add();
        downloadTable.add(infoCancel).colspan(2).padBottom(padLarge).row();

        downloadButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                downloadAndExtractFiles(choiceList);
            }
            return true;
        });

        // Progress

        // External download link
        Link manualDownload = new Link(I18n.txt("gui.download.manual"), skin, "link", "http://gaia.ari.uni-heidelberg.de/gaiasky/files/autodownload");
        downloadTable.add(manualDownload).center().colspan(2);

        //topCell.setActor(downloadTable);

        downloadTable.pack();
        datasetsScroll.setWidth(Math.min(stage.getWidth() * 0.9f, 1620f));
        datasetsScroll.setHeight(Math.min(stage.getHeight() * 0.5f, 1500f));

        // Update selected
        updateDatasetsSelected();
    }

    private void reloadAvailable(Table content, float width) {
        List<DatasetDesc> available = dd.datasets.stream().filter(d -> !d.exists).collect(Collectors.toList());
        reloadPane(content, width, available);
    }

    private void reloadInstalled(Table content, float width) {
        List<DatasetDesc> installed = dd.datasets.stream().filter(d -> d.exists).collect(Collectors.toList());
        reloadPane(content, width, installed);
    }

    private void reloadPane(Table content, float width, List<DatasetDesc> datasets) {
        content.clear();
        Cell left = content.add().top().left().padRight(pad10);
        Cell right = content.add().top().left();

        Table leftTable = new Table(skin);
        leftTable.align(Align.topRight);
        OwnScrollPane leftScroll = new OwnScrollPane(leftTable, skin, "minimalist-nobg");
        leftScroll.setScrollingDisabled(true, false);
        leftScroll.setForceScroll(false, true);
        leftScroll.setSmoothScrolling(false);
        leftScroll.setFadeScrollBars(false);
        for (DatasetDesc dd : datasets) {
            Table t = new Table(skin);
            t.pad(pad10);
            OwnLabel title = new OwnLabel(dd.shortDescription, skin, "ui-23");
            title.setWidth(width * 0.43f);
            t.add(title).left().padRight(pad10);
            Link link = new Link("INSTALL", skin, "https://tonisagrista.com");
            link.setWidth(width * 0.3f);
            t.add(link).right().row();
            t.add(new OwnLabel(I18n.txt("gui.download.version.server", dd.serverVersion), skin, "menuitem-shortcut")).left();
            t.pack();
            OwnButton button = new OwnButton(t, skin, "dataset", false);
            button.setWidth(width * 0.52f);
            leftTable.add(button).row();
        }
        leftScroll.setWidth(width * 0.52f);
        leftScroll.setHeight(Math.min(stage.getHeight() * 0.5f, 1500f));
        left.setActor(leftScroll);

        OwnLabel desc = new OwnLabel("Here is a text\nThat is nice to have\nPlease update it\nAs long as it lasts.", skin);
        desc.setWidth(width * 0.5f);
        right.setActor(desc);

        content.pack();
    }

    private void updateDatasetsSelected() {
        int nSelected = 0;
        for (Trio<DatasetDesc, OwnCheckBox, OwnLabel> choice : choiceList) {
            if (choice.getSecond().isChecked()) {
                nSelected++;
            }
        }

        if (nSelected <= 0) {
            currentDownloadFile.setText(I18n.txt("gui.download.selected.none"));
            downloadButton.setDisabled(true);
        } else if (nSelected == 1) {
            currentDownloadFile.setText(I18n.txt("gui.download.selected.one"));
            downloadButton.setDisabled(false);
        } else {
            currentDownloadFile.setText(I18n.txt("gui.download.selected.more", nSelected));
            downloadButton.setDisabled(false);
        }
    }

    private synchronized void downloadAndExtractFiles(List<Trio<DatasetDesc, OwnCheckBox, OwnLabel>> choices) {
        toDownload = new Array<>();

        for (Trio<DatasetDesc, OwnCheckBox, OwnLabel> entry : choices) {
            if (entry.getSecond().isChecked())
                toDownload.add(entry);
        }

        // Disable all checkboxes
        setDisabled(choices, true);
        // Disable all rubbishes
        setDisabled(rubbishes, true);

        logger.info(toDownload.size + " new data files selected to download");

        current = -1;
        downloadNext();

    }

    public void refresh() {
        content.clear();
        bottom.clear();
        build();
    }

    private void downloadNext() {
        current++;
        downloadCurrent();
    }

    private void downloadCurrent() {
        if (current >= 0 && current < toDownload.size) {
            // Download next
            Trio<DatasetDesc, OwnCheckBox, OwnLabel> trio = toDownload.get(current);
            DatasetDesc currentDataset = trio.getFirst();
            String name = currentDataset.name;
            String url = currentDataset.file.replace("@mirror-url@", Settings.settings.program.url.dataMirror);

            String filename = FilenameUtils.getName(url);
            FileHandle tempDownload = Gdx.files.absolute(SysUtils.getTempDir(Settings.settings.data.location) + "/" + filename + ".part");

            ProgressRunnable progressDownload = (read, total, progress, speed) -> {
                double readMb = (double) read / 1e6d;
                double totalMb = (double) total / 1e6d;
                final String progressString = progress >= 100 ? I18n.txt("gui.done") : I18n.txt("gui.download.downloading", nf.format(progress));
                double mbPerSecond = speed / 1000d;
                final String speedString = nf.format(readMb) + "/" + nf.format(totalMb) + " MB   -   " + nf.format(mbPerSecond) + " MB/s";
                // Since we are downloading on a background thread, post a runnable to touch UI
                GaiaSky.postRunnable(() -> {
                    downloadButton.setText(progressString);
                    downloadProgress.setValue((float) progress);
                    downloadSpeed.setText(speedString);
                });
            };
            ProgressRunnable progressHashResume = (read, total, progress, speed) -> {
                double readMb = (double) read / 1e6d;
                double totalMb = (double) total / 1e6d;
                final String progressString = progress >= 100 ? I18n.txt("gui.done") : I18n.txt("gui.download.checksumming", nf.format(progress));
                double mbPerSecond = speed / 1000d;
                final String speedString = nf.format(readMb) + "/" + nf.format(totalMb) + " MB   -   " + nf.format(mbPerSecond) + " MB/s";
                // Since we are downloading on a background thread, post a runnable to touch UI
                GaiaSky.postRunnable(() -> {
                    downloadButton.setText(progressString);
                    downloadProgress.setValue((float) progress);
                    downloadSpeed.setText(speedString);
                });
            };

            ChecksumRunnable finish = (digest) -> {
                // Unpack
                int errors = 0;
                logger.info("Extracting: " + tempDownload.path());
                String dataLocation = Settings.settings.data.location + File.separatorChar;
                // Checksum
                if (digest != null && currentDataset.sha256 != null) {
                    String serverDigest = currentDataset.sha256;
                    try {
                        boolean ok = serverDigest.equals(digest);
                        if (ok) {
                            logger.info("SHA256 ok: " + name);
                        } else {
                            logger.error("SHA256 check failed: " + name);
                            errors++;
                        }
                    } catch (Exception e) {
                        logger.info("Error checking SHA256: " + name);
                        errors++;
                    }
                } else {
                    logger.info("No digest found for dataset: " + name);
                }

                if (errors == 0) {
                    try {
                        // Extract
                        decompress(tempDownload.path(), new File(dataLocation), downloadButton, downloadProgress);
                        // Remove archive
                        cleanupTempFile(tempDownload.path());
                    } catch (Exception e) {
                        logger.error(e, "Error decompressing: " + name);
                        errors++;
                    }
                }

                // Done
                GaiaSky.postRunnable(() -> {
                    downloadButton.setText(I18n.txt("gui.download.download"));
                    downloadProgress.setValue(0);
                    downloadProgress.setVisible(false);
                    downloadSpeed.setText("");
                    downloadSpeed.setVisible(false);
                    cancelCell.setActor(null);
                    me.acceptButton.setDisabled(false);
                    // Enable all
                    setDisabled(choiceList, false);
                    // Enable rubbishes
                    setDisabled(rubbishes, false);
                });

                if (errors == 0) {
                    // Add to downloaded list for later selection
                    downloaded.add(trio.getFirst());
                    // Ok message
                    setMessageOk(I18n.txt("gui.download.idle"));
                    setStatusFound(currentDataset, trio.getThird());

                    GaiaSky.postRunnable(this::downloadNext);
                } else {
                    logger.info("Error getting dataset: " + name);
                    setStatusError(currentDataset, trio.getThird());
                    setMessageError(I18n.txt("gui.download.failed", name));
                }

            };

            Runnable fail = () -> {
                logger.error("Download failed: " + name);
                setStatusError(currentDataset, trio.getThird());
                setMessageError(I18n.txt("gui.download.failed", name));
                me.acceptButton.setDisabled(false);
                downloadProgress.setVisible(false);
                downloadSpeed.setVisible(false);
                cancelCell.setActor(null);
                GaiaSky.postRunnable(this::downloadNext);
            };

            Runnable cancel = () -> {
                logger.error(I18n.txt("gui.download.cancelled", name));
                setStatusCancelled(currentDataset, trio.getThird());
                setMessageError(I18n.txt("gui.download.cancelled", name));
                me.acceptButton.setDisabled(false);
                downloadProgress.setVisible(false);
                downloadSpeed.setVisible(false);
                cancelCell.setActor(null);
                GaiaSky.postRunnable(this::downloadNext);
            };

            // Download
            downloadButton.setDisabled(true);
            me.acceptButton.setDisabled(true);
            downloadProgress.setVisible(true);
            downloadSpeed.setVisible(true);
            setStatusProgress(trio.getThird());
            setMessageOk(I18n.txt("gui.download.downloading.info", (current + 1), toDownload.size, currentDataset.name));
            final Net.HttpRequest request = DownloadHelper.downloadFile(url, tempDownload, progressDownload, progressHashResume, finish, fail, cancel);

            // Cancel button
            OwnTextButton cancelDownloadButton = new OwnTextButton(I18n.txt("gui.download.cancel"), skin);
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
            cancelCell.setActor(cancelDownloadButton);
        } else {
            // Finished all downloads!
            // RELOAD DATASETS VIEW
            GaiaSky.postRunnable(this::reloadAll);
        }
    }

    private void setDisabled(Array<OwnImageButton> l, boolean disabled) {
        for (OwnImageButton b : l)
            b.setDisabled(disabled);
    }

    private void setDisabled(List<Trio<DatasetDesc, OwnCheckBox, OwnLabel>> choices, boolean disabled) {
        for (Trio<DatasetDesc, OwnCheckBox, OwnLabel> t : choices) {
            // Uncheck all if enabling again
            if (!disabled)
                t.getSecond().setChecked(false);
            // Only enable datasets which we don't have
            if (disabled || (!t.getThird().getText().toString().equals(I18n.txt("gui.download.status.found"))))
                t.getSecond().setDisabled(disabled);
        }
    }

    private void decompress(String in, File out, OwnTextButton b, ProgressBar p) throws Exception {
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
            if (!parent.exists())
                parent.mkdirs();

            IOUtils.copy(tarIs, new FileOutputStream(curFile));

            // Every 250 ms we update the view
            long current = System.currentTimeMillis();
            long elapsed = current - last;
            if (elapsed > 250) {
                GaiaSky.postRunnable(() -> {
                    float val = (float) ((fIs.getBytesRead() / 1000d) / sizeKb);
                    b.setText(I18n.txt("gui.download.extracting", nf.format(fIs.getBytesRead() / 1000d) + "/" + sizeKbStr + " Kb"));
                    p.setValue(val * 100);
                });
                last = current;
            }

        }
    }

    /**
     * Returns the file size
     *
     * @param inputFilePath A file
     * @return The size in bytes
     */
    private long fileSize(String inputFilePath) {
        return new File(inputFilePath).length();
    }

    /**
     * Returns the GZ uncompressed size
     *
     * @param inputFilePath A gzipped file
     * @return The uncompressed size in bytes
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
            try {
                final Stream<Path> stream = java.nio.file.Files.find(tempDir, 2, (path, basicFileAttributes) -> {
                    final File file = path.toFile();
                    return !file.isDirectory() && file.getName().endsWith("tar.gz.part");
                });
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

    private void setStatusOutdated(DatasetDesc ds, OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.outdated"));
        label.setColor(ColorUtils.gYellowC);
        if (ds.releaseNotes != null && !ds.releaseNotes.isEmpty()) {
            label.setText(label.getText());
            label.addListener(new OwnTextTooltip(I18n.txt("gui.download.releasenotes", ds.releaseNotes), skin, 10));
        }
    }

    private void setStatusFound(DatasetDesc ds, OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.found"));
        label.setColor(ColorUtils.gGreenC);
    }

    private void setStatusNotFound(DatasetDesc ds, OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.notfound"));
        label.setColor(ColorUtils.gRedC);
    }

    private void setStatusError(DatasetDesc ds, OwnLabel label) {
        label.setText("Failed");
        label.setText(I18n.txt("gui.download.status.failed"));
        label.setColor(ColorUtils.gRedC);
    }

    private void setStatusCancelled(DatasetDesc ds, OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.cancelled"));
        label.setColor(ColorUtils.gRedC);
    }

    private void setStatusProgress(OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.working"));
        label.setColor(ColorUtils.gBlueC);
    }

    private void setMessageOk(String text) {
        currentDownloadFile.setText(text);
        currentDownloadFile.setColor(currentDownloadFile.getStyle().fontColor);
    }

    private void setMessageError(String text) {
        currentDownloadFile.setText(text);
        currentDownloadFile.setColor(ColorUtils.gRedC);
    }

    @Override
    protected void accept() {
        // Select downloaded catalogs
        for (DatasetDesc dd : downloaded) {
            if (dd.type.startsWith("catalog")) {
                Settings.settings.data.addSelectedCatalog(dd.check);
            }
        }
    }

    @Override
    protected void cancel() {
    }

    @Override
    public void dispose() {

    }

    private void backupScrollValues() {
        if (datasetsScroll != null) {
            this.scrollY = datasetsScroll.getScrollY();
            this.scrollX = datasetsScroll.getScrollX();
        }
    }

    private void restoreScrollValues() {
        if (datasetsScroll != null) {
            GaiaSky.postRunnable(() -> {
                datasetsScroll.setScrollX(scrollX);
                datasetsScroll.setScrollY(scrollY);
            });

        }
    }

    @Override
    public void setKeyboardFocus() {
        stage.setKeyboardFocus(downloadButton);
    }

    /**
     * Drops the current view and regenerates all window content
     */
    private void reloadAll() {
        if (choiceList != null)
            choiceList.clear();
        if (toDownload != null)
            toDownload.clear();
        dd = DataDescriptorUtils.instance().buildDatasetsDescriptor(null);
        backupScrollValues();
        content.clear();
        build();
        pack();
        restoreScrollValues();
    }

}
