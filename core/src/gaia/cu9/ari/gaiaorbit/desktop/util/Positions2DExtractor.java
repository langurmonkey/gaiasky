package gaia.cu9.ari.gaiaorbit.desktop.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;
import com.badlogic.gdx.files.FileHandle;

import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopNumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.ConfInit;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Nature;
import gaia.cu9.ari.gaiaorbit.util.coord.MoonAACoordinates;
import gaia.cu9.ari.gaiaorbit.util.coord.vsop87.AbstractVSOP87;
import gaia.cu9.ari.gaiaorbit.util.coord.vsop87.VSOP87;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

public class Positions2DExtractor {

    public static void main(String[] args) throws IOException {
        Positions2DExtractor p2d = new Positions2DExtractor();
        new LogWriter();

        I18n.initialize(new FileHandle("/home/tsagrista/git/gaiasandbox/android/assets/i18n/gsbundle"));

        Gdx.files = new LwjglFiles();
        try {
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());
            DateFormatFactory.initialize(new DesktopDateFormatFactory());
            ConfInit.initialize(new DesktopConfInit());
        } catch (IOException e) {
            Logger.getLogger(Positions2DExtractor.class).error(e);
        } catch (Exception e) {
            Logger.getLogger(Positions2DExtractor.class).error(e);
        }

        p2d.process();
    }

    int year = 2016;
    int month = 1;
    int day = 1;

    String filePath = "/tmp/sun-earth-moon.txt";

    // Solar mass
    double Mo = 1.989e30;
    // Earth mass
    double Me = 5.972e24;
    // Moon mass
    double Mm = 7.34767309e22;

    int steps = 1000;
    long MS_IN_YEAR = (long) Nature.Y_TO_S * 1000l;

    public void process() throws IOException {

        MoonAACoordinates moonCoord = new MoonAACoordinates();
        AbstractVSOP87 earthCoord = (AbstractVSOP87) VSOP87.instance.getVOSP87("Earth");

        FileWriter fw = new FileWriter(filePath);
        BufferedWriter bw = new BufferedWriter(fw);

        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 0, 0, 0);
        Date ini = c.getTime();
        long inims = ini.getTime();

        long dtms = MS_IN_YEAR / steps;
        Vector3d e = new Vector3d(), m = new Vector3d();

        bw.write("precomputed=True");
        bw.newLine();
        bw.newLine();

        bw.write("# nbodies");
        bw.newLine();
        bw.write("3");
        bw.newLine();

        bw.write("# number of time steps");
        bw.newLine();
        bw.write(Integer.toString(steps));
        bw.newLine();

        bw.write("# base date");
        bw.newLine();
        bw.write(ini.toString());
        bw.newLine();

        bw.write("# base date [ms since January 1, 1970, 00:00:00 GMT]");
        bw.newLine();
        bw.write(Long.toString(ini.getTime()));
        bw.newLine();

        bw.write("#t[ms] x[km] y[km] mass[kg]");
        bw.newLine();
        for (long t = inims; t < inims + MS_IN_YEAR; t += dtms) {
            Instant now = Instant.ofEpochMilli(t);
            // Sun
            bw.write(t + " 0.0 0.0 " + Mo);
            bw.newLine();

            // Earth
            earthCoord.getEclipticCartesianCoordinates(now, e);
            e.scl(Constants.U_TO_KM);
            bw.write(t + " " + e.x + " " + e.z + " " + Me);
            bw.newLine();

            // Moon
            moonCoord.getEclipticCartesianCoordinates(now, m);
            m.scl(Constants.U_TO_KM);
            m.add(e);
            bw.write(t + " " + m.x + " " + m.z + " " + Mm);
            bw.newLine();

        }

        bw.close();
    }

}
