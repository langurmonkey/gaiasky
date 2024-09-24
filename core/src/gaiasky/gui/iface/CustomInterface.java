/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.iface;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Keys;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.api.IGuiInterface;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.scene2d.OwnLabel;

import java.util.*;

public class CustomInterface implements IObserver, IGuiInterface {
    private final Object lock;
    private final Skin skin;
    private final Stage ui;
    Map<Integer, Widget> customElements;
    private List<Integer> sizes;

    public CustomInterface(Stage ui, Skin skin, Object lock) {
        this.skin = skin;
        this.ui = ui;
        customElements = new HashMap<>();

        initSizes(skin);

        this.lock = lock;
        EventManager.instance.subscribe(this, Event.ADD_CUSTOM_IMAGE, Event.ADD_CUSTOM_MESSAGE, Event.REMOVE_OBJECTS, Event.REMOVE_ALL_OBJECTS, Event.ADD_CUSTOM_TEXT);
    }

    private void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    private void initSizes(Skin skin) {
        sizes = new ArrayList<>();
        ObjectMap<String, LabelStyle> ls = skin.getAll(LabelStyle.class);
        Keys<String> keys = ls.keys();
        for (String key : keys) {
            if (key.startsWith("msg-")) {
                // Hit
                key = key.substring(4);
                sizes.add(Integer.parseInt(key));
            }
        }

        Collections.sort(sizes);

    }

    public void reAddObjects() {
        Set<Integer> keys = customElements.keySet();
        for (Integer key : keys) {
            ui.addActor(customElements.get(key));
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        synchronized (lock) {
            switch (event) {
                case ADD_CUSTOM_IMAGE -> {
                    Integer id = (Integer) data[0];
                    Texture tex = (Texture) data[1];
                    float x = MathUtilsDouble.lint((Float) data[2], 0, 1, 0, width);
                    float y = MathUtilsDouble.lint((Float) data[3], 0, 1, 0, height);
                    Image img;
                    boolean add = false;
                    if (customElements.containsKey(id)) {
                        if (customElements.get(id) instanceof Image) {
                            img = (Image) customElements.get(id);
                        } else {
                            removeObject(id);
                            img = new Image(tex);
                            add = true;
                        }
                    } else {
                        img = new Image(tex);
                        add = true;
                    }
                    img.setPosition(x, y);
                    if (data.length > 4) {
                        float r = (Float) data[4];
                        float g = (Float) data[5];
                        float b = (Float) data[6];
                        float a = (Float) data[7];
                        img.setColor(r, g, b, a);
                    }
                    if (add)
                        ui.addActor(img);
                    customElements.put(id, img);
                }
                case ADD_CUSTOM_MESSAGE -> {
                    Integer id = (Integer) data[0];
                    String msg = (String) data[1];
                    float x = MathUtilsDouble.lint((Float) data[2], 0, 1, 0, width);
                    float y = MathUtilsDouble.lint((Float) data[3], 0, 1, 0, height);
                    float r = (Float) data[4];
                    float g = (Float) data[5];
                    float b = (Float) data[6];
                    float a = (Float) data[7];
                    float s = (Float) data[8];
                    int size = (int) s;
                    int cSize = findClosestSize(size);
                    float scaleFactor = (float) size / (float) cSize;
                    String style = "msg-" + cSize;
                    OwnLabel customMsg;
                    boolean add = false;
                    if (customElements.containsKey(id)) {
                        if (customElements.get(id) instanceof OwnLabel) {
                            customMsg = (OwnLabel) customElements.get(id);
                            customMsg.setText(msg);
                            customMsg.setStyle(skin.get(style, LabelStyle.class));
                        } else {
                            removeObject(id);
                            customMsg = new OwnLabel(msg, skin, style);
                            add = true;
                        }
                    } else {
                        customMsg = new OwnLabel(msg, skin, style);
                        add = true;
                    }
                    customMsg.setColor(r, g, b, a);
                    customMsg.setPosition(x, y);
                    customMsg.setFontScale(scaleFactor);
                    if (add)
                        ui.addActor(customMsg);
                    customElements.put(id, customMsg);
                }
                case ADD_CUSTOM_TEXT -> {
                    Integer id = (Integer) data[0];
                    String msg = (String) data[1];
                    float x = MathUtilsDouble.lint((Float) data[2], 0, 1, 0, width);
                    float y = MathUtilsDouble.lint((Float) data[3], 0, 1, 0, height);
                    float w = MathUtilsDouble.clamp(MathUtilsDouble.lint((Float) data[4], 0, 1, 0, width), 0, width - x);
                    float h = MathUtilsDouble.clamp(MathUtilsDouble.lint((Float) data[5], 0, 1, 0, height), 0, height - y);
                    float r = (Float) data[6];
                    float g = (Float) data[7];
                    float b = (Float) data[8];
                    float a = (Float) data[9];
                    float s = (Float) data[10];
                    int size = (int) s;
                    int cSize = findClosestSize(size);
                    String style = "msg-" + cSize;
                    TextArea customText = null;
                    boolean add = false;
                    if (customElements.containsKey(id)) {
                        if (customElements.get(id) instanceof TextArea) {
                            customText = (TextArea) customElements.get(id);
                            customText.setText(msg);
                            customText.setStyle(skin.get(style, TextFieldStyle.class));
                        } else {
                            removeObject(id);
                            customText = new TextArea(msg, skin, style);
                            add = true;
                        }
                    } else {
                        customText = new TextArea(msg, skin, style);
                        add = true;
                    }
                    customText.setColor(r, g, b, a);
                    customText.setPosition(x, y);
                    customText.setDisabled(true);
                    if (w > 0)
                        customText.setWidth(w);
                    if (h > 0)
                        customText.setHeight(h);
                    if (add)
                        ui.addActor(customText);
                    customElements.put(id, customText);
                }
                case REMOVE_OBJECTS -> {
                    int[] ids = (int[]) data[0];
                    for (int identifier : ids)
                        removeObject(identifier);
                }
                case REMOVE_ALL_OBJECTS -> {
                    Set<Integer> keys = customElements.keySet();
                    Iterator<Integer> it = keys.iterator();
                    while (it.hasNext()) {
                        Integer key = it.next();
                        Widget toRemove = customElements.get(key);
                        if (toRemove != null) {
                            toRemove.remove();
                            it.remove();
                        }
                    }
                    customElements.clear();
                }
                default -> {
                }
            }
        }
    }

    private void removeObject(Integer id) {
        Widget toRemove = customElements.get(id);
        if (toRemove != null) {
            toRemove.remove();
            customElements.remove(id);
        }
    }

    private int findClosestSize(int size) {
        for (int i = 0; i < sizes.size(); i++) {
            int current = sizes.get(i);
            if (size == current || size < current) {
                return current;
            }
        }
        return sizes.get(sizes.size() - 1);
    }

    @Override
    public void dispose() {
        unsubscribe();
    }

    @Override
    public void update() {

    }

    @Override
    public boolean isOn() {
        return true;
    }

}
