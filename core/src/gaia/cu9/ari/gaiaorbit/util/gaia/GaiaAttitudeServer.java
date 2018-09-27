package gaia.cu9.ari.gaiaorbit.util.gaia;

import java.util.Date;

import gaia.cu9.ari.gaiaorbit.util.BinarySearchTree;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.math.Quaterniond;

/**
 * Provides caching of the last Nsl37 attitude requested. This allows for
 * calculating the attitude only once in each time step and using it in several
 * points in the processing.
 * 
 * @author Toni Sagrista
 *
 */
public class GaiaAttitudeServer {
    private static final Log logger = Logger.getLogger(GaiaAttitudeServer.class);

    public static GaiaAttitudeServer instance;

    // List of attitudes in a BST sorted by activation date
    private BinarySearchTree attitudes;
    // Dummy attitude for launch sequence
    Attitude dummyAttitude;
    Nsl37 nsl;

    // The previous attitude
    AttitudeIntervalBean prevAttitude = null, current;

    // The first activation date
    Date initialDate;

    public GaiaAttitudeServer(String folder, String... files) {
        if (GlobalConf.data.REAL_GAIA_ATTITUDE) {
            attitudes = AttitudeXmlParser.parseFolder(folder, GlobalConf.runtime.STRIPPED_FOV_MODE, files);
            initialDate = ((AttitudeIntervalBean) attitudes.findMin()).activationTime;
            current = new AttitudeIntervalBean("current", null, null, "dummy");
            // Dummy attitude
            dummyAttitude = new ConcreteAttitude(0, new Quaterniond(), false);
        } else {
            // Use NSL as approximation
            nsl = new Nsl37();
        }
    }

    /**
     * Returns the NSL37 attitude for the given date.
     * 
     * @param date
     *            The date
     * @return The attitude
     */
    public synchronized Attitude getAttitude(Date date) {
        Attitude result;
        if (GlobalConf.data.REAL_GAIA_ATTITUDE) {
            // Find AttitudeType in timeSlots
            if (date.before(initialDate)) {
                result = dummyAttitude;
            } else {
                current.activationTime = date;
                AttitudeIntervalBean att = (AttitudeIntervalBean) attitudes.findIntervalStart(current);

                if (prevAttitude != null && !att.equals(prevAttitude)) {
                    // Change!
                    logger.info(I18n.bundle.format("notif.attitude.changed", att.toString(), att.activationTime));
                }

                prevAttitude = att;

                // Get actual attitude
                result = att.get(date);
            }
        } else {
            result = nsl.getAttitude(date);
        }

        return result;

    }

    public synchronized String getCurrentAttitudeName() {
        if (prevAttitude != null) {
            return prevAttitude.file;
        }
        return null;
    }

}
