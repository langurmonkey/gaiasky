/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.math.MathUtils;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.ModePopupInfo;
import gaiasky.scene.camera.NaturalCamera;
import org.lwjgl.glfw.GLFW;

public class GameMouseKbdListener extends AbstractMouseKbdListener implements IObserver {
    private final NaturalCamera camera;
    private float prevX = 0, prevY = 0;
    private float dx = 0, dy = 0;
    private boolean prevValid = false;

    public GameMouseKbdListener(GameGestureListener l, NaturalCamera naturalCamera) {
        super(l, naturalCamera);
        this.camera = naturalCamera;
        EventManager.instance.subscribe(this, Event.MOUSE_CAPTURE_CMD, Event.MOUSE_CAPTURE_TOGGLE);
    }

    public GameMouseKbdListener(NaturalCamera naturalCamera) {
        this(new GameGestureListener(), naturalCamera);
    }

    @Override
    public boolean pollKeys() {
        float keySensitivity = 3e-1f;
        // Run mode
        float multiplier = isKeyPressed(Keys.SHIFT_LEFT) ? 3f : 1f;
        double minTranslateUnits = 1e-5;
        boolean result = false;
        if (anyPressed(Keys.W, Keys.A, Keys.S, Keys.D)) {
            camera.vel.setZero();
            if (anyPressed(Keys.Q, Keys.E, Keys.SPACE, Keys.C)) {
                camera.setGamepadInput(false);
            }
            result = true;
        }

        if (isKeyPressed(Keys.W)) {
            camera.forward(keySensitivity * multiplier, minTranslateUnits);
            result = true;
        } else if (isKeyPressed(Keys.S)) {
            camera.forward(-keySensitivity * multiplier, minTranslateUnits);
            result = true;
        }

        if (isKeyPressed(Keys.D)) {
            camera.strafe(keySensitivity * multiplier, minTranslateUnits);
            result = true;
        } else if (isKeyPressed(Keys.A)) {
            camera.strafe(-keySensitivity * multiplier, minTranslateUnits);
            result = true;
        }

        if (isKeyPressed(Keys.Q)) {
            camera.addRoll(keySensitivity, true);
            result = true;
        } else if (isKeyPressed(Keys.E)) {
            camera.addRoll(-keySensitivity, true);
            result = true;
        }

        if (isKeyPressed(Keys.SPACE)) {
            camera.vertical(keySensitivity * multiplier, minTranslateUnits);
            result = true;
        } else if (isKeyPressed(Keys.C)) {
            camera.vertical(-keySensitivity * multiplier, minTranslateUnits);
            result = true;
        }
        return result;
    }

    @Override
    public float getResponseTime() {
        return 0.1f;
    }

    @Override
    public void activate() {
        super.activate();
        GaiaSky.postRunnable(() -> {
            camera.setDiverted(true);
            // Capture mouse.
            setMouseCapture(true);
            // Unfocus camera.
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

            EventManager.publish(Event.MODE_POPUP_CMD, this, mpi, "gamemode", 10f);
        });
    }

    @Override
    public void deactivate() {
        super.deactivate();
        // Release mouse
        setMouseCapture(false);
        EventManager.publish(Event.MODE_POPUP_CMD, this, null, "gamemode");
        prevValid = false;
    }

    @Override
    public boolean keyDown(int keyCode) {
        return super.keyDown(keyCode);
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
        if (isActive()) {
            float dt = Gdx.graphics.getDeltaTime() * 2e2f;
            float mouseXSensitivity = 1f / dt;
            float mouseYSensitivity = -1f / dt;
            if (!prevValid) {
                updatePreviousMousePosition((float) screenX, (float) screenY);
                prevValid = true;
            }
            float limit = 17f;
            dx = MathUtils.clamp(lowPass(mouseXSensitivity * ((float) screenX - prevX), dx, 14f), -limit, limit);
            dy = MathUtils.clamp(lowPass(mouseYSensitivity * ((float) screenY - prevY), dy, 14f), -limit, limit);
            camera.addYaw(dx, true);
            camera.addPitch(dy, true);

            updatePreviousMousePosition((float) screenX, (float) screenY);
            camera.setGamepadInput(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(float x, float y, int pointer, int button) {
        if (isActive()) {
            if (button == Input.Buttons.RIGHT) {
                int w = Gdx.graphics.getWidth();
                int h = Gdx.graphics.getHeight();
                Gdx.input.setCursorPosition(w / 2, h / 2);
                prevX = w / 2;
                prevY = h / 2;
            }
            return true;
        }
        return false;
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
        case MOUSE_CAPTURE_CMD -> setMouseCapture((Boolean) data[0]);
        case MOUSE_CAPTURE_TOGGLE -> toggleMouseCapture();
        }
    }

    private static class GameGestureListener extends GestureAdapter {
        private GameGestureListener() {
        }
    }
}
