/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.vr.openxr;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import gaiasky.render.RenderingContext;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.util.Constants;
import gaiasky.util.math.Matrix4Utils;
import org.lwjgl.openxr.XrCompositionLayerProjectionView;
import org.lwjgl.openxr.XrPosef;
import org.lwjgl.openxr.XrQuaternionf;
import org.lwjgl.openxr.XrVector3f;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.stackPush;

public class XrViewManager {

    private final Quaternion quaternion = new Quaternion();
    private final Quaternion rot = new Quaternion();
    private final Matrix4 viewMat = new Matrix4();

    public void updateCamera(XrCompositionLayerProjectionView layerView,
                             PerspectiveCamera camera) {
        updateCamera(layerView, camera, null, null);
    }

    public void updateCamera(XrCompositionLayerProjectionView layerView,
                             PerspectiveCamera camera,
                             NaturalCamera naturalCamera,
                             RenderingContext rc) {
        XrPosef pose = layerView.pose();
        XrVector3f position = pose.position$();
        XrQuaternionf orientation = pose.orientation();
        try (MemoryStack stack = stackPush()) {
            // Set camera projection.
            Matrix4Utils.put(camera.projection, XrHelper.createProjectionMatrixBuffer(stack, layerView.fov(), camera.near, camera.far, false));
        }
        quaternion.set(orientation.x(), orientation.y(), orientation.z(), orientation.w());
        // Set camera view.
        camera.view.idt().translate(position.x(), position.y(), position.z()).rotate(quaternion).inv();

        camera.combined.set(camera.projection);
        Matrix4.mul(camera.combined.val, camera.view.val);

        camera.position.set(position.x(), position.y(), position.z());
        camera.direction.set(0, 0, -1).mul(quaternion);
        camera.up.set(0, 1, 0).mul(quaternion);

        // Update main camera.
        if (naturalCamera != null) {
            naturalCamera.vrOffset.set(camera.position).scl(Constants.M_TO_U);
            naturalCamera.direction.set(camera.direction);
            naturalCamera.up.set(camera.up);
            rc.vrOffset = naturalCamera.vrOffset;
        }
    }
}
