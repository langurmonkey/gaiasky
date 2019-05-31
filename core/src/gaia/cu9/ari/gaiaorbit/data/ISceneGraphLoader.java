/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data;

import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;

import java.io.FileNotFoundException;

public interface ISceneGraphLoader {

    Array<? extends SceneGraphNode> loadData() throws FileNotFoundException;

    void setName(String name);

    void setDescription(String description);

    void initialize(String[] files) throws RuntimeException;

}
