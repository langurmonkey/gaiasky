package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class Text implements Component {

    public float labelFactor;
    public float labelMax;
    public float textScale = -1;

    public void setLabelFactor(Double labelFactor) {
        this.labelFactor = labelFactor.floatValue();
    }

    public void setLabelMax(Double labelMax) {
        this.labelMax = labelMax.floatValue();
    }

    public void setTextScale(Double textScale) {
        this.textScale = textScale.floatValue();
    }
}
