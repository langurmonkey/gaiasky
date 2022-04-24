package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.data.util.PointCloudData;
import gaiasky.render.SceneGraphRenderer.RenderGroup;

public class Verts implements Component {
    protected boolean blend = true, depth = true, additive = true;

    protected int glPrimitive;

    /** The render group **/
    protected RenderGroup renderGroup;

    /** Whether to close the polyline (connect end point to start point) or not **/
    protected boolean closedLoop = true;

    // Line width
    protected float primitiveSize = 1f;

    protected PointCloudData pointCloudData;
}
