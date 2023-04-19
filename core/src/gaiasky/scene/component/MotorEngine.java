/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.api.ISpacecraft;
import gaiasky.scene.record.MachineDefinition;
import gaiasky.util.Constants;
import gaiasky.util.Pair;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3d;

import java.util.stream.Stream;

public class MotorEngine implements Component, ISpacecraft, ICopy {
    /**
     * Factor (adapt to be able to navigate small and large scale structures)
     **/
    public static final double[] thrustFactor = new double[14];
    public static final double thrustBase = 1e12d;
    /** Max speed in relativistic mode **/
    private static final double relativisticSpeedCap = Constants.C_US * 0.99999;

    static {
        double val = 0.01;
        for (int i = 0; i < 14; i++) {
            thrustFactor[i] = val * Math.pow(10, i);
        }
    }

    /** The current name of this spacecraft **/
    public String machineName;
    /** Seconds to reach full power **/
    public double fullPowerTime;
    /** Force, acceleration and velocity **/
    public Vector3d force, accel, vel;
    /** Direction and up vectors **/
    public Vector3d direction, up;
    public Pair<Vector3d, Vector3d> dirup;
    /** Float counterparts **/
    public Vector3 posf, directionf, upf;
    /** Instantaneous engine power, do not set manually **/
    public double currentEnginePower;
    /** Engine thrust vector **/
    public Vector3d thrust;
    /** This is the magnitude of the thrust **/
    public double thrustMagnitude;

    /** Mass in kg **/
    public double mass;

    /** Responsiveness in [{@link Constants#MIN_SC_RESPONSIVENESS}, {@link Constants#MAX_SC_RESPONSIVENESS}] **/
    public double responsiveness;
    /** Responsiveness in [0, 1] **/
    public double drag;

    /** Only the rotation matrix **/
    public Matrix4 rotationMatrix;

    /**
     * Index of the current engine power setting
     */
    public int thrustFactorIndex = 0;

    /** Yaw, pitch and roll **/
    // power in each angle in [0..1]
    public double yawp, pitchp, rollp;
    // angular forces
    public double yawf, pitchf, rollf;
    // angular accelerations in deg/s^2
    public double yawa, pitcha, rolla;
    // angular velocities in deg/s
    public double yawv, pitchv, rollv;
    // angles in radians
    public double yaw, pitch, roll;

    // Are we in the process of stabilising or stopping the spaceship?
    public boolean leveling, stopping;

    public Quaternion qf;

    public int currentMachine = 0;
    public MachineDefinition[] machines;

    public boolean render;

    public void setMachines(Object[] machines) {
        this.machines = Stream.of(machines).toArray(MachineDefinition[]::new);
    }

    /**
     * Sets the current engine power
     *
     * @param currentEnginePower The power in [-1..1]
     */
    public void setCurrentEnginePower(double currentEnginePower) {
        this.currentEnginePower = MathUtilsDouble.clamp(currentEnginePower, -1, 1);
    }

    /**
     * Sets the current yaw power
     *
     * @param yawp The yaw power in [-1..1]
     */
    public void setYawPower(double yawp) {
        this.yawp = MathUtilsDouble.clamp(yawp, -1, 1);
    }

    /**
     * Sets the current pitch power
     *
     * @param pitchp The pitch power in [-1..1]
     */
    public void setPitchPower(double pitchp) {
        this.pitchp = MathUtilsDouble.clamp(pitchp, -1, 1);
    }

    /**
     * Sets the current roll power
     *
     * @param rollp The roll power in [-1..1]
     */
    public void setRollPower(double rollp) {
        this.rollp = MathUtilsDouble.clamp(rollp, -1, 1);
    }

    public void stopAllMovement() {
        setCurrentEnginePower(0);

        setYawPower(0);
        setPitchPower(0);
        setRollPower(0);

        leveling = false;
        stopping = false;
    }

    @Override
    public Vector3d force() {
        return force;
    }

    @Override
    public Vector3d accel() {
        return accel;
    }

    @Override
    public Vector3d vel() {
        return vel;
    }

    @Override
    public Vector3d direction() {
        return direction;
    }

    @Override
    public Vector3d up() {
        return up;
    }

    @Override
    public Vector3d thrust() {
        return thrust;
    }

    @Override
    public double currentEnginePower() {
        return currentEnginePower;
    }

    @Override
    public void currentEnginePower(double power) {
        setCurrentEnginePower(power);
    }

    @Override
    public double thrustMagnitude() {
        return thrustMagnitude;
    }

    @Override
    public double[] thrustFactor() {
        return thrustFactor;
    }

    @Override
    public double relativisticSpeedCap() {
        return relativisticSpeedCap;
    }

    @Override
    public double drag() {
        return drag;
    }

    @Override
    public double mass() {
        return mass;
    }

    @Override
    public int thrustFactorIndex() {
        return thrustFactorIndex;
    }

    @Override
    public boolean leveling() {
        return leveling;
    }

    @Override
    public boolean stopping() {
        return stopping;
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.force = new Vector3d(this.force);
        copy.accel = new Vector3d(this.accel);
        copy.vel = new Vector3d(this.vel);

        copy.fullPowerTime = this.fullPowerTime;

        copy.posf = new Vector3(this.posf);
        copy.direction = new Vector3d(this.direction);
        copy.directionf = new Vector3(this.directionf);
        copy.up = new Vector3d(this.up);
        copy.upf = new Vector3(this.upf);
        copy.thrust = new Vector3d(this.thrust);

        copy.mass = this.mass;

        copy.rotationMatrix = new Matrix4(this.rotationMatrix);

        copy.thrustFactorIndex = this.thrustFactorIndex;

        copy.currentEnginePower = this.currentEnginePower;

        copy.yawp = this.yawp;
        copy.yawf = this.yawf;
        copy.yawa = this.yawa;
        copy.yawv = this.yawv;

        copy.pitchp = this.pitchp;
        copy.pitchf = this.pitchf;
        copy.pitcha = this.pitcha;
        copy.pitchv = this.pitchv;

        copy.rollp = this.rollp;
        copy.rollf = this.rollf;
        copy.rolla = this.rolla;
        copy.rollv = this.rollv;

        copy.leveling = this.leveling;
        copy.stopping = this.stopping;

        return copy;
    }
}
