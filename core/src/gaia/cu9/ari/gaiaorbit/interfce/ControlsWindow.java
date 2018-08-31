package gaia.cu9.ari.gaiaorbit.interfce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.components.CameraComponent;
import gaia.cu9.ari.gaiaorbit.interfce.components.DatasetsComponent;
import gaia.cu9.ari.gaiaorbit.interfce.components.MusicComponent;
import gaia.cu9.ari.gaiaorbit.interfce.components.ObjectsComponent;
import gaia.cu9.ari.gaiaorbit.interfce.components.TimeComponent;
import gaia.cu9.ari.gaiaorbit.interfce.components.VisibilityComponent;
import gaia.cu9.ari.gaiaorbit.interfce.components.VisualEffectsComponent;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.scene2d.CollapsiblePane;
import gaia.cu9.ari.gaiaorbit.util.scene2d.CollapsibleWindow;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnScrollPane;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextIconButton;

public class ControlsWindow extends CollapsibleWindow implements IObserver {
    /**
     * The user interface stage
     */
    protected Stage ui;
    protected Skin skin;
    protected VerticalGroup mainVertical;
    protected OwnScrollPane windowScroll;
    protected Table guiLayout;
    protected OwnImageButton recCamera = null, playCamera = null, playstop = null;
    protected TiledDrawable separator;
    /**
     * The scene graph
     */
    private ISceneGraph sg;

    /**
     * Entities that will go in the visibility check boxes
     */
    private ComponentType[] visibilityEntities;
    private boolean[] visible;

    /** Access panes **/
    private Map<String, CollapsiblePane> panes;

    public ControlsWindow(String title, Skin skin, Stage ui) {
        super(title, skin);
        this.setName(title);
        this.skin = skin;
        this.ui = ui;

        /** Global resources **/
        TextureRegion septexreg = ((TextureRegionDrawable) skin.newDrawable("separator")).getRegion();
        septexreg.getTexture().setWrap(TextureWrap.Repeat, TextureWrap.ClampToEdge);
        this.separator = new TiledDrawable(septexreg);

        EventManager.instance.subscribe(this, Events.TOGGLE_TIME_CMD, Events.GUI_SCROLL_POSITION_CMD, Events.GUI_FOLD_CMD, Events.GUI_MOVE_CMD, Events.RECALCULATE_OPTIONS_SIZE);
    }

    public void initialize() {
        /** Global layout **/
        guiLayout = new Table();
        guiLayout.align(Align.left);

        List<Actor> mainActors = new ArrayList<Actor>();
        panes = new HashMap<String, CollapsiblePane>();

        /** ----TIME GROUP---- **/
        playstop = new OwnImageButton(skin, "playstop");
        playstop.setName("play stop");
        playstop.setChecked(GlobalConf.runtime.TIME_ON);
        playstop.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.TOGGLE_TIME_CMD, playstop.isChecked(), true);
                return true;
            }
            return false;
        });
        playstop.addListener(new TextTooltip(txt("gui.tooltip.playstop"), skin));

        TimeComponent timeComponent = new TimeComponent(skin, ui);
        timeComponent.initialize();

        CollapsiblePane time = new CollapsiblePane(ui, txt("gui.time"), timeComponent.getActor(), skin, true, playstop);
        time.align(Align.left);
        mainActors.add(time);
        panes.put(timeComponent.getClass().getSimpleName(), time);

        /** ----CAMERA---- **/
        // Record camera button
        recCamera = new OwnImageButton(skin, "rec");
        recCamera.setName("recCam");
        recCamera.setChecked(GlobalConf.runtime.RECORD_CAMERA);
        recCamera.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.RECORD_CAMERA_CMD, recCamera.isChecked(), true);
                return true;
            }
            return false;
        });
        recCamera.addListener(new TextTooltip(txt("gui.tooltip.reccamera"), skin));

        // Play camera button
        playCamera = new OwnImageButton(skin, "play");
        playCamera.setName("playCam");
        playCamera.setChecked(false);
        playCamera.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.SHOW_PLAYCAMERA_ACTION, ui, skin);
                return true;
            }
            return false;
        });

        playCamera.addListener(new TextTooltip(txt("gui.tooltip.playcamera"), skin));

        CameraComponent cameraComponent = new CameraComponent(skin, ui);
        cameraComponent.initialize();

        CollapsiblePane camera = new CollapsiblePane(ui, txt("gui.camera"), cameraComponent.getActor(), skin, false, recCamera, playCamera);
        camera.align(Align.left);
        mainActors.add(camera);
        panes.put(cameraComponent.getClass().getSimpleName(), camera);

        /** ----OBJECT TOGGLES GROUP---- **/
        VisibilityComponent visibilityComponent = new VisibilityComponent(skin, ui);
        visibilityComponent.setVisibilityEntitites(visibilityEntities, visible);
        visibilityComponent.initialize();

        CollapsiblePane visibility = new CollapsiblePane(ui, txt("gui.visibility"), visibilityComponent.getActor(), skin, false);
        visibility.align(Align.left);
        mainActors.add(visibility);
        panes.put(visibilityComponent.getClass().getSimpleName(), visibility);

        /** ----LIGHTING GROUP---- **/
        VisualEffectsComponent visualEffectsComponent = new VisualEffectsComponent(skin, ui);
        visualEffectsComponent.initialize();

        CollapsiblePane visualEffects = new CollapsiblePane(ui, txt("gui.lighting"), visualEffectsComponent.getActor(), skin, false);
        visualEffects.align(Align.left);
        mainActors.add(visualEffects);
        panes.put(visualEffectsComponent.getClass().getSimpleName(), visualEffects);

        /** ----DATASETS---- **/
        DatasetsComponent datasetsComponent = new DatasetsComponent(skin, ui);
        datasetsComponent.initialize();

        CollapsiblePane datasets = new CollapsiblePane(ui, txt("gui.dataset.title"), datasetsComponent.getActor(), skin, false);
        datasets.align(Align.left);
        mainActors.add(datasets);
        panes.put(datasetsComponent.getClass().getSimpleName(), datasets);

        /** ----OBJECTS TREE---- **/
        ObjectsComponent objectsComponent = new ObjectsComponent(skin, ui);
        objectsComponent.setSceneGraph(sg);
        objectsComponent.initialize();

        CollapsiblePane objects = new CollapsiblePane(ui, txt("gui.objects"), objectsComponent.getActor(), skin, false);
        objects.align(Align.left);
        mainActors.add(objects);
        panes.put(objectsComponent.getClass().getSimpleName(), objects);

        /** ----GAIA SCAN GROUP---- **/
        //	GaiaComponent gaiaComponent = new GaiaComponent(skin, ui);
        //	gaiaComponent.initialize();
        //
        //	CollapsiblePane gaia = new CollapsiblePane(ui, txt("gui.gaiascan"), gaiaComponent.getActor(), skin, false);
        //	gaia.align(Align.left);
        //	mainActors.add(gaia);
        //	panes.put(gaiaComponent.getClass().getSimpleName(), gaia);

        /** ----MUSIC GROUP---- **/
        MusicComponent musicComponent = new MusicComponent(skin, ui);
        musicComponent.initialize();

        Actor[] musicActors = MusicActorsManager.getMusicActors() != null ? MusicActorsManager.getMusicActors().getActors(skin) : null;

        CollapsiblePane music = new CollapsiblePane(ui, txt("gui.music"), musicComponent.getActor(), skin, false, musicActors);
        music.align(Align.left);
        mainActors.add(music);
        panes.put(musicComponent.getClass().getSimpleName(), music);

        Table buttonsTable = null;
        /** BUTTONS **/
        Image prefsImg = new Image(skin.getDrawable("prefs-icon"));
        Button preferences = new OwnTextIconButton("", prefsImg, skin);
        preferences.setName("preferences");
        preferences.addListener(new TextTooltip(txt("gui.preferences"), skin));
        preferences.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    EventManager.instance.post(Events.SHOW_PREFERENCES_ACTION);
                }
                return false;
            }
        });
        Image tutImg = new Image(skin.getDrawable("tutorial-icon"));
        Button tutorial = new OwnTextIconButton("", tutImg, skin);
        tutorial.setName("tutorial");
        tutorial.addListener(new TextTooltip(txt("gui.tutorial"), skin));
        tutorial.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    EventManager.instance.post(Events.SHOW_TUTORIAL_ACTION);
                }
                return false;
            }
        });
        Image helpImg = new Image(skin.getDrawable("help-icon"));
        Button about = new OwnTextIconButton("", helpImg, skin);
        about.setName("about");
        about.addListener(new TextTooltip(txt("gui.help"), skin));
        about.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    EventManager.instance.post(Events.SHOW_ABOUT_ACTION);
                }
                return false;
            }
        });
        Image carImg = new Image(skin.getDrawable("car-icon"));
        Button runScript = new OwnTextIconButton("", carImg, skin);
        runScript.setName("run script");
        runScript.addListener(new TextTooltip(txt("gui.script.runscript"), skin));
        runScript.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    EventManager.instance.post(Events.SHOW_RUNSCRIPT_ACTION, ui, skin);
                }
                return false;
            }
        });
        Image logImg = new Image(skin.getDrawable("log-icon"));
        Button showLog = new OwnTextIconButton("", logImg, skin);
        showLog.setName("show log");
        showLog.addListener(new TextTooltip(txt("gui.log.tooltip"), skin));
        showLog.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.SHOW_LOG_ACTION);
            }
            return false;
        });

        buttonsTable = new Table(skin);
        buttonsTable.add(preferences).pad(1).top().left();
        buttonsTable.add(runScript).pad(1).top().left();
        buttonsTable.add(about).pad(1).top().left();
        buttonsTable.add(showLog).pad(1).top().left();

        buttonsTable.pack();

        /** ADD GROUPS TO VERTICAL LAYOUT **/

        int padBottom = Math.round(10 * GlobalConf.SCALE_FACTOR);
        int padSides = Math.round(5 * GlobalConf.SCALE_FACTOR);
        int padSeparator = Math.round(2 * GlobalConf.SCALE_FACTOR);

        guiLayout.padTop(padSides);

        int size = mainActors.size();
        for (int i = 0; i < size; i++) {
            Actor actor = mainActors.get(i);
            guiLayout.add(actor).prefWidth(175 * GlobalConf.SCALE_FACTOR).left().padBottom(padBottom).padLeft(padSides);
            if (i < size - 1) {
                // Not last
                guiLayout.row();
                guiLayout.add(new Image(separator)).left().fill(true, false).padBottom(padSeparator).padLeft(padSides);
                guiLayout.row();
            }
        }
        guiLayout.align(Align.top | Align.left);
        guiLayout.setWidth(200 * GlobalConf.SCALE_FACTOR);

        windowScroll = new OwnScrollPane(guiLayout, skin, "minimalist-nobg");
        windowScroll.setFadeScrollBars(false);
        windowScroll.setScrollingDisabled(true, false);
        windowScroll.setOverscroll(false, false);
        windowScroll.setSmoothScrolling(true);
        windowScroll.pack();
        windowScroll.setWidth(guiLayout.getWidth() + windowScroll.getStyle().vScroll.getMinWidth());

        mainVertical = new VerticalGroup();
        mainVertical.space(padSides);
        mainVertical.align(Align.right).align(Align.top);
        mainVertical.addActor(windowScroll);
        // Add buttons only in desktop version
        mainVertical.addActor(buttonsTable);
        mainVertical.pack();

        /** ADD TO MAIN WINDOW **/
        add(mainVertical).top().left().expand();
        setPosition(0, Math.round(Gdx.graphics.getHeight() - getHeight()));

        setWidth(mainVertical.getWidth());
        pack();
        recalculateSize();
    }

    public void recalculateSize() {
        // Save position
        float topy = getY() + getHeight();

        // Calculate new size
        guiLayout.pack();
        if (windowScroll != null) {
            windowScroll.setHeight(Math.min(guiLayout.getHeight() + 30, Gdx.graphics.getHeight() - 120));
            windowScroll.pack();

            mainVertical.setHeight(windowScroll.getHeight() + 30);
            mainVertical.pack();

            setHeight(windowScroll.getHeight() + 40);
        }
        pack();
        validate();

        // Restore position
        setY(topy - getHeight());
    }

    public void setSceneGraph(ISceneGraph sg) {
        this.sg = sg;
    }

    public void setVisibilityToggles(ComponentType[] entities, boolean[] visible) {
        this.visibilityEntities = entities;
        this.visible = visible;
    }

    private String txt(String key) {
        return I18n.bundle.get(key);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case TOGGLE_TIME_CMD:
            // Pause has been toggled, update playstop button only if this does
            // not come from this interface
            if (!(Boolean) data[1]) {
                Boolean timeOn = null;
                if (data[0] != null) {
                    timeOn = (Boolean) data[0];
                } else {
                    timeOn = !playstop.isChecked();
                }
                playstop.setCheckedNoFire(timeOn);
            }
            break;
        case GUI_SCROLL_POSITION_CMD:
            this.windowScroll.setScrollY((float) data[0]);
            break;
        case GUI_FOLD_CMD:
            boolean collapse;
            if (data.length >= 1) {
                collapse = (boolean) data[0];
            } else {
                // Toggle
                collapse = !isCollapsed();
            }
            if (collapse) {
                collapse();
            } else {
                expand();
            }
            break;
        case GUI_MOVE_CMD:
            float x = (float) data[0];
            float y = (float) data[1];
            float width = Gdx.graphics.getWidth();
            float height = Gdx.graphics.getHeight();
            float windowWidth = getWidth();
            float windowHeight = getHeight();

            x = MathUtilsd.clamp(x * width, 0, width - windowWidth);
            y = MathUtilsd.clamp(y * height - windowHeight, 0, height - windowHeight);

            setPosition(Math.round(x), Math.round(y));

            break;
        case RECALCULATE_OPTIONS_SIZE:
            recalculateSize();
            break;
        default:
            break;
        }

    }

    public CollapsiblePane getCollapsiblePane(String name) {
        if (panes.containsKey(name)) {
            return panes.get(name);
        } else {
            return null;
        }
    }

}
