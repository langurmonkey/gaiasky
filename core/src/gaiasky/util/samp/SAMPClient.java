/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.samp;

import com.badlogic.gdx.scenes.scene2d.Stage;
import gaiasky.GaiaSky;
import gaiasky.data.group.DatasetOptions;
import gaiasky.data.group.STILDataProvider;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interfce.DatasetLoadDialog;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.script.EventScriptingInterface;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.parse.Parser;
import org.apache.commons.io.FilenameUtils;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.*;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class SAMPClient implements IObserver {
    private static final Log logger = Logger.getLogger(SAMPClient.class);

    private static SAMPClient instance;

    public static SAMPClient getInstance() {
        if (instance == null)
            instance = new SAMPClient();
        return instance;
    }

    private HubConnector conn;
    private STILDataProvider provider;
    private Map<String, StarGroup> mapIdSg;
    private Map<String, String> mapIdUrl;
    private boolean preventProgrammaticEvents = false;

    public SAMPClient() {
        super();
        EventManager.instance.subscribe(this, Events.FOCUS_CHANGED, Events.DISPOSE);
    }

    public void initialize() {
        // Disable logging
        java.util.logging.Logger.getLogger("org.astrogrid.samp").setLevel(Level.OFF);

        // Init provider
        provider = new STILDataProvider();

        // Init map
        mapIdSg = new HashMap<>();
        mapIdUrl = new HashMap<>();

        ClientProfile cp = DefaultClientProfile.getProfile();
        conn = new GaiaSkyHubConnector(cp);

        // Configure it with metadata about this application
        Metadata meta = new Metadata();
        meta.setName(GlobalConf.APPLICATION_NAME);
        meta.setDescriptionText("3D Universe application focused on ESA's Gaia satellite");
        meta.setDocumentationUrl(GlobalConf.DOCUMENTATION);
        meta.setIconUrl(GlobalConf.ICON_URL);
        meta.put("author.name", GlobalConf.AUTHOR_NAME_PLAIN);
        meta.put("author.email", GlobalConf.AUTHOR_EMAIL);
        meta.put("author.affiliation", GlobalConf.AUTHOR_AFFILIATION_PLAIN);
        meta.put("home.page", GlobalConf.WEBPAGE);
        meta.put("gaiasky.version", GlobalConf.version.version);

        conn.declareMetadata(meta);

        // Load table
        conn.addMessageHandler(new AbstractMessageHandler("table.load.votable") {
            public Map processCall(HubConnection c, String senderId, Message msg) {
                // do stuff
                String name = (String) msg.getParam("name");
                String id = (String) msg.getParam("table-id");
                String url = (String) msg.getParam("url");

                boolean loaded = loadVOTable(url, id, name);

                if (loaded) {
                    logger.info("VOTable " + name + " loaded successfully");
                } else {
                    logger.info("Error loading VOTable " + name);
                }

                return null;
            }
        });

        // Select one row
        conn.addMessageHandler(new AbstractMessageHandler("table.highlight.row") {
            public Map processCall(HubConnection c, String senderId, Message msg) {
                // do stuff
                Long row = Parser.parseLong((String) msg.getParam("row"));
                String id = (String) msg.getParam("table-id");

                // First, fetch table if not here
                boolean loaded = mapIdSg.containsKey(id);

                // If table here, select
                if (loaded) {
                    logger.info("Select row " + row + " of " + id);

                    if (mapIdSg.containsKey(id)) {
                        StarGroup sg = mapIdSg.get(id);
                        sg.setFocusIndex(row.intValue());
                        preventProgrammaticEvents = true;
                        EventManager.instance.post(Events.FOCUS_CHANGE_CMD, sg);
                        preventProgrammaticEvents = false;
                    }
                }
                return null;
            }
        });

        // Select multiple rows
        conn.addMessageHandler(new AbstractMessageHandler("table.select.rowList") {
            public Map processCall(HubConnection c, String senderId, Message msg) {
                // do stuff
                ArrayList<String> rows = (ArrayList<String>) msg.getParam("row-list");
                String id = (String) msg.getParam("table-id");

                // First, fetch table if not here
                boolean loaded = mapIdSg.containsKey(id);

                // If table here, select
                if (loaded && rows != null && !rows.isEmpty()) {
                    logger.info("Select " + rows.size() + " rows of " + id + ". Gaia Sky does not support multiple selection, so only the first entry is selected.");
                    // We use the first one, as multiple selections are not supported in Gaia Sky
                    int row = Integer.parseInt(rows.get(0));
                    if (mapIdSg.containsKey(id)) {
                        StarGroup sg = mapIdSg.get(id);
                        sg.setFocusIndex(row);
                        preventProgrammaticEvents = true;
                        EventManager.instance.post(Events.FOCUS_CHANGE_CMD, sg);
                        preventProgrammaticEvents = false;
                    }
                }

                return null;
            }
        });

        // Point in sky
        conn.addMessageHandler(new AbstractMessageHandler("coord.pointAt.sky") {
            public Map processCall(HubConnection c, String senderId, Message msg) {
                // do stuff
                float ra = Float.parseFloat((String) msg.getParam("ra"));
                float dec = Float.parseFloat((String) msg.getParam("dec"));
                logger.info("Point to coordinate (ra,dec): (" + ra + ", " + dec + ")");

                EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FREE_MODE);
                EventManager.instance.post(Events.FREE_MODE_COORD_CMD, ra, dec);

                return null;
            }
        });

        // This step required even if no custom message handlers added.
        conn.declareSubscriptions(conn.computeSubscriptions());

        // Keep a look out for hubs if initial one shuts down
        conn.setAutoconnect(10);

    }

    public String getStatus() {
        if (conn == null) {
            return "Not initialized";
        } else {
            if (conn.isConnected()) {
                try {
                    return "Connected: " + conn.getConnection().getRegInfo().getHubId();
                } catch (Exception e) {
                    return "Error getting state";
                }
            } else {
                return "Not connected";
            }
        }
    }

    /**
     * Loads a VOTable into a star group
     *
     * @param url  The URL to fetch the table
     * @param id   The table id
     * @param name The table name
     * @return Boolean indicating whether loading succeeded or not
     */
    private boolean loadVOTable(String url, String id, String name) {
        logger.info("Loading VOTable: " + name + " from " + url);
        // Load selected file
        try {
            DataSource ds = new URLDataSource(new URL(url));
            Stage ui = GaiaSky.instance.mainGui.getGuiStage();
            String fileName = ds.getName();
            final DatasetLoadDialog dld = new DatasetLoadDialog(I18n.txt("gui.dsload.title") + ": " + fileName, fileName, GlobalResources.skin, ui);
            Runnable doLoad = () -> {
                try {
                    DatasetOptions dops = dld.generateDatasetOptions();
                    // Access dld properties
                    EventScriptingInterface.instance().loadDataset(FilenameUtils.getName(fileName), ds, CatalogInfo.CatalogInfoType.UI, dops, false);
                    // Open UI datasets
                    EventScriptingInterface.instance().maximizeInterfaceWindow();
                    EventScriptingInterface.instance().expandGuiComponent("DatasetsComponent");
                } catch (Exception e) {
                    logger.error(I18n.txt("notif.error", fileName), e);
                }
            };
            dld.setAcceptRunnable(doLoad);
            dld.show(ui);
            return true;
        } catch (Exception e) {
            logger.error(I18n.txt("notif.error", url), e);
            return false;
        }

    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case FOCUS_CHANGED:
            if (!preventProgrammaticEvents && data[0] instanceof StarGroup) {
                StarGroup sg = (StarGroup) data[0];
                if (conn != null && conn.isConnected() && mapIdSg.containsValue(sg)) {
                    String id = sg.getName();
                    String url = mapIdUrl.get(id);
                    int row = sg.getCandidateIndex();

                    Message msg = new Message("table.highlight.row");
                    msg.addParam("row", Integer.toString(row));
                    msg.addParam("table-id", id);
                    msg.addParam("url", url);

                    try {
                        conn.getConnection().notifyAll(msg);
                    } catch (SampException e) {
                        logger.error(e);
                    }
                }
            }
            break;
        case DISPOSE:
            if (conn != null) {
                conn.setActive(false);
            }
            break;
        }

    }

}
