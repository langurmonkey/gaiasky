package gaia.cu9.ari.gaiaorbit.desktop.util;

import java.io.*;
import java.util.Properties;
import java.util.Set;

public class I18nFormatter {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage:");
            System.out.println("    I18nFormatter [REFERENCE_I18N] [OTHER_I18N]");
            System.out.println("Example:");
            System.out.println("    I18nFormatter ../assets/i18n/gsbundle.properties ../assets/i18n/gsbundle_ca.properties");
            return;
        }

        File f0 = new File(args[0]);
        File f1 = new File(args[1]);

        if (!checkFile(f0) || !checkFile(f1)) {
            return;
        }

        try {
            FileInputStream fis0 = new FileInputStream(f0);
            CommentedProperties p0 = new CommentedProperties();
            p0.load(fis0);

            FileInputStream fis1 = new FileInputStream(f1);
            InputStreamReader isr1 = new InputStreamReader(fis1,"UTF-8");
            Properties p1 = new Properties();
            p1.load(isr1);

            fis0.close();
            isr1.close();

            // Output properties, clone of p0
            CommentedProperties op = p0.clone();

            Set<Object> keys = p0.keySet();
            for (Object key : keys) {
                boolean has = p1.getProperty((String) key) != null;
                if (has) {
                    // Substitute value
                    String val = p1.getProperty((String) key);
                    op.setProperty((String) key, val);
                } else {
                    System.err.println("Property not found: " + key);
                    // Remove
                    op.remove(key);
                }
            }

            // Store result
            File outf = new File(args[1].substring(0, args[1].lastIndexOf(".")) + ".mod.properties");
            if(outf.exists()){
                outf.delete();
            }

            FileOutputStream fos1 = new FileOutputStream(outf, true);
            PrintStream ps = new PrintStream(fos1, true, "UTF-8");
            op.store(ps, null, "UTF-8");
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean checkFile(File f) {
        if (!f.exists()) {
            System.out.println("File does not exist: " + f);
            return false;
        }
        if (!f.isFile()) {
            System.out.println("Not a file: " + f);
            return false;
        }
        if (!f.canRead()) {
            System.out.println("Can not read: " + f);
            return false;
        }
        return true;
    }
}
