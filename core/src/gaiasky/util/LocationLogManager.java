package gaiasky.util;

import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.time.Instant;
import java.util.LinkedList;

/**
 * This guy is in charge of logging the visited locations and serving
 * them to the corresponding subsystems.
 */
public class LocationLogManager implements IObserver {
    protected static final Log logger = Logger.getLogger(LocationLogManager.class);

    private static final int MAX_SIZE = 200;
    private static LocationLogManager instance;

    public static LocationLogManager instance() {
        if (instance == null) {
            initialize();
        }
        return instance;
    }

    public static void initialize() {
        instance = new LocationLogManager();
    }

    /**
     * Array that holds the locations
     */
    private final LinkedList<LocationRecord> locations;

    /**
     * A single location
     */
    public static class LocationRecord {
        public String name;
        public Vector3b position;
        public Vector3d direction;
        public Vector3d up;
        public Instant simulationTime;
        public Instant entryTime;

        @Override
        public String toString() {
            String elapsedStr = "";
            if (entryTime != null) {
                elapsedStr = elapsedString();
            }
            return name + " (" + I18n.txt("gui.locationlog.ago", elapsedStr) + ")";
        }

        public String elapsedString() {
            long elapsedMs = Instant.now().toEpochMilli() - entryTime.toEpochMilli();
            return GlobalResources.msToTimeString(elapsedMs);
        }

        public String toStringFull() {
            return "LocationRecord{" + "\n\tname='" + name + '\'' + "\n\tposition=" + position + "\n\tdirection=" + direction + "\n\tup=" + up + "\n\tsimulationTime=" + simulationTime + "\n\tentryTime=" + entryTime + "\n" + '}';
        }
    }

    public LocationLogManager() {
        this.locations = new LinkedList<>();
    }

    public LinkedList<LocationRecord> getLocations() {
        return this.locations;
    }

    public void clearLocations() {
        this.locations.clear();
    }

    /*
     * Starts capturing locations.
     * Must be called at some point during initialization, otherwise no locations will be captured.
     */
    public void startCapturing() {
        if (!EventManager.instance.isSubscribedTo(this, Event.CAMERA_NEW_CLOSEST))
            EventManager.instance.subscribe(this, Event.CAMERA_NEW_CLOSEST);
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
            int stop = Math.max(0, locations.size() - 10);
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
                record.position = new Vector3b().set(camera.getPos());
                record.direction = new Vector3d().set(camera.getDirection());
                record.up = new Vector3d().set(camera.getUp());

                if (locations.size() == MAX_SIZE) {
                    locations.pollFirst();
                }
                locations.add(record);
                EventManager.publish(Event.NEW_LOCATION_RECORD, this, locations);
                logger.debug(I18n.txt("gui.locationlog.newrecord", record.toStringFull()));
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
}
