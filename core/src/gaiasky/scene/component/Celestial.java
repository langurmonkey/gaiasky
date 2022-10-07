package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;

public class Celestial implements Component, ICopy {

    /** Name to pull info from wikipedia **/
    public String wikiname;

    /** Red, green and blue colors and their revamped cousins **/
    public float[] colorPale;

    /** B-V color index **/
    public float colorBv;

    /** Inner radius for billboard rendering **/
    public double innerRad;

    public void setColorBv(Double colorBv) {
        this.colorBv = colorBv.floatValue();
    }

    public void setColorbv(Double colorBv) {
        setColorBv(colorBv);
    }

    @Override
    public Component getCopy(Engine engine) {
        return engine.createComponent(this.getClass());
    }
}
