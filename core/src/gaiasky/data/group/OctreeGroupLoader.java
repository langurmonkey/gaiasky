/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.GaiaSky;
import gaiasky.data.StreamingOctreeLoader;
import gaiasky.data.octreegen.MetadataBinaryIO;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.octreewrapper.AbstractOctreeWrapper;
import gaiasky.scenegraph.octreewrapper.OctreeWrapper;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.CatalogInfo;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.tree.LoadStatus;
import gaiasky.util.tree.OctreeNode;

import java.io.IOException;
import java.util.List;

/**
 * Implements the loading and streaming of octree nodes from files. This version
 * loads star groups using {@link gaiasky.data.group.BinaryDataProvider}.
 */
public class OctreeGroupLoader extends StreamingOctreeLoader {
    private static final Log logger = Logger.getLogger(OctreeGroupLoader.class);

    /**
     * The version of the data to load - before version 2, the data
     * format was not annotated with the version, so this info must come
     * from outside
     */
    private int dataVersionHint;

    /**
     * Binary particle reader
     **/
    private final BinaryDataProvider particleReader;

    /**
     * Epoch of stars loaded through this
     */
    private double epoch = AstroUtils.JD_J2015_5;

    public OctreeGroupLoader() {
        instance = this;
        particleReader = new BinaryDataProvider();
    }

    @Override
    protected AbstractOctreeWrapper loadOctreeData() {
        /*
         * LOAD METADATA
         */
        logger.info(I18n.msg("notif.loading", metadata));

        MetadataBinaryIO metadataReader = new MetadataBinaryIO();
        OctreeNode root = metadataReader.readMetadataMapped(metadata);

        if (root != null) {
            logger.info(I18n.msg("notif.nodeloader", root.numNodesRec(), metadata));
            logger.info(I18n.msg("notif.loading", particles));

            /*
             * CREATE OCTREE WRAPPER WITH ROOT NODE - particle group is by default
             * parallel, so we never use OctreeWrapperConcurrent
             */
            AbstractOctreeWrapper octreeWrapper = new OctreeWrapper("Universe", root);
            octreeWrapper.setFadeout(new double[] { 8e3, 5e5 });
            // Catalog info
            String name = this.name != null ? this.name : "LOD data";
            String description = this.description != null ? this.description : "Octree-based LOD dataset";
            CatalogInfo ci = new CatalogInfo(name, description, null, CatalogInfoSource.LOD, 1.5f, octreeWrapper);
            ci.nParticles = params.containsKey("nobjects") ? (Long) params.get("nobjects") : -1;
            ci.sizeBytes = params.containsKey("size") ? (Long) params.get("size") : -1;
            EventManager.publish(Event.CATALOG_ADD, this, ci, false);

            dataVersionHint = name.contains("DR2") || name.contains("dr2") || description.contains("DR2") || description.contains("dr2") ? 0 : 1;

            /*
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

    public boolean loadOctant(final OctreeNode octant, final AbstractOctreeWrapper octreeWrapper, final boolean fullInit) {
        FileHandle octantFile = Settings.settings.data.dataFileHandle(particles + "particles_" + String.format("%06d", octant.pageId) + ".bin");
        if (!octantFile.exists() || octantFile.isDirectory()) {
            return false;
        }
        List<IParticleRecord> data = particleReader.loadDataMapped(octantFile.path(), 1.0, dataVersionHint);
        StarGroup sg = StarGroup.getDefaultStarGroup("stargroup-%%SGID%%", data, fullInit);
        sg.setEpoch(epoch);
        sg.setCatalogInfoBare(octreeWrapper.getCatalogInfo());

        synchronized (octant) {
            sg.octant = octant;
            // Add objects to octree wrapper node.
            octreeWrapper.add(sg, octant);
            // Add to index and HIP map.
            if (GaiaSky.instance != null && GaiaSky.instance.sceneGraph != null)
                GaiaSky.instance.sceneGraph.addNodeAuxiliaryInfo(sg);

            nLoadedStars += sg.size();
            octant.add(sg);

            // Put it at the end of the queue
            touch(octant);

            octant.setStatus(LoadStatus.LOADED);
            // Update counts
            octant.touch(data.size());

            addLoadedInfo(octant.pageId, octant.countObjects());
        }
        return true;

    }

    public void setEpoch(Double epoch) {
        this.epoch = AstroUtils.getJulianDate(epoch);
    }

}
