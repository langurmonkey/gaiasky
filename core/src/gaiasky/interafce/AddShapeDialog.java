/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaiasky.scenegraph.IFocus;
import gaiasky.util.Constants;
import gaiasky.util.I18n;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.FloatValidator;

public class AddShapeDialog extends GenericDialog {

    public OwnCheckBox track, showLabel;
    public OwnTextField name, size;
    public ColorPicker color;
    public OwnSelectBox<Units> units;
    public OwnSelectBox<Shape> shape;
    public OwnSelectBox<Primitive> primitive;

    private final IFocus object;
    private final float fieldWidth;
    private final float titleWidth;
    private final float cpSize;

    public AddShapeDialog(final String title, final IFocus object, final Skin skin, final Stage ui) {
        super(title, skin, ui);

        this.object = object;
        fieldWidth = 288f;
        titleWidth = 150f;
        cpSize = 32f;

        setAcceptText(I18n.txt("gui.ok"));
        setCancelText(I18n.txt("gui.cancel"));

        buildSuper();
    }

    @Override
    protected void build() {
        content.clear();

        OwnLabel info = new OwnLabel(I18n.txt("gui.shape.info", object.getName()), skin, "hud-subheader");
        content.add(info).left().padBottom(pad15).row();

        // Name
        addName(content, "1 AU around " + object.getName());

        // Size
        FloatValidator val = new FloatValidator(0f, Float.MAX_VALUE);
        size = new OwnTextField("1.0", skin, val);
        size.setWidth(fieldWidth * 0.6f);
        units = new OwnSelectBox<>(skin);
        units.setWidth(fieldWidth * 0.3f);
        units.setItems(Units.values());
        units.setSelected(Units.AU);
        HorizontalGroup sizeGroup = new HorizontalGroup();
        sizeGroup.space(15f);
        sizeGroup.addActor(size);
        sizeGroup.addActor(units);
        content.add(new OwnLabel(I18n.txt("gui.shape.size"), skin, titleWidth)).left().padRight(pad10).padBottom(pad10);
        content.add(sizeGroup).left().padBottom(pad10).row();

        // Show label
        showLabel = new OwnCheckBox(I18n.txt("gui.shape.label"), skin, pad5);
        showLabel.setChecked(true);
        content.add(showLabel).left().colspan(2).padRight(pad10).padBottom(pad10).row();

        // Track
        track = new OwnCheckBox(I18n.txt("gui.shape.track"), skin, pad5);
        track.setChecked(true);
        content.add(track).left().colspan(2).padRight(pad10).padBottom(pad5);

        // Separator
        addSeparator(2);

        // Shape
        shape = new OwnSelectBox<>(skin);
        shape.setWidth(fieldWidth);
        shape.setItems(Shape.values());
        shape.setSelected(Shape.SPHERE);
        content.add(new OwnLabel(I18n.txt("gui.shape.shape"), skin, titleWidth)).left().padRight(pad10).padBottom(pad10);
        content.add(shape).left().padBottom(pad10).row();

        // Color
        addColor(content);

        // Primitive
        primitive = new OwnSelectBox<>(skin);
        primitive.setWidth(fieldWidth);
        primitive.setItems(Primitive.values());
        primitive.setSelected(Primitive.LINES);
        content.add(new OwnLabel(I18n.txt("gui.shape.primitive"), skin, titleWidth)).left().padRight(pad10).padBottom(pad10);
        content.add(primitive).left().padBottom(pad10).row();

    }

    private void addName(Table container, String text) {
        name = new OwnTextField(text, skin);
        name.setWidth(fieldWidth);
        container.add(new OwnLabel(I18n.txt("gui.shape.name"), skin, titleWidth)).left().padRight(pad10).padBottom(pad10);
        container.add(name).left().padBottom(pad10).row();
    }

    private void addColor(Table container) {
        color = new ColorPicker(new float[] { 0.3f, 0.4f, 1f, 1f }, stage, skin);
        container.add(new OwnLabel(I18n.txt("gui.shape.color"), skin, titleWidth)).left().padRight(pad10).padBottom(pad5);
        Table lc = new Table(skin);
        lc.add(color).size(cpSize);
        container.add(lc).left().padBottom(pad5).row();
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

    public enum Shape {
        SPHERE,
        ICOSPHERE,
        OCTAHEDRONSPHERE,
        CYLINDER,
        RING,
        CONE
    }

    public enum Units {
        M(Constants.M_TO_U),
        KM(Constants.KM_TO_U),
        AU(Constants.AU_TO_U),
        LY(Constants.LY_TO_U),
        PC(Constants.PC_TO_U);
        private double toInternal;

        Units(double toInternal) {
            this.toInternal = toInternal;
        }
        public double toInternal(double value){
            return value * toInternal;
        }
    }

    public enum Primitive {
        LINES,
        TRIANGLES
    }
}
