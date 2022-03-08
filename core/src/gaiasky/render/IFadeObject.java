package gaiasky.render;

import gaiasky.util.math.Vector2d;

public interface IFadeObject {
    /**
     * Gets the fade in distances.
     * @return The fade in distances in internal units.
     */
    public Vector2d getFadeIn();
    /**
     * Sets the near and far fade in distances.
     * @param nearPc Near fade in distance in parsecs.
     * @param farPc Far fade in distance in parsecs.
     */
    public void setFadeIn(double nearPc, double farPc);

    /**
     * Gets the fade out distances.
     * @return The fade out distances in internal units.
     */
    public Vector2d getFadeOut();

    /**
     * Sets the near and far fade out distances.
     * @param nearPc Near fade out distance in parsecs.
     * @param farPc Far fade out distance in parsecs.
     */
    public void setFadeOut(double nearPc, double farPc);
}
