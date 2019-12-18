/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import gaiasky.desktop.util.SysUtils;
import gaiasky.util.Logger.Log;
import gaiasky.util.gdx.loader.PFMTextureLoader.PFMTextureParameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages a slave instance configured with an MPCDI file.
 */
public class SlaveManager {
    private static final Log logger = Logger.getLogger(SlaveManager.class);

    public static SlaveManager instance;

    public static void initialize() {
        if (instance == null && GlobalConf.program.NET_SLAVE) {
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

    public static void load(AssetManager manager){
        if(projectionActive()){
            instance.loadAssets(manager);
        }
    }

    private boolean initialized = false;

    public String bufferId, regionId;
    public Path pfm;
    public int xResolution, yResolution;
    public float yaw, pitch, roll, upAngle, downAngle, rightAngle, leftAngle;
    public float cameraFov;

    public SlaveManager() {
        super();
        if (GlobalConf.program.isSlave()) {
            if(GlobalConf.program.isSlaveMPCDIPresent()) {
                logger.info("Using slave configuration file: " + GlobalConf.program.NET_SLAVE_CONFIG);
                String mpcdi = GlobalConf.program.NET_SLAVE_CONFIG;
                try {
                    File loc = unpackMpcdi(mpcdi);
                    parseMpcdi(mpcdi, loc);

                    if (initialized) {
                        pushToConf();
                        printInfo();
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            } else if (GlobalConf.program.areSlaveConfigPropertiesPresent()){
                yaw = GlobalConf.program.NET_SLAVE_YAW;
                pitch = GlobalConf.program.NET_SLAVE_PITCH;
                roll = GlobalConf.program.NET_SLAVE_ROLL;

                xResolution = GlobalConf.screen.FULLSCREEN_WIDTH;
                yResolution = GlobalConf.screen.FULLSCREEN_HEIGHT;
                upAngle = downAngle = rightAngle = leftAngle = GlobalConf.scene.CAMERA_FOV / 2f;
                pfm = Paths.get(GlobalConf.program.NET_SLAVE_WARP);

                initialized = true;

                setDefaultConf();
                printInfo();
            }
        } else {
            // Not a slave
        }
    }

    /**
     * Unpacks the given MPCDI file and returns the unzip location
     *
     * @param mpcdi
     * @return
     * @throws IOException
     */
    private File unpackMpcdi(String mpcdi) throws IOException {
        if (mpcdi != null && !mpcdi.isEmpty()) {
            // Using MPCDI
            Path mpcdiPath = Path.of(mpcdi);
            if (!mpcdiPath.isAbsolute()) {
                // Assume mpcdi folder
                mpcdiPath = Path.of(SysUtils.getDefaultMpcdiDir().getPath(), mpcdi);
            }
            logger.info(I18n.txt("notif.loading", mpcdiPath));

            String unpackDirName = "mpcdi_" + System.nanoTime();
            File unzipLocation = new File(SysUtils.getDefaultTmpDir(), unpackDirName + File.separator);
            unzipLocation.mkdirs();
            ZipUtils.unzip(mpcdiPath.toString(), unzipLocation.getAbsolutePath());

            return unzipLocation;

        } else {
            // Not using MPCDI
        }
        return null;

    }

    /**
     * Parses the mpcdi.xml file and extracts the relevant information
     *
     * @param mpcdi
     * @param loc
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void parseMpcdi(String mpcdi, File loc) throws IOException, ParserConfigurationException, SAXException {
        if (loc != null) {
            // Parse mpcdi.xml
            Path mpcdiXml = Path.of(loc.getPath(), "mpcdi.xml");
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
                    logger.warn("Gaya Sky only supports the '3d' profile");
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
                                    pfm = Path.of(loc.getPath(), pfmFile);
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

    private void pushToConf() {
        if (initialized) {
            GlobalConf.screen.FULLSCREEN_WIDTH = GlobalConf.screen.SCREEN_WIDTH = xResolution;
            GlobalConf.screen.FULLSCREEN_HEIGHT = GlobalConf.screen.SCREEN_HEIGHT = yResolution;
            GlobalConf.screen.FULLSCREEN = true;
            GlobalConf.scene.CAMERA_FOV = cameraFov;

            setDefaultConf();
        }
    }

    private void setDefaultConf() {
        GlobalConf.runtime.DISPLAY_GUI = false;
        GlobalConf.runtime.INPUT_ENABLED = false;
        GlobalConf.scene.CROSSHAIR_FOCUS = false;
        GlobalConf.scene.CROSSHAIR_HOME = false;
        GlobalConf.scene.CROSSHAIR_CLOSEST = false;
    }

    private void printInfo(){
        logger.info("Slave configuration modified with MPCDI settings");
        logger.info("   Resolution: " + xResolution + "x" + yResolution);
        logger.info("   Fov: " + cameraFov);
        logger.info("   Yaw/Pitch/Roll: " + yaw + "/" + pitch + "/" + roll);
    }

    public void loadAssets(AssetManager manager){
        if(pfm != null && Files.exists(pfm)){
            PFMTextureParameter param =new PFMTextureParameter();
            param.invert = true;
            param.internalFormat = GL20.GL_RGB;
            param.magFilter = TextureFilter.Linear;
            param.minFilter = TextureFilter.Linear;
            param.genMipMaps = false;
            manager.load(pfm.toString(), Texture.class, param);
        }
    }

}
