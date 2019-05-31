/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.NaturalCamera;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import org.lwjgl.glfw.GLFW;

public class GameMouseKbdListener extends MouseKbdListener implements IObserver {
    private static Log logger = Logger.getLogger(GameMouseKbdListener.class);

    private int prevX = Integer.MIN_VALUE, prevY = Integer.MIN_VALUE;
    private float dx = 0, dy = 0;

    private static class GameGestureListener extends GestureAdapter {
        private GameGestureListener() {
        }
    }

    public GameMouseKbdListener(GameGestureListener l, NaturalCamera naturalCamera) {
        super(l, naturalCamera);
        EventManager.instance.subscribe(this, Events.MOUSE_CAPTURE_CMD, Events.MOUSE_CAPTURE_TOGGLE);
    }

    public GameMouseKbdListener(NaturalCamera naturalCamera) {
        this(new GameGestureListener(), naturalCamera);
    }

    @Override
    public void update() {
        float keySensitivity = 15f;
        // Run mode
        float multiplier = isKeyPressed(Keys.SHIFT_LEFT) ? 3f : 1f;
        if (isKeyPressed(Keys.W)) {
            camera.forward(0.1f * keySensitivity * multiplier);
        }
        if (isKeyPressed(Keys.S)) {
            camera.forward(-0.1f * keySensitivity * multiplier);
        }
        if (isKeyPressed(Keys.D)) {
            camera.strafe(0.1f * keySensitivity * multiplier);
        }
        if (isKeyPressed(Keys.A)) {
            camera.strafe(-0.1f * keySensitivity * multiplier);
        }
        if (isKeyPressed(Keys.Q)) {
            camera.addRoll(0.8f * keySensitivity, true);
        }
        if (isKeyPressed(Keys.E)) {
            camera.addRoll(-0.8f * keySensitivity, true);
        }
        if(isKeyPressed(Keys.SPACE)) {
            camera.vertical(0.1f * keySensitivity * multiplier);
        }
        if(isKeyPressed(Keys.C)) {
            camera.vertical(-0.1f * keySensitivity * multiplier);
        }
    }

    @Override
    public float getResponseTime() {
        return 0.1f;
    }

    @Override
    public void activate() {
        camera.setDiverted(true);
        // Capture mouse
        setMouseCapture(true);

        String title = "GAME_MODE mode activated!";
        String[] msgs = new String[]{
                "<W,A,S,D>  move",
                "<Q,E>  roll",
                "<SPACE,C>  up and down",
                "<Mouse>  look around",
                "<SHIFT+CTRL+L>  toggle capture mouse",
                "<1>  go back to normal mode"
        };
        EventManager.instance.post(Events.SCREEN_NOTIFICATION_CMD, title, msgs, 10f);

    }

    @Override
    public void deactivate() {
        // Release mouse
        setMouseCapture(false);
    }

    @Override
    public boolean keyDown(int keycode) {
        return super.keyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        return super.keyUp(keycode);
    }

    private void updatePreviousMousePosition(int x, int y) {
        prevX = x;
        prevY = y;
    }

    private float lowPass(float newValue, float smoothedValue, float smoothing) {
        smoothedValue += (newValue - smoothedValue) / smoothing;
        return smoothedValue;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        float mouseXSensitivity = 2f;
        float mouseYSensitivity = -2f;
        if (prevX == Integer.MIN_VALUE) {
            updatePreviousMousePosition(screenX, screenY);
        }
        dx = lowPass(mouseXSensitivity * (screenX - prevX), dx, 5f);
        dy = lowPass(mouseYSensitivity * (screenY - prevY), dy, 5f);
        camera.addYaw(dx, true);
        camera.addPitch(dy, true);

        updatePreviousMousePosition(screenX, screenY);
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
    public void notify(Events event, Object... data) {
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
