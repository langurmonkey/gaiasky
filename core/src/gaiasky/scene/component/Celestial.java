package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class Celestial implements Component {
    /** Threshold over fov factor **/
    public float thOverFactor;

    /** Name to pull info from wikipedia **/
    public String wikiname;

    /** Color for billboard rendering **/
    public float[] colorBillboard;

    /** Red, green and blue colors and their revamped cousins **/
    public float[] colorPale;

    /** B-V color index **/
    public float colorbv;

    public void setColorbv(Double colorbv) {
        this.colorbv = colorbv.floatValue();
    }
}
