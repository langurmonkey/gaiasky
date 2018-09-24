package gaia.cu9.ari.gaiaorbit.desktop.util;

import java.time.Instant;

import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormat;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;

/**
 * Basic worker which listens to logs in the event system
 * and prints them on the console. Creating an instance of this takes care of
 * all logging needs through {@link gaia.cu9.ari.gaiaorbit.util.Logger}.
 * @author tsagrista
 *
 */
public class LogWriter implements IObserver {
    private static final String TAG_SEPARATOR = " - ";

    private IDateFormat df;
    public Array<String> logMessages;

    public LogWriter() {
        this.df = new DesktopDateFormat(I18n.locale, true, true);
        this.logMessages = new Array<String>();
        EventManager.instance.subscribe(this, Events.JAVA_EXCEPTION, Events.POST_NOTIFICATION);
    }

    @Override
    public void notify(Events event, Object... data) {
        Instant now = Instant.now();
        String message = "";
        switch (event) {
        case JAVA_EXCEPTION:
            if (data.length == 1) {
                message = df.format(now) + TAG_SEPARATOR + ((Throwable) data[0]).getLocalizedMessage();
            } else {
                message = df.format(now) + TAG_SEPARATOR + (String) data[1] + TAG_SEPARATOR + ((Throwable) data[0]).getLocalizedMessage();
            }
            logMessages.add(message);
            System.out.println(message);
            break;
        case POST_NOTIFICATION:
            for (int i = 0; i < data.length; i++) {
                message += (String) data[i];
                if (i < data.length - 1 && !(i == data.length - 2 && data[data.length - 1] instanceof Boolean)) {
                    message += TAG_SEPARATOR;
                }

            }
            message = df.format(now) + TAG_SEPARATOR + message;
            logMessages.add(message);
            System.out.println(message);
            break;
        default:
            break;
        }

    }
}
