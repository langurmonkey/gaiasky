/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.interfce.minimap.*;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.TextureWidget;

public class MinimapWindow extends GenericDialog {
    private FrameBuffer tfb, sfb;
    private TextureWidget topProjection, sideProjection;
    int side, side2;
    int sideshort, sideshort2;

    private OwnLabel mapName;

    private Array<IMinimapScale> scales;
    private IMinimapScale current;

    public MinimapWindow(Stage stage, Skin skin) {
        super(I18n.txt("gui.minimap.title"), skin, stage);
        side = (int) (GlobalConf.UI_SCALE_FACTOR * 225);
        side2 = side / 2;
        sideshort = (int) (GlobalConf.UI_SCALE_FACTOR * 112.5);
        sideshort2 = sideshort / 2;

        setModal(false);

        OrthographicCamera ortho = new OrthographicCamera();

        ShapeRenderer sr = new ShapeRenderer();
        sr.setAutoShapeType(true);

        SpriteBatch sb = new SpriteBatch(1000, GlobalResources.spriteShader);

        BitmapFont font = skin.getFont(GlobalConf.UI_SCALE_FACTOR != 1 ? "ui-20" : "ui-11");

        tfb = new FrameBuffer(Format.RGBA8888, side, side, true);
        sfb = new FrameBuffer(Format.RGBA8888, side, sideshort, true);

        topProjection = new TextureWidget(tfb);
        sideProjection = new TextureWidget(sfb);

        setCancelText(I18n.txt("gui.close"));

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
        LocalGroupMinimapScale lgms = new LocalGroupMinimapScale();
        lgms.initialize(ortho, sb, sr, font, side, sideshort);

        scales.add(issms);
        scales.add(ossms);
        scales.add(hsms);
        scales.add(ocms);
        scales.add(snms);
        scales.add(mmms);
        scales.add(lgms);

        current = null;

        // Build
        buildSuper();

        // Pack
        pack();

    }

    @Override
    protected void build() {
        float pb = 10 * GlobalConf.UI_SCALE_FACTOR;
        mapName = new OwnLabel("", skin, "header");
        OwnLabel headerSide = new OwnLabel(I18n.txt("gui.minimap.side"), skin);
        Container<TextureWidget> mapSide = new Container<>();
        mapSide.setActor(sideProjection);
        OwnLabel headerTop = new OwnLabel(I18n.txt("gui.minimap.top"), skin);
        Container<TextureWidget> mapTop = new Container<>();
        mapTop.setActor(topProjection);

        content.add(mapName).left().padBottom(pad).row();

        content.add(headerSide).left().padBottom(pb).row();
        content.add(sideProjection).left().padBottom(pb).row();

        content.add(headerTop).left().padBottom(pb).row();
        content.add(topProjection).left();

    }

    @Override
    protected void accept() {
    }

    @Override
    protected void cancel() {
    }

    private void updateMapName(String mapName){
        if(this.mapName != null)
            this.mapName.setText(mapName);
    }

    public void act(float delta) {
        super.act(delta);
        ICamera cam = GaiaSky.instance.cam;
        double distSun = cam.getPos().len();
        for (IMinimapScale mms : scales) {
            if (mms.isActive(cam.getPos(), distSun)) {
                mms.update();
                mms.renderSideProjection(sfb);
                mms.renderTopProjection(tfb);
                if(current == null || current != mms){
                    current = mms;
                    updateMapName(current.getName());
                }
                break;
            }
        }
    }


}
