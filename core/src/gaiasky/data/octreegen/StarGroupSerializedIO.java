/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen;

import com.badlogic.gdx.utils.Array;
import gaiasky.data.group.IStarGroupDataProvider;
import gaiasky.data.group.SerializedDataProvider;
import gaiasky.scenegraph.AbstractPositionEntity;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.StarGroup.StarBean;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and writes star groups
 * 
 * @author Toni Sagrista
 *
 */
public class StarGroupSerializedIO implements IStarGroupIO {
    private static final Log logger = Logger.getLogger(StarGroupSerializedIO.class);

    IStarGroupDataProvider provider;

    public StarGroupSerializedIO() {
        provider = new SerializedDataProvider();
    }

    /**
     * Writes the list to the output stream. The list must contain a single star
     * vgroup.
     * 
     * @param list
     *            The list with the star vgroup to write
     * @param out
     *            The output stream to write to
     */
    public void writeParticles(Array<AbstractPositionEntity> list, OutputStream out) {
        if (list.size > 0) {
            StarGroup sg = (StarGroup) list.get(0);
            List<StarBean> l = new ArrayList<>(sg.size());
            for (ParticleBean p : sg.data()) {
                l.add((StarBean) p);
            }

            try {
                ObjectOutputStream oos = new ObjectOutputStream(out);
                oos.writeObject(l);
                oos.close();

            } catch (Exception e) {
                logger.error(e);
            }
        }
    }

    /**
     * Reads a single star vgroup from the given input stream.
     * 
     * @param in
     *            The input stream to read the star vgroup from
     * @return A list with a single star vgroup object
     */
    public Array<AbstractPositionEntity> readParticles(InputStream in) throws FileNotFoundException {
        Array<ParticleBean> data = provider.loadData(in, 1.0);
        StarGroup sg = new StarGroup();
        sg.setData(data);

        Array<AbstractPositionEntity> l = new Array<>(1);
        l.add(sg);
        return l;
    }
}
