/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
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
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import gaiasky.util.*;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import gaiasky.util.update.VersionCheckEvent;
import gaiasky.util.update.VersionChecker;
import oshi.SystemInfo;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;

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
        var tabGroup = new HorizontalGroup();
        tabGroup.align(Align.left);

        final var tabHelp = new OwnTextButton(I18n.msg("gui.help.help"), skin, "toggle-big");
        tabHelp.pad(pad10);
        tabHelp.setWidth(tabWidth);
        final var tabAbout = new OwnTextButton(I18n.msg("gui.help.about"), skin, "toggle-big");
        tabAbout.pad(pad10);
        tabAbout.setWidth(tabWidth);
        final var tabSystem = new OwnTextButton(I18n.msg("gui.help.system"), skin, "toggle-big");
        tabSystem.pad(pad10);
        tabSystem.setWidth(tabWidth);
        final var tabUpdates = showUpdateTab ? new OwnTextButton(I18n.msg("gui.newversion"), skin, "toggle-big") : null;
        if (showUpdateTab) {
            tabUpdates.pad(pad10);
            tabUpdates.setWidth(tabWidth);
        }

        tabGroup.addActor(tabHelp);
        tabGroup.addActor(tabAbout);
        tabGroup.addActor(tabSystem);
        if (showUpdateTab)
            tabGroup.addActor(tabUpdates);

        tabButtons = new Array<>();
        tabButtons.add(tabHelp);
        tabButtons.add(tabAbout);
        tabButtons.add(tabSystem);
        if (showUpdateTab)
            tabButtons.add(tabUpdates);

        content.add(tabGroup).align(Align.left).padLeft(pad10);
        content.row();
        content.pad(pad18);

        /* CONTENT 1 - HELP */
        final var contentHelp = new Table(skin);
        contentHelp.top();

        var gaiasky = new OwnLabel(Settings.getApplicationTitle(Settings.settings.runtime.openXr), skin, "main-title");

        // User manual
        var homepageTitle = new OwnLabel(I18n.msg("gui.help.homepage"), skin);
        var homepageTxt = new OwnLabel(I18n.msg("gui.help.help1"), skin);
        var homepageLink = new Link(Settings.WEBPAGE, linkStyle, Settings.WEBPAGE);

        // Wiki
        var docsTitle = new OwnLabel(I18n.msg("gui.help.docs"), skin);
        var docsTxt = new OwnLabel(I18n.msg("gui.help.help2"), skin);
        var docsLink = new Link(Settings.DOCUMENTATION, linkStyle, Settings.DOCUMENTATION);

        // Icon
        var gsIcon = Gdx.files.internal(Settings.settings.runtime.openXr ? "icon/gsvr_icon.png" : "icon/gs_icon.png");
        var iconTex = new Texture(gsIcon);
        iconTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        var gaiaSkyIcon = new Image(iconTex);
        gaiaSkyIcon.setOrigin(Align.center);

        // Add all to content
        contentHelp.add(gaiaSkyIcon).colspan(2).padTop(pad34).align(Align.center);
        contentHelp.row();
        contentHelp.add(gaiasky).pad(pad18).padBottom(pad18 * 5f).colspan(2);
        contentHelp.row();
        contentHelp.add(homepageTitle).align(Align.left).padRight(pad34);
        contentHelp.add(homepageTxt).align(Align.left);
        contentHelp.row();
        contentHelp.add(new OwnLabel("", skin)).padBottom(pad18);
        contentHelp.add(homepageLink).align(Align.left).padBottom(pad18);
        contentHelp.row();
        contentHelp.add(docsTitle).align(Align.left).padRight(pad34);
        contentHelp.add(docsTxt).align(Align.left);
        contentHelp.row();
        contentHelp.add(new OwnLabel("", skin)).padBottom(pad18 * 4f);
        contentHelp.add(docsLink).align(Align.left).padBottom(pad18 * 4f);
        contentHelp.pack();

        /* CONTENT 2 - ABOUT */
        final var contentAbout = new Table(skin);
        contentAbout.top();

        // Intro
        var intro = new OwnTextArea(I18n.msg("gui.help.gscredits", Settings.settings.version.version), skin.get("regular", TextFieldStyle.class));
        intro.setDisabled(true);
        intro.setPrefRows(3);
        intro.setWidth(contentWidth);

        // Home page
        var homePageTitle = new OwnLabel(I18n.msg("gui.help.homepage"), skin);
        var homepage = new Link(Settings.WEBPAGE, linkStyle, Settings.WEBPAGE);

        // Twitter
        var devNewsTitle = new OwnLabel(I18n.msg("gui.help.devnews"), skin);
        var tw = new Link(Settings.SOCIAL_MEDIA_NAME, linkStyle, Settings.SOCIAL_MEDIA_URL);

        // Author
        var authorTitle = new OwnLabel(I18n.msg("gui.help.author"), skin);

        var author = new Table(skin);
        var authorName = new OwnLabel(Settings.AUTHOR_NAME, skin);
        var authorMail = new Link(Settings.AUTHOR_EMAIL, linkStyle, "mailto:" + Settings.AUTHOR_EMAIL);
        var authorPage = new Link("tonisagrista.com", linkStyle, "https://tonisagrista.com");
        var authorMasto = new Link("@jumpinglangur@mastodon.social", linkStyle, "https://mastodon.social/@jumpinglangur");
        author.add(authorName).left().row();
        author.add(authorMail).left().row();
        author.add(authorPage).left().row();
        author.add(authorMasto).left().row();

        // Contributor
        var contribTitle = new OwnLabel(I18n.msg("gui.help.contributors"), skin);

        var contrib = new Table(skin);
        contrib.align(Align.left);
        var contribName = new OwnLabel("Apl. Prof. Dr. Stefan Jordan", skin);
        var contribMail = new Link("jordan@ari.uni-heidelberg.de", linkStyle, "mailto:jordan@ari.uni-heidelberg.de");
        contrib.add(contribName).left().row();
        contrib.add(contribMail).left().row();

        // License
        var licenseHorizontal = new HorizontalGroup();
        licenseHorizontal.space(pad18);

        var licenseVertical = new VerticalGroup();
        var licenseText = new OwnTextArea(I18n.msg("gui.help.license"), skin.get("regular", TextFieldStyle.class));
        licenseText.setDisabled(true);
        licenseText.setPrefRows(3);
        licenseText.setWidth(taWidth2 / 2f);
        var licenseLink = new Link(Settings.LICENSE_URL, linkStyle, Settings.LICENSE_URL);

        licenseVertical.addActor(licenseText);
        licenseVertical.addActor(licenseLink);

        licenseHorizontal.addActor(licenseVertical);

        // Thanks
        HorizontalGroup thanks = new HorizontalGroup();
        thanks.space(pad18).pad(pad18);
        Container<Actor> thanksSc = new Container<>(thanks);
        thanksSc.setBackground(skin.getDrawable("bg-clear"));

        var zah = new Image(getSpriteDrawable(Gdx.files.internal("img/zah.png")));
        var dlr = new Image(getSpriteDrawable(Gdx.files.internal("img/dlr.png")));
        var bwt = new Image(getSpriteDrawable(Gdx.files.internal("img/bwt.png")));
        var dpac = new Image(getSpriteDrawable(Gdx.files.internal("img/dpac.png")));

        thanks.addActor(zah);
        thanks.addActor(dlr);
        thanks.addActor(bwt);
        thanks.addActor(dpac);

        contentAbout.add(intro).colspan(2).left().padTop(pad34);
        contentAbout.row();
        contentAbout.add(homePageTitle).left().padRight(pad18).padTop(pad18);
        contentAbout.add(homepage).left().padTop(pad18);
        contentAbout.row();
        contentAbout.add(devNewsTitle).left().padRight(pad18).padTop(pad18);
        contentAbout.add(tw).left().padTop(pad18);
        contentAbout.row();
        contentAbout.add(authorTitle).left().padRight(pad18).padTop(pad18);
        contentAbout.add(author).left().padTop(pad10).padTop(pad18);
        contentAbout.row();
        contentAbout.add(contribTitle).left().padRight(pad18).padTop(pad18);
        contentAbout.add(contrib).left().padTop(pad18);
        contentAbout.row();
        contentAbout.add(licenseHorizontal).colspan(2).center().padTop(pad18);
        contentAbout.row();
        contentAbout.add(thanksSc).colspan(2).center().padTop(pad18 * 4f);
        contentAbout.pack();

        /* CONTENT 3 - SYSTEM */
        final Table contentSystem = new Table(skin);
        contentSystem.top();

        // Build info
        var buildInfo = new OwnLabel(I18n.msg("gui.help.buildinfo"), skin, "header");

        var versionTitle = new OwnLabel(I18n.msg("gui.help.version", Settings.APPLICATION_NAME), skin);
        var version = new OwnLabel(Settings.settings.version.version, skin);

        var revisionTitle = new OwnLabel(I18n.msg("gui.help.buildnumber"), skin);
        var revision = new OwnLabel(Settings.settings.version.build, skin);

        var timeTitle = new OwnLabel(I18n.msg("gui.help.buildtime"), skin);
        var time = new OwnLabel(Settings.settings.version.buildTime.toString(), skin);

        var systemTitle = new OwnLabel(I18n.msg("gui.help.buildsys"), skin);
        var system = new OwnTextArea(Settings.settings.version.system, skin.get("regular", TextFieldStyle.class));
        system.setDisabled(true);
        system.setPrefRows(3);
        system.setWidth(taWidth * 2f / 3f);

        var builderTitle = new OwnLabel(I18n.msg("gui.help.builder"), skin);
        var builder = new OwnLabel(Settings.settings.version.builder, skin);

        // Paths
        var paths = new OwnLabel(I18n.msg("gui.help.paths"), skin, "header");
        var pathLength = 48;

        var configTitle = new OwnLabel(I18n.msg("gui.help.paths.config"), skin);
        var configPath = SysUtils.getConfigDir().toAbsolutePath().toString();
        var config = new OwnLabel(TextUtils.capString(configPath, pathLength, true), skin);
        config.addListener(new OwnTextTooltip(configPath, skin, 15));

        var dataTitle = new OwnLabel(I18n.msg("gui.help.paths.data"), skin);
        var dataPath = SysUtils.getDataDir().toAbsolutePath().toString();
        var data = new OwnLabel(TextUtils.capString(dataPath, pathLength, true), skin);
        data.addListener(new OwnTextTooltip(dataPath, skin, 15));

        var screenshotsTitle = new OwnLabel(I18n.msg("gui.help.paths.screenshots"), skin);
        var screenshotsPath = SysUtils.getDefaultScreenshotsDir().toAbsolutePath().toString();
        var screenshots = new OwnLabel(TextUtils.capString(screenshotsPath, pathLength, true), skin);
        screenshots.addListener(new OwnTextTooltip(screenshotsPath, skin, 15));

        var framesTitle = new OwnLabel(I18n.msg("gui.help.paths.frames"), skin);
        var framesPath = SysUtils.getDefaultFramesDir().toAbsolutePath().toString();
        var frames = new OwnLabel(TextUtils.capString(framesPath, pathLength, true), skin);
        frames.addListener(new OwnTextTooltip(framesPath, skin, 15));

        var musicTitle = new OwnLabel(I18n.msg("gui.help.paths.music"), skin);
        var musicPath = SysUtils.getDefaultMusicDir().toAbsolutePath().toString();
        var music = new OwnLabel(TextUtils.capString(musicPath, pathLength, true), skin);
        music.addListener(new OwnTextTooltip(musicPath, skin, 15));

        var mappingsTitle = new OwnLabel(I18n.msg("gui.help.paths.mappings"), skin);
        var mappingsPath = SysUtils.getDefaultMappingsDir().toAbsolutePath().toString();
        var mappings = new OwnLabel(TextUtils.capString(mappingsPath, pathLength, true), skin);
        mappings.addListener(new OwnTextTooltip(mappingsPath, skin, 15));

        var cameraTitle = new OwnLabel(I18n.msg("gui.help.paths.camera"), skin);
        var cameraPath = SysUtils.getDefaultCameraDir().toAbsolutePath().toString();
        var camera = new OwnLabel(TextUtils.capString(cameraPath, pathLength, true), skin);
        camera.addListener(new OwnTextTooltip(cameraPath, skin, 15));

        // Java info
        var javaInfo = new OwnLabel(I18n.msg("gui.help.javainfo"), skin, "header");

        var javaVersionTitle = new OwnLabel(I18n.msg("gui.help.javaversion"), skin);
        var javaVersion = new OwnLabel(System.getProperty("java.version"), skin);

        var javaRuntimeTitle = new OwnLabel(I18n.msg("gui.help.javaname"), skin);
        var javaRuntime = new OwnLabel(System.getProperty("java.runtime.name"), skin);

        var javaVMNameTitle = new OwnLabel(I18n.msg("gui.help.javavmname"), skin);
        var javaVMName = new OwnLabel(System.getProperty("java.vm.name"), skin);

        var javaVMVersionTitle = new OwnLabel(I18n.msg("gui.help.javavmversion"), skin);
        var javaVMVersion = new OwnLabel(System.getProperty("java.vm.version"), skin);

        var javaVMVendorTitle = new OwnLabel(I18n.msg("gui.help.javavmvendor"), skin);
        var javaVMVendor = new OwnLabel(System.getProperty("java.vm.vendor"), skin);

        TextButton memInfoButton = new OwnTextButton(I18n.msg("gui.help.meminfo"), skin, "default");
        memInfoButton.setName("memoryinfo");
        memInfoButton.pad(0, pad18, 0, pad18);
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
        var sysInfo = new OwnLabel(I18n.msg("gui.help.sysinfo"), skin, "header");

        var sysOSTitle = new OwnLabel(I18n.msg("gui.help.os"), skin);
        OwnLabel sysOS;

        try {
            var si = new SystemInfo();
            var os = si.getOperatingSystem();
            sysOS = new OwnLabel(
                    I18n.msg("gui.help.os.family") + ": " + os.getFamily()
                            + "\n" + I18n.msg("gui.help.os.name") + ": " + os.getVersionInfo().getCodeName()
                            + "\n" + I18n.msg("gui.help.os.version") + ": " + os.getVersionInfo().getVersion()
                            + "\n" + I18n.msg("gui.help.os.build") + ": " + os.getVersionInfo().getBuildNumber()
                            + "\n" + I18n.msg("gui.help.os.manufacturer") + ": " + os.getManufacturer()
                            + "\n" + I18n.msg("gui.help.os.arch") + ": " + System.getProperty("os.arch"), skin);
        } catch (Error e) {
            sysOS = new OwnLabel(System.getProperty("os.name") + "\n" + System.getProperty("os.version") + "\n" + System.getProperty("os.arch"), skin);
        }

        var glRendererTitle = new OwnLabel(I18n.msg("gui.help.graphicsdevice"), skin);
        var glRendererStr = Gdx.gl.glGetString(GL20.GL_RENDERER);
        var glRenderer = new OwnLabel(TextUtils.breakSpaces(glRendererStr, 48), skin);

        // OpenGL info
        var glInfo = new OwnLabel(I18n.msg("gui.help.openglinfo"), skin, "header");

        var glVendorTitle = new OwnLabel(I18n.msg("gui.help.glvendor"), skin);
        var glVendor = new OwnLabel(Gdx.gl.glGetString(GL20.GL_VENDOR), skin);

        var glVersionTitle = new OwnLabel(I18n.msg("gui.help.openglversion"), skin);
        var glVersion = new OwnLabel(Gdx.gl.glGetString(GL20.GL_VERSION), skin);

        var glslVersionTitle = new OwnLabel(I18n.msg("gui.help.glslversion"), skin);
        var glslVersion = new OwnLabel(Gdx.gl.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION), skin);

        var glExtensionsTitle = new OwnLabel(I18n.msg("gui.help.glextensions"), skin);
        var extensions = GlobalResources.getGLExtensions();

        var buf = BufferUtils.newIntBuffer(16);
        Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, buf);
        int maxSize = buf.get(0);
        int lines = GlobalResources.countOccurrences(extensions, '\n');
        var maxTexSize = new OwnTextArea("Max texture size: " + maxSize + '\n' + extensions, skin, "default");
        maxTexSize.setDisabled(true);
        maxTexSize.setPrefRows(lines);
        maxTexSize.clearListeners();

        var glExtensionsScroll = new OwnScrollPane(maxTexSize, skin, "default-nobg");
        glExtensionsScroll.setWidth(taWidth);
        glExtensionsScroll.setHeight(taHeight);
        glExtensionsScroll.setForceScroll(false, true);
        glExtensionsScroll.setSmoothScrolling(true);
        glExtensionsScroll.setFadeScrollBars(false);
        scrolls.add(glExtensionsScroll);

        // BUILD
        contentSystem.add(buildInfo).colspan(2).left().padTop(pad34);
        contentSystem.row();
        contentSystem.add(new Separator(skin, "small")).colspan(2).bottom().left().expandX().fillX().padBottom(pad20);
        contentSystem.row();
        contentSystem.add(versionTitle).align(Align.topLeft).padRight(pad18).width(titleWidth);
        contentSystem.add(version).align(Align.left);
        contentSystem.row();
        contentSystem.add(revisionTitle).align(Align.topLeft).padRight(pad18).padTop(pad10);
        contentSystem.add(revision).align(Align.left).padTop(pad10);
        contentSystem.row();
        contentSystem.add(timeTitle).align(Align.topLeft).padRight(pad18).padTop(pad10);
        contentSystem.add(time).align(Align.left).padTop(pad10);
        contentSystem.row();
        contentSystem.add(builderTitle).align(Align.topLeft).padRight(pad18).padTop(pad10);
        contentSystem.add(builder).align(Align.left).padTop(pad10);
        contentSystem.row();
        contentSystem.add(systemTitle).align(Align.topLeft).padRight(pad18).padTop(pad10);
        contentSystem.add(system).align(Align.left).padTop(pad10);
        contentSystem.row();

        // PATHS
        contentSystem.add(paths).colspan(2).align(Align.left).padTop(pad34);
        contentSystem.row();
        contentSystem.add(new Separator(skin, "small")).colspan(2).bottom().left().expandX().fillX().padBottom(pad20);
        contentSystem.row();
        contentSystem.add(configTitle).align(Align.topLeft).padRight(pad18);
        contentSystem.add(config).align(Align.left);
        contentSystem.row();
        contentSystem.add(dataTitle).align(Align.topLeft).padRight(pad18);
        contentSystem.add(data).align(Align.left);
        contentSystem.row();
        contentSystem.add(screenshotsTitle).align(Align.topLeft).padRight(pad18);
        contentSystem.add(screenshots).align(Align.left);
        contentSystem.row();
        contentSystem.add(framesTitle).align(Align.topLeft).padRight(pad18);
        contentSystem.add(frames).align(Align.left);
        contentSystem.row();
        contentSystem.add(cameraTitle).align(Align.topLeft).padRight(pad18);
        contentSystem.add(camera).align(Align.left);
        contentSystem.row();
        contentSystem.add(mappingsTitle).align(Align.topLeft).padRight(pad18);
        contentSystem.add(mappings).align(Align.left);
        contentSystem.row();
        contentSystem.add(musicTitle).align(Align.topLeft).padRight(pad18);
        contentSystem.add(music).align(Align.left);
        contentSystem.row();

        // JAVA
        contentSystem.add(javaInfo).colspan(2).align(Align.left).padTop(pad34 * 2f);
        contentSystem.row();
        contentSystem.add(new Separator(skin, "small")).colspan(2).bottom().left().expandX().fillX().padBottom(pad20);
        contentSystem.row();
        contentSystem.add(javaVersionTitle).align(Align.topLeft).padRight(pad18);
        contentSystem.add(javaVersion).align(Align.left);
        contentSystem.row();
        contentSystem.add(javaRuntimeTitle).align(Align.topLeft).padRight(pad18).padTop(pad10);
        contentSystem.add(javaRuntime).align(Align.left).padTop(pad10);
        contentSystem.row();
        contentSystem.add(javaVMNameTitle).align(Align.topLeft).padRight(pad18).padTop(pad10);
        contentSystem.add(javaVMName).align(Align.left).padTop(pad10);
        contentSystem.row();
        contentSystem.add(javaVMVersionTitle).align(Align.topLeft).padRight(pad18).padTop(pad10);
        contentSystem.add(javaVMVersion).align(Align.left).padTop(pad10);
        contentSystem.row();
        contentSystem.add(javaVMVendorTitle).align(Align.topLeft).padRight(pad18).padTop(pad10);
        contentSystem.add(javaVMVendor).align(Align.left).padTop(pad10);
        contentSystem.row();
        contentSystem.add(memInfoButton).colspan(2).align(Align.left).padTop(pad18);
        contentSystem.row();

        // SYSTEM
        contentSystem.add(sysInfo).colspan(2).align(Align.left).padTop(pad34 * 2f);
        contentSystem.row();
        contentSystem.add(new Separator(skin, "small")).colspan(2).bottom().left().expandX().fillX().padBottom(pad20);
        contentSystem.row();
        try {
            var si = new SystemInfo();
            var hal = si.getHardware();
            var cp = hal.getProcessor();

            var cpuTitle = new OwnLabel(I18n.msg("gui.help.cpu"), skin);
            var cpu = new OwnLabel(cp.toString(), skin);

            contentSystem.add(cpuTitle).align(Align.topLeft).padRight(pad18).padTop(pad10).padBottom(pad18);
            contentSystem.add(cpu).align(Align.left).padTop(pad10).padBottom(pad18);
            contentSystem.row();
        } catch (Error e) {
            contentSystem.add(new OwnLabel(I18n.msg("gui.help.cpu.no"), skin)).colspan(2).align(Align.left).padTop(pad18).padBottom(pad18).row();
        }
        contentSystem.add(sysOSTitle).align(Align.topLeft).padRight(pad18).padBottom(pad18);
        contentSystem.add(sysOS).align(Align.left).padBottom(pad18);
        contentSystem.row();

        contentSystem.add(glRendererTitle).align(Align.topLeft).padRight(pad18).padTop(pad18);
        contentSystem.add(glRenderer).align(Align.left).padTop(pad10);
        contentSystem.row();

        // GL
        contentSystem.add(glInfo).colspan(2).align(Align.left).padTop(pad34 * 2f);
        contentSystem.row();
        contentSystem.add(new Separator(skin, "small")).colspan(2).bottom().left().expandX().fillX().padBottom(pad20);
        contentSystem.row();
        contentSystem.add(glVersionTitle).align(Align.topLeft).padRight(pad18);
        contentSystem.add(glVersion).align(Align.left);
        contentSystem.row();
        contentSystem.add(glVendorTitle).align(Align.topLeft).padRight(pad18).padTop(pad10);
        contentSystem.add(glVendor).align(Align.left).padTop(pad10);
        contentSystem.row();
        contentSystem.add(glslVersionTitle).align(Align.topLeft).padRight(pad18).padTop(pad10);
        contentSystem.add(glslVersion).align(Align.left).padTop(pad10);
        contentSystem.row();
        contentSystem.add(glExtensionsTitle).align(Align.topLeft).padRight(pad18).padTop(pad10);
        contentSystem.add(glExtensionsScroll).align(Align.left).padTop(pad10);

        OwnScrollPane systemScroll = new OwnScrollPane(contentSystem, skin, "minimalist-nobg");
        systemScroll.setFadeScrollBars(false);
        systemScroll.setScrollingDisabled(true, false);
        systemScroll.setOverscroll(false, false);
        systemScroll.setSmoothScrolling(true);
        systemScroll.setHeight(800f);
        systemScroll.pack();


        /* CONTENT 4 - UPDATES */

        final var contentUpdates = showUpdateTab ? new Table(skin) : null;
        if (showUpdateTab) {
            contentUpdates.top();

            // This is the table that displays it all
            checkTable = new Table(skin);
            checkLabel = new OwnLabel("", skin);

            checkTable.add(checkLabel).top().left().padBottom(pad10).row();
            if (Settings.settings.program.update.lastCheck == null || new Date().getTime() - Settings.settings.program.update.lastCheck.toEpochMilli() > Settings.ProgramSettings.UpdateSettings.VERSION_CHECK_INTERVAL_MS) {
                // Check!
                checkLabel.setText(I18n.msg("gui.newversion.checking"));
                getCheckVersionThread().start();
            } else {
                // Inform latest
                newVersionCheck(Settings.settings.version.version, Settings.settings.version.versionNumber, Settings.settings.version.buildTime, false);
            }
            contentUpdates.add(checkTable).left().top().padTop(pad34);
        }

        /* ADD ALL CONTENT */
        addTabContent(contentHelp);
        addTabContent(contentAbout);
        addTabContent(systemScroll);
        if (showUpdateTab)
            addTabContent(contentUpdates);

        content.add(tabStack).expand().fill();

        // Set tab listeners.
        setUpTabListeners();
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
            getNewVersion.pad(0, pad18, 0, pad18);
            getNewVersion.setHeight(40f);
            getNewVersion.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    Gdx.net.openURI(Settings.WEBPAGE_DOWNLOADS);
                    return true;
                }
                return false;
            });
            checkTable.add(getNewVersion).center().padTop(pad18).padBottom(pad10).row();

            Link link = new Link(uri, linkStyle, uri);
            checkTable.add(link).center();

        } else {
            if (log)
                logger.info(I18n.msg("gui.newversion.nonew", Settings.settings.program.update.getLastCheckedString()));
            checkLabel.setText(I18n.msg("gui.newversion.nonew", Settings.settings.program.update.getLastCheckedString()));
            // Add check now button
            OwnTextButton checkNewVersion = new OwnTextButton(I18n.msg("gui.newversion.checknow"), skin);
            checkNewVersion.pad(pad10, pad18, pad10, pad18);
            checkNewVersion.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    checkLabel.setText(I18n.msg("gui.newversion.checking"));
                    logger.info(I18n.msg("gui.newversion.checking"));
                    getCheckVersionThread().start();
                    return true;
                }
                return false;
            });
            checkTable.add(checkNewVersion).center().padTop(pad18);
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
                    checkTable.add(checkLabel).top().left().padBottom(pad10).row();
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
