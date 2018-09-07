Running Gaia Sky
****************

If you installed the software using an installer or a package manager
system (``rpm``, ``deb``), you just need to use the standard running
procedures of your Operating System to run the application.

**Linux**

Just type ``gaiasky`` in a terminal or use your favourite desktop
environment search engine to run the Gaia Sky launcher.

**Windows**

In windows, this means clicking on ``Start`` and then browsing the start
menu folder ``Gaia Sky``. You can run the executable from there.

**macOS X**

Locate the launcher in your install directory (usually ``/Applications``) and double click on it.

**Code and pakcage**

However, if you are a maverick and do not like installers, you can also
run the Gaia Sky directly from the source code in ``GitHub`` or
using the ``tgz`` package.

CLI arguments
=============

Gaia Sky offers a few command line arguments:

.. code-block:: bash

    $  gaiasky -h
            
       Usage: gaiasky [options]
           Options:
              -c, --cat-chooser
                Displays the catalog chooser dialog at startup
                Default: false
              -d, --ds-download
                Displays the download dialog at startup
                Default: false
              -h, --help
                Shows help
              -v, --version
                Lists version and build inforamtion
                Default: false


.. _running-from-source:

Running from source
===================

Requirements
------------

If you want to compile the source code, you will need the following:

-  ``JDK8+``
-  ``git``

Please, be aware that only ``tags`` are guaranteed to work
(`here <https://gitlab.com/langurmonkey/gaiasky/tags>`__). The ``master``
branch holds the development version and the configuration files are
possibly messed up and not ready to work out-of-the-box. So remember to
use a ``tag`` version if you want to run it right away from source.

First, clone the repository:

.. code-block:: bash

    $  git clone https://gitlab.com/langurmonkey/gaiasky.git

Getting the catalog data
------------------------

.. hint:: As of version ``2.1.0``, Gaia Sky provides a self-contained downloade manager to get all the data packs available.  

Gaia Sky offers a downloade manager to get the desired data packs. The ``base-data`` pack is necessary for Gaia Sky to run, and
contains the solar system, the Milky Way model, NBG, SDSS, etc. Catalog files are optional. You can bring up the downloade manager at
any time by clicking on the button ``Data download`` in the preferences window, data tab. More information on the download
manager can be found in :ref:`download-manager`.

You can also download the data packs manually `here <http://gaia.ari.uni-heidelberg.de/gaiasky/files/autodownload>`_.

Compiling and running
---------------------

To compile the code and run Gaia Sky run the following.

.. code-block:: bash

    $  gradlew core:run
    
In order to pull the latest changes from the GitHub repository:

.. code-block:: bash

	$  git pull


Packaging Gaia Sky
-----------------

Gaia Sky can be exported to a folder to be run as a standalone app with the following.

.. code-block:: bash

	$ gradlew core:dist
	
That will create a new folder called ``releases/gaiasky-[version].[revison]`` with the exported application. Run scripts
are provided with the name ``gaiasky`` (Linux, macOS) and ``gaiasky.cmd`` (Windows).

Also, to export Gaia Sky into a ``tar.gz`` archive file, run the following.

.. code-block:: bash

    $  gradlew core:createTar

In order to produce the desktop installers for the various systems you
need a licensed version of ``Install4j``. Then, you need to run:

.. code-block:: bash

    $  gradlew core:pack

These command will produce the different OS packages (``.exe``, ``.dmg``, ``.deb``, ``.rpm``, etc.) 
of Gaia Sky into ``releases/packages-[version].[revision]`` folder.

Running from downloaded package
===============================

If you prefer to run the application from the ``tar.gz`` package, follow the instructions below.

Linux
-----

In order to run the application on Linux, open the terminal, uncompress
the archive, give execution permissions to the ``gaiasky`` script and then
run it.

.. code-block:: bash

    $  tar zxvf gaiasky-[version].tar.gz
    $  cd gaiasky-[version]/
    $  gaiasky

Windows
-------

In order to run the application on Windows, open a terminal window (type
``cmd`` in the start menu search box) and run the ``gaiasky.cmd`` file.

.. code-block:: bash

    $  cd path_to_gaiasky_folder
    $  gaiasky.cmd

macOS X
-------

To run the application on macOS, follow the instructions in the
`Linux <#linux>`__ section.
