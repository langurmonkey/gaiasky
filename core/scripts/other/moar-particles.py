#! python

import numpy as np
import random
import argparse
import sys
import gzip
import os

def read_particles_file(filename):
    """
    Read particles from file, automatically handling .gz extension
    Returns: (particles, header_line) where header_line is the first line if it looks like a header
    """
    particles = []
    header_line = None
    
    try:
        if filename.endswith('.gz'):
            with gzip.open(filename, 'rt') as f:
                lines = list(f)
        else:
            with open(filename, 'r') as f:
                lines = list(f)
        
        if not lines:
            return particles, header_line
            
        # Check if first line looks like a header (contains letters)
        first_line = lines[0].strip()
        if first_line and any(c.isalpha() for c in first_line):
            header_line = first_line
            start_index = 1
        else:
            start_index = 0
        
        # Process the remaining lines
        for line in lines[start_index:]:
            if line.strip():
                parts = line.split()
                if len(parts) >= 4:
                    try:
                        # Parse first 4 columns as floats (x, y, z, size)
                        x, y, z, size = map(float, parts[:4])
                        # Store all columns - first 4 as floats, rest as strings to preserve formatting
                        particle_data = [x, y, z, size] + parts[4:]
                        particles.append(particle_data)
                    except ValueError:
                        print(f"Warning: Skipping line with invalid numeric data: {line.strip()}")
                        
    except FileNotFoundError:
        print(f"Error: Input file '{filename}' not found.")
        sys.exit(1)
    except Exception as e:
        print(f"Error reading input file: {e}")
        sys.exit(1)
    
    return particles, header_line

def write_particles_file(filename, particles, header_line=None):
    """
    Write particles to file, automatically handling .gz extension
    """
    try:
        if filename.endswith('.gz'):
            with gzip.open(filename, 'wt') as f:
                if header_line:
                    f.write(header_line + '\n')
                for particle in particles:
                    # Write all columns - first 4 are floats, rest are strings
                    line_parts = [f"{particle[0]}", f"{particle[1]}", f"{particle[2]}", f"{particle[3]}"]
                    if len(particle) > 4:
                        line_parts.extend(particle[4:])
                    f.write(" ".join(line_parts) + "\n")
        else:
            with open(filename, 'w') as f:
                if header_line:
                    f.write(header_line + '\n')
                for particle in particles:
                    # Write all columns - first 4 are floats, rest are strings
                    line_parts = [f"{particle[0]}", f"{particle[1]}", f"{particle[2]}", f"{particle[3]}"]
                    if len(particle) > 4:
                        line_parts.extend(particle[4:])
                    f.write(" ".join(line_parts) + "\n")
    except Exception as e:
        print(f"Error writing to output file: {e}")
        sys.exit(1)

def spawn_dust_particles(input_file, output_file, multiplier=5, position_spread=0.1, size_spread=0.1):
    """
    Spawn additional dust particles near existing ones.
    
    Parameters:
    - input_file: path to input data file
    - output_file: path to output data file
    - multiplier: how many new particles per original particle (default: 5)
    - position_spread: standard deviation for position randomization (default: 0.1)
    - size_spread: standard deviation for size randomization (default: 0.1)
    """
    
    # Read original data
    original_particles, header_line = read_particles_file(input_file)
    print(f"Read {len(original_particles)} original particles from {input_file}")
    if header_line:
        print(f"Header detected and will be preserved: {header_line}")
    
    # Generate new particles
    new_particles = []
    
    for particle in original_particles:
        x, y, z, size = particle[:4]
        
        # Keep the original particle with all its columns
        new_particles.append(particle)
        
        # Generate new particles around this one
        for _ in range(multiplier):
            # Add Gaussian noise to position (more realistic than uniform)
            new_x = x + random.gauss(0, position_spread)
            new_y = y + random.gauss(0, position_spread)
            new_z = z + random.gauss(0, position_spread * 0.5)  # less spread in Z
            
            # Add some variation to size, but keep it positive
            new_size = max(0.01, size + random.gauss(0, size_spread))
            
            # Create new particle with modified position/size but same additional columns
            new_particle = [new_x, new_y, new_z, new_size]
            if len(particle) > 4:
                # Copy additional columns (like RGB colors) from original particle
                new_particle.extend(particle[4:])
            
            new_particles.append(new_particle)
    
    print(f"Generated {len(new_particles)} total particles ({len(new_particles) - len(original_particles)} new)")
    
    # Write all particles to output file (preserve header if present)
    write_particles_file(output_file, new_particles, header_line)
    print(f"Saved to {output_file}")

def spawn_dust_particles_spiral_aware(input_file, output_file, multiplier=5, 
                                     radial_spread=0.05, angular_spread=0.1, 
                                     height_spread=0.02, size_spread=0.1):
    """
    Spawn particles while better preserving spiral structure by working in cylindrical coordinates.
    """
    
    # Read original data
    original_particles, header_line = read_particles_file(input_file)
    print(f"Read {len(original_particles)} original particles from {input_file}")
    if header_line:
        print(f"Header detected and will be preserved: {header_line}")
    
    new_particles = []
    
    for particle in original_particles:
        x, y, z, size = particle[:4]
        
        # Keep original particle with all its columns
        new_particles.append(particle)
        
        # Convert to cylindrical coordinates
        r = np.sqrt(x**2 + y**2)
        theta = np.arctan2(y, x)
        
        # Generate new particles
        for _ in range(multiplier):
            # Add noise in cylindrical coordinates (preserves spiral structure better)
            new_r = max(0.01, r + random.gauss(0, radial_spread))
            new_theta = theta + random.gauss(0, angular_spread)
            new_z = z + random.gauss(0, height_spread)
            
            # Convert back to Cartesian
            new_x = new_r * np.cos(new_theta)
            new_y = new_r * np.sin(new_theta)
            
            # Size variation
            new_size = max(0.01, size + random.gauss(0, size_spread))
            
            # Create new particle with modified position/size but same additional columns
            new_particle = [new_x, new_y, new_z, new_size]
            if len(particle) > 4:
                # Copy additional columns (like RGB colors) from original particle
                new_particle.extend(particle[4:])
            
            new_particles.append(new_particle)
    
    print(f"Generated {len(new_particles)} total particles ({len(new_particles) - len(original_particles)} new)")
    
    # Write to file (preserve header if present)
    write_particles_file(output_file, new_particles, header_line)
    print(f"Saved to {output_file}")

def main():
    parser = argparse.ArgumentParser(
        description='Spawn additional dust particles near existing ones for galaxy simulation',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s input.txt output.txt
  %(prog)s input.dat.gz output_dense.dat.gz
  %(prog)s input.txt output.dat.gz --method spiral --multiplier 10
  %(prog)s input.dat.gz output.txt --method simple --position-spread 0.05 --multiplier 8
  %(prog)s input.dat.gz output.dat.gz --method spiral --radial-spread 0.02 --angular-spread 0.05
        """
    )
    
    # Required arguments
    parser.add_argument('input_file', help='Input file containing original particle data (X Y Z size format). '
                                         'Supports .gz compressed files.')
    parser.add_argument('output_file', help='Output file for enhanced particle data. '
                                          'Use .gz extension for compressed output.')
    
    # Method selection
    parser.add_argument('--method', choices=['simple', 'spiral'], default='spiral',
                       help='Method for spawning particles: "simple" for Cartesian coordinates, '
                            '"spiral" for cylindrical coordinates (better for galaxies, default: spiral)')
    
    # Common parameters
    parser.add_argument('--multiplier', type=int, default=5, metavar='N',
                       help='Number of new particles per original particle (default: 5)')
    parser.add_argument('--size-spread', type=float, default=0.1, metavar='SPREAD',
                       help='Standard deviation for size randomization (default: 0.1)')
    
    # Simple method parameters
    parser.add_argument('--position-spread', type=float, default=0.1, metavar='SPREAD',
                       help='Standard deviation for position randomization (simple method only, default: 0.1)')
    
    # Spiral method parameters
    parser.add_argument('--radial-spread', type=float, default=0.03, metavar='SPREAD',
                       help='Radial spread for spiral method (default: 0.03)')
    parser.add_argument('--angular-spread', type=float, default=0.08, metavar='SPREAD',
                       help='Angular spread for spiral method in radians (default: 0.08)')
    parser.add_argument('--height-spread', type=float, default=0.01, metavar='SPREAD',
                       help='Vertical (Z) spread for spiral method (default: 0.01)')
    
    args = parser.parse_args()
    
    print(f"Processing: {args.input_file} -> {args.output_file}")
    print(f"Method: {args.method}, Multiplier: {args.multiplier}")
    
    # Show compression status
    input_compressed = " (compressed)" if args.input_file.endswith('.gz') else ""
    output_compressed = " (compressed)" if args.output_file.endswith('.gz') else ""
    print(f"Input format: {args.input_file}{input_compressed}")
    print(f"Output format: {args.output_file}{output_compressed}")
    
    if args.method == 'simple':
        print(f"Position spread: {args.position_spread}, Size spread: {args.size_spread}")
        spawn_dust_particles(
            input_file=args.input_file,
            output_file=args.output_file,
            multiplier=args.multiplier,
            position_spread=args.position_spread,
            size_spread=args.size_spread
        )
    else:  # spiral method
        print(f"Radial spread: {args.radial_spread}, Angular spread: {args.angular_spread}, "
              f"Height spread: {args.height_spread}, Size spread: {args.size_spread}")
        spawn_dust_particles_spiral_aware(
            input_file=args.input_file,
            output_file=args.output_file,
            multiplier=args.multiplier,
            radial_spread=args.radial_spread,
            angular_spread=args.angular_spread,
            height_spread=args.height_spread,
            size_spread=args.size_spread
        )

if __name__ == "__main__":
    main()
