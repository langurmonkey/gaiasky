package gaiasky.util.gdx.model.data;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

public class OwnModelMaterial {
    public enum MaterialType {
        Lambert,
        Phong,
        PBR
    }

    public String id;

    public MaterialType type;

    public Color ambient;
    public Color diffuse;
    public Color specular;
    public Color emissive;
    public Color reflection;
    public Color metallic;
    public Color roughness;

    public float shininess;
    public float opacity = 1.f;

    public Array<OwnModelTexture> textures;
}
