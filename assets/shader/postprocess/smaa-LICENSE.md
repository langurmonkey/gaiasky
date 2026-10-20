# SMAA license and attribution

The Gaia Sky SMAA implementation lives in:

- `assets/shader/postprocess/smaa-edges.vert.glsl`, `smaa-edges.frag.glsl`
- `assets/shader/postprocess/smaa-weights.vert.glsl`, `smaa-weights.frag.glsl`
- `assets/shader/postprocess/smaa-blend.vert.glsl`, `smaa-blend.frag.glsl`
- `assets/shader/lib/smaa-presets.glsl`
- `assets/img/smaa/smaa-area.png`, `assets/img/smaa/smaa-search.png` (precomputed lookup textures)
- `core/src/gaiasky/render/postprocess/filters/Smaa*Filter.java`
- `core/src/gaiasky/render/postprocess/effects/Smaa.java`

SMAA (Subpixel Morphological Antialiasing) was designed by Jorge Jimenez,
Jose I. Echevarria, Belen Masia, Fernando Navarro and Diego Gutierrez.
The GLSL port used here is the work of Damien Seguin
(https://github.com/iryoku/smaa, https://github.com/malcolm-kee/glsl-smaa).

The files are adapted for Gaia Sky: GLSL 330 core, luma edge detection only,
and the WebGL specifics of the original port removed. The original algorithm,
the lookup tables and the license below are kept as-is.

## License

Copyright (C) 2013 Jorge Jimenez (jorge@iryoku.com)
Copyright (C) 2013 Jose I. Echevarria (joseignacioechevarria@gmail.com)
Copyright (C) 2013 Belen Masia (bmasia@unizar.es)
Copyright (C) 2013 Fernando Navarro (fernandn@microsoft.com)
Copyright (C) 2013 Diego Gutierrez (diegog@unizar.es)

Copyright (C) 2018 Damien Seguin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
