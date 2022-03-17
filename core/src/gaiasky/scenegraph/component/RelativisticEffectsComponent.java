/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.FloatAttribute;
import gaiasky.util.gdx.shader.attribute.Matrix3Attribute;
import gaiasky.util.gdx.shader.attribute.Vector3Attribute;
import gaiasky.util.gdx.shader.attribute.Vector4Attribute;
import gaiasky.util.gravwaves.RelativisticEffectsManager;

import java.util.Map;
import java.util.Set;

public class RelativisticEffectsComponent {

    public RelativisticEffectsComponent() {
        super();
    }

    public void doneLoading(Map<String, Material> materials) {
        Set<String> keys = materials.keySet();
        for (String key : keys) {
            Material mat = materials.get(key);
            setUpRelativisticEffectsMaterial(mat);
            setUpGravitationalWavesMaterial(mat);
        }
    }

    public void doneLoading(Material mat) {
        setUpRelativisticEffectsMaterial(mat);
        setUpGravitationalWavesMaterial(mat);
    }

    public void setUpRelativisticEffectsMaterial(Array<Material> materials) {
        for (Material material : materials) {
            setUpRelativisticEffectsMaterial(material);
        }
    }

    public void setUpRelativisticEffectsMaterial(Material mat) {
        mat.set(new FloatAttribute(FloatAttribute.Vc, 0f));
        mat.set(new Vector3Attribute(Vector3Attribute.VelDir, new Vector3()));
    }

    public void removeRelativisticEffectsMaterial(Array<Material> materials) {
        for (Material material : materials) {
            removeRelativisticEffectsMaterial(material);
        }
    }

    public void removeRelativisticEffectsMaterial(Material mat) {
        mat.remove(FloatAttribute.Vc);
        mat.remove(Vector3Attribute.VelDir);
    }

    public void setUpGravitationalWavesMaterial(Array<Material> materials) {
        for (Material material : materials) {
            setUpGravitationalWavesMaterial(material);
        }
    }

    public void setUpGravitationalWavesMaterial(Material mat) {
        //mat.set(new Vector4Attribute(Vector4Attribute.Hterms, new float[4]));
        //mat.set(new Vector3Attribute(Vector3Attribute.Gw, new Vector3()));
        //mat.set(new Matrix3Attribute(Matrix3Attribute.Gwmat3, new Matrix3()));
        //mat.set(new FloatAttribute(FloatAttribute.Ts, 0f));
        //mat.set(new FloatAttribute(FloatAttribute.Omgw, 0f));
    }

    public void removeGravitationalWavesMaterial(Array<Material> materials) {
        for (Material material : materials) {
            removeGravitationalWavesMaterial(material);
        }
    }

    public void removeGravitationalWavesMaterial(Material mat) {
        //mat.remove(Vector4Attribute.Hterms);
        //mat.remove(Vector3Attribute.Gw);
        //mat.remove(Matrix3Attribute.Gwmat3);
        //mat.remove(FloatAttribute.Ts);
        //mat.remove(FloatAttribute.Omgw);
    }

    public void updateRelativisticEffectsMaterial(Material material, ICamera camera) {
        updateRelativisticEffectsMaterial(material, camera, -1);
    }

    public void updateRelativisticEffectsMaterial(Material material, ICamera camera, float vc) {
        if (material.get(FloatAttribute.Vc) == null) {
            setUpRelativisticEffectsMaterial(material);
        }
        RelativisticEffectsManager rem = RelativisticEffectsManager.getInstance();
        if (vc != -1) {
            // v/c
            ((FloatAttribute) material.get(FloatAttribute.Vc)).value = vc;
        } else {

            // v/c
            ((FloatAttribute) material.get(FloatAttribute.Vc)).value = rem.vc;
        }
        // Velocity direction
        ((Vector3Attribute) material.get(Vector3Attribute.VelDir)).value.set(rem.velDir);
    }

    public void updateGravitationalWavesMaterial(Material material) {
        if (material.get(Vector4Attribute.Hterms) == null) {
            setUpGravitationalWavesMaterial(material);
        }
        RelativisticEffectsManager rem = RelativisticEffectsManager.getInstance();
        // hterms
        ((Vector4Attribute) material.get(Vector4Attribute.Hterms)).value = rem.hterms;

        // gw
        ((Vector3Attribute) material.get(Vector3Attribute.Gw)).value.set(rem.gw);

        // gwmat3
        ((Matrix3Attribute) material.get(Matrix3Attribute.Gwmat3)).value.set(rem.gwmat3);

        // ts
        ((FloatAttribute) material.get(FloatAttribute.Ts)).value = rem.gwtime;

        // omgw
        ((FloatAttribute) material.get(FloatAttribute.Omgw)).value = rem.omgw;
    }

    public boolean hasGravitationalWaves(Material mat) {
        //return mat.get(Vector4Attribute.Hterms) != null;
        return false;
    }

    public boolean hasRelativisticEffects(Material mat) {
        return mat.get(Vector3Attribute.VelDir) != null;
    }
}
