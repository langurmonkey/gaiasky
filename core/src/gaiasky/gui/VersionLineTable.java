package gaiasky.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextTooltip;

/**
 * Produces a table with status information (version, build, build time, OpenGL version, GLSL version, etc.)
 * to be shown at startup.
 */
public class VersionLineTable extends Table {

    public VersionLineTable(Skin skin) {
        this(skin, false);
    }

    public VersionLineTable(Skin skin, boolean vr) {
        super(skin);

        float pad16 = 16f;
        float pad32 = 32f;
        setFillParent(true);
        top().left();
        pad(pad16);

        final Settings settings = Settings.settings;

        String style = vr ? "hud" : "hud-med";

        // Gaia Sky version
        OwnLabel gsVersion = new OwnLabel(settings.version.version, skin, style);
        gsVersion.addListener(new OwnTextTooltip(I18n.msg("gui.help.version", Settings.APPLICATION_NAME), skin));
        add(gsVersion).bottom().left().padRight(pad16);

        // Gaia Sky build
        OwnLabel gsBuild = new OwnLabel(I18n.msg("gui.buildandtime", settings.version.build, settings.version.getBuildTimePretty()), skin, style);
        gsBuild.addListener(new OwnTextTooltip(I18n.msg("gui.help.buildandtime"), skin));
        gsBuild.setColor(ColorUtils.oLightGrayC);
        add(gsBuild).bottom().left().padRight(pad32 * 2f);

        // Graphics device
        OwnLabel device = new OwnLabel(Gdx.gl.glGetString(GL20.GL_RENDERER), skin, style);
        device.addListener(new OwnTextTooltip(I18n.msg("gui.help.graphicsdevice"), skin));
        device.setColor(ColorUtils.oDarkGrayC);
        add(device).bottom().left().padRight(pad32);

        // OpenGL version
        OwnLabel glVersion = new OwnLabel(I18n.msg("notif.glversion", Gdx.gl.glGetString(GL20.GL_VERSION)), skin, style);
        glVersion.addListener(new OwnTextTooltip(I18n.msg("gui.help.openglversion"), skin));
        glVersion.setColor(ColorUtils.oDarkGrayC);
        add(glVersion).bottom().left().padRight(pad32);

        // GLSL version
        OwnLabel glslVersion = new OwnLabel(I18n.msg("notif.glslversion", Gdx.gl.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION)), skin, style);
        glslVersion.addListener(new OwnTextTooltip(I18n.msg("gui.help.glslversion"), skin));
        glslVersion.setColor(ColorUtils.oDarkGrayC);
        add(glslVersion).bottom().left().padRight(pad32);

        // VR
        if (!vr && settings.runtime.openXr) {
            OwnLabel vrLabel = new OwnLabel(TextUtils.surroundBrackets(I18n.msg("gui.vr.mode")), skin, style);
            vrLabel.setColor(ColorUtils.gPinkC);
            vrLabel.addListener(new OwnTextTooltip(I18n.msg("gui.vr.mode.tooltip"), skin));
            add(vrLabel).bottom().left().padRight(pad32);
        }

        // Master instance
        if (settings.program.net.master.active) {
            OwnLabel master = new OwnLabel(TextUtils.surroundBrackets(I18n.msg("gui.master.instance")), skin, style);
            master.setColor(ColorUtils.gBlueC);
            master.addListener(new OwnTextTooltip(I18n.msg("gui.master.instance.tooltip"), skin));
            add(master).bottom().left().padRight(pad32);
        }

        // Slave instance
        if (settings.program.net.slave.active) {
            OwnLabel slave = new OwnLabel(TextUtils.surroundBrackets(I18n.msg("gui.slave.instance")), skin, style);
            slave.setColor(ColorUtils.gYellowC);
            slave.addListener(new OwnTextTooltip(I18n.msg("gui.slave.instance.tooltip"), skin));
            add(slave).colspan(2).bottom().right().padBottom(pad16).row();
        }

        // Safe graphics mode
        if (settings.program.safeMode) {
            OwnLabel safeMode = new OwnLabel(TextUtils.surroundBrackets(I18n.msg("gui.debug.safemode")), skin, style);
            safeMode.setColor(ColorUtils.gRedC);
            safeMode.addListener(new OwnTextTooltip(I18n.msg("gui.debug.safemode.tooltip"), skin));
            add(safeMode).bottom().left().padRight(pad32);
        }

        // Offline mode
        if (settings.program.offlineMode) {
            OwnLabel offlineMode = new OwnLabel(TextUtils.surroundBrackets(I18n.msg("gui.system.offlinemode")), skin, style);
            offlineMode.setColor(ColorUtils.gRedC);
            offlineMode.addListener(new OwnTextTooltip(I18n.msg("gui.system.offlinemode.tooltip"), skin));
            add(offlineMode).bottom().left().padRight(pad32);
        }

    }
}
