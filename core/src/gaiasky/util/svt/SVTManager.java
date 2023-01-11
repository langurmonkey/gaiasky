package gaiasky.util.svt;

import com.badlogic.gdx.assets.AssetManager;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.record.VirtualTextureComponent;

import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Manages the SVT cache and indirection buffers. It processes the view determination
 * buffer, determines the observed tiles, loads them, adds them to the cache and
 * updates the indirection buffer.
 */
public class SVTManager implements IObserver {

    private AssetManager manager;
    private Set<SVTQuadtreeNode> observedTiles;

    public SVTManager() {
        super();
    }

    public void initialize(AssetManager manager) {
        this.manager = manager;
        this.observedTiles = new HashSet<>();
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
                if (svt != null) {
                    var tile = svt.tree.getTile((int) level, (int) x, (int) y);
                    if (tile != null) {
                        observedTiles.add(tile);
                    }
                }
            }
        }
        pixels.clear();


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
