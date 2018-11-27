package gaia.cu9.ari.gaiaorbit.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.data.stars.HYGBinaryLoader;
import gaia.cu9.ari.gaiaorbit.data.stars.STILCatalogLoader;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopConfInit;
import gaia.cu9.ari.gaiaorbit.interfce.ConsoleLogger;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.Star;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Pair;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.gaia.GaiaAttitudeServer;
import gaia.cu9.ari.gaiaorbit.util.gaia.Satellite;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Quaterniond;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import net.jafama.FastMath;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Loads a catalog and selects the stars that are observed during a certain
 * period of time.
 *
 * @author tsagrista
 */
public class GaiaCatalogFilter {

    Array<AbstractPositionEntity> catalog;
    long MAX_OVERLAP_TIME;
    double BAM_2;
    double angleEdgeRad;
    Matrix4d trf;

    NumberFormat nf;

    String catal = "tycho";

    //    String catal = "hygxyz";

    public GaiaCatalogFilter() {
        super();
    }

    public void initialize() throws Exception {
        // Init Gdx files
        Gdx.files = new LwjglFiles();

        // Init log writer
        new ConsoleLogger();

        // Initialize date format
        DateFormatFactory.initialize(new DesktopDateFormatFactory());

        // Init global conf
        DesktopConfInit confInit = new DesktopConfInit();
        confInit.initGlobalConf();

        // Initialize i18n
        I18n.initialize();

        // Load attitude
        GlobalConf.data.REAL_GAIA_ATTITUDE = true;
        GlobalConf.data.LIMIT_MAG_LOAD = 20f;
        GaiaAttitudeServer.instance = new GaiaAttitudeServer("data/attitudexml/", "OPS_RSLS_0022916_rsls_nsl_gareq1_afterFirstSpinPhaseOptimization.2.xml");

        // Load catalog
        if (catal.equals("hyg")) {
            HYGBinaryLoader loader = new HYGBinaryLoader();
            loader.initialize(new String[] { "data/hygxyz.bin" });
            catalog = loader.loadData();
        } else if (catal.equals("tycho")) {
            STILCatalogLoader loader = new STILCatalogLoader();
            loader.initialize(new String[] { "/home/tsagrista/Workspaces/objectserver/data/tycho.vot.gz" });
            catalog = loader.loadData();
            // Add Sun
            CelestialBody sun = new Star(new Vector3d(0, 0, 0), 4.83f, 4.83f, 0.656f, "Sol", (int) System.currentTimeMillis());
            sun.initialize();
            catalog.add(sun);
        }

        // Format
        nf = new DecimalFormat("##00");

        // Initialize constants and so
        float h = (float) Satellite.FOV_AC_ACTIVE;
        float w = (float) Satellite.FOV_AL;
        angleEdgeRad = (Math.sqrt(h * h + w * w) * Math.PI / 180D);
        // In ms
        MAX_OVERLAP_TIME = (long) (angleEdgeRad / (Satellite.SCANRATE * (Math.PI / (3600D * 180D)))) * 1000;

        BAM_2 = Satellite.BASICANGLE_DEGREE / 2D;

        trf = new Matrix4d();
    }

    /**
     * Produces the filtered catalogs for each day between the selected dates.
     *
     * @param iniY The initial year.
     * @param iniM The initial month. Starts at 1 for January.
     * @param iniD The initial day of the month. Starts at 1.
     * @param endY The end year.
     * @param endM The end month. Starts at 1 for January.
     * @param endD The end day of the month. Starts at 1.
     */
    public void filterCatalog(int iniY, int iniM, int iniD, int endY, int endM, int endD) {
        // Some constants
        final long msDay = 24 * 60 * 60 * 1000;
        // 5 min overlap
        final long overlap = 5 * 60 * 1000;

        GregorianCalendar iniCal = new GregorianCalendar(iniY, iniM - 1, iniD);
        GregorianCalendar endCal = new GregorianCalendar(endY, endM - 1, endD);
        Date ini = iniCal.getTime();
        Date end = endCal.getTime();

        Date current = new Date(ini.getTime());

        Set<AbstractPositionEntity> out = new HashSet<AbstractPositionEntity>(100000);

        while (current.getTime() < end.getTime()) {
            out.clear();
            long dayStart = current.getTime();
            // Process day
            for (long t = dayStart - overlap; t < dayStart + msDay + overlap * 2; t += MAX_OVERLAP_TIME) {
                Pair<Vector3d, Vector3d> dirs = getDirections(new Date(t));

                for (AbstractPositionEntity p : catalog) {
                    double poslen = p.pos.len();
                    boolean observed = FastMath.acos(p.pos.dot(dirs.getFirst()) / poslen) < angleEdgeRad || FastMath.acos(p.pos.dot(dirs.getSecond()) / poslen) < angleEdgeRad;
                    // Sun should always be there because of the scene graph
                    observed = observed || p.name.equalsIgnoreCase("sol") || p.name.equalsIgnoreCase("sun");
                    if (observed) {
                        out.add(p);
                    }
                }
            }

            // Write out to file
            Calendar cal = Calendar.getInstance();
            cal.setTime(current);
            String bin = System.getProperty("java.io.tmpdir") + File.separator + catal + "-" + nf.format(cal.get(Calendar.YEAR)) + nf.format(cal.get(Calendar.MONTH) + 1) + nf.format(cal.get(Calendar.DAY_OF_MONTH)) + ".bin";

            try {
                writeToBinary(out, bin);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Advance a day
            current.setTime(current.getTime() + msDay);

        }

    }

    public void writeToBinary(Set<AbstractPositionEntity> catalog, String bin) throws IOException {
        File binFile = new File(bin);
        binFile.mkdirs();
        if (binFile.exists()) {
            binFile.delete();
            binFile.createNewFile();
        }
        // Create an output stream to the file.
        FileOutputStream file_output = new FileOutputStream(binFile);
        // Wrap the FileOutputStream with a DataOutputStream
        DataOutputStream data_out = new DataOutputStream(file_output);

        // Size of stars
        data_out.writeInt(catalog.size());
        for (AbstractPositionEntity ape : catalog) {
            CelestialBody s = (CelestialBody) ape;
            // name_length, name, appmag, absmag, colorbv, ra, dec, dist
            data_out.writeInt(s.name.length());
            data_out.writeChars(s.name);
            data_out.writeFloat(s.appmag);
            data_out.writeFloat(s.absmag);
            data_out.writeFloat(s.colorbv);
            data_out.writeFloat((float) s.posSph.x);
            data_out.writeFloat((float) s.posSph.y);
            data_out.writeFloat((float) s.pos.len());
            data_out.writeInt(new Long(s.id).intValue());
        }
        file_output.close();
        System.out.println(new Date() + " - " + catalog.size() + " particles written to binary file " + bin);
    }

    public Pair<Vector3d, Vector3d> getDirections(Date d) {
        trf.idt();
        Quaterniond quat = GaiaAttitudeServer.instance.getAttitude(d).getQuaternion();

        trf.rotate(quat).rotate(0, 0, 1, 180);
        Vector3d dir1 = new Vector3d().set(0, 0, 1).rotate(BAM_2, 0, 1, 0).mul(trf).nor();
        Vector3d dir2 = new Vector3d().set(0, 0, 1).rotate(-BAM_2, 0, 1, 0).mul(trf).nor();
        return new Pair<Vector3d, Vector3d>(dir1, dir2);
    }

    public static void main(String[] args) throws Exception {
        GaiaCatalogFilter gcf = new GaiaCatalogFilter();
        gcf.initialize();
        gcf.filterCatalog(2016, 4, 18, 2017, 12, 30);
    }
}
