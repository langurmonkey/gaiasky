package gaia.cu9.ari.gaiaorbit.data.octreegen;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.group.IStarGroupDataProvider;
import gaia.cu9.ari.gaiaorbit.data.group.SerializedDataProvider;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

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
