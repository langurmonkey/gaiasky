/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.BufferUtils;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import gaiasky.util.update.VersionCheckEvent;
import gaiasky.util.update.VersionChecker;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

import java.nio.IntBuffer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;

/**
 * The help window with About, Help and System sections.
 */
public class AboutWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(AboutWindow.class);

    private final LabelStyle linkStyle;
    private Table checkTable;
    private OwnLabel checkLabel;
    private MemInfoWindow memInfoWindow;

    public AboutWindow(Stage stage, Skin skin) {
        super(I18n.msg("gui.help.help") + " - " + Settings.settings.version.version + " - " + I18n.msg("gui.build", Settings.settings.version.build), skin, stage);
        this.linkStyle = skin.get("link", LabelStyle.class);

        setCancelText(I18n.msg("gui.close"));

        // Build
        buildSuper();

    }

    @Override
    protected void build() {
        final float contentWidth = 940f;
        final float titleWidth = 250f;
        final float taWidth = 650f;
        final float taWidth2 = 1280f;
        final float taHeight = 160f;
        final float tabWidth = 240f;
        final float buttonHeight = 40f;

        // Only show update tab if not launched via install4j
        boolean showUpdateTab = !SysUtils.launchedViaInstall4j();

        // Create the tab buttons
        HorizontalGroup group = new HorizontalGroup();
        group.align(Align.left);

        final Button tabHelp = new OwnTextButton(I18n.msg("gui.help.help"), skin, "toggle-big");
        tabHelp.pad(pad5);
        tabHelp.setWidth(tabWidth);
        final Button tabAbout = new OwnTextButton(I18n.msg("gui.help.about"), skin, "toggle-big");
        tabAbout.pad(pad5);
        tabAbout.setWidth(tabWidth);
        final Button tabSystem = new OwnTextButton(I18n.msg("gui.help.system"), skin, "toggle-big");
        tabSystem.pad(pad5);
        tabSystem.setWidth(tabWidth);
        final Button tabUpdates = showUpdateTab ? new OwnTextButton(I18n.msg("gui.newversion"), skin, "toggle-big") : null;
        if (showUpdateTab) {
            tabUpdates.pad(pad5);
            tabUpdates.setWidth(tabWidth);
        }

        group.addActor(tabHelp);
        group.addActor(tabAbout);
        group.addActor(tabSystem);
        if (showUpdateTab)
            group.addActor(tabUpdates);

        content.add(group).align(Align.left).padLeft(pad5);
        content.row();
        content.pad(pad10);

        // Create the tab content. Just using images here for simplicity.
        Stack tabContent = new Stack();

        /* CONTENT 1 - HELP */
        final Table contentHelp = new Table(skin);
        contentHelp.top();

        OwnLabel gaiasky = new OwnLabel(Settings.getApplicationTitle(Settings.settings.runtime.openVr), skin, "main-title");

        // User manual
        Label homepageTitle = new OwnLabel(I18n.msg("gui.help.homepage"), skin);
        Label homepageTxt = new OwnLabel(I18n.msg("gui.help.help1"), skin);
        Link homepageLink = new Link(Settings.WEBPAGE, linkStyle, Settings.WEBPAGE);

        // Wiki
        Label docsTitle = new OwnLabel(I18n.msg("gui.help.docs"), skin);
        Label docsTxt = new OwnLabel(I18n.msg("gui.help.help2"), skin);
        Link docsLink = new Link(Settings.DOCUMENTATION, linkStyle, Settings.DOCUMENTATION);

        // Icon
        FileHandle gsIcon = Gdx.files.internal(Settings.settings.runtime.openVr ? "icon/gsvr_icon.png" : "icon/gs_icon.png");
        Texture iconTex = new Texture(gsIcon);
        iconTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Image gaiaSkyIcon = new Image(iconTex);
        gaiaSkyIcon.setOrigin(Align.center);

        // Add all to content
        contentHelp.add(gaiasky).pad(pad10).padTop(pad20).padBottom(pad10 * 5f).colspan(2);
        contentHelp.row();
        contentHelp.add(homepageTitle).align(Align.left).padRight(pad20);
        contentHelp.add(homepageTxt).align(Align.left);
        contentHelp.row();
        contentHelp.add(new OwnLabel("", skin)).padBottom(pad10);
        contentHelp.add(homepageLink).align(Align.left).padBottom(pad10);
        contentHelp.row();
        contentHelp.add(docsTitle).align(Align.left).padRight(pad20);
        contentHelp.add(docsTxt).align(Align.left);
        contentHelp.row();
        contentHelp.add(new OwnLabel("", skin)).padBottom(pad10 * 4f);
        contentHelp.add(docsLink).align(Align.left).padBottom(pad10 * 4f);
        contentHelp.row();
        contentHelp.add(gaiaSkyIcon).colspan(2).align(Align.center);
        contentHelp.pack();

        /* CONTENT 2 - ABOUT */
        final Table contentAbout = new Table(skin);
        contentAbout.top();

        // Intro
        TextArea intro = new OwnTextArea(I18n.msg("gui.help.gscredits", Settings.settings.version.version), skin.get("regular", TextFieldStyle.class));
        intro.setDisabled(true);
        intro.setPrefRows(3);
        intro.setWidth(contentWidth);

        // Home page
        Label homePageTitle = new OwnLabel(I18n.msg("gui.help.homepage"), skin);
        Link homepage = new Link(Settings.WEBPAGE, linkStyle, Settings.WEBPAGE);

        // Twitter
        Label devNewsTitle = new OwnLabel(I18n.msg("gui.help.devnews"), skin);
        Link tw = new Link(Settings.SOCIAL_MEDIA_NAME, linkStyle, Settings.SOCIAL_MEDIA_URL);

        // Author
        Label authorTitle = new OwnLabel(I18n.msg("gui.help.author"), skin);

        Table author = new Table(skin);
        Label authorName = new OwnLabel(Settings.AUTHOR_NAME, skin);
        Link authorMail = new Link(Settings.AUTHOR_EMAIL, linkStyle, "mailto:" + Settings.AUTHOR_EMAIL);
        Link authorPage = new Link("tonisagrista.com", linkStyle, "https://tonisagrista.com");
        Link authorMasto = new Link("@jumpinglangur@mastodon.social", linkStyle, "https://mastodon.social/@jumpinglangur");
        author.add(authorName).left().row();
        author.add(authorMail).left().row();
        author.add(authorPage).left().row();
        author.add(authorMasto).left().row();

        // Contributor
        Label contribTitle = new OwnLabel(I18n.msg("gui.help.contributors"), skin);

        Table contrib = new Table(skin);
        contrib.align(Align.left);
        Label contribName = new OwnLabel("Apl. Prof. Dr. Stefan Jordan", skin);
        Link contribMail = new Link("jordan@ari.uni-heidelberg.de", linkStyle, "mailto:jordan@ari.uni-heidelberg.de");
        contrib.add(contribName).left().row();
        contrib.add(contribMail).left().row();

        // License
        HorizontalGroup licenseHorizontal = new HorizontalGroup();
        licenseHorizontal.space(pad10);

        VerticalGroup licenseVertical = new VerticalGroup();
        TextArea licenseText = new OwnTextArea(I18n.msg("gui.help.license"), skin.get("regular", TextFieldStyle.class));
        licenseText.setDisabled(true);
        licenseText.setPrefRows(3);
        licenseText.setWidth(taWidth2 / 2f);
        Link licenseLink = new Link(Settings.LICENSE_URL, linkStyle, Settings.LICENSE_URL);

        licenseVertical.addActor(licenseText);
        licenseVertical.addActor(licenseLink);

        licenseHorizontal.addActor(licenseVertical);

        // Thanks
        HorizontalGroup thanks = new HorizontalGroup();
        thanks.space(pad10).pad(pad10);
        Container<Actor> thanksSc = new Container<>(thanks);
        thanksSc.setBackground(skin.getDrawable("bg-clear"));

        Image zah = new Image(getSpriteDrawable(Gdx.files.internal("img/zah.png")));
        Image dlr = new Image(getSpriteDrawable(Gdx.files.internal("img/dlr.png")));
        Image bwt = new Image(getSpriteDrawable(Gdx.files.internal("img/bwt.png")));
        Image dpac = new Image(getSpriteDrawable(Gdx.files.internal("img/dpac.png")));

        thanks.addActor(zah);
        thanks.addActor(dlr);
        thanks.addActor(bwt);
        thanks.addActor(dpac);

        contentAbout.add(intro).colspan(2).left().padTop(pad20);
        contentAbout.row();
        contentAbout.add(homePageTitle).left().padRight(pad10).padTop(pad10);
        contentAbout.add(homepage).left().padTop(pad10);
        contentAbout.row();
        contentAbout.add(devNewsTitle).left().padRight(pad10).padTop(pad10);
        contentAbout.add(tw).left().padTop(pad10);
        contentAbout.row();
        contentAbout.add(authorTitle).left().padRight(pad10).padTop(pad10);
        contentAbout.add(author).left().padTop(pad5).padTop(pad10);
        contentAbout.row();
        contentAbout.add(contribTitle).left().padRight(pad10).padTop(pad10);
        contentAbout.add(contrib).left().padTop(pad10);
        contentAbout.row();
        contentAbout.add(licenseHorizontal).colspan(2).center().padTop(pad10);
        contentAbout.row();
        contentAbout.add(thanksSc).colspan(2).center().padTop(pad10 * 4f);
        contentAbout.pack();

        /* CONTENT 3 - SYSTEM */
        final Table contentSystem = new Table(skin);
        contentSystem.top();

        // Build info
        Label buildInfo = new OwnLabel(I18n.msg("gui.help.buildinfo"), skin, "header");

        Label versionTitle = new OwnLabel(I18n.msg("gui.help.version", Settings.APPLICATION_NAME), skin);
        Label version = new OwnLabel(Settings.settings.version.version, skin);

        Label revisionTitle = new OwnLabel(I18n.msg("gui.help.buildnumber"), skin);
        Label revision = new OwnLabel(Settings.settings.version.build, skin);

        Label timeTitle = new OwnLabel(I18n.msg("gui.help.buildtime"), skin);
        Label time = new OwnLabel(Settings.settings.version.buildTime.toString(), skin);

        Label systemTitle = new OwnLabel(I18n.msg("gui.help.buildsys"), skin);
        TextArea system = new OwnTextArea(Settings.settings.version.system, skin.get("regular", TextFieldStyle.class));
        system.setDisabled(true);
        system.setPrefRows(3);
        system.setWidth(taWidth * 2f / 3f);

        Label builderTitle = new OwnLabel(I18n.msg("gui.help.builder"), skin);
        Label builder = new OwnLabel(Settings.settings.version.builder, skin);

        // Paths
        Label paths = new OwnLabel(I18n.msg("gui.help.paths"), skin, "header");

        Label configTitle = new OwnLabel(I18n.msg("gui.help.paths.config"), skin);
        Label config = new OwnLabel(SysUtils.getConfigDir().toAbsolutePath().toString(), skin);
        Label dataTitle = new OwnLabel(I18n.msg("gui.help.paths.data"), skin);
        Label data = new OwnLabel(SysUtils.getDataDir().toAbsolutePath().toString(), skin);
        Label screenshotsTitle = new OwnLabel(I18n.msg("gui.help.paths.screenshots"), skin);
        Label screenshots = new OwnLabel(SysUtils.getDefaultScreenshotsDir().toAbsolutePath().toString(), skin);
        Label framesTitle = new OwnLabel(I18n.msg("gui.help.paths.frames"), skin);
        Label frames = new OwnLabel(SysUtils.getDefaultFramesDir().toAbsolutePath().toString(), skin);
        Label musicTitle = new OwnLabel(I18n.msg("gui.help.paths.music"), skin);
        Label music = new OwnLabel(SysUtils.getDefaultMusicDir().toAbsolutePath().toString(), skin);
        Label mappingsTitle = new OwnLabel(I18n.msg("gui.help.paths.mappings"), skin);
        Label mappings = new OwnLabel(SysUtils.getDefaultMappingsDir().toAbsolutePath().toString(), skin);
        Label cameraTitle = new OwnLabel(I18n.msg("gui.help.paths.camera"), skin);
        Label camera = new OwnLabel(SysUtils.getDefaultCameraDir().toAbsolutePath().toString(), skin);

        // Java info
        Label javaInfo = new OwnLabel(I18n.msg("gui.help.javainfo"), skin, "header");

        Label javaVersionTitle = new OwnLabel(I18n.msg("gui.help.javaversion"), skin);
        Label javaVersion = new OwnLabel(System.getProperty("java.version"), skin);

        Label javaRuntimeTitle = new OwnLabel(I18n.msg("gui.help.javaname"), skin);
        Label javaRuntime = new OwnLabel(System.getProperty("java.runtime.name"), skin);

        Label javaVMNameTitle = new OwnLabel(I18n.msg("gui.help.javavmname"), skin);
        Label javaVMName = new OwnLabel(System.getProperty("java.vm.name"), skin);

        Label javaVMVersionTitle = new OwnLabel(I18n.msg("gui.help.javavmversion"), skin);
        Label javaVMVersion = new OwnLabel(System.getProperty("java.vm.version"), skin);

        Label javaVMVendorTitle = new OwnLabel(I18n.msg("gui.help.javavmvendor"), skin);
        Label javaVMVendor = new OwnLabel(System.getProperty("java.vm.vendor"), skin);

        TextButton memInfoButton = new OwnTextButton(I18n.msg("gui.help.meminfo"), skin, "default");
        memInfoButton.setName("memoryinfo");
        memInfoButton.pad(0, pad10, 0, pad10);
        memInfoButton.setHeight(buttonHeight);
        memInfoButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (memInfoWindow == null) {
                    memInfoWindow = new MemInfoWindow(stage, skin);
                }
                if (!memInfoWindow.isVisible() || !memInfoWindow.hasParent())
                    memInfoWindow.show(stage);
                return true;
            }
            return false;
        });

        // System info
        Label sysInfo = new OwnLabel(I18n.msg("gui.help.sysinfo"), skin, "header");

        Label sysOSTitle = new OwnLabel(I18n.msg("gui.help.os"), skin);
        Label sysOS;

        try {
            SystemInfo si = new SystemInfo();
            sysOS = new OwnLabel(si.getOperatingSystem().toString() + "\n" + "Arch: " + System.getProperty("os.arch"), skin);
        } catch (Error e) {
            sysOS = new OwnLabel(System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"), skin);
        }

        Label glRendererTitle = new OwnLabel(I18n.msg("gui.help.graphicsdevice"), skin);
        Label glRenderer = new OwnLabel(Gdx.gl.glGetString(GL20.GL_RENDERER), skin);

        // OpenGL info
        Label glInfo = new OwnLabel(I18n.msg("gui.help.openglinfo"), skin, "header");

        Label glVendorTitle = new OwnLabel(I18n.msg("gui.help.glvendor"), skin);
        Label glVendor = new OwnLabel(Gdx.gl.glGetString(GL20.GL_VENDOR), skin);

        Label glVersionTitle = new OwnLabel(I18n.msg("gui.help.openglversion"), skin);
        Label glVersion = new OwnLabel(Gdx.gl.glGetString(GL20.GL_VERSION), skin);

        Label glslVersionTitle = new OwnLabel(I18n.msg("gui.help.glslversion"), skin);
        Label glslVersion = new OwnLabel(Gdx.gl.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION), skin);

        Label glExtensionsTitle = new OwnLabel(I18n.msg("gui.help.glextensions"), skin);
        String extensions = GlobalResources.getGLExtensions();

        IntBuffer buf = BufferUtils.newIntBuffer(16);
        Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, buf);
        int maxSize = buf.get(0);
        int lines = GlobalResources.countOccurrences(extensions, '\n');
        OwnTextArea maxTexSize = new OwnTextArea("Max texture size: " + maxSize + '\n' + extensions, skin, "default");
        maxTexSize.setDisabled(true);
        maxTexSize.setPrefRows(lines);
        maxTexSize.clearListeners();

        OwnScrollPane glExtensionsScroll = new OwnScrollPane(maxTexSize, skin, "default-nobg");
        glExtensionsScroll.setWidth(taWidth);
        glExtensionsScroll.setHeight(taHeight);
        glExtensionsScroll.setForceScroll(false, true);
        glExtensionsScroll.setSmoothScrolling(true);
        glExtensionsScroll.setFadeScrollBars(false);
        scrolls.add(glExtensionsScroll);

        // BUILD
        contentSystem.add(buildInfo).colspan(2).align(Align.left).padTop(pad20).padBottom(pad5);
        contentSystem.row();
        contentSystem.add(versionTitle).align(Align.topLeft).padRight(pad10).width(titleWidth);
        contentSystem.add(version).align(Align.left);
        contentSystem.row();
        contentSystem.add(revisionTitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(revision).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(timeTitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(time).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(builderTitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(builder).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(systemTitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(system).align(Align.left).padTop(pad5);
        contentSystem.row();

        // PATHS
        contentSystem.add(paths).colspan(2).align(Align.left).padTop(pad10).padBottom(pad5);
        contentSystem.row();
        contentSystem.add(configTitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(config).align(Align.left);
        contentSystem.row();
        contentSystem.add(dataTitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(data).align(Align.left);
        contentSystem.row();
        contentSystem.add(screenshotsTitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(screenshots).align(Align.left);
        contentSystem.row();
        contentSystem.add(framesTitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(frames).align(Align.left);
        contentSystem.row();
        contentSystem.add(cameraTitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(camera).align(Align.left);
        contentSystem.row();
        contentSystem.add(mappingsTitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(mappings).align(Align.left);
        contentSystem.row();
        contentSystem.add(musicTitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(music).align(Align.left);
        contentSystem.row();

        // JAVA
        contentSystem.add(javaInfo).colspan(2).align(Align.left).padTop(pad10).padBottom(pad5);
        contentSystem.row();
        contentSystem.add(javaVersionTitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(javaVersion).align(Align.left);
        contentSystem.row();
        contentSystem.add(javaRuntimeTitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(javaRuntime).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(javaVMNameTitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(javaVMName).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(javaVMVersionTitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(javaVMVersion).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add( javaVMVendorTitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(javaVMVendor).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(memInfoButton).colspan(2).align(Align.left).padTop(pad10);
        contentSystem.row();

        // SYSTEM
        contentSystem.add(sysInfo).colspan(2).align(Align.left).padTop(pad10).padBottom(pad5);
        contentSystem.row();
        try {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hal = si.getHardware();
            CentralProcessor cp = hal.getProcessor();

            Label cpuTitle = new OwnLabel(I18n.msg("gui.help.cpu"), skin);
            Label cpu = new OwnLabel(cp.toString(), skin);

            contentSystem.add(cpuTitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
            contentSystem.add(cpu).align(Align.left).padTop(pad5);
            contentSystem.row();
        } catch (Error e) {
            contentSystem.add(new OwnLabel(I18n.msg("gui.help.cpu.no"), skin)).colspan(2).align(Align.left).padTop(pad10).padBottom(pad10).row();
        }
        contentSystem.add(sysOSTitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(sysOS).align(Align.left);
        contentSystem.row();
        contentSystem.add(glRendererTitle).align(Align.topLeft).padRight(pad10).padTop(pad10);
        contentSystem.add(glRenderer).align(Align.left).padTop(pad5);
        contentSystem.row();

        // GL
        contentSystem.add(glInfo).colspan(2).align(Align.left).padTop(pad10).padBottom(pad5);
        contentSystem.row();
        contentSystem.add(glVersionTitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(glVersion).align(Align.left);
        contentSystem.row();
        contentSystem.add(glVendorTitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(glVendor).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(glslVersionTitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(glslVersion).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(glExtensionsTitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(glExtensionsScroll).align(Align.left).padTop(pad5);

        OwnScrollPane systemScroll = new OwnScrollPane(contentSystem, skin, "minimalist-nobg");
        systemScroll.setFadeScrollBars(false);
        systemScroll.setScrollingDisabled(true, false);
        systemScroll.setOverscroll(false, false);
        systemScroll.setSmoothScrolling(true);
        systemScroll.setHeight(800f);
        systemScroll.pack();


        /* CONTENT 4 - UPDATES */

        final Table contentUpdates = showUpdateTab ? new Table(skin) : null;
        if (showUpdateTab) {
            contentUpdates.top();

            // This is the table that displays it all
            checkTable = new Table(skin);
            checkLabel = new OwnLabel("", skin);

            checkTable.add(checkLabel).top().left().padBottom(pad5).row();
            if (Settings.settings.program.update.lastCheck == null || new Date().getTime() - Settings.settings.program.update.lastCheck.toEpochMilli() > Settings.ProgramSettings.UpdateSettings.VERSION_CHECK_INTERVAL_MS) {
                // Check!
                checkLabel.setText(I18n.msg("gui.newversion.checking"));
                getCheckVersionThread().start();
            } else {
                // Inform latest
                newVersionCheck(Settings.settings.version.version, Settings.settings.version.versionNumber, Settings.settings.version.buildTime, false);
            }
            contentUpdates.add(checkTable).left().top().padTop(pad20);
        }

        /* ADD ALL CONTENT */
        tabContent.addActor(contentHelp);
        tabContent.addActor(contentAbout);
        tabContent.addActor(systemScroll);
        if (showUpdateTab)
            tabContent.addActor(contentUpdates);

        content.add(tabContent).expand().fill();

        // Listen to changes in the tab button checked states
        // Set visibility of the tab content to match the checked state
        ChangeListener tabListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                contentHelp.setVisible(tabHelp.isChecked());
                contentAbout.setVisible(tabAbout.isChecked());
                systemScroll.setVisible(tabSystem.isChecked());
                if (showUpdateTab)
                    contentUpdates.setVisible(tabUpdates.isChecked());
            }
        };
        tabHelp.addListener(tabListener);
        tabAbout.addListener(tabListener);
        tabSystem.addListener(tabListener);
        if (showUpdateTab)
            tabUpdates.addListener(tabListener);

        // Let only one tab button be checked at a time
        ButtonGroup<Button> tabs = new ButtonGroup<>();
        tabs.setMinCheckCount(1);
        tabs.setMaxCheckCount(1);
        tabs.add(tabHelp);
        tabs.add(tabAbout);
        tabs.add(tabSystem);
        if (showUpdateTab)
            tabs.add(tabUpdates);

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

    private SpriteDrawable getSpriteDrawable(FileHandle fh) {
        Texture tex = new Texture(fh);
        return new SpriteDrawable(new Sprite(tex));
    }

    /**
     * Checks the given tag time against the current version time and:
     * <ul>
     * <li>Displays a "new version available" message if the given version is
     * newer than the current.</li>
     * <li>Display a "you have the latest version" message and a "check now"
     * button if the given version is older.</li>
     * </ul>
     *
     * @param tagVersion    The version to check
     * @param versionNumber The version number
     * @param tagDate       The date
     */
    private void newVersionCheck(String tagVersion, Integer versionNumber, Instant tagDate, boolean log) {
        Settings.settings.program.update.lastCheck = Instant.now();
        if (versionNumber > Settings.settings.version.versionNumber) {
            DateTimeFormatter df = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM).withLocale(I18n.locale).withZone(ZoneOffset.UTC);
            if (log) {
                logger.info(I18n.msg("gui.newversion.available", Settings.settings.version.version, tagVersion + " [" + df.format(tagDate) + "]"));
            }
            // There's a new version!
            checkLabel.setText(I18n.msg("gui.newversion.available", Settings.settings.version, tagVersion + " [" + df.format(tagDate) + "]"));
            final String uri = Settings.WEBPAGE_DOWNLOADS;

            OwnTextButton getNewVersion = new OwnTextButton(I18n.msg("gui.newversion.getit"), skin);
            getNewVersion.pad(0, pad10, 0, pad10);
            getNewVersion.setHeight(40f);
            getNewVersion.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    Gdx.net.openURI(Settings.WEBPAGE_DOWNLOADS);
                    return true;
                }
                return false;
            });
            checkTable.add(getNewVersion).center().padTop(pad10).padBottom(pad5).row();

            Link link = new Link(uri, linkStyle, uri);
            checkTable.add(link).center();

        } else {
            if (log)
                logger.info(I18n.msg("gui.newversion.nonew", Settings.settings.program.update.getLastCheckedString()));
            checkLabel.setText(I18n.msg("gui.newversion.nonew", Settings.settings.program.update.getLastCheckedString()));
            // Add check now button
            OwnTextButton checkNewVersion = new OwnTextButton(I18n.msg("gui.newversion.checknow"), skin);
            checkNewVersion.pad(pad5, pad10, pad5, pad10);
            checkNewVersion.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    checkLabel.setText(I18n.msg("gui.newversion.checking"));
                    logger.info(I18n.msg("gui.newversion.checking"));
                    getCheckVersionThread().start();
                    return true;
                }
                return false;
            });
            checkTable.add(checkNewVersion).center().padTop(pad10);
        }
    }

    private Thread getCheckVersionThread() {
        // Start version check
        VersionChecker vc = new VersionChecker(Settings.settings.program.url.versionCheck);
        vc.setListener(event -> {
            if (event instanceof VersionCheckEvent) {
                VersionCheckEvent vce = (VersionCheckEvent) event;
                if (!vce.isFailed()) {
                    checkTable.clear();
                    checkTable.add(checkLabel).top().left().padBottom(pad5).row();
                    // All is fine
                    newVersionCheck(vce.getTag(), vce.getVersionNumber(), vce.getTagTime(), true);

                } else {
                    // Handle failed case
                    logger.info(I18n.msg("gui.newversion.fail"));
                    checkLabel.setText(I18n.msg("notif.error", "Could not get last version"));
                    checkLabel.setColor(Color.RED);
                }
            }
            return false;
        });
        return new Thread(vc);
    }

}
