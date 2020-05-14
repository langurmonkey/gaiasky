package gaiasky.util;

import gaiasky.util.math.StdRandom;

public class LoadingTextGenerator {


    private static final String[] verbs = new String[]{
            "looking for",
            "initializing",
            "setting up",
            "pumping up",
            "adjusting",
            "fitting",
            "tracing",
            "tuning",
            "introducing",
            "downloading",
            "modelling",
            "listening to",
            "following",
            "caching",
            "restoring",
            "dodging",
            "registering",
            "computing",
            "synchronizing",
            "sorting"
    };

    private static final String[] adjectives = new String[]{
            "big",
            "irrelevant",
            "negligible",
            "omnious",
            "stoic",
            "undecipherable",
            "interesting",
            "small",
            "", "", "", "", "", "", "", ""
    };

    private static final String[] objects = new String[]{
            "flux capacitor",
            "continuum transfunctioner",
            "atmospheres",
            "rocky planets",
            "extraterrestrials",
            "downloaded RAM modules",
            "GPU performance curves",
            "stars",
            "asteroids",
            "planetary orbits",
            "molecular clouds",
            "white dwarfs",
            "blue supergiants",
            "red giants",
            "quasars",
            "cepheids",
            "lonely star",
            "outliers",
            "fundamental physical constants",
            "gravity strength",
            "spatiotemporal serendipities",
            "Vulcan planet",
            "sound of space",
            "dust maps",
            "anomalous materials",
            "manifolds",
            "'all your base are belong to us'",
            "certainty of death",
            "locations",
            "USS enterprise",
            "death star",
            "keyboard layout",
            "networks"
    };

    public static String next(){
        String verb = verbs[StdRandom.uniform(verbs.length)] + " ";
        String adj = adjectives[StdRandom.uniform(adjectives.length)];
        adj = !adj.isBlank() ? adj + " " : adj;
        String obj = objects[StdRandom.uniform(objects.length)];
        return verb + adj + obj;
    }
}
