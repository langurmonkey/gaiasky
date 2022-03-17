/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.Matrix4Attribute;
import gaiasky.util.gdx.shader.attribute.Vector3Attribute;
import gaiasky.util.math.Vector3b;

import java.util.Map;
import java.util.Set;

public class VelocityBufferComponent {

    public void doneLoading(Map<String, Material> materials) {
        Set<String> keys = materials.keySet();
        for (String key : keys) {
            Material mat = materials.get(key);
            setUpVelocityBufferMaterial(mat);
        }
    }

    public void doneLoading(Material mat) {
        setUpVelocityBufferMaterial(mat);
    }

    public void setUpVelocityBufferMaterial(Array<Material> materials) {
        for (Material material : materials) {
            setUpVelocityBufferMaterial(material);
        }
    }

    public void setUpVelocityBufferMaterial(Material mat) {
        mat.set(new Matrix4Attribute(Matrix4Attribute.PrevProjView, new Matrix4()));
        mat.set(new Vector3Attribute(Vector3Attribute.DCamPos, new Vector3()));
    }

    public void removeVelocityBufferMaterial(Material mat) {
        mat.remove(Matrix4Attribute.PrevProjView);
        mat.remove(Vector3Attribute.DCamPos);
    }

    public void updateVelocityBufferMaterial(Material material, ICamera cam) {
        if (material.get(Matrix4Attribute.PrevProjView) == null) {
            setUpVelocityBufferMaterial(material);
        }

        // Previous projection view matrix
        ((Matrix4Attribute) material.get(Matrix4Attribute.PrevProjView)).value.set(cam.getPreviousProjView());

        // Camera position difference
        Vector3 dCamPos = ((Vector3Attribute) material.get(Vector3Attribute.DCamPos)).value;
        Vector3b dp = cam.getPreviousPos();
        Vector3b p = cam.getPos();
        dCamPos.set(dp.x.subtract(p.x).floatValue(), dp.y.subtract(p.y).floatValue(), dp.z.subtract(p.z).floatValue());
    }

    public boolean hasVelocityBuffer(Material mat) {
        return mat.get(Matrix4Attribute.PrevProjView) != null;
    }
}
