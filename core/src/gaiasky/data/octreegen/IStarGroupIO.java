/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen;

import gaiasky.scenegraph.SceneGraphNode;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface IStarGroupIO {

    void writeParticles(List<SceneGraphNode> list, OutputStream out);
    void writeParticles(List<SceneGraphNode> list, OutputStream out, boolean compat);

    List<SceneGraphNode> readParticles(InputStream in) throws FileNotFoundException;
}
