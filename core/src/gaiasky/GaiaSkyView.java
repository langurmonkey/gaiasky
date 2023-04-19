/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.RenderUtils;
import gaiasky.util.scene2d.OwnLabel;

public class GaiaSkyView implements ApplicationListener, IObserver {

    private final Skin skin;
    private final ShaderProgram spriteShader;
    private final Vector2 lastTexSize;
    /* Window */
    public Lwjgl3Window window;
    /* Graphics */
    public Lwjgl3Graphics graphics;
    /* Input */
    public Lwjgl3Input input;
    private Stage ui;
    private SpriteBatch sb;
    private boolean initGui = false;
    private boolean initializing = true;

    public GaiaSkyView(final Skin skin, final ShaderProgram spriteShader) {
        super();
        this.skin = skin;
        this.spriteShader = spriteShader;
        this.lastTexSize = new Vector2(-1, -1);
        EventManager.instance.subscribe(this, Event.INITIALIZED_INFO);
    }

    public void setWindow(Lwjgl3Window window) {
        this.window = window;
    }

    @Override
    public void create() {
        sb = new SpriteBatch(100, this.spriteShader);
    }

    private void createInitGui() {
        if (!initGui) {
            Viewport vp = new ScreenViewport();
            Stage ui = new Stage(vp, sb);
            vp.update(graphics.getWidth(), graphics.getHeight(), true);

            OwnLabel l = new OwnLabel("The external view will appear here as soon as Gaia Sky finishes loading", skin, "default");
            Container<OwnLabel> c = new Container<>(l);
            c.center();
            c.setFillParent(true);

            ui.addActor(c);

            this.ui = ui;

            initGui = true;
        }

    }

    private void removeGui() {
        ui.clear();
        initializing = false;
    }

    @Override
    public void resize(int width, int height) {
        if (ui != null) {
            ui.getViewport().update(width, height, true);
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        updateGlobals();
        createInitGui();

        if (initializing) {
            ui.act(graphics.getDeltaTime());
            ui.getViewport().apply();
            ui.draw();
        } else {
            FrameBuffer renderBuffer = GaiaSky.instance.getBackRenderBuffer();
            if (renderBuffer != null) {
                RenderUtils.renderKeepAspect(renderBuffer, sb, graphics, lastTexSize);
            }
        }
    }

    private void updateGlobals() {
        if (graphics == null)
            graphics = (Lwjgl3Graphics) Gdx.graphics;
        if (input == null)
            input = (Lwjgl3Input) Gdx.input;

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.INITIALIZED_INFO) {// Initialize full gui
            postRunnable(this::removeGui);
        }

    }

    public void postRunnable(Runnable r) {
        if (window != null)
            window.postRunnable(r);
        else
            Gdx.app.postRunnable(r);
    }
}
