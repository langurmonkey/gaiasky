/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

public class SerializedDataProvider extends AbstractStarGroupDataProvider {
    static {
        logger = Logger.getLogger(SerializedDataProvider.class);
    }

    public SerializedDataProvider() {
        super();
    }

    public List<IParticleRecord> loadData(String file, double factor) {
        logger.info(I18n.msg("notif.datafile", file));

        FileHandle f = Settings.settings.data.dataFileHandle(file);
        loadData(f.read(), factor);
        logger.info(I18n.msg("notif.nodeloader", list.size(), file));

        return list;
    }

    public List<IParticleRecord> loadData(InputStream is, double factor) {
        try {
            ObjectInputStream ois = new ObjectInputStream(is);
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
        return null;
    }

}
