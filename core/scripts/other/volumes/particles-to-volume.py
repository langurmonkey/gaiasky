#!/usr/bin/env python3
"""Generate the galaxy volume textures (one emissive, one occlusive) from particle channels.

    particles-to-volume.py RESOLUTION INPUT_DIR OUTPUT_DIR
        [--intensity NAME=FLOAT ...] [--smoothing [NAME=]FACTOR ...]

RESOLUTION is the number of voxels along the longest side of the box; the other axes follow the
shape of the data, so the voxels stay cubic. Per-channel floats in [0, 1] dim individual channels,
and per-channel smoothing factors of 1 or less make them thinner. Both are lengths or pure ratios,
so both mean the same thing at every resolution.


WHY THIS REPLACES particles-to-3dtex.py
---------------------------------------
The old script deposited each particle into one voxel and then blurred the result by a width
counted in voxels, stored the *count* of particles per voxel (i.e. mass per voxel, not mass per
unit volume) and divided the whole channel by the maximum of that count. Every one of those three
steps is resolution dependent, which is why the same particles drawn at 200 and at 800 came out
with a different brightness, a different dust thickness and a different granularity:

  * Mass per voxel scales as 1/RESOLUTION^3, so dividing by the largest one *moves* the whole
    image when the resolution changes. What the renderer integrates is the line integral of the
    stored field with a step fixed in box fractions, so the resolution-independent quantity to
    store is mass per unit box volume (a density): as the grid refines, a density field converges
    to a fixed function of the box coordinate, and its ray integral converges with it.
  * A blur width counted in voxels shrinks in world units as the grid refines, so the same data
    gets sharper and sharper. The width here is a length: a fraction of the channel's *local*
    inter-particle spacing, which is a property of the particles, not of the grid.
  * Even with both of those fixed, the maximum of the *sampled* field is not a physical quantity
    when the sampled features are thinner than a voxel (the dust disk is): it scales as 1/voxel
    size, so normalising by it makes a thin sheet fade as the grid refines. The reference used
    here is therefore evaluated on the *particles*, with the physical width, and is exactly
    resolution free.

A whole run is then a fixed mapping from the particles to the stored bytes, and two runs of the
same data at different resolutions produce the same picture (to within the renderer's own sampling
of a field that is, by then, the same field in both).


WHAT IS WRITTEN
---------------
Two RGBA8 textures and their metadata, named after the common prefix of the channels:

    <prefix>_emissive_rgba.raw.gz   the sum of the emissive channels
    <prefix>_occlusive_rgba.raw.gz  the sum of the occlusive channels (the dust)

Which channels are which is the --emissive/--occlusive pattern lists (default 'gas,bulge' and
'dust'; a channel in neither list is skipped). Each channel is normalised to its own brightness
before they are added, so '--intensity X=1' means "channel X at its own full range" and lowering
it dims exactly that channel - there is no renormalisation anywhere that would push the brightness
back up, which is the feedback the old peak-divided merge had.

Per voxel the stored bytes are

    RGB = the colour of the light (the chromaticity, i.e. hue and relative chroma)
    A   = the brightness of the light, in units of the brightest voxel of the merged channels

so the product RGB*A is the emission, exactly, with no gain to tune and nothing clipped. The
occlusive volume is the same convention, and because a dust channel has one colour, its A comes
out as the plain normalised density, which is what the shader's extinction wants.


TUNING A CHANNEL
----------------
Two knobs, one per channel, both pure numbers, both applied before the merge:

  --intensity X=0..1   how much light the channel puts out.
  --smoothing X=f      how far its particles are smeared; f multiplies the channel's own width
                       (a fraction of its local inter-particle spacing, with a floor for the
                       channels packed tighter than a voxel). 1 is the default, 0.5 half as
                       thick.

A smoothing factor is a ratio of two lengths and the floor is itself a length in the box, so the
knob behaves identically at 200, 400 and 800 - it changes the picture, never its resolution
behaviour. Note that thinning a channel also lowers its *column* through the box: the bytes are
normalised by the peak density, which grows as the width shrinks, while the mass along a ray does
not, so a thinner dust is a more transparent dust. Raise its intensity to put the extinction back
if that is not what you want; the two knobs do not fight, they compose.
"""

import argparse
import glob
import gzip
import os
import re
import sys
import time

import numpy as np
from scipy.ndimage import gaussian_filter
from scipy.spatial import cKDTree

# --------------------------------------------------------------------------- tunables

# Kernel width, as a fraction of the channel's *local* inter-particle spacing: the median
# distance from a particle to its 4th neighbour. Local, not the mean spacing of the box, because
# the box figure assumes the channel fills the box and the dust does not - it is a thin disk in a
# few percent of it. Both the width and the spacing are lengths on the particles, so the width is
# the same in world units at every resolution.
DEFAULT_WIDTH = 0.85

# Default width of a channel whose particles are packed tighter than the floor below: it is the
# floor, not DEFAULT_WIDTH times the spacing, that a channel gets when the two disagree.
#
# Floor on the width, as a fraction of the box. It exists for the channels whose particles are
# packed far more tightly than their local structure needs - the dust disk here is half a voxel
# thick at 400 and a quarter of one at 800 - because a field sharper than the grid cannot be
# stored: the mass of a particle would land in one voxel, the bytes would hold a point cloud
# rather than a density, and the peak of the *sampled* field (which is what the bytes are scaled
# by) would then be an artefact of the voxel size.
#
# It is a length in the box and not in voxels, deliberately. A width counted in voxels shrinks in
# world units as the grid refines, so the channel gets sharper and sharper and the picture changes
# with the resolution; a width in the box is the same at 200, at 400 and at 800, and only the
# sampling of the field improves. 1/256 is about 1.5 voxels at a 400-wide grid, which is the
# coarsest sampling that still carries a smooth field for the renderer's 0.01-UVW step.
#
# It is a default, not a limit: --smoothing scales it along with everything else, so a channel on
# the floor can be taken below it. The run then notes that the grid cannot sample the result.
MIN_WIDTH_BOX = 1.0 / 256.0

# Below this many voxels the field is under-sampled at the grid being written and the run says so:
# the field itself is still the right one, but the file cannot resolve it.
MIN_WIDTH_SAMPLED_VOXELS = 1.5

# Half-width of the particle-to-voxel binning, in voxels, as a variance: a voxel is a box of unit
# width, whose variance is 1/12. Subtracted in quadrature from the width asked for, so the field
# ends up with the width asked for rather than the width plus a voxel.
BINNING_VARIANCE = 1.0 / 12.0

# Neighbour used for the local spacing, and how many neighbours are summed when the field is
# evaluated on the particles. The 1st neighbour is the noisiest (two particles can share a voxel),
# so a few neighbours in.
LOCAL_SPACING_NEIGHBOURS = 4
FIELD_NEIGHBOURS = 64

# Truncation of the kernel used to evaluate the field on the particles.
KERNEL_CUTOFF = 3.0

# The box is the data bounds grown by this, i.e. 10% larger than the particle extent.
BOX_PADDING = 1.05

# The largest side of the box, in units of the object's size. The shader multiplies the metadata
# bounds by the object's diameter, so a box of side 1 draws the volume exactly the size the scene
# asks for; a box in raw data units (side ~7 here) would draw it seven times too large.
NORMALIZED_SIDE = 1.0

DEFAULT_EMISSIVE = "gas,bulge"
DEFAULT_OCCLUSIVE = "dust"

# Colour of a channel whose particles carry no colour column, by kind.
DEFAULT_COLOR = {
    "dust": (0.80, 0.60, 0.40),
    "gas": (0.30, 0.50, 0.80),
    "hii": (1.00, 0.40, 0.60),
    "bulge": (1.00, 0.90, 0.60),
    "star": (1.00, 0.95, 0.90),
}
COLOR_NEUTRAL = (0.30, 0.50, 0.80)

LUMA = np.array([0.2126, 0.7152, 0.0722], dtype=np.float32)


# --------------------------------------------------------------------------- arguments


def channel_kind(name: str) -> str:
    """Classify a channel by its file name, for the width and colour defaults."""
    lowered = name.lower()
    for kind in ("dust", "gas", "hii", "bulge", "star"):
        if kind in lowered:
            return kind
    return ""


def parse_assignment(value: str):
    """Parse one NAME=VALUE argument into (name, float)."""
    name, separator, number = value.partition("=")
    if not separator or not name.strip():
        raise argparse.ArgumentTypeError(f"expected NAME=VALUE, got '{value}'")
    try:
        return name.strip().lower(), float(number)
    except ValueError:
        raise argparse.ArgumentTypeError(f"'{number}' is not a number in '{value}'") from None


def parse_intensity(value: str):
    """Parse one --intensity NAME=FLOAT, requiring the float to be in [0, 1]."""
    name, number = parse_assignment(value)
    if not 0.0 <= number <= 1.0:
        raise argparse.ArgumentTypeError(
            f"intensity {number} out of range; it scales the stored bytes, so it must be in [0, 1]")
    return name, number


def parse_smoothing(value: str):
    """Parse one --smoothing: a bare FACTOR for every channel, or NAME=FACTOR for one.

    A bare number and a NAME=VALUE are told apart by the '=', so the same flag does both jobs and
    there is no second flag to remember.
    """
    name, separator, number = value.partition("=")
    if not separator:
        name, number = None, value
    elif not name.strip():
        raise argparse.ArgumentTypeError(f"expected FACTOR or NAME=FACTOR, got '{value}'")
    try:
        factor = float(number)
    except ValueError:
        raise argparse.ArgumentTypeError(f"'{number}' is not a number in '{value}'") from None
    if factor <= 0.0:
        raise argparse.ArgumentTypeError(
            f"smoothing {factor} must be greater than 0: at 0 the field is a point cloud, whose "
            f"peak density - the number the bytes are normalised by - is infinite")
    return (name.strip().lower() if name else None), factor


def parse_patterns(value: str):
    """Parse a comma-separated channel pattern list."""
    patterns = [p.strip().lower() for p in value.split(",") if p.strip()]
    if not patterns:
        raise argparse.ArgumentTypeError("expected at least one channel pattern")
    return patterns


def match(pattern: str, names: list[str]) -> str:
    """Resolve a case-insensitive substring pattern to exactly one channel name."""
    hits = [n for n in names if pattern in n.lower()]
    if not hits:
        raise ValueError(f"'{pattern}' matches no channel; the channels are {', '.join(names)}")
    if len(hits) > 1:
        raise ValueError(f"'{pattern}' is ambiguous: it matches {', '.join(hits)}")
    return hits[0]


# --------------------------------------------------------------------------- particles


def read_particles(path: str):
    """Read one channel: positions, mass and (optionally) colour.

    The column layout is the one Gaia Sky's galaxy data uses (and the one the header line in the
    files names): x y z size [r g b]. 'size' is the only per-particle weight available and is used
    as the mass; --mass unit gives every particle the same one.
    """
    with gzip.open(path, "rt") as handle:
        rows = [line for line in handle
                if line.strip() and (line.lstrip()[0].isdigit() or line.lstrip()[0] in "+-.")]
    if not rows:
        return np.zeros((0, 3)), np.zeros(0), None
    table = np.loadtxt(rows, ndmin=2)
    if table.shape[1] < 4:
        raise ValueError(f"{path}: expected at least x y z size, found {table.shape[1]} columns")
    return table[:, :3], table[:, 3], (table[:, 4:7] if table.shape[1] >= 7 else None)


def local_spacing(positions: np.ndarray) -> float:
    """Median distance from a particle to its LOCAL_SPACING_NEIGHBOURS-th neighbour."""
    count = len(positions)
    if count < 2:
        return 0.0
    tree = cKDTree(positions)
    distances, _ = tree.query(positions, k=min(LOCAL_SPACING_NEIGHBOURS + 1, count), workers=-1)
    return float(np.median(distances[:, -1]))


def field_on_particles(positions: np.ndarray, mass: np.ndarray, width: float) -> np.ndarray:
    """Evaluate the density field on the particles themselves.

    This is the physical peak of the channel, and it is computed from the particles alone - no
    grid, no voxel size - so the reference the bytes are normalised by is exactly the same at every
    resolution. It is also the peak of the *continuous* field rather than of the sampled one: on a
    feature thinner than a voxel the sampled peak is an artefact of the voxel size, and this is
    not. The nearest FIELD_NEIGHBOURS neighbours are summed; the kernel is truncated at
    KERNEL_CUTOFF widths, where a Gaussian has already fallen to 1e-4 of its value.
    """
    count = len(positions)
    tree = cKDTree(positions)
    distances, indices = tree.query(positions, k=min(FIELD_NEIGHBOURS, count), workers=-1)
    if count == 1:
        distances, indices = distances.reshape(-1, 1), indices.reshape(-1, 1)
    kernel = np.exp(-0.5 * (distances / width) ** 2, dtype=np.float32)
    kernel[distances > KERNEL_CUTOFF * width] = 0.0
    normalisation = (2.0 * np.pi * width * width) ** -1.5
    return (mass[indices] * kernel).sum(axis=1) * normalisation


# --------------------------------------------------------------------------- volume geometry


def make_box(positions: np.ndarray, resolution: int):
    """The box every channel shares, its side normalised, plus the grid it is sampled on.

    One box for every channel is what makes the emissive and the occlusive volumes align: the
    shader intersects each box independently, and two boxes that differ would put the dust and the
    gas at different places in the same object.

    The box is shaped like the data (a flat galaxy gets a flat box), so the grid inherits that
    shape and the voxels come out cubic - the same detail along the thin axis as along the wide
    ones, for a tenth of the memory of a cube.
    """
    low = positions.min(axis=0)
    high = positions.max(axis=0)
    span = (high - low) * BOX_PADDING

    # Scaling about the origin, not about the centre: the particle pipeline places a point at its
    # coordinate times the object's size with no recentring, so a data set that is not centred on
    # the origin has to stay off-centre, in the box and in the particles alike.
    scale = NORMALIZED_SIDE / float(span.max())
    low, high, span = low * scale, high * scale, span * scale

    voxel = float(span.max()) / resolution
    grid = np.maximum(1, np.rint(span / voxel).astype(int))
    return low, high, span, voxel, grid, scale


def to_file_order(grid: np.ndarray) -> np.ndarray:
    """Galactic-order array -> the order the file is indexed in: galactic (x, z, -y).

    Two things stack up, and both have to be undone at the other end.

    The first is the permutation glTexImage3D needs: a volume file is a C-order dump, so its last
    axis varies fastest, while GL reads its client buffer with the first axis varying fastest, and
    VolumeTextureLoader.glGrid() declares the extents reversed to compensate. The texture GL ends
    up with is therefore the file's three axes in reverse, and this writes them out in reverse.

    The second is a rotation. The renderer places the particles of the same data set through
    BillboardSetInitializer.transformData(), which maps a galactic point (x, y, z) to
    (-y, z, x). The volume shader identifies the texture's axes with those same local axes, so the
    texture has to be authored in that rotated frame or the volume is 90 degrees off every other
    representation of the same data.

    Galactic (x, y, z) -> file (x_t, y_t, z_t) = (x, z, -y).
    """
    if grid.ndim == 4:
        return np.ascontiguousarray(grid.transpose(0, 2, 1, 3)[:, :, ::-1, :])
    return np.ascontiguousarray(grid.transpose(0, 2, 1)[:, :, ::-1])


def stored_bounds(box_min: np.ndarray, box_max: np.ndarray):
    """The box in storage order: the (x, z, -y) extremes of the galactic box.

    The metadata has to be in the order the file is indexed in, because the shader turns a position
    into texture coordinates per axis. Axis 2 is the galactic Y backwards, so it is the one axis
    whose stored extremes are not simply a reordering: the interval flips.
    """
    return (np.array([box_min[0], box_min[2], -box_max[1]]),
            np.array([box_max[0], box_max[2], -box_min[1]]))


# --------------------------------------------------------------------------- conversion


def channel_width(spacing: float, factor: float) -> float:
    """A channel's kernel width in box units: its own width, times the --smoothing factor.

    The channel's own width is a fraction of the spacing between its particles, which is a property
    of the particles rather than of the grid, so it is the same length at every resolution. The
    floor catches the channels whose particles are packed closer together than a voxel; it is a
    length in the box and not a count of voxels, for the same reason. `factor` scales both, so
    --smoothing 0.5 halves a channel at any resolution.
    """
    return max(DEFAULT_WIDTH * spacing, MIN_WIDTH_BOX) * factor


def convert_channel(positions, mass, colour, kind, box_min, voxel, grid, factor,
                    mass_mode, label):
    """One channel -> (a8, rgb8): the brightness bytes and the chromaticity bytes.

    Steps: deposit the mass into voxels, blur by the residual width, turn the mass into a density
    (mass per unit box volume), normalise by the field's physical peak, and split the emission into
    a per-voxel chromaticity (RGB) and a per-voxel brightness (A).

    `factor` is the --smoothing factor of this channel, applied to its width by channel_width.
    """
    count = len(positions)
    spacing = local_spacing(positions)
    width = channel_width(spacing, factor)
    width_voxels = width / voxel
    residual = np.sqrt(max(width_voxels ** 2 - BINNING_VARIANCE, 0.0))

    floored = ", from the floor" if DEFAULT_WIDTH * spacing < MIN_WIDTH_BOX else ""
    print(f"  {label}: {count} particles, local spacing {spacing:.3e} "
          f"({spacing / voxel:.2f} voxels), width {width:.3e} box units "
          f"= {width_voxels:.2f} voxels (smoothing {factor:g}{floored}), "
          f"blur {residual:.2f} voxels")
    if width_voxels < MIN_WIDTH_SAMPLED_VOXELS:
        print(f"    note: the width is {width_voxels:.2f} voxels, below the "
              f"{MIN_WIDTH_SAMPLED_VOXELS:g} voxels this grid can carry - the field is the right "
              f"one but this resolution cannot sample it. Write a larger resolution to see it.")

    # --- deposit: mass per voxel, and the mass-weighted colour with it, so that the colour is
    # defined everywhere the density is and stays a weighted mean (never a sum, which would
    # brighten what it spreads).
    fraction = (positions - box_min) / voxel
    voxels = np.floor(fraction).astype(np.int64)
    inside = np.all((voxels >= 0) & (voxels < grid), axis=1)
    voxels, deposited_mass = voxels[inside], mass[inside]
    deposited_colour = colour[inside] if colour is not None else None
    if not inside.all():
        print(f"    {int((~inside).sum())} particles outside the box")

    shape = tuple(int(g) for g in grid)
    total = int(np.prod(grid))
    flat = np.ravel_multi_index((voxels[:, 0], voxels[:, 1], voxels[:, 2]), shape)
    mass_grid = np.bincount(flat, weights=deposited_mass, minlength=total).astype(np.float32)
    mass_grid = mass_grid.reshape(shape)

    if deposited_colour is not None:
        colour_grid = np.empty((*shape, 3), np.float32)
        for c in range(3):
            component = np.bincount(flat, weights=deposited_mass * deposited_colour[:, c],
                                    minlength=total).astype(np.float32)
            colour_grid[..., c] = component.reshape(shape)
    else:
        colour_grid = None

    if residual > 0.0:
        mass_grid = gaussian_filter(mass_grid, residual, mode="reflect")
        if colour_grid is not None:
            for c in range(3):
                colour_grid[..., c] = gaussian_filter(colour_grid[..., c], residual, mode="reflect")

    # Mass -> density: the renderer integrates the stored field along a ray with a step fixed in
    # box fractions, so the field it wants is mass per unit box volume.
    density = mass_grid / (voxel ** 3)

    occupied = mass_grid > 0.0
    if colour_grid is not None:
        colour_grid[occupied] /= mass_grid[occupied, None]
        del mass_grid
    else:
        colour_grid = np.empty((*shape, 3), np.float32)
        colour_grid[:] = DEFAULT_COLOR.get(kind, COLOR_NEUTRAL)
        occupied = np.ones(shape, bool)

    # --- the reference the bytes are scaled by: the field the same particles and the same kernel
    # produce, sampled on the particles. Both are resolution free - no grid, no voxel size enters -
    # so this is one number for a channel, and the same number at 200 and at 800. It is evaluated
    # with the width actually deposited, floor included, so that it is the peak of the field that
    # is about to be stored rather than of one the grid cannot hold.
    density_on_particles = field_on_particles(positions, mass, width)
    if colour is not None:
        luminosity = np.maximum(colour.max(axis=1), 0.0)
    else:
        luminosity = np.full(count, max(DEFAULT_COLOR.get(kind, COLOR_NEUTRAL)))
    emission_peak = float((density_on_particles * luminosity).max())
    if emission_peak <= 0.0:
        raise ValueError(f"{label}: channel has no emission (check the colours and the masses)")

    # --- split into chromaticity and brightness. emission = density * colour, so RGB carries the
    # colour (per voxel, exact, no gain to tune) and A carries how much light there is.
    emission = density[..., None] * colour_grid
    brightest = emission.max(axis=-1)
    a8 = (np.clip(brightest / emission_peak, 0.0, 1.0) * 255.0 + 0.5).astype(np.uint8)
    rgb8 = np.zeros((*shape, 3), np.uint8)
    lit = brightest > 0.0
    rgb8[lit] = (np.clip(emission[lit] / brightest[lit, None], 0.0, 1.0) * 255.0 + 0.5).astype(np.uint8)

    stored_peak = float((a8.max() / 255.0))
    print(f"    peak {emission_peak:.4e} per box unit, stored peak {stored_peak:.3f}, "
          f"{100.0 * np.count_nonzero(a8) / a8.size:.2f}% of the box lit")
    return a8, rgb8, emission_peak, spacing, width, width_voxels


# --------------------------------------------------------------------------- merge and write


def merge(channels, scale_reference):
    """Add the channels and split the sum back into brightness (A) and chromaticity (RGB).

    `channels` is a list of (rgb8, a8, intensity). The product rgb*a is each channel's emission in
    units of its own peak, so the sum is one field of light and the two bytes of a voxel are just
    its direction and its length. Nothing is renormalised by a maximum of the *sum*: the reference
    is taken from the channels at unit intensity and does not move when an intensity is lowered, so
    lowering one channel dims it and never brightens another.
    """
    shape = channels[0][0].shape
    total = np.zeros((*shape[:3], 3), np.float32)
    for rgb8, a8, intensity in channels:
        brightness = (a8.astype(np.float32) / 255.0) * intensity
        total += (rgb8.astype(np.float32) / 255.0) * brightness[..., None]
    brightest = total.max(axis=-1)
    reference = scale_reference
    if reference <= 0.0:
        raise ValueError("the merged field is empty")
    a8 = np.clip(brightest / reference, 0.0, 1.0)
    a8 = (a8 * 255.0 + 0.5).astype(np.uint8)
    rgb8 = np.zeros_like(total, np.uint8)
    lit = brightest > 0.0
    rgb8[lit] = (np.clip(total[lit] / brightest[lit, None], 0.0, 1.0) * 255.0 + 0.5).astype(np.uint8)
    del total
    return rgb8, a8


def ray_integral(a8, rgb8, voxel, axis) -> float:
    """Mean light delivered to a pixel by a ray through the box, face on.

    This is the number that has to be the same at every resolution, and printing it is the cheap
    way to check that it is: it is the line integral of the emission, which is what the renderer
    accumulates, averaged over the rays of the widest cross-section of the box. The voxel size
    enters because one voxel contributes its brightness times its own length along the ray, and
    `axis` is the thin axis of the box, along which a face-on ray runs.
    """
    emission = (a8.astype(np.float32) / 255.0)[..., None] * (rgb8.astype(np.float32) / 255.0)
    rays = emission.sum(axis=axis) * voxel
    return float((rays @ LUMA).mean())


def write_rgba(rgb8, a8, path, voxel):
    """Write RGB (colour) and A (brightness) as one gzipped RGBA8 texture in file order."""
    rgba = np.empty((*rgb8.shape[:3], 4), np.uint8)
    rgba[..., :3] = rgb8
    rgba[..., 3] = a8
    with gzip.open(path, "wb", compresslevel=6) as handle:
        handle.write(to_file_order(rgba).tobytes())
    print(f"  wrote {path} ({rgba.nbytes / 1024 / 1024:.2f} MB raw)")


def write_metadata(path, name, lines):
    with open(path, "w") as handle:
        handle.write("# Generated by particles-to-volume.py\n")
        handle.write("# Grid, world size, bounds and voxel size are in storage order (W, H, D)\n")
        handle.write("# = galactic (X, Z, -Y), the order the file is indexed in.\n")
        for line in lines:
            handle.write(f"{line}\n")
    print(f"  wrote {path}")


# --------------------------------------------------------------------------- main


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Generate the emissive and occlusive galaxy volume textures from particle "
                    "channels, at a resolution, with a per-channel intensity and smoothing.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument("resolution", type=int,
                        help="Voxels along the longest side of the box. The other axes follow the "
                             "shape of the data, so the voxels stay cubic and the memory follows "
                             "the data too (400 is 25 MB of RGBA for a flat galaxy, a cube of the "
                             "same 400 is ten times that).")
    parser.add_argument("input_dir", help="Directory holding the .dat.gz particle channels")
    parser.add_argument("output_dir", help="Directory to write the emissive and occlusive volumes")

    parser.add_argument("--emissive", type=parse_patterns, default=None, metavar="LIST",
                        help=f"Channels summed into the emissive volume, comma separated and "
                             f"matched as case-insensitive substrings. Default: {DEFAULT_EMISSIVE}")
    parser.add_argument("--occlusive", type=parse_patterns, default=None, metavar="LIST",
                        help=f"Channels summed into the occlusive volume. Default: {DEFAULT_OCCLUSIVE}")
    parser.add_argument("--intensity", type=parse_intensity, action="append", default=[],
                        metavar="NAME=FLOAT",
                        help="Brightness of one channel, in [0, 1]: 1 is the channel at its own "
                             "full range. Repeatable, e.g. --intensity Bulge=0.3. Channels left "
                             "out are 1.")
    parser.add_argument("--smoothing", type=parse_smoothing, action="append", default=[],
                        metavar="[NAME=]FACTOR",
                        help="How far a channel's particles are smeared, as a multiple of that "
                             "channel's own width. 1 (the default) writes the channel as it comes "
                             "out; 0.5 makes it half as thick and 0.25 a quarter, 2 twice as "
                             "thick. A bare number applies to every channel, NAME=FACTOR to one, "
                             "e.g. --smoothing ArmDust=0.5. It is a length, the same at every "
                             "resolution, so the picture does not change with it. Must be > 0.")
    parser.add_argument("--mass", choices=["size", "unit"], default="size",
                        help="What the 4th column of a particle file is: a mass, or nothing "
                             "('unit' gives every particle the same mass).")
    parser.add_argument("--prefix", default=None,
                        help="Prefix of the output files. Default: the common prefix of the input "
                             "channel file names (e.g. 'galaxy_').")
    parser.add_argument("--keep-channels", action="store_true",
                        help="Also write each channel's own RGBA texture, next to the merged ones.")
    args = parser.parse_args()

    if args.resolution < 8:
        parser.error("resolution must be at least 8")
    if not os.path.isdir(args.input_dir):
        parser.error(f"'{args.input_dir}' is not a directory")

    # --- which channels, and in which volume
    files = sorted(glob.glob(os.path.join(args.input_dir, "*.dat.gz")))
    if not files:
        parser.error(f"no .dat.gz particle channels in '{args.input_dir}'")
    names = [os.path.basename(f)[:-len(".dat.gz")] for f in files]
    emissive_patterns = args.emissive if args.emissive is not None else parse_patterns(DEFAULT_EMISSIVE)
    occlusive_patterns = args.occlusive if args.occlusive is not None else parse_patterns(DEFAULT_OCCLUSIVE)
    try:
        emissive = [match(p, names) for p in emissive_patterns]
        occlusive = [match(p, names) for p in occlusive_patterns]
    except ValueError as error:
        parser.error(str(error))
    overlap = set(emissive) & set(occlusive)
    if overlap:
        parser.error(f"channels in both volumes: {', '.join(sorted(overlap))}")
    skipped = [n for n in names if n not in emissive and n not in occlusive]

    intensities = {}
    for name, value in args.intensity:
        try:
            intensities[match(name, names)] = value
        except ValueError as error:
            parser.error(str(error))
    factors = {}
    default_factor = 1.0
    for name, value in args.smoothing:
        if name is None:
            default_factor = value
        else:
            try:
                factors[match(name, names)] = value
            except ValueError as error:
                parser.error(str(error))

    print("particle channels -> volume textures")
    print(f"  input  {args.input_dir}")
    print(f"  output {args.output_dir}")
    print(f"  emissive  {', '.join(emissive) or '(none)'}")
    print(f"  occlusive {', '.join(occlusive) or '(none)'}")
    if skipped:
        print(f"  skipped   {', '.join(skipped)} (in neither volume)")
    if not emissive and not occlusive:
        parser.error("no channel is in either volume")

    os.makedirs(args.output_dir, exist_ok=True)
    started = time.time()

    # --- read every channel that is used, and share one box between them
    channels = {}
    for name in emissive + occlusive:
        path = os.path.join(args.input_dir, name + ".dat.gz")
        positions, mass, colour = read_particles(path)
        if args.mass == "unit":
            mass = np.ones_like(mass)
        if len(positions) == 0:
            print(f"  note: {name} has no particles, skipping")
            continue
        channels[name] = (positions, mass, colour)

    if not channels:
        print("Error: no channel has any particles")
        return 1

    everything = np.concatenate([c[0] for c in channels.values()])
    box_min, box_max, span, voxel, grid, scale = make_box(everything, args.resolution)
    stored_grid = np.array([grid[0], grid[2], grid[1]])
    # The thin axis of the box: the direction a face-on ray runs along, and the one the box is
    # flat in for a galaxy disk.
    thin_axis = int(np.argmin(span))
    bounds_min, bounds_max = stored_bounds(box_min, box_max)
    print(f"  box {np.round(span, 4)} (galactic x, y, z), {scale:.4f} source units per stored unit")
    print(f"  grid {grid[0]}x{grid[1]}x{grid[2]} galactic = "
          f"{stored_grid[0]}x{stored_grid[1]}x{stored_grid[2]} storage, voxel {voxel:.5f}")

    converted = {}
    details = {}
    for group, names_in_group in (("emissive", emissive), ("occlusive", occlusive)):
        for name in names_in_group:
            if name not in channels:
                continue
            print(f"  converting {name} ({group})")
            positions, mass, colour = channels[name]
            positions = positions * scale
            mass = mass * scale ** 3
            kind = channel_kind(name)
            factor = factors.get(name, default_factor)
            a8, rgb8, _, spacing, width, width_voxels = convert_channel(
                positions, mass, colour, kind, box_min, voxel, grid, factor, args.mass, name)
            converted[name] = (group, a8, rgb8)
            details[name] = (spacing, width, width_voxels, factor)

    # --- the reference the merged bytes are scaled by: the brightest voxel of the sum of the
    # channels at unit intensity. Taken before any intensity is applied, so that the reference is a
    # property of the data and the intensities only ever dim.
    reference = {}
    for group, names_in_group in (("emissive", emissive), ("occlusive", occlusive)):
        group_names = [n for n in names_in_group if n in converted]
        if not group_names:
            continue
        unit = np.zeros(tuple(int(g) for g in grid) + (3,), np.float32)
        for name in group_names:
            _, a8, rgb8 = converted[name]
            unit += (rgb8.astype(np.float32) / 255.0) * (a8.astype(np.float32) / 255.0)[..., None]
        reference[group] = float(unit.max())
        del unit
        print(f"  {group}: merged reference {reference[group]:.4f} "
              f"({len(group_names)} channel{'s' if len(group_names) > 1 else ''} at unit intensity)")

    prefix = args.prefix
    if prefix is None:
        used = sorted(converted)
        prefix = os.path.commonprefix(used)
        if prefix and not prefix.endswith(("_", "-")):
            prefix += "_"
        prefix = prefix or "volume_"

    outputs = {}
    for group, names_in_group in (("emissive", emissive), ("occlusive", occlusive)):
        group_names = [n for n in names_in_group if n in converted]
        if not group_names:
            continue
        parts = [(converted[n][2], converted[n][1], intensities.get(n, 1.0)) for n in group_names]
        rgb8, a8 = merge(parts, reference[group])
        del parts
        base = os.path.join(args.output_dir, f"{prefix}{group}")
        write_rgba(rgb8, a8, base + "_rgba.raw.gz", voxel)
        light = ray_integral(a8, rgb8, voxel, thin_axis)
        outputs[group] = (base, light)
        if args.keep_channels:
            for name in group_names:
                _, a8c, rgb8c = converted[name]
                write_rgba(rgb8c, a8c, os.path.join(args.output_dir, name + "_rgba.raw.gz"), voxel)

        metadata = [
            f"Volume: {group}",
            f"Grid size: {stored_grid[0]}x{stored_grid[1]}x{stored_grid[2]}",
            f"Galactic grid (x, y, z): {grid[0]}x{grid[1]}x{grid[2]}",
            f"World size: [{span[0]:.6f}, {span[2]:.6f}, {span[1]:.6f}]",
            f"Volume bounds: [{bounds_min[0]:.6f}, {bounds_min[1]:.6f}, {bounds_min[2]:.6f}] "
            f"to [{bounds_max[0]:.6f}, {bounds_max[1]:.6f}, {bounds_max[2]:.6f}]",
            f"Data bounds: [{box_min[0]:.6f}, {box_min[1]:.6f}, {box_min[2]:.6f}] "
            f"to [{box_max[0]:.6f}, {box_max[1]:.6f}, {box_max[2]:.6f}]",
            f"Voxel size: {voxel:.8f} (cubic; the largest box side is {NORMALIZED_SIDE:g}, in "
            f"units of the object's size)",
            f"Resolution: {args.resolution} voxels on the longest side of the box",
            f"Normalization: {scale:.6f} (stored units = source units times this)",
            f"RGB = chromaticity of the light, A = brightness in units of the reference below",
            f"Merged reference: {reference[group]:.6f} (brightest voxel of the channels at unit "
            f"intensity; lowering an intensity dims, it never renormalises)",
            f"Mean ray integral (the number that must match across resolutions): {light:.8f}",
        ]
        for name in group_names:
            _, a8c, rgb8c = converted[name]
            spacing, width, width_voxels, factor = details[name]
            metadata.append(
                f"Source: {name} intensity {intensities.get(name, 1.0):.4f}, "
                f"smoothing {factor:g}x, "
                f"kind '{channel_kind(name)}', {len(channels[name][0])} particles, "
                f"stored peak {a8c.max() / 255.0:.3f}, kernel width {width:.8f} box units "
                f"({width_voxels:.2f} voxels, {width / (DEFAULT_WIDTH * spacing):.2f} x the "
                f"default width, {spacing:.8f} local inter-particle spacing)")
        write_metadata(base + "_metadata.txt", group, metadata)

    print(f"\nDone in {time.time() - started:.1f} s")
    for group, (base, light) in outputs.items():
        print(f"  {group:9s} {base}_rgba.raw.gz   mean ray integral {light:.8f}")
    print("\nPoint the scene at them with the material's volume0 (emissive) and volume1 "
          "(occlusive).\nTwo resolutions that agree on 'mean ray integral' draw the same picture.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
