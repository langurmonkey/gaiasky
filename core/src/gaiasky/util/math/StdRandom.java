/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.utils.TimeUtils;
import net.jafama.FastMath;

import java.util.Random;

public final class StdRandom {

    private static Random random; // pseudo-random number generator
    private static long seed; // pseudo-random number generator seed

    // static initializer
    static {
        // this is how the seed was set in Java 1.4
        seed = TimeUtils.millis();
        random = new Random(seed);
    }

    // don't instantiate
    private StdRandom() {
    }

    /**
     * Returns the seed of the pseudorandom number generator.
     *
     * @return the seed
     */
    public static long getSeed() {
        return seed;
    }

    /**
     * Sets the seed of the pseudorandom number generator.
     * This method enables you to produce the same sequence of "random"
     * number for each execution of the program.
     * Ordinarily, you should call this method at most once per program.
     *
     * @param s the seed
     */
    public static void setSeed(long s) {
        seed = s;
        random.setSeed(s);
    }

    /**
     * Returns a random real number uniformly in [0, 1).
     *
     * @return a random real number uniformly in [0, 1)
     */
    public static double uniform() {
        return random.nextDouble();
    }

    /**
     * Returns a random integer uniformly in [0, n).
     *
     * @param n number of possible integers
     *
     * @return a random integer uniformly between 0 (inclusive) and <code>N</code> (exclusive)
     *
     * @throws IllegalArgumentException if <code>n &le; 0</code>
     */
    public static int uniform(int n) {
        if (n <= 0)
            throw new IllegalArgumentException("Parameter N must be positive");
        return random.nextInt(n);
    }

    ///////////////////////////////////////////////////////////////////////////
    //  STATIC METHODS BELOW RELY ON JAVA.UTIL.RANDOM ONLY INDIRECTLY VIA
    //  THE STATIC METHODS ABOVE.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns a random real number uniformly in [0, 1).
     *
     * @return a random real number uniformly in [0, 1)
     *
     * @deprecated Replaced by {@link #uniform()}.
     */
    @Deprecated
    public static double random() {
        return uniform();
    }

    /**
     * Returns a random integer uniformly in [a, b).
     *
     * @param a the left endpoint
     * @param b the right endpoint
     *
     * @return a random integer uniformly in [a, b)
     *
     * @throws IllegalArgumentException if <code>b &le; a</code>
     * @throws IllegalArgumentException if <code>b - a &ge; Integer.MAX_VALUE</code>
     */
    public static int uniform(int a, int b) {
        if (b <= a)
            throw new IllegalArgumentException("Invalid range");
        if ((long) b - a >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid range");
        return a + uniform(b - a);
    }

    /**
     * Returns a random real number uniformly in [a, b).
     *
     * @param a the left endpoint
     * @param b the right endpoint
     *
     * @return a random real number uniformly in [a, b)
     *
     * @throws IllegalArgumentException unless <code>a &lt; b</code>
     */
    public static double uniform(double a, double b) {
        if (!(a < b))
            throw new IllegalArgumentException("Invalid range");
        return a + uniform() * (b - a);
    }

    /**
     * Returns a random boolean from a Bernoulli distribution with success
     * probability <em>p</em>.
     *
     * @param p the probability of returning <code>true</code>
     *
     * @return <code>true</code> with probability <code>p</code> and
     * <code>false</code> with probability <code>p</code>
     *
     * @throws IllegalArgumentException unless <code>p &ge; 0.0</code> and <code>p &le; 1.0</code>
     */
    public static boolean bernoulli(double p) {
        if (!(p >= 0.0 && p <= 1.0))
            throw new IllegalArgumentException("Probability must be between 0.0 and 1.0");
        return uniform() < p;
    }

    /**
     * Returns a random boolean from a Bernoulli distribution with success
     * probability 1/2.
     *
     * @return <code>true</code> with probability 1/2 and
     * <code>false</code> with probability 1/2
     */
    public static boolean bernoulli() {
        return bernoulli(0.5);
    }

    /**
     * Returns a random real number from a standard Gaussian distribution.
     *
     * @return a random real number from a standard Gaussian distribution
     * (mean 0 and standard deviation 1).
     */
    public static double gaussian() {
        // use the polar form of the Box-Muller transform
        double r, x, y;
        do {
            x = uniform(-1.0, 1.0);
            y = uniform(-1.0, 1.0);
            r = x * x + y * y;
        } while (r >= 1 || r == 0);
        return x * FastMath.sqrt(-2 * FastMath.log(r) / r);

        // Remark:  y * FastMath.sqrt(-2 * FastMath.log(r) / r)
        // is an independent random gaussian
    }

    /**
     * Returns a random real number from a Gaussian distribution with mean &mu;
     * and standard deviation &sigma;.
     *
     * @param mu    the mean
     * @param sigma the standard deviation
     *
     * @return a real number distributed according to the Gaussian distribution
     * with mean <code>mu</code> and standard deviation <code>sigma</code>
     */
    public static double gaussian(double mu, double sigma) {
        return mu + sigma * gaussian();
    }

    /**
     * Returns a random integer from a geometric distribution with success
     * probability <em>p</em>.
     *
     * @param p the parameter of the geometric distribution
     *
     * @return a random integer from a geometric distribution with success
     * probability <code>p</code>; or <code>Integer.MAX_VALUE</code> if
     * <code>p</code> is (nearly) equal to <code>1.0</code>.
     *
     * @throws IllegalArgumentException unless <code>p &ge; 0.0</code> and <code>p &le; 1.0</code>
     */
    public static int geometric(double p) {
        if (!(p >= 0.0 && p <= 1.0))
            throw new IllegalArgumentException("Probability must be between 0.0 and 1.0");
        // using algorithm given by Knuth
        return (int) FastMath.ceil(Math.log(uniform()) / FastMath.log(1.0 - p));
    }

    /**
     * Returns a random integer from a Poisson distribution with mean &lambda;.
     *
     * @param lambda the mean of the Poisson distribution
     *
     * @return a random integer from a Poisson distribution with mean <code>lambda</code>
     *
     * @throws IllegalArgumentException unless <code>lambda &gt; 0.0</code> and not infinite
     */
    public static int poisson(double lambda) {
        if (!(lambda > 0.0))
            throw new IllegalArgumentException("Parameter lambda must be positive");
        if (Double.isInfinite(lambda))
            throw new IllegalArgumentException("Parameter lambda must not be infinite");
        // using algorithm given by Knuth
        // see http://en.wikipedia.org/wiki/Poisson_distribution
        int k = 0;
        double p = 1.0;
        double L = FastMath.exp(-lambda);
        do {
            k++;
            p *= uniform();
        } while (p >= L);
        return k - 1;
    }

    /**
     * Returns a random real number from the standard Pareto distribution.
     *
     * @return a random real number from the standard Pareto distribution
     */
    public static double pareto() {
        return pareto(1.0);
    }

    /**
     * Returns a random real number from a Pareto distribution with
     * shape parameter &alpha;.
     *
     * @param alpha shape parameter
     *
     * @return a random real number from a Pareto distribution with shape
     * parameter <code>alpha</code>
     *
     * @throws IllegalArgumentException unless <code>alpha &gt; 0.0</code>
     */
    public static double pareto(double alpha) {
        if (!(alpha > 0.0))
            throw new IllegalArgumentException("Shape parameter alpha must be positive");
        return FastMath.pow(1 - uniform(), -1.0 / alpha) - 1.0;
    }

    /**
     * Returns a random real number from the Cauchy distribution.
     *
     * @return a random real number from the Cauchy distribution.
     */
    public static double cauchy() {
        return FastMath.tan(Math.PI * (uniform() - 0.5));
    }

    /**
     * Returns a random integer from the specified discrete distribution.
     *
     * @param probabilities the probability of occurrence of each integer
     *
     * @return a random integer from a discrete distribution:
     * <code>i</code> with probability <code>probabilities[i]</code>
     *
     * @throws NullPointerException     if <code>probabilities</code> is <code>null</code>
     * @throws IllegalArgumentException if sum of array entries is not (very nearly) equal to <code>1.0</code>
     * @throws IllegalArgumentException unless <code>probabilities[i] &ge; 0.0</code> for each index <code>i</code>
     */
    public static int discrete(double[] probabilities) {
        if (probabilities == null)
            throw new NullPointerException("argument array is null");
        double EPSILON = 1E-14;
        double sum = 0.0;
        for (int i = 0; i < probabilities.length; i++) {
            if (!(probabilities[i] >= 0.0))
                throw new IllegalArgumentException("array entry " + i + " must be nonnegative: " + probabilities[i]);
            sum += probabilities[i];
        }
        if (sum > 1.0 + EPSILON || sum < 1.0 - EPSILON)
            throw new IllegalArgumentException("sum of array entries does not approximately equal 1.0: " + sum);

        // the for loop may not return a value when both r is (nearly) 1.0 and when the
        // cumulative sum is less than 1.0 (as a result of floating-point roundoff error)
        while (true) {
            double r = uniform();
            sum = 0.0;
            for (int i = 0; i < probabilities.length; i++) {
                sum = sum + probabilities[i];
                if (sum > r)
                    return i;
            }
        }
    }

    /**
     * Returns a random integer from the specified discrete distribution.
     *
     * @param frequencies the frequency of occurrence of each integer
     *
     * @return a random integer from a discrete distribution:
     * <code>i</code> with probability proportional to <code>frequencies[i]</code>
     *
     * @throws NullPointerException     if <code>frequencies</code> is <code>null</code>
     * @throws IllegalArgumentException if all array entries are <code>0</code>
     * @throws IllegalArgumentException if <code>frequencies[i]</code> is negative for any index <code>i</code>
     * @throws IllegalArgumentException if sum of frequencies exceeds <code>Integer.MAX_VALUE</code> (2<sup>31</sup> - 1)
     */
    public static int discrete(int[] frequencies) {
        if (frequencies == null)
            throw new NullPointerException("argument array is null");
        long sum = 0;
        for (int i = 0; i < frequencies.length; i++) {
            if (frequencies[i] < 0)
                throw new IllegalArgumentException("array entry " + i + " must be nonnegative: " + frequencies[i]);
            sum += frequencies[i];
        }
        if (sum == 0)
            throw new IllegalArgumentException("at least one array entry must be positive");
        if (sum >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("sum of frequencies overflows an int");

        // pick index i with probabilitity proportional to frequency
        double r = uniform((int) sum);
        sum = 0;
        for (int i = 0; i < frequencies.length; i++) {
            sum += frequencies[i];
            if (sum > r)
                return i;
        }

        // can't reach here
        assert false;
        return -1;
    }

    /**
     * Returns a random real number from an exponential distribution
     * with rate &lambda;.
     *
     * @param lambda the rate of the exponential distribution
     *
     * @return a random real number from an exponential distribution with
     * rate <code>lambda</code>
     *
     * @throws IllegalArgumentException unless <code>lambda &gt; 0.0</code>
     */
    public static double exp(double lambda) {
        if (!(lambda > 0.0))
            throw new IllegalArgumentException("Rate lambda must be positive");
        return -Math.log(1 - uniform()) / lambda;
    }

    /**
     * Returns a random real number from a beta distribution
     * with parameters &alpha; and &beta;. The result is normalized in [-1,1]
     *
     * @param alpha the shape parameter &alpha;
     * @param beta  the shape parameter &beta;
     *
     * @return a random real number from a beta distribution normalized in [-1,1]
     *
     * @throws IllegalArgumentException unless <code>alpha, beta &gt; 0.0</code>
     */
    public static double beta(double alpha, double beta) {
        if (alpha <= 0 || beta <= 0)
            throw new IllegalArgumentException("Alpha and beta must be positive and non-zero");
        double x = uniform();
        return (Math.pow(x, alpha - 1) * FastMath.pow(1 - x, beta - 1)) * 2.0 - 1.0;
    }

    /**
     * Rearranges the elements of the specified array in uniformly random order.
     *
     * @param a the array to shuffle
     *
     * @throws NullPointerException if <code>a</code> is <code>null</code>
     */
    public static void shuffle(Object[] a) {
        if (a == null)
            throw new NullPointerException("argument array is null");
        int n = a.length;
        for (int i = 0; i < n; i++) {
            int r = i + uniform(n - i); // between i and n-1
            Object temp = a[i];
            a[i] = a[r];
            a[r] = temp;
        }
    }

    /**
     * Rearranges the elements of the specified array in uniformly random order.
     *
     * @param a the array to shuffle
     *
     * @throws NullPointerException if <code>a</code> is <code>null</code>
     */
    public static void shuffle(double[] a) {
        if (a == null)
            throw new NullPointerException("argument array is null");
        int n = a.length;
        for (int i = 0; i < n; i++) {
            int r = i + uniform(n - i); // between i and n-1
            double temp = a[i];
            a[i] = a[r];
            a[r] = temp;
        }
    }

    /**
     * Rearranges the elements of the specified array in uniformly random order.
     *
     * @param a the array to shuffle
     *
     * @throws NullPointerException if <code>a</code> is <code>null</code>
     */
    public static void shuffle(int[] a) {
        if (a == null)
            throw new NullPointerException("argument array is null");
        int n = a.length;
        for (int i = 0; i < n; i++) {
            int r = i + uniform(n - i); // between i and n-1
            int temp = a[i];
            a[i] = a[r];
            a[r] = temp;
        }
    }

    /**
     * Rearranges the elements of the specified subarray in uniformly random order.
     *
     * @param a  the array to shuffle
     * @param lo the left endpoint (inclusive)
     * @param hi the right endpoint (inclusive)
     *
     * @throws NullPointerException      if <code>a</code> is <code>null</code>
     * @throws IndexOutOfBoundsException unless <code>(0 &le; lo) &amp;&amp; (lo &le; hi) &amp;&amp; (hi &lt; a.length)</code>
     */
    public static void shuffle(Object[] a, int lo, int hi) {
        if (a == null)
            throw new NullPointerException("argument array is null");
        if (lo < 0 || lo > hi || hi >= a.length) {
            throw new IndexOutOfBoundsException("Illegal subarray range");
        }
        for (int i = lo; i <= hi; i++) {
            int r = i + uniform(hi - i + 1); // between i and hi
            Object temp = a[i];
            a[i] = a[r];
            a[r] = temp;
        }
    }

    /**
     * Rearranges the elements of the specified subarray in uniformly random order.
     *
     * @param a  the array to shuffle
     * @param lo the left endpoint (inclusive)
     * @param hi the right endpoint (inclusive)
     *
     * @throws NullPointerException      if <code>a</code> is <code>null</code>
     * @throws IndexOutOfBoundsException unless <code>(0 &le; lo) &amp;&amp; (lo &le; hi) &amp;&amp; (hi &lt; a.length)</code>
     */
    public static void shuffle(double[] a, int lo, int hi) {
        if (a == null)
            throw new NullPointerException("argument array is null");
        if (lo < 0 || lo > hi || hi >= a.length) {
            throw new IndexOutOfBoundsException("Illegal subarray range");
        }
        for (int i = lo; i <= hi; i++) {
            int r = i + uniform(hi - i + 1); // between i and hi
            double temp = a[i];
            a[i] = a[r];
            a[r] = temp;
        }
    }

    /**
     * Rearranges the elements of the specified subarray in uniformly random order.
     *
     * @param a  the array to shuffle
     * @param lo the left endpoint (inclusive)
     * @param hi the right endpoint (inclusive)
     *
     * @throws NullPointerException      if <code>a</code> is <code>null</code>
     * @throws IndexOutOfBoundsException unless <code>(0 &le; lo) &amp;&amp; (lo &le; hi) &amp;&amp; (hi &lt; a.length)</code>
     */
    public static void shuffle(int[] a, int lo, int hi) {
        if (a == null)
            throw new NullPointerException("argument array is null");
        if (lo < 0 || lo > hi || hi >= a.length) {
            throw new IndexOutOfBoundsException("Illegal subarray range");
        }
        for (int i = lo; i <= hi; i++) {
            int r = i + uniform(hi - i + 1); // between i and hi
            int temp = a[i];
            a[i] = a[r];
            a[r] = temp;
        }
    }

}
