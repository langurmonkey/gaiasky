package gaiasky.scene.api;

import gaiasky.util.math.Vector3d;

public interface ISpacecraft {

    Vector3d force();

    Vector3d accel();

    Vector3d vel();

    Vector3d direction();

    Vector3d up();

    Vector3d thrust();

    double currentEnginePower();

    void currentEnginePower(double power);

    double thrustMagnitude();

    double[] thrustFactor();

    double relativisticSpeedCap();

    double drag();

    double mass();

    int thrustFactorIndex();

    boolean leveling();

    boolean stopping();

    void stopAllMovement();

}
