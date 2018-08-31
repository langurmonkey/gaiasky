package gaia.cu9.ari.gaiaorbit.data.stars;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;

/**
 * Abstract catalog loader with the transformation from spherical to cartesian coordinates
 * @author Toni Sagrista
 *
 */
public abstract class AbstractCatalogLoader {
    /** Catalog files to load **/
    public String[] files;
    public List<CatalogFilter> filters;

    /**
     * Name
     */
    protected String name;
    /** Description **/
    protected String description;

    public void initialize(String[] files) {
        this.files = files;
        this.filters = new ArrayList<CatalogFilter>(0);
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
