User Interface
**************

.. _download-manager:

Data download manager
=====================

As of version ``2.1.0`` Gaia Sky provides an integrated download manager to help visualize and obtain the
available data packs and catalogs. Chances are that the download manager is the first thing you see when you launch
Gaia Sky for the first time.

.. figure:: img/dm/dm.png
  :alt: The download manager in action
  :width: 100%
  
The download manager pops up automatically when Gaia Sky is started if no base data or no catalog files are detected. It
can also be launched manually from the preferences window, data tab.

Using the download manager, the user can select whatever datasets she wants, then click download and wait for the download and
extract processes to finish. Once done, the data will be available to Gaia Sky the next time it starts.


GUI window
==========

The Gaia Sky GUI is divided into seven panes, `Time <#time>`__,
`Camera <#camera>`__, `Type visibility <#type-visibility>`__, `Visual settings <#visual-settings>`__, `Datasets <#datasets>`__, `Objects <#objects>`__,  and `Music <#music>`__.

+----------------------------------------------------+-----------------------------------------------+
| .. image:: img/ui/gs-interface-collapsed.png       | Controls window with all panes, except the    |
|   :width: 15%                                      | Time pane, collapsed.                         |
|   :alt: User interface with all panes collapsed    |                                               |
+----------------------------------------------------+-----------------------------------------------+            
| .. image:: img/ui/gs-interface-expanded.png        | Controls window with the Time pane and the    |
|   :width: 15%                                      | Camera pane expanded.                         |
|   :alt: User interface with camera pane expanded   |                                               |
+----------------------------------------------------+-----------------------------------------------+

The seven panes, except for the Time pane, are collapsed at startup. To expand them and reveal its controls just click on the little plus 
icon |plus-icon| at the right of the pane title. Use the minus icon |minus-icon| to collapse it again. Panes can also be detached
to their own window. To do so, use the detach icon |detach-icon|.

.. |plus-icon| image:: img/ui/plus-icon.png
.. |minus-icon| image:: img/ui/minus-icon.png
.. |detach-icon| image:: img/ui/detach-icon.png

Time
----

You can play and pause the simulation using the |play-icon|/|pause-icon| ``Play/Pause`` buttons in
the ``Controls`` window to the left. You can also use ``SPACE`` to play
and pause the time. You can also change time warp, which is expressed as
a factor. Use ``,`` and ``.`` to divide by 2 and double the value of the
time warp.

.. |play-icon| image:: img/ui/play-icon.png
.. |pause-icon| image:: img/ui/pause-icon.png

Camera
------

In the camera options pane on the left you can select the type of
camera. This can also be done by using the ``NUMPAD 0-4`` keys.

There are five camera modes:

* ``Free mode`` -- The camera is not linked to any object and its velocity is exponential with respect to the distance to the origin (Sun).
* ``Focus mode`` -- The camera is linked to a focus object and it rotates and rolls with respect to it.
* ``Gaia scene`` -- Provides an outside view of the Gaia satellite. The camera can not be rotated or translated in this mode.
* ``Spacecraft``-- Take control of a spacecraft and navigate around at will.
* ``Gaia FOV`` -- The camera simulates either of the fields of view of Gaia, or both.

For more information on the camera modes, see the :ref:`camera-modes` section.

Additionally, there are a number of sliders for you to control different
parameters of the camera:

-  **Field of view**: Controls the field of view angle of the camera.
   The bigger it is, the larger the portion of the scene represented.
-  **Camera speed**: Controls the longitudinal speed of the camera.
-  **Rotation speed**: Controls the transversal speed of the camera, how
   fast it rotates around an object.
-  **Turn speed**: Controls the turning speed of the camera.

You can **lock the camera** to the focus when in focus mode. Doing so
links the reference system of the camera to that of the object and thus
it moves with it.

.. hint:: **Lock the camera** so that it stays at the same relative position to the focus object.

Finally, we can also **lock the orientation** of the camera to that of
the focus so that the same transformation matrix is applied to both.

.. hint:: **Lock the orientation** so that the camera also rotates with the focus.

Additionally, we can also enable the **crosshair**, which will mark the
currently focused object.

Type visibility
---------------

Most graphical elements can be turned off and on using these toggles.
For example you can remove the stars from the display by clicking on the
``stars`` toggle. The object types available are the following:

-  Stars
-  Planets
-  Moons
-  Satellites, the spacecrafts
-  Asteroids
-  Labels, all the text labels
-  Equatorial grid
-  Ecliptic grid
-  Galactic grid
-  Orbits, the orbit lines
-  Atmospheres, the atmospheres of planets
-  Constellations, the constellation lines
-  Boundaries, the constellation boundaries
-  Milky way
-  Others

By checking the **proper motion vectors** checkbox we can enable the
representation of star proper motions if the currently loaded catalog
provides them. Once proper motions are activated, we can control the
number of displayed proper motions and their length by using the two
sliders that appear.

.. _interface-lighting:
.. _visua-settings:

Visual settings
---------------

Here are a few options to control the lighting of the scene:

-  **Star brightness**: Controls the brightness of stars.
-  **Star size**: Controls the size of point-like stars.
-  **Min. star opacity**: Sets a minimum opacity for the faintest stars.
-  **Ambient light**: Controls the amount of ambient light. This only
   affects the models such as the planets or satellites.
-  **Label size**: Controls the size of the labels.

Objects
-------

There is a list of focus objects that can be selected from the
interface. When an object is selected the camera automatically centers
it in the view and you can rotate around it or zoom in and out. Objects
can also be selected by double-clicking on them directly in the view or
by using the search box provided above the list. You can also invoke a
search dialogue by pressing ``CTRL+F``.

Datasets
--------

This tab contains all the datasets currently loaded. Datasets are usually star
catalogs which can be loaded independently. For example, any DR2 catalogs will
be shown here. Also, datasets added with SAMP are displayed in this section. 
For each dataset, controls to mute/unmute it and delete it are provided.

Music
-----

Since version ``0.800b``, Gaia Sky also offers a music player in its
interface. By default it ships with only a few *spacey* melody, but you
can add your own by dropping them in the folder ``$HOME/.gaiasky/music``.

.. hint:: Drop your ``mp3``, ``ogg`` or ``wav`` files in the folder ``$HOME/.gaiasky/music`` and these will be available during your Gaia Sky sessions to play.

In order to start playing, click on the |audio-play| ``Play`` button. To pause the track, click on the |audio-pause| ``Pause`` icon. To skip to the next track,
click on the |audio-fwd| ``Forward`` icon. To go to the previous track, click on the |audio-bwd| ``Backward`` icon.
The volume can be controlled using the slider at the bottom of the pane.

.. |audio-play| image:: img/ui/audio-play.png
.. |audio-pause| image:: img/ui/audio-pause.png
.. |audio-fwd| image:: img/ui/audio-fwd.png
.. |audio-bwd| image:: img/ui/audio-bwd.png


Bottom buttons
==============

The buttons at the bottom of the control panel are described here.

Preferences window
------------------

You can launch the preferences window any time during the execution of
the program. To do so, click on the |prefsicon| ``Preferences`` button at the bottom
of the GUI window. For a detailed description of the configuration
options refer to the :ref:`Configuration
Instructions <configuration>`.

.. |prefsicon| image:: img/ui/prefs-icon.png

.. _running-scripts:

Running scripts
---------------

In order to run Python scripts, click on the |scriptrun| ``Run script`` button at
the bottom of the GUI window. A new window will pop up allowing you to
select the script you want to run. Once you have selected it, the script
will be checked for errors. If no errors were found, you will be
notified in the box below and you'll be able to run the script right
away by clicking on the ``Run`` button. If the script contains errors,
you will be notified in the box below and you will not be able to run
the script until these errors are dealt with.

.. hint:: Add your own scripts to the folder ``$HOME/.gaiasky/scripts`` so that Gaia Sky can find them.

.. |scriptrun| image:: img/ui/car-icon.png

About/help
----------

The help button |helpicon| brings up the help dialog, where information on the current system,
OpenGL settings, Java memory, updates and contact can be found.

.. |helpicon| image:: img/ui/help-icon.png

Log
---

The log button |logicon| brings up the log window, which displays the Gaia Sky log
for the current session. The log can be exported to a file by clicking on the ``Export to
file`` button. The location of the exported log files is ``~/.gaiasky``.

.. |logicon| image:: img/ui/log-icon.png


Spacecraft UI controls
======================

The spacecraft mode UI is described in  :ref:`spacecraft-mode`.



