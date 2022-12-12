package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.AssetBean;
import gaiasky.data.attitude.IAttitudeServer;
import gaiasky.data.util.AttitudeLoader.AttitudeLoaderParameters;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.FocusActive;
import gaiasky.scene.entity.FocusHit;
import gaiasky.scene.entity.SpacecraftRadio;
import gaiasky.scene.record.AtmosphereComponent;
import gaiasky.scene.record.CloudComponent;
import gaiasky.scene.record.MachineDefinition;
import gaiasky.scene.record.ModelComponent;
import gaiasky.scene.system.render.draw.billboard.BillboardEntityRenderSystem;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.system.update.GraphUpdater;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.coord.SpacecraftCoordinates;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.DepthTestAttribute;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

/**
 * Initializes the old ModelBody objects, together with Planet, Satellite,
 * HeliotropicSatellite, GenericSpacecraft, Spacecraft, Billboard and
 * BillboardGalaxy.
 */
public class ModelInitializer extends AbstractInitSystem {
    private static final Logger.Log logger = Logger.getLogger(ModelInitializer.class.getSimpleName());

    /** Reference to the spacecraft radio. **/
    private SpacecraftRadio radio;

    public ModelInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    public static void initializeModelSize(Model model) {
        if (model.model != null && model.model.params != null) {
            if (model.model.params.containsKey("diameter")) {
                model.modelSize = (Double) model.model.params.get("diameter");
            } else if (model.model.params.containsKey("size")) {
                model.modelSize = (Double) model.model.params.get("size");
            } else if (model.model.params.containsKey("width")) {
                model.modelSize = (Double) model.model.params.get("width");
            } else if (model.model.params.containsKey("height")) {
                model.modelSize = (Double) model.model.params.get("height");
            } else if (model.model.params.containsKey("depth")) {
                model.modelSize = (Double) model.model.params.get("depth");
            }
        }
    }

    @Override
    public void initializeEntity(Entity entity) {
        // Component retrieval
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var celestial = Mapper.celestial.get(entity);
        var model = Mapper.model.get(entity);
        var scaffolding = Mapper.modelScaffolding.get(entity);
        var sa = Mapper.sa.get(entity);
        var label = Mapper.label.get(entity);
        var atmosphere = Mapper.atmosphere.get(entity);
        var cloud = Mapper.cloud.get(entity);
        var attitude = Mapper.attitude.get(entity);
        var engine = Mapper.engine.get(entity);
        var fade = Mapper.fade.get(entity);
        var focus = Mapper.focus.get(entity);
        var bb = Mapper.billboard.get(entity);

        boolean isPlanet = atmosphere != null || cloud != null;
        boolean isSatellite = attitude != null;
        boolean isSpacecraft = engine != null;
        boolean isBillboard = fade != null;
        boolean isBillboardGal = Mapper.tagBillboardGalaxy.has(entity);

        // Focus hits.
        focus.hitCoordinatesConsumer = FocusHit::addHitCoordinateModel;
        focus.hitRayConsumer = FocusHit::addHitRayModel;

        // All celestial labels use the same consumer.
        label.label = true;
        label.renderConsumer = LabelEntityRenderSystem::renderCelestial;
        label.renderFunction = LabelView::renderTextCelestial;

        if (!Mapper.tagQuatOrientation.has(entity)) {
            // In celestial bodies, size is given as a radius in Km. The size is the diameter in internal units.
            body.size = (float) ((body.size * 2.0) * Constants.KM_TO_U);
        } else if (engine != null) {
            body.size = (float) (body.size * Constants.KM_TO_U);
        } else {
            // Billboards, just double it.
            body.size = body.size * 2f;
        }

        // First init spacecraft if needed
        if (isSpacecraft) {
            initializeSpacecraft(entity, base, body, graph, model, scaffolding, engine);
        }

        // Initialize model body
        initializeModel(base, body, model, celestial, bb, sa, label, scaffolding, graph, focus, isBillboardGal);

        // Init billboard
        if (isBillboard) {
            initializeBillboard(scaffolding, sa, label);
        }

        if (isSatellite) {
            initializeSatellite(attitude, scaffolding, sa, label);
        }

        if (isPlanet) {
            // Initialize planet
            initializePlanet(base, body, model, scaffolding, sa, label, atmosphere, cloud);
            EntityUtils.setColor2Data(body, celestial, 0.6f);
        } else {
            EntityUtils.setColor2Data(body, celestial, 0.1f);
        }
    }

    @Override
    public void setUpEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var model = Mapper.model.get(entity);
        var atmosphere = Mapper.atmosphere.get(entity);
        var cloud = Mapper.cloud.get(entity);
        var attitude = Mapper.attitude.get(entity);
        var parentOrientation = Mapper.parentOrientation.get(entity);
        var engine = Mapper.engine.get(entity);
        var fade = Mapper.fade.get(entity);

        AssetManager manager = AssetBean.manager();
        if (model != null && model.model != null) {
            // All models.
            model.model.doneLoading(manager, graph.localTransform, body.color);
        }
        if (atmosphere != null && model != null) {
            initializeAtmosphere(manager, atmosphere.atmosphere, model.model, body.size);
        }
        if (cloud != null) {
            initializeClouds(manager, cloud.cloud);
        }

        if (parentOrientation != null) {
            // Satellites.
            if (parentOrientation.parentOrientation) {
                parentOrientation.parentrc = graph.parent.getComponent(Rotation.class).rc;
            }
            parentOrientation.orientationf = new Matrix4();
        }
        if (attitude != null) {
            // Attitude-based models.
            if (attitude.attitudeLocation != null && manager.isLoaded(attitude.attitudeLocation)) {
                attitude.attitudeServer = manager.get(attitude.attitudeLocation);
            }
        }
        if (engine != null) {
            // Spacecraft radio.
            EventManager.publish(Event.SPACECRAFT_LOADED, this, entity);
            if (this.radio != null) {
                EventManager.instance.unsubscribe(this.radio, Event.CAMERA_MODE_CMD);
                this.radio = null;
            }
            EventManager.instance.subscribe(new SpacecraftRadio(entity), Event.CAMERA_MODE_CMD, Event.SPACECRAFT_STABILISE_CMD, Event.SPACECRAFT_STOP_CMD, Event.SPACECRAFT_THRUST_DECREASE_CMD, Event.SPACECRAFT_THRUST_INCREASE_CMD, Event.SPACECRAFT_THRUST_SET_CMD, Event.SPACECRAFT_MACHINE_SELECTION_CMD);
        }
        if (fade != null) {
            // Billboards -- add depth test attribute, set to false.
            if (model != null && model.model != null && model.model.instance != null) {
                // Disable depth test.
                Array<gaiasky.util.gdx.shader.Material> mats = model.model.instance.materials;
                for (Material mat : mats) {
                    mat.set(new DepthTestAttribute(false));
                }
            }

        }
    }

    private void initializeSpacecraft(Entity entity, Base base, Body body, GraphNode graph, Model model, ModelScaffolding scaffolding, MotorEngine engine) {
        // Model renderer.
        model.renderConsumer = ModelEntityRenderSystem::renderSpacecraft;

        // Updater.
        graph.positionUpdaterConsumer = GraphUpdater::updateSpacecraft;

        base.ct = new ComponentTypes(ComponentType.Satellites);
        engine.rotationMatrix = new Matrix4();

        this.radio = new SpacecraftRadio(entity);
        EventManager.instance.subscribe(this.radio, Event.CAMERA_MODE_CMD);

        // position attributes
        engine.force = new Vector3d();
        engine.accel = new Vector3d();
        engine.vel = new Vector3d();

        // position and orientation
        body.pos.set(1e7 * Constants.KM_TO_U, 0, 1e8 * Constants.KM_TO_U);
        engine.direction = new Vector3d(1, 0, 0);
        engine.up = new Vector3d(0, 1, 0);
        engine.dirup = new Pair<>(engine.direction, engine.up);

        engine.posf = new Vector3();
        engine.directionf = new Vector3(1, 0, 0);
        engine.upf = new Vector3(0, 1, 0);

        // engine thrust direction
        // our spacecraft is a rigid solid so thrust is always the camera direction vector
        engine.thrust = new Vector3d(engine.direction).scl(engine.thrustMagnitude);
        engine.currentEnginePower = 0;

        // not stabilising
        engine.leveling = false;

        engine.qf = new Quaternion();

        // Use first model
        setToMachine(engine.machines[engine.currentMachine], false, body, model, scaffolding, engine);

        // Coordinates
        SpacecraftCoordinates scc = new SpacecraftCoordinates();
        scc.setSpacecraft(engine);

        var coord = Mapper.coordinates.get(entity);
        coord.coordinates = scc;

        var label = Mapper.label.get(entity);
        label.labelFactor = 0;
    }

    private void initializeModel(Base base, Body body, Model model, Celestial celestial, Billboard bb, SolidAngle sa, Label label, ModelScaffolding scaffolding, GraphNode graph, Focus focus, boolean isBillboardGal) {
        // Billboard.
        bb.renderConsumer = isBillboardGal ? BillboardEntityRenderSystem::renderBillboardGalaxy : BillboardEntityRenderSystem::renderBillboardCelestial;

        // Focus consumer.
        focus.activeFunction = FocusActive::isFocusActiveTrue;

        // Model renderer.
        if (model.renderConsumer == null) {
            model.renderConsumer = ModelEntityRenderSystem::renderGenericModel;
        }
        // Model size. Used to compute an accurate solid angle.
        initializeModelSize(model);

        // Default values
        celestial.innerRad = 0.2f;
        graph.orientation = new Matrix4d();

        scaffolding.billboardSizeFactor = 2.0f;
        scaffolding.locThresholdLabel = 1000.0f;
        scaffolding.locVaMultiplier = 2.8f;

        sa.thresholdPoint = Math.toRadians(0.30);
        sa.thresholdLabel = (Math.toRadians(1e-6) / Settings.settings.scene.label.number)
                * (base.ct.get(ComponentType.Moons.ordinal()) ? 3000.0 : 25.0);

        label.labelMax = (float) (0.5e-4 / Constants.DISTANCE_SCALE_FACTOR);
        label.labelFactor = 1;

        if (isRandomizeModel(scaffolding)) {
            // Ignore current model component (if any) and create a random one
            model.model = new ModelComponent(true);
            model.model.randomizeAll(scaffolding.getSeed("model"), body.size);
            if (Settings.settings.program.debugInfo) {
                logger.debug("::" + base.getName() + "::");
                logger.debug("============MODEL===========");
                model.model.print(logger);
            }
        }
        if (model.model != null) {
            model.model.initialize(base.getName());
        }
    }

    private void initializeBillboard(ModelScaffolding scaffolding, SolidAngle sa, Label label) {
        double baseThreshold = Math.toRadians(0.30);
        sa.thresholdNone = 0.002;
        sa.thresholdPoint = baseThreshold / 1e9;
        sa.thresholdQuad = baseThreshold / 8.0;
        sa.thresholdLabel = Math.toRadians(0.2);

        label.textScale = 0.3f;
        label.solidAnglePow = 1f;
        label.labelFactor = (float) (1e1f * Constants.DISTANCE_SCALE_FACTOR);

        scaffolding.billboardSizeFactor = 0.6e-3f;
    }

    private void initializePlanet(Base base, Body body, Model model, ModelScaffolding scaffolding, SolidAngle sa, Label label, Atmosphere atmosphere, Cloud cloud) {
        model.renderConsumer = ModelEntityRenderSystem::renderPlanet;

        double thPoint = sa.thresholdPoint;
        sa.thresholdNone = thPoint / 1e6;
        sa.thresholdPoint = thPoint / 3e4;
        sa.thresholdQuad = thPoint / 2.0;
        label.labelFactor = (float) (1.5e1 * Constants.DISTANCE_SCALE_FACTOR);

        if (isRandomizeCloud(scaffolding)) {
            // Ignore current cloud component (if any) and create a random one
            cloud.cloud = new CloudComponent();
            cloud.cloud.randomizeAll(scaffolding.getSeed("cloud"), body.size);
            logger.debug("============CLOUD===========");
            cloud.cloud.print(logger);
        }
        if (isRandomizeAtmosphere(scaffolding)) {
            // Ignore current atmosphere component (if any) and create a random one
            atmosphere.atmosphere = new AtmosphereComponent();
            atmosphere.atmosphere.randomizeAll(scaffolding.getSeed("atmosphere"), body.size);
            logger.debug("============ATM===========");
            atmosphere.atmosphere.print(logger);
        }
        if (cloud.cloud != null) {
            cloud.cloud.initialize(base.getName(), false);
        }
    }

    public void initializeSatellite(Attitude attitude, ModelScaffolding scaffolding, SolidAngle sa, Label label) {
        double thPoint = sa.thresholdPoint;
        sa.thresholdNone = thPoint / 1e18;
        sa.thresholdPoint = thPoint / 3.3e10;
        sa.thresholdQuad = thPoint / 8.0;
        sa.thresholdLabel = (Math.toRadians(1e-7) / Settings.settings.scene.label.number);
        label.labelFactor = (float) (0.5e1 * Constants.DISTANCE_SCALE_FACTOR);
        label.labelMax = label.labelMax * 2f;

        scaffolding.billboardSizeFactor = 10f;
        attitude.nonRotatedPos = new Vector3d();
        if (attitude.attitudeLocation != null && !attitude.attitudeLocation.isBlank()) {
            AssetBean.manager().load(attitude.attitudeLocation, IAttitudeServer.class, new AttitudeLoaderParameters(attitude.provider));
        }
    }

    public boolean isRandomizeModel(ModelScaffolding scaffolding) {
        return scaffolding.randomize != null && scaffolding.randomize.contains("model");
    }

    protected boolean isRandomizeAtmosphere(ModelScaffolding scaffolding) {
        return scaffolding.randomize != null && scaffolding.randomize.contains("atmosphere");
    }

    protected boolean isRandomizeCloud(ModelScaffolding scaffolding) {
        return scaffolding.randomize != null && scaffolding.randomize.contains("cloud");
    }

    /**
     * Sets this spacecraft to the given machine definition.
     *
     * @param machine The machine definition.
     */
    private void setToMachine(final MachineDefinition machine, final boolean initialize, Body body, Model model, ModelScaffolding scaffolding, MotorEngine engine) {
        model.model = machine.getModel();
        engine.thrustMagnitude = machine.getPower() * engine.thrustBase;
        engine.fullPowerTime = machine.getFullpowertime();
        engine.mass = machine.getMass();
        scaffolding.shadowMapValues = machine.getShadowvalues();
        engine.drag = machine.getDrag();
        engine.responsiveness = MathUtilsDouble.lint(machine.getResponsiveness(), 0d, 1d, Constants.MIN_SC_RESPONSIVENESS, Constants.MAX_SC_RESPONSIVENESS);
        engine.machineName = machine.getName();
        body.setSize(machine.getSize() * Constants.KM_TO_U);

        if (initialize) {
            // Neither loading nor initialized
            if (!model.model.isModelLoading() && !model.model.isModelInitialised()) {
                model.model.initialize(null);
            }
        }
    }

    private void initializeAtmosphere(AssetManager manager, AtmosphereComponent atmosphereComponent, ModelComponent modelComponent, float size) {
        if (atmosphereComponent != null) {
            // Initialize atmosphere model
            atmosphereComponent.doneLoading(modelComponent.instance.materials.first(), size);
        }
    }

    private void initializeClouds(AssetManager manager, CloudComponent cloudComponent) {
        if (cloudComponent != null) {
            cloudComponent.doneLoading(manager);
        }
    }
}
