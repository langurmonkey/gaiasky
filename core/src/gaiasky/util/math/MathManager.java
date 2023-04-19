/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Settings;

public class MathManager implements IObserver {

    public static MathManager instance;
    private final Trigonometry trigonometry;
    private final FastTrigonometry fastTrigonometry;
    public ITrigonometry trigonometryInterface;

    MathManager(boolean highAccuracy) {
        trigonometry = new Trigonometry();
        fastTrigonometry = new FastTrigonometry();

        trigonometryInterface = highAccuracy ? trigonometry : fastTrigonometry;

        EventManager.instance.subscribe(this, Event.HIGH_ACCURACY_CMD);
    }

    public static void initialize() {
        initialize(Settings.settings.data.highAccuracy);
    }

    public static void initialize(boolean highAccuracy) {
        if (instance == null)
            instance = new MathManager(highAccuracy);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.HIGH_ACCURACY_CMD) {
            boolean highAcc = (Boolean) data[0];
            trigonometryInterface = highAcc ? trigonometry : fastTrigonometry;
        }

    }

}
