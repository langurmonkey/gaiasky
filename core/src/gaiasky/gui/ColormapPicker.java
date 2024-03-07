/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.gui.beans.AttributeComboBoxBean;
import gaiasky.gui.beans.ComboBoxBean;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.OctreeObjectView;
import gaiasky.util.CatalogInfo;
import gaiasky.util.ObjectDoubleMap;
import gaiasky.util.Pair;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.filter.attrib.*;
import gaiasky.util.i18n.I18n;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.*;
import gaiasky.util.tree.IOctreeObject;
import gaiasky.util.ucd.UCD;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.HexColorValidator;
import gaiasky.util.validator.IValidator;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ColormapPicker extends ColorPickerAbstract {

    // Stores minimum and maximum mapping values for the session
    private static final Map<String, double[]> minMaxMap = new HashMap<>();
    public static Array<Pair<String, Integer>> cmapList;

    static {
        cmapList = new Array<>(false, 9);
        cmapList.add(new Pair<>("reds", 0));
        cmapList.add(new Pair<>("greens", 1));
        cmapList.add(new Pair<>("blues", 2));
        cmapList.add(new Pair<>("rainbow18", 3));
        cmapList.add(new Pair<>("rainbow", 4));
        cmapList.add(new Pair<>("seismic", 5));
        cmapList.add(new Pair<>("carnation", 6));
        cmapList.add(new Pair<>("hotmetal", 7));
        cmapList.add(new Pair<>("cool", 8));
    }

    private final CatalogInfo catalogInfo;
    private final FocusView view;
    private final Drawable drawableColor;
    private final Drawable drawableColormap;
    private int cmapIndex;
    private IAttribute cmapAttrib;
    private double cmapMin, cmapMax;

    private ColormapPicker(String name,
                           CatalogInfo ci,
                           Stage stage,
                           Skin skin) {
        super(name, stage, skin);
        this.catalogInfo = ci;
        this.drawableColor = skin.getDrawable("white");
        this.drawableColormap = skin.getDrawable("iconic-star");
        this.view = new FocusView();
        initialize();
    }

    public ColormapPicker(float[] rgba,
                          CatalogInfo ci,
                          Stage stage,
                          Skin skin) {
        this(null, rgba, ci, stage, skin);
    }

    public ColormapPicker(String name,
                          float[] rgba,
                          CatalogInfo ci,
                          Stage stage,
                          Skin skin) {
        this(name, ci, stage, skin);
        setPickedColor(rgba);
    }

    static int getCmapIndex(String name) {
        for (Pair<String, Integer> cmapDef : cmapList) {
            if (cmapDef.getFirst().equalsIgnoreCase(name))
                return cmapDef.getSecond();
        }
        return -1;
    }

    static String getCmapName(int index) {
        return cmapList.get(index).getFirst();
    }

    protected void initialize() {

        this.addListener(new ClickListener() {
            public boolean touchDown(InputEvent event,
                                     float x,
                                     float y,
                                     int pointer,
                                     int button) {
                return super.touchDown(event, x, y, pointer, button);
            }

            public void touchUp(InputEvent event,
                                float x,
                                float y,
                                int pointer,
                                int button) {
                if (event.getButton() == Buttons.LEFT) {
                    // Launch color picker window
                    ColorPickerColormapDialog cpd = new ColorPickerColormapDialog(name, color, stage, skin);
                    cpd.setAcceptListener(() -> {
                        if (cpd.plainColor.isChecked()) {
                            setPickedColor(cpd.color);
                            if (newColorRunnable != null) {
                                newColorRunnable.run();
                            }
                        } else if (cpd.colormap.isChecked()) {
                            setPickedColormap(cpd.cmapImage);
                            if (newColormapRunnable != null) {
                                newColormapRunnable.run();
                            }
                        }
                    });
                    cpd.show(stage);

                }

                // Bubble up
                super.touchUp(event, x, y, pointer, button);
            }
        });
        this.addListener(event -> {
            if (event instanceof InputEvent) {
                Type type = ((InputEvent) event).getType();
                // Click
                if (type == Type.enter) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Hand);
                } else if (type == Type.exit) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                }
                return true;
            }
            return false;
        });

    }

    public Drawable getDrawable() {
        if (catalogInfo.plainColor)
            return drawableColor;
        else
            return drawableColormap;
    }

    public void setNewColorRunnable(Runnable r) {
        newColorRunnable = r;
    }

    public void setNewColormapRunnable(Runnable r) {
        newColormapRunnable = r;
    }

    @Override
    public void setPickedColor(float[] rgba) {
        catalogInfo.plainColor = true;

        // Plain color
        initColor();
        System.arraycopy(rgba, 0, this.color, 0, rgba.length);
        super.setColor(rgba[0], rgba[1], rgba[2], rgba[3]);
        super.setDrawable(getDrawable());
    }

    public void setPickedColormap(Image cmapImageSmall) {
        catalogInfo.plainColor = false;
        catalogInfo.hlCmapAttribute = this.cmapAttrib;
        catalogInfo.hlCmapIndex = this.cmapIndex;
        catalogInfo.hlCmapMin = this.cmapMin;
        catalogInfo.hlCmapMax = this.cmapMax;

        super.setColor(1, 1, 1, 1);
        super.setDrawable(cmapImageSmall.getDrawable());
    }

    public int getPickedCmapIndex() {
        return cmapIndex;
    }

    public IAttribute getPickedCmapAttribute() {
        return cmapAttrib;
    }

    public double getPickedCmapMin() {
        return cmapMin;
    }

    public double getPickedCmapMax() {
        return cmapMax;
    }

    /** A color picker and colormap dialog **/
    private class ColorPickerColormapDialog extends GenericDialog {
        private final Map<Integer, Image> cmapImages;
        private final DecimalFormat nf;
        private final ColorPickerColormapDialog cpd;
        private final Array<Entity> pgarray;
        private final Array<IOctreeObject> apearray;
        private CheckBox plainColor, colormap;
        private Image cmapImage;
        private Cell cmapImageCell;
        private float[] color;
        private OwnTextField minMap, maxMap;
        private OwnTextField[] textfields;
        private OwnTextField hexfield;
        private OwnSlider[] sliders;
        private Image newColorImage;
        private boolean changeEvents = true;

        public ColorPickerColormapDialog(String elementName,
                                         float[] color,
                                         Stage stage,
                                         Skin skin) {
            super(TextUtils.capString(I18n.msg("gui.colorpicker.title") + (elementName != null ? ": " + elementName : ""), 30), skin, stage);
            this.cpd = this;
            this.color = new float[4];
            this.color[0] = color[0];
            this.color[1] = color[1];
            this.color[2] = color[2];
            this.color[3] = color[3];

            this.pgarray = new Array<>();
            this.apearray = new Array<>();
            this.nf = new DecimalFormat("0.00");

            cmapImages = new HashMap<>();
            for (Pair<String, Integer> cmapDef : cmapList) {
                var tex = new Texture(Gdx.files.internal("img/cmap/cmap_" + cmapDef.getFirst() + ".png"));
                var image = new Image(tex);
                cmapImages.put(cmapDef.getSecond(), new Image(tex));
            }

            setAcceptText(I18n.msg("gui.ok"));
            setCancelText(I18n.msg("gui.cancel"));
            setModal(true);

            buildSuper();
        }

        public ColorPickerColormapDialog(float[] color,
                                         Stage stage,
                                         Skin skin) {
            this(null, color, stage, skin);
        }

        @Override
        protected void build() {
            content.clear();

            // Table containing the actual widget
            Table container = new Table(skin);
            Container<Table> cont = new Container<>(container);

            // Radio buttons.
            plainColor = new OwnCheckBox(I18n.msg("gui.colorpicker.plaincolor"), skin, "radio", pad10);
            plainColor.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (plainColor.isChecked()) {
                        container.clear();
                        addColorPickerWidget(container);
                        pack();
                    }
                    return true;
                }
                return false;
            });
            plainColor.setChecked(catalogInfo.plainColor);
            content.add(plainColor).left().padBottom(pad10).row();

            colormap = new OwnCheckBox(I18n.msg("gui.colorpicker.colormap"), skin, "radio", pad10);
            colormap.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (colormap.isChecked()) {
                        container.clear();
                        addColormapWidget(container);
                        pack();
                    }
                    return true;
                }
                return false;
            });
            colormap.setChecked(!catalogInfo.plainColor);
            content.add(colormap).left().padBottom(pad10 * 2f).row();

            content.add(new Separator(skin, "gray")).center().growX().padBottom(pad10 * 2f).row();

            new ButtonGroup<>(plainColor, colormap);

            content.add(cont).left();
        }

        private void addColormapWidget(Table container) {
            float sbWidth = 272f;

            // Color map
            container.add(new OwnLabel(I18n.msg("gui.colorpicker.colormap"), skin)).left().padRight(pad18).padBottom(pad10).padTop(pad18 * 2);
            ComboBoxBean[] gqs = new ComboBoxBean[cmapList.size];
            for (Pair<String, Integer> cmapDef : cmapList) {
                gqs[cmapDef.getSecond()] = new ComboBoxBean(I18n.msg("gui.colormap." + cmapDef.getFirst()), cmapDef.getSecond());
            }

            OwnSelectBox<ComboBoxBean> cmap = new OwnSelectBox<>(skin);
            cmap.setItems(gqs);
            cmap.setWidth(sbWidth);
            cmap.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    cmapIndex = cmap.getSelectedIndex();
                    updateCmapImage(cmap.getSelected().value, catalogInfo.hlCmapAlpha);
                    return true;
                }
                return false;
            });
            container.add(cmap).colspan(2).left().padBottom(pad10).padTop(pad18 * 2).row();

            // Alpha
            container.add(new OwnLabel(I18n.msg("gui.colorpicker.alpha"), skin)).left().padRight(pad18).padBottom(pad10);
            OwnSlider alphaSlider = new OwnSlider(0f, 1f, 0.01f, skin);
            alphaSlider.showValueLabel(false);
            alphaSlider.setWidth(sbWidth);
            alphaSlider.setValue(color[3]);
            alphaSlider.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    catalogInfo.hlCmapAlpha = alphaSlider.getValue();
                    updateCmapImage(cmap.getSelected().value, catalogInfo.hlCmapAlpha);
                    return true;
                }
                return false;
            });
            container.add(alphaSlider).colspan(2).left().padBottom(pad10).padTop(pad10).row();

            // Color map image
            cmapImageCell = container.add();
            cmapImageCell.colspan(3).center().padBottom(pad18 * 2).padTop(pad10).row();
            updateCmapImage(cmap.getSelected().value, catalogInfo.hlCmapAlpha);

            // Attribute
            container.add(new OwnLabel(I18n.msg("gui.colorpicker.attribute"), skin)).left().padRight(pad18).padBottom(pad10);
            var catalog = catalogInfo.entity;
            view.setEntity(catalog);
            boolean stars = view.getStarSet() != null || view.isOctree();
            Array<AttributeComboBoxBean> attrs = new Array<>(false, stars ? 12 : 7);
            // Add particle attributes (dist, alpha, delta)
            attrs.add(new AttributeComboBoxBean(new AttributeDistance()));
            attrs.add(new AttributeComboBoxBean(new AttributeRA()));
            attrs.add(new AttributeComboBoxBean(new AttributeDEC()));
            attrs.add(new AttributeComboBoxBean(new AttributeEclLatitude()));
            attrs.add(new AttributeComboBoxBean(new AttributeEclLongitude()));
            attrs.add(new AttributeComboBoxBean(new AttributeGalLatitude()));
            attrs.add(new AttributeComboBoxBean(new AttributeGalLongitude()));
            if (stars) {
                // Star-only attributes (appmag, absmag, mualpha, mudelta, radvel)
                attrs.add(new AttributeComboBoxBean(new AttributeAppmag()));
                attrs.add(new AttributeComboBoxBean(new AttributeAbsmag()));
                attrs.add(new AttributeComboBoxBean(new AttributeMualpha()));
                attrs.add(new AttributeComboBoxBean(new AttributeMudelta()));
                attrs.add(new AttributeComboBoxBean(new AttributeRadvel()));
            }
            // Colors
            attrs.add(new AttributeComboBoxBean(new AttributeColorRed()));
            attrs.add(new AttributeComboBoxBean(new AttributeColorGreen()));
            attrs.add(new AttributeComboBoxBean(new AttributeColorBlue()));
            // Extra attributes
            if (view.isSet()) {
                var set = view.getSet();
                if (!set.data().isEmpty()) {
                    IParticleRecord first = set.data().get(0);
                    if (first.hasExtra()) {
                        ObjectMap.Keys<UCD> ucds = first.extraKeys();
                        for (UCD ucd : ucds)
                            attrs.add(new AttributeComboBoxBean(new AttributeUCD(ucd)));
                    }
                }
            }

            OwnSelectBox<AttributeComboBoxBean> attribs = new OwnSelectBox<>(skin);
            attribs.setItems(attrs);
            attribs.setWidth(sbWidth);
            attribs.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    cmapAttrib = attribs.getSelected().attr;
                    recomputeAttributeMinMax(catalogInfo, cmapAttrib);
                    return true;
                }
                return false;
            });
            container.add(attribs).colspan(2).left().padBottom(pad10).row();

            // Min mapping value
            container.add(new OwnLabel(I18n.msg("gui.colorpicker.min"), skin)).left().padRight(pad18).padBottom(pad10);
            minMap = new OwnTextField(Double.toString(getCmapMin(cmapAttrib, catalogInfo)), skin);
            minMap.setWidth(sbWidth * 0.9f);
            minMap.addListener(event -> {
                if (event instanceof ChangeEvent && minMap.isValid()) {
                    cmapMin = Parser.parseFloat(minMap.getText());
                    updateMinMaxMap(catalogInfo, cmapAttrib, 0, cmapMin);
                    return true;
                }
                return false;
            });
            container.add(minMap).left().padBottom(pad10).padRight(pad10);
            // Reload
            OwnImageButton reloadCmap = new OwnImageButton(skin, "reload");
            reloadCmap.addListener(new OwnTextTooltip(I18n.msg("gui.colorpicker.minmax.reload"), skin, 3));
            reloadCmap.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    recomputeAttributeMinMax(catalogInfo, cmapAttrib, true);
                    return true;
                }
                return false;
            });
            container.add(reloadCmap).left().padBottom(pad10).row();

            // Max mapping value
            container.add(new OwnLabel(I18n.msg("gui.colorpicker.max"), skin)).left().padRight(pad18).padBottom(pad10);
            maxMap = new OwnTextField(Double.toString(getCmapMax(cmapAttrib, catalogInfo)), skin);
            maxMap.setWidth(sbWidth * 0.9f);
            maxMap.addListener(event -> {
                if (event instanceof ChangeEvent && maxMap.isValid()) {
                    cmapMax = Parser.parseFloat(maxMap.getText());
                    updateMinMaxMap(catalogInfo, cmapAttrib, 1, cmapMax);
                    return true;
                }
                return false;
            });
            container.add(maxMap).colspan(2).left().padBottom(pad10);
            container.add().row();

            // Select
            cmap.setSelectedIndex(catalogInfo.hlCmapIndex);
            alphaSlider.setValue(catalogInfo.hlCmapAlpha);
            attribs.setSelectedIndex(findIndex(catalogInfo.hlCmapAttribute, attrs));

            // Trigger first update
            attribs.getSelection().fireChangeEvent();
            cmap.getSelection().fireChangeEvent();

        }

        private void updateCmapImage(Integer colorMapIndex,
                                     float alpha) {
            cmapImage = cmapImages.get(colorMapIndex);
            cmapImage.setColor(1, 1, 1, alpha);
            cmapImageCell.clearActor();
            cmapImageCell.setActor(cmapImage);
            pack();
        }

        private double getCmapMin(IAttribute attrib,
                                  CatalogInfo ci) {
            if (attrib != null && ci != null) {
                String key = key(ci, attrib);
                if (!minMaxMap.containsKey(key))
                    return ci.hlCmapMin;
                else
                    return minMaxMap.get(key)[0];
            } else {
                return 0;
            }
        }

        private double getCmapMax(IAttribute attrib,
                                  CatalogInfo ci) {
            String key = key(ci, attrib);
            if (!minMaxMap.containsKey(key))
                return ci.hlCmapMax;
            else
                return minMaxMap.get(key)[1];
        }

        private void updateMinMaxMap(CatalogInfo ci,
                                     IAttribute attrib,
                                     int index,
                                     double value) {
            String key = key(ci, attrib);
            if (!minMaxMap.containsKey(key)) {
                minMaxMap.put(key, new double[] { value, value });
            } else {
                minMaxMap.get(key)[index] = value;
            }
        }

        private String key(CatalogInfo ci,
                           IAttribute attrib) {
            return (ci != null ? ci.name + "-" : "") + (attrib != null ? attrib.toString() : "dummy");
        }

        /** Compute every time, this is costly **/
        private void recomputeAttributeMinMax(CatalogInfo ci,
                                              IAttribute attrib) {
            recomputeAttributeMinMax(ci, attrib, false);
        }

        private void recomputeAttributeMinMax(CatalogInfo ci,
                                              IAttribute attrib,
                                              boolean force) {
            String key = key(ci, attrib);
            view.setEntity(ci.entity);
            if (!force && minMaxMap.containsKey(key)) {
                double[] minmax = minMaxMap.get(key);
                // Set to fields
                minMap.setText(Double.toString(minmax[0]));
                maxMap.setText(Double.toString(minmax[1]));
                cmapMin = minmax[0];
                cmapMax = minmax[1];
            } else if (view.isSet() || view.isOctree()) {
                pgarray.clear();
                apearray.clear();
                if (view.isOctree()) {
                    var octant = view.getOctant();
                    octant.addParticlesTo(apearray);
                    for (IOctreeObject ape : apearray) {
                        pgarray.add(((OctreeObjectView) ape).getEntity());
                    }
                } else {
                    pgarray.add(ci.entity);
                }
                double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
                for (Entity pg : pgarray) {
                    var set = Mapper.particleSet.has(pg) ? Mapper.particleSet.get(pg) : Mapper.starSet.get(pg);
                    for (IParticleRecord pb : set.data()) {
                        double val = attrib.getNumber(pb);
                        if (!Double.isNaN(val) && !Double.isInfinite(val)) {
                            if (val < min)
                                min = val;
                            if (val > max)
                                max = val;
                        }
                    }
                }
                // Set to fields
                minMap.setText(Double.toString(min));
                maxMap.setText(Double.toString(max));
                cmapMin = min;
                cmapMax = max;
                // Add to map
                minMaxMap.put(key, new double[] { min, max });
            }
        }

        private int findIndex(IAttribute attribute,
                              Array<AttributeComboBoxBean> attribs) {
            int i = 0;
            for (AttributeComboBoxBean attr : attribs) {
                if (attr.attr.equals(attribute))
                    return i;
                i++;
            }
            return 0;
        }

        private void addColorPickerWidget(Table container) {
            float textfieldLen = 80f;
            float sliderLen = 240f;
            float colsize = 160f;

            HorizontalGroup hg = new HorizontalGroup();
            hg.space(pad18);
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
                            public void clicked(InputEvent event,
                                                float x,
                                                float y) {
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
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    if (changeEvents && hexfield.isValid()) {
                        float[] newcol = ColorUtils.hexToRgba(hexfield.getText());
                        System.arraycopy(newcol, 0, color, 0, newcol.length);
                        cpd.updateColor(true, true, false);
                    }
                }
            });

            container.add(hg).padBottom(pad18 * 2f).colspan(3).row();

            container.add(new OwnLabel(I18n.msg("gui.colorpicker.red"), skin)).padRight(pad18).padBottom(pad18);
            container.add(sred).left().padRight(pad18).padBottom(pad18);
            container.add(tred).padBottom(pad18).row();

            container.add(new OwnLabel(I18n.msg("gui.colorpicker.green"), skin)).padRight(pad18).padBottom(pad18);
            container.add(sgreen).left().padRight(pad18).padBottom(pad18);
            container.add(tgreen).padBottom(pad18).row();

            container.add(new OwnLabel(I18n.msg("gui.colorpicker.blue"), skin)).padRight(pad18).padBottom(pad18);
            container.add(sblue).left().padRight(pad18).padBottom(pad18);
            container.add(tblue).padBottom(pad18).row();

            container.add(new OwnLabel(I18n.msg("gui.colorpicker.alpha"), skin)).padRight(pad18).padBottom(pad18);
            container.add(salpha).left().padRight(pad18).padBottom(pad18);
            container.add(talpha).padBottom(pad18).row();

            container.add(new OwnLabel(I18n.msg("gui.colorpicker.hex"), skin)).padRight(pad18).padBottom(pad18);
            container.add(hexfield).colspan(2).left().padBottom(pad18).row();

            container.add(coltable).colspan(3).padBottom(pad18).row();
        }

        @Override
        protected boolean accept() {
            // Nothing
            return true;
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

        public void setColor(float red,
                             float green,
                             float blue,
                             float alpha) {
            color[0] = red;
            color[1] = green;
            color[2] = blue;
            color[3] = alpha;
            updateColor();
        }

        public void updateColor() {
            updateColor(true, true, true);
        }

        public void updateColor(boolean updateSliders,
                                boolean updateTextFields,
                                boolean updateHex) {
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
        private final ColorPickerColormapDialog cpd;
        private final float[] color;
        private final int idx;
        private final boolean slider;

        public UpdaterListener(boolean slider,
                               ColorPickerColormapDialog cpd,
                               float[] color,
                               int idx) {
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
