package gaiasky.gui;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.render.ComponentTypes;
import gaiasky.util.Logger;

public class VRGui<T extends IGui> implements IGui {

    private T right;
    private T left;

    public VRGui(Class<T> clazz, int hOffset, Skin skin, Graphics graphics, Float unitsPerPixel) {
        super();
        try {
            right = clazz.getDeclaredConstructor(Skin.class, Graphics.class, Float.class, Boolean.class).newInstance(skin, graphics, unitsPerPixel, true);
            right.setVr(true);
            right.setHoffset(-hOffset);
            left = clazz.getDeclaredConstructor(Skin.class, Graphics.class, Float.class, Boolean.class).newInstance(skin, graphics, unitsPerPixel, true);
            left.setVr(true);
            left.setHoffset(hOffset);
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }

    }

    @Override
    public void dispose() {
        right.dispose();
        left.dispose();
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        right.initialize(assetManager, sb);
        left.initialize(assetManager, sb);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        right.doneLoading(assetManager);
        left.doneLoading(assetManager);
    }

    @Override
    public void update(double dt) {
        right.update(dt);
        left.update(dt);
    }

    public T right() {
        return right;
    }

    public T left() {
        return left;
    }

    @Override
    public void render(int rw, int rh) {
    }

    @Override
    public void resize(int width, int height) {
        right.resize(width, height);
        left.resize(width, height);
    }

    @Override
    public void resizeImmediate(int width, int height) {
        right.resizeImmediate(width, height);
        left.resizeImmediate(width, height);
    }

    @Override
    public boolean cancelTouchFocus() {
        return false;
    }

    @Override
    public Stage getGuiStage() {
        return null;
    }

    @Override
    public void setVisibilityToggles(ComponentTypes.ComponentType[] entities, ComponentTypes visible) {
    }

    @Override
    public Actor findActor(String name) {
        return null;
    }

    @Override
    public void setHoffset(int hoffset) {
        right.setHoffset(hoffset);
        left.setHoffset(hoffset);
    }

    @Override
    public void setVr(boolean vr) {
        right.setVr(vr);
        left.setVr(vr);
    }

    public void updateViewportSize(int w, int h, boolean centerCamera){
        right.getGuiStage().getViewport().update(w, h, centerCamera);
        left.getGuiStage().getViewport().update(w, h, centerCamera);
    }

    @Override
    public boolean mustDraw() {
        return right.mustDraw() || left.mustDraw();
    }

    @Override
    public boolean updateUnitsPerPixel(float upp) {
        return right.updateUnitsPerPixel(upp) && left.updateUnitsPerPixel(upp);
    }
}