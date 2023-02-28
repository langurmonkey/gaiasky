package gaiasky.gui.vr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.util.Settings;

/**
 * A viewport which does not depend on the screen size at all. To be used in VR GUIs.
 */
public class FixedScreenViewport extends Viewport {
    private final int width;
    private final int height;

    /** Creates a new viewport using a new {@link OrthographicCamera}. */
    public FixedScreenViewport(int width, int height) {
        this(width, height, new OrthographicCamera());
    }

    public FixedScreenViewport(int width, int height, Camera camera) {
        setCamera(camera);
        this.width = width;
        this.height = height;
    }

    public void update(int screenWidth, int screenHeight, boolean centerCamera) {
        setScreenBounds(0, 0, width, height);
        setWorldSize(width, height);
        apply(centerCamera);
    }

    @Override
    public void apply(boolean centerCamera) {
        int bbw = Settings.settings.graphics.backBufferResolution[0];
        int bbh = Settings.settings.graphics.backBufferResolution[1];
        if (width != bbw || height != bbh) {
            Gdx.gl.glViewport(0, 0, width * bbw / width, height * bbh / height);
        } else {
            Gdx.gl.glViewport(0, 0, width, height);
        }
        Gdx.gl.glViewport(0, 0, width, height);
        getCamera().viewportWidth = width;
        getCamera().viewportHeight = height;
        if (centerCamera)
            getCamera().position.set(width / 2f, height / 2f, 0f);
        getCamera().update();
    }

    public float getUnitsPerPixel() {
        return 1;
    }
}
