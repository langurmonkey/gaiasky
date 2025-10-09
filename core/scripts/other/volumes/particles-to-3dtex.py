#!/usr/bin/env python3
"""
Convert Gaia Sky particle channel files (.dat.gz) to 3D volume textures
"""

import argparse
import gzip
import os
import numpy as np
from dataclasses import dataclass
from typing import List, Tuple
from scipy.ndimage import gaussian_filter
import time
from tqdm import tqdm

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

class VolumeConfig:
    def __init__(self, grid_size=256, world_size=30.0, world_bounds=None):
        self.grid_size = grid_size
        if world_bounds is not None:
            # Calculate world size from bounds with some padding
            min_bound, max_bound = world_bounds
            self.world_bounds = (min_bound, max_bound)
            # Use the maximum dimension for a cubic volume
            dimensions = max_bound - min_bound
            self.world_size = np.max(dimensions) * 1.1  # 10% padding
            self.world_center = (min_bound + max_bound) / 2
        else:
            self.world_size = world_size
            self.world_center = np.zeros(3)
            self.world_bounds = (np.array([-world_size/2, -world_size/2, -world_size/2]), 
                               np.array([world_size/2, world_size/2, world_size/2]))
        
        self.voxel_size = self.world_size / grid_size

def compute_world_bounds(input_dir: str) -> Tuple[np.ndarray, np.ndarray]:
    """Compute bounding box from all particle files"""
    print("Computing world bounds from all particle files...")
    
    particle_files = [f for f in os.listdir(input_dir) if f.endswith('.dat.gz')]
    
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
    
    # Add 10% padding to ensure all particles are inside
    padding = (all_max - all_min) * 0.05
    all_min -= padding
    all_max += padding
    
    print(f"With 5% padding:")
    print(f"  X: [{all_min[0]:.3f}, {all_max[0]:.3f}]")
    print(f"  Y: [{all_min[1]:.3f}, {all_max[1]:.3f}]")
    print(f"  Z: [{all_min[2]:.3f}, {all_max[2]:.3f}]")
    
    return all_min, all_max

def parse_particle_file(filename: str, world_bounds: Tuple[np.ndarray, np.ndarray] = None) -> ParticleChannel:
    """Parse gzipped particle files - handles both with and without headers"""
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

def particles_to_volume_ultrafast(channel: ParticleChannel, config: VolumeConfig) -> Tuple[np.ndarray, np.ndarray]:
    """Conversion using direct voxel assignment"""
    
    print(f"Converting {channel.particle_count} particles to volume grid...")
    start_time = time.time()
    
    density_grid = np.zeros((config.grid_size, config.grid_size, config.grid_size), dtype=np.float32)
    color_grid = np.zeros((config.grid_size, config.grid_size, config.grid_size, 3), dtype=np.float32)
    
    if channel.particle_count == 0:
        return density_grid, color_grid
    
    # Convert all particles to grid coordinates
    positions = np.array([[p.x, p.y, p.z] for p in channel.particles])
    sizes = np.array([p.size for p in channel.particles])
    
    if channel.has_color:
        colors = np.array([[p.r, p.g, p.b] for p in channel.particles])
    else:
        colors = np.ones((len(channel.particles), 3))
    
    # Convert to grid coordinates using computed bounds
    min_bound, max_bound = config.world_bounds
    # Center the volume around the data
    volume_min = config.world_center - config.world_size / 2
    volume_max = config.world_center + config.world_size / 2
    
    grid_coords_float = (positions - volume_min) / (volume_max - volume_min) * config.grid_size
    grid_coords = grid_coords_float.astype(int)
    
    # Filter valid particles
    valid_mask = (
        (grid_coords[:, 0] >= 0) & (grid_coords[:, 0] < config.grid_size) &
        (grid_coords[:, 1] >= 0) & (grid_coords[:, 1] < config.grid_size) &
        (grid_coords[:, 2] >= 0) & (grid_coords[:, 2] < config.grid_size)
    )
    
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
            for c in range(3):
                color_grid[x, y, z, c] += colors[i, c] * weights[i] * sizes[i]
    
    # Normalize color grid
    if channel.has_color:
        nonzero_mask = density_grid > 0
        for c in range(3):
            color_grid[nonzero_mask, c] /= density_grid[nonzero_mask]
        # Fill in color for zero-density regions with default
        zero_mask = density_grid == 0
        for c in range(3):
            color_grid[zero_mask, c] = 0.5  # Default gray
    
    elapsed = time.time() - start_time
    print(f"Volume conversion completed in {elapsed:.2f} seconds")
    print(f"Volume density range: {density_grid.min():.6f} to {density_grid.max():.6f}")
    print(f"Non-zero voxels: {np.sum(density_grid > 0)} / {config.grid_size ** 3}")
    
    return density_grid, color_grid

def particles_to_volume_smart(channel: ParticleChannel, config: VolumeConfig) -> Tuple[np.ndarray, np.ndarray]:
    """SMART conversion with adaptive sampling - good balance of speed and quality"""
    
    print(f"Converting {channel.particle_count} particles to volume grid (smart sampling)...")
    start_time = time.time()
    
    density_grid = np.zeros((config.grid_size, config.grid_size, config.grid_size), dtype=np.float32)
    color_grid = np.zeros((config.grid_size, config.grid_size, config.grid_size, 3), dtype=np.float32)
    
    if channel.particle_count == 0:
        return density_grid, color_grid
    
    # Extract particle data
    positions = np.array([[p.x, p.y, p.z] for p in channel.particles])
    sizes = np.array([p.size for p in channel.particles])
    
    if channel.has_color:
        colors = np.array([[p.r, p.g, p.b] for p in channel.particles])
    else:
        colors = np.ones((len(channel.particles), 3))
    
    # Convert to grid coordinates using computed bounds
    min_bound, max_bound = config.world_bounds
    # Center the volume around the data
    volume_min = config.world_center - config.world_size / 2
    volume_max = config.world_center + config.world_size / 2
    
    grid_coords_float = (positions - volume_min) / (volume_max - volume_min) * config.grid_size
    grid_coords = grid_coords_float.astype(int)
    
    # Filter valid particles
    valid_mask = (
        (grid_coords[:, 0] >= 0) & (grid_coords[:, 0] < config.grid_size) &
        (grid_coords[:, 1] >= 0) & (grid_coords[:, 1] < config.grid_size) &
        (grid_coords[:, 2] >= 0) & (grid_coords[:, 2] < config.grid_size)
    )
    
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
        size_factor = min(2.0, sizes[i] / config.voxel_size)  # Limit influence
        
        # Small kernel around particle
        for dx in range(-kernel_radius, kernel_radius + 1):
            for dy in range(-kernel_radius, kernel_radius + 1):
                for dz in range(-kernel_radius, kernel_radius + 1):
                    x = cx + dx
                    y = cy + dy
                    z = cz + dz
                    
                    if (0 <= x < config.grid_size and 
                        0 <= y < config.grid_size and 
                        0 <= z < config.grid_size):
                        
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

def process_channel_specific(channel: ParticleChannel, density_grid: np.ndarray, color_grid: np.ndarray) -> Tuple[np.ndarray, np.ndarray]:
    """Apply channel-specific processing"""
    channel_name = channel.name.lower()
    
    # Apply Gaussian filtering based on channel type
    if 'dust' in channel_name:
        density_grid = gaussian_filter(density_grid, sigma=2.0)
        if not channel.has_color:
            color_grid[:,:,:,0] = 0.8; color_grid[:,:,:,1] = 0.6; color_grid[:,:,:,2] = 0.4
    elif 'gas' in channel_name:
        density_grid = gaussian_filter(density_grid, sigma=1.5)
        if not channel.has_color:
            color_grid[:,:,:,0] = 0.3; color_grid[:,:,:,1] = 0.5; color_grid[:,:,:,2] = 0.8
    elif 'hii' in channel_name:
        density_grid = gaussian_filter(density_grid, sigma=1.2)
        if not channel.has_color:
            color_grid[:,:,:,0] = 1.0; color_grid[:,:,:,1] = 0.4; color_grid[:,:,:,2] = 0.6
    elif 'bulge' in channel_name:
        density_grid = gaussian_filter(density_grid, sigma=1.0)
        if not channel.has_color:
            color_grid[:,:,:,0] = 1.0; color_grid[:,:,:,1] = 0.9; color_grid[:,:,:,2] = 0.6
    elif 'star' in channel_name:
        density_grid = gaussian_filter(density_grid, sigma=0.8)
        if not channel.has_color:
            color_grid[:,:,:,0] = 1.0; color_grid[:,:,:,1] = 0.95; color_grid[:,:,:,2] = 0.9
    
    print(f"Applied {channel_name} channel processing")
    return density_grid, color_grid

def create_volume_texture(density_grid: np.ndarray, output_path: str):
    """Save 3D texture as raw binary for OpenGL"""
    if density_grid.max() > 0:
        density_grid = density_grid / density_grid.max()
    
    density_8bit = (density_grid * 255).astype(np.uint8)
    
    with open(output_path, 'wb') as f:
        f.write(density_8bit.tobytes())
    
    print(f"Saved density texture: {output_path} ({density_8bit.nbytes / 1024 / 1024:.2f} MB)")

def create_color_texture(color_grid: np.ndarray, output_path: str):
    """Save 3D color texture as raw RGB"""
    color_8bit = (np.clip(color_grid, 0, 1) * 255).astype(np.uint8)
    
    with open(output_path, 'wb') as f:
        f.write(color_8bit.tobytes())
    
    print(f"Saved color texture: {output_path} ({color_8bit.nbytes / 1024 / 1024:.2f} MB)")

def save_metadata(channel: ParticleChannel, config: VolumeConfig, output_dir: str, basename: str):
    """Save metadata file for the volume texture"""
    metadata_path = os.path.join(output_dir, f"{basename}_metadata.txt")
    min_bound, max_bound = config.world_bounds
    volume_min = config.world_center - config.world_size / 2
    volume_max = config.world_center + config.world_size / 2
    
    with open(metadata_path, 'w') as f:
        f.write(f"Channel: {channel.name}\n")
        f.write(f"Grid size: {config.grid_size}\n")
        f.write(f"World size: {config.world_size:.3f}\n")
        f.write(f"Data bounds: [{min_bound[0]:.3f}, {min_bound[1]:.3f}, {min_bound[2]:.3f}] to [{max_bound[0]:.3f}, {max_bound[1]:.3f}, {max_bound[2]:.3f}]\n")
        f.write(f"Volume bounds: [{volume_min[0]:.3f}, {volume_min[1]:.3f}, {volume_min[2]:.3f}] to [{volume_max[0]:.3f}, {volume_max[1]:.3f}, {volume_max[2]:.3f}]\n")
        f.write(f"Voxel size: {config.voxel_size:.6f}\n")
        f.write(f"Has color: {channel.has_color}\n")
        f.write(f"Original particle count: {channel.particle_count}\n")
    
    print(f"Saved metadata: {metadata_path}")

def convert_particles_to_volumes(input_dir: str, output_dir: str, grid_size: int, world_bounds: Tuple[np.ndarray, np.ndarray], method: str = "ultrafast"):
    """Main conversion function"""
    
    os.makedirs(output_dir, exist_ok=True)
    config = VolumeConfig(grid_size=grid_size, world_bounds=world_bounds)
    
    particle_files = [f for f in os.listdir(input_dir) if f.endswith('.dat.gz')]
    
    if not particle_files:
        print(f"No .dat.gz files found in {input_dir}")
        return
    
    print(f"Found {len(particle_files)} particle files to process")
    print(f"Volume configuration: {grid_size}³ grid, world size {config.world_size:.3f}")
    print(f"Using method: {method}")
    
    for particle_file in particle_files:
        print(f"\n{'='*50}")
        print(f"Processing {particle_file}...")
        
        filepath = os.path.join(input_dir, particle_file)
        channel = parse_particle_file(filepath, world_bounds)
        
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
        
        # Generate output filenames
        basename = os.path.splitext(os.path.splitext(particle_file)[0])[0]
        
        # Save volume textures
        density_path = os.path.join(output_dir, f"{basename}_density.raw")
        color_path = os.path.join(output_dir, f"{basename}_color.raw")
        
        create_volume_texture(density_grid, density_path)
        create_color_texture(color_grid, color_path)
        
        # Save metadata
        save_metadata(channel, config, output_dir, basename)
        
        print(f"Completed processing {particle_file}")

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
        type=int,
        default=128,  # Reduced default for speed
        help="Resolution of the 3D volume grid (cubed)"
    )
    
    parser.add_argument(
        "--fixed-world-size",
        type=float,
        help="Use fixed world size instead of computing from data (for testing)"
    )
    
    parser.add_argument(
        "--method",
        choices=["ultrafast", "smart"],
        default="ultrafast",
        help="Conversion method: 'ultrafast' (nearest voxel) or 'smart' (3x3x3 kernel)"
    )
    
    parser.add_argument(
        "--skip-existing",
        action="store_true",
        help="Skip processing if output files already exist"
    )
    
    args = parser.parse_args()
    
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
    print(f"Grid size: {args.grid_size}")
    
    try:
        # Compute world bounds from all files
        if args.fixed_world_size:
            world_bounds = None
            print(f"Using fixed world size: {args.fixed_world_size}")
        else:
            world_bounds = compute_world_bounds(args.input_dir)
        
        convert_particles_to_volumes(
            input_dir=args.input_dir,
            output_dir=args.output_dir,
            grid_size=args.grid_size,
            world_bounds=world_bounds,
            method=args.method
        )
        print("\nConversion completed successfully!")
        return 0
    except Exception as e:
        print(f"\nError during conversion: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == "__main__":
    exit(main())
