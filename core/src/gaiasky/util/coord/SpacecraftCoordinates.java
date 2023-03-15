package gaiasky.util.coord;

import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.api.ISpacecraft;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.math.IntersectorDouble;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.time.Instant;

/**
 * Computes the coordinates of the spacecraft object.
 */
public class SpacecraftCoordinates implements IBodyCoordinates {
    private static final Log logger = Logger.getLogger(SpacecraftCoordinates.class);

    private ISpacecraft spacecraft;
    private Vector3d D31, D32, D33;
    private Vector3b B31, B32, B33, B34;

    public SpacecraftCoordinates() {
        this.D31 = new Vector3d();
        this.D32 = new Vector3d();
        this.D33 = new Vector3d();

        this.B31 = new Vector3b();
        this.B32 = new Vector3b();
        this.B33 = new Vector3b();
        this.B34 = new Vector3b();
    }

    public void setSpacecraft(ISpacecraft sc) {
        this.spacecraft = sc;
    }

    @Override
    public void doneLoading(Object... params) {

    }

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant date, Vector3b out) {
        getEclipticCartesianCoordinates(date, out);

        // To spherical
        Coordinates.cartesianToSpherical(out, out);
        return out;
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant date, Vector3b out) {
        getEquatorialCartesianCoordinates(date, out);
        out.mul(Coordinates.eqToEcl());

        return out;
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant instant, Vector3b out) {
        return computePosition(GaiaSky.instance.time.getDt(),
                GaiaSky.instance.getICamera().getSecondClosestBody(),
                spacecraft.currentEnginePower(),
                spacecraft.thrust(),
                spacecraft.direction(),
                spacecraft.force(),
                spacecraft.accel(),
                spacecraft.vel(),
                out);
    }

    public Vector3b computePosition(double dt, IFocus closest, double currentEnginePower, Vector3d thrust, Vector3d direction, Vector3d force, Vector3d accel, Vector3d vel, Vector3b posb) {
        double mass = spacecraft.mass();

        spacecraft.currentEnginePower(Math.signum(currentEnginePower));
        currentEnginePower = spacecraft.currentEnginePower();

        // Compute force from thrust
        thrust.set(direction).scl(spacecraft.thrustMagnitude() * spacecraft.thrustFactor()[spacecraft.thrustFactorIndex()] * currentEnginePower);
        force.set(thrust);

        // Scale force if relativistic effects are on
        if (Settings.settings.runtime.relativisticAberration) {
            double speed = vel.len();
            double scale = (spacecraft.relativisticSpeedCap() - speed) / spacecraft.relativisticSpeedCap();
            force.scl(scale);
        }

        double friction = (spacecraft.drag() * 2e16) * dt;
        force.add(D31.set(vel).scl(-friction));

        if (spacecraft.stopping()) {
            double speed = vel.len();
            if (speed != 0) {
                spacecraft.currentEnginePower(-1);
                thrust.set(vel).nor().scl(spacecraft.thrustMagnitude() * spacecraft.thrustFactor()[spacecraft.thrustFactorIndex()] * currentEnginePower);
                force.set(thrust);
            }

            Vector3d nextVel = D33.set(force).scl(1d / mass).scl(Constants.M_TO_U).scl(dt).add(vel);

            if (vel.angle(nextVel) > 90) {
                spacecraft.currentEnginePower(0);
                force.scl(0);
                vel.scl(0);
                EventManager.publish(Event.SPACECRAFT_STOP_CMD, this, false);
            }
        }

        // Compute new acceleration in m/s^2
        accel.set(force).scl(1d / mass);

        // Integrate other quantities
        // convert metres to internal units so we have the velocity in u/s
        Vector3d acc = D31.set(accel).scl(Constants.M_TO_U);

        if (Settings.settings.spacecraft.velocityDirection) {
            double velocityLength = vel.len();
            vel.set(direction).nor().scl(velocityLength);
        }
        vel.add(acc.scl(dt));

        Vector3b velocity = B32.set(vel);
        Vector3b newPosition = B33.set(posb).add(velocity.scl(dt));
        Vector3b pos = posb.put(B34);
        // Check collision!
        IFocus me = GaiaSky.instance.getICamera().getClosestBody();
        if (closest != null && !closest.isEmpty() && closest != spacecraft) {
            double twoRadii = closest.getRadius() + me.getRadius();
            // d1 is the new distance to the centre of the object
            if (!vel.isZero() && IntersectorDouble.distanceSegmentPoint(pos.put(D31), newPosition.put(D32), closest.getPos().put(D33)) < twoRadii) {
                logger.info("Crashed against " + closest.getName() + "!");

                Array<Vector3d> intersections = IntersectorDouble.intersectRaySphere(pos.put(D31), newPosition.put(D32), closest.getPos().put(D31), twoRadii);

                // Teleport outside
                if (intersections.size >= 1) {
                    posb.set(intersections.get(0));
                }

                spacecraft.stopAllMovement();
            } else if (posb.dstDouble(closest.getPos()) < twoRadii) {
                posb.set(B31.set(posb).sub(closest.getPos()).nor().scl(posb.dst(closest.getPos(), B32)));
            } else {
                posb.set(newPosition);
            }
        } else {
            posb.set(newPosition);
        }

        return posb;
    }

}
