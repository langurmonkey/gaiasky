/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.Timer.Task;
import gaia.cu9.ari.gaiaorbit.assets.*;
import gaia.cu9.ari.gaiaorbit.assets.GaiaAttitudeLoader.GaiaAttitudeLoaderParameter;
import gaia.cu9.ari.gaiaorbit.assets.SGLoader.SGLoaderParameter;
import gaia.cu9.ari.gaiaorbit.data.AssetBean;
import gaia.cu9.ari.gaiaorbit.data.StreamingOctreeLoader;
import gaia.cu9.ari.gaiaorbit.data.util.PointCloudData;
import gaia.cu9.ari.gaiaorbit.desktop.util.CrashReporter;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.*;
import gaia.cu9.ari.gaiaorbit.render.*;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.PostProcessBean;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.RenderType;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.script.HiddenHelperUser;
import gaia.cu9.ari.gaiaorbit.script.ScriptingServer;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.gaia.GaiaAttitudeServer;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.utils.PingPongBuffer;
import gaia.cu9.ari.gaiaorbit.util.gdx.g2d.BitmapFont;
import gaia.cu9.ari.gaiaorbit.util.gdx.loader.BitmapFontLoader;
import gaia.cu9.ari.gaiaorbit.util.gdx.loader.G3dModelLoader;
import gaia.cu9.ari.gaiaorbit.util.gdx.loader.ObjLoader;
import gaia.cu9.ari.gaiaorbit.util.gdx.loader.is.GzipInputStreamProvider;
import gaia.cu9.ari.gaiaorbit.util.gdx.loader.is.RegularInputStreamProvider;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModel;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.*;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.samp.SAMPClient;
import gaia.cu9.ari.gaiaorbit.util.time.GlobalClock;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.time.RealTimeClock;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;
import org.lwjgl.opengl.GL30;

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
    public IGui initialGui, loadingGui, mainGui, spacecraftGui, stereoGui, debugGui;

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
     * Runnables
     */
    private final Array<Runnable> runnables;
    private Map<String, Runnable> runnablesMap;

    /**
     * Creates an instance of Gaia Sky.
     */
    public GaiaSky() {
        this(false, false);
    }

    /**
     * Creates an instance of Gaia Sky.
     *
     * @param dsdownload Force-show the datasets download window
     * @param catchooser Force-show the catalog chooser window
     */
    public GaiaSky(boolean dsdownload, boolean catchooser) {
        super();
        instance = this;
        this.runnables = new Array<>();
        this.runnablesMap = new HashMap<>();
        this.dsDownload = dsdownload;
        this.catChooser = catchooser;
        this.renderProcess = runnableInitialGui;
    }

    @Override
    public void create() {
        startTime = TimeUtils.millis();
        Gdx.app.setLogLevel(Application.LOG_INFO);
        clogger = new ConsoleLogger(true, true);

        // Basic info
        logger.info(GlobalConf.version.version, I18n.bundle.format("gui.build", GlobalConf.version.build));
        logger.info("Display mode", Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight(), "Fullscreen: " + Gdx.graphics.isFullscreen());
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
        cam = new CameraManager(manager, CameraMode.FOCUS_MODE);

        // Set asset manager to asset bean
        AssetBean.setAssetManager(manager);

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
        SceneGraphRenderer.initialise(manager);
        sgr = SceneGraphRenderer.instance;

        // Initialise scripting gateway server
        ScriptingServer.initialize();

        // Tell the asset manager to load all the assets
        Set<AssetBean> assets = AssetBean.getAssets();
        for (AssetBean ab : assets) {
            ab.load(manager);
        }

        EventManager.instance.subscribe(this, Events.LOAD_DATA_CMD);

        initialGui = new InitialGui(dsDownload, catChooser);
        initialGui.initialize(manager);
        Gdx.input.setInputProcessor(initialGui.getGuiStage());

        // GL clear state
        Gdx.gl.glClearColor(0,0,0,0);
        Gdx.gl.glClearDepthf(1);
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
        }

        /*
         * SCENE GRAPH RENDERER
         */
        AbstractRenderer.initialize(sg);
        sgr.doneLoading(manager);
        sgr.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

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
        inputMultiplexer.addProcessor(new KeyInputController());

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
        EventManager.instance.subscribe(this, Events.TOGGLE_AMBIENT_LIGHT, Events.AMBIENT_LIGHT_CMD, Events.RECORD_CAMERA_CMD, Events.CAMERA_MODE_CMD, Events.STEREOSCOPIC_CMD, Events.FRAME_SIZE_UDPATE, Events.SCREENSHOT_SIZE_UDPATE, Events.POST_RUNNABLE, Events.UNPOST_RUNNABLE, Events.SCENE_GRAPH_ADD_OBJECT_CMD, Events.SCENE_GRAPH_ADD_OBJECT_NO_POST_CMD, Events.SCENE_GRAPH_REMOVE_OBJECT_CMD, Events.HOME_CMD);

        // Re-enable input
        if (!GlobalConf.runtime.STRIPPED_FOV_MODE)
            EventManager.instance.post(Events.INPUT_ENABLED_CMD, true);

        // Set current date
        EventManager.instance.post(Events.TIME_CHANGE_CMD, Instant.now());

        // Resize GUIs to current size
        for (IGui gui : guis)
            gui.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Initialise frames
        frames = 0;

        // Debug info scheduler
        Task debugTask1 = new Task() {
            @Override
            public void run() {
                // FPS
                EventManager.instance.post(Events.FPS_INFO, 1f / Gdx.graphics.getDeltaTime());
                // Current session time
                EventManager.instance.post(Events.DEBUG1, TimeUtils.timeSinceMillis(startTime) / 1000d);
                // Memory
                EventManager.instance.post(Events.DEBUG2, MemInfo.getUsedMemory(), MemInfo.getFreeMemory(), MemInfo.getTotalMemory(), MemInfo.getMaxMemory());
                // Observed objects
                EventManager.instance.post(Events.DEBUG3, "On display: " + OctreeNode.nObjectsObserved + ", Total loaded: " + StreamingOctreeLoader.getNLoadedStars());
                // Observed octants
                EventManager.instance.post(Events.DEBUG4, "Observed octants: " + OctreeNode.nOctantsObserved + ", Load queue: " + StreamingOctreeLoader.getLoadQueueSize(), StreamingOctreeLoader.getLoadQueueSize());
                // Frame buffers
                EventManager.instance.post(Events.DEBUG_BUFFERS, GLFrameBuffer.getManagedStatus());
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

        initialized = true;
    }

    /**
     * Moves the camera home. That is either the Earth, if it exists, or somewhere close to the Sun
     */
    private void goHome() {
        if (sg.containsNode("earth") && !GlobalConf.program.NET_SLAVE && isOn(ComponentType.Planets.ordinal())) {
            // Set focus to Earth
            EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE);
            EventManager.instance.post(Events.FOCUS_CHANGE_CMD, sg.getNode("earth"), true);
            EventManager.instance.post(Events.GO_TO_OBJECT_CMD);
        } else {
            // At 5 AU in Y looking towards origin (top-down look)
            EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FREE_MODE);
            EventManager.instance.post(Events.CAMERA_POS_CMD, (Object) new double[] { 0, 5 * Constants.AU_TO_U, 0 });
            EventManager.instance.post(Events.CAMERA_DIR_CMD, (Object) new double[] { 0, -1, 0 });
            EventManager.instance.post(Events.CAMERA_UP_CMD, (Object) new double[] { 0, 0, 1 });
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

        mainGui = new FullGui();
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
            update(Gdx.graphics.getDeltaTime());

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
                renderSgr(cam, t, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), null, pp.getPostProcessBean(RenderType.screen));

                if (GlobalConf.runtime.DISPLAY_GUI) {
                    // Render the GUI, setting the viewport
                    GuiRegistry.render(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                }
            }
            // Clean lists
            sgr.clearLists();
            // Number of frames
            frames++;

            if (GlobalConf.screen.LIMIT_FPS > 0) {
                sleep(GlobalConf.screen.LIMIT_FPS);
            }
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
        }
    };

    @Override
    public void render() {
        try {
            renderProcess.run();
        } catch (Throwable t) {
            CrashReporter.reportCrash(t, logger);
            // Quit
            Gdx.app.exit();
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
            for (Runnable r : runnables) {
                r.run();
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
        if(!initialized){
            resizeImmediate(width, height, true, true, true);
        }else {
            resizeWidth = width;
            resizeHeight = height;
            lastResizeTime = System.currentTimeMillis();
        }
    }

    private void updateResize(){
        long currResizeTime = System.currentTimeMillis();
        if(currResizeTime - lastResizeTime > 300l) {
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
        gui.update(Gdx.graphics.getDeltaTime());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        gui.render(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public Array<IFocus> getFocusableEntities() {
        return sg.getFocusableObjects();
    }

    public FrameBuffer getFrameBuffer(int w, int h) {
        String key = getKey(w, h);
        if (!fbmap.containsKey(key)) {
            FrameBuffer fb = PingPongBuffer.createMainFrameBuffer(w, h, true, Format.RGB888, true);
            fbmap.put(key, fb);
        }
        return fbmap.get(key);
    }

    private String getKey(int w, int h) {
        return w + "x" + h;
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
            loadingGui = new LoadingGui();
            loadingGui.initialize(manager);

            Gdx.input.setInputProcessor(loadingGui.getGuiStage());
            this.renderProcess = runnableLoadingGui;

            /* LOAD SCENE GRAPH */
            if (sg == null) {
                dataLoadString = TextUtils.concatenate(",", GlobalConf.data.CATALOG_JSON_FILES, GlobalConf.data.OBJECTS_JSON_FILES);
                manager.load(dataLoadString, ISceneGraph.class, new SGLoaderParameter(time, GlobalConf.performance.MULTITHREADING, GlobalConf.performance.NUMBER_THREADS()));
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
            if (stereoMode){
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
            //Gdx.app.postRunnable(() -> {
                //clearFrameBufferMap();
            //});
            break;
        case SCENE_GRAPH_ADD_OBJECT_CMD:
            final SceneGraphNode nodeToAdd = (SceneGraphNode) data[0];
            final boolean addToIndex = data.length == 1 ? true : (Boolean) data[1];
            if (sg != null) {
                Gdx.app.postRunnable(() -> {
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
                Gdx.app.postRunnable(() -> {
                    sg.remove(nodeToRemove, removeFromIndex);
                });
            }
            break;
        case HOME_CMD:
            goHome();
            break;
        case POST_RUNNABLE:
            synchronized (runnables) {
                runnablesMap.put((String) data[0], (Runnable) data[1]);
                runnables.add((Runnable) data[1]);
            }
            break;
        case UNPOST_RUNNABLE:
            synchronized (runnables) {
                Runnable r = runnablesMap.get(data[0]);
                runnables.removeValue(r, true);
                runnablesMap.remove(data[0]);
            }
            break;
        default:
            break;
        }

    }

    public boolean isInitialised() {
        return initialized;
    }

}
