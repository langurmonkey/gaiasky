package gaia.cu9.ari.gaiaorbit.data.group;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.I18n;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class TGASHYGDataProvider extends AbstractStarGroupDataProvider {
    private static boolean dumpToDisk = false;
    private static String format = "bin";

    public static void setDumpToDisk(boolean dump, String format) {
        TGASHYGDataProvider.dumpToDisk = dump;
        TGASHYGDataProvider.format = format;
    }

    STILDataProvider hip;
    TGASDataProvider tgas;

    public TGASHYGDataProvider() {
        super();
        hip = new STILDataProvider();
        tgas = new TGASDataProvider();
    }

    public void setParallaxErrorFactor(double parallaxErrorFactor) {
        super.setParallaxErrorFactorFaint(parallaxErrorFactor);
        if (tgas != null) {
            tgas.setParallaxErrorFactorFaint(parallaxErrorFactor);
            tgas.setParallaxErrorFactorBright(parallaxErrorFactor);
        }
    }

    @Override
    public Array<StarBean> loadData(String file) {
        return loadData(file, 1d);
    }

    @Override
    public Array<StarBean> loadData(String file, double factor) {
        Array<StarBean> tgasdata = tgas.loadData("data/tgas_final/tgas.csv");
        Array<StarBean> hipdata = (Array<StarBean>) hip.loadData("data/catalog/hipparcos/hip.vot");

        StarGroup aux = new StarGroup();
        ObjectIntMap<String> tgasindex = aux.generateIndex(tgasdata);
        ObjectIntMap<String> hygindex = aux.generateIndex(hipdata);

        // Merge everything, discarding hyg stars already in tgas
        // Contains removed HIP numbers
        Set<Integer> removed = new HashSet<Integer>();

        for (int i = 0; i < tgasdata.size; i++) {
            StarBean curr = tgasdata.get(i);
            int hip = (int) curr.data[StarBean.I_HIP];
            if (hip > 0 && hygindex.containsKey("HIP " + hip)) {
                removed.add((int) curr.data[StarBean.I_HIP]);
            }
        }

        // Add from hip to TGAS
        for (int i = 0; i < hipdata.size; i++) {
            StarBean curr = hipdata.get(i);
            Integer hip = (int) curr.data[StarBean.I_HIP];
            if (!removed.contains(hip)) {
                // Add to TGAS data
                tgasdata.add(curr);
            } else {
                // Use proper name
                if (tgasindex.containsKey("HIP " + i)) {
                    int oldidx = tgasindex.get("HIP " + i, -1);
                    tgasdata.get(oldidx).name = curr.name;
                }
            }
        }

        list = tgasdata;

        sphericalPositions = hip.sphericalPositions;
        sphericalPositions.putAll(tgas.sphericalPositions);

        colors = hip.colors;
        colors.putAll(tgas.colors);

        if (dumpToDisk) {
            dumpToDisk(list, "/tmp/tgashyg." + format, format);
        }

        logger.info(I18n.bundle.format("notif.nodeloader", list.size, file));

        return list;
    }

    @Override
    public Array<? extends ParticleBean> loadData(InputStream is, double factor) {
        return loadData("", factor);
    }

    @Override
    public Array<? extends ParticleBean> loadDataMapped(String file, double factor) {
        // TODO Auto-generated method stub
        return null;
    }

}
