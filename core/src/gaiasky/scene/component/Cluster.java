package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.gdx.model.IntModel;

/**
 * Some properties for star cluster objects.
 */
public class Cluster implements Component {

    // The texture, for when the cluster is far away
    public Texture clusterTex;

    // Distance of this cluster to Sol, in internal units
    public double dist;

    // Radius of this cluster in degrees
    public double raddeg;

    // Number of stars of this cluster
    public int nstars;

    // Years since epoch
    public double ySinceEpoch;

    /**
     * Fade alpha between quad and model. Attribute contains model opacity. Quad
     * opacity is <code>1-fadeAlpha</code>
     **/
    public float fadeAlpha;

    public IntModel model;
    public Matrix4 modelTransform;
}
