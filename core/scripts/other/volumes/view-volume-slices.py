#!/usr/bin/env python3
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.widgets import Slider
import sys

def interactive_volume_viewer(filename, grid_size=128):
    """Interactive 3D volume viewer with slice scrolling"""
    
    # Read the raw file
    with open(filename, 'rb') as f:
        data = np.fromfile(f, dtype=np.uint8)
    
    # Reshape to 3D
    volume = data.reshape((grid_size, grid_size, grid_size))
    
    # Create figure and subplots
    fig, (ax1, ax2, ax3) = plt.subplots(1, 3, figsize=(15, 5))
    plt.subplots_adjust(bottom=0.25)
    
    # Initial slices
    z_slice = volume[:, :, grid_size//2]
    y_slice = volume[:, grid_size//2, :]
    x_slice = volume[grid_size//2, :, :]
    
    # Plot initial slices
    im1 = ax1.imshow(z_slice, cmap='hot', origin='lower')
    ax1.set_title('XY Slice')
    ax1.set_xlabel('X')
    ax1.set_ylabel('Y')
    
    im2 = ax2.imshow(y_slice, cmap='hot', origin='lower')
    ax2.set_title('XZ Slice')
    ax2.set_xlabel('Z')
    ax2.set_ylabel('X')
    
    im3 = ax3.imshow(x_slice, cmap='hot', origin='lower')
    ax3.set_title('YZ Slice')
    ax3.set_xlabel('Z')
    ax3.set_ylabel('Y')
    
    # Add sliders
    ax_slider_z = plt.axes([0.25, 0.1, 0.5, 0.03])
    ax_slider_y = plt.axes([0.25, 0.05, 0.5, 0.03])
    ax_slider_x = plt.axes([0.25, 0.15, 0.5, 0.03])
    
    slider_z = Slider(ax_slider_z, 'Z-slice', 0, grid_size-1, valinit=grid_size//2, valstep=1)
    slider_y = Slider(ax_slider_y, 'Y-slice', 0, grid_size-1, valinit=grid_size//2, valstep=1)
    slider_x = Slider(ax_slider_x, 'X-slice', 0, grid_size-1, valinit=grid_size//2, valstep=1)
    
    def update(val):
        z = int(slider_z.val)
        y = int(slider_y.val)
        x = int(slider_x.val)
        
        im1.set_data(volume[:, :, z])
        im2.set_data(volume[:, y, :])
        im3.set_data(volume[x, :, :])
        
        ax1.set_title(f'XY Slice (Z={z})')
        ax2.set_title(f'XZ Slice (Y={y})')
        ax3.set_title(f'YZ Slice (X={x})')
        
        fig.canvas.draw_idle()
    
    slider_z.on_changed(update)
    slider_y.on_changed(update)
    slider_x.on_changed(update)
    
    plt.show()

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python view_volume.py <raw_file>")
        print("Example: python view_volume.py galaxy_ArmDust-low_density.raw")
        sys.exit(1)
    
    filename = sys.argv[1]
    interactive_volume_viewer(filename)
