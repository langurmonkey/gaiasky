/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntIntMap;
import gaiasky.util.Bits;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.model.IntMeshPart;
import gaiasky.util.gdx.shader.attribute.Attributes;

public abstract class BaseIntShader implements IntShader {
    private final Array<String> uniforms = new Array<>();
    private final Array<Validator> validators = new Array<>();
    private final Array<Setter> setters = new Array<>();
    private final IntArray globalUniforms = new IntArray();
    private final IntArray localUniforms = new IntArray();
    private final IntIntMap attributes = new IntIntMap();
    private final IntArray tempArray = new IntArray();
    private final Attributes combinedAttributes = new Attributes();
    public ExtShaderProgram program;
    public RenderContext context;
    public Camera camera;
    private int[] locations;
    private IntMesh currentMesh;

    /**
     * Register an uniform which might be used by this shader. Only possible prior to the call to init().
     *
     * @return The ID of the uniform to use in this shader.
     */
    public int register(String alias, Validator validator, Setter setter) {
        if (locations != null)
            throw new GdxRuntimeException("Cannot register an uniform after initialization");
        int existing = getUniformID(alias);
        if (existing >= 0) {
            validators.set(existing, validator);
            setters.set(existing, setter);
            return existing;
        }
        uniforms.add(alias);
        validators.add(validator);
        setters.add(setter);
        return uniforms.size - 1;
    }

    public int register(String alias, Validator validator) {
        return register(alias, validator, null);
    }

    public int register(String alias, Setter setter) {
        return register(alias, null, setter);
    }

    public int register(String alias) {
        return register(alias, null, null);
    }

    public int register(Uniform uniform, Setter setter) {
        return register(uniform.alias, uniform, setter);
    }

    public int register(Uniform uniform) {
        return register(uniform, null);
    }

    /** @return the ID of the input or negative if not available. */
    public int getUniformID(String alias) {
        int n = uniforms.size;
        for (int i = 0; i < n; i++)
            if (uniforms.get(i).equals(alias))
                return i;
        return -1;
    }

    /** @return The input at the specified id. */
    public String getUniformAlias(int id) {
        return uniforms.get(id);
    }

    /** Initialize this shader, causing all registered uniforms/attributes to be fetched. */
    public void init(ExtShaderProgram program, IntRenderable renderable) {
        if (locations != null) {
            throw new GdxRuntimeException("Already initialized");
        }
        if (!program.isCompiled()) {
            throw new GdxRuntimeException("Shader is not compiled: " + program.getName());
        }
        this.program = program;

        int n = uniforms.size;
        locations = new int[n];
        for (int i = 0; i < n; i++) {
            String input = uniforms.get(i);
            Validator validator = validators.get(i);
            Setter setter = setters.get(i);
            if (validator != null && !validator.validate(this, i, renderable))
                locations[i] = -1;
            else {
                locations[i] = program.fetchUniformLocation(input, false);
                if (locations[i] >= 0 && setter != null) {
                    if (setter.isGlobal(this, i))
                        globalUniforms.add(i);
                    else
                        localUniforms.add(i);
                }
            }
            if (locations[i] < 0) {
                validators.set(i, null);
                setters.set(i, null);
            }
        }
        if (renderable != null) {
            VertexAttributes attrs = renderable.meshPart.mesh.getVertexAttributes();
            int c = attrs.size();
            for (int i = 0; i < c; i++) {
                VertexAttribute attr = attrs.get(i);
                int location = program.getAttributeLocation(attr.alias);
                if (location >= 0)
                    attributes.put(attr.getKey(), location);
            }
        }
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        this.camera = camera;
        this.context = context;
        program.begin();
        currentMesh = null;
        for (int u, i = 0; i < globalUniforms.size; ++i)
            if (setters.get(u = globalUniforms.get(i)) != null)
                setters.get(u).set(this, u, null, null);
    }

    private final int[] getAttributeLocations(VertexAttributes attrs) {
        tempArray.clear();
        int n = attrs.size();
        for (int i = 0; i < n; i++) {
            tempArray.add(attributes.get(attrs.get(i).getKey(), -1));
        }
        tempArray.shrink();
        return tempArray.items;
    }

    @Override
    public void render(IntRenderable renderable) {
        if (renderable.worldTransform.det3x3() == 0)
            return;
        combinedAttributes.clear();
        if (renderable.environment != null)
            combinedAttributes.set(renderable.environment);
        if (renderable.material != null)
            combinedAttributes.set(renderable.material);
        render(renderable, combinedAttributes);
    }

    public void render(IntRenderable renderable, Attributes combinedAttributes) {
        for (int u, i = 0; i < localUniforms.size; ++i)
            if (setters.get(u = localUniforms.get(i)) != null)
                setters.get(u).set(this, u, renderable, combinedAttributes);
        if (currentMesh != renderable.meshPart.mesh) {
            if (currentMesh != null)
                currentMesh.unbind(program, tempArray.items);
            currentMesh = renderable.meshPart.mesh;
            currentMesh.bind(program, getAttributeLocations(renderable.meshPart.mesh.getVertexAttributes()));
        }
        renderMesh(program, renderable.meshPart);
    }

    /**
     * If necessary, override.
     *
     * @param program The shader program.
     * @param meshPart The mesh part.
     */
    public void renderMesh(ExtShaderProgram program, IntMeshPart meshPart) {
        meshPart.mesh.render(program, meshPart.primitiveType, meshPart.offset, meshPart.size, false);
    }

    @Override
    public void end() {
        if (currentMesh != null) {
            currentMesh.unbind(program, tempArray.items);
            currentMesh = null;
        }
        program.end();
    }

    @Override
    public void dispose() {
        program = null;
        uniforms.clear();
        validators.clear();
        setters.clear();
        localUniforms.clear();
        globalUniforms.clear();
        locations = null;
    }

    /** Whether this IntShader instance implements the specified uniform, only valid after a call to init(). */
    public final boolean has(int inputID) {
        return inputID >= 0 && inputID < locations.length && locations[inputID] >= 0;
    }

    public final int loc(int inputID) {
        return (inputID >= 0 && inputID < locations.length) ? locations[inputID] : -1;
    }

    public final boolean set(int uniform, Matrix4 value) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformMatrix(locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, Matrix3 value) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformMatrix(locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, Vector3 value) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformf(locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, Vector2 value) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformf(locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, Color value) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformf(locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, float value) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformf(locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, float v1, float v2) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformf(locations[uniform], v1, v2);
        return true;
    }

    public final boolean set(int uniform, float v1, float v2, float v3) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformf(locations[uniform], v1, v2, v3);
        return true;
    }

    public final boolean set(int uniform, float v1, float v2, float v3, float v4) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformf(locations[uniform], v1, v2, v3, v4);
        return true;
    }

    public final boolean set(int uniform, int value) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformi(locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, int v1, int v2) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformi(locations[uniform], v1, v2);
        return true;
    }

    public final boolean set(int uniform, int v1, int v2, int v3) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformi(locations[uniform], v1, v2, v3);
        return true;
    }

    public final boolean set(int uniform, int v1, int v2, int v3, int v4) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformi(locations[uniform], v1, v2, v3, v4);
        return true;
    }

    public final boolean set(int uniform, TextureDescriptor textureDesc) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformi(locations[uniform], context.textureBinder.bind(textureDesc));
        return true;
    }

    public final boolean set(int uniform, GLTexture texture) {
        if (locations[uniform] < 0)
            return false;
        program.setUniformi(locations[uniform], context.textureBinder.bind(texture));
        return true;
    }

    public interface Validator {
        /** @return True if the input is valid for the renderable, false otherwise. */
        boolean validate(BaseIntShader shader, int inputID, IntRenderable renderable);
    }

    public interface Setter {
        /** @return True if the uniform only has to be set once per render call, false if the uniform must be set for each renderable. */
        boolean isGlobal(BaseIntShader shader, int inputID);

        void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes);
    }

    public abstract static class GlobalSetter implements Setter {
        @Override
        public boolean isGlobal(BaseIntShader shader, int inputID) {
            return true;
        }
    }

    public abstract static class LocalSetter implements Setter {
        @Override
        public boolean isGlobal(BaseIntShader shader, int inputID) {
            return false;
        }
    }

    public record Uniform(String alias, Bits materialMask, Bits environmentMask, Bits overallMask) implements Validator {

        public Uniform(String alias, Bits materialMask, Bits environmentMask) {
                this(alias, materialMask, environmentMask, Bits.empty(128));
            }

            public Uniform(String alias, int overallIndex) {
                this(alias, Bits.empty(128), Bits.empty(128), Bits.indices(overallIndex));
            }

            public Uniform(String alias) {
                this(alias, Bits.empty(128), Bits.empty(128));
            }

            public boolean validate(BaseIntShader shader, int inputID, IntRenderable renderable) {
                Bits matFlags = (renderable != null && renderable.material != null) ? renderable.material.getMask() : Bits.empty();
                Bits envFlags = (renderable != null && renderable.environment != null) ? renderable.environment.getMask() : Bits.empty();
                return ((matFlags.copy().and(materialMask)).equals(materialMask)) && ((envFlags.copy().and(environmentMask)).equals(environmentMask))
                        && (((matFlags.copy().or(envFlags)).and(overallMask)).equals(overallMask));
            }
        }
}
