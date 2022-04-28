package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import uk.ac.starlink.util.DataSource;

public class Catalog implements Component {

    /** Catalog description **/
    protected String description;

    /**
     * Fully qualified name of data provider class
     */
    protected String provider;

    /**
     * Path of data file
     */
    protected String datafile;

    /**
     * STIL data source, if no data file exists
     */
    protected DataSource ds;

    protected Array<Entity> objects;
}
