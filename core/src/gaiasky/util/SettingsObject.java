/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.utils.Disposable;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * To be implemented by all settings object in the {@link Settings}.
 */
public abstract class SettingsObject implements Cloneable, Disposable {

    /**
     * The parent of this object, if any.
     */
    @JsonIgnore
    protected SettingsObject parent;

    @JsonIgnore
    public boolean isEnabled(){
        return parent != null && parent.isEnabled();
    }

    /**
     * Applies the settings contained in this settings object by sending the required events.
     * To be called when a new settings object is made effective.
     */
    abstract void apply();

    @Override
    public SettingsObject clone() {
        try {
            return (SettingsObject) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @JsonIgnore
    public void setParent(SettingsObject s){
        this.parent = s;
        this.setParentRecursive(s);
    }

    @JsonIgnore
    protected abstract void setParentRecursive(SettingsObject s);

    @JsonIgnore
    protected abstract void setupListeners();
}
