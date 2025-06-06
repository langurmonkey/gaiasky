/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.vr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.gui.main.AbstractGui;
import gaiasky.gui.main.VersionLineTable;
import gaiasky.gui.main.WelcomeGui;
import gaiasky.util.Settings;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;

/**
 * A welcome GUI for VR mode. It informs the user to manage the datasets in the main screen.
 */
public class WelcomeGuiVR extends AbstractGui {

    private Table center, bottom;
    private final WelcomeGui wg;

    public WelcomeGuiVR(final Skin skin, final Graphics graphics, final Float unitsPerPixel, final Boolean vr) {
        super(graphics, unitsPerPixel);
        wg = (WelcomeGui) GaiaSky.instance.welcomeGui;
        this.skin = skin;
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        FixedScreenViewport vp = new FixedScreenViewport(getBackBufferWidth(), getBackBufferHeight());
        stage = new Stage(vp, sb);

        center = new Table();
        center.setFillParent(true);
        center.center();

        String textStyle = "header-raw";

        // Bottom info.
        bottom = new VersionLineTable(skin, true);
        bottom.bottom();

        // Logo.
        FileHandle gsIcon = Gdx.files.internal("icon/gs_icon_256.png");
        Texture iconTex = new Texture(gsIcon);
        iconTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image logo = new Image(iconTex);
        logo.setScale(0.75f);
        logo.setOrigin(Align.center);
        center.add(logo).padBottom(20f).row();
        // Title.
        HorizontalGroup titleGroup = new HorizontalGroup();
        titleGroup.space(64f);
        OwnLabel gaiaSky = new OwnLabel(Settings.getApplicationTitle(Settings.settings.runtime.openXr), skin, "main-title");
        OwnLabel version = new OwnLabel(Settings.settings.version.version, skin, "main-title");
        version.setColor(skin.getColor("theme"));
        titleGroup.addActor(gaiaSky);
        titleGroup.addActor(version);
        center.add(titleGroup).padBottom(110f).row();

        // Check window!
        var w1 = new OwnLabel(I18n.msg("gui.vr.welcome.1"), skin, textStyle);
        w1.setAlignment(Align.center);
        center.add(w1).padBottom(40f).row();
        if(wg.baseDataPresent()) {
            var w2 = new OwnLabel(I18n.msg("gui.vr.welcome.2"), skin, textStyle);
            w2.setAlignment(Align.center);
            var w3 = new OwnLabel(I18n.msg("gui.vr.welcome.3"), skin, "header-blue");
            w3.setAlignment(Align.center);
            center.add(w2).row();
            center.add(w3);
        } else {
            var w2 = new OwnLabel(I18n.msg("gui.welcome.start.nobasedata"), skin, textStyle);
            w2.setColor(ColorUtils.aOrangeC);
            w2.setAlignment(Align.center);
            center.add(w2);
        }

        rebuildGui();
    }

    @Override
    public void doneLoading(AssetManager assetManager) {

    }

    @Override
    protected void rebuildGui() {
        if (stage != null) {
            stage.clear();
            stage.addActor(center);
            stage.addActor(bottom);
        }
    }
}
