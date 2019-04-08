/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;

/**
 * Interface all network checkers must comply.
 *
 * @author tsagrista
 */
public interface INetworkChecker {

    void start();

    boolean executing();

    void setFocus(IFocus focus);

    void doNotify();

    void stopExecution();

    void setParameters(Table table, Skin skin, float pad);
}
