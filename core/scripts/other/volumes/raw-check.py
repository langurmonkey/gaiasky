#!/usr/bin/env python3
import numpy as np
import sys

def check_raw_file(filename, grid_size=128):
    """Quick check if raw file was created correctly"""
    
    with open(filename, 'rb') as f:
        data = np.fromfile(f, dtype=np.uint8)
    
    expected_size = grid_size ** 3
    actual_size = len(data)
    
    print(f"File: {filename}")
    print(f"Expected size: {expected_size} bytes")
    print(f"Actual size: {actual_size} bytes")
    print(f"Data range: {data.min()} to {data.max()}")
    print(f"Non-zero values: {np.sum(data > 0)} / {len(data)}")
    
    if actual_size != expected_size:
        print("❌ SIZE MISMATCH!")
    else:
        print("✅ Size matches expected")
    
    # Show first few values
    print(f"First 10 values: {data[:10]}")

if __name__ == "__main__":
    check_raw_file(sys.argv[1])
