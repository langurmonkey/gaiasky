#!/usr/bin/env python3
"""Render a top (face-on) and a side (edge-on) image of a galaxy volume directory.

    uv run --with numpy --with matplotlib python volume-preview.py DIR [options]

DIR holds a <prefix>_emissive_rgba.raw.gz and a <prefix>_occlusive_rgba.raw.gz with their
_metadata.txt files - the pair particles-to-volume.py writes, or combine-volumes.py. Two PNGs
are written next to them (or into --output):

    <dir-name>_top.png    looking down the thin axis: the disk face on
    <dir-name>_side.png   looking along an in-plane axis: the disk edge on

Each pixel is a ray through the box, not a slice: it integrates the emission along the ray and
attenuates it with the dust, with the same constants as the renderer
(assets/shader/volume.galaxy.fragment.glsl, DUST_SIGMA = 1000, DUST_EMISSION = 0):

    emitted += texel.rgb * texel.a * duvw * transmittance      per sample, emission first
    transmittance *= exp(-DUST_SIGMA * dust.a * duvw)          then the dust, on what is behind

`duvw` is one voxel measured in the volume's own [0,1] UVW coordinates, so the integral is the
one the shader approximates with its 0.01-UVW steps. Both file formats in this directory use the
same convention - RGB is the colour of the light and A its density - so this reads the output of
either generator; the colour gain the old one baked into the bytes is simply already in there.

The emitted light is linear and very dim (the metadata's "mean ray integral" is ~3e-4), so an
automatic exposure maps a percentile of the brightest view's pixels to white. Past that exposure
the brightest channel of each pixel goes through a soft shoulder, so the core rolls off instead of
clipping to flat white, and the pixel is scaled by that one factor, so the ratios between its three
channels survive into the picture. A display gamma follows, and then --saturation, the chroma of
the output: the light of a galaxy is close to neutral, so --saturation 1 (the physical rendering,
and the one to use when comparing against an earlier run) reads as black and white, and the
default, 2, is what makes the colour the data carries visible. --exposure overrides the automatic
value and --gamma 1 disables the display gamma; the two views always share one exposure, so they
are comparable.
"""

import argparse
import glob
import gzip
import os
import re
import sys

import numpy as np

# From the shader: Beer-Lambert extinction per unit of UVW path. The dust absorbs, it does not emit.
DUST_SIGMA = 1000.0

# Display gamma. The renderer works in linear light; without this the faint outer parts of the
# image are all but invisible. --gamma 1 turns it off.
DEFAULT_GAMMA = 2.2

# Exposure of an automatic run: this percentile of the pixels of the two views is mapped to white.
# The very brightest pixel is always the centre of the bulge, so mapping *it* to white leaves the
# disk and the dust invisible. Lower the percentile to brighten the image, raise it to darken.
DEFAULT_PERCENTILE = 99.0

# Knee of the tone curve, in units of the exposure reference (the 99th percentile of the pixels, so
# 1 is that percentile). Below it the light is passed through untouched - the picture is exactly the
# one the exposure asks for - and above it the curve rolls off towards 1 without ever reaching it,
# which is what stops a bright core from clipping to flat white. It is a shoulder, not a filmic
# curve: a curve that compresses from 0 upwards lifts everything dim along with it, and multiplied
# by this volume's exposure (the reference is 4e-2, so the light a disk pixel emits is 1e-3) that
# leaves the faint disk nearly as bright as the core.
TONE_KNEE = 0.8

# Chroma of the output, as a multiple of the distance from grey, applied in display space after the
# gamma. 1 leaves the light alone - the physical rendering - and is right for measuring, but a
# galaxy's light is nearly neutral (this one's disk is within 10% of grey, its core within 20%), so
# 1 is also a picture that reads as black and white. This is the value that makes the data's own
# colour visible; raise it for more, lower it towards 1 for less.
DEFAULT_SATURATION = 2.0

# Luminance weights (Rec. 709), the axis the chroma is scaled about.
LUMA = np.array([0.2126, 0.7152, 0.0722], dtype=np.float32)

# Every pixel of the output images is drawn with this many pixels per inch, so a figure's size in
# inches is just its size in pixels over this. The images are written at --size pixels across.
OUTPUT_DPI = 100.0

# Storage axes: 0 = galactic X, 1 = galactic Z, 2 = galactic -Y. Only the last one needs naming:
# it is the one whose direction is reversed with respect to the galactic axis it stands for, so
# its images have their rows flipped.
AXIS_NEG_Y = 2


# --------------------------------------------------------------------------- arguments


def parse_arguments(argv):
    parser = argparse.ArgumentParser(
        description="Render top and side views of the galaxy volume textures in a directory.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument("directory", help="Directory holding <prefix>_emissive_rgba.raw.gz and "
                                          "<prefix>_occlusive_rgba.raw.gz with their metadata.")
    parser.add_argument("-o", "--output", default=None,
                        help="Directory for the PNGs. Default: the input directory.")
    parser.add_argument("--size", type=int, default=1024,
                        help="Width of the output images in pixels; the height follows the shape "
                             "of the box, so the pixels stay square.")
    parser.add_argument("--exposure", type=float, default=None,
                        help="Brightness multiplier applied before the tonemap. Default: "
                             "automatic, from the --percentile of the pixels.")
    parser.add_argument("--percentile", type=float, default=DEFAULT_PERCENTILE,
                        help="Percentile of the pixels that the automatic exposure maps to white.")
    parser.add_argument("--gamma", type=float, default=DEFAULT_GAMMA,
                        help="Display gamma; 1 is linear light.")
    parser.add_argument("--saturation", type=float, default=DEFAULT_SATURATION,
                        help="Chroma of the output, as a multiple of the distance from grey, "
                             "applied in display space. 1 is the physical rendering, which reads "
                             "as black and white because the light is nearly neutral; higher "
                             "makes the colour of the data visible, 0 is greyscale.")
    parser.add_argument("--dust-scale", type=float, default=1.0,
                        help="Multiplier on the dust density. 0 is the same as --no-dust.")
    parser.add_argument("--no-dust", action="store_true",
                        help="Ignore the occlusive volume and show the emission alone.")
    parser.add_argument("--no-caption", action="store_true",
                        help="Do not print the settings onto the images.")
    return parser.parse_args(argv)


# --------------------------------------------------------------------------- input


def read_metadata(path):
    """Grid size and world size off a *_metadata.txt, plus the file as it was read.

    Both numbers are in storage order (W, H, D), which is galactic (X, Z, -Y) - see
    to_file_order in particles-to-volume.py. combine-volumes.py writes the same two lines, so
    this reads either generator's output.
    """
    try:
        text = open(path).read()
    except OSError as error:
        raise SystemExit(f"Error: cannot read {path}: {error}") from None
    grid = re.search(r"^Grid size:\s*(\d+)\s*x\s*(\d+)\s*x\s*(\d+)", text, re.M)
    world = re.search(r"^World size:\s*\[([^\]]+)\]", text, re.M)
    if grid is None or world is None:
        raise SystemExit(f"Error: {path} has no 'Grid size:' or 'World size:' line; this does not "
                         f"look like the metadata of a volume written by particles-to-volume.py")
    shape = tuple(int(v) for v in grid.groups())
    extent = tuple(float(v) for v in world.group(1).replace(",", " ").split())
    if len(extent) != 3:
        raise SystemExit(f"Error: {path}: 'World size' needs three numbers, found {len(extent)}")
    return {"grid": shape, "world": extent, "text": text}


def find_volume(directory, kind):
    """The one *<kind>*_rgba.raw.gz in a directory, with the metadata beside it."""
    hits = sorted(glob.glob(os.path.join(directory, "*%s*rgba.raw.gz" % kind)))
    if not hits:
        raise SystemExit(f"Error: no *{kind}*rgba.raw.gz in {directory}")
    if len(hits) > 1:
        raise SystemExit(f"Error: {len(hits)} *{kind}* files in {directory}, name one: "
                         + ", ".join(os.path.basename(h) for h in hits))
    path = hits[0]
    metadata = path.replace("_rgba.raw.gz", "_metadata.txt")
    return path, read_metadata(metadata)


def load_volume(path):
    """The texture as a (W, H, D, 4) uint8 array, in the order the file is indexed in."""
    try:
        with gzip.open(path, "rb") as handle:
            raw = handle.read()
    except OSError as error:
        raise SystemExit(f"Error: cannot read {path}: {error}") from None
    return np.frombuffer(raw, np.uint8)


# --------------------------------------------------------------------------- render


def integrate(emissive, occlusive, ray_axis, dust_scale):
    """March a ray along `ray_axis` through the box: the emitted light, and the dust optical depth.

    Emission comes first within a sample and the dust absorbs only what is behind it, which is the
    shader's ordering: a dust lane hides the far side of the galaxy, not the light it shares a
    voxel with. The arrays are uint8 and are scaled inside the loop, so nothing is copied whole.
    The optical depth is the exponent the shader ends up multiplying by, DUST_SIGMA times the dust
    density integrated along the ray; it is returned because it is the number that says how thick
    the dust is, whatever the exposure does to the picture.
    """
    # Ray axis first, colour channels last: sample(k) is then one texel along the ray, in the
    # order the file stores it (R, G, B, A).
    emissive = np.moveaxis(emissive, ray_axis, 0)
    samples = emissive.shape[0]
    # One voxel in UVW: the volume's coordinates run over [0, 1] on every axis, so a voxel is a
    # fraction of the axis it sits on. This is the step the shader takes, at its finest.
    step_emissive = 1.0 / samples

    shape = emissive.shape[1:-1]
    emitted = np.zeros((*shape, 3), np.float32)
    transmittance = np.ones(shape, np.float32)

    dust = None
    step_dust = 0.0
    depth = None
    if occlusive is not None and dust_scale > 0.0:
        dust = np.moveaxis(occlusive, ray_axis, 0)
        step_dust = 1.0 / dust.shape[0]
        depth = np.zeros(shape, np.float32)

    for k in range(samples):
        texel = emissive[k]
        colour = texel[..., :3].astype(np.float32) * (1.0 / 255.0)
        brightness = texel[..., 3].astype(np.float32) * (step_emissive / 255.0)
        emitted += colour * brightness[..., None] * transmittance[..., None]
        del texel, colour, brightness
        if dust is not None:
            sample_depth = DUST_SIGMA * dust_scale * dust[k, ..., 3].astype(np.float32) \
                * (step_dust / 255.0)
            depth += sample_depth
            transmittance *= np.exp(-sample_depth)
    del emissive, dust
    return emitted, depth


def layout(world):
    """(name, ray axis, horizontal axis, vertical axis) for the two views of a box.

    The thin axis is the one to look down for a face-on view; the widest of the two remaining ones
    is the one to look along for an edge-on view. Galactic X goes across both images and the other
    axis goes up, which for the -Y axis (storage axis 2) means the rows are flipped so that +Y is
    up rather than down.
    """
    thin = int(np.argmin(world))
    plane = [axis for axis in range(3) if axis != thin]
    along = plane[int(np.argmax([world[axis] for axis in plane]))]
    views = []
    for name, ray in (("top", thin), ("side", along)):
        remaining = sorted(axis for axis in range(3) if axis != ray)
        views.append((name, ray, remaining[0], remaining[1]))
    return views


def render(emissive, occlusive, view, dust_scale):
    """One view -> (image, optical depth), both with the vertical axis as rows, bottom row first.

    The optical depth is in the same orientation as the image, so it can be reported alongside it.
    """
    _, ray_axis, horizontal, vertical = view
    emitted, depth = integrate(emissive, occlusive, ray_axis, dust_scale)
    # After the march the two remaining axes are still in ascending order: (horizontal, vertical).
    image = np.transpose(emitted, (1, 0, 2))
    if depth is not None:
        depth = np.transpose(depth, (1, 0))
    if vertical == AXIS_NEG_Y:
        image = image[::-1]
        if depth is not None:
            depth = depth[::-1]
    del emitted
    return np.ascontiguousarray(image), (None if depth is None else np.ascontiguousarray(depth))


def tonemap(image, exposure, gamma, saturation):
    """Linear light -> 8-bit pixels: exposure, shoulder, gamma, then chroma.

    Clipping every channel of a pixel at 1 is what turns a bright core flat white: the channel that
    is brightest saturates first and the other two are dragged up to it, so the hue of everything
    bright is destroyed. Instead the *brightest* channel of a pixel goes through the shoulder and
    the pixel is scaled by the factor that channel comes out at, so the ratios between its three
    channels survive the whole way and no channel is ever clipped.

    The shoulder leaves everything below TONE_KNEE alone, so the exposure alone decides the picture
    there, and rolls the rest off towards 1 - which keeps the core from flattening without lifting
    the faint disk along with it.

    `saturation` then scales the chroma about the luminance in display space, where it reads the
    way it looks: 1 leaves the light alone, 2 doubles the distance from grey.
    """
    x = np.maximum(image * exposure, 0.0)
    peak = x.max(axis=-1, keepdims=True)
    # Identity at and below the knee, asymptotic to 1 above it, and a single factor per pixel. The
    # value below the knee stays the value: only what is above it moves.
    excess = np.maximum(peak - TONE_KNEE, 0.0) / (1.0 - TONE_KNEE)
    rolled = TONE_KNEE + (1.0 - TONE_KNEE) * (1.0 - 1.0 / (1.0 + excess))
    shoulder = np.where(peak > TONE_KNEE, rolled, peak) / np.maximum(peak, 1e-30)
    x = x * np.where(peak > 0.0, shoulder, 0.0)
    if gamma != 1.0:
        x = x ** (1.0 / gamma)
    if saturation != 1.0:
        luminance = x @ LUMA
        x = np.clip(luminance[..., None] + saturation * (x - luminance[..., None]), 0.0, 1.0)
    return (x * 255.0 + 0.5).astype(np.uint8)


def save_png(path, image, caption, width_px):
    """Write an 8-bit RGB array as a PNG of `width_px` pixels across, rows bottom-up.

    Image rows are the vertical axis and columns the horizontal one, so the aspect of the figure
    is the aspect of the box; matplotlib resamples the array to it.
    """
    from matplotlib.backends.backend_agg import FigureCanvasAgg
    from matplotlib.figure import Figure

    height, width = image.shape[:2]
    height_px = max(1, int(round(width_px * height / width)))
    figure = Figure(figsize=(width_px / OUTPUT_DPI, height_px / OUTPUT_DPI),
                    dpi=OUTPUT_DPI, facecolor="black")
    FigureCanvasAgg(figure)
    axes = figure.add_axes([0.0, 0.0, 1.0, 1.0], facecolor="black")
    axes.imshow(image, origin="lower", interpolation="bilinear")
    axes.set_axis_off()
    if caption:
        axes.text(0.008, 0.988, caption, transform=axes.transAxes, va="top", ha="left",
                  color="0.85", fontsize=max(6.0, width_px / 128.0), family="monospace",
                  bbox={"facecolor": "black", "alpha": 0.55, "edgecolor": "none", "pad": 3})
    figure.savefig(path, dpi=OUTPUT_DPI, facecolor="black")
    del figure


# --------------------------------------------------------------------------- main


def main(argv=None):
    args = parse_arguments(sys.argv[1:] if argv is None else argv)
    directory = args.directory
    if not os.path.isdir(directory):
        raise SystemExit(f"Error: '{directory}' is not a directory")
    if args.size < 16:
        raise SystemExit("Error: --size must be at least 16")
    if args.gamma <= 0.0:
        raise SystemExit("Error: --gamma must be greater than 0")
    if args.exposure is not None and args.exposure <= 0.0:
        raise SystemExit("Error: --exposure must be greater than 0")
    if args.saturation < 0.0:
        raise SystemExit("Error: --saturation must not be negative")

    emissive_path, emissive_meta = find_volume(directory, "emissive")
    print(f"emissive  {emissive_path}")
    emissive = load_volume(emissive_path)
    grid = emissive_meta["grid"]
    expected = int(np.prod(grid)) * 4
    if emissive.size != expected:
        raise SystemExit(f"Error: {emissive_path} holds {emissive.size / 1024 / 1024:.2f} MB but the "
                         f"metadata says {grid[0]}x{grid[1]}x{grid[2]}x4 bytes "
                         f"({expected / 1024 / 1024:.2f} MB)")
    emissive = emissive.reshape(*grid, 4)

    occlusive = None
    occlusive_meta = None
    if not args.no_dust and args.dust_scale > 0.0:
        occlusive_path, occlusive_meta = find_volume(directory, "occlusive")
        print(f"occlusive {occlusive_path}")
        occlusive = load_volume(occlusive_path)
        if occlusive_meta["grid"] != grid:
            raise SystemExit(f"Error: the emissive grid is {grid} but the occlusive one is "
                             f"{occlusive_meta['grid']}; the two must be a matching pair")
        occlusive = occlusive.reshape(*grid, 4)

    world = emissive_meta["world"]
    print(f"  grid {grid[0]}x{grid[1]}x{grid[2]} storage = galactic (X, Z, -Y), "
          f"world {world[0]:.4f} x {world[1]:.4f} x {world[2]:.4f}")
    print(f"  {int(np.count_nonzero(emissive[..., 3]))} lit voxels of {emissive[..., 3].size} in the "
          f"emissive volume")

    views = layout(world)
    rendered = {}
    depths = {}
    for view in views:
        name, ray_axis, horizontal, vertical = view
        image, depth = render(emissive, occlusive, view, args.dust_scale)
        rendered[name] = image
        print(f"  {name}: ray along storage axis {ray_axis}, image {image.shape[1]} x {image.shape[0]} "
              f"(horizontal = axis {horizontal}, vertical = axis {vertical}"
              f"{', rows flipped to put +galactic Y up' if vertical == AXIS_NEG_Y else ''})")
        if depth is not None:
            depths[name] = depth
            print(f"    dust optical depth along the ray: median {np.median(depth):.2f}, "
                  f"mean {depth.mean():.2f}, max {depth.max():.2f}; the mean transmittance it "
                  f"leaves is e^-tau = {float(np.exp(-depth).mean()):.3f}")
    del emissive, occlusive

    dust_text = "no dust" if occlusive_meta is None else f"dust x{args.dust_scale:g}"
    del depths

    # One exposure for both views, so they can be compared pixel for pixel.
    if args.exposure is None:
        peaks = np.concatenate([image.max(axis=-1).ravel() for image in rendered.values()])
        reference = float(np.percentile(peaks, args.percentile))
        if reference <= 0.0:
            raise SystemExit("Error: the images are black; nothing to expose")
        exposure = 1.0 / reference
        print(f"  automatic exposure {exposure:.4g} (the {args.percentile:g}th percentile of the "
              f"pixels, {reference:.3g} linear, is white)")
    else:
        exposure = args.exposure
        print(f"  exposure {exposure:.4g} (given)")

    stem = os.path.basename(os.path.abspath(directory))
    output = args.output or directory
    os.makedirs(output, exist_ok=True)
    for name, image in rendered.items():
        caption = (f"{stem}  {name}  grid {grid[0]}x{grid[1]}x{grid[2]}  "
                   f"exposure {exposure:.4g}  gamma {args.gamma:g}  sat {args.saturation:g}  "
                   f"{dust_text}")
        path = os.path.join(output, f"{stem}_{name}.png")
        save_png(path, tonemap(image, exposure, args.gamma, args.saturation),
                 "" if args.no_caption else caption, args.size)
        print(f"  wrote {path}")
        del image
    return 0


if __name__ == "__main__":
    sys.exit(main())
