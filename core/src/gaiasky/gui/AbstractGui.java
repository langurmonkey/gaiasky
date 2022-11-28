/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

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
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;

/**
 * Provides general methods and attributes that all GUIs should have
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
     * Horizontal offset, for VR.
     */
    protected int hOffset;

    /**
     * Units per pixel, 1/uiScale.
     * This only works with a screen viewport.
     */
    protected float unitsPerPixel = 1;

    /** Lock for sync. **/
    protected Object lock;

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
    public void sethOffset(int hOffset) {
        this.hOffset = hOffset;
    }

    @Override
    public void setVr(boolean vr) {
        this.vr = vr;
    }

    @Override
    public boolean mustDraw() {
        return true;
    }

    public boolean updateUnitsPerPixel(float upp){
        this.unitsPerPixel = upp;
        if(stage.getViewport() instanceof ScreenViewport){
            ScreenViewport svp = (ScreenViewport) stage.getViewport();
            svp.setUnitsPerPixel(this.unitsPerPixel);
            svp.update(graphics.getWidth(), graphics.getHeight(), true);
            return true;
        }
        return false;
    }
}
