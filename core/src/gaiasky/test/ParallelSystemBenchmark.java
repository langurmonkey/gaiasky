package gaiasky.test;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.SingleMatrix;
import gaiasky.scene.system.ParallelSystem;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Tests the speed of {@link gaiasky.scene.system.ParallelSystem} and compares it to
 * {@link com.badlogic.ashley.systems.IteratingSystem}, for multiple numbers of entities.
 * TODO Use JMH.
 */
public class ParallelSystemBenchmark {

    /** Number of rounds for each test. **/
    private static int ROUNDS = 10;

    /** Number of rounds for warm-up. **/
    private static int ROUNDS_WARMUP = 0;

    /** Number of iterations that we run the engine. **/
    private static int ENGINE_ITERATIONS = 2;

    /**
     * Number of entities to try.
     */
    private static int[] SIZES = new int[] { 250_000, 500_000, 1_000_000, 2_000_000, 5_000_000 };

    public static void main(String[] args) {
        (new ParallelSystemBenchmark()).test();
    }

    /**
     * The sequential system.
     */
    private class MySequentialSystem extends IteratingSystem {
        private Consumer<Entity> fn;

        public MySequentialSystem(Family family, Consumer<Entity> fn) {
            super(family);
            this.fn = fn;
        }

        @Override
        protected void processEntity(Entity entity, float deltaTime) {
            fn.accept(entity);
        }
    }

    /**
     * The parallel system
     */
    private class MyParallelSystem extends ParallelSystem {
        private Consumer<Entity> fn;

        public MyParallelSystem(Family family, Consumer<Entity> fn) {
            super(family);
            this.fn = fn;
        }

        @Override
        protected void processEntity(Entity entity, float deltaTime) {
            fn.accept(entity);
        }
    }

    protected Logger log;
    private DecimalFormat df;

    public ParallelSystemBenchmark() {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-2s] %5$s %n");
        log = Logger.getLogger(getClass().getSimpleName());
        log.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());

        this.df = new DecimalFormat("0.0#");
    }

    private void test() {
        log.info(pad("Java version", 22) + System.getProperty("java.version"));
        log.info(pad("ROUNDS", 22) + formatNumber(ROUNDS, 22));
        log.info(pad("ROUNDS (warm-up)", 22) + formatNumber(ROUNDS_WARMUP, 22));
        log.info(pad("ENGINE_ITERATIONS", 22) + formatNumber(ENGINE_ITERATIONS, 22));
        log.info(pad("N_ENTITIES", 22) + pad(Arrays.toString(SIZES), 22));
        log.info("");

        final Random rand = new Random(123l);
        Consumer<Entity> func = entity -> {
            Base base = entity.getComponent(Base.class);
            Body body = entity.getComponent(Body.class);
            body.distToCamera = base.opacity + 200_000_000;
            double value = 0;
            for (int i = 0; i < body.distToCamera; i++) {
                value += Math.atan(rand.nextDouble()) * Math.log(i);
            }
            for (int j = 0; j < 200_000_000; j++) {
                value += Math.cos(rand.nextDouble()) * Math.log10(j);
            }
            SingleMatrix m = entity.getComponent(SingleMatrix.class);
            m.matrix.idt().rotate(3, 1, 0, 32).scl(23).det();

            base.id = (long) (value);
        };

        EntitySystem sequential = new MySequentialSystem(Family.all(Base.class, Body.class).get(), func);
        EntitySystem parallel = new MyParallelSystem(Family.all(Base.class, Body.class).get(), func);

        // Prepare engine
        Engine engine = new PooledEngine();
        for (int nEntities : SIZES) {
            engine.removeAllSystems();
            engine.removeAllEntities();

            // Prepare entities
            for (int i = 0; i < nEntities; i++) {
                Entity entity = new Entity();
                entity.add(new Base());
                entity.add(new SingleMatrix());
                entity.getComponent(SingleMatrix.class).matrix = new Matrix4();

                engine.addEntity(entity);
            }

            log.info(pad(nEntities + " entities", 20) + pad("clock time", 28) + pad("cpu time", 28));
            log.info("-------------------------------------------------------------------");

            // Parallel
            test(engine, parallel, ROUNDS_WARMUP, false);
            test(engine, parallel, ROUNDS, true);

            // Sequential
            test(engine, sequential, ROUNDS_WARMUP, false);
            test(engine, sequential, ROUNDS, true);


            log.info("-------------------------------------------------------------------");
            log.info("");
        }
    }

    private void test(Engine engine, EntitySystem system, int rounds, boolean record) {
        if(rounds == 0) {
            return;
        }

        String name = pad(system.getClass().getSimpleName(), 20);

        // Add system
        engine.removeAllSystems();
        engine.addSystem(system);

        long[][] elapsed = new long[2][rounds];

        for (int round = 0; round < rounds; round++) {
            long cpuStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
            long clockStart = System.nanoTime();

            // Run
            for (int iter = 0; iter < ENGINE_ITERATIONS; iter++) {
                engine.update(1f);
            }

            elapsed[0][round] = System.nanoTime() - clockStart;
            elapsed[1][round] = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - cpuStart;
        }

        if (record) {
            double meanClockMs = mean(elapsed[0]) / 1_000_000d;
            double stdevClock = stdev(elapsed[0], meanClockMs);

            double meanCpuMs = mean(elapsed[1]) / 1_000_000d;
            double stdevCpu = stdev(elapsed[1], meanCpuMs);

            log.info(name + pad(meanClockMs + " (±" + format(stdevClock) + ") ms", 28) + pad(meanCpuMs + " (±" + format(stdevCpu) + ") ms", 28));
        }

        System.gc();
    }

    private double mean(long[] array) {
        long total = 0;
        for (int i = 0; i < array.length; i++) {
            total += array[i];
        }
        return total / array.length;
    }

    private double stdev(long[] array, double mean) {
        double sum = 0;
        double n = array.length;
        for (int i = 0; i < n; i++) {
            sum += Math.pow(array[i] / 1_000_000d - mean, 2.0);
        }
        return Math.sqrt(sum / n);
    }

    private String pad(String str, int len) {
        String strPad = str;
        while (strPad.length() < len) {
            strPad += " ";
        }
        return strPad;
    }

    private String format(double num) {
        return df.format(num);
    }

    private String formatNumber(int num, int pad) {
        return pad(formatNumber(num), 22);
    }

    private String formatNumber(int num) {
        if (num > 1e9) {
            return df.format(num / 1_000_000_000d) + " G";
        } else if (num > 1e6) {
            return df.format(num / 1_000_000d) + " M";
        } else if (num > 1e3) {
            return df.format(num / 1_000d) + " k";
        } else {
            return Integer.toString(num);
        }
    }

}
