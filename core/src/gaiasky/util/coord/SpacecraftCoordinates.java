/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.api.ISpacecraft;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.SpacecraftView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.math.IntersectorDouble;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.math.Vector3D;

import java.time.Instant;
import java.util.Map;

public class SpacecraftCoordinates implements IBodyCoordinates {
    private static final Log logger = Logger.getLogger(SpacecraftCoordinates.class);

    private ISpacecraft spacecraft;
    private final Vector3D D31;
    private final Vector3D D32;
    private final Vector3D D33;
    private final Vector3Q B31;
    private final Vector3Q B32;
    private final Vector3Q B33;
    private final Vector3Q B34;

    public SpacecraftCoordinates() {
        this.D31 = new Vector3D();
        this.D32 = new Vector3D();
        this.D33 = new Vector3D();

        this.B31 = new Vector3Q();
        this.B32 = new Vector3Q();
        this.B33 = new Vector3Q();
        this.B34 = new Vector3Q();
    }

    public void setSpacecraft(ISpacecraft sc) {
        this.spacecraft = sc;
    }

    @Override
    public void doneLoading(Object... params) {

    }

    @Override
    public Vector3Q getEclipticSphericalCoordinates(Instant date, Vector3Q out) {
        getEclipticCartesianCoordinates(date, out);

        // To spherical
        Coordinates.cartesianToSpherical(out, out);
        return out;
    }

    @Override
    public Vector3Q getEclipticCartesianCoordinates(Instant date, Vector3Q out) {
        getEquatorialCartesianCoordinates(date, out);
        out.mul(Coordinates.eqToEcl());

        return out;
    }

    @Override
    public Vector3Q getEquatorialCartesianCoordinates(Instant instant, Vector3Q out) {
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

    public Vector3Q computePosition(double dt, IFocus closest, double currentEnginePower, Vector3D thrust, Vector3D direction, Vector3D force, Vector3D accel, Vector3D vel, Vector3Q posb) {
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

            Vector3D nextVel = D33.set(force).scl(1d / mass).scl(Constants.M_TO_U).scl(dt).add(vel);

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
        Vector3D acc = D31.set(accel).scl(Constants.M_TO_U);

        if (Settings.settings.spacecraft.velocityDirection) {
            double velocityLength = vel.len();
            vel.set(direction).nor().scl(velocityLength);
        }
        vel.add(acc.scl(dt));

        Vector3Q velocity = B32.set(vel);
        Vector3Q newPosition = B33.set(posb).add(velocity.scl(dt));
        Vector3Q pos = posb.put(B34);
        // Check collision!
        IFocus me = GaiaSky.instance.getICamera().getClosestBody();
        if (closest != null && !closest.isEmpty()
                && (closest instanceof FocusView && spacecraft instanceof SpacecraftView &&
                ((FocusView) closest).getEntity() != ((SpacecraftView) spacecraft).getEntity())) {

            double twoRadii = closest.getRadius() + me.getRadius();
            // d1 is the new distance to the centre of the object
            if (!vel.isZero() && IntersectorDouble.distanceSegmentPoint(pos.put(D31), newPosition.put(D32), closest.getPos().put(D33)) < twoRadii) {
                logger.info("Crashed against " + closest.getName() + "!");

                Array<Vector3D> intersections = IntersectorDouble.intersectRaySphere(pos.put(D31), newPosition.put(D32), closest.getPos().put(D31), twoRadii);

                // Teleport outside
                if (intersections.size >= 1) {
                    posb.set(intersections.get(0));
                }

                spacecraft.stopAllMovement();
            } else if (posb.dstD(closest.getPos()) < twoRadii) {
                posb.set(B31.set(posb).sub(closest.getPos()).nor().scl(posb.dst(closest.getPos(), B32)));
            } else {
                posb.set(newPosition);
            }
        } else {
            posb.set(newPosition);
        }

        return posb;
    }

    @Override
    public void updateReferences(Map<String, Entity> index) {
    }

    @Override
    public IBodyCoordinates getCopy() {
        return this;
    }
}
