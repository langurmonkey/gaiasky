package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaiasky.util.I18n;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextTooltip;

public class VersionLineTable extends Table {
    public VersionLineTable(Skin skin) {
        super(skin);

        float pad16 = 16f;
        float pad32 = 32f;
        setFillParent(true);
        top().left();
        pad(pad16);

        final Settings settings = Settings.settings;

        // Gaia Sky version
        OwnLabel gsVersion = new OwnLabel(settings.version.version, skin, "hud-big");
        gsVersion.addListener(new OwnTextTooltip(I18n.txt("gui.help.version", Settings.APPLICATION_NAME), skin));
        add(gsVersion).bottom().left().padRight(pad16);

        // Gaia Sky build
        OwnLabel gsBuild = new OwnLabel(I18n.txt("gui.buildandtime", settings.version.build, settings.version.getBuildTimePretty()), skin, "hud-med");
        gsBuild.addListener(new OwnTextTooltip(I18n.txt("gui.help.buildandtime"), skin));
        gsBuild.setColor(ColorUtils.oLightGrayC);
        add(gsBuild).bottom().left().padRight(pad32 * 2f);

        // Graphics device
        OwnLabel device = new OwnLabel(Gdx.gl.glGetString(GL20.GL_RENDERER), skin, "hud-med");
        device.addListener(new OwnTextTooltip(I18n.txt("gui.help.graphicsdevice"), skin));
        device.setColor(ColorUtils.oDarkGrayC);
        add(device).bottom().left().padRight(pad32);

        // OpenGL version
        OwnLabel glVersion = new OwnLabel(I18n.txt("notif.glversion", Gdx.gl.glGetString(GL20.GL_VERSION)), skin, "hud-med");
        glVersion.addListener(new OwnTextTooltip(I18n.txt("gui.help.openglversion"), skin));
        glVersion.setColor(ColorUtils.oDarkGrayC);
        add(glVersion).bottom().left().padRight(pad32);

        // GLSL version
        OwnLabel glslVersion = new OwnLabel(I18n.txt("notif.glslversion", Gdx.gl.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION)), skin, "hud-med");
        glslVersion.addListener(new OwnTextTooltip(I18n.txt("gui.help.glslversion"), skin));
        glslVersion.setColor(ColorUtils.oDarkGrayC);
        add(glslVersion).bottom().left().padRight(pad32);

        // VR
        if (settings.runtime.openVr) {
            OwnLabel vr = new OwnLabel(TextUtils.surroundBrackets(I18n.txt("gui.vr.mode")), skin, "hud-med");
            vr.setColor(ColorUtils.gPinkC);
            vr.addListener(new OwnTextTooltip(I18n.txt("gui.vr.mode.tooltip"), skin));
            add(vr).bottom().left().padRight(pad32);
        }

        // Master instance
        if (settings.program.net.master.active) {
            OwnLabel master = new OwnLabel(TextUtils.surroundBrackets(I18n.txt("gui.master.instance")), skin, "hud-med");
            master.setColor(ColorUtils.gBlueC);
            master.addListener(new OwnTextTooltip(I18n.txt("gui.master.instance.tooltip"), skin));
            add(master).bottom().left().padRight(pad32);
        }

        // Slave instance
        if (settings.program.net.slave.active) {
            OwnLabel slave = new OwnLabel(TextUtils.surroundBrackets(I18n.txt("gui.slave.instance")), skin, "hud-med");
            slave.setColor(ColorUtils.gYellowC);
            slave.addListener(new OwnTextTooltip(I18n.txt("gui.slave.instance.tooltip"), skin));
            add(slave).colspan(2).bottom().right().padBottom(pad16).row();
        }

        // Safe graphics mode
        if (settings.program.safeMode) {
            OwnLabel safeMode = new OwnLabel(TextUtils.surroundBrackets(I18n.txt("gui.debug.safemode")), skin, "hud-med");
            safeMode.setColor(ColorUtils.gRedC);
            safeMode.addListener(new OwnTextTooltip(I18n.txt("gui.debug.safemode.tooltip"), skin));
            add(safeMode).bottom().left().padRight(pad32);
        }

    }
}
