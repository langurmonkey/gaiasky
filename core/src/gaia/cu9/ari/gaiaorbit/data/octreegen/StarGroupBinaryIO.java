package gaia.cu9.ari.gaiaorbit.data.octreegen;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.group.BinaryDataProvider;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;

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
