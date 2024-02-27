/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip.TextTooltipStyle;

public class OwnTextHotkeyTooltip extends Tooltip<Table> {
    private final OwnLabel label;
    private OwnLabel labelHotkey;

    public OwnTextHotkeyTooltip(String text, String hotkey, Skin skin, int breakSpaces) {
        this(text, hotkey, skin, TooltipManager.getInstance(), skin.get(TextTooltipStyle.class), breakSpaces);
    }

    public OwnTextHotkeyTooltip(String text, String hotkey, Skin skin) {
        this(text, hotkey, skin, -1);
    }

    public OwnTextHotkeyTooltip(String text, String hotkey, Skin skin, final TooltipManager manager, TextTooltipStyle style, int breakSpaces) {
        super(null, manager);

        // Warp text if breakSpaces <= 0
        if (breakSpaces > 0) {
            StringBuilder sb = new StringBuilder(text);
            int spaces = 0;
            for (int i = 0; i < sb.length(); i++) {
                char c = sb.charAt(i);
                if (c == ' ') {
                    spaces++;
                }
                if (spaces == breakSpaces) {
                    sb.setCharAt(i, '\n');
                    spaces = 0;
                }
            }
            text = sb.toString();
        }

        Table table = new Table(skin);

        label = new OwnLabel(text, skin);

        if (hotkey != null)
            labelHotkey = new OwnLabel("[" + hotkey + "]", skin, "hotkey");

        table.add(label).padRight(labelHotkey != null ? 10f : 0f);
        if (labelHotkey != null) {
            table.add(labelHotkey);
        }

        table.pack();

        getContainer().setActor(table);
        getContainer().pack();
        getContainer().width(new Value() {
            public float get(Actor context) {
                return Math.min(manager.maxWidth, label.getGlyphLayout().width + 10f + (labelHotkey != null ? labelHotkey.getGlyphLayout().width + 60f : 0f));
            }
        });

        setStyle(style);

        getContainer().pad(8f);
    }

    public void setStyle(TextTooltipStyle style) {
        if (style == null)
            throw new NullPointerException("style cannot be null");
        label.setStyle(style.label);
        getContainer().setBackground(style.background);
        getContainer().maxWidth(style.wrapWidth);
    }

}
