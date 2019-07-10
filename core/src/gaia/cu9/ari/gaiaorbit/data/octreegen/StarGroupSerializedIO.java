/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data.octreegen;

import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.data.group.IStarGroupDataProvider;
import gaia.cu9.ari.gaiaorbit.data.group.SerializedDataProvider;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

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
            List<StarBean> l = new ArrayList<StarBean>(sg.size());
            for (StarBean p : sg.data())
                l.add(p);

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
        @SuppressWarnings("unchecked")
        Array<StarBean> data = (Array<StarBean>) provider.loadData(in, 1.0);
        StarGroup sg = new StarGroup();
        sg.setData(data);

        Array<AbstractPositionEntity> l = new Array<AbstractPositionEntity>(1);
        l.add(sg);
        return l;
    }
}
