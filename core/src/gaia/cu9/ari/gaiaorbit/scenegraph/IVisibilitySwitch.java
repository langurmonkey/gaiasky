/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

public interface IVisibilitySwitch {
    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String name);

    boolean isVisible();

    void setVisible(boolean visible);
}
