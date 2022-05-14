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
import gaiasky.scene.entity.SpacecraftRadio;
import gaiasky.scenegraph.MachineDefinition;
import gaiasky.scenegraph.Planet;
import gaiasky.scenegraph.component.AtmosphereComponent;
import gaiasky.scenegraph.component.CloudComponent;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.DepthTestAttribute;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

/**
 * Initializes the old ModelBody objects, together with Planet, Satellite,
 * HeliotropicSatellite, GenericSpacecraft, Spacecraft, Billboard and
 * BillboardGalaxy.
 */
public class ModelInitializer extends InitSystem {

    public ModelInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
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
        var text = Mapper.text.get(entity);
        var atmosphere = Mapper.atmosphere.get(entity);
        var cloud = Mapper.cloud.get(entity);
        var attitude = Mapper.attitude.get(entity);
        var engine = Mapper.engine.get(entity);
        var fade = Mapper.fade.get(entity);

        boolean isPlanet = atmosphere != null || cloud != null;
        boolean isSatellite = attitude != null;
        boolean isSpacecraft = engine != null;
        boolean isBillboard = fade != null;

        // In celestial bodies, size is given as a radius in Km. The size is the diameter in internal units.
        body.size = (float) ((body.size * 2.0) * Constants.KM_TO_U);

        // First init spacecraft if needed
        if (isSpacecraft) {
            initializeSpacecraft(base, body, model, scaffolding, engine);
        }

        // Initialize model body
        initializeModel(base, body, model, sa, text, scaffolding, graph);

        // Init billboard
        if (isBillboard) {
            initializeBillboard(sa, text);
        }

        if (isSatellite) {
            initializeSatellite(attitude);
        }

        if (isPlanet) {
            // Initialize planet
            initializePlanet(base, body, scaffolding, atmosphere, cloud);
            setColor2Data(body, celestial, 0.6f);

        } else {
            setColor2Data(body, celestial, 0.1f);
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
            // All models
            model.model.doneLoading(manager, graph.localTransform, body.color);
        }
        if (atmosphere != null || cloud != null) {
            // Planets
            initializeAtmosphere(manager, atmosphere.atmosphere, model.model, body.size);
            initializeClouds(manager, cloud.cloud);
        }
        if (parentOrientation != null) {
            // Satellites
            if (parentOrientation.parentOrientation) {
                parentOrientation.parentrc = graph.parent.getComponent(Rotation.class).rc;
            }
            parentOrientation.orientationf = new Matrix4();
        }
        if (attitude != null) {
            // Heliotropic satellites
            if (attitude.attitudeLocation != null && manager.isLoaded(attitude.attitudeLocation)) {
                attitude.attitudeServer = manager.get(attitude.attitudeLocation);
            }
        }
        if (engine != null) {
            // Spacecraft
            // Broadcast me
            // TODO activate
            //EventManager.publish(Event.SPACECRAFT_LOADED, this, this);

            EventManager.instance.subscribe(new SpacecraftRadio(entity), Event.CAMERA_MODE_CMD, Event.SPACECRAFT_STABILISE_CMD, Event.SPACECRAFT_STOP_CMD, Event.SPACECRAFT_THRUST_DECREASE_CMD, Event.SPACECRAFT_THRUST_INCREASE_CMD, Event.SPACECRAFT_THRUST_SET_CMD, Event.SPACECRAFT_MACHINE_SELECTION_CMD);
        }
        if (fade != null) {
            // Billboards -- add depth test attribute, set to false
            if (model.model != null && model.model.instance != null) {
                // Disable depth test
                Array<gaiasky.util.gdx.shader.Material> mats = model.model.instance.materials;
                for (Material mat : mats) {
                    mat.set(new DepthTestAttribute(false));
                }
            }

        }
    }

    private void initializeSpacecraft(Base base, Body body, Model model, ModelScaffolding scaffolding, MotorEngine engine) {
        base.ct = new ComponentTypes(ComponentType.Satellites);
        engine.rotationMatrix = new Matrix4();

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
    }

    private void initializeModel(Base base, Body body, Model model, SolidAngle sa, Text text, ModelScaffolding scaffolding, GraphNode graph) {
        // Default values
        graph.orientation = new Matrix4d();

        sa.thresholdPoint = Math.toRadians(0.30);
        sa.thresholdFactor = (float) (sa.thresholdPoint / Settings.settings.scene.label.number);

        text.labelMax = (float) (0.5e-4 / Constants.DISTANCE_SCALE_FACTOR);

        if (isRandomizeModel(scaffolding)) {
            // Ignore current model component (if any) and create a random one
            model.model = new ModelComponent(true);
            model.model.randomizeAll(scaffolding.getSeed("model"), body.size);
            if (Settings.settings.program.debugInfo) {
                Logger.getLogger(Planet.class).debug("::" + base.getName() + "::");
                Logger.getLogger(Planet.class).debug("============MODEL===========");
                model.model.print(Logger.getLogger(Planet.class));
            }
        }
        if (model.model != null) {
            model.model.initialize(base.getName());
        }
    }

    private void initializeBillboard(SolidAngle sa, Text text) {
        double thPoint = sa.thresholdPoint;
        sa.thresholdNone = 0.002;
        sa.thresholdPoint = thPoint / 1e9;
        sa.thresholdQuad = thPoint / 8;

        text.labelFactor = 1e1f;
    }

    private void initializePlanet(Base base, Body body, ModelScaffolding scaffolding, Atmosphere atmosphere, Cloud cloud) {
        if (isRandomizeCloud(scaffolding)) {
            // Ignore current cloud component (if any) and create a random one
            cloud.cloud = new CloudComponent();
            cloud.cloud.randomizeAll(scaffolding.getSeed("cloud"), body.size);
            Logger.getLogger(Planet.class).debug("============CLOUD===========");
            cloud.cloud.print(Logger.getLogger(Planet.class));
        }
        if (isRandomizeAtmosphere(scaffolding)) {
            // Ignore current atmosphere component (if any) and create a random one
            atmosphere.atmosphere = new AtmosphereComponent();
            atmosphere.atmosphere.randomizeAll(scaffolding.getSeed("atmosphere"), body.size);
            Logger.getLogger(Planet.class).debug("============ATM===========");
            atmosphere.atmosphere.print(Logger.getLogger(Planet.class));
        }
        if (cloud.cloud != null) {
            cloud.cloud.initialize(base.getName(), false);
        }
    }

    public void initializeSatellite(Attitude attitude) {
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

    protected void setColor2Data(final Body body, final Celestial celestial, final float plus) {
        celestial.colorPale = new float[] { Math.min(1, body.color[0] + plus), Math.min(1, body.color[1] + plus), Math.min(1, body.color[2] + plus) };
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
        engine.responsiveness = MathUtilsd.lint(machine.getResponsiveness(), 0d, 1d, Constants.MIN_SC_RESPONSIVENESS, Constants.MAX_SC_RESPONSIVENESS);
        engine.machineName = machine.getName();
        body.setSize(machine.getSize());

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
