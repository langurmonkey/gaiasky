/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.gui.beans.DistanceUnitComboBoxBean;
import gaiasky.gui.beans.PrimitiveComboBoxBean;
import gaiasky.gui.beans.ShapeComboBoxBean;
import gaiasky.scenegraph.IFocus;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnCheckBox;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnSelectBox;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.FloatValidator;

public class AddShapeDialog extends GenericDialog {

    public OwnCheckBox track, showLabel;
    public OwnTextField name, size;
    public ColorPicker color;
    public OwnSelectBox<DistanceUnitComboBoxBean> units;
    public OwnSelectBox<ShapeComboBoxBean> shape;
    public OwnSelectBox<PrimitiveComboBoxBean> primitive;

    private final IFocus object;
    private final String objectName;
    private final float fieldWidth;
    private final float titleWidth;
    private final float cpSize;

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
        content.add(info).left().padBottom(pad15).row();

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
        int i = 0;
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
        content.add(new OwnLabel(I18n.msg("gui.shape.size"), skin, titleWidth)).left().padRight(pad10).padBottom(pad10);
        content.add(sizeGroup).left().padBottom(pad10).row();

        // Show label
        showLabel = new OwnCheckBox(I18n.msg("gui.shape.label"), skin, pad5);
        showLabel.setChecked(true);
        content.add(showLabel).left().colspan(2).padRight(pad10).padBottom(pad10).row();

        // Track
        track = new OwnCheckBox(I18n.msg("gui.shape.track"), skin, pad5);
        track.setChecked(true);
        content.add(track).left().colspan(2).padRight(pad10).padBottom(pad5);

        // Separator
        addSeparator(2);

        // Shape
        shape = new OwnSelectBox<>(skin);
        shape.setWidth(fieldWidth);
        shape.setItems(ShapeComboBoxBean.defaultShapes());
        shape.setSelectedIndex(0);
        content.add(new OwnLabel(I18n.msg("gui.shape.shape"), skin, titleWidth)).left().padRight(pad10).padBottom(pad10);
        content.add(shape).left().padBottom(pad10).row();

        // Color
        addColor(content);

        // Primitive
        primitive = new OwnSelectBox<>(skin);
        primitive.setWidth(fieldWidth);
        primitive.setItems(PrimitiveComboBoxBean.defaultShapes());
        primitive.setSelectedIndex(0);
        content.add(new OwnLabel(I18n.msg("gui.shape.primitive"), skin, titleWidth)).left().padRight(pad10).padBottom(pad10);
        content.add(primitive).left().padBottom(pad10).row();

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
        container.add(new OwnLabel(I18n.msg("gui.shape.name"), skin, titleWidth)).left().padRight(pad10).padBottom(pad10);
        container.add(name).left().padBottom(pad10).row();
    }

    private void addColor(Table container) {
        color = new ColorPicker(new float[] { 0.3f, 0.4f, 1f, 1f }, stage, skin);
        container.add(new OwnLabel(I18n.msg("gui.shape.color"), skin, titleWidth)).left().padRight(pad10).padBottom(pad5);
        Table lc = new Table(skin);
        lc.add(color).size(cpSize);
        container.add(lc).left().padBottom(pad5).row();
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
    protected void accept() {

    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }
}
