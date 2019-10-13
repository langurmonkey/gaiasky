/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.format;

import java.time.Instant;

public interface IDateFormat {
    public String format(Instant date);

    public Instant parse(String date);
}
