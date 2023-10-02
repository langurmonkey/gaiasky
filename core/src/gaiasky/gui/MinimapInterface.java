/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.Separator;

public class MinimapInterface extends TableGuiInterface {
    private final MinimapWidget minimap;
    private final OwnLabel mapName;

    public MinimapInterface(final Skin skin, final ShaderProgram shapeShader, final ShaderProgram spriteShader) {
        super(skin);
        minimap = new MinimapWidget(skin, shapeShader, spriteShader);

        Table mainTable = new Table(skin);
        mainTable.pad(10f);
        mainTable.setBackground("bg-pane");

        Table side = new Table(skin);
        side.add(minimap.getSideProjection());
        Table top = new Table(skin);
        top.add(minimap.getTopProjection());

        mapName = new OwnLabel("", skin, "header");
        OwnLabel sideLabel = new OwnLabel(I18n.msg("gui.minimap.vert.side"), skin, "default-red");
        Table sideLabelTable = new Table(skin);
        sideLabelTable.add(sideLabel);
        OwnLabel topLabel = new OwnLabel(I18n.msg("gui.minimap.vert.top"), skin, "default-red");
        Table topLabelTable = new Table(skin);
        topLabelTable.add(topLabel);

        mainTable.add(mapName).right().colspan(3).row();
        mainTable.add(sideLabelTable).width(25f).center().padBottom(20f);
        mainTable.add(new Separator(skin, "small")).growY().padBottom(20f);
        mainTable.add(side).padBottom(20f).row();
        mainTable.add(topLabelTable).width(25f).center();
        mainTable.add(new Separator(skin, "small")).growY();
        mainTable.add(top);
        add(mainTable);

        pack();

    }

    private void updateMapName(String name) {
        if (this.mapName != null)
            mapName.setText(name);
    }

    public void update() {
        if (minimap != null) {
            minimap.update();
            String mapName = minimap.getCurrentName();
            if (mapName != null && !mapName.equals(this.mapName.getName())) {
                updateMapName(mapName);
            }
        }
    }

    @Override
    public void dispose() {
        minimap.dispose();
    }
}
