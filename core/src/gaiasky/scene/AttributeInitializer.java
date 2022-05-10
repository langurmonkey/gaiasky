package gaiasky.scene;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import gaiasky.scene.component.*;
import gaiasky.util.Logger;

import java.util.HashMap;
import java.util.Map;

public class AttributeInitializer {
    private static final Logger.Log logger = Logger.getLogger(AttributeInitializer.class);

    private final Engine engine;
    private final  Map<String, Class<? extends Component>> attributeMap;

    public AttributeInitializer(final Engine engine) {
        this.engine = engine;
        this.attributeMap = new HashMap<>();
    }

    public Map<String, Class<? extends Component>> initializeAttributes() {
        if (this.engine != null) {
            // Base
            putAll(Base.class, "id", "name", "names", "opacity", "ct");

            // Body
            putAll(Body.class, "position", "positionKm", "positionPc", "pos", "posKm", "posPc", "size", "sizeKm", "sizePc", "sizepc", "sizeM", "sizeAU", "color", "labelcolor");

            // GraphNode
            putAll(GraphNode.class, "parent");

            // Coordinates
            putAll(Coordinates.class, "coordinates");

            // Rotation
            putAll(Rotation.class, "rotation");

            // Celestial
            putAll(Celestial.class, "wikiname", "colorbv");

            // Magnitude
            putAll(Magnitude.class, "appmag", "absmag");

            // SolidAngleThresholds
            putAll(SolidAngle.class, "thresholdNone", "thresholdPoint", "thresholdQuad");

            // Text
            putAll(Text.class, "labelFactor", "labelMax", "textScale");

            // ModelScaffolding
            putAll(ModelScaffolding.class, "refplane", "randomize", "seed", "sizescalefactor", "locvamultiplier", "locthoverfactor", "shadowvalues");

            // Model
            putAll(Model.class, "model");

            // Atmosphere
            putAll(Atmosphere.class, "atmosphere");

            // Cloud
            putAll(Cloud.class, "cloud");

            // RenderFlags
            putAll(RenderFlags.class, "renderquad");

            // Machine
            putAll(MotorEngine.class, "machines");

            // Trajectory
            putAll(Trajectory.class, "provider", "orbit", "model:Orbit", "trail", "orbittrail", "newmethod", "onlybody");

            // RefSysTransform
            putAll(RefSysTransform.class, "transformName", "transformFunction", "transformValues");

            // AffineTransformations
            putAll(AffineTransformations.class, "transformations");

            // Fade
            putAll(Fade.class, "fadein", "fadeout", "fade", "fadepc", "positionobjectname");

            // DatasetDescription
            putAll(DatasetDescription.class, "catalogInfo", "cataloginfo");

            // Label
            putAll(Label.class, "label", "label2d", "labelposition");

            // RenderType
            putAll(RenderType.class, "rendergroup", "billboardRenderGroup:Particle");

            // BillboardDataset
            putAll(BillboardSet.class, "data:BillboardGroup");

            // Title
            putAll(Title.class, "scale:Text2D", "lines:Text2D", "align:Text2D");

            // Axis
            putAll(Axis.class, "axesColors");

            // LocationMark
            putAll(LocationMark.class, "location", "distFactor");

            // Constel
            putAll(Constel.class, "ids");

            // Boundaries
            putAll(Boundaries.class, "boundaries");

            // ParticleSet
            putAll(ParticleSet.class, "provider:ParticleGroup", "datafile", "providerparams", "factor", "profiledecay", "colornoise", "particlesizelimits");

            // StarSet
            putAll(StarSet.class, "provider:StarGroup", "datafile:StarGroup", "providerparams:StarGroup", "factor:StarGroup", "profiledecay:StarGroup", "colornoise:StarGroup", "particlesizelimits:StarGroup");

            // Attitude
            putAll(Attitude.class, "provider:HeliotropicSatellite", "attitudeLocation");

            // ParticleExtra
            putAll(ParticleExtra.class, "primitiveRenderScale");

            return attributeMap;
        } else {
            throw new RuntimeException("Can't initialize attributes: engine is null");
        }
    }

    private void putAll(Class<? extends Component> clazz, String... attributes) {
        for (String attribute : attributes) {
            if (attributeMap.containsKey(attribute)) {
                logger.warn("Attribute already defined: " + attribute);
                throw new RuntimeException("Attribute already defined: " + attribute);
            } else {
                attributeMap.put(attribute, clazz);
            }
        }
    }
}
