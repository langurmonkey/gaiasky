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
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectSet;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.record.GalaxyGenerator;
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

    /** Called when Export is pressed. **/
    protected abstract void onExport();

    /** Called when Randomize is pressed, after the seed has been replaced by a fresh one. **/
    protected abstract void onRandomize();

    /** @return The key that identifies what is being edited, used to remember the scroll position. **/
    protected abstract Object getPersistenceKey();

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
        var ry = new OwnSliderReset(I18n.msg("gui.galaxy.ds.ry"), rotMin, rotMax, rotStep, 0f, skin);
        var rz = new OwnSliderReset(I18n.msg("gui.galaxy.ds.rz"), rotMin, rotMax, rotStep, 0f, skin);

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
