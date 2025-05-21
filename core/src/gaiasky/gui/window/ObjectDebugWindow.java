/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.view.FilterView;
import gaiasky.scene.view.FocusView;
import gaiasky.util.CatalogInfo;
import gaiasky.util.Logger;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * This window provides a way to review and manipulate objects' internals. It acts on {@link com.badlogic.ashley.core.Entity}
 * objects as base units. It exposes the internal {@link com.badlogic.ashley.core.Component} objects and
 * their attributes.
 */
public class ObjectDebugWindow extends GenericDialog implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(ObjectDebugWindow.class);

    private final Scene scene;
    private final FocusView view;
    private final FilterView filterView;
    private final SortedSet<String> matching;
    private final Array<OwnLabel> matchingLabels;
    private final Vector2 aux;

    private String currentInputText = "";
    private OwnTextField searchInput;
    private final Array<Timer.Task> tasks;
    private Cell<?> infoCell, contentCell;
    private OwnLabel infoMessage;
    private Table candidates;
    private int cIdx = -1;

    private static final float width = 950f;
    private static final float height = 820f;

    public ObjectDebugWindow(Stage ui, Skin skin, Scene scene) {
        super(I18n.msg("gui.debug.object.title"), skin, ui);

        this.scene = scene;
        this.view = new FocusView();
        this.filterView = new FilterView();
        this.matching = new TreeSet<>();
        this.matchingLabels = new Array<>(10);
        this.tasks = new Array<>(20);
        this.aux = new Vector2();

        // Do not close with ENTER or ESC.
        keysListener = false;

        setModal(false);
        setAcceptText(I18n.msg("gui.close"));

        buildSuper();
        info(I18n.msg("gui.debug.info"));

        EventManager.instance.subscribe(this, Event.FOCUS_CHANGED);
    }

    @Override
    protected void build() {
        content.clear();

        candidates = new Table(skin);
        candidates.setBackground("table-bg");
        candidates.setFillParent(false);

        searchInput = new OwnTextField("", skin);
        searchInput.setWidth(width);
        searchInput.setMessageText(I18n.msg("gui.objects.search"));
        searchInput.addListener(event -> {
            if (event instanceof InputEvent ie) {
                int matchingSize = matching.size();
                int code = ie.getKeyCode();
                if (ie.getType() == InputEvent.Type.keyUp) {
                    if (code == Input.Keys.ESCAPE || code == Input.Keys.ENTER) {
                        if (cIdx >= 0) {
                            checkString(searchInput.getText(), scene);
                        }
                        removeCandidates();
                        return false;
                    } else if (code == Input.Keys.TAB && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && matchingSize > 0) {
                        cIdx = cIdx - 1 < 0 ? matchingSize - 1 : cIdx - 1;
                        selectMatch();
                        return false;
                    } else if (code == Input.Keys.TAB && !Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && matchingSize > 0) {
                        cIdx = (cIdx + 1) % matchingSize;
                        selectMatch();
                        return false;
                    } else if (!searchInput.getText().equals(currentInputText) && !searchInput.getText().isBlank()) {
                        // Process only if text changed
                        currentInputText = searchInput.getText();
                        String name = currentInputText.toLowerCase().trim();

                        // New task
                        Timer.Task task = new Timer.Task() {
                            public void run() {
                                synchronized (matching) {
                                    matchingNodes(name, scene);

                                    if (!matching.isEmpty()) {
                                        cIdx = -1;
                                        candidates.clear();
                                        matching.forEach(match -> {
                                            OwnLabel m = new OwnLabel(match, skin);
                                            m.addListener((evt) -> {
                                                if (evt instanceof InputEvent iEvt) {
                                                    if (iEvt.getType() == InputEvent.Type.touchDown) {
                                                        checkString(match, scene);
                                                        searchInput.setText(match);
                                                        accept();
                                                        return true;
                                                    }
                                                }
                                                return false;
                                            });
                                            matchingLabels.add(m);
                                            m.setWidth(searchInput.getWidth());
                                            Cell<?> c = candidates.add(m).left().padBottom(pad10);
                                            c.row();
                                        });
                                        candidates.pack();
                                        searchInput.localToStageCoordinates(aux.set(0, 0));
                                        candidates.setPosition(aux.x, aux.y, Align.topLeft);
                                        stage.addActor(candidates);
                                    } else {
                                        removeCandidates();
                                    }
                                }
                            }
                        };
                        // Cancel others
                        cancelTasks();
                        tasks.add(task);
                        // Schedule with delay
                        Timer.schedule(task, 0.5f);

                        // Actually check and select
                        if (!checkString(name, scene)) {
                            if (name.matches("[0-9]+")) {
                                // Check with 'HIP '
                                if (checkString("hip " + name, scene)) {
                                    cancelTasks();
                                    removeCandidates();
                                }
                            } else if (name.matches("hip [0-9]+") || name.matches("HIP [0-9]+")) {
                                // Check without 'HIP '
                                if (checkString(name.substring(4), scene)) {
                                    cancelTasks();
                                    removeCandidates();
                                }
                            }
                        } else {
                            cancelTasks();
                            removeCandidates();
                        }
                        return false;

                    } else {
                        removeCandidates();
                        return false;
                    }
                }
            }
            return false;
        });

        // Info message
        infoMessage = new OwnLabel("", skin, "default-blue");

        content.add(searchInput).top().left().expand().row();
        infoCell = content.add();
        infoCell.top().left().padTop(pad10).expand().row();
        contentCell = content.add();
        contentCell.top().left().padTop(pad10).expandX().expandY();

        pack();
    }

    private void updateContent(Entity e) {
        if (e == null) {
            return;
        }
        contentCell.clearActor();

        var c = new Table(skin);

        view.setEntity(e);

        // Entity name
        var title = new OwnLabel(view.getName(), skin, "header");
        c.add(title).padBottom(pad10).row();
        c.add(new Separator(skin)).padBottom(pad10).fillX().expandX().row();

        // Components and attributes.
        var cc = new Table(skin);

        var comps = e.getComponents();
        for (var comp : comps) {
            cc.add(getComponentActor(comp)).expandX().left().padBottom(pad10).row();
        }
        var scroll = new OwnScrollPane(cc, skin, "minimalist-nobg");
        scroll.setWidth(width);
        scroll.setHeight(height);

        c.add(scroll).row();

        contentCell.setActor(c);

        pack();
    }

    @Override
    public GenericDialog show(Stage stage) {
        var gd = super.show(stage);

        var focus = (FocusView) GaiaSky.instance.getCameraManager().getFocus();
        if (focus != null) {
            updateContent(focus.getEntity());
        }
        return gd;
    }

    private Actor getComponentActor(Component component) {
        var c = new Table(skin);
        c.left();
        c.padLeft(pad20);

        var fields = component.getClass().getDeclaredFields();
        for (var field : fields) {
            var modifiers = field.getModifiers();
            var type = field.getType();

            var hg = new HorizontalGroup();
            hg.space(pad10);
            // Name
            var name = TextUtils.capString(field.getName(), 20);
            var nameLabel = new OwnLabel(name, skin);
            // Type
            var typeStr = TextUtils.capString(type.getSimpleName(), 13);
            var typeLabel = new OwnLabel(typeStr, skin, "default-blue");

            hg.addActor(nameLabel);
            hg.addActor(typeLabel);
            hg.setWidth(300f);
            c.add(hg).left().padBottom(pad10).padRight(pad18);
            try {
                var value = field.get(component);

                var fin = Modifier.isFinal(modifiers);
                var priv = Modifier.isPrivate(modifiers);
                var nil = value == null;
                var primStr = isPrimitiveOrStringType(type);
                var bool = isBoolean(type);

                var valueStr = toString(value, type);
                var valueCapped = TextUtils.capString(valueStr, 40);
                Actor fieldActor;
                if (fin || priv || nil || !primStr) {
                    // Just show value, not editable.
                    fieldActor = new OwnLabel(valueCapped, skin);
                    fieldActor.setColor(ColorUtils.oDarkGrayC);
                    fieldActor.addListener(new OwnTextTooltip(valueStr, skin));
                } else {
                    if (bool) {
                        fieldActor = new OwnCheckBox("", skin);
                        ((OwnCheckBox) fieldActor).setChecked((Boolean) value);
                        fieldActor.addListener((event) -> {
                            if (event instanceof ChangeListener.ChangeEvent) {
                                try {
                                    field.set(component, ((OwnCheckBox) fieldActor).isChecked());
                                } catch (IllegalAccessException e) {
                                    logger.error("Error setting value '" + ((OwnCheckBox) fieldActor).isChecked() + "' to " + name);
                                }
                            }
                            return false;
                        });
                    } else {
                        var hor = new HorizontalGroup();
                        hor.space(pad10);
                        var tf = new OwnTextField(valueCapped, skin);
                        tf.setWidth(450f);
                        tf.setErrorColor(ColorUtils.gRedC);
                        final Image applyImage = new Image(skin.getDrawable("iconic-check"));
                        var button = new OwnTextIconButton("", applyImage, skin);
                        button.addListener(new OwnTextTooltip(I18n.msg("gui.debug.object.setval"), skin));
                        button.addListener((event) -> {
                            if (event instanceof ChangeListener.ChangeEvent) {
                                try {
                                    var t = tf.getText();
                                    var val = parse(field.getType(), t);
                                    field.set(component, val);
                                    tf.setToRegularColor();
                                } catch (IllegalAccessException e) {
                                    logger.debug(I18n.msg("gui.debug.error.set", tf.getText(), name));
                                } catch (Exception e) {
                                    logger.debug(I18n.msg("gui.debug.error.parse", tf.getText(), name));
                                    tf.setToErrorColor();
                                }
                            }
                            return false;
                        });
                        hor.addActor(tf);
                        hor.addActor(button);
                        fieldActor = hor;
                    }
                }
                c.add(fieldActor).left().padBottom(pad10).expandX().row();
            } catch (IllegalAccessException e) {
                logger.debug(I18n.msg("gui.debug.error.access", name), e);
            }
        }

        return new CollapsiblePane(stage,
                                   component.getClass().getSimpleName(),
                                   c,
                                   width,
                                   skin,
                                   "header-s",
                                   "expand-collapse",
                                   null,
                                   false,
                                   null);
    }

    private static final Set<Class<?>> WRAPPER_TYPE_MAP;

    static {
        WRAPPER_TYPE_MAP = new HashSet<>(16);
        WRAPPER_TYPE_MAP.add(Integer.class);
        WRAPPER_TYPE_MAP.add(Byte.class);
        WRAPPER_TYPE_MAP.add(Character.class);
        WRAPPER_TYPE_MAP.add(Boolean.class);
        WRAPPER_TYPE_MAP.add(Double.class);
        WRAPPER_TYPE_MAP.add(Float.class);
        WRAPPER_TYPE_MAP.add(Long.class);
        WRAPPER_TYPE_MAP.add(Short.class);
        WRAPPER_TYPE_MAP.add(Void.class);
    }

    private static boolean isPrimitiveOrStringType(Class source) {
        return source.isPrimitive() || WRAPPER_TYPE_MAP.contains(source) || isString(source);
    }

    private static boolean isBoolean(Class source) {
        return source.equals(boolean.class) || source.equals(Boolean.class);
    }

    private static boolean isString(Class source) {
        return source.equals(String.class);
    }

    public static Object parse(Class clazz, String value) throws NumberFormatException, NullPointerException {
        if (Boolean.class == clazz || Boolean.TYPE == clazz) return Boolean.parseBoolean(value);
        if (Byte.class == clazz || Byte.TYPE == clazz) return Byte.parseByte(value);
        if (Short.class == clazz || Short.TYPE == clazz) return Short.parseShort(value);
        if (Integer.class == clazz || Integer.TYPE == clazz) return Integer.parseInt(value);
        if (Long.class == clazz || Long.TYPE == clazz) return Long.parseLong(value);
        if (Float.class == clazz || Float.TYPE == clazz) return Float.parseFloat(value);
        if (Double.class == clazz || Double.TYPE == clazz) return Double.parseDouble(value);
        return value;
    }

    private static String toString(Object value, Class type) {
        if (value == null) {
            return "EMPTY";
        } else if (Entity.class.isAssignableFrom(type)) {
            var entity = (Entity) value;
            var base = Mapper.base.get(entity);
            return base != null ? base.getName() : Entity.class.getSimpleName();
        } else if (type.isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < length; i++) {
                Object element = java.lang.reflect.Array.get(value, i);
                sb.append(toString(element, element.getClass()));
                if (i < length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb.toString();
        } else if (type.isAssignableFrom(Array.class)) {
            var arr = (Array) value;
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < arr.size; i++) {
                Object element = arr.get(i);
                sb.append(toString(element, element.getClass()));
                if (i < arr.size - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb.toString();
        } else if (type.isAnnotationPresent(FunctionalInterface.class)) {
            return "Functional interface";
        } else {
            return value.toString();
        }
    }

    /**
     * Consults the scene index with the given search string and selects the object and returns true if the
     * object exists and is focusable, and returns false otherwise.
     *
     * @param text  The text to look up.
     * @param scene The scene object.
     *
     * @return True if the object exists and is focusable, false otherwise.
     */
    private boolean checkString(String text, Scene scene) {
        try {
            if (scene.index().containsEntity(text)) {
                Entity entity = scene.getEntity(text);
                if (Mapper.focus.has(entity)) {
                    view.setEntity(entity);
                    view.getFocus(text);
                    filterView.setEntity(entity);

                    boolean timeOverflow = view.isCoordinatesTimeOverflow();
                    boolean canSelect = view.getSet() == null || view.getSet().canSelect(filterView);
                    boolean ctOn = GaiaSky.instance.isOn(view.getCt());
                    Optional<CatalogInfo> ci = GaiaSky.instance.getCatalogInfoFromEntity(entity);
                    boolean datasetVisible = ci.isEmpty() || ci.get().isVisible(true);
                    if (!timeOverflow && canSelect && ctOn && datasetVisible) {
                        GaiaSky.postRunnable(() -> {
                            updateContent(entity);
                        });
                        info(null);
                    } else if (timeOverflow) {
                        info(I18n.msg("gui.objects.search.timerange", text));
                    } else if (!canSelect) {
                        info(I18n.msg("gui.objects.search.filter", text));
                    } else if (!datasetVisible) {
                        info(I18n.msg("gui.objects.search.dataset.invisible", text, ci.get().name));
                    } else {
                        info(I18n.msg("gui.objects.search.invisible", text, Mapper.base.get(entity).ct.toString()));
                    }
                    return true;
                }
            } else {
                info(null);
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return false;
    }

    private void info(String info) {
        if (info == null) {
            infoMessage.setText("");
            info(false);
        } else {
            infoMessage.setText(info);
            info(true);
        }
    }

    private void info(boolean visible) {
        if (visible) {
            infoCell.setActor(infoMessage);
        } else {
            infoCell.setActor(null);
        }
        pack();
    }

    public void clearText() {
        removeCandidates();
        searchInput.setText("");
    }

    private void cancelTasks() {
        // Cancel all tasks
        if (!tasks.isEmpty()) {
            for (Timer.Task t : tasks) {
                t.cancel();
            }
        }
        tasks.clear();
    }

    private void removeCandidates() {
        if (candidates != null) {
            candidates.clear();
            candidates.remove();
        }
        cIdx = -1;
    }

    private void selectMatch() {
        for (int i = 0; i < matchingLabels.size; i++) {
            OwnLabel l = matchingLabels.get(i);
            if (i == cIdx) {
                l.setColor(ColorUtils.gYellowC);
                searchInput.setText(l.getText().toString());
            } else {
                l.setColor(ColorUtils.gWhiteC);
            }
        }
    }

    private void matchingNodes(String text, Scene scene) {
        matching.clear();
        matchingLabels.clear();
        scene.matchingFocusableNodes(text, matching, 10, null);
    }

    @Override
    protected boolean accept() {
        cancelTasks();
        removeCandidates();
        info(null);
        return true;
    }

    @Override
    protected void cancel() {
        // Unused
    }

    @Override
    public void dispose() {
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        // Only respond if the window is in the stage.
        if (me.hasParent())
            if (event == Event.FOCUS_CHANGED) {
                var focus = (FocusView) data[0];
                if (focus != null && focus.getEntity() != null)
                    GaiaSky.postRunnable(() -> {
                        updateContent(focus.getEntity());
                    });
            }
    }
}
