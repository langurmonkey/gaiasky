/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.samp;

import com.badlogic.gdx.scenes.scene2d.Stage;
import gaiasky.GaiaSky;
import gaiasky.data.group.DatasetOptions;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interfce.DatasetLoadDialog;
import gaiasky.scenegraph.FadeNode;
import gaiasky.scenegraph.ParticleGroup;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.StarCluster;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.script.EventScriptingInterface;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.parse.Parser;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.*;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;

import java.net.URL;
import java.nio.file.Path;
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
    private TwoWayHashmap<String, FadeNode> idToNode;
    private Map<String, String> idToUrl;
    private boolean preventProgrammaticEvents = false;

    public SAMPClient() {
        super();
        EventManager.instance.subscribe(this, Events.FOCUS_CHANGED, Events.CATALOG_REMOVE, Events.DISPOSE);
    }

    public void initialize() {
        // Disable logging
        java.util.logging.Logger.getLogger("org.astrogrid.samp").setLevel(Level.OFF);

        // Init map
        idToNode = new TwoWayHashmap<>();
        idToUrl = new HashMap<>();

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

                if (!loaded) {
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
                boolean loaded = idToNode.containsKey(id);

                // If table here, select
                if (loaded) {
                    logger.info("Select row " + row + " of " + id);

                    if (idToNode.containsKey(id)) {
                        if (idToNode.getForward(id) instanceof ParticleGroup) {
                            // Stars or particles
                            ParticleGroup pg = (ParticleGroup) idToNode.getForward(id);
                            pg.setFocusIndex(row.intValue());
                            preventProgrammaticEvents = true;
                            EventManager.instance.post(Events.FOCUS_CHANGE_CMD, pg);
                            preventProgrammaticEvents = false;
                        } else if (idToNode.getForward(id) instanceof FadeNode) {
                            // Star cluster
                            FadeNode fn = idToNode.getForward(id);
                            if (fn.children != null && fn.children.size > row.intValue()) {
                                SceneGraphNode sgn = fn.children.get(row.intValue());
                                preventProgrammaticEvents = true;
                                EventManager.instance.post(Events.FOCUS_CHANGE_CMD, sgn);
                                preventProgrammaticEvents = false;
                            } else {
                                logger.info("Star cluster to select not found: " + row);
                            }
                        }
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
                boolean loaded = idToNode.containsKey(id);

                // If table here, select
                if (loaded && rows != null && !rows.isEmpty()) {
                    logger.info("Select " + rows.size() + " rows of " + id + ". Gaia Sky does not support multiple selection, so only the first entry is selected.");
                    // We use the first one, as multiple selections are not supported in Gaia Sky
                    int row = Integer.parseInt(rows.get(0));
                    if (idToNode.containsKey(id)) {
                        FadeNode fn = idToNode.getForward(id);
                        if (fn instanceof ParticleGroup) {
                            ParticleGroup pg = (ParticleGroup) fn;
                            pg.setFocusIndex(row);
                            preventProgrammaticEvents = true;
                            EventManager.instance.post(Events.FOCUS_CHANGE_CMD, pg);
                            preventProgrammaticEvents = false;
                        }
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
            Path path = Path.of(fileName);
            final DatasetLoadDialog dld = new DatasetLoadDialog(I18n.txt("gui.dsload.title") + ": " + fileName, fileName, GlobalResources.skin, ui);
            Runnable doLoad = () -> {
                try {
                    DatasetOptions dops = dld.generateDatasetOptions();
                    // Load dataset
                    EventScriptingInterface.instance().loadDataset(id, ds, CatalogInfo.CatalogInfoType.SAMP, dops, true);
                    // Select first
                    CatalogInfo ci = CatalogManager.instance().get(id);
                    if (ci.object != null) {
                        if (ci.object instanceof ParticleGroup) {
                            ParticleGroup pg = (ParticleGroup) ci.object;
                            if (pg.data() != null && !pg.data().isEmpty() && pg.isVisibilityOn()) {
                                EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraManager.CameraMode.FOCUS_MODE);
                                EventManager.instance.post(Events.FOCUS_CHANGE_CMD, pg.getRandomParticleName());
                            }
                        } else if (ci.object.children != null && !ci.object.children.isEmpty() && ci.object.children.get(0).isVisibilityOn()) {
                            EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraManager.CameraMode.FOCUS_MODE);
                            EventManager.instance.post(Events.FOCUS_CHANGE_CMD, ci.object.children.get(0));
                        }
                        // Open UI datasets
                        EventScriptingInterface.instance().maximizeInterfaceWindow();
                        EventScriptingInterface.instance().expandGuiComponent("DatasetsComponent");
                        idToNode.add(id, ci.object);
                        idToUrl.put(id, url);
                    }
                } catch (Exception e) {
                    logger.error(I18n.txt("notif.error", fileName), e);
                }
            };
            dld.setAcceptRunnable(() -> {
                Thread t = new Thread(doLoad);
                t.start();
            });
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
                if (!preventProgrammaticEvents) {
                    if (conn != null && conn.isConnected()) {
                        if (data[0] instanceof ParticleGroup) {
                            ParticleGroup pg = (ParticleGroup) data[0];
                            if (idToNode.containsValue(pg)) {
                                String id = idToNode.getBackward(pg);
                                String url = idToUrl.get(id);
                                int row = pg.getCandidateIndex();

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
                        } else if (data[0] instanceof StarCluster) {
                            StarCluster sc = (StarCluster) data[0];
                            if (sc.parent != null && sc.parent instanceof FadeNode) {
                                // Comes from a catalog
                                FadeNode parent = (FadeNode) sc.parent;
                                if (idToNode.containsValue(parent)) {
                                    String id = idToNode.getBackward(parent);
                                    String url = idToUrl.get(id);
                                    int row = parent.children.indexOf(sc, true);

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
                        }
                    }
                }
                break;
            case CATALOG_REMOVE:
                String dsName = (String) data[0];
                if (idToNode.containsKey(dsName)) {
                    idToNode.removeKey(dsName);
                }
                if (idToUrl.containsKey(dsName)) {
                    idToUrl.remove(dsName);
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
