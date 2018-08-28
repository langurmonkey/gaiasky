package gaia.cu9.ari.gaiaorbit.data.group;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;

public class SerializedDataProvider extends AbstractStarGroupDataProvider {

    public SerializedDataProvider() {
    }

    public Array<StarBean> loadData(String file) {
        return loadData(file, 1d);
    }

    public Array<StarBean> loadData(String file, double factor) {
        logger.info(I18n.bundle.format("notif.datafile", file));

        FileHandle f = Gdx.files.internal(file);
        loadData(f.read(), factor);
        logger.info(I18n.bundle.format("notif.nodeloader", list.size, file));

        return list;
    }

    public Array<StarBean> loadData(InputStream is, double factor) {
        try {
            ObjectInputStream ois = new ObjectInputStream(is);
            @SuppressWarnings("unchecked")
            List<StarBean> l = (List<StarBean>) ois.readObject(); // cast is needed.
            ois.close();

            // Convert to Array, reconstruct index
            int n = l.size();
            initLists(n);

            for (int i = 0; i < n; i++) {
                StarBean point = l.get(i);
                list.add(point);
            }

            return list;
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    @Override
    public Array<? extends ParticleBean> loadDataMapped(String file, double factor) {
        // TODO Auto-generated method stub
        return null;
    }

}
