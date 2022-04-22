package gaiasky.scene.component;

import com.artemis.Component;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scenegraph.MachineDefinition;
import gaiasky.util.Constants;
import gaiasky.util.Pair;
import gaiasky.util.math.Vector3d;

import java.util.stream.Stream;

public class Machine extends Component {
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

    public Quaternion qf;

    public int currentMachine = 0;
    public MachineDefinition[] machines;

    public boolean render;

    public void setMachines(Object[] machines) {
        this.machines = Stream.of(machines).toArray(MachineDefinition[]::new);
    }
}
