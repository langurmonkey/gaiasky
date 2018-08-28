Scripting
*********

Gaia Sky offers the possibility to run ``Python`` scripts in the same
``JVM`` using `Jython <http://www.jython.org/>`__.

You can find some example scripts in the
`scripts <http://github.com/langurmonkey/gaiasky/tree/master/assets/scripts>`__
folder of the project.

.. hint:: Add your own scripts to the folder ``$HOME/.gaiasky/scripts`` so that Gaia Sky can run them.

An interface is provided in order to encapsulate some complex-behaviour
functions and to make scripting easier. This scripting interface is
described in the following section.


The scripting API
=================

The scripting API is a set of methods which can be called to interact with Gaia Sky. The available methods differ depending on the version of Gaia Sky.

API documentation
-----------------

The only up-to-date API documentation for each version is in the interface header files themselves. Below is a list of links to the different APIs.

- `API Gaia Sky master (development branch) <https://github.com/langurmonkey/gaiasky/blob/master/core/src/gaia/cu9/ari/gaiaorbit/script/IScriptingInterface.java>`__
- `API Gaia Sky 2.0.3 <https://github.com/langurmonkey/gaiasky/blob/2.0.3/core/src/gaia/cu9/ari/gaiaorbit/script/IScriptingInterface.java>`__
- `API Gaia Sky 2.0.2 <https://github.com/langurmonkey/gaiasky/blob/2.0.2/core/src/gaia/cu9/ari/gaiaorbit/script/IScriptingInterface.java>`__
- `API Gaia Sky 2.0.1 <https://github.com/langurmonkey/gaiasky/blob/2.0.1/core/src/gaia/cu9/ari/gaiaorbit/script/IScriptingInterface.java>`__
- `API Gaia Sky 2.0.0 <https://github.com/langurmonkey/gaiasky/blob/2.0.0/core/src/gaia/cu9/ari/gaiaorbit/script/IScriptingInterface.java>`__
- `API Gaia Sky 1.5.0 <https://github.com/langurmonkey/gaiasky/blob/1.5.0/core/src/gaia/cu9/ari/gaiaorbit/script/IScriptingInterface.java>`__
- `API Gaia Sky 1.0.4 <https://github.com/langurmonkey/gaiasky/blob/1.0.4/core/src/gaia/cu9/ari/gaiaorbit/script/IScriptingInterface.java>`__
- `API Gaia Sky 1.0.3 <https://github.com/langurmonkey/gaiasky/blob/1.0.3/core/src/gaia/cu9/ari/gaiaorbit/script/IScriptingInterface.java>`__
- `API Gaia Sky 1.0.2 <https://github.com/langurmonkey/gaiasky/blob/1.0.2/core/src/gaia/cu9/ari/gaiaorbit/script/IScriptingInterface.java>`__
- `API Gaia Sky 1.0.1 <https://github.com/langurmonkey/gaiasky/blob/1.0.1/core/src/gaia/cu9/ari/gaiaorbit/script/IScriptingInterface.java>`__
- `API Gaia Sky 1.0.0 <https://github.com/langurmonkey/gaiasky/blob/1.0.0/core/src/gaia/cu9/ari/gaiaorbit/script/IScriptingInterface.java>`__


Using the scripting API
-----------------------

In order to import the scripting interface package in your script, you
just need to import the default implementation
``EventScriptingInterface`` from the package ``gaia.cu9.ari.gaiaorbit.script``:

.. code:: python

    # Import scripting interface
    from gaia.cu9.ari.gaiaorbit.script import EventScriptingInterface

Then, we need to get the scripting interface instance before start using it.

.. code:: python

    gs = EventScriptingInterface.instance()

Now, we can start executing functions.

.. code:: python

    # Disable input
    gs.disableInput()
    gs.cameraStop()
    gs.minimizeInterfaceWindow()

    # Welcome
    gs.setHeadlineMessage("Welcome to the Gaia Sky")
    gs.setSubheadMessage("Explore Gaia, the Solar System and the whole Galaxy!")
    [...]


Synchronizing with the main loop cycles
---------------------------------------

Sometimes, when updating animations or creating camera paths, it is necessary to 
sync the execution of scripts with the thread which runs the main loop (main thread). 
However, the scripting engine runs scripts in separate threads asynchronously, 
making it a non-obvious task to achieve this synchronization.
In order to fix this, a new mechanism has been added in Gaia Sky ``2.0.3``. Now, runnables
can be parked so that they run at the end of the update-render processing of each loop
cycle. A runnable is a class which extends ``java.lang.Runnable``, and implements 
a very simple ``public void run()`` method.

Runnables can be **posted**, meaning that they are run only once at the end fo the current
cycle, or **parked**, meaning that they run until they stop or they are unparked. Parked
runnables must provide a name identifier in order to be later accessed and unparked.

Let's see an example:

.. code:: python

    from gaia.cu9.ari.gaiaorbit.script import EventScriptingInterface
    from java.lang import Runnable

    class PrintRunnable(Runnable):
        def run(self):
            print("I RUN!")

    class FrameCounterRunnable(Runnable):
        def __init__(self):
            self.n = 0

        def run(self):
            self.n = self.n + 1
            if self.n % 30 == 0:
                print "Number of frames: %d" % self.n


    gs = EventScriptingInterface.instance()
    # We post a simple runnable which prints "I RUN!" once
    gs.postRunnable(PrintRunnable())
    # We park a runnable which counts the frames and prints the current number 
    # of frames every 30 of them
    gs.parkRunnable("frame_counter", FrameCounterRunnable())
    gs.sleep(30.0)
    # We unpark the frame counter
    gs.unparkRunnable("frame_counter")
    print "Exiting script"


In this example, we create two runnables. The first, which only prints 'I RUN!" on
the console, is posted using ``postRunnable(Runnable)``, so it only runs once. The
second, which counts frames, is parked with ``parkRunnable(String, Runnable)``, so it
runs until we unpark it with ``unparkRunnable(String)``. The parked runnable is run
every cycle, so it is able to count the frames and print its progress every
30th execution.

A more useful example can be found `here <https://gitlab.com/langurmonkey/gaiasky/blob/master/assets/scripts/showcases/line-objects-update.py>`__. In this script, a polyline is created between the Earth and the Moon. Then, a
parked runnable is used to update the line points with the new postions of the bodies. Finally,
time is started so that the bodies start moving and the line positions are updated correctly and in
synch with the main thread.

More examples
-------------

You can find more examples by looking at the ``scripts``
`folder <http://github.com/langurmonkey/gaiasky/tree/master/assets/scripts>`__ in the
Gaia Sky package.

How to run scripts
------------------

Each script is executed in its own thread in the virtual machine, and
runs alongside Gaia Sky. In order to run a script, follow the
procedure described in the :ref:`running-scripts` section.
