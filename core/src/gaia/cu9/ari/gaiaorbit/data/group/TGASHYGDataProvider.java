package gaia.cu9.ari.gaiaorbit.data.group;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;

import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;

public class TGASHYGDataProvider extends AbstractStarGroupDataProvider {
    private static boolean dumpToDisk = false;
    private static String format = "bin";

    public static void setDumpToDisk(boolean dump, String format) {
        TGASHYGDataProvider.dumpToDisk = dump;
        TGASHYGDataProvider.format = format;
    }

    HYGDataProvider hyg;
    TGASDataProvider tgas;

    public TGASHYGDataProvider() {
        super();
        hyg = new HYGDataProvider();
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
        Array<StarBean> hygdata = hyg.loadData("data/hyg/hygxyz.bin");

        StarGroup aux = new StarGroup();
        ObjectIntMap<String> tgasindex = aux.generateIndex(tgasdata);
        ObjectIntMap<String> hygindex = aux.generateIndex(hygdata);

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
        for (int i = 0; i < hygdata.size; i++) {
            StarBean curr = hygdata.get(i);
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

        sphericalPositions = hyg.sphericalPositions;
        sphericalPositions.putAll(tgas.sphericalPositions);

        colors = hyg.colors;
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
