/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package gaiasky.vr.openxr;

import gaiasky.util.Settings;
import gaiasky.util.gdx.loader.OwnObjLoader;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.vr.openxr.input.XrControllerDevice;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWNativeGLX;
import org.lwjgl.glfw.GLFWNativeWGL;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;
import org.lwjgl.system.linux.X11;
import org.lwjgl.system.windows.User32;

import java.nio.FloatBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GLX13.*;
import static org.lwjgl.openxr.XR10.XR_TYPE_API_LAYER_PROPERTIES;
import static org.lwjgl.openxr.XR10.XR_TYPE_EXTENSION_PROPERTIES;
import static org.lwjgl.system.MemoryStack.stackInts;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memPutInt;

/**
 * A helper class with some static methods to help applications with OpenXR related tasks that are cumbersome in
 * some way.
 */
public final class XrHelper {

    private XrHelper() {
    }

    public static <T extends StructBuffer> T fill(T buffer, int offset, int value) {
        long ptr = buffer.address() + offset;
        int stride = buffer.sizeof();
        for (int i = 0; i < buffer.limit(); i++) {
            memPutInt(ptr + i * stride, value);
        }
        return buffer;
    }

    /**
     * Allocates an {@link XrApiLayerProperties.Buffer} onto the given stack with the given number of layers and
     * sets the type of each element in the buffer to {@link XR10#XR_TYPE_API_LAYER_PROPERTIES XR_TYPE_API_LAYER_PROPERTIES}.
     *
     * <p>Note: you can't use the buffer after the stack is gone!</p>
     *
     * @param stack     the stack to allocate the buffer on
     * @param numLayers the number of elements the buffer should get
     * @return the created buffer
     */
    public static XrApiLayerProperties.Buffer prepareApiLayerProperties(MemoryStack stack, int numLayers) {
        return fill(XrApiLayerProperties.malloc(numLayers, stack), XrApiLayerProperties.TYPE, XR_TYPE_API_LAYER_PROPERTIES);
    }

    /**
     * Allocates an {@link XrExtensionProperties.Buffer} onto the given stack with the given number of extensions
     * and sets the type of each element in the buffer to {@link XR10#XR_TYPE_EXTENSION_PROPERTIES XR_TYPE_EXTENSION_PROPERTIES}.
     *
     * <p>Note: you can't use the buffer after the stack is gone!</p>
     *
     * @param stack         the stack onto which to allocate the buffer
     * @param numExtensions the number of elements the buffer should get
     * @return the created buffer
     */
    public static XrExtensionProperties.Buffer prepareExtensionProperties(MemoryStack stack, int numExtensions) {
        return fill(XrExtensionProperties.malloc(numExtensions, stack), XrExtensionProperties.TYPE, XR_TYPE_EXTENSION_PROPERTIES);
    }

    /**
     * Allocates an {@link XrActionSuggestedBinding.Buffer} onto the given stack with the given number of bindings.
     *
     * <p>Note: you can't use the buffer after the stack is gone!</p>
     *
     * @param stack       the stack to allocate the buffer on
     * @param numBindings the number of elements the buffer should get
     * @return the created buffer
     */
    public static XrActionSuggestedBinding.Buffer prepareActionSuggestedBindings(MemoryStack stack, int numBindings) {
        return XrActionSuggestedBinding.malloc(numBindings, stack);
    }

    /**
     * Allocates a {@link FloatBuffer} onto the given stack and fills it such that it can be used as parameter
     * to the <b>set</b> method of <b>Matrix4f</b>. The buffer will be filled such that it represents a projection
     * matrix with the given <b>fov</b>, <b>nearZ</b> (a.k.a. near plane), <b>farZ</b> (a.k.a. far plane).
     *
     * @param stack      The stack onto which the buffer should be allocated
     * @param fov        The desired Field of View for the projection matrix. You should normally use the value of
     *                   {@link XrCompositionLayerProjectionView#fov}.
     * @param nearZ      The nearest Z value that the user should see (also known as the near plane)
     * @param farZ       The furthest Z value that the user should see (also known as far plane)
     * @param zZeroToOne True if the z-axis of the coordinate system goes from 0 to 1 (Vulkan).
     *                   False if the z-axis of the coordinate system goes from -1 to 1 (OpenGL).
     * @return A {@link FloatBuffer} that contains the matrix data of the desired projection matrix. Use the
     * <b>set</b> method of a <b>Matrix4f</b> instance to copy the buffer values to that matrix.
     */
    public static FloatBuffer createProjectionMatrixBuffer(MemoryStack stack, XrFovf fov, float nearZ, float farZ, boolean zZeroToOne) {
        float tanLeft = (float) Math.tan(fov.angleLeft());
        float tanRight = (float) Math.tan(fov.angleRight());
        float tanDown = (float) Math.tan(fov.angleDown());
        float tanUp = (float) Math.tan(fov.angleUp());
        float tanAngleWidth = tanRight - tanLeft;
        float tanAngleHeight;
        if (zZeroToOne) {
            tanAngleHeight = tanDown - tanUp;
        } else {
            tanAngleHeight = tanUp - tanDown;
        }

        FloatBuffer m = stack.mallocFloat(16);

        m.put(0, 2.0f / tanAngleWidth);
        m.put(4, 0.0f);
        m.put(8, (tanRight + tanLeft) / tanAngleWidth);
        m.put(12, 0.0f);

        m.put(1, 0.0f);
        m.put(5, 2.0f / tanAngleHeight);
        m.put(9, (tanUp + tanDown) / tanAngleHeight);
        m.put(13, 0.0f);

        m.put(2, 0.0f);
        m.put(6, 0.0f);
        if (zZeroToOne) {
            m.put(10, -farZ / (farZ - nearZ));
            m.put(14, -(farZ * nearZ) / (farZ - nearZ));
        } else {
            m.put(10, -(farZ + nearZ) / (farZ - nearZ));
            m.put(14, -(farZ * (nearZ + nearZ)) / (farZ - nearZ));
        }

        m.put(3, 0.0f);
        m.put(7, 0.0f);
        m.put(11, -1.0f);
        m.put(15, 0.0f);

        return m;
    }

    /**
     * <p>
     * Allocates a {@code XrGraphicsBindingOpenGL**} struct for the current platform onto the given stack. It should
     * be included in the next-chain of the {@link XrSessionCreateInfo} that will be used to create an
     * OpenXR session with OpenGL rendering. (Every platform requires a different OpenGL graphics binding
     * struct, so this method spares users the trouble of working with all these cases themselves.)
     * </p>
     *
     * <p>
     * Note: {@link XR10#xrCreateSession} must be called <b>before</b> the given stack is dropped!
     * </p>
     *
     * <p>
     * Note: Linux support is not finished, so only Windows works at the moment. This should be fixed in the
     * future. Until then, Vulkan is the only cross-platform rendering API for the OpenXR Java bindings.
     * </p>
     *
     * @param stack  The stack onto which to allocate the graphics binding struct
     * @param window The GLFW window handle used to create the OpenGL context
     * @return A {@code XrGraphicsBindingOpenGL**} struct that can be used to create a session
     * @throws IllegalStateException If the current platform is not supported
     */
    public static Struct createOpenGLBinding(MemoryStack stack, long window) {
        //Bind the OpenGL context to the OpenXR instance and create the session
        if (Platform.get() == Platform.WINDOWS) {
            return XrGraphicsBindingOpenGLWin32KHR.calloc(stack).set(KHROpenGLEnable.XR_TYPE_GRAPHICS_BINDING_OPENGL_WIN32_KHR, NULL, User32.GetDC(GLFWNativeWin32.glfwGetWin32Window(window)), GLFWNativeWGL.glfwGetWGLContext(window));
        } else if (Platform.get() == Platform.LINUX) {
            long xDisplay = GLFWNativeX11.glfwGetX11Display();

            long glXContext = GLFWNativeGLX.glfwGetGLXContext(window);
            long glXWindowHandle = GLFWNativeGLX.glfwGetGLXWindow(window);

            int fbXID = glXQueryDrawable(xDisplay, glXWindowHandle, GLX_FBCONFIG_ID);
            PointerBuffer fbConfigBuf = glXChooseFBConfig(xDisplay, X11.XDefaultScreen(xDisplay), stackInts(GLX_FBCONFIG_ID, fbXID, 0));
            if (fbConfigBuf == null) {
                throw new IllegalStateException("Your framebuffer config was null, make a github issue");
            }
            long fbConfig = fbConfigBuf.get();

            return XrGraphicsBindingOpenGLXlibKHR.calloc(stack).set(KHROpenGLEnable.XR_TYPE_GRAPHICS_BINDING_OPENGL_XLIB_KHR, NULL, xDisplay, (int) Objects.requireNonNull(glXGetVisualFromFBConfig(xDisplay, fbConfig)).visualid(), fbConfig, glXWindowHandle, glXContext);
        } else {
            throw new IllegalStateException("macOS not supported");
        }
    }

    public static IntModel loadRenderModel(XrDriver driver, XrControllerDevice controllerDevice) {
        IntModel model = null;
        OwnObjLoader ol = new OwnObjLoader();
        
        if (driver != null && (driver.hmdName.contains("oculus") || driver.hmdName.contains("Oculus") || driver.hmdName.contains("Rift") )) {
            // Oculus Rift CV1.
            if (controllerDevice.deviceType.isLeft()) {
                model = ol.loadModel(Settings.settings.data.dataFileHandle("$data/default-data/models/controllers/oculus/oculus-left.obj"));
            } else {
                model = ol.loadModel(Settings.settings.data.dataFileHandle("$data/default-data/models/controllers/oculus/oculus-right.obj"));
            }
        }else if (driver != null && (driver.hmdName.contains("SteamVR") || driver.hmdName.contains("Index") || driver.hmdName.contains("index"))) {
            // Valve index.
            if (controllerDevice.deviceType.isLeft()) {
                model = ol.loadModel(Settings.settings.data.dataFileHandle("$data/default-data/models/controllers/index/index-left.obj"));
            } else {
                model = ol.loadModel(Settings.settings.data.dataFileHandle("$data/default-data/models/controllers/index/index-right.obj"));
            }
        } else if (driver != null && (driver.hmdName.contains("HTC") ||driver.hmdName.contains("htc") || driver.hmdName.contains("vive")|| driver.hmdName.contains("Vive"))) {
            // HTC vive controller model.
            model = ol.loadModel(Settings.settings.data.dataFileHandle("$data/default-data/models/controllers/vive/vr_controller_vive.obj"));
        } else {
            // Load default model.
            model = ol.loadModel(Settings.settings.data.dataFileHandle("$data/default-data/models/controllers/generic/generic_vr_controller.obj"));
        }

        return model;
    }
}
