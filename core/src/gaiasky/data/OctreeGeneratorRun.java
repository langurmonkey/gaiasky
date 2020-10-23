/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import gaiasky.data.group.AbstractStarGroupDataProvider;
import gaiasky.data.group.IStarGroupDataProvider;
import gaiasky.data.group.STILDataProvider;
import gaiasky.data.octreegen.IStarGroupIO;
import gaiasky.data.octreegen.MetadataBinaryIO;
import gaiasky.data.octreegen.StarGroupBinaryIO;
import gaiasky.data.octreegen.StarGroupSerializedIO;
import gaiasky.data.octreegen.generator.IOctreeGenerator;
import gaiasky.data.octreegen.generator.OctreeGeneratorMag;
import gaiasky.data.octreegen.generator.OctreeGeneratorParams;
import gaiasky.data.util.HipNames;
import gaiasky.desktop.format.DesktopDateFormatFactory;
import gaiasky.desktop.format.DesktopNumberFormatFactory;
import gaiasky.desktop.util.DesktopConfInit;
import gaiasky.interafce.ConsoleLogger;
import gaiasky.interafce.MessageBean;
import gaiasky.interafce.NotificationsInterface;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.StarGroup.StarBean;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import gaiasky.util.tree.OctreeNode;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Generates an octree of star groups.
 *
 * @author tsagrista
 */
public class OctreeGeneratorRun {
    private static final Log logger = Logger.getLogger(OctreeGeneratorRun.class);

    private static JCommander jc;
    private static String[] arguments;

    public static void main(String[] args) {
        arguments = args;
        OctreeGeneratorRun ogt = new OctreeGeneratorRun();
        jc = JCommander.newBuilder().addObject(ogt).build();
        jc.setProgramName("OctreeGeneratorRun");
        jc.parse(args);
        if (ogt.help) {
            jc.usage();
        } else {
            ogt.run();
        }
    }

    @Parameter(names = {"-l", "--loader"}, description = "Name of the star group loader class")
    private String loaderClass = null;

    @Parameter(names = {"-i", "--input"}, description = "Location of the input catalog")
    private String input = null;

    @Parameter(names = {"-o", "--output"}, description = "Output folder. Defaults to system temp")
    private String outFolder;

    @Parameter(names = "--maxpart", description = "Maximum number of objects in an octant")
    private int maxPart = 100000;

    @Parameter(names = "--pllxerrfaint", description = "Parallax error factor for faint (gmag>=13.1) stars, acceptance criteria as a percentage of parallax error with respect to parallax, in [0..1]")
    private double pllxerrfaint = 0.125;

    @Parameter(names = "--pllxerrbright", description = "Parallax error factor for bright (gmag<13.1) stars, acceptance criteria as a percentage of parallax error with respect to parallax, in [0..1]")
    private double pllxerrbright = 0.25;

    @Parameter(names = "--pllxzeropoint", description = "Zero point value for the parallax in mas")
    private double pllxzeropoint = 0d;

    @Parameter(names = {"-c", "--nomagcorrections"}, description = "Flag to skip magnitude and color corrections for extinction and reddening")
    private boolean magCorrections = true;

    @Parameter(names = {"-p", "--postprocess"}, description = "Low object count nodes (<=100) will be merged with their parents if parents have less than 1000 objects. Avoids very large and mostly empty subtrees")
    private boolean postprocess = false;

    @Parameter(names = "--childcount", description = "If --postprocess is on, children nodes with less than --childcount objects and whose parents have less than --parentcount objects) will be merged with thier parents. Defaults to 100")
    private long childCount = 100;

    @Parameter(names = "--parentcount", description = "If --postprocess is on, children nodes with less than --childcount objects and whose parent has less than --parentcount objects will be merged with thier parents. Defaults to 1000")
    private long parentCount = 1000;

    @Parameter(names = {"-s", "--suncentre", "--suncenter"}, description = "Make the Sun the centre of the octree")
    private boolean sunCentre = false;

    @Parameter(names = "--nfiles", description = "Data file number cap. Defaults to unlimited")
    private int fileNumCap = -1;

    @Parameter(names = {"--hip"}, description = "Location (absolute or relative to data folder \"data/...\") of the Hipparcos catalog to add on top of the catalog provided by -l")
    private String hip = null;

    @Parameter(names = "--hip-names", description = "Directory containing HIP names files (Name_To_HIP.dat, Var_To_HIP.dat, etc.), in case the HIP catalog does not already provide them")
    private String hipNamesDir = null;

    @Parameter(names = "--xmatchfile", description = "Crossmatch file between Gaia and HIP, containing source_id to hip data, only if --hip is enabled")
    private String xmatchFile = null;

    @Parameter(names = "--distcap", description = "Maximum distance in parsecs. Stars beyond this distance are not loaded")
    private double distcap = Long.MAX_VALUE;

    @Parameter(names = "--ruwe", description = "RUWE threshold value. All stars with a RUWE larger than this value will not be used. Also, if present, --pllxerrfaint and --pllxerrbright are ignored")
    private double ruwe = Double.NaN;

    @Parameter(names = "--columns", description = "Column name list separated by commas, in order of appearance, if loading using the CSVCatalogDataProvider (see AbstractStarGroupDataProvider.ColId)")
    private String columns = null;

    @Parameter(names = "--additional", description = "Comma-separated list of files or folders with (optionally gzipped) csv files containing additional columns (matched by name) of main catalog. The file can be gzipped and must contain a Gaia sourceid column in the first position")
    private String additionalFiles = null;

    @Parameter(names = "--compat-mode", description = "Use compatibility mode format (DR1/DR2), where the files have tycho ids")
    private boolean compatibilityMode = false;

    @Parameter(names = "--serialized", description = "Use java serialization instead of the binary format to output particle files")
    private boolean serialized = false;

    @Parameter(names = {"-h", "--help"}, help = true)
    private boolean help = false;

    protected Map<Long, float[]> colors;

    public OctreeGeneratorRun() {
        super();
        colors = new HashMap<>();
    }

    public void run() {
        try {
            if (outFolder == null) {
                outFolder = System.getProperty("java.io.tmpdir");
            }
            if (!outFolder.endsWith("/"))
                outFolder += "/";

            Path outPath = Path.of(outFolder);
            Files.createDirectories(outPath);

            // Assets location
            String ASSETS_LOC = GlobalConf.ASSETS_LOC;

            Gdx.files = new Lwjgl3Files();

            // Add notification watch
            new ConsoleLogger();

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            // Initialize i18n
            I18n.initialize(Path.of(ASSETS_LOC, "i18n/gsbundle"));

            // Initialize configuration
            Path dummyv = Path.of(ASSETS_LOC, "data/dummyversion");
            if (!Files.exists(dummyv)) {
                dummyv = Path.of(ASSETS_LOC, "dummyversion");
            }
            ConfInit.initialize(new DesktopConfInit(new FileInputStream(Path.of(ASSETS_LOC, "conf/global.properties").toFile()), new FileInputStream(dummyv.toFile())));

            OctreeNode root = generateOctree();

            if (root != null) {
                // Save arguments and structure
                StringBuffer argStr = new StringBuffer();
                for (int i = 0; i < arguments.length; i++) {
                    argStr.append(arguments[i]).append(" ");
                }
                try (PrintStream out = new PrintStream(new FileOutputStream(outFolder + "log"))) {
                    out.print(argStr);
                    out.println();
                    out.println();
                    for (MessageBean msg : NotificationsInterface.getHistorical()) {
                        out.println(msg.toString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private OctreeNode generateOctree() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        long startMs = TimeUtils.millis();

        OctreeGeneratorParams ogp = new OctreeGeneratorParams(maxPart, sunCentre, postprocess, childCount, parentCount);
        IOctreeGenerator og = new OctreeGeneratorMag(ogp);

        List<ParticleBean> listLoader = null, list = null;
        Map<Long, Integer> xmatchTable = null;
        long[] countsPerMagGaia = null;

        if (loaderClass != null) {
            /* CATALOG */
            String fullLoaderClass = "gaiasky.data.group." + loaderClass;
            IStarGroupDataProvider loader = (IStarGroupDataProvider) Class.forName(fullLoaderClass).getDeclaredConstructor().newInstance();
            loader.setColumns(columns);
            loader.setParallaxErrorFactorFaint(pllxerrfaint);
            loader.setParallaxErrorFactorBright(pllxerrbright);
            loader.setParallaxZeroPoint(pllxzeropoint);
            loader.setFileNumberCap(fileNumCap);
            loader.setMagCorrections(magCorrections);
            loader.setDistanceCap(distcap);
            loader.setAdditionalFiles(additionalFiles);
            loader.setRUWECap(ruwe);
            countsPerMagGaia = loader.getCountsPerMag();

            if (hip != null && xmatchFile != null && !xmatchFile.isEmpty()) {
                // Load xmatchTable
                xmatchTable = readXmatchTable(xmatchFile);
                if (!xmatchTable.isEmpty()) {
                    // IDs which must be loaded regardless (we need them to update x-matched HIP stars)
                    loader.setMustLoadIds(new HashSet<>(xmatchTable.keySet()));
                }
            }

            /* LOAD CATALOG */
            listLoader = loader.loadData(input);
        }

        if (hip != null) {
            /* HIPPARCOS */
            STILDataProvider stil = new STILDataProvider();

            // All hip stars for which we have a Gaia star, bypass plx >= 0 condition in STILDataProvider
            if (xmatchTable != null && !xmatchTable.isEmpty()) {
                Set<Long> mustLoad = new HashSet<>();
                for (int hipNumber : xmatchTable.values()) {
                    mustLoad.add(Long.valueOf(hipNumber));
                }
                stil.setMustLoadIds(mustLoad);
            }

            List<ParticleBean> listHip = stil.loadData(hip);

            // Update HIP names using external source, if needed
            if (hipNamesDir != null) {
                HipNames hipNames = new HipNames();
                hipNames.load(Paths.get(hipNamesDir));

                Map<Integer, Array<String>> hn = hipNames.getHipNames();
                for (ParticleBean pb : listHip) {
                    StarBean star = (StarBean) pb;
                    if (hn.containsKey(star.hip())) {
                        Array<String> names = hn.get(star.hip());
                        for (String name : names)
                            star.addName(name);
                    }
                }
            }

            // Combine counts per magnitude
            long[] countsPerMagHip = stil.getCountsPerMag();
            combineCountsPerMag(countsPerMagGaia, countsPerMagHip);

            // Create HIP map
            Map<Integer, StarBean> hipMap = new HashMap<>();
            for (ParticleBean star : listHip) {
                hipMap.put(((StarBean) star).hip(), (StarBean) star);
            }

            // Check x-match file
            int hipnum = listHip.size();
            int starhits = 0;
            int notFoundHipStars = 0;

            Vector3d aux1 = new Vector3d();
            Vector3d aux2 = new Vector3d();
            if (listLoader != null) {
                for (ParticleBean pb : listLoader) {
                    StarBean gaiaStar = (StarBean) pb;
                    // Check if star is also in HIP catalog
                    if (xmatchTable == null || !xmatchTable.containsKey(gaiaStar.id)) {
                        // No hit, add to main list
                        listHip.add(gaiaStar);
                    } else {
                        // Update hipStar using gaiaStar data, only when:
                        // TODO gaia.ruwe small enough (if present) and gaia.pllx_err <= hip.pllx_err
                        int hipId = xmatchTable.get(gaiaStar.id);
                        if (hipMap.containsKey(hipId)) {
                            // Hip Star
                            StarBean hipStar = hipMap.get(hipId);

                            // Check parallax errors
                            Double gaiaPllxErr = gaiaStar.getExtra("pllx_err");
                            Double hipPllxErr = hipStar.getExtra("e_plx");

                            if (gaiaPllxErr <= hipPllxErr) {
                                // SIZE
                                double size = gaiaStar.size();
                                // POSITION
                                double x = gaiaStar.x(), y = gaiaStar.y(), z = gaiaStar.z();
                                aux1.set(x, y, z);
                                boolean negativeGaiaDistance = Math.abs(aux1.len() - AbstractStarGroupDataProvider.NEGATIVE_DIST) < 1e-10;
                                if (negativeGaiaDistance) {
                                    // Negative distance in Gaia star!
                                    // Use Gaia position, HIP distance and name(s)

                                    // Fetch Gaia RA/DEC
                                    Coordinates.cartesianToSpherical(aux1, aux2);
                                    double gaiaRA = aux2.x;
                                    double gaiaDEC = aux2.y;

                                    // Fetch HIP distance
                                    aux1.set(hipStar.x(), hipStar.y(), hipStar.z());
                                    Coordinates.cartesianToSpherical(aux1, aux2);
                                    double hipDIST = aux2.z;

                                    // Compute new cartesian position
                                    aux1.set(gaiaRA, gaiaDEC, hipDIST);
                                    Coordinates.sphericalToCartesian(aux1, aux2);
                                    x = aux2.x;
                                    y = aux2.y;
                                    z = aux2.z;

                                    size = hipStar.size();
                                }

                                hipStar.id = gaiaStar.id;
                                hipStar.data[StarBean.I_X] = x;
                                hipStar.data[StarBean.I_Y] = y;
                                hipStar.data[StarBean.I_Z] = z;
                                hipStar.data[StarBean.I_PMX] = gaiaStar.pmx();
                                hipStar.data[StarBean.I_PMY] = gaiaStar.pmy();
                                hipStar.data[StarBean.I_PMZ] = gaiaStar.pmz();
                                hipStar.data[StarBean.I_MUALPHA] = gaiaStar.mualpha();
                                hipStar.data[StarBean.I_MUDELTA] = gaiaStar.mudelta();
                                hipStar.data[StarBean.I_RADVEL] = gaiaStar.radvel();
                                hipStar.data[StarBean.I_APPMAG] = gaiaStar.appmag();
                                hipStar.data[StarBean.I_ABSMAG] = gaiaStar.absmag();
                                hipStar.data[StarBean.I_COL] = gaiaStar.col();
                                hipStar.data[StarBean.I_SIZE] = size;
                                hipStar.addNames(gaiaStar.names);
                                starhits++;
                            }
                        } else {
                            notFoundHipStars++;
                        }
                    }
                }
                logger.info(starhits + " of " + hipnum + " HIP stars' data updated due to being matched to a Gaia star (" + notFoundHipStars + " not found - negative parallax?)");
                // Free up some memory
                listLoader.clear();
            }
            // Main list is listHip
            list = listHip;

        } else {
            list = listLoader;
        }

        if (list == null || list.isEmpty()) {
            logger.info("No stars were loaded, please check out the parameters");
            return null;
        }

        long loadingMs = TimeUtils.millis();
        double loadingSecs = ((loadingMs - startMs) / 1000.0);
        logger.info("TIME STATS: Data loaded in " + loadingSecs + " seconds");

        logger.info("Generating octree with " + list.size() + " actual stars");

        OctreeNode octree = og.generateOctree(list);

        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        out.println(octree.toString(true));

        long generatingMs = TimeUtils.millis();
        double generatingSecs = ((generatingMs - loadingMs) / 1000.0);
        logger.info("TIME STATS: Octree generated in " + generatingSecs + " seconds");

        /** NUMBERS **/
        logger.info("Octree generated with " + octree.numNodes() + " octants and " + octree.nObjects + " particles");
        logger.info(og.getDiscarded() + " particles have been discarded due to density");

        /** CLEAN CURRENT OUT DIR **/
        File metadataFile = new File(outFolder, "metadata.bin");
        delete(metadataFile);
        File particlesFolder = new File(outFolder, "particles/");
        delete(particlesFolder);

        /** WRITE METADATA **/
        metadataFile.createNewFile();

        logger.info("Writing metadata (" + octree.numNodes() + " nodes): " + metadataFile.getAbsolutePath());

        MetadataBinaryIO metadataWriter = new MetadataBinaryIO();
        metadataWriter.writeMetadata(octree, new FileOutputStream(metadataFile));

        /** WRITE PARTICLES **/
        IStarGroupIO particleWriter = serialized ? new StarGroupSerializedIO() : new StarGroupBinaryIO();
        particlesFolder.mkdirs();
        writeParticlesToFiles(particleWriter, octree, compatibilityMode);

        long writingMs = TimeUtils.millis();
        double writingSecs = (writingMs - generatingMs) / 1000.0;
        double totalSecs = loadingSecs + generatingSecs + writingSecs;

        int[][] stats = octree.stats();
        NumberFormat formatter = new DecimalFormat("##########0.0000");
        if (countsPerMagGaia != null) {
            logger.info("=================");
            logger.info("STAR COUNTS STATS");
            logger.info("=================");
            for (int level = 0; level < countsPerMagGaia.length; level++) {
                logger.info("Magnitude " + level + ": " + countsPerMagGaia[level] + " stars (" + formatter.format((double) countsPerMagGaia[level] * 100d / (double) list.size()) + "%)");
            }
            logger.info();
        }

        logger.info("============");
        logger.info("OCTREE STATS");
        logger.info("============");
        logger.info("Octants: " + octree.numNodes());
        logger.info("Particles: " + list.size());
        logger.info("Depth: " + octree.getMaxDepth());
        int level = 0;
        for (int[] levelinfo : stats) {
            logger.info("   Level " + level + ": " + levelinfo[0] + " octants, " + levelinfo[1] + " stars (" + formatter.format((double) levelinfo[1] * 100d / (double) list.size()) + "%)");
            level++;
        }

        logger.info();
        logger.info("================");
        logger.info("FINAL TIME STATS");
        logger.info("================");
        logger.info("Loading: " + loadingSecs + " secs (" + formatTimeSecs((long) loadingSecs) + ")");
        logger.info("Generating: " + generatingSecs + " secs (" + formatTimeSecs((long) generatingSecs) + ")");
        logger.info("Writing: " + writingSecs + " secs (" + formatTimeSecs((long) writingSecs) + ")");
        logger.info("Total: " + totalSecs + " secs (" + formatTimeSecs((long) totalSecs) + ")");

        return octree;
    }

    private String formatTimeSecs(long secs) {
        long hours = secs / 3600l;
        long minutes = (secs % 3600l) / 60l;
        long seconds = secs % 60l;

        return String.format(Locale.getDefault(), "%dh %dm %ds", hours, minutes, seconds);
    }

    private long[] combineCountsPerMag(long[] cpm1, long[] cpm2) {
        if (cpm1 == null)
            return cpm2;
        else if (cpm2 == null)
            return cpm1;
        else {
            for (int i = 0; i < cpm1.length; i++)
                cpm1[i] += cpm2[i];
        }
        return cpm1;
    }

    private void writeParticlesToFiles(IStarGroupIO particleWriter, OctreeNode current) throws IOException {
        writeParticlesToFiles(particleWriter, current, true);
    }

    private void writeParticlesToFiles(IStarGroupIO particleWriter, OctreeNode current, boolean compat) throws IOException {
        // Write current
        if (current.ownObjects > 0) {
            File particles = new File(outFolder + "/particles/", "particles_" + String.format("%06d", current.pageId) + ".bin");
            logger.info("Writing " + current.ownObjects + " particles of node " + current.pageId + " to " + particles.getAbsolutePath());
            particleWriter.writeParticles(current.objects, new BufferedOutputStream(new FileOutputStream(particles)), compat);
        }

        // Write each child
        if (current.childrenCount > 0)
            for (OctreeNode child : current.children) {
                if (child != null)
                    writeParticlesToFiles(particleWriter, child, compat);
            }
    }

    private Map<Long, Integer> readXmatchTable(String xmatchFile) {
        Map<Long, Integer> map = new HashMap<>();
        File xm = new File(xmatchFile);
        if (xm.exists()) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(xm)));
                // Assume no header
                String line;
                while ((line = br.readLine()) != null) {
                    String[] tokens = line.split(",");
                    Long sourceId = Parser.parseLong(tokens[0]);
                    Integer hip = Parser.parseInt(tokens[1]);
                    map.put(sourceId, hip);
                }
                br.close();
                logger.error("Cross-match table read with " + map.size() + " entries: " + xmatchFile);
            } catch (Exception e) {
                logger.error(e);
            }
        } else {
            logger.error("Cross-match file '" + xmatchFile + "' does not exist");
        }
        return map;
    }

    private void delete(File element) {
        if (element.isDirectory()) {
            for (File sub : element.listFiles()) {
                delete(sub);
            }
        }
        element.delete();
    }

    protected void dumpToDiskCsv(Array<StarBean> data, String filename) {
        String sep = ", ";
        try {
            PrintWriter writer = new PrintWriter(filename, StandardCharsets.UTF_8);
            writer.println("name, x[km], y[km], z[km], absmag, appmag, r, g, b");
            Vector3d gal = new Vector3d();
            for (StarBean star : data) {
                float[] col = colors.get(star.id);
                gal.set(star.x(), star.y(), star.z()).scl(Constants.U_TO_KM);
                //gal.mul(Coordinates.equatorialToGalactic());
                writer.println(star.namesConcat() + sep + gal.x + sep + gal.y + sep + gal.z + sep + star.absmag() + sep + star.appmag() + sep + col[0] + sep + col[1] + sep + col[2]);
            }
            writer.close();
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
