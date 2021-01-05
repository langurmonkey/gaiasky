/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.scenegraph.ParticleGroup.ParticleRecord;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

public class SerializedDataProvider extends AbstractStarGroupDataProvider {

    public SerializedDataProvider() {
        super();
    }

    public List<ParticleRecord> loadData(String file, double factor, boolean compat) {
        logger.info(I18n.bundle.format("notif.datafile", file));

        FileHandle f = GlobalConf.data.dataFileHandle(file);
        loadData(f.read(), factor, compat);
        logger.info(I18n.bundle.format("notif.nodeloader", list.size(), file));

        return list;
    }

    public List<ParticleRecord> loadData(InputStream is, double factor, boolean compat) {
        try {
            ObjectInputStream ois = new ObjectInputStream(is);
            @SuppressWarnings("unchecked")
            List<ParticleRecord> l = (List<ParticleRecord>) ois.readObject(); // cast is needed.
            ois.close();

            // Convert to Array, reconstruct index
            int n = l.size();
            initLists(n);

            for (int i = 0; i < n; i++) {
                ParticleRecord point = l.get(i);
                list.add(point);
            }

            return list;
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    @Override
    public List<ParticleRecord> loadDataMapped(String file, double factor, boolean compat) {
        // TODO Auto-generated method stub
        return null;
    }

}
