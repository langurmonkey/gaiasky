/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.desktop.util.SysUtils;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.datadesc.DataDescriptor;
import gaiasky.util.datadesc.DataDescriptorUtils;
import gaiasky.util.datadesc.DatasetDesc;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.vr.openvr.VRStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Welcome screen that allows access to the main application, as well as the dataset manager and the catalog selection.
 *
 * @author Toni Sagrista
 */
public class WelcomeGui extends AbstractGui {
    private static final Log logger = Logger.getLogger(WelcomeGui.class);

    private VRStatus vrStatus;
    private boolean skipWelcome;

    protected DownloadDataWindow ddw;
    protected CatalogSelectionWindow cdw;

    private FileHandle dataDescriptor;
    private Array<FileHandle> catalogFiles;

    private boolean downloadError = false;
    private Texture bgTex;
    private DatasetsWidget dw;
    private DataDescriptor dd;

    /** Lock object for synchronisation **/

    /**
     * Creates an initial GUI
     *
     * @param skipWelcome Skips the welcome screen if possible
     * @param vrStatus    The status of VR
     */
    public WelcomeGui(boolean skipWelcome, VRStatus vrStatus) {
        lock = new Object();
        this.skipWelcome = skipWelcome;
        this.vrStatus = vrStatus;
    }

    @Override
    public void initialize(AssetManager assetManager) {

        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
        skin = GlobalResources.skin;

        if (vrStatus.vrInitFailed()) {
            if (vrStatus.equals(VRStatus.ERROR_NO_CONTEXT))
                GaiaSky.postRunnable(() -> GuiUtils.addNoVRConnectionExit(skin, ui));
            else if (vrStatus.equals(VRStatus.ERROR_RENDERMODEL))
                GaiaSky.postRunnable(() -> GuiUtils.addNoVRDataExit(skin, ui));

        } else if (GlobalConf.program.isSlave()) {
            // If slave, data load can start
            gaiaSky();
        } else {
            dw = new DatasetsWidget(skin);
            catalogFiles = dw.buildCatalogFiles();

            // Otherwise, check for updates, etc.
            clearGui();

            dataDescriptor = Gdx.files.absolute(SysUtils.getDefaultTmpDir() + "/gaiasky-data.json");
            DownloadHelper.downloadFile(GlobalConf.program.DATA_DESCRIPTOR_URL, dataDescriptor, null, (digest) -> {
                GaiaSky.postRunnable(() -> {
                    // Data descriptor ok. Skip welcome screen only if flag and basedata present
                    if (skipWelcome && basicDataPresent()) {
                        gaiaSky();
                    } else {
                        buildWelcomeUI();
                    }
                });
            }, () -> {
                // Fail?
                downloadError = true;
                logger.error("No internet connection or server is down! We will attempt to continue");
                if (basicDataPresent()) {
                    // Go on all in
                    GaiaSky.postRunnable(() -> {
                        GuiUtils.addNoConnectionWindow(skin, ui, () -> buildWelcomeUI());
                    });
                } else {
                    // Error and exit
                    logger.error("No base data present - need an internet connection to continue, exiting");
                    GaiaSky.postRunnable(() -> {
                        GuiUtils.addNoConnectionExit(skin, ui);
                    });
                }
            }, null);

            /** CAPTURE SCROLL FOCUS **/
            ui.addListener(event -> {
                if (event instanceof InputEvent) {
                    InputEvent ie = (InputEvent) event;

                    if (ie.getType() == Type.keyUp) {
                        if (ie.getKeyCode() == Input.Keys.ESCAPE) {
                            Gdx.app.exit();
                        } else if (ie.getKeyCode() == Input.Keys.ENTER) {
                            if (basicDataPresent()) {
                                gaiaSky();
                            } else {
                                addDatasetManagerWindow(dd);
                            }
                        }
                    }
                }
                return false;
            });

        }

    }

    private void buildWelcomeUI() {
        dd = !downloadError ? DataDescriptorUtils.instance().buildDatasetsDescriptor(dataDescriptor) : null;
        // Center table
        Table center = new Table(skin);
        center.setFillParent(true);
        center.center();
        if (bgTex == null)
            bgTex = new Texture(Gdx.files.internal("img/splash/splash.jpg"));
        Drawable bg = new SpriteDrawable(new Sprite(bgTex));
        center.setBackground(bg);

        float pad10 = 10f * GlobalConf.UI_SCALE_FACTOR;
        float pad15 = 15f * GlobalConf.UI_SCALE_FACTOR;
        float pad20 = 20f * GlobalConf.UI_SCALE_FACTOR;
        float pad25 = 25f * GlobalConf.UI_SCALE_FACTOR;

        float bw = 350f * GlobalConf.UI_SCALE_FACTOR;
        float bh = 85f * GlobalConf.UI_SCALE_FACTOR;

        int numCatalogsAvailable = numCatalogsAvailable();
        int numGaiaDRCatalogsSelected = numGaiaDRCatalogsSelected();
        int numStarCatalogsSelected = numStarCatalogsSelected();
        int numTotalCatalogsSelected = numTotalCatalogsSelected();
        boolean basicDataPresent = basicDataPresent();

        // Title
        HorizontalGroup titleGroup = new HorizontalGroup();
        titleGroup.space(pad20);
        OwnLabel title = new OwnLabel(I18n.txt("gui.welcome.title", GlobalConf.APPLICATION_NAME, GlobalConf.version.version), skin, "main-title");
        OwnLabel gs = new OwnLabel(GlobalConf.APPLICATION_NAME + " " + GlobalConf.version.version, skin, "main-title");
        gs.setColor(skin.getColor("theme"));
        titleGroup.addActor(title);
        titleGroup.addActor(gs);

        String textStyle = "main-title-s";

        // Start Gaia Sky button
        OwnTextIconButton startButton = new OwnTextIconButton(I18n.txt("gui.welcome.start", GlobalConf.APPLICATION_NAME), skin, "start");
        startButton.setSpace(pad15);
        startButton.setContentAlign(Align.center);
        startButton.align(Align.center);
        startButton.setSize(bw, bh);
        startButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                gaiaSky();
            }
            return true;
        });
        Table startGroup = new Table(skin);
        OwnLabel startLabel = new OwnLabel(I18n.txt("gui.welcome.start.desc", GlobalConf.APPLICATION_NAME), skin, textStyle);
        startGroup.add(startLabel).top().left().padTop(pad10).padBottom(pad10).row();
        if (!basicDataPresent) {
            // No basic data, can't start!
            startButton.setDisabled(true);

            OwnLabel noBaseData = new OwnLabel(I18n.txt("gui.welcome.start.nobasedata"), skin, textStyle);
            noBaseData.setColor(ColorUtils.gRedC);
            startGroup.add(noBaseData).bottom().left();
        } else if (numCatalogsAvailable > 0 && numTotalCatalogsSelected == 0) {
            OwnLabel noCatsSelected = new OwnLabel(I18n.txt("gui.welcome.start.nocatalogs"), skin, textStyle);
            noCatsSelected.setColor(ColorUtils.gRedC);
            startGroup.add(noCatsSelected).bottom().left();
        } else if (numGaiaDRCatalogsSelected > 1 || numStarCatalogsSelected == 0) {
            OwnLabel tooManyDR = new OwnLabel(I18n.txt("gui.welcome.start.check"), skin, textStyle);
            tooManyDR.setColor(ColorUtils.gRedC);
            startGroup.add(tooManyDR).bottom().left();
        } else {
            OwnLabel ready = new OwnLabel(I18n.txt("gui.welcome.start.ready"), skin, textStyle);
            ready.setColor(ColorUtils.gGreenC);
            startGroup.add(ready).bottom().left();

        }

        // Data manager button
        OwnTextIconButton downloadButton = new OwnTextIconButton(I18n.txt("gui.welcome.dsmanager"), skin, "cloud-download");
        downloadButton.setSpace(pad15);
        downloadButton.setContentAlign(Align.center);
        downloadButton.align(Align.center);
        downloadButton.setSize(bw, bh);
        downloadButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                addDatasetManagerWindow(dd);
            }
            return true;
        });
        Table downloadGroup = new Table(skin);
        OwnLabel downloadLabel = new OwnLabel(I18n.txt("gui.welcome.dsmanager.desc"), skin, textStyle);
        downloadGroup.add(downloadLabel).top().left().padTop(pad10).padBottom(pad10);
        if (dd != null && dd.updatesAvailable) {
            downloadGroup.row();
            OwnLabel updates = new OwnLabel(I18n.txt("gui.welcome.dsmanager.updates", dd.numUpdates), skin, textStyle);
            updates.setColor(ColorUtils.gYellowC);
            downloadGroup.add(updates).bottom().left();
        } else if (!basicDataPresent) {
            downloadGroup.row();
            OwnLabel getBasedata = new OwnLabel(I18n.txt("gui.welcome.dsmanager.info"), skin, textStyle);
            getBasedata.setColor(ColorUtils.gGreenC);
            downloadGroup.add(getBasedata).bottom().left();
        }

        // Catalog selection button
        OwnTextIconButton catalogButton = new OwnTextIconButton(I18n.txt("gui.welcome.catalogsel"), skin, "check");
        catalogButton.setSpace(pad15);
        catalogButton.setContentAlign(Align.center);
        catalogButton.align(Align.center);
        catalogButton.setSize(bw, bh);
        catalogButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                String noticeKey;
                if (numCatalogsAvailable() > 0 && numGaiaDRCatalogsSelected() > 1) {
                    noticeKey = "gui.dschooser.morethanonedr";
                } else {
                    noticeKey = "gui.dschooser.nocatselected";
                }
                addCatalogSelectionWindow(noticeKey);
            }
            return true;
        });
        Table catalogGroup = new Table(skin);
        OwnLabel catalogLabel = new OwnLabel(I18n.txt("gui.welcome.catalogsel.desc"), skin, textStyle);
        catalogGroup.add(catalogLabel).top().left().padTop(pad10).padBottom(pad10).row();
        if (numCatalogsAvailable == 0) {
            // No catalog files, disable and add notice
            catalogButton.setDisabled(true);
            OwnLabel noCatalogs = new OwnLabel(I18n.txt("gui.welcome.catalogsel.nocatalogs"), skin, textStyle);
            noCatalogs.setColor(ColorUtils.aOrangeC);
            catalogGroup.add(noCatalogs).bottom().left();
        } else if (numGaiaDRCatalogsSelected > 1) {
            OwnLabel tooManyDR = new OwnLabel(I18n.txt("gui.welcome.catalogsel.manydrcatalogs"), skin, textStyle);
            tooManyDR.setColor(ColorUtils.gRedC);
            catalogGroup.add(tooManyDR).bottom().left();
        } else if (numStarCatalogsSelected == 0) {
            OwnLabel noStarCatalogs = new OwnLabel(I18n.txt("gui.welcome.catalogsel.nostarcatalogs"), skin, textStyle);
            noStarCatalogs.setColor(ColorUtils.aOrangeC);
            catalogGroup.add(noStarCatalogs).bottom().left();

        } else {
            OwnLabel ok = new OwnLabel(I18n.txt("gui.welcome.catalogsel.selected", numTotalCatalogsSelected(), numCatalogsAvailable()), skin, textStyle);
            ok.setColor(ColorUtils.gBlueC);
            catalogGroup.add(ok).bottom().left();
        }

        // Exit button
        OwnTextIconButton quitButton = new OwnTextIconButton(I18n.txt("gui.exit"), skin, "quit");
        quitButton.setSpace(pad10);
        quitButton.align(Align.center);
        quitButton.setSize(bw * 0.5f, bh * 0.6f);
        quitButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        center.add(titleGroup).center().padBottom(pad15 * 5f).colspan(2).row();
        center.add(startButton).center().top().padBottom(pad15 * 4f).padRight(pad25);
        center.add(startGroup).top().left().padBottom(pad15 * 4f).row();
        center.add(downloadButton).center().top().padBottom(pad20).padRight(pad25);
        center.add(downloadGroup).left().top().padBottom(pad20).row();
        center.add(catalogButton).center().top().padBottom(pad15 * 8f).padRight(pad25);
        center.add(catalogGroup).left().top().padBottom(pad15 * 8f).row();
        center.add(quitButton).center().top().colspan(2);

        // Bottom left table
        Table bottomRight = new Table();
        bottomRight.setFillParent(true);
        bottomRight.right().bottom();
        bottomRight.pad(pad10);
        bottomRight.add(new OwnLabel(GlobalConf.version.version + " - build " + GlobalConf.version.build, skin, "hud-med"));

        ui.addActor(center);
        ui.addActor(bottomRight);

    }

    private void gaiaSky() {
        if (bgTex != null)
            bgTex.dispose();
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
        EventManager.instance.post(Events.LOAD_DATA_CMD);
    }

    /**
     * Reloads the view completely
     */
    private void reloadView() {
        if (dw == null) {
            dw = new DatasetsWidget(skin);
        }
        catalogFiles = dw.buildCatalogFiles();
        clearGui();
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
        buildWelcomeUI();
    }

    private boolean isCatalogSelected() {
        return GlobalConf.data.CATALOG_JSON_FILES != null && GlobalConf.data.CATALOG_JSON_FILES.size > 0;
    }

    private int numTotalCatalogsSelected() {
        return GlobalConf.data.CATALOG_JSON_FILES.size;
    }

    private int numCatalogsAvailable() {
        return catalogFiles.size;
    }

    private int numGaiaDRCatalogsSelected() {
        int matches = 0;
        for (String f : GlobalConf.data.CATALOG_JSON_FILES) {
            String filename = Path.of(f).getFileName().toString();
            if (isGaiaDRCatalogFile(filename)) {
                matches++;
            }
        }
        return matches;
    }

    private boolean isGaiaDRCatalogFile(String name) {
        return name.matches("^\\S*catalog-[e]?dr\\d+(int\\d+)?-\\S+(\\.json)$");
    }

    private int numStarCatalogsSelected() {
        int matches = 0;
        for (String f : GlobalConf.data.CATALOG_JSON_FILES) {
            // File name with no extension
            Path path = Path.of(f);
            String filenameExt = path.getFileName().toString();
            try {
                DatasetDesc dataset = dd.findDatasetByDescriptor(path);
                if ((dataset != null && dataset.isStarDataset()) || isGaiaDRCatalogFile(filenameExt)) {
                    matches++;
                }
            } catch (IOException e) {
                logger.error(e);
            }
        }
        return matches;
    }

    /**
     * Checks if the basic Gaia Sky data folders are present
     * in the default data folder
     *
     * @return True if basic data is found
     */
    private boolean basicDataPresent() {
        Array<Path> required = new Array<>();
        fillBasicDataFiles(required);

        for (Path p : required) {
            if (!Files.exists(p) || !Files.isReadable(p)) {
                logger.info("Data files not found: " + p.toString());
                return false;
            }
        }

        return true;
    }

    private void fillBasicDataFiles(Array<Path> required) {
        Path dataPath = Paths.get(GlobalConf.data.DATA_LOCATION).normalize();
        required.add(dataPath.resolve("data-main.json"));
        required.add(dataPath.resolve("asteroids.json"));
        required.add(dataPath.resolve("planets.json"));
        required.add(dataPath.resolve("satellites.json"));
        required.add(dataPath.resolve("tex/base"));
        required.add(dataPath.resolve("attitudexml"));
        required.add(dataPath.resolve("orbit"));
        required.add(dataPath.resolve("oort"));
        required.add(dataPath.resolve("galaxy"));
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
    }

    private void addDatasetManagerWindow(DataDescriptor dd) {
        if (ddw == null) {
            ddw = new DownloadDataWindow(ui, skin, dd);
            ddw.setAcceptRunnable(() -> {
                Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                reloadView();
            });
        }
        ddw.show(ui);
    }

    private void addCatalogSelectionWindow(String noticeKey) {
        if (cdw == null) {
            cdw = new CatalogSelectionWindow(ui, skin, noticeKey);
            cdw.setAcceptRunnable(() -> {
                Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                reloadView();
            });
        } else {
            cdw.refresh();
        }
        cdw.show(ui);
    }

    public void clearGui() {
        if (ui != null) {
            ui.clear();
        }
        if (ddw != null) {
            ddw.remove();
        }
        if (cdw != null) {
            cdw.remove();
        }
    }

    @Override
    protected void rebuildGui() {

    }

}
