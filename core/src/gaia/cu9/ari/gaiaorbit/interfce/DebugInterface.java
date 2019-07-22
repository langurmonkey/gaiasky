/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.TextUtils;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnSlider;

public class DebugInterface extends Table implements IObserver, IGuiInterface {
    private OwnLabel debugRuntime, debugUsed, debugFree, debugAlloc, debugMax,  debugObjectsDisplay, debugObjectsLoaded, debugOcObserved, debugOcQueue, debugSamp, fps, spf, device;
    private OwnSlider queueStatus;
    private int previousQueueSize = 0, currentQueueMax = 0;
    /** Lock object for synchronization **/
    private Object lock;
    private Skin skin;

    private INumberFormat fpsFormatter, spfFormatter, memFormatter, timeFormatter;

    public DebugInterface(Skin skin, Object lock) {
        super(skin);
        this.setBackground("table-bg");

        this.skin = skin;
        float pad05 = 5f * GlobalConf.SCALE_FACTOR;
        float pad10 = 10f * GlobalConf.SCALE_FACTOR;
        float pad20 = 20f * GlobalConf.SCALE_FACTOR;
        float pad40 = 40f * GlobalConf.SCALE_FACTOR;

        pad(pad05);

        // Formatters
        fpsFormatter = NumberFormatFactory.getFormatter("#.00");
        spfFormatter = NumberFormatFactory.getFormatter("#.00##");
        memFormatter = NumberFormatFactory.getFormatter("#000.00");
        timeFormatter = NumberFormatFactory.getFormatter("00");

        /* FPS */
        fps = new OwnLabel("", skin, "hud-big");
        fps.setColor(skin.getColor("green"));
        add(fps).colspan(2).right().padBottom(pad05);
        row();

        /* SPF */
        spf = new OwnLabel("", skin, "hud-med");
        add(spf).colspan(2).right().padBottom(pad10);
        row();

        /* GRAPHICS DEVICE */
        HorizontalGroup deviceGroup = new HorizontalGroup();
        deviceGroup.space(pad05);
        String glDevice = Gdx.gl.glGetString(GL20.GL_RENDERER);
        String glDeviceShort = TextUtils.capString(glDevice, 30);
        device = new OwnLabel(glDeviceShort, skin, "hud-big");
        device.setColor(skin.getColor("blue"));
        deviceGroup.addActor(device);
        if(glDevice.length() != glDeviceShort.length()){
            OwnImageButton deviceTooltip = new OwnImageButton(skin, "tooltip");
            deviceTooltip.addListener(new TextTooltip(glDevice, skin));
            device.addListener(new TextTooltip(glDevice, skin));
            deviceGroup.addActor(deviceTooltip);
        }
        add(deviceGroup).colspan(2).right().padBottom(pad40);
        row();

        /* RUNNING TIME */
        debugRuntime = new OwnLabel("", skin, "hud");
        Label runTimeLabel = new OwnLabel("Run time", skin, "hud-big");
        runTimeLabel.setColor(skin.getColor("highlight"));
        add(runTimeLabel).right().padRight(pad10).padBottom(pad20);
        add(debugRuntime).right().padBottom(pad20);
        row();

        /* MEMORY */
        debugUsed = new OwnLabel("", skin, "hud");
        debugFree = new OwnLabel("", skin, "hud");
        debugAlloc = new OwnLabel("", skin, "hud");
        debugMax = new OwnLabel("", skin, "hud");

        Table debugMemoryTable = new Table(skin);
        debugMemoryTable.add(new OwnLabel("Used:", skin, "hud")).right().padRight(pad10).padBottom(pad05);
        debugMemoryTable.add(debugUsed).right().padRight(pad10).padBottom(pad05);
        debugMemoryTable.add(new OwnLabel("Free:", skin, "hud")).right().padRight(pad10).padBottom(pad05);
        debugMemoryTable.add(debugFree).right().padBottom(pad05).row();
        debugMemoryTable.add(new OwnLabel("Alloc:", skin, "hud")).right().padRight(pad10);
        debugMemoryTable.add(debugAlloc).right().padRight(pad10);
        debugMemoryTable.add(new OwnLabel("Max:", skin, "hud")).right().padRight(pad10);
        debugMemoryTable.add(debugMax).right();

        Label memoryLabel = new OwnLabel("RAM [MB]", skin, "hud-big");
        memoryLabel.setColor(skin.getColor("highlight"));
        add(memoryLabel).right().padRight(pad10).padBottom(pad20);
        add(debugMemoryTable).right().padBottom(pad20);
        row();


        /* OBJECTS */
        debugObjectsDisplay = new OwnLabel("", skin, "hud");
        debugObjectsLoaded = new OwnLabel("", skin, "hud");

        Table objectsTable = new Table(skin);
        objectsTable.add(new OwnLabel("On display:", skin, "hud")).right().padRight(pad10).padBottom(pad05);
        objectsTable.add(debugObjectsDisplay).right().padBottom(pad05).row();
        objectsTable.add(new OwnLabel("Loaded:", skin, "hud")).right().padRight(pad10);
        objectsTable.add(debugObjectsLoaded).right();

        Label objectsLabel = new OwnLabel("Objects", skin, "hud-big");
        objectsLabel.setColor(skin.getColor("highlight"));
        add(objectsLabel).right().padRight(pad10).padBottom(pad20);
        add(objectsTable).right().padBottom(pad20);
        row();

        /* OCTANTS */
        debugOcObserved = new OwnLabel("", skin, "hud");
        debugOcQueue = new OwnLabel("", skin, "hud");

        Table octantsTable = new Table(skin);
        octantsTable.add(new OwnLabel("Observed:", skin, "hud")).right().padRight(pad10).padBottom(pad05);
        octantsTable.add(debugOcObserved).right().padBottom(pad05).row();
        octantsTable.add(new OwnLabel("Queue:", skin, "hud")).right().padRight(pad10);

        HorizontalGroup dbg4 = new HorizontalGroup();
        dbg4.space(pad05);
        queueStatus = new OwnSlider(0, 100, 1, false, skin, "default-horizontal");
        queueStatus.setWidth(70f * GlobalConf.SCALE_FACTOR);
        queueStatus.setValue(0);
        dbg4.addActor(debugOcQueue);
        dbg4.addActor(queueStatus);
        octantsTable.add(dbg4).right().row();

        Label lodLabel = new OwnLabel("LOD", skin, "hud-big");
        lodLabel.setColor(skin.getColor("highlight"));
        add(lodLabel).right().padRight(pad10).padBottom(pad20);
        add(octantsTable).right().padBottom(pad20);
        row();

        /* SAMP */
        debugSamp = new OwnLabel("", skin, "hud");
        Label sampLabel = new OwnLabel("SAMP", skin, "hud-big");
        sampLabel.setColor(skin.getColor("highlight"));
        add(sampLabel).right().padRight(pad10);
        add(debugSamp).right();
        row();

        pack();


        this.setVisible(GlobalConf.program.SHOW_DEBUG_INFO);
        this.lock = lock;
        EventManager.instance.subscribe(this, Events.DEBUG_TIME, Events.DEBUG_RAM, Events.DEBUG_OBJECTS, Events.DEBUG_QUEUE, Events.FPS_INFO, Events.SHOW_DEBUG_CMD, Events.SAMP_INFO);
    }

    private void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
    }


    private Color getColor(double v, double max){
        if(v > max * 0.95){
            return skin.getColor("red");
        } else if( v > max * 0.9) {
            return skin.getColor("orange");
        } else if (v > max * 0.8) {
            return skin.getColor("blue");
        } else {
            return skin.getColor("green");
        }
    }

    @Override
    public void notify(Events event, Object... data) {
        synchronized (lock) {
            switch (event) {
            case DEBUG_TIME:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0 && data[0] != null) {
                    // Double with run time
                    Double runTime = (Double) data[0];
                    debugRuntime.setText(getRunTimeString(runTime));
                }
                break;

            case DEBUG_RAM:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0 && data[0] != null) {
                    // Doubles (MB):
                    // used/free/total/max
                    Double used = (Double) data[0];
                    Double free = (Double) data[1];
                    Double alloc = (Double) data[2];
                    Double max = (Double) data[3];
                    debugUsed.setText(memFormatter.format(used));
                    debugUsed.setColor(getColor(used, alloc));
                    debugFree.setText(memFormatter.format(free));
                    debugAlloc.setText(memFormatter.format(alloc));
                    debugAlloc.setColor(getColor(alloc, max));
                    debugMax.setText(memFormatter.format(max));
                }
                break;
            case DEBUG_OBJECTS:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0 && data[0] != null) {
                    Integer display = (Integer) data[0];
                    Integer loaded = (Integer) data[1];
                    debugObjectsDisplay.setText(display);
                    debugObjectsLoaded.setText(loaded);
                }
                break;
            case DEBUG_QUEUE:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0 && data[0] != null) {
                    int observed = (Integer)data[0];
                    int queueSize = (Integer) data[1];
                    // Text
                    debugOcObserved.setText(observed);
                    debugOcQueue.setText(queueSize);

                    // Slider
                    if (previousQueueSize < queueSize) {
                        // Reset status
                        currentQueueMax = queueSize;
                    }
                    queueStatus.setValue(queueSize * 100f / currentQueueMax);
                    previousQueueSize = queueSize;
                }
                break;
            case FPS_INFO:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0 && data[0] != null) {
                    double dfps = (Float) data[0];
                    double dspf = 1000 / dfps;
                    fps.setText(fpsFormatter.format(dfps).concat(" FPS"));
                    spf.setText(spfFormatter.format(dspf).concat(" ms"));
                }
                break;
            case SAMP_INFO:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0 && data[0] != null) {
                    debugSamp.setText((String) data[0]);
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
