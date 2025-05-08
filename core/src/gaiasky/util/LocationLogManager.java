/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.math.Vector3D;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

import java.time.Instant;
import java.util.LinkedList;

/**
 * Manages the location log model.
 */
public class LocationLogManager implements IObserver {
    protected static final Log logger = Logger.getLogger(LocationLogManager.class);

    private static final int MAX_SIZE = 200;
    private static LocationLogManager instance;
    /**
     * Array that holds the locations
     */
    private final LinkedList<LocationRecord> locations;

    public LocationLogManager() {
        this.locations = new LinkedList<>();
    }

    public static LocationLogManager instance() {
        if (instance == null) {
            initialize();
        }
        return instance;
    }

    public static void initialize() {
        instance = new LocationLogManager();
    }

    public LinkedList<LocationRecord> getLocations() {
        return this.locations;
    }

    public void clearLocations() {
        this.locations.clear();
    }

    /**
     * Starts capturing locations.
     * Must be called at some point during initialization, otherwise no locations will be captured.
     */
    public void startCapturing() {
        if (!EventManager.instance.isSubscribedTo(this, Event.CAMERA_NEW_CLOSEST)) {
            EventManager.instance.subscribe(this, Event.CAMERA_NEW_CLOSEST);
        }
    }

    /**
     * Stops capturing locations.
     */
    public void stopCapturing() {
        EventManager.instance.unsubscribe(this, Event.CAMERA_NEW_CLOSEST);
    }

    /**
     * Adds a new record with the given object, camera and time
     */
    public void addRecord(final IFocus object, final ICamera camera, final ITimeFrameProvider time) {
        if (object != null) {
            final Instant entryTime = Instant.now();
            final Instant simulationTime = time.getTime();
            final String name = object.getClosestName();

            // Check if the new record is already in the 10 most recent items
            boolean found = false;
            int stop = FastMath.max(0, locations.size() - 10);
            for (int i = locations.size() - 1; i >= stop; i--) {
                if (locations.get(i).name.equalsIgnoreCase(name)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Create record
                final LocationRecord record = new LocationRecord();
                record.name = name;
                record.entryTime = entryTime;
                record.simulationTime = simulationTime;
                record.position = new Vector3Q().set(camera.getPos());
                record.direction = new Vector3D().set(camera.getDirection());
                record.up = new Vector3D().set(camera.getUp());

                if (locations.size() == MAX_SIZE) {
                    locations.pollFirst();
                }
                locations.add(record);
                EventManager.publish(Event.NEW_LOCATION_RECORD, this, locations);
                logger.debug(I18n.msg("gui.locationlog.newrecord", record.toStringFull()));
            }
        }
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.CAMERA_NEW_CLOSEST) {
            final IFocus closest = (IFocus) data[0];
            this.addRecord(closest, GaiaSky.instance.getICamera(), GaiaSky.instance.time);

        }
    }

    /**
     * A single location
     */
    public static class LocationRecord {
        public String name;
        public Vector3Q position;
        public Vector3D direction;
        public Vector3D up;
        public Instant simulationTime;
        public Instant entryTime;

        @Override
        public String toString() {
            String elapsedStr = "";
            if (entryTime != null) {
                elapsedStr = elapsedString();
            }
            return name + " (" + I18n.msg("gui.locationlog.ago", elapsedStr) + ")";
        }

        public String elapsedString() {
            long elapsedMs = Instant.now().toEpochMilli() - entryTime.toEpochMilli();
            return GlobalResources.msToTimeString(elapsedMs);
        }

        public String toStringFull() {
            return "LocationRecord{" + "\n\tname='" + name + '\'' + "\n\tposition=" + position + "\n\tdirection=" + direction + "\n\tup=" + up + "\n\tsimulationTime=" + simulationTime + "\n\tentryTime=" + entryTime + "\n" + '}';
        }
    }
}
