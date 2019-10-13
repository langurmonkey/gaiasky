/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.IPostProcessor.PostProcessBean;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.ICamera;

public interface IMainRenderer {

    FrameBuffer getFrameBuffer(int w, int h);

    void preRenderScene();

    void renderSgr(ICamera camera, double dt, int width, int height, FrameBuffer frameBuffer, PostProcessBean ppb);

    ICamera getICamera();

    double getT();

    CameraManager getCameraManager();

    IPostProcessor getPostProcessor();
}
