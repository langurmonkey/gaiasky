package gaia.cu9.ari.gaiaorbit.data;

import java.io.FileNotFoundException;

import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;

public interface ISceneGraphLoader {

    public Array<? extends SceneGraphNode> loadData() throws FileNotFoundException;

    public void setName(String name);

    public void setDescription(String description);

    public void initialize(String[] files) throws RuntimeException;

}
