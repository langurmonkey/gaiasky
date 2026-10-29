/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.BlendMode;
import gaiasky.render.ComponentTypes;
import gaiasky.render.gdx.graphics.VolumeTexture;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.record.MaterialComponent;
import gaiasky.scene.record.ModelComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the {@code Volume} child of a procedural galaxy, the entity that raymarches the generated
 * density field.
 * <p>
 * Same arrangement as the Milky Way's own volume: the billboard group stays the parent (and keeps the
 * label, the focus and the transform), and the volume is a child at the same position and size, so both
 * share the object's transform without duplicating it. Nothing here talks to GL; the caller has already
 * generated the textures and is responsible for running the scene's initialize and set-up passes.
 */
public final class GalaxyVolumeEntity {

    /** Volume shader of a galaxy, as used by the Milky Way's own volume. **/
    public static final String FRAGMENT_SHADER = "shader/volume.galaxy.fragment.glsl";
    public static final String VERTEX_SHADER = "shader/pbr.vertex.glsl";

    /** Name suffix of the volume child. **/
    public static final String NAME_SUFFIX = " (volume)";

    private GalaxyVolumeEntity() {
    }

    /**
     * Creates the volume child of a galaxy.
     *
     * @param scene     The scene, for its archetypes.
     * @param parent    The galaxy's billboard group.
     * @param emissive  The generated gas+bulge volume, or null for none.
     * @param occlusive The generated dust volume, or null for none.
     *
     * @return The child, with its parent name set, or null if there is nothing to render.
     */
    public static Entity create(Scene scene, Entity parent, VolumeTexture emissive, VolumeTexture occlusive) {
        if (emissive == null && occlusive == null) {
            return null;
        }
        var archetype = scene.archetypes().get("Volume");
        var entity = archetype.createEntity();

        var parentBase = Mapper.base.get(parent);
        var parentBody = Mapper.body.get(parent);
        var parentCoord = Mapper.coordinates.get(parent);
        var parentFade = Mapper.fade.get(parent);
        var parentRender = Mapper.render.get(parent);

        // Identity, inherited from the group: same transform, same size, same fading.
        var base = Mapper.base.get(entity);
        base.setName(parentBase.getName() + NAME_SUFFIX);
        base.setComponentType(ComponentTypes.ComponentType.Galaxies);

        var body = Mapper.body.get(entity);
        body.setSize((double) parentBody.size);
        body.cameraCollision = false;

        var coord = Mapper.coordinates.get(entity);
        coord.coordinates = parentCoord.coordinates;

        var fade = Mapper.fade.get(entity);
        if (parentFade.fadeIn != null) {
            fade.fadeIn = parentFade.fadeIn.cpy();
        }
        if (parentFade.fadeOut != null) {
            fade.fadeOut = parentFade.fadeOut.cpy();
        }
        fade.fadePositionObjectName = parentFade.fadePositionObjectName;

        var render = Mapper.render.get(entity);
        render.halfResolutionBuffer = parentRender.halfResolutionBuffer;

        var graph = Mapper.graph.get(entity);
        graph.setParent(parentBase.getName());

        var focus = Mapper.focus.get(entity);
        focus.focusable = false;

        // Reference-system transform: the child must be expressed in the same frame as its parent,
        // otherwise the field is rotated away from the object it belongs to. The Milky Way's volume
        // declares the same transformName as its billboard group for exactly this reason.
        var parentTransform = Mapper.transform.get(parent);
        var transform = Mapper.transform.get(entity);
        if (parentTransform != null && parentTransform.transformName != null) {
            transform.setTransformName(parentTransform.transformName);
        }

        // Volume: the galaxy raymarcher, and the box the field lives in.
        var volume = Mapper.volume.get(entity);
        volume.vertexShader = VERTEX_SHADER;
        volume.fragmentShader = FRAGMENT_SHADER;

        // Model: a unit sphere, as the Milky Way volume uses. The raymarcher computes its own entry and
        // exit points from the box, so the mesh only has to enclose the box.
        var model = Mapper.model.get(entity);
        var modelComponent = new ModelComponent();
        model.model = modelComponent;
        modelComponent.setModelType("sphere");
        Map<String, Object> params = new HashMap<>();
        // Types matter: ModelCache casts these entries, and the JSON loader hands over Long and
        // Double. An Integer quality is a ClassCastException at model creation time.
        params.put("diameter", 1.0);
        params.put("flip", true);
        params.put("quality", 30L);
        modelComponent.setParams(params);
        // Emission adds: a volume is light, not a surface.
        modelComponent.setBlendMode(BlendMode.ADDITIVE);
        modelComponent.setUseColor(true);
        modelComponent.setCulling(false);

        // Material: dust is the only slot that occludes, and only where it is dense enough to read as a
        // lane; the emissive slot never hides anything behind it.
        var mtc = new MaterialComponent();
        mtc.setVolumeDepthThreshold(new double[]{0.0, 0.015});
        if (emissive != null) {
            mtc.setGeneratedVolume(0, emissive);
        }
        if (occlusive != null) {
            mtc.setGeneratedVolume(1, occlusive);
        }
        modelComponent.setMaterial(mtc);

        return entity;
    }
}
