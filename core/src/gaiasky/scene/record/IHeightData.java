package gaiasky.scene.record;

/**
 * Interface to be implemented by any kind of height data.
 */
public interface IHeightData {

    /**
     * Gets the height normalized in [0,1] for the given UV coordinates.
     * @param u The U coordinate.
     * @param v The V coordinate.
     * @return The height value in [0,1].
     */
    double getNormalizedHeight(double u, double v);

}
