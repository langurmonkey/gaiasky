package gaia.cu9.ari.gaiaorbit.util.samp;

import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnector;

import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

/**
 * Extends hub connector to provide some very basic logging using
 * the Gaia Sky internal logging system.
 * @author tsagrista
 *
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
