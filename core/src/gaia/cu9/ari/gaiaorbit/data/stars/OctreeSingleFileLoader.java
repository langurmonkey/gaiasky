package gaia.cu9.ari.gaiaorbit.data.stars;

import java.io.FileNotFoundException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.ISceneGraphLoader;
import gaia.cu9.ari.gaiaorbit.data.octreegen.MetadataBinaryIO;
import gaia.cu9.ari.gaiaorbit.data.octreegen.ParticleDataBinaryIO;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.scenegraph.Star;
import gaia.cu9.ari.gaiaorbit.scenegraph.octreewrapper.AbstractOctreeWrapper;
import gaia.cu9.ari.gaiaorbit.scenegraph.octreewrapper.OctreeWrapper;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.tree.LoadStatus;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

/**
 * Implements the loading from a single particle file. This version loads single
 * stars using the
 * {@link gaia.cu9.ari.gaiaorbit.data.octreegen.ParticleDataBinaryIO}
 * 
 * @author tsagrista
 */
public class OctreeSingleFileLoader implements ISceneGraphLoader {

    String metadata, particles;
    String name, description;

    @Override
    public Array<? extends SceneGraphNode> loadData() throws FileNotFoundException {
        // Logger.info(this.getClass().getSimpleName(),
        // I18n.bundle.format("notif.limitmag",
        // GlobalConf.data.LIMIT_MAG_LOAD));

        Logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.loading", metadata));

        MetadataBinaryIO metadataReader = new MetadataBinaryIO();
        OctreeNode root = metadataReader.readMetadata(Gdx.files.internal(metadata).read(), LoadStatus.LOADED);

        Logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.nodeloader", root.numNodes(), metadata));
        Logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.loading", particles));

        ParticleDataBinaryIO particleReader = new ParticleDataBinaryIO();
        Array<AbstractPositionEntity> particleList = particleReader.readParticles(Gdx.files.internal(particles).read());

        Logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.nodeloader", particleList.size, particles));

        /**
         * CREATE OCTREE WRAPPER WITH ROOT NODE
         */
        AbstractOctreeWrapper octreeWrapper = new OctreeWrapper("Universe", root);

        Array<SceneGraphNode> result = new Array<SceneGraphNode>(1);
        result.add(octreeWrapper);

        /**
         * ADD STARS
         */
        // Update model
        for (SceneGraphNode sgn : particleList) {
            Star s = (Star) sgn;

            OctreeNode octant = metadataReader.nodesMap.get(s.octantId).getFirst();
            octant.add(s);
            s.octant = octant;

            // Add objects to octree wrapper node
            octreeWrapper.add(s, octant);
        }

        Logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.catalog.init", particleList.size));

        return result;
    }

    @Override
    public void initialize(String[] files) throws RuntimeException {
        if (files == null || files.length < 2) {
            throw new RuntimeException("Error loading octree files: " + files.length);
        }
        particles = files[0];
        metadata = files[1];
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
