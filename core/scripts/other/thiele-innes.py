import numpy as np
from astropy import units

def thiele_innes_to_campbell(A, B, F, G, parallax):
    """
    Converts Thiele-Innes elements (A, B, F, G in mas) to Campbell elements.
    The relation between Thiele-Innes and Campbell elements is as follows:

    A = a * (cos ω * cos Ω - sin ω * sin Ω * cos i)
    B = a * (cos ω * sin Ω + sin ω * cos Ω * cos i)
    F = -a * (sin ω * cos Ω + cos ω * sin Ω * cos i)
    G = -a * (sin ω * sin Ω - cos ω * cos Ω * cos i)
    
    Returns: semimajor axis (km, mas), inclination, Omega, omega (radians & degrees)
    """
    # Convert to arcsec for internal calculations
    A_asec = A * 1e-3
    B_asec = B * 1e-3
    F_asec = F * 1e-3
    G_asec = G * 1e-3

    # See following article for formulas:
    # https://arxiv.org/html/2502.20553v1

    # Step 1: Compute a^2
    u = A_asec**2 + B_asec**2 + F_asec**2 + G_asec**2
    v = A_asec * G_asec - B_asec * F_asec
    a_asec_squared = u / 2.0 + np.sqrt((u / (2.0 - v) * (u / (2.0 + v))))
    a_asec = np.sqrt(a_asec_squared)
    a_mas = a_asec * 1e3  # back to mas

    # Step 2: Compute cos(i)
    w = u / v
    c1 = (w - np.sqrt(w**2 - 4.0)) / 2.0
    c2 = (w + np.sqrt(w**2 - 4.0)) / 2.0
    cos_i = c1 if w > 0 else c2
    i = np.arccos(cos_i)

    # Step 3: Compute Omega + omega and Omega - omega
    X = A_asec + G_asec
    Y = B_asec - F_asec
    Z = B_asec + F_asec
    W = A_asec - G_asec

    sum_angle = np.arctan2(Y, X)
    diff_angle = np.arctan2(Z, W)

    Omega = (sum_angle + diff_angle) / 2
    omega = (sum_angle - diff_angle) / 2

    # Normalize angles to [0, 2pi)
    Omega = Omega % (2 * np.pi)
    omega = omega % (2 * np.pi)

    # Convert semimajor axis to AU then to km
    a_au = a_asec / (parallax * 1e-3)
    a_km = (a_au * units.au).to(units.km).value

    # Verification
    Ac = a_mas * (np.cos(omega) * np.cos(Omega) - np.sin(omega) * np.sin(Omega) * np.cos(i))
    Bc = a_mas * (np.cos(omega) * np.sin(Omega) + np.sin(omega) * np.cos(Omega) * np.cos(i))
    Fc = a_mas * (-np.sin(omega) * np.cos(Omega) - np.cos(omega) * np.sin(Omega) * np.cos(i))
    Gc = a_mas * (-np.sin(omega) * np.sin(Omega) + np.cos(omega) * np.cos(Omega) * np.cos(i))

    print("\nVerification:")
    print(f"A: {A:.6f} vs {Ac:.6f} (diff: {A - Ac:.2e})")
    print(f"B: {B:.6f} vs {Bc:.6f} (diff: {B - Bc:.2e})")
    print(f"F: {F:.6f} vs {Fc:.6f} (diff: {F - Fc:.2e})")
    print(f"G: {G:.6f} vs {Gc:.6f} (diff: {G - Gc:.2e})")

    return {
        'semimajor_axis_km': a_km,
        'semimajor_axis_mas': a_mas,
        'inclination_rad': i,
        'Omega_rad': Omega,
        'omega_rad': omega,
        'inclination_deg': np.degrees(i),
        'Omega_deg': np.degrees(Omega),
        'omega_deg': np.degrees(omega)
    }


# Example usage
if __name__ == "__main__":
    # Example Thiele-Innes elements (in milliarcseconds)
    A = -0.06
    B = -0.208
    F = 0.181
    G = -0.438
    # Solution a: 0.505
    # Solution i: 75.5
    parallax = 50.0  # in milliarcseconds

    campbell = thiele_innes_to_campbell(A, B, F, G, parallax)

    a = campbell['semimajor_axis_mas']
    a_km = campbell['semimajor_axis_km']
    i = campbell['inclination_rad']
    Omega = campbell['Omega_rad']
    w = campbell['omega_rad']

    print("\n# Campbell Elements:")
    print(f"a: {a_km:.4f} km, {a:.6f} mas")
    print(f"i: {i:.4f} rad, {campbell['inclination_deg']:.2f}°")
    print(f"Ω: {Omega:.4f} rad, {campbell['Omega_deg']:.2f}°")
    print(f"ω: {w:.4f} rad, {campbell['omega_deg']:.2f}°")
