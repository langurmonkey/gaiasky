package gaia.cu9.ari.gaiaorbit.data;

import java.io.File;
import java.io.FileInputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;
import com.badlogic.gdx.files.FileHandle;

import gaia.cu9.ari.gaiaorbit.data.group.TGASHYGDataProvider;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopNumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopConfInit;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopSysUtilsFactory;
import gaia.cu9.ari.gaiaorbit.desktop.util.LogWriter;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.ConfInit;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.SysUtilsFactory;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;

public class TGASHYGToBinary {

    public static void main(String[] args) {
        TGASHYGDataProvider tgashyg = new TGASHYGDataProvider();
        TGASHYGDataProvider.setDumpToDisk(true, "bin");
        tgashyg.setParallaxErrorFactor(0.14);

        try {
            // Logger
            new LogWriter();
            
            // Assets location
            String ASSETS_LOC = (System.getProperty("assets.location") != null ? System.getProperty("assets.location") : "");

            Gdx.files = new LwjglFiles();

            // Sys utils
            SysUtilsFactory.initialize(new DesktopSysUtilsFactory());

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            ConfInit.initialize(new DesktopConfInit(new FileInputStream(new File(ASSETS_LOC + "conf/global.properties")), new FileInputStream(new File(ASSETS_LOC + "data/dummyversion"))));

            I18n.initialize(new FileHandle(ASSETS_LOC + "i18n/gsbundle"));

            tgashyg.loadData("nofile", 1);

        } catch (Exception e) {
            Logger.getLogger(TGASHYGToBinary.class).error(e);
        }

    }

}
