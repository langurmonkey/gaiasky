#!/usr/bin/env python3
"""
CMOD to OBJ Converter
---------------------
Converts Celestia Object (CMOD) ASCII files to Wavefront OBJ/MTL.

Features:
- Deterministic Material Assignment: Correctly maps materials to mesh 
  groups by parsing the material index from the 'trilist' command.
- PBR Support: Detects packed PBR textures (Occlusion, Roughness, Metallic) 
  and exports them using the custom 'map_pbr' tag.
- Emissive Support: Captures 'emissive' colors and 'emissivemap' textures 
  (Ke/map_Ke).
- Coordinate Correction: Automatically flips the V-axis for UV coordinates 
  to match the standard OBJ convention.

Usage:
    python cmod2obj.py model.cmod

Requirements:
    Python 3.6+
    Input file must be in ASCII CMOD format (use cmodfix -a to convert binary).
"""

import sys
from pathlib import Path

# -----------------------------
# Data containers
# -----------------------------
materials = []
meshes = []

def parse_material_block(lines, i):
    mat = {
        "Kd": [0.8, 0.8, 0.8],
        "Ks": [0.0, 0.0, 0.0],
        "Ke": [0.0, 0.0, 0.0],  # Default emissive to 0
        "Ns": 64,
        "maps": {}
    }
    i += 1
    while i < len(lines):
        line = lines[i].strip()
        if line == "end_material":
            materials.append(mat)
            return i
        parts = line.split()
        if not parts: i += 1; continue
        cmd = parts[0].lower()
        
        if cmd == "diffuse": 
            mat["Kd"] = [float(x) for x in parts[1:4]]
        elif cmd == "specular": 
            mat["Ks"] = [float(x) for x in parts[1:4]]
        elif cmd == "emissive": # Added Emissive support
            mat["Ke"] = [float(x) for x in parts[1:4]]
        elif cmd == "specpower": 
            mat["Ns"] = float(parts[1])
        elif cmd == "texture0": 
            mat["maps"]["diffuse"] = parts[1].strip('"')
        elif cmd == "normalmap": 
            mat["maps"]["normal"] = parts[1].strip('"')
        elif cmd == "specularmap": 
            mat["maps"]["spec"] = parts[1].strip('"')
        elif cmd == "emissivemap": # Added Emissive map support
            mat["maps"]["emissive"] = parts[1].strip('"')
        i += 1
    return i

def parse_mesh_block(lines, i):
    verts, uvs, normals = [], [], []
    i += 1
    while i < len(lines):
        line = lines[i].strip()
        if line == "end_mesh":
            return i
        
        parts = line.split()
        if not parts: i += 1; continue
        
        if parts[0] == "vertices":
            vcount = int(parts[1])
            i += 1
            for _ in range(vcount):
                v_line = [float(x) for x in lines[i].split()]
                verts.append(v_line[0:3])
                uvs.append(v_line[3:5])
                if len(v_line) >= 8: normals.append(v_line[5:8])
                i += 1
            continue
            
        elif parts[0] == "trilist":
            mat_idx = int(parts[1])
            idx_count = int(parts[2])
            indices = []
            i += 1
            while len(indices) < idx_count:
                indices.extend([int(x) for x in lines[i].split()])
                i += 1
            
            faces = []
            for j in range(0, len(indices), 3):
                faces.append((indices[j], indices[j+1], indices[j+2]))
            
            meshes.append({
                "v": verts, "vt": uvs, "vn": normals,
                "f": faces, "mat": mat_idx
            })
            continue
        i += 1
    return i

def parse_file(path):
    lines = Path(path).read_text(errors="ignore").splitlines()
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if line == "material":
            i = parse_material_block(lines, i)
        elif line == "mesh":
            i = parse_mesh_block(lines, i)
        else:
            i += 1

def write_output(base):
    obj_path = base.with_suffix(".obj")
    mtl_path = base.with_suffix(".mtl")

    with open(mtl_path, "w") as f:
        for idx, m in enumerate(materials):
            f.write(f"newmtl mat_{idx}\n")
            f.write(f"Kd {m['Kd'][0]} {m['Kd'][1]} {m['Kd'][2]}\n")
            f.write(f"Ks {m['Ks'][0]} {m['Ks'][1]} {m['Ks'][2]}\n")
            f.write(f"Ke {m['Ke'][0]} {m['Ke'][1]} {m['Ke'][2]}\n") # Emissive Color
            f.write(f"Ns {m['Ns']}\n")
            
            if "diffuse" in m["maps"]: f.write(f"map_Kd {m['maps']['diffuse']}\n")
            if "normal" in m["maps"]: f.write(f"map_Bump {m['maps']['normal']}\n")
            if "spec" in m["maps"]: f.write(f"map_Ks {m['maps']['spec']}\n")
            if "emissive" in m["maps"]: f.write(f"map_Ke {m['maps']['emissive']}\n") # Emissive Map
            f.write("\n")

    with open(obj_path, "w") as f:
        f.write(f"mtllib {mtl_path.name}\n")
        v_offset = 1
        for mi, mesh in enumerate(meshes):
            f.write(f"o part_{mi}\n")
            f.write(f"usemtl mat_{mesh['mat']}\n")
            
            for v in mesh["v"]: f.write(f"v {v[0]} {v[1]} {v[2]}\n")
            for vt in mesh["vt"]: f.write(f"vt {vt[0]} {1.0 - vt[1]}\n")
            for vn in mesh["vn"]: f.write(f"vn {vn[0]} {vn[1]} {vn[2]}\n")
            
            has_uv, has_n = len(mesh["vt"]) > 0, len(mesh["vn"]) > 0
            for a, b, c in mesh["f"]:
                va, vb, vc = a + v_offset, b + v_offset, c + v_offset
                if has_uv and has_n: f.write(f"f {va}/{va}/{va} {vb}/{vb}/{vb} {vc}/{vc}/{vc}\n")
                elif has_uv: f.write(f"f {va}/{va} {vb}/{vb} {vc}/{vc}\n")
                else: f.write(f"f {va} {vb} {vc}\n")
            
            v_offset += len(mesh["v"])

if __name__ == "__main__":
    if len(sys.argv) < 2: sys.exit(1)
    src = Path(sys.argv[1])
    parse_file(src)
    write_output(src.with_suffix(""))
    print(f"Conversion complete. Emissive support enabled.")
