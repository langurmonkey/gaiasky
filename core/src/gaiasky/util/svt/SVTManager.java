package gaiasky.util.svt;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
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

import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Manages the SVT cache and indirection buffers. It processes the view determination
 * buffer, determines the observed tiles, loads them, adds them to the cache and
 * updates the indirection buffer.
 */
public class SVTManager implements IObserver {
    private static final Log logger = Logger.getLogger(SVTManager.class);

    /** Maximum number of tiles to process per frame. */
    private static final int MAX_TILES_PER_FRAME = 3;
    /** Size of the square cache texture. */
    private static final int CACHE_BUFFER_SIZE = 4096;

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
    private int cacheSize = -1;

    /**
     * Contains the currently paged tile in each position of the matrix.
     * The two dimensions are {@link SVTManager#CACHE_BUFFER_SIZE} / tileSize.
     */
    private SVTQuadtreeNode<Path>[][] cacheBufferArray;

    /**
     * Direct access to the location in the cache for each paged tile.
     */
    private Map<SVTQuadtreeNode<Path>, Pair<Integer, Integer>> tileLocation;

    /**
     * The actual textures that hold the cache and the indirection buffers.
     */
    private Texture cacheBuffer, indirectionBuffer;

    /**
     * A 1x1 pixmap used to draw in the indirection texture.
     */
    private final Pixmap indirectionPixmap;

    // Is the cache displaying in the UI already?
    private boolean cacheInUi = false;

    public SVTManager() {
        super();
        this.observedTiles = new HashSet<>();
        this.tilePixmaps = new HashMap<>();
        this.tileLocation = new HashMap<>();
        this.queuedTiles = new ArrayBlockingQueue<>(150);
        this.indirectionPixmap = new Pixmap(1, 1, Format.RGBA8888);
    }

    public void initialize(AssetManager manager) {
        this.manager = manager;

        // Initialize cache buffer.
        var cacheTextureData = new PixmapTextureData(new Pixmap(CACHE_BUFFER_SIZE, CACHE_BUFFER_SIZE, Format.RGBA8888), Format.RGBA8888, false, false, false);
        cacheBuffer = new Texture(cacheTextureData);

        EventManager.instance.subscribe(this, Event.SVT_VIEW_DETERMINATION_PROCESS);
    }

    public void update(final FloatBuffer pixels) {
        observedTiles.clear();
        int size = pixels.capacity() / 4;
        pixels.rewind();
        for (int i = 0; i < size; i++) {
            float level = pixels.get();
            float x = pixels.get();
            float y = pixels.get();
            float id = pixels.get();

            if (id > 0) {
                var svt = VirtualTextureComponent.getSVT((int) id);
                if (tileSize < 0) {
                    tileSize = svt.tileSize;

                    // This must be exact, CACHE_BUFFER_SIZE must be divisible by tileSize.
                    cacheSize = CACHE_BUFFER_SIZE / tileSize;
                    cacheBufferArray = new SVTQuadtreeNode[cacheSize][cacheSize];

                    // Initialize indirection buffer.
                    var indirectionTextureData = new PixmapTextureData(new Pixmap(cacheSize, cacheSize, Format.RGBA8888), null, false, false, false);
                    indirectionBuffer = new Texture(indirectionTextureData);
                }

                if (svt != null) {
                    var tile = svt.tree.getTile((int) level, (int) x, (int) y);
                    if (tile != null) {
                        observedTiles.add(tile);
                    }
                }
            }
        }
        pixels.clear();

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
                    if (tileLocation.size() < cacheSize * cacheSize) {
                        // Find first free location in cache.
                        outer1:
                        for (int j = 0; j < cacheSize; j++) {
                            for (int i = 0; i < cacheSize; i++) {
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
                        for (int j = 0; j < cacheSize; j++) {
                            for (int i = 0; i < cacheSize; i++) {
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
            logger.info("Paged in " + addedTiles + " virtual tiles.");
        }
        if (removedTiles > 0) {
            logger.info("Paged out " + removedTiles + " virtual tiles.");
        }

        if (!cacheInUi && (addedTiles > 0 || removedTiles > 0) && tileLocation.size() > 1) {
            GaiaSky.postRunnable(() -> {
                // Create UI view
                EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "SVT cache", cacheBuffer, 0.2f);
            });
            cacheInUi = true;
        }

    }

    private void putTileInCache(SVTQuadtreeNode<Path> tile, int i, int j, long now) {
        assert !tileLocation.containsKey(tile) : "Tile is already in the cache: " + tile;
        tileLocation.put(tile, new Pair<>(i, j));
        cacheBufferArray[i][j] = tile;

        var path = tile.object.toString();
        var pixmap = tilePixmaps.get(path);

        // Update cache buffer.
        int x = i * tileSize;
        int y = j * tileSize;
        cacheBuffer.draw(pixmap, x, y);

        // Update indirection buffer.
        indirectionPixmap.setColor(1.0f, 2.0f, 3.0f, 2.0f);
        indirectionPixmap.drawPixel(i, j);
        indirectionBuffer.draw(indirectionPixmap, i, j);

        // Update tile last accessed time and status.
        tile.accessed = now;
        tile.state = STATE_CACHED;

        logger.info("Tile added -> xy[" + x + "," + y + "] ij[" + i + "," + j + "]: " + tile);
    }

    private void removeTileFromCache(SVTQuadtreeNode<Path> tile) {
        var pair = tileLocation.remove(tile);
        int i = pair.getFirst();
        int j = pair.getSecond();
        cacheBufferArray[i][j] = null;

        // Update indirection buffer with special -1 token.
        indirectionPixmap.setColor(-1.0f, -1.0f, -1.0f, -1.0f);
        indirectionPixmap.drawPixel(i, j);
        indirectionBuffer.draw(indirectionPixmap, i, j);

        // Reset last accessed time and status.
        tile.accessed = 0;
        tile.state = STATE_LOADED;

        logger.info("Tile removed -> ij[" + i + "," + j + "]: " + tile);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.SVT_VIEW_DETERMINATION_PROCESS) {
            // Compute visible tiles.
            var pixels = (FloatBuffer) data[0];
            update(pixels);
        }
    }
}
