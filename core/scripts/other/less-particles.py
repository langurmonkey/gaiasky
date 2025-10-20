#! python

import numpy as np
import random
import argparse
import sys
import gzip

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
            print(f"Detected header: {first_line}")
        else:
            start_index = 0
        
        # Process the remaining lines
        for line_num, line in enumerate(lines[start_index:], start_index):
            if line.strip():
                parts = line.split()
                if len(parts) >= 4:
                    try:
                        # Parse first 4 columns as floats (x, y, z, size)
                        x, y, z, size = map(float, parts[:4])
                        # Store all columns - first 4 as floats, rest as strings to preserve formatting
                        particle_data = [x, y, z, size] + parts[4:] + [line_num]
                        particles.append(particle_data)
                    except ValueError:
                        print(f"Warning: Skipping line {line_num+1} - invalid numeric data: {line.strip()}")
                        
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
                    # Write all columns - first 4 are floats, rest are strings (skip line_num at the end)
                    line_parts = [f"{particle[0]}", f"{particle[1]}", f"{particle[2]}", f"{particle[3]}"]
                    # Include all additional columns except the last one (line_num)
                    if len(particle) > 5:  # x,y,z,size + line_num + at least one extra column
                        line_parts.extend(particle[4:-1])
                    elif len(particle) == 5:  # x,y,z,size + line_num only (no extra columns)
                        # No extra columns to add
                        pass
                    f.write(" ".join(line_parts) + "\n")
        else:
            with open(filename, 'w') as f:
                if header_line:
                    f.write(header_line + '\n')
                for particle in particles:
                    # Write all columns - first 4 are floats, rest are strings (skip line_num at the end)
                    line_parts = [f"{particle[0]}", f"{particle[1]}", f"{particle[2]}", f"{particle[3]}"]
                    # Include all additional columns except the last one (line_num)
                    if len(particle) > 5:  # x,y,z,size + line_num + at least one extra column
                        line_parts.extend(particle[4:-1])
                    elif len(particle) == 5:  # x,y,z,size + line_num only (no extra columns)
                        # No extra columns to add
                        pass
                    f.write(" ".join(line_parts) + "\n")
    except Exception as e:
        print(f"Error writing to output file: {e}")
        sys.exit(1)

def take_fraction_particles(input_file, output_file, fraction=0.5, method='random'):
    """
    Take a fraction of particles from the input file.
    
    Parameters:
    - input_file: path to input data file
    - output_file: path to output data file
    - fraction: fraction of particles to keep (0.0-1.0)
    - method: 'random' for random selection, 'first' for first N, 'last' for last N
    """
    
    if not 0.0 <= fraction <= 1.0:
        print(f"Error: Fraction must be between 0.0 and 1.0, got {fraction}")
        sys.exit(1)
    
    # Read original data
    particles, header_line = read_particles_file(input_file)
    total_particles = len(particles)
    
    if total_particles == 0:
        print("Error: Input file contains no particles")
        sys.exit(1)
    
    print(f"Read {total_particles} particles from {input_file}")
    if header_line:
        print(f"Header detected and will be preserved")
    
    # Check if particles have additional columns (like colors)
    if particles and len(particles[0]) > 5:  # x,y,z,size,line_num + at least one extra
        extra_columns = len(particles[0]) - 5
        print(f"Detected {extra_columns} additional column(s) per particle (e.g., colors)")
    
    print(f"Fraction: {fraction} ({fraction*100:.1f}%)")
    
    # Calculate number of particles to keep
    n_keep = max(1, int(round(total_particles * fraction)))
    print(f"Keeping {n_keep} of {total_particles} particles")
    
    # Select particles based on method
    if method == 'random':
        selected_particles = random.sample(particles, n_keep)
        print("Selection method: random sampling")
    elif method == 'first':
        selected_particles = particles[:n_keep]
        print("Selection method: first N particles")
    elif method == 'last':
        selected_particles = particles[-n_keep:]
        print("Selection method: last N particles")
    elif method == 'uniform':
        # Uniformly spaced selection throughout the file
        indices = np.linspace(0, total_particles - 1, n_keep, dtype=int)
        selected_particles = [particles[i] for i in indices]
        print("Selection method: uniformly spaced")
    else:
        print(f"Error: Unknown selection method '{method}'")
        sys.exit(1)
    
    # Write selected particles to output file (preserve header if present)
    write_particles_file(output_file, selected_particles, header_line)
    print(f"Saved {len(selected_particles)} particles to {output_file}")

def main():
    parser = argparse.ArgumentParser(
        description='Take a fraction of particles from a file',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s input.txt output.txt --fraction 0.1
  %(prog)s input.dat.gz output_small.dat.gz --fraction 0.25
  %(prog)s input.txt output.txt --fraction 0.5 --method first
  %(prog)s input.dat.gz output.txt --fraction 0.75 --method uniform
  %(prog)s input.txt output.dat.gz --fraction 0.01 --method last
        """
    )
    
    # Required arguments
    parser.add_argument('input_file', help='Input file containing particle data (X Y Z size format). '
                                         'Supports .gz compressed files.')
    parser.add_argument('output_file', help='Output file for selected particles. '
                                          'Use .gz extension for compressed output.')
    
    # Fraction argument
    parser.add_argument('--fraction', type=float, required=True, metavar='F',
                       help='Fraction of particles to keep (0.0-1.0, required)')
    
    # Selection method
    parser.add_argument('--method', choices=['random', 'first', 'last', 'uniform'], 
                       default='random',
                       help='Selection method: "random" (default), "first", "last", or "uniform"')
    
    args = parser.parse_args()
    
    # Validate fraction
    if not 0.0 <= args.fraction <= 1.0:
        parser.error("Fraction must be between 0.0 and 1.0")
    
    print(f"Processing: {args.input_file} -> {args.output_file}")
    
    # Show compression status
    input_compressed = " (compressed)" if args.input_file.endswith('.gz') else ""
    output_compressed = " (compressed)" if args.output_file.endswith('.gz') else ""
    print(f"Input format: {args.input_file}{input_compressed}")
    print(f"Output format: {args.output_file}{output_compressed}")
    
    take_fraction_particles(
        input_file=args.input_file,
        output_file=args.output_file,
        fraction=args.fraction,
        method=args.method
    )

if __name__ == "__main__":
    main()
