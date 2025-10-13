package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class Volume implements Component {

    public String vertexShader = "assets/shader/pbr.vertex.glsl";
    public String fragmentShader = "assets/shader/volume.fragment.glsl";
    public int key;
}
