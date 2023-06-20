/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import com.badlogic.ashley.core.Entity;
import gaiasky.data.api.IOrbitDataProvider;
import gaiasky.data.util.OrbitDataLoader;
import gaiasky.data.util.PointCloudData;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Trajectory;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.coord.chebyshev.ChebyshevEphemeris;
import gaiasky.util.coord.vsop2000.VSOP2000;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3b;

import java.io.IOException;
import java.time.Instant;

/**
 * This class provides orbit data by using the coordinates provider of the attached
 * object, if any.
 */
public class OrbitBodyDataProvider implements IOrbitDataProvider {
    private static final String writeDataPath = "/tmp/";
    private static final boolean writeData = false;

    private int i = 1;
    private PointCloudData data, data0, data1;
    private Entity entity;
    private Trajectory trajectory;
    private final Vector3b aux1 = new Vector3b();
    private final Vector3b aux2 = new Vector3b();

    @Override
    public void initialize(Entity entity, Trajectory trajectory) {
        this.entity = entity;
        this.trajectory = trajectory;
    }

    private PointCloudData getNextData(int numSamples) {
        i = (i + 1) % 2;
        if (i == 0) {
            if (data0 == null) {
                data0 = new PointCloudData(numSamples);
            } else {
                data0.clear();
            }
            return data0;
        } else {
            if (data1 == null) {
                data1 = new PointCloudData(numSamples);
            } else {
                data1.clear();
            }
            return data1;
        }
    }

    @Override
    public void load(String file, OrbitDataLoader.OrbitDataLoaderParameters parameter) {
        if (trajectory.body != null) {
            Entity body = trajectory.body;
            var coordinates = Mapper.coordinates.get(body);
            if (coordinates != null && coordinates.coordinates != null) {
                double period = parameter.orbitalPeriod * 0.999d;
                int numSamples = parameter.numSamples > 0 ? parameter.numSamples : (int) (300.0 * period / 365.0);
                numSamples = Math.max(200, Math.min(2000, numSamples));
                var verts = Mapper.verts.get(entity);
                data = getNextData(numSamples);
                String bodyDesc = parameter.name;
                double last = 0, accum = 0;

                // Milliseconds of this orbit in one revolution.
                double orbitalMs = period * 86400000.0;
                double stepMs = orbitalMs / (double) numSamples;

                Instant d;
                if (period > 40000) {
                    // For long-period, it is better to recompute more often because they can deviate significantly.
                    d = Instant.ofEpochMilli(parameter.ini.getTime() - (long) (orbitalMs * 0.8));
                } else if (parameter.entity != null && Mapper.base.get(parameter.entity).ct.isEnabled(ComponentTypes.ComponentType.Moons)) {
                    // For moon orbits, it is better to recompute more often because they can deviate significantly.
                    d = Instant.ofEpochMilli(parameter.ini.getTime() - (long) (orbitalMs * 0.4));
                } else if (parameter.entity != null && parameter.entity.getComponent(Base.class).ct.isEnabled(ComponentTypes.ComponentType.Moons)) {
                    // For moon orbits, it is better to recompute more often because they can deviate significantly.
                    d = Instant.ofEpochMilli(parameter.ini.getTime() - (long) (orbitalMs * 0.4));
                } else {
                    // Shorter period orbits don't deviate enough to be noticeable.
                    d = Instant.ofEpochMilli(parameter.ini.getTime());
                }

                // Load orbit data.
                for (int i = 0; i <= numSamples; i++) {
                    if (coordinates.coordinates instanceof VSOP2000 || coordinates.coordinates instanceof ChebyshevEphemeris) {
                        // VSOP2000 gives native rectangular ecliptic coordinates.
                        coordinates.coordinates.getEclipticCartesianCoordinates(d, aux1);
                        Coordinates.cartesianToSpherical(aux1, aux2);
                        double eclX = aux2.x.doubleValue();

                        if (last == 0) {
                            last = Math.toDegrees(eclX);
                        }

                        accum += Math.toDegrees(eclX) - last;
                        last = Math.toDegrees(eclX);

                        if (accum > 360) {
                            break;
                        }
                    } else {
                        // VSOP87, MoonAA, Pluto output native spherical ecliptic coordinates.
                        AstroUtils.getEclipticCoordinates(bodyDesc, d, aux1, Settings.settings.data.highAccuracy);
                        double eclX = aux1.x.doubleValue();

                        if (last == 0) {
                            last = Math.toDegrees(eclX);
                        }

                        accum += Math.toDegrees(eclX) - last;
                        last = Math.toDegrees(eclX);

                        if (accum > 360) {
                            break;
                        }

                        Coordinates.sphericalToCartesian(aux1, aux1);
                    }
                    aux1.mul(Coordinates.eclToEq());
                    data.x.add(aux1.x.doubleValue());
                    data.y.add(aux1.y.doubleValue());
                    data.z.add(aux1.z.doubleValue());
                    data.time.add(d);

                    d = Instant.ofEpochMilli(d.toEpochMilli() + (long) stepMs);
                }
                // Close the circle.
                data.x.add(data.x.get(0));
                data.y.add(data.y.get(0));
                data.z.add(data.z.get(0));
                d = Instant.ofEpochMilli(d.toEpochMilli() + (long) stepMs);
                data.time.add(Instant.ofEpochMilli(d.toEpochMilli()));

                if (writeData) {
                    try {
                        OrbitDataWriter.writeOrbitData(writeDataPath + "orb." + bodyDesc.toUpperCase() + ".dat", data);
                    } catch (IOException e) {
                        Logger.getLogger(this.getClass()).error(e);
                    }
                }

                Logger.getLogger(this.getClass()).info(I18n.msg("notif.orbitdataof.loaded", parameter.name, data.getNumPoints()));
            }
        }

    }

    @Override
    public void load(String file, OrbitDataLoader.OrbitDataLoaderParameters source, boolean newMethod) {
        load(file, source);
    }

    @Override
    public PointCloudData getData() {
        return data;
    }
}
