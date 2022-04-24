package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import org.lwjgl.system.CallbackI.V;

public class Celestial implements Component {
    /** Threshold over fov factor **/
    public float thOverFactor;

    /** Name to pull info from wikipedia **/
    public String wikiname;

    /** Color for billboard rendering **/
    public float[] ccBillboard;

    /** B-V color index **/
    public float colorbv;

    /** Component alpha mirror **/
    public float compalpha;

    public void setColorbv(Double colorbv) {
        this.colorbv = colorbv.floatValue();
    }
}
