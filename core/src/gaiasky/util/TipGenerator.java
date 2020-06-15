package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import gaiasky.interfce.KeyBindings;
import gaiasky.util.math.StdRandom;
import gaiasky.util.scene2d.OwnLabel;

import java.util.ArrayList;
import java.util.List;

public class TipGenerator {

    private Skin skin;
    private List<String[]> tips;
    private int[] sequence;
    private int currentIndex = 0;


    public TipGenerator(Skin skin) {
        super();
        this.skin = skin;

        this.tips = new ArrayList<>();
        try {
            FileHandle fh = Gdx.files.internal("text/tips");
            String tipStr = fh.readString();
            String[] lines = tipStr.split("\\r\\n|\\n|\\r");
            KeyBindings kb = KeyBindings.instance;
            for (String line : lines) {
                String[] tokens = line.split("\\|");
                int n = tokens.length;
                String[] tip = new String[n];
                if (n < 3) {
                    tip = new String[3];
                    n = 3;
                }
                tip[0] = tokens[0].isBlank() ? null : tokens[0];
                tip[n - 1] = tokens.length < 3 ? null : (tokens[n - 1].isBlank() ? null : tokens[n - 1]);
                for (int i = 1; i < n - 1; i++) {
                    if (tokens[i].isBlank()) {
                        tip[i] = null;
                    } else {
                        String keys = kb.getStringKeys(tokens[i]);
                        if (keys == null)
                            tip[i] = tokens[i];
                        else
                            tip[i] = keys;
                    }
                }
                tips.add(tip);
            }
        } catch (Exception e) {
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
        this.tips.add(new String[]{"Use", "0", "to switch to free camera mode"});
        this.tips.add(new String[]{null, "double-click", "on any object to select it"});
        this.tips.add(new String[]{"Press", "u", "to open and close the controls window"});
        this.tips.add(new String[]{"Press", "f", "to search for any object by name"});
        this.tips.add(new String[]{"Press", "/", "to search for any object by name"});
        this.tips.add(new String[]{"Press", "ctrl", "k", "to enter panorama mode"});
    }

    public void newTip(WidgetGroup tip) {
        String[] l = tips.get(sequence[currentIndex]);
        int n = l.length;
        String[] keys = (n == 3 && l[1] == null) ? null : new String[n - 2];
        if (keys != null) {
            for (int i = 1; i < n - 1; i++)
                keys[i - 1] = l[i];
        }
        addTip(tip, l[0], keys, l[n - 1]);
        currentIndex = (currentIndex + 1) % tips.size();
    }

    private void addTip(WidgetGroup g, String pre, String[] keys, String post) {
        float pad5 = GlobalConf.UI_SCALE_FACTOR * 5f;
        float pad2 = GlobalConf.UI_SCALE_FACTOR * 2f;
        g.clear();
        if (pre != null) {
            OwnLabel prel = new OwnLabel(pre, skin, "main-title-s");
            prel.setColor(0.5f, 0.5f, 0.5f, 1f);
            g.addActor(prel);
        }
        if (keys != null)
            for (int i = 0; i < keys.length; i++) {
                TextButton key = new TextButton(keys[i], skin, "key-big");
                key.pad(pad2, pad5, pad2, pad5);
                g.addActor(key);
                if (i < keys.length - 1) {
                    OwnLabel plus = new OwnLabel("+", skin, "main-title-s");
                    g.addActor(plus);
                }
            }
        if (post != null) {
            OwnLabel postL = new OwnLabel(post, skin, "main-title-s");
            postL.setColor(0.5f, 0.5f, 0.5f, 1f);
            g.addActor(postL);
        }
    }
}
