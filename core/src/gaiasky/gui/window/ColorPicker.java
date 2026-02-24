/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.HexColorValidator;
import gaiasky.util.validator.IValidator;

import java.text.DecimalFormat;
import java.util.Arrays;

public class ColorPicker extends ColorPickerAbstract {

    public ColorPicker(Stage stage, Skin skin) {
        this(stage, skin, true);
    }

    public ColorPicker(Stage stage, Skin skin, boolean showAlpha) {
        super(I18n.msg("gui.colorpicker.title"), stage, skin);
        this.skin = skin;
        this.stage = stage;
        initialize(showAlpha);
    }

    public ColorPicker(String name, Stage stage, Skin skin) {
        super(name, stage, skin);
        this.skin = skin;
        this.stage = stage;
        initialize(true);
    }

    public ColorPicker(float[] rgba, Stage stage, Skin skin) {
        this(stage, skin);
        setPickedColor(rgba);
    }

    public ColorPicker(Color color, Stage stage, Skin skin) {
        this(stage, skin);
        setPickedColor(color);
    }

    public ColorPicker(Color color, Stage stage, Skin skin, boolean showAlpha) {
        this(stage, skin, showAlpha);
        setPickedColor(color);
    }

    public ColorPicker(String name, float[] rgba, Stage stage, Skin skin) {
        this(name, stage, skin);
        setPickedColor(rgba);
    }

    protected void initialize(boolean showAlpha) {
        this.addListener(event -> {
            if (event instanceof InputEvent ie) {
                Type type = ie.getType();
                // Click
                if ((type == Type.touchDown) && (ie.getButton() == Buttons.LEFT)) {
                    // Launch color picker window
                    ColorPickerDialog cpd = new ColorPickerDialog(name, color, showAlpha, stage, skin);
                    cpd.setAcceptListener(() -> {
                        // Set color and run runnable, if any
                        setPickedColor(cpd.color);
                        if (newColorRunnable != null) {
                            newColorRunnable.run();
                        }
                    });
                    cpd.show(stage);

                } else if (type == Type.enter) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Hand);
                } else if (type == Type.exit) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                }
                return true;
            }
            return false;
        });
        if (name != null && !name.isBlank()) {
            this.addListener(new OwnTextTooltip(name, skin));
        }
    }

    public void setNewColorRunnable(Runnable r) {
        newColorRunnable = r;
    }

    /** A color picker dialog **/
    private class ColorPickerDialog extends GenericDialog {
        private final DecimalFormat nf;
        private final ColorPickerDialog cpd;
        private final float[] color;
        private final float[] colorBak;
        private OwnTextField[] textfields;
        private OwnTextField hexfield;
        private OwnSlider[] sliders;
        private OwnImage newColorImage;
        private boolean changeEvents = true;
        private boolean showAlpha = true;

        public ColorPickerDialog(String elementName, float[] color, boolean showAlpha, Stage stage, Skin skin) {
            super(I18n.msg("gui.colorpicker.title") + (elementName != null ? ": " + elementName : ""), skin, stage);
            this.cpd = this;
            this.color = new float[4];
            this.colorBak = new float[4];
            this.color[0] = color[0];
            this.color[1] = color[1];
            this.color[2] = color[2];
            this.color[3] = color[3];
            this.showAlpha = showAlpha;

            this.nf = new DecimalFormat("0.00");

            setAcceptText(I18n.msg("gui.ok"));
            setCancelText(I18n.msg("gui.cancel"));
            setModal(true);

            buildSuper();
        }

        public ColorPickerDialog(String elementName, float[] color, Stage stage, Skin skin) {
            this(elementName, color, true, stage, skin);
        }

        public ColorPickerDialog(float[] color, Stage stage, Skin skin) {
            this(null, color, stage, skin);
        }

        @Override
        protected void build() {
            float textFieldLen = 140f;
            float sliderLen = 200f;
            float colSize = 160f;
            content.clear();

            HorizontalGroup hg = new HorizontalGroup();
            hg.space(pad18);
            var oldColorImage = new OwnImage(skin.getDrawable("white"), false);
            oldColorImage.setColor(color[0], color[1], color[2], color[3]);
            Table oColor = new Table();
            oColor.add(oldColorImage).size(colSize);
            newColorImage = new OwnImage(skin.getDrawable("white"), false);
            newColorImage.setColor(color[0], color[1], color[2], color[3]);
            Table col = new Table();
            col.add(newColorImage).size(colSize);
            hg.addActor(oColor);
            hg.addActor(new OwnLabel(">", skin));
            hg.addActor(col);

            /* Sliders */
            sliders = new OwnSlider[4];
            OwnSlider sRed, sGreen, sBlue, sAlpha;
            sRed = new OwnSlider(0f, 1f, 0.01f, skin);
            sRed.showValueLabel(false);
            sRed.setWidth(sliderLen);
            sRed.setValue(color[0]);
            sliders[0] = sRed;
            sGreen = new OwnSlider(0f, 1f, 0.01f, skin);
            sGreen.showValueLabel(false);
            sGreen.setWidth(sliderLen);
            sGreen.setValue(color[1]);
            sliders[1] = sGreen;
            sBlue = new OwnSlider(0f, 1f, 0.01f, skin);
            sBlue.showValueLabel(false);
            sBlue.setWidth(sliderLen);
            sBlue.setValue(color[2]);
            sliders[2] = sBlue;
            sAlpha = new OwnSlider(0f, 1f, 0.01f, skin);
            sAlpha.showValueLabel(false);
            sAlpha.setWidth(sliderLen);
            sAlpha.setValue(color[3]);
            sliders[3] = sAlpha;

            /* Inputs */
            textfields = new OwnTextField[4];
            FloatValidator floatValidator = new FloatValidator(0f, 1f);
            OwnTextField tRed, tGreen, tBlue, tAlpha;
            tRed = new OwnTextField(nf.format(color[0]), skin, floatValidator);
            tRed.setWidth(textFieldLen);
            textfields[0] = tRed;
            tGreen = new OwnTextField(nf.format(color[1]), skin, floatValidator);
            tGreen.setWidth(textFieldLen);
            textfields[1] = tGreen;
            tBlue = new OwnTextField(nf.format(color[2]), skin, floatValidator);
            tBlue.setWidth(textFieldLen);
            textfields[2] = tBlue;
            tAlpha = new OwnTextField(nf.format(color[3]), skin, floatValidator);
            tAlpha.setWidth(textFieldLen);
            textfields[3] = tAlpha;

            /* Hex */
            IValidator hVal = new HexColorValidator(true);
            hexfield = new OwnTextField(ColorUtils.rgbaToHex(color), skin, hVal);
            hexfield.setWidth(sliderLen);

            /* Color table */
            Table colTable = new Table();
            float size = 24f;
            float cPad = 1.6f;
            int i = 1;
            int n = 4 * 4 * 4;
            float a = 1f;
            for (float r = 0f; r <= 1f; r += 0.3333f) {
                for (float g = 0f; g <= 1f; g += 0.3333f) {
                    for (float b = 0f; b <= 1f; b += 0.3333f) {
                        var c = new OwnImage(skin.getDrawable("white"), false);
                        c.setColor(r, g, b, a);
                        final float[] pick = new float[]{r, g, b, a};
                        c.addListener(new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                cpd.setColor(pick);
                            }
                        });
                        c.addListener(new TextTooltip(Arrays.toString(pick), skin));
                        if (i % (n / 4) == 0) {
                            colTable.add(c).size(size).pad(cPad).row();
                        } else {
                            colTable.add(c).size(size).pad(cPad);
                        }
                        i++;
                    }
                }
            }

            // Connect sliders
            sRed.addListener(new UpdaterListener(true, this, color, 0));
            sGreen.addListener(new UpdaterListener(true, this, color, 1));
            sBlue.addListener(new UpdaterListener(true, this, color, 2));
            sAlpha.addListener(new UpdaterListener(true, this, color, 3));

            // Connect text fields
            tRed.addListener(new UpdaterListener(false, this, color, 0));
            tGreen.addListener(new UpdaterListener(false, this, color, 1));
            tBlue.addListener(new UpdaterListener(false, this, color, 2));
            tAlpha.addListener(new UpdaterListener(false, this, color, 3));

            // Connect hex
            hexfield.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (changeEvents && hexfield.isValid()) {
                        float[] newcol = ColorUtils.hexToRgba(hexfield.getText());
                        System.arraycopy(newcol, 0, color, 0, newcol.length);
                        cpd.updateColor(true, true, false);
                    }
                }
            });

            content.add(hg).padBottom(pad18 * 2f).colspan(3).row();

            content.add(new OwnLabel(I18n.msg("gui.colorpicker.red"), skin)).padRight(pad18).padBottom(pad18);
            content.add(sRed).left().padRight(pad18).padBottom(pad18);
            content.add(tRed).padBottom(pad18).row();

            content.add(new OwnLabel(I18n.msg("gui.colorpicker.green"), skin)).padRight(pad18).padBottom(pad18);
            content.add(sGreen).left().padRight(pad18).padBottom(pad18);
            content.add(tGreen).padBottom(pad18).row();

            content.add(new OwnLabel(I18n.msg("gui.colorpicker.blue"), skin)).padRight(pad18).padBottom(pad18);
            content.add(sBlue).left().padRight(pad18).padBottom(pad18);
            content.add(tBlue).padBottom(pad18).row();

            if (showAlpha) {
                content.add(new OwnLabel(I18n.msg("gui.colorpicker.alpha"), skin)).padRight(pad18).padBottom(pad18);
                content.add(sAlpha).left().padRight(pad18).padBottom(pad18);
                content.add(tAlpha).padBottom(pad18).row();
            }

            content.add(new OwnLabel(I18n.msg("gui.colorpicker.hex"), skin)).padRight(pad18).padBottom(pad18);
            content.add(hexfield).colspan(2).left().padBottom(pad18).row();

            content.add(colTable).colspan(3).padBottom(pad18).row();

        }

        @Override
        protected boolean accept() {
            // Nothing
            return true;
        }

        @Override
        protected void cancel() {
            System.arraycopy(this.colorBak, 0, this.color, 0, this.color.length);
        }

        @Override
        public void dispose() {

        }

        @Override
        public GenericDialog show(Stage stage) {
            // Backup color
            System.arraycopy(this.color, 0, this.colorBak, 0, this.color.length);
            return super.show(stage);
        }

        public void setColor(float[] color) {
            setColor(color[0], color[1], color[2], color[3]);
        }

        public void setColor(float red, float green, float blue, float alpha) {
            color[0] = red;
            color[1] = green;
            color[2] = blue;
            color[3] = alpha;
            updateColor();
        }

        public void updateColor() {
            updateColor(true, true, true);
        }

        public void updateColor(boolean updateSliders, boolean updateTextFields, boolean updateHex) {
            changeEvents = false;
            // Update sliders and textfields
            for (int i = 0; i < color.length; i++) {
                if (updateSliders)
                    sliders[i].setValue(color[i]);

                if (updateTextFields)
                    textfields[i].setText(nf.format(color[i]));
            }
            // Update hex
            if (updateHex)
                hexfield.setText(ColorUtils.rgbaToHex(color));

            // Update image
            newColorImage.setColor(color[0], color[1], color[2], color[3]);
            changeEvents = true;
        }
    }

    private class UpdaterListener implements EventListener {
        private final ColorPickerDialog cpd;
        private final float[] color;
        private final int idx;
        private final boolean slider;

        public UpdaterListener(boolean slider, ColorPickerDialog cpd, float[] color, int idx) {
            super();
            this.cpd = cpd;
            this.slider = slider;
            this.color = color;
            this.idx = idx;
        }

        @Override
        public boolean handle(Event event) {
            if (cpd.changeEvents && event instanceof ChangeEvent) {
                float c = color[idx];
                boolean update = true;
                if (slider) {
                    c = cpd.sliders[idx].getValue();
                } else {
                    // Update slider
                    if (cpd.textfields[idx].isValid()) {
                        c = Float.parseFloat(cpd.textfields[idx].getText());
                    } else {
                        update = false;
                    }
                }
                if (update && c != color[idx]) {
                    color[idx] = c;
                    cpd.updateColor(!slider, slider, true);
                }

                return true;
            }
            return false;
        }
    }
}
