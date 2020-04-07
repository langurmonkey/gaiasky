/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data;

import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.SceneGraphNode;
import uk.ac.starlink.util.DataSource;

import java.io.FileNotFoundException;
import java.io.InputStream;

public interface ISceneGraphLoader {

    Array<? extends SceneGraphNode> loadData() throws FileNotFoundException;

    void setName(String name);

    void setDescription(String description);

    void initialize(String[] files) throws RuntimeException;

    void initialize(DataSource ds);

}
