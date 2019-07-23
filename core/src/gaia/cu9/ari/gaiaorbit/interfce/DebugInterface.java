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
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.TextUtils;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnSlider;

public class DebugInterface extends Table implements IObserver, IGuiInterface {
    private OwnLabel debugRuntime, debugRAMUsed, debugRAMFree, debugRAMAlloc, debugRAMTotal,  debugVRAMUsed, debugVRAMTotal, debugObjectsDisplay, debugObjectsLoaded, debugOcObserved, debugOcQueue, debugSamp, fps, spf, device;
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
        float pad05 = 5f;
        float pad10 = 10f;
        float pad20 = 20f;
        float pad40 = 40f;

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
        Table timeTable = new Table(skin);
        timeTable.add(debugRuntime);
        Label runTimeLabel = new OwnLabel(I18n.txt("gui.debug.runtime"), skin, "hud-big");
        runTimeLabel.setColor(skin.getColor("highlight"));
        add(timeTable).right().padRight(pad10).padBottom(pad20);
        add(runTimeLabel).left().padBottom(pad20);
        row();

        /* MEMORY */
        debugRAMUsed = new OwnLabel("", skin, "hud");
        debugRAMFree = new OwnLabel("", skin, "hud");
        debugRAMAlloc = new OwnLabel("", skin, "hud");
        debugRAMTotal = new OwnLabel("", skin, "hud");

        Table debugRAMTable = new Table(skin);
        debugRAMTable.add(new OwnLabel(I18n.txt("gui.debug.ram.used"), skin, "hud")).right().padRight(pad10).padBottom(pad05);
        debugRAMTable.add(debugRAMUsed).right().padBottom(pad05).row();
        debugRAMTable.add(new OwnLabel(I18n.txt("gui.debug.ram.free"), skin, "hud")).right().padRight(pad10).padBottom(pad05);
        debugRAMTable.add(debugRAMFree).right().padBottom(pad05).row();
        debugRAMTable.add(new OwnLabel(I18n.txt("gui.debug.ram.alloc"), skin, "hud")).right().padRight(pad10);
        debugRAMTable.add(debugRAMAlloc).right().row();
        debugRAMTable.add(new OwnLabel(I18n.txt("gui.debug.ram.total"), skin, "hud")).right().padRight(pad10);
        debugRAMTable.add(debugRAMTotal).right();

        Label memoryLabel = new OwnLabel(I18n.txt("gui.debug.ram"), skin, "hud-big");
        memoryLabel.setColor(skin.getColor("highlight"));
        add(debugRAMTable).right().padRight(pad10).padBottom(pad20);
        add(memoryLabel).left().padBottom(pad20);
        row();


        /* VMEMORY */
        debugVRAMUsed = new OwnLabel("", skin, "hud");
        debugVRAMTotal = new OwnLabel("", skin, "hud");

        Table debugVRAMTable = new Table(skin);
        debugVRAMTable.add(new OwnLabel(I18n.txt("gui.debug.vram.used"), skin, "hud")).right().padRight(pad10).padBottom(pad05);
        debugVRAMTable.add(debugVRAMUsed).right().padBottom(pad05).row();
        debugVRAMTable.add(new OwnLabel(I18n.txt("gui.debug.vram.total"), skin, "hud")).right().padRight(pad10).padBottom(pad05);
        debugVRAMTable.add(debugVRAMTotal).right().padBottom(pad05);

        Label vmemoryLabel = new OwnLabel(I18n.txt("gui.debug.vram"), skin, "hud-big");
        vmemoryLabel.setColor(skin.getColor("highlight"));
        add(debugVRAMTable).right().padRight(pad10).padBottom(pad20);
        add(vmemoryLabel).left().padBottom(pad20);
        row();


        /* OBJECTS */
        debugObjectsDisplay = new OwnLabel("", skin, "hud");
        debugObjectsLoaded = new OwnLabel("", skin, "hud");

        Table objectsTable = new Table(skin);
        objectsTable.add(new OwnLabel(I18n.txt("gui.debug.obj.display"), skin, "hud")).right().padRight(pad10).padBottom(pad05);
        objectsTable.add(debugObjectsDisplay).right().padBottom(pad05).row();
        objectsTable.add(new OwnLabel(I18n.txt("gui.debug.obj.loaded"), skin, "hud")).right().padRight(pad10);
        objectsTable.add(debugObjectsLoaded).right();

        Label objectsLabel = new OwnLabel(I18n.txt("gui.debug.obj"), skin, "hud-big");
        objectsLabel.setColor(skin.getColor("highlight"));
        add(objectsTable).right().padRight(pad10).padBottom(pad20);
        add(objectsLabel).left().padBottom(pad20);
        row();

        /* OCTANTS */
        debugOcObserved = new OwnLabel("", skin, "hud");
        debugOcQueue = new OwnLabel("", skin, "hud");
        queueStatus = new OwnSlider(0, 100, 1, false, skin, "default-horizontal");
        queueStatus.setValue(0);

        Table octantsTable = new Table(skin);
        octantsTable.add(new OwnLabel(I18n.txt("gui.debug.lod.observed"), skin, "hud")).right().padRight(pad10).padBottom(pad05);
        octantsTable.add(debugOcObserved).right().padBottom(pad05).row();
        octantsTable.add(new OwnLabel(I18n.txt("gui.debug.lod.queue"), skin, "hud")).right().padRight(pad10).padBottom(pad05);
        octantsTable.add(debugOcQueue).right().padBottom(pad05).row();
        octantsTable.add(queueStatus).center().colspan(2).padTop(pad05);

        Label lodLabel = new OwnLabel(I18n.txt("gui.debug.lod"), skin, "hud-big");
        lodLabel.setColor(skin.getColor("highlight"));
        add(octantsTable).right().padRight(pad10).padBottom(pad20);
        add(lodLabel).left().padBottom(pad20);
        row();

        /* SAMP */
        debugSamp = new OwnLabel("", skin, "hud");
        Table sampTable = new Table(skin);
        sampTable.add(debugSamp);
        Label sampLabel = new OwnLabel(I18n.txt("gui.debug.samp"), skin, "hud-big");
        sampLabel.setColor(skin.getColor("highlight"));
        add(sampTable).right().padRight(pad10);
        add(sampLabel).left();
        row();

        pack();


        this.setVisible(GlobalConf.program.SHOW_DEBUG_INFO);
        this.lock = lock;
        EventManager.instance.subscribe(this, Events.DEBUG_TIME, Events.DEBUG_RAM, Events.DEBUG_VRAM, Events.DEBUG_OBJECTS, Events.DEBUG_QUEUE, Events.FPS_INFO, Events.SHOW_DEBUG_CMD, Events.SAMP_INFO);
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
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0) {
                    // Double with run time
                    Double runTime = (Double) data[0];
                    debugRuntime.setText(getRunTimeString(runTime));
                }
                break;

            case DEBUG_RAM:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0) {
                    // Doubles (MB):
                    // used/free/total/max
                    Double used = (Double) data[0];
                    Double free = (Double) data[1];
                    Double alloc = (Double) data[2];
                    Double total = (Double) data[3];
                    String unit = " " + I18n.txt("gui.debug.ram.unit");
                    debugRAMUsed.setText(memFormatter.format(used) + unit);
                    debugRAMUsed.setColor(getColor(used, alloc));
                    debugRAMFree.setText(memFormatter.format(free) + unit);
                    debugRAMAlloc.setText(memFormatter.format(alloc) + unit);
                    debugRAMAlloc.setColor(getColor(alloc, total));
                    debugRAMTotal.setText(memFormatter.format(total) + unit);
                }
                break;
            case DEBUG_VRAM:
                if(GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0){
                    Double used = (Double) data[0];
                    Double total = (Double) data[1];
                    if(used <= 0 || total <= 0){
                        debugVRAMUsed.setText(I18n.txt("gui.debug.na"));
                        debugVRAMTotal.setText(I18n.txt("gui.debug.na"));
                    }else {
                        String unit = " " + I18n.txt("gui.debug.vram.unit");
                        debugVRAMUsed.setText(memFormatter.format(used) + unit);
                        debugVRAMUsed.setColor(getColor(used, total));
                        debugVRAMTotal.setText(memFormatter.format(total) + unit);
                    }
                }
                break;
            case DEBUG_OBJECTS:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0) {
                    Integer display = (Integer) data[0];
                    Integer loaded = (Integer) data[1];
                    debugObjectsDisplay.setText(display);
                    debugObjectsLoaded.setText(loaded);
                }
                break;
            case DEBUG_QUEUE:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0) {
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
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0) {
                    double dfps = (Float) data[0];
                    double dspf = 1000 / dfps;
                    fps.setText(fpsFormatter.format(dfps).concat(" " + I18n.txt("gui.debug.fps")));
                    spf.setText(spfFormatter.format(dspf).concat(" " + I18n.txt("gui.debug.ms")));
                }
                break;
            case SAMP_INFO:
                if (GlobalConf.program.SHOW_DEBUG_INFO && data.length > 0) {
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
