/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonWriter;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.TLESource;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.TextUtils;
import gaiasky.util.coord.TLEParser;
import gaiasky.util.coord.TLEParser.OrbitalElements;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Initializes entities that have a {@link TLESource} component.
 */
public class TLEInitializer extends AbstractInitSystem {

    private final Scene scene;

    public TLEInitializer(Scene scene, boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        this.scene = scene;
    }

    @Override
    public void initializeEntity(Entity entity) {
    }

    @Override
    public void setUpEntity(Entity entity) {
        // Run update in background thread.
        var executorService = GaiaSky.instance.getExecutorService();
            executorService.execute(() -> {
                var base = Mapper.base.get(entity);

                final var now = Instant.now();
                final var tle = Mapper.tle.get(entity);
                final var fileName = TextUtils.sanitizeFilename(tle.nameTLE + "-TLE.json");
                final var cacheDir = SysUtils.getDataCacheDir(Settings.settings.data.location);
                var ignored = cacheDir.toFile().mkdirs();
                final var filePath = cacheDir.resolve(fileName);
                var mustUpdate = false;
                if (Files.exists(filePath)) {
                    // Check date.
                    JsonReader jsonReader = new JsonReader();
                    try {
                        var data = jsonReader.parse(new FileHandle(filePath.toFile()));
                        var lastUpdateStr = data.getString("lastUpdate");
                        Instant lastUpdate = Instant.parse(lastUpdateStr);
                        mustUpdate = ((now.getEpochSecond() - lastUpdate.getEpochSecond()) / 86400.0) > tle.updateIntervalTLE;
                    } catch (Exception e) {
                        // Error reading file, update.
                        mustUpdate = true;
                    }
                } else {
                    // No file yet, first update.
                    mustUpdate = true;
                }

                // Fetch data!
                OrbitalElements elements = null;
                if (mustUpdate) {
                    logger.info("Fetching TLE data for " + base.getName());
                    try {
                        // Fetch and parse TLE data.
                        TLEParser parser = new TLEParser();
                        elements = parser.getOrbitalElements(tle.urlTLE, tle.nameTLE);

                        // Delete JSON if exists.
                        if (Files.exists(filePath)) {
                            try {
                                Files.delete(filePath);
                            } catch (IOException ignored1) {
                            }
                        }

                        // Write elements to JSON file.
                        writeElements(filePath, now, elements);
                    } catch (Exception e) {
                        logger.error("Error fetching TLE data for " + base.getName(), e);
                    }
                } else {
                    logger.info("Using cached file: " + fileName);
                }

                // Update orbit data.
                if (elements == null) {
                    // Read from file.
                    JsonReader jsonReader = new JsonReader();
                    try {
                        var data = jsonReader.parse(new FileHandle(filePath.toFile()));
                        var elem = data.get("elements");
                        elements = new OrbitalElements();
                        elements.epochJD = elem.getDouble("epoch");
                        elements.period = elem.getDouble("period");
                        elements.semiMajorAxis = elem.getDouble("semiMajorAxis");
                        elements.eccentricity = elem.getDouble("eccentricity");
                        elements.inclination = elem.getDouble("inclination");
                        elements.ascendingNode = elem.getDouble("ascendingNode");
                        elements.meanAnomaly = elem.getDouble("meanAnomaly");
                        elements.argOfPericenter = elem.getDouble("argOfPericenter");
                    } catch (Exception e) {
                        // Error reading file, update.
                        logger.error(e);
                    }
                }

                var trajectory = Mapper.trajectory.get(entity);
                if (elements != null && trajectory.oc != null) {
                    // Upload orbital components in trajectory (in-memory only!).
                    trajectory.oc.epoch = elements.epochJD;
                    trajectory.oc.period = elements.period;
                    trajectory.oc.i = elements.inclination;
                    trajectory.oc.e = elements.eccentricity;
                    trajectory.oc.semiMajorAxis = elements.semiMajorAxis;
                    trajectory.oc.ascendingNode = elements.ascendingNode;
                    trajectory.oc.argOfPericenter = elements.argOfPericenter;
                    trajectory.oc.meanAnomaly = elements.meanAnomaly;

                    // Re-sample data in a posted runnable.
                    GaiaSky.postRunnable(() -> {
                        var verts = Mapper.verts.get(entity);
                        var render = Mapper.render.get(entity);
                        TrajectoryInitializer.loadTrajectory(entity, base, trajectory, verts);
                        verts.markForUpdate(render);
                        // Update object.
                        var attachedEntity = trajectory.body;
                        if (attachedEntity != null) {
                            var coord = Mapper.coordinates.get(attachedEntity);
                            if (coord.coordinates != null) {
                                coord.coordinates.doneLoading(scene, attachedEntity);
                            }
                        }
                    });
                }


            });
    }

    private void writeElements(Path filePath, Instant now, OrbitalElements elements) throws IOException {
        var f = Gdx.files.absolute(filePath.toString());
        try (OutputStreamWriter writer = new OutputStreamWriter(f.write(false), StandardCharsets.UTF_8)) {
            JsonWriter jsonWriter = new JsonWriter(writer);
            jsonWriter.setOutputType(JsonWriter.OutputType.json); // Pretty-print or minified

            // Root element
            jsonWriter.object();
            jsonWriter.set("lastUpdate", now.toString());

            jsonWriter.object("elements");
            jsonWriter.set("period", elements.period);
            jsonWriter.set("epoch", elements.epochJD);
            jsonWriter.set("semiMajorAxis", elements.semiMajorAxis);
            jsonWriter.set("eccentricity", elements.eccentricity);
            jsonWriter.set("inclination", elements.inclination);
            jsonWriter.set("ascendingNode", elements.ascendingNode);
            jsonWriter.set("argOfPericenter", elements.argOfPericenter);
            jsonWriter.set("meanAnomaly", elements.meanAnomaly);
            jsonWriter.pop(); // end elements

            jsonWriter.pop(); // end root object

            jsonWriter.flush();
        } catch (Exception e) {
            logger.error("Error writing JSON: " + filePath, e);
        }
    }
}
