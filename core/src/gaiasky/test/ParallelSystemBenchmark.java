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
import java.util.concurrent.ThreadLocalRandom;
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
    private static final int ROUNDS = 10;

    /** Number of rounds for warm-up. **/
    private static final int ROUNDS_WARMUP = 2;

    /** Number of iterations that we run the engine. **/
    private static final int ENGINE_ITERATIONS = 10;

    /**
     * Number of entities to use.
     */
    private static final int[] SIZES = new int[] { 50, 100, 250, 500, 1_000, 2_000, 5_000, 10_000 };
    private final DecimalFormat df;
    protected Logger log;

    public ParallelSystemBenchmark() {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-2s] %5$s %n");
        log = Logger.getLogger(getClass().getSimpleName());
        log.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());

        this.df = new DecimalFormat("0.0#");
    }

    public static void main(String[] args) {
        (new ParallelSystemBenchmark()).test();
    }

    private void test() {
        int pad = 22;
        log.info(pad("Java version", pad) + System.getProperty("java.version"));
        log.info(pad("ROUNDS", pad) + formatNumber(ROUNDS, pad));
        log.info(pad("ROUNDS (warm-up)", pad) + formatNumber(ROUNDS_WARMUP, pad));
        log.info(pad("ENGINE_ITERATIONS", pad) + formatNumber(ENGINE_ITERATIONS, pad));
        log.info(pad("N_ENTITIES", pad) + pad(Arrays.toString(SIZES), pad));
        log.info("");

        Consumer<Entity> func = entity -> {
            Base base = entity.getComponent(Base.class);
            Body body = entity.getComponent(Body.class);
            body.distToCamera = 20;
            double value = 0;
            for (int i = 0; i < body.distToCamera; i++) {
                value += Math.atan(ThreadLocalRandom.current().nextDouble()) * Math.log(i) * Math.pow(body.distToCamera, 12);
            }
            SingleMatrix m = entity.getComponent(SingleMatrix.class);
            m.matrix.idt().rotate(3, 1, 0, 32).scl(23);

            base.id = (long) (value);
        };

        // Prepare engine
        for (int nEntities : SIZES) {
            System.gc();
            Engine engine = new PooledEngine();

            // Prepare entities
            for (int i = 0; i < nEntities; i++) {
                Entity entity = new Entity();
                entity.add(new Base());
                entity.add(new Body());
                entity.add(new SingleMatrix());
                entity.getComponent(SingleMatrix.class).matrix = new Matrix4();

                engine.addEntity(entity);
            }

            // Create systems
            EntitySystem sequential = new MySequentialSystem(Family.all(Base.class, SingleMatrix.class).get(), func);
            EntitySystem parallel = new MyParallelSystem(Family.all(Base.class, SingleMatrix.class).get(), func);

            log.info(pad(nEntities + " entities", 20) + pad("clock time", 28) + pad("cpu time", 28));
            log.info("----------------------------------------------------------------------");

            // Warm-up
            test(engine, sequential, ROUNDS_WARMUP, false);
            test(engine, parallel, ROUNDS_WARMUP, false);

            // Test
            test(engine, sequential, ROUNDS, true);
            test(engine, parallel, ROUNDS, true);

            log.info("----------------------------------------------------------------------");
            log.info("");

            engine.removeAllEntities();
            engine.removeAllSystems();
        }
    }

    private void test(Engine engine, EntitySystem system, int rounds, boolean record) {
        if (rounds == 0) {
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
        for (long l : array) {
            total += l;
        }
        return total / array.length;
    }

    private double stdev(long[] array, double mean) {
        double sum = 0;
        double n = array.length;
        for (long l : array) {
            sum += Math.pow(l / 1_000_000d - mean, 2.0);
        }
        return Math.sqrt(sum / n);
    }

    private String pad(String str, int len) {
        StringBuilder strPad = new StringBuilder(str);
        while (strPad.length() < len) {
            strPad.append(" ");
        }
        return strPad.toString();
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

    /**
     * The sequential system.
     */
    private static class MySequentialSystem extends IteratingSystem {
        private final Consumer<Entity> fn;

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
    private static class MyParallelSystem extends ParallelSystem {
        private final Consumer<Entity> fn;

        public MyParallelSystem(Family family, Consumer<Entity> fn) {
            super(family);
            this.fn = fn;
        }

        @Override
        protected void processEntity(Entity entity, float deltaTime) {
            fn.accept(entity);
        }
    }

}
