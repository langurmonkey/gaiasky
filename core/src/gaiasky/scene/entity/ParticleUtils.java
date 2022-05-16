package gaiasky.scene.entity;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.GaiaSky;
import gaiasky.scene.component.Model;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Bits;
import gaiasky.util.ModelCache;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.FloatAttribute;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3d;

import java.util.Map;
import java.util.TreeMap;

/**
 * Contains utilities common to particle and star objects and sets.
 */
public class ParticleUtils {

    private final Vector3d D31 = new Vector3d();

    public ParticleUtils() {
    }



    public void updateFocusDataPos(ParticleSet particleSet) {
        if (particleSet.focusIndex < 0) {
            particleSet.focus = null;
        } else {
            particleSet.focus = particleSet.pointData.get(particleSet.focusIndex);
            particleSet.focusPosition.set(particleSet.focus.x(), particleSet.focus.y(), particleSet.focus.z());
            Vector3d posSph = Coordinates.cartesianToSpherical(particleSet.focusPosition, D31);
            particleSet.focusPositionSph.set((float) (MathUtilsd.radDeg * posSph.x), (float) (MathUtilsd.radDeg * posSph.y));
            particleSet.updateFocus(GaiaSky.instance.getICamera());
        }
    }

    /**
     * Initializes the star model.
     *
     * @param manager The asset manager.
     * @param model   The model component.
     */
    public void initModel(final AssetManager manager, final Model model) {
        if (model == null) {
            throw new RuntimeException("The incoming star model component can't be null!");
        }

        Texture tex = manager.get(Settings.settings.data.dataFile("tex/base/star.jpg"), Texture.class);
        Texture lut = manager.get(Settings.settings.data.dataFile("tex/base/lut.jpg"), Texture.class);
        tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        Map<String, Object> params = new TreeMap<>();
        params.put("quality", 120L);
        params.put("diameter", 1d);
        params.put("flip", false);

        Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Bits.indexes(Usage.Position, Usage.Normal, Usage.TextureCoordinates), GL20.GL_TRIANGLES);
        IntModel intModel = pair.getFirst();
        Material mat = pair.getSecond().get("base");
        mat.clear();
        mat.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
        mat.set(new TextureAttribute(TextureAttribute.Normal, lut));
        // Only to activate view vector (camera position)
        mat.set(new ColorAttribute(ColorAttribute.Specular));
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
        Matrix4 modelTransform = new Matrix4();

        var mc = model.model;
        mc = new ModelComponent(false);
        mc.initialize(null);
        mc.env = new Environment();
        mc.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
        mc.env.set(new FloatAttribute(FloatAttribute.Time, 0f));
        mc.instance = new IntModelInstance(intModel, modelTransform);
        // Relativistic effects
        if (Settings.settings.runtime.relativisticAberration)
            mc.rec.setUpRelativisticEffectsMaterial(mc.instance.materials);
        mc.setModelInitialized(true);
    }
}
