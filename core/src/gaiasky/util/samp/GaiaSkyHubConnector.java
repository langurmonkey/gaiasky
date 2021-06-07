/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.samp;

import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnector;

/**
 * Extends hub connector to provide some very basic logging using
 * the Gaia Sky internal logging system.
 */
public class GaiaSkyHubConnector extends HubConnector {
    private static final Log logger = Logger.getLogger(GaiaSkyHubConnector.class);

    public GaiaSkyHubConnector(ClientProfile profile) {
        super(profile);
    }

    @Override
    protected void connectionChanged(boolean isConnected) {
        super.connectionChanged(isConnected);
        logger.info(isConnected ? "Connected to SAMP hub" : "Disconnected from SAMP hub");
    }

    @Override
    protected void disconnect() {
        super.disconnect();
        logger.info("Disconnected from SAMP hub");
    }

}
