package gaiasky.vr.openxr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.desktop.util.HiOpenXRGL.XrResultException;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.vr.openvr.VRContext.VRDevice;
import gaiasky.vr.openvr.VRContext.VRDeviceType;
import gaiasky.vr.openvr.VRDeviceListener;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL31;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.opengl.GL11.GL_RGB10_A2;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL21.GL_SRGB8_ALPHA8;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.openxr.EXTDebugUtils.*;
import static org.lwjgl.openxr.KHROpenGLEnable.*;
import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.MemoryStack.stackMalloc;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

public class OpenXRDriver implements Disposable {
    private static final Log logger = Logger.getLogger(OpenXRDriver.class);

    private long systemID;
    private boolean missingXrDebug;
    private XrInstance xrInstance;
    private XrSession xrSession;
    private XrDebugUtilsMessengerEXT xrDebugMessenger;
    private XrSpace xrAppSpace;  //The real world space in which the program runs
    private long glColorFormat;
    private XrView.Buffer views;       //Each view reperesents an eye in the headset with views[0] being left and views[1] being right
    private XrActionSet gameplayActionSet;
    private XrAction leftPose, rightPose, leftHaptic, rightHaptic, buttonA, buttonB, buttonX, buttonY, axisThumbstickX, axisThumbstickY, axisTrigger, buttonThumbstick, buttonTrigger;
    private XrSpace leftPoseSpace, rightPoseSpace;
    private Swapchain[] swapchains;  //One swapchain per view
    private XrViewConfigurationView.Buffer viewConfigs;
    private final int viewConfigType = XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO;

    //Runtime
    XrEventDataBuffer eventDataBuffer;
    int sessionState;
    boolean sessionRunning;

    static class Swapchain {
        XrSwapchain handle;
        int width;
        int height;
        XrSwapchainImageOpenGLKHR.Buffer images;
    }

    /**
     * Runs the initialization sequence of OpenXR by creating an instance, a session,
     * a reference space and initializing the swapchain.
     */
    public void initializeOpenXR() {
        createOpenXRInstance();
        initializeXRSystem();
        initializeOpenXRSession();
        createOpenXRReferenceSpace();
        createOpenXRSwapchains();
        initializeInput();
    }

    /**
     * Creates the OpenXR instance object.
     * First method to call in the OpenXR initialization sequence.
     */
    public void createOpenXRInstance() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pi = stack.mallocInt(1);

            boolean hasCoreValidationLayer = false;
            check(xrEnumerateApiLayerProperties(pi, null));
            int numLayers = pi.get(0);

            XrApiLayerProperties.Buffer pLayers = XrHelper.prepareApiLayerProperties(stack, numLayers);
            check(xrEnumerateApiLayerProperties(pi, pLayers));
            for (int index = 0; index < numLayers; index++) {
                XrApiLayerProperties layer = pLayers.get(index);

                String layerName = layer.layerNameString();
                logger.info("OpenXR layer available: " + layerName);
                if (layerName.equals("XR_APILAYER_LUNARG_core_validation")) {
                    hasCoreValidationLayer = true;
                }
            }
            logger.info(numLayers + " XR layers are available:");

            check(xrEnumerateInstanceExtensionProperties((ByteBuffer) null, pi, null));
            int numExtensions = pi.get(0);

            XrExtensionProperties.Buffer properties = XrHelper.prepareExtensionProperties(stack, numExtensions);

            check(xrEnumerateInstanceExtensionProperties((ByteBuffer) null, pi, properties));

            PointerBuffer extensions = stack.mallocPointer(2);

            boolean missingOpenGL = true;
            missingXrDebug = true;
            for (int i = 0; i < numExtensions; i++) {
                XrExtensionProperties prop = properties.get(i);

                String extensionName = prop.extensionNameString();
                logger.info("OpenXR extension loaded: " + extensionName);
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
            logger.info("OpenXR loaded with " + numExtensions + " extensions");

            if (missingOpenGL) {
                throw new IllegalStateException("OpenXR library does not provide required extension: " + XR_KHR_OPENGL_ENABLE_EXTENSION_NAME);
            }

            PointerBuffer wantedLayers;
            if (hasCoreValidationLayer) {
                wantedLayers = stack.callocPointer(1);
                wantedLayers.put(0, stack.UTF8("XR_APILAYER_LUNARG_core_validation"));
                logger.info("Enabling XR core validation");
            } else {
                wantedLayers = null;
            }

            XrInstanceCreateInfo createInfo = XrInstanceCreateInfo.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .createFlags(0)
                    .applicationInfo(XrApplicationInfo.calloc(stack)
                            .applicationName(stack.UTF8(Settings.getApplicationName(true)))
                            .apiVersion(XR_CURRENT_API_VERSION))
                    .enabledApiLayerNames(wantedLayers)
                    .enabledExtensionNames(extensions);

            PointerBuffer pp = stack.mallocPointer(1);
            check(xrCreateInstance(createInfo, pp));
            xrInstance = new XrInstance(pp.get(0), createInfo);
        }
    }

    /**
     * Creates the system with which to later create a session.
     * Second method to call in the OpenXR initialization sequence.
     */
    public void initializeXRSystem() {
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
            logger.info("Headset found with System ID: " + systemID);
        }
    }

    /**
     * Creates the XrSession object.
     * Third method to call in the OpenXR initialization sequence.
     */
    public void initializeOpenXRSession() {
        try (MemoryStack stack = stackPush()) {
            //Initialize OpenXR's OpenGL compatability
            XrGraphicsRequirementsOpenGLKHR graphicsRequirements = XrGraphicsRequirementsOpenGLKHR.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .minApiVersionSupported(0)
                    .maxApiVersionSupported(0);

            xrGetOpenGLGraphicsRequirementsKHR(xrInstance, systemID, graphicsRequirements);

            //Bind the OpenGL context to the OpenXR instance and create the session
            Lwjgl3Graphics graphics = (Lwjgl3Graphics) Gdx.graphics;
            Struct graphicsBinding = XrHelper.createGraphicsBindingOpenGL(stack, graphics.getWindow().getWindowHandle());

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
                            logger.info("XR Debug Utils: " + callbackData.messageString());
                            return 0;
                        });

                logger.info("Enabling OpenXR debug utils");
                check(xrCreateDebugUtilsMessengerEXT(xrInstance, ciDebugUtils, pp));
                xrDebugMessenger = new XrDebugUtilsMessengerEXT(pp.get(0), xrInstance);
            }
        }
    }

    /**
     * Creates an XrSpace from the previously created session.
     * Fourth method to call in the OpenXR initialization sequence.
     */
    public void createOpenXRReferenceSpace() {
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

    /**
     * Initializes the XR swapchains.
     * Fifth method to call in the OpenXR initialization sequence.
     */
    public void createOpenXRSwapchains() {
        try (MemoryStack stack = stackPush()) {
            XrSystemProperties systemProperties = XrSystemProperties.calloc(stack);
            memPutInt(systemProperties.address(), XR_TYPE_SYSTEM_PROPERTIES);
            check(xrGetSystemProperties(xrInstance, systemID, systemProperties));

            logger.info("Headset name:" + memUTF8(memAddress(systemProperties.systemName())) + " vendor:" + systemProperties.vendorId());

            XrSystemTrackingProperties trackingProperties = systemProperties.trackingProperties();
            logger.info("Headset orientationTracking:" + trackingProperties.orientationTracking() + " positionTracking:" + trackingProperties.positionTracking());

            XrSystemGraphicsProperties graphicsProperties = systemProperties.graphicsProperties();
            logger.info("Headset MaxWidth:" + graphicsProperties.maxSwapchainImageWidth() + " MaxHeight:" + graphicsProperties.maxSwapchainImageHeight() + " MaxLayerCount:" + graphicsProperties.maxLayerCount());

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

    public boolean pollEvents() {
        XrEventDataBaseHeader event = readNextOpenXREvent();
        if (event == null) {
            return false;
        }

        do {
            switch (event.type()) {
            case XR_TYPE_EVENT_DATA_INSTANCE_LOSS_PENDING: {
                XrEventDataInstanceLossPending instanceLossPending = XrEventDataInstanceLossPending.create(event.address());
                logger.error("XrEventDataInstanceLossPending by " + instanceLossPending.lossTime());
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
                logger.info("Ignoring event type " + event.type());
                break;
            }
            }
            event = readNextOpenXREvent();
        }
        while (event != null);

        return false;
    }

    boolean OpenXRHandleSessionStateChangedEvent(XrEventDataSessionStateChanged stateChangedEvent) {
        int oldState = sessionState;
        sessionState = stateChangedEvent.state();

        logger.debug("XrEventDataSessionStateChanged: state " + oldState + "->" + sessionState + " session=" + stateChangedEvent.session() + " time=" + stateChangedEvent.time());

        if ((stateChangedEvent.session() != NULL) && (stateChangedEvent.session() != xrSession.address())) {
            logger.error("XrEventDataSessionStateChanged for unknown session");
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
        case XR_SESSION_STATE_EXITING -> {
            // Do not attempt to restart because user closed this session.
            //*requestRestart = false;
            return true;
        }
        case XR_SESSION_STATE_LOSS_PENDING -> {
            // Poll for a new instance.
            //*requestRestart = true;
            return true;
        }
        default -> {
            return false;
        }
        }
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
                logger.debug(dataEventsLost.lostEventCount() + " events lost");
            }
            return XrEventDataBaseHeader.create(eventDataBuffer.address());
        }
        if (result == XR_EVENT_UNAVAILABLE) {
            return null;
        }
        throw new IllegalStateException(String.format("[XrResult failure %d in xrPollEvent]", result));
    }

    public void dispose() {
        // Destroy OpenXR
        eventDataBuffer.free();
        views.free();
        viewConfigs.free();
        for (Swapchain swapchain : swapchains) {
            xrDestroySwapchain(swapchain.handle);
            swapchain.images.free();
        }

        xrDestroySpace(xrAppSpace);
        if (xrDebugMessenger != null) {
            xrDestroyDebugUtilsMessengerEXT(xrDebugMessenger);
        }
        xrDestroySession(xrSession);
        xrDestroyInstance(xrInstance);

        destroyInput();
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

    public void addListener(VRDeviceListener listener) {
    }

    public void removeListener(VRDeviceListener listener) {
    }

    public int getWidth() {
        return swapchains[0].width;
    }

    public int getHeight() {
        return swapchains[0].height;
    }

    public VRDevice getDeviceByType(VRDeviceType type) {
        return null;
    }

    public Array<VRDevice> getDevicesByType(VRDeviceType type) {
        return new Array<>();
    }

    public Array<VRDevice> getDevices() {
        return new Array<>();
    }
}
