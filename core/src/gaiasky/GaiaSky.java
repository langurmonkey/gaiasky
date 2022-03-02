/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.Timer.Task;
import gaiasky.assets.*;
import gaiasky.assets.GaiaAttitudeLoader.GaiaAttitudeLoaderParameter;
import gaiasky.assets.SGLoader.SGLoaderParameter;
import gaiasky.data.AssetBean;
import gaiasky.data.StreamingOctreeLoader;
import gaiasky.data.util.PointCloudData;
import gaiasky.desktop.util.CrashReporter;
import gaiasky.desktop.util.SysUtils;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.interafce.*;
import gaiasky.render.*;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.IPostProcessor.PostProcessBean;
import gaiasky.render.IPostProcessor.RenderType;
import gaiasky.scenegraph.*;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.screenshot.ScreenshotsManager;
import gaiasky.script.EventScriptingInterface;
import gaiasky.script.HiddenHelperUser;
import gaiasky.script.IScriptingInterface;
import gaiasky.script.ScriptingServer;
import gaiasky.util.Logger;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.ds.GaiaSkyExecutorService;
import gaiasky.util.gaia.GaiaAttitudeServer;
import gaiasky.util.gdx.contrib.postprocess.utils.PingPongBuffer;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.loader.*;
import gaiasky.util.gdx.loader.is.GzipInputStreamProvider;
import gaiasky.util.gdx.loader.is.RegularInputStreamProvider;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.shader.*;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.samp.SAMPClient;
import gaiasky.util.time.GlobalClock;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.time.RealTimeClock;
import gaiasky.util.tree.OctreeNode;
import gaiasky.vr.openvr.VRContext;
import gaiasky.vr.openvr.VRContext.VRDevice;
import gaiasky.vr.openvr.VRContext.VRDeviceType;
import gaiasky.vr.openvr.VRStatus;
import org.lwjgl.opengl.GL30;
import org.lwjgl.openvr.Texture;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VRCompositor;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The main class. Holds all the entities manages the update/draw cycle and all
 * other top-level functions of Gaia Sky.
 */
public class GaiaSky implements ApplicationListener, IObserver, IMainRenderer {
    private static final Log logger = Logger.getLogger(GaiaSky.class);

    /**
     * Current render process.
     * One of {@link #runnableInitialGui}, {@link #runnableLoadingGui} or {@link #runnableRender}.
     **/
    private Runnable renderProcess;

    /**
     * Singleton instance
     **/
    public static GaiaSky instance;

    /**
     * Window
     **/
    public Lwjgl3Window window;
    /**
     * Graphics
     **/
    public Graphics graphics;

    /**
     * The {@link VRContext} setup in {@link #createVR()}, may be null if no HMD is
     * present or SteamVR is not installed
     */
    public VRContext vrContext;

    /**
     * Loading frame buffers
     **/
    public FrameBuffer vrLoadingLeftFb, vrLoadingRightFb;
    /**
     * Loading texture
     **/
    public Texture vrLoadingLeftTex, vrLoadingRightTex;

    /**
     * Maps the VR devices to model objects
     */
    private HashMap<VRDevice, StubModel> vrDeviceToModel;

    // Asset manager
    public AssetManager assetManager;

    // Camera
    public CameraManager cameraManager;

    // Data load string
    private String dataLoadString;

    // Reference to the scene graph
    public ISceneGraph sceneGraph;
    // Scene graph renderer
    public SceneGraphRenderer sgr;
    // Main post processor
    private IPostProcessor postProcessor;

    // Start time
    private long startTime;

    // Time since the start in seconds
    private double t;

    // The frame number
    public long frames;

    // Frame buffer map
    private Map<String, FrameBuffer> frameBufferMap;

    // Registry
    private GuiRegistry guiRegistry;

    /**
     * Dynamic resolution level, the index in {@link gaiasky.util.Settings.GraphicsSettings#dynamicResolutionScale}
     * 0 - native
     * 1 - level 1
     * 2 - level 2
     */
    private int dynamicResolutionLevel = 0;
    private long lastDynamicResolutionChange = 0;

    /**
     * Provisional console logger
     */
    private ConsoleLogger consoleLogger;

    public InputMultiplexer inputMultiplexer;

    /**
     * The user interfaces
     */
    public IGui welcomeGui, welcomeGuiVR, loadingGui, loadingGuiVR, mainGui, spacecraftGui, stereoGui, debugGui, crashGui, controllerGui;

    /**
     * List of GUIs
     */
    private List<IGui> guis;

    /**
     * Time
     */
    public ITimeFrameProvider time;

    // The sprite batch to render the back buffer to screen
    private SpriteBatch renderBatch;

    /** Settings reference **/
    private Settings settings;

    /**
     * Camera recording or not?
     */
    private boolean camRecording = false;

    // Gaia Sky has finished initialization
    private boolean initialized = false;
    // Window has been created successfully
    public boolean windowCreated = false;

    /**
     * Used to wait for new frames
     */
    public final Object frameMonitor = new Object();

    /**
     * Set log level to debug
     */
    private final boolean debugMode;

    /**
     * Whether to attempt a connection to the VR HMD
     */
    private final boolean vr;

    /**
     * Headless mode
     */
    private final boolean headless;

    /**
     * Skip welcome screen if possible
     */
    private final boolean skipWelcome;

    /**
     * Forbids the creation of the scripting server
     */
    private final boolean noScripting;

    /**
     * Save state on exit
     */
    public boolean saveState = true;

    /**
     * External view with final rendered scene and no UI
     */
    public boolean externalView;

    /**
     * External UI window
     */
    public GaiaSkyView gaiaSkyView = null;

    /**
     * Global resources holder
     */
    private GlobalResources globalResources;

    /**
     * The global catalog manager
     */
    private CatalogManager catalogManager;

    /**
     * The scripting interface
     */
    private IScriptingInterface scripting;

    /**
     * The dataset updater -- sorts and updates dataset metadata
     */
    private GaiaSkyExecutorService executorService;

    /**
     * The bookmarks manager
     */
    private BookmarksManager bookmarksManager;

    /**
     * The SAMP client
     */
    private SAMPClient sampClient;

    /**
     * Runnables
     */
    private final List<Runnable> parkedRunnables;
    private final Map<String, Runnable> parkedRunnablesMap;

    /**
     * Creates an instance of Gaia Sky.
     */
    public GaiaSky() {
        this(false, false, false, false, false, false);
    }

    /**
     * Creates an instance of Gaia Sky.
     *
     * @param skipWelcome  Skips welcome screen if possible.
     * @param vr           Launch in VR mode.
     * @param externalView Open a new window with a view of the rendered scene.
     * @param headless     Launch in headless mode, without window.
     * @param debugMode    Output debug information.
     */
    public GaiaSky(final boolean skipWelcome, final boolean vr, final boolean externalView, final boolean headless, final boolean noScriptingServer, final boolean debugMode) {
        super();
        instance = this;
        this.settings = Settings.settings;

        // Flags
        this.skipWelcome = skipWelcome;
        this.vr = vr;
        this.externalView = externalView;
        this.headless = headless;
        this.noScripting = noScriptingServer;
        this.debugMode = debugMode;

        this.parkedRunnablesMap = new HashMap<>();
        this.parkedRunnables = new ArrayList<>();

        this.renderProcess = runnableInitialGui;
    }

    @Override
    public void create() {
        startTime = TimeUtils.millis();
        // Log level
        Gdx.app.setLogLevel(debugMode ? Application.LOG_DEBUG : Application.LOG_INFO);
        Logger.level = debugMode ? Logger.LoggerLevel.DEBUG : Logger.LoggerLevel.INFO;

        consoleLogger = new ConsoleLogger();

        if (debugMode)
            logger.debug("Logging level set to DEBUG");

        // Init graphics and window
        graphics = Gdx.graphics;
        window = headless ? null : ((Lwjgl3Graphics) graphics).getWindow();

        // Basic info
        logger.info(settings.version.version, I18n.txt("gui.build", settings.version.build));
        logger.info("Display mode", graphics.getWidth() + "x" + graphics.getHeight(), "Fullscreen: " + Gdx.graphics.isFullscreen());
        logger.info("Device", GL30.glGetString(GL30.GL_RENDERER));
        logger.info(I18n.txt("notif.glversion", GL30.glGetString(GL30.GL_VERSION)));
        logger.info(I18n.txt("notif.glslversion", GL30.glGetString(GL30.GL_SHADING_LANGUAGE_VERSION)));
        logger.info(I18n.txt("notif.javaversion", System.getProperty("java.version"), System.getProperty("java.vendor")));

        // Frame buffer map
        frameBufferMap = new HashMap<>();

        // Disable all kinds of input
        EventManager.publish(Event.INPUT_ENABLED_CMD, this, false);

        if (!settings.initialized) {
            logger.error(new RuntimeException(I18n.txt("notif.error", "global configuration not initialized")));
            return;
        }

        // Initialise times
        final ITimeFrameProvider clock = new GlobalClock(1, Instant.now());
        final ITimeFrameProvider real = new RealTimeClock();
        time = settings.runtime.realTime ? real : clock;
        t = 0;

        // Initialise i18n
        I18n.initialize();

        // Tooltips
        TooltipManager.getInstance().initialTime = 1f;
        TooltipManager.getInstance().hideAll();

        // Initialise asset manager
        final FileHandleResolver internalResolver = new InternalFileHandleResolver();
        final FileHandleResolver dataResolver = fileName -> settings.data.dataFileHandle(fileName);
        assetManager = new AssetManager(internalResolver);
        assetManager.setLoader(com.badlogic.gdx.graphics.Texture.class, ".pfm", new PFMTextureLoader(dataResolver));
        assetManager.setLoader(PFMData.class, new PFMDataLoader(dataResolver));
        assetManager.setLoader(ISceneGraph.class, new SGLoader(dataResolver));
        assetManager.setLoader(PointCloudData.class, new OrbitDataLoader(dataResolver));
        assetManager.setLoader(GaiaAttitudeServer.class, new GaiaAttitudeLoader(dataResolver));
        assetManager.setLoader(ExtShaderProgram.class, new ShaderProgramProvider(internalResolver, ".vertex.glsl", ".fragment.glsl"));
        assetManager.setLoader(BitmapFont.class, new BitmapFontLoader(internalResolver));
        assetManager.setLoader(AtmosphereShaderProvider.class, new AtmosphereShaderProviderLoader<>(internalResolver));
        assetManager.setLoader(GroundShaderProvider.class, new GroundShaderProviderLoader<>(internalResolver));
        assetManager.setLoader(TessellationShaderProvider.class, new TessellationShaderProviderLoader<>(internalResolver));
        assetManager.setLoader(RelativisticShaderProvider.class, new RelativisticShaderProviderLoader<>(internalResolver));
        assetManager.setLoader(IntModel.class, ".obj", new OwnObjLoader(new RegularInputStreamProvider(), internalResolver));
        assetManager.setLoader(IntModel.class, ".obj.gz", new OwnObjLoader(new GzipInputStreamProvider(), internalResolver));
        assetManager.setLoader(IntModel.class, ".g3dj", new G3dModelLoader(new JsonReader(), internalResolver));
        assetManager.setLoader(IntModel.class, ".g3db", new G3dModelLoader(new UBJsonReader(), internalResolver));
        // Remove Model loaders

        // Init global resources
        this.globalResources = new GlobalResources(assetManager);

        // Initialize screenshots manager
        new ScreenshotsManager(globalResources);

        // Catalog manager
        this.catalogManager = new CatalogManager();

        this.scripting = new EventScriptingInterface(this.assetManager, this.catalogManager);

        // Initialise master manager
        MasterManager.initialize();
        // Load slave assets
        SlaveManager.load(assetManager);

        // Initialise dataset updater
        this.executorService = new GaiaSkyExecutorService();

        // Bookmarks
        this.bookmarksManager = new BookmarksManager();

        // Location log
        LocationLogManager.initialize();

        // Init timer thread
        Timer.instance();

        // Initialise Cameras
        cameraManager = new CameraManager(assetManager, CameraMode.FOCUS_MODE, vr, globalResources);

        // Set asset manager to asset bean
        AssetBean.setAssetManager(assetManager);

        // Create vr context if possible
        final VRStatus vrStatus = createVR();
        cameraManager.updateFrustumPlanes();

        // Tooltip to 1s
        TooltipManager.getInstance().initialTime = 1f;

        // Initialise Gaia attitudes
        assetManager.load(Constants.ATTITUDE_FOLDER, GaiaAttitudeServer.class, new GaiaAttitudeLoaderParameter());

        // Initialise hidden helper user
        HiddenHelperUser.initialize();

        // Initialise gravitational waves helper
        RelativisticEffectsManager.initialize(time);

        // GUI
        guis = new ArrayList<>(3);

        // Post-processor
        postProcessor = PostProcessorFactory.instance.getPostProcessor();

        // Scene graph renderer
        sgr = new SceneGraphRenderer(vrContext, globalResources);
        sgr.initialize(assetManager);

        // Initialise scripting gateway server
        if (!noScripting)
            ScriptingServer.initialize();

        // Tell the asset manager to load all the assets
        final Set<AssetBean> assets = AssetBean.getAssets();
        for (AssetBean ab : assets) {
            ab.load(assetManager);
        }

        renderBatch = globalResources.getSpriteBatch();

        EventManager.instance.subscribe(this, Event.LOAD_DATA_CMD);

        welcomeGui = new WelcomeGui(globalResources.getSkin(), graphics, 1f / settings.program.ui.scale, skipWelcome, vrStatus);
        welcomeGui.initialize(assetManager, globalResources.getSpriteBatch());
        Gdx.input.setInputProcessor(welcomeGui.getGuiStage());

        if (settings.runtime.openVr) {
            welcomeGuiVR = new VRGui<>(WelcomeGuiVR.class, (int) (settings.graphics.backBufferResolution[0] / 2f), globalResources.getSkin(), graphics, 1f / settings.program.ui.scale);
            welcomeGuiVR.initialize(assetManager, globalResources.getSpriteBatch());
        }

        // GL clear state
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClearDepthf(1f);
    }

    /**
     * Attempt to create a VR context. This operation will only succeed if an HMD is connected
     * and detected via OpenVR
     **/
    private VRStatus createVR() {
        if (vr) {
            // Initializing the VRContext may fail if no HMD is connected or SteamVR
            // is not installed.
            try {
                //OpenVRQuery.queryOpenVr();
                settings.runtime.openVr = true;
                Constants.initialize(settings.scene.distanceScaleVr);

                vrContext = new VRContext();
                vrContext.pollEvents();

                final VRDevice hmd = vrContext.getDeviceByType(VRDeviceType.HeadMountedDisplay);
                logger.info("Initialization of VR successful");
                if (hmd == null) {
                    logger.info("HMD device is null!");
                } else {
                    logger.info("HMD device is not null: " + vrContext.getDeviceByType(VRDeviceType.HeadMountedDisplay).toString());
                }

                vrDeviceToModel = new HashMap<>();

                if (settings.graphics.resolution[0] != vrContext.getWidth()) {
                    logger.info("Warning, resizing according to VRSystem values:  [" + settings.graphics.resolution[0] + "x" + settings.graphics.resolution[1] + "] -> [" + vrContext.getWidth() + "x" + vrContext.getHeight() + "]");
                    // Do not resize the screen!
                    settings.graphics.backBufferResolution[1] = vrContext.getHeight();
                    settings.graphics.backBufferResolution[0] = vrContext.getWidth();
                    //this.resizeImmediate(vrContext.getWidth(), vrContext.getHeight(), true, true, true);
                }
                settings.graphics.vsync = false;

                graphics.setWindowedMode(settings.graphics.resolution[0], settings.graphics.resolution[1]);
                graphics.setVSync(settings.graphics.vsync);

                vrLoadingLeftFb = new FrameBuffer(Format.RGBA8888, vrContext.getWidth(), vrContext.getHeight(), true);
                vrLoadingLeftTex = org.lwjgl.openvr.Texture.create();
                vrLoadingLeftTex.set(vrLoadingLeftFb.getColorBufferTexture().getTextureObjectHandle(), VR.ETextureType_TextureType_OpenGL, VR.EColorSpace_ColorSpace_Gamma);

                vrLoadingRightFb = new FrameBuffer(Format.RGBA8888, vrContext.getWidth(), vrContext.getHeight(), true);
                vrLoadingRightTex = org.lwjgl.openvr.Texture.create();
                vrLoadingRightTex.set(vrLoadingRightFb.getColorBufferTexture().getTextureObjectHandle(), VR.ETextureType_TextureType_OpenGL, VR.EColorSpace_ColorSpace_Gamma);

                // Sprite batch for VR - uses back buffer resolution
                globalResources.setSpriteBatchVR(new SpriteBatch(500, globalResources.getSpriteShader()));
                globalResources.getSpriteBatchVR().getProjectionMatrix().setToOrtho2D(0, 0, settings.graphics.backBufferResolution[0], settings.graphics.backBufferResolution[1]);

                // Enable visibility of 'Others' if off (for VR controllers)
                if (!settings.scene.visibility.get(ComponentType.Others.name())) {
                    EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, this, "element.others", false);
                }
                return VRStatus.OK;
            } catch (Exception e) {
                // If initializing the VRContext failed
                settings.runtime.openVr = false;
                logger.error(e);
                logger.error("Initialisation of VR context failed");
                return VRStatus.ERROR_NO_CONTEXT;
            }
        } else {
            // Desktop mode
            settings.runtime.openVr = false;
            Constants.initialize(settings.scene.distanceScaleDesktop);
        }
        return VRStatus.NO_VR;
    }

    /**
     * Execute this when the models have finished loading. This sets the models
     * to their classes and removes the Loading message
     */
    private void doneLoading() {
        windowCreated = true;
        // Dispose of initial and loading GUIs
        welcomeGui.dispose();
        welcomeGui = null;

        loadingGui.dispose();
        loadingGui = null;

        // Dispose vr loading GUI
        if (settings.runtime.openVr) {
            welcomeGuiVR.dispose();
            welcomeGuiVR = null;

            loadingGuiVR.dispose();
            loadingGuiVR = null;

            vrLoadingLeftTex.clear();
            vrLoadingLeftFb.dispose();
            vrLoadingLeftTex = null;
            vrLoadingLeftFb = null;

            vrLoadingRightTex.clear();
            vrLoadingRightFb.dispose();
            vrLoadingRightTex = null;
            vrLoadingRightFb = null;
        }

        // Get attitude
        if (assetManager.isLoaded(Constants.ATTITUDE_FOLDER)) {
            GaiaAttitudeServer.instance = assetManager.get(Constants.ATTITUDE_FOLDER);
        }

        /*
         * SAMP client
         */
        sampClient = new SAMPClient(this.catalogManager);
        sampClient.initialize(globalResources.getSkin());

        /*
         * POST-PROCESSOR
         */
        postProcessor.doneLoading(assetManager);

        /*
         * GET SCENE GRAPH
         */
        if (assetManager.isLoaded(dataLoadString)) {
            sceneGraph = assetManager.get(dataLoadString);
        } else {
            throw new RuntimeException("Error loading scene graph from data load string: " + dataLoadString + ", and files: " + TextUtils.concatenate(File.pathSeparator, settings.data.dataFiles));
        }

        /*
         * SCENE GRAPH RENDERER
         */
        AbstractRenderer.initialize(sceneGraph);
        sgr.doneLoading(assetManager);
        sgr.resize(graphics.getWidth(), graphics.getHeight(), (int) Math.round(graphics.getWidth() * settings.graphics.backBufferScale), (int) Math.round(graphics.getHeight() * settings.graphics.backBufferScale));

        // First time, set assets
        final Array<SceneGraphNode> nodes = sceneGraph.getNodes();
        for (SceneGraphNode sgn : nodes) {
            sgn.doneLoading(assetManager);
        }

        // Initialise input multiplexer to handle various input processors
        // The input multiplexer
        guiRegistry = new GuiRegistry(this.globalResources.getSkin(), this.sceneGraph, this.catalogManager);
        inputMultiplexer = new InputMultiplexer();
        guiRegistry.setInputMultiplexer(inputMultiplexer);
        Gdx.input.setInputProcessor(inputMultiplexer);

        // Stop updating log list
        consoleLogger.setUseHistorical(false);

        // Init GUIs, step 2
        reinitialiseGUI2();

        // Publish visibility
        EventManager.publish(Event.VISIBILITY_OF_COMPONENTS, this, SceneGraphRenderer.visible);

        // Key bindings
        inputMultiplexer.addProcessor(new KeyboardInputController(Gdx.input));

        EventManager.publish(Event.SCENE_GRAPH_LOADED, this, sceneGraph);

        touchSceneGraph();

        // Initialise time in GUI
        EventManager.publish(Event.TIME_CHANGE_INFO, this, time.getTime());

        // Subscribe to events
        EventManager.instance.subscribe(this, Event.TOGGLE_AMBIENT_LIGHT, Event.AMBIENT_LIGHT_CMD, Event.RECORD_CAMERA_CMD, Event.CAMERA_MODE_CMD, Event.STEREOSCOPIC_CMD, Event.CUBEMAP_CMD, Event.FRAME_SIZE_UPDATE, Event.SCREENSHOT_SIZE_UPDATE, Event.PARK_RUNNABLE, Event.UNPARK_RUNNABLE, Event.SCENE_GRAPH_ADD_OBJECT_CMD, Event.SCENE_GRAPH_ADD_OBJECT_NO_POST_CMD, Event.SCENE_GRAPH_REMOVE_OBJECT_CMD, Event.HOME_CMD, Event.UI_SCALE_CMD, Event.PER_OBJECT_VISIBILITY_CMD, Event.FORCE_OBJECT_LABEL_CMD, Event.LABEL_COLOR_CMD);

        // Re-enable input
        EventManager.publish(Event.INPUT_ENABLED_CMD, this, true);

        // Set current date
        EventManager.publish(Event.TIME_CHANGE_CMD, this, Instant.now());

        // Resize GUIs to current size
        for (IGui gui : guis)
            gui.resize(graphics.getWidth(), graphics.getHeight());

        if (settings.runtime.openVr) {
            // Resize post-processors and render systems
            postRunnable(() -> resizeImmediate(vrContext.getWidth(), vrContext.getHeight(), true, false, false, false));
        }

        // Initialise frames
        frames = 0;

        // Debug info scheduler
        final Task debugTask1 = new Task() {
            @Override
            public void run() {
                // FPS
                EventManager.publish(Event.FPS_INFO, this, 1f / graphics.getDeltaTime());
                // Current session time
                EventManager.publish(Event.DEBUG_TIME, this, TimeUtils.timeSinceMillis(startTime) / 1000d);
                // Memory
                EventManager.publish(Event.DEBUG_RAM, this, MemInfo.getUsedMemory(), MemInfo.getFreeMemory(), MemInfo.getTotalMemory(), MemInfo.getMaxMemory());
                // Observed objects
                EventManager.publish(Event.DEBUG_OBJECTS, this, OctreeNode.nObjectsObserved, StreamingOctreeLoader.getNLoadedStars());
                // Observed octants
                EventManager.publish(Event.DEBUG_QUEUE, this, OctreeNode.nOctantsObserved, StreamingOctreeLoader.getLoadQueueSize());
                // VRAM
                EventManager.publish(Event.DEBUG_VRAM, this, VMemInfo.getUsedMemory(), VMemInfo.getTotalMemory());
                // Threads
                EventManager.publish(Event.DEBUG_THREADS, this, executorService.pool().getActiveCount(), executorService.pool().getPoolSize());
                // Dynamic resolution
                EventManager.publish(Event.DEBUG_DYN_RES, this, dynamicResolutionLevel, settings.graphics.dynamicResolutionScale[dynamicResolutionLevel]);
            }
        };

        final Task debugTask10 = new Task() {
            @Override
            public void run() {
                EventManager.publish(Event.SAMP_INFO, this, sampClient.getStatus());
            }
        };

        // Every second
        Timer.schedule(debugTask1, 2, 1);
        // Every 10 seconds
        Timer.schedule(debugTask10, 2, 10);

        // Start capturing locations
        final Task startCapturing = new Task() {
            @Override
            public void run() {
                LocationLogManager.instance().startCapturing();
            }
        };
        Timer.schedule(startCapturing, 1f);

        // Release notes
        guiRegistry.publishReleaseNotes();

        // Go home
        goHome();

        EventManager.publish(Event.INITIALIZED_INFO, this);
        initialized = true;
    }

    public void touchSceneGraph() {
        // Update whole tree to initialize positions
        OctreeNode.LOAD_ACTIVE = false;
        boolean timeOnBak = settings.runtime.timeOn;
        settings.runtime.timeOn = true;
        time.update(1e-5);
        // Update whole scene graph
        sceneGraph.update(time, cameraManager);
        sgr.clearLists();
        time.update(0);
        settings.runtime.timeOn = timeOnBak;
        OctreeNode.LOAD_ACTIVE = true;
    }

    /**
     * Moves the camera home. That is either the Earth, if it exists, or somewhere close to the Sun
     */
    private void goHome() {
        final IFocus homeObject = sceneGraph.findFocus(settings.scene.homeObject);
        boolean isOn = true;
        if (homeObject != null && (isOn = GaiaSky.instance.isOn(homeObject.getCt())) && !settings.program.net.slave.active) {
            // Set focus to Earth
            EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE);
            EventManager.publish(Event.FOCUS_CHANGE_CMD, this, homeObject, true);
            EventManager.publish(Event.GO_TO_OBJECT_CMD, this);
            if (settings.runtime.openVr) {
                // Free mode by default in VR
                EventManager.instance.postDelayed(Event.CAMERA_MODE_CMD, this, 1000L, CameraMode.FREE_MODE);
            }
        } else {
            // At 5 AU in Y looking towards origin (top-down look)
            EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
            EventManager.publish(Event.CAMERA_POS_CMD, this, (Object) new double[] { 0d, 5d * Constants.AU_TO_U, 0d });
            EventManager.publish(Event.CAMERA_DIR_CMD, this, (Object) new double[] { 0d, -1d, 0d });
            EventManager.publish(Event.CAMERA_UP_CMD, this, (Object) new double[] { 0d, 0d, 1d });
        }

        if (!isOn) {
            Task t = new Task() {
                @Override
                public void run() {
                    logger.info("The home object '" + settings.scene.homeObject + "' is invisible due to its type(s): " + homeObject.getCt());
                }
            };
            Timer.schedule(t, 1);
        }
    }

    /**
     * Re-initialises all the GUI (step 1)
     */
    public void reinitialiseGUI1() {
        if (guis != null && !guis.isEmpty()) {
            for (IGui gui : guis)
                gui.dispose();
            guis.clear();
        }

        mainGui = new FullGui(globalResources.getSkin(), graphics, 1f / settings.program.ui.scale, globalResources, catalogManager);
        mainGui.initialize(assetManager, globalResources.getSpriteBatch());

        debugGui = new DebugGui(globalResources.getSkin(), graphics, 1f / settings.program.ui.scale);
        debugGui.initialize(assetManager, globalResources.getSpriteBatch());

        spacecraftGui = new SpacecraftGui(globalResources.getSkin(), graphics, 1f / settings.program.ui.scale);
        spacecraftGui.initialize(assetManager, globalResources.getSpriteBatch());

        stereoGui = new StereoGui(globalResources.getSkin(), graphics, 1f / settings.program.ui.scale);
        stereoGui.initialize(assetManager, globalResources.getSpriteBatch());

        controllerGui = new ControllerGui(globalResources.getSkin(), graphics, 1f / settings.program.ui.scale);
        controllerGui.initialize(assetManager, globalResources.getSpriteBatch());

        if (guis != null) {
            guis.add(mainGui);
            guis.add(debugGui);
            guis.add(spacecraftGui);
            guis.add(stereoGui);
            guis.add(controllerGui);
        }
    }

    /**
     * Second step in GUI initialisation.
     */
    public void reinitialiseGUI2() {
        // Reinitialise registry to listen to relevant events
        if (guiRegistry != null)
            guiRegistry.dispose();
        guiRegistry = new GuiRegistry(this.globalResources.getSkin(), this.sceneGraph, this.catalogManager);
        guiRegistry.setInputMultiplexer(inputMultiplexer);

        // Unregister all current GUIs
        guiRegistry.unregisterAll();

        // Only for the Full GUI
        ((FullGui) mainGui).setSceneGraph(sceneGraph);
        mainGui.setVisibilityToggles(ComponentType.values(), SceneGraphRenderer.visible);

        for (IGui gui : guis)
            gui.doneLoading(assetManager);

        if (settings.program.modeStereo.active) {
            guiRegistry.set(stereoGui);
            guiRegistry.setPrevious(mainGui);
        } else {
            guiRegistry.set(mainGui);
            guiRegistry.setPrevious(null);
        }
        guiRegistry.registerGui(debugGui);
        guiRegistry.addProcessor(debugGui);

        guiRegistry.registerGui(controllerGui);
        guiRegistry.addProcessor(controllerGui);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        // Stop
        running.set(false);

        // Revert back-buffer resolution
        if (dynamicResolutionLevel > 0 && settings.graphics.backBufferScale == settings.graphics.dynamicResolutionScale[0]) {
            settings.graphics.backBufferScale = 1f;
            dynamicResolutionLevel = 0;
        }

        // Dispose
        if (saveState && !crashed.get()) {
            SettingsManager.persistSettings(new File(System.getProperty("properties.file")));
            if (bookmarksManager != null)
                bookmarksManager.persistBookmarks();
        }

        if (vrContext != null)
            vrContext.dispose();

        ScriptingServer.dispose();

        // Flush frames
        EventManager.publish(Event.FLUSH_FRAMES, this);

        // Dispose all
        if (guis != null)
            for (IGui gui : guis)
                gui.dispose();

        EventManager.publish(Event.DISPOSE, this);
        if (sceneGraph != null) {
            sceneGraph.dispose();
        }
        ModelCache.cache.dispose();

        // Shutdown dataset updater thread pool
        if (executorService != null) {
            executorService.shutDownThreadPool();
        }

        // Scripting
        ScriptingServer.dispose();

        // Renderer
        if (sgr != null)
            sgr.dispose();

        // Post processor
        if (postProcessor != null)
            postProcessor.dispose();

        // Dispose music manager
        MusicManager.dispose();

        // Clear temp
        try {
            Path tmp = SysUtils.getTempDir(settings.data.location);
            if (java.nio.file.Files.exists(tmp) && java.nio.file.Files.isDirectory(tmp))
                GlobalResources.deleteRecursively(tmp);
        } catch (Exception e) {
            logger.error(e, "Error deleting tmp directory");
        }
    }

    /**
     * Renders the scene
     **/
    private final Runnable runnableRender = () -> {

        // Asynchronous load of textures and resources
        assetManager.update();

        if (!settings.runtime.updatePause) {
            synchronized (frameMonitor) {
                frameMonitor.notify();
            }
            /*
             * UPDATE
             */
            update(graphics.getDeltaTime());

            /*
             * FRAME OUTPUT
             */
            EventManager.publish(Event.RENDER_FRAME, this, this);

            /*
             * SCREENSHOT OUTPUT - simple|advanced mode
             */
            EventManager.publish(Event.RENDER_SCREENSHOT, this, this);

            /*
             * SCREEN OUTPUT
             */
            if (settings.graphics.screenOutput) {
                int tw = graphics.getWidth();
                int th = graphics.getHeight();
                if (tw == 0 || th == 0) {
                    // Hack - on Windows the reported width and height is 0 when the window is minimized
                    tw = settings.graphics.resolution[0];
                    th = settings.graphics.resolution[1];
                }
                int w = (int) (tw * settings.graphics.backBufferScale);
                int h = (int) (th * settings.graphics.backBufferScale);
                /* RENDER THE SCENE */
                preRenderScene();
                if (settings.runtime.openVr) {
                    renderSgr(cameraManager, t, settings.graphics.backBufferResolution[0], settings.graphics.backBufferResolution[1], tw, th, null, postProcessor.getPostProcessBean(RenderType.screen));
                } else {
                    PostProcessBean ppb = postProcessor.getPostProcessBean(RenderType.screen);
                    if (ppb != null)
                        renderSgr(cameraManager, t, w, h, tw, th, null, ppb);
                }

                // Render the GUI, setting the viewport
                if (settings.runtime.openVr) {
                    guiRegistry.render(settings.graphics.backBufferResolution[0], settings.graphics.backBufferResolution[1]);
                } else {
                    guiRegistry.render(tw, th);
                }
            }
        }
        // Clean lists
        sgr.clearLists();
        // Number of frames
        frames++;

        if (settings.graphics.fpsLimit > 0.0) {
            // If FPS limit is on, dynamic resolution is off
            sleep(settings.graphics.fpsLimit);
        } else if (!settings.program.isStereoOrCubemap() && settings.graphics.dynamicResolution && TimeUtils.millis() - startTime > 10000 && TimeUtils.millis() - lastDynamicResolutionChange > 500 && !settings.runtime.openVr) {
            // Dynamic resolution, adjust the back-buffer scale depending on the frame rate
            float fps = 1f / graphics.getDeltaTime();

            if (fps < 30 && dynamicResolutionLevel < settings.graphics.dynamicResolutionScale.length - 1) {
                // Downscale
                settings.graphics.backBufferScale = settings.graphics.dynamicResolutionScale[++dynamicResolutionLevel];
                postRunnable(() -> resizeImmediate(graphics.getWidth(), graphics.getHeight(), true, true, false, false));
                lastDynamicResolutionChange = TimeUtils.millis();
            } else if (fps > 60 && dynamicResolutionLevel > 0) {
                // Move up
                settings.graphics.backBufferScale = settings.graphics.dynamicResolutionScale[--dynamicResolutionLevel];
                postRunnable(() -> resizeImmediate(graphics.getWidth(), graphics.getHeight(), true, true, false, false));
                lastDynamicResolutionChange = TimeUtils.millis();
            }
        }
    };

    public void resetDynamicResolution() {
        dynamicResolutionLevel = 0;
        settings.graphics.backBufferScale = 1f;
        postRunnable(() -> resizeImmediate(graphics.getWidth(), graphics.getHeight(), true, true, false, false));
        lastDynamicResolutionChange = 0;
    }

    public FrameBuffer getBackRenderBuffer() {
        return sgr.getCurrentSGR().getResultBuffer();
    }

    /**
     * Displays the initial GUI
     **/
    private final Runnable runnableInitialGui = () -> {
        renderGui(welcomeGui);
        if (settings.runtime.openVr) {
            try {
                vrContext.pollEvents();
            } catch (Exception e) {
                logger.error(e);
            }

            renderVRGui((VRGui<?>) welcomeGuiVR);
        }
    };

    /**
     * Displays the loading GUI
     **/
    private final Runnable runnableLoadingGui = () -> {
        boolean finished = false;
        try {
            finished = assetManager.update();
        } catch (GdxRuntimeException e) {
            // Resource failed to load
            logger.warn(e.getLocalizedMessage());
        }
        if (finished) {
            doneLoading();
            renderProcess = runnableRender;
        } else {
            // Display loading screen
            if (settings.runtime.openVr) {
                renderGui(loadingGui);

                try {
                    vrContext.pollEvents();
                } catch (Exception e) {
                    logger.error(e);
                }
                renderVRGui((VRGui<?>) loadingGuiVR);
            } else {
                renderGui(loadingGui);
            }
        }
    };

    private void renderVRGui(VRGui<?> vrGui) {
        vrLoadingLeftFb.begin();
        renderGui((vrGui).left());
        vrLoadingLeftFb.end();

        vrLoadingRightFb.begin();
        renderGui((vrGui).right());
        vrLoadingRightFb.end();

        /* SUBMIT TO VR COMPOSITOR */
        VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Left, vrLoadingLeftTex, null, VR.EVRSubmitFlags_Submit_Default);
        VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Right, vrLoadingRightTex, null, VR.EVRSubmitFlags_Submit_Default);
    }

    // Has the application crashed?
    private AtomicBoolean crashed = new AtomicBoolean(false);
    // Running state
    private AtomicBoolean running = new AtomicBoolean(true);

    public void setCrashed(boolean crashed) {
        this.crashed.set(crashed);
    }

    public boolean isCrashed() {
        return crashed.get();
    }

    @Override
    public void render() {
        try {
            if (running.get() && !crashed.get()) {
                // Run the render process
                renderProcess.run();

                // Run parked runnables
                synchronized (parkedRunnables) {
                    if (parkedRunnables.size() > 0) {
                        Iterator<Runnable> it = parkedRunnables.iterator();
                        while (it.hasNext()) {
                            Runnable r = it.next();
                            try {
                                r.run();
                            } catch (Exception e) {
                                logger.error(e);
                                // If it crashed, remove it
                                it.remove();
                            }
                        }
                    }
                }
            } else if (crashGui != null) {
                // Crash information
                renderGui(crashGui);
            }
        } catch (Throwable t) {
            // Report the crash
            CrashReporter.reportCrash(t, logger);
            // Set up crash window
            crashGui = new CrashGui(globalResources.getSkin(), graphics, 1f / settings.program.ui.scale, t);
            crashGui.initialize(assetManager, globalResources.getSpriteBatch());
            Gdx.input.setInputProcessor(crashGui.getGuiStage());
            // Flag up
            crashed.set(true);
        }

        // Create UI window if needed
        if (externalView && gaiaSkyView == null) {
            postRunnable(() -> {
                // Create window
                Lwjgl3Application app = (Lwjgl3Application) Gdx.app;
                Lwjgl3WindowConfiguration config = new Lwjgl3WindowConfiguration();
                config.setWindowPosition(0, 0);
                config.setWindowedMode(graphics.getWidth(), graphics.getHeight());
                config.setTitle(settings.APPLICATION_NAME + " - External view");
                config.useVsync(false);
                config.setWindowIcon(Files.FileType.Internal, "icon/gs_icon.png");
                gaiaSkyView = new GaiaSkyView(globalResources.getSkin(), globalResources.getSpriteShader());
                Lwjgl3Window newWindow = app.newWindow(gaiaSkyView, config);
                gaiaSkyView.setWindow(newWindow);
            });
        }
    }

    private long start = System.currentTimeMillis();

    /**
     * Pause the main thread for a certain amount of time to match the
     * given target frame rate.
     *
     * @param fps The target frame rate
     */
    private void sleep(double fps) {
        if (fps > 0) {
            long diff = System.currentTimeMillis() - start;
            long targetDelay = Math.round((1000.0 / fps));
            if (diff < targetDelay) {
                try {
                    Thread.sleep(targetDelay - diff);
                } catch (InterruptedException ignored) {
                }
            }
            start = System.currentTimeMillis();
        }
    }

    /**
     * Update method.
     *
     * @param dt Delta time in seconds.
     */
    public void update(double dt) {
        // Resize if needed
        updateResize();

        Timer.instance();
        // The actual frame time difference in seconds
        double dtGs;
        if (settings.frame.active) {
            // If frame output is active, we need to set our delta t according to
            // the configured frame rate of the frame output system
            dtGs = 1.0 / settings.frame.targetFps;
        } else if (camRecording) {
            // If Camera is recording, we need to set our delta t according to
            // the configured frame rate of the camrecorder
            dtGs = 1.0 / settings.camrecorder.targetFps;
        } else {
            // Max time step is 0.05 seconds (20 FPS). Not in RENDER_OUTPUT MODE.
            dtGs = Math.min(dt, 0.05);
        }

        this.t += dtGs;

        // Update GUI 
        guiRegistry.update(dtGs);
        EventManager.publish(Event.UPDATE_GUI, this, dtGs);

        // Update clock
        time.update(dtGs);

        // Update events
        EventManager.instance.dispatchDelayedMessages();

        // Update cameras
        cameraManager.update(dtGs, time);

        // Update GravWaves params
        RelativisticEffectsManager.getInstance().update(time, cameraManager.current);

        // Update scene graph
        sceneGraph.update(time, cameraManager);

        // Swap proximity buffers
        cameraManager.swapBuffers();

    }

    public void preRenderScene() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }

    public void renderSgr(final ICamera camera, final double t, final int width, final int height, final int tw, final int th, final FrameBuffer frameBuffer, final PostProcessBean ppb) {
        sgr.render(camera, t, width, height, tw, th, frameBuffer, ppb);
    }

    private long lastResizeTime = Long.MAX_VALUE;
    private int resizeWidth, resizeHeight;

    @Override
    public void resize(final int width, final int height) {
        if (width != 0 && height != 0) {
            if (!initialized) {
                resizeImmediate(width, height, true, true, true, true);
            }

            resizeWidth = width;
            resizeHeight = height;
            lastResizeTime = System.currentTimeMillis();

            if (renderBatch != null)
                renderBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        }
    }

    private void updateResize() {
        long currResizeTime = System.currentTimeMillis();
        if (currResizeTime - lastResizeTime > 100L) {
            resizeImmediate(resizeWidth, resizeHeight, !settings.runtime.openVr, !settings.runtime.openVr, true, true);
            lastResizeTime = Long.MAX_VALUE;
        }
    }

    public void resizeImmediate(final int width, final int height, boolean resizePostProcessors, boolean resizeRenderSys, boolean resizeGuis, boolean resizeScreenConf) {
        try {
            final int renderWidth = (int) Math.round(width * settings.graphics.backBufferScale);
            final int renderHeight = (int) Math.round(height * settings.graphics.backBufferScale);

            // Resize global UI sprite batch
            globalResources.getSpriteBatch().getProjectionMatrix().setToOrtho2D(0, 0, renderWidth, renderHeight);

            if (!initialized) {
                if (welcomeGui != null)
                    welcomeGui.resize(width, height);
                if (loadingGui != null)
                    loadingGui.resizeImmediate(width, height);
            } else {
                if (resizePostProcessors)
                    postProcessor.resizeImmediate(renderWidth, renderHeight);

                if (resizeGuis)
                    for (IGui gui : guis)
                        gui.resizeImmediate(width, height);

                sgr.resize(width, height, renderWidth, renderHeight, resizeRenderSys);

                if (resizeScreenConf)
                    settings.graphics.resize(width, height);
            }

            cameraManager.updateAngleEdge(renderWidth, renderHeight);
            cameraManager.resize(width, height);
        } catch (Exception e) {
            logger.error(e);
            // TODO This try-catch block is a provisional fix for Windows, as GLFW crashes when minimizing with lwjgl 3.2.3 and libgdx 1.9.10
        }
    }

    /**
     * Renders a particular GUI
     *
     * @param gui The GUI to render
     */
    private void renderGui(final IGui gui) {
        gui.update(graphics.getDeltaTime());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        gui.render(graphics.getWidth(), graphics.getHeight());
    }

    public Array<IFocus> getFocusableEntities() {
        return sceneGraph.getFocusableObjects();
    }

    public FrameBuffer getFrameBuffer(final int w, final int h) {
        final String key = getKey(w, h);
        if (!frameBufferMap.containsKey(key)) {
            final FrameBuffer fb = PingPongBuffer.createMainFrameBuffer(w, h, true, true, true, true, Format.RGB888, true);
            frameBufferMap.put(key, fb);
        }
        return frameBufferMap.get(key);
    }

    private String getKey(int w, int h) {
        return w + "x" + h;
    }

    public HashMap<VRDevice, StubModel> getVRDeviceToModel() {
        return vrDeviceToModel;
    }

    public IScriptingInterface scripting() {
        return this.scripting;
    }

    public GaiaSkyExecutorService getExecutorService() {
        return this.executorService;
    }

    public GuiRegistry getGuiRegistry() {
        return this.guiRegistry;
    }

    public BookmarksManager getBookmarksManager() {
        return this.bookmarksManager;
    }

    public ICamera getICamera() {
        return this.cameraManager.current;
    }

    public double getT() {
        return this.t;
    }

    public CameraManager getCameraManager() {
        return this.cameraManager;
    }

    public IPostProcessor getPostProcessor() {
        return this.postProcessor;
    }

    public boolean isOn(final int ordinal) {
        return this.sgr.isOn(ordinal);
    }

    public boolean isOn(final ComponentType comp) {
        return this.sgr.isOn(comp);
    }

    public boolean isOn(final ComponentTypes cts) {
        return this.sgr.allOn(cts);
    }

    public Optional<CatalogInfo> getCatalogInfoFromObject(SceneGraphNode node) {
        if (node instanceof FadeNode) {
            return catalogManager.getByObject((FadeNode) node);
        }
        return Optional.empty();
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case LOAD_DATA_CMD:
            // Init components that need assets in data folder
            reinitialiseGUI1();
            postProcessor.initialize(assetManager);

            // Initialise loading screen
            loadingGui = new LoadingGui(globalResources.getSkin(), graphics, 1f / settings.program.ui.scale, false);
            loadingGui.initialize(assetManager, globalResources.getSpriteBatch());

            Gdx.input.setInputProcessor(loadingGui.getGuiStage());

            // Also VR
            if (settings.runtime.openVr) {
                loadingGuiVR = new VRGui<>(LoadingGui.class, (int) (settings.graphics.backBufferResolution[0] / 4f), globalResources.getSkin(), graphics, 1f / settings.program.ui.scale);
                loadingGuiVR.initialize(assetManager, globalResources.getSpriteBatch());
            }

            this.renderProcess = runnableLoadingGui;

            /* LOAD SCENE GRAPH */
            if (sceneGraph == null) {
                dataLoadString = "SceneGraphData";
                final String[] dataFilesToLoad = new String[settings.data.dataFiles.size()];
                int i = 0;
                // Add data files
                // Our resolver in the SGLoader itself will resolve their full paths
                for (String dataFile : settings.data.dataFiles) {
                    dataFilesToLoad[i] = dataFile;
                    i++;
                }
                assetManager.load(dataLoadString, ISceneGraph.class, new SGLoaderParameter(dataFilesToLoad, time, settings.performance.multithreading, settings.performance.getNumberOfThreads()));
            }
            break;
        case TOGGLE_AMBIENT_LIGHT:
            // TODO No better place to put this??
            ModelComponent.toggleAmbientLight((Boolean) data[1]);
            break;
        case AMBIENT_LIGHT_CMD:
            ModelComponent.setAmbientLight((float) data[0]);
            break;
        case RECORD_CAMERA_CMD:
            if (data != null && data.length > 0) {
                camRecording = (Boolean) data[0];
            } else {
                camRecording = !camRecording;
            }
            break;
        case CAMERA_MODE_CMD:
            // Register/unregister GUI
            final CameraMode mode = (CameraMode) data[0];
            if (settings.program.modeStereo.isStereoHalfViewport()) {
                guiRegistry.change(stereoGui);
            } else if (mode == CameraMode.SPACECRAFT_MODE) {
                guiRegistry.change(spacecraftGui);
            } else {
                guiRegistry.change(mainGui);
            }
            break;
        case STEREOSCOPIC_CMD:
            final boolean stereoMode = (Boolean) data[0];
            if (stereoMode && guiRegistry.current != stereoGui) {
                guiRegistry.change(stereoGui);
            } else if (!stereoMode && guiRegistry.previous != stereoGui) {
                IGui prev = guiRegistry.current != null ? guiRegistry.current : mainGui;
                guiRegistry.change(guiRegistry.previous, prev);
            }

            // Disable dynamic resolution
            // Post a message to the screen
            if (stereoMode) {
                resetDynamicResolution();

                String[] keysStrToggle = KeyBindings.instance.getStringArrayKeys("action.toggle/element.stereomode");
                String[] keysStrProfile = KeyBindings.instance.getStringArrayKeys("action.switchstereoprofile");
                final ModePopupInfo mpi = new ModePopupInfo();
                mpi.title = I18n.txt("gui.stereo.title");
                mpi.header = I18n.txt("gui.stereo.notice.header");
                ;
                mpi.addMapping(I18n.txt("gui.stereo.notice.back"), keysStrToggle);
                mpi.addMapping(I18n.txt("gui.stereo.notice.profile"), keysStrProfile);

                EventManager.publish(Event.MODE_POPUP_CMD, this, mpi, "stereo", 120f);
            } else {
                EventManager.publish(Event.MODE_POPUP_CMD, this, null, "stereo");
            }
            break;
        case CUBEMAP_CMD:
            boolean cubemapMode = (Boolean) data[0];
            if(cubemapMode) {
                resetDynamicResolution();
            }

            break;
        case SCREENSHOT_SIZE_UPDATE:
        case FRAME_SIZE_UPDATE:
            //GaiaSky.postRunnable(() -> {
            //clearFrameBufferMap();
            //});
            break;
        case SCENE_GRAPH_ADD_OBJECT_CMD:
            final SceneGraphNode nodeToAdd = (SceneGraphNode) data[0];
            final boolean addToIndex = data.length == 1 || (Boolean) data[1];
            if (sceneGraph != null) {
                postRunnable(() -> {
                    try {
                        sceneGraph.insert(nodeToAdd, addToIndex);
                    } catch (Exception e) {
                        logger.error(e);
                    }
                });
            }
            break;
        case SCENE_GRAPH_ADD_OBJECT_NO_POST_CMD:
            final SceneGraphNode nodeToAddp = (SceneGraphNode) data[0];
            final boolean addToIndexp = data.length == 1 || (Boolean) data[1];
            if (sceneGraph != null) {
                try {
                    sceneGraph.insert(nodeToAddp, addToIndexp);
                } catch (Exception e) {
                    logger.error(e);
                }
            }
            break;
        case SCENE_GRAPH_REMOVE_OBJECT_CMD:
            SceneGraphNode aux;
            if (data[0] instanceof String) {
                aux = sceneGraph.getNode((String) data[0]);
                if (aux == null)
                    return;
            } else {
                aux = (SceneGraphNode) data[0];
            }
            final SceneGraphNode nodeToRemove = aux;
            final boolean removeFromIndex = data.length == 1 || (Boolean) data[1];
            if (sceneGraph != null) {
                postRunnable(() -> sceneGraph.remove(nodeToRemove, removeFromIndex));
            }
            break;
        case UI_SCALE_CMD:
            if (guis != null) {
                float uiScale = (Float) data[0];
                for (IGui gui : guis) {
                    gui.updateUnitsPerPixel(1f / uiScale);
                }
            }
            break;
        case HOME_CMD:
            goHome();
            break;
        case PER_OBJECT_VISIBILITY_CMD:
            final IVisibilitySwitch vs = (IVisibilitySwitch) data[0];
            String name = (String) data[1];
            boolean state = (boolean) data[2];
            vs.setVisible(state, name.toLowerCase());
            logger.info(I18n.txt("notif.visibility.object.set", vs.getName(), I18n.txt("gui." + state)));
            break;
        case FORCE_OBJECT_LABEL_CMD:
            final SceneGraphNode forceLabelObject = (SceneGraphNode) data[0];
            name = (String) data[1];
            state = (boolean) data[2];
            forceLabelObject.setForceLabel(state, name.toLowerCase());
            logger.info(I18n.txt("notif.object.flag", "forceLabel", forceLabelObject.getName(), I18n.txt("gui." + state)));
            break;
        case LABEL_COLOR_CMD:
            final SceneGraphNode labelColorObject = (SceneGraphNode) data[0];
            name = (String) data[1];
            float[] labelColor = (float[]) data[2];
            labelColorObject.setLabelcolor(labelColor, name);
            break;
        case PARK_RUNNABLE:
            synchronized (parkedRunnables) {
                final String key = (String) data[0];
                final Runnable runnable = (Runnable) data[1];
                parkRunnable(key, runnable);
            }
            break;
        case UNPARK_RUNNABLE:
            synchronized (parkedRunnables) {
                final String key = (String) data[0];
                removeRunnable(key);
            }
            break;
        default:
            break;
        }

    }

    public boolean isInitialised() {
        return initialized;
    }

    public boolean isHeadless() {
        return headless;
    }

    public GlobalResources getGlobalResources() {
        return this.globalResources;
    }

    /**
     * Parks a runnable that will run every frame right the update() method (before render)
     * until it is unparked.
     *
     * @param key      The key to identify the runnable.
     * @param runnable The runnable.
     */
    public void parkRunnable(final String key, final Runnable runnable) {
        synchronized (parkedRunnables) {
            parkedRunnablesMap.put(key, runnable);
            parkedRunnables.add(runnable);
        }
    }

    /**
     * Removes a previously parked runnable.
     *
     * @param key The key of the runnable to remove.
     */
    public void removeRunnable(final String key) {
        synchronized (parkedRunnables) {
            final Runnable r = parkedRunnablesMap.get(key);
            if (r != null) {
                parkedRunnables.remove(r);
                parkedRunnablesMap.remove(key);
            }
        }
    }

    /**
     * Posts a runnable that will run once after the current frame.
     *
     * @param r The runnable to post.
     */
    public static synchronized void postRunnable(final Runnable r) {
        if (instance != null && instance.window != null)
            instance.window.postRunnable(r);
        else
            Gdx.app.postRunnable(r);
    }

}
