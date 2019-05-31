/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.validator;

import gaia.cu9.ari.gaiaorbit.scenegraph.Loc;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;

public class ExistingLocationValidator extends CallbackValidator {

    SceneGraphNode parent;

    public ExistingLocationValidator(SceneGraphNode parent) {
        this.parent = parent;
    }

    @Override
    protected boolean validateLocal(String value) {
        return value != null && !value.isEmpty() && parent.getChildByNameAndType(value, Loc.class) != null;
    }

}
