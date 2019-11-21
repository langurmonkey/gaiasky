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
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.IPostProcessor.RenderType;
import gaiasky.render.PostProcessorFactory;
import gaiasky.util.GlobalResources;
import gaiasky.util.scene2d.OwnLabel;

public class GaiaSkyView implements ApplicationListener, IObserver {

    /** Window **/
    public static Lwjgl3Window window;
    /** Graphics **/
    public static Lwjgl3Graphics graphics;
    /** Input **/
    public static Lwjgl3Input input;

    private Skin skin;
    private Stage ui;

    private SpriteBatch sb;
    private FrameBuffer renderBuffer;
    private boolean initGui = false;
    private boolean initializing = true;

    private Vector2 lastTexSize;

    public GaiaSkyView() {
        super();
        this.skin = GlobalResources.skin;
        this.lastTexSize = new Vector2(-1, -1);
        EventManager.instance.subscribe(this, Events.INITIALIZED_INFO);
    }

    public void setWindow(Lwjgl3Window window) {
        GaiaSkyView.window = window;
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

            OwnLabel l = new OwnLabel("The external view will appear here as soon as Gaia Sky finishes loading", skin, "ui-15");
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
            renderBuffer = PostProcessorFactory.instance.getPostProcessor().getPostProcessBean(RenderType.screen).pp.getCombinedBuffer().getResultBuffer();
            if (renderBuffer != null) {
                Texture tex = renderBuffer.getColorBufferTexture();
                float tw = tex.getWidth();
                float th = tex.getHeight();
                float tar = tw / th;
                float gw = graphics.getWidth();
                float gh = graphics.getHeight();
                float gar = gw / gh;

                if(tw != lastTexSize.x || th != lastTexSize.y){
                    // Adapt projection matrix to new size
                    Matrix4 mat = sb.getProjectionMatrix();
                    mat.setToOrtho2D(0, 0, tw, th);
                }

                System.out.println(tex.toString() + " g/ " + gw + "x" + gh + " (" + gar + ")  t/ " + tw + "x" + th + " (" + tar + ")");

                // Output
                float x = 0, y = 0;
                float w = tw, h = th;
                int sx = 0, sy = 0;
                int sw = (int) gw, sh = (int) gh;

                if (gw > tw && gh > th) {
                    x = 0;
                    y = 0;
                    w = tw;
                    h = th;
                    // Texture contained in screen, extend texture keeping ratio
                    if (gar > tar) {
                        // Graphics are stretched horizontally
                        sw = (int) tw;
                        sh = (int) (tw / gar);

                        sx = (int) ((tw - sw) / 2f);
                        sy = (int) ((th - sh) / 2f);
                    } else {
                        // Graphics are stretched vertically
                        sw = (int) (th * gar);
                        sh = (int) th;

                        sx = (int) ((tw - sw) / 2f);
                        sy = (int) ((th - sh) / 2f);
                    }
                } else if (tw >= gw) {
                    sx = (int) ((tw - gw) / 2f);
                    if (th >= gh) {
                        sy = (int) ((th - gh) / 2f);
                    } else {
                        x = 0;
                        y = 0;
                        w = tw;
                        h = th;
                        sw = (int) (th * gar);
                        sh = (int) th;

                        sx = (int) ((tw - sw) / 2f);
                        sy = (int) ((th - sh) / 2f);
                    }
                } else {
                    w = gw;
                    if (th >= gh) {
                        x = 0;
                        y = 0;
                        w = tw;
                        h = th;
                        sw = (int) tw;
                        sh = (int) (tw / gar);

                        sx = (int) ((tw - sw) / 2f);
                        sy = (int) ((th - sh) / 2f);
                    } else {
                        h = gh;
                    }
                }
                sb.begin();
                sb.draw(tex, x, y, w, h, sx, sy, sw, sh, false, true);
                sb.end();

                lastTexSize.set(tw, th);
            }
        }
    }

    private Vector2 maintainRatio(float ar, float w, float h) {
        float nar = w / h;
        if (nar == ar) {
            return lastTexSize.set(w, h);
        } else {

            return lastTexSize.set(w, h);
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
    public void notify(Events event, Object... data) {
        switch (event) {
        case INITIALIZED_INFO:
            // Initialize full gui
            postRunnable(() -> removeGui());
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
