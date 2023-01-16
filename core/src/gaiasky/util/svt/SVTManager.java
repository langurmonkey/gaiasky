package gaiasky.util.svt;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.record.VirtualTextureComponent;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Pair;
import gaiasky.util.gdx.graphics.FloatTextureDataExt;
import gaiasky.util.gdx.graphics.TextureExt;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Manages the SVT cache and indirection buffers. It processes the tile detection
 * buffer, determines the observed tiles, loads them, adds them to the cache and
 * updates the indirection buffer.
 */
public class SVTManager implements IObserver {
    private static final Log logger = Logger.getLogger(SVTManager.class);

    /** Maximum number of tiles to process per frame. */
    private static final int MAX_TILES_PER_FRAME = 3;
    /**
     * Size of the square cache texture.
     * This needs to be a multiple of the tile size, and tile sizes are powers of two,
     * capping at 1024, so this should be a multiple of 1024 to be on the safe side.
     **/
    private static final int CACHE_BUFFER_SIZE = 1024 * 8;

    // Tile state tokens.
    private static final int STATE_NOT_LOADED = 0;
    private static final int STATE_LOADING = 1;
    private static final int STATE_LOADED = 2;
    private static final int STATE_QUEUED = 3;
    private static final int STATE_CACHED = 4;

    private AssetManager manager;
    /**
     * The set of observed tiles from the camera.
     */
    private final Set<SVTQuadtreeNode<Path>> observedTiles;
    /**
     * Maps tile path objects to actual pixmaps.
     */
    private final Map<String, Pixmap> tilePixmaps;
    /**
     * Tiles queued to be paged in.
     */
    private final Queue<SVTQuadtreeNode<Path>> queuedTiles;

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
    private final Map<SVTQuadtreeNode<Path>, Pair<Integer, Integer>> tileLocation;

    /**
     * The cache buffer texture.
     */
    private Texture cacheBuffer;
    /**
     * The indirection buffer texture. {@link TextureExt} enables drawing to
     * any mipmap level.
     */
    private TextureExt indirectionBuffer;

    /**
     * Buffer used to draw in the indirection buffer.
     */
    private FloatBuffer floatBuffer;

    // Are the textures displaying in the UI already?
    private boolean uiViewCreated = false;

    public SVTManager() {
        super();
        this.observedTiles = new HashSet<>();
        this.tilePixmaps = new HashMap<>();
        this.tileLocation = new HashMap<>();
        this.queuedTiles = new ArrayBlockingQueue<>(150);
    }

    public void initialize(AssetManager manager) {
        this.manager = manager;

        // Initialize cache buffer.
        var cacheTextureData = new PixmapTextureData(new Pixmap(CACHE_BUFFER_SIZE, CACHE_BUFFER_SIZE, Format.RGBA8888), Format.RGBA8888, false, false, false);
        cacheBuffer = new Texture(cacheTextureData);
        cacheBuffer.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        // Initialize float buffer to draw pixels (1x1 with 4 components per pixel).
        floatBuffer = BufferUtils.createFloatBuffer(4);

        EventManager.instance.subscribe(this, Event.SVT_TILE_DETECTION_READY);
    }

    public void update(final FloatBuffer tileDetectionBuffer) {
        observedTiles.clear();
        int size = tileDetectionBuffer.capacity() / 4;
        tileDetectionBuffer.rewind();
        for (int i = 0; i < size; i++) {
            // We need to add the
            float level = tileDetectionBuffer.get();
            float x = tileDetectionBuffer.get();
            float y = tileDetectionBuffer.get();
            float id = tileDetectionBuffer.get();

            if (id > 0) {
                var svt = VirtualTextureComponent.getSVT((int) id);
                if (tileSize < 0) {
                    tileSize = svt.tileSize;

                    // This must be exact, CACHE_BUFFER_SIZE must be divisible by tileSize.
                    cacheSizeInTiles = CACHE_BUFFER_SIZE / tileSize;
                    cacheBufferArray = new SVTQuadtreeNode[cacheSizeInTiles][cacheSizeInTiles];

                    // Initialize indirection buffer.
                    var indirectionSize = (int) Math.pow(2.0, svt.tree.depth);
                    // We use RGBA with 32-bit floating point numbers per channel for the indirection buffer.
                    var indirectionData = new FloatTextureDataExt(indirectionSize * svt.tree.root.length, indirectionSize, GL30.GL_RGBA32F, GL30.GL_RGBA, GL30.GL_FLOAT, true, false);
                    indirectionBuffer = new TextureExt(indirectionData);
                    // Important to set the minification filter to use mipmaps.
                    indirectionBuffer.setFilter(TextureFilter.MipMapNearestNearest, TextureFilter.Nearest);
                }

                if (svt != null) {
                    var tile = svt.tree.getTile((int) level, (int) x, (int) y);
                    if (tile != null) {
                        observedTiles.add(tile);
                    }
                }
            }
        }
        tileDetectionBuffer.clear();

        var now = TimeUtils.millis();

        // Process observed tiles.
        for (var tile : observedTiles) {
            var path = tile.object.toString();
            switch (tile.state) {
            case STATE_NOT_LOADED -> {
                // Load tile.
                if (!manager.contains(path)) {
                    manager.load(path, Pixmap.class);
                    tile.state = STATE_LOADING;
                }
            }
            case STATE_LOADING -> {
                // Check if done.
                if (manager.isLoaded(path)) {
                    // Retrieve texture and put in queue.
                    tilePixmaps.put(path, manager.get(path));
                    queuedTiles.add(tile);
                    tile.state = STATE_QUEUED;
                }
            }
            case STATE_LOADED -> {
                // Already loaded, just queue.
                queuedTiles.add(tile);
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
        while ((tile = queuedTiles.poll()) != null && addedTiles < MAX_TILES_PER_FRAME) {
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
                            putTileInCache(tile, pair.getFirst(), pair.getSecond(), now);
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

        if (!uiViewCreated && (addedTiles > 0 || removedTiles > 0) && tileLocation.size() > 1) {
            GaiaSky.postRunnable(() -> {
                // Create UI view
                EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "SVT cache", cacheBuffer, 0.1f);
                //EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "SVT indirection", indirectionBuffer, 4f);
            });
            uiViewCreated = true;
        }

        if (addedTiles > 0 || removedTiles > 0) {
            // Post update through event system.
            EventManager.publish(Event.SVT_CACHE_INDIRECTION_UPDATE, this, cacheBuffer, indirectionBuffer);
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
    private void putTileInCache(SVTQuadtreeNode<Path> tile, int i, int j, long now) {
        assert !tileLocation.containsKey(tile) : "Tile is already in the cache: " + tile;
        tileLocation.put(tile, new Pair<>(i, j));
        cacheBufferArray[i][j] = tile;

        var path = tile.object.toString();
        var pixmap = tilePixmaps.get(path);

        /*
         * Update cache buffer with tile at [x,y].
         */
        int x = i * tileSize;
        int y = j * tileSize;
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

        logger.debug("Tile added -> xy[" + x + "," + y + "] ij[" + i + "," + j + "]: " + tile);
    }

    /**
     * Remove the current tile from the cache buffer, if it is cached.
     *
     * @param tile The tile to remove.
     */
    private void removeTileFromCache(SVTQuadtreeNode<Path> tile) {
        var pair = tileLocation.remove(tile);
        int i = pair.getFirst();
        int j = pair.getSecond();
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
    private void fillIndirectionBuffer(SVTQuadtreeNode<Path> tile, int cacheX, int cacheY) {
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
    private void fillIndirectionTileWith(SVTQuadtreeNode<Path> tile, float r, float g, float b, float a) {
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
        indirectionBuffer.draw(floatBuffer, xy[0], xy[1], 1, 1, tile.mipLevel(), GL30.GL_RGBA, GL30.GL_FLOAT);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.SVT_TILE_DETECTION_READY) {
            // Compute visible tiles.
            var pixels = (FloatBuffer) data[0];
            update(pixels);
        }
    }

}
