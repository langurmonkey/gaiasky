import numpy as np


def keplerian_to_cartesian(period_days, epoch_jd, target_jd,
                           sma_km, ecc, inc_deg, raan_deg,
                           argp_deg, mean_anom_deg,
                           mu_km3s2=1.32712440018e11):
    # Convert degrees to radians
    inc = np.radians(inc_deg)
    raan = np.radians(raan_deg)
    argp = np.radians(argp_deg)
    M0 = np.radians(mean_anom_deg)

    # Time since epoch (in seconds)
    delta_t = (target_jd - epoch_jd) * 86400.0

    # Mean motion (rad/s)
    n = 2 * np.pi / (period_days * 86400.0)

    # Updated mean anomaly at target time
    M = (M0 + n * delta_t) % (2 * np.pi)

    # Solve Kepler's Equation: M = E - e*sin(E)
    def solve_kepler(M, e, tol=1e-10):
        E = M if e < 0.8 else np.pi
        for _ in range(100):
            f = E - e * np.sin(E) - M
            fp = 1 - e * np.cos(E)
            dE = -f / fp
            E += dE
            if abs(dE) < tol:
                break
        return E

    E = solve_kepler(M, ecc)

    # True anomaly
    nu = 2 * np.arctan2(np.sqrt(1 + ecc) * np.sin(E / 2),
                        np.sqrt(1 - ecc) * np.cos(E / 2))

    # Distance
    r = sma_km * (1 - ecc * np.cos(E))

    # Position in perifocal frame
    x_pf = r * np.cos(nu)
    y_pf = r * np.sin(nu)
    z_pf = 0

    # Velocity in perifocal frame
    p = sma_km * (1 - ecc**2)
    vx_pf = -np.sqrt(mu_km3s2 / p) * np.sin(nu)
    vy_pf =  np.sqrt(mu_km3s2 / p) * (ecc + np.cos(nu))
    vz_pf = 0

    # Rotation matrix from perifocal to inertial
    cos_O = np.cos(raan)
    sin_O = np.sin(raan)
    cos_i = np.cos(inc)
    sin_i = np.sin(inc)
    cos_w = np.cos(argp)
    sin_w = np.sin(argp)

    R = np.array([
        [cos_O * cos_w - sin_O * sin_w * cos_i, -cos_O * sin_w - sin_O * cos_w * cos_i, sin_O * sin_i],
        [sin_O * cos_w + cos_O * sin_w * cos_i, -sin_O * sin_w + cos_O * cos_w * cos_i, -cos_O * sin_i],
        [sin_w * sin_i,                         cos_w * sin_i,                          cos_i]
    ])

    r_vec = R @ np.array([x_pf, y_pf, z_pf])
    v_vec = R @ np.array([vx_pf, vy_pf, vz_pf])

    return r_vec, v_vec

# Example usage
if __name__ == "__main__":
    period_days = 48681.19346262312
    epoch_jd = 2450000.5
    sma_km = 3903318039.031277657  # 1 AU
    ecc = 0.963225755046038
    inc_deg = 113.453816997171
    raan_deg = 139.3811920815948
    argp_deg = 152.9821676305871
    mean_anom_deg = 7.631696167124212  # at epoch

    n_samples = 2000
    dt = period_days / float(n_samples)
    t_jd = epoch_jd

    print(f"t,x,y,z,vx,vy,vz")
    for i in range(n_samples):
        
        pos_km, vel_kms = keplerian_to_cartesian(period_days,
                                                  epoch_jd,
                                                  t_jd,
                                                  sma_km,
                                                  ecc,
                                                  inc_deg,
                                                  raan_deg,
                                                  argp_deg,
                                                  mean_anom_deg)
        print(f"{t_jd},{pos_km[0]},{pos_km[1]},{pos_km[2]},{vel_kms[0]},{vel_kms[1]},{vel_kms[2]}")
        t_jd = t_jd + dt
