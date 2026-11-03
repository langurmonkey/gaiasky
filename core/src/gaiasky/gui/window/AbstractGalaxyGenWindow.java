/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectSet;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.record.GalaxyGenerator;
import gaiasky.gui.beans.ComboBoxBean;
import gaiasky.render.BlendMode;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.GalaxyAttributes.ChannelType;
import gaiasky.scene.record.GalaxyAttributes.Distribution;
import gaiasky.scene.record.GalaxyAttributes.HeightProfile;
import gaiasky.scene.record.GalaxyGenerator.GalaxyMorphology;
import gaiasky.scene.component.Body;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Matrix4Utils;
import gaiasky.util.math.StdRandom;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.LongValidator;

import java.text.DecimalFormat;
import java.util.List;

/**
 * The functionality shared by the procedural galaxy windows: the seed and morphology row, the size slider,
 * the position and rotation controls, the scrollable channel list, and the generate, export and
 * randomize buttons.
 * <p>
 * Everything that depends on <em>what</em> is being edited is left to the subclass through the
 * abstract hooks - which entities the controls act on, what goes into the channel list, what
 * Generate and Export do. This class only owns the layout, so two windows that edit different
 * things still look and behave the same.
 * <p>
 * It also owns the window's lifecycle: the title comes from {@link #getWindowTitleText()}, and the
 * window watches {@link Event#FOCUS_CHANGED} so that focusing a galaxy retargets it. Subclasses
 * <strong>must</strong> set their entities up before calling {@code buildSuper()}, because the
 * scaffold reads them while building. {@link #init(FocusView, String)} is that single entry point,
 * and {@link #reinitialize(FocusView)} is what the focus event drives.
 */
public abstract class AbstractGalaxyGenWindow extends GenericDialog implements IObserver {
    protected static final int SLIDER_STEPS = 1_000;
    protected static final float PAD5 = 5f;

    /* Layout, in pixels. Named here rather than in each window so that both look alike. */
    protected static final float FIELD_WIDTH_TOTAL = 950f;
    protected static final float TAB_CONTENT_WIDTH = 900f;
    protected static final float SCROLL_HEIGHT = 700f;
    protected static final float FULL_WIDTH_BOX = 850f;
    protected static final float HALF_WIDTH_BOX = FULL_WIDTH_BOX / 2f - 12f;
    protected static final float THIRD_WIDTH_BOX = FULL_WIDTH_BOX / 3f - 12f;
    protected static final float BUTTON_WIDTH = 380f;

    /**
     * Scroll position of the channel list, keyed by {@link #getPersistenceKey()}. Static so that it
     * survives the rebuild on every change, and so that reopening a window lands where it was left.
     */
    private static final ObjectFloatMap<Object> scrollPositions = new ObjectFloatMap<>();
    /** Items whose channel pane is expanded, keyed by the item itself. Static for the same reason. */
    private static final ObjectSet<Object> expandedItems = new ObjectSet<>();

    protected final GalaxyGenerator gen;
    protected final Scene scene;
    /** The morphology currently selected in the box. **/
    protected GalaxyMorphology morphology;
    /** Set when the morphology box changes, so that Generate knows it has to rebuild everything. **/
    protected boolean morphologyChanged;
    protected OwnSelectBox<GalaxyMorphology> morphologyBox;
    protected OwnSliderPlus sizeSlider;
    protected OwnTextIconButton randomizeButton;

    protected final Matrix4 m = new Matrix4();
    /** Reused to resolve the entity of {@link Event#FOCUS_CHANGED}, so no view is allocated per event. **/
    protected final FocusView eventView = new FocusView();
    private OwnScrollPane scroll;

    protected AbstractGalaxyGenWindow(Scene scene, Stage stage, Skin skin) {
        super("", skin, stage);
        this.scene = scene;
        this.gen = new GalaxyGenerator();
        setModal(false);
        setCancelText(I18n.msg("gui.close"));
    }

    // ---------------------------------------------------------------------------------------------
    // Hooks. Everything the scaffold needs to know about the thing being edited.
    // ---------------------------------------------------------------------------------------------

    /**
     * Sets up the entities this window edits. Called from the subclass' constructor before
     * {@code buildSuper()} - the scaffold reads the entities while building, so this has to have
     * happened first - and again from {@link #reinitialize(FocusView)} whenever the focus changes.
     *
     * @param target The focused entity to edit, or null to create a new object.
     * @param name   The name of the object to create when {@code target} is null, or null to derive
     *               one.
     */
    protected abstract void init(FocusView target,
                                 String name);

    /**
     * @param view The newly focused entity.
     *
     * @return Whether this window can edit it. Focusing something this window does not handle - a
     *         planet, a star - must leave it alone rather than retarget it at an unrelated object.
     */
    protected abstract boolean isEditableFocus(FocusView view);

    /**
     * Retargets the window at a focused entity and rebuilds it. This is the focus event's entry
     * point; the subclass only has to implement {@link #init(FocusView, String)}.
     *
     * @param target The focused entity to edit, or null to create a new object.
     */
    public void reinitialize(FocusView target) {
        init(target, null);
        rebuild();
    }

    /** @return The text of the header inside the content area. **/
    protected abstract String getHeaderText();

    /** @return The text of the window's own title bar. **/
    protected abstract String getWindowTitleText();

    /**
     * @return The entities whose body size follows the size slider, and which the position button
     *         moves. A billboard galaxy has two (full and half resolution); a hybrid one has the
     *         billboard and its volume.
     */
    protected abstract List<Entity> getSizedEntities();

    /**
     * @return The entities that receive the rotation set with the rx/ry/rz sliders. Defaults to the
     *         sized ones, which is right for both current windows.
     */
    protected List<Entity> getTransformedEntities() {
        return getSizedEntities();
    }

    /** @return The entity whose transform holds the rotation the sliders show. **/
    protected abstract Entity getTransformSource();

    /** @return The seed shown in the seed field. **/
    protected abstract long getSeed();

    /** @param seed The new seed, already validated. **/
    protected abstract void setSeed(long seed);

    /** @param morphology The newly selected morphology. **/
    protected abstract void onMorphologyChanged(GalaxyMorphology morphology);

    /**
     * Fills the channel list. The subclass adds one row per editable item, and is responsible for
     * the add and delete controls of each of them.
     *
     * @param table The table to fill, already inside the scroll pane.
     */
    protected abstract void buildChannelSections(Table table);

    /**
     * Adds the add-channel and any other channel-list buttons above the generate row. Leaving it
     * empty is fine for a window with a fixed set of channels.
     *
     * @param table The table to fill.
     */
    protected void buildChannelButtons(Table table) {
    }

    /** Called when Generate is pressed. **/
    protected abstract void onGenerate();

    /**
     * Discards the generation of the current object so that the renderer draws it again. Every edit
     * that changes what the compute pass produces goes through this, so the subclass decides what
     * "discard" means for the entities it holds - the billboard window releases its datasets, the
     * volume window its textures.
     */
    protected abstract void regenerate();

    /** Called when Export is pressed. **/
    protected abstract void onExport();

    /** Called when Randomize is pressed, after the seed has been replaced by a fresh one. **/
    protected abstract void onRandomize();

    /** @return The key that identifies what is being edited, used to remember the scroll position. **/
    protected abstract Object getPersistenceKey();

    /**
     * Attaches the mouse-over hint of a label key to a control.
     * <p>
     * The hint is read from {@code <key>.info}, so a control labelled with an i18n key gets its
     * tooltip from the same place as its text, and a key without a hint simply gets none instead of
     * showing a missing-key marker.
     *
     * @param actor The control to annotate.
     * @param key   The i18n key the control is labelled with.
     *
     * @param <T> The actor type.
     *
     * @return The same actor, so this can wrap a constructor call.
     */
    protected <T extends Actor> T tip(T actor, String key) {
        if (actor != null && I18n.hasMessage(key + ".info")) {
            actor.addListener(new OwnTextTooltip(I18n.msg(key + ".info"), skin));
        }
        return actor;
    }

    /**
     * @param entity An entity of this window, possibly null.
     *
     * @return Its body component, or null if the entity or the component is not there. Subclasses
     *         are allowed to declare entities that do not exist yet - a billboard galaxy with no
     *         half-resolution child, for instance - so every loop over them goes through this.
     */
    protected static Body bodyOf(Entity entity) {
        return entity == null ? null : Mapper.body.get(entity);
    }

    /** @return The size to show on the slider, in internal units. **/
    protected double getSize() {
        double size = 0;
        for (var entity : getSizedEntities()) {
            var body = bodyOf(entity);
            if (body != null) {
                size = Math.max(size, body.size);
            }
        }
        return size;
    }

    // ---------------------------------------------------------------------------------------------
    // Behaviour that the hooks above drive. Subclasses override only to change it.
    // ---------------------------------------------------------------------------------------------

    /**
     * Applies a new size to everything the window edits.
     *
     * @param internalSize The new size, in internal units.
     */
    protected void applySize(double internalSize) {
        for (var entity : getSizedEntities()) {
            var body = bodyOf(entity);
            if (body != null) {
                body.setSize(internalSize);
            }
        }
    }

    /**
     * Moves everything the window edits to a point in front of the camera.
     */
    protected void applyPosition() {
        GaiaSky.postRunnable(() -> {
            var camera = GaiaSky.instance.getICamera();
            var cdir = new Vector3D(camera.getDirection());
            var pos = new Vector3Q(camera.getPos());
            pos.add(cdir.nor().scl(getSize()));

            for (var entity : getSizedEntities()) {
                var body = bodyOf(entity);
                if (body != null) {
                    body.pos.set(pos);
                }
            }
        });
    }

    /**
     * Applies a rotation to everything the window edits.
     *
     * @param yaw   Rotation about y, in degrees.
     * @param pitch Rotation about x, in degrees.
     * @param roll  Rotation about z, in degrees.
     */
    protected void applyRotation(float yaw, float pitch, float roll) {
        m.setFromEulerAngles(pitch, yaw, roll);
        for (var entity : getTransformedEntities()) {
            if (entity == null) {
                continue;
            }
            var trf = Mapper.transform.get(entity);
            if (trf == null) {
                continue;
            }
            if (trf.matrix == null) {
                trf.matrix = new Matrix4D();
            }
            trf.matrix.set(m);
        }
    }

    /**
     * Rebuilds the UI. Called after anything that changes the structure of the channel list.
     */
    protected void rebuild() {
        content.clear();
        build();
    }

    /** @return Whether the pane of the given item was expanded when the window was last rebuilt. **/
    protected static boolean isExpanded(Object item) {
        return expandedItems.contains(item);
    }

    /** @param item     The item the pane belongs to. @param expanded Whether it is expanded. **/
    protected static void setExpanded(Object item,
                                      boolean expanded) {
        if (expanded) {
            expandedItems.add(item);
        } else {
            expandedItems.remove(item);
        }
    }

    @Override
    protected void build() {
        // Subscribed here rather than in the constructor: notify() calls abstract methods, so the
        // subclass has to be built - its fields assigned - before an event can reach it. Subscribing
        // is idempotent (EventManager keeps a set), so rebuilding does not stack handlers.
        EventManager.instance.subscribe(this, Event.FOCUS_CHANGED);

        // Title. OwnWindow draws it from its own label, so it is set here rather than in the
        // constructor: the text names what is being edited, which only init() can resolve.
        getTitleLabel().setText(getWindowTitleText());

        // Header
        var mainTitle = new OwnLabel(getHeaderText(), skin, "header");
        content.add(mainTitle).left().padBottom(pad20).row();

        // FIRST: the parameters shared by every channel.
        buildSeedAndMorphology();
        buildSize();
        buildPosition();
        buildRotation();

        // SECOND: the channel list.
        var channelTable = new Table(skin);
        channelTable.top();
        buildChannelSections(channelTable);

        scroll = new OwnScrollPane(channelTable, skin, "minimalist");
        scroll.setScrollingDisabled(true, false);
        scroll.setForceScroll(false, true);
        scroll.setSmoothScrolling(false);
        scroll.setWidth(FULL_WIDTH_BOX + 100f);
        scroll.setHeight(SCROLL_HEIGHT);
        scroll.addListener(new InputListener() {
            @Override
            public boolean scrolled(InputEvent event,
                                    float x,
                                    float y,
                                    float amountX,
                                    float amountY) {
                scrollPositions.put(getPersistenceKey(), scroll.getScrollY());
                return super.scrolled(event, x, y, amountX, amountY);
            }
        });

        content.add(new OwnLabel(I18n.msg("gui.galaxy.channels"), skin, "header")).left().padBottom(pad20).row();
        content.add(scroll).left().row();
        // The content height is only known once the table is laid out, so the scroll position is
        // restored after packing - before the buttons are added, as the legacy window does.
        content.pack();
        scroll.setScrollY(scrollPositions.get(getPersistenceKey(), 0f));

        // THIRD: the actions.
        buildButtons();
    }

    private void buildSeedAndMorphology() {
        var seedLabel = new OwnLabel(I18n.msg("gui.galaxy.randomize.seed"), skin);
        var seedValidator = new LongValidator(-999999L, 999999L);
        var seedField = new OwnTextField(Long.toString(getSeed()), skin, seedValidator);
        tip(seedField, "gui.galaxy.randomize.seed");
        tip(seedLabel, "gui.galaxy.randomize.seed");
        seedField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                if (seedField.isValid()) {
                    setSeed(seedField.getLongValue(123L));
                }
            }
        });

        var morphologyLabel = new OwnLabel(I18n.msg("gui.galaxy.morphology"), skin);
        morphologyBox = new OwnSelectBox<>(skin);
        tip(morphologyBox, "gui.galaxy.morphology");
        tip(morphologyLabel, "gui.galaxy.morphology");
        morphologyBox.setItems(GalaxyMorphology.values());
        if (morphology != null) {
            morphologyBox.setSelected(morphology);
        }
        morphologyBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                morphology = morphologyBox.getSelected();
                morphologyChanged = true;
                if (randomizeButton != null) {
                    // The label names the morphology, so it has to follow the box.
                    randomizeButton.setText(I18n.msg("gui.galaxy.randomize", morphology.name()));
                }
                onMorphologyChanged(morphology);
            }
        });

        var seedMorphTable = new Table(skin);
        seedMorphTable.add(seedLabel).padRight(pad10);
        seedMorphTable.add(seedField).padRight(pad34);
        seedMorphTable.add(morphologyLabel).padRight(pad10);
        seedMorphTable.add(morphologyBox);
        content.add(seedMorphTable).left().padBottom(pad20).row();
    }

    private void buildSize() {
        sizeSlider = new OwnSliderPlus(I18n.msg("gui.galaxy.size"), 0.2f, 40.0f, 0.1f, skin);
        tip(sizeSlider, "gui.galaxy.size");
        sizeSlider.setWidth(FIELD_WIDTH_TOTAL);
        sizeSlider.setValue((float) (getSize() * Constants.U_TO_KPC));
        sizeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                applySize(sizeSlider.getValue() * Constants.KPC_TO_U);
            }
        });
        content.add(sizeSlider).left().padBottom(pad20).row();
    }

    private void buildPosition() {
        var position = new OwnTextButton(I18n.msg("gui.galaxy.pos"), skin);
        tip(position, "gui.galaxy.pos");
        position.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                applyPosition();
            }
        });
        content.add(position).left().padBottom(pad20).row();
    }

    private void buildRotation() {
        var source = getTransformSource();
        var trf = source == null ? null : Mapper.transform.get(source);
        if (trf == null || trf.matrix == null) {
            m.idt();
        } else {
            trf.matrix.putIn(m);
        }
        var euler = Matrix4Utils.recoverEulerAngles(m);

        float rotMin = -180.0f;
        float rotMax = 180.0f;
        float rotStep = (rotMax - rotMin) / SLIDER_STEPS;
        var rx = new OwnSliderReset(I18n.msg("gui.galaxy.ds.rx"), rotMin, rotMax, rotStep, 0f, skin);
        tip(rx, "gui.galaxy.ds.rx");
        var ry = new OwnSliderReset(I18n.msg("gui.galaxy.ds.ry"), rotMin, rotMax, rotStep, 0f, skin);
        tip(ry, "gui.galaxy.ds.ry");
        var rz = new OwnSliderReset(I18n.msg("gui.galaxy.ds.rz"), rotMin, rotMax, rotStep, 0f, skin);
        tip(rz, "gui.galaxy.ds.rz");

        rx.setWidth(THIRD_WIDTH_BOX);
        rx.setValue(euler[0]);
        ry.setWidth(THIRD_WIDTH_BOX);
        ry.setValue(euler[1]);
        rz.setWidth(THIRD_WIDTH_BOX);
        rz.setValue(euler[2]);

        ChangeListener rotation = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                applyRotation(rx.getMappedValue(), ry.getMappedValue(), rz.getMappedValue());
            }
        };
        rx.addListener(rotation);
        ry.addListener(rotation);
        rz.addListener(rotation);

        var trfTable = new Table(skin);
        trfTable.add(rx).left().padRight(PAD5);
        trfTable.add(ry).left().padRight(PAD5);
        trfTable.add(rz).left();
        content.add(trfTable).left().padBottom(pad20).row();
    }

    private void buildButtons() {
        var buttonsTop = new Table(skin);
        var buttonsBottom = new Table(skin);

        buildChannelButtons(buttonsTop);

        // Generate
        var generate = new OwnTextIconButton(I18n.msg("gui.galaxy.generate"), skin, "generate");
        tip(generate, "gui.galaxy.generate");
        generate.setColor(ColorUtils.gGreenC);
        generate.setWidth(BUTTON_WIDTH);
        generate.pad(pad10, pad20, pad10, pad20);
        generate.addListener(new OwnTextTooltip(I18n.msg("gui.galaxy.generate.info"), skin));
        generate.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                onGenerate();
            }
        });

        // Export
        var export = new OwnTextIconButton("", skin, "export");
        export.pad(pad10, pad20, pad10, pad20);
        export.addListener(new OwnTextTooltip(I18n.msg("gui.galaxy.export.info"), skin));
        export.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                onExport();
            }
        });

        var genTable = new Table(skin);
        genTable.add(generate).left().padRight(pad20);
        genTable.add(export);
        buttonsTop.add(genTable).colspan(2).center();

        // Randomize
        var morphologyName = morphologyBox != null && morphologyBox.getSelected() != null
                ? morphologyBox.getSelected().name()
                : GalaxyMorphology.Sc.name();
        randomizeButton = new OwnTextIconButton(I18n.msg("gui.galaxy.randomize", morphologyName), skin, "random");
        tip(randomizeButton, "gui.galaxy.randomize");
        randomizeButton.setColor(ColorUtils.gYellowC);
        randomizeButton.setWidth(BUTTON_WIDTH - 80f);
        randomizeButton.pad(pad10, pad20, pad10, pad20);
        randomizeButton.addListener(new OwnTextTooltip(I18n.msg("gui.galaxy.randomize.info"), skin));
        randomizeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                // New seed, same morphology.
                long seed = StdRandom.uniform(999999);
                setSeed(seed);
                onRandomize();
            }
        });
        buttonsBottom.add(randomizeButton).center().padBottom(pad10).row();

        content.add(buttonsTop).center().padTop(pad34).padBottom(pad20).row();
        content.add(new Separator(skin, "gray")).fillX().expandX().padBottom(pad20).row();
        content.add(buttonsBottom).center();
    }

    protected OwnTextIconButton newAddChannelButton(Entity entity, String key, float buttonWidth) {
        var button = new OwnTextIconButton(I18n.msg(key), skin, "new");
        button.setWidth(buttonWidth);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                GaiaSky.postRunnable(() -> {
                    var bb = Mapper.billboardSet.get(entity);
                    var ds = bb.datasets;
                    var newDs = new BillboardDataset[ds.length + 1];
                    System.arraycopy(ds, 0, newDs, 0, ds.length);

                    var dataset = new BillboardDataset();
                    dataset.setType(ChannelType.POINT);
                    dataset.setLayers(new int[]{0, 1, 2});
                    dataset.setBaseColor(new double[]{0.8, 0.8, 0.8});
                    dataset.setMaxSize(20.0);
                    newDs[ds.length] = dataset;

                    bb.datasets = newDs;

                    rebuild();
                });
            }
        });
        button.pad(pad10, pad20, pad10, pad20);
        button.addListener(new OwnTextTooltip(I18n.msg(key + ".info"), skin));
        return button;
    }

    /**
     * @return The channel types that a billboard dataset may take in this window. All of them by
     *         default; a window that draws the diffuse channels some other way narrows this to the
     *         ones it actually samples as particles.
     */
    protected ChannelType[] getBillboardChannelTypes() {
        return ChannelType.values();
    }

    /**
     * @param current The type the dataset being edited has, or null.
     *
     * @return The channel types of {@link #getBillboardChannelTypes()} as select box items. A type the
     *         window does not offer - one loaded from a file, say - is appended so that the box can
     *         still show the dataset as it is instead of failing to select anything.
     */
    private Array<ComboBoxBean<ChannelType>> billboardChannelBeans(ChannelType current) {
        var allowed = new Array<>(getBillboardChannelTypes());
        if (current != null && !allowed.contains(current, true)) {
            allowed.add(current);
        }
        return new Array<>(ComboBoxBean.getValues(allowed));
    }

    /**
     * Adds the billboard datasets of the given entity to the given table.
     *
     * @param entity          The entity.
     * @param datasetsTable   The table.
     * @param thirdWidthBox   The 1/3 width.
     * @param halfWidthBox    The half width.
     * @param fullWidthBox    The bull width.
     * @param tabContentWidth The tab content width.
     */
    protected void addBillboardDatasets(Entity entity, Table datasetsTable, float thirdWidthBox, float halfWidthBox, float fullWidthBox, float tabContentWidth) {
        if (entity == null) {
            return;
        }
        final var cpSize = 32f;
        var render = Mapper.render.get(entity);
        var billboard = Mapper.billboardSet.get(entity);
        var datasets = billboard.datasets;
        var half = render.halfResolutionBuffer;
        int channel = 0;
        for (var ds : datasets) {
            var dsTable = new Table(skin);
            // Type
            OwnSelectBox<ComboBoxBean<ChannelType>> type = new OwnSelectBox<>(skin);
            var typeBeans = billboardChannelBeans(ds.type);
            type.setItems(typeBeans);
            type.setWidth(halfWidthBox);
            // Selected by value, not by ordinal: the offered list is a subset of the enum.
            for (int i = 0; i < typeBeans.size; i++) {
                if (typeBeans.get(i).value == ds.type) {
                    type.setSelectedIndex(i);
                    break;
                }
            }
            type.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.type = type.getSelected().value;
                    if (ds.type == ChannelType.DUST) {
                        GaiaSky.postRunnable(() -> {
                            // Subtractive blending.
                            ds.setBlending(BlendMode.SUBTRACTIVE);
                            ds.setDepthMask(false);
                        });
                    }
                    rebuild();
                }
            });
            tip(type, "gui.galaxy.ds.type");
            OwnLabel typeLabel = new OwnLabel(I18n.msg("gui.galaxy.ds.type"), skin);
            typeLabel.setWidth(halfWidthBox);

            dsTable.add(typeLabel).left().padRight(pad20).padBottom(pad18);
            dsTable.add(type).left().padBottom(pad18).row();

            // Distribution
            OwnSelectBox<ComboBoxBean<Distribution>> distribution = new OwnSelectBox<>(skin);
            distribution.setItems(ComboBoxBean.getValues(Distribution.class));
            distribution.setWidth(halfWidthBox);
            distribution.setSelectedIndex(ds.distribution.ordinal());
            distribution.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.distribution = distribution.getSelected().value;
                    rebuild();
                    regenerate();
                }
            });
            tip(distribution, "gui.galaxy.ds.distribution");
            OwnLabel distributionLabel = new OwnLabel(I18n.msg("gui.galaxy.ds.distribution"), skin);
            distributionLabel.setWidth(halfWidthBox);

            dsTable.add(distributionLabel).left().padRight(pad20).padBottom(pad18);
            dsTable.add(distribution).left().padBottom(pad18).row();

            // Num particles
            float nMin = ds.type.nParticles[0];
            float nMax = ds.type.nParticles[1];
            float nStep = (nMax - nMin) / SLIDER_STEPS;
            var nParticles = new OwnSliderReset(I18n.msg("gui.galaxy.ds.particles"), nMin, nMax, nStep, nMin + (nMax - nMin) / 2, skin);
            tip(nParticles, "gui.galaxy.ds.particles");
            nParticles.setNumberFormatter(new DecimalFormat("#####0"));
            nParticles.setWidth(fullWidthBox);
            nParticles.setValue(ds.particleCount);
            nParticles.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setParticleCount((long) nParticles.getValue());
                }
            });
            dsTable.add(nParticles).colspan(2).left().padBottom(pad18).row();

            // Dataset translation, XYZ
            float transMin = -1.0f;
            float transMax = 1.0f;
            float transStep = (transMax - transMin) / SLIDER_STEPS;
            var x = new OwnSliderReset(I18n.msg("gui.galaxy.ds.x"), transMin, transMax, transStep, 0f, skin);
            tip(x, "gui.galaxy.ds.x");
            x.setWidth(thirdWidthBox);
            x.setValue(ds.translation.x);
            x.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.translation.x = x.getValue();
                }
            });
            var y = new OwnSliderReset(I18n.msg("gui.galaxy.ds.y"), transMin, transMax, transStep, 0f, skin);
            tip(y, "gui.galaxy.ds.y");
            y.setWidth(thirdWidthBox);
            y.setValue(ds.translation.y);
            y.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.translation.y = y.getValue();
                }
            });
            var z = new OwnSliderReset(I18n.msg("gui.galaxy.ds.z"), transMin, transMax, transStep, 0f, skin);
            tip(z, "gui.galaxy.ds.z");
            z.setWidth(thirdWidthBox);
            z.setValue(ds.translation.z);
            z.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.translation.z = z.getValue();
                }
            });
            Table translationTable = new Table(skin);
            translationTable.add(x).left().padRight(PAD5);
            translationTable.add(y).left().padRight(PAD5);
            translationTable.add(z).left();
            dsTable.add(translationTable).colspan(2).left().padBottom(pad18).row();

            // Dataset rotation
            float rotMin = -180.0f;
            float rotMax = 180.0f;
            float rotStep = (rotMax - rotMin) / SLIDER_STEPS;
            var rx = new OwnSliderReset(I18n.msg("gui.galaxy.ds.rx"), rotMin, rotMax, rotStep, 0f, skin);
            tip(rx, "gui.galaxy.ds.rx");
            rx.setWidth(thirdWidthBox);
            rx.setValue(ds.rotation.x);
            rx.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.rotation.x = rx.getValue();
                }
            });
            var ry = new OwnSliderReset(I18n.msg("gui.galaxy.ds.ry"), rotMin, rotMax, rotStep, 0f, skin);
            tip(ry, "gui.galaxy.ds.ry");
            ry.setWidth(thirdWidthBox);
            ry.setValue(ds.rotation.y);
            ry.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.rotation.y = ry.getValue();
                }
            });
            var rz = new OwnSliderReset(I18n.msg("gui.galaxy.ds.rz"), rotMin, rotMax, rotStep, 0f, skin);
            tip(rz, "gui.galaxy.ds.rz");
            rz.setWidth(thirdWidthBox);
            rz.setValue(ds.rotation.z);
            rz.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.rotation.z = rz.getValue();
                }
            });
            Table rotTable = new Table(skin);
            rotTable.add(rx).left().padRight(PAD5);
            rotTable.add(ry).left().padRight(PAD5);
            rotTable.add(rz).left();
            dsTable.add(rotTable).colspan(2).left().padBottom(pad18).row();

            // Dataset scale
            float sMin = -3.0f;
            float sMax = 3.0f;
            float sStep = (sMax - sMin) / SLIDER_STEPS;
            var sx = new OwnSliderReset(I18n.msg("gui.galaxy.ds.sx"), sMin, sMax, sStep, 1f, skin);
            tip(sx, "gui.galaxy.ds.sx");
            sx.setWidth(thirdWidthBox);
            sx.setValue(ds.scale.x);
            sx.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.scale.x = sx.getValue();
                }
            });
            var sy = new OwnSliderReset(I18n.msg("gui.galaxy.ds.sy"), sMin, sMax, sStep, 1f, skin);
            tip(sy, "gui.galaxy.ds.sy");
            sy.setWidth(thirdWidthBox);
            sy.setValue(ds.scale.y);
            sy.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.scale.y = sy.getValue();
                }
            });
            var sz = new OwnSliderReset(I18n.msg("gui.galaxy.ds.sz"), sMin, sMax, sStep, 1f, skin);
            tip(sz, "gui.galaxy.ds.sz");
            sz.setWidth(thirdWidthBox);
            sz.setValue(ds.scale.z);
            sz.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.scale.z = sz.getValue();
                }
            });
            var sclTable = new Table(skin);
            sclTable.add(sx).left().padRight(PAD5);
            sclTable.add(sy).left().padRight(PAD5);
            sclTable.add(sz).left();
            dsTable.add(sclTable).colspan(2).left().padBottom(pad18).row();

            // Colors and color noise
            var cLabel = new OwnLabel(I18n.msg("gui.galaxy.ds.color"), skin);
            var c1 = new ColorPicker("c1", ds.getColorRGBA(0), stage, skin);
            tip(c1, "gui.galaxy.ds.color");
            tip(cLabel, "gui.galaxy.ds.color");
            c1.setNewColorRunnable(() -> {
                ds.setColorRGBA(c1.color(), 0);
            });
            var c2 = new ColorPicker("c2", ds.getColorRGBA(1), stage, skin);
            tip(c2, "gui.galaxy.ds.color");
            c2.setNewColorRunnable(() -> {
                ds.setColorRGBA(c2.color(), 1);
            });
            var c3 = new ColorPicker("c3", ds.getColorRGBA(2), stage, skin);
            tip(c3, "gui.galaxy.ds.color");
            c3.setNewColorRunnable(() -> {
                ds.setColorRGBA(c3.color(), 2);
            });
            var c4 = new ColorPicker("c4", ds.getColorRGBA(3), stage, skin);
            tip(c4, "gui.galaxy.ds.color");
            c4.setNewColorRunnable(() -> {
                ds.setColorRGBA(c4.color(), 3);
            });
            var colorsTable = new Table(skin);
            colorsTable.add(cLabel).left().padRight(pad34);
            colorsTable.add(c1).size(cpSize).left().padRight(pad10);
            colorsTable.add(c2).size(cpSize).left().padRight(pad10);
            colorsTable.add(c3).size(cpSize).left().padRight(pad10);
            colorsTable.add(c4).size(cpSize).left().padRight(pad10);
            var colorNoise = new OwnSliderReset(I18n.msg("gui.galaxy.ds.color.noise"), 0.0f, 1f, 0.01f, 0f, skin);
            tip(colorNoise, "gui.galaxy.ds.color.noise");
            colorNoise.setWidth(halfWidthBox);
            colorNoise.setValue(ds.colorNoise);
            colorNoise.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setColorNoise((double) colorNoise.getValue());
                }
            });
            dsTable.add(colorsTable).left().padRight(PAD5).padBottom(pad18);
            dsTable.add(colorNoise).left().padBottom(pad18).row();

            if (ds.distribution.isFlat()) {
                // Warp
                var warp = new OwnSliderReset(I18n.msg("gui.galaxy.ds.warp"), ds.type.warpStrength[0], ds.type.warpStrength[1], 0.001f, 0f, skin);
                tip(warp, "gui.galaxy.ds.warp");
                warp.setWidth(fullWidthBox);
                warp.setValue(ds.warpStrength);
                warp.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setWarpStrength((double) warp.getValue());
                    }
                });
                dsTable.add(warp).colspan(2).left().padBottom(pad18).row();

                // Height scale
                var heightScale = new OwnSliderReset(I18n.msg("gui.galaxy.ds.height.scale"), 0.0f, 0.2f, 0.001f, 0.01f, skin);
                tip(heightScale, "gui.galaxy.ds.height.scale");
                heightScale.setWidth(halfWidthBox);
                heightScale.setValue(ds.heightScale);
                heightScale.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setHeightScale((double) heightScale.getValue());
                    }
                });

                // Height profile
                OwnSelectBox<HeightProfile> heightProfile = new OwnSelectBox<>(skin);
                heightProfile.setItems(HeightProfile.values());
                heightProfile.setWidth(halfWidthBox);
                heightProfile.setSelected(ds.heightProfile);
                heightProfile.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.heightProfile = heightProfile.getSelected();
                    }
                });
                tip(heightProfile, "gui.galaxy.ds.height.profile");
                OwnLabel heightProfileLabel = new OwnLabel(I18n.msg("gui.galaxy.ds.height.profile"), skin);
                heightProfileLabel.setWidth(halfWidthBox);

                dsTable.add(heightProfile).left().padBottom(pad18).padRight(PAD5);
                dsTable.add(heightScale).left().padBottom(pad18).row();

            }

            // Bar only has base_radius. All the others have also min_radius.
            // Min and base radii
            float minRadMin = ds.type.minRadius[0];
            float minRadMax = ds.type.minRadius[1];
            float minRadStep = (minRadMax - minRadMin) / SLIDER_STEPS;
            var minRadius = new OwnSliderReset(I18n.msg("gui.galaxy.ds.radius.min"),
                                               minRadMin,
                                               minRadMax,
                                               minRadStep,
                                               minRadMin + (minRadMax - minRadMin) / 2f,
                                               skin);
            tip(minRadius, "gui.galaxy.ds.radius.min");
            minRadius.setWidth(halfWidthBox);
            minRadius.setValue(ds.minRadius);
            minRadius.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setMinRadius((double) minRadius.getValue());
                }
            });
            float baseRadMin = ds.type.baseRadius[0];
            float baseRadMax = ds.type.baseRadius[1];
            float baseRadStep = (baseRadMax - baseRadMin) / SLIDER_STEPS;
            var baseRadius = new OwnSliderReset(I18n.msg("gui.galaxy.ds.radius.base"),
                                                baseRadMin,
                                                baseRadMax,
                                                baseRadStep,
                                                baseRadMin + (baseRadMax - baseRadMin) / 2f,
                                                skin);
            tip(baseRadius, "gui.galaxy.ds.radius.base");
            baseRadius.setWidth(ds.distribution.isBar() ? fullWidthBox : halfWidthBox);
            baseRadius.setValue(ds.baseRadius);
            baseRadius.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setBaseRadius((double) baseRadius.getValue());
                }
            });
            if (!ds.distribution.isBar()) {
                dsTable.add(minRadius).left().padRight(PAD5).padBottom(pad18);
                dsTable.add(baseRadius).left().padBottom(pad18).row();
            } else {
                dsTable.add(baseRadius).colspan(2).left().padBottom(pad18).row();
            }

            // Particle size and size noise
            float sizeMin = ds.type.size[0];
            float sizeMax = ds.type.size[1];
            var size = new OwnSliderReset(I18n.msg("gui.galaxy.ds.size"), sizeMin, sizeMax, true, sizeMin + (sizeMax - sizeMin) / 2f, skin);
            tip(size, "gui.galaxy.ds.size");
            size.setNumberFormatter(new DecimalFormat("######0.########"));
            size.setDisplayValueMapped(true);
            size.setLogarithmicExponent(4.0);
            size.setWidth(thirdWidthBox + 115f);
            size.setMappedValue(ds.size);
            size.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setSize((double) size.getMappedValue());
                }
            });
            var sizeMask = new OwnCheckBox(I18n.msg("gui.galaxy.ds.size.perlin"), skin, 4f);
            tip(sizeMask, "gui.galaxy.ds.size.perlin");
            sizeMask.setChecked(ds.sizeMask);
            sizeMask.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    ds.setSizeMask(sizeMask.isChecked());
                    if (ds.sizeMask) {
                        ds.sizeNoise = 20f;
                    } else {
                        ds.sizeNoise = 0.2f;
                    }
                    GaiaSky.postRunnable(() -> {
                        rebuild();
                    });
                }
            });
            OwnSliderReset sizeNoise;
            if (ds.sizeMask) {
                sizeNoise = new OwnSliderReset(I18n.msg("gui.galaxy.ds.size.scale"), 1.0f, 50f, 0.01f, 0f, skin);
                tip(sizeNoise, "gui.galaxy.ds.size.scale");
            } else {
                sizeNoise = new OwnSliderReset(I18n.msg("gui.galaxy.ds.size.noise"), 0.0f, 1f, 0.01f, 10f, skin);
                tip(sizeNoise, "gui.galaxy.ds.size.noise");
            }
            sizeNoise.setWidth(thirdWidthBox);
            sizeNoise.setValue(ds.sizeNoise);
            sizeNoise.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.sizeNoise = sizeNoise.getValue();
                }
            });
            var sizeTable = new Table(skin);
            sizeTable.add(size).left().padRight(pad34);
            sizeTable.add(sizeMask).right().padRight(PAD5);
            sizeTable.add(sizeNoise).left().padRight(PAD5);

            dsTable.add(sizeTable).colspan(2).left().padBottom(pad18).row();

            // Intensity
            float iMin = ds.type.intensity[0];
            float iMax = ds.type.intensity[1];
            float iStep = (iMax - iMin) / SLIDER_STEPS;
            var intensity = new OwnSliderReset(I18n.msg("gui.galaxy.ds.intensity"), iMin, iMax, iStep, iMin + (iMax - iMin) / 2f, skin);
            tip(intensity, "gui.galaxy.ds.intensity");
            intensity.setDisplayValueMapped(true);
            intensity.setNumberFormatter(new DecimalFormat("#0.######"));
            intensity.setWidth(fullWidthBox);
            intensity.setMappedValue(ds.intensity);
            intensity.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setIntensity((double) intensity.getMappedValue());
                }
            });
            dsTable.add(intensity).colspan(2).left().padBottom(pad18).row();

            if (ds.distribution.isLogarithmicSpiral()) {
                // Number of arms
                var numArms = new OwnSliderReset(I18n.msg("gui.galaxy.ds.arm.number"), 1f, 8f, 1f, 4f, skin);
                tip(numArms, "gui.galaxy.ds.arm.number");
                numArms.setNumberFormatter(new DecimalFormat("#0"));
                numArms.setWidth(halfWidthBox);
                numArms.setValue(ds.numArms);
                numArms.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setSpiralArms((long) numArms.getValue());
                    }
                });
                // Arm sigma
                float minSigma = ds.type.armSigma[0];
                float maxSigma = ds.type.armSigma[1];
                float stepSigma = (maxSigma - minSigma) / SLIDER_STEPS;
                var armSigma = new OwnSliderReset(I18n.msg("gui.galaxy.ds.arm.sigma"), minSigma, maxSigma, stepSigma, 0.2f, skin);
                tip(armSigma, "gui.galaxy.ds.arm.sigma");
                armSigma.setNumberFormatter(new DecimalFormat("#0.####"));
                armSigma.setWidth(halfWidthBox);
                armSigma.setValue(ds.armSigma);
                armSigma.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setArmSigma((double) armSigma.getValue());
                    }
                });

                dsTable.add(numArms).left().padRight(PAD5).padBottom(pad18);
                dsTable.add(armSigma).left().padBottom(pad18).row();
            }
            if (ds.distribution.isDensityWaveSpiral()) {
                // Number of arms
                var numArms = new OwnSliderReset(I18n.msg("gui.galaxy.ds.arm.number"), 2f, 4f, 2f, 4f, skin);
                tip(numArms, "gui.galaxy.ds.arm.number");
                numArms.setNumberFormatter(new DecimalFormat("#0"));
                numArms.setWidth(fullWidthBox);
                numArms.setValue(ds.numArms);
                numArms.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setSpiralArms((long) numArms.getValue());
                    }
                });
                dsTable.add(numArms).colspan(2).left().padBottom(pad18).row();
            }

            if (ds.distribution.isAnySpiral()) {
                // Spiral angle
                float angleMin = ds.type.baseAngle[0];
                float angleMax = ds.type.baseAngle[1];
                var baseAngle = new OwnSliderReset(I18n.msg("gui.galaxy.ds.angle"),
                                                   angleMin,
                                                   angleMax,
                                                   true,
                                                   angleMin + (angleMax - angleMin) / 2f,
                                                   skin);
                tip(baseAngle, "gui.galaxy.ds.angle");
                baseAngle.setLogarithmicExponent(1.2);
                baseAngle.setNumberFormatter(new DecimalFormat("##0.###"));
                baseAngle.setWidth(fullWidthBox);
                baseAngle.setValue(ds.baseAngle);
                baseAngle.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setBaseAngle((double) baseAngle.getValue());
                    }
                });
                dsTable.add(baseAngle).colspan(2).left().padBottom(pad18).row();
            }

            if (ds.distribution.isDensityWaveSpiral()) {
                // Single eccentricity.
                float eMin = ds.type.eccentricity[0];
                float eMax = ds.type.eccentricity[1];
                float eStep = (eMax - eMin) / SLIDER_STEPS;
                var ec = new OwnSliderReset(I18n.msg("gui.galaxy.ds.eccentricity"), eMin, eMax, eStep, 0.3f, skin);
                tip(ec, "gui.galaxy.ds.eccentricity");
                ec.setNumberFormatter(new DecimalFormat("0.####"));
                ec.setWidth(fullWidthBox);
                ec.setValue(ds.eccentricity[0]);
                ec.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setEccentricity((double) ec.getValue());
                    }
                });
                dsTable.add(ec).colspan(2).left().padBottom(pad18).row();
            } else if (ds.distribution.isEllipsoid()) {
                // Eccentricity in X and Y.
                float eMin = ds.type.eccentricity[0];
                float eMax = ds.type.eccentricity[1];
                float eStep = (eMax - eMin) / SLIDER_STEPS;
                var ecx = new OwnSliderReset(I18n.msg("gui.galaxy.ds.eccentricity.x"), eMin, eMax, eStep, 0.0f, skin);
                tip(ecx, "gui.galaxy.ds.eccentricity.x");
                ecx.setNumberFormatter(new DecimalFormat("0.####"));
                ecx.setWidth(halfWidthBox);
                ecx.setValue(ds.eccentricity[0]);
                ecx.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setEccentricityX((double) ecx.getValue());
                    }
                });
                var ecy = new OwnSliderReset(I18n.msg("gui.galaxy.ds.eccentricity.y"), eMin, eMax, eStep, 0.0f, skin);
                tip(ecy, "gui.galaxy.ds.eccentricity.y");
                ecy.setNumberFormatter(new DecimalFormat("0.####"));
                ecy.setWidth(halfWidthBox);
                ecy.setValue(ds.eccentricity[1]);
                ecy.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setEccentricityY((double) ecy.getValue());
                    }
                });
                dsTable.add(ecy).left().padRight(PAD5).padBottom(pad18);
                dsTable.add(ecx).left().padBottom(pad18).row();
            }

            if (ds.distribution.isDensityWaveSpiral()) {
                // Displacement in x and y
                float dMin = ds.type.spiralDeltaPos[0];
                float dMax = ds.type.spiralDeltaPos[1];
                float dStep = (dMax - dMin) / SLIDER_STEPS;
                var deltaX = new OwnSliderReset(I18n.msg("gui.galaxy.ds.delta.x"), dMin, dMax, dStep, 0f, skin);
                tip(deltaX, "gui.galaxy.ds.delta.x");
                deltaX.setWidth(halfWidthBox);
                deltaX.setValue(ds.spiralDeltaPos[0]);
                deltaX.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.spiralDeltaPos[0] = deltaX.getValue();
                    }
                });
                var deltaY = new OwnSliderReset(I18n.msg("gui.galaxy.ds.delta.y"), dMin, dMax, dStep, 0f, skin);
                tip(deltaY, "gui.galaxy.ds.delta.y");
                deltaY.setWidth(halfWidthBox);
                deltaY.setValue(ds.spiralDeltaPos[1]);
                deltaY.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.spiralDeltaPos[1] = deltaY.getValue();
                    }
                });
                dsTable.add(deltaX).left().padRight(PAD5).padBottom(pad18);
                dsTable.add(deltaY).left().padBottom(pad18).row();
            }

            if (ds.distribution.isBar()) {
                // Aspect
                float aspectMin = ds.type.aspect[0];
                float aspectMax = ds.type.aspect[1];
                float aspectStep = (aspectMax - aspectMin) / SLIDER_STEPS;
                var aspect = new OwnSliderReset(I18n.msg("gui.galaxy.ds.aspect"), aspectMin, aspectMax, aspectStep, 1f, skin);
                tip(aspect, "gui.galaxy.ds.aspect");
                aspect.setNumberFormatter(new DecimalFormat("#0.###"));
                aspect.setWidth(fullWidthBox);
                aspect.setValue(ds.aspect);
                aspect.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setAspect((double) aspect.getMappedValue());
                    }
                });
                dsTable.add(aspect).colspan(2).left().padBottom(pad18).row();
            }

            // Delete dataset (top icon)
            var delete = new OwnTextIconButton("", skin, "rubbish");
            tip(delete, "gui.galaxy.delete");
            delete.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    var e = entity;
                    var bb = Mapper.billboardSet.get(e);
                    bb.removeDataset(ds);
                    EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this, Mapper.render.get(e));
                    rebuild();
                }
            });

            // Header
            String key = "gui.galaxy.channel." + (half ? "half" : "full");
            String name = ds.type.name();
            String title = I18n.msg(key, channel, name);
            CollapsiblePane groupPane = new CollapsiblePane(stage, title,
                                                            dsTable, tabContentWidth, skin, "header-s", "expand-collapse",
                                                            null, isExpanded(ds), null, delete);
            // Update set of expanded datasets whenever this group pane is expanded or collapsed.
            groupPane.setExpandCollapseRunnable(() -> {
                var expanded = groupPane.isExpanded();
                setExpanded(ds, expanded);
            });

            datasetsTable.add(groupPane).padBottom(pad18).row();
            channel++;
        }
    }

    @Override
    protected boolean accept() {
        return false;
    }

    @Override
    protected void cancel() {
        // Closing is the only action.
    }

    // ---------------------------------------------------------------------------------------------
    // IObserver.
    // ---------------------------------------------------------------------------------------------

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        if (event == Event.FOCUS_CHANGED && data.length > 0) {
            if (data[0] instanceof String name) {
                eventView.setEntity(scene.getEntity(name));
            } else if (data[0] instanceof FocusView fv) {
                eventView.setEntity(fv.getEntity());
            } else {
                return;
            }
            if (isEditableFocus(eventView)) {
                reinitialize(eventView);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Cleanup. The window is an observer, so it has to unsubscribe when it goes away; leaving the
    // subscription behind keeps a removed window alive and lets it retarget entities it no longer
    // draws.
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean remove() {
        EventManager.instance.removeAllSubscriptions(this);
        return super.remove();
    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
