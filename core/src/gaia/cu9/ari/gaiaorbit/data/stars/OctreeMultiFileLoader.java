package gaia.cu9.ari.gaiaorbit.data.stars;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.data.StreamingOctreeLoader;
import gaia.cu9.ari.gaiaorbit.data.octreegen.MetadataBinaryIO;
import gaia.cu9.ari.gaiaorbit.data.octreegen.ParticleDataBinaryIO;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.scenegraph.octreewrapper.AbstractOctreeWrapper;
import gaia.cu9.ari.gaiaorbit.scenegraph.octreewrapper.OctreeWrapper;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.tree.LoadStatus;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

/**
 * Implements the loading and streaming of octree nodes from files. This version
 * loads single stars using the
 * {@link gaia.cu9.ari.gaiaorbit.data.octreegen.ParticleDataBinaryIO}
 * 
 * @author tsagrista
 */
public class OctreeMultiFileLoader extends StreamingOctreeLoader {
    /**
     * Data will be pre-loaded at startup down to this octree depth.
     */
    private static final int PRELOAD_DEPTH = 3;

    /** Binary particle reader **/
    private ParticleDataBinaryIO particleReader;

    public OctreeMultiFileLoader() {
        super();
        StreamingOctreeLoader.instance = this;

        particleReader = new ParticleDataBinaryIO();

    }

    @Override
    protected AbstractOctreeWrapper loadOctreeData() {
        /**
         * LOAD METADATA
         */

        Logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.loading", metadata));

        MetadataBinaryIO metadataReader = new MetadataBinaryIO();
        OctreeNode root = metadataReader.readMetadata(Gdx.files.internal(metadata).read());

        Logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.nodeloader", root.numNodes(), metadata));
        Logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.loading", particles));

        /**
         * CREATE OCTREE WRAPPER WITH ROOT NODE
         */
        AbstractOctreeWrapper octreeWrapper = new OctreeWrapper("Universe", root);

        /**
         * LOAD LOD LEVELS - LOAD PARTICLE DATA
         */

        try {
            int depthLevel = Math.min(OctreeNode.maxDepth, PRELOAD_DEPTH);
            loadLod(depthLevel, octreeWrapper);
            flushLoadedIds();

            OctreeNode solOctant = root.getBestOctant(new Vector3d());
            boolean found = false;
            while (!found) {
                loadOctant(solOctant, octreeWrapper, false);
                SceneGraphNode sol = octreeWrapper.getNode("Sol");
                if (sol == null) {
                    solOctant = solOctant.parent;
                } else {
                    found = true;
                }
            }

        } catch (IOException e) {
            Logger.error(e);
        }
        return octreeWrapper;
    }

    public boolean loadOctant(final OctreeNode octant, final AbstractOctreeWrapper octreeWrapper, boolean fullinit) throws IOException {
        FileHandle octantFile = Gdx.files.internal(particles + "particles_" + String.format("%06d", octant.pageId) + ".bin");
        if (!octantFile.exists() || octantFile.isDirectory()) {
            return false;
        }
        Array<AbstractPositionEntity> data = particleReader.readParticles(octantFile.read());
        synchronized (octant) {
            for (AbstractPositionEntity star : data) {
                if (fullinit)
                    star.doneLoading(null);

                star.octant = octant;
                // Add objects to octree wrapper node
                octreeWrapper.add(star, octant);
                // Aux info
                if (GaiaSky.instance != null && GaiaSky.instance.sg != null)
                    GaiaSky.instance.sg.addNodeAuxiliaryInfo(star);
            }
            nLoadedStars += data.size;
            octant.objects = data;

            // Put it at the end of the queue
            touch(octant);

            octant.setStatus(LoadStatus.LOADED);

            addLoadedInfo(octant.pageId, octant.countObjects());
        }
        return true;

    }

}
