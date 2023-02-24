package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.math.Vector3d;
import gaiasky.vr.openvr.VRContext;

public class VRDevice implements Component, IObserver {

    public VRContext.VRDevice device;

    // Points in the beam.
    public Vector3d beamP0 = new Vector3d();
    public Vector3d beamP1 = new Vector3d();
    public Vector3d beamP2 = new Vector3d();

    // Default colors for normal and select mode.
    private static final Color normal = ColorUtils.gRedC;
    private static final Color select = ColorUtils.gGreenC;

    // Color for each point.
    public float[] colorP0 = new float[] { normal.r, normal.g, normal.b, 0.7f };
    public float[] colorP1 = new float[] { normal.r, normal.g, normal.b, 0.4f };
    public float[] colorP2 = new float[] { normal.r, normal.g, normal.b, 0.0f };
    public boolean hitUI = false;

    public VRDevice() {
        EventManager.instance.subscribe(this, Event.VR_SELECTING_STATE);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        // Update colors!
        if (event == Event.VR_SELECTING_STATE) {
            var dev = (VRContext.VRDevice) data[2];
            if (dev != null && dev == this.device) {
                var selecting = (Boolean) data[0];
                var completion = (Double) data[1];
                if (selecting) {
                    // Start.
                    colorP0[0] = select.r;
                    colorP0[1] = select.g;
                    colorP0[2] = select.b;
                    colorP0[3] = (float) MathUtils.clamp(completion + 0.3f, 0f, 1.0f);
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
                    colorP0[3] = 0.7f;
                    // Middle.
                    colorP1[0] = normal.r;
                    colorP1[1] = normal.g;
                    colorP1[2] = normal.b;
                    colorP1[3] = 0.4f;
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
