package gaiasky.util.gdx;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.Array;

/**
 * Responsible for sorting {@link IntRenderable} lists by whatever criteria (material, distance to camera, etc.)
 *
 * @author badlogic
 */
public interface IntRenderableSorter {
    /**
     * Sorts the array of {@link IntRenderable} instances based on some criteria, e.g. material, distance to camera etc.
     *
     * @param renderables the array of renderables to be sorted
     */
    void sort(Camera camera, Array<IntRenderable> renderables);
}
