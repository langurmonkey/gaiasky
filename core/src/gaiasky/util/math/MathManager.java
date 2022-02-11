/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Settings;

public class MathManager implements IObserver {

    public static MathManager instance;

    public static void initialize() {
        initialize(Settings.settings.data.highAccuracy);
    }

    public static void initialize(boolean highAccuracy) {
        if (instance == null)
            instance = new MathManager(highAccuracy);
    }

    public ITrigonometry trigonometryInterface;

    private final Trigonometry trigonometry;
    private final FastTrigonometry fastTrigonometry;

    MathManager(boolean highAccuracy) {
        trigonometry = new Trigonometry();
        fastTrigonometry = new FastTrigonometry();

        trigonometryInterface = highAccuracy ? trigonometry : fastTrigonometry;

        EventManager.instance.subscribe(this, Event.HIGH_ACCURACY_CMD);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.HIGH_ACCURACY_CMD) {
            boolean highAcc = (Boolean) data[0];
            trigonometryInterface = highAcc ? trigonometry : fastTrigonometry;
        }

    }

}
