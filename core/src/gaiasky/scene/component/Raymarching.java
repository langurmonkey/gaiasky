package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class Raymarching implements Component {

    public String raymarchingShader;
    public boolean isOn = false;

    public void setShader(String shader) {
        this.setRaymarchingShader(shader);
    }

    public void setRaymarchingShader(String shader) {
        this.raymarchingShader = shader;
    }
}
