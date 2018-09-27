package gaia.cu9.ari.gaiaorbit.util.scene2d;

/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip.TextTooltipStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.scenes.scene2d.ui.Value;

/** A tooltip that shows a label.
 * @author Nathan Sweet */
public class OwnTextTooltip extends Tooltip<Label> {
    private Label label;

    public OwnTextTooltip(String text, Skin skin, int breakSpaces) {
        this(text, TooltipManager.getInstance(), skin.get(TextTooltipStyle.class), breakSpaces);
    }

    public OwnTextTooltip(String text, Skin skin) {
        this(text, skin, -1);
    }

    public OwnTextTooltip(String text, Skin skin, String styleName) {
        this(text, TooltipManager.getInstance(), skin.get(styleName, TextTooltipStyle.class), -1);
    }

    public OwnTextTooltip(String text, TextTooltipStyle style) {
        this(text, TooltipManager.getInstance(), style, -1);
    }

    public OwnTextTooltip(String text, TooltipManager manager, Skin skin) {
        this(text, manager, skin.get(TextTooltipStyle.class), -1);
    }

    public OwnTextTooltip(String text, TooltipManager manager, Skin skin, String styleName) {
        this(text, manager, skin.get(styleName, TextTooltipStyle.class), -1);
    }

    public OwnTextTooltip(String text, final TooltipManager manager, TextTooltipStyle style, int breakSpaces) {
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

        label = new Label(text, style.label);
        label.setWrap(true);

        getContainer().setActor(label);
        getContainer().width(new Value() {
            public float get(Actor context) {
                return Math.min(manager.maxWidth, getContainer().getActor().getGlyphLayout().width);
            }
        });

        setStyle(style);
    }

    public void setStyle(TextTooltipStyle style) {
        if (style == null)
            throw new NullPointerException("style cannot be null");
        if (!(style instanceof TextTooltipStyle))
            throw new IllegalArgumentException("style must be a TextTooltipStyle.");
        getContainer().getActor().setStyle(style.label);
        getContainer().setBackground(style.background);
        getContainer().maxWidth(style.wrapWidth);
    }

}
