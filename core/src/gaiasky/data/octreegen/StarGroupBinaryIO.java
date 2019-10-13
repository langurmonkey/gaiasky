/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen;

import com.badlogic.gdx.utils.Array;
import gaiasky.data.group.BinaryDataProvider;
import gaiasky.scenegraph.AbstractPositionEntity;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.StarGroup.StarBean;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Loads and writes star groups
 * 
 * @author Toni Sagrista
 *
 */
public class StarGroupBinaryIO implements IStarGroupIO {

    BinaryDataProvider provider;

    public StarGroupBinaryIO() {
        provider = new BinaryDataProvider();
    }

    /**
     * Writes the list to the output stream. The list must contain a single star
     * group.
     * 
     * @param list
     *            The list with the star group to write
     * @param out
     *            The output stream to write to
     */
    public void writeParticles(Array<AbstractPositionEntity> list, OutputStream out) {
        if (list.size > 0) {
            StarGroup sg = (StarGroup) list.get(0);
            provider.writeData(sg.data(), out);
        }
    }

    /**
     * Reads a single star group from the given input stream.
     * 
     * @param in
     *            The input stream to read the star group from
     * @return A list with a single star group object
     */
    public Array<AbstractPositionEntity> readParticles(InputStream in) {
        @SuppressWarnings("unchecked")
        Array<StarBean> data = (Array<StarBean>) provider.loadData(in, 1.0);
        StarGroup sg = new StarGroup();
        sg.setData(data);

        Array<AbstractPositionEntity> l = new Array<>(1);
        l.add(sg);
        return l;
    }
}
