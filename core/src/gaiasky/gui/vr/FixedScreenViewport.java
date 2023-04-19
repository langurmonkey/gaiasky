/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.vr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.util.Settings;
import gaiasky.util.camera.CameraUtils;

public class FixedScreenViewport extends Viewport {
    private final int width;
    private final int height;
    final Vector3 tmp = new Vector3();

    /** Creates a new viewport using a new {@link OrthographicCamera}. */
    public FixedScreenViewport(int width, int height) {
        this(width, height, new OrthographicCamera());
    }

    public FixedScreenViewport(int width, int height, Camera camera) {
        setCamera(camera);
        this.width = width;
        this.height = height;
    }

    public Vector2 unproject (Vector2 screenCoords) {
        tmp.set(screenCoords.x, screenCoords.y, 1);
        CameraUtils.unproject(getCamera(), tmp, getScreenX(), getScreenY(), getScreenWidth(), getScreenHeight(), height);
        screenCoords.set(tmp.x, tmp.y);
        return screenCoords;
    }

    /** Transforms the specified screen coordinate to world coordinates.
     * @return The vector that was passed in, transformed to world coordinates.
     * @see Camera#unproject(Vector3) */
    public Vector3 unproject (Vector3 screenCoords) {
        CameraUtils.unproject(getCamera(), screenCoords, getScreenX(), getScreenY(), getScreenWidth(), getScreenHeight(), height);
        return screenCoords;
    }

    @Override
    public Vector2 toScreenCoordinates (Vector2 worldCoords, Matrix4 transformMatrix) {
        tmp.set(worldCoords.x, worldCoords.y, 0);
        tmp.mul(transformMatrix);
        getCamera().project(tmp, getScreenX(), getScreenY(), getScreenWidth(), getScreenHeight());
        tmp.y = height - tmp.y;
        worldCoords.x = tmp.x;
        worldCoords.y = tmp.y;
        return worldCoords;
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
