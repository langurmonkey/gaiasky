/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.GaiaSky;
import gaiasky.gui.minimap.*;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3d;
import gaiasky.util.scene2d.OwnTextHotkeyTooltip;
import gaiasky.util.scene2d.TextureWidget;

public class MinimapWidget implements Disposable {
    private final FrameBuffer tfb;
    private final FrameBuffer sfb;
    private final TextureWidget topProjection;
    private final TextureWidget sideProjection;
    private final Vector3d aux3d;
    private final Array<IMinimapScale> scales;
    int side, side2;
    int sideShort, sideShort2;
    private IMinimapScale current;

    public MinimapWidget(final Skin skin, final ShaderProgram shapeShader, final ShaderProgram spriteShader) {
        side = (int) (1.4f * Settings.settings.program.minimap.size);
        side2 = side / 2;
        sideShort = (int) (0.7f * Settings.settings.program.minimap.size);
        sideShort2 = sideShort / 2;

        OrthographicCamera orthographic = new OrthographicCamera();

        ShapeRenderer sr = new ShapeRenderer(200, shapeShader);
        sr.setAutoShapeType(true);

        SpriteBatch sb = new SpriteBatch(1000, spriteShader);

        BitmapFont font = skin.getFont("ui-23");

        tfb = new FrameBuffer(Format.RGBA8888, side, side, true);
        sfb = new FrameBuffer(Format.RGBA8888, side, sideShort, true);

        topProjection = new TextureWidget(tfb);
        sideProjection = new TextureWidget(sfb);

        // Init scales
        scales = new Array<>();

        InnerSolarSystemMinimapScale issms = new InnerSolarSystemMinimapScale();
        issms.initialize(orthographic, sb, sr, font, side, sideShort);
        OuterSolarSystemMinimapScale ossms = new OuterSolarSystemMinimapScale();
        ossms.initialize(orthographic, sb, sr, font, side, sideShort);
        HeliosphereMinimapScale hsms = new HeliosphereMinimapScale();
        hsms.initialize(orthographic, sb, sr, font, side, sideShort);
        OortCloudMinimapScale ocms = new OortCloudMinimapScale();
        ocms.initialize(orthographic, sb, sr, font, side, sideShort);
        SolarNeighbourhoodMinimapScale snms = new SolarNeighbourhoodMinimapScale();
        snms.initialize(orthographic, sb, sr, font, side, sideShort);
        MilkyWayMinimapScale mmms = new MilkyWayMinimapScale();
        mmms.initialize(orthographic, sb, sr, font, side, sideShort);
        LocalGroup1MinimapScale lg1ms = new LocalGroup1MinimapScale();
        lg1ms.initialize(orthographic, sb, sr, font, side, sideShort);
        LocalGroup2MinimapScale lg2ms = new LocalGroup2MinimapScale();
        lg2ms.initialize(orthographic, sb, sr, font, side, sideShort);
        HighZMinimapScale hzms = new HighZMinimapScale();
        hzms.initialize(orthographic, sb, sr, font, side, sideShort);

        scales.add(issms);
        scales.add(ossms);
        scales.add(hsms);
        scales.add(ocms);
        scales.add(snms);
        scales.add(mmms);
        scales.add(lg1ms);
        scales.add(lg2ms);
        scales.add(hzms);

        current = null;

        String minimapHotkey = KeyBindings.instance.getStringKeys("action.toggle/gui.minimap.title");
        topProjection.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.minimap.title") + " - " + I18n.msg("gui.minimap.top"), minimapHotkey, skin));
        sideProjection.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.minimap.title") + " - " + I18n.msg("gui.minimap.side"), minimapHotkey, skin));
        aux3d = new Vector3d();
    }

    public void update() {
        ICamera cam = GaiaSky.instance.cameraManager;
        double distSun = cam.getPos().lenDouble();
        for (IMinimapScale mms : scales) {
            if (mms.isActive(cam.getPos().tov3d(aux3d), distSun)) {
                mms.update();
                mms.renderSideProjection(sfb);
                mms.renderTopProjection(tfb);
                if (current == null || current != mms) {
                    current = mms;
                }
                break;
            }
        }
    }

    public TextureWidget getSideProjection() {
        return sideProjection;
    }

    public TextureWidget getTopProjection() {
        return topProjection;
    }

    public String getCurrentName() {
        if (current != null)
            return current.getName();
        else
            return null;
    }

    public void dispose() {
    }
}
