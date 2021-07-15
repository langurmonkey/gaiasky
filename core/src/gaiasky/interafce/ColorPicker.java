/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnSlider;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.HexColorValidator;
import gaiasky.util.validator.IValidator;

import java.util.Arrays;

/**
 * A little widget showing a color and allowing to change it
 * using a color picker dialog.
 */
public class ColorPicker extends ColorPickerAbstract {

    public ColorPicker(Stage stage, Skin skin) {
        super(I18n.txt("gui.colorpicker.title"), stage, skin);
        this.skin = skin;
        this.stage = stage;
        initialize();
    }

    public ColorPicker(String name, Stage stage, Skin skin) {
        super(name, stage, skin);
        this.skin = skin;
        this.stage = stage;
        initialize();
    }

    public ColorPicker(float[] rgba, Stage stage, Skin skin) {
        this(stage, skin);
        setPickedColor(rgba);
    }

    public ColorPicker(String name, float[] rgba, Stage stage, Skin skin) {
        this(name, stage, skin);
        setPickedColor(rgba);
    }

    protected void initialize() {
        this.addListener(new ClickListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return super.touchDown(event, x, y, pointer, button);
            }

            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (event instanceof InputEvent) {
                    Type type = event.getType();
                    // Click
                    if ((type == Type.touchUp) && (event.getButton() == Buttons.LEFT)) {
                        // Launch color picker window
                        ColorPickerDialog cpd = new ColorPickerDialog(name, color, stage, skin);
                        cpd.setAcceptRunnable(() -> {
                            // Set color and run runnable, if any
                            setPickedColor(cpd.color);
                            if (newColorRunnable != null) {
                                newColorRunnable.run();
                            }
                        });
                        cpd.show(stage);

                    } else if (type == Type.enter) {
                        Gdx.graphics.setCursor(GlobalResources.getLinkCursor());
                    } else if (type == Type.exit) {
                        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                    }
                }
            }
        });
    }

    public void setNewColorRunnable(Runnable r) {
        newColorRunnable = r;
    }

    private void initColor() {
        if (color == null || color.length != 4) {
            color = new float[4];
        }
    }

    public void setPickedColor(float[] rgba) {
        initColor();
        System.arraycopy(rgba, 0, this.color, 0, rgba.length);
        super.setColor(rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    public void setPickedColor(float r, float g, float b, float a) {
        initColor();
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = a;
        super.setColor(r, g, b, a);
    }

    public float[] getPickedColor() {
        return color;
    }

    public double[] getPickedColorDouble() {
        double[] c = new double[color.length];
        for (int i = 0; i < color.length; i++)
            c[i] = color[i];
        return c;
    }

    /** A color picker dialog **/
    private class ColorPickerDialog extends GenericDialog {
        private float[] color;
        private final INumberFormat nf;
        private OwnTextField[] textfields;
        private OwnTextField hexfield;
        private OwnSlider[] sliders;
        private Image newColorImage;
        private boolean changeEvents = true;
        private final ColorPickerDialog cpd;

        public ColorPickerDialog(String elementName, float[] color, Stage stage, Skin skin) {
            super(I18n.txt("gui.colorpicker.title") + (elementName != null ? ": " + elementName : ""), skin, stage);
            this.cpd = this;
            this.color = new float[4];
            this.color[0] = color[0];
            this.color[1] = color[1];
            this.color[2] = color[2];
            this.color[3] = color[3];

            this.nf = NumberFormatFactory.getFormatter("0.00");

            setAcceptText(I18n.txt("gui.ok"));
            setCancelText(I18n.txt("gui.cancel"));
            setModal(true);

            buildSuper();
        }

        public ColorPickerDialog(float[] color, Stage stage, Skin skin) {
            this(null, color, stage, skin);
        }

        @Override
        protected void build() {
            float textfieldLen = 80f;
            float sliderLen = 240f;
            float colsize = 160f;
            content.clear();

            HorizontalGroup hg = new HorizontalGroup();
            hg.space(pad10);
            Image oldColorImage = new Image(skin.getDrawable("white"));
            oldColorImage.setColor(color[0], color[1], color[2], color[3]);
            Table ocol = new Table();
            ocol.add(oldColorImage).size(colsize);
            newColorImage = new Image(skin.getDrawable("white"));
            newColorImage.setColor(color[0], color[1], color[2], color[3]);
            Table col = new Table();
            col.add(newColorImage).size(colsize);
            hg.addActor(ocol);
            hg.addActor(new OwnLabel(">", skin));
            hg.addActor(col);

            /* Sliders */
            sliders = new OwnSlider[4];
            OwnSlider sred, sgreen, sblue, salpha;
            sred = new OwnSlider(0f, 1f, 0.01f, skin);
            sred.showValueLabel(false);
            sred.setWidth(sliderLen);
            sred.setValue(color[0]);
            sliders[0] = sred;
            sgreen = new OwnSlider(0f, 1f, 0.01f, skin);
            sgreen.showValueLabel(false);
            sgreen.setWidth(sliderLen);
            sgreen.setValue(color[1]);
            sliders[1] = sgreen;
            sblue = new OwnSlider(0f, 1f, 0.01f, skin);
            sblue.showValueLabel(false);
            sblue.setWidth(sliderLen);
            sblue.setValue(color[2]);
            sliders[2] = sblue;
            salpha = new OwnSlider(0f, 1f, 0.01f, skin);
            salpha.showValueLabel(false);
            salpha.setWidth(sliderLen);
            salpha.setValue(color[3]);
            sliders[3] = salpha;

            /* Inputs */
            textfields = new OwnTextField[4];
            FloatValidator fval = new FloatValidator(0f, 1f);
            OwnTextField tred, tgreen, tblue, talpha;
            tred = new OwnTextField(nf.format(color[0]), skin, fval);
            tred.setWidth(textfieldLen);
            textfields[0] = tred;
            tgreen = new OwnTextField(nf.format(color[1]), skin, fval);
            tgreen.setWidth(textfieldLen);
            textfields[1] = tgreen;
            tblue = new OwnTextField(nf.format(color[2]), skin, fval);
            tblue.setWidth(textfieldLen);
            textfields[2] = tblue;
            talpha = new OwnTextField(nf.format(color[3]), skin, fval);
            talpha.setWidth(textfieldLen);
            textfields[3] = talpha;

            /* Hex */
            IValidator hval = new HexColorValidator(true);
            hexfield = new OwnTextField(ColorUtils.rgbaToHex(color), skin, hval);
            hexfield.setWidth(sliderLen);

            /* Color table */
            Table coltable = new Table();
            float size = 24f;
            float cpad = 1.6f;
            int i = 1;
            int n = 4 * 4 * 4;
            float a = 1f;
            for (float r = 0f; r <= 1f; r += 0.3333f) {
                for (float g = 0f; g <= 1f; g += 0.3333f) {
                    for (float b = 0f; b <= 1f; b += 0.3333f) {
                        Image c = new Image(skin.getDrawable("white"));
                        c.setColor(r, g, b, a);
                        final float[] pick = new float[] { r, g, b, a };
                        c.addListener(new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                cpd.setColor(pick);
                            }
                        });
                        c.addListener(new TextTooltip(Arrays.toString(pick), skin));
                        if (i % (n / 4) == 0) {
                            coltable.add(c).size(size).pad(cpad).row();
                        } else {
                            coltable.add(c).size(size).pad(cpad);
                        }
                        i++;
                    }
                }
            }

            // Connect sliders
            sred.addListener(new UpdaterListener(true, this, color, 0));
            sgreen.addListener(new UpdaterListener(true, this, color, 1));
            sblue.addListener(new UpdaterListener(true, this, color, 2));
            salpha.addListener(new UpdaterListener(true, this, color, 3));

            // Connect textfields
            tred.addListener(new UpdaterListener(false, this, color, 0));
            tgreen.addListener(new UpdaterListener(false, this, color, 1));
            tblue.addListener(new UpdaterListener(false, this, color, 2));
            talpha.addListener(new UpdaterListener(false, this, color, 3));

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

            content.add(hg).padBottom(pad10 * 2f).colspan(3).row();

            content.add(new OwnLabel(I18n.txt("gui.colorpicker.red"), skin)).padRight(pad10).padBottom(pad10);
            content.add(sred).left().padRight(pad10).padBottom(pad10);
            content.add(tred).padBottom(pad10).row();

            content.add(new OwnLabel(I18n.txt("gui.colorpicker.green"), skin)).padRight(pad10).padBottom(pad10);
            content.add(sgreen).left().padRight(pad10).padBottom(pad10);
            content.add(tgreen).padBottom(pad10).row();

            content.add(new OwnLabel(I18n.txt("gui.colorpicker.blue"), skin)).padRight(pad10).padBottom(pad10);
            content.add(sblue).left().padRight(pad10).padBottom(pad10);
            content.add(tblue).padBottom(pad10).row();

            content.add(new OwnLabel(I18n.txt("gui.colorpicker.alpha"), skin)).padRight(pad10).padBottom(pad10);
            content.add(salpha).left().padRight(pad10).padBottom(pad10);
            content.add(talpha).padBottom(pad10).row();

            content.add(new OwnLabel(I18n.txt("gui.colorpicker.hex"), skin)).padRight(pad10).padBottom(pad10);
            content.add(hexfield).colspan(2).left().padBottom(pad10).row();

            content.add(coltable).colspan(3).padBottom(pad10).row();

        }

        @Override
        protected void accept() {
            // Nothing
        }

        @Override
        protected void cancel() {
            color = null;
        }

        @Override
        public void dispose() {

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
