package gaiasky.desktop.util;

/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */

import gaiasky.vr.openxr.ShadersGL;
import gaiasky.vr.openxr.XrHelper;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL31;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.openxr.EXTDebugUtils.*;
import static org.lwjgl.openxr.KHROpenGLEnable.*;
import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.MemoryStack.stackMalloc;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Slightly tweaked combination of
 * https://github.com/maluoi/OpenXRSamples/blob/master/SingleFileExample
 * and
 * https://github.com/ReliaSolve/OpenXR-OpenGL-Example
 * Missing actions (xr input) and can only run on windows.
 * Requires a stereo headset and an install of the OpenXR runtime to run.
 */
public class HiOpenXRGL {

    long window;

    //XR globals
    //Init
    XrInstance xrInstance;
    long systemID;
    XrSession xrSession;
    boolean missingXrDebug;
    XrDebugUtilsMessengerEXT xrDebugMessenger;
    XrSpace xrAppSpace;  //The real world space in which the program runs
    long glColorFormat;
    XrView.Buffer views;       //Each view reperesents an eye in the headset with views[0] being left and views[1] being right
    Swapchain[] swapchains;  //One swapchain per view
    XrViewConfigurationView.Buffer viewConfigs;
    int viewConfigType = XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO;

    private XrActionSet gameplayActionSet;
    private XrAction leftPose, rightPose, leftHaptic, rightHaptic, buttonA, buttonB, buttonX, buttonY, axisThumbstickX, axisThumbstickY, axisTrigger, buttonThumbstick, buttonTrigger;
    private XrSpace leftPoseSpace, rightPoseSpace;

    //Runtime
    XrEventDataBuffer eventDataBuffer;
    int sessionState;
    boolean sessionRunning;

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

    static class Swapchain {
        XrSwapchain handle;
        int width;
        int height;
        XrSwapchainImageOpenGLKHR.Buffer images;
    }

    public static void main(String[] args) throws InterruptedException {
        HiOpenXRGL helloOpenXR = new HiOpenXRGL();

        helloOpenXR.createOpenXRInstance();
        helloOpenXR.initializeOpenXRSystem();
        helloOpenXR.initializeAndBindOpenGL();
        helloOpenXR.createXRReferenceSpace();
        helloOpenXR.createXRSwapchains();
        helloOpenXR.createOpenGLResources();
        helloOpenXR.initializeInput();

        helloOpenXR.eventDataBuffer = XrEventDataBuffer.calloc()
                .type$Default();

        while (!helloOpenXR.pollEvents() && !glfwWindowShouldClose(helloOpenXR.window)) {
            if (helloOpenXR.sessionRunning) {
                helloOpenXR.renderFrameOpenXR();
            } else {
                // Throttle loop since xrWaitFrame won't be called.
                Thread.sleep(250);
            }
        }

        // Wait until idle
        glFinish();

        // Destroy OpenXR
        helloOpenXR.destroyInput();
        helloOpenXR.eventDataBuffer.free();
        helloOpenXR.views.free();
        helloOpenXR.viewConfigs.free();
        for (Swapchain swapchain : helloOpenXR.swapchains) {
            xrDestroySwapchain(swapchain.handle);
            swapchain.images.free();
        }

        xrDestroySpace(helloOpenXR.xrAppSpace);
        if (helloOpenXR.xrDebugMessenger != null) {
            xrDestroyDebugUtilsMessengerEXT(helloOpenXR.xrDebugMessenger);
        }
        xrDestroySession(helloOpenXR.xrSession);
        xrDestroyInstance(helloOpenXR.xrInstance);

        //Destroy OpenGL
        for (int texture : helloOpenXR.depthTextures.values()) {
            glDeleteTextures(texture);
        }
        glDeleteFramebuffers(helloOpenXR.swapchainFramebuffer);
        glDeleteBuffers(helloOpenXR.cubeVertexBuffer);
        glDeleteBuffers(helloOpenXR.cubeIndexBuffer);
        glDeleteBuffers(helloOpenXR.quadVertexBuffer);
        glDeleteVertexArrays(helloOpenXR.cubeVAO);
        glDeleteVertexArrays(helloOpenXR.quadVAO);
        glDeleteProgram(helloOpenXR.screenShader);
        glDeleteProgram(helloOpenXR.textureShader);
        glDeleteProgram(helloOpenXR.colorShader);

        glfwTerminate();
    }

    public void createOpenXRInstance() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pi = stack.mallocInt(1);

            boolean hasCoreValidationLayer = false;
            check(xrEnumerateApiLayerProperties(pi, null));
            int numLayers = pi.get(0);

            XrApiLayerProperties.Buffer pLayers = XrHelper.prepareApiLayerProperties(stack, numLayers);
            check(xrEnumerateApiLayerProperties(pi, pLayers));
            System.out.println(numLayers + " XR layers are available:");
            for (int index = 0; index < numLayers; index++) {
                XrApiLayerProperties layer = pLayers.get(index);

                String layerName = layer.layerNameString();
                System.out.println(layerName);
                if (layerName.equals("XR_APILAYER_LUNARG_core_validation")) {
                    hasCoreValidationLayer = true;
                }
            }
            System.out.println("-----------");

            check(xrEnumerateInstanceExtensionProperties((ByteBuffer) null, pi, null));
            int numExtensions = pi.get(0);

            XrExtensionProperties.Buffer properties = XrHelper.prepareExtensionProperties(stack, numExtensions);

            check(xrEnumerateInstanceExtensionProperties((ByteBuffer) null, pi, properties));

            System.out.printf("OpenXR loaded with %d extensions:%n", numExtensions);
            System.out.println("~~~~~~~~~~~~~~~~~~");

            PointerBuffer extensions = stack.mallocPointer(2);

            boolean missingOpenGL = true;
            missingXrDebug = true;
            for (int i = 0; i < numExtensions; i++) {
                XrExtensionProperties prop = properties.get(i);

                String extensionName = prop.extensionNameString();
                System.out.println(extensionName);
                if (extensionName.equals(XR_KHR_OPENGL_ENABLE_EXTENSION_NAME)) {
                    missingOpenGL = false;
                    extensions.put(prop.extensionName());
                }
                if (extensionName.equals(XR_EXT_DEBUG_UTILS_EXTENSION_NAME)) {
                    missingXrDebug = false;
                    extensions.put(prop.extensionName());
                }
            }
            extensions.flip();
            System.out.println("~~~~~~~~~~~~~~~~~~");

            if (missingOpenGL) {
                throw new IllegalStateException("OpenXR library does not provide required extension: " + XR_KHR_OPENGL_ENABLE_EXTENSION_NAME);
            }

            PointerBuffer wantedLayers;
            if (hasCoreValidationLayer) {
                wantedLayers = stack.callocPointer(1);
                wantedLayers.put(0, stack.UTF8("XR_APILAYER_LUNARG_core_validation"));
                System.out.println("Enabling XR core validation");
            } else {
                wantedLayers = null;
            }

            XrInstanceCreateInfo createInfo = XrInstanceCreateInfo.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .createFlags(0)
                    .applicationInfo(XrApplicationInfo.calloc(stack)
                            .applicationName(stack.UTF8("HiOpenXR"))
                            .apiVersion(XR_CURRENT_API_VERSION))
                    .enabledApiLayerNames(wantedLayers)
                    .enabledExtensionNames(extensions);

            PointerBuffer pp = stack.mallocPointer(1);
            check(xrCreateInstance(createInfo, pp));
            xrInstance = new XrInstance(pp.get(0), createInfo);
        }
    }

    public void initializeOpenXRSystem() {
        try (MemoryStack stack = stackPush()) {
            //Get headset
            LongBuffer pl = stack.longs(0);

            check(xrGetSystem(
                    xrInstance,
                    XrSystemGetInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .formFactor(XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY),
                    pl
            ));

            systemID = pl.get(0);
            if (systemID == 0) {
                throw new IllegalStateException("No compatible headset detected");
            }
            System.out.printf("Headset found with System ID: %d\n", systemID);
        }
    }

    public void initializeAndBindOpenGL() {
        try (MemoryStack stack = stackPush()) {
            //Initialize OpenXR's OpenGL compatability
            XrGraphicsRequirementsOpenGLKHR graphicsRequirements = XrGraphicsRequirementsOpenGLKHR.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .minApiVersionSupported(0)
                    .maxApiVersionSupported(0);

            xrGetOpenGLGraphicsRequirementsKHR(xrInstance, systemID, graphicsRequirements);

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

            //Bind the OpenGL context to the OpenXR instance and create the session
            Struct graphicsBinding = XrHelper.createGraphicsBindingOpenGL(stack, window);

            PointerBuffer pp = stack.mallocPointer(1);

            check(xrCreateSession(
                    xrInstance,
                    XrSessionCreateInfo.malloc(stack)
                            .type$Default()
                            .next(graphicsBinding.address())
                            .createFlags(0)
                            .systemId(systemID),
                    pp
            ));

            xrSession = new XrSession(pp.get(0), xrInstance);

            if (!missingXrDebug) {
                XrDebugUtilsMessengerCreateInfoEXT ciDebugUtils = XrDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                        .type$Default()
                        .messageSeverities(
                                XR_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |
                                        XR_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                                        XR_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                        )
                        .messageTypes(
                                XR_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                                        XR_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                                        XR_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT |
                                        XR_DEBUG_UTILS_MESSAGE_TYPE_CONFORMANCE_BIT_EXT
                        )
                        .userCallback((messageSeverity, messageTypes, pCallbackData, userData) -> {
                            XrDebugUtilsMessengerCallbackDataEXT callbackData = XrDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                            System.out.println("XR Debug Utils: " + callbackData.messageString());
                            return 0;
                        });

                System.out.println("Enabling OpenXR debug utils");
                check(xrCreateDebugUtilsMessengerEXT(xrInstance, ciDebugUtils, pp));
                xrDebugMessenger = new XrDebugUtilsMessengerEXT(pp.get(0), xrInstance);
            }
        }
    }

    public void createXRReferenceSpace() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);

            check(xrCreateReferenceSpace(
                    xrSession,
                    XrReferenceSpaceCreateInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .referenceSpaceType(XR_REFERENCE_SPACE_TYPE_LOCAL)
                            .poseInReferenceSpace(XrPosef.malloc(stack)
                                    .orientation(XrQuaternionf.malloc(stack)
                                            .x(0)
                                            .y(0)
                                            .z(0)
                                            .w(1))
                                    .position$(XrVector3f.calloc(stack))),
                    pp
            ));

            xrAppSpace = new XrSpace(pp.get(0), xrSession);
        }
    }

    public void createXRSwapchains() {
        try (MemoryStack stack = stackPush()) {
            XrSystemProperties systemProperties = XrSystemProperties.calloc(stack);
            memPutInt(systemProperties.address(), XR_TYPE_SYSTEM_PROPERTIES);
            check(xrGetSystemProperties(xrInstance, systemID, systemProperties));

            System.out.printf("Headset name:%s vendor:%d \n",
                    memUTF8(memAddress(systemProperties.systemName())),
                    systemProperties.vendorId());

            XrSystemTrackingProperties trackingProperties = systemProperties.trackingProperties();
            System.out.printf("Headset orientationTracking:%b positionTracking:%b \n",
                    trackingProperties.orientationTracking(),
                    trackingProperties.positionTracking());

            XrSystemGraphicsProperties graphicsProperties = systemProperties.graphicsProperties();
            System.out.printf("Headset MaxWidth:%d MaxHeight:%d MaxLayerCount:%d \n",
                    graphicsProperties.maxSwapchainImageWidth(),
                    graphicsProperties.maxSwapchainImageHeight(),
                    graphicsProperties.maxLayerCount());

            IntBuffer pi = stack.mallocInt(1);

            check(xrEnumerateViewConfigurationViews(xrInstance, systemID, viewConfigType, pi, null));
            viewConfigs = XrHelper.fill(
                    XrViewConfigurationView.calloc(pi.get(0)), // Don't use malloc() because that would mess up the `next` field
                    XrViewConfigurationView.TYPE,
                    XR_TYPE_VIEW_CONFIGURATION_VIEW
            );

            check(xrEnumerateViewConfigurationViews(xrInstance, systemID, viewConfigType, pi, viewConfigs));
            int viewCountNumber = pi.get(0);

            views = XrHelper.fill(
                    XrView.calloc(viewCountNumber),
                    XrView.TYPE,
                    XR_TYPE_VIEW
            );

            if (viewCountNumber > 0) {
                check(xrEnumerateSwapchainFormats(xrSession, pi, null));
                LongBuffer swapchainFormats = stack.mallocLong(pi.get(0));
                check(xrEnumerateSwapchainFormats(xrSession, pi, swapchainFormats));

                long[] desiredSwapchainFormats = {
                        GL_RGB10_A2,
                        GL_RGBA16F,
                        GL_SRGB8_ALPHA8,
                        // The two below should only be used as a fallback, as they are linear color formats without enough bits for color
                        // depth, thus leading to banding.
                        GL_RGBA8,
                        GL31.GL_RGBA8_SNORM
                };

                out:
                for (long glFormatIter : desiredSwapchainFormats) {
                    for (int i = 0; i < swapchainFormats.limit(); i++) {
                        if (glFormatIter == swapchainFormats.get(i)) {
                            glColorFormat = glFormatIter;
                            break out;
                        }
                    }
                }

                if (glColorFormat == 0) {
                    throw new IllegalStateException("No compatable swapchain / framebuffer format availible");
                }

                swapchains = new Swapchain[viewCountNumber];
                for (int i = 0; i < viewCountNumber; i++) {
                    XrViewConfigurationView viewConfig = viewConfigs.get(i);

                    Swapchain swapchainWrapper = new Swapchain();

                    XrSwapchainCreateInfo swapchainCreateInfo = XrSwapchainCreateInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .createFlags(0)
                            .usageFlags(XR_SWAPCHAIN_USAGE_SAMPLED_BIT | XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT)
                            .format(glColorFormat)
                            .sampleCount(viewConfig.recommendedSwapchainSampleCount())
                            .width(viewConfig.recommendedImageRectWidth())
                            .height(viewConfig.recommendedImageRectHeight())
                            .faceCount(1)
                            .arraySize(1)
                            .mipCount(1);

                    PointerBuffer pp = stack.mallocPointer(1);
                    check(xrCreateSwapchain(xrSession, swapchainCreateInfo, pp));

                    swapchainWrapper.handle = new XrSwapchain(pp.get(0), xrSession);
                    swapchainWrapper.width = swapchainCreateInfo.width();
                    swapchainWrapper.height = swapchainCreateInfo.height();

                    check(xrEnumerateSwapchainImages(swapchainWrapper.handle, pi, null));
                    int imageCount = pi.get(0);

                    XrSwapchainImageOpenGLKHR.Buffer swapchainImageBuffer = XrHelper.fill(
                            XrSwapchainImageOpenGLKHR.create(imageCount),
                            XrSwapchainImageOpenGLKHR.TYPE,
                            XR_TYPE_SWAPCHAIN_IMAGE_OPENGL_KHR
                    );

                    check(xrEnumerateSwapchainImages(swapchainWrapper.handle, pi, XrSwapchainImageBaseHeader.create(swapchainImageBuffer.address(), swapchainImageBuffer.capacity())));
                    swapchainWrapper.images = swapchainImageBuffer;
                    swapchains[i] = swapchainWrapper;
                }
            }
        }
    }

    private void createOpenGLResources() {
        swapchainFramebuffer = glGenFramebuffers();
        depthTextures = new HashMap<>(0);
        for (Swapchain swapchain : swapchains) {
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

    public void initializeInput() {
        gameplayActionSet = createActionSet(xrInstance, "gameplay");

        // Haptic.
        leftHaptic = createAction(gameplayActionSet, "left-haptic", XR_ACTION_TYPE_VIBRATION_OUTPUT);
        rightHaptic = createAction(gameplayActionSet, "right-haptic", XR_ACTION_TYPE_VIBRATION_OUTPUT);
        // Poses.
        leftPose = createAction(gameplayActionSet, "left-hand", XR_ACTION_TYPE_POSE_INPUT);
        rightPose = createAction(gameplayActionSet, "right-hand", XR_ACTION_TYPE_POSE_INPUT);
        leftPoseSpace = createActionSpace(xrSession, leftPose);
        rightPoseSpace = createActionSpace(xrSession, rightPose);
        // Buttons.
        buttonA = createAction(gameplayActionSet, "a-button", XR_ACTION_TYPE_BOOLEAN_INPUT);
        buttonB = createAction(gameplayActionSet, "b-button", XR_ACTION_TYPE_BOOLEAN_INPUT);
        buttonX = createAction(gameplayActionSet, "x-button", XR_ACTION_TYPE_BOOLEAN_INPUT);
        buttonY = createAction(gameplayActionSet, "y-button", XR_ACTION_TYPE_BOOLEAN_INPUT);
        buttonThumbstick = createAction(gameplayActionSet, "thumbstick-button", XR_ACTION_TYPE_BOOLEAN_INPUT);
        buttonTrigger = createAction(gameplayActionSet, "trigger-button", XR_ACTION_TYPE_BOOLEAN_INPUT);
        // Axes.
        axisThumbstickX = createAction(gameplayActionSet, "thumbstick-x-axis", XR_ACTION_TYPE_FLOAT_INPUT);
        axisThumbstickY = createAction(gameplayActionSet, "thumbstick-y-axis", XR_ACTION_TYPE_FLOAT_INPUT);
        axisTrigger = createAction(gameplayActionSet, "trigger-axis", XR_ACTION_TYPE_FLOAT_INPUT);

        long leftHapticPath = getPath(xrInstance, "/user/hand/left/output/haptic");
        long rightHapticPath = getPath(xrInstance, "/user/hand/right/output/haptic");
        long leftPosePath = getPath(xrInstance, "/user/hand/left/input/grip/pose");
        long rightPosePath = getPath(xrInstance, "/user/hand/right/input/grip/pose");
        long buttonARightPath = getPath(xrInstance, "/user/hand/right/input/a/click");
        long buttonBRightPath = getPath(xrInstance, "/user/hand/right/input/b/click");
        long buttonALeftPath = getPath(xrInstance, "/user/hand/left/input/a/click");
        long buttonBLeftPath = getPath(xrInstance, "/user/hand/left/input/b/click");
        long buttonXPath = getPath(xrInstance, "/user/hand/left/input/x/click");
        long buttonYPath = getPath(xrInstance, "/user/hand/left/input/y/click");
        long buttonThumbstickLeftPath = getPath(xrInstance, "/user/hand/left/input/thumbstick/click");
        long buttonThumbstickRightPath = getPath(xrInstance, "/user/hand/right/input/thumbstick/click");
        long buttonTriggerLeftPath = getPath(xrInstance, "/user/hand/left/input/trigger/click");
        long buttonTriggerRightPath = getPath(xrInstance, "/user/hand/right/input/trigger/click");
        long axisThumbstickXLeftPath = getPath(xrInstance, "/user/hand/left/input/thumbstick/x");
        long axisThumbstickYLeftPath = getPath(xrInstance, "/user/hand/left/input/thumbstick/y");
        long axisThumbstickXRightPath = getPath(xrInstance, "/user/hand/right/input/thumbstick/x");
        long axisThumbstickYRightPath = getPath(xrInstance, "/user/hand/right/input/thumbstick/y");
        long axisTriggerLeftPath = getPath(xrInstance, "/user/hand/left/input/trigger/value");
        long axisTriggerRightPath = getPath(xrInstance, "/user/hand/left/input/trigger/value");

        long oculusTouchPath = getPath(xrInstance, "/interaction_profiles/oculus/touch_controller");
        long indexControllerPath = getPath(xrInstance, "/interaction_profiles/valve/index_controller");

        // OCULUS TOUCH
        try (MemoryStack stack = stackPush()) {
            XrActionSuggestedBinding.Buffer suggestedBindings = XrHelper.prepareActionSuggestedBindings(stack, 16);
            suggestedBindings
                    .action(leftHaptic).binding(leftHapticPath).position(1)
                    .action(rightHaptic).binding(rightHapticPath).position(2)
                    .action(leftPose).binding(leftPosePath).position(3)
                    .action(rightPose).binding(rightPosePath).position(4)
                    .action(buttonA).binding(buttonARightPath).position(5)
                    .action(buttonB).binding(buttonBRightPath).position(6)
                    .action(buttonA).binding(buttonXPath).position(7)
                    .action(buttonB).binding(buttonYPath).position(8)
                    .action(buttonThumbstick).binding(buttonThumbstickLeftPath).position(9)
                    .action(buttonThumbstick).binding(buttonThumbstickRightPath).position(10)

                    .action(axisThumbstickX).binding(axisThumbstickXLeftPath).position(11)
                    .action(axisThumbstickY).binding(axisThumbstickYLeftPath).position(12)
                    .action(axisThumbstickX).binding(axisThumbstickXRightPath).position(13)
                    .action(axisThumbstickY).binding(axisThumbstickYRightPath).position(14)
                    .action(axisTrigger).binding(axisTriggerLeftPath).position(15)
                    .action(axisTrigger).binding(axisTriggerRightPath).position(16);

            XrInteractionProfileSuggestedBinding suggestedBinding = XrInteractionProfileSuggestedBinding.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .interactionProfile(oculusTouchPath)
                    .suggestedBindings(suggestedBindings);
            check(xrSuggestInteractionProfileBindings(xrInstance, suggestedBinding));
        }

        // VALVE INDEX
        try (MemoryStack stack = stackPush()) {
            XrActionSuggestedBinding.Buffer suggestedBindings = XrHelper.prepareActionSuggestedBindings(stack, 18);
            suggestedBindings
                    .action(leftHaptic).binding(leftHapticPath)
                    .action(rightHaptic).binding(rightHapticPath)
                    .action(leftPose).binding(leftPosePath)
                    .action(rightPose).binding(rightPosePath)
                    .action(buttonA).binding(buttonARightPath)
                    .action(buttonB).binding(buttonBRightPath)
                    .action(buttonA).binding(buttonALeftPath)
                    .action(buttonB).binding(buttonBLeftPath)
                    .action(buttonThumbstick).binding(buttonThumbstickLeftPath)
                    .action(buttonThumbstick).binding(buttonThumbstickRightPath)
                    .action(buttonTrigger).binding(buttonTriggerLeftPath)
                    .action(buttonTrigger).binding(buttonTriggerRightPath)

                    .action(axisThumbstickX).binding(axisThumbstickXLeftPath)
                    .action(axisThumbstickY).binding(axisThumbstickYLeftPath)
                    .action(axisThumbstickX).binding(axisThumbstickXRightPath)
                    .action(axisThumbstickY).binding(axisThumbstickYRightPath)
                    .action(axisTrigger).binding(axisTriggerLeftPath)
                    .action(axisTrigger).binding(axisTriggerRightPath);
            suggestedBindings.rewind();

            XrInteractionProfileSuggestedBinding suggestedBinding = XrInteractionProfileSuggestedBinding.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .interactionProfile(indexControllerPath)
                    .suggestedBindings(suggestedBindings);
            check(xrSuggestInteractionProfileBindings(xrInstance, suggestedBinding));
        }

    }

    public void destroyInput() {
        destroyActionSpace(leftPoseSpace);
        destroyActionSpace(rightPoseSpace);

        destroyAction(leftPose);
        destroyAction(rightPose);

        destroyActionSet(gameplayActionSet);
    }

    public XrActionSet createActionSet(XrInstance instance, String name) {
        try (MemoryStack stack = stackPush()) {
            // Create action set.
            XrActionSetCreateInfo setCreateInfo = XrActionSetCreateInfo.malloc(stack)
                    .type$Default()
                    .actionSetName(stack.UTF8("gameplay"))
                    .localizedActionSetName(stack.UTF8("gameplay"))
                    .priority(0);

            PointerBuffer pp = stack.mallocPointer(1);
            check(xrCreateActionSet(instance, setCreateInfo, pp));
            return new XrActionSet(pp.get(0), instance);
        }
    }

    /**
     * Creates a new action.
     *
     * @param actionSet The action set.
     * @param name      The name of the action.
     * @param type      The action type.
     */
    public XrAction createAction(XrActionSet actionSet, String name, int type) {
        try (MemoryStack stack = stackPush()) {
            // Create action.
            XrActionCreateInfo createInfo = XrActionCreateInfo.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .actionName(stack.UTF8(name))
                    .localizedActionName(stack.UTF8(name))
                    .countSubactionPaths(0)
                    .actionType(type);

            PointerBuffer pp = stack.mallocPointer(1);
            check(xrCreateAction(actionSet, createInfo, pp));
            return new XrAction(pp.get(0), actionSet);
        }
    }

    public XrSpace createActionSpace(XrSession session, XrAction action) {
        try (MemoryStack stack = stackPush()) {
            XrActionSpaceCreateInfo createInfo = XrActionSpaceCreateInfo.malloc(stack)
                    .type$Default()
                    .poseInActionSpace(XrPosef.malloc(stack)
                            .position$(XrVector3f.calloc(stack).set(0, 0, 0))
                            .orientation(XrQuaternionf.malloc(stack)
                                    .x(0)
                                    .y(0)
                                    .z(0)
                                    .w(1)))
                    .action(action);

            PointerBuffer pp = stack.mallocPointer(1);
            check(xrCreateActionSpace(session, createInfo, pp));
            return new XrSpace(pp.get(0), session);
        }
    }

    public long getPath(XrInstance instance, String name) {
        try (MemoryStack stack = stackPush()) {
            LongBuffer path = stack.longs(0);
            check(xrStringToPath(instance, stack.UTF8(name), path));
            return path.get();
        }
    }

    public void destroyActionSet(XrActionSet actionSet) {
        xrDestroyActionSet(actionSet);
    }

    public void destroyAction(XrAction action) {
        xrDestroyAction(action);
    }

    public void destroyActionSpace(XrSpace space) {
        xrDestroySpace(space);
    }

    private boolean pollEvents() {
        glfwPollEvents();
        XrEventDataBaseHeader event = readNextOpenXREvent();
        if (event == null) {
            return false;
        }

        do {
            switch (event.type()) {
            case XR_TYPE_EVENT_DATA_INSTANCE_LOSS_PENDING: {
                XrEventDataInstanceLossPending instanceLossPending = XrEventDataInstanceLossPending.create(event.address());
                System.err.printf("XrEventDataInstanceLossPending by %d\n", instanceLossPending.lossTime());
                //*requestRestart = true;
                return true;
            }
            case XR_TYPE_EVENT_DATA_SESSION_STATE_CHANGED: {
                XrEventDataSessionStateChanged sessionStateChangedEvent = XrEventDataSessionStateChanged.create(event.address());
                return OpenXRHandleSessionStateChangedEvent(sessionStateChangedEvent/*, requestRestart*/);
            }
            case XR_TYPE_EVENT_DATA_INTERACTION_PROFILE_CHANGED:
                break;
            case XR_TYPE_EVENT_DATA_REFERENCE_SPACE_CHANGE_PENDING:
            default: {
                System.out.printf("Ignoring event type %d\n", event.type());
                break;
            }
            }
            event.close();
            event = readNextOpenXREvent();
        }
        while (event != null);

        return false;
    }

    private XrEventDataBaseHeader readNextOpenXREvent() {
        // It is sufficient to just clear the XrEventDataBuffer header to
        // XR_TYPE_EVENT_DATA_BUFFER rather than recreate it every time
        eventDataBuffer.clear();
        eventDataBuffer.type$Default();
        int result = xrPollEvent(xrInstance, eventDataBuffer);
        if (result == XR_SUCCESS) {
            if (eventDataBuffer.type() == XR_TYPE_EVENT_DATA_EVENTS_LOST) {
                XrEventDataEventsLost dataEventsLost = XrEventDataEventsLost.create(eventDataBuffer.address());
                System.out.printf("%d events lost\n", dataEventsLost.lostEventCount());
            }
            return XrEventDataBaseHeader.create(eventDataBuffer.address());
        }
        if (result == XR_EVENT_UNAVAILABLE) {
            return null;
        }
        throw new IllegalStateException(String.format("[XrResult failure %d in xrPollEvent]", result));
    }

    boolean OpenXRHandleSessionStateChangedEvent(XrEventDataSessionStateChanged stateChangedEvent) {
        int oldState = sessionState;
        sessionState = stateChangedEvent.state();

        System.out.printf("XrEventDataSessionStateChanged: state %s->%s session=%d time=%d\n", oldState, sessionState, stateChangedEvent.session(), stateChangedEvent.time());

        if ((stateChangedEvent.session() != NULL) && (stateChangedEvent.session() != xrSession.address())) {
            System.err.println("XrEventDataSessionStateChanged for unknown session");
            return false;
        }

        switch (sessionState) {
        case XR_SESSION_STATE_READY -> {
            assert (xrSession != null);
            try (MemoryStack stack = stackPush()) {
                check(xrBeginSession(
                        xrSession,
                        XrSessionBeginInfo.malloc(stack)
                                .type$Default()
                                .next(NULL)
                                .primaryViewConfigurationType(viewConfigType)
                ));
                sessionRunning = true;
                return false;
            }
        }
        case XR_SESSION_STATE_STOPPING -> {
            assert (xrSession != null);
            sessionRunning = false;
            check(xrEndSession(xrSession));
            return false;
        }
        case XR_SESSION_STATE_EXITING, XR_SESSION_STATE_LOSS_PENDING -> {
            // Do not attempt to restart because user closed this session.
            // Poll for a new instance.
            //*requestRestart = false;
            return true;
        }
        default -> {
            return false;
        }
        }
    }

    private void renderFrameOpenXR() {
        try (MemoryStack stack = stackPush()) {
            XrFrameState frameState = XrFrameState.calloc(stack)
                    .type$Default();

            check(xrWaitFrame(
                    xrSession,
                    XrFrameWaitInfo.calloc(stack)
                            .type$Default(),
                    frameState
            ));

            check(xrBeginFrame(
                    xrSession,
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

            check(xrEndFrame(
                    xrSession,
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
        check(xrLocateViews(
                xrSession,
                XrViewLocateInfo.malloc(stack)
                        .type$Default()
                        .next(NULL)
                        .viewConfigurationType(viewConfigType)
                        .displayTime(predictedDisplayTime)
                        .space(xrAppSpace),
                viewState,
                pi,
                views
        ));

        if ((viewState.viewStateFlags() & XR_VIEW_STATE_POSITION_VALID_BIT) == 0 ||
                (viewState.viewStateFlags() & XR_VIEW_STATE_ORIENTATION_VALID_BIT) == 0) {
            return false;  // There is no valid tracking poses for the views.
        }

        int viewCountOutput = pi.get(0);
        assert (viewCountOutput == views.capacity());
        assert (viewCountOutput == viewConfigs.capacity());
        assert (viewCountOutput == swapchains.length);

        XrCompositionLayerProjectionView.Buffer projectionLayerViews = XrHelper.fill(
                XrCompositionLayerProjectionView.calloc(viewCountOutput, stack), // Use calloc() since malloc() messes up the `next` field
                XrCompositionLayerProjectionView.TYPE,
                XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW
        );

        // Render view to the appropriate part of the swapchain image.
        for (int viewIndex = 0; viewIndex < viewCountOutput; viewIndex++) {
            // Each view has a separate swapchain which is acquired, rendered to, and released.
            Swapchain viewSwapchain = swapchains[viewIndex];

            check(xrAcquireSwapchainImage(
                    viewSwapchain.handle,
                    XrSwapchainImageAcquireInfo.calloc(stack)
                            .type$Default(),
                    pi
            ));
            int swapchainImageIndex = pi.get(0);

            check(xrWaitSwapchainImage(
                    viewSwapchain.handle,
                    XrSwapchainImageWaitInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .timeout(XR_INFINITE_DURATION)
            ));

            XrCompositionLayerProjectionView projectionLayerView = projectionLayerViews.get(viewIndex)
                    .pose(views.get(viewIndex).pose())
                    .fov(views.get(viewIndex).fov())
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

            check(xrReleaseSwapchainImage(
                    viewSwapchain.handle,
                    XrSwapchainImageReleaseInfo.calloc(stack)
                            .type$Default()
            ));
        }

        layer.space(xrAppSpace);
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
                Swapchain swapchain = swapchains[viewIndex];

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
            if (viewIndex == swapchains.length - 1) {
                glFlush();
            }
        }
    }

    public void check(int result) throws IllegalStateException {
        if (XR_SUCCEEDED(result)) {
            return;
        }

        if (xrInstance != null) {
            ByteBuffer str = stackMalloc(XR_MAX_RESULT_STRING_SIZE);
            if (xrResultToString(xrInstance, result, str) >= 0) {
                throw new XrResultException(memUTF8(str, memLengthNT1(str)));
            }
        }

        throw new XrResultException("XR method returned " + result);
    }

    public static class XrResultException extends RuntimeException {
        public XrResultException(String s) {
            super(s);
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
