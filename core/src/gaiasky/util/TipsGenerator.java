package gaiasky.util;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import gaiasky.interafce.KeyBindings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.StdRandom;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextButton;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

public class TipsGenerator {
    private static final int MAX_KEYS = 100;

    private final Skin skin;
    private final List<String[]> tips;
    private final int[] sequence;
    private int currentIndex = 0;

    public TipsGenerator(Skin skin) {
        super();
        this.skin = skin;

        this.tips = new ArrayList<>();
        for (int j = 0; j < MAX_KEYS; j++) {
            try {
                String tipStr = I18n.txt("tip." + j);
                String[] lines = tipStr.split("\\r\\n|\\n|\\r");
                KeyBindings kb = KeyBindings.instance;
                for (String line : lines) {
                    String[] tokens = line.split("\\|");
                    int n = tokens.length;
                    String[] tip = new String[n];
                    for (int i = 0; i < n; i++) {
                        String style = "";
                        String text = tokens[i];
                        if (tokens[i].startsWith("%%")) {
                            int idx = text.indexOf(" ");
                            text = tokens[i].substring(idx + 1);
                            style = tokens[i].substring(0, idx) + " ";
                        }
                        if (text.isBlank()) {
                            tip[i] = null;
                        } else {
                            String keys = kb.getStringKeys(text);
                            if (keys == null)
                                tip[i] = style + text;
                            else
                                tip[i] = style + keys;
                        }
                    }
                    tips.add(tip);
                }
            } catch (MissingResourceException e) {
                // Skip
            }
        }
        if (this.tips.isEmpty()) {
            fallback();
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

    private void fallback() {
        this.tips.add(new String[] { "Use", "0", "to switch to free camera mode" });
        this.tips.add(new String[] { null, "double-click", "on any object to select it" });
        this.tips.add(new String[] { "Press", "u", "to open and close the controls window" });
        this.tips.add(new String[] { "Press", "f", "to search for any object by name" });
        this.tips.add(new String[] { "Press", "/", "to search for any object by name" });
        this.tips.add(new String[] { "Press", "ctrl", "k", "to enter panorama mode" });
    }

    public void newTip(WidgetGroup tip) {
        String[] l = tips.get(sequence[currentIndex]);
        addTip(tip, l);
        currentIndex = (currentIndex + 1) % tips.size();
    }

    private void addTip(WidgetGroup g, String[] tip) {
        float pad5 = 8f;
        float pad2 = 3.2f;
        g.clear();

        for (String part : tip) {
            boolean def = true;
            String style = "main-title-s";
            String text = part;
            if (part.startsWith("%%")) {
                int idx = text.indexOf(" ");
                text = part.substring(idx + 1);
                style = part.substring(2, idx);
                def = false;
            }
            if (skin.has(style, Label.LabelStyle.class)) {
                OwnLabel label = new OwnLabel(text, skin, style);
                if (def) {
                    label.setColor(0.5f, 0.5f, 0.5f, 1f);
                    g.addActor(label);
                }
                g.addActor(label);
            } else if (skin.has(style, TextButton.TextButtonStyle.class)) {
                String[] keys = text.split("\\+");
                int n = keys.length;
                for (int i = 0; i < n; i++) {
                    String t = keys[i];
                    if(I18n.hasKey("key." + keys[i])){
                       t = I18n.txt("key." + keys[i]);
                    }
                    OwnTextButton button = new OwnTextButton(t, skin, style);
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
}
