package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.Celestial;
import gaiasky.scene.component.Magnitude;
import gaiasky.scene.component.Size;
import gaiasky.util.Constants;
import gaiasky.util.color.ColorUtils;

/**
 * Initializes star and particle entities.
 */
public class StarInitializationSystem extends IteratingSystem {

    public StarInitializationSystem(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Body body = Mapper.body.get(entity);
        Size size = Mapper.size.get(entity);
        Celestial celestial = Mapper.celestial.get(entity);
        Magnitude magnitude = Mapper.magnitude.get(entity);

        setDerivedAttributes(body, size, celestial, magnitude);
        size.radius = body.size * Constants.STAR_SIZE_FACTOR;
        size.modelDistance = 172.4643429 * size.radius;
    }

    private static final float DISC_FACTOR = 1.5f;
    /**
     * Sets the derived attributes of this star or particle entity.
     */
    protected void setDerivedAttributes(Body body, Size size, Celestial celestial, Magnitude magnitude) {
        double flux = Math.pow(10, -magnitude.absmag / 2.5f);
        setRGB(celestial.colorbv, body, celestial);

        // Calculate size - This contains arbitrary boundary values to make
        // things nice on the render side
        body.size = (float) (Math.min((Math.pow(flux, 0.5f) * Constants.PC_TO_U * 0.16f), 1e9f) / DISC_FACTOR);
        size.computedSize = 0;
    }

    /**
     * Sets the color
     *
     * @param bv B-V color index
     */
    protected void setRGB(float bv, Body body, Celestial celestial) {
        if (body.cc == null)
            body.cc = ColorUtils.BVtoRGB(bv);
        setColor2Data(body, celestial);
    }

    protected void setColor2Data(Body body, Celestial celestial) {
        final float plus = .1f;
        celestial.ccPale = new float[] { Math.min(1, body.cc[0] + plus), Math.min(1, body.cc[1] + plus), Math.min(1, body.cc[2] + plus) };
    }
}
