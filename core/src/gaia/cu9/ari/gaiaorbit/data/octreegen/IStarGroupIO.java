/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data.octreegen;

import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IStarGroupIO {

    void writeParticles(Array<AbstractPositionEntity> list, OutputStream out);

    Array<AbstractPositionEntity> readParticles(InputStream in) throws FileNotFoundException;
}
