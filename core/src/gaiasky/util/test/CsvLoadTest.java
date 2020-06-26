package gaiasky.util.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import gaiasky.data.group.CsvCatalogDataProvider;
import gaiasky.desktop.format.DesktopDateFormatFactory;
import gaiasky.desktop.format.DesktopNumberFormatFactory;
import gaiasky.desktop.util.DesktopConfInit;
import gaiasky.interafce.ConsoleLogger;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.StarGroup.StarBean;
import gaiasky.util.ConfInit;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.NumberFormatFactory;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CsvLoadTest {
    private static final Log logger = Logger.getLogger(CsvLoadTest.class);

    public static void main(String[] args) throws Exception {

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


        // Create loader
        CsvCatalogDataProvider loader = new CsvCatalogDataProvider();
        loader.setColumns("sourceid,ra,dec,pllx,ra_err,dec_err,pllx_err,pmra,pmdec,radvel,gmag,bpmag,rpmag,ruwe,ref_epoch");
        loader.setMagCorrections(true);
        loader.setParallelBufferSize(500000);
        loader.setParallaxErrorFactorBright(155);
        loader.setParallaxErrorFactorFaint(155);
        loader.setAdaptiveParallax(true);

        // Parallel 500000
        logger.info("Running parallel (buffer = 500000)");
        long startP500k = System.currentTimeMillis();
        loader.setParallel(true);
        List<ParticleBean> p1 = loader.loadData("/home/tsagrista/Downloads/edr2/");
        long endP500k = System.currentTimeMillis();

        // Serial
        logger.info("Running serial");
        long startS = System.currentTimeMillis();
        loader.setParallel(false);
        List<ParticleBean> s1 = loader.loadData("/home/tsagrista/Downloads/edr2/");
        long endS = System.currentTimeMillis();

        // Check consistency
        AtomicInteger errors = new AtomicInteger(0);
        logger.info("Checking consistency...");
        if (p1.size() != s1.size()) {
            logger.warn("Lists have different size! " + p1.size() + " != " + s1.size());
        }
        Map<Long, StarBean> s1map = new HashMap<Long, StarBean>();
        for(ParticleBean pb : s1)
            s1map.put(((StarBean)pb).id, (StarBean) pb);

        Object lock = new Object();
        p1.parallelStream().forEach(one -> {
            StarBean sone = (StarBean) one;
            if(s1map.containsKey(sone.id)){
                StarBean stwo = s1map.get(sone.id);
                if (!equal(sone.data, stwo.data)) {
                    errors.incrementAndGet();
                }
            } else{
                synchronized (lock) {
                    logger.warn("Missing source id: " + sone.id);
                    errors.incrementAndGet();
                }
            }
        });
        logger.info("Consistency errors: " + errors.get());


        logger.info("Parallel (500K):  " + ((endP500k - startP500k) / 1000d) + " seconds");
        logger.info("Serial:          " + ((endS - startS) / 1000d) + " seconds");


    }

    private static boolean equal(double[] one, double[] two) {
        if (one == null && two != null || one != null && two == null)
            return false;
        if (one.length != two.length)
            return false;

        for (int i = 0; i < one.length; i++) {
            boolean n1 = Double.isNaN(one[i]);
            boolean n2 = Double.isNaN(two[i]);
            if(n1 && !n2 || !n1 && n2)
                return false;
            else if(n1 && n2)
                continue;
            else
                if(one[i] != two[i])
                    return false;

        }
        return true;
    }
}
