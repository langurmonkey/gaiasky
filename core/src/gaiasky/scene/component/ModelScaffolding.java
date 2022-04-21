package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.render.ShadowMapImpl;
import gaiasky.scenegraph.component.ITransform;

import java.util.Arrays;
import java.util.List;

public class ModelScaffolding extends Component {

    /** Transformations, applied each cycle **/
    public ITransform[] transformations;

    /** Multiplier for Loc view angle **/
    public float locVaMultiplier = 1f;
    /** ThOverFactor for Locs **/
    public float locThOverFactor = 1f;

    /** Size factor, which can be set to scale model objects up or down **/
    public float sizeScaleFactor = 1f;

    /** Fade opacity, special to model bodies **/
    protected float fadeOpacity;

    /** Shadow map properties **/
    private ShadowMapImpl shadowMap;

    /** State flag; whether to render the shadow (number of times left) **/
    public int shadow;

    /** Name of the reference plane for this object. Defaults to equator **/
    public String refPlane;
    /** Name of the transformation to the reference plane **/
    public String refPlaneTransform;
    public String inverseRefPlaneTransform;

    /**
     * Array with shadow camera distance, cam near and cam far as a function of
     * the radius of the object
     */
    public double[] shadowMapValues;

    /** The seed for random components **/
    protected List<Long> seed = Arrays.asList(1L);

    /** The components to randomize---possible values are ["model", "cloud", "atmosphere"] **/
    protected List<String> randomize;
}
