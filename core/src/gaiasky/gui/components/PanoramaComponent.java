/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.beans.ComboBoxBean;
import gaiasky.render.postprocess.effects.CubmeapProjectionEffect.CubemapProjection;
import gaiasky.util.scene2d.OwnSelectBox;

/**
 * Component that appears in panorama mode and is used to control some of its parameters.
 */
public class PanoramaComponent extends CubemapComponent implements IObserver {

    protected OwnSelectBox<ComboBoxBean<CubemapProjection>> projection;

    public PanoramaComponent(Skin skin,
                             Stage stage) {
        super(skin, stage, "360");
        EventManager.instance.subscribe(this, Event.CUBEMAP_PROJECTION_CMD);
    }



    @Override
    public void initializeComponent(float componentWidth) {

        // Stereo profile
        projection = new OwnSelectBox<>(skin);

        var values = ComboBoxBean.getValues(CubemapProjection.getPanoramaProjections());
        projection.setItems(values);
        projection.setWidth(componentWidth);
        projection.setSelectedIndex(GaiaSky.settings().program.modeCubemap.projection.ordinal());
        projection.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                EventManager.publish(Event.CUBEMAP_PROJECTION_CMD, projection, projection.getSelected().value);
            }
            return false;
        });
    }

    @Override
    public void addToTable(Table t) {
        t.add(projection).left().padBottom(pad9).row();
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
       if (event == Event.CUBEMAP_PROJECTION_CMD) {
           if (source != projection) {
               var projection = (CubemapProjection) data[0];
               this.projection.setSelectedIndex(projection.ordinal());
           }
       }
    }


}
