#!/usr/bin/env python

"""
Orbit Simulator and Anisotropic Cloud Generator

This script simulates an orbit based on orbital mechanics equations and generates
anisotropic particle clouds based on the generated orbital states. The user can
choose to sample the orbit in time or in true anomaly (nu), and optionally generate
the anisotropic clouds and print the results.

Usage:
    python keplerian-to-cartesian.py [options]

Options:
    --sample-time          Sample the orbit in time.
    --sample-nu            Sample the orbit in true anomaly (nu).
    --generate-clouds      Generate anisotropic particle clouds.
    --print-samples        Print out the generated orbit samples.
    --print-cloud          Print out cloud particle data.
    --n-samples=<n>        Number of samples to generate (default: 5000).
    --sigma-cross-km=<km>  Cross-track dispersion for particle clouds in km (default: 5000000.0).
    --mu-km3s2=<mu>        Gravitational constant in km^3/s^2 (default: 1.32712440018e11).
    --help                 Show this help message.

Example usage:
    python keplerian-to-cartesian.py --sample_time --generate_clouds --n_samples=1000 --sigma_cross_km=1000000.0
    python keplerian-to-cartesian.py --sample_nu --print_samples
"""

import argparse
import numpy as np

PI2 = 2.0 * np.pi

def mean_anomaly(M0, n, dt_days):
    """
    Compute the mean anomaly at a given time.

    Parameters:
        M0 (float): Mean anomaly at epoch (in radians).
        n (float): Mean motion in radians per day.
        dt_days (float): Time since epoch in Julian days.

    Returns:
        float: Mean anomaly at time t (in radians, normalized to [0, 2π)).
    """
    M = M0 + n * dt_days
    return M % PI2

def solve_kepler(M, ecc, tol=1e-10, max_iter=100):
    """
    Solve Kepler's Equation M = E - e*sin(E) for the eccentric anomaly E.
    
    Parameters:
        M (float): Mean anomaly in radians.
        ecc (float): Orbital eccentricity (0 <= e < 1).
        tol (float): Convergence tolerance.
        max_iter (int): Maximum number of iterations.
        
    Returns:
        float: Eccentric anomaly E in radians.
    """
    M = M % PI2  # Normalize M to [0, 2π)

    # Initial guess: for low e, E ≈ M; for high e, try something smarter
    if ecc < 0.8:
        E = M
    else:
        E = np.pi

    for i in range(max_iter):
        f = E - ecc * np.sin(E) - M
        f_prime = 1 - ecc * np.cos(E)
        delta = -f / f_prime
        E += delta
        if abs(delta) < tol:
            return E

    raise RuntimeError(f"Kepler solver did not converge after {max_iter} iterations (M={M}, e={ecc})")

def true_anomaly(E, ecc):
    """
    Compute the true anomaly ν from the eccentric anomaly E and eccentricity e.

    Parameters:
        E (float): Eccentric anomaly in radians.
        ecc (float): Orbital eccentricity (0 <= e < 1 for elliptical orbits).

    Returns:
        float: True anomaly ν in radians, normalized to [0, 2π).
    """
    sin_v = (np.sqrt(1 - ecc**2) * np.sin(E)) / (1 - ecc * np.cos(E))
    cos_v = (np.cos(E) - ecc) / (1 - ecc * np.cos(E))
    v = np.atan2(sin_v, cos_v)
    return v % PI2

def time_to_true_anomaly(ecc, M0, n, dt_days):
    """
    Compute the true anomaly at time t = epoch + dt_days.

    Parameters:
        ecc (float): Eccentricity (0 <= e < 1)
        M0 (float): Mean anomaly at epoch (radians)
        n (float): Mean motion (radians/day)
        dt_days (float): Time since epoch in Julian days

    Returns:
        float: True anomaly in radians
    """
    M = mean_anomaly(M0, n, dt_days)
    E = solve_kepler(M, ecc)
    v = true_anomaly(E, ecc)
    return v
    
def true_anomaly_to_time(nu_rad, ecc, M0, n, epoch_jd):
    # Convert true anomaly to eccentric anomaly
    E = 2 * np.arctan2(np.tan(nu_rad / 2) * np.sqrt((1 - ecc) / (1 + ecc)), 1)
    E = (E + PI2) % PI2
    M = E - ecc * np.sin(E)
    delta_t = (M - M0) / n
    return epoch_jd + delta_t  # JD

def time_to_state_vectors(period_days, sma_km, ecc, inc_deg, raan_deg, argp_deg, mean_anom_deg, epoch_jd, dt_days, mu):
    """
    Compute position and velocity vectors at a specific time since epoch.
    
    Parameters:
        period_days (float): Orbital period in days
        sma_km (float): Semimajor axis in km
        ecc (float): Eccentricity (0 <= e < 1)
        inc_deg (float): Inclination in degrees
        raan_deg (float): Longitude of ascending node in degrees
        argp_deg (float): Argument of periapsis in degrees
        epoch_jd (float): Epoch (Julian Date)
        dt_days (float): Time since epoch in Julian days
        mu (float): Gravitational constant in km^3/s^2

    Returns:
        tuple: (position_km, velocity_km_s), each a 3-element numpy array
    """
    n = PI2 / period_days  # mean motion (rad/days)
    M0 = np.radians(mean_anom_deg)

    M = mean_anomaly(M0, n, dt_days)
    E = solve_kepler(M, ecc)
    v = true_anomaly(E, ecc)

    # Radius
    r = sma_km * (1 - ecc * np.cos(E))

    # Position in orbital plane
    x_orb = r * np.cos(v)
    y_orb = r * np.sin(v)
    z_orb = 0.0

    # Velocity in orbital plane
    h = np.sqrt(mu * sma_km * (1 - ecc**2))
    vx_orb = -mu / h * np.sin(E)
    vy_orb = mu / h * np.sqrt(1 - ecc**2) * np.cos(E)
    vz_orb = 0.0

    # Rotation to ECI
    inc = np.radians(inc_deg)
    raan = np.radians(raan_deg)
    argp = np.radians(argp_deg)

    cos_O, sin_O = np.cos(raan), np.sin(raan)
    cos_i, sin_i = np.cos(inc), np.sin(inc)
    cos_w, sin_w = np.cos(argp), np.sin(argp)

    R = np.array([
        [cos_O*cos_w - sin_O*sin_w*cos_i, -cos_O*sin_w - sin_O*cos_w*cos_i, sin_O*sin_i],
        [sin_O*cos_w + cos_O*sin_w*cos_i, -sin_O*sin_w + cos_O*cos_w*cos_i, -cos_O*sin_i],
        [sin_w*sin_i,                    cos_w*sin_i,                     cos_i]
    ])

    pos = R @ np.array([x_orb, y_orb, z_orb])
    vel = R @ np.array([vx_orb, vy_orb, vz_orb])

    return pos, vel

def sample_time(period_days, epoch_jd, sma_km, ecc, inc_deg, raan_deg, argp_deg, mean_anom_deg, mu, n_samples=500, prt=False):
    if prt:
        print(f"t,x,y,z,vx,vy,vz")

    delta_t = period_days / n_samples
    t = 0.0
    result = []

    for i in range(n_samples):
        pos_km, vel_kms = time_to_state_vectors(period_days, sma_km, ecc, inc_deg, raan_deg, argp_deg, mean_anom_deg, epoch_jd, t, mu)        
        result.append([t, pos_km, vel_kms])
        
        if prt:
            print(f"{t},{pos_km[0]},{pos_km[1]},{pos_km[2]},{vel_kms[0]},{vel_kms[1]},{vel_kms[2]}")

        t = t + delta_t

    return result

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

def sample_nu(period_days, epoch_jd, sma_km, ecc, inc_deg, raan_deg, argp_deg, mean_anom_deg,n_samples=500, prt=False):
    n = PI2 / period_days  # mean motion (rad/days)
    M0 = np.radians(mean_anom_deg)

    if prt:
        print(f"t,x,y,z,vx,vy,vz")
    # Find initial true anomaly at epoch
    nu0 = time_to_true_anomaly(ecc, M0, n, 0.0)

    nu_step = PI2 / n_samples
    nu = nu0
    result = []

    for i in range(n_samples):
        t_jd = true_anomaly_to_time(nu, ecc, M0, n, epoch_jd)
        
        pos_km, vel_kms = true_anomaly_to_state_vectors(sma_km, ecc, inc_deg, raan_deg, argp_deg, np.degrees(nu))
        result.append([t_jd, pos_km, vel_kms])

        if prt:
            print(f"{t_jd},{pos_km[0]},{pos_km[1]},{pos_km[2]},{vel_kms[0]},{vel_kms[1]},{vel_kms[2]}")

        nu = nu + nu_step

    return result

def generate_anisotropic_clouds(state_array, n_particles=100, sigma_cross_km=500000.0, seed=None):
    """
    Generate particle clouds with anisotropic dispersion:
      - Along-track sigma = distance to next point (estimated)
      - Cross-track sigma = user-defined, orthogonal to velocity

    Returns:
        list of (time, particle_positions, velocity)
    """
    rng = np.random.default_rng(seed)
    result = []

    # Estimate along-track distances
    positions = [p for _, p, _ in state_array]
    along_sigmas = []
    for i in range(len(positions)):
        if i == 0:
            d = np.linalg.norm(positions[i + 1] - positions[i])
        elif i == len(positions) - 1:
            d = np.linalg.norm(positions[i] - positions[i - 1])
        else:
            d = 0.5 * (np.linalg.norm(positions[i + 1] - positions[i]) +
                       np.linalg.norm(positions[i] - positions[i - 1]))
        along_sigmas.append(d)

    for (time, pos, vel), sigma_along in zip(state_array, along_sigmas):
        vhat = vel / np.linalg.norm(vel)

        # Create an orthonormal basis: vhat, nhat, bhat
        if np.allclose(vhat, [0, 0, 1]):
            ref = np.array([1, 0, 0])
        else:
            ref = np.array([0, 0, 1])
        nhat = np.cross(vhat, ref)
        nhat /= np.linalg.norm(nhat)
        bhat = np.cross(vhat, nhat)

        # Generate Gaussian noise along these axes
        deltas = (rng.normal(0, sigma_along * 0.5, size=(n_particles, 1)) * vhat +
                  rng.normal(0, sigma_cross_km, size=(n_particles, 1)) * nhat +
                  rng.normal(0, sigma_cross_km, size=(n_particles, 1)) * bhat)
        
        cloud = pos + deltas
        result.append((time, cloud, vel.copy()))

    return result

def set_axes_equal(ax):
    '''Set 3D plot axes to equal scale.'''
    x_limits = ax.get_xlim3d()
    y_limits = ax.get_ylim3d()
    z_limits = ax.get_zlim3d()

    x_range = abs(x_limits[1] - x_limits[0])
    x_middle = np.mean(x_limits)
    y_range = abs(y_limits[1] - y_limits[0])
    y_middle = np.mean(y_limits)
    z_range = abs(z_limits[1] - z_limits[0])
    z_middle = np.mean(z_limits)

    plot_radius = 0.5 * max([x_range, y_range, z_range])

    ax.set_xlim3d([x_middle - plot_radius, x_middle + plot_radius])
    ax.set_ylim3d([y_middle - plot_radius, y_middle + plot_radius])
    ax.set_zlim3d([z_middle - plot_radius, z_middle + plot_radius])

def plot_orbit(points):
    import matplotlib.pyplot as plt

    points.append(points[0])
    points = [point[1] for point in points]
    
    points = np.array(points)
    if points.ndim != 2 or points.shape[1] != 3:
        raise ValueError(f"Expected shape (n, 3), got {points.shape}")

    x, y, z = points[:, 0], points[:, 1], points[:, 2]

    fig = plt.figure()
    ax = fig.add_subplot(111, projection='3d')
    ax.plot(x, y, z, label="Orbit")
    ax.scatter([0], [0], [0], color='orange', label='Central body')
    ax.set_xlabel("x [km]")
    ax.set_ylabel("y [km]")
    ax.set_zlabel("z [km]")
    ax.set_title("3D Orbit")
    ax.legend()

    set_axes_equal(ax)
    plt.show()

def parse_orbital_elements(arg):
    try:
        values = [float(x) for x in arg.split(',')]
        if len(values) != 8:
            raise ValueError("Expected 8 orbital elements")
        keys = ['period', 'epoch', 'sma', 'ecc', 'inc', 'raan', 'argp', 'mean_anom']
        return dict(zip(keys, values))
    except Exception as e:
        raise argparse.ArgumentTypeError(f"Invalid orbital elements: {e}")

# Example usage
def main():
    # Setup argument parser
    parser = argparse.ArgumentParser(description="Generate orbital samples and particle clouds.")
    parser.add_argument("orbital_elements", type=parse_orbital_elements,
                        help="Comma-separated list: period[d],epoch[jd],sma[km],ecc,inc[deg],raan[deg],argp[deg],mean_anom[deg]")
    parser.add_argument('-t', '--sample-time', action='store_true', help="Sample orbital position in time")
    parser.add_argument('-n', '--sample-nu', action='store_true', help="Sample orbital position in true anomaly")
    parser.add_argument('-g', '--generate-clouds', action='store_true', help="Generate anisotropic particle clouds")
    parser.add_argument('--print-orbit', action='store_true', help="Print out orbit sample data to standard output")
    parser.add_argument("--draw-orbit", action='store_true', help="Draw orbit using sampled true anomaly or time")
    parser.add_argument('--print-cloud', action='store_true', help="Print out cloud particle data to standard output")
    parser.add_argument('--n-particles', type=int, default=100, help="Number of particles in the cloud (default: 100)")
    parser.add_argument('--sigma-cross-km', type=float, default=500000.0, help="Cross-track dispersion in km (default: 500000.0 km)")
    parser.add_argument('--n-samples', type=int, default=5000, help="Number of samples to generate for the orbit (default: 5000)")
    args = parser.parse_args()

    # Orbital elements
    elements = args.orbital_elements
    period_days = elements['period']
    epoch_jd = elements['epoch']
    sma_km = elements['sma']  # 1 AU
    ecc = elements['ecc']
    inc_deg = elements['inc']
    raan_deg = elements['raan']
    argp_deg = elements['argp']
    mean_anom_deg = elements['mean_anom']  # at epoch

    # period_days = 48681.19346262312
    # epoch_jd = 2450000.5
    # sma_km = 3903318039.031277657  # 1 AU
    # ecc = 0.963225755046038
    # inc_deg = 113.453816997171
    # raan_deg = 139.3811920815948
    # argp_deg = 152.9821676305871
    # mean_anom_deg = 7.631696167124212  # at epoch
    mu = 1.32712440018e11  # km^3/s^2 (Sun)

    if args.sample_time:
        result = sample_time(period_days, epoch_jd, sma_km, ecc, inc_deg, raan_deg, argp_deg, mean_anom_deg, mu, args.n_samples, args.print_orbit)
    elif args.sample_nu:
        result = sample_nu(period_days, epoch_jd, sma_km, ecc, inc_deg, raan_deg, argp_deg, mean_anom_deg, args.n_samples, args.print_orbit)
    else:
        raise ValueError("You must choose either --sample-time or --sample-nu.")

    if args.draw_orbit:
        plot_orbit(result)

    if args.generate_clouds:
        clouds = generate_anisotropic_clouds(result, n_particles=args.n_particles, sigma_cross_km=args.sigma_cross_km)
    else:
        clouds = []

    # Printing out the result if --print-cloud is enabled
    if args.print_cloud:
        KM_TO_U = 1.0e-6
        print("#X Y Z")
        for t, particles, vel in clouds:
            for p in particles:
                print(' '.join([f"{p[1] * KM_TO_U:.6f}", f"{p[2] * KM_TO_U:.6f}", f"{p[0] * KM_TO_U:.6f}"]))

if __name__ == "__main__":
    main()
    
