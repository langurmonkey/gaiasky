/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Archetype;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;

import java.util.Locale;

public class Base implements Component, ICopy {

    /** Reference to the archetype used to create this entity, if any. **/
    public Archetype archetype;

    /**
     * The internal identifier
     **/
    public long id = -1;

    /**
     * The name(s) of the node, if any.
     */
    public String[] names;

    /**
     * The index of the localized name in the {@link #names} array.
     */
    public int localizedNameIndex = 0;

    /**
     * Time of last visibility change in milliseconds
     */
    public long lastStateChangeTimeMs = 0;

    /**
     * The ownOpacity value (alpha)
     */
    public float opacity = 1f;

    /**
     * Component types, for managing visibility
     */
    public ComponentTypes ct;

    /**
     * Flag indicating whether the object has been computed in this step.
     */
    public boolean computed = true;

    /**
     * Is this node visible?
     */
    public boolean visible = true;

    /**
     * Is this just a copy?
     */
    public boolean copy = false;

    /**
     * Has this been updated at least once?
     */
    public boolean initialUpdate = false;

    /**
     * Extra attributes.
     */
    public ObjectMap<String, Object> attributes;

    public String getName() {
        return names != null ? names[0] : null;
    }

    public void setName(String name) {
        if (names != null)
            names[0] = name;
        else
            names = new String[] { name };
        updateLocalizedName();
    }

    public void setNames(String[] names) {
        this.names = names;
        updateLocalizedName();
    }

    /**
     * Sets the given index in the names list to the given name.
     * If the index is out of bounds, nothing happens.
     *
     * @param name  The new name.
     * @param index The index in the names list.
     */
    public void setName(String name, int index) {
        if (names != null && index >= 0 && index < names.length) {
            names[index] = name;
        }
    }

    public boolean hasName(String candidate) {
        return hasName(candidate, false);
    }

    public boolean hasName(String candidate, boolean matchCase) {
        if (names == null) {
            return false;
        } else {
            for (String name : names) {
                if (matchCase) {
                    if (name.equals(candidate))
                        return true;
                } else {
                    if (name.equalsIgnoreCase(candidate))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds a name to the list of names.
     *
     * @param name The name.
     *
     * @return The index of the added name.
     */
    public int addName(String name) {
        if (!hasName(name)) {
            if (names != null) {
                // Extend array
                String[] newNames = new String[names.length + 1];
                System.arraycopy(names, 0, newNames, 0, names.length);
                newNames[names.length] = name;
                int idx = names.length;
                names = newNames;
                return idx;
            } else {
                names = new String[] { name };
                return 0;
            }
        } else {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equalsIgnoreCase(name))
                    return i;
            }
            return -1;
        }
    }

    public String getLocalizedName() {
        if (localizedNameIndex >= 0 && names.length > localizedNameIndex) {
            return names[localizedNameIndex];
        } else {
            return getName();
        }
    }

    public void updateLocalizedName() {
        if (names != null && names.length > 0) {
            String base = names[0].toLowerCase(Locale.ROOT).replace(' ', '_');
            if (I18n.hasObject(base)) {
                String localizedName = I18n.obj(base);
                if (!localizedName.equalsIgnoreCase(names[localizedNameIndex])) {
                    if (localizedNameIndex == 0) {
                        // Add localized name to list
                        localizedNameIndex = addName(localizedName);
                    } else {
                        // Update it
                        setName(localizedName, localizedNameIndex);
                    }
                }
            }
        }
    }

    public void setAltname(String altname) {
        setAltName(altname);
    }

    public void setAltName(String altname) {
        this.addName(altname);
    }

    public boolean hasCt(ComponentType ct) {
        return ct != null && this.ct.isEnabled(ct);
    }

    public void setComponentType(String ct) {
        setCt(ct);
    }

    public void setCt(String ct) {
        this.ct = new ComponentTypes();
        if (!ct.isEmpty())
            this.ct.set(ComponentType.valueOf(ct).ordinal());
    }

    public void setComponentTypes(String[] cts) {
        setCt(cts);
    }

    public void setComponentType(String[] cts) {
        setCt(cts);
    }

    public void setCt(String[] cts) {
        this.ct = new ComponentTypes();
        for (String s : cts) {
            if (!s.isEmpty()) {
                this.ct.set(ComponentType.valueOf(s).ordinal());
            }
        }
    }

    public void setComponentType(ComponentType ct) {
        this.ct = new ComponentTypes(ct);
    }

    public boolean isVisible() {
        return this.visible || msSinceStateChange() <= Settings.settings.scene.fadeMs;
    }

    /**
     * Computes the elapsed number of milliseconds since the last visibility state change
     * for the given base component.
     *
     * @return The elapsed time [ms] since the last visibility state change.
     */
    public long msSinceStateChange() {
        return (long) (GaiaSky.instance.getT() * 1000f) - lastStateChangeTimeMs;
    }

    /**
     * Gets the visibility opacity factor for this base component.
     *
     * @return The visibility opacity factor.
     */
    public float getVisibilityOpacityFactor() {
        long msSinceStateChange = msSinceStateChange();

        // Fast track
        if (msSinceStateChange > Settings.settings.scene.fadeMs)
            return visible ? 1 : 0;

        // Fading
        float opacity = MathUtilsDouble.lint(msSinceStateChange, 0, Settings.settings.scene.fadeMs, 0, 1);
        if (!visible) {
            opacity = 1 - opacity;
        }
        return opacity;
    }

    public void addExtraAttribute(String name, Object value) {
        if (attributes == null) {
            attributes = new ObjectMap<>();
        }
        attributes.put(name, value);
    }

    public ObjectMap<String, Object> getExtraAttributes() {
        return attributes;
    }

    public boolean hasExtraAttributes() {
        return attributes != null && !attributes.isEmpty();
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.copy = true;
        copy.names = names;
        copy.archetype = archetype;
        copy.visible = visible;
        copy.ct = ct;
        copy.id = id;
        copy.lastStateChangeTimeMs = lastStateChangeTimeMs;
        copy.localizedNameIndex = localizedNameIndex;
        copy.opacity = opacity;
        return copy;
    }

}
