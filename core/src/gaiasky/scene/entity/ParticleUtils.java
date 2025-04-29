/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.component.*;
import gaiasky.scene.record.ModelComponent;
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
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3d;

import java.util.Map;
import java.util.TreeMap;

public class ParticleUtils {

    private final Vector3d D31 = new Vector3d();
    /** Auxiliary color. **/
    protected final Color c = new Color();
    /** Auxiliary HSV array. **/
    protected final float[] hsv = new float[3];

    public ParticleUtils() {
    }

    public float getDatasetSizeFactor(Entity entity, Highlight hl, DatasetDescription dd) {
        return ((hl != null && hl.highlighted && dd != null && dd.catalogInfo != null) ? dd.catalogInfo.hlSizeFactor : 1) * getPointScaling(hl, Mapper.graph.get(entity));
    }

    public float getPointScaling(Highlight hl, GraphNode graph) {
        if (hl != null && graph.parent != null) {
            if (Mapper.octree.has(graph.parent)) {
                var hlParent = Mapper.highlight.get(graph.parent);
                return hl.pointscaling * hlParent.pointscaling;
            }

            return hl.pointscaling;
        } else {
            return 1;
        }
    }

    public double getVariableSizeScaling(final StarSet set, final int idx) {
        IParticleRecord ipr = set.pointData.get(idx);
        if (ipr.isVariable()) {
            double[] times = ipr.variTimes();
            float[] sizes = ipr.variMags();
            int n = ipr.nVari();

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
                    return MathUtilsDouble.lint(t, x0, x1, sizes[i], sizes[i + 1]) / ipr.size();
                }
            }
        }
        return 1;
    }

    /**
     * Saturates the color of the particle with the given index in the given set.
     *
     * @param index     Particle index.
     * @param set       Particle set.
     * @param highlight Highlight component.
     * @return The color, saturated if necessary.
     */
    public float saturateColor(int index, ParticleSet set, Highlight highlight) {
        var saturate = Settings.settings.scene.star.saturate;
        var colorPacked = getColor(index, set, highlight);
        if (!highlight.highlighted && saturate != 0) {
            // Saturate colors a bit.
            Color.abgr8888ToColor(c, colorPacked);
            c.toHsv(hsv);
            hsv[1] = MathUtils.clamp(hsv[1] + saturate, 0.0f, 1.0f);
            c.fromHsv(hsv);
            colorPacked = c.toFloatBits();
        }
        return colorPacked;
    }

    public float getColor(int index, ParticleSet set, Highlight highlight) {
        return highlight.highlighted ? Color.toFloatBits(highlight.hlc[0], highlight.hlc[1], highlight.hlc[2], highlight.hlc[3]) : set.pointData.get(index).color();
    }

    public float[] getColor(Body body, Highlight highlight) {
        return highlight.highlighted ? highlight.hlc : body.color;
    }

    /**
     * Evaluates the filter of this dataset (if any) for the given particle index
     *
     * @param index The index to filter
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
            particleSet.focusPositionSph.set((float) (MathUtilsDouble.radDeg * posSph.x), (float) (MathUtilsDouble.radDeg * posSph.y));
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

        Map<String, Object> params = new TreeMap<>();
        params.put("quality", 120L);
        params.put("diameter", 1d);
        params.put("flip", false);

        Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Bits.indices(Usage.Position, Usage.Normal, Usage.TextureCoordinates), GL20.GL_TRIANGLES);
        IntModel intModel = pair.getFirst();
        Material mat = pair.getSecond().get("base");
        mat.clear();
        // Only to activate view vector (camera position)
        mat.set(new ColorAttribute(ColorAttribute.Specular));
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
        Matrix4 modelTransform = new Matrix4();

        model.model = new ModelComponent(false);
        model.model.initialize(null);
        model.model.env = new Environment();
        model.model.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
        model.model.env.set(new FloatAttribute(FloatAttribute.Time, 0f));
        model.model.instance = new IntModelInstance(intModel, modelTransform);
        // Relativistic effects
        if (Settings.settings.runtime.relativisticAberration)
            model.model.rec.setUpRelativisticEffectsMaterial(model.model.instance.materials);
        model.model.setModelInitialized(true);
    }
}
