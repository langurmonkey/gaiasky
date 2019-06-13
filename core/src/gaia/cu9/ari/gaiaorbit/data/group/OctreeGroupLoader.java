/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data.group;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.data.StreamingOctreeLoader;
import gaia.cu9.ari.gaiaorbit.data.octreegen.MetadataBinaryIO;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.octreewrapper.AbstractOctreeWrapper;
import gaia.cu9.ari.gaiaorbit.scenegraph.octreewrapper.OctreeWrapper;
import gaia.cu9.ari.gaiaorbit.util.CatalogInfo;
import gaia.cu9.ari.gaiaorbit.util.CatalogInfo.CatalogInfoType;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.tree.LoadStatus;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

import java.io.IOException;

/**
 * Implements the loading and streaming of octree nodes from files. This version
 * loads star groups using the
 * {@link gaia.cu9.ari.gaiaorbit.data.group.SerializedDataProvider}
 * 
 * @author tsagrista
 */
public class OctreeGroupLoader extends StreamingOctreeLoader {
    private static final Log logger = Logger.getLogger(OctreeGroupLoader.class);

    /**
     * Whether to use the binary file format. If false, we use the java
     * serialization method
     **/
    private Boolean binary = true;

    /** Binary particle reader **/
    private IParticleGroupDataProvider particleReader;

    public OctreeGroupLoader() {
        instance = this;

        particleReader = binary ? new BinaryDataProvider() : new SerializedDataProvider();

    }

    @Override
    protected AbstractOctreeWrapper loadOctreeData() {
        /**
         * LOAD METADATA
         */

        logger.info(I18n.bundle.format("notif.loading", metadata));

        MetadataBinaryIO metadataReader = new MetadataBinaryIO();
        OctreeNode root = metadataReader.readMetadataMapped(metadata);

        if (root != null) {
            logger.info(I18n.bundle.format("notif.nodeloader", root.numNodes(), metadata));
            logger.info(I18n.bundle.format("notif.loading", particles));

            /**
             * CREATE OCTREE WRAPPER WITH ROOT NODE - particle group is by default
             * parallel, so we never use OctreeWrapperConcurrent
             */
            AbstractOctreeWrapper octreeWrapper = new OctreeWrapper("Universe", root);
            // Catalog info
            String name = this.name != null ? this.name : "LOD data";
            String description = this.description != null ? this.description : "Octree-based LOD dataset";
            CatalogInfo ci = new CatalogInfo(name, description, null, CatalogInfoType.LOD, octreeWrapper);
            EventManager.instance.post(Events.CATALOG_ADD, ci, false);

            /**
             * LOAD LOD LEVELS - LOAD PARTICLE DATA
             */

            try {
                int depthLevel = Math.min(OctreeNode.maxDepth, PRELOAD_DEPTH);
                loadLod(depthLevel, octreeWrapper);
                flushLoadedIds();
            } catch (IOException e) {
                logger.error(e);
            }

            return octreeWrapper;
        } else {
            logger.info("Dataset not found: " + metadata + " - " + particles);
            return null;
        }
    }

    public boolean loadOctant(final OctreeNode octant, final AbstractOctreeWrapper octreeWrapper, boolean fullInit) {
        FileHandle octantFile = GlobalConf.data.dataFileHandle(particles + "particles_" + String.format("%06d", octant.pageId) + ".bin");
        if (!octantFile.exists() || octantFile.isDirectory()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Array<StarBean> data = (Array<StarBean>) particleReader.loadDataMapped(octantFile.path(), 1.0);
        StarGroup sg = StarGroup.getDefaultStarGroup("stargroup-%%SGID%%", data, fullInit);

        synchronized (octant) {
            sg.octant = octant;
            sg.octantId = octant.pageId;
            // Add objects to octree wrapper node
            octreeWrapper.add(sg, octant);
            // Aux info
            if (GaiaSky.instance != null && GaiaSky.instance.sg != null)
                GaiaSky.instance.sg.addNodeAuxiliaryInfo(sg);

            nLoadedStars += sg.size();
            octant.add(sg);

            // Put it at the end of the queue
            touch(octant);

            octant.setStatus(LoadStatus.LOADED);

            addLoadedInfo(octant.pageId, octant.countObjects());
        }
        return true;

    }

}
