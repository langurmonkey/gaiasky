/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen;

import gaiasky.scenegraph.AbstractPositionEntity;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface IStarGroupIO {

    void writeParticles(List<AbstractPositionEntity> list, OutputStream out);
    void writeParticles(List<AbstractPositionEntity> list, OutputStream out, boolean compat);

    List<AbstractPositionEntity> readParticles(InputStream in) throws FileNotFoundException;
    List<AbstractPositionEntity> readParticles(InputStream in, boolean compat) throws FileNotFoundException;
}
