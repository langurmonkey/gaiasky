/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import gaiasky.gui.main.KeyBindings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.StdRandom;
import gaiasky.util.scene2d.OwnImage;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextButton;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

/**
 * Generates tip strings.
 */
public class TipsGenerator {
    private static final int MAX_KEYS = 100;

    private final Skin skin;
    private final List<TipPart[]> tips;
    private final int[] sequence;
    private int currentIndex = 0;

    public TipsGenerator(Skin skin) {
        super();
        this.skin = skin;

        this.tips = new ArrayList<>();
        for (int j = 0; j < MAX_KEYS; j++) {
            try {
                String tipStr = I18n.msg("tip." + j);
                String[] lines = tipStr.split("\\r\\n|\\n|\\r");
                KeyBindings kb = KeyBindings.instance;
                for (String line : lines) {
                    String[] tokens = line.split("\\|");
                    int n = tokens.length;
                    TipPart[] parts = new TipPart[n];
                    for (int i = 0; i < n; i++) {
                        var part = new TipPart();
                        var token = tokens[i].strip();
                        if (token.startsWith("$$")) {
                            // Drawable.
                            int idx = token.indexOf(" ");
                            if (idx < 0) {
                                idx = token.length();
                            }
                            part.drawable = token.substring(2, idx).strip();
                            parts[i] = part;
                        } else {
                            // Text with optional style.
                            if (token.startsWith("%%")) {
                                // Custom style.
                                int idx = token.indexOf(" ");
                                if (idx < 0) {
                                    idx = token.length();
                                }
                                part.style = token.substring(2, idx).strip();
                                part.text = token.substring(idx + 1).strip();
                            } else {
                                part.text = token.strip();
                            }
                            // Check keyboard mappings.
                            if (!part.text.isBlank()) {
                                String keys = kb.getStringKeys(part.text);
                                if (keys != null) {
                                    part.text = keys;
                                }
                            }
                        }
                        parts[i] = part;
                    }
                    tips.add(parts);
                }
            } catch (MissingResourceException e) {
                // Skip
            }
        }

        int n = this.tips.size();
        sequence = new int[n];

        for (int i = 0; i < n; i++) {
            sequence[i] = i;
        }
        // Shuffle
        for (int i = 0; i < n; i++) {
            int randomIndexToSwap = StdRandom.uniform(n);
            int temp = sequence[randomIndexToSwap];
            sequence[randomIndexToSwap] = sequence[i];
            sequence[i] = temp;
        }
    }

    public void newTip(WidgetGroup tip) {
        TipPart[] l = tips.get(sequence[currentIndex]);
        addTip(tip, l);
        currentIndex = (currentIndex + 1) % tips.size();
    }

    private void addTip(WidgetGroup g, TipPart[] tip) {
        float pad5 = 8f;
        float pad2 = 3.2f;
        g.clear();

        for (TipPart part : tip) {
            if (part == null) {
                continue;
            }
            if (part.drawable != null && !part.drawable.isBlank()) {
                var drawable = skin.getDrawable(part.drawable);
                if (drawable != null) {
                    OwnImage image = new OwnImage(drawable);
                    g.addActor(image);
                }
            } else if (skin.has(part.style, Label.LabelStyle.class)) {
                // Simple styled label.
                OwnLabel label = new OwnLabel(part.text, skin, part.style);
                g.addActor(label);
            } else if (skin.has(part.style, TextButton.TextButtonStyle.class)) {
                // Probably key mappings.
                String[] keys = part.text.split("\\+");
                int n = keys.length;
                for (int i = 0; i < n; i++) {
                    String t = keys[i];
                    if (I18n.hasMessage("key." + keys[i])) {
                        t = I18n.msg("key." + keys[i]);
                    }
                    OwnTextButton button = new OwnTextButton(t, skin, part.style);
                    button.pad(pad2, pad5, pad2, pad5);
                    g.addActor(button);
                    if (i < n - 1) {
                        OwnLabel plus = new OwnLabel("+", skin, "main-title-s");
                        plus.setColor(0.5f, 0.5f, 0.5f, 1f);
                        g.addActor(plus);
                    }
                }
            }
        }
    }

    private static class TipPart {
        public String style = "main-title-s";
        public String text;
        public String drawable;

        public TipPart() {
        }

        public TipPart(String text) {
            this.text = text;
        }

        public TipPart(String text, String style) {
            this.text = text;
            this.style = style;
        }

        @Override
        public String toString() {
            return drawable != null ? "D-" + drawable : "%%" + style + " " + text;
        }
    }
}
