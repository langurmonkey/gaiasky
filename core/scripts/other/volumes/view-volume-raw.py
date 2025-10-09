#!/usr/bin/env python3
"""
Volume Renderer for Gaia Sky .raw files using Vedo
Supports both density and color volumes
"""

import numpy as np
import argparse
import sys
import os

def load_volume_file(filename, grid_size=128):
    """Load a .raw volume file and return as 3D array"""
    try:
        with open(filename, 'rb') as f:
            data = np.fromfile(f, dtype=np.uint8)
        
        # Check if it's a color file (3 channels) or density file (1 channel)
        expected_size_density = grid_size ** 3
        expected_size_color = grid_size ** 3 * 3
        
        if len(data) == expected_size_density:
            # Density file - single channel
            volume = data.reshape((grid_size, grid_size, grid_size))
            print(f"Loaded density volume: {volume.shape}")
            return volume, 'density'
        elif len(data) == expected_size_color:
            # Color file - RGB channels
            volume = data.reshape((grid_size, grid_size, grid_size, 3))
            print(f"Loaded color volume: {volume.shape}")
            return volume, 'color'
        else:
            print(f"Warning: File size {len(data)} doesn't match expected size for grid {grid_size}")
            print(f"Expected {expected_size_density} for density or {expected_size_color} for color")
            # Try to auto-detect
            if len(data) % 3 == 0:
                # Might be color data
                actual_grid = int(round((len(data) // 3) ** (1/3)))
                if actual_grid ** 3 * 3 == len(data):
                    volume = data.reshape((actual_grid, actual_grid, actual_grid, 3))
                    print(f"Auto-detected color volume with grid size: {actual_grid}")
                    return volume, 'color'
            # Try as density
            actual_grid = int(round(len(data) ** (1/3)))
            if actual_grid ** 3 == len(data):
                volume = data.reshape((actual_grid, actual_grid, actual_grid))
                print(f"Auto-detected density volume with grid size: {actual_grid}")
                return volume, 'density'
            else:
                raise ValueError(f"Cannot determine volume format for file size {len(data)}")
                
    except Exception as e:
        print(f"Error loading {filename}: {e}")
        raise

def volume_render_vedo(raw_file, grid_size=128, color_file=None):
    """Volume rendering with Vedo supporting both density and color volumes"""
    try:
        from vedo import Volume, show
        import vedo
        
        print(f"Loading with Vedo: {raw_file}")
        
        # Load main volume file
        volume, volume_type = load_volume_file(raw_file, grid_size)
        
        # Load color file if provided
        color_volume = None
        if color_file and os.path.exists(color_file):
            color_volume, color_type = load_volume_file(color_file, grid_size)
            if color_type != 'color':
                print(f"Warning: {color_file} is not a color file (expected 3 channels)")
                color_volume = None
        
        # Create volume object
        if volume_type == 'density':
            # Density volume
            if color_volume is not None:
                # We have separate color data
                print("Creating volume with external color data...")
                
                # Convert to float for better processing
                density_float = volume.astype(np.float32) / 255.0
                
                # Create volume with density data
                vol = Volume(density_float)
                
                # Apply color mapping based on external color data
                # We'll use the average color for each voxel
                color_avg = np.mean(color_volume, axis=3) / 255.0
                vol.cmap('jet', color_avg)  # Use color data for colormap
                
            else:
                # Standard density volume with colormap
                print("Creating density volume with hot colormap...")
                vol = Volume(volume)
                vol.cmap('hot')  # Apply hot colormap
                
        elif volume_type == 'color':
            # Color volume - use luminance as density and colors from RGB
            print("Creating volume from RGB color data...")
            
            # Calculate luminance from RGB for density
            luminance = np.mean(volume, axis=3).astype(np.float32) / 255.0
            
            # Create volume with luminance as density
            vol = Volume(luminance)
            
            # Use the original RGB colors for mapping
            # Convert RGB to single value for colormap indexing
            color_index = np.mean(volume, axis=3)  # Use average as index
            vol.cmap('hot', color_index)  # Apply colormap based on color data
        
        # Set volume properties for better rendering
        vol.alpha([0, 0.0, 0.3, 0.6, 1.0])  # Opacity transfer function
        vol.alpha_unit(0.5)  # Adjust opacity
        
        print(f"Volume data range: {volume.min()} to {volume.max()}")
        if volume_type == 'density':
            print(f"Non-zero voxels: {np.sum(volume > 0)} / {volume.size}")
        
        print("Starting Vedo volume renderer...")
        print("Controls:")
        print("  - Left mouse: Rotate")
        print("  - Middle mouse: Pan")
        print("  - Mouse wheel: Zoom")
        print("  - 'a': Toggle axes")
        print("  - 'r': Reset camera")
        print("  - 'q': Quit")
        
        show(vol, axes=1, bg='black', title=os.path.basename(raw_file))
        
    except ImportError:
        print("Vedo not available. Install with: pip install vedo")
        return False
    except Exception as e:
        print(f"Vedo error: {e}")
        import traceback
        traceback.print_exc()
        return False
    return True

def find_matching_color_file(density_file):
    """Find the corresponding color file for a density file"""
    base_name = density_file.replace('_density.raw', '')
    color_file = base_name + '_color.raw'
    
    if os.path.exists(color_file):
        return color_file
    else:
        # Try other naming patterns
        alternatives = [
            density_file.replace('density', 'color'),
            density_file.replace('_density', '_color'),
        ]
        for alt in alternatives:
            if os.path.exists(alt):
                return alt
    return None

def main():
    parser = argparse.ArgumentParser(description='Volume renderer for Gaia Sky .raw files')
    parser.add_argument('raw_file', help='Path to the .raw volume file (density or color)')
    parser.add_argument('--grid-size', type=int, default=128, 
                       help='Grid size (default: 128)')
    parser.add_argument('--color-file', help='Path to color volume file (optional)')
    parser.add_argument('--auto-color', action='store_true',
                       help='Automatically find matching color file')
    
    args = parser.parse_args()
    
    # Check if file exists
    if not os.path.exists(args.raw_file):
        print(f"Error: File '{args.raw_file}' does not exist")
        sys.exit(1)
    
    # Handle color file
    color_file = args.color_file
    if args.auto_color and not color_file:
        color_file = find_matching_color_file(args.raw_file)
        if color_file:
            print(f"Auto-found color file: {color_file}")
        else:
            print("No matching color file found")
    
    if color_file and not os.path.exists(color_file):
        print(f"Warning: Color file '{color_file}' not found")
        color_file = None
    
    success = volume_render_vedo(args.raw_file, args.grid_size, color_file)
    
    if not success:
        sys.exit(1)

if __name__ == "__main__":
    main()
