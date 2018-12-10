package gaia.cu9.ari.gaiaorbit.desktop.vr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopNumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopConfInit;
import gaia.cu9.ari.gaiaorbit.interfce.ConsoleLogger;
import gaia.cu9.ari.gaiaorbit.util.ConfInit;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.vr.OpenVRQuery;

import java.io.File;
import java.io.FileInputStream;

public class OpenVRQueryDesktop {

    public static void main(String[] args) {
        try {
            // Assets location
            String ASSETS_LOC = (System.getProperty("assets.location") != null ? System.getProperty("assets.location") : "../assets/");

            new ConsoleLogger();

            Gdx.files = new Lwjgl3Files();

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            // Initialize i18n
            I18n.initialize(Gdx.files.internal("i18n/gsbundle"));

            // Initialize conf
            ConfInit.initialize(new DesktopConfInit(new FileInputStream(new File(ASSETS_LOC + "conf/global.vr.properties")), new FileInputStream(new File(ASSETS_LOC + "data/dummyversion"))));

            I18n.initialize(new FileHandle(ASSETS_LOC + "i18n/gsbundle"));

            OpenVRQuery.queryOpenVr();

        } catch (Exception e) {
            Logger.getLogger(OpenVRQueryDesktop.class).error(e);
        }

    }

}
