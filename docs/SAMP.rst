SAMP integration
****************

As of commit `4d0d133 <https://gitlab.com/langurmonkey/gaiasky/commit/4d0d13304d1e2b6991ad2cc84429a37083ae0954>`__, or
version ``2.0.0``, Gaia Sky supports interoperability via `SAMP <http://www.ivoa.net/documents/SAMP/>`__.
However, due to the nature of Gaia Sky, not all functions are yet implemented and not all types of data tables
are supported.

Since Gaia Sky only displays 3D positional information there are a few restrictions as to how the integration with SAMP is implemneted.

The current implementation only allows using Gaia Sky as a SAMP client. This means that
when Gaia Sky is started, it automatically looks for a preexisting SAMP hub. If it is found, then
a connection is attempted. If it is not found, then Gaia Sky will attempt further
connections at regular intervals of 10 seconds. Gaia Sky will
never run its own SAMP hub, so the user always needs a SAMP-hub application (Topcat,
Aladin, etc.) to use the interoperability that SAMP offers.

Also, the only supported format in SAMP is VOTable through the STIL data provider described below. 


.. _stil-data-provider:

STIL data provider
==================

Gaia Sky supports the loading of data in VOTable, CSV, ASCII, etc. using the ``STIL`` `library <http://www.star.bristol.ac.uk/~mbt/stil/>`__.
It tries to make educated guesses using UCDs and column names to attribute semantics to columns.
Here is what this provider can work with: 


Positions
---------

For the **positional data**, Gaia Sky will look for spherical and cartesian coordinates. In the case of spherical coordinates, the following are supported: 

-  Equatorial (`pos.eq.ra`, `pos.eq.dec`)
-  Galactic (`pos.galactic.lon`, `pos.galactic.lat`)
-  Ecliptic (`pos.ecliptic.lon`, `pos.ecliptic.lat`)

To work out the distance, it looks for `pos.parallax` and `pos.distance`. If either of those are found, they are used. Otherwise, a default parallax of 0.04 mas is used. 
With respect to cartesian coordinates, it recognizes `pos.cartesian.x|y|z`, and they are interpreted in the equatorial system by default.
If no UCDs are available, only equatorial coordinates (ra, dec) are supported, and they are looked up using the column names.

Proper motions
--------------

**Proper motions** are not yet supported via SAMP.

Magnitudes
----------

**Magnitudes** are supported using the `phot.mag` or `phot.mag;stat.mean` UCDs. Otherwise, they are
discovered using the column names `mag`, `bmag`, `gmag`, `phot_g_mean_mag`. If no magnitudes are found,
the default value of 15 is used.


Colors
------

**Colors** are discovered using the `phot.color` UCD. If not present, the column names `b_v`, `v_i`,
`bp_rp`, `bp_g` and `g_rp` are used, if present. If no color is discovered at all, the default value of 0.656 is used.


Others
------

Other physical quantities (mass, flux, T_eff, radius, etc.) are not yet supported via SAMP.

Implemented features
====================

The following SAMP features are implemented:

-  Load VOTable (``table.load.votable``) - The VOTable will be loaded into Gaia Sky if it adheres to the format above.
-  Highlight row (``table.highlight.row``) - The row (object) is set as the new focus if the table it comes frome is already loaded. Otherwise, Gaia Sky will **not** load the table lazily.
-  Broadcast selection (``table.highlight.row``) - When a star of a table loaded via SAMP is selected, Gaia Sky broadcasts it as a row highlight, so that other clients may act on it.
-  Point at sky (``coord.pointAt.sky``) - Puts camera in free mode and points it to the specific direction.
-  Multi selection (``table.select.rowList``) - Gaia Sky does not have multiple selections so far, so only the first one is used right now.

Unimplemented features
======================

The following SAMP functions are not yet implemented:

-  ``table.load.*`` - Only VOTable supported.
-  ``image.load.fits``
-  ``spectrum.load.ssa-generic``
-  ``client.env.get``
-  ``bibcode.load``
-  ``voresource.loadlist``
-  ``coverage.load.moc.fits``



