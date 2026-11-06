/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.component.*;
import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.scene.record.GalaxyGenerator;
import gaiasky.util.Constants;
import gaiasky.util.coord.StaticCoordinates;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3Q;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * The exported descriptor of a hybrid galaxy has to read back as the galaxy it describes, so every key
 * it writes has to be one the loader knows: either an attribute of the archetype attribute map, or a
 * property with a setter in the class the loader instantiates for that object.
 * <p>
 * A misspelled key is silent - the loader logs nothing in the field case and drops the attribute into
 * the extra attributes map in the entity case - so the checks here are on the names themselves.
 */
public class HybridGalaxyExportTest {

    /** Keys the loader skips on purpose. Mirrors {@code JsonLoader#PARAM_SKIP}. **/
    private static final Set<String> SKIPPED = Set.of("args", "impl", "archetype", "comment", "comments");

    private static JsonValue attributeMap() {
        try {
            return new JsonReader().parse(read("assets/archetypes/attributemap.json")).get("components");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String read(String relative)
            throws IOException {
        for (String root : List.of("..", ".", "../..")) {
            Path p = Paths.get(root, relative);
            if (Files.exists(p)) {
                return Files.readString(p);
            }
        }
        throw new IOException("Cannot find " + relative + " from " + Paths.get(".").toAbsolutePath());
    }

    /** Collects every attribute key of a component of the map, aliases included. **/
    private static Set<String> attributesOf(JsonValue components,
                                            String component) {
        Set<String> keys = new HashSet<>();
        var c = components.get(component);
        for (JsonValue a = c.child; a != null; a = a.next) {
            keys.add(a.name);
            if (a.has("aliases")) {
                keys.addAll(Arrays.asList(a.get("aliases").asStringArray()));
            }
        }
        return keys;
    }

    private static void assertSetters(Class<?> clazz,
                                      JsonValue object) {
        for (JsonValue v = object.child; v != null; v = v.next) {
            if (SKIPPED.contains(v.name)) {
                continue;
            }
            String setter = "set" + Character.toUpperCase(v.name.charAt(0)) + v.name.substring(1);
            boolean found = false;
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(setter) && m.getParameterCount() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(clazz.getSimpleName() + " has no setter for the exported key '" + v.name + "'", found);
        }
    }

    /**
     * The billboard half, with the components {@code convertToJson(Entity)} reads. The base is named
     * through the field: {@code setName()} resolves the localised name through I18n, which no unit
     * test has.
     */
    private static Entity billboardEntity(String name) {
        var e = new Entity();
        var base = new Base();
        base.names = new String[] {name};
        base.ct = new ComponentTypes(ComponentType.Galaxies);
        e.add(base);

        var body = new Body();
        body.size = (float) (10 * Constants.KPC_TO_U);
        e.add(body);

        e.add(new GraphNode());
        e.add(new Label());

        var coords = new Coordinates();
        coords.coordinates = staticCoordinates();
        e.add(coords);
        e.add(new RefSysTransform());

        var bb = new BillboardSet();
        bb.datasets = new gaiasky.scene.record.BillboardDataset[0];
        bb.textureFiles = new String[] {};
        e.add(bb);

        var focus = new Focus();
        focus.focusable = true;
        e.add(focus);

        var fade = new Fade();
        fade.setFadeObjectName(name);
        fade.setFadeOut(new double[] {5000, 10000});
        e.add(fade);

        e.add(new Render());
        return e;
    }

    private static StaticCoordinates staticCoordinates() {
        var stc = new StaticCoordinates();
        stc.setPosition(new Vector3Q());
        return stc;
    }

    /**
     * Builds the volume entity the way {@code SceneModule.createNewProceduralVolumeGalaxy} does, with
     * the components the export reads.
     */
    private static Entity volumeEntity(String name,
                                       Entity parent,
                                       GalaxyAttributes[] fields) {
        var e = new Entity();
        // Assign the names directly: setName() resolves the localised name through I18n, which no
        // unit test has.
        var base = new Base();
        base.names = new String[] {name};
        base.ct = new ComponentTypes(ComponentType.Galaxies);
        e.add(base);

        var body = new Body();
        // The size is a diameter already in internal units, as the volume shader expects.
        body.sizeIsRadiusFlag = false;
        body.sizeInUnitsFlag = true;
        body.setSize(2.0 * 10 * Constants.KPC_TO_U);
        body.cameraCollision = false;
        body.labelColor = new float[] {0, 0, 0, 0};
        e.add(body);

        var graph = new GraphNode();
        graph.parent = parent;
        e.add(graph);

        var coords = new Coordinates();
        coords.coordinates = staticCoordinates();
        e.add(coords);

        var fade = new Fade();
        fade.setFadeObjectName("A test galaxy");
        fade.setFadeOut(new double[] {5000, 10000});
        e.add(fade);

        var focus = new Focus();
        focus.focusable = false;
        e.add(focus);

        var render = new Render();
        render.halfResolutionBuffer = false;
        e.add(render);

        var volume = new Volume();
        volume.vertexShader = "shader/pbr.vertex.glsl";
        volume.fragmentShader = "shader/volume.galaxy.fragment.glsl";
        volume.setIterations(new int[] {50, 70, 90, 110});
        volume.fields = fields;
        e.add(volume);

        return e;
    }

    private static GalaxyAttributes[] fields() {
        var emissive = new GalaxyAttributes();
        emissive.type = GalaxyAttributes.ChannelType.GAS;
        emissive.distribution = GalaxyAttributes.Distribution.SPIRAL_LOG;
        emissive.intensity = 1.4f;

        var dust = new GalaxyAttributes();
        dust.type = GalaxyAttributes.ChannelType.DUST;
        dust.distribution = GalaxyAttributes.Distribution.SPIRAL;
        dust.occlusionThreshold = 0.03f;
        dust.noiseMask = 0.8f;

        return new GalaxyAttributes[] {emissive, dust};
    }

    private static JsonValue volumeJson() {
        var parent = billboardEntity("A test galaxy");
        var volume = volumeEntity("A test galaxy (VOLUME)", parent, fields());
        return new GalaxyGenerator().convertVolumeToJson(volume);
    }

    @Test
    public void theVolumeEntityNamesItsParentAndSizesItselfByRadius() {
        var json = volumeJson();

        Assert.assertEquals("Volume", json.getString("archetype"));
        Assert.assertEquals("A test galaxy (VOLUME)", json.getString("name"));
        Assert.assertEquals("A test galaxy", json.getString("parent"));
        Assert.assertEquals("Galaxies", json.getString("componentType"));
        // The body size is a 20 kpc diameter; sizePc is a radius, so half of it. Same key and same
        // meaning as the billboard half, which is what keeps the two halves at the same extent.
        Assert.assertEquals(10000.0, json.getDouble("sizePc"), 1e-3);
        Assert.assertNull(json.get("diameterPc"));
        Assert.assertFalse(json.getBoolean("cameraCollision"));
        Assert.assertFalse(json.getBoolean("focusable"));
    }

    @Test
    public void theVolumeHalfSpanMatchesTheBillboardHalf() {
        var parent = billboardEntity("A test galaxy");
        var billboardJson = new GalaxyGenerator().convertToJson(parent);
        var volumeJson = new GalaxyGenerator().convertVolumeToJson(volumeEntity("A test galaxy (VOLUME)", parent, fields()));

        Assert.assertEquals(billboardJson.getDouble("sizePc"), volumeJson.getDouble("sizePc"), 1e-3);
    }

    @Test
    public void theVolumeExportsItsReferenceSystemTransform() {
        var volume = volumeEntity("A test galaxy (VOLUME)", billboardEntity("A test galaxy"), fields());
        var trf = new RefSysTransform();
        trf.setTransformMatrix(new Matrix4D(new float[] {0.5f, 0.5f, 0.5f, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}));
        volume.add(trf);

        var json = new GalaxyGenerator().convertVolumeToJson(volume);
        var matrix = json.get("transformMatrix");
        Assert.assertNotNull("the volume has to carry the tilt of the galaxy it belongs to", matrix);
        Assert.assertEquals(16, matrix.size);
        Assert.assertEquals(1.0, matrix.get(5).asDouble(), 1e-6);
    }

    @Test
    public void everyTopLevelKeyIsOneTheLoaderKnows() {
        var components = attributeMap();
        // The volume entity is a Volume archetype, so its keys resolve through those components.
        Set<String> known = new HashSet<>();
        for (String c : List.of("Base", "Body", "GraphNode", "Render", "Coordinates", "Fade", "Focus", "Model", "Volume", "RefSysTransform")) {
            known.addAll(attributesOf(components, c));
        }

        for (JsonValue v = volumeJson().child; v != null; v = v.next) {
            if (SKIPPED.contains(v.name)) {
                continue;
            }
            Assert.assertTrue("the exporter writes '" + v.name + "', which no component declares", known.contains(v.name));
        }
    }

    @Test
    public void everyFieldKeyHasASetterInGalaxyAttributes() {
        var fields = GalaxyGenerator.fieldsToJson(fields());
        Assert.assertEquals(2, fields.size);
        assertSetters(GalaxyAttributes.class, fields.get(0));
        assertSetters(GalaxyAttributes.class, fields.get(1));
    }

    @Test
    public void theFieldsKeepTheirChannelAndTheirShape() {
        var fields = GalaxyGenerator.fieldsToJson(fields());
        Assert.assertEquals(GalaxyAttributes.class.getName(), fields.get(0).getString("impl"));

        var gas = fields.get(0);
        Assert.assertEquals("GAS", gas.getString("type"));
        Assert.assertEquals("SPIRAL_LOG", gas.getString("distribution"));
        Assert.assertEquals(1.4, gas.getDouble("intensity"), 1e-6);

        var dust = fields.get(1);
        Assert.assertEquals("DUST", dust.getString("type"));
        Assert.assertEquals(0.03, dust.getDouble("occlusionThreshold"), 1e-6);
        Assert.assertEquals(0.8, dust.getDouble("noiseMask"), 1e-6);
        // A complete field: the defaults of the generator today are not the ones of tomorrow.
        Assert.assertEquals(12, dust.get("baseColors").size);
        Assert.assertEquals(2, dust.get("eccentricity").size);
        Assert.assertEquals(3, dust.get("scale").size);
        Assert.assertNotNull(dust.get("laneRough"));
        // Zero means "the default" for these two, and one octave is not three.
        Assert.assertNull(dust.get("noiseMaskOctaves"));
        Assert.assertNull(gas.get("noiseMaskFreq"));
    }

    @Test
    public void theVolumeKeepsItsShaderAndItsIterationCount() {
        // Flat keys, as the attribute map declares them for the Volume component.
        var json = volumeJson();
        Assert.assertEquals("shader/volume.galaxy.fragment.glsl", json.getString("fragmentShader"));
        Assert.assertTrue(json.getBoolean("writeDepth"));
        Assert.assertEquals(4, json.get("iterations").size);
        Assert.assertEquals(110, json.get("iterations").get(3).asInt());
    }

    @Test
    public void theFadeDistancesAreWrittenInParsecs() {
        // The component stores them in internal units; writing them raw multiplies them by PC_TO_U
        // again on load, which puts the fade an entire galaxy-size too far.
        var fade = volumeJson().get("fadeOut");
        Assert.assertEquals(5000.0, fade.get(0).asDouble(), 1e-3);
        Assert.assertEquals(10000.0, fade.get(1).asDouble(), 1e-3);
    }

    @Test
    public void aVolumeWithoutFieldsEmitsNoFieldsBlock() {
        var parent = billboardEntity("A test galaxy");
        var json = new GalaxyGenerator().convertVolumeToJson(volumeEntity("A test galaxy (VOLUME)", parent, null));
        Assert.assertEquals(null, json.get("fields"));
    }

    @Test
    public void theHybridDescriptorHasBothObjects() {
        var parent = billboardEntity("A test galaxy");
        var volume = volumeEntity("A test galaxy (VOLUME)", parent, fields());
        var json = new GalaxyGenerator().convertToHybridJson(parent, volume);

        var objects = json.get("objects");
        Assert.assertEquals(2, objects.size);
        Assert.assertEquals("Volume", objects.get(1).getString("archetype"));
        // The billboard half is written by the same function the billboard window uses.
        Assert.assertEquals("BillboardGroup", objects.get(0).getString("archetype"));
    }

    @Test
    public void theDescriptorIsValidJson() {
        var parent = billboardEntity("A test galaxy");
        var volume = volumeEntity("A test galaxy (VOLUME)", parent, fields());
        var text = new GalaxyGenerator().convertToHybridJson(parent, volume).toJson(OutputType.json);
        // Round trip: what is printed has to parse.
        var parsed = new JsonReader().parse(text);
        Assert.assertEquals(2, parsed.get("objects").size);
    }

    @Test
    public void wholeNumbersAreExportedAsDoubles() {
        var parent = billboardEntity("A test galaxy");
        var volume = volumeEntity("A test galaxy (VOLUME)", parent, fields());
        var text = GalaxyGenerator.toJsonString(new GalaxyGenerator().convertToHybridJson(parent, volume));
        var objects = new JsonReader().parse(text).get("objects");
        // Written as integers these come back as int[]/Long, and no setPositionPc(int[]) or
        // setSizePc(Long) exists: the attribute is dropped on load.
        Assert.assertTrue(objects.get(0).get("coordinates").get("positionPc").child.isDouble());
        Assert.assertTrue(objects.get(1).get("sizePc").isDouble());
    }
}
