package gaia.cu9.ari.gaiaorbit.data.orbit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;
import com.badlogic.gdx.files.FileHandle;

import gaia.cu9.ari.gaiaorbit.assets.OrbitDataLoader.OrbitDataLoaderParameter;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopNumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopConfInit;
import gaia.cu9.ari.gaiaorbit.desktop.util.LogWriter;
import gaia.cu9.ari.gaiaorbit.util.ConfInit;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.math.MathManager;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Samples an orbit for a particular Body.
 * 
 * @author Toni Sagrista
 *
 */
public class OrbitSamplerDataProvider implements IOrbitDataProvider {
    private static boolean writeData = false;
    private static final String writeDataPath = "/tmp/";
    PolylineData data;

    public static void main(String[] args) {
        try {
            // Assets location
            String ASSETS_LOC = GlobalConf.ASSETS_LOC;
            
            // Logger
            new LogWriter();

            Gdx.files = new LwjglFiles();

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            ConfInit.initialize(new DesktopConfInit(new FileInputStream(new File(ASSETS_LOC + "conf/global.properties")), new FileInputStream(new File(ASSETS_LOC + "data/dummyversion"))));

            I18n.initialize(new FileHandle(ASSETS_LOC + "i18n/gsbundle"));

            // Initialize math manager
            MathManager.initialize();

            OrbitSamplerDataProvider.writeData = true;
            OrbitSamplerDataProvider me = new OrbitSamplerDataProvider();

            Date now = new Date();
            String[] bodies = new String[] { "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Moon", "Pluto" };
            float[] periods = new float[] { 87.9691f, 224.701f, 365.256363f, 686.971f, 4332.59f, 10759.22f, 30799.095f, 60190.03f, 27.321682f, 90560f };
            for (int i = 0; i < bodies.length; i++) {

                String b = bodies[i];
                float period = periods[i];
                OrbitDataLoaderParameter param = new OrbitDataLoaderParameter(me.getClass(), b, now, true, period, 500);
                me.load(null, param);

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void load(String file, OrbitDataLoaderParameter parameter) {
        // Sample using VSOP
        // If num samples is not defined, we use 300 samples per year of period
        int numSamples = parameter.numSamples > 0 ? parameter.numSamples : (int) (300 * parameter.orbitalPeriod / 365);
        numSamples = Math.max(100, Math.min(2000, numSamples));
        data = new PolylineData();
        String bodyDesc = parameter.name;
        Instant d = Instant.ofEpochMilli(parameter.ini.getTime());
        double last = 0, accum = 0;
        Vector3d ecl = new Vector3d();

        // Milliseconds of this orbit in one revolution
        long orbitalMs = (long) parameter.orbitalPeriod * 24 * 60 * 60 * 1000;
        long stepMs = orbitalMs / numSamples;

        // Load vsop orbit data
        for (int i = 0; i <= numSamples; i++) {
            AstroUtils.getEclipticCoordinates(bodyDesc, d, ecl, true);

            if (last == 0) {
                last = Math.toDegrees(ecl.x);
            }

            accum += Math.toDegrees(ecl.x) - last;
            last = Math.toDegrees(ecl.x);

            if (accum > 355) {
                break;
            }

            Coordinates.sphericalToCartesian(ecl, ecl);
            ecl.mul(Coordinates.eclToEq()).scl(1);
            data.x.add(ecl.x);
            data.y.add(ecl.y);
            data.z.add(ecl.z);
            d = Instant.ofEpochMilli(d.toEpochMilli() + stepMs);
            data.time.add(Instant.ofEpochMilli(d.toEpochMilli()));
        }

        // Close the circle
        data.x.add(data.x.get(0));
        data.y.add(data.y.get(0));
        data.z.add(data.z.get(0));
        d = Instant.ofEpochMilli(d.toEpochMilli() + stepMs);
        data.time.add(Instant.ofEpochMilli(d.toEpochMilli()));

        if (writeData) {
            try {
                OrbitDataWriter.writeOrbitData(writeDataPath + "orb." + bodyDesc.toString().toUpperCase() + ".dat", data);
            } catch (IOException e) {
                Logger.getLogger(this.getClass()).error(e);
            }
        }

        Logger.getLogger(this.getClass()).info(I18n.bundle.format("notif.orbitdataof.loaded", parameter.name, data.getNumPoints()));

    }

    @Override
    public void load(String file, OrbitDataLoaderParameter parameter, boolean newmethod) {
        load(file, parameter);
    }

    public PolylineData getData() {
        return data;
    }

}
