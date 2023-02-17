/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.util.gdx.IntMeshPartBuilder.VertexInfo;
import gaiasky.util.gdx.IntModelBuilder;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.shader.Material;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModelCache {
    /** Model cache **/
    public static ModelCache cache = new ModelCache();
    final Map<String, IntModel> modelCache;
    public IntModelBuilder mb;

    public ModelCache() {
        modelCache = new HashMap<>();
        mb = new IntModelBuilder();
    }

    public Pair<IntModel, Map<String, Material>> getModel(String shape, Map<String, Object> params, Bits attributes, int primitiveType) {
        String key = getKey(shape, params, attributes, primitiveType);
        IntModel model = null;
        Map<String, Material> materials = new HashMap<>();
        Material mat;
        if (modelCache.containsKey(key)) {
            model = modelCache.get(key);
            mat = model.materials.first();
        } else {
            mat = new Material();
            switch (shape) {
            case "sphere" -> {
                var quality = ((Long) params.get("quality")).intValue();
                var diameter = params.containsKey("diameter") ? ((Double) params.get("diameter")).floatValue() : 1f;
                var flip = params.containsKey("flip") ? (Boolean) params.get("flip") : false;
                model = mb.createSphere(diameter, diameter, diameter, quality, quality, flip, primitiveType, mat, attributes);
                modelCache.put(key, model);
            }
            case "icosphere" -> {
                var recursion = ((Long) params.get("recursion")).intValue();
                var diameter = params.containsKey("diameter") ? ((Double) params.get("diameter")).floatValue() : 1f;
                var flip = params.containsKey("flip") ? (Boolean) params.get("flip") : false;
                model = mb.createIcoSphere(diameter / 2, recursion, flip, false, primitiveType, mat, attributes);
                modelCache.put(key, model);
            }
            case "octahedronsphere" -> {
                var divisions = ((Long) params.get("divisions")).intValue();
                var diameter = params.containsKey("diameter") ? ((Double) params.get("diameter")).floatValue() : 1f;
                var flip = params.containsKey("flip") ? (Boolean) params.get("flip") : false;
                model = mb.createOctahedronSphere(diameter / 2, divisions, flip, false, primitiveType, mat, attributes);
                modelCache.put(key, model);
            }
            case "plane", "patch", "surface", "billboard" -> {
                var divisionsU = ((Long) params.get("divisionsu")).intValue();
                var divisionsV = ((Long) params.get("divisionsv")).intValue();
                var side = params.containsKey("size") ? ((Double) params.get("size")).floatValue() : (params.containsKey("side") ? ((Double) params.get("side")).floatValue() : -1);
                var width = side;
                var height = side;
                if (side < 0) {
                    width = ((Double) params.get("width")).floatValue();
                    height = ((Double) params.get("height")).floatValue();
                }
                var flip = params.containsKey("flip") ? (Boolean) params.get("flip") : false;
                model = mb.createPlane(width, height, divisionsU, divisionsV, flip, primitiveType, mat, attributes);
                modelCache.put(key, model);
            }
            case "disc" -> {
                // Prepare model
                var diameter2 = (params.containsKey("diameter") ? ((Double) params.get("diameter")).floatValue() : 1f) / 2f;
                // Initialize disc model

                // TOP VERTICES
                var vt00 = new VertexInfo();
                vt00.setPos(-diameter2, 0, -diameter2);
                vt00.setNor(0, 1, 0);
                vt00.setUV(0, 0);
                var vt01 = new VertexInfo();
                vt01.setPos(diameter2, 0, -diameter2);
                vt01.setNor(0, 1, 0);
                vt01.setUV(0, 1);
                var vt11 = new VertexInfo();
                vt11.setPos(diameter2, 0, diameter2);
                vt11.setNor(0, 1, 0);
                vt11.setUV(1, 1);
                var vt10 = new VertexInfo();
                vt10.setPos(-diameter2, 0, diameter2);
                vt10.setNor(0, 1, 0);
                vt10.setUV(1, 0);

                // BOTTOM VERTICES
                var vb00 = new VertexInfo();
                vb00.setPos(-diameter2, 0, -diameter2);
                vb00.setNor(0, 1, 0);
                vb00.setUV(0, 0);
                var vb01 = new VertexInfo();
                vb01.setPos(diameter2, 0, -diameter2);
                vb01.setNor(0, 1, 0);
                vb01.setUV(0, 1);
                var vb11 = new VertexInfo();
                vb11.setPos(diameter2, 0, diameter2);
                vb11.setNor(0, 1, 0);
                vb11.setUV(1, 1);
                var vb10 = new VertexInfo();
                vb10.setPos(-diameter2, 0, diameter2);
                vb10.setNor(0, 1, 0);
                vb10.setUV(1, 0);

                mb.begin();
                mb.part("up", primitiveType, attributes, mat).rect(vt00, vt01, vt11, vt10);
                mb.part("down", primitiveType, attributes, mat).rect(vb00, vb10, vb11, vb01);
                model = mb.end();
            }
            case "twofacedbillboard" -> {
                // Prepare model
                var diameter2 = (params.containsKey("diameter") ? ((Double) params.get("diameter")).floatValue() : 1f) / 2f;
                // Initialize disc model

                // TOP VERTICES
                var vt00 = new VertexInfo();
                vt00.setPos(-diameter2, -diameter2, 0);
                vt00.setNor(0, 1, 0);
                vt00.setUV(1, 1);

                var vt01 = new VertexInfo();
                vt01.setPos(-diameter2, diameter2, 0);
                vt01.setNor(0, 1, 0);
                vt01.setUV(1, 0);

                var vt11 = new VertexInfo();
                vt11.setPos(diameter2, diameter2, 0);
                vt11.setNor(0, 1, 0);
                vt11.setUV(0, 0);

                var vt10 = new VertexInfo();
                vt10.setPos(diameter2, -diameter2, 0);
                vt10.setNor(0, 1, 0);
                vt10.setUV(0, 1);

                // BOTTOM VERTICES
                var vb00 = new VertexInfo();
                vb00.setPos(-diameter2, -diameter2, 0);
                vb00.setNor(0, 1, 0);
                vb00.setUV(1, 1);

                var vb01 = new VertexInfo();
                vb01.setPos(-diameter2, diameter2, 0);
                vb01.setNor(0, 1, 0);
                vb01.setUV(1, 0);

                var vb11 = new VertexInfo();
                vb11.setPos(diameter2, diameter2, 0);
                vb11.setNor(0, 1, 0);
                vb11.setUV(0, 0);

                var vb10 = new VertexInfo();
                vb10.setPos(diameter2, -diameter2, 0);
                vb10.setNor(0, 1, 0);
                vb10.setUV(0, 1);

                mb.begin();
                mb.part("up", primitiveType, attributes, mat).rect(vt00, vt01, vt11, vt10);
                mb.part("down", primitiveType, attributes, mat).rect(vb00, vb10, vb11, vb01);
                model = mb.end();
            }
            case "cylinder" -> {
                // Use builder
                var width = ((Double) params.get("width")).floatValue();
                var height = ((Double) params.get("height")).floatValue();
                var depth = ((Double) params.get("depth")).floatValue();
                var divisions = ((Long) params.get("divisions")).intValue();
                var flip = params.containsKey("flip") ? (Boolean) params.get("flip") : false;

                model = mb.createCylinder(width, height, depth, divisions, flip, primitiveType, mat, attributes);
            }
            case "ring" -> {
                // Sphere with cylinder
                Material ringMat = new Material();
                materials.put("ring", ringMat);

                var quality = ((Long) params.get("quality")).intValue();
                var divisions = ((Long) params.get("divisions")).intValue();
                var innerRad = ((Double) params.get("innerradius")).floatValue();
                var outerRad = ((Double) params.get("outerradius")).floatValue();
                var sph = params.containsKey("sphere-in-ring") ? (Boolean) params.get("sphere-in-ring") : true;

                if (sph) {
                    model = ModelCache.cache.mb.createSphereRing(1, quality, quality, innerRad, outerRad, divisions, primitiveType, mat, ringMat, attributes);
                } else {
                    model = ModelCache.cache.mb.createRing(1, quality, quality, innerRad, outerRad, divisions, primitiveType, mat, ringMat, attributes);
                }
            }
            case "cone" -> {
                var width = ((Double) params.get("width")).floatValue();
                var height = ((Double) params.get("height")).floatValue();
                var depth = ((Double) params.get("depth")).floatValue();
                var divisions = ((Long) params.get("divisions")).intValue();
                int hDivisions = 0;
                if (params.containsKey("hdivisions")) {
                    hDivisions = ((Long) params.get("hdivisions")).intValue();
                }

                if (hDivisions == 0)
                    model = mb.createCone(width, height, depth, divisions, primitiveType, mat, attributes);
                else
                    model = mb.createCone(width, height, depth, divisions, hDivisions, primitiveType, mat, attributes);

            }
            case "cube", "box" -> {
                float width, height, depth;
                if (params.containsKey("width")) {
                    width = ((Double) params.get("width")).floatValue();
                    height = ((Double) params.get("height")).floatValue();
                    depth = ((Double) params.get("depth")).floatValue();
                } else {
                    width = ((Double) params.get("size")).floatValue();
                    height = ((Double) params.get("size")).floatValue();
                    depth = ((Double) params.get("size")).floatValue();
                }
                model = mb.createBox(width, height, depth, mat, attributes);
            }
            }
        }
        materials.put("base", mat);

        return new Pair<>(model, materials);
    }

    private String getKey(String shape, Map<String, Object> params, Bits attributes, int primitiveType) {
        StringBuilder key = new StringBuilder(shape + "-" + attributes + "-" + primitiveType);
        Set<String> keys = params.keySet();
        Object[] par = keys.toArray();
        for (Object object : par) {
            key.append("-").append(params.get(object.toString()));
        }
        return key.toString();

    }

    public void dispose() {
        Collection<IntModel> models = modelCache.values();
        for (IntModel model : models) {
            try {
                model.dispose();
            } catch (Exception e) {
                // Do nothing
            }
        }
    }
}
