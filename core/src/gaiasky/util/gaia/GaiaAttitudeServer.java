/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import gaiasky.util.BinarySearchTree;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Quaterniond;

import java.util.Date;

/**
 * Provides caching of the last Nsl37 attitude requested. This allows for
 * calculating the attitude only once in each time step and using it in several
 * points in the processing.
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

    public GaiaAttitudeServer(String folder) {
        if (Settings.settings.data.realGaiaAttitude) {
            attitudes = AttitudeXmlParser.parseFolder(folder);
            if(attitudes != null) {
                initialDate = ((AttitudeIntervalBean) attitudes.findMin()).activationTime;
                current = new AttitudeIntervalBean("current", null, null, "dummy");
                // Dummy attitude
                dummyAttitude = new ConcreteAttitude(0, new Quaterniond(), false);
            }else{
                logger.error("Error loading real attitude: " + folder);
            }
        } else {
            // Use NSL as approximation
            nsl = new Nsl37();
        }
    }

    /**
     * Returns the NSL37 attitude for the given date.
     *
     * @param date The date
     * @return The attitude
     */
    public synchronized Attitude getAttitude(Date date) {
        Attitude result;
        if (Settings.settings.data.realGaiaAttitude) {
            // Find AttitudeType in timeSlots
            if (date.before(initialDate)) {
                result = dummyAttitude;
            } else {
                try {
                    current.activationTime = date;
                    AttitudeIntervalBean att = (AttitudeIntervalBean) attitudes.findIntervalStart(current);

                    if (prevAttitude != null && !att.equals(prevAttitude)) {
                        // Change!
                        logger.info(I18n.msg("notif.attitude.changed", att.toString(), att.activationTime));
                    }

                    prevAttitude = att;

                    // Get actual attitude
                    result = att.get(date);
                } catch (Exception e) {
                    logger.error(e);
                    // Fallback solution
                    if (nsl == null) {
                        nsl = new Nsl37();
                    }
                    result = nsl.getAttitude(date);
                }
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
