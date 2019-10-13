/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.StarGroup.StarBean;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

public class SerializedDataProvider extends AbstractStarGroupDataProvider {

    public SerializedDataProvider() {
    }

    public Array<StarBean> loadData(String file) {
        return loadData(file, 1d);
    }

    public Array<StarBean> loadData(String file, double factor) {
        logger.info(I18n.bundle.format("notif.datafile", file));

        FileHandle f = GlobalConf.data.dataFileHandle(file);
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
