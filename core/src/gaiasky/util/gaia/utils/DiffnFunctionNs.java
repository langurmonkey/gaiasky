/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.utils;

/**
 * 
 * This interface provides the method through which a set of ODEs may be coded
 * and supplied to the methods in the class RungeKutta. This class is taken from
 * the Java library developed by Michael Thomas Flanagan available at
 * <a href="www.ee.ucl.ac.uk/~mflanaga">Michael Thomas Flanagan</a>.
 * 
 * <p>NOTE: This is for RSLS use only - do not use in any other software</p>
 *
 * @author Francesca De Angeli (fda@ast.cam.ac.uk)
 * 
 *         Modified by L. Lindegren to use long (e.g. time in ns) as independent
 *         variable
 * 
 */
public interface DiffnFunctionNs {
    /**
    	 * 
    	 * 
    	 * @param t
    	 *            time
    	 * @param y
    	 *            the set of dependent variables
    	 * 
    	 * @return double[] derivatives of y with respect to time
    	 */
    double[] derivn(final long t, final double[] y);
}
