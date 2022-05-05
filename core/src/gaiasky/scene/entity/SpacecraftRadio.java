package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.Model;
import gaiasky.scene.component.ModelScaffolding;
import gaiasky.scene.component.MotorEngine;
import gaiasky.scenegraph.MachineDefinition;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.MathUtilsd;

/**
 * Picks up spacecraft events and relays them to the entity.
 */
public class SpacecraftRadio extends EntityRadio {
    private static final Log logger = Logger.getLogger(SpacecraftRadio.class);

    public SpacecraftRadio(Entity entity) {
        super(entity);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        var engine = entity.getComponent(MotorEngine.class);
        switch (event) {
        case CAMERA_MODE_CMD:
            CameraMode mode = (CameraMode) data[0];
            engine.render = mode == CameraMode.SPACECRAFT_MODE;
            break;
        case SPACECRAFT_STABILISE_CMD:
            engine.leveling = (Boolean) data[0];
            break;
        case SPACECRAFT_STOP_CMD:
            engine.stopping = (Boolean) data[0];
            break;
        case SPACECRAFT_THRUST_DECREASE_CMD:
            decreaseThrustFactorIndex(engine, true);
            break;
        case SPACECRAFT_THRUST_INCREASE_CMD:
            increaseThrustFactorIndex(engine, true);
            break;
        case SPACECRAFT_THRUST_SET_CMD:
            setThrustFactorIndex(engine, (Integer) data[0], false);
            break;
        case SPACECRAFT_MACHINE_SELECTION_CMD:
            int newMachineIndex = (Integer) data[0];
            // Update machine
            GaiaSky.postRunnable(() -> {
                setToMachine(entity.getComponent(Body.class),
                        entity.getComponent(Model.class),
                        entity.getComponent(ModelScaffolding.class),
                        engine,
                        engine.machines[newMachineIndex], true);
                engine.currentMachine = newMachineIndex;
                // TODO activate
                //EventManager.publish(Event.SPACECRAFT_MACHINE_SELECTION_INFO, this, engine.machines[newMachineIndex]);
            });
            break;
        default:
            break;
        }

    }

    /**
     * Sets this spacecraft to the given machine definition.
     *
     * @param machine The machine definition.
     */
    private void setToMachine(final Body body, final Model model, final ModelScaffolding scaffolding, final MotorEngine engine, final MachineDefinition machine, final boolean initialize) {
        model.model = machine.getModel();
        engine.thrustMagnitude = machine.getPower() * engine.thrustBase;
        engine.fullPowerTime = machine.getFullpowertime();
        engine.mass = machine.getMass();
        scaffolding.shadowMapValues = machine.getShadowvalues();
        engine.drag = machine.getDrag();
        engine.responsiveness = MathUtilsd.lint(machine.getResponsiveness(), 0d, 1d, Constants.MIN_SC_RESPONSIVENESS, Constants.MAX_SC_RESPONSIVENESS);
        engine.machineName = machine.getName();
        body.setSizeKm(machine.getSize());

        if (initialize) {
            // Neither loading nor initialized
            if (!model.model.isModelLoading() && !model.model.isModelInitialised()) {
                model.model.initialize(null);
            }
        }
    }

    public void increaseThrustFactorIndex(MotorEngine engine, boolean broadcast) {
        engine.thrustFactorIndex = (engine.thrustFactorIndex + 1) % engine.thrustFactor.length;
        logger.info("Thrust factor: " + engine.thrustFactor[engine.thrustFactorIndex]);
        if (broadcast) {
            // TODO activate
            //EventManager.publish(Event.SPACECRAFT_THRUST_INFO, this, engine.thrustFactorIndex);
        }
    }

    public void decreaseThrustFactorIndex(MotorEngine engine, boolean broadcast) {
        engine.thrustFactorIndex = engine.thrustFactorIndex - 1;
        if (engine.thrustFactorIndex < 0)
            engine.thrustFactorIndex = engine.thrustFactor.length - 1;
        logger.info("Thrust factor: " + engine.thrustFactor[engine.thrustFactorIndex]);
        if (broadcast) {
            // TODO activate
            //EventManager.publish(Event.SPACECRAFT_THRUST_INFO, this, engine.thrustFactorIndex);
        }
    }

    public void setThrustFactorIndex(MotorEngine engine, int i, boolean broadcast) {
        assert i >= 0 && i < engine.thrustFactor.length : "Index " + i + " out of range of thrustFactor vector: [0.." + (engine.thrustFactor.length - 1);
        engine.thrustFactorIndex = i;
        logger.info("Thrust factor: " + engine.thrustFactor[engine.thrustFactorIndex]);
        if (broadcast) {
            // TODO activate
            //EventManager.publish(Event.SPACECRAFT_THRUST_INFO, this, engine.thrustFactorIndex);
        }
    }
}
