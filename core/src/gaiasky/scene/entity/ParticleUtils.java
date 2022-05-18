package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.scenegraph.octreewrapper.OctreeWrapper;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.scenegraph.particle.VariableRecord;
import gaiasky.util.*;
import gaiasky.util.coord.AstroUtils;
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

    public float highlightedSizeFactor(Highlight highlight, DatasetDescription datasetDesc) {
        return (highlight.highlighted && datasetDesc.catalogInfo != null) ? datasetDesc.catalogInfo.hlSizeFactor : getPointscaling(highlight);
    }

    public float getPointscaling(Highlight highlight) {
        //var graph = Mapper.graph.get(entity);

        // TODO octree
        //if(graph.parent instanceof OctreeWrapper) {
        //    return ((OctreeWrapper) parent).getPointscaling() * pointscaling;
        //}
        return highlight.pointscaling;
    }

    public double getVariableSizeScaling(final StarSet set, final int idx) {
        IParticleRecord ipr = set.pointData.get(idx);
        if (ipr instanceof VariableRecord) {
            VariableRecord vr = (VariableRecord) ipr;
            double[] times = vr.variTimes;
            float[] sizes = vr.variMags;
            int n = vr.nVari;

            // Days since epoch
            double t = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), set.variabilityEpochJd);
            double t0 = times[0];
            double t1 = times[n - 1];
            double period = t1 - t0;
            t = t % period;
            for (int i = 0; i < n - 1; i++) {
                double x0 = times[i] - t0;
                double x1 = times[i + 1] - t0;
                if (t >= x0 && t <= x1) {
                    return MathUtilsd.lint(t, x0, x1, sizes[i], sizes[i + 1]) / vr.size();
                }
            }
        }
        return 1;
    }
    public float getColor(int index, ParticleSet set, Highlight highlight) {
        return highlight.highlighted ? Color.toFloatBits(highlight.hlc[0], highlight.hlc[1], highlight.hlc[2], highlight.hlc[3]) : set.pointData.get(index).col();
    }

    public float[] getColor(Body body, Highlight highlight) {
        return highlight.highlighted ? highlight.hlc : body.color;
    }

    /**
     * Evaluates the filter of this dataset (if any) for the given particle index
     *
     * @param index The index to filter
     *
     * @return The result of the filter evaluation, true if the particle passed the filtering, false otherwise
     */
    public boolean filter(int index, ParticleSet particleSet, DatasetDescription datasetDescription) {
        final CatalogInfo catalogInfo = datasetDescription.catalogInfo;
        if (catalogInfo != null && catalogInfo.filter != null) {
            return catalogInfo.filter.evaluate(particleSet.get(index));
        }
        return true;
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
