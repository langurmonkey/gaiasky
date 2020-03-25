/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.stars;

import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.SceneGraphNode;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract catalog loader with the transformation from spherical to cartesian coordinates
 * @author Toni Sagrista
 *
 */
public abstract class AbstractCatalogLoader {
    /** Catalog files to load **/
    public String[] files;
    public List<CatalogFilter> filters;

    /** Name **/
    protected String name;
    /** Description **/
    protected String description;

    // Default parent name
    protected String parentName;

    public void initialize(String[] files) {
        this.files = files;
        this.filters = new ArrayList<>(0);
    }

    public abstract Array<? extends SceneGraphNode> loadData() throws FileNotFoundException;

    public void addFilter(CatalogFilter cf) {
        filters.add(cf);
    }

    /**
     * Runs all filters on the star and returns true only if all have passed.
     * @param s The star
     * @return True if all filters have passed
     */
    protected boolean runFiltersAnd(CelestialBody s) {
        if (filters == null || filters.isEmpty())
            return true;
        for (CatalogFilter filter : filters) {
            if (!filter.filter(s))
                return false;
        }
        return true;
    }

    /**
     * Runs all filters on the star and returns true if any of them passes
     * @param s The star
     * @return True if any filter has passed
     */
    protected boolean runFiltersOr(CelestialBody s) {
        if (filters == null || filters.isEmpty())
            return true;
        for (CatalogFilter filter : filters) {
            if (filter.filter(s))
                return true;
        }
        return false;
    }

    public void setParentName(String parentName){
        this.parentName = parentName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
