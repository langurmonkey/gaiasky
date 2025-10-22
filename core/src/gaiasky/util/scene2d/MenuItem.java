/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import gaiasky.gui.main.GSKeys;

public class MenuItem extends Button {
    private static final Vector2 tmpVector = new Vector2();

    //MenuItem is modified version of TextButton
    /** Menu that this item belongs to */
    ContextMenu containerMenu;
    private MenuItemStyle style;
    private Image image;
    private boolean generateDisabledImage = true;
    private Label label;
    private Color shortcutLabelColor;
    private Label shortcutLabel;
    private Image subMenuImage;
    private Cell<Image> subMenuIconCell;
    private ContextMenu subMenu;

    public MenuItem(String text, Skin skin) {
        this(text, skin, (Image) null, skin.get(MenuItemStyle.class));
    }

    public MenuItem(String text, Skin skin, String styleName) {
        this(text, skin, (Image) null, skin.get(styleName, MenuItemStyle.class));
    }

    public MenuItem(String text, Skin skin, ChangeListener changeListener) {
        this(text, skin, (Image) null, skin.get(MenuItemStyle.class));
        addListener(changeListener);
    }

    public MenuItem(String text, Skin skin, Drawable drawable) {
        this(text, skin, drawable, skin.get(MenuItemStyle.class));
    }

    public MenuItem(String text, Skin skin, Drawable drawable, ChangeListener changeListener) {
        this(text, skin, drawable, skin.get(MenuItemStyle.class));
        addListener(changeListener);
    }

    public MenuItem(String text, Skin skin, Drawable drawable, String styleName) {
        this(text, skin, drawable, skin.get(styleName, MenuItemStyle.class));
    }

    public MenuItem(String text, Skin skin, Image image) {
        this(text, skin, image, skin.get(MenuItemStyle.class));
    }

    public MenuItem(String text, Skin skin, Image image, ChangeListener changeListener) {
        this(text, skin, image, skin.get(MenuItemStyle.class));
        addListener(changeListener);
    }

    public MenuItem(String text, Skin skin, Image image, String styleName) {
        this(text, skin, image, skin.get(styleName, MenuItemStyle.class));
    }

    // Base constructors

    public MenuItem(String text, Skin skin, Image image, MenuItemStyle style) {
        super(style);
        init(text, skin, image, style);
    }

    public MenuItem(String text, Skin skin, Drawable drawable, MenuItemStyle style) {
        super(style);
        init(text, skin, new Image(drawable), style);
    }

    private void init(String text, Skin skin, Image image, MenuItemStyle style) {
        this.style = style;
        this.image = image;

        setSkin(skin);

        defaults().space(3);

        if (image != null)
            image.setScaling(Scaling.fit);
        add(image).size(22).padRight(5);

        label = new Label(text, new LabelStyle(style.font, style.fontColor));
        label.setAlignment(Align.left);
        add(label).expand().fill();

        add(shortcutLabel = new Label("", skin, "menuitem-shortcut")).padLeft(10).right();
        shortcutLabelColor = shortcutLabel.getStyle().fontColor;

        subMenuIconCell = add(subMenuImage = new Image(style.subMenu)).padLeft(3).padRight(3).size(style.subMenu.getMinWidth(), style.subMenu.getMinHeight());
        subMenuIconCell.setActor(null);

        addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (subMenu != null) { //makes submenu item not clickable
                    event.stop();
                }
            }
        });

        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (subMenu != null) { //removes selection of child submenu if mouse moved to parent submenu
                    subMenu.setActiveItem(null, false);
                    subMenu.setActiveSubMenu(null);
                }

                if (subMenu == null || isDisabled()) { //hides last visible submenu (if any)
                    hideSubMenu();
                } else {
                    showSubMenu();
                }
            }
        });
    }

    public ContextMenu getSubMenu() {
        return subMenu;
    }

    public void setSubMenu(final ContextMenu subMenu) {
        this.subMenu = subMenu;

        if (subMenu == null) {
            subMenuIconCell.setActor(null);
        } else {
            subMenuIconCell.setActor(subMenuImage);
        }
    }

    void packContainerMenu() {
        if (containerMenu != null)
            containerMenu.pack();
    }

    @Override
    protected void setParent(Group parent) {
        super.setParent(parent);
        if (parent instanceof ContextMenu)
            containerMenu = (ContextMenu) parent;
        else
            containerMenu = null;
    }

    void hideSubMenu() {
        if (containerMenu != null) {
            containerMenu.setActiveSubMenu(null);
        }
    }

    void showSubMenu() {
        Stage stage = getStage();
        Vector2 pos = localToStageCoordinates(tmpVector.setZero());

        float availableSpaceLeft = pos.x;
        float availableSpaceRight = stage.getWidth() - (pos.x + getWidth());
        boolean canFitOnTheRight = pos.x + getWidth() + subMenu.getWidth() <= stage.getWidth();
        float subMenuX;
        if (canFitOnTheRight || availableSpaceRight > availableSpaceLeft) {
            subMenuX = pos.x + getWidth() - 1;
        } else {
            subMenuX = pos.x - subMenu.getWidth() + 1;
        }

        subMenu.setPosition(subMenuX, pos.y - subMenu.getHeight() + getHeight());

        if (subMenu.getY() < 0) {
            subMenu.setY(subMenu.getY() + subMenu.getHeight() - getHeight());
        }

        stage.addActor(subMenu);
        containerMenu.setActiveSubMenu(subMenu);
    }

    void fireChangeEvent() {
        ChangeListener.ChangeEvent changeEvent = POOLS.obtain(ChangeListener.ChangeEvent.class);
        fire(changeEvent);
        POOLS.free(changeEvent);
    }

    @Override
    public MenuItemStyle getStyle() {
        return style;
    }

    @Override
    public void setStyle(ButtonStyle style) {
        if (!(style instanceof MenuItemStyle textButtonStyle))
            throw new IllegalArgumentException("style must be a MenuItemStyle.");
        super.setStyle(style);
        this.style = textButtonStyle;
        if (label != null) {
            LabelStyle labelStyle = label.getStyle();
            labelStyle.font = textButtonStyle.font;
            labelStyle.fontColor = textButtonStyle.fontColor;
            label.setStyle(labelStyle);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color fontColor;
        if (isDisabled() && style.disabledFontColor != null)
            fontColor = style.disabledFontColor;
        else if (isPressed() && style.downFontColor != null)
            fontColor = style.downFontColor;
        else if (isChecked() && style.checkedFontColor != null)
            fontColor = (isOver() && style.checkedOverFontColor != null) ? style.checkedOverFontColor : style.checkedFontColor;
        else if (isOver() && style.overFontColor != null)
            fontColor = style.overFontColor;
        else
            fontColor = style.fontColor;
        if (fontColor != null)
            label.getStyle().fontColor = fontColor;

        if (isDisabled())
            shortcutLabel.getStyle().fontColor = style.disabledFontColor;
        else
            shortcutLabel.getStyle().fontColor = shortcutLabelColor;

        if (image != null && generateDisabledImage) {
            if (isDisabled())
                image.setColor(Color.GRAY);
            else
                image.setColor(Color.WHITE);
        }

        super.draw(batch, parentAlpha);
    }

    @Override
    public boolean isOver() {
        if (containerMenu == null || containerMenu.getActiveItem() == null) {
            return super.isOver();
        } else {
            return containerMenu.getActiveItem() == this;
        }
    }

    public boolean isGenerateDisabledImage() {
        return generateDisabledImage;
    }

    /**
     * Changes generateDisabledImage property, when true that function is
     * enabled. When it is enabled and this MenuItem is disabled then image
     * color will be changed to gray meaning that it is disabled, by default it
     * is enabled.
     */
    public void setGenerateDisabledImage(boolean generateDisabledImage) {
        this.generateDisabledImage = generateDisabledImage;
    }

    public CharSequence getShortcut() {
        return shortcutLabel.getText();
    }

    /**
     * Set shortcuts text displayed in this menu item. This DOES NOT set actual
     * hot key for this menu item, it only makes shortcut text visible in item.
     *
     * @param keycode from {@link Keys}.
     */
    public MenuItem setShortcut(int keycode) {
        return setShortcut(GSKeys.toString(keycode));
    }

    /**
     * Set shortcuts text displayed in this menu item. This DOES NOT set actual
     * hot key for this menu item, it only makes shortcut text visible in item.
     *
     * @param text text that will be displayed
     *
     * @return this object for the purpose of chaining methods
     */
    public MenuItem setShortcut(String text) {
        shortcutLabel.setText(text);
        packContainerMenu();
        return this;
    }

    /**
     * Creates platform dependent shortcut text. Converts int keycodes to String
     * text. Eg. Keys.CONTROL_LEFT, Keys.SHIFT_LEFT, Keys.F5 will be converted
     * to Ctrl+Shift+F5 on Windows and Linux, and to ⌘⇧F5 on Mac.
     * <p>
     * CONTROL_LEFT and CONTROL_RIGHT are mapped to Ctrl. The same goes for Alt
     * (ALT_LEFT, ALT_RIGHT) and Shift (SHIFT_LEFT, SHIFT_RIGHT).
     * <p>
     * This DOES NOT set actual hot key for this menu item, it only makes
     * shortcut text visible in item.
     *
     * @param keycodes keycodes from {@link Keys} that are used to create shortcut
     *                 text
     *
     * @return this object for the purpose of chaining methods
     */
    public MenuItem setShortcut(int... keycodes) {
        shortcutLabel.setText(OsUtils.getShortcutFor(keycodes));
        packContainerMenu();
        return this;
    }

    @Override
    protected void setStage(Stage stage) {
        super.setStage(stage);
        label.invalidate(); //fixes issue with disappearing menu item after holding right mouse button and dragging down while opening menu
    }

    public Image getImage() {
        return image;
    }

    public Cell<?> getImageCell() {
        return getCell(image);
    }

    public Label getLabel() {
        return label;
    }

    public Cell<?> getLabelCell() {
        return getCell(label);
    }

    public CharSequence getText() {
        return label.getText();
    }

    public void setText(CharSequence text) {
        label.setText(text);
    }

    public Cell<Image> getSubMenuIconCell() {
        return subMenuIconCell;
    }

    public Cell<Label> getShortcutCell() {
        return getCell(shortcutLabel);
    }

    static public class MenuItemStyle extends TextButtonStyle {
        public Drawable subMenu;

        public MenuItemStyle() {
        }

        public MenuItemStyle(Drawable subMenu) {
            this.subMenu = subMenu;
        }

        public MenuItemStyle(MenuItemStyle style) {
            super(style);
            this.subMenu = style.subMenu;
        }
    }

}