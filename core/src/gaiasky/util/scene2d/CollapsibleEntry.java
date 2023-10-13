/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class CollapsibleEntry extends OwnButton {
    private Cell<?> contentCell, actorCell;
    private OwnImageButton collapse;
    private final Actor content;
    private Runnable expandRunnable, collapseRunnable;

    public CollapsibleEntry(Actor title, Actor content, Skin skin, String styleName, boolean changeCursor) {
        super(skin, styleName, changeCursor);
        this.content = content;
        createActor(title, skin);
    }

    public CollapsibleEntry(Actor title, Actor content, Skin skin) {
        this(title, content, skin, "dataset-nofocus", false);
    }

    public void setWidth(float width) {
        contentCell.width(width);
        actorCell.width(width - 40f);
        super.setWidth(width);
    }

    public void collapse() {
        contentCell.setActor(null);
        me.pack();
        if (collapseRunnable != null)
            collapseRunnable.run();
    }

    public void expand() {
        contentCell.setActor(content);
        me.pack();
        if (expandRunnable != null)
            expandRunnable.run();
    }

    private void createActor(Actor title, Skin skin) {
        // Unchecked = expand
        // Checked   = collapse
        pad(5f);
        collapse = new OwnImageButton(skin, "expand-collapse");
        collapse.setCheckedNoFire(false); // start collapsed (show expand icon)
        add(collapse).top().left().padBottom(10f).padRight(15f);
        actorCell = add(title).top().left().padBottom(10f);
        actorCell.row();
        contentCell = add().top().left().colspan(2).expandX();

        collapse.addListener(event -> {
            if (event instanceof ChangeEvent) {
                boolean checked = collapse.isChecked();
                if (!checked) {
                    collapse();
                } else {
                    expand();
                }
                return true;
            }
            return false;
        });
        title.addListener(new ClickListener() {
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (event.getButton() == Buttons.LEFT) {
                    boolean checked = collapse.isChecked();
                    if (checked) {
                        collapse();
                    } else {
                        expand();
                    }
                    collapse.setCheckedNoFire(!checked);
                }
                // Bubble up
                super.touchUp(event, x, y, pointer, button);
            }
        });
        title.addListener(event -> {
            if (event instanceof InputEvent) {
                Type type = ((InputEvent) event).getType();
                if (type == Type.enter) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Hand);
                } else if (type == Type.exit) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                }
                return true;
            }
            return false;
        });
    }

    public void setExpandRunnable(Runnable r) {
        expandRunnable = r;
    }

    public void setCollapseRunnable(Runnable r) {
        collapseRunnable = r;
    }
}
