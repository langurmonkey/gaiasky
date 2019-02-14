package gaia.cu9.ari.gaiaorbit.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import gaia.cu9.ari.gaiaorbit.data.group.IStarGroupDataProvider;
import gaia.cu9.ari.gaiaorbit.data.group.STILDataProvider;
import gaia.cu9.ari.gaiaorbit.data.octreegen.IStarGroupIO;
import gaia.cu9.ari.gaiaorbit.data.octreegen.MetadataBinaryIO;
import gaia.cu9.ari.gaiaorbit.data.octreegen.StarGroupBinaryIO;
import gaia.cu9.ari.gaiaorbit.data.octreegen.StarGroupSerializedIO;
import gaia.cu9.ari.gaiaorbit.data.octreegen.generator.IOctreeGenerator;
import gaia.cu9.ari.gaiaorbit.data.octreegen.generator.OctreeGeneratorMag;
import gaia.cu9.ari.gaiaorbit.data.octreegen.generator.OctreeGeneratorParams;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopNumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopConfInit;
import gaia.cu9.ari.gaiaorbit.interfce.ConsoleLogger;
import gaia.cu9.ari.gaiaorbit.interfce.MessageBean;
import gaia.cu9.ari.gaiaorbit.interfce.NotificationsInterface;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.parse.Parser;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Generates an octree of star groups. Each octant should have only one object,
 * a star group.
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
        jc.setProgramName("OctreeGeneratorTest");
        jc.parse(args);
        if (ogt.help) {
            jc.usage();
        } else {
            ogt.run();
        }
    }

    @Parameter(names = {"-l", "--loader"}, description = "Name of the star group loader class", required = true)
    private String loaderClass = null;

    @Parameter(names = {"-i", "--input"}, description = "Location of the input catalog", required = true)
    private String input = null;

    @Parameter(names = {"-o", "--output"}, description = "Output folder. Defaults to system temp")
    private String outFolder;

    @Parameter(names = "--maxpart", description = "Maximum number of objects in an octant")
    private int maxPart = 100000;

    @Parameter(names = "--serialized", description = "Use the java serialization method instead of the binary format to output the particle files")
    private boolean serialized = false;

    @Parameter(names = "--pllxerrfaint", description = "Parallax error factor for faint (gmag>=13.1) stars, acceptance criteria as a percentage of parallax error with respect to parallax, in [0..1]")
    private double pllxerrfaint = 0.125;

    @Parameter(names = "--pllxerrbright", description = "Parallax error factor for bright (gmag<13.1) stars, acceptance criteria as a percentage of parallax error with respect to parallax, in [0..1]")
    private double pllxerrbright = 0.25;

    @Parameter(names = "--pllxzeropoint", description = "Zero point value for the parallax in mas")
    private double pllxzeropoint = 0d;

    @Parameter(names = {"-c", "--magcorrections"}, description = "Flag to apply magnitude and color corrections for extinction and reddening")
    private boolean magCorrections = false;

    @Parameter(names = {"-p", "--postprocess"}, description = "Low object count nodes (<=100) will be merged with their parents if parents have less than 1000 objects. Avoids very large and mostly empty subtrees")
    private boolean postprocess = false;

    @Parameter(names = "--childcount", description = "If --postprocess is on, children nodes with less than --childcount objects and whose parents have less than --parentcount objects) will be merged with thier parents. Defaults to 100")
    private long childCount = 100;

    @Parameter(names = "--parentcount", description = "If --postprocess is on, children nodes with less than --childcount objects and whose parent has less than --parentcount objects will be merged with thier parents. Defaults to 1000")
    private long parentCount = 1000;

    @Parameter(names = {"-s", "--suncentre", "--suncenter"}, description = "Make the Sun the centre of the octree")
    private boolean sunCentre = false;

    @Parameter(names = "--nfiles", description = "Caps the number of data files to load. Defaults to unlimited")
    private int fileNumCap = -1;

    @Parameter(names = {"--hip", "--addhip"}, description = "Add the Hipparcos catalog additionally to the catalog provided by -l")
    private boolean addHip = false;

    @Parameter(names = "--xmatchfile", description = "Crossmatch file with source_id to hip, only if --hip is enabled")
    private String xmatchFile = null;

    @Parameter(names = "--geodistfile", description = "Use this file or directory to lookup distances. Argument is a file or directory with files of of <sourceid, dist[pc]>. If this argument is used, both --pllxerrfaint and --pllxerrbright are ignored")
    private String geodistFile = null;

    @Parameter(names = "--distcap", description = "Specifies a maximum distance in parsecs. Stars beyond this distance are not loaded")
    private double distcap = Long.MAX_VALUE;

    @Parameter(names = "--ruwe", description = "RUWE threshold value. All stars with a RUWE larger than this value will not be used. Must be used in conjunction with --ruwe-file. Also, if present, --pllxerrfaint and --pllxerrbright are ignored")
    private double ruwe = Double.NaN;

    @Parameter(names = "--ruwe-file", description = "Location of gzipped file containing the RUWE value for each source id")
    private String ruweFile = null;

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
            } else {
                if (!outFolder.endsWith("/"))
                    outFolder += "/";

                File outfolderFile = new File(outFolder);
                outfolderFile.mkdirs();
            }

            // Assets location
            String ASSETS_LOC = GlobalConf.ASSETS_LOC;

            Gdx.files = new LwjglFiles();

            // Add notification watch
            new ConsoleLogger();

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            // Initialize i18n
            I18n.initialize(new FileHandle(ASSETS_LOC + "i18n/gsbundle"));

            // Initialize configuration
            ConfInit.initialize(new DesktopConfInit(new FileInputStream(new File(ASSETS_LOC + "conf/global.properties")), new FileInputStream(new File(ASSETS_LOC + "data/dummyversion"))));

            generateOctree();

            // Save arguments and structure
            StringBuffer argstr = new StringBuffer();
            for (int i = 0; i < arguments.length; i++) {
                argstr.append(arguments[i]).append(" ");
            }
            try (PrintStream out = new PrintStream(new FileOutputStream(outFolder + "log"))) {
                out.print(argstr);
                out.println();
                out.println();
                for (MessageBean msg : NotificationsInterface.getHistorical()) {
                    out.println(msg.toString());
                }
            }
        } catch (FileNotFoundException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private OctreeNode generateOctree() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        long startMs = TimeUtils.millis();

        OctreeGeneratorParams ogp = new OctreeGeneratorParams(maxPart, sunCentre, postprocess, childCount, parentCount);
        //IOctreeGenerator og = new OctreeGeneratorPart(ogp);
        IOctreeGenerator og = new OctreeGeneratorMag(ogp);

        /* CATALOG */
        String fullLoaderClass = "gaia.cu9.ari.gaiaorbit.data.group." + loaderClass;
        IStarGroupDataProvider loader = (IStarGroupDataProvider) Class.forName(fullLoaderClass).newInstance();
        loader.setParallaxErrorFactorFaint(pllxerrfaint);
        loader.setParallaxErrorFactorBright(pllxerrbright);
        loader.setParallaxZeroPoint(pllxzeropoint);
        loader.setFileNumberCap(fileNumCap);
        loader.setMagCorrections(magCorrections);
        loader.setDistanceCap(distcap);
        loader.setGeoDistancesFile(geodistFile);
        loader.setRUWEFile(ruweFile);
        loader.setRUWECap(ruwe);
        long[] cpm = loader.getCountsPerMag();

        Map<Long, Integer> xmatchTable = null;
        if (addHip && xmatchFile != null && !xmatchFile.isEmpty()) {
            // Load xmatchTable
            xmatchTable = readXmatchTable(xmatchFile);
            if(!xmatchTable.isEmpty()){
                // IDs which must be loaded regardless (we need them to update x-matched HIP stars)
                loader.setMustLoadIds(new HashSet<>(xmatchTable.keySet()));
            }
        }

        /* LOAD CATALOG */
        @SuppressWarnings("unchecked")
        Array<StarBean> listGaia = (Array<StarBean>) loader.loadData(input);
        Array<StarBean> list;

        if (addHip) {
            /* HIPPARCOS */
            STILDataProvider stil = new STILDataProvider();

            // All hip stars for which we have a Gaia star, bypass plx >= 0 condition in STILDataProvider
            Set<Long> mustLoad = new HashSet<>();
            xmatchTable.values().stream().forEach(hip -> mustLoad.add(new Long(hip)));
            stil.setMustLoadIds(mustLoad);

            Array<StarBean> listHip = (Array<StarBean>) stil.loadData("data/catalog/hipparcos/hip.vot");
            long[] cpmhip = stil.getCountsPerMag();
            combineCpm(cpm, cpmhip);
            Map<Integer, StarBean> hipMap = new HashMap<>();
            for (StarBean star : listHip) {
                hipMap.put(star.hip(), star);
            }

            /* Check x-match file */
            int hipnum = listHip.size;
            int starhits = 0;
            for (StarBean gaiaStar : listGaia) {
                // Check if star is also in HYG catalog
                if (xmatchTable == null || !xmatchTable.containsKey(gaiaStar.id)) {
                    // No hit, add to main list
                    listHip.add(gaiaStar);
                } else {
                    // Update hipStar using gaiaStar data
                    int hipId = xmatchTable.get(gaiaStar.id);
                    StarBean hipStar = hipMap.get(hipId);
                    hipStar.id = gaiaStar.id;
                    hipStar.data[StarBean.I_X] = gaiaStar.x();
                    hipStar.data[StarBean.I_Y] = gaiaStar.y();
                    hipStar.data[StarBean.I_Z] = gaiaStar.z();
                    hipStar.data[StarBean.I_PMX] = gaiaStar.pmx();
                    hipStar.data[StarBean.I_PMY] = gaiaStar.pmy();
                    hipStar.data[StarBean.I_PMZ] = gaiaStar.pmz();
                    hipStar.data[StarBean.I_MUALPHA] = gaiaStar.mualpha();
                    hipStar.data[StarBean.I_MUDELTA] = gaiaStar.mudelta();
                    hipStar.data[StarBean.I_RADVEL] = gaiaStar.radvel();
                    hipStar.data[StarBean.I_APPMAG] = gaiaStar.appmag();
                    hipStar.data[StarBean.I_ABSMAG] = gaiaStar.absmag();
                    hipStar.data[StarBean.I_COL] = gaiaStar.col();
                    hipStar.data[StarBean.I_SIZE] = gaiaStar.size();
                    starhits++;
                }
            }
            logger.info(starhits + " of " + hipnum + " HIP stars' data updated due to being matched to a Gaia star");

            // Main list is listHip
            list = listHip;

            // Free some memory
            listGaia.clear();
        } else {
            list = listGaia;
        }

        long loadingMs = TimeUtils.millis();
        double loadingSecs = ((loadingMs - startMs) / 1000.0);
        logger.info("TIME STATS: Data loaded in " + loadingSecs + " seconds");

        logger.info("Generating octree with " + list.size + " actual stars");

        OctreeNode octree = og.generateOctree(list);

        System.out.println(octree.toString(true));

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
        writeParticlesToFiles(particleWriter, octree);

        long writingMs = TimeUtils.millis();
        double writingSecs = (writingMs - generatingMs) / 1000.0;
        double totalSecs = loadingSecs + generatingSecs + writingSecs;

        int[][] stats = octree.stats();
        NumberFormat formatter = new DecimalFormat("##########0.0000");
        if (cpm != null) {
            logger.info("=================");
            logger.info("STAR COUNTS STATS");
            logger.info("=================");
            for (int level = 0; level < cpm.length; level++) {
                logger.info("Magnitude " + level + ": " + cpm[level] + " stars (" + formatter.format((double) cpm[level] * 100d / (double) list.size) + "%)");
            }
            logger.info();
        }

        logger.info("============");
        logger.info("OCTREE STATS");
        logger.info("============");
        logger.info("Octants: " + octree.numNodes());
        logger.info("Particles: " + list.size);
        logger.info("Depth: " + octree.getMaxDepth());
        int level = 0;
        for (int[] levelinfo : stats) {
            logger.info("   Level " + level + ": " + levelinfo[0] + " octants, " + levelinfo[1] + " stars (" + formatter.format((double) levelinfo[1] * 100d / (double) list.size) + "%)");
            level++;
        }
        logger.info();
        logger.info("================");
        logger.info("FINAL TIME STATS");
        logger.info("================");
        logger.info("Loading: " + loadingSecs + " seconds");
        logger.info("Generating: " + generatingSecs + " seconds");
        logger.info("Writing: " + writingSecs + " seconds");
        logger.info("Total: " + totalSecs + " seconds");

        return octree;
    }

    private long[] combineCpm(long[] cpm1, long[] cpm2) {
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
        // Write current
        if (current.ownObjects > 0) {
            File particles = new File(outFolder + "/particles/", "particles_" + String.format("%06d", current.pageId) + ".bin");
            logger.info("Writing " + current.ownObjects + " particles of node " + current.pageId + " to " + particles.getAbsolutePath());
            particleWriter.writeParticles(current.objects, new BufferedOutputStream(new FileOutputStream(particles)));
        }

        // Write each child
        if (current.childrenCount > 0)
            for (OctreeNode child : current.children) {
                if (child != null)
                    writeParticlesToFiles(particleWriter, child);
            }
    }

    private Map<Long, Integer> readXmatchTable(String xmatchFile) {
        File xm = new File(xmatchFile);
        if (xm.exists()) {
            try {
                Map<Long, Integer> map = new HashMap<>();
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(xm)));
                // Skip header line
                br.readLine();
                String line;
                while ((line = br.readLine()) != null) {
                    String[] tokens = line.split(",");
                    Long sourceId = Parser.parseLong(tokens[0]);
                    Integer hip = Parser.parseInt(tokens[1]);
                    map.put(sourceId, hip);
                }
                br.close();
                logger.error("Cross-match table read: " + xmatchFile);
                return map;
            } catch (Exception e) {
                logger.error(e);
            }
        } else {
            logger.error("Cross-match file '" + xmatchFile + "' does not exist");
        }
        return null;
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
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            writer.println("name, x[km], y[km], z[km], absmag, appmag, r, g, b");
            Vector3d gal = new Vector3d();
            for (StarBean star : data) {
                float[] col = colors.get(star.id);
                gal.set(star.x(), star.y(), star.z()).scl(Constants.U_TO_KM);
                //gal.mul(Coordinates.equatorialToGalactic());
                writer.println(star.name + sep + gal.x + sep + gal.y + sep + gal.z + sep + star.absmag() + sep + star.appmag() + sep + col[0] + sep + col[1] + sep + col[2]);
            }
            writer.close();
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
