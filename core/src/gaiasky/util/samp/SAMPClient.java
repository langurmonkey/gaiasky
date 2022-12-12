/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.samp;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.GaiaSky;
import gaiasky.data.group.DatasetOptions;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.DatasetLoadDialog;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.script.EventScriptingInterface;
import gaiasky.util.*;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;
import gaiasky.util.parse.Parser;
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

/**
 * Implements the SAMP client, which attempts the connection to a hub and handles
 * SAMP messages over that connection.
 */
public class SAMPClient implements IObserver {
    private static final Log logger = Logger.getLogger(SAMPClient.class);
    private final CatalogManager catalogManager;
    private HubConnector conn;
    private TwoWayHashmap<String, Entity> idToNode;
    private Map<String, String> idToUrl;
    private boolean preventProgrammaticEvents = false;

    public SAMPClient(final CatalogManager catalogManager) {
        super();
        this.catalogManager = catalogManager;
        EventManager.instance.subscribe(this, Event.FOCUS_CHANGED, Event.CATALOG_REMOVE, Event.DISPOSE);
    }

    public void initialize(final Skin skin) {
        // Disable logging
        java.util.logging.Logger.getLogger("org.astrogrid.samp").setLevel(Level.OFF);

        // Init map
        idToNode = new TwoWayHashmap<>();
        idToUrl = new HashMap<>();

        ClientProfile cp = DefaultClientProfile.getProfile();
        conn = new GaiaSkyHubConnector(cp);

        // Configure it with metadata about this application
        Metadata meta = new Metadata();
        meta.setName(Settings.APPLICATION_NAME);
        meta.setDescriptionText("Open source 3D universe simulator for desktop and VR with support for more than a billion objects.");
        meta.setDocumentationUrl(Settings.DOCUMENTATION);
        meta.setIconUrl(Settings.ICON_URL.replaceAll("[^\\x00-\\x7F]", "?"));
        meta.put("author.name", Settings.AUTHOR_NAME_PLAIN);
        meta.put("author.email", Settings.AUTHOR_EMAIL);
        meta.put("author.affiliation", Settings.AUTHOR_AFFILIATION_PLAIN);
        meta.put("home.page", Settings.WEBPAGE);
        meta.put("gaiasky.version", Settings.settings.version.version);

        conn.declareMetadata(meta);

        // Load table
        conn.addMessageHandler(new AbstractMessageHandler("table.load.votable") {
            public Map<Object, Object> processCall(HubConnection c, String senderId, Message msg) {
                // do stuff
                String name = (String) msg.getParam("name");
                String id = (String) msg.getParam("table-id");
                String url = (String) msg.getParam("url");

                boolean loaded = loadVOTable(url, id, name, skin);

                if (!loaded) {
                    logger.info(I18n.msg("samp.error.votable", name));
                }

                return null;
            }
        });

        // Select one row
        conn.addMessageHandler(new AbstractMessageHandler("table.highlight.row") {
            public Map<Object, Object> processCall(HubConnection c, String senderId, Message msg) {
                // do stuff
                long row = Parser.parseLong((String) msg.getParam("row"));
                String id = (String) msg.getParam("table-id");

                // First, fetch table if not here
                boolean loaded = idToNode.containsKey(id);

                // If table here, select
                if (loaded) {
                    logger.info("Select row " + row + " of " + id);

                    if (idToNode.containsKey(id)) {
                        Entity node = idToNode.getForward(id);
                        if (Mapper.particleSet.has(node) || Mapper.starSet.has(node)) {
                            // Stars or particles
                            ParticleSet pg = Mapper.particleSet.has(node) ? Mapper.particleSet.get(node) : Mapper.starSet.get(node);
                            pg.setFocusIndex((int) row);
                            preventProgrammaticEvents = true;
                            EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE);
                            EventManager.publish(Event.FOCUS_CHANGE_CMD, this, node);
                            preventProgrammaticEvents = false;
                        } else {
                            // Star cluster?
                            var graph = Mapper.graph.get(node);
                            if (graph.children != null && graph.children.size > (int) row) {
                                Entity sgn = graph.children.get((int) row);
                                preventProgrammaticEvents = true;
                                EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE);
                                EventManager.publish(Event.FOCUS_CHANGE_CMD, this, sgn);
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
            public Map<Object, Object> processCall(HubConnection c, String senderId, Message msg) {
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
                        Entity node = idToNode.getForward(id);
                        if (Mapper.particleSet.has(node) || Mapper.starSet.has(node)) {
                            // Stars or particles
                            ParticleSet pg = Mapper.particleSet.has(node) ? Mapper.particleSet.get(node) : Mapper.starSet.get(node);
                            pg.setFocusIndex(row);
                            preventProgrammaticEvents = true;
                            EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
                            EventManager.publish(Event.FOCUS_CHANGE_CMD, this, pg);
                            preventProgrammaticEvents = false;
                        }
                    }
                }

                return null;
            }
        });

        // Point in sky
        conn.addMessageHandler(new AbstractMessageHandler("coord.pointAt.sky") {
            public Map<Object, Object> processCall(HubConnection c, String senderId, Message msg) {
                // do stuff
                double ra = Parser.parseDouble((String) msg.getParam("ra"));
                double dec = Parser.parseDouble((String) msg.getParam("dec"));
                logger.info("Point to coordinate (ra,dec): (" + ra + ", " + dec + ")");

                EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
                EventManager.publish(Event.FREE_MODE_COORD_CMD, this, ra, dec);

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
            return I18n.msg("gui.debug.samp.notinit");
        } else {
            if (conn.isConnected()) {
                try {
                    return I18n.msg("gui.debug.samp.connected", conn.getConnection().getRegInfo().getHubId());
                } catch (Exception e) {
                    return I18n.msg("gui.debug.samp.error");
                }
            } else {
                return I18n.msg("gui.debug.samp.notconnected");
            }
        }
    }

    /**
     * Loads a VOTable into a star group
     *
     * @param url  The URL to fetch the table
     * @param id   The table id
     * @param name The table name
     *
     * @return Boolean indicating whether loading succeeded or not
     */
    private boolean loadVOTable(final String url, final String id, final String name, final Skin skin) {
        logger.info("Loading VOTable: " + name + " from " + url);
        // Load selected file
        try {
            DataSource dataSource = new URLDataSource(new URL(url));
            Stage ui = GaiaSky.instance.mainGui.getGuiStage();
            String fileName = dataSource.getName();
            final DatasetLoadDialog dld = new DatasetLoadDialog(I18n.msg("gui.dsload.title") + ": " + fileName, fileName, skin, ui);
            Runnable doLoad = () -> {
                try {
                    DatasetOptions datasetOptions = dld.generateDatasetOptions();
                    // Load dataset
                    boolean loaded = ((EventScriptingInterface) GaiaSky.instance.scripting()).loadDataset(id, dataSource, CatalogInfoSource.SAMP, datasetOptions, true);
                    if (loaded) {
                        // Select first
                        CatalogInfo ci = catalogManager.get(id);
                        var node = ci.entity;
                        if (node != null) {
                            var graph = Mapper.graph.get(node);
                            if (Mapper.particleSet.has(node) || Mapper.starSet.has(node)) {
                                // Stars or particles
                                ParticleSet pg = Mapper.particleSet.has(node) ? Mapper.particleSet.get(node) : Mapper.starSet.get(node);
                                if (pg.data() != null && !pg.data().isEmpty() && EntityUtils.isVisibilityOn(node)) {
                                    EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
                                    EventManager.publish(Event.FOCUS_CHANGE_CMD, this, pg.getRandomParticleName());
                                }
                            } else if (graph.children != null && !graph.children.isEmpty() && EntityUtils.isVisibilityOn(graph.children.get(0))) {
                                EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
                                EventManager.publish(Event.FOCUS_CHANGE_CMD, this, graph.children.get(0));
                            }
                            // Open UI datasets
                            GaiaSky.instance.scripting().maximizeInterfaceWindow();
                            GaiaSky.instance.scripting().expandGuiComponent("DatasetsComponent");
                            idToNode.add(id, node);
                            idToUrl.put(id, url);
                        }
                    }
                } catch (Exception e) {
                    logger.error(I18n.msg("notif.error", fileName), e);
                }
            };
            dld.setAcceptRunnable(() -> {
                Thread t = new Thread(doLoad);
                t.start();
            });
            dld.show(ui);
            return true;
        } catch (Exception e) {
            logger.error(I18n.msg("notif.error", url), e);
            return false;
        }

    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case FOCUS_CHANGED:
            if (!preventProgrammaticEvents) {
                if (conn != null && conn.isConnected()) {
                    if (data[0] instanceof FocusView) {
                        var focus = (FocusView) data[0];
                        var entity = focus.getEntity();
                        if (focus.isSet()) {
                            var pg = focus.getSet();
                            if (idToNode.containsValue(entity)) {
                                String id = idToNode.getBackward(entity);
                                String url = idToUrl.get(id);
                                int row = pg.candidateFocusIndex;

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
                        } else if (Mapper.cluster.has(entity)) {
                            var graph = Mapper.graph.get(entity);
                            var cluster = Mapper.cluster.get(entity);
                            if (graph.parent != null && Mapper.fade.has(graph.parent)) {
                                // Comes from a catalog
                                if (idToNode.containsValue(graph.parent)) {
                                    String id = idToNode.getBackward(graph.parent);
                                    String url = idToUrl.get(id);
                                    var parentGraph = Mapper.graph.get(graph.parent);
                                    int row = parentGraph.children.indexOf(entity, true);

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
            }
            break;
        case CATALOG_REMOVE:
            String dsName = (String) data[0];
            if (idToNode.containsKey(dsName)) {
                idToNode.removeKey(dsName);
            }
            idToUrl.remove(dsName);
            break;
        case DISPOSE:
            if (conn != null) {
                conn.setActive(false);
            }
            break;
        }

    }

}
