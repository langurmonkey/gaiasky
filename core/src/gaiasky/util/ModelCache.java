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
            case "sphere":
                int quality = ((Long) params.get("quality")).intValue();
                float diameter = params.containsKey("diameter") ? ((Double) params.get("diameter")).floatValue() : 1f;
                boolean flip = params.containsKey("flip") ? (Boolean) params.get("flip") : false;
                model = mb.createSphere(diameter, diameter, diameter, quality, quality, flip, primitiveType, mat, attributes);
                modelCache.put(key, model);
                break;
            case "icosphere":
                int recursion = ((Long) params.get("recursion")).intValue();
                diameter = params.containsKey("diameter") ? ((Double) params.get("diameter")).floatValue() : 1f;
                flip = params.containsKey("flip") ? (Boolean) params.get("flip") : false;
                model = mb.createIcoSphere(diameter / 2, recursion, flip, false, primitiveType, mat, attributes);
                modelCache.put(key, model);
                break;
            case "octahedronsphere":
                int divisions = ((Long) params.get("divisions")).intValue();
                diameter = params.containsKey("diameter") ? ((Double) params.get("diameter")).floatValue() : 1f;
                flip = params.containsKey("flip") ? (Boolean) params.get("flip") : false;
                model = mb.createOctahedronSphere(diameter / 2, divisions, flip, false, primitiveType, mat, attributes);
                modelCache.put(key, model);
                break;
            case "plane":
            case "patch":
                int divisionsU = ((Long) params.get("divisionsu")).intValue();
                int divisionsV = ((Long) params.get("divisionsv")).intValue();
                float side = ((Double) params.get("side")).floatValue();
                model = mb.createPlane(side, divisionsU, divisionsV, primitiveType, mat, attributes);
                modelCache.put(key, model);
                break;
            case "disc":
                // Prepare model
                float diameter2 = (params.containsKey("diameter") ? ((Double) params.get("diameter")).floatValue() : 1f) / 2f;
                // Initialize disc model

                // TOP VERTICES
                VertexInfo vt00 = new VertexInfo();
                vt00.setPos(-diameter2, 0, -diameter2);
                vt00.setNor(0, 1, 0);
                vt00.setUV(0, 0);
                VertexInfo vt01 = new VertexInfo();
                vt01.setPos(diameter2, 0, -diameter2);
                vt01.setNor(0, 1, 0);
                vt01.setUV(0, 1);
                VertexInfo vt11 = new VertexInfo();
                vt11.setPos(diameter2, 0, diameter2);
                vt11.setNor(0, 1, 0);
                vt11.setUV(1, 1);
                VertexInfo vt10 = new VertexInfo();
                vt10.setPos(-diameter2, 0, diameter2);
                vt10.setNor(0, 1, 0);
                vt10.setUV(1, 0);

                // BOTTOM VERTICES
                VertexInfo vb00 = new VertexInfo();
                vb00.setPos(-diameter2, 0, -diameter2);
                vb00.setNor(0, 1, 0);
                vb00.setUV(0, 0);
                VertexInfo vb01 = new VertexInfo();
                vb01.setPos(diameter2, 0, -diameter2);
                vb01.setNor(0, 1, 0);
                vb01.setUV(0, 1);
                VertexInfo vb11 = new VertexInfo();
                vb11.setPos(diameter2, 0, diameter2);
                vb11.setNor(0, 1, 0);
                vb11.setUV(1, 1);
                VertexInfo vb10 = new VertexInfo();
                vb10.setPos(-diameter2, 0, diameter2);
                vb10.setNor(0, 1, 0);
                vb10.setUV(1, 0);

                mb.begin();
                mb.part("up", primitiveType, attributes, mat).rect(vt00, vt01, vt11, vt10);
                mb.part("down", primitiveType, attributes, mat).rect(vb00, vb10, vb11, vb01);
                model = mb.end();
                break;
            case "twofacedbillboard":
                // Prepare model
                diameter2 = (params.containsKey("diameter") ? ((Double) params.get("diameter")).floatValue() : 1f) / 2f;
                // Initialize disc model

                // TOP VERTICES
                vt00 = new VertexInfo();
                vt00.setPos(-diameter2, -diameter2, 0);
                vt00.setNor(0, 1, 0);
                vt00.setUV(1, 1);

                vt01 = new VertexInfo();
                vt01.setPos(-diameter2, diameter2, 0);
                vt01.setNor(0, 1, 0);
                vt01.setUV(1, 0);

                vt11 = new VertexInfo();
                vt11.setPos(diameter2, diameter2, 0);
                vt11.setNor(0, 1, 0);
                vt11.setUV(0, 0);

                vt10 = new VertexInfo();
                vt10.setPos(diameter2, -diameter2, 0);
                vt10.setNor(0, 1, 0);
                vt10.setUV(0, 1);

                // BOTTOM VERTICES
                vb00 = new VertexInfo();
                vb00.setPos(-diameter2, -diameter2, 0);
                vb00.setNor(0, 1, 0);
                vb00.setUV(1, 1);

                vb01 = new VertexInfo();
                vb01.setPos(-diameter2, diameter2, 0);
                vb01.setNor(0, 1, 0);
                vb01.setUV(1, 0);

                vb11 = new VertexInfo();
                vb11.setPos(diameter2, diameter2, 0);
                vb11.setNor(0, 1, 0);
                vb11.setUV(0, 0);

                vb10 = new VertexInfo();
                vb10.setPos(diameter2, -diameter2, 0);
                vb10.setNor(0, 1, 0);
                vb10.setUV(0, 1);

                mb.begin();
                mb.part("up", primitiveType, attributes, mat).rect(vt00, vt01, vt11, vt10);
                mb.part("down", primitiveType, attributes, mat).rect(vb00, vb10, vb11, vb01);
                model = mb.end();
                break;
            case "cylinder":
                // Use builder
                float width = ((Double) params.get("width")).floatValue();
                float height = ((Double) params.get("height")).floatValue();
                float depth = ((Double) params.get("depth")).floatValue();
                divisions = ((Long) params.get("divisions")).intValue();
                flip = params.containsKey("flip") ? (Boolean) params.get("flip") : false;

                model = mb.createCylinder(width, height, depth, divisions, flip, primitiveType, mat, attributes);

                break;
            case "ring":
                // Sphere with cylinder
                Material ringMat = new Material();
                materials.put("ring", ringMat);

                quality = ((Long) params.get("quality")).intValue();
                divisions = ((Long) params.get("divisions")).intValue();
                float innerRad = ((Double) params.get("innerradius")).floatValue();
                float outerRad = ((Double) params.get("outerradius")).floatValue();
                boolean sph = params.containsKey("sphere-in-ring") ? (Boolean) params.get("sphere-in-ring") : true;

                if (sph) {
                    model = ModelCache.cache.mb.createSphereRing(1, quality, quality, innerRad, outerRad, divisions, primitiveType, mat, ringMat, attributes);
                } else {
                    model = ModelCache.cache.mb.createRing(1, quality, quality, innerRad, outerRad, divisions, primitiveType, mat, ringMat, attributes);
                }
                break;
            case "cone":
                width = ((Double) params.get("width")).floatValue();
                height = ((Double) params.get("height")).floatValue();
                depth = ((Double) params.get("depth")).floatValue();
                divisions = ((Long) params.get("divisions")).intValue();
                int hDivisions = 0;
                if (params.containsKey("hdivisions")) {
                    hDivisions = ((Long) params.get("hdivisions")).intValue();
                }

                if (hDivisions == 0)
                    model = mb.createCone(width, height, depth, divisions, primitiveType, mat, attributes);
                else
                    model = mb.createCone(width, height, depth, divisions, hDivisions, primitiveType, mat, attributes);

                break;
            case "cube":
            case "box":
                if (params.containsKey("width")) {
                    width = ((Double) params.get("width")).floatValue();
                    height = ((Double) params.get("height")).floatValue();
                    depth = ((Double) params.get("depth")).floatValue();
                } else {
                    width = ((Double) params.get("size")).floatValue();
                    height = width;
                    depth = width;
                }
                model = mb.createBox(width, height, depth, mat, attributes);
                break;
            }
        }
        materials.put("base", mat);

        return new Pair<>(model, materials);
    }

    private String getKey(String shape, Map<String, Object> params, Bits attributes, int primitiveType) {
        StringBuilder key = new StringBuilder(shape + "-" + attributes + "-" + primitiveType);
        Set<String> keys = params.keySet();
        Object[] par = keys.toArray();
        for (Object o : par) {
            key.append("-").append(params.get(o));
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
