package gaiasky.desktop.util;

/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.gui.ConsoleLogger;
import gaiasky.util.SettingsManager;
import gaiasky.util.i18n.I18n;
import gaiasky.vr.openxr.OpenXRDriver;
import gaiasky.vr.openxr.OpenXRDriver.Swapchain;
import gaiasky.vr.openxr.ShadersGL;
import gaiasky.vr.openxr.XrHelper;
import gaiasky.vr.openxr.input.OpenXRInputListener;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Slightly tweaked combination of
 * https://github.com/maluoi/OpenXRSamples/blob/master/SingleFileExample
 * and
 * https://github.com/ReliaSolve/OpenXR-OpenGL-Example
 * Missing actions (xr input) and can only run on windows.
 * Requires a stereo headset and an install of the OpenXR runtime to run.
 */
public class HiOpenXRGL {

    static OpenXRDriver driver;

    long window;

    //GL globals
    Map<XrSwapchainImageOpenGLKHR, Integer> depthTextures; //Swapchain images only provide a color texture so we have to create depth textures seperatley

    int swapchainFramebuffer;
    int cubeVertexBuffer;
    int cubeIndexBuffer;
    int quadVertexBuffer;
    int cubeVAO;
    int quadVAO;
    int screenShader;
    int textureShader;
    int colorShader;

    public static void main(String[] args) throws Exception {
        // Enable logging.
        Gdx.files = new Lwjgl3Files();
        SettingsManager.initialize(new FileInputStream("assets/conf/config.yaml"), new FileInputStream("assets/dummyversion"));
        I18n.initialize(Gdx.files.internal("assets/i18n/gsbundle"),
                Gdx.files.internal("assets/i18n/objects"));

        // Add notif watch
        new ConsoleLogger();

        // Main classes.
        HiOpenXRGL hi = new HiOpenXRGL();
        driver = new OpenXRDriver();

        driver.createOpenXRInstance();
        driver.initializeXRSystem();
        hi.initializeAndBindOpenGL();
        driver.initializeOpenXRSession(hi.window);
        driver.createOpenXRReferenceSpace();
        driver.createOpenXRSwapchains();
        hi.createOpenGLResources();
        driver.initializeInput();

        driver.addListener(new OpenXRInputListener() {
            @Override
            public boolean buttonA(boolean value) {
                System.out.println("BUTTON A " + value);
                return false;
            }

            @Override
            public boolean buttonB(boolean value) {
                System.out.println("BUTTON B " + value);
                return false;
            }

            @Override
            public boolean buttonTrigger(boolean value) {
                return false;
            }

            @Override
            public boolean buttonThumbstick(boolean value) {
                return false;
            }

            @Override
            public boolean thumbstick(XrVector2f value) {
                return false;
            }

            @Override
            public boolean trigger(float value) {
                return false;
            }
        });

        while (!driver.pollEvents() && !glfwWindowShouldClose(hi.window)) {
            if (driver.isRunning()) {
                hi.renderFrameOpenXR();
            } else {
                // Throttle loop since xrWaitFrame won't be called.
                Thread.sleep(250);
            }
        }

        // Wait until idle
        glFinish();

        // Destroy OpenXR
        driver.dispose();

        //Destroy OpenGL
        for (int texture : hi.depthTextures.values()) {
            glDeleteTextures(texture);
        }
        glDeleteFramebuffers(hi.swapchainFramebuffer);
        glDeleteBuffers(hi.cubeVertexBuffer);
        glDeleteBuffers(hi.cubeIndexBuffer);
        glDeleteBuffers(hi.quadVertexBuffer);
        glDeleteVertexArrays(hi.cubeVAO);
        glDeleteVertexArrays(hi.quadVAO);
        glDeleteProgram(hi.screenShader);
        glDeleteProgram(hi.textureShader);
        glDeleteProgram(hi.colorShader);

        glfwTerminate();
    }

    public void initializeAndBindOpenGL() {
        try (MemoryStack stack = stackPush()) {
            var graphicsRequirements = driver.getXrGraphicsRequirements(stack);
            //Init glfw
            if (!glfwInit()) {
                throw new IllegalStateException("Failed to initialize GLFW.");
            }

            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 5);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_DOUBLEBUFFER, GL_FALSE);
            window = glfwCreateWindow(640, 480, "Hello World", NULL, NULL);
            glfwMakeContextCurrent(window);
            GL.createCapabilities();

            // Check if OpenGL version is supported by OpenXR runtime
            int actualMajorVersion = glGetInteger(GL_MAJOR_VERSION);
            int actualMinorVersion = glGetInteger(GL_MINOR_VERSION);

            int minMajorVersion = XR_VERSION_MAJOR(graphicsRequirements.minApiVersionSupported());
            int minMinorVersion = XR_VERSION_MINOR(graphicsRequirements.minApiVersionSupported());

            int maxMajorVersion = XR_VERSION_MAJOR(graphicsRequirements.maxApiVersionSupported());
            int maxMinorVersion = XR_VERSION_MINOR(graphicsRequirements.maxApiVersionSupported());

            if (minMajorVersion > actualMajorVersion || (minMajorVersion == actualMajorVersion && minMinorVersion > actualMinorVersion)) {
                throw new IllegalStateException(
                        "The OpenXR runtime supports only OpenGL " + minMajorVersion + "." + minMinorVersion +
                                " and later, but we got OpenGL " + actualMajorVersion + "." + actualMinorVersion
                );
            }

            if (actualMajorVersion > maxMajorVersion || (actualMajorVersion == maxMajorVersion && actualMinorVersion > maxMinorVersion)) {
                throw new IllegalStateException(
                        "The OpenXR runtime supports only OpenGL " + maxMajorVersion + "." + minMajorVersion +
                                " and earlier, but we got OpenGL " + actualMajorVersion + "." + actualMinorVersion
                );
            }
        }
    }

    private void createOpenGLResources() {
        swapchainFramebuffer = glGenFramebuffers();
        depthTextures = new HashMap<>(0);
        for (Swapchain swapchain : driver.swapchains) {
            for (XrSwapchainImageOpenGLKHR swapchainImage : swapchain.images) {
                int texture = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, texture);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, swapchain.width, swapchain.height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
                depthTextures.put(swapchainImage, texture);
            }
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        textureShader = ShadersGL.createShaderProgram(ShadersGL.texVertShader, ShadersGL.texFragShader);
        colorShader = ShadersGL.createShaderProgram(ShadersGL.colVertShader, ShadersGL.colFragShader);
        screenShader = ShadersGL.createShaderProgram(ShadersGL.screenVertShader, ShadersGL.texFragShader);

        {
            cubeVertexBuffer = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, cubeVertexBuffer);
            glBufferData(GL_ARRAY_BUFFER, Geometry.cubeVertices, GL_STATIC_DRAW);

            cubeIndexBuffer = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cubeIndexBuffer);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, Geometry.cubeIndices, GL_STATIC_DRAW);

            cubeVAO = glGenVertexArrays();
            glBindVertexArray(cubeVAO);
            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 4 * 3 * 2, 0);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 24, 12);
        }
        {
            quadVAO = glGenVertexArrays();
            quadVertexBuffer = glGenBuffers();
            glBindVertexArray(quadVAO);
            glBindBuffer(GL_ARRAY_BUFFER, quadVertexBuffer);
            glBufferData(GL_ARRAY_BUFFER, Geometry.quadVertices, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 4, 0);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * 4, 2 * 4);
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void renderFrameOpenXR() {
        try (MemoryStack stack = stackPush()) {
            XrFrameState frameState = XrFrameState.calloc(stack)
                    .type$Default();

            driver.check(xrWaitFrame(
                    driver.xrSession,
                    XrFrameWaitInfo.calloc(stack)
                            .type$Default(),
                    frameState
            ));

            driver.check(xrBeginFrame(
                    driver.xrSession,
                    XrFrameBeginInfo.calloc(stack)
                            .type$Default()
            ));

            XrCompositionLayerProjection layerProjection = XrCompositionLayerProjection.calloc(stack)
                    .type$Default();

            PointerBuffer layers = stack.callocPointer(1);
            boolean didRender = false;

            if (frameState.shouldRender()) {
                if (renderLayerOpenXR(stack, frameState.predictedDisplayTime(), layerProjection)) {
                    layers.put(0, layerProjection.address());
                    didRender = true;
                } else {
                    System.out.println("Didn't render");
                }
            } else {
                System.out.println("Shouldn't render");
            }

            driver.check(xrEndFrame(
                    driver.xrSession,
                    XrFrameEndInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .displayTime(frameState.predictedDisplayTime())
                            .environmentBlendMode(XR_ENVIRONMENT_BLEND_MODE_OPAQUE)
                            .layers(didRender ? layers : null)
                            .layerCount(didRender ? layers.remaining() : 0)
            ));
        }
    }

    private boolean renderLayerOpenXR(MemoryStack stack, long predictedDisplayTime, XrCompositionLayerProjection layer) {
        XrViewState viewState = XrViewState.calloc(stack)
                .type$Default();

        IntBuffer pi = stack.mallocInt(1);
        driver.check(xrLocateViews(
                driver.xrSession,
                XrViewLocateInfo.malloc(stack)
                        .type$Default()
                        .next(NULL)
                        .viewConfigurationType(driver.viewConfigType)
                        .displayTime(predictedDisplayTime)
                        .space(driver.xrAppSpace),
                viewState,
                pi,
                driver.views
        ));

        if ((viewState.viewStateFlags() & XR_VIEW_STATE_POSITION_VALID_BIT) == 0 ||
                (viewState.viewStateFlags() & XR_VIEW_STATE_ORIENTATION_VALID_BIT) == 0) {
            return false;  // There is no valid tracking poses for the views.
        }

        int viewCountOutput = pi.get(0);
        assert (viewCountOutput == driver.views.capacity());
        assert (viewCountOutput == driver.viewConfigs.capacity());
        assert (viewCountOutput == driver.swapchains.length);

        XrCompositionLayerProjectionView.Buffer projectionLayerViews = XrHelper.fill(
                XrCompositionLayerProjectionView.calloc(viewCountOutput, stack), // Use calloc() since malloc() messes up the `next` field
                XrCompositionLayerProjectionView.TYPE,
                XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW
        );

        // Render view to the appropriate part of the swapchain image.
        for (int viewIndex = 0; viewIndex < viewCountOutput; viewIndex++) {
            // Each view has a separate swapchain which is acquired, rendered to, and released.
            Swapchain viewSwapchain = driver.swapchains[viewIndex];

            driver.check(xrAcquireSwapchainImage(
                    viewSwapchain.handle,
                    XrSwapchainImageAcquireInfo.calloc(stack)
                            .type$Default(),
                    pi
            ));
            int swapchainImageIndex = pi.get(0);

            driver.check(xrWaitSwapchainImage(
                    viewSwapchain.handle,
                    XrSwapchainImageWaitInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .timeout(XR_INFINITE_DURATION)
            ));

            XrCompositionLayerProjectionView projectionLayerView = projectionLayerViews.get(viewIndex)
                    .pose(driver.views.get(viewIndex).pose())
                    .fov(driver.views.get(viewIndex).fov())
                    .subImage(si -> si
                            .swapchain(viewSwapchain.handle)
                            .imageRect(rect -> rect
                                    .offset(offset -> offset
                                            .x(0)
                                            .y(0))
                                    .extent(extent -> extent
                                            .width(viewSwapchain.width)
                                            .height(viewSwapchain.height)
                                    )));

            OpenGLRenderView(projectionLayerView, viewSwapchain.images.get(swapchainImageIndex), viewIndex);

            driver.check(xrReleaseSwapchainImage(
                    viewSwapchain.handle,
                    XrSwapchainImageReleaseInfo.calloc(stack)
                            .type$Default()
            ));
        }

        layer.space(driver.xrAppSpace);
        layer.views(projectionLayerViews);
        return true;
    }

    private static Matrix4f modelviewMatrix = new Matrix4f();
    private static Matrix4f projectionMatrix = new Matrix4f();
    private static Matrix4f viewMatrix = new Matrix4f();

    private static FloatBuffer mvpMatrix = BufferUtils.createFloatBuffer(16);

    private void OpenGLRenderView(XrCompositionLayerProjectionView layerView, XrSwapchainImageOpenGLKHR swapchainImage, int viewIndex) {
        glBindFramebuffer(GL_FRAMEBUFFER, swapchainFramebuffer);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, swapchainImage.image(), 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextures.get(swapchainImage), 0);

        XrRect2Di imageRect = layerView.subImage().imageRect();
        glViewport(
                imageRect.offset().x(),
                imageRect.offset().y(),
                imageRect.extent().width(),
                imageRect.extent().height()
        );

        float[] DarkSlateGray = { 0.184313729f, 0.309803933f, 0.309803933f };
        glClearColor(DarkSlateGray[0], DarkSlateGray[1], DarkSlateGray[2], 1.0f);
        glClearDepth(1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        glFrontFace(GL_CW);
        glCullFace(GL_BACK);
        glEnable(GL_DEPTH_TEST);

        XrPosef pose = layerView.pose();
        XrVector3f pos = pose.position$();
        XrQuaternionf orientation = pose.orientation();
        try (MemoryStack stack = stackPush()) {
            projectionMatrix.set(XrHelper.createProjectionMatrixBuffer(stack, layerView.fov(), 0.1f, 100f, false));
        }
        viewMatrix.translationRotateScaleInvert(
                pos.x(), pos.y(), pos.z(),
                orientation.x(), orientation.y(), orientation.z(), orientation.w(),
                1, 1, 1
        );

        glDisable(GL_CULL_FACE); // Disable back-face culling so we can see the inside of the world-space cube and backside of the plane

        {   // Rotating plane
            modelviewMatrix.translation(0, 0, -3).rotate((float) -glfwGetTime(), 1, 0, 0);
            glUseProgram(colorShader);
            glUniformMatrix4fv(glGetUniformLocation(colorShader, "projection"), false, projectionMatrix.get(mvpMatrix));
            glUniformMatrix4fv(glGetUniformLocation(colorShader, "view"), false, viewMatrix.get(mvpMatrix));
            glUniformMatrix4fv(glGetUniformLocation(colorShader, "model"), false, modelviewMatrix.get(mvpMatrix));
            glBindVertexArray(quadVAO);
            glDrawArrays(GL_TRIANGLES, 0, 6);
        }

        {   // World-space cube
            modelviewMatrix.identity().scale(10);
            glUniformMatrix4fv(glGetUniformLocation(colorShader, "model"), false, modelviewMatrix.get(mvpMatrix));
            glBindVertexArray(cubeVAO);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cubeIndexBuffer);
            glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_SHORT, 0);
        }

        glEnable(GL_CULL_FACE);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        if (viewIndex == 0 || true) { // The view to the GLFW window
            try (MemoryStack stack = stackPush()) {
                Swapchain swapchain = driver.swapchains[viewIndex];

                IntBuffer ww = stack.mallocInt(1);
                IntBuffer wh = stack.mallocInt(1);
                glfwGetWindowSize(window, ww, wh);

                int wh2 = (int) (((float) swapchain.height / swapchain.width) * ww.get(0));
                if (wh2 > wh.get(0)) {
                    int ww2 = (int) (((float) swapchain.width / swapchain.height) * wh.get(0));
                    glViewport(ww2 * viewIndex, 0, ww2, wh.get(0));
                } else {
                    glViewport(ww.get(0) * viewIndex, 0, ww.get(0), wh2);
                }
            }
            glFrontFace(GL_CCW);
            glUseProgram(screenShader);
            glBindVertexArray(quadVAO);
            glDisable(GL_DEPTH_TEST);
            glBindTexture(GL_TEXTURE_2D, swapchainImage.image());
            glDrawArrays(GL_TRIANGLES, 0, 6);
            if (viewIndex == driver.swapchains.length - 1) {
                glFlush();
            }
        }
    }

    private static class Geometry {

        static float[] cubeVertices = {
                -0.5f, 0.5f, -0.5f, 0.25f, 0f, 0f, -0.5f, -0.5f, 0.5f, 0.25f, 0f, 0f, -0.5f, -0.5f, -0.5f, 0.25f, 0f, 0f, -0.5f, 0.5f, -0.5f, 0.25f, 0f, 0f, -0.5f, 0.5f, 0.5f, 0.25f, 0f, 0f, -0.5f, -0.5f, 0.5f, 0.25f, 0f, 0f,
                0.5f, 0.5f, -0.5f, 1f, 0f, 0f, 0.5f, -0.5f, -0.5f, 1f, 0f, 0f, 0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0.5f, 0.5f, -0.5f, 1f, 0f, 0f, 0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0.5f, 0.5f, 0.5f, 1f, 0f, 0f,
                -0.5f, -0.5f, -0.5f, 0f, 0.25f, 0f, -0.5f, -0.5f, 0.5f, 0f, 0.25f, 0f, 0.5f, -0.5f, 0.5f, 0f, 0.25f, 0f, -0.5f, -0.5f, -0.5f, 0f, 0.25f, 0f, 0.5f, -0.5f, 0.5f, 0f, 0.25f, 0f, 0.5f, -0.5f, -0.5f, 0f, 0.25f, 0f,
                -0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 0.5f, 0.5f, 0.5f, 0f, 1f, 0f, -0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 0.5f, 0.5f, 0.5f, 0f, 1f, 0f, -0.5f, 0.5f, 0.5f, 0f, 1f, 0f,
                -0.5f, -0.5f, -0.5f, 0f, 0f, 0.25f, 0.5f, -0.5f, -0.5f, 0f, 0f, 0.25f, 0.5f, 0.5f, -0.5f, 0f, 0f, 0.25f, -0.5f, -0.5f, -0.5f, 0f, 0f, 0.25f, 0.5f, 0.5f, -0.5f, 0f, 0f, 0.25f, -0.5f, 0.5f, -0.5f, 0f, 0f, 0.25f,
                -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, -0.5f, 0.5f, 0.5f, 0f, 0f, 1f, 0.5f, 0.5f, 0.5f, 0f, 0f, 1f, -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0.5f, 0.5f, 0.5f, 0f, 0f, 1f, 0.5f, -0.5f, 0.5f, 0f, 0f, 1f
        };

        // Winding order is clockwise. Each side uses a different color.
        static short[] cubeIndices = {
                0, 1, 2, 3, 4, 5,
                6, 7, 8, 9, 10, 11,
                12, 13, 14, 15, 16, 17,
                18, 19, 20, 21, 22, 23,
                24, 25, 26, 27, 28, 29,
                30, 31, 32, 33, 34, 35,
        };

        static float[] quadVertices = { // vertex attributes for a quad that fills the entire screen in Normalized Device Coordinates.
                // positions   // texCoords
                -1.0f, 1.0f, 0.0f, 1.0f,
                -1.0f, -1.0f, 0.0f, 0.0f,
                1.0f, -1.0f, 1.0f, 0.0f,

                -1.0f, 1.0f, 0.0f, 1.0f,
                1.0f, -1.0f, 1.0f, 0.0f,
                1.0f, 1.0f, 1.0f, 1.0f
        };
    }

}
