.. _graphics-conf:

Graphics settings
*****************

Most of the graphics settings can be adjusted using the ``Preferences dialog``.

Resolution and mode
===================

You can find the ``Resolution and mode`` configuration under the
``Graphics`` tab. There you can switch between full screen mode and
windowed mode. In the case of full screen, you can choose the resolution
from a list of supported resolutions in a drop down menu. If you choose
windowed mode, you can enter the resolution you want. You can also
choose whether the window should be resizable or not. In order to switch
from full screen mode to windowed mode during the execution, use the key
``F11``.


.. _graphics-quality-setting:

Graphics quality
================

This setting governs the size of the textures, the complexity of the
models and also the quality of the graphical effects (``light glow``,
``lens flare``, etc.). Here are the differences:

*  ``High`` Contains some high-resolution textures (4K) and specular and normal maps for most celestial bodies. Planets and moons have a high vertex count. Graphical effects use a large number of samples to get the best visuals.
*  ``Normal`` contains lower resolution textures (2K when available) and some specular and normal maps are deactivated. The graphical effects use a reasonable amount of quality for nice visuals without compromising the performance too much.
*  ``Low`` Offers a noticeable performance gain on less powerful systems. Same textures and model quality as in the ``Normal`` setting. The ``volumetric light`` effect is turned off completely and the ``lens flare`` effect uses a low number of ghosts.

.. _graphics-antialiasing:

Antialiasing
============

In the ``Graphics`` tab you can also find the antialiasing
configuration. Applying antialiasing removes the jagged edges of the
scene and makes it look better. However, it does not come free of cost,
and usually has a penalty on the frames per second (FPS). There are four
main options, described below.

Find more information on antialiasing in the :ref:`performance-antialiasing` section.

**No Antialiasing**

If you choose this no antialiasing will be applied, and therefore you
will probably see jagged edges around models. This has no penalty on
either the CPU or the GPU. If want you enable antialiasing with
``override application settings`` in your graphics card driver
configuration program, you can leave the application antialiasing
setting to off.

**FXAA - Fast Approximate Antialiasing**

This is a post-processing antialiasing which is very fast and produces
reasonably good results. It has some impact on the FPS depending on how
fast your graphics card is. As it is a post-processing effect, this will
work also when you take screenshots or output the frames. Here is more info on FXAA_.

.. _FXAA: http://en.wikipedia.org/wiki/Fast\_approximate\_anti-aliasing

**NFAA - Normal Field Antialiasing**

This is yet another post-processing antialiasing technique. It is based
on generating a normal map to detect the edges for later smoothing. It
may look better on some devices and the penalty in FPS is small. It will
also work for the screenshots and frame outputs.

**MSAA - Multi-Sample Antialiasing**

As of version ``1.0.1`` MSAA is not offered anymore. This is implemented
by the graphics card and may not always be available. You can choose the
number of samples (from 2 to 16, from worse to better) and it has a
bigger cost on FPS than the post-processing options. It also looks
better. However, this being reliant on a special multisample frame
buffer in the graphics card makes it not available for screenshots and
frame outputs.

Line style
==========

Whether to render lines with an advanced quad system or using simple
``GL_LINES``. The former will look better at the expense of requiring
more processing power in the GPU.

Vertical synchronization (V-sync)
=================================

This option limits the frames per second to match your monitor's refresh
rate and prevent screen tearing. It is recommended to leave it enabled
unless you want to test how many FPS you can get or you want to fry your
card.

Maximum frame rate (FPS)
========================

This option lets the user choose the maximum frame rate of Gaia Sky. If set, the
frame rate of the application will be capped to that value using delays in the 
main loop.

Other graphics settings
=======================

Here are other graphics settings offered in the configuration window.

-  **Bloom effect**: Controls the bloom effect, causing light to bleed over the edges of darker parts.
-  **Motion blur**: Enable or disable the screen space motion blur effect.
-  **Lens flare**: Enable or disable the screen space lens flare effect.
-  **Star glow**: Enable or disable star glows. If enabled, the stars
   are rendered using a glow texture in a post-processing step. This can
   have a performance hit on some older graphics cards.
-  **Brightness**: Controls the overall brightness of the image.
-  **Contrast**: Controls the overall contrast of the image.
-  **Hue**: Controls the hue value of the image.
-  **Saturation**: Controls the saturation value of the image.
-  **Gamma correction**: Controls the gamma correction value of the image. This should be calibrated with your monitor.


