/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.i18n.I18n;
import gaiasky.util.Settings;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

public class SerializedDataProvider extends AbstractStarGroupDataProvider {

    public SerializedDataProvider() {
        super();
    }

    public List<IParticleRecord> loadData(String file, double factor) {
        logger.info(I18n.txt("notif.datafile", file));

        FileHandle f = Settings.settings.data.dataFileHandle(file);
        loadData(f.read(), factor);
        logger.info(I18n.txt("notif.nodeloader", list.size(), file));

        return list;
    }

    public List<IParticleRecord> loadData(InputStream is, double factor) {
        try {
            ObjectInputStream ois = new ObjectInputStream(is);
            @SuppressWarnings("unchecked")
            List<IParticleRecord> l = (List<IParticleRecord>) ois.readObject(); // cast is needed.
            ois.close();

            // Convert to Array, reconstruct index
            int n = l.size();
            initLists(n);

            list.addAll(l);

            return list;
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    @Override
    public List<IParticleRecord> loadDataMapped(String file, double factor) {
        // TODO Auto-generated method stub
        return null;
    }

}
