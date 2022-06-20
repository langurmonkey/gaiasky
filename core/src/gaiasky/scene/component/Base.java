package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsd;

import java.util.Locale;

public class Base implements Component {
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
     * Force to render the label of this entity,
     * bypassing the solid angle check
     */
    public boolean forceLabel = false;

    /**
     * Is this just a copy?
     */
    public boolean copy = false;

    /**
     * Has this been updated at least once?
     */
    public boolean initialUpdate = false;


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
                        int idx = addName(localizedName);
                        localizedNameIndex = idx;
                    } else {
                        // Update it
                        setName(localizedName, localizedNameIndex);
                    }
                }
            }
        }
    }
    public void setCt(String ct) {
        this.ct = new ComponentTypes();
        if (!ct.isEmpty())
            this.ct.set(ComponentType.valueOf(ct).ordinal());
    }

    public void setCt(String[] cts) {
        this.ct = new ComponentTypes();
        for (String s : cts) {
            if (!s.isEmpty()) {
                this.ct.set(ComponentType.valueOf(s).ordinal());
            }
        }
    }

    public boolean isVisible() {
        return this.visible || msSinceStateChange() <= Settings.settings.scene.fadeMs;
    }

    /**
     * Computes the elapsed number of milliseconds since the last visibility state change
     * for the given base component.
     * @return The elapsed time [ms] since the last visibility state change.
     */
    public long msSinceStateChange() {
        return (long) (GaiaSky.instance.getT() * 1000f) - lastStateChangeTimeMs;
    }

    /**
     * Gets the visibility opacity factor for this base component.
     * @return The visibility opacity factor.
     */
    public float getVisibilityOpacityFactor() {
        long msSinceStateChange = msSinceStateChange();

        // Fast track
        if (msSinceStateChange > Settings.settings.scene.fadeMs)
            return visible ? 1 : 0;

        // Fading
        float opacity = MathUtilsd.lint(msSinceStateChange, 0, Settings.settings.scene.fadeMs, 0, 1);
        if (!visible) {
            opacity = 1 - opacity;
        }
        return opacity;
    }
}
