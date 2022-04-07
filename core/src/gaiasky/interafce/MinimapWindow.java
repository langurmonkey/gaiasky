/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.TextureWidget;

public class MinimapWindow extends GenericDialog {
    private OwnLabel mapName;
    private final MinimapWidget minimap;


    public MinimapWindow(final Stage stage, final Skin skin, final ShaderProgram shapeShader, final ShaderProgram spriteShader) {
        super(I18n.msg("gui.minimap.title"), skin, stage);
        minimap = new MinimapWidget(skin, shapeShader, spriteShader);

        setModal(false);
        setCancelText(I18n.msg("gui.close"));

        // Build
        buildSuper();
        // Pack
        pack();
    }

    @Override
    protected void build() {
        float pb = 16f;
        mapName = new OwnLabel("", skin, "header");
        OwnLabel headerSide = new OwnLabel(I18n.msg("gui.minimap.side"), skin);
        Container<TextureWidget> mapSide = new Container<>();
        mapSide.setActor(minimap.getSideProjection());
        OwnLabel headerTop = new OwnLabel(I18n.msg("gui.minimap.top"), skin);
        Container<TextureWidget> mapTop = new Container<>();
        mapTop.setActor(minimap.getTopProjection());

        content.add(mapName).left().padBottom(pad10).row();

        content.add(headerSide).left().padBottom(pb).row();
        content.add(mapSide).left().padBottom(pb).row();

        content.add(headerTop).left().padBottom(pb).row();
        content.add(mapTop).left();

    }

    @Override
    protected void accept() {
    }

    @Override
    protected void cancel() {
    }

    @Override
    public void dispose() {

    }

    private void updateMapName(String mapName){
        if(this.mapName != null)
            this.mapName.setText(mapName);
    }

    public void act(float delta) {
        super.act(delta);
        if(minimap != null) {
            minimap.update();
            String mapName = minimap.getCurrentName();
            if (!mapName.equals(this.mapName.getName())) {
                updateMapName(mapName);
            }
        }
    }


}
