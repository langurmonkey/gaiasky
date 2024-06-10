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
import gaiasky.util.TextUtils;

public class OwnTextTooltip extends Tooltip<Label> {
    private final OwnLabel label;

    public OwnTextTooltip(String text, Skin skin, int breakSpaces) {
        this(text, TooltipManager.getInstance(), skin.get(TextTooltipStyle.class), breakSpaces);
    }

    public OwnTextTooltip(String text, Skin skin) {
        this(text, skin, 12);
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
        text = TextUtils.breakSpaces(text, breakSpaces);

        label = new OwnLabel(text, style.label);

        getContainer().setActor(label);

        setStyle(style);

        getContainer().pad(8f);
    }

    public void setStyle(TextTooltipStyle style) {
        if (style == null)
            throw new NullPointerException("style cannot be null");
        getContainer().getActor().setStyle(style.label);
        getContainer().setBackground(style.background);
        getContainer().maxWidth(style.wrapWidth);
    }

    public void setText(String newText) {
        if(label != null) {
            label.setText(newText);
        }
    }

}
