Running Gaia Sky
****************

If you installed the software using an installer or a package manager
system (``rpm``, ``deb``), you just need to use the standard running
procedures of your Operating System to run the application.

**Windows**

In windows, this means clicking on ``Start`` and then browsing the start
menu folder ``Gaia Sky``. You can run the executable from there.

**Linux**

Just type ``gaiasky`` in a terminal or use your favourite desktop
environment search engine to run the Gaia Sky launcher.

**macOS X**

Locate the launcher in your install directory (usually ``/Applications``) and double click on it.

**Code and pakcage**

However, if you are a maverick and do not like installers, you can also
run the Gaia Sky directly from the source code in ``GitHub`` or
using the ``tgz`` package.


.. _running-from-source:

Running from source
===================

Requirements
------------

If you want to compile the source code, you will need the following:

-  `JDK8 or
   above <http://www.oracle.com/technetwork/java/javase/downloads/index.html>`__

Please, be aware that only ``tags`` are guaranteed to work
(`here <https://github.com/langurmonkey/gaiasky/tags>`__). The ``master``
branch holds the development version and the configuration files are
possibly messed up and not ready to work out-of-the-box. So remember to
use a ``tag`` version if you want to run it right away from source.

Also, this guide is for **Unix-like systems only**. If you are working
on Windows, you will need `git for
windows <http://git-scm.com/download/win>`__, which contains a version of
MinGW (bash) packed with ``git``, ``vim`` and some other utils. All other
parts of the process should work the same under Windows systems.

First, clone the repository:

.. code-block:: bash

    $  git clone https://github.com/langurmonkey/gaiasky.git
    $  cd gaiasky

Getting the catalog data
------------------------

.. hint:: As of version ``2.0.3``, Gaia Sky will automatically download the default catalog if no other catalog is found.  

If you have version ``2.0.3`` or above, Gaia Sky will scan some folders
looking for catalog files. if no catalog files are found, Gaia Sky will
offer to download the default catalog. If you want any other catalog listed
below, you need to download it. The catalog files are not in the repository.

You can get other catalogs `here <https://zah.uni-heidelberg.de/institutes/ari/gaia/outreach/gaiasky/downloads/#dr2catalogs>`_.

Compiling and running
---------------------

To compile the code and run Gaia Sky run the following.

.. code-block:: bash

    $  gradlew core:run
    
In order to pull the latest changes from the GitHub repository:

.. code-block:: bash

	$  git pull
	
Remember that the master branch is the development branch and therefore intrinsically unstable. It is not guaranteed to always work.


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
