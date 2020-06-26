/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scenegraph.ISceneGraph;

/**
 * Provides general methods and attributes that all GUIs should have
 *
 * @author tsagrista
 */
public abstract class AbstractGui implements IObserver, IGui {

    /**
     * The user interface stage
     */
    protected Stage ui;
    /**
     * The skin to use
     */
    protected Skin skin;
    /**
     * The GUI interfaces, if any
     */
    protected Array<IGuiInterface> interfaces;

    /**
     * The name of this GUI
     */
    protected String name;

    /**
     * Whether we're in VR mode
     */
    protected boolean vr;

    /**
     * Horizontal offset, for VR
     */
    protected int hoffset;

    /** Lock for sync **/
    protected Object lock;

    public AbstractGui() {
        lock = new Object();
        name = this.getClass().getSimpleName();
    }

    @Override
    public void update(double dt) {
        ui.act((float) dt);
    }

    @Override
    public void render(int rw, int rh) {
        synchronized (lock) {
            ui.draw();
        }
    }

    @Override
    public Stage getGuiStage() {
        return ui;
    }

    public String getName() {
        return name;
    }

    @Override
    public void dispose() {
        if (interfaces != null)
            for (IGuiInterface iface : interfaces)
                iface.dispose();

        if (ui != null)
            ui.dispose();
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void resize(final int width, final int height) {
        GaiaSky.postRunnable(() -> resizeImmediate(width, height));
    }

    @Override
    public void resizeImmediate(final int width, final int height) {
        ui.getViewport().update(width, height, true);
        rebuildGui();
    }

    /**
     * Adds the already created GUI objects to the stage.
     */
    protected abstract void rebuildGui();

    @Override
    public boolean cancelTouchFocus() {
        if (ui.getKeyboardFocus() != null || ui.getScrollFocus() != null) {
            ui.setScrollFocus(null);
            ui.setKeyboardFocus(null);
            return true;
        }
        return false;
    }

    @Override
    public Actor findActor(String name) {
        return ui.getRoot().findActor(name);
    }

    @Override
    public void setVisibilityToggles(ComponentType[] entities, ComponentTypes visible) {
        // Empty by default
    }

    @Override
    public void setSceneGraph(ISceneGraph sg) {
        // Empty by default
    }

    @Override
    public void notify(final Events event, final Object... data) {
        // Empty by default
    }

    @Override
    public void setHoffset(int hoffset) {
        this.hoffset = hoffset;
    }

    @Override
    public void setVr(boolean vr) {
        this.vr = vr;
    }

    @Override
    public boolean mustDraw() {
        return true;
    }
}
