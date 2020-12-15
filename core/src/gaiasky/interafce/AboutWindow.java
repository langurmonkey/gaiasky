/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

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
import gaiasky.desktop.util.SysUtils;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.IDateFormat;
import gaiasky.util.scene2d.*;
import gaiasky.util.update.VersionCheckEvent;
import gaiasky.util.update.VersionChecker;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

import java.nio.IntBuffer;
import java.time.Instant;
import java.util.Date;

/**
 * The help window with About, Help and System sections.
 *
 * @author tsagrista
 */
public class AboutWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(AboutWindow.class);

    private final LabelStyle linkStyle;
    private Table checkTable;
    private OwnLabel checkLabel;

    public AboutWindow(Stage stage, Skin skin) {
        super(I18n.txt("gui.help.help") + " - " + GlobalConf.version.version + " - " + I18n.txt("gui.build", GlobalConf.version.build), skin, stage);
        this.linkStyle = skin.get("link", LabelStyle.class);

        setCancelText(I18n.txt("gui.close"));

        // Build
        buildSuper();

    }

    @Override
    protected void build() {
        float taWidth = 700f;
        float taWidth2 = 1280f;
        float taHeight = 160f;
        float tabWidth = 240f;

        // Create the tab buttons
        HorizontalGroup group = new HorizontalGroup();
        group.align(Align.left);

        final Button tabHelp = new OwnTextButton(I18n.txt("gui.help.help"), skin, "toggle-big");
        tabHelp.pad(pad5);
        tabHelp.setWidth(tabWidth);
        final Button tabAbout = new OwnTextButton(I18n.txt("gui.help.about"), skin, "toggle-big");
        tabAbout.pad(pad5);
        tabAbout.setWidth(tabWidth);
        final Button tabSystem = new OwnTextButton(I18n.txt("gui.help.system"), skin, "toggle-big");
        tabSystem.pad(pad5);
        tabSystem.setWidth(tabWidth);
        final Button tabUpdates = new OwnTextButton(I18n.txt("gui.newversion"), skin, "toggle-big");
        tabUpdates.pad(pad5);
        tabUpdates.setWidth(tabWidth);

        group.addActor(tabHelp);
        group.addActor(tabAbout);
        group.addActor(tabSystem);
        group.addActor(tabUpdates);
        content.add(group).align(Align.left).padLeft(pad5);
        content.row();
        content.pad(pad10);

        // Create the tab content. Just using images here for simplicity.
        Stack tabContent = new Stack();

        /* CONTENT 1 - HELP */
        final Table contentHelp = new Table(skin);
        contentHelp.align(Align.top);

        OwnLabel gaiasky = new OwnLabel(GlobalConf.getApplicationTitle(GlobalConf.runtime.OPENVR), skin, "main-title");

        // User manual
        Label homepageTitle = new OwnLabel(I18n.txt("gui.help.homepage"), skin);
        Label homepageTxt = new OwnLabel(I18n.txt("gui.help.help1"), skin);
        Link homepageLink = new Link(GlobalConf.WEBPAGE, linkStyle, GlobalConf.WEBPAGE);

        // Wiki
        Label docsTitle = new OwnLabel("Docs", skin);
        Label docsTxt = new OwnLabel(I18n.txt("gui.help.help2"), skin);
        Link docsLink = new Link(GlobalConf.DOCUMENTATION, linkStyle, GlobalConf.DOCUMENTATION);

        // Icon
        FileHandle gsIcon = Gdx.files.internal(GlobalConf.runtime.OPENVR ? "icon/gsvr_icon.png" : "icon/gs_icon.png");
        Texture iconTex = new Texture(gsIcon);
        iconTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Image gaiaskyIcon = new Image(iconTex);
        gaiaskyIcon.setOrigin(Align.center);

        // Add all to content
        contentHelp.add(gaiasky).pad(pad10 * 2f).padBottom(pad10 * 5f).colspan(2);
        contentHelp.row();
        contentHelp.add(homepageTitle).align(Align.left).padRight(pad10);
        contentHelp.add(homepageTxt).align(Align.left);
        contentHelp.row();
        contentHelp.add(new OwnLabel("", skin));
        contentHelp.add(homepageLink).align(Align.left).padBottom(pad10);
        contentHelp.row();
        contentHelp.add(docsTitle).align(Align.left).padRight(pad10).padBottom(pad10);
        contentHelp.add(docsTxt).align(Align.left);
        contentHelp.row();
        contentHelp.add(new OwnLabel("", skin));
        contentHelp.add(docsLink).align(Align.left).padBottom(pad10 * 4f);
        contentHelp.row();
        contentHelp.add(gaiaskyIcon).colspan(2).align(Align.center);
        contentHelp.pack();

        /* CONTENT 2 - ABOUT */
        final Table contentAbout = new Table(skin);
        contentAbout.top().left();

        // Intro
        TextArea intro = new OwnTextArea(I18n.txt("gui.help.gscredits", GlobalConf.version.version), skin.get("regular", TextFieldStyle.class));
        intro.setDisabled(true);
        intro.setPrefRows(3);
        intro.setWidth(contentHelp.getWidth());

        // Home page
        Label homepagetitle = new OwnLabel(I18n.txt("gui.help.homepage"), skin);
        Link homepage = new Link(GlobalConf.WEBPAGE, linkStyle, GlobalConf.WEBPAGE);

        // Twitter
        Label twtitle = new OwnLabel(I18n.txt("gui.help.twitter"), skin);
        Link tw = new Link("@GaiaSky_Dev", linkStyle, "https://twitter.com/GaiaSky_Dev");

        // Author
        Label authortitle = new OwnLabel(I18n.txt("gui.help.author"), skin);

        Table author = new Table(skin);
        Label authorname = new OwnLabel(GlobalConf.AUTHOR_NAME, skin);
        Link authormail = new Link(GlobalConf.AUTHOR_EMAIL, linkStyle, "mailto:" + GlobalConf.AUTHOR_EMAIL);
        Link authorpage = new Link("www.tonisagrista.com", linkStyle, "https://tonisagrista.com");
        Link authormasto = new Link("@jumpinglangur@mastodont.cat", linkStyle, "https://mastodont.cat/@jumpinglangur");
        author.add(authorname).left().row();
        author.add(authormail).left().row();
        author.add(authorpage).left().row();
        author.add(authormasto).left().row();

        // Contributor
        Label contribtitle = new OwnLabel(I18n.txt("gui.help.contributors"), skin);

        Table contrib = new Table(skin);
        contrib.align(Align.left);
        Label contribname = new OwnLabel("Apl. Prof. Dr. Stefan Jordan", skin);
        Link contribmail = new Link("jordan@ari.uni-heidelberg.de", linkStyle, "mailto:jordan@ari.uni-heidelberg.de");
        contrib.add(contribname).left().row();
        contrib.add(contribmail).left().row();

        // License
        HorizontalGroup licenseh = new HorizontalGroup();
        licenseh.space(pad10 * 2f);

        VerticalGroup licensev = new VerticalGroup();
        TextArea licensetext = new OwnTextArea(I18n.txt("gui.help.license"), skin.get("regular", TextFieldStyle.class));
        licensetext.setDisabled(true);
        licensetext.setPrefRows(3);
        licensetext.setWidth(taWidth2 / 2f);
        Link licenselink = new Link("https://opensource.org/licenses/MPL-2.0", linkStyle, "https://opensource.org/licenses/MPL-2.0");

        licensev.addActor(licensetext);
        licensev.addActor(licenselink);

        licenseh.addActor(licensev);

        // Thanks
        HorizontalGroup thanks = new HorizontalGroup();
        thanks.space(pad10 * 2f).pad(pad10);
        Container<Actor> thanksc = new Container<>(thanks);
        thanksc.setBackground(skin.getDrawable("bg-clear"));

        Image zah = new Image(getSpriteDrawable(Gdx.files.internal("img/zah.png")));
        Image dlr = new Image(getSpriteDrawable(Gdx.files.internal("img/dlr.png")));
        Image bwt = new Image(getSpriteDrawable(Gdx.files.internal("img/bwt.png")));
        Image dpac = new Image(getSpriteDrawable(Gdx.files.internal("img/dpac.png")));

        thanks.addActor(zah);
        thanks.addActor(dlr);
        thanks.addActor(bwt);
        thanks.addActor(dpac);

        contentAbout.add(intro).colspan(2).left().padTop(pad10 * 2f);
        contentAbout.row();
        contentAbout.add(homepagetitle).left().padRight(pad10 * 2f).padTop(pad10 * 2f);
        contentAbout.add(homepage).left().padTop(pad10 * 2f);
        contentAbout.row();
        contentAbout.add(twtitle).left().padRight(pad10 * 2f).padTop(pad10 * 2f);
        contentAbout.add(tw).left().padTop(pad10 * 2f);
        contentAbout.row();
        contentAbout.add(authortitle).left().padRight(pad10).padTop(pad10 * 2f);
        contentAbout.add(author).left().padTop(pad5).padTop(pad10 * 2f);
        contentAbout.row();
        contentAbout.add(contribtitle).left().padRight(pad10).padTop(pad10 * 2f);
        contentAbout.add(contrib).left().padTop(pad10 * 2f);
        contentAbout.row();
        contentAbout.add(licenseh).colspan(2).center().padTop(pad10 * 2f);
        contentAbout.row();
        contentAbout.add(thanksc).colspan(2).center().padTop(pad10 * 4f);

        /* CONTENT 3 - SYSTEM */
        final Table contentSystem = new Table(skin);
        contentSystem.top().left();

        // Build info
        Label buildinfo = new OwnLabel(I18n.txt("gui.help.buildinfo"), skin, "header");

        Label versiontitle = new OwnLabel(I18n.txt("gui.help.version", GlobalConf.APPLICATION_NAME), skin);
        Label version = new OwnLabel(GlobalConf.version.version, skin);

        Label revisiontitle = new OwnLabel(I18n.txt("gui.help.buildnumber"), skin);
        Label revision = new OwnLabel(GlobalConf.version.build, skin);

        Label timetitle = new OwnLabel(I18n.txt("gui.help.buildtime"), skin);
        Label time = new OwnLabel(GlobalConf.version.buildtime.toString(), skin);

        Label systemtitle = new OwnLabel(I18n.txt("gui.help.buildsys"), skin);
        TextArea system = new OwnTextArea(GlobalConf.version.system, skin.get("regular", TextFieldStyle.class));
        system.setDisabled(true);
        system.setPrefRows(3);
        system.setWidth(taWidth * 2f / 3f);

        Label buildertitle = new OwnLabel(I18n.txt("gui.help.builder"), skin);
        Label builder = new OwnLabel(GlobalConf.version.builder, skin);

        // Paths
        Label paths = new OwnLabel(I18n.txt("gui.help.paths"), skin, "header");

        Label configtitle = new OwnLabel(I18n.txt("gui.help.paths.config"), skin);
        Label config = new OwnLabel(SysUtils.getConfigDir().toAbsolutePath().toString(), skin);
        Label datatitle = new OwnLabel(I18n.txt("gui.help.paths.data"), skin);
        Label data = new OwnLabel(SysUtils.getDataDir().toAbsolutePath().toString(), skin);
        Label screenshotstitle = new OwnLabel(I18n.txt("gui.help.paths.screenshots"), skin);
        Label screenshots = new OwnLabel(SysUtils.getDefaultScreenshotsDir().toAbsolutePath().toString(), skin);
        Label framestitle = new OwnLabel(I18n.txt("gui.help.paths.frames"), skin);
        Label frames = new OwnLabel(SysUtils.getDefaultFramesDir().toAbsolutePath().toString(), skin);
        Label musictitle = new OwnLabel(I18n.txt("gui.help.paths.music"), skin);
        Label music = new OwnLabel(SysUtils.getDefaultMusicDir().toAbsolutePath().toString(), skin);
        Label mappingstitle = new OwnLabel(I18n.txt("gui.help.paths.mappings"), skin);
        Label mappings = new OwnLabel(SysUtils.getDefaultMappingsDir().toAbsolutePath().toString(), skin);
        Label cameratitle = new OwnLabel(I18n.txt("gui.help.paths.camera"), skin);
        Label camera = new OwnLabel(SysUtils.getDefaultCameraDir().toAbsolutePath().toString(), skin);


        // Java info
        Label javainfo = new OwnLabel(I18n.txt("gui.help.javainfo"), skin, "header");

        Label javaversiontitle = new OwnLabel(I18n.txt("gui.help.javaversion"), skin);
        Label javaversion = new OwnLabel(System.getProperty("java.version"), skin);

        Label javaruntimetitle = new OwnLabel(I18n.txt("gui.help.javaname"), skin);
        Label javaruntime = new OwnLabel(System.getProperty("java.runtime.name"), skin);

        Label javavmnametitle = new OwnLabel(I18n.txt("gui.help.javavmname"), skin);
        Label javavmname = new OwnLabel(System.getProperty("java.vm.name"), skin);

        Label javavmversiontitle = new OwnLabel(I18n.txt("gui.help.javavmversion"), skin);
        Label javavmversion = new OwnLabel(System.getProperty("java.vm.version"), skin);

        Label javavmvendortitle = new OwnLabel(I18n.txt("gui.help.javavmvendor"), skin);
        Label javavmvendor = new OwnLabel(System.getProperty("java.vm.vendor"), skin);

        TextButton memoryinfobutton = new OwnTextButton(I18n.txt("gui.help.meminfo"), skin, "default");
        memoryinfobutton.setName("memoryinfo");
        memoryinfobutton.setSize(240f, 32f);
        memoryinfobutton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.DISPLAY_MEM_INFO_WINDOW);
                return true;
            }
            return false;
        });

        // System info
        Label sysinfo = new OwnLabel(I18n.txt("gui.help.sysinfo"), skin, "header");


        Label sysostitle = new OwnLabel(I18n.txt("gui.help.os"), skin);
        Label sysos = new OwnLabel(System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"), skin);

        Label glrenderertitle = new OwnLabel(I18n.txt("gui.help.graphicsdevice"), skin);
        Label glrenderer = new OwnLabel(Gdx.gl.glGetString(GL20.GL_RENDERER), skin);

        // OpenGL info
        Label glinfo = new OwnLabel(I18n.txt("gui.help.openglinfo"), skin, "header");

        Label glvendortitle = new OwnLabel(I18n.txt("gui.help.glvendor"), skin);
        Label glvendor = new OwnLabel(Gdx.gl.glGetString(GL20.GL_VENDOR), skin);

        Label glversiontitle = new OwnLabel(I18n.txt("gui.help.openglversion"), skin);
        Label glversion = new OwnLabel(Gdx.gl.glGetString(GL20.GL_VERSION), skin);

        Label glslversiontitle = new OwnLabel(I18n.txt("gui.help.glslversion"), skin);
        Label glslversion = new OwnLabel(Gdx.gl.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION), skin);

        Label glextensionstitle = new OwnLabel(I18n.txt("gui.help.glextensions"), skin);
        String extensions = GlobalResources.getGLExtensions();

        IntBuffer buf = BufferUtils.newIntBuffer(16);
        Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, buf);
        int maxSize = buf.get(0);
        int lines = GlobalResources.countOccurrences(extensions, '\n');
        OwnTextArea maxTexSize = new OwnTextArea("Max texture size: " + maxSize + '\n' + extensions, skin, "disabled-nobg");
        maxTexSize.setDisabled(true);
        maxTexSize.setPrefRows(lines);
        maxTexSize.clearListeners();

        OwnScrollPane glextensionsscroll = new OwnScrollPane(maxTexSize, skin, "default-nobg");
        glextensionsscroll.setWidth(taWidth);
        glextensionsscroll.setHeight(taHeight);
        glextensionsscroll.setForceScroll(false, true);
        glextensionsscroll.setSmoothScrolling(true);
        glextensionsscroll.setFadeScrollBars(false);
        scrolls.add(glextensionsscroll);

        // BUILD
        contentSystem.add(buildinfo).colspan(2).align(Align.left).padTop(pad10 * 2f).padBottom(pad5);
        contentSystem.row();
        contentSystem.add(versiontitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(version).align(Align.left);
        contentSystem.row();
        contentSystem.add(revisiontitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(revision).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(timetitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(time).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(buildertitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(builder).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(systemtitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(system).align(Align.left).padTop(pad5);
        contentSystem.row();

        // PATHS
        contentSystem.add(paths).colspan(2).align(Align.left).padTop(pad10 * 2f).padBottom(pad5);
        contentSystem.row();
        contentSystem.add(configtitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(config).align(Align.left);
        contentSystem.row();
        contentSystem.add(datatitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(data).align(Align.left);
        contentSystem.row();
        contentSystem.add(screenshotstitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(screenshots).align(Align.left);
        contentSystem.row();
        contentSystem.add(framestitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(frames).align(Align.left);
        contentSystem.row();
        contentSystem.add(cameratitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(camera).align(Align.left);
        contentSystem.row();
        contentSystem.add(mappingstitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(mappings).align(Align.left);
        contentSystem.row();
        contentSystem.add(musictitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(music).align(Align.left);
        contentSystem.row();

        // JAVA
        contentSystem.add(javainfo).colspan(2).align(Align.left).padTop(pad10 * 2f).padBottom(pad5);
        contentSystem.row();
        contentSystem.add(javaversiontitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(javaversion).align(Align.left);
        contentSystem.row();
        contentSystem.add(javaruntimetitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(javaruntime).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(javavmnametitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(javavmname).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(javavmversiontitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(javavmversion).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(javavmvendortitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(javavmvendor).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(memoryinfobutton).colspan(2).align(Align.left).padTop(pad10);
        contentSystem.row();

        // SYSTEM
        contentSystem.add(sysinfo).colspan(2).align(Align.left).padTop(pad10 * 2f).padBottom(pad5);
        contentSystem.row();
        try {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hal = si.getHardware();
            CentralProcessor cp = hal.getProcessor();

            Label cputitle = new OwnLabel(I18n.txt("gui.help.cpu"), skin);
            Label cpu = new OwnLabel(cp.getName(), skin);

            Label cpuarchtitle = new OwnLabel(I18n.txt("gui.help.cpuarch"), skin);
            Label cpuarch = new OwnLabel(cp.isCpu64bit() ? "64-bit" : "32-bit", skin);

            contentSystem.add(cputitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
            contentSystem.add(cpu).align(Align.left).padTop(pad5);
            contentSystem.row();

            contentSystem.add(cpuarchtitle).align(Align.topLeft).padRight(pad10).padTop(pad5).padBottom(pad5);
            contentSystem.add(cpuarch).align(Align.left).padTop(pad5).padBottom(pad5);
            contentSystem.row();
        }catch(Error e){
            contentSystem.add(new OwnLabel(I18n.txt("gui.help.cpu.no"), skin)).colspan(2).align(Align.left).padTop(pad10).padBottom(pad10).row();
        }
        contentSystem.add(sysostitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(sysos).align(Align.left);
        contentSystem.row();
        contentSystem.add(glrenderertitle).align(Align.topLeft).padRight(pad10).padTop(pad10);
        contentSystem.add(glrenderer).align(Align.left).padTop(pad5);
        contentSystem.row();

        // GL
        contentSystem.add(glinfo).colspan(2).align(Align.left).padTop(pad10 * 2f).padBottom(pad5);
        contentSystem.row();
        contentSystem.add(glversiontitle).align(Align.topLeft).padRight(pad10);
        contentSystem.add(glversion).align(Align.left);
        contentSystem.row();
        contentSystem.add(glvendortitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(glvendor).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(glslversiontitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(glslversion).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(glextensionstitle).align(Align.topLeft).padRight(pad10).padTop(pad5);
        contentSystem.add(glextensionsscroll).align(Align.left).padTop(pad5);

        OwnScrollPane systemScroll = new OwnScrollPane(contentSystem, skin, "minimalist-nobg");
        systemScroll.setFadeScrollBars(false);
        systemScroll.setScrollingDisabled(true, false);
        systemScroll.setOverscroll(false, false);
        systemScroll.setSmoothScrolling(true);
        systemScroll.setHeight(800f);
        systemScroll.pack();


        /* CONTENT 4 - UPDATES */
        final Table contentUpdates = new Table(skin);
        contentUpdates.align(Align.top);

        // This is the table that displays it all
        checkTable = new Table(skin);
        checkLabel = new OwnLabel("", skin);

        checkTable.add(checkLabel).top().left().padBottom(pad5).row();
        if (GlobalConf.program.VERSION_LAST_TIME == null || new Date().getTime() - GlobalConf.program.VERSION_LAST_TIME.toEpochMilli() > GlobalConf.ProgramConf.VERSION_CHECK_INTERVAL_MS) {
            // Check!
            checkLabel.setText(I18n.txt("gui.newversion.checking"));
            getCheckVersionThread().start();
        } else {
            // Inform latest
            newVersionCheck(GlobalConf.version.version, GlobalConf.version.versionNumber, GlobalConf.version.buildtime, false);
        }

        contentUpdates.add(checkTable).left().top().padTop(pad10 * 2f);

        /** ADD ALL CONTENT **/
        tabContent.addActor(contentHelp);
        tabContent.addActor(contentAbout);
        tabContent.addActor(systemScroll);
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
                contentUpdates.setVisible(tabUpdates.isChecked());
            }
        };
        tabHelp.addListener(tabListener);
        tabAbout.addListener(tabListener);
        tabSystem.addListener(tabListener);
        tabUpdates.addListener(tabListener);

        // Let only one tab button be checked at a time
        ButtonGroup<Button> tabs = new ButtonGroup<>();
        tabs.setMinCheckCount(1);
        tabs.setMaxCheckCount(1);
        tabs.add(tabHelp);
        tabs.add(tabAbout);
        tabs.add(tabSystem);
        tabs.add(tabUpdates);

    }

    private boolean exists(FileHandle fh) {
        try {
            fh.read().close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void accept() {
    }

    @Override
    protected void cancel() {
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
        GlobalConf.program.VERSION_LAST_TIME = Instant.now();
        if (versionNumber > GlobalConf.version.versionNumber) {
            IDateFormat df = DateFormatFactory.getFormatter(I18n.locale, DateFormatFactory.DateType.DATETIME);
            if (log) {
                logger.info(I18n.txt("gui.newversion.available", GlobalConf.version.version, tagVersion + " [" + df.format(tagDate) + "]"));
            }
            // There's a new version!
            checkLabel.setText(I18n.txt("gui.newversion.available", GlobalConf.version, tagVersion + " [" + df.format(tagDate) + "]"));
            final String uri = GlobalConf.WEBPAGE_DOWNLOADS;

            OwnTextButton getNewVersion = new OwnTextButton(I18n.txt("gui.newversion.getit"), skin);
            getNewVersion.pad(pad5, pad10, pad5, pad10);
            getNewVersion.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    Gdx.net.openURI(GlobalConf.WEBPAGE_DOWNLOADS);
                    return true;
                }
                return false;
            });
            checkTable.add(getNewVersion).center().padTop(pad10).padBottom(pad5).row();

            Link link = new Link(uri, linkStyle, uri);
            checkTable.add(link).center();

        } else {
            if (log)
                logger.info(I18n.txt("gui.newversion.nonew", GlobalConf.program.getLastCheckedString()));
            checkLabel.setText(I18n.txt("gui.newversion.nonew", GlobalConf.program.getLastCheckedString()));
            // Add check now button
            OwnTextButton checkNewVersion = new OwnTextButton(I18n.txt("gui.newversion.checknow"), skin);
            checkNewVersion.pad(pad5, pad10, pad5, pad10);
            checkNewVersion.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    checkLabel.setText(I18n.txt("gui.newversion.checking"));
                    logger.info(I18n.txt("gui.newversion.checking"));
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
        VersionChecker vc = new VersionChecker(GlobalConf.program.VERSION_CHECK_URL);
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
                    logger.info(I18n.txt("gui.newversion.fail"));
                    checkLabel.setText(I18n.txt("notif.error", "Could not get last version"));
                    checkLabel.setColor(Color.RED);
                }
            }
            return false;
        });
        return new Thread(vc);
    }

}
