/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.iface;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.Link;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextTooltip;

import java.text.DecimalFormat;

public class DebugInterface extends TableGuiInterface implements IObserver {
    private final OwnLabel debugRuntime;
    private final OwnLabel debugRAMUsed;
    private final OwnLabel debugRAMFree;
    private final OwnLabel debugRAMAlloc;
    private final OwnLabel debugRAMTotal;
    private final OwnLabel debugVRAMUsed;
    private final OwnLabel debugVRAMTotal;
    private final OwnLabel threadsRunning;
    private final OwnLabel threadsSize;
    private final OwnLabel debugObjectsDisplay;
    private final OwnLabel debugObjectsLoaded;
    private final OwnLabel debugOcObserved;
    private final OwnLabel debugOcQueue;
    private final OwnLabel debugSamp;
    private final OwnLabel debugDynRes;
    private final OwnLabel fps;
    private final OwnLabel spf;
    /** Lock object for synchronization **/
    private final Object lock;
    private final Skin skin;
    private final Cell<Table> extraCell;
    private final Table extra;
    private final DecimalFormat fpsFormatter;
    private final DecimalFormat spfFormatter;
    private final DecimalFormat memFormatter;
    private final DecimalFormat timeFormatter;
    private boolean maximized;

    public DebugInterface(Skin skin, Object lock) {
        super(skin);
        this.setBackground("table-bg");
        this.maximized = false;

        this.skin = skin;
        float pad05 = 8f;
        float pad10 = 16f;
        float pad20 = 24f;
        float minWidth = 120f;

        pad(pad05);

        // Formatters
        fpsFormatter = new DecimalFormat("#.00");
        spfFormatter = new DecimalFormat("#.00##");
        memFormatter = new DecimalFormat("#000.00");
        timeFormatter = new DecimalFormat("00");

        /* FPS */
        fps = new OwnLabel("", skin, "hud-big");
        fps.setAlignment(Align.right);
        fps.setColor(skin.getColor("green"));
        fps.addListener(new OwnTextTooltip(I18n.msg("gui.debug.fps.info"), skin));
        add(fps).right().minWidth(minWidth).padBottom(pad05);
        row();

        /* SPF */
        spf = new OwnLabel("", skin, "hud-med");
        spf.setAlignment(Align.right);
        spf.addListener(new OwnTextTooltip(I18n.msg("gui.debug.spf.info"), skin));
        add(spf).right().minWidth(minWidth).padBottom(pad05);
        row();

        /* MINIMIZE/MAXIMIZE */
        extra = new Table(skin);

        var toggleSize = new Link(maximized ? "(-)" : "(+)", skin, null);
        var toggleSizeTooltip = new OwnTextTooltip(I18n.msg("gui.maximize.pane"), skin);
        toggleSize.addListener(toggleSizeTooltip);
        toggleSize.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                if (maximized) {
                    // Minimize.
                    maximized = false;
                    extra.addAction(Actions.sequence(
                            Actions.alpha(1f),
                            Actions.fadeOut(Settings.settings.program.ui.getAnimationSeconds()),
                            Actions.run(() -> {
                                extraCell.setActor(null);
                                toggleSize.setText("(+)");
                                toggleSizeTooltip.setText(I18n.msg("gui.maximize.pane"));
                            })
                    ));
                } else {
                    // Maximize.
                    maximized = true;
                    extraCell.setActor(extra);
                    extra.addAction(Actions.sequence(
                            Actions.alpha(0f),
                            Actions.fadeIn(Settings.settings.program.ui.getAnimationSeconds()),
                            Actions.run(() -> {
                                toggleSize.setText("(-)");
                                toggleSizeTooltip.setText(I18n.msg("gui.minimize.pane"));
                            })
                    ));
                }
                pack();
            }
        });

        extraCell = add();
        row();
        if (maximized) {
            extraCell.setActor(extra);
        }
        add(toggleSize).right().row();

        /* GRAPHICS DEVICE */
        final var settings = Settings.settings;
        HorizontalGroup deviceGroup = new HorizontalGroup();
        deviceGroup.space(pad05);
        String glDevice = Gdx.gl.glGetString(GL20.GL_RENDERER);
        String glDeviceShort = TextUtils.capString(glDevice, 30);
        OwnLabel device = new OwnLabel(glDeviceShort, skin, "hud-big");
        device.setColor(skin.getColor("blue"));
        device.addListener(new OwnTextTooltip(glDevice, skin));
        deviceGroup.addActor(device);
        if (glDevice.length() != glDeviceShort.length()) {
            OwnImageButton deviceTooltip = new OwnImageButton(skin, "tooltip");
            deviceTooltip.addListener(new OwnTextTooltip(glDevice, skin));
            deviceGroup.addActor(deviceTooltip);
        }
        extra.add(deviceGroup).colspan(2).right().padBottom(pad05).row();

        if (settings.program.net.master.active) {
            OwnLabel master = new OwnLabel(TextUtils.surroundBrackets(I18n.msg("gui.master.instance")), skin, "hud-big");
            master.setColor(ColorUtils.gYellowC);
            master.addListener(new OwnTextTooltip(I18n.msg("gui.master.instance.tooltip"), skin));

            extra.add(master).colspan(2).right().padBottom(pad10).row();
        }
        if (settings.program.net.slave.active) {
            OwnLabel slave = new OwnLabel(TextUtils.surroundBrackets(I18n.msg("gui.slave.instance")), skin, "hud-big");
            slave.setColor(ColorUtils.gYellowC);
            slave.addListener(new OwnTextTooltip(I18n.msg("gui.slave.instance.tooltip"), skin));

            extra.add(slave).colspan(2).right().padBottom(pad10).row();
        }
        if (settings.program.safeMode) {
            OwnLabel safeMode = new OwnLabel(TextUtils.surroundBrackets(I18n.msg("gui.debug.safemode")), skin, "hud-big");
            safeMode.setColor(ColorUtils.gRedC);
            safeMode.addListener(new OwnTextTooltip(I18n.msg("gui.debug.safemode.tooltip"), skin));

            extra.add(safeMode).colspan(2).right().padBottom(pad10).row();
        }


        /* RUNNING TIME */
        debugRuntime = new OwnLabel("", skin, "hud");
        Table timeTable = new Table(skin);
        timeTable.add(debugRuntime);
        Label runTimeLabel = new OwnLabel(I18n.msg("gui.debug.runtime"), skin, "hud-big");
        runTimeLabel.addListener(new OwnTextTooltip(I18n.msg("gui.debug.runtime.info"), skin));
        runTimeLabel.setColor(skin.getColor("theme"));
        extra.add(timeTable).right().padRight(pad10).padBottom(pad20);
        extra.add(runTimeLabel).left().padBottom(pad20);
        extra.row();

        /* MEMORY */
        debugRAMUsed = new OwnLabel("", skin, "hud");
        debugRAMFree = new OwnLabel("", skin, "hud");
        debugRAMAlloc = new OwnLabel("", skin, "hud");
        debugRAMTotal = new OwnLabel("", skin, "hud");

        Table debugRAMTable = new Table(skin);
        debugRAMTable.add(new OwnLabel(I18n.msg("gui.debug.ram.used"), skin, "hud")).right().padRight(pad10);
        debugRAMTable.add(debugRAMUsed).right().row();
        debugRAMTable.add(new OwnLabel(I18n.msg("gui.debug.ram.free"), skin, "hud")).right().padRight(pad10).padBottom(pad05);
        debugRAMTable.add(debugRAMFree).right().padBottom(pad05).row();
        debugRAMTable.add(new OwnLabel(I18n.msg("gui.debug.ram.alloc"), skin, "hud")).right().padRight(pad10);
        debugRAMTable.add(debugRAMAlloc).right().row();
        debugRAMTable.add(new OwnLabel(I18n.msg("gui.debug.ram.total"), skin, "hud")).right().padRight(pad10);
        debugRAMTable.add(debugRAMTotal).right();

        Label memoryLabel = new OwnLabel(I18n.msg("gui.debug.ram"), skin, "hud-big");
        memoryLabel.addListener(new OwnTextTooltip(I18n.msg("gui.debug.ram.info"), skin));
        memoryLabel.setColor(skin.getColor("theme"));
        extra.add(debugRAMTable).right().padRight(pad10).padBottom(pad20);
        extra.add(memoryLabel).left().padBottom(pad20);
        extra.row();


        /* VIDEO MEMORY */
        debugVRAMUsed = new OwnLabel("", skin, "hud");
        debugVRAMTotal = new OwnLabel("", skin, "hud");

        Table debugVRAMTable = new Table(skin);
        debugVRAMTable.add(new OwnLabel(I18n.msg("gui.debug.vram.used"), skin, "hud")).right().padRight(pad10);
        debugVRAMTable.add(debugVRAMUsed).right().row();
        debugVRAMTable.add(new OwnLabel(I18n.msg("gui.debug.vram.total"), skin, "hud")).right().padRight(pad10);
        debugVRAMTable.add(debugVRAMTotal).right();

        Label vmemoryLabel = new OwnLabel(I18n.msg("gui.debug.vram"), skin, "hud-big");
        vmemoryLabel.addListener(new OwnTextTooltip(I18n.msg("gui.debug.vram.info"), skin));
        vmemoryLabel.setColor(skin.getColor("theme"));
        extra.add(debugVRAMTable).right().padRight(pad10).padBottom(pad20);
        extra.add(vmemoryLabel).left().padBottom(pad20);
        extra.row();

        /* THREADS */
        threadsRunning = new OwnLabel("", skin, "hud");
        threadsSize = new OwnLabel("", skin, "hud");

        Table debugThreadsTable = new Table(skin);
        debugThreadsTable.add(new OwnLabel(I18n.msg("gui.debug.threads.running"), skin, "hud")).right().padRight(pad10);
        debugThreadsTable.add(threadsRunning).right().row();
        debugThreadsTable.add(new OwnLabel(I18n.msg("gui.debug.threads.poolsize"), skin, "hud")).right().padRight(pad10);
        debugThreadsTable.add(threadsSize).right();

        Label threadsLabel = new OwnLabel(I18n.msg("gui.debug.threads"), skin, "hud-big");
        threadsLabel.addListener(new OwnTextTooltip(I18n.msg("gui.debug.threads.info"), skin));
        threadsLabel.setColor(skin.getColor("theme"));
        extra.add(debugThreadsTable).right().padRight(pad10).padBottom(pad20);
        extra.add(threadsLabel).left().padBottom(pad20);
        extra.row();

        /* DYN RES */
        debugDynRes = new OwnLabel("", skin, "hud");
        Label dynResLabel = new OwnLabel(I18n.msg("gui.debug.dynres.short"), skin, "hud-big");
        dynResLabel.setColor(skin.getColor("theme"));
        extra.add(debugDynRes).right().padRight(pad10).padBottom(pad20);
        extra.add(dynResLabel).left().padBottom(pad20);
        extra.row();

        /* OBJECTS */
        debugObjectsDisplay = new OwnLabel("", skin, "hud");
        debugObjectsLoaded = new OwnLabel("", skin, "hud");

        Table objectsTable = new Table(skin);
        objectsTable.add(new OwnLabel(I18n.msg("gui.debug.obj.display"), skin, "hud")).right().padRight(pad10);
        objectsTable.add(debugObjectsDisplay).right().row();
        objectsTable.add(new OwnLabel(I18n.msg("gui.debug.obj.loaded"), skin, "hud")).right().padRight(pad10);
        objectsTable.add(debugObjectsLoaded).right();

        Label objectsLabel = new OwnLabel(I18n.msg("gui.debug.obj"), skin, "hud-big");
        objectsLabel.addListener(new OwnTextTooltip(I18n.msg("gui.debug.obj.info"), skin));
        objectsLabel.setColor(skin.getColor("theme"));
        extra.add(objectsTable).right().padRight(pad10).padBottom(pad20);
        extra.add(objectsLabel).left().padBottom(pad20);
        extra.row();

        /* OCTANTS */
        debugOcObserved = new OwnLabel("", skin, "hud");
        debugOcQueue = new OwnLabel("", skin, "hud");

        Table octantsTable = new Table(skin);
        octantsTable.add(new OwnLabel(I18n.msg("gui.debug.lod.observed"), skin, "hud")).right().padRight(pad10);
        octantsTable.add(debugOcObserved).right().row();
        octantsTable.add(new OwnLabel(I18n.msg("gui.debug.lod.queue"), skin, "hud")).right().padRight(pad10).padBottom(pad05);
        octantsTable.add(debugOcQueue).right().padBottom(pad05).row();

        Label lodLabel = new OwnLabel(I18n.msg("gui.debug.lod"), skin, "hud-big");
        lodLabel.addListener(new OwnTextTooltip(I18n.msg("gui.debug.lod.info"), skin));
        lodLabel.setColor(skin.getColor("theme"));
        extra.add(octantsTable).right().padRight(pad10).padBottom(pad20);
        extra.add(lodLabel).left().padBottom(pad20);
        extra.row();

        /* SAMP */
        debugSamp = new OwnLabel("", skin, "hud");
        Table sampTable = new Table(skin);
        sampTable.add(debugSamp);
        Label sampLabel = new OwnLabel(I18n.msg("gui.debug.samp"), skin, "hud-big");
        sampLabel.addListener(new OwnTextTooltip(I18n.msg("gui.debug.samp.info"), skin));
        sampLabel.setColor(skin.getColor("theme"));
        extra.add(sampTable).right().padRight(pad10);
        extra.add(sampLabel).left();
        extra.row();

        pack();

        this.setVisible(settings.program.debugInfo);
        this.lock = lock;
        EventManager.instance.subscribe(this, Event.DEBUG_TIME, Event.DEBUG_RAM, Event.DEBUG_VRAM, Event.DEBUG_THREADS, Event.DEBUG_OBJECTS, Event.DEBUG_QUEUE, Event.DEBUG_DYN_RES, Event.FPS_INFO, Event.SHOW_DEBUG_CMD, Event.SAMP_INFO);
    }

    private void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    private Color getColor(double v, double max) {
        if (v > max * 0.95) {
            return skin.getColor("red");
        } else if (v > max * 0.9) {
            return skin.getColor("orange");
        } else if (v > max * 0.85) {
            return skin.getColor("yellow");
        } else {
            return skin.getColor("green");
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        synchronized (lock) {
            final boolean debug = Settings.settings.program.debugInfo;
            switch (event) {
            case DEBUG_TIME -> {
                if (debug && data.length > 0) {
                    // Double with run time
                    Double runTime = (Double) data[0];
                    debugRuntime.setText(getRunTimeString(runTime));
                }
            }
            case DEBUG_RAM -> {
                if (debug && data.length > 0) {
                    // Doubles (MB):
                    // used/free/total/max
                    Double used = (Double) data[0];
                    Double free = (Double) data[1];
                    Double alloc = (Double) data[2];
                    Double total = (Double) data[3];
                    String unit = " " + I18n.msg("gui.debug.ram.unit");
                    debugRAMUsed.setText(memFormatter.format(used) + unit);
                    debugRAMUsed.setColor(getColor(used, alloc));
                    debugRAMFree.setText(memFormatter.format(free) + unit);
                    debugRAMAlloc.setText(memFormatter.format(alloc) + unit);
                    debugRAMAlloc.setColor(getColor(alloc, total));
                    debugRAMTotal.setText(memFormatter.format(total) + unit);
                }
            }
            case DEBUG_VRAM -> {
                if (debug && data.length > 0) {
                    Double used = (Double) data[0];
                    Double total = (Double) data[1];
                    if (used <= 0 || total <= 0) {
                        debugVRAMUsed.setText(I18n.msg("gui.debug.na"));
                        debugVRAMTotal.setText(I18n.msg("gui.debug.na"));
                    } else {
                        String unit = " " + I18n.msg("gui.debug.vram.unit");
                        debugVRAMUsed.setText(memFormatter.format(used) + unit);
                        debugVRAMUsed.setColor(getColor(used, total));
                        debugVRAMTotal.setText(memFormatter.format(total) + unit);
                    }
                }
            }
            case DEBUG_THREADS -> {
                if (debug && data.length > 0) {
                    Integer active = (Integer) data[0];
                    Integer poolSize = (Integer) data[1];
                    threadsRunning.setText(active);
                    threadsSize.setText(poolSize);
                }
            }
            case DEBUG_OBJECTS -> {
                if (debug && data.length > 0) {
                    Integer display = (Integer) data[0];
                    Integer loaded = (Integer) data[1];
                    debugObjectsDisplay.setText(display);
                    debugObjectsLoaded.setText(loaded);
                }
            }
            case DEBUG_QUEUE -> {
                if (debug && data.length > 0) {
                    int observed = (Integer) data[0];
                    int queueSize = (Integer) data[1];
                    // Text
                    debugOcObserved.setText(observed);
                    debugOcQueue.setText(queueSize);
                }
            }
            case DEBUG_DYN_RES -> {
                if (debug && data.length > 0) {
                    debugDynRes.setText("L" + data[0] + ": " + fpsFormatter.format((Double) data[1]));
                }
            }
            case FPS_INFO -> {
                if (debug && data.length > 0) {
                    double dfps = (Float) data[0];
                    double dspf = 1000 / dfps;
                    fps.setText(fpsFormatter.format(dfps).concat(" " + I18n.msg("gui.debug.fps")));
                    spf.setText(spfFormatter.format(dspf).concat(" " + I18n.msg("gui.debug.ms")));
                }
            }
            case SAMP_INFO -> {
                if (debug && data.length > 0) {
                    debugSamp.setText((String) data[0]);
                }
            }
            case SHOW_DEBUG_CMD -> {
                boolean showDebugInfo;
                if (data.length >= 1) {
                    showDebugInfo = (boolean) data[0];
                } else {
                    showDebugInfo = !this.isVisible();
                }
                Settings.settings.program.debugInfo = showDebugInfo;
                if (showDebugInfo) {
                    // Display.
                    this.addAction(Actions.sequence(
                            Actions.visible(true),
                            Actions.alpha(0f),
                            Actions.fadeIn(Settings.settings.program.ui.getAnimationSeconds())
                    ));
                } else {
                    // Hide.
                    this.addAction(Actions.sequence(
                            Actions.alpha(1f),
                            Actions.fadeOut(Settings.settings.program.ui.getAnimationSeconds()),
                            Actions.visible(false)
                    ));
                }
            }
            default -> {
            }
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

    @Override
    public void update() {

    }

}
