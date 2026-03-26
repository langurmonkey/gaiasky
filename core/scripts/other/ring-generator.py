import argparse
import random
import math
import sys
from PIL import Image

def get_alpha_at_radius(img, radius, r_inner, r_outer):
    """Maps a physical radius to a pixel and returns its Alpha value (0.0 to 1.0)."""
    if img is None:
        return 1.0
    
    width, height = img.size
    # Mapping: y=0 -> r_inner, y=height-1 -> r_outer
    norm = (radius - r_inner) / (r_outer - r_inner)
    y = int(norm * (height - 1))
    y = max(0, min(height - 1, y))
    
    # Get pixel data
    pixel = img.getpixel((0, y))
    
    # Check if image has an alpha channel (RGBA)
    if isinstance(pixel, tuple) and len(pixel) == 4:
        return pixel[3] / 255.0
    
    # Fallback if image is RGB or L (no alpha)
    return 1.0

def generate_rings_votable(args):
    AU_IN_KM = 149597870.7
    DAY_IN_SECONDS = 86400

    # Load image and ensure it's RGBA to get the alpha channel
    dist_img = None
    if args.image:
        try:
            dist_img = Image.open(args.image).convert('RGBA')
            print(f"Sampling radial density from alpha channel of: {args.image}")
        except Exception as e:
            print(f"Error loading image: {e}. Defaulting to uniform distribution.", file=sys.stderr)

    header = f"""<?xml version="1.0" encoding="utf-8"?>
<VOTABLE version="1.4" xmlns="http://www.ivoa.net/xml/VOTable/v1.3">
 <RESOURCE type="results">
  <TABLE ID="ring_particles" name="ring_particles">
   <FIELD ID="ID" datatype="long" name="ID"/>
   <FIELD ID="epoch" datatype="double" name="epoch" unit="d"/>
   <FIELD ID="semimajoraxis" datatype="double" name="semimajoraxis" unit="AU"/>
   <FIELD ID="eccentricity" datatype="double" name="eccentricity"/>
   <FIELD ID="inclination" datatype="double" name="inclination" unit="deg"/>
   <FIELD ID="ascendingnode" datatype="double" name="ascendingnode" unit="deg"/>
   <FIELD ID="argofpericenter" datatype="double" name="argofpericenter" unit="deg"/>
   <FIELD ID="meananomaly" datatype="double" name="meananomaly" unit="deg"/>
   <FIELD ID="period" datatype="double" name="period" unit="d"/>
   <DATA>
    <TABLEDATA>"""

    footer = """    </TABLEDATA>
   </DATA>
  </TABLE>
 </RESOURCE>
</VOTABLE>"""

    with open(args.output, 'w') as f:
        f.write(header + "\n")
        
        count = 0
        attempts = 0
        while count < args.number:
            attempts += 1
            # Pick a candidate radius
            a_km = random.uniform(args.r_inner, args.r_outer)
            
            # Rejection sampling based on Alpha
            alpha = get_alpha_at_radius(dist_img, a_km, args.r_inner, args.r_outer)
            
            if random.random() < alpha:
                count += 1
                a_au = a_km / AU_IN_KM
                ecc = random.uniform(0, 0.0005)
                inc = random.uniform(0, 0.3)
                node, arg_p, m_anom = [random.uniform(0, 360) for _ in range(3)]
                
                # Period calculation
                period_days = (2 * math.pi * math.sqrt(a_km**3 / args.gm)) / DAY_IN_SECONDS

                row = (f"     <TR>\n"
                       f"      <TD>{count}</TD><TD>{args.epoch}</TD>\n"
                       f"      <TD>{a_au:.12f}</TD><TD>{ecc:.8f}</TD><TD>{inc:.8f}</TD>\n"
                       f"      <TD>{node:.4f}</TD><TD>{arg_p:.4f}</TD><TD>{m_anom:.4f}</TD>\n"
                       f"      <TD>{period_days:.8f}</TD>\n"
                       f"     </TR>")
                f.write(row + "\n")
                
                # Progress hint for very large sets
                if count % 1000 == 0:
                    print(f"Generated {count}/{args.number} particles...", end='\r')
            
            # Safety break for empty images
            if attempts > args.number * 100 and count == 0:
                print("\nWarning: No particles accepted. Is your image fully transparent?")
                break
            
        f.write(footer)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate ring particles using Alpha mask.")
    parser.add_argument("-n", "--number", type=int, default=1000, help="Target particle count")
    parser.add_argument("-o", "--output", type=str, default="saturn_particles.vot")
    parser.add_argument("--image", type=str, help="PNG with Alpha channel for density")
    parser.add_argument("--gm", type=float, help="Gravitational parameter (/mu) of planet, GM", default=37931187)
    parser.add_argument("--r_inner", type=float, default=74500)
    parser.add_argument("--r_outer", type=float, default=139775)
    parser.add_argument("--epoch", type=float, default=0.0)
    
    args = parser.parse_args()
    generate_rings_votable(args)
    print(f"\nDone! Output saved to {args.output}")
