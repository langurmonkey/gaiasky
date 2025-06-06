/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.api.IGui;
import gaiasky.gui.api.IGuiInterface;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;

/**
 * Base implementation for top-level GUIs. Contains the essentials used by (almost) all.
 */
public abstract class AbstractGui implements IObserver, IGui {

    /**
     * Graphics instance.
     */
    protected Graphics graphics;
    /**
     * The user interface stage.
     */
    protected Stage stage;
    /**
     * The skin to use.
     */
    protected Skin skin;
    /**
     * The GUI interfaces, if any.
     */
    protected Array<IGuiInterface> interfaces;

    /**
     * The name of this GUI.
     */
    protected String name;

    /**
     * Whether we're in VR mode.
     */
    protected boolean vr;

    /**
     * Units per pixel, 1/uiScale.
     * This only works with a screen viewport.
     */
    protected float unitsPerPixel = 1;

    /** Lock for sync. **/
    protected final Object lock;

    protected int backBufferWidth = -1, backBufferHeight = -1;

    public AbstractGui(Graphics graphics, Float unitsPerPixel) {
        this.graphics = graphics;
        this.unitsPerPixel = unitsPerPixel;
        lock = new Object();
        name = this.getClass().getSimpleName();
    }

    @Override
    public void update(double dt) {
        stage.act((float) dt);
    }

    @Override
    public void render(int rw, int rh) {
        synchronized (lock) {
            stage.draw();
        }
    }

    @Override
    public Stage getGuiStage() {
        return stage;
    }

    public String getName() {
        return name;
    }

    @Override
    public void dispose() {
        if (interfaces != null)
            for (IGuiInterface iface : interfaces)
                iface.dispose();

        if (stage != null)
            stage.dispose();
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void resize(final int width, final int height) {
        GaiaSky.postRunnable(() -> resizeImmediate(width, height));
    }

    @Override
    public void resizeImmediate(final int width, final int height) {
        stage.getViewport().update(width, height, true);
        rebuildGui();
    }

    /**
     * Adds the already created GUI objects to the stage.
     */
    protected abstract void rebuildGui();

    @Override
    public boolean cancelTouchFocus() {
        if (stage.getKeyboardFocus() != null || stage.getScrollFocus() != null) {
            stage.setScrollFocus(null);
            stage.setKeyboardFocus(null);
            return true;
        }
        return false;
    }

    @Override
    public Actor findActor(String name) {
        return stage.getRoot().findActor(name);
    }

    @Override
    public void setVisibilityToggles(ComponentType[] entities, ComponentTypes visible) {
        // Empty by default
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        // Empty by default
    }

    @Override
    public void setVR(boolean vr) {
        this.vr = vr;
    }

    @Override
    public boolean isVR() {
        return vr;
    }

    @Override
    public boolean mustDraw() {
        return true;
    }

    public boolean updateUnitsPerPixel(float upp) {
        this.unitsPerPixel = upp;
        if (stage.getViewport() instanceof ScreenViewport svp) {
            svp.setUnitsPerPixel(this.unitsPerPixel);
            svp.update(graphics.getWidth(), graphics.getHeight(), true);
            return true;
        }
        return false;
    }

    public float getUnitsPerPixel() {
        return unitsPerPixel;
    }

    @Override
    public void setBackBufferSize(int width, int height) {
        this.backBufferHeight = height;
        this.backBufferWidth = width;
    }

    public int getBackBufferWidth() {
        return backBufferWidth > 0 ? backBufferWidth : 1920;
    }

    public int getBackBufferHeight() {
        return backBufferHeight > 0 ? backBufferHeight : 1080;
    }
}
