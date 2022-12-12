/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Clone of the Satellite class in GaiaParams with very few needed parameters.
 */
public class Satellite {

    /**
     * Value of the inertial scan rate (spin rate) of the satellite around the SRS z-axis, defined - for a given CCD TDI period (and along-scan pixel dimension) and the design-value of the telescope focal length - as the speed with which electrons move through the CCDs in TDI mode
     * <p>
     * Status: CDB<br/>
     * Unit: arcsec s^-1<br/>
     * Basic : false<br/>
     * Scalar: true
     */
    @ParamMetaData(description = "Value of the inertial scan rate (spin rate) of the satellite around the SRS z-axis, defined - for a given CCD TDI period (and along-scan pixel dimension) and the design-value of the telescope focal length - as the speed with which electrons move through the CCDs in TDI mode", source = "", status = "CDB", basic = false, scalar = true, unit = "arcsec s^-1")
    public static final double SCANRATE;
    /**
     * Nominal value of the constant angle between the SRS z-axis and the direction to the nominal Sun (also refered to as solar-aspect angle, SAA, revolving angle, and satellite scan-axis tilt angle)
     * <p>
     * Source: ESA, 21 May 2013, 'Gaia mission requirements document (MRD)', GAIA-EST-RD-00553, issue 3, revision 1, Requirement SCI-010. Reference document: A.G.A. Brown, U. Bastian, L. Lindegren, et al., 6 September 2006, 'On the definition of the solar aspect angle', GAIA-CG-TN-LEI-AB-010-02<br/>
     * Status: CONF<br/>
     * Unit: deg<br/>
     * Basic : true<br/>
     * Scalar: true
     */
    @ParamMetaData(description = "Nominal value of the constant angle between the SRS z-axis and the direction to the nominal Sun (also refered to as solar-aspect angle, SAA, revolving angle, and satellite scan-axis tilt angle)", source = "ESA, 21 May 2013, 'Gaia mission requirements document (MRD)', GAIA-EST-RD-00553, issue 3, revision 1, Requirement SCI-010. Reference document: A.G.A. Brown, U. Bastian, L. Lindegren, et al., 6 September 2006, 'On the definition of the solar aspect angle', GAIA-CG-TN-LEI-AB-010-02", status = "CONF", basic = true, scalar = true, unit = "deg")
    public static final double SOLARASPECTANGLE_NOMINAL;
    /**
     * Initial value \nu_0 of the revolving phase \nu at time t_0 corresponding to the scanning-law reference epoch (see parameter :Satellite:Mission_ReferenceEpoch_ScanningLaw_TCB)
     * <p>
     * Source: Current working hypothesis; the real value, which optimises GAREQ, is contained in an MDB table. Reference documents: L. Lindegren, 2 February 1998, 'The scanning law for Gaia', SAG-LL-014 (erratum SAG-LL-014A released 9 February 1998), L. Lindegren, 29 December 1998, 'Simulation of Gaia scanning of arbitrary directions', SAG-LL-026, L. Lindegren, 1 July 2000, 'Attitude parameterisation for Gaia', SAG-LL-030, and L. Lindegren, 19 February 2001, 'Calculating the Gaia nominal scanning law', SAG-LL-035<br/>
     * Status: DEPR<br/>
     * Unit: rad<br/>
     * Basic : true<br/>
     * Scalar: true
     */
    @ParamMetaData(description = "Initial value \\nu_0 of the revolving phase \\nu at time t_0 corresponding to the scanning-law reference epoch (see parameter :Satellite:Mission_ReferenceEpoch_ScanningLaw_TCB)", source = "Current working hypothesis; the real value, which optimises GAREQ, is contained in an MDB table. Reference documents: L. Lindegren, 2 February 1998, 'The scanning law for Gaia', SAG-LL-014 (erratum SAG-LL-014A released 9 February 1998), L. Lindegren, 29 December 1998, 'Simulation of Gaia scanning of arbitrary directions', SAG-LL-026, L. Lindegren, 1 July 2000, 'Attitude parameterisation for Gaia', SAG-LL-030, and L. Lindegren, 19 February 2001, 'Calculating the Gaia nominal scanning law', SAG-LL-035", status = "DEPR", basic = true, scalar = true, unit = "rad")
    public static final double REVOLVINGPHASE_INITIAL;
    /**
     * Initial value \Omega_0 of the scan (or spin) phase \Omega at time t_0 corresponding to the scanning-law reference epoch (see parameter :Satellite:Mission_ReferenceEpoch_ScanningLaw_TCB)
     * <p>
     * Source: Current working hypothesis; the real value, which optimises GAREQ, is contained in an MDB table. Reference documents: L. Lindegren, 2 February 1998, 'The scanning law for Gaia', SAG-LL-014 (erratum SAG-LL-014A released 9 February 1998), L. Lindegren, 29 December 1998, 'Simulation of Gaia scanning of arbitrary directions', SAG-LL-026, L. Lindegren, 1 July 2000, 'Attitude parameterisation for Gaia', SAG-LL-030, and L. Lindegren, 19 February 2001, 'Calculating the Gaia nominal scanning law', SAG-LL-035<br/>
     * Status: DEPR<br/>
     * Unit: rad<br/>
     * Basic : true<br/>
     * Scalar: true
     */
    @ParamMetaData(description = "Initial value \\Omega_0 of the scan (or spin) phase \\Omega at time t_0 corresponding to the scanning-law reference epoch (see parameter :Satellite:Mission_ReferenceEpoch_ScanningLaw_TCB)", source = "Current working hypothesis; the real value, which optimises GAREQ, is contained in an MDB table. Reference documents: L. Lindegren, 2 February 1998, 'The scanning law for Gaia', SAG-LL-014 (erratum SAG-LL-014A released 9 February 1998), L. Lindegren, 29 December 1998, 'Simulation of Gaia scanning of arbitrary directions', SAG-LL-026, L. Lindegren, 1 July 2000, 'Attitude parameterisation for Gaia', SAG-LL-030, and L. Lindegren, 19 February 2001, 'Calculating the Gaia nominal scanning law', SAG-LL-035", status = "DEPR", basic = true, scalar = true, unit = "rad")
    public static final double SCANPHASE_INITIAL;
    /**
     * The number of spin-axis revolutions around the solar direction per Julian year. The value K = 5.8 guarantees a good long-term scan pattern on the sky. The basic requirement for K is to be non-integer such that K * xi >= 260 degrees (see SAG-LL-014)
     * <p>
     * Status: CONF<br/>
     * Unit: yr^-1<br/>
     * Basic : true<br/>
     * Scalar: true
     */
    @ParamMetaData(description = "The number of spin-axis revolutions around the solar direction per Julian year. The value K = 5.8 guarantees a good long-term scan pattern on the sky. The basic requirement for K is to be non-integer such that K * xi >= 260 degrees (see SAG-LL-014)", source = "", status = "CONF", basic = true, scalar = true, unit = "yr^-1")
    public static final double SPINAXIS_NUMBEROFLOOPSPERYEAR;
    /**
     * Basic angle, i.e., the angle between FoV2 (following FoV) and FoV1 (preceding FoV). The sense of rotation is mathematically positive (counter-clockwise) about the +Z-axis of the SRS (Scanning Reference System; see U. Bastian, 5 July 2007, 'Reference systems, conventions, and notations for Gaia', GAIA-CA-SP-ARI-BAS-003-06, issue 6, revision 1, formerly known as GAIA-ARI-BAS-003); stars thus transit FoV1 first
     * <p>
     * Source: EADS-Astrium, 4 June 2013, 'PayLoad Module (PLM) Requirements Specification', GAIA.ASF.SP.PLM.00009, issue 6, revision 0, Requirement PLM-386 and ESA, 21 May 2013, 'Gaia mission requirements document (MRD)', GAIA-EST-RD-00553, issue 3, revision 1, Requirement SCI-120<br/>
     * Status: CONF<br/>
     * Unit: deg<br/>
     * Basic : true<br/>
     * Scalar: true
     */
    @ParamMetaData(description = "Basic angle, i.e., the angle between FoV2 (following FoV) and FoV1 (preceding FoV). The sense of rotation is mathematically positive (counter-clockwise) about the +Z-axis of the SRS (Scanning Reference System; see U. Bastian, 5 July 2007, 'Reference systems, conventions, and notations for Gaia', GAIA-CA-SP-ARI-BAS-003-06, issue 6, revision 1, formerly known as GAIA-ARI-BAS-003); stars thus transit FoV1 first", source = "EADS-Astrium, 4 June 2013, 'PayLoad Module (PLM) Requirements Specification', GAIA.ASF.SP.PLM.00009, issue 6, revision 0, Requirement PLM-386 and ESA, 21 May 2013, 'Gaia mission requirements document (MRD)', GAIA-EST-RD-00553, issue 3, revision 1, Requirement SCI-120", status = "CONF", basic = true, scalar = true, unit = "deg")
    public static final double BASICANGLE_DEGREE;
    /**
     * Total FoV (i.e., including 'intra-instrument' but excluding 'inter-instrument' dead zones between CCDs), in the along-scan direction, per viewing direction (telescope)
     * <p>
     * Status: CONF<br/>
     * Unit: deg<br/>
     * Basic : false<br/>
     * Scalar: true
     */
    @ParamMetaData(description = "Total FoV (i.e., including 'intra-instrument' but excluding 'inter-instrument' dead zones between CCDs), in the along-scan direction, per viewing direction (telescope)", source = "", status = "CONF", basic = false, scalar = true, unit = "deg")
    public static final double FOV_AL;
    /**
     * Total FoV (i.e., including dead zones between CCDs) in the across-scan direction per viewing direction (telescope)
     * <p>
     * Status: CONF<br/>
     * Unit: deg<br/>
     * Basic : false<br/>
     * Scalar: true
     */
    @ParamMetaData(description = "Total FoV (i.e., including dead zones between CCDs) in the across-scan direction per viewing direction (telescope)", source = "", status = "CONF", basic = false, scalar = true, unit = "deg")
    public static final double FOV_AC;
    /**
     * Total actice FoV (i.e., excluding dead zones between CCDs) in the across-scan direction per viewing direction (telescope)
     * <p>
     * Status: CONF<br/>
     * Unit: deg<br/>
     * Basic : false<br/>
     * Scalar: true
     */
    @ParamMetaData(description = "Total actice FoV (i.e., excluding dead zones between CCDs) in the across-scan direction per viewing direction (telescope)", source = "", status = "CONF", basic = false, scalar = true, unit = "deg")
    public static final double FOV_AC_ACTIVE;

    static {
        SCANRATE = 59.9641857803;
    } // [arcsec s^-1]

    static {
        SOLARASPECTANGLE_NOMINAL = 45.0;
    } // [deg]

    static {
        REVOLVINGPHASE_INITIAL = 0.0;
    } // [rad]

    static {
        SCANPHASE_INITIAL = 0.0;
    } // [rad]

    static {
        SPINAXIS_NUMBEROFLOOPSPERYEAR = 5.8;
    } // [yr^-1]

    static {
        BASICANGLE_DEGREE = 106.5;
    } // [deg]

    static {
        FOV_AL = 1.16761;
    } // [deg]

    static {
        FOV_AC = 0.69079;
    } // [deg] 

    static {
        FOV_AC_ACTIVE = 0.67586;
    } // [deg]

    // Define our annotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ParamMetaData {
        String source();

        String unit();

        String description();

        String status();

        boolean scalar();

        boolean basic();
    }
}
