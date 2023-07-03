/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import gaiasky.util.Logger.Log;
import gaiasky.util.gdx.loader.OwnTextureLoader.OwnTextureParameter;
import gaiasky.util.gdx.loader.PFMData;
import gaiasky.util.gdx.loader.PFMDataLoader.PFMDataParameter;
import gaiasky.util.i18n.I18n;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages slave Gaia Sky instances that connect to master instances.
 */
public class SlaveManager {
    private static final Log logger = Logger.getLogger(SlaveManager.class);

    public static SlaveManager instance;
    public String bufferId, regionId;
    public Path pfm, blend;
    public int xResolution, yResolution;
    public float yaw, pitch, roll, upAngle, downAngle, rightAngle, leftAngle;
    public float cameraFov;
    private boolean initialized = false;
    public SlaveManager() {
        super();
        Settings settings = Settings.settings;
        if (settings.program.net.slave.active) {
            if (settings.program.net.isSlaveMPCDIPresent()) {
                logger.info("Using slave configuration file: " + settings.program.net.slave.configFile);
                String mpcdi = settings.program.net.slave.configFile;
                try {
                    Path loc = unpackMpcdi(mpcdi);
                    parseMpcdi(mpcdi, loc);

                    if (initialized) {
                        pushToConf();
                        printInfo();
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            } else if (settings.program.net.areSlaveConfigPropertiesPresent()) {
                yaw = settings.program.net.slave.yaw;
                pitch = settings.program.net.slave.pitch;
                roll = settings.program.net.slave.roll;

                xResolution = settings.graphics.fullScreen.resolution[0];
                yResolution = settings.graphics.fullScreen.resolution[1];
                upAngle = downAngle = rightAngle = leftAngle = settings.scene.camera.fov / 2f;
                if (settings.program.net.slave.warpFile != null && !settings.program.net.slave.warpFile.isEmpty())
                    pfm = Paths.get(settings.program.net.slave.warpFile);
                else
                    pfm = null;

                if (settings.program.net.slave.blendFile != null && !settings.program.net.slave.blendFile.isEmpty())
                    blend = Paths.get(settings.program.net.slave.blendFile);
                else
                    blend = null;

                initialized = true;

                setDefaultConf();
                printInfo();
            }
        }
    }

    public static void initialize() {
        if (instance == null && Settings.settings.program.net.slave.active) {
            instance = new SlaveManager();
        }
    }

    /**
     * Checks if a special projection is active in this slave (yaw/pitch/roll, etc.)
     *
     * @return True if a special projection is active
     */
    public static boolean projectionActive() {
        return instance != null && instance.initialized;
    }

    public static void load(AssetManager manager) {
        if (projectionActive()) {
            instance.loadAssets(manager);
            // Mute cursor
            Gdx.graphics.setSystemCursor(SystemCursor.None);
        }
    }

    /**
     * Unpacks the given MPCDI file and returns the unzip location.
     *
     * @param mpcdi The MPCDI configuration file.
     *
     * @return THe path where the contents were unpacked.
     *
     * @throws IOException If creating directories fails.
     */
    private Path unpackMpcdi(String mpcdi) throws IOException {
        if (mpcdi != null && !mpcdi.isEmpty()) {
            // Using MPCDI
            Path mpcdiPath = Path.of(mpcdi);
            if (!mpcdiPath.isAbsolute()) {
                // Assume mpcdi folder
                mpcdiPath = SysUtils.getDefaultMpcdiDir().resolve(mpcdi);
            }
            logger.info(I18n.msg("notif.loading", mpcdiPath));

            String unpackDirName = "mpcdi_" + System.nanoTime();
            Path unzipLocation = SysUtils.getTempDir(Settings.settings.data.location).resolve(unpackDirName);
            Files.createDirectories(unzipLocation);
            ZipUtils.unzip(mpcdiPath.toString(), unzipLocation.toAbsolutePath().toString());

            return unzipLocation;

        }

        return null;

    }

    /**
     * Parses the mpcdi.xml file and extracts the relevant information
     *
     * @param mpcdi The name of the MPCDI file.
     * @param loc   The directory of the MPCDI file to load.
     */
    private void parseMpcdi(String mpcdi, Path loc) throws IOException, ParserConfigurationException, SAXException {
        if (loc != null) {
            // Parse mpcdi.xml
            Path mpcdiXml = loc.resolve("mpcdi.xml");
            if (!Files.exists(mpcdiXml)) {
                logger.error("mpcdi.xml file not found in " + mpcdi);
                return;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(mpcdiXml.toFile());

            if (doc != null) {
                // Get data and modify config
                Element root = doc.getDocumentElement();
                String profile = root.getAttribute("profile");
                if (!profile.equalsIgnoreCase("3d")) {
                    logger.warn("Gaia Sky only supports the '3d' profile");
                }
                // Display
                NodeList displays = root.getElementsByTagName("display");
                if (displays.getLength() != 1) {
                    logger.warn(displays.getLength() + " <display> elements found in MPCDI, which goes against the specs");
                }
                Node displayNode = displays.item(0);
                if (displayNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element display = (Element) displayNode;
                    NodeList buffers = display.getElementsByTagName("buffer");
                    if (buffers.getLength() > 1) {
                        logger.warn("Gaia Sky only supports one buffer per display, found " + buffers.getLength());
                    }
                    Node bufferNode = buffers.item(0);
                    if (bufferNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element buffer = (Element) bufferNode;
                        bufferId = buffer.getAttribute("id");
                        int xRes = Integer.parseInt(buffer.getAttribute("xResolution"));
                        int yRes = Integer.parseInt(buffer.getAttribute("yResolution"));
                        NodeList regions = buffer.getElementsByTagName("region");
                        if (regions.getLength() > 1) {
                            logger.warn("Gaia Sky only supports one region per buffer, found " + buffers.getLength());
                        }
                        Node regionNode = regions.item(0);
                        if (regionNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element region = (Element) regionNode;
                            regionId = region.getAttribute("id");
                            float x = Float.parseFloat(region.getAttribute("x"));
                            float y = Float.parseFloat(region.getAttribute("y"));
                            float xs = Float.parseFloat(region.getAttribute("xSize"));
                            float ys = Float.parseFloat(region.getAttribute("ySize"));
                            xResolution = Integer.parseInt(region.getAttribute("xResolution"));
                            yResolution = Integer.parseInt(region.getAttribute("yResolution"));

                            if (x != 0f || y != 0f || xs != 1f || ys != 1f) {
                                logger.error("Gaia Sky only supports a region that covers the full frame (i.e. in [0, 1])");
                                logger.error("Stopping the parsing of MPCDI file " + mpcdi);
                                return;
                            }

                            NodeList frustums = region.getElementsByTagName("frustum");
                            if (frustums.getLength() != 1) {
                                logger.warn("More than one <frustum> tag goes against the specification of MPCDI, found " + frustums.getLength());
                            }
                            Node frustumNode = frustums.item(0);
                            if (frustumNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element frustum = (Element) frustumNode;
                                yaw = Float.parseFloat(frustum.getElementsByTagName("yaw").item(0).getTextContent());
                                pitch = Float.parseFloat(frustum.getElementsByTagName("pitch").item(0).getTextContent());
                                roll = Float.parseFloat(frustum.getElementsByTagName("roll").item(0).getTextContent());

                                rightAngle = Float.parseFloat(frustum.getElementsByTagName("rightAngle").item(0).getTextContent());
                                leftAngle = Float.parseFloat(frustum.getElementsByTagName("leftAngle").item(0).getTextContent());
                                upAngle = Float.parseFloat(frustum.getElementsByTagName("upAngle").item(0).getTextContent());
                                downAngle = Float.parseFloat(frustum.getElementsByTagName("downAngle").item(0).getTextContent());

                                cameraFov = upAngle - downAngle;

                                initialized = true;
                            }
                        }
                    }
                }

                // Files
                NodeList filesNodes = root.getElementsByTagName("files");
                if (filesNodes.getLength() != 1) {
                    logger.warn(filesNodes.getLength() + " <files> elements found in MPCDI, which goes against the specs");
                } else {
                    Node filesNode = filesNodes.item(0);
                    if (filesNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element files = (Element) filesNode;
                        Node filesetNode = files.getElementsByTagName("fileset").item(0);
                        if (filesetNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element fileset = (Element) filesetNode;
                            String reg = fileset.getAttribute("region");
                            if (reg.equals(regionId)) {
                                NodeList geometryWarpFiles = fileset.getElementsByTagName("geometryWarpFile");
                                if (geometryWarpFiles.getLength() < 1) {
                                    logger.warn("Geometry warp file not found!");
                                } else {
                                    Element geowarp = (Element) geometryWarpFiles.item(0);
                                    Element path = (Element) geowarp.getElementsByTagName("path").item(0);
                                    String pfmFile = path.getTextContent();
                                    pfm = loc.resolve(pfmFile);
                                    if (!Files.exists(pfm)) {
                                        logger.error("The geometry warp file does not exist: " + pfm);
                                    }
                                    NodeList interpolationNodes = geowarp.getElementsByTagName("interpolation");
                                    if (interpolationNodes.getLength() > 0) {
                                        Element interpolation = (Element) interpolationNodes.item(0);
                                        if (!interpolation.getTextContent().equals("linear")) {
                                            logger.warn("WARN: only linear interpolation supported, found " + interpolation.getTextContent());
                                        }
                                    }
                                }
                            } else {
                                logger.warn("Region in fileset tag does not match region id: " + regionId + " != " + reg);
                            }
                        }

                    }
                }

            }
        }
    }

    public boolean isWarpOrBlend() {
        return pfm != null || blend != null;
    }

    private void pushToConf() {
        if (initialized) {
            Settings.settings.graphics.fullScreen.resolution[0] = Settings.settings.graphics.resolution[0] = xResolution;
            Settings.settings.graphics.fullScreen.resolution[1] = Settings.settings.graphics.resolution[1] = yResolution;
            Settings.settings.graphics.fullScreen.active = true;
            Settings.settings.scene.camera.fov = cameraFov;

            setDefaultConf();
        }
    }

    private void setDefaultConf() {
        Settings.settings.runtime.displayGui = false;
        Settings.settings.runtime.inputEnabled = false;
        Settings.settings.scene.crosshair.focus = false;
        Settings.settings.scene.crosshair.home = false;
        Settings.settings.scene.crosshair.closest = false;
    }

    private void printInfo() {
        logger.info("Slave configuration modified with MPCDI settings");
        logger.info("   Resolution: " + xResolution + "x" + yResolution);
        logger.info("   Fov: " + cameraFov);
        logger.info("   Yaw/Pitch/Roll: " + yaw + "/" + pitch + "/" + roll);
    }

    public void loadAssets(AssetManager manager) {
        if (pfm != null && Files.exists(pfm)) {
            PFMDataParameter param = new PFMDataParameter();
            param.invert = false;
            manager.load(pfm.toString(), PFMData.class, param);
        }
        if (blend != null && Files.exists(blend)) {
            OwnTextureParameter param = new OwnTextureParameter();
            param.magFilter = Texture.TextureFilter.Linear;
            param.format = Pixmap.Format.RGBA8888;
            manager.load(blend.toString(), Texture.class, param);
        }
    }

}
