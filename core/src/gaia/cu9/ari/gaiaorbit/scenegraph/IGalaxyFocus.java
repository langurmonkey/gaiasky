package gaia.cu9.ari.gaiaorbit.scenegraph;

/**
 * Interface that galaxies must implement
 */
public interface IGalaxyFocus extends IFocus {

    /**
     * Gets the alternative name of this galaxy, if any
     * @return The alternative name
     */
    public String getAltname();
}
