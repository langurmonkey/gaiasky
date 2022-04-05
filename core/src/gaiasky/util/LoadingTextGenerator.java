package gaiasky.util;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.util.i18n.I18n;
import gaiasky.util.i18n.I18nUtils;
import gaiasky.util.math.StdRandom;

import java.util.Locale;
import java.util.Scanner;

/**
 * Manages and generates sentences to display during loading.
 * It uses a series of files containing verbs, adjectives and objects, which are
 * optionally localized, and an order file to compose random sentences.
 */
public class LoadingTextGenerator {

    private String[][] set;
    private final String[] verbs;
    private final String[] adjectives;
    private final String[] objects;

    public LoadingTextGenerator() {
        Locale locale = I18n.getLocaleFromLanguageTag(Settings.settings.program.locale);
        verbs = read(I18nUtils.getI18nFile("text/verbs", locale));
        adjectives = read(I18nUtils.getI18nFile("text/adjectives", locale), 4);
        objects = read(I18nUtils.getI18nFile("text/objects", locale));
        set = createOrder(I18nUtils.getI18nFile("text/order", locale));
    }

    private String[] read(FileHandle fh) {
        return read(fh, 0);
    }

    private String[] read(FileHandle fh, int nBlanks) {
        String[] fromFile = fh.readString().split("\\r\\n|\\n|\\r");
        if (nBlanks <= 0) {
            return fromFile;
        }
        String[] result = new String[fromFile.length + nBlanks];
        for (int i = 0; i < result.length; i++) {
            if (i < fromFile.length) {
                result[i] = fromFile[i];
            } else {
                result[i] = "";
            }
        }
        return result;
    }

    private String[][] createOrder(FileHandle file) {
        Scanner scanner = new Scanner(file.readString());
        String order = null;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            // Skip comments
            if (!line.startsWith("#")) {
                order = line;
                break;
            }
        }
        String[][] result = null;
        if (order != null && !order.isBlank()) {
            // Split by spaces
            String[] tokens = order.split("\\s+");
            if (tokens.length == 3) {
                result = new String[3][];
                for (int i = 0; i < 3; i++) {
                    if (tokens[i].equalsIgnoreCase("V")) {
                        // Verbs
                        result[i] = verbs;
                    } else if (tokens[i].equalsIgnoreCase("A")) {
                        // Adjectives
                        result[i] = adjectives;
                    } else if (tokens[i].equalsIgnoreCase("O")) {
                        // Objects
                        result[i] = objects;
                    }
                }
            }
        }
        // Use default order
        if (result == null) {
            result = new String[][] { verbs, adjectives, objects };
        }
        return result;
    }

    private String next(int index, String sep) {
        String next = set[index][StdRandom.uniform(set[index].length)];
        next = !next.isBlank() ? next + sep : "";
        return next;
    }

    public String next() {
        String first = next(0, " ");
        String second = next(1, " ");
        String third = next(2, "");
        return first + second + third;
    }
}
