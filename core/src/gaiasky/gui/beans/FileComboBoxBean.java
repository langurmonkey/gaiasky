/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.beans;

import java.nio.file.Path;

public class FileComboBoxBean {
    public String name;
    public String file;

    public FileComboBoxBean(Path file) {
        super();
        this.name = file.getFileName().toString();
        this.file = file.toString();
    }

    static String stripExtension(String str) {
        // Handle null case specially.
        if (str == null)
            return null;

        // Get position of last '.'.
        int pos = str.lastIndexOf(".");

        // If there wasn't any '.' just return the string as is.
        if (pos == -1)
            return str;

        // Otherwise return the string, up to the dot.
        return str.substring(0, pos);
    }

    @Override
    public String toString() {
        return name;
    }

}