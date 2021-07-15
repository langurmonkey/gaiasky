/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.GaiaSky;
import gaiasky.interafce.minimap.*;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.math.Vector3d;
import gaiasky.util.scene2d.OwnTextHotkeyTooltip;
import gaiasky.util.scene2d.TextureWidget;

public class MinimapWidget implements Disposable {
    private final FrameBuffer tfb;
    private final FrameBuffer sfb;
    private final TextureWidget topProjection;
    private final TextureWidget sideProjection;
    int side, side2;
    int sideshort, sideshort2;
    private Vector3d aux3d;

    private final Array<IMinimapScale> scales;
    private IMinimapScale current;

    public MinimapWidget(Skin skin) {
        side = (int) (1.4f * GlobalConf.program.MINIMAP_SIZE);
        side2 = side / 2;
        sideshort = (int) (0.7f * GlobalConf.program.MINIMAP_SIZE);
        sideshort2 = sideshort / 2;

        OrthographicCamera ortho = new OrthographicCamera();

        ShapeRenderer sr = new ShapeRenderer(100, GlobalResources.getShapeShader());
        sr.setAutoShapeType(true);

        SpriteBatch sb = new SpriteBatch(1000, GlobalResources.getSpriteShader());

        BitmapFont font = skin.getFont("ui-20");

        tfb = new FrameBuffer(Format.RGBA8888, side, side, true);
        sfb = new FrameBuffer(Format.RGBA8888, side, sideshort, true);

        topProjection = new TextureWidget(tfb);
        sideProjection = new TextureWidget(sfb);

        // Init scales
        scales = new Array<>();

        InnerSolarSystemMinimapScale issms = new InnerSolarSystemMinimapScale();
        issms.initialize(ortho, sb, sr, font, side, sideshort);
        OuterSolarSystemMinimapScale ossms = new OuterSolarSystemMinimapScale();
        ossms.initialize(ortho, sb, sr, font, side, sideshort);
        HeliosphereMinimapScale hsms = new HeliosphereMinimapScale();
        hsms.initialize(ortho, sb, sr, font, side, sideshort);
        OortCloudMinimapScale ocms = new OortCloudMinimapScale();
        ocms.initialize(ortho, sb, sr, font, side, sideshort);
        SolarNeighbourhoodMinimapScale snms = new SolarNeighbourhoodMinimapScale();
        snms.initialize(ortho, sb, sr, font, side, sideshort);
        MilkyWayMinimapScale mmms = new MilkyWayMinimapScale();
        mmms.initialize(ortho, sb, sr, font, side, sideshort);
        LocalGroup1MinimapScale lg1ms = new LocalGroup1MinimapScale();
        lg1ms.initialize(ortho, sb, sr, font, side, sideshort);
        LocalGroup2MinimapScale lg2ms = new LocalGroup2MinimapScale();
        lg2ms.initialize(ortho, sb, sr, font, side, sideshort);
        HighZMinimapScale hzms = new HighZMinimapScale();
        hzms.initialize(ortho, sb, sr, font, side, sideshort);

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
        topProjection.addListener(new OwnTextHotkeyTooltip(I18n.txt("gui.minimap.title") + " - " + I18n.txt("gui.minimap.top"), minimapHotkey, skin));
        sideProjection.addListener(new OwnTextHotkeyTooltip(I18n.txt("gui.minimap.title") + " - " + I18n.txt("gui.minimap.side"), minimapHotkey, skin));
        aux3d = new Vector3d();
    }

    public void update() {
        ICamera cam = GaiaSky.instance.cam;
        double distSun = cam.getPos().lend();
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

    public void dispose(){
    }
}
