package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.data.util.PointCloudData;
import gaiasky.render.RenderGroup;

public class Verts implements Component {
    public boolean blend = true, depth = true, additive = true;

    public int glPrimitive;

    /** The render group **/
    public RenderGroup renderGroup;

    /** Whether to close the polyline (connect end point to start point) or not **/
    public boolean closedLoop = true;

    // Line width
    public float primitiveSize = 1f;

    public PointCloudData pointCloudData;
}
