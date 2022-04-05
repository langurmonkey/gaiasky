package gaiasky.util.i18n;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.util.Locale;

public class I18nUtils {
    public static FileHandle getI18nFile(String baseFile, Locale locale) {
        FileHandle fh = null;
        if (locale != null) {
            FileHandle locFh = Gdx.files.internal(baseFile + "_" + locale.toLanguageTag());
            if (locFh.exists()) {
                fh = locFh;
            }
        }
        if (fh == null)
            fh = Gdx.files.internal(baseFile);
        return fh;
    }
}
