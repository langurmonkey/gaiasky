/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.math.Vector3D;
import gaiasky.vr.openxr.input.XrControllerDevice;

public class VRDevice implements Component, IObserver {

    public XrControllerDevice device;

    // Intersection sphere.
    public IntModelInstance intersectionModel;
    public Vector3D intersection;

    // Points in the beam.
    public Vector3D beamP0 = new Vector3D();
    public Vector3D beamP1 = new Vector3D();
    public Vector3D beamP2 = new Vector3D();
    // Final point, always very, very far. Not used for rendering.
    public Vector3D beamPn = new Vector3D();

    // Default colors for normal and select mode.
    private static final Color normal = ColorUtils.gRedC;
    private static final Color select = ColorUtils.gGreenC;

    // Color for each point.
    public float[] colorP0 = new float[] { normal.r, normal.g, normal.b, 0.8f };
    public float[] colorP1 = new float[] { normal.r, normal.g, normal.b, 0.1f };
    public float[] colorP2 = new float[] { normal.r, normal.g, normal.b, 0.0f };
    // Whether the controller hits the UI.
    public boolean hitUI = false;
    // If the UI is hit, is this controller interacting with the UI?
    public boolean interacting = false;

    public VRDevice() {
        EventManager.instance.subscribe(this, Event.VR_SELECTING_STATE);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        // Update colors!
        if (event == Event.VR_SELECTING_STATE) {
            var dev = (XrControllerDevice) data[2];
            if (dev != null && dev == this.device) {
                var selecting = (Boolean) data[0];
                var completion = (Double) data[1];
                if (selecting) {
                    // Start.
                    colorP0[0] = select.r;
                    colorP0[1] = select.g;
                    colorP0[2] = select.b;
                    colorP0[3] = (float) MathUtils.clamp(completion + 0.2f, 0f, 1.0f);
                    // Middle.
                    colorP1[0] = select.r;
                    colorP1[1] = select.g;
                    colorP1[2] = select.b;
                    colorP1[3] = (float) MathUtils.clamp(completion + 0.1f, 0f, 1.0f);
                    // End.
                    colorP2[0] = select.r;
                    colorP2[1] = select.g;
                    colorP2[2] = select.b;
                    colorP2[3] = completion.floatValue();
                } else {
                    // Revert to red.
                    // Start.
                    colorP0[0] = normal.r;
                    colorP0[1] = normal.g;
                    colorP0[2] = normal.b;
                    colorP0[3] = 0.8f;
                    // Middle.
                    colorP1[0] = normal.r;
                    colorP1[1] = normal.g;
                    colorP1[2] = normal.b;
                    colorP1[3] = 0.1f;
                    // End.
                    colorP2[0] = normal.r;
                    colorP2[1] = normal.g;
                    colorP2[2] = normal.b;
                    colorP2[3] = 0.0f;
                }
            }
        }
    }
}
