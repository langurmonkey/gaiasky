/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.stars;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.data.ISceneGraphLoader;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.Star;
import gaiasky.util.math.Vector3d;

/**
 * Adds the sun manually
 *
 * @author Toni Sagrista
 * @deprecated
 */
public class SunLoader extends AbstractCatalogLoader implements ISceneGraphLoader {

    @Override
    public Array<? extends CelestialBody> loadData() {
        Array<Star> result = new Array<>(false, 1);
        /** ADD SUN MANUALLY **/
        Star sun = new Star(new Vector3d(0, 0, 0), -26.73f, 4.85f, 0.656f, new String[] { "Sun", "Sol", "Sonne" }, TimeUtils.millis());
        if (runFiltersAnd(sun)) {
            sun.initialize();
            result.add(sun);
        }
        return result;
    }

}
