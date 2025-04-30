/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.datasets;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.gui.beans.AttributeComboBoxBean;
import gaiasky.gui.beans.StringIndexComobBoxBean;
import gaiasky.gui.beans.TransformComboBoxBean;
import gaiasky.gui.window.GenericDialog;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.AffineTransformations;
import gaiasky.scene.record.*;
import gaiasky.scene.system.initialize.MeshInitializer;
import gaiasky.util.CatalogInfo;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.TextUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.DoubleValidator;

import java.util.Arrays;
import java.util.Vector;

/**
 * A window to define a sequence of transformation matrices to apply to a dataset.
 */
public class DatasetTransformsWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(DatasetTransformsWindow.class);

    private final CatalogInfo ci;
    private Table transformsTable;
    private AffineTransformations affine;
    private boolean transformsEdited;

    public DatasetTransformsWindow(CatalogInfo ci,
                                   Skin skin,
                                   Stage stage) {
        super(I18n.msg("gui.dataset.transform") + " - " + ci.name, skin, stage);
        this.ci = ci;
        this.transformsEdited = false;

        setAcceptText(I18n.msg("gui.ok"));
        setCancelText(I18n.msg("gui.cancel"));
        setModal(false);

        content.add().width(700f).row();
        content.getMinHeight();

        // Build
        buildSuper();
    }

    private boolean hasAffine(Entity entity) {
        return Mapper.affine.has(entity);
    }

    private AffineTransformations getAffine(Entity entity) {
        if (Mapper.affine.has(entity)) {
            return Mapper.affine.get(entity);
        }
        return null;
    }

    private void setAffine(Entity entity, Vector<ITransform> transformations) {
        if (Mapper.affine.has(entity)) {
            Mapper.affine.get(entity).setTransformations(transformations);

            // Special operation.
            if (Mapper.mesh.has(entity)) {
                MeshInitializer.initializeCoordinateSystem(entity, Mapper.mesh.get(entity));
            }

            // Down the chain, only for non-celestials.
            if (Mapper.graph.has(entity) && !Mapper.celestial.has(entity)) {
                var graph = Mapper.graph.get(entity);
                if (graph.children != null && !graph.children.isEmpty()) {
                    for (var ch : graph.children) {
                        setAffine(ch, transformations);
                    }
                }
            }
            // Octree.
        }
    }

    @Override
    protected void build() {
        if (hasAffine(ci.entity)) {
            // Title
            content.add(new OwnLabel(I18n.msg("gui.dataset.transform"), skin, "hud-header")).left().padBottom(pad18).padTop(pad20).row();

            // Info
            content.add(new OwnLabel(TextUtils.breakCharacters(I18n.msg("gui.dataset.transform.info"), 55), skin, "default-scblue")).left().padBottom(pad18 * 2f).row();

            // Add transform button
            OwnTextIconButton addTransform = new OwnTextIconButton(I18n.msg("gui.dataset.transform.add"), skin, "add");
            addTransform.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.transform.add"), skin));
            addTransform.pad(pad18);
            content.add(addTransform).top().left().padBottom(pad34).row();
            addTransform.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    ((DatasetTransformsWindow) me).addTransform();
                    transformsEdited = true;
                    return true;
                }
                return false;
            });

            // Transforms table and scroll pane
            transformsTable = new Table(skin);
            transformsTable.top().left();
            var scrollPane = new OwnScrollPane(transformsTable, skin, "minimalist-nobg");
            scrollPane.setScrollbarsVisible(true);
            scrollPane.setFadeScrollBars(false);
            scrollPane.setScrollingDisabled(true, false);
            scrollPane.setWidth(780f);
            scrollPane.setHeight(700f);

            content.add(scrollPane).top().left().row();

            affine = getAffine(ci.entity);
            if (affine != null) {
                affine = affine.deepCopy();
            }
            generateTransformationsTable(affine);
        }

    }

    private final TransformComboBoxBean[] transformTypes = {
            getBean("translation", TranslateTransform.class, 0),
            getBean("rotation", RotateTransform.class, 1),
            getBean("scaling", ScaleTransform.class, 2),
            getBean("refsys", MatrixTransform.class, 3),
    };

    private final StringIndexComobBoxBean[] refSysTypes = {
            getStrBean("eqtogal", 0),
            getStrBean("galtoeq", 1),
            getStrBean("eqtoecl", 2),
            getStrBean("ecltoeq", 3),
            getStrBean("galtoecl", 4),
            getStrBean("ecltogal", 5),
    };

    private TransformComboBoxBean getBean(String type, Class<? extends ITransform> clss, int index) {
        return new TransformComboBoxBean(I18n.msg("gui.dataset.transform.op." + type), type, clss, index);
    }

    private StringIndexComobBoxBean getStrBean(String sht, int index) {
        return new StringIndexComobBoxBean(I18n.msg("gui.dataset.transform.refsys." + sht), sht, index);
    }

    private int getIndex(ITransform transform) {
        return switch (transform.getClass().getSimpleName()) {
            case "TranslateTransform" -> 0;
            case "RotateTransform" -> 1;
            case "ScaleTransform" -> 2;
            default -> 3;
        };
    }

    private void generateTransformationsTable(AffineTransformations affine) {
        float selectWidth = 350f;
        transformsTable.clearChildren();

        if (affine != null && !affine.isEmpty()) {
            int index = 0;
            for (var transform : affine.transformations) {
                Table table = new Table(skin);
                table.pad(pad34);
                table.setBackground("default-round");


                // Buttons
                Table buttons = new Table(skin);

                // Up
                OwnTextIconButton up = new OwnTextIconButton("", skin, "caret-up");
                up.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.transform.up"), skin));
                up.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        moveUp(transform);
                        transformsEdited = true;
                        return true;
                    }
                    return false;
                });
                // Down
                OwnTextIconButton down = new OwnTextIconButton("", skin, "caret-down");
                down.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.transform.down"), skin));
                down.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        moveDown(transform);
                        transformsEdited = true;
                        return true;
                    }
                    return false;
                });
                // Rubbish
                OwnTextIconButton rubbish = new OwnTextIconButton("", skin, "rubbish");
                rubbish.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.transform.remove"), skin));
                rubbish.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        deleteTransform(transform);
                        transformsEdited = true;
                        return true;
                    }
                    return false;
                });
                buttons.add(up).padRight(pad10);
                buttons.add(down).padRight(pad10);
                buttons.add(rubbish);

                // Type
                final int idx = index;
                OwnSelectBox<TransformComboBoxBean> type = new OwnSelectBox<>(skin, "big");
                type.setWidth(selectWidth);
                type.setItems(transformTypes);
                type.setSelectedIndex(getIndex(transform));
                type.addListener((event) -> {
                    if (event instanceof ChangeEvent ce) {
                        var bean = type.getSelected();
                        if (!transform.getClass().equals(bean.clazz())) {
                            GaiaSky.postRunnable(() -> {
                                try {
                                    affine.replace(bean.clazz().getDeclaredConstructor().newInstance(), idx);
                                    generateTransformationsTable(affine);
                                } catch (Exception e) {
                                    logger.error(e);
                                }
                                transformsEdited = true;
                            });
                        }

                    }
                    return false;
                });

                table.add(new OwnLabel((index + 1) + ":", skin, "hud-header")).top().left().padRight(pad20);
                table.add(type).top().left().expandX().padRight(pad18);
                table.add(buttons).top().right().padBottom(pad10).row();
                table.add(new Separator(skin, "small")).colspan(3).width(700f).top().center().row();

                // Content
                Table transformTable = new Table(skin);
                Cell<?> content = table.add(transformTable).colspan(3).width(700f).padTop(pad20);

                DoubleValidator tValidator = new DoubleValidator(-10_000_000_000.0, 10_000_000_000.0);
                DoubleValidator aValidator = new DoubleValidator(-100.0, 100.0);
                DoubleValidator angleValidator = new DoubleValidator(-360.0, 360.0);
                DoubleValidator sValidator = new DoubleValidator(-1_000_000.0, 1_000_000.0);

                if (transform instanceof TranslateTransform tr) {
                    // Labels
                    transformTable.add(new OwnLabel(I18n.msg("gui.dataset.transform.translate.x"), skin)).center().padRight(pad10).padBottom(pad10);
                    transformTable.add(new OwnLabel(I18n.msg("gui.dataset.transform.translate.y"), skin)).center().padRight(pad10).padBottom(pad10);
                    transformTable.add(new OwnLabel(I18n.msg("gui.dataset.transform.translate.z"), skin)).center().padBottom(pad10).row();
                    // Inputs
                    double[] translationIn = tr.getVector() != null ? tr.getVector() : new double[]{0, 0, 0};
                    OwnTextField x = new OwnTextField(Double.toString(translationIn[0] * Constants.U_TO_PC), skin, tValidator);
                    x.setWidth(220f);
                    OwnTextField y = new OwnTextField(Double.toString(translationIn[1] * Constants.U_TO_PC), skin, tValidator);
                    y.setWidth(220f);
                    OwnTextField z = new OwnTextField(Double.toString(translationIn[2] * Constants.U_TO_PC), skin, tValidator);
                    z.setWidth(220f);
                    transformTable.add(x).center().padRight(pad10);
                    transformTable.add(y).center().padRight(pad10);
                    transformTable.add(z).center();

                    // Listener.
                    EventListener el = (event) -> {
                        if (event instanceof ChangeEvent ce) {
                            double[] translation = new double[3];
                            if (x.isValid()) {
                                translation[0] = x.getDoubleValue(0f) * Constants.PC_TO_U;
                                transformsEdited = true;
                            }
                            if (y.isValid()) {
                                translation[1] = y.getDoubleValue(0f) * Constants.PC_TO_U;
                                transformsEdited = true;
                            }
                            if (z.isValid()) {
                                translation[2] = z.getDoubleValue(0f) * Constants.PC_TO_U;
                                transformsEdited = true;
                            }
                            if (transformsEdited) {
                                tr.setVector(translation);
                            }
                            return true;
                        }
                        return false;
                    };
                    x.addListener(el);
                    y.addListener(el);
                    z.addListener(el);

                } else if (transform instanceof RotateTransform tr) {
                    // Labels
                    transformTable.add(new OwnLabel(I18n.msg("gui.dataset.transform.rotate.axis.x"), skin)).center().padRight(pad10).padBottom(pad10);
                    transformTable.add(new OwnLabel(I18n.msg("gui.dataset.transform.rotate.axis.y"), skin)).center().padRight(pad10).padBottom(pad10);
                    transformTable.add(new OwnLabel(I18n.msg("gui.dataset.transform.rotate.axis.z"), skin)).center().padRight(pad20).padBottom(pad10);
                    transformTable.add(new OwnLabel(I18n.msg("gui.dataset.transform.rotate.angle"), skin)).center().padBottom(pad10).row();
                    // Inputs
                    double[] axisIn = tr.getAxis() != null ? tr.getAxis() : new double[]{1, 0, 0};
                    OwnTextField x = new OwnTextField(Double.toString(axisIn[0]), skin, aValidator);
                    OwnTextField y = new OwnTextField(Double.toString(axisIn[1]), skin, aValidator);
                    OwnTextField z = new OwnTextField(Double.toString(axisIn[2]), skin, aValidator);
                    OwnTextField angle = new OwnTextField(Double.toString(tr.getAngle()), skin, angleValidator);

                    transformTable.add(x).center().padRight(pad10);
                    transformTable.add(y).center().padRight(pad10);
                    transformTable.add(z).center().padRight(pad20);
                    transformTable.add(angle).center();

                    // Listener.
                    EventListener el = (event) -> {
                        if (event instanceof ChangeEvent ce) {
                            double[] axis = new double[3];
                            double angleDeg = 0;
                            if (x.isValid()) {
                                axis[0] = x.getDoubleValue(0f);
                                transformsEdited = true;
                            }
                            if (y.isValid()) {
                                axis[1] = y.getDoubleValue(0f);
                                transformsEdited = true;
                            }
                            if (z.isValid()) {
                                axis[2] = z.getDoubleValue(0f);
                                transformsEdited = true;
                            }
                            if (angle.isValid()) {
                                angleDeg = angle.getDoubleValue(0f);
                                transformsEdited = true;
                            }
                            if (transformsEdited) {
                                tr.setAxis(axis);
                                tr.setAngle(angleDeg);
                            }
                            return true;
                        }
                        return false;
                    };
                    x.addListener(el);
                    y.addListener(el);
                    z.addListener(el);
                    angle.addListener(el);

                } else if (transform instanceof ScaleTransform tr) {
                    // Labels
                    transformTable.add(new OwnLabel(I18n.msg("gui.dataset.transform.scale.x"), skin)).center().padRight(pad10).padBottom(pad10);
                    transformTable.add(new OwnLabel(I18n.msg("gui.dataset.transform.scale.y"), skin)).center().padRight(pad10).padBottom(pad10);
                    transformTable.add(new OwnLabel(I18n.msg("gui.dataset.transform.scale.z"), skin)).center().padBottom(pad10).row();
                    // Inputs
                    double[] scaleIn = tr.getScale() != null ? tr.getScale() : new double[]{1, 1, 1};
                    OwnTextField x = new OwnTextField(Double.toString(scaleIn[0]), skin, sValidator);
                    OwnTextField y = new OwnTextField(Double.toString(scaleIn[1]), skin, sValidator);
                    OwnTextField z = new OwnTextField(Double.toString(scaleIn[2]), skin, sValidator);
                    transformTable.add(x).center().padRight(pad10);
                    transformTable.add(y).center().padRight(pad10);
                    transformTable.add(z).center();

                    // Listener.
                    EventListener el = (event) -> {
                        if (event instanceof ChangeEvent ce) {
                            double[] scale = new double[3];
                            if (x.isValid()) {
                                scale[0] = x.getDoubleValue(0f);
                                transformsEdited = true;
                            }
                            if (y.isValid()) {
                                scale[1] = y.getDoubleValue(0f);
                                transformsEdited = true;
                            }
                            if (z.isValid()) {
                                scale[2] = z.getDoubleValue(0f);
                                transformsEdited = true;
                            }
                            if (transformsEdited) {
                                tr.setScale(scale);
                            }
                            return true;
                        }
                        return false;
                    };
                    x.addListener(el);
                    y.addListener(el);
                    z.addListener(el);

                } else if (transform instanceof MatrixTransform tr) {
                    OwnSelectBox<StringIndexComobBoxBean> refSysTransforms = new OwnSelectBox<>(skin);
                    refSysTransforms.setWidth(selectWidth);
                    refSysTransforms.setItems(refSysTypes);
                    if (!tr.isEmpty()) {
                        var matrix = tr.getMatDouble();
                        int refSysIndex = findRefSysTransformIndex(matrix);
                        if (refSysIndex >= 0) {
                            refSysTransforms.setSelectedIndex(refSysIndex);
                        }
                    } else {
                        refSysTransforms.setSelectedIndex(0);
                        tr.setMatrix(Coordinates.getTransformD(refSysTypes[0].value));
                    }
                    transformTable.add(refSysTransforms).center().padRight(pad10);

                    // Listener
                    refSysTransforms.addListener((event) -> {
                        if (event instanceof ChangeEvent ce) {
                            var bean = refSysTransforms.getSelected();
                            var newMatrix = Coordinates.getTransformD(bean.value);
                            tr.setMatrix(newMatrix);
                            transformsEdited = true;
                            return true;
                        }
                        return false;
                    });

                }


                transformsTable.add(table).row();
                index++;
            }
        } else {
            // Indication
            transformsTable.add(new OwnLabel(I18n.msg("gui.dataset.transform.no"), skin)).left().padBottom(pad18).row();
        }
        pack();

    }

    private int findRefSysTransformIndex(Matrix4d matrix) {
        var map = Coordinates.getMap();
        for (var key : map.keySet()) {
            if (key.length() < 10) {
                // We use only short keys.
                var mat = map.get(key);
                if (Arrays.equals(mat.val, matrix.val)) {
                    // We have the matrix and the key.
                    for (var refSys : refSysTypes) {
                        if (refSys.value.equalsIgnoreCase(key)) {
                            // Hit!
                            return refSys.index;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private void addTransform() {
        affine.setTranslate(new double[]{0, 0, 0});
        // Reload table
        GaiaSky.postRunnable(() -> generateTransformationsTable(affine));
    }

    private void deleteTransform(ITransform transform) {
        if (affine != null && transform != null) {
            boolean removed = affine.transformations.remove(transform);
            if (removed) {
                // Reload table
                GaiaSky.postRunnable(() -> generateTransformationsTable(affine));
            }
        }
    }

    private void moveUp(ITransform transform) {
        if (affine != null && transform != null) {
            var currentIndex = affine.transformations.indexOf(transform);
            if (currentIndex > 0) {
                boolean removed = affine.transformations.remove(transform);
                if (removed) {
                    affine.transformations.insertElementAt(transform, currentIndex - 1);
                    // Reload table
                    GaiaSky.postRunnable(() -> generateTransformationsTable(affine));
                }
            }
        }
    }

    private void moveDown(ITransform transform) {
        if (affine != null && transform != null) {
            var currentIndex = affine.transformations.indexOf(transform);
            if (currentIndex < affine.transformations.size() - 1) {
                boolean removed = affine.transformations.remove(transform);
                if (removed) {
                    affine.transformations.insertElementAt(transform, currentIndex + 1);
                    // Reload table
                    GaiaSky.postRunnable(() -> generateTransformationsTable(affine));
                }
            }
        }
    }

    private AttributeComboBoxBean getAttributeBean(IAttribute attr,
                                                   Array<AttributeComboBoxBean> attrs) {
        for (AttributeComboBoxBean attribute : attrs) {
            if (attr.toString().contains(attribute.name)) {
                return attribute;
            }
        }
        return null;
    }

    @Override
    protected boolean accept() {
        // Filter
        if (transformsEdited) {
            if (hasAffine(ci.entity)) {
                // Update.
                setAffine(ci.entity, affine.transformations);
            } else {
                // ERROR!
                throw new RuntimeException("No affine component found.");
            }
        }
        return true;
    }

    @Override
    protected void cancel() {
    }

    @Override
    public void dispose() {

    }
}
