/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;

/**
 * Head-up display GUI which only displays information and has no options
 * window.
 */
public class HUDGui implements IGui {
    private Skin skin;
    /**
     * The user interface stage
     */
    protected Stage ui;

    protected FocusInfoInterface focusInterface;
    protected NotificationsInterface notificationsInterface;
    protected MessagesInterface messagesInterface;
    protected DebugInterface debugInterface;
    protected RunStateInterface inputInterface;

    protected Array<IGuiInterface> interfaces;

    /** Lock object for synchronization **/
    private Object lock;

    private Lwjgl3Graphics graphics;
    private float unitsPerPixel;

    public HUDGui(final Skin skin, final Lwjgl3Graphics graphics, final Float unitsPerPixel) {
        super();
        this.skin = skin;
        this.graphics = graphics;
        this.unitsPerPixel = unitsPerPixel;
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        ui = new Stage(vp, sb);
        lock = new Object();
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        interfaces = new Array<>();
        buildGui();
    }

    private void buildGui() {
        float pad = 8f;

        // FOCUS INFORMATION - BOTTOM RIGHT
        focusInterface = new FocusInfoInterface(skin);
        focusInterface.setFillParent(true);
        focusInterface.right().bottom();
        focusInterface.pad(0, 0, pad, pad);
        interfaces.add(focusInterface);

        // DEBUG INFO - TOP RIGHT
        debugInterface = new DebugInterface(skin, lock);
        debugInterface.setFillParent(true);
        debugInterface.right().top();
        debugInterface.pad(pad, 0, 0, pad);
        interfaces.add(debugInterface);

        // NOTIFICATIONS INTERFACE - BOTTOM LEFT
        notificationsInterface = new NotificationsInterface(skin, lock, true, false);
        notificationsInterface.setFillParent(true);
        notificationsInterface.left().bottom();
        notificationsInterface.pad(0, pad, pad, 0);
        interfaces.add(notificationsInterface);

        // MESSAGES INTERFACE - LOW CENTER
        messagesInterface = new MessagesInterface(skin, lock);
        messagesInterface.setFillParent(true);
        messagesInterface.left().bottom();
        messagesInterface.pad(0, 480f, 240f, 0);
        interfaces.add(messagesInterface);

        // INPUT STATE
        inputInterface = new RunStateInterface(skin);
        inputInterface.setFillParent(true);
        inputInterface.right().top();
        inputInterface.pad(80f, 0, 0, pad);
        interfaces.add(inputInterface);

        // Add to GUI
        rebuildGui();
    }

    public void rebuildGui() {
        if (ui != null) {
            ui.clear();

            if (debugInterface != null) {
                ui.addActor(debugInterface);
            }
            if (notificationsInterface != null) {
                ui.addActor(notificationsInterface);
            }
            if (messagesInterface != null) {
                ui.addActor(messagesInterface);
            }
            if (focusInterface != null) {
                ui.addActor(focusInterface);
            }
            if (inputInterface != null) {
                ui.addActor(inputInterface);
            }
        }
    }

    @Override
    public void dispose() {
        for (IGuiInterface iface : interfaces)
            iface.dispose();

        ui.dispose();
    }

    @Override
    public void update(double dt) {
        ui.act((float) dt);
        notificationsInterface.update();
    }

    @Override
    public void render(int rw, int rh) {
        synchronized (lock) {
            ui.draw();
        }
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
    public Stage getGuiStage() {
        return ui;
    }

    @Override
    public void setVisibilityToggles(ComponentType[] entities, ComponentTypes visible) {
    }

    @Override
    public Actor findActor(String name) {
        return ui.getRoot().findActor(name);
    }

    @Override
    public void sethOffset(int hOffset) {
    }

    @Override
    public void setVr(boolean vr) {
    }

    @Override
    public boolean mustDraw() {
        return true;
    }

    @Override
    public boolean updateUnitsPerPixel(float upp){
        this.unitsPerPixel = upp;
        if(ui.getViewport() instanceof ScreenViewport){
            ScreenViewport svp = (ScreenViewport) ui.getViewport();
            svp.setUnitsPerPixel(this.unitsPerPixel);
            svp.update(graphics.getWidth(), graphics.getHeight(), true);
            return true;
        }
        return false;
    }

}
