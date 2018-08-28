.. _configuration:

Settings and configuration
**************************

Gaia Sky can be configured using the provided ``GUI`` (**G**\ raphical **U**\ ser **I**\ nterface).
However, some functions are not represented in the GUI, so you may need
to dive deep into the :ref:`properties file <properties-file>`.

.. hint:: As of version ``1.0.5`` the old swing-based preferences dialog has been deprecated in favour of an OpenGL-based one, so it does not pop up at startup anymore.

Graphics settings
=================

Please refer to the :ref:`graphics-conf` chapter.

.. _user-interface-config:

User interface
==============

The ``User Interface`` section allows the user to set the language and the
theme of the user interface.

One can select between a choice of languages using the language
drop-down. There are currently three visual themes available:

-  ``dark-green``, black and green theme.
-  ``dark-green-x2``, scaled version of dark-green (for HiDPI screens).
-  ``dark-blue``, black and blue theme.
-  ``dark-blue-x2``, scaled version of dark-blue (for HiDPI screens).
-  ``dark-orange``, orange and blue theme.
-  ``dark-orange-x2``, scaled version of dark-orange (for HiDPI screens).
-  ``bright-green``, a bright theme with greenish tones.
-  ``bright-green-x2``, scaled version of birght-green (for HiDPI screens).
-  ``night-red``, a red theme for low-light environments.
-  ``night-red-x2``, scaled version of night-red (for HiDPI screens).

Performance
===========

In the ``Performance`` tab you can select how many background worker threads can be
used by Gaia Sky. By default, multithreading is enabled and the number of threads 
is equal to the number of CPU coures (setting ``let the program decide``).

More detailed info can be found in the :ref:`performance section <performance>`.

Draw distance
-------------

Certain big datasets use levels of detail (LOD) to prevent data clutter and make
them manageable. Using this setting the user can adjust the draw distance, allowing
more stars into the viewport.
Refer to the :ref:`draw-distance` section for more info.

Controls
========

You can see the key associations in the ``Controls`` tab. Controls are
not editable. Check out the `Controls <Controls.rst>`_\  documentation
to know more.

.. _screenshots-configuration:

Screenshot configuration
========================

You can take screenshots anytime when the application is running by
pressing ``F5``. There are two screenshot modes available: \*
``Simple``, the classic screenshot of what is currently on screen, with
the same resolution. \* ``Advanced``, where you can define the
resolution of the screenshots.
Additionally, the output format (either ``JPG`` or ``PNG``) and the quality (in
case of ``JPG`` format) can also be set in the configuration file, usually
located in ``$HOME/.gaiasky/global.properties``. The keys to modify are:

*  ``screenshot.format``
*  ``screenshot.quality``


.. _frame-output-config:

Frame output
============

There is a feature in Gaia Sky that enables the output of every
frame as a ``JPG`` or ``PNG`` image. This is useful to produce videos. In order to
configure the frame output system, use the ``Frame output`` tab. There
you can select the output folder, the image prefix name, the output
image resolution (in case of ``Advanced`` mode) and the target frame rate.
Additionally, the output format (either ``JPG`` or ``PNG``) and the quality (in
case of ``JPG`` format) can also be set in the configuration file, usually
located in ``$HOME/.gaiasky/global.properties``. The keys to modify are:

*  ``graphics.render.format``
*  ``graphics.render.quality``

.. note:: Use ``F6`` to activate the frame output mode and start saving each frame as an image. Use ``F6`` again to deactivate it.

When the program is in frame output mode, it does not run in
real time but it adjusts the internal clock to produce as many frames
per second as specified here. You have to take it into account when you
later use your favourite video encoder
(`ffmpeg <https://www.ffmpeg.org/>`__) to convert the frame images into
a video.

.. _camera-recording-config:

Camera recording
================

Here you can set the desired **frames per second** to capture the camera
paths. If your device is not fast enough in producing the specified
frame rate, the application will slow down while recording so that
enough frames are captured. Same behaviour will be uploading during
camera playback.

You can also enable **automatic frame recording** during playback. This will
automatically activate the frame output system (see :ref:`frame-output-config`)
during a camera file playback.

.. _360-mode-config:

360 mode
========

Here you can define the **cube map side resolution** for the 360 mode.
With this mode a cube map will be rendered (the whole scene in all
direction ``+X``, ``-X``, ``+Y``, ``-Y``, ``+Z``, ``-Z``) and then
it will be transformed into a flat image using an equirectangular
projection. This allows for the creation of 360 (VR) videos.

.. figure:: img/screenshots/360/20161111_screenshot_00000.jpg
   :width: 90%
   :alt: 360 mode in Gaia Sky

   360 mode in Gaia Sky


Data
====

As of version ``1.0.0`` you can use the **Data** tab to select the
catalogues to load. You can select as many catalogs as you want.

The Data tab can also be used to toggle between high and low accuracy positions and
to enalbe a catalog selection screen at startup.

Gaia
====

Here you can choose the attitude of the satellite. You can either use
the ``real attitude`` (takes a while to load but will ensure that Gaia
points to where it should) and the ``NSL``, which is an analytical
implementation of the nominal attitude of the satellite. It behaves the
same as the real thing, but the observation direction is not ensured.


System
======

Use this tab to enable and disable the debug info using the ``Show debug info`` checkbox. When the
debug info is enabled, the program prints the frames per
second and other useful information at the top-right of the screen. 

Also, you can enable anonymous usage reporting, which connects to an analytics web service 
to build up some usage statistics. Only the events of application startup and shutdown are reported, and they
are totally anonymous. This setting is disabled by default to protect the user's privacy.

Finally, if you want to revert to the default settings, click on the ``Reload default settings`` button.

