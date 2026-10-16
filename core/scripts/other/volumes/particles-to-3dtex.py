"""
Convert Gaia Sky particle channel files (.dat.gz) to 3D volume textures
"""

import argparse
import gzip
import os
import subprocess
import sys
import time
from dataclasses import dataclass

import numpy as np
from scipy.ndimage import gaussian_filter
from tqdm import tqdm

skip_files = ["galaxy_HII", "galaxy_Stars"]

# Artifacts that --format can request. Each token names exactly one output file;
# 'all' is shorthand for the whole set.
_FORMAT_TOKENS = ("density", "color", "rgba")
ALL_FORMATS = frozenset(_FORMAT_TOKENS)

def parse_format(value: str) -> set[str]:
    """Parse --format into the set of artifacts to write.

    Accepts a '|'- or ','-separated union of {density, color, rgba}, or 'all'.
    A token does not imply any other, so '--format color' writes the colour
    texture and nothing else:

        density -> <base>_density.raw.gz  (R8,   density)
        color   -> <base>_color.raw.gz    (RGB8, colour only)
        rgba    -> <base>_rgba.raw.gz     (RGBA8, colour in RGB, density in A)
    """
    tokens = [t.strip().lower() for t in value.replace("|", ",").split(",") if t.strip()]
    if not tokens:
        raise argparse.ArgumentTypeError("--format must not be empty")
    if "all" in tokens:
        if len(tokens) > 1:
            raise argparse.ArgumentTypeError("'all' cannot be combined with other tokens")
        return set(ALL_FORMATS)
    unknown = [t for t in tokens if t not in _FORMAT_TOKENS]
    if unknown:
        raise argparse.ArgumentTypeError(
            f"unknown format token(s): {', '.join(unknown)}; expected any of "
            f"{', '.join(_FORMAT_TOKENS)} (or 'all')"
        )
    return set(tokens)

def parse_grid_size(value: str):
    """Parse --grid-size: '400' (cubic), or '400x40x400' in storage order (W, H, D).

    Storage order is the order of the file and of the metadata's 'Grid size:', which is
    galactic (X, Z, -Y). So a flat galaxy disk, whose thickness is the galactic Z, wants
    '400x40x400': a texture 400 wide, 40 tall, 400 deep, holding a 400x400x40 galactic
    grid. 'x', '*' and ',' all separate the axes.
    """
    parts = [p for p in value.strip().lower().replace("*", "x").replace(",", "x").split("x") if p.strip()]
    try:
        numbers = [int(p) for p in parts]
    except ValueError:
        raise argparse.ArgumentTypeError(f"'{value}' is not a resolution or a WxHxD grid") from None
    if len(numbers) == 1 and numbers[0] > 0:
        return numbers[0]
    if len(numbers) == 3 and all(n > 0 for n in numbers):
        return tuple(numbers)
    raise argparse.ArgumentTypeError(
        f"grid must be one positive resolution or three (WxHxD), got '{value}'")

def must_skip(string):
    return any(i in string for i in skip_files)


def parse_group(value: str) -> dict:
    """Parse --group into {'emissive': [(pattern, weight), ...], 'occlusive': [...]}.

    The spec is '<emissive>|<occlusive>', and each side is a '+'-separated list of
    channel names, each optionally followed by ':weight':

        --group 'Gas+Bulge:0.6|ArmDust'

    A pattern is matched as a case-insensitive substring of the channel name, and it has
    to match exactly one of the channels this run converts; an unknown or ambiguous
    pattern stops the run rather than silently dropping a channel. Either side may be
    empty, and a spec with no '|' is taken to be all-emissive.
    """
    emissive, separator, occlusive = value.partition("|")
    groups = {}
    for name, side in (("emissive", emissive), ("occlusive", occlusive)):
        entries = []
        for item in side.split("+"):
            item = item.strip()
            if not item:
                continue
            pattern, _, weight = item.partition(":")
            try:
                entries.append((pattern.strip(), float(weight) if weight else 1.0))
            except ValueError:
                raise argparse.ArgumentTypeError(
                    f"'{weight}' in '{item}' is not a number") from None
        groups[name] = entries
    if not groups["emissive"] and not groups["occlusive"]:
        raise argparse.ArgumentTypeError("no channel named")
    return groups


def run_group_combiner(groups: dict, channels: list, out_dir: str, color_normalize: str,
                       norm: str) -> int:
    """Merge the channels just written into one emissive and one occlusive texture.

    Delegates to combine-volumes.py as a separate process, so the merge itself lives in
    one place, stays usable on its own on textures from an earlier run, and can be
    re-run with different weights without converting the particles again.
    """
    script = os.path.join(os.path.dirname(os.path.abspath(__file__)), "combine-volumes.py")
    if not os.path.exists(script):
        print(f"Error: --group needs {script}, which is missing")
        return 1

    command = [sys.executable, script, "-o", out_dir,
               "--color-normalize", color_normalize, "--norm", norm]

    for name, entries in groups.items():
        option = "--emissive" if name == "emissive" else "--occlusive"
        for pattern, weight in entries:
            matches = [c for c in channels if pattern.lower() in c.lower()]
            if not matches:
                print(f"Error: --group: nothing matches '{pattern}'; this run wrote "
                      f"{', '.join(channels) if channels else 'nothing'}")
                return 1
            if len(matches) > 1:
                print(f"Error: --group: '{pattern}' is ambiguous, it matches "
                      f"{', '.join(matches)}")
                return 1
            command += [option, os.path.join(out_dir, f"{matches[0]}_rgba.raw.gz")]
            if weight != 1.0:
                command += ["--weight", f"{matches[0]}={weight}"]

    # Flush before handing the terminal over: the parent's prints are block buffered when
    # stdout is a pipe, so without this the child's output would overtake them.
    print(f"\n{'=' * 50}\nCombining groups")
    print("  " + " ".join(command), flush=True)
    return subprocess.run(command).returncode


@dataclass
class Particle:
    x: float
    y: float 
    z: float
    size: float
    r: float = 0.0
    g: float = 0.0
    b: float = 0.0

class ParticleChannel:
    def __init__(self, name: str):
        self.name = name
        self.particles = []
        self.has_color = False
        self.particle_count = 0
    
    def add_particle(self, particle: Particle):
        self.particles.append(particle)
        self.particle_count = len(self.particles)

def storage_to_galactic(values):
    """(W, H, D) in storage order -> (nx, ny, nz) in galactic order.

    _to_file_order() writes storage = galactic (x, z, -y), so storage axis 0 is galactic X
    and axis 2 is galactic Y: the first and last axes are transposed. A transposition is its
    own inverse, so this is the same function as galactic_to_storage() - the two exist
    because the two directions read differently at the call sites.
    """
    return values[0], values[2], values[1]

def galactic_to_storage(values):
    """(nx, ny, nz) in galactic order -> (W, H, D) in storage order."""
    return values[0], values[2], values[1]

# The permutation between the two orders, as an index array: storage[k] = galactic[...].
# Used to permute the *sizes* (world size, voxel size) the same way the grid is permuted.
# A size has no sign, so the negation the frame rotation puts on the galactic Y axis is not
# part of this: it is applied by stored_bounds(), and only there.
_GALACTIC_TO_STORAGE = np.array([0, 2, 1])

def _grid_axes(grid) -> tuple[np.ndarray, np.ndarray]:
    """Validate a resolution and return it as (storage order, galactic order)."""
    scalar = np.isscalar(grid)
    storage = (int(grid),) * 3 if scalar else tuple(int(g) for g in grid)
    if len(storage) != 3 or any(g <= 0 for g in storage):
        raise ValueError(f"grid must be a positive resolution or three of them, got {grid!r}")
    return np.array(storage, dtype=int), np.array(storage_to_galactic(storage), dtype=int)

class VolumeConfig:
    """The geometry of one volume: its grid resolution and the world-space box it covers.

    Both are per-axis, and both are *galactic* (x, y, z) internally - the order the
    particle files use and the order the rasteriser indexes. The file, and therefore the
    metadata, is in *storage* order (W, H, D) = galactic (X, Z, -Y), because
    _to_file_order() permutes the array on the way out; see stored_grid() and
    stored_bounds(). Both orders are written to the metadata, so neither has to be
    inferred from the other.

    The box used to be forced to a cube (`max(span) * 1.05` on every axis). On a flat
    galaxy disk that put most of the texture over empty space: 400 cubed of RGBA is
    244 MiB, where a 400x400x40 grid of the *same voxel size* is 24 MiB. The grid and the
    box have to be flattened together, or the voxels become anisotropic - the same memory,
    but banded along the stretched axis.

    Args:
        grid: A single resolution (cubic), or three in *storage* order (W, H, D), so that
            `--grid-size 400x40x400` means what the metadata's 'Grid size:' says.
        world_bounds: The (min, max) data bounds, in galactic order.
        cube: Force a cubic box of the largest span. Defaults to True for a scalar grid
            (the historical behaviour, so `--grid-size 400` is unchanged) and False for a
            per-axis grid, which reads as a request for a box shaped like the data.
        world_size: Fallback box size when there are no data bounds; scalar or three
            galactic values. Only its shape survives - see `normalize`.
        normalize: Rescale the box about the origin so its largest side is
            NORMALIZED_SIDE, i.e. so the box is the object's size and the *data* only
            decides its shape. This is what leaves the object's size in the JSON as the
            only thing that decides how large the volume is drawn. Pass False to keep the
            box in the source units, as the generator did before, where the metadata bounds
            also carried a size multiplier of their own.
    """

    # Margin the box adds to the data bounds. compute_world_bounds() has already padded
    # those by 5% per side, so the box ends up 10% larger than the raw particle extent,
    # as the cubic box always has.
    PADDING = 1.05

    # Side of the largest axis of the box, in the units the metadata declares. The box is
    # normalized to it, so the numbers the renderer reads are the object's size times a
    # number of order 1, and the source units - arbitrary, and different from one data set
    # to the next - never reach it. See _normalize().
    NORMALIZED_SIDE = 1.0

    def __init__(self, grid=256, world_bounds=None, cube=None, world_size=1.0, normalize=True):
        scalar_grid = np.isscalar(grid)
        self.storage_grid, self.grid = _grid_axes(grid)

        if world_bounds is not None:
            self.world_bounds = (np.asarray(world_bounds[0], dtype=float),
                                 np.asarray(world_bounds[1], dtype=float))
            min_bound, max_bound = self.world_bounds
            span = max_bound - min_bound
            if scalar_grid if cube is None else cube:
                span = np.repeat(np.max(span) * self.PADDING, 3)
            else:
                span = span * self.PADDING
            self.world_center = (min_bound + max_bound) / 2
        else:
            self.world_center = np.zeros(3)
            side = np.asarray(world_size, dtype=float)
            span = np.repeat(side, 3) if side.ndim == 0 else side
            self.world_bounds = (self.world_center - span / 2, self.world_center + span / 2)

        # The box, in galactic order.
        self.world_size = span
        self.box_min = self.world_center - span / 2
        self.box_max = self.world_center + span / 2

        # The data bounds as they came in, before _normalize() rescaled them. The metadata
        # records both, so a run can always be related back to the source data.
        self.raw_data_bounds = self.world_bounds

        self.normalization = 1.0
        if normalize:
            self._normalize()

        # Per-axis, in galactic order, in the normalized units. voxel_scale is the single
        # number the isotropic kernels (the smart splat and the Gaussian blurs) want; the
        # particle sizes are scaled by the same factor (see parse_particle_file), so a
        # splat's radius in voxels - the ratio of the two - is unaffected.
        self.voxel_size = self.world_size / self.grid
        self.voxel_scale = float(np.mean(self.voxel_size))

    def _normalize(self):
        """Rescale the box about the origin so its largest side is NORMALIZED_SIDE.

        The renderer takes the volume's size from the box alone: the shader multiplies the
        metadata bounds by the object's size. A box of side 7.06 therefore draws the galaxy
        7.06 times the size the JSON asks for, which is what made the Milky Way three and a
        half times too large. Normalizing here leaves the object's size as the only thing
        that decides how large the volume is drawn, and the data as the only thing that
        decides its shape.

        Scaling about the origin, not about the box centre, is deliberate: the particle
        pipeline places a point at its coordinate times size/2 with no recentring, so a data
        set whose centre is off the origin has to stay off-centre, in the box and in the
        particles alike.
        """
        self.normalization = float(self.NORMALIZED_SIDE / np.max(self.world_size))
        self.world_size = self.world_size * self.normalization
        self.world_center = self.world_center * self.normalization
        self.box_min = self.world_center - self.world_size / 2
        self.box_max = self.world_center + self.world_size / 2
        self.world_bounds = (self.world_bounds[0] * self.normalization,
                             self.world_bounds[1] * self.normalization)

    def set_grid(self, grid):
        """Replace the resolution, keeping the box.

        Used by --voxel-size, which derives the grid from the box rather than the box from
        the grid, and can only be resolved once the box is known.
        """
        self.storage_grid, self.grid = _grid_axes(grid)
        self.voxel_size = self.world_size / self.grid
        self.voxel_scale = float(np.mean(self.voxel_size))

    def stored_grid(self) -> np.ndarray:
        """The grid resolution in storage order, i.e. as the metadata and the file have it."""
        return self.storage_grid

    def stored_bounds(self) -> tuple[np.ndarray, np.ndarray]:
        """The box in storage order: the (x, z, -y) extremes of the galactic box.

        The metadata has to be in the order the *file* is indexed in, because the shader
        turns a position into texture coordinates per axis: the first coordinate samples
        storage axis 0. For a cubic box centred on the origin the two orders coincide, which
        is why this went unnoticed until the box became flat.

        Axis 2 is the galactic Y backwards (see _to_file_order()), so it is the one axis
        whose stored extremes are not simply a reordering: the interval flips, and the
        minimum comes from the galactic maximum.
        """
        return (np.array([self.box_min[0], self.box_min[2], -self.box_max[1]]),
                np.array([self.box_max[0], self.box_max[2], -self.box_min[1]]))

def compute_world_bounds(input_dir: str) -> tuple[np.ndarray, np.ndarray]:
    """Compute bounding box from all particle files"""
    print("Computing world bounds from all particle files...")
    
    particle_files = [f for f in os.listdir(input_dir) if f.endswith('.dat.gz') and not must_skip(f)]
    
    if not particle_files:
        raise ValueError(f"No .dat.gz files found in {input_dir}")
    
    # Initialize with extreme values
    all_min = np.array([np.inf, np.inf, np.inf])
    all_max = np.array([-np.inf, -np.inf, -np.inf])
    total_particles = 0
    
    for particle_file in tqdm(particle_files, desc="Scanning files"):
        filepath = os.path.join(input_dir, particle_file)
        
        try:
            with gzip.open(filepath, 'rt') as f:
                for line_num, line in enumerate(f):
                    values = line.strip().split()
                    
                    # Skip empty lines
                    if len(values) == 0:
                        continue
                    
                    # Skip header lines by checking if first value is numeric
                    try:
                        float(values[0])
                    except ValueError:
                        continue
                    
                    # Try to parse as particle data
                    if len(values) >= 4:
                        try:
                            x, y, z, size = map(float, values[:4])
                            
                            # Update bounds
                            all_min[0] = min(all_min[0], x)
                            all_min[1] = min(all_min[1], y) 
                            all_min[2] = min(all_min[2], z)
                            all_max[0] = max(all_max[0], x)
                            all_max[1] = max(all_max[1], y)
                            all_max[2] = max(all_max[2], z)
                            
                            total_particles += 1
                            
                        except ValueError as e:
                            # Skip lines that can't be parsed
                            continue
        except Exception as e:
            print(f"Error reading {particle_file}: {e}")
            continue
    
    print(f"Computed bounds from {total_particles} particles across {len(particle_files)} files")
    print(f"Bounding box:")
    print(f"  X: [{all_min[0]:.3f}, {all_max[0]:.3f}] -> span: {all_max[0] - all_min[0]:.3f}")
    print(f"  Y: [{all_min[1]:.3f}, {all_max[1]:.3f}] -> span: {all_max[1] - all_min[1]:.3f}")
    print(f"  Z: [{all_min[2]:.3f}, {all_max[2]:.3f}] -> span: {all_max[2] - all_min[2]:.3f}")
    
    # Add 5% padding to ensure all particles are inside
    padding = (all_max - all_min) * 0.05
    all_min -= padding
    all_max += padding
    
    print(f"With 5% padding:")
    print(f"  X: [{all_min[0]:.3f}, {all_max[0]:.3f}]")
    print(f"  Y: [{all_min[1]:.3f}, {all_max[1]:.3f}]")
    print(f"  Z: [{all_min[2]:.3f}, {all_max[2]:.3f}]")
    
    return all_min, all_max

def parse_particle_file(filename: str, world_bounds: (None|tuple[np.ndarray, np.ndarray]) = None,
                        scale: float = 1.0) -> ParticleChannel:
    """Parse gzipped particle files - handles both with and without headers.

    `world_bounds` is in the *source* units and only decides which particles are inside the
    box. `scale` then converts the coordinates and the particle sizes to the normalized
    units the box is in (see VolumeConfig._normalize()); the sampling is unaffected, because
    the box is scaled by the same factor, so a particle lands in the voxel it always landed
    in. The sizes have to follow the coordinates: a splat's radius in voxels is the
    particle's size over the voxel size, and both are scaled.
    """
    channel_name = os.path.basename(filename)
    channel = ParticleChannel(channel_name)
    
    try:
        with gzip.open(filename, 'rt') as f:
            particles_parsed = 0
            particles_skipped = 0
            
            for line_num, line in enumerate(f):
                values = line.strip().split()
                
                # Skip empty lines
                if len(values) == 0:
                    continue
                
                # Try to parse the first value as float to detect header lines
                try:
                    # If first value is not a number, it's probably a header line
                    float(values[0])
                except ValueError:
                    # This line is likely a header, skip it
                    continue
                
                # Now try to parse as particle data
                if len(values) >= 4:
                    try:
                        x, y, z, size = map(float, values[:4])
                        
                        # Skip particles outside world bounds if bounds are provided
                        if world_bounds is not None:
                            min_bound, max_bound = world_bounds
                            if (x < min_bound[0] or x > max_bound[0] or 
                                y < min_bound[1] or y > max_bound[1] or 
                                z < min_bound[2] or z > max_bound[2]):
                                particles_skipped += 1
                                continue
                        
                        # Normalized units from here on; the culling above needed the
                        # source ones. See the docstring.
                        x, y, z, size = x * scale, y * scale, z * scale, size * scale
                        
                        # Check if this line has color data (7 columns)
                        if len(values) >= 7:
                            try:
                                r, g, b = map(float, values[4:7])
                                particle = Particle(x, y, z, size, r, g, b)
                                channel.has_color = True
                            except ValueError:
                                # Color values might be invalid, use default
                                particle = Particle(x, y, z, size)
                        else:
                            particle = Particle(x, y, z, size)
                            
                        channel.add_particle(particle)
                        particles_parsed += 1
                        
                    except ValueError as e:
                        print(f"Warning: Could not parse line {line_num} in {filename}: {e}")
                        continue
            
            if particles_skipped > 0:
                print(f"  Skipped {particles_skipped} particles outside world bounds")
                
    except Exception as e:
        print(f"Error reading {filename}: {e}")
    
    print(f"Parsed {channel.particle_count} particles from {channel_name}")
    return channel

def to_grid_coords(positions: np.ndarray, config: VolumeConfig) -> np.ndarray:
    """World positions -> integer grid indices, per axis, in galactic order.

    The box is config.box_min..box_max, not the data bounds: the same box the metadata
    declares, so a particle lands in the voxel index the renderer will sample it from.
    """
    fraction = (positions - config.box_min) / (config.box_max - config.box_min)
    return (fraction * config.grid).astype(int)

def particles_to_volume_ultrafast(channel: ParticleChannel, config: VolumeConfig) -> tuple[np.ndarray, np.ndarray]:
    """Conversion using direct voxel assignment"""
    
    print(f"Converting {channel.particle_count} particles to volume grid...")
    start_time = time.time()
    
    density_grid = np.zeros(tuple(config.grid), dtype=np.float32)
    color_grid = np.zeros((*config.grid, 3), dtype=np.float32)
    
    if channel.particle_count == 0:
        return density_grid, color_grid
    
    # Convert all particles to grid coordinates
    positions = np.array([[p.x, p.y, p.z] for p in channel.particles])
    sizes = np.array([p.size for p in channel.particles])
    
    if channel.has_color:
        colors = np.array([[p.r, p.g, p.b] for p in channel.particles])
    else:
        colors = np.ones((len(channel.particles), 3))
    
    grid_coords = to_grid_coords(positions, config)
    
    # Filter valid particles. The grid is per-axis, so the test is too.
    valid_mask = np.all((grid_coords >= 0) & (grid_coords < config.grid), axis=1)
    
    grid_coords = grid_coords[valid_mask]
    sizes = sizes[valid_mask]
    colors = colors[valid_mask]
    
    print(f"  {len(grid_coords)} particles within volume bounds")
    
    if len(grid_coords) == 0:
        return density_grid, color_grid
    
    # Calculate weights based on particle size (normalized)
    max_size = np.max(sizes) if np.max(sizes) > 0 else 1.0
    weights = sizes / max_size
    
    # Add density to the nearest voxel for each particle
    for i, (x, y, z) in enumerate(grid_coords):
        density_grid[x, y, z] += weights[i]
        if channel.has_color:
            # Colour must accumulate with the SAME weight as density, so that
            # the later division by density_grid yields a size-weighted mean
            # colour. An extra factor of sizes[i] here would bias the mean
            # towards large particles and scale it by the particle size.
            for c in range(3):
                color_grid[x, y, z, c] += colors[i, c] * weights[i]
    
    # Normalize color grid
    if channel.has_color:
        nonzero_mask = density_grid > 0
        for c in range(3):
            color_grid[nonzero_mask, c] /= density_grid[nonzero_mask]
        # Zero-density voxels are left black (0,0,0). color_grid starts as
        # zeros and is only written where density > 0, so no fill is needed.
    
    elapsed = time.time() - start_time
    print(f"Volume conversion completed in {elapsed:.2f} seconds")
    print(f"Volume density range: {density_grid.min():.6f} to {density_grid.max():.6f}")
    print(f"Non-zero voxels: {np.sum(density_grid > 0)} / {int(np.prod(config.grid))}")
    
    return density_grid, color_grid

def particles_to_volume_smart(channel: ParticleChannel, config: VolumeConfig) -> tuple[np.ndarray, np.ndarray]:
    """SMART conversion with adaptive sampling - good balance of speed and quality"""
    
    print(f"Converting {channel.particle_count} particles to volume grid (smart sampling)...")
    start_time = time.time()
    
    density_grid = np.zeros(tuple(config.grid), dtype=np.float32)
    color_grid = np.zeros((*config.grid, 3), dtype=np.float32)
    
    if channel.particle_count == 0:
        return density_grid, color_grid
    
    # Extract particle data
    positions = np.array([[p.x, p.y, p.z] for p in channel.particles])
    sizes = np.array([p.size for p in channel.particles])
    
    if channel.has_color:
        colors = np.array([[p.r, p.g, p.b] for p in channel.particles])
    else:
        colors = np.ones((len(channel.particles), 3))
    
    grid_coords = to_grid_coords(positions, config)
    
    # Filter valid particles. The grid is per-axis, so the test is too.
    valid_mask = np.all((grid_coords >= 0) & (grid_coords < config.grid), axis=1)
    
    grid_coords = grid_coords[valid_mask]
    sizes = sizes[valid_mask]
    colors = colors[valid_mask]
    
    print(f"  {len(grid_coords)} particles within volume bounds")
    
    if len(grid_coords) == 0:
        return density_grid, color_grid
    
    # Use a simple Gaussian splatting approach
    # For each particle, add density to a small 3x3x3 region
    kernel_radius = 1  # 3x3x3 kernel
    
    for i in tqdm(range(len(grid_coords)), desc="Processing particles"):
        cx, cy, cz = grid_coords[i]
        size_factor = min(2.0, sizes[i] / config.voxel_scale)  # Limit influence
        
        # Small kernel around particle
        for dx in range(-kernel_radius, kernel_radius + 1):
            for dy in range(-kernel_radius, kernel_radius + 1):
                for dz in range(-kernel_radius, kernel_radius + 1):
                    x = cx + dx
                    y = cy + dy
                    z = cz + dz
                    
                    if (0 <= x < config.grid[0] and 
                        0 <= y < config.grid[1] and 
                        0 <= z < config.grid[2]):
                        
                        # Simple distance-based weight
                        dist = np.sqrt(dx**2 + dy**2 + dz**2)
                        weight = np.exp(-dist * 2) * size_factor
                        
                        density_grid[x, y, z] += weight
                        if channel.has_color:
                            for c in range(3):
                                color_grid[x, y, z, c] += colors[i, c] * weight
    
    # Normalize color grid
    if channel.has_color:
        nonzero_mask = density_grid > 0
        for c in range(3):
            color_grid[nonzero_mask, c] /= density_grid[nonzero_mask]
    
    elapsed = time.time() - start_time
    print(f"Volume conversion completed in {elapsed:.2f} seconds")
    print(f"Volume density range: {density_grid.min():.6f} to {density_grid.max():.6f}")
    
    return density_grid, color_grid

def _blur_color(color_grid: np.ndarray, density_grid: np.ndarray, sigma: float) -> np.ndarray:
    """Spread the colour over the same neighbourhood as the density blur.

    process_channel_specific() blurs the density, which pushes density into
    voxels that were never assigned a colour - under 'ultrafast' a particle only
    writes its own voxel. Those voxels then carry density with a black colour:
    measured at 50.6% of the Gas and 31.9% of the Bulge density mass. Taking the
    emission colour from the texture would therefore turn every blurred tail
    into a dark rim, defeating the blur.

    Blurring the density-weighted colour and dividing by the blurred density
    propagates the colour with exactly the same kernel, so the colour field is
    defined wherever the density is and remains a weighted mean (never a sum,
    so the spread does not brighten anything).
    """
    if sigma <= 0:
        return color_grid

    numerator = gaussian_filter(color_grid * density_grid[..., None], sigma=(sigma, sigma, sigma, 0))
    denominator = gaussian_filter(density_grid, sigma=sigma)

    out = np.zeros_like(color_grid)
    nonzero = denominator > 1e-9
    out[nonzero] = numerator[nonzero] / denominator[nonzero, None]
    return out

def process_channel_specific(channel: ParticleChannel, density_grid: np.ndarray, color_grid: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    """Apply channel-specific processing"""
    channel_name = channel.name.lower()
    
    # Gaussian sigma for the density blur, and the fallback colour used for
    # channels that carry no colour column of their own.
    if 'dust' in channel_name:
        sigma, default_color = 0.2, (0.8, 0.6, 0.4)
    elif 'gas' in channel_name:
        sigma, default_color = 1.5, (0.3, 0.5, 0.8)
    elif 'hii' in channel_name:
        sigma, default_color = 1.2, (1.0, 0.4, 0.6)
    elif 'bulge' in channel_name:
        sigma, default_color = 3.0, (1.0, 0.9, 0.6)
    elif 'star' in channel_name:
        sigma, default_color = 0.3, (1.0, 0.95, 0.9)
    else:
        sigma, default_color = 0.0, None
    
    # Colour first, from the *unblurred* density, so that it spreads as far as
    # the density is about to.
    if channel.has_color:
        color_grid = _blur_color(color_grid, density_grid, sigma)
    
    if sigma > 0:
        density_grid = gaussian_filter(density_grid, sigma=sigma)
    
    if not channel.has_color and default_color is not None:
        for c in range(3):
            color_grid[:,:,:,c] = default_color[c]
    
    print(f"Applied {channel_name} channel processing")
    return density_grid, color_grid

def _to_file_order(grid: np.ndarray) -> np.ndarray:
    """Galactic-order array -> the order the file is indexed in: galactic (x, z, -y).

    Two things stack up here, and both have to be undone at the other end.

    The first is the permutation glTexImage3D needs. A volume file is a C-order dump, so its
    last axis varies fastest, while GL reads its client buffer with the *first* axis varying
    fastest; VolumeTextureLoader.glGrid() declares the extents reversed to compensate. The
    texture GL ends up with is therefore the file's three axes in reverse, and this writes
    them out in reverse.

    The second is a rotation. The renderer places the particles of the same data set through
    BillboardSetInitializer.transformData():

        aux.set(x, z, y).scl(size / 2).rotate(-90, 0, 1, 0).mul(transform).add(position)

    so a galactic point (x, y, z) ends up at the object-local (-y, z, x) - a 90 deg turn in
    the disk plane, about the axis the disk is thin along. The volume shader identifies the
    texture's three axes with those same local axes and samples the box with them, so the
    texture has to be authored in that rotated frame: without it the volume is 90 deg off
    every other representation of the same data, which shows on the dust lanes against the
    star field and nowhere else, the gas being close to axisymmetric.

    Galactic (x, y, z) -> texture (x_t, y_t, z_t) is then

        x_t = -y,   y_t = z,   z_t = x

    and the file, which holds the texture reversed, is (z_t, y_t, x_t) = (x, z, -y). Reading
    it back is that read left to right: flip the axis the galactic Y runs along, then undo
    the transposition - see combine-volumes._from_file_order().
    """
    if grid.ndim == 4:
        # (x, y, z, channels) -> keep the channel axis last.
        return np.ascontiguousarray(grid.transpose(0, 2, 1, 3)[:, :, ::-1, :])
    return np.ascontiguousarray(grid.transpose(0, 2, 1)[:, :, ::-1])

def _write_bytes(data: bytes, output_path: str, compresslevel: int = 6):
    """Write raw voxel bytes to output_path, gzip-compressed (.raw.gz)."""
    with gzip.open(output_path, 'wb', compresslevel=compresslevel) as f:
        f.write(data)

def _normalize_density(density_grid: np.ndarray) -> np.ndarray:
    """Scale density to [0, 1] by its own maximum."""
    peak = density_grid.max()
    return density_grid / peak if peak > 0 else density_grid

# Percentile of the occupied-voxel colour used to derive the gain, per mode.
COLOR_NORMALIZE_PERCENTILES = {"max": 100.0, "p99": 99.0, "p999": 99.9}

def normalize_color(color_grid: np.ndarray, density_grid: np.ndarray,
                    mode: str = "p999") -> tuple[np.ndarray, float]:
    """Scale the colour grid so the stored 8-bit values use their range.

    The source particle colours are emission coefficients, not display colours:
    Gas averages ~0.02, Bulge ~0.02, so `clip(c, 0, 1) * 255` rounds ~82% of
    occupied Gas voxels to 0 and the whole channel is carried by 32 distinct
    levels. This restores the information before quantisation.

    A *single scalar* gain is applied to all three channels, which preserves the
    chromaticity (hue) of every voxel. Per-channel normalisation would give each
    channel the full range and thereby wash the hues towards grey - exactly the
    information a per-voxel colour texture exists to carry.

    The gain is derived from the occupied voxels only (density > 0); zero-density
    voxels are never visible, and for channels without colour data the constant
    fill would otherwise not represent anything.

    Returns the scaled grid and the gain that was applied (1.0 = no change).
    """
    if mode == "none":
        return color_grid, 1.0

    occupied = density_grid > 0
    if not np.any(occupied):
        return color_grid, 1.0

    percentile = COLOR_NORMALIZE_PERCENTILES[mode]
    peak = float(np.percentile(color_grid[occupied], percentile))
    if peak <= 0:
        return color_grid, 1.0

    gain = 1.0 / peak
    return color_grid * gain, gain

def create_volume_texture(density_grid: np.ndarray, output_path: str):
    """Save 3D texture as gzipped raw binary for OpenGL"""
    density_grid = _normalize_density(density_grid)
    
    density_8bit = _to_file_order((density_grid * 255).astype(np.uint8))
    
    _write_bytes(density_8bit.tobytes(), output_path)
    
    print(f"Saved density texture: {output_path} ({density_8bit.nbytes / 1024 / 1024:.2f} MB raw)")

def create_color_texture(color_grid: np.ndarray, output_path: str):
    """Save 3D color texture as gzipped raw RGB"""
    color_8bit = _to_file_order((np.clip(color_grid, 0, 1) * 255).astype(np.uint8))
    
    _write_bytes(color_8bit.tobytes(), output_path)
    
    print(f"Saved color texture: {output_path} ({color_8bit.nbytes / 1024 / 1024:.2f} MB raw)")

def create_rgba_texture(density_grid: np.ndarray, color_grid: np.ndarray, output_path: str):
    """Save density and colour packed into one RGBA texture.

    RGB carries the (already normalised) voxel colour, A carries the density. The
    loader distinguishes the format from the file size - 4*n^3 bytes - so the
    alpha channel here must be byte-identical to what create_volume_texture()
    writes for the same channel, otherwise the two files would disagree about
    the density. Both go through _normalize_density() with the same maximum.
    """
    density_grid = _normalize_density(density_grid)

    rgba = np.empty((*color_grid.shape[:3], 4), dtype=np.uint8)
    rgba[..., :3] = (np.clip(color_grid, 0, 1) * 255).astype(np.uint8)
    rgba[..., 3] = (density_grid * 255).astype(np.uint8)

    rgba_8bit = _to_file_order(rgba)
    _write_bytes(rgba_8bit.tobytes(), output_path)

    print(f"Saved RGBA texture: {output_path} ({rgba_8bit.nbytes / 1024 / 1024:.2f} MB raw)")

def _format_axes(values) -> str:
    """Render a per-axis quantity as '400x40x400'."""
    return "x".join(str(int(v)) for v in values)

def _format_vector(values) -> str:
    """Render a vector the way the samples and the combiner write theirs."""
    return "[" + ", ".join(f"{float(v):.3f}" for v in values) + "]"

def save_metadata(channel: ParticleChannel, config: VolumeConfig, output_dir: str, basename: str,
                  color_gain: float = 1.0, color_normalize: str = "none", formats=frozenset()):
    """Save metadata file for the volume texture.

    The grid and the box are written in *storage* order (W, H, D), because that is the
    order the file is indexed in and the shader turns a position into texture coordinates
    per axis - the first coordinate indexes storage axis 0. Both were cubic while the box
    was forced to a cube, so the two orders coincided and nothing had to say which was
    meant. The galactic order is written on its own line as well, so a reader never has to
    permute anything by hand.
    """
    metadata_path = os.path.join(output_dir, f"{basename}_metadata.txt")
    data_min, data_max = config.world_bounds
    box_min, box_max = config.stored_bounds()
    grid = config.stored_grid()
    raw_min, raw_max = config.raw_data_bounds
    
    with open(metadata_path, 'w') as f:
        f.write("# Grid, world size, bounds and voxel size are in storage order (W, H, D)\n")
        f.write("# = galactic (X, Z, -Y), the order the file is indexed in; see\n")
        f.write("# particles-to-3dtex._to_file_order() for why the frame is rotated.\n")
        f.write(f"Channel: {channel.name}\n")
        f.write(f"Grid size: {_format_axes(grid)}\n")
        f.write(f"Galactic grid (x, y, z): {_format_axes(config.grid)}\n")
        f.write(f"World size: {_format_vector(config.world_size[_GALACTIC_TO_STORAGE])}\n")
        f.write(f"Data bounds: {_format_vector(data_min)} to {_format_vector(data_max)}\n")
        f.write(f"Volume bounds: {_format_vector(box_min)} to {_format_vector(box_max)}\n")
        f.write(f"Voxel size: {_format_vector(config.voxel_size[_GALACTIC_TO_STORAGE])}\n")
        # Everything above is in normalized units, so a reader can no longer size the volume
        # from the metadata alone - the object's size in the scene does that. The factor and
        # the source bounds are recorded so the link to the data is not lost.
        f.write(f"Normalization: {config.normalization:.6f} (stored units = source units "
                f"times this, so the largest box side is {VolumeConfig.NORMALIZED_SIDE:g}; "
                f"source data bounds {_format_vector(raw_min)} to {_format_vector(raw_max)})\n")
        f.write(f"Has color: {channel.has_color}\n")
        f.write(f"Original particle count: {channel.particle_count}\n")
        # Colour quantisation info. The gain is already applied to the stored
        # bytes, so nothing needs to be undone at load time; it is recorded so
        # the colour files stay reproducible and comparable across runs.
        f.write(f"Formats: {', '.join(sorted(formats)) if formats else 'none'}\n")
        f.write(f"Color gain: {color_gain:.6f} (mode: {color_normalize})\n")
    
    print(f"Saved metadata: {metadata_path}")

def convert_particles_to_volumes(input_dir: str, output_dir: str, grid, world_bounds: tuple[np.ndarray, np.ndarray], method: str = "ultrafast", formats: frozenset = ALL_FORMATS, color_normalize: str = "p999", group: dict = None, group_norm: str = "max", voxel_size: float = None, world_size: float = None, normalize: bool = True):
    """Main conversion function.

    `grid` is one resolution or three, in storage order (W, H, D); see parse_grid_size().
    `voxel_size` wins over it when given: the grid is then derived per axis from the box,
    which is what keeps the voxel size of an N^3 grid while flattening only the axes where
    the data has no extent. It is in the normalized units the box is in, so the box being
    a side of 1, `--voxel-size 1/400` asks for 400 voxels on the largest axis.

    `world_bounds` is in the source units; the box built from it is normalized unless
    `normalize` is False, and the particles are converted to match. See VolumeConfig.
    """

    os.makedirs(output_dir, exist_ok=True)
    # --voxel-size derives the grid from the box, so the box has to be shaped like the data
    # for the answer to mean anything: a cubic box would ask for just as many voxels on an
    # axis that is mostly empty, which is the waste being avoided.
    config = VolumeConfig(grid=grid, world_bounds=world_bounds,
                          cube=False if voxel_size else None,
                          world_size=world_size if world_size is not None else 1.0,
                          normalize=normalize)
    if voxel_size:
        galactic_grid = [max(1, int(round(span / voxel_size))) for span in config.world_size]
        config.set_grid(galactic_to_storage(galactic_grid))
    
    particle_files = [f for f in os.listdir(input_dir) if f.endswith('.dat.gz') and not must_skip(f)]
    
    if not particle_files:
        print(f"No .dat.gz files found in {input_dir}")
        return
    
    print(f"Found {len(particle_files)} particle files to process")
    print(f"Grid: {_format_axes(config.stored_grid())} (storage, WxHxD) = "
          f"{_format_axes(config.grid)} (galactic, x, y, z)")
    print(f"Box and voxel below are galactic: {_format_vector(config.box_min)} to "
          f"{_format_vector(config.box_max)}, voxel {_format_vector(config.voxel_size)}")
    print(f"Box normalized by {config.normalization:.6f}, so its largest side is "
          f"{VolumeConfig.NORMALIZED_SIDE:g} in units of the object's size (particles "
          f"scaled to match); source data bounds "
          f"{_format_vector(config.raw_data_bounds[0])} to {_format_vector(config.raw_data_bounds[1])}")
    if not np.allclose(config.voxel_size, config.voxel_size[0], rtol=0.1):
        print("Warning: the grid is not proportional to the box, so the voxels are "
              "anisotropic - the same memory, but banded along the stretched axis. A "
              "grid with roughly span_proportional axes gives cubic voxels.")
    print(f"Using method: {method}")
    print(f"Writing formats: {'|'.join(sorted(formats))} (colour normalization: {color_normalize})")
    
    # Basenames of the channels actually written as RGBA, for --group to merge. A
    # channel that yields no particles is skipped, so it must not be offered.
    rgba_channels = []
    
    for particle_file in particle_files:
        print(f"\n{'='*50}")
        print(f"Processing {particle_file}...")
        
        filepath = os.path.join(input_dir, particle_file)
        channel = parse_particle_file(filepath, world_bounds, config.normalization)
        
        if channel.particle_count == 0:
            print(f"Warning: No particles found in {particle_file}, skipping")
            continue
        
        # Choose conversion method
        if method == "smart":
            density_grid, color_grid = particles_to_volume_smart(channel, config)
        else:  # ultrafast
            density_grid, color_grid = particles_to_volume_ultrafast(channel, config)
        
        # Apply channel-specific processing
        density_grid, color_grid = process_channel_specific(channel, density_grid, color_grid)
        
        # Stretch the (very dark) source colours over the 8-bit range. Done once,
        # before writing, so the colour and RGBA files agree byte for byte.
        color_grid, color_gain = normalize_color(color_grid, density_grid, color_normalize)
        
        # Generate output filenames
        basename = os.path.splitext(os.path.splitext(particle_file)[0])[0]
        
        # Save exactly the artifacts requested by --format. Nothing is written
        # implicitly: '--format color' produces the colour texture only, with no
        # density file alongside it.
        if "density" in formats:
            density_path = os.path.join(output_dir, f"{basename}_density.raw.gz")
            create_volume_texture(density_grid, density_path)
        
        if "color" in formats:
            color_path = os.path.join(output_dir, f"{basename}_color.raw.gz")
            create_color_texture(color_grid, color_path)
        
        if "rgba" in formats:
            rgba_path = os.path.join(output_dir, f"{basename}_rgba.raw.gz")
            create_rgba_texture(density_grid, color_grid, rgba_path)
            rgba_channels.append(basename)
        
        # Save metadata
        save_metadata(channel, config, output_dir, basename, color_gain, color_normalize, formats)
        
        print(f"Completed processing {particle_file}")
    
    if group is not None:
        return run_group_combiner(group, rgba_channels, output_dir, color_normalize, group_norm)
    return 0

def main():
    parser = argparse.ArgumentParser(
        description="Convert Gaia Sky particle channel files to 3D volume textures",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )
    
    parser.add_argument(
        "input_dir",
        help="Input directory containing .dat.gz particle files"
    )
    
    parser.add_argument(
        "output_dir", 
        help="Output directory for volume textures"
    )
    
    parser.add_argument(
        "--grid-size",
        type=parse_grid_size,
        default=None,
        metavar="N|WxHxD",
        help="Grid resolution: one number for a cubic grid, or three as WxHxD. The three "
             "are in storage order (the order of the file, and of the metadata's 'Grid "
             "size:'), i.e. galactic (X, Z, -Y) - a flat disk wants '400x40x400', a 400 "
             "wide, 40 tall and 400 deep texture. A per-axis grid also makes the box "
             "follow the data instead of being forced to a cube. Default: 128."
    )
    
    parser.add_argument(
        "--voxel-size",
        type=float,
        default=None,
        metavar="SIZE",
        help="Size of one voxel, as an alternative to --grid-size: the grid is derived per "
             "axis from the box. The box is normalized, its largest side being 1, so "
             "--voxel-size 1/400 asks for 400 voxels along the largest axis. Use it to keep "
             "the voxel size of an N^3 grid while flattening the axes where the data has no "
             "extent."
    )
    
    parser.add_argument(
        "--fixed-world-size",
        type=float,
        help="Use a fixed box instead of computing one from the data (for testing). Only its "
             "shape survives: the box is normalized to a side of 1."
    )

    parser.add_argument(
        "--no-normalize",
        action="store_true",
        help="Keep the box in the source units instead of normalizing its largest side to 1. "
             "The metadata bounds then act as a size multiplier on the object - a box of side "
             "7 draws the volume 7 times the size in the JSON - which is what the generator "
             "did before, and what makes the volumes it wrote incomparable with new ones."
    )
    
    parser.add_argument(
        "--method",
        choices=["ultrafast", "smart"],
        default="ultrafast",
        help="Conversion method: 'ultrafast' (nearest voxel) or 'smart' (3x3x3 kernel)"
    )
    
    parser.add_argument(
        "--format",
        type=parse_format,
        default=set("rgba"),
        metavar="FMT[|FMT...]",
        help="Artifacts to write, as a '|'- or ','-separated set of "
             "{density, color, rgba}, or 'all'. 'density' writes the R8 density "
             "texture only; 'color' writes the RGB colour texture only; 'rgba' "
             "packs colour (RGB) and density (A) into one texture. Nothing is "
             "written implicitly, so '--format color' produces no density file."
    )
    
    parser.add_argument(
        "--color-normalize",
        choices=["none", "max", "p99", "p999"],
        default="p999",
        help="Scale the colour grid before 8-bit quantisation, using a single "
             "scalar gain (hue preserving) taken from the occupied voxels"
    )
    
    parser.add_argument(
        "--skip-existing",
        action="store_true",
        help="Skip processing if output files already exist"
    )
    
    parser.add_argument(
        "--group",
        type=parse_group,
        metavar="EMISSIVE|OCCLUSIVE",
        help="After converting, merge the channels into one emissive and one occlusive "
             "texture by calling combine-volumes.py. The spec is "
             "'<emissive>|<occlusive>', each side a '+'-separated list of channel names "
             "with an optional ':weight', e.g. 'Gas+Bulge:0.6|ArmDust'. The merged files "
             "are written next to the per-channel ones, named after their common prefix. "
             "Requires 'rgba' in --format."
    )
    
    parser.add_argument(
        "--group-norm",
        choices=["max", "none"],
        default="max",
        help="Density normalisation for the textures written by --group"
    )
    
    args = parser.parse_args()

    if args.voxel_size is not None and args.grid_size is not None:
        parser.error("--grid-size and --voxel-size are alternatives; pass one")
    grid = args.grid_size if args.grid_size is not None else 128

    if args.group is not None and "rgba" not in args.format:
        print("Error: --group merges the RGBA textures, so 'rgba' must be in --format")
        return 1

    if args.group is not None and not args.group["emissive"] and not args.group["occlusive"]:
        print("Error: --group names no channels")
        return 1
    
    if not os.path.exists(args.input_dir):
        print(f"Error: Input directory '{args.input_dir}' does not exist")
        return 1
    
    if os.path.exists(args.output_dir) and os.listdir(args.output_dir) and not args.skip_existing:
        print(f"Warning: Output directory '{args.output_dir}' is not empty")
        response = input("Continue? (y/n): ")
        if response.lower() != 'y':
            return 1
    
    print("Gaia Sky Particle to Volume Converter")
    print(f"Input: {args.input_dir}")
    print(f"Output: {args.output_dir}")
    if args.voxel_size is not None:
        print(f"Voxel size: {args.voxel_size} (the grid follows the box)")
    else:
        print(f"Grid size: {grid if isinstance(grid, int) else _format_axes(grid)}"
              + ("" if isinstance(grid, int) else " (storage, WxHxD)"))
    
    try:
        # Compute world bounds from all files
        if args.fixed_world_size:
            world_bounds = None
            print(f"Using fixed world size: {args.fixed_world_size}")
        else:
            world_bounds = compute_world_bounds(args.input_dir)
        
        status = convert_particles_to_volumes(
            input_dir=args.input_dir,
            output_dir=args.output_dir,
            grid=grid,
            world_bounds=world_bounds,
            method=args.method,
            formats=args.format,
            color_normalize=args.color_normalize,
            group=args.group,
            group_norm=args.group_norm,
            voxel_size=args.voxel_size,
            world_size=args.fixed_world_size,
            normalize=not args.no_normalize
        )
        if status:
            print("\nThe conversion succeeded but the group merge failed; see above.")
            return status
        print("\nConversion completed successfully!")
        return 0
    except Exception as e:
        print(f"\nError during conversion: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == "__main__":
    exit(main())
