/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.math.MathUtils;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import org.lwjgl.glfw.GLFW;

public class GameMouseKbdListener extends MouseKbdListener implements IObserver {
    private static final Log logger = Logger.getLogger(GameMouseKbdListener.class);

    private float prevX = 0, prevY = 0;
    private float dx = 0, dy = 0;
    private boolean prevValid = false;

    private static class GameGestureListener extends GestureAdapter {
        private GameGestureListener() {
        }
    }

    public GameMouseKbdListener(GameGestureListener l, NaturalCamera naturalCamera) {
        super(l, naturalCamera);
        EventManager.instance.subscribe(this, Event.MOUSE_CAPTURE_CMD, Event.MOUSE_CAPTURE_TOGGLE);
    }

    public GameMouseKbdListener(NaturalCamera naturalCamera) {
        this(new GameGestureListener(), naturalCamera);
    }

    @Override
    public void update() {
        float keySensitivity = 3e-1f;
        // Run mode
        float multiplier = isKeyPressed(Keys.SHIFT_LEFT) ? 3f : 1f;
        double minTranslateUnits = 1e-5;
        if (anyPressed(Keys.W, Keys.A, Keys.S, Keys.D)) {
            camera.vel.setZero();
            if (anyPressed(Keys.Q, Keys.E, Keys.SPACE, Keys.C)) {
                camera.setInputByController(false);
            }
        }

        if (isKeyPressed(Keys.W)) {
            camera.forward(1f * keySensitivity * multiplier, minTranslateUnits);
        } else if (isKeyPressed(Keys.S)) {
            camera.forward(-1f * keySensitivity * multiplier, minTranslateUnits);
        }

        if (isKeyPressed(Keys.D)) {
            camera.strafe(1f * keySensitivity * multiplier, minTranslateUnits);
        } else if (isKeyPressed(Keys.A)) {
            camera.strafe(-1f * keySensitivity * multiplier, minTranslateUnits);
        }

        if (isKeyPressed(Keys.Q)) {
            camera.addRoll(8f * keySensitivity, true);
        } else if (isKeyPressed(Keys.E)) {
            camera.addRoll(-8f * keySensitivity, true);
        }

        if (isKeyPressed(Keys.SPACE)) {
            camera.vertical(1f * keySensitivity * multiplier, minTranslateUnits);
        } else if (isKeyPressed(Keys.C)) {
            camera.vertical(-1f * keySensitivity * multiplier, minTranslateUnits);
        }

    }

    @Override
    public float getResponseTime() {
        return 0.1f;
    }

    @Override
    public void activate() {
        GaiaSky.postRunnable(() -> {
            camera.setDiverted(true);
            // Capture mouse
            setMouseCapture(true);
            // Unfocus
            GaiaSky.instance.mainGui.getGuiStage().unfocusAll();

            ModePopupInfo mpi = new ModePopupInfo();
            mpi.title = "Game mode";
            mpi.header = "You have entered Game mode!";
            mpi.addMapping("Move forward", "W");
            mpi.addMapping("Move backward", "S");
            mpi.addMapping("Strafe left", "A");
            mpi.addMapping("Strafe right", "D");
            mpi.addMapping("Roll left", "Q");
            mpi.addMapping("Roll right", "E");
            mpi.addMapping("Move up", "Space");
            mpi.addMapping("Move down", "C");
            mpi.addMapping("Look around", "Mouse");
            mpi.addMapping("Toggle mouse capture", "SHIFT", "CTRL", "L");
            mpi.addMapping("Go back to focus mode", "1");

            EventManager.publish(Event.MODE_POPUP_CMD, this, mpi, "gamemode", 120f);
        });
    }

    @Override
    public void deactivate() {
        // Release mouse
        setMouseCapture(false);
        EventManager.publish(Event.MODE_POPUP_CMD, this, null, "gamemode");
        prevValid = false;
    }

    @Override
    public boolean keyDown(int keycode) {
        return super.keyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        return super.keyUp(keycode);
    }

    private void updatePreviousMousePosition(float x, float y) {
        prevX = x;
        prevY = y;
    }

    private float lowPass(float newValue, float smoothedValue, float smoothing) {
        if (smoothing > 0) {
            smoothedValue += (newValue - smoothedValue) / smoothing;
            return smoothedValue;
        } else {
            return newValue;
        }
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        float dt = Gdx.graphics.getDeltaTime() * 2e2f;
        float x = screenX;
        float y = screenY;
        float mouseXSensitivity = 1f / dt;
        float mouseYSensitivity = -1f / dt;
        if (!prevValid) {
            updatePreviousMousePosition(x, y);
            prevValid = true;
        }
        float limit = 17f;
        dx = MathUtils.clamp(lowPass(mouseXSensitivity * (x - prevX), dx, 14f), -limit, limit);
        dy = MathUtils.clamp(lowPass(mouseYSensitivity * (y - prevY), dy, 14f), -limit, limit);
        camera.addYaw(dx, true);
        camera.addPitch(dy, true);

        updatePreviousMousePosition(x, y);
        camera.setInputByController(false);
        return true;
    }

    @Override
    public boolean touchUp(float x, float y, int pointer, int button) {
        if (button == Input.Buttons.RIGHT) {
            int w = Gdx.graphics.getWidth();
            int h = Gdx.graphics.getHeight();
            Gdx.input.setCursorPosition(w / 2, h / 2);
            prevX = w / 2;
            prevY = h / 2;
        }
        return true;
    }

    private void setMouseCapture(boolean state) {
        int cursorStatus = state ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL;
        GLFW.glfwSetInputMode(((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle(), GLFW.GLFW_CURSOR, cursorStatus);
    }

    private void toggleMouseCapture() {
        if (GLFW.glfwGetInputMode(((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle(), GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_DISABLED) {
            GLFW.glfwSetInputMode(((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        } else {
            GLFW.glfwSetInputMode(((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case MOUSE_CAPTURE_CMD:
            setMouseCapture((Boolean) data[0]);
            break;
        case MOUSE_CAPTURE_TOGGLE:
            toggleMouseCapture();
            break;
        }
    }
}
