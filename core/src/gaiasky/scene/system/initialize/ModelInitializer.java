/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.AssetBean;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
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
import gaiasky.util.*;
import gaiasky.util.coord.SpacecraftCoordinates;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.DepthTestAttribute;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;

public class ModelInitializer extends AbstractInitSystem {
    private static final Logger.Log logger = Logger.getLogger(ModelInitializer.class);

    /**
     * Reference to the spacecraft radio.
     **/
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
            } else if (model.model.params.containsKey("side")) {
                model.modelSize = (Double) model.model.params.get("side");
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
        var orientation = Mapper.orientation.get(entity);
        var engine = Mapper.engine.get(entity);
        var fade = Mapper.fade.get(entity);
        var focus = Mapper.focus.get(entity);
        var bb = Mapper.billboard.get(entity);

        boolean isPlanet = atmosphere != null || cloud != null;
        boolean isSatellite = base.ct.isEnabled(ComponentType.Satellites);
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

        if (engine != null) {
            // In engines, the size is given in Km
            body.size = (float) (body.size * Constants.KM_TO_U);
        } else {
            // The rest of the bodies use the flags.
            body.size = (float) ((body.size * (body.sizeIsRadiusFlag ? 2.0 : 1.0)) * (body.sizeInUnitsFlag ? 1.0 : Constants.KM_TO_U));
        }

        // First init spacecraft if needed
        if (isSpacecraft) {
            initializeSpacecraft(entity, base, body, graph, model, scaffolding, engine, label);
        }

        // Initialize model body
        initializeModel(entity, base, body, model, celestial, bb, sa, label, scaffolding, graph, focus, isBillboardGal);

        // Init billboard
        if (isBillboard) {
            initializeBillboard(scaffolding, sa, label, isBillboardGal);
        }

        if (isSatellite) {
            initializeSatellite(scaffolding, sa, label);
        }

        if (isPlanet) {
            // Initialize planet
            initializePlanet(base, body, model, scaffolding, sa, label, atmosphere, cloud);
        }

        if (orientation != null) {
            initializeOrientation(orientation);
        }
    }

    @Override
    public void setUpEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var model = Mapper.model.get(entity);
        var atmosphere = Mapper.atmosphere.get(entity);
        var cloud = Mapper.cloud.get(entity);
        var orientation = Mapper.orientation.get(entity);
        var parentOrientation = Mapper.parentOrientation.get(entity);
        var engine = Mapper.engine.get(entity);
        var fade = Mapper.fade.get(entity);

        AssetManager manager = AssetBean.manager();
        if (model != null && model.model != null) {
            // All models.
            model.model.doneLoading(manager, graph.localTransform, body.color);
            // Initialize tessellated.
            model.model.tessellated = Settings.settings.scene.renderer.elevation.type.isTessellation() && body.size > 500.0 * Constants.KM_TO_U;
            // Set units.
            model.model.setUnits(Constants.KM_TO_U);
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
                parentOrientation.setRigidRotation(graph.parent.getComponent(Orientation.class).rotationComponent);
            }
            parentOrientation.orientationf = new Matrix4();
        }

        if (orientation != null) {
            // Quaternion-based orientation.
            orientation.setUp(manager);
        }
        if (engine != null) {
            // Spacecraft radio.
            EventManager.publish(Event.SPACECRAFT_LOADED, this, entity);
            if (this.radio != null) {
                EventManager.instance.unsubscribe(this.radio, Event.CAMERA_MODE_CMD);
                this.radio = null;
            }
            EventManager.instance.subscribe(new SpacecraftRadio(entity),
                                            Event.CAMERA_MODE_CMD,
                                            Event.SPACECRAFT_STABILISE_CMD,
                                            Event.SPACECRAFT_STOP_CMD,
                                            Event.SPACECRAFT_THRUST_DECREASE_CMD,
                                            Event.SPACECRAFT_THRUST_INCREASE_CMD,
                                            Event.SPACECRAFT_THRUST_SET_CMD,
                                            Event.SPACECRAFT_MACHINE_SELECTION_CMD);
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

    private void initializeSpacecraft(Entity entity,
                                      Base base,
                                      Body body,
                                      GraphNode graph,
                                      Model model,
                                      ModelScaffolding scaffolding,
                                      MotorEngine engine,
                                      Label label) {

        // Model renderer.
        model.renderConsumer = ModelEntityRenderSystem::renderSpacecraft;

        // Updater.
        graph.positionUpdaterConsumer = GraphUpdater::updateSpacecraft;

        base.ct = new ComponentTypes(ComponentType.Satellites);
        engine.rotationMatrix = new Matrix4();

        this.radio = new SpacecraftRadio(entity);
        EventManager.instance.subscribe(this.radio, Event.CAMERA_MODE_CMD);

        // position attributes
        engine.force = new Vector3D();
        engine.accel = new Vector3D();
        engine.vel = new Vector3D();

        // position and orientation
        body.pos.set(1e7 * Constants.KM_TO_U, 0, 1e8 * Constants.KM_TO_U);
        engine.direction = new Vector3D(1, 0, 0);
        engine.up = new Vector3D(0, 1, 0);
        engine.dirup = new Pair<>(engine.direction, engine.up);

        engine.posf = new Vector3();
        engine.directionf = new Vector3(1, 0, 0);
        engine.upf = new Vector3(0, 1, 0);

        // engine thrust direction
        // our spacecraft is a rigid solid so thrust is always the camera direction vector
        engine.thrust = new Vector3D(engine.direction).scl(engine.thrustMagnitude);
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

        // No label.
        label.label = false;
        label.labelMax = 0;
        label.renderConsumer = null;
        label.labelFactor = 0;
    }

    private void initializeModel(Entity entity,
                                 Base base,
                                 Body body,
                                 Model model,
                                 Celestial celestial,
                                 Billboard bb,
                                 SolidAngle sa,
                                 Label label,
                                 ModelScaffolding scaffolding,
                                 GraphNode graph,
                                 Focus focus,
                                 boolean isBillboardGal) {

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
        graph.orientation = new Matrix4D();

        scaffolding.billboardSizeFactor = 2.0f;
        scaffolding.locThresholdLabel = 1000.0f;
        scaffolding.locVaMultiplier = 2.8f;

        sa.thresholdPoint = FastMath.toRadians(0.30);
        sa.thresholdLabel = (Math.toRadians(1e-6) / Settings.settings.scene.label.number) * (base.ct.get(ComponentType.Moons.ordinal()) ? 3000.0 : 25.0);
        if (isBillboardGal) {
            sa.thresholdQuad = FastMath.toRadians(0.3);
        }

        label.labelMax = (float) (0.5e-4 / Constants.DISTANCE_SCALE_FACTOR);
        if (label.labelFactor == 0) {
            label.labelFactor = 1;
        }

        if (isRandomizeSurface(scaffolding)) {
            // Ignore current model component (if any) and create a random one
            model.model = new ModelComponent(true);
            model.model.randomizeAll(scaffolding.getSeed("model"), body.size);
            if (Settings.settings.program.debugInfo) {
                logger.debug("::" + base.getName() + "::");
                logger.debug("============MODEL===========");
                model.model.print(logger);
            }
        }

        // Clouds also act as ambient occlusion to models.
        if (model.model != null) {
            if (Mapper.cloud.has(entity)) {
                var cloud = Mapper.cloud.get(entity);
                if (model.model.mtc != null && cloud.cloud != null) {
                    // Do not add cloud occlusion with current configuration if clouds are pulled from URL.
                    if (!TextUtils.isValidURL(cloud.cloud.url)) {
                        if (cloud.cloud.diffuseSvt != null && cloud.cloud.svtParams != null) {
                            // Cloud shadows unsupported with SVT.
                            // This is because we can't ensure that the cloud SVT is exactly the same (levels, size, etc.) as the
                            // main diffuse SVT, and the visibility determination happens only once per layer.
                            cloud.cloud.diffuse = null;
                            cloud.cloud.diffuseCubemap = null;
                            //model.model.mtc.setAoSVT(cloud.cloud.svtParams);
                            //model.model.mtc.setOcclusionClouds(true);
                        } else if (cloud.cloud.diffuse != null && !cloud.cloud.diffuse.endsWith(Constants.GEN_KEYWORD)) {
                            model.model.mtc.ao = cloud.cloud.diffuse;
                            model.model.mtc.setOcclusionClouds(true);
                        } else if (cloud.cloud.diffuseCubemap != null) {
                            model.model.mtc.setAmbientOcclusionCubemap(cloud.cloud.diffuseCubemap.location);
                            model.model.mtc.setOcclusionClouds(true);
                        }
                    }
                }
            }

            // Initialize model.
            model.model.initialize(base.getName());
        }
    }

    private void initializeBillboard(ModelScaffolding scaffolding, SolidAngle sa, Label label, boolean isBillboardGalaxy) {
        double baseThreshold = FastMath.toRadians(0.30);
        sa.thresholdLabel = FastMath.toRadians(0.2);
        sa.thresholdNone = 0.002;
        sa.thresholdPoint = baseThreshold / 1e9;
        if (!isBillboardGalaxy) {
            sa.thresholdQuad = baseThreshold / 8.0;
        }

        label.textScale = 0.3f;
        label.solidAnglePow = 1f;
        label.labelFactor = (float) (1e1f * Constants.DISTANCE_SCALE_FACTOR);

        scaffolding.billboardSizeFactor = 0.6e-3f;
    }

    private void initializePlanet(Base base, Body body, Model model, ModelScaffolding scaffolding, SolidAngle
            sa, Label label, Atmosphere atmosphere, Cloud cloud) {
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
            atmosphere.atmosphere.randomizeAll(scaffolding.getSeed("atmosphere"), body.size / 2.0);
            logger.debug("============ATM===========");
            atmosphere.atmosphere.print(logger);
        }
        if (cloud.cloud != null) {
            cloud.cloud.materialComponent = model.model.mtc;
            cloud.cloud.initialize(base.getName(), false);
        }
    }

    public void initializeSatellite(ModelScaffolding scaffolding, SolidAngle sa, Label label) {
        double thPoint = sa.thresholdPoint;
        sa.thresholdNone = thPoint / 1e18;
        sa.thresholdPoint = thPoint / 3.3e10;
        sa.thresholdQuad = thPoint / 8.0;
        sa.thresholdLabel = (Math.toRadians(1e-7) / Settings.settings.scene.label.number);
        label.labelFactor = (float) (0.5e1 * Constants.DISTANCE_SCALE_FACTOR);
        label.labelMax = label.labelMax * 2f;

        scaffolding.billboardSizeFactor = 10f;

    }

    public void initializeOrientation(Orientation orientation) {
        orientation.initialize(AssetBean.manager());
    }

    public boolean isRandomizeSurface(ModelScaffolding scaffolding) {
        return scaffolding.randomize != null && (scaffolding.randomize.contains("surface") || scaffolding.randomize.contains("model"));
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
    private void setToMachine(final MachineDefinition machine, final boolean initialize, Body body, Model
            model, ModelScaffolding scaffolding, MotorEngine engine) {
        model.model = machine.getModel();
        engine.thrustMagnitude = machine.getPower() * MotorEngine.thrustBase;
        engine.fullPowerTime = machine.getFullpowertime();
        engine.mass = machine.getMass();
        scaffolding.selfShadow = machine.isSelfShadow();
        engine.drag = machine.getDrag();
        engine.responsiveness = MathUtilsDouble.lint(machine.getResponsiveness(),
                                                     0d,
                                                     1d,
                                                     Constants.MIN_SC_RESPONSIVENESS,
                                                     Constants.MAX_SC_RESPONSIVENESS);
        engine.machineName = machine.getName();
        body.setSize(machine.getSize() * Constants.KM_TO_U);

        if (initialize) {
            // Neither loading nor initialized
            if (!model.model.isModelLoading() && !model.model.isModelInitialised()) {
                model.model.initialize(null);
            }
        }
    }

    private void initializeAtmosphere(AssetManager manager, AtmosphereComponent atmosphereComponent, ModelComponent
            modelComponent, float size) {
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
