/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;

public class ExistingLocationValidator extends CallbackValidator {

    Entity parent;

    public ExistingLocationValidator(Entity parent) {
        this.parent = parent;
    }

    @Override
    protected boolean validateLocal(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        var graph = Mapper.graph.get(parent);
        var child = graph.getChildByName(value);
        return child != null && Mapper.loc.has(child);
    }
}
