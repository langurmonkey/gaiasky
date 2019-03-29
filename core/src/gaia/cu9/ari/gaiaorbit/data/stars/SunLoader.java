package gaia.cu9.ari.gaiaorbit.data.stars;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaia.cu9.ari.gaiaorbit.data.ISceneGraphLoader;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.Star;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Adds the sun manually
 * 
 * @author Toni Sagrista
 *
 */
public class SunLoader extends AbstractCatalogLoader implements ISceneGraphLoader {

    @Override
    public Array<? extends CelestialBody> loadData() {
        Array<Star> result = new Array<>(1);
        /** ADD SUN MANUALLY **/
        Star sun = new Star(new Vector3d(0, 0, 0), -26.73f, 4.85f, 0.656f, "Sol", TimeUtils.millis());
        if (runFiltersAnd(sun)) {
            sun.initialize();
            result.add(sun);
        }
        return result;
    }

}
