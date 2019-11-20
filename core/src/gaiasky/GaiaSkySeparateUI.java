/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interfce.*;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.SceneGraphRenderer;
import gaiasky.util.GlobalResources;
import gaiasky.util.scene2d.OwnLabel;

public class GaiaSkySeparateUI implements ApplicationListener, IObserver {

    /** Window **/
    public static Lwjgl3Window window;
    /** Graphics **/
    public static Lwjgl3Graphics graphics;
    /** Input **/
    public static Lwjgl3Input input;

    private FullGui gui;
    private Skin skin;
    private Stage ui;

    private SpriteBatch sb;
    private boolean initGui = false;
    private boolean initializing = true;

    private InputMultiplexer im;

    public GaiaSkySeparateUI() {
        super();
        this.skin = GlobalResources.skin;
        EventManager.instance.subscribe(this, Events.INITIALIZED_INFO, Events.SHOW_QUIT_ACTION, Events.SHOW_ABOUT_ACTION, Events.SHOW_PREFERENCES_ACTION, Events.SHOW_SEARCH_ACTION);
    }

    public void setWindow(Lwjgl3Window window) {
        this.window = window;
    }

    @Override
    public void create() {
        sb = new SpriteBatch(100, GlobalResources.spriteShader);
    }

    private void createInitGui() {
        if (!initGui) {
            Viewport vp = new ScreenViewport();
            Stage ui = new Stage(vp, sb);
            vp.update(graphics.getWidth(), graphics.getHeight(), true);

            OwnLabel l = new OwnLabel("UI controls will appear here as soon as Gaia Sky finishes loading", skin, "ui-15");
            Container<OwnLabel> c = new Container<>(l);
            c.center();
            c.setFillParent(true);

            ui.addActor(c);

            this.ui = ui;

            initGui = true;
        }

    }

    private void createGui() {
        ui.clear();

        gui = new FullGui(graphics);
        gui.initialize(ui);
        gui.setSceneGraph(GaiaSky.instance.sg);
        gui.setVisibilityToggles(ComponentType.values(), SceneGraphRenderer.visible);
        gui.doneLoading(null);

        im = new InputMultiplexer();
        Gdx.input.setInputProcessor(im);

        // Key bindings
        im.addProcessor(new KeyInputController(input));
        // Mouse
        im.addProcessor(ui);

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
            gui.update(graphics.getDeltaTime());
            gui.render(graphics.getWidth(), graphics.getHeight());
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

    protected SearchDialog searchDialog;
    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case INITIALIZED_INFO:
            // Initialize full gui
            postRunnable(() -> createGui());
            break;
        case SHOW_QUIT_ACTION:
            QuitWindow quit = new QuitWindow(ui, skin);
            if (data.length > 0) {
                quit.setAcceptRunnable((Runnable) data[0]);
            }
            quit.show(ui);
            break;
        case SHOW_ABOUT_ACTION:
            (new AboutWindow(ui, skin)).show(ui);
            break;
        case SHOW_PREFERENCES_ACTION:
            (new PreferencesWindow(ui, skin)).show(ui);
            break;
        case SHOW_SEARCH_ACTION:
            if (searchDialog == null) {
                searchDialog = new SearchDialog(skin, ui, GaiaSky.instance.sg);
            } else {
                searchDialog.clearText();
            }
            searchDialog.show(ui);

            break;
        default:
            break;
        }

    }

    public static void postRunnable(Runnable r) {
        if (window != null)
            window.postRunnable(r);
        else
            Gdx.app.postRunnable(r);
    }
}
