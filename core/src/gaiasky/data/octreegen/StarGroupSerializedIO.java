/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen;

import gaiasky.data.group.IStarGroupDataProvider;
import gaiasky.data.group.SerializedDataProvider;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and writes star groups using java's default {@link ObjectOutputStream} and
 * {@link ObjectInputStream} through {@link SerializedDataProvider}.
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
     *            The list with the star group to write.
     * @param out
     *            The output stream to write to.
     */
    public void writeParticles(List<SceneGraphNode> list, OutputStream out) {
        writeParticles(list, out, 1);
    }

    public void writeParticles(List<SceneGraphNode> list, OutputStream out, int version) {
        if (list.size() > 0) {
            StarGroup sg = (StarGroup) list.get(0);
            List<IParticleRecord> l = new ArrayList<>(sg.size());
            for (IParticleRecord p : sg.data()) {
                l.add(p);
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
     * Reads a single star group from the given input stream.
     * 
     * @param in
     *            The input stream to read the star group from
     * @return A list with a single star group object
     */
    public List<SceneGraphNode> readParticles(InputStream in) {
        List<IParticleRecord> data = provider.loadData(in, 1.0);
        StarGroup sg = new StarGroup();
        sg.setData(data);

        List<SceneGraphNode> l = new ArrayList<>(1);
        l.add(sg);
        return l;
    }
}
