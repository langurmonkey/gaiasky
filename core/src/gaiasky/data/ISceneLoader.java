/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data;

import com.artemis.World;
import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.SceneGraphNode;
import uk.ac.starlink.util.DataSource;

import java.io.FileNotFoundException;
import java.util.Map;

/**
 * Defines the interface for scene loaders.
 */
public interface ISceneLoader {

    void loadData(World world) throws FileNotFoundException;

    void setName(String name);

    void setDescription(String description);

    void setParams(Map<String, Object> params);

    void initialize(String[] files) throws RuntimeException;

    void initialize(DataSource ds);

}
