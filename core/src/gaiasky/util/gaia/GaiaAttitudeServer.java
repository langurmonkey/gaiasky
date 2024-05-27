/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import gaiasky.data.api.OrientationServer;
import gaiasky.util.BinarySearchTree;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.QuaternionDouble;

import java.time.Instant;
import java.util.Date;

public class GaiaAttitudeServer implements OrientationServer {
    private static final Log logger = Logger.getLogger(GaiaAttitudeServer.class);
    // Dummy attitude for launch sequence
    IAttitude dummyAttitude;
    Nsl37 nsl;
    // The previous attitude
    AttitudeIntervalBean prevAttitude = null, current;
    // The first activation date
    Instant initialDate;
    // List of attitudes in a BST sorted by activation date
    private BinarySearchTree<AttitudeIntervalBean> attitudes;
    private IAttitude attitude;

    public GaiaAttitudeServer(String folder) {
        if (Settings.settings.data.realGaiaAttitude) {
            attitudes = AttitudeXmlParser.parseFolder(folder);
            if (attitudes != null) {
                initialDate = ((AttitudeIntervalBean) attitudes.findMin()).activationTime;
                current = new AttitudeIntervalBean("current", null, null, "dummy");
                // Dummy attitude
                dummyAttitude = new ConcreteAttitude(0, new QuaternionDouble(), false);
            } else {
                logger.error("Error loading real attitude: " + folder);
            }
        } else {
            // Use NSL as approximation
            nsl = new Nsl37();
        }
    }

    @Override
    public QuaternionDouble getOrientation(Date date) {
        return getOrientation(date.toInstant());
    }

    @Override
    public QuaternionDouble getOrientation(Instant instant) {
        if (attitude != null && attitude.getTime() == instant.toEpochMilli()) {
            return attitude.getQuaternion();
        } else {
            attitude = getAttitude(instant);
            if (attitude != null) {
                return attitude.getQuaternion();
            } else {
                return null;
            }
        }
    }

    /**
     * Returns the NSL37 attitude for the given date.
     *
     * @param date The date
     *
     * @return The attitude
     */
    public synchronized IAttitude getAttitude(final Instant date) {
        IAttitude result;
        if (Settings.settings.data.realGaiaAttitude) {
            // Find AttitudeType in timeSlots
            if (date.compareTo(initialDate) < 0) {
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

    @Override
    public QuaternionDouble getCurrentOrientation() {
        return hasOrientation() ? attitude.getQuaternion() : null;
    }

    @Override
    public boolean hasOrientation() {
        return attitude != null;
    }

    public synchronized String getCurrentAttitudeName() {
        if (prevAttitude != null) {
            return prevAttitude.file;
        }
        return null;
    }

}
