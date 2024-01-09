/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Quaternion;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.ISpacecraft;
import gaiasky.scene.component.Coordinates;
import gaiasky.scene.component.MotorEngine;
import gaiasky.scene.record.MachineDefinition;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Pair;
import gaiasky.util.coord.IBodyCoordinates;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

public class SpacecraftView extends BaseView implements ISpacecraft {
    private static final Log logger = Logger.getLogger(SpacecraftView.class);
    private final Vector3d D31 = new Vector3d();
    public MotorEngine engine;
    public Coordinates coord;

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
        check(entity, Mapper.engine, MotorEngine.class);
        check(entity, Mapper.coordinates, Coordinates.class);

    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.engine = Mapper.engine.get(entity);
        this.coord = Mapper.coordinates.get(entity);
    }

    @Override
    protected void entityCleared() {
        super.entityCleared();
        this.engine = null;
        this.coord = null;
    }

    public boolean isStopping() {
        return engine.stopping;
    }

    public boolean isStabilising() {
        return engine.leveling;
    }

    public MachineDefinition[] getMachines() {
        return engine.machines;
    }

    public int getCurrentMachine() {
        return engine.currentMachine;
    }

    public Vector3d force() {
        return engine.force;
    }

    public Vector3d accel() {
        return engine.accel;
    }

    public Vector3b pos() {
        return body.pos;
    }

    public Vector3d vel() {
        return engine.vel;
    }

    public Vector3d direction() {
        return engine.direction;
    }

    public Vector3d up() {
        return engine.up;
    }

    public Vector3d thrust() {
        return engine.thrust;
    }

    public float size() {
        return body.size;
    }

    public double getResponsiveness() {
        return engine.responsiveness;
    }

    public double getDrag() {
        return engine.drag;
    }

    public double currentEnginePower() {
        return engine.currentEnginePower;
    }

    @Override
    public void currentEnginePower(double power) {
        engine.currentEnginePower = power;
    }

    @Override
    public double thrustMagnitude() {
        return engine.thrustMagnitude();
    }

    @Override
    public double[] thrustFactor() {
        return engine.thrustFactor();
    }

    @Override
    public double relativisticSpeedCap() {
        return engine.relativisticSpeedCap();
    }

    @Override
    public double drag() {
        return engine.drag();
    }

    @Override
    public double mass() {
        return engine.mass();
    }

    @Override
    public int thrustFactorIndex() {
        return engine.thrustFactorIndex();
    }

    @Override
    public boolean leveling() {
        return engine.leveling();
    }

    @Override
    public boolean stopping() {
        return engine.stopping();
    }

    public Quaternion getRotationQuaternion() {
        return engine.qf;
    }

    public IBodyCoordinates getCoordinates() {
        return coord.coordinates;
    }

    public double getYawPower() {
        return engine.yawp;
    }

    /**
     * Sets the current yaw power
     *
     * @param yawp The yaw power in [-1..1]
     */
    public void setYawPower(double yawp) {
        engine.setYawPower(yawp);
    }

    public double getPitchPower() {
        return engine.pitchp;
    }

    /**
     * Sets the current pitch power
     *
     * @param pitchp The pitch power in [-1..1]
     */
    public void setPitchPower(double pitchp) {
        engine.setPitchPower(pitchp);
    }

    public double getRollPower() {
        return engine.rollp;
    }

    /**
     * Sets the current roll power
     *
     * @param rollp The roll power in [-1..1]
     */
    public void setRollPower(double rollp) {
        engine.setRollPower(rollp);
    }

    /**
     * Sets the current engine power
     *
     * @param currentEnginePower The power in [-1..1]
     */
    public void setCurrentEnginePower(double currentEnginePower) {
        engine.setCurrentEnginePower(currentEnginePower);
    }

    public void stopAllMovement() {
        engine.setCurrentEnginePower(0);

        engine.setYawPower(0);
        engine.setPitchPower(0);
        engine.setRollPower(0);

        engine.leveling = false;
        engine.stopping = false;

    }

    public void increaseThrustFactorIndex(boolean broadcast) {
        engine.thrustFactorIndex = (engine.thrustFactorIndex + 1) % MotorEngine.thrustFactor.length;
        logger.info("Thrust factor: " + MotorEngine.thrustFactor[engine.thrustFactorIndex]);
        if (broadcast)
            EventManager.publish(Event.SPACECRAFT_THRUST_INFO, this, engine.thrustFactorIndex);
    }

    public void decreaseThrustFactorIndex(boolean broadcast) {
        engine.thrustFactorIndex = engine.thrustFactorIndex - 1;
        if (engine.thrustFactorIndex < 0)
            engine.thrustFactorIndex = MotorEngine.thrustFactor.length - 1;
        logger.info("Thrust factor: " + MotorEngine.thrustFactor[engine.thrustFactorIndex]);
        if (broadcast)
            EventManager.publish(Event.SPACECRAFT_THRUST_INFO, this, engine.thrustFactorIndex);
    }

    public void setThrustFactorIndex(int i, boolean broadcast) {
        assert i >= 0 && i < MotorEngine.thrustFactor.length : "Index " + i + " out of range of thrustFactor vector: [0.." + (MotorEngine.thrustFactor.length - 1);
        engine.thrustFactorIndex = i;
        logger.info("Thrust factor: " + MotorEngine.thrustFactor[engine.thrustFactorIndex]);
        if (broadcast)
            EventManager.publish(Event.SPACECRAFT_THRUST_INFO, this, engine.thrustFactorIndex);
    }

    public double computeDirectionUp(double dt, Pair<Vector3d, Vector3d> pair) {
        // Yaw, pitch and roll
        engine.yawf = engine.yawp * engine.responsiveness;
        engine.pitchf = engine.pitchp * engine.responsiveness;
        engine.rollf = engine.rollp * engine.responsiveness;

        // Friction
        double friction = (engine.drag * 2e7) * dt;
        engine.yawf -= engine.yawv * friction;
        engine.pitchf -= engine.pitchv * friction;
        engine.rollf -= engine.rollv * friction;

        // accel
        engine.yawa = engine.yawf / engine.mass;
        engine.pitcha = engine.pitchf / engine.mass;
        engine.rolla = engine.rollf / engine.mass;

        // vel
        engine.yawv += engine.yawa * dt;
        engine.pitchv += engine.pitcha * dt;
        engine.rollv += engine.rolla * dt;

        // pos
        double yawDiff = (engine.yawv * dt) % 360d;
        double pitchDiff = (engine.pitchv * dt) % 360d;
        double rollDiff = (engine.rollv * dt) % 360d;

        Vector3d direction = pair.getFirst();
        Vector3d up = pair.getSecond();

        // apply yaw
        direction.rotate(up, yawDiff);

        // apply pitch
        Vector3d aux1 = D31.set(direction).crs(up);
        direction.rotate(aux1, pitchDiff);
        up.rotate(aux1, pitchDiff);

        // apply roll
        up.rotate(direction, -rollDiff);

        return rollDiff;
    }

    public void pollKeys(double dt) {
        double powerStep = dt / engine.fullPowerTime;
        if (Gdx.input.isKeyPressed(Keys.W))
            setCurrentEnginePower(engine.currentEnginePower + powerStep);
        if (Gdx.input.isKeyPressed(Keys.S))
            setCurrentEnginePower(engine.currentEnginePower - powerStep);

        if (Gdx.input.isKeyPressed(Keys.A))
            setRollPower(engine.rollp + powerStep);
        if (Gdx.input.isKeyPressed(Keys.D))
            setRollPower(engine.rollp - powerStep);

        if (Gdx.input.isKeyPressed(Keys.DOWN))
            setPitchPower(engine.pitchp + powerStep);
        if (Gdx.input.isKeyPressed(Keys.UP))
            setPitchPower(engine.pitchp - powerStep);

        if (Gdx.input.isKeyPressed(Keys.LEFT))
            setYawPower(engine.yawp + powerStep);
        if (Gdx.input.isKeyPressed(Keys.RIGHT))
            setYawPower(engine.yawp - powerStep);
    }
}
