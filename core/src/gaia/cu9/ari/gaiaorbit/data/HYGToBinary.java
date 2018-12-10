package gaia.cu9.ari.gaiaorbit.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.data.stars.HYGBinaryLoader;
import gaia.cu9.ari.gaiaorbit.data.stars.HYGCSVLoader;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopNumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopConfInit;
import gaia.cu9.ari.gaiaorbit.interfce.ConsoleLogger;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.Star;
import gaia.cu9.ari.gaiaorbit.util.ConfInit;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;

import java.io.*;

/**
 * Small utility to convert a the HYG CSV catalog to binary in the following
 * format: - 32 bits (int) with the number of stars, starNum repeat the
 * following starNum times (for each star) - 32 bits (int) - The the length of
 * the name, or nameLength - 16 bits * nameLength (chars) - The name of the star
 * - 32 bits (float) - appmag - 32 bits (float) - absmag - 32 bits (float) -
 * colorbv - 32 bits (float) - ra - 32 bits (float) - dec - 32 bits (float) -
 * distance
 *
 * @author Toni Sagrista
 */
public class HYGToBinary {
    private static final Log logger = Logger.getLogger(HYGToBinary.class);

    static String fileIn = "/home/tsagrista/git/gaiasky/android/assets-bak/data/hygxyz.csv";
    static String fileInPm = "/home/tsagrista/git/gaiasky/android/assets-bak/data/hip_pm.csv";
    static String fileOut = "/home/tsagrista/git/gaiasky/android/assets-bak/data/hygxyz.bin";

    public static void main(String[] args) {
        HYGToBinary hyg = new HYGToBinary();

        try {
            // Assets location
            String ASSETS_LOC = GlobalConf.ASSETS_LOC;

            // Init logger
            new ConsoleLogger();

            // Init files
            Gdx.files = new Lwjgl3Files();

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            ConfInit.initialize(new DesktopConfInit(new FileInputStream(new File(ASSETS_LOC + "conf/global.properties")), new FileInputStream(new File(ASSETS_LOC + "data/dummyversion"))));

            I18n.initialize(new FileHandle(ASSETS_LOC + "i18n/gsbundle"));

            GlobalConf.data.LIMIT_MAG_LOAD = 20;

            //hyg.compareCSVtoBinary(fileIn, fileOut);

            hyg.convertToBinary(fileIn, fileInPm, fileOut);

        } catch (Exception e) {
            logger.error(e);
        }

    }

    public void compareCSVtoBinary(String csv, String bin) {

        try {
            HYGCSVLoader csvLoader = new HYGCSVLoader();
            HYGBinaryLoader binLoader = new HYGBinaryLoader();

            csvLoader.files = new String[] { csv };
            Array<AbstractPositionEntity> csvStars = csvLoader.loadData();
            binLoader.files = new String[] { bin };
            Array<AbstractPositionEntity> binStars = binLoader.loadData();

            if (csvStars.size != binStars.size) {
                System.err.println("Different sizes");
            }

            int different = 0;
            for (int i = 0; i < csvStars.size; i++) {
                CelestialBody csvs = (CelestialBody) csvStars.get(i);
                CelestialBody bins = (CelestialBody) binStars.get(i);

                if (!equals(csvs, bins) && csvs.name.equals("Betelgeuse")) {
                    logger.info("Different stars: " + csvs + " // " + bins);
                    different++;
                }
            }

            logger.info("Found " + different + " different stars");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void convertToBinary(String csv, String pm, String bin) {
        HYGCSVLoader cat = new HYGCSVLoader();
        try {
            cat.files = new String[] { csv };
            cat.setPmFile(pm);
            Array<AbstractPositionEntity> stars = cat.loadData();

            // Write to binary
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
            data_out.writeInt(stars.size);
            for (AbstractPositionEntity ape : stars) {
                Particle s = (Particle) ape;
                // name_length, name, appmag, absmag, colorbv, ra[deg], dec[deg], dist[u], mualpha[mas/yr], mudelta[mas/yr], radvel[km/s], id, hip
                data_out.writeInt(s.name.length());
                data_out.writeChars(s.name);
                data_out.writeFloat(s.appmag);
                data_out.writeFloat(s.absmag);
                data_out.writeFloat(s.colorbv);
                data_out.writeFloat(s.posSph.x);
                data_out.writeFloat(s.posSph.y);
                data_out.writeFloat((float) s.pos.len());
                data_out.writeFloat(s.pmSph.x);
                data_out.writeFloat(s.pmSph.y);
                data_out.writeFloat(s.pmSph.z);
                data_out.writeInt(new Long(s.id).intValue());
                data_out.writeInt(((Star) s).hip);
            }
            data_out.close();
            file_output.close();
            System.out.println(stars.size + " stars written to binary file " + bin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean equals(CelestialBody s1, CelestialBody s2) {
        return s1.id == s2.id && s1.posSph.x == s2.posSph.x && s1.posSph.y == s2.posSph.y && s1.pos.x == s2.pos.x && s1.pos.y == s2.pos.y && s1.pos.z == s2.pos.z && s1.absmag == s2.absmag;
    }
}