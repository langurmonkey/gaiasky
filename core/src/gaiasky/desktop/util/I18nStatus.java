package gaiasky.desktop.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import gaiasky.gui.ConsoleLogger;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class computes the translation status for each language.
 */
public class I18nStatus {
    private static final Log logger = Logger.getLogger(I18nStatus.class);

    public static void main(String[] args) {
        CLIArgs cliArgs = new CLIArgs();
        JCommander jc = JCommander.newBuilder().addObject(cliArgs).build();
        jc.setProgramName("translationstatus");
        try {
            jc.parse(args);

            if (cliArgs.help) {
                jc.usage();
                return;
            }
        } catch (Exception e) {
            System.out.print("bad program arguments\n\n");
            jc.usage();
            return;
        }
        // Assets location
        String ASSETS_LOC = Settings.ASSETS_LOC;
        I18n.locale = Locale.getDefault();

        Gdx.files = new Lwjgl3Files();

        // Add notification watch
        new ConsoleLogger();

        DecimalFormat df = new DecimalFormat("0.0#");

        try {
            PathMatcher globalMatcher = FileSystems.getDefault().getPathMatcher("glob:" + cliArgs.bundleFile + "*.properties");
            PathMatcher languageMatcher = FileSystems.getDefault().getPathMatcher("glob:" + cliArgs.bundleFile + "*_*.properties");

            Path i18nDir = Path.of(ASSETS_LOC, "i18n");
            List<Path> candidatePaths = Files.list(i18nDir).collect(Collectors.toList());
            List<Path> languagePaths = new ArrayList<>();
            Path main = null;
            for (Path p : candidatePaths) {
                Path fileName = p.getFileName();
                if (globalMatcher.matches(fileName)) {
                    if (languageMatcher.matches(fileName)) {
                        languagePaths.add(p);
                        logger.info("Language file: " + fileName);
                    } else {
                        main = p;
                        logger.info("Main file: " + fileName);
                    }
                }
            }

            if (main == null) {
                logger.error("Main file not found");
                return;
            }

            if (languagePaths.isEmpty()) {
                logger.error("No language files found");
                return;
            }

            // Load main file
            InputStream is = new FileInputStream(main.toString());
            Properties mainProps = new Properties();
            mainProps.load(is);
            is.close();

            int totalKeys = mainProps.size();
            logger.info("Total keys: " + totalKeys);
            logger.info("");

            for (Path p : languagePaths) {
                String name = p.getFileName().toString();
                name = name.substring(0, name.lastIndexOf(".properties"));

                String languageCode = name.substring(name.indexOf("_") + 1);
                String country = null;
                if (languageCode.contains("_")) {
                    country = languageCode.substring(languageCode.indexOf("_") + 1);
                    languageCode = languageCode.substring(0, languageCode.indexOf("_"));
                }
                Locale locale = country == null ? new Locale(languageCode) : new Locale(languageCode, country);

                // Load path
                is = new FileInputStream(p.toString());
                Properties lang = new Properties();
                lang.load(is);
                is.close();

                int translatedKeys = 0;
                Set<Object> missingKeys = new HashSet<>();
                Enumeration<Object> mainKeys = mainProps.keys();
                Iterator<Object> it = mainKeys.asIterator();
                while (it.hasNext()) {
                    Object key = it.next();
                    if (lang.containsKey(key)) {
                        translatedKeys++;
                    } else {
                        missingKeys.add(key);
                    }
                }
                Set<Object> unknownKeys = new HashSet<>();
                it = lang.keys().asIterator();
                while (it.hasNext()) {
                    Object key = it.next();
                    if (!mainProps.containsKey(key)) {
                        unknownKeys.add(key);
                    }
                }
                int extra = lang.size() - translatedKeys;

                double percentage = 100.0 * ((double) (translatedKeys + extra) / (double) totalKeys);
                int unknownCount = unknownKeys.size();

                logger.info(locale.getDisplayName() + " (" + locale + ")");
                logger.info("Translated: " + translatedKeys + "/" + totalKeys + (extra > 0 ? " (+" + extra + " extra)" : ""));
                logger.info(df.format(percentage) + "%");
                if (cliArgs.showUnknown && translatedKeys < lang.size()) {
                    logger.info(unknownCount + " unknown keys:");
                    StringBuilder sb = new StringBuilder();
                    for (Object key : unknownKeys) {
                        sb.append(key).append(" ");
                    }
                    String keyString = TextUtils.breakCharacters(sb.toString(), 100);
                    logger.info(keyString);
                }
                if (cliArgs.showUntranslated && missingKeys.size() > 0) {
                    logger.info(missingKeys.size() + " untranslated keys:");
                    StringBuilder sb = new StringBuilder();
                    for (Object key : missingKeys) {
                        sb.append(key).append(" ");
                    }
                    String keyString = TextUtils.breakCharacters(sb.toString(), 100);
                    logger.info(keyString);
                }

                logger.info("");
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private static class CLIArgs {
        @Parameter(names = { "-h", "--help" }, description = "Show program options and usage information.", help = true, order = 0) private boolean help = false;
        @Parameter(names = { "-s", "--show-untranslated" }, description = "Show untranslated keys for each language.", order = 1) private boolean showUntranslated = false;
        @Parameter(names = { "-u", "--show-unknown" }, description = "Show unknown keys for each language.", order = 2) private boolean showUnknown = false;
        @Parameter(names = { "-f", "--file" }, description = "The name of the file to check, either 'gsbundle' or 'objects'.", order = 3) private String bundleFile = "gsbundle";

    }
}
