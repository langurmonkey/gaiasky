/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.update;

import com.badlogic.gdx.scenes.scene2d.Event;

import java.time.Instant;

public class VersionCheckEvent extends Event {
    private final String tag;
    private final Instant tagTime;
    private final boolean failed;

    public VersionCheckEvent(boolean falied) {
        this.tag = null;
        this.tagTime = null;
        this.failed = falied;
    }

    public VersionCheckEvent(String tag, Instant tagTime) {
        this.tag = tag;
        this.tagTime = tagTime;
        this.failed = false;
    }

    public String getTag() {
        return tag;
    }

    public Instant getTagTime() {
        return tagTime;
    }

    public boolean isFailed() {
        return failed;
    }
}
