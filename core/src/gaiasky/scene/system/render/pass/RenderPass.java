/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.pass;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.system.render.SceneRenderer;

import java.util.function.Supplier;

/**
 * Abstract render pass.
 */
public abstract class RenderPass implements Disposable {
    protected final SceneRenderer sceneRenderer;
    protected boolean enabled;
    protected Supplier<Boolean> condition;

    public RenderPass(SceneRenderer renderer, boolean enabled, Supplier<Boolean> condition) {
        this.sceneRenderer = renderer;
        this.enabled = enabled;
        this.condition = condition;
    }

    public RenderPass(SceneRenderer renderer, Supplier<Boolean> condition) {
        this(renderer, true, condition);
    }

    public RenderPass(SceneRenderer renderer) {
        this(renderer, true, null);
    }

    public void setCondition(Supplier<Boolean> condition) {
        this.condition = condition;
    }

    public void removeCondition() {
        this.condition = null;
    }

    public void enable() {
        setEnabled(true);
    }

    public void disable() {
        setEnabled(false);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected abstract void initializeRenderPass();

    public void initialize() {
        if (enabled && (condition != null && condition.get())) {
            initializeRenderPass();
        }
    }

    /**
     * Override if needed. Fetches resources from the asset manager after loading.
     *
     * @param manager The asset manager.
     */
    public void doneLoading(AssetManager manager) {
    }

    protected abstract void renderPass(ICamera camera, Object... params);

    public final void render(ICamera camera, Object... params) {
        if (enabled && (condition != null && condition.get())) {
            renderPass(camera, params);
        }
    }

    public abstract void dispose();

}
