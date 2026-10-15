#!/usr/bin/env python3
"""
Combine per-channel 3D volume textures into one emissive and one occlusive texture.

particles-to-3dtex.py writes one RGBA texture per emission channel (Gas, Bulge, ...)
and per absorbing channel (ArmDust), each normalised on its own, and the renderer
raymarches each of them separately with its own texture unit. This script merges them
instead:

    emissive  <- every emitting channel, one raymarch, colour = density-weighted mean
    occlusive <- every absorbing channel, one raymarch, colour = density-weighted mean

The inputs are the textures themselves (_rgba.raw.gz, or _density.raw.gz for a volume
that carries no colour), so the merge is a post-processing step that needs no particle
data. The <base>_metadata.txt next to each texture is read to

  * check that every input covers the same grid and the same volume bounds. Merging
    misaligned grids produces a texture that is wrong in a way nothing downstream
    reports: gas and dust offset by a voxel still renders, it just renders lies;
  * recover the colour gain the generator baked in, so that the merge is a
    density-weighted mean in the *emission* space rather than a mean of channels that
    were each exposed differently (Gas 15.33x, Bulge 19.02x at 400^3, i.e. a silent
    1.24x bias on the hue).

What the merge cannot recover is the *relative scale between channels*, because the
generator normalises each channel by its own maximum before writing. The weights are
therefore a free artistic knob, not a physical quantity - which is already true today,
where the shader receives two independent [0,1] fields.

Examples:

    # One emissive texture from gas and bulge, one occlusive from the dust.
    combine-volumes.py -o out/ \\
        --emissive galaxy_Gas_rgba.raw.gz \\
        --emissive galaxy_Bulge_rgba.raw.gz --weight bulge=0.6 \\
        --occlusive galaxy_ArmDust_rgba.raw.gz

    # Writes out/galaxy_emissive_rgba.raw.gz (+ _metadata.txt)
    #        out/galaxy_occlusive_rgba.raw.gz (+ _metadata.txt)

See also: particles-to-3dtex.py --group, which runs this at the end of a conversion.
"""

import argparse
import gzip
import os
import re
import sys
from dataclasses import dataclass

import numpy as np

# ---------------------------------------------------------------------------
# Texture I/O, shared with particles-to-3dtex.py. Keep the two in sync.
#
# _from_file_order() is the only piece the generator does not have, and it exists so that a
# texture reads back the way its author wrote it. The pair defines the axis convention
# shared by the generator, these merged files and VolumeTextureLoader: galactic (x, y, z)
# is written out as (x, z, -y) - a reordering of the file's axes that the C-order/GL
# mismatch forces, times the 90 deg in-plane rotation that puts the volume in the object
# frame the renderer samples it in (see particles-to-3dtex._to_file_order()). If the pair
# ever diverges from the generator's, the merged volume is silently rotated relative to the
# textures it was built from - nothing reports it, the galaxy just ends up on its side, or
# its dust lanes 90 deg off the star field. Merging a texture with itself and comparing the
# bytes is what pins it (see --check).
# ---------------------------------------------------------------------------

def _read_bytes(path: str) -> bytes:
    if path.endswith(".gz"):
        with gzip.open(path, "rb") as f:
            return f.read()
    with open(path, "rb") as f:
        return f.read()


def _write_bytes(data: bytes, output_path: str, compresslevel: int = 6):
    with gzip.open(output_path, "wb", compresslevel=compresslevel) as f:
        f.write(data)


def _to_file_order(grid: np.ndarray) -> np.ndarray:
    """Galactic order -> file order (x, z, -y): out[i, k, ny-1-j] = in[i, j, k].

    The exact inverse of _from_file_order(), and a byte-compatible copy of
    particles-to-3dtex._to_file_order() - the two must stay identical, or a merge rotates
    what it merges.
    """
    if grid.ndim == 4:
        return np.ascontiguousarray(grid.transpose(0, 2, 1, 3)[:, :, ::-1, :])
    return np.ascontiguousarray(grid.transpose(0, 2, 1)[:, :, ::-1])


def _from_file_order(grid: np.ndarray) -> np.ndarray:
    """Inverse of _to_file_order(): (x, z, -y) -> (x, y, z), so a stored texture reads back
    as it was written.

    Not the same expression as the writer: the flip has to be applied to the galactic Y axis,
    which is the *last* axis of the array in galactic order and the *last* one in file order
    too - but the reader starts from the file, so the flip comes first here and the
    transposition second.
    """
    if grid.ndim == 4:
        return np.ascontiguousarray(grid[:, :, ::-1, :].transpose(0, 2, 1, 3))
    return np.ascontiguousarray(grid[:, :, ::-1].transpose(0, 2, 1))


# Artifacts particles-to-3dtex.py appends to the channel basename, longest first so
# that ".raw.gz" cannot shadow "_rgba.raw.gz". Mirrors VolumeTextureLoader.
_VOLUME_SUFFIXES = (
    "_density.raw.gz", "_color.raw.gz", "_rgba.raw.gz",
    "_density.raw", "_color.raw", "_rgba.raw",
    ".raw.gz", ".raw",
)


def _volume_basename(path: str) -> tuple[str, str]:
    """Split a volume path into (directory, channel basename).

    The suffix is stripped from the *file name* only, exactly as
    VolumeTextureLoader.volumeBasename() does, so that a directory whose name happens
    to contain ".raw" is not mangled.
    """
    directory, name = os.path.split(path)
    for suffix in _VOLUME_SUFFIXES:
        if len(name) > len(suffix) and name.endswith(suffix):
            return directory, name[: -len(suffix)]
    return directory, name


def _detect_format(total_bytes: int, declared_grid=None) -> tuple[tuple | None, int | None]:
    """Resolve the grid and the channel count, from the metadata where possible.

    With a declared grid the resolution comes from it and only the channel count is read
    off the file size. That is the only way to handle a grid that is not a cube: a byte
    count says nothing about how the voxels are split between the axes, so 400x400x40 is
    simply unclassifiable from its size alone. VolumeTextureLoader does the same, for the
    same reason. Without a declared grid, the cube a byte count implies.
    """
    if declared_grid is not None:
        voxels = int(np.prod(declared_grid))
        if voxels > 0 and total_bytes % voxels == 0:
            channels = total_bytes // voxels
            if channels in (1, 3, 4):
                return tuple(int(g) for g in declared_grid), int(channels)
        return None, None

    for channels in (1, 4, 3):
        voxels = total_bytes / channels
        if voxels != int(voxels):
            continue
        edge = int(round(voxels ** (1.0 / 3.0)))
        if edge > 0 and edge ** 3 * channels == total_bytes:
            return (edge, edge, edge), channels
    return None, None


def _storage_to_galactic(grid) -> tuple[int, int, int]:
    """Storage order (W, H, D) -> galactic (x, y, z).

    The inverse of what _to_file_order() does when writing: an array read back with
    _from_file_order() is laid out in galactic order, so its shape is the galactic grid,
    while the metadata and the file describe it in storage order. Storage is galactic
    (x, z, -y), so this is the permutation (0, 2, 1) - the sign the frame rotation puts on
    the galactic Y axis belongs to the bounds, not to a count of voxels.
    """
    return int(grid[0]), int(grid[2]), int(grid[1])


def _format_grid(grid) -> str:
    """Render a grid resolution as '400x40x400'."""
    return "x".join(str(int(g)) for g in grid)


def _format_vector(values) -> str:
    return "[" + ", ".join(f"{float(v):.3f}" for v in values) + "]"


# ---------------------------------------------------------------------------
# Metadata
# ---------------------------------------------------------------------------

_BOUNDS_RE = re.compile(
    r"\[\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*\]\s*to\s*"
    r"\[\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*\]"
)
_GAIN_RE = re.compile(r"(-?\d+(?:\.\d+)?(?:[eE][-+]?\d+)?)")


def _read_metadata(path: str) -> dict:
    """Parse a <base>_metadata.txt into a {lowercased key: value} dict."""
    directory, base = _volume_basename(path)
    metadata_path = os.path.join(directory, base + "_metadata.txt")
    if not os.path.exists(metadata_path):
        return {}
    meta = {}
    with open(metadata_path, "r") as f:
        for line in f:
            if ":" in line:
                key, _, value = line.partition(":")
                meta[key.strip().lower()] = value.strip()
    return meta


def _parse_bounds(meta: dict) -> tuple[np.ndarray, np.ndarray] | tuple[None, None]:
    """Read the volume bounds. The generator writes 'Volume bounds:'; 'World bounds:'
    is the older spelling and VolumeTextureLoader accepts either."""
    for key in ("volume bounds", "world bounds"):
        match = _BOUNDS_RE.search(meta.get(key, ""))
        if match:
            values = [float(v) for v in match.groups()]
            return np.array(values[:3]), np.array(values[3:])
    return None, None


def _parse_color_gain(meta: dict) -> float:
    """The scalar gain particles-to-3dtex.py applied before quantising the colour."""
    match = _GAIN_RE.search(meta.get("color gain", ""))
    if not match:
        return 1.0
    gain = float(match.group(1))
    return gain if gain > 0 else 1.0


_GRID_RE = re.compile(r"^\s*(\d+)\s*(?:[xX*,\s]\s*(\d+)\s*[xX*,\s]\s*(\d+))?\s*$")


def _parse_grid(meta: dict) -> tuple[int, int, int] | None:
    """Read 'Grid size:', either one resolution (a cube) or 'WxHxD'.

    The three, when there are three, are in storage order (W, H, D) - the order the file
    is indexed in - which is what VolumeTextureLoader reads them as.
    """
    match = _GRID_RE.match(meta.get("grid size", ""))
    if not match:
        return None
    if match.group(2) is None:
        edge = int(match.group(1))
        return (edge, edge, edge)
    return (int(match.group(1)), int(match.group(2)), int(match.group(3)))


# ---------------------------------------------------------------------------
# Loading
# ---------------------------------------------------------------------------

@dataclass
class VolumeInput:
    path: str
    key: str                       # channel basename, e.g. 'galaxy_Gas'
    grid: tuple[int, int, int]     # galactic (x, y, z): the shape of the arrays below
    storage_grid: tuple[int, int, int]  # (W, H, D): what the metadata and the file say
    density: np.ndarray            # float32 (galactic grid), [0, 1]
    rgb: np.ndarray | None         # float32 (galactic grid, 3), [0, 1], gains undone
    color_gain: float
    metadata: dict
    weight: float = 1.0


def load_input(path: str) -> VolumeInput:
    if not os.path.exists(path):
        raise SystemExit(f"error: no such file: {path}")

    data = np.frombuffer(_read_bytes(path), dtype=np.uint8)

    directory, key = _volume_basename(path)
    meta = _read_metadata(path)
    declared_grid = _parse_grid(meta)

    storage_grid, channels = _detect_format(data.size, declared_grid)
    if storage_grid is None:
        if declared_grid is not None:
            raise SystemExit(
                f"error: {path}: {data.size} bytes is not a "
                f"{_format_grid(declared_grid)} grid of 1, 3 or 4 channels, which is what "
                "its metadata declares. The texture and its metadata are out of step (a "
                "stale or truncated file?); refusing to merge with guessed geometry."
            )
        raise SystemExit(
            f"error: {path}: cannot detect the volume format from {data.size} bytes. "
            "Expected n^3 (density), 3*n^3 (color) or 4*n^3 (color+density), or a "
            "'Grid size:' in the metadata."
        )
    if declared_grid is None:
        # Legitimate for the cubic volumes written before the metadata carried a grid, but
        # it cannot be told apart from a non-cubic volume whose byte count happens to admit
        # a cubic reading, so say which one was assumed.
        print(f"  note: {key} has no 'Grid size:' in its metadata, so its grid was "
              f"inferred from the file length as {_format_grid(storage_grid)} "
              f"({channels} channel{'s' if channels > 1 else ''}). A non-cubic grid "
              "cannot be recovered that way.", file=sys.stderr)
    if channels == 3:
        raise SystemExit(
            f"error: {path}: an RGB volume carries no density, so it cannot be merged. "
            "Combine the _rgba or _density artifact instead."
        )

    # The file is in storage order; read it back into galactic order, which is the order the
    # generator worked in and the order everything below merges in.
    volume = _from_file_order(data.reshape((*storage_grid, channels) if channels == 4 else storage_grid))

    density = np.ascontiguousarray(volume[..., 3] if channels == 4 else volume)
    density = density.astype(np.float32)
    density *= np.float32(1.0 / 255.0)

    rgb = None
    gain = 1.0
    if channels == 4:
        rgb = np.ascontiguousarray(volume[..., :3]).astype(np.float32)
        rgb *= np.float32(1.0 / 255.0)
        gain = _parse_color_gain(meta)
        if gain != 1.0:
            # Back to emission space. Applied per input, so the relative weight of two
            # inputs is decided by their emission, not by how each was exposed.
            rgb *= np.float32(1.0 / gain)
    del volume

    return VolumeInput(path=path, key=key, grid=_storage_to_galactic(storage_grid),
                       storage_grid=storage_grid, density=density, rgb=rgb,
                       color_gain=gain, metadata=meta)


# ---------------------------------------------------------------------------
# Merging
# ---------------------------------------------------------------------------

# Percentile of the occupied-voxel colour used to derive the gain. Mirrors
# particles-to-3dtex.COLOR_NORMALIZE_PERCENTILES, so a merged texture is quantised
# exactly like a per-channel one.
COLOR_NORMALIZE_PERCENTILES = {"max": 100.0, "p99": 99.0, "p999": 99.9}


def normalize_color(rgb: np.ndarray, density: np.ndarray, mode: str) -> tuple[np.ndarray, float]:
    """Scale the merged colour with a single scalar gain, hue preserving.

    A copy of particles-to-3dtex.normalize_color(), for the same reason: one gain over
    all three channels keeps every voxel's chromaticity, which is the information the
    colour texture exists to carry. Per-channel scaling would wash the hues to grey.
    """
    if mode == "none":
        return rgb, 1.0

    occupied = density > 0
    if not np.any(occupied):
        return rgb, 1.0

    peak = float(np.percentile(rgb[occupied], COLOR_NORMALIZE_PERCENTILES[mode]))
    if peak <= 0:
        return rgb, 1.0

    gain = 1.0 / peak
    rgb *= np.float32(gain)
    return rgb, gain


def merge(inputs: list[VolumeInput], norm: str, fallback_color: tuple[float, float, float]):
    """Density-weighted merge of one group.

    Densities add (each scaled by its weight). The colour is the density-weighted mean
    over the inputs that carry one: a voxel's emissivity is proportional to its density,
    so the mean emission colour of a merged voxel is sum(d_i * c_i) / sum(d_i). An input
    with no colour contributes density only, so a density-only file cannot drag the
    group's colour towards a fill value it never had.

    Returns (rgb, density, peak, has_color).
    """
    shape = tuple(int(g) for g in inputs[0].grid)
    density = np.zeros(shape, dtype=np.float32)
    rgb_sum = np.zeros((*shape, 3), dtype=np.float32)
    weight_sum = np.zeros(shape, dtype=np.float32)
    scratch = None

    for volume in inputs:
        weighted = volume.density * np.float32(volume.weight)
        density += weighted
        if volume.rgb is not None:
            if scratch is None:
                scratch = np.empty_like(rgb_sum)
            np.multiply(volume.rgb, weighted[..., None], out=scratch)
            rgb_sum += scratch
            weight_sum += weighted
        del weighted

    has_color = any(volume.rgb is not None for volume in inputs)
    if has_color:
        with np.errstate(divide="ignore", invalid="ignore"):
            np.divide(rgb_sum, weight_sum[..., None], out=rgb_sum,
                      where=weight_sum[..., None] > 0)
        rgb = rgb_sum
    else:
        # No input carried colour, so the group has none either; a flat fill keeps the
        # texture displayable without inventing structure.
        rgb = rgb_sum
        rgb[...] = fallback_color

    peak = float(density.max())
    if norm == "max" and peak > 0:
        density *= np.float32(1.0 / peak)

    return rgb, density, peak, has_color


# ---------------------------------------------------------------------------
# Output
# ---------------------------------------------------------------------------

def write_rgba(rgb: np.ndarray, density: np.ndarray, output_path: str):
    """Pack colour (RGB) and density (A) into one RGBA texture, as the generator does."""
    rgba = np.empty((*rgb.shape[:3], 4), dtype=np.uint8)

    np.clip(rgb, 0.0, 1.0, out=rgb)
    rgb *= np.float32(255.0)
    rgba[..., :3] = rgb.astype(np.uint8)

    np.clip(density, 0.0, 1.0, out=density)
    density *= np.float32(255.0)
    rgba[..., 3] = density.astype(np.uint8)

    _write_bytes(_to_file_order(rgba).tobytes(), output_path)
    print(f"Saved RGBA texture: {output_path} ({rgba.nbytes / 1024 / 1024:.2f} MB raw)")


def save_metadata(inputs: list[VolumeInput], group: str, output_dir: str, basename: str,
                  grid: tuple[int, int, int], bounds_min: np.ndarray, bounds_max: np.ndarray,
                  norm: str, peak: float, color_mode: str, color_gain: float, has_color: bool,
                  clipped: int):
    """Write <basename>_metadata.txt, in the shape the generator writes and
    VolumeTextureLoader reads.

    The grid and the box are the inputs' own, in *storage* order (W, H, D) - the order the
    file is indexed in, which is the order the shader samples it in too. The generator
    writes a matching note at the top of its metadata; this keeps the two files
    interchangeable for the loader.
    """
    metadata_path = os.path.join(output_dir, f"{basename}_metadata.txt")
    grid = tuple(int(g) for g in grid)
    world_size = np.asarray(bounds_max, dtype=float) - np.asarray(bounds_min, dtype=float)

    with open(metadata_path, "w") as f:
        f.write("# Grid, world size, bounds and voxel size are in storage order (W, H, D).\n")
        f.write(f"Channel: {'+'.join(v.key for v in inputs)}\n")
        f.write(f"Group: {group}\n")
        f.write(f"Grid size: {_format_grid(grid)}\n")
        f.write(f"World size: {_format_vector(world_size)}\n")
        f.write(f"Volume bounds: {_format_vector(bounds_min)} to {_format_vector(bounds_max)}\n")
        f.write(f"Voxel size: {_format_vector(world_size / np.asarray(grid, dtype=float))}\n")
        f.write(f"Has color: {has_color}\n")
        f.write(f"Formats: rgba\n")
        f.write(f"Color gain: {color_gain:.6f} (mode: {color_mode})\n")
        f.write(f"Sources: " + ", ".join(f"{v.key} (weight {v.weight:g})" for v in inputs) + "\n")
        f.write(f"Color gains undone: " + ", ".join(f"{v.key} {v.color_gain:.6f}" for v in inputs) + "\n")
        f.write(f"Density normalization: {norm} (peak {peak:.6f})\n")
        if clipped:
            f.write(f"Clipped voxels: {clipped} (density above 1 with --norm none)\n")

    print(f"Saved metadata: {metadata_path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_weight(text: str) -> tuple[str, float]:
    name, separator, value = text.partition("=")
    if not separator or not name.strip():
        raise argparse.ArgumentTypeError(f"expected NAME=VALUE, got '{text}'")
    try:
        weight = float(value)
    except ValueError:
        raise argparse.ArgumentTypeError(f"'{value}' is not a number")
    if weight < 0:
        raise argparse.ArgumentTypeError(f"weight for '{name}' must not be negative")
    return name.strip(), weight


def resolve_weight(name: str, inputs: list[VolumeInput]) -> VolumeInput:
    """Match a --weight name against the input channel basenames.

    Exact match first (ignoring case), then a unique substring match, so 'bulge',
    'Bulge' and 'galaxy_Bulge' all name the same input. An ambiguous or unknown name is
    an error rather than a guess: a silently ignored weight is a silently wrong image.
    """
    folded = name.lower()
    exact = [v for v in inputs if v.key.lower() == folded]
    if len(exact) == 1:
        return exact[0]
    partial = [v for v in inputs if folded in v.key.lower()]
    if len(partial) == 1:
        return partial[0]
    if not partial:
        raise SystemExit(
            f"error: --weight {name}: no input matches; the inputs are "
            f"{', '.join(v.key for v in inputs)}"
        )
    raise SystemExit(
        f"error: --weight {name}: ambiguous, matches {', '.join(v.key for v in partial)}"
    )


def common_prefix(keys: list[str]) -> str:
    """Longest common prefix of the channel names, trimmed to a separator boundary so
    'galaxy_Gas' + 'galaxy_Bulge' becomes 'galaxy_' and not 'galaxy_'. """
    prefix = os.path.commonprefix(keys)
    if prefix and not prefix.endswith(("_", "-", ".")):
        cut = max(prefix.rfind("_"), prefix.rfind("-"), prefix.rfind("."))
        prefix = prefix[: cut + 1] if cut >= 0 else ""
    return prefix


def check_alignment(inputs: list[VolumeInput]):
    """Refuse to merge textures that do not share a grid and a volume box."""
    reference = inputs[0]
    ref_min, ref_max = _parse_bounds(reference.metadata)
    if ref_min is None:
        raise SystemExit(
            f"error: {reference.path}: no 'Volume bounds:' in its _metadata.txt. The "
            "bounds place the volume in world space, so merging without them would "
            "move it. Regenerate the texture (particles-to-3dtex.py always writes the "
            "metadata) or supply the file that goes with it."
        )

    for volume in inputs[1:]:
        if tuple(volume.storage_grid) != tuple(reference.storage_grid):
            raise SystemExit(
                f"error: {volume.path} is {_format_grid(volume.storage_grid)} but "
                f"{reference.path} is {_format_grid(reference.storage_grid)}. Merging "
                "different grids needs a resample, which this script does not do."
            )
        other_min, other_max = _parse_bounds(volume.metadata)
        if other_min is None:
            raise SystemExit(f"error: {volume.path}: no 'Volume bounds:' in its _metadata.txt.")
        if not np.allclose(other_min, ref_min, atol=1e-3) or not np.allclose(other_max, ref_max, atol=1e-3):
            raise SystemExit(
                f"error: {volume.path} covers {_format_vector(other_min)} to "
                f"{_format_vector(other_max)}, but {reference.path} covers "
                f"{_format_vector(ref_min)} to {_format_vector(ref_max)}. Merging them "
                "would place the two channels in different boxes, so one of them would be "
                "stretched and offset against the other."
            )
    return ref_min, ref_max, reference


def main():
    parser = argparse.ArgumentParser(
        description="Combine per-channel volume textures into one emissive and one "
                    "occlusive texture.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )

    parser.add_argument("--emissive", action="append", default=[], metavar="PATH",
                        help="A volume texture to merge into the emissive texture "
                             "(_rgba.raw.gz or _density.raw.gz). Repeatable.")
    parser.add_argument("--occlusive", action="append", default=[], metavar="PATH",
                        help="A volume texture to merge into the occlusive texture. "
                             "Repeatable.")
    parser.add_argument("--weight", action="append", default=[], type=parse_weight,
                        metavar="NAME=VALUE",
                        help="Scale a channel's density before merging. NAME is matched "
                             "against the channel name ('bulge', 'Bulge', 'galaxy_Bulge'). "
                             "Repeatable. Weights are relative, not physical: the "
                             "generator normalises each channel on its own, so the "
                             "relative scale between channels is already gone.")
    parser.add_argument("-o", "--out-dir", default=".", metavar="DIR",
                        help="Directory to write the merged textures to.")
    parser.add_argument("--prefix", default=None,
                        help="Prefix for the output names (default: the longest common "
                             "prefix of the input channel names, e.g. 'galaxy_').")
    parser.add_argument("--norm", choices=["max", "none"], default="max",
                        help="Density normalisation: 'max' divides by the merged peak, so "
                             "the brightest voxel is 255; 'none' writes the weighted sum "
                             "as-is and clips at 1.")
    parser.add_argument("--color-normalize", choices=["none", "max", "p99", "p999"],
                        default="p999",
                        help="Scale the merged colour before 8-bit quantisation, using a "
                             "single hue-preserving gain taken from the occupied voxels.")
    parser.add_argument("--fallback-color", default="0.8,0.6,0.4", metavar="R,G,B",
                        help="Colour for a group whose inputs carry no colour at all.")
    parser.add_argument("--check", action="store_true",
                        help="Self-check: merge each input on its own and compare the "
                             "result with the original file. Costs an extra merge per "
                             "input.")

    args = parser.parse_args()

    if not args.emissive and not args.occlusive:
        parser.error("nothing to merge: pass at least one --emissive or --occlusive")

    overlap = set(args.emissive) & set(args.occlusive)
    if overlap:
        parser.error(f"these are listed as both emissive and occlusive: {', '.join(sorted(overlap))}")

    try:
        fallback_color = tuple(float(c) for c in args.fallback_color.split(","))
        if len(fallback_color) != 3:
            raise ValueError
    except ValueError:
        parser.error("--fallback-color expects three comma-separated numbers, e.g. 0.8,0.6,0.4")

    os.makedirs(args.out_dir, exist_ok=True)

    groups = [(name, paths) for name, paths in (("emissive", args.emissive),
                                                ("occlusive", args.occlusive)) if paths]

    for name, paths in groups:
        if len(set(paths)) != len(paths):
            parser.error(f"the same file is listed twice in the {name} group: {paths}")

    all_inputs = {}
    for _, paths in groups:
        for path in paths:
            all_inputs[path] = load_input(path)

    volumes = list(all_inputs.values())

    # Weights share one namespace across both groups, so a typo is an error even when
    # the group it was meant for is not the one being merged.
    for name, weight in args.weight:
        resolve_weight(name, volumes).weight = weight

    bounds_min, bounds_max, reference = check_alignment(volumes)
    grid = tuple(int(g) for g in reference.storage_grid)
    prefix = args.prefix if args.prefix is not None else common_prefix([v.key for v in volumes])

    for name, paths in groups:
        inputs = [all_inputs[path] for path in paths]
        print(f"\n{name.capitalize()} group (grid {_format_grid(grid)}, "
              f"bounds {_format_vector(bounds_min)} to {_format_vector(bounds_max)}):")
        for volume in inputs:
            print(f"  {volume.key:<24} weight {volume.weight:<6g} "
                  f"gain undone {volume.color_gain:>10.4f}   {volume.path}")

        rgb, density, peak, has_color = merge(inputs, args.norm, fallback_color)

        clipped = 0
        if args.norm == "none":
            clipped = int(np.count_nonzero(density > 1.0))
            if clipped:
                print(f"  warning: {clipped} voxels exceed a density of 1 and will be "
                      f"clipped; the merged peak is {float(density.max()):.3f}")

        rgb, color_gain = normalize_color(rgb, density, args.color_normalize)

        basename = f"{prefix}{name}"
        write_rgba(rgb, density, os.path.join(args.out_dir, f"{basename}_rgba.raw.gz"))
        save_metadata(inputs, name, args.out_dir, basename, grid,
                      bounds_min, bounds_max, args.norm, peak, args.color_normalize,
                      color_gain, has_color, clipped)
        del rgb, density

    if args.check:
        # Merge each input on its own and compare the bytes we would write with the bytes
        # of the original file. This is what pins the axis convention against the
        # generator: _to_file_order() and _from_file_order() must be exactly inverse *and*
        # match particles-to-3dtex._to_file_order(), or the merged volume comes out
        # silently rotated relative to the textures it was built from. Bytes are compared
        # rather than arrays, because a self consistent permutation error would cancel out
        # if both sides went through the same helpers.
        print("\nSelf-check: each input merged on its own, compared with the original file.")
        for volume in volumes:
            weight, volume.weight = volume.weight, 1.0
            rgb, density, _, has_color = merge([volume], "none", fallback_color)
            rgb, _ = normalize_color(rgb, density, args.color_normalize)
            volume.weight = weight

            rgba = np.empty((*rgb.shape[:3], 4), dtype=np.uint8)
            np.clip(rgb, 0.0, 1.0, out=rgb)
            rgba[..., :3] = (rgb * np.float32(255.0)).astype(np.uint8)
            np.clip(density, 0.0, 1.0, out=density)
            rgba[..., 3] = (density * np.float32(255.0)).astype(np.uint8)
            written = np.frombuffer(_to_file_order(rgba).tobytes(), dtype=np.uint8)
            del rgb, density, rgba

            original = np.frombuffer(_read_bytes(volume.path), dtype=np.uint8)
            original_rgba = original.size == written.size
            written = written.reshape(-1, 4) if original_rgba else written[3::4]
            original = original.reshape(-1, 4) if original_rgba else original

            written_density, original_density = written[:, 3], original[:, 3]
            if np.array_equal(written_density, original_density):
                report = "density byte-identical to the original"
            else:
                off = int(np.abs(written_density.astype(np.int16)
                                 - original_density.astype(np.int16)).max())
                report = f"density DIFFERS by up to {off} of 255"

            if has_color and original_rgba:
                # Only the voxels the renderer can actually sample: the generator writes a
                # colour into voxels whose density rounds to zero (the colour is blurred
                # from the pre-blur density, then the density is quantised), and the merge
                # sets those to black because nothing reads them.
                visible = written_density > 0
                dropped = int(np.count_nonzero((~visible)
                                               & (original[:, :3].max(axis=1) > 0)))
                off = int(np.abs(written[visible, :3].astype(np.int16)
                                 - original[visible, :3].astype(np.int16)).max())
                report += (f", colour off by {off} of 255 over the {int(visible.sum())} "
                           "visible voxels")
                if dropped:
                    report += (f" ({dropped} zero-density voxels drop their colour, which "
                               "nothing samples)")

            print(f"  {volume.key}: {report}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
