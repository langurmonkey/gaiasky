/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.samp;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.i18n.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.SampException;

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
        String hubName = "-";
        try {
            hubName = super.getConnection().getRegInfo().getHubId();
        }catch (NullPointerException | SampException ignored) {}

        logger.info(isConnected ? I18n.msg("samp.connected", hubName) : I18n.msg("samp.disconnected"));
        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, isConnected ? I18n.msg("samp.connected", hubName) : I18n.msg("samp.disconnected"));
    }

    @Override
    protected void disconnect() {
        super.disconnect();
        logger.info(I18n.msg("samp.disconnected"));
    }

}
