package gaiasky.data.attitude;

import gaiasky.util.gaia.IAttitude;

import java.util.Date;

/**
 * Attitude server interface.
 */
public interface IAttitudeServer {
    IAttitude getAttitude(final Date date);
}
