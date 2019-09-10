/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.math;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;

public class MathManager implements IObserver {

    public static MathManager instance;

    public static void initialize() {
        initialize(GlobalConf.data.HIGH_ACCURACY_POSITIONS);
    }

    public static void initialize(boolean highAccuracy) {
        if (instance == null)
            instance = new MathManager(highAccuracy);
    }

    public ITrigonometry trigo;

    private Trigonometry trigonometry;
    private FastTrigonometry fastTrigonometry;

    MathManager(boolean highAccuracy) {
        trigonometry = new Trigonometry();
        fastTrigonometry = new FastTrigonometry();

        trigo = highAccuracy ? trigonometry : fastTrigonometry;

        EventManager.instance.subscribe(this, Events.HIGH_ACCURACY_CMD);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case HIGH_ACCURACY_CMD:
            boolean highAcc = (Boolean) data[0];
            trigo = highAcc ? trigonometry : fastTrigonometry;
            break;
        default:
            break;
        }

    }

}
