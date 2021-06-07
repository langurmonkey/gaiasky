/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen;

import gaiasky.data.group.BinaryDataProvider;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.particle.IParticleRecord;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and writes star groups.
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
     * @param list The list with the star group to write
     * @param out  The output stream to write to
     */
    public void writeParticles(List<SceneGraphNode> list, OutputStream out) {
        writeParticles(list, out, BinaryDataProvider.DEFAULT_OUTPUT_VERSION);
    }

    public void writeParticles(List<SceneGraphNode> list, OutputStream out, int version) {
        if (list.size() > 0) {
            StarGroup sg = (StarGroup) list.get(0);
            provider.writeData(sg.data(), out, version);
        }
    }

    /**
     * Reads a single star group from the given input stream.
     *
     * @param in The input stream to read the star group from
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
