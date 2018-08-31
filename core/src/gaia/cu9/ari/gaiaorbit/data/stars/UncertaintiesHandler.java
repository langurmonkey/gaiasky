package gaia.cu9.ari.gaiaorbit.data.stars;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.scenegraph.IStarFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup;
import gaia.cu9.ari.gaiaorbit.util.Logger;

public class UncertaintiesHandler implements IObserver {
    private static final boolean PRELOAD = true;
    private static UncertaintiesHandler singleton;

    public static UncertaintiesHandler getInstance() {
        if (singleton == null) {
            singleton = new UncertaintiesHandler();
        }
        return singleton;
    }

    private String path;
    private Set<Long> sourceIds;
    private Array<ParticleGroup> particleGroups;
    private double[][] colors;
    private int coloridx = 0;

    private UncertaintiesHandler() {
        path = "/media/tsagrista/Daten/Gaia/Coryn-data/data3/";

        particleGroups = new Array<ParticleGroup>();
        colors = new double[][] { { 0, 1, 0, 1 }, { 1, 0, 0, 1 }, { 0, 0, 1, 1 }, { 1, 1, 0, 1 }, { 1, 0, 1, 1 }, { 0, 1, 1, 1 }, { 0.5, 1, 1, 1 } };

        // Generate set with starids for which we have uncertainties
        sourceIds = new HashSet<Long>();
        Path dir = Paths.get(path);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{csv}")) {
            for (Path entry : stream) {
                String fname = entry.getFileName().toString();
                int pos = fname.lastIndexOf(".");
                if (pos > 0) {
                    fname = fname.substring(0, pos);
                    Long id = Long.parseLong(fname);
                    sourceIds.add(id);

                    if (PRELOAD) {
                        ParticleGroup pg = load(id);
                        GaiaSky.instance.sg.getRoot().addChild(pg, true);
                        particleGroups.add(pg);
                    }
                }
            }
        } catch (NoSuchFileException e) {
            Logger.getLogger(this.getClass()).error("Directory " + path + " not found");
        } catch (IOException e) {
            Logger.getLogger(this.getClass()).error(e);
        }

        EventManager.instance.subscribe(this, Events.SHOW_UNCERTAINTIES, Events.HIDE_UNCERTAINTIES);
    }

    public boolean containsStar(Long id) {
        return sourceIds != null && sourceIds.contains(id);
    }

    public boolean containsUncertainties() {
        return particleGroups.size > 0;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case SHOW_UNCERTAINTIES:
            if (data[0] instanceof IStarFocus) {
                final IStarFocus s = (IStarFocus) data[0];
                Gdx.app.postRunnable(new Runnable() {

                    @Override
                    public void run() {
                        ParticleGroup pg = load(s.getCandidateId());

                        GaiaSky.instance.sg.getRoot().addChild(pg, true);
                        particleGroups.add(pg);
                    }

                });

            }
            break;
        case HIDE_UNCERTAINTIES:
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    for (ParticleGroup pg : particleGroups) {
                        GaiaSky.instance.sg.getRoot().removeChild(pg, true);
                    }
                    particleGroups.clear();
                }
            });
            break;
        default:
            break;
        }

    }

    private ParticleGroup load(long sid) {
        String source_id = String.valueOf(sid);
        ParticleGroup pg = new ParticleGroup();
        pg.setColor(colors[coloridx]);
        coloridx = (coloridx + 1) % colors.length;
        pg.setSize(3.5d);
        pg.setProfiledecay(0.3);
        pg.setName("");
        pg.setLabelcolor(new double[] { 1, 1, 1, 0 });
        pg.setLabelposition(new double[] { 0, 0, 0 });
        pg.setCt("Others");
        pg.setParent("Universe");
        pg.setProvider("gaia.cu9.ari.gaiaorbit.data.group.UncertaintiesProvider");
        pg.setDatafile(path + source_id + ".csv");
        pg.initialize();
        return pg;
    }

}
