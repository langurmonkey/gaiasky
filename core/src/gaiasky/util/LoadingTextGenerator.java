package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.util.math.StdRandom;

public class LoadingTextGenerator {

    private static final String[] verbs;
    private static final String[] adjectives;
    private static final String[] objects;
    static {
        verbs = read(Gdx.files.internal("text/verbs"));
        adjectives = read(Gdx.files.internal("text/adjectives"));
        objects = read(Gdx.files.internal("text/objects"));
    }

    private static String[] read(FileHandle fh){
       return fh.readString().split("\\r\\n|\\n|\\r");
    }

    public static String next() {
        String verb = verbs[StdRandom.uniform(verbs.length)] + " ";
        String adj = adjectives[StdRandom.uniform(adjectives.length)];
        adj = !adj.isBlank() ? adj + " " : adj;
        String obj = objects[StdRandom.uniform(objects.length)];
        return verb + adj + obj;
    }
}
