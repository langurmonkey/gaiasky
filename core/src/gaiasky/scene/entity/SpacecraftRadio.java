/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.Model;
import gaiasky.scene.component.ModelScaffolding;
import gaiasky.scene.component.MotorEngine;
import gaiasky.scene.record.MachineDefinition;
import gaiasky.scene.view.SpacecraftView;
import gaiasky.util.Constants;
import gaiasky.util.math.MathUtilsDouble;

public class SpacecraftRadio extends EntityRadio {

    private final SpacecraftView view;

    public SpacecraftRadio(Entity entity) {
        super(entity);
        this.view = new SpacecraftView();
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        view.setEntity(entity);
        switch (event) {
        case CAMERA_MODE_CMD:
            CameraMode mode = (CameraMode) data[0];
            view.engine.render = mode == CameraMode.SPACECRAFT_MODE;
            break;
        case SPACECRAFT_STABILISE_CMD:
            view.engine.leveling = (Boolean) data[0];
            break;
        case SPACECRAFT_STOP_CMD:
            view.engine.stopping = (Boolean) data[0];
            break;
        case SPACECRAFT_THRUST_DECREASE_CMD:
            view.decreaseThrustFactorIndex(true);
            break;
        case SPACECRAFT_THRUST_INCREASE_CMD:
            view.increaseThrustFactorIndex(true);
            break;
        case SPACECRAFT_THRUST_SET_CMD:
            view.setThrustFactorIndex((Integer) data[0], false);
            break;
        case SPACECRAFT_MACHINE_SELECTION_CMD:
            int newMachineIndex = (Integer) data[0];
            // Update machine
            GaiaSky.postRunnable(() -> {
                setToMachine(entity.getComponent(Body.class),
                        entity.getComponent(Model.class),
                        entity.getComponent(ModelScaffolding.class),
                        view.engine,
                        view.engine.machines[newMachineIndex], true);
                view.engine.currentMachine = newMachineIndex;
                EventManager.publish(Event.SPACECRAFT_MACHINE_SELECTION_INFO, this, view.engine.machines[newMachineIndex]);
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
        engine.responsiveness = MathUtilsDouble.lint(machine.getResponsiveness(), 0d, 1d, Constants.MIN_SC_RESPONSIVENESS, Constants.MAX_SC_RESPONSIVENESS);
        engine.machineName = machine.getName();
        body.setSizeKm(machine.getSize());

        if (initialize) {
            // Neither loading nor initialized
            if (!model.model.isModelLoading() && !model.model.isModelInitialised()) {
                model.model.initialize(null);
            }
        }
    }

}
