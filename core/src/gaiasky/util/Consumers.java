package gaiasky.util;

public class Consumers {
    @FunctionalInterface
    public interface Consumer3<One, Two, Three> {
        void apply(One one, Two two, Three three);
    }

    @FunctionalInterface
    public interface Consumer4<One, Two, Three, Four> {
        void apply(One one, Two two, Three three, Four four);
    }

    @FunctionalInterface
    public interface Consumer5<One, Two, Three, Four, Five> {
        void apply(One one, Two two, Three three, Four four, Five five);
    }

    @FunctionalInterface
    public interface Consumer6<One, Two, Three, Four, Five, Six> {
        void apply(One one, Two two, Three three, Four four, Five five, Six six);
    }

    @FunctionalInterface
    public interface Consumer7<One, Two, Three, Four, Five, Six, Seven> {
        void apply(One one, Two two, Three three, Four four, Five five, Six six, Seven seven);
    }

    @FunctionalInterface
    public interface Consumer8<One, Two, Three, Four, Five, Six, Seven, Eight> {
        void apply(One one, Two two, Three three, Four four, Five five, Six six, Seven seven, Eight eight);
    }

    @FunctionalInterface
    public interface Consumer9<One, Two, Three, Four, Five, Six, Seven, Eight, Nine> {
        void apply(One one, Two two, Three three, Four four, Five five, Six six, Seven seven, Eight eight, Nine nine);
    }

    @FunctionalInterface
    public interface Consumer10<One, Two, Three, Four, Five, Six, Seven, Eight, Nine, Ten> {
        void apply(One one, Two two, Three three, Four four, Five five, Six six, Seven seven, Eight eight, Nine nine, Ten ten);
    }
}
