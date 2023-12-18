/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.gui.beans.DistanceUnitComboBoxBean;
import gaiasky.gui.beans.OrientationComboBoxBean;
import gaiasky.gui.beans.PrimitiveComboBoxBean;
import gaiasky.gui.beans.ShapeComboBoxBean;
import gaiasky.scene.api.IFocus;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.FloatValidator;

public class AddShapeDialog extends GenericDialog {

    private final IFocus object;
    private final String objectName;
    private final float fieldWidth;
    private final float titleWidth;
    private final float cpSize;
    public OwnCheckBox track, showLabel;
    public OwnTextField name, size;
    public ColorPicker color;
    public OwnSelectBox<DistanceUnitComboBoxBean> units;
    public OwnSelectBox<ShapeComboBoxBean> shape;
    public OwnSelectBox<PrimitiveComboBoxBean> primitive;
    public OwnSelectBox<OrientationComboBoxBean> orientation;
    // We can recompute the name only when the filed
    // has not been manually edited
    private boolean canRecomputeName = true;

    public AddShapeDialog(final String title, final IFocus object, final String objectName, final Skin skin, final Stage ui) {
        super(title, skin, ui);
        this.objectName = objectName;
        this.object = object;
        fieldWidth = 288f;
        titleWidth = 150f;
        cpSize = 32f;

        setAcceptText(I18n.msg("gui.ok"));
        setCancelText(I18n.msg("gui.cancel"));

        buildSuper();
    }

    @Override
    protected void build() {
        content.clear();

        OwnLabel info = new OwnLabel(I18n.msg("gui.shape.info", object.getName()), skin, "hud-subheader");
        content.add(info).left().padBottom(pad20).row();

        // Name
        addName(content, "1 " + I18n.msg("gui.unit.au") + " -> " + objectName);

        // Size
        FloatValidator val = new FloatValidator(0f, Float.MAX_VALUE);
        size = new OwnTextField("1.0", skin, val);
        size.setWidth(fieldWidth * 0.6f);
        size.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                recomputeObjectName();
            }
            return true;
        });
        units = new OwnSelectBox<>(skin);
        units.setWidth(fieldWidth * 0.3f);
        units.setItems(DistanceUnitComboBoxBean.defaultBeans());
        units.setSelectedIndex(2);
        units.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                recomputeObjectName();
            }
            return true;
        });
        HorizontalGroup sizeGroup = new HorizontalGroup();
        sizeGroup.space(15f);
        sizeGroup.addActor(size);
        sizeGroup.addActor(units);
        content.add(new OwnLabel(I18n.msg("gui.shape.size"), skin, titleWidth)).left().padRight(pad18).padBottom(pad18);
        content.add(sizeGroup).left().padBottom(pad18).row();

        // Show label
        showLabel = new OwnCheckBox(I18n.msg("gui.shape.label"), skin, pad10);
        showLabel.setChecked(true);
        content.add(showLabel).left().colspan(2).padRight(pad18).padBottom(pad18).row();

        // Track
        track = new OwnCheckBox(I18n.msg("gui.shape.track"), skin, pad10);
        track.setChecked(true);
        content.add(track).left().colspan(2).padRight(pad18).padBottom(pad34).row();

        // Separator
        addSeparator(2);

        // Shape
        shape = new OwnSelectBox<>(skin);
        shape.setWidth(fieldWidth);
        shape.setItems(ShapeComboBoxBean.defaultShapes());
        shape.setSelectedIndex(0);
        content.add(new OwnLabel(I18n.msg("gui.shape.shape"), skin, titleWidth)).left().padRight(pad18).padBottom(pad18);
        content.add(shape).left().padBottom(pad18).row();

        // Color
        addColor(content);

        // Primitive
        primitive = new OwnSelectBox<>(skin);
        primitive.setWidth(fieldWidth);
        primitive.setItems(PrimitiveComboBoxBean.defaultShapes());
        primitive.setSelectedIndex(0);
        content.add(new OwnLabel(I18n.msg("gui.shape.primitive"), skin, titleWidth)).left().padRight(pad18).padBottom(pad18);
        content.add(primitive).left().padBottom(pad18).row();

        // Orientation
        orientation = new OwnSelectBox<>(skin);
        orientation.setWidth(fieldWidth);
        orientation.setItems(OrientationComboBoxBean.defaultOrientations());
        orientation.setSelectedIndex(0);
        content.add(new OwnLabel(I18n.msg("gui.shape.orientation"), skin, titleWidth)).left().padRight(pad18).padBottom(pad18);
        content.add(orientation).left().padBottom(pad18).row();


    }

    private void addName(Table container, String text) {
        name = new OwnTextField(text, skin);
        name.setWidth(fieldWidth);
        name.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (!name.getText().equals(text)) {
                    canRecomputeName = false;
                }
            }
            return true;
        });
        container.add(new OwnLabel(I18n.msg("gui.shape.name"), skin, titleWidth)).left().padRight(pad18).padBottom(pad18);
        container.add(name).left().padBottom(pad18).row();
    }

    private void addColor(Table container) {
        color = new ColorPicker(new float[] { 0.3f, 0.4f, 1f, 0.5f }, stage, skin);
        container.add(new OwnLabel(I18n.msg("gui.shape.color"), skin, titleWidth)).left().padRight(pad18).padBottom(pad10);
        Table lc = new Table(skin);
        lc.add(color).size(cpSize);
        container.add(lc).left().padBottom(pad10).row();
    }

    private void recomputeObjectName() {
        if (canRecomputeName) {
            String newName = size.getText()
                    + " "
                    + units.getSelected().name
                    + " -> "
                    + objectName;
            name.setText(newName);
            canRecomputeName = true;
        }
    }

    @Override
    protected boolean accept() {
        return true;
    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }
}
