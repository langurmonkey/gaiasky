package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;

public class DebugInterface extends Table implements IObserver, IGuiInterface {
    private OwnLabel debug1, debug2, debug3, debug4, debugBuffers, fps, spf, device;
    /** Lock object for synchronization **/
    private Object lock;

    private INumberFormat fpsFormatter, spfFormatter, memFormatter, timeFormatter;

    public DebugInterface(Skin skin, Object lock) {
        super(skin);
        float spacing = 10 * GlobalConf.SCALE_FACTOR;
        
        
        // Formatters
        fpsFormatter = NumberFormatFactory.getFormatter("#.00");
        spfFormatter = NumberFormatFactory.getFormatter("#.00##");
        memFormatter = NumberFormatFactory.getFormatter("#000.00");
        timeFormatter = NumberFormatFactory.getFormatter("00");

        fps = new OwnLabel("", skin, "hud-big");
        add(fps).right();
        row();

        spf = new OwnLabel("", skin, "hud-med");
        add(spf).right();
        row();
        
        device = new OwnLabel(Gdx.gl.glGetString(GL20.GL_RENDERER), skin, "hud-big");
        add(device).right().padTop(spacing);
        row();
        
        debug1 = new OwnLabel("", skin, "hud");
        add(debug1).right().padTop(spacing);
        row();

        debug2 = new OwnLabel("", skin, "hud");
        add(debug2).right();
        row();

        debug3 = new OwnLabel("", skin, "hud");
        add(debug3).right();
        row();

        debug4 = new OwnLabel("", skin, "hud");
        add(debug4).right();
        row();

        debugBuffers = new OwnLabel("", skin, "hud");
        add(debugBuffers).right();
        row();

        this.setVisible(GlobalConf.program.SHOW_DEBUG_INFO);
        this.lock = lock;
        EventManager.instance.subscribe(this, Events.DEBUG1, Events.DEBUG2, Events.DEBUG3, Events.DEBUG4, Events.DEBUG_BUFFERS, Events.FPS_INFO, Events.SHOW_DEBUG_CMD);
    }

    private void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void notify(Events event, Object... data) {
        synchronized (lock) {
            switch (event) {
            case DEBUG1:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0 && data[0] != null) {
                    // Double with run time
                    Double runTime = (Double) data[0];
                    debug1.setText("Run time: " + getRunTimeString(runTime));
                }
                break;

            case DEBUG2:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0 && data[0] != null) {
                    // Doubles (MB):
                    // used/free/total/max
                    Double used = (Double) data[0];
                    Double free = (Double) data[1];
                    Double total = (Double) data[2];
                    Double max = (Double) data[3];
                    debug2.setText("Mem[MB] - used: " + memFormatter.format(used) + "  free: " + memFormatter.format(free) + "  total: " + memFormatter.format(total) + "  max: " + memFormatter.format(max));
                }

                break;

            case DEBUG3:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0 && data[0] != null)
                    debug3.setText((String) data[0]);
                break;
            case DEBUG4:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0 && data[0] != null)
                    debug4.setText((String) data[0]);
                break;
            case DEBUG_BUFFERS:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0 && data[0] != null)
                    debugBuffers.setText((String) data[0]);
                break;
            case FPS_INFO:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0 && data[0] != null) {
                    double dfps = (Float)data[0];
                    double dspf = 1000 / dfps;
                    fps.setText(fpsFormatter.format(dfps).concat(" FPS"));
                    spf.setText(spfFormatter.format(dspf).concat(" ms"));
                }
                break;
            case SHOW_DEBUG_CMD:
                boolean shw;
                if (data.length >= 1) {
                    shw = (boolean) data[0];
                } else {
                    shw = !this.isVisible();
                }
                GlobalConf.program.SHOW_DEBUG_INFO = shw;
                this.setVisible(GlobalConf.program.SHOW_DEBUG_INFO);
                break;
            default:
                break;
            }
        }
    }

    private String getRunTimeString(Double seconds) {
        double hours = seconds / 3600d;
        double minutes = (seconds % 3600d) / 60d;
        double secs = seconds % 60d;

        return timeFormatter.format(hours) + ":" + timeFormatter.format(minutes) + ":" + timeFormatter.format(secs);
    }

    @Override
    public void dispose() {
        unsubscribe();
    }

}
