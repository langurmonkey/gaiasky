/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.ILineRenderable;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scene.api.ISpacecraft;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.coord.SpacecraftCoordinates;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.stream.Stream;

/**
 * The spacecraft
 */
public class Spacecraft extends GenericSpacecraft implements ISpacecraft, ILineRenderable, IObserver {
    private static final Log logger = Logger.getLogger(Spacecraft.class);


    /** Max speed in relativistic mode **/
    private static final double relativisticSpeedCap = Constants.C_US * 0.99999;

    /**
     * Factor (adapt to be able to navigate small and large scale structures)
     **/
    public static final double[] thrustFactor = new double[14];

    static {
        double val = 0.01;
        for (int i = 0; i < 14; i++) {
            thrustFactor[i] = val * Math.pow(10, i);
        }
    }

    /** The current name of this spacecraft **/
    private String machineName;

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

    private static final double thrustBase = 1e12d;
    /** This is the magnitude of the thrust **/
    public double thrustMagnitude;

    /** Mass in kg **/
    private double mass;


    /** Responsiveness in [{@link Constants#MIN_SC_RESPONSIVENESS}, {@link Constants#MAX_SC_RESPONSIVENESS}] **/
    private double responsiveness;
    /** Responsiveness in [0, 1] **/
    private double drag;

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

    private final Quaternion qf;

    private int currentMachine = 0;
    private MachineDefinition[] machines;

    private boolean render;

    public Spacecraft() {
        super();
        ct = new ComponentTypes(ComponentType.Satellites);
        localTransform = new Matrix4();
        rotationMatrix = new Matrix4();
        EventManager.instance.subscribe(this, Event.CAMERA_MODE_CMD);

        // position attributes
        force = new Vector3d();
        accel = new Vector3d();
        vel = new Vector3d();

        // position and orientation
        pos.set(1e7 * Constants.KM_TO_U, 0, 1e8 * Constants.KM_TO_U);
        direction = new Vector3d(1, 0, 0);
        up = new Vector3d(0, 1, 0);
        dirup = new Pair<>(direction, up);

        posf = new Vector3();
        directionf = new Vector3(1, 0, 0);
        upf = new Vector3(0, 1, 0);

        // engine thrust direction
        // our spacecraft is a rigid solid so thrust is always the camera direction vector
        thrust = new Vector3d(direction).scl(thrustMagnitude);
        currentEnginePower = 0;

        // not stabilising
        leveling = false;

        qf = new Quaternion();

    }

    public void initialize() {
        // Use first model
        setToMachine(machines[currentMachine], false);

        // Coordinates
        SpacecraftCoordinates scc = new SpacecraftCoordinates();
        scc.setSpacecraft(this);
        this.coordinates = scc;

        // Initialize model
        super.initialize();

        this.labelFactor = 0;
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);

        // Broadcast me
        //EventManager.publish(Event.SPACECRAFT_LOADED, this, this);

        //EventManager.instance.subscribe(this, Event.CAMERA_MODE_CMD, Event.SPACECRAFT_STABILISE_CMD, Event.SPACECRAFT_STOP_CMD, Event.SPACECRAFT_THRUST_DECREASE_CMD, Event.SPACECRAFT_THRUST_INCREASE_CMD, Event.SPACECRAFT_THRUST_SET_CMD, Event.SPACECRAFT_MACHINE_SELECTION_CMD);
    }

    /**
     * Sets this spacecraft to the given machine definition.
     *
     * @param machine The machine definition.
     */
    private void setToMachine(final MachineDefinition machine, final boolean initialize) {
        this.mc = machine.getModel();
        this.thrustMagnitude = machine.getPower() * thrustBase;
        this.fullPowerTime = machine.getFullpowertime();
        this.mass = machine.getMass();
        this.shadowMapValues = machine.getShadowvalues();
        this.drag = machine.getDrag();
        this.responsiveness = MathUtilsd.lint(machine.getResponsiveness(), 0d, 1d, Constants.MIN_SC_RESPONSIVENESS, Constants.MAX_SC_RESPONSIVENESS);
        this.machineName = machine.getName();
        this.setSize(machine.getSize());

        if (initialize) {
            // Neither loading nor initialized
            if (!this.mc.isModelLoading() && !this.mc.isModelInitialised()) {
                this.mc.initialize(null);
            }
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case CAMERA_MODE_CMD:
            CameraMode mode = (CameraMode) data[0];
            render = mode == CameraMode.SPACECRAFT_MODE;
            break;
        case SPACECRAFT_STABILISE_CMD:
            leveling = (Boolean) data[0];
            break;
        case SPACECRAFT_STOP_CMD:
            stopping = (Boolean) data[0];
            break;
        case SPACECRAFT_THRUST_DECREASE_CMD:
            decreaseThrustFactorIndex(true);
            break;
        case SPACECRAFT_THRUST_INCREASE_CMD:
            increaseThrustFactorIndex(true);
            break;
        case SPACECRAFT_THRUST_SET_CMD:
            setThrustFactorIndex((Integer) data[0], false);
            break;
        case SPACECRAFT_MACHINE_SELECTION_CMD:
            int newMachineIndex = (Integer) data[0];
            // Update machine
            GaiaSky.postRunnable(() -> {
                this.setToMachine(machines[newMachineIndex], true);
                this.currentMachine = newMachineIndex;
                //EventManager.publish(Event.SPACECRAFT_MACHINE_SELECTION_INFO, this, machines[newMachineIndex]);
            });
            break;
        default:
            break;
        }

    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);

        if (render) {
            //EventManager.publish(Event.SPACECRAFT_INFO, this, yaw % 360, pitch % 360, roll % 360, vel.len(), thrustFactor[thrustFactorIndex], currentEnginePower, yawp, pitchp, rollp);
        }
    }

    protected void updateLocalTransform() {
        // Local transform
        try {

            // Spacecraft
            localTransform.idt().setToLookAt(posf, directionf.add(posf), upf).inv();
            localTransform.scale(size, size, size);

            // Rotation for attitude indicator
            rotationMatrix.idt().setToLookAt(directionf, upf);
            rotationMatrix.getRotation(qf);

        } catch (Exception e) {
        }
    }


    public double computeDirectionUp(double dt, Pair<Vector3d, Vector3d> pair) {
        // Yaw, pitch and roll
        yawf = yawp * responsiveness;
        pitchf = pitchp * responsiveness;
        rollf = rollp * responsiveness;

        // Friction
        double friction = (drag * 2e7) * dt;
        yawf -= yawv * friction;
        pitchf -= pitchv * friction;
        rollf -= rollv * friction;

        // accel
        yawa = yawf / mass;
        pitcha = pitchf / mass;
        rolla = rollf / mass;

        // vel
        yawv += yawa * dt;
        pitchv += pitcha * dt;
        rollv += rolla * dt;

        // pos
        double yawDiff = (yawv * dt) % 360d;
        double pitchDiff = (pitchv * dt) % 360d;
        double rollDiff = (rollv * dt) % 360d;

        Vector3d direction = pair.getFirst();
        Vector3d up = pair.getSecond();

        // apply yaw
        direction.rotate(up, yawDiff);

        // apply pitch
        Vector3d aux1 = D31.get().set(direction).crs(up);
        direction.rotate(aux1, pitchDiff);
        up.rotate(aux1, pitchDiff);

        // apply roll
        up.rotate(direction, -rollDiff);

        return rollDiff;
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        if (yawv != 0 || pitchv != 0 || rollv != 0 || vel.len2() != 0 || render) {
            // We use the simulation time for the integration
            // Poll keys
            if (camera.getMode().isSpacecraft())
                pollKeys(Gdx.graphics.getDeltaTime());

            double dt = time.getDt();

            // POSITION
            coordinates.getEquatorialCartesianCoordinates(time.getTime(), pos);

            if (leveling) {
                // No velocity, we just stop Euler angle motions
                if (yawv != 0) {
                    yawp = -Math.signum(yawv) * MathUtilsd.clamp(Math.abs(yawv), 0, 1);
                }
                if (pitchv != 0) {
                    pitchp = -Math.signum(pitchv) * MathUtilsd.clamp(Math.abs(pitchv), 0, 1);
                }
                if (rollv != 0) {
                    rollp = -Math.signum(rollv) * MathUtilsd.clamp(Math.abs(rollv), 0, 1);
                }
                if (Math.abs(yawv) < 1e-3 && Math.abs(pitchv) < 1e-3 && Math.abs(rollv) < 1e-3) {
                    setYawPower(0);
                    setPitchPower(0);
                    setRollPower(0);

                    yawv = 0;
                    pitchv = 0;
                    rollv = 0;
                    //EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, false);
                }
            }

            double rollDiff = computeDirectionUp(dt, dirup);

            double len = direction.len();
            pitch = Math.asin(direction.y / len);
            yaw = Math.atan2(direction.z, direction.x);
            roll += rollDiff;

            pitch = Math.toDegrees(pitch);
            yaw = Math.toDegrees(yaw);
        }
        // Update float vectors
        Vector3b camPos = B31.get().set(pos).add(camera.getInversePos());
        camPos.put(posf);
        direction.put(directionf);
        up.put(upf);

    }

    private void pollKeys(double dt) {
        double powerStep = dt / fullPowerTime;
        if (Gdx.input.isKeyPressed(Keys.W))
            setCurrentEnginePower(currentEnginePower + powerStep);
        if (Gdx.input.isKeyPressed(Keys.S))
            setCurrentEnginePower(currentEnginePower - powerStep);

        if (Gdx.input.isKeyPressed(Keys.A))
            setRollPower(rollp + powerStep);
        if (Gdx.input.isKeyPressed(Keys.D))
            setRollPower(rollp - powerStep);

        if (Gdx.input.isKeyPressed(Keys.DOWN))
            setPitchPower(pitchp + powerStep);
        if (Gdx.input.isKeyPressed(Keys.UP))
            setPitchPower(pitchp - powerStep);

        if (Gdx.input.isKeyPressed(Keys.LEFT))
            setYawPower(yawp + powerStep);
        if (Gdx.input.isKeyPressed(Keys.RIGHT))
            setYawPower(yawp - powerStep);
    }


    public void stopAllMovement() {
        setCurrentEnginePower(0);

        setYawPower(0);
        setPitchPower(0);
        setRollPower(0);

        leveling = false;
        stopping = false;

    }

    /**
     * Sets the current engine power
     *
     * @param currentEnginePower The power in [-1..1]
     */
    public void setCurrentEnginePower(double currentEnginePower) {
        this.currentEnginePower = MathUtilsd.clamp(currentEnginePower, -1, 1);
    }

    /**
     * Sets the current yaw power
     *
     * @param yawp The yaw power in [-1..1]
     */
    public void setYawPower(double yawp) {
        this.yawp = MathUtilsd.clamp(yawp, -1, 1);
    }

    /**
     * Sets the current pitch power
     *
     * @param pitchp The pitch power in [-1..1]
     */
    public void setPitchPower(double pitchp) {
        this.pitchp = MathUtilsd.clamp(pitchp, -1, 1);
    }

    /**
     * Sets the current roll power
     *
     * @param rollp The roll power in [-1..1]
     */
    public void setRollPower(double rollp) {
        this.rollp = MathUtilsd.clamp(rollp, -1, 1);
    }

    public void increaseThrustFactorIndex(boolean broadcast) {
        thrustFactorIndex = (thrustFactorIndex + 1) % thrustFactor.length;
        logger.info("Thrust factor: " + thrustFactor[thrustFactorIndex]);
        //if (broadcast)
            //EventManager.publish(Event.SPACECRAFT_THRUST_INFO, this, thrustFactorIndex);
    }

    public void decreaseThrustFactorIndex(boolean broadcast) {
        thrustFactorIndex = thrustFactorIndex - 1;
        if (thrustFactorIndex < 0)
            thrustFactorIndex = thrustFactor.length - 1;
        logger.info("Thrust factor: " + thrustFactor[thrustFactorIndex]);
        //if (broadcast)
            //EventManager.publish(Event.SPACECRAFT_THRUST_INFO, this, thrustFactorIndex);
    }

    public void setThrustFactorIndex(int i, boolean broadcast) {
        assert i >= 0 && i < thrustFactor.length : "Index " + i + " out of range of thrustFactor vector: [0.." + (thrustFactor.length - 1);
        thrustFactorIndex = i;
        logger.info("Thrust factor: " + thrustFactor[thrustFactorIndex]);
        //if (broadcast)
            //EventManager.publish(Event.SPACECRAFT_THRUST_INFO, this, thrustFactorIndex);
    }

    /**
     * Adds this entity to the necessary render lists after the distance to the
     * camera and the view angle have been determined.
     */
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender() && this.viewAngleApparent > thresholdPoint * camera.getFovFactor()) {
            super.addToRenderLists(camera);
            if (Settings.settings.spacecraft.showAxes)
                addToRender(this, RenderGroup.LINE);
        }
    }

    public void setModel(ModelComponent mc) {
        this.mc = mc;
    }

    /**
     * Sets the absolute size of this entity
     */
    public void setSize(Double size) {
        this.size = (float) (size * Constants.KM_TO_U);
    }

    public void setSize(Long size) {
        this.size = (float) (size * Constants.KM_TO_U);
    }

    public void setMass(Double mass) {
        this.mass = mass;
    }

    public boolean isStopping() {
        return stopping;
    }

    public boolean isStabilising() {
        return leveling;
    }

    @Override
    public double getDistToCamera() {
        return distToCamera;
    }

    public Quaternion getRotationQuaternion() {
        return qf;
    }

    @Override
    public boolean hasAtmosphere() {
        return false;
    }

    public void dispose() {
        super.dispose();
    }

    /** Model rendering. SPACECRAFT_MODE in spacecraft mode is not affected by the relativistic aberration **/
    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc, RenderGroup group) {
        if (group == RenderGroup.MODEL_PIX) {
            render(modelBatch, alpha, t, true);
        }
    }

    /** Default model rendering. **/
    public void render(IntModelBatch modelBatch, float alpha, double t, boolean shadowEnv) {
        ICamera cam = GaiaSky.instance.getICamera();

        if (mc.isModelInitialised()) {
            // Good, render
            if (shadowEnv)
                prepareShadowEnvironment();
            mc.setTransparency(alpha * fadeOpacity);
            if (cam.getMode().isSpacecraft())
                // In SPACECRAFT_MODE mode, we are not affected by relativistic aberration or Doppler shift
                mc.updateRelativisticEffects(cam, 0);
            else
                mc.updateRelativisticEffects(cam);
            mc.updateVelocityBufferUniforms(cam);
            modelBatch.render(mc.instance, mc.env);
        } else {
            // Keep loading
            mc.load(localTransform);
        }
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        // Direction
        Vector3d d = D31.get().set(direction);
        d.nor().scl(.5e-4);
        renderer.addLine(this, posf.x, posf.y, posf.z, posf.x + d.x, posf.y + d.y, posf.z + d.z, 1, 0, 0, 1);

        // Up
        Vector3d u = D31.get().set(up);
        u.nor().scl(.2e-4);
        renderer.addLine(this, posf.x, posf.y, posf.z, posf.x + u.x, posf.y + u.y, posf.z + u.z, 0, 0, 1, 1);

    }

    @Override
    public <T extends SceneGraphNode> T getSimpleCopy() {
        Spacecraft copy = super.getSimpleCopy();
        copy.force.set(this.force);
        copy.accel.set(this.accel);
        copy.vel.set(this.vel);

        copy.fullPowerTime = this.fullPowerTime;

        copy.posf.set(this.posf);
        copy.direction.set(this.direction);
        copy.directionf.set(this.directionf);
        copy.up.set(this.up);
        copy.upf.set(this.upf);
        copy.thrust.set(this.thrust);

        copy.mass = this.mass;

        copy.rotationMatrix.set(this.rotationMatrix);

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

        return (T) copy;
    }

    @Override
    protected boolean mustUpdatePosition(ITimeFrameProvider time) {
        return true;
    }

    @Override
    public float getLineWidth() {
        return 1;
    }

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINES;
    }

    public double getResponsiveness() {
        return responsiveness;
    }

    public double getDrag() {
        return drag;
    }

    public MachineDefinition[] getMachines() {
        return machines;
    }

    public void setMachines(Object[] machines) {
        this.machines = Stream.of(machines).toArray(MachineDefinition[]::new);
    }

    public int getCurrentMachine() {
        return currentMachine;
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
}
