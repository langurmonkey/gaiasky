/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.svt;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.record.CloudComponent;
import gaiasky.scene.record.MaterialComponent;
import gaiasky.scene.record.NamedComponent;
import gaiasky.scene.record.VirtualTextureComponent;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class SVTManager implements IObserver {
    private static final Log logger = Logger.getLogger(SVTManager.class);

    /**
     * Size of the square cache texture. All SVTs share the same cache, so
     * the size needs to be a multiple of the tile size, and tile sizes are powers of two,
     * capping at 1024. Hence, the cache size needs to be a multiple of 1024.
     **/
    private static final int CACHE_BUFFER_SIZE = 1024 * Settings.settings.scene.renderer.virtualTextures.cacheSize;

    // Tile state tokens.
    private static final int STATE_NOT_LOADED = 0;
    private static final int STATE_LOADING = 1;
    private static final int STATE_LOADED = 2;
    private static final int STATE_QUEUED = 3;
    private static final int STATE_CACHED = 4;

    private static int svtSequenceId = 1;

    public static int nextSvtId() {
        return svtSequenceId++;
    }

    private AssetManager manager;

    /**
     * Map virtual texture ID to component.
     */
    private final IntMap<Array<VirtualTextureComponent>> vtIdMap;
    /**
     * The set of observed tiles from the camera.
     */
    private final Array<SVTQuadtreeNode<Path>> observedTiles;
    /**
     * Maps tile path objects to actual pixmaps.
     */
    private final Map<String, Pixmap> tilePixmaps;
    /**
     * Tiles queued to be paged in.
     */
    private final Deque<SVTQuadtreeNode<Path>> queuedTiles;

    /**
     * The tile size. The system can't mix SVTs with different tile sizes,
     * so only one is supported.
     **/
    private int tileSize = -1;

    /**
     * The dimension in tiles of each dimension of the cache buffer. This is also equal to
     * each dimension of the indirection buffer.
     */
    private int cacheSizeInTiles = -1;

    /**
     * Contains the currently paged tile in each position of the matrix.
     * The two dimensions are {@link SVTManager#CACHE_BUFFER_SIZE} / tileSize.
     */
    private SVTQuadtreeNode<Path>[][] cacheBufferArray;

    /**
     * Direct access to the location in the cache for each paged tile.
     */
    private final Map<SVTQuadtreeNode<Path>, int[]> tileLocation;

    /**
     * The cache buffer texture.
     */
    private Texture cacheBuffer;

    /**
     * Buffer used to draw in the indirection buffer.
     */
    private FloatBuffer floatBuffer;

    // Are the textures displaying in the UI already?
    private boolean DEBUG_UI_VIEW = false;

    public SVTManager() {
        super();
        this.observedTiles = new Array<>(50);
        this.tilePixmaps = new HashMap<>();
        this.tileLocation = new HashMap<>();
        this.vtIdMap = new IntMap<>(30);
        this.queuedTiles = new ArrayDeque<>(50);
    }

    public void doneLoading(AssetManager manager) {
        this.manager = manager;

        EventManager.instance.subscribe(this, Event.SVT_MATERIAL_INFO);
    }

    /**
     * Flushes the current observed tiles queue, and refills it using the data in the given tile detection buffer.
     *
     * @param tileDetectionBuffer The tile detection buffer data.
     */
    public void updateObservedTiles(final FloatBuffer tileDetectionBuffer) {
        observedTiles.clear();
        int size = tileDetectionBuffer.capacity() / 4;
        tileDetectionBuffer.rewind();
        for (int i = 0; i < size; i++) {
            // Each pixel has level, x, y, and id.
            float level = tileDetectionBuffer.get(); // r
            float x = tileDetectionBuffer.get(); // g
            float y = tileDetectionBuffer.get(); // b
            float id = tileDetectionBuffer.get(); // a

            if (id > 0 && vtIdMap.containsKey((int) id)) {
                var svts = vtIdMap.get((int) id);
                for (var svt : svts) {
                    observeSvt(svt, level, x, y);
                }
            }
        }
        tileDetectionBuffer.clear();

        if (!observedTiles.isEmpty() && cacheBuffer == null) {
            // Initialize cache buffer.
            var cacheTextureData = new PixmapTextureData(new Pixmap(CACHE_BUFFER_SIZE, CACHE_BUFFER_SIZE, Format.RGBA8888), Format.RGBA8888, false, false, false);
            cacheBuffer = new Texture(cacheTextureData);
            cacheBuffer.setFilter(TextureFilter.Linear, TextureFilter.Linear);

            // Initialize float buffer to draw pixels (1x1 with 4 components per pixel).
            floatBuffer = BufferUtils.createFloatBuffer(4);
        }

        // Process observed tiles.
        processObservedTiles();

    }

    private void observeSvt(VirtualTextureComponent svt,
                            float level,
                            float x,
                            float y) {
        // Initialize tile size first time.
        if (tileSize < 0) {
            tileSize = svt.tileSize;
            // This must be exact, CACHE_BUFFER_SIZE must be divisible by tileSize.
            cacheSizeInTiles = CACHE_BUFFER_SIZE / tileSize;
            cacheBufferArray = new SVTQuadtreeNode[cacheSizeInTiles][cacheSizeInTiles];
        }

        var tile = svt.tree.getTile((int) level, (int) x, (int) y);
        // Try recursive lookup to higher levels.
        // This is useful in incomplete levels.
        if (tile == null && level > 0) {
            double[] uv = svt.tree.getUV((int) level, (int) x, (int) y);
            int l = (int) level;
            do {
                l -= 1;
                tile = svt.tree.getTileFromUV(l, uv[0], uv[1]);
            } while (tile == null && l > 0);
        }
        if (tile != null && !observedTiles.contains(tile, true)) {
            observedTiles.add(tile);
        }
    }

    /**
     * Processes the current observed tiles queue.
     */
    public void processObservedTiles() {
        var now = TimeUtils.millis();
        for (var tile : observedTiles) {
            var path = tile.object.toString();
            switch (tile.state) {
                case STATE_NOT_LOADED -> {
                    // Load tile.
                    if (!manager.contains(path)) {
                        manager.load(path, Pixmap.class);
                        tile.state = STATE_LOADING;
                    } else {
                        // In case the same SVT is used for multiple channels.
                        if (tile.state == STATE_NOT_LOADED) {
                            tile.state = STATE_LOADING;
                        }
                    }
                }
                case STATE_LOADING -> {
                    // Check if done.
                    if (manager.isLoaded(path)) {
                        var pixmap = (Pixmap) manager.get(path);
                        // Rescale if necessary, this should be avoided, as it is SLOW.
                        if (pixmap.getWidth() != tile.tree.tileSize) {
                            logger.warn("Rescaling tile: " + tile.toStringShort());
                            Pixmap aux = new Pixmap(tile.tree.tileSize, tile.tree.tileSize, pixmap.getFormat());
                            aux.drawPixmap(pixmap,
                                    0, 0, pixmap.getWidth(), pixmap.getHeight(),
                                    0, 0, tile.tree.tileSize, tile.tree.tileSize);
                            manager.unload(path);
                            pixmap = aux;
                        }
                        // Retrieve texture and put in queue.
                        tilePixmaps.put(path, pixmap);
                        // Add to head of queue.
                        queuedTiles.offerFirst(tile);
                        tile.state = STATE_QUEUED;
                    }
                }
                case STATE_LOADED -> {
                    // Already loaded, just add to the head of the queue.
                    queuedTiles.offerFirst(tile);
                    tile.state = STATE_QUEUED;
                }
                case STATE_QUEUED, STATE_CACHED -> {
                    // Update last accessed.
                    tile.accessed = now;
                }
            }
        }

        int addedTiles = 0;
        int removedTiles = 0;
        SVTQuadtreeNode<Path> tile;
        SVTQuadtreeNode<Path> finalTile = null;
        while ((tile = queuedTiles.poll()) != null && addedTiles < Settings.settings.scene.renderer.virtualTextures.maxTilesPerFrame) {
            finalTile = tile;
            if (tile.state == STATE_QUEUED) {
                if (!tileLocation.containsKey(tile)) {
                    if (tileLocation.size() < cacheSizeInTiles * cacheSizeInTiles) {
                        // Find first free location in cache.
                        outer1:
                        for (int j = 0; j < cacheSizeInTiles; j++) {
                            for (int i = 0; i < cacheSizeInTiles; i++) {
                                if (cacheBufferArray[i][j] == null) {
                                    // Use this location.
                                    putTileInCache(tile, i, j, now);
                                    addedTiles++;
                                    break outer1;
                                }
                            }
                        }

                    } else {
                        // We have no free locations, offload least recently used tile.
                        SVTQuadtreeNode<Path> lru = null;
                        for (int j = 0; j < cacheSizeInTiles; j++) {
                            for (int i = 0; i < cacheSizeInTiles; i++) {
                                var candidate = cacheBufferArray[i][j];
                                // Do not touch level-0 tiles.
                                if (candidate.level > 0 && (lru == null || candidate.accessed < lru.accessed)) {
                                    lru = candidate;
                                }
                            }
                        }

                        if (lru != null) {
                            // Unload lru.
                            var pair = tileLocation.get(lru);
                            removeTileFromCache(lru);
                            removedTiles++;
                            // Page in the new tile in [i,j].
                            putTileInCache(tile, pair[0], pair[1], now);
                            addedTiles++;
                        }
                    }
                } else {
                    // Tile already in the cache, update state!
                    tile.state = STATE_CACHED;
                }
            }
        }

        if (addedTiles > 0) {
            logger.debug("Paged in " + addedTiles + " virtual tiles.");
        }
        if (removedTiles > 0) {
            logger.debug("Paged out " + removedTiles + " virtual tiles.");
        }

        if (DEBUG_UI_VIEW && (addedTiles > 0 || removedTiles > 0)) {
            final var lastTile = finalTile;
            GaiaSky.postRunnable(() -> {
                // Create UI views with SVT cache and indirection textures.
                EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "SVT cache", cacheBuffer, 0.05f);
                //EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "SVT indirection", ((VirtualTextureComponent) lastTile.tree.aux).indirectionBuffer, 0.5f);
            });
            DEBUG_UI_VIEW = false;
        }

    }

    /**
     * Puts the given tile at the given location in the cache buffer.
     *
     * @param tile The tile.
     * @param i    The column in the cache.
     * @param j    The row in the cache.
     * @param now  The current time.
     */
    private void putTileInCache(SVTQuadtreeNode<Path> tile,
                                int i,
                                int j,
                                long now) {
        assert !tileLocation.containsKey(tile) : "Tile is already in the cache: " + tile;
        tileLocation.put(tile, new int[]{i, j});
        cacheBufferArray[i][j] = tile;

        var path = tile.object.toString();
        var pixmap = tilePixmaps.get(path);

        /*
         * Update cache buffer with tile at [x,y].
         */
        int x = i * tile.tree.tileSize;
        int y = j * tile.tree.tileSize;
        cacheBuffer.draw(pixmap, x, y);

        /*
         * Update indirection buffer.
         * Each pixel in the indirection buffer has:
         * - R: X coordinate in cache buffer.
         * - G: Y coordinate in cache buffer.
         * - B: reverse mip level.
         * - A: 1 - means tile is valid.
         */
        fillIndirectionBuffer(tile, i, j);

        // Update tile last accessed time and status.
        tile.accessed = now;
        tile.state = STATE_CACHED;

        // Set material attributes if needed.
        if (tile.tree.aux instanceof VirtualTextureComponent component) {
            if (!component.svtAttributesSet()) {
                component.setSVTAttributes(cacheBuffer);
            }
        }

        logger.debug("Tile added -> xy[" + x + "," + y + "] ij[" + i + "," + j + "]: " + tile);
    }

    /**
     * Remove the current tile from the cache buffer, if it is cached.
     *
     * @param tile The tile to remove.
     */
    private void removeTileFromCache(SVTQuadtreeNode<Path> tile) {
        var pair = tileLocation.remove(tile);
        int i = pair[0];
        int j = pair[1];
        // Remove from buffer array.
        cacheBufferArray[i][j] = null;
        // Clear tile in indirection buffer.
        clearIndirectionBuffer(tile);

        // Reset last accessed time and status.
        tile.accessed = 0;
        tile.state = STATE_LOADED;

        logger.debug("Tile removed -> ij[" + i + "," + j + "]: " + tile);
    }

    /**
     * Clears a tile in the indirection buffer at the given level.
     *
     * @param tile The tile to remove.
     */
    private void clearIndirectionBuffer(SVTQuadtreeNode<Path> tile) {
        // a = 0 means the tile is not valid.
        fillIndirectionTileWith(tile, 0f, 0f, 0f, 0f);
    }

    /**
     * Fill the indirection buffer with the given tile, whose position in the
     * cache is [i,j].
     * Each pixel in the indirection buffer has:
     * <ul>
     * <li>R: X coordinate of the tile in cache buffer.</li>
     * <li>G: Y coordinate of the tile in cache buffer.</li>
     * <li>B: reverse mip level of the tile.</li>
     * <li>A: 1 - means the tile is valid.</li>
     * </ul>
     *
     * @param tile   The tile.
     * @param cacheX The tile column in the cache buffer.
     * @param cacheY The tile row in the cache buffer.
     */
    private void fillIndirectionBuffer(SVTQuadtreeNode<Path> tile,
                                       int cacheX,
                                       int cacheY) {
        var x = (float) cacheX;
        var y = (float) cacheY;
        fillIndirectionTileWith(tile, x, y, tile.level, 1f);
    }

    /**
     * Fills the given tile in the indirection buffer with the given data.
     *
     * @param tile The tile.
     * @param r    The red channel, in [0,1].
     * @param g    The green channel, in [0,1].
     * @param b    The blue channel, in [0,1].
     * @param a    The alpha channel, in [0,1].
     */
    private void fillIndirectionTileWith(SVTQuadtreeNode<Path> tile,
                                         float r,
                                         float g,
                                         float b,
                                         float a) {
        // a=0 means the tile is not valid.
        floatBuffer.rewind();
        floatBuffer.put(0, r);
        floatBuffer.put(1, g);
        floatBuffer.put(2, b);
        floatBuffer.put(3, a);

        var tileUV = tile.getUV();
        var xy = tile.tree.getColRow(tile.level, tileUV[0], tileUV[1]);
        // In OpenGL, level 0 is the base level with the highest resolution, while n is the nth mipmap reduction image.
        // In our system, 0 is the root, the lowest detailed tiles, while depth is the base level (the highest resolution).
        if (tile.tree.aux instanceof VirtualTextureComponent component) {
            if (component.indirectionBuffer != null) {
                component.indirectionBuffer.draw(floatBuffer, xy[0], xy[1], 1, 1, tile.mipLevel(), GL30.GL_RGBA, GL30.GL_FLOAT);
            }
        }
    }

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        if (event == Event.SVT_MATERIAL_INFO) {
            // Put in list.
            var id = (Integer) data[0];
            var comp = (NamedComponent) data[1];
            if (comp instanceof MaterialComponent mc) {
                for (var vtc : mc.svts) {
                    addToVTMap(id, vtc);
                }
            } else if (comp instanceof CloudComponent cc) {
                addToVTMap(id, cc.diffuseSvt);
            }
        }
    }

    private void addToVTMap(int id,
                            VirtualTextureComponent component) {
        if (component == null) {
            return;
        }
        if (!vtIdMap.containsKey(id)) {
            vtIdMap.put(id, new Array<>());
        }
        var array = vtIdMap.get(id);
        if (!array.contains(component, true)) {
            array.add(component);
        }
    }

}
