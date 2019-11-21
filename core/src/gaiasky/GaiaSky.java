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
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interfce.*;
import gaiasky.render.*;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.IPostProcessor.PostProcessBean;
import gaiasky.render.IPostProcessor.RenderType;
import gaiasky.scenegraph.*;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.script.HiddenHelperUser;
import gaiasky.script.ScriptingServer;
import gaiasky.util.Logger;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.gaia.GaiaAttitudeServer;
import gaiasky.util.gdx.contrib.postprocess.utils.PingPongBuffer;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.loader.BitmapFontLoader;
import gaiasky.util.gdx.loader.G3dModelLoader;
import gaiasky.util.gdx.loader.ObjLoader;
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
import java.time.Instant;
import java.util.*;

/**
 * The main class. Holds all the entities manages the update/draw cycle as well
 * as the image rendering.
 *
 * @author Toni Sagrista
 */
public class GaiaSky implements ApplicationListener, IObserver, IMainRenderer {
    private static final Log logger = Logger.getLogger(GaiaSky.class);

    /**
     * Current render process.
     * One of {@link #runnableInitialGui}, {@link #runnableLoadingGui} or {@link #runnableRender}.
     **/
    private Runnable renderProcess;

    /**
     * Attitude folder
     **/
    private static String ATTITUDE_FOLDER = "data/attitudexml/";

    /**
     * Singleton instance
     **/
    public static GaiaSky instance;

    /** Window **/
    public static Lwjgl3Window window;
    /** Graphics **/
    public static Lwjgl3Graphics graphics;

    /**
     * The {@link VRContext} setup in {@link #createVR()}, may be null if no HMD is
     * present or SteamVR is not installed
     */
    public VRContext vrContext;

    /**
     * Loading fb
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
    public AssetManager manager;

    // Camera
    public CameraManager cam;

    // Data load string
    private String dataLoadString;

    public ISceneGraph sg;
    private SceneGraphRenderer sgr;
    private IPostProcessor pp;

    // Start time
    private long startTime;

    // Time since the start in seconds
    private double t;

    // The frame number
    public long frames;

    // Frame buffer map
    private Map<String, FrameBuffer> fbmap;

    // Registry
    private GuiRegistry guiRegistry;

    /**
     * Provisional console logger
     */
    private ConsoleLogger clogger;

    /**
     * The user interfaces
     */
    public IGui initialGui, loadingGui, loadingGuiVR, mainGui, spacecraftGui, stereoGui, debugGui;

    /**
     * List of GUIs
     */
    private List<IGui> guis;

    /**
     * Time
     */
    public ITimeFrameProvider time;

    /**
     * Camera recording or not?
     */
    private boolean camRecording = false;

    private boolean initialized = false;

    /**
     * Whether to attempt a connection to the VR HMD
     */
    private boolean vr;

    /**
     * Forces the dataset download window
     */
    private boolean dsDownload;

    /**
     * Forces the catalog chooser window
     */
    private boolean catChooser;

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
    public GaiaSkyView gaiaskyUI = null;

    /**
     * Runnables
     */
    private final Array<Runnable> runnables;
    private Map<String, Runnable> runnablesMap;

    /**
     * Creates an instance of Gaia Sky.
     */
    public GaiaSky() {
        this(false, false, false, false);
    }

    /**
     * Creates an instance of Gaia Sky.
     *
     * @param dsdownload Force-show the datasets download window
     * @param catchooser Force-show the catalog chooser window
     * @param vr Launch in VR mode
     * @param externalView Open a new window with a view of the rendered scene
     */
    public GaiaSky(boolean dsdownload, boolean catchooser, boolean vr, boolean externalView) {
        super();
        instance = this;
        this.runnables = new Array<>();
        this.runnablesMap = new HashMap<>();
        this.vr = vr;
        this.dsDownload = dsdownload;
        this.catChooser = catchooser;
        this.externalView = externalView;
        this.renderProcess = runnableInitialGui;

    }

    @Override
    public void create() {
        startTime = TimeUtils.millis();
        Gdx.app.setLogLevel(Application.LOG_INFO);
        clogger = new ConsoleLogger(true, true);

        // Init graphics and window
        graphics = (Lwjgl3Graphics) Gdx.graphics;
        window = graphics.getWindow();

        // Basic info
        logger.info(GlobalConf.version.version, I18n.bundle.format("gui.build", GlobalConf.version.build));
        logger.info("Display mode", graphics.getWidth() + "x" + graphics.getHeight(), "Fullscreen: " + Gdx.graphics.isFullscreen());
        logger.info("Device", GL30.glGetString(GL30.GL_RENDERER));
        logger.info(I18n.bundle.format("notif.glversion", GL30.glGetString(GL30.GL_VERSION)));
        logger.info(I18n.bundle.format("notif.glslversion", GL30.glGetString(GL30.GL_SHADING_LANGUAGE_VERSION)));
        logger.info("Java version", System.getProperty("java.version"), System.getProperty("java.vendor"));

        // Frame buffer map
        fbmap = new HashMap<>();

        // Disable all kinds of input
        EventManager.instance.post(Events.INPUT_ENABLED_CMD, false);

        if (!GlobalConf.initialized()) {
            logger.error(new RuntimeException("FATAL: Global configuration not initlaized"));
            return;
        }

        // Initialise times
        ITimeFrameProvider clock = new GlobalClock(1, Instant.now());
        ITimeFrameProvider real = new RealTimeClock();
        time = GlobalConf.runtime.REAL_TIME ? real : clock;
        t = 0;

        // Initialise i18n
        I18n.initialize();

        // Tooltips
        TooltipManager.getInstance().initialTime = 1f;
        TooltipManager.getInstance().hideAll();

        // Initialise asset manager
        FileHandleResolver internalResolver = new InternalFileHandleResolver();
        FileHandleResolver dataResolver = fileName -> GlobalConf.data.dataFileHandle(fileName);
        manager = new AssetManager(internalResolver);
        //manager.setLoader(Model.class, ".obj", new AdvancedObjLoader(resolver));
        manager.setLoader(ISceneGraph.class, new SGLoader(dataResolver));
        manager.setLoader(PointCloudData.class, new OrbitDataLoader(dataResolver));
        manager.setLoader(GaiaAttitudeServer.class, new GaiaAttitudeLoader(dataResolver));
        manager.setLoader(ExtShaderProgram.class, new ShaderProgramProvider(internalResolver, ".vertex.glsl", ".fragment.glsl"));
        manager.setLoader(BitmapFont.class, new BitmapFontLoader(internalResolver));
        //manager.setLoader(DefaultIntShaderProvider.class, new DefaultShaderProviderLoader<>(resolver));
        manager.setLoader(AtmosphereShaderProvider.class, new AtmosphereShaderProviderLoader<>(internalResolver));
        manager.setLoader(GroundShaderProvider.class, new GroundShaderProviderLoader<>(internalResolver));
        manager.setLoader(TessellationShaderProvider.class, new TessellationShaderProviderLoader<>(internalResolver));
        manager.setLoader(RelativisticShaderProvider.class, new RelativisticShaderProviderLoader<>(internalResolver));
        manager.setLoader(IntModel.class, ".obj", new ObjLoader(new RegularInputStreamProvider(), internalResolver));
        manager.setLoader(IntModel.class, ".obj.gz", new ObjLoader(new GzipInputStreamProvider(), internalResolver));
        manager.setLoader(IntModel.class, ".g3dj", new G3dModelLoader(new JsonReader(), internalResolver));
        manager.setLoader(IntModel.class, ".g3db", new G3dModelLoader(new UBJsonReader(), internalResolver));
        // Remove Model loaders

        // Init global resources
        GlobalResources.initialize(manager);

        // Catalog manager
        CatalogManager.initialize();

        // Initialise master manager
        MasterManager.initialize();

        // Init timer if needed
        Timer.instance();

        // Initialise Cameras
        cam = new CameraManager(manager, CameraMode.FOCUS_MODE, vr);

        // Set asset manager to asset bean
        AssetBean.setAssetManager(manager);

        // Create vr context if possible
        VRStatus vrStatus = createVR();
        cam.updateFrustumPlanes();

        // Tooltip to 1s
        TooltipManager.getInstance().initialTime = 1f;

        // Initialise Gaia attitudes
        manager.load(ATTITUDE_FOLDER, GaiaAttitudeServer.class, new GaiaAttitudeLoaderParameter());

        // Initialise hidden helper user
        HiddenHelperUser.initialize();

        // Initialise gravitational waves helper
        RelativisticEffectsManager.initialize(time);

        // GUI
        guis = new ArrayList<>(3);

        // Post-processor
        pp = PostProcessorFactory.instance.getPostProcessor();

        // Scene graph renderer
        SceneGraphRenderer.initialise(manager, vrContext);
        sgr = SceneGraphRenderer.instance;

        // Initialise scripting gateway server
        ScriptingServer.initialize();

        // Tell the asset manager to load all the assets
        Set<AssetBean> assets = AssetBean.getAssets();
        for (AssetBean ab : assets) {
            ab.load(manager);
        }

        EventManager.instance.subscribe(this, Events.LOAD_DATA_CMD);

        initialGui = new InitialGui(dsDownload, catChooser, vrStatus);
        initialGui.initialize(manager);
        Gdx.input.setInputProcessor(initialGui.getGuiStage());

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
                GlobalConf.runtime.OPENVR = true;
                Constants.initialize(GlobalConf.scene.DIST_SCALE_VR);

                vrContext = new VRContext();
                vrContext.pollEvents();

                VRDevice hmd = vrContext.getDeviceByType(VRDeviceType.HeadMountedDisplay);
                logger.info("Initialization of VR successful");
                if (hmd == null) {
                    logger.info("HMD device is null!");
                } else {
                    logger.info("HMD device is not null: " + vrContext.getDeviceByType(VRDeviceType.HeadMountedDisplay).toString());
                }

                vrDeviceToModel = new HashMap<>();

                GlobalConf.runtime.OPENVR = true;
                if (GlobalConf.screen.SCREEN_WIDTH != vrContext.getWidth()) {
                    logger.info("Warning, resizing according to VRSystem values:  [" + GlobalConf.screen.SCREEN_WIDTH + "x" + GlobalConf.screen.SCREEN_HEIGHT + "] -> [" + vrContext.getWidth() + "x" + vrContext.getHeight() + "]");
                    // Do not resize the screen!
                    GlobalConf.screen.BACKBUFFER_HEIGHT = vrContext.getHeight();
                    GlobalConf.screen.BACKBUFFER_WIDTH = vrContext.getWidth();
                    this.resizeImmediate(vrContext.getWidth(), vrContext.getHeight(), true, true, true);
                }
                GlobalConf.screen.VSYNC = false;

                graphics.setWindowedMode(GlobalConf.screen.SCREEN_WIDTH, GlobalConf.screen.SCREEN_HEIGHT);
                graphics.setVSync(GlobalConf.screen.VSYNC);

                vrLoadingLeftFb = new FrameBuffer(Format.RGBA8888, vrContext.getWidth(), vrContext.getHeight(), true);
                vrLoadingLeftTex = org.lwjgl.openvr.Texture.create();
                vrLoadingLeftTex.set(vrLoadingLeftFb.getColorBufferTexture().getTextureObjectHandle(), VR.ETextureType_TextureType_OpenGL, VR.EColorSpace_ColorSpace_Gamma);

                vrLoadingRightFb = new FrameBuffer(Format.RGBA8888, vrContext.getWidth(), vrContext.getHeight(), true);
                vrLoadingRightTex = org.lwjgl.openvr.Texture.create();
                vrLoadingRightTex.set(vrLoadingRightFb.getColorBufferTexture().getTextureObjectHandle(), VR.ETextureType_TextureType_OpenGL, VR.EColorSpace_ColorSpace_Gamma);

                // Enable visibility of 'Others' if off (for VR controllers)
                if (!GlobalConf.scene.VISIBILITY[ComponentType.Others.ordinal()]) {
                    EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.others", false, true);
                }
                return VRStatus.OK;
            } catch (Exception e) {
                // If initializing the VRContext failed
                logger.debug(e);
                logger.error(e.getLocalizedMessage());
                logger.error("Initialisation of VR context failed");
                return VRStatus.ERROR_NO_CONTEXT;
            }
        } else {
            // Desktop mode
            GlobalConf.runtime.OPENVR = false;
            Constants.initialize(GlobalConf.scene.DIST_SCALE_DESKTOP);
        }
        return VRStatus.NO_VR;
    }

    /**
     * Execute this when the models have finished loading. This sets the models
     * to their classes and removes the Loading message
     */
    private void doneLoading() {
        // Dispose of initial and loading GUIs
        initialGui.dispose();
        initialGui = null;

        loadingGui.dispose();
        loadingGui = null;

        // Dispose vr loading GUI
        if (GlobalConf.runtime.OPENVR) {
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
        if (manager.isLoaded(ATTITUDE_FOLDER)) {
            GaiaAttitudeServer.instance = manager.get(ATTITUDE_FOLDER);
        }

        /*
         * SAMP
         */
        SAMPClient.getInstance().initialize();

        /*
         * POST-PROCESSOR
         */
        pp.doneLoading(manager);

        /*
         * GET SCENE GRAPH
         */
        if (manager.isLoaded(dataLoadString)) {
            sg = manager.get(dataLoadString);
        } else {
            throw new RuntimeException("Error loading scene graph from data load string: " + dataLoadString + ", and files: " + TextUtils.concatenate(File.pathSeparator, GlobalConf.data.CATALOG_JSON_FILES));
        }

        /*
         * SCENE GRAPH RENDERER
         */
        AbstractRenderer.initialize(sg);
        sgr.doneLoading(manager);
        sgr.resize(graphics.getWidth(), graphics.getHeight());

        // First time, set assets
        Array<SceneGraphNode> nodes = sg.getNodes();
        for (SceneGraphNode sgn : nodes) {
            sgn.doneLoading(manager);
        }

        // Initialise input multiplexer to handle various input processors
        // The input multiplexer
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        GuiRegistry.setInputMultiplexer(inputMultiplexer);
        Gdx.input.setInputProcessor(inputMultiplexer);

        // Destroy console logger
        clogger.dispose();
        clogger = null;

        // Init GUIs, step 2
        reinitialiseGUI2();

        // Publish visibility
        EventManager.instance.post(Events.VISIBILITY_OF_COMPONENTS, SceneGraphRenderer.visible);

        // Key bindings
        inputMultiplexer.addProcessor(new KeyInputController(Gdx.input));

        EventManager.instance.post(Events.SCENE_GRAPH_LOADED, sg);

        // Update whole tree to initialize positions
        OctreeNode.LOAD_ACTIVE = false;
        time.update(0.000000001f);
        // Update whole scene graph
        sg.update(time, cam);
        sgr.clearLists();
        time.update(0);
        OctreeNode.LOAD_ACTIVE = true;

        // Initialise time in GUI
        EventManager.instance.post(Events.TIME_CHANGE_INFO, time.getTime());

        // Subscribe to events
        EventManager.instance.subscribe(this, Events.TOGGLE_AMBIENT_LIGHT, Events.AMBIENT_LIGHT_CMD, Events.RECORD_CAMERA_CMD, Events.CAMERA_MODE_CMD, Events.STEREOSCOPIC_CMD, Events.FRAME_SIZE_UDPATE, Events.SCREENSHOT_SIZE_UDPATE, Events.PARK_POST_RUNNABLE, Events.UNPARK_POST_RUNNABLE, Events.SCENE_GRAPH_ADD_OBJECT_CMD, Events.SCENE_GRAPH_ADD_OBJECT_NO_POST_CMD, Events.SCENE_GRAPH_REMOVE_OBJECT_CMD, Events.HOME_CMD);

        // Re-enable input
        EventManager.instance.post(Events.INPUT_ENABLED_CMD, true);

        // Set current date
        EventManager.instance.post(Events.TIME_CHANGE_CMD, Instant.now());

        // Resize GUIs to current size
        for (IGui gui : guis)
            gui.resize(graphics.getWidth(), graphics.getHeight());

        // Initialise frames
        frames = 0;

        // Debug info scheduler
        Task debugTask1 = new Task() {
            @Override
            public void run() {
                // FPS
                EventManager.instance.post(Events.FPS_INFO, 1f / graphics.getDeltaTime());
                // Current session time
                EventManager.instance.post(Events.DEBUG_TIME, TimeUtils.timeSinceMillis(startTime) / 1000d);
                // Memory
                EventManager.instance.post(Events.DEBUG_RAM, MemInfo.getUsedMemory(), MemInfo.getFreeMemory(), MemInfo.getTotalMemory(), MemInfo.getMaxMemory());
                // Observed objects
                EventManager.instance.post(Events.DEBUG_OBJECTS, OctreeNode.nObjectsObserved, StreamingOctreeLoader.getNLoadedStars());
                // Observed octants
                EventManager.instance.post(Events.DEBUG_QUEUE, OctreeNode.nOctantsObserved, StreamingOctreeLoader.getLoadQueueSize());
                // VRAM
                EventManager.instance.post(Events.DEBUG_VRAM, VMemInfo.getUsedMemory(), VMemInfo.getTotalMemory());
            }
        };

        Task debugTask10 = new Task() {
            @Override
            public void run() {
                EventManager.instance.post(Events.SAMP_INFO, SAMPClient.getInstance().getStatus());
            }
        };

        // Every second
        Timer.schedule(debugTask1, 2, 1);
        // Every 10 seconds
        Timer.schedule(debugTask10, 2, 10);

        // Go home
        goHome();

        EventManager.instance.post(Events.INITIALIZED_INFO);
        initialized = true;
    }

    /**
     * Moves the camera home. That is either the Earth, if it exists, or somewhere close to the Sun
     */
    private void goHome() {
        if (sg.containsNode(GlobalConf.scene.STARTUP_OBJECT) && !GlobalConf.program.NET_SLAVE && isOn(ComponentType.Planets.ordinal())) {
            // Set focus to Earth
            EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE);
            EventManager.instance.post(Events.FOCUS_CHANGE_CMD, sg.findFocus(GlobalConf.scene.STARTUP_OBJECT), true);
            EventManager.instance.post(Events.GO_TO_OBJECT_CMD);
            if (GlobalConf.runtime.OPENVR) {
                // Free mode by default in VR
                EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FREE_MODE);
            }
        } else {
            // At 5 AU in Y looking towards origin (top-down look)
            EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FREE_MODE);
            EventManager.instance.post(Events.CAMERA_POS_CMD, new double[] { 0, 5 * Constants.AU_TO_U, 0 });
            EventManager.instance.post(Events.CAMERA_DIR_CMD, new double[] { 0, -1, 0 });
            EventManager.instance.post(Events.CAMERA_UP_CMD, new double[] { 0, 0, 1 });
        }
    }

    /**
     * Reinitialises all the GUI (step 1)
     */
    public void reinitialiseGUI1() {
        if (guis != null && !guis.isEmpty()) {
            for (IGui gui : guis)
                gui.dispose();
            guis.clear();
        }

        mainGui = new FullGui(graphics);
        mainGui.initialize(manager);

        debugGui = new DebugGui();
        debugGui.initialize(manager);

        spacecraftGui = new SpacecraftGui();
        spacecraftGui.initialize(manager);

        stereoGui = new StereoGui();
        stereoGui.initialize(manager);

        if (guis != null) {
            guis.add(mainGui);
            guis.add(debugGui);
            guis.add(spacecraftGui);
            guis.add(stereoGui);
        }
    }

    /**
     * Second step in GUI initialisation.
     */
    public void reinitialiseGUI2() {
        // Reinitialise registry to listen to relevant events
        if (guiRegistry != null)
            guiRegistry.dispose();
        guiRegistry = new GuiRegistry(GlobalResources.skin);

        // Unregister all current GUIs
        GuiRegistry.unregisterAll();

        // Only for the Full GUI
        mainGui.setSceneGraph(sg);
        mainGui.setVisibilityToggles(ComponentType.values(), SceneGraphRenderer.visible);

        for (IGui gui : guis)
            gui.doneLoading(manager);

        if (GlobalConf.program.STEREOSCOPIC_MODE) {
            GuiRegistry.set(stereoGui);
            GuiRegistry.setPrevious(mainGui);
        } else {
            GuiRegistry.set(mainGui);
            GuiRegistry.setPrevious(null);
        }
        GuiRegistry.registerGui(debugGui);
        GuiRegistry.addProcessor(debugGui);
    }

    @Override
    public void pause() {
        EventManager.instance.post(Events.FLUSH_FRAMES);
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        if (saveState)
            ConfInit.instance.persistGlobalConf(new File(System.getProperty("properties.file")));

        if (vrContext != null)
            vrContext.dispose();

        // Flush frames
        EventManager.instance.post(Events.FLUSH_FRAMES);

        // Dispose all
        for (IGui gui : guis)
            gui.dispose();

        EventManager.instance.post(Events.DISPOSE);
        if (sg != null) {
            sg.dispose();
        }
        ModelCache.cache.dispose();

        // Scripting
        ScriptingServer.dispose();

        // Renderer
        if (sgr != null)
            sgr.dispose();

        // Post processor
        if (pp != null)
            pp.dispose();

        // Dispose music manager
        MusicManager.dispose();
    }

    /**
     * Renders the scene
     **/
    private Runnable runnableRender = () -> {

        // Asynchronous load of textures and resources
        manager.update();

        if (!GlobalConf.runtime.UPDATE_PAUSE) {
            EventManager.instance.post(Events.FRAME_TICK, frames);
            /*
             * UPDATE
             */
            update(graphics.getDeltaTime());

            /*
             * FRAME OUTPUT
             */
            EventManager.instance.post(Events.RENDER_FRAME, this);

            /*
             * SCREENSHOT OUTPUT - simple|redraw mode
             */
            EventManager.instance.post(Events.RENDER_SCREENSHOT, this);

            /*
             * SCREEN OUTPUT
             */
            if (GlobalConf.screen.SCREEN_OUTPUT) {
                /* RENDER THE SCENE */
                preRenderScene();
                if (GlobalConf.runtime.OPENVR) {
                    renderSgr(cam, t, GlobalConf.screen.BACKBUFFER_WIDTH, GlobalConf.screen.BACKBUFFER_HEIGHT, null, pp.getPostProcessBean(RenderType.screen));
                } else {
                    renderSgr(cam, t, graphics.getWidth(), graphics.getHeight(), null, pp.getPostProcessBean(RenderType.screen));
                }

                // Render the GUI, setting the viewport
                if (GlobalConf.runtime.OPENVR) {
                    GuiRegistry.render(GlobalConf.screen.BACKBUFFER_WIDTH, GlobalConf.screen.BACKBUFFER_HEIGHT);
                } else {
                    GuiRegistry.render(graphics.getWidth(), graphics.getHeight());
                }
            }
        }
        // Clean lists
        sgr.clearLists();
        // Number of frames
        frames++;

        if (GlobalConf.screen.LIMIT_FPS > 0) {
            sleep(GlobalConf.screen.LIMIT_FPS);
        }
    };

    /**
     * Displays the initial GUI
     **/
    private Runnable runnableInitialGui = () -> renderGui(initialGui);

    /**
     * Displays the loading GUI
     **/
    private Runnable runnableLoadingGui = () -> {
        if (manager.update()) {
            doneLoading();
            renderProcess = runnableRender;
        } else {
            // Display loading screen
            renderGui(loadingGui);
            if (GlobalConf.runtime.OPENVR) {
                try {
                    vrContext.pollEvents();
                } catch (Exception e) {
                    logger.error(e);
                }

                vrLoadingLeftFb.begin();
                renderGui(((VRGui) loadingGuiVR).left());
                vrLoadingLeftFb.end();

                vrLoadingRightFb.begin();
                renderGui(((VRGui) loadingGuiVR).right());
                vrLoadingRightFb.end();

                /** SUBMIT TO VR COMPOSITOR **/
                VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Left, vrLoadingLeftTex, null, VR.EVRSubmitFlags_Submit_Default);
                VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Right, vrLoadingRightTex, null, VR.EVRSubmitFlags_Submit_Default);
            }
        }
    };

    @Override
    public void render() {
        try {
            // Run the render process
            renderProcess.run();
        } catch (Throwable t) {
            CrashReporter.reportCrash(t, logger);
            // Quit
            Gdx.app.exit();
        }

        // Create UI window if needed
        if (externalView && gaiaskyUI == null) {
            postRunnable(() -> {
                // Create window
                Lwjgl3Application app = (Lwjgl3Application) Gdx.app;
                Lwjgl3WindowConfiguration config = new Lwjgl3WindowConfiguration();
                config.setWindowPosition(0, 0);
                config.setWindowedMode(graphics.getWidth(), graphics.getHeight());
                config.setTitle(GlobalConf.APPLICATION_NAME + " - External view");
                config.useVsync(false);
                config.setWindowIcon(Files.FileType.Internal, "icon/gs_icon.png");
                gaiaskyUI = new GaiaSkyView();
                Lwjgl3Window newWindow = app.newWindow(gaiaskyUI, config);
                gaiaskyUI.setWindow(newWindow);
            });
        }
    }

    private long start = System.currentTimeMillis();

    private void sleep(int fps) {
        if (fps > 0) {
            long diff = System.currentTimeMillis() - start;
            long targetDelay = 1000 / fps;
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
     * @param deltat Delta time in seconds.
     */
    public void update(double deltat) {
        // Resize if needed
        updateResize();

        Timer.instance();
        // The current actual dt in seconds
        double dt;
        if (GlobalConf.frame.RENDER_OUTPUT) {
            // If RENDER_OUTPUT is active, we need to set our dt according to
            // the fps
            dt = 1f / GlobalConf.frame.RENDER_TARGET_FPS;
        } else if (camRecording) {
            // If Camera is recording, we need to set our dt according to
            // the fps
            dt = 1f / GlobalConf.frame.CAMERA_REC_TARGET_FPS;
        } else {
            // Max time step is 0.1 seconds. Not in RENDER_OUTPUT MODE.
            dt = Math.min(deltat, 0.1f);
        }

        this.t += dt;

        // Update GUI 
        GuiRegistry.update(dt);
        EventManager.instance.post(Events.UPDATE_GUI, dt);

        double dtScene = dt;
        if (!GlobalConf.runtime.TIME_ON) {
            dtScene = 0;
        }
        // Update clock
        time.update(dtScene);

        // Update events
        EventManager.instance.dispatchDelayedMessages();

        // Update cameras
        cam.update(dt, time);

        // Precompute isOn for all stars and galaxies
        Particle.renderOn = isOn(ComponentType.Stars);

        // Update GravWaves params
        RelativisticEffectsManager.getInstance().update(time, cam.current);

        // Update scene graph
        sg.update(time, cam);

        // Run parked runnables
        synchronized (runnables) {
            Iterator<Runnable> it = runnables.iterator();
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

    public void preRenderScene() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }

    public void renderSgr(ICamera camera, double t, int width, int height, FrameBuffer frameBuffer, PostProcessBean ppb) {
        sgr.render(camera, t, width, height, frameBuffer, ppb);
    }

    private long lastResizeTime = Long.MAX_VALUE;
    private int resizeWidth, resizeHeight;

    @Override
    public void resize(final int width, final int height) {
        if (GlobalConf.runtime.OPENVR) {
            postRunnable(() -> resizeImmediate(graphics.getWidth(), graphics.getHeight(), false, false, false));
        } else {
            if (!initialized) {
                resizeImmediate(graphics.getWidth(), graphics.getHeight(), true, true, true);
            } else {
                resizeWidth = graphics.getWidth();
                resizeHeight = graphics.getHeight();
                lastResizeTime = System.currentTimeMillis();
            }
        }
    }

    private void updateResize() {
        long currResizeTime = System.currentTimeMillis();
        if (currResizeTime - lastResizeTime > 100l) {
            resizeImmediate(resizeWidth, resizeHeight, true, true, true);
            lastResizeTime = Long.MAX_VALUE;
        }
    }

    public void resizeImmediate(final int width, final int height, boolean resizePostProcessors, boolean resizeRenderSys, boolean resizeGuis) {
        if (!initialized) {
            if (initialGui != null)
                initialGui.resize(width, height);
            if (loadingGui != null)
                loadingGui.resizeImmediate(width, height);
        } else {
            if (resizePostProcessors)
                pp.resizeImmediate(width, height);

            if (resizeGuis)
                for (IGui gui : guis)
                    gui.resizeImmediate(width, height);

            sgr.resize(width, height, resizeRenderSys);
        }

        cam.updateAngleEdge(width, height);
        cam.resize(width, height);
    }

    /**
     * Renders a particular GUI
     *
     * @param gui The GUI to render
     */
    private void renderGui(IGui gui) {
        gui.update(graphics.getDeltaTime());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        gui.render(graphics.getWidth(), graphics.getHeight());
    }

    public Array<IFocus> getFocusableEntities() {
        return sg.getFocusableObjects();
    }

    public FrameBuffer getFrameBuffer(int w, int h) {
        String key = getKey(w, h);
        if (!fbmap.containsKey(key)) {
            FrameBuffer fb = PingPongBuffer.createMainFrameBuffer(w, h, true, true, Format.RGB888, true);
            fbmap.put(key, fb);
        }
        return fbmap.get(key);
    }

    private String getKey(int w, int h) {
        return w + "x" + h;
    }

    public HashMap<VRDevice, StubModel> getVRDeviceToModel() {
        return vrDeviceToModel;
    }

    public ICamera getICamera() {
        return cam.current;
    }

    public double getT() {
        return t;
    }

    public CameraManager getCameraManager() {
        return cam;
    }

    public IPostProcessor getPostProcessor() {
        return pp;
    }

    public boolean isOn(int ordinal) {
        return sgr.isOn(ordinal);
    }

    public boolean isOn(ComponentType comp) {
        return sgr.isOn(comp);
    }

    public boolean isOn(ComponentTypes cts) {
        return sgr.isOn(cts);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case LOAD_DATA_CMD:
            // Init components that need assets in data folder
            reinitialiseGUI1();
            pp.initialize(manager);

            // Initialise loading screen
            loadingGui = new LoadingGui(vr);
            loadingGui.initialize(manager);

            Gdx.input.setInputProcessor(loadingGui.getGuiStage());

            // Also VR
            if (GlobalConf.runtime.OPENVR) {
                loadingGuiVR = new VRGui(LoadingGui.class, 200);
                loadingGuiVR.initialize(manager);
            }

            this.renderProcess = runnableLoadingGui;

            /* LOAD SCENE GRAPH */
            if (sg == null) {
                dataLoadString = "SceneGraphData";
                String[] dataFilesToLoad = new String[GlobalConf.data.CATALOG_JSON_FILES.size + 1];
                // Prepare files to load
                int i = 0;
                for (String dataFile : GlobalConf.data.CATALOG_JSON_FILES) {
                    dataFilesToLoad[i] = dataFile;
                    i++;
                }
                dataFilesToLoad[i] = GlobalConf.data.OBJECTS_JSON_FILES;
                manager.load(dataLoadString, ISceneGraph.class, new SGLoaderParameter(dataFilesToLoad, time, GlobalConf.performance.MULTITHREADING, GlobalConf.performance.NUMBER_THREADS()));
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
            if (data != null) {
                camRecording = (Boolean) data[0];
            } else {
                camRecording = !camRecording;
            }
            break;
        case CAMERA_MODE_CMD:
            // Register/unregister GUI
            CameraMode mode = (CameraMode) data[0];
            if (GlobalConf.program.isStereoHalfViewport()) {
                GuiRegistry.change(stereoGui);
            } else if (mode == CameraMode.SPACECRAFT_MODE) {
                GuiRegistry.change(spacecraftGui);
            } else {
                GuiRegistry.change(mainGui);
            }
            break;
        case STEREOSCOPIC_CMD:
            boolean stereoMode = (Boolean) data[0];
            if (stereoMode && GuiRegistry.current != stereoGui) {
                GuiRegistry.change(stereoGui);
            } else if (!stereoMode && GuiRegistry.previous != stereoGui) {
                IGui prev = GuiRegistry.current != null ? GuiRegistry.current : mainGui;
                GuiRegistry.change(GuiRegistry.previous, prev);
            }

            // Post a message to the screen
            if (stereoMode) {
                ModePopupInfo mpi = new ModePopupInfo();
                mpi.title = "Stereoscopic mode";
                mpi.header = "You have entered Stereoscopic mode!";
                mpi.addMapping("Back to normal mode", "CTRL", "S");
                mpi.addMapping("Switch stereo profile", "CTRL", "SHIFT", "S");

                EventManager.instance.post(Events.MODE_POPUP_CMD, mpi, 120f);
            }

            break;
        case SCREENSHOT_SIZE_UDPATE:
        case FRAME_SIZE_UDPATE:
            //GaiaSky.postRunnable(() -> {
            //clearFrameBufferMap();
            //});
            break;
        case SCENE_GRAPH_ADD_OBJECT_CMD:
            final SceneGraphNode nodeToAdd = (SceneGraphNode) data[0];
            final boolean addToIndex = data.length == 1 ? true : (Boolean) data[1];
            if (sg != null) {
                postRunnable(() -> {
                    try {
                        sg.insert(nodeToAdd, addToIndex);
                    } catch (Exception e) {
                        logger.error(e);
                    }
                });
            }
            break;
        case SCENE_GRAPH_ADD_OBJECT_NO_POST_CMD:
            final SceneGraphNode nodeToAddp = (SceneGraphNode) data[0];
            final boolean addToIndexp = data.length == 1 ? true : (Boolean) data[1];
            if (sg != null) {
                try {
                    sg.insert(nodeToAddp, addToIndexp);
                } catch (Exception e) {
                    logger.error(e);
                }
            }
            break;
        case SCENE_GRAPH_REMOVE_OBJECT_CMD:
            SceneGraphNode aux;
            if (data[0] instanceof String) {
                aux = sg.getNode((String) data[0]);
                if (aux == null)
                    return;
            } else {
                aux = (SceneGraphNode) data[0];
            }
            final SceneGraphNode nodeToRemove = aux;
            final boolean removeFromIndex = data.length == 1 ? true : (Boolean) data[1];
            if (sg != null) {
                postRunnable(() -> {
                    sg.remove(nodeToRemove, removeFromIndex);
                });
            }
            break;
        case HOME_CMD:
            goHome();
            break;
        case PARK_POST_RUNNABLE:
            synchronized (runnables) {
                String key = (String) data[0];
                Runnable runnable = (Runnable) data[1];
                runnablesMap.put(key, runnable);
                runnables.add(runnable);
            }
            break;
        case UNPARK_POST_RUNNABLE:
            synchronized (runnables) {
                String key = (String) data[0];
                Runnable r = runnablesMap.get(key);
                if (r != null) {
                    runnables.removeValue(r, true);
                    runnablesMap.remove(data[0]);
                }
            }
            break;
        default:
            break;
        }

    }

    public boolean isInitialised() {
        return initialized;
    }

    public static void postRunnable(Runnable r) {
        if (window != null)
            window.postRunnable(r);
        else
            Gdx.app.postRunnable(r);
    }
}
