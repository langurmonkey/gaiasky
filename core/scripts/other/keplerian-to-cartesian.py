import numpy as np

def true_anomaly_to_time(nu_rad, ecc, n, epoch_jd):
    # Convert true anomaly to eccentric anomaly
    E = 2 * np.arctan2(np.tan(nu_rad / 2) * np.sqrt((1 - ecc) / (1 + ecc)), 1)
    E = E % (2 * np.pi)
    M = E - ecc * np.sin(E)
    delta_t = M / n
    return epoch_jd + delta_t / 86400.0  # JD

def true_anomaly_to_state_vectors(sma_km, ecc, inc_deg, raan_deg, argp_deg, nu_deg, mu_km3s2=1.32712440018e11):
    inc = np.radians(inc_deg)
    raan = np.radians(raan_deg)
    argp = np.radians(argp_deg)
    nu = np.radians(nu_deg)

    # Distance and perifocal position
    r = sma_km * (1 - ecc**2) / (1 + ecc * np.cos(nu))
    x_pf = r * np.cos(nu)
    y_pf = r * np.sin(nu)
    z_pf = 0

    # Perifocal velocity
    p = sma_km * (1 - ecc**2)
    vx_pf = -np.sqrt(mu_km3s2 / p) * np.sin(nu)
    vy_pf =  np.sqrt(mu_km3s2 / p) * (ecc + np.cos(nu))
    vz_pf = 0

    # Rotation matrix
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

    n = 2 * np.pi / (period_days * 86400.0)  # mean motion (rad/s)

    n_samples = 500
    print(f"t,x,y,z,vx,vy,vz")

    for i in range(n_samples):
        nu = 2 * np.pi * i / n_samples
        t_jd = true_anomaly_to_time(nu, ecc, n, epoch_jd)
        pos_km, vel_kms = true_anomaly_to_state_vectors(sma_km, ecc, inc_deg, raan_deg, argp_deg, np.degrees(nu))
        print(f"{t_jd},{pos_km[0]},{pos_km[1]},{pos_km[2]},{vel_kms[0]},{vel_kms[1]},{vel_kms[2]}")

