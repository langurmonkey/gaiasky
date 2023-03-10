package gaiasky.vr.openxr;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import gaiasky.render.RenderingContext;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.util.Constants;
import org.joml.Matrix4f;
import org.lwjgl.openxr.XrCompositionLayerProjectionView;
import org.lwjgl.openxr.XrPosef;
import org.lwjgl.openxr.XrQuaternionf;
import org.lwjgl.openxr.XrVector3f;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Manages an OpenXR view and sets the perspective camera to its right state.
 */
public class XrViewManager {

    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Quaternion quaternion = new Quaternion();

    public void updateCamera(XrCompositionLayerProjectionView layerView, PerspectiveCamera camera) {
        updateCamera(layerView, camera, null, null);
    }

    public void updateCamera(XrCompositionLayerProjectionView layerView, PerspectiveCamera camera, NaturalCamera naturalCamera, RenderingContext rc) {
        XrPosef pose = layerView.pose();
        XrVector3f position = pose.position$();
        XrQuaternionf orientation = pose.orientation();
        try (MemoryStack stack = stackPush()) {
            projectionMatrix.set(XrHelper.createProjectionMatrixBuffer(stack, layerView.fov(), camera.near, camera.far, false));
        }
        viewMatrix.translationRotateScaleInvert(position.x(), position.y(), position.z(), orientation.x(), orientation.y(), orientation.z(), orientation.w(), 1, 1, 1);

        projectionMatrix.get(camera.projection.val);
        viewMatrix.get(camera.view.val);
        camera.combined.set(camera.projection);
        Matrix4.mul(camera.combined.val, camera.view.val);

        quaternion.set(orientation.x(), orientation.y(), orientation.z(), orientation.w());
        camera.position.set(position.x(), position.y(), position.z());
        camera.direction.set(0, 0, -1).mul(quaternion);
        camera.up.set(0, 1, 0).mul(quaternion);

        // Update main camera
        if (naturalCamera != null) {
            naturalCamera.vrOffset.set(camera.position).scl(Constants.M_TO_U);
            naturalCamera.direction.set(camera.direction);
            naturalCamera.up.set(camera.up);
            rc.vrOffset = naturalCamera.vrOffset;
        }
    }
}
