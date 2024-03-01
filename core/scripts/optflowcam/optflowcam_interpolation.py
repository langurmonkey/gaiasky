# Original source from Lisa Piotrowski et. al., licensed under GPLv3.0.
# https://github.com/LivelyLiz/OptFlowCam
#
# Paper: Piotrowski, Motejat, Roessl, Theisel
#        OptFlowCam: A 3D-Image-Flow-Based Metric in Camera Space for Camera Paths
#        in Scenes with Extreme Scale Variations. Eurographics 2024
# 
# Modifications by Toni Sagrista:
# - add target location parsing (targets --points of interest-- added to keyframes file format).
# - add frame rate as argument.
# - clean up code (non ascii characters, unused functions, etc.).

import numpy as np

import bisect
import copy
from functools import partial
from multiprocessing.pool import Pool
from collections import namedtuple

AxisAngle = namedtuple("AxisAngle", "axis angle")

def get_zoom_pan_parameter_functions(w0: float, w1: float,
                                     u0: float, u1: float,
                                     rho: float=np.sqrt(2)) -> tuple:
    '''
    Returns the parameter functions u and w as well as the path length S in the
    zoom and panning metric. With rho=sqrt(2), this is equivalent to the
    formulas we used in our paper.
    '''
    # see
    # J. J. van Wijk and W. A. A. Nuij, "Smooth and efficient zooming and panning"
    # in IEEE Symposium on Information Visualization 2003 (IEEE Cat. No.03TH8714), 2003-10, pp. 15-23. 
    # doi: 10.1109/INFVIS.2003.1249004.
  
    # no panning
    if np.isclose(u1 - u0, 0, atol=1e-14):
        us = lambda s: u0

        # also no zooming
        if np.isclose(w1-w0, 0, atol=1e-14):
            return us, lambda s: w0, 1
        
        S = np.abs(np.log(w1/w0))/rho

        k = -1 if w1 < w0 else 1
        ws = lambda s: w0 * np.exp(k*rho*s)

        return us, ws, S

    # avoid ws being close to 0
    # because of numerical issues
    w0 = max(w0, 1e-6)
    w1 = max(w1, 1e-6)

    bi = lambda i: (w1**2 - w0**2 + (-1)**i * rho**4 * (u1-u0)**2)/(2 * [w0, w1][i] * rho**2 * (u1-u0))
    def ri(bi):
        if np.abs(bi) > 1e6:
            return -np.sign(bi)*np.log( 2*np.abs(bi))
        else:
            return np.log(-bi + np.sqrt(bi**2 + 1))

    b0 = bi(int(0))
    b1 = bi(int(1))

    r0 = ri(b0)
    r1 = ri(b1)

    assert not np.isnan(r0) and not np.isnan(r1)

    S = (r1 - r0)/rho
    
    us = lambda s: w0/(rho**2) * np.cosh(r0) * np.tanh(rho*s + r0) - w0/(rho**2) * np.sinh(r0) + u0
    ws = lambda s: w0 * np.cosh(r0)/np.cosh(rho*s + r0)

    return us, ws, S

def get_zoom_pan_parameter(t: float, w0: float, w1: float, 
                                     u0: float, u1: float, 
                                     rho: float=np.sqrt(2)) -> tuple:
    '''
    Returns the parameter u and w for the path parameter t, where t0=0
    gives w0 and u0 and t1=1 gives w1 and u1.
    '''

    us, ws, S = get_zoom_pan_parameter_functions(w0, w1, u0, u1, rho)
    return us(t*S), ws(t*S)

def get_zoom_pan_parameters(n: int, w0: float, w1: float, 
                            u0: float, u1: float, 
                            rho: float=np.sqrt(2)) -> tuple:
    '''
    Returns the parameter list for u and w for the range [0,1] divided 
    into n samples.
    '''

    us, ws, S = get_zoom_pan_parameter_functions(w0, w1, u0, u1, rho)

    ss = np.linspace(0, S, n)
    return us(ss), ws(ss)


def unpack_camera(cam: dict) -> tuple:
    '''
    Returns an orthonormal basis for the given camera configuration.

    Input camera object needs to have a view and up vector.
    '''

    pos = np.array(cam["position"])
    s = cam["frustum_scale"]
    view, up, right = get_orthonormal_basis(cam)

    return pos, view, up, right, s

def normalized(a):
    '''
    Returns a normalized ndarray and prints a warning
    if the norm is close to zero
    '''
    
    l2 = np.linalg.norm(a)
    if l2 < 1e-15:
        print("Norm near zero for ", a)
        return a
    return a / l2

def get_orthonormal_basis(cam: dict) -> tuple:
    '''
    Returns an orthonormal basis for the given camera configuration.

    Input camera object needs to have a view and up vector.
    '''

    view = normalized(np.array(cam["view"]))
    up = normalized(np.array(cam["up"]))
    right = normalized(np.cross(up, view))
    up = normalized(np.cross(view, right))

    assert np.all([not np.isclose(np.linalg.norm(v), 0) for v in [view, up, right]]), "Given vectors do not form a basis for camera orientation!"

    return view, up, right

def get_rotation(start: dict, end: dict) -> tuple:
    vecs1 = [start[key] for key in ["view", "right", "up"]]
    vecs2 = [end[key] for key in ["view", "right", "up"]]

    # basis matrix for start
    R1 = np.array([vecs1[1], vecs1[2], vecs1[0]]).T
    R2 = np.array([vecs2[1], vecs2[2], vecs2[0]]).T

    # rotation matrix around z-axis
    R_beta = lambda t, beta: np.array([[np.cos(t*beta), -np.sin(t*beta), 0.0], 
                                       [np.sin(t*beta),  np.cos(t*beta), 0.0], 
                                       [0.0, 0.0, 1.0]], dtype=float)

    axis = None
    beta_end = None

    eigenvals, eigenvecs = np.linalg.eig(R2.T-R1.T)

    # tolerance has to be so high because some cases produce eigenvalues that are not zero
    indices = np.where(np.isclose(eigenvals.real,0, atol=1e-7))
    if len(indices[0]) == 0:
        raise ValueError("Cannot determine axis of rotation")
    
    axis = eigenvecs[:,indices[0][0]].flatten()
    
    # need to normalize in case there was an imaginary part and
    # the real part alone is not unit length anymore
    axis = normalized( axis.real )

    # in case the chosen axis is identical to the view vector
    # we need to determine the rest of the matrix with another vector
    if np.isclose(np.linalg.norm(np.cross(axis, vecs1[0])), 0):
        c1 = normalized( np.cross(vecs1[1], axis) )
        c2 = normalized( np.cross(axis, c1) )
    else:
        c2 = normalized( np.cross(axis, vecs1[0]) )
        c1 = normalized( np.cross(c2, axis) )

    # matrix that rotates "axis" so that it is aligned with the z-axis
    # and v1 so that its y-coordinate is zero
    R_axis = np.array([c1, c2, axis]).T

    # can get angle of rotation by determining the angle of the transformed vector
    # projected to xy plane with the x-axis
    h = R_axis.T @ vecs2[0]

    # get the angle of rotation
    if beta_end == None:
        beta_end = np.arctan2(h[1], h[0])

    # interpolating rotation matrix
    R_f = lambda t: R_axis @ R_beta(t, beta_end) @ R_axis.T @ R1

    return AxisAngle(axis, beta_end), R_f

def interpolate_t(t: float, focal: float, metric="3DImageFlow", **kwargs) -> dict:
    if "start" in kwargs and "end" in kwargs:
        start = kwargs["start"]
        end = kwargs["end"]

        # assure orthonormal camera reference frame
        pos1, view1, up1, right1, s1 = unpack_camera(start)
        pos2, view2, up2, right2, s2 = unpack_camera(end)
    
    else:
        for arg in ["pos1", "view1", "up1", "right1", "s1", "pos2", "view2", "up2", "right2", "s2"]:
            if not arg in kwargs:
                raise ValueError(f"Function 'interpolate_t' needs keyword argument '{arg}' if no 'start' and 'end' are given.")

        pos1 = kwargs["pos1"]
        view1 = kwargs["view1"]
        up1 = kwargs["up1"]
        right1 = kwargs["right1"]
        s1 = kwargs["s1"]

        pos2 = kwargs["pos2"]
        view2 = kwargs["view2"]
        up2 = kwargs["up2"]
        right2 = kwargs["right2"]
        s2 = kwargs["s2"]

    if not "R_f" in kwargs:
        _, R_f = get_rotation({"view":view1, "up":up1, "right": right1},
                          {"view":view2, "up":up2, "right": right2})
    else:
        R_f =  kwargs["R_f"]

    # interpolating look at points
    look1 = pos1 + s1*focal*view1
    look2 = pos2 + s2*focal*view2

    look_diff = look2 - look1

    if metric == "LookatLinear":
        pos = (1-t)*pos1 + t*pos2
        look = (1-t)*look1 + t*look2
        view = normalized(look - pos)
        up = normalized((1-t)*up1 + t*up2)

        cam = {
            "position" : pos.tolist(),
                "view" : view.tolist(),
                  "up" : up.tolist(),
       "frustum_scale" : np.linalg.norm(look - pos)/focal,
               "focal" : focal
            }

        return cam

    elif metric == "TransformationsLinear":
        u = t
        w = (1-t)*s1 + t*s2

        cam = cam_from_params(u, w, R_f(t), focal, look1, look_diff)
        return cam

    elif metric == "3DImageFlow":
        rho = kwargs["rho"]

        w0 = s1; w1 = s2
        u0 = 0;  u1 = np.linalg.norm(look_diff)

        look_diff_n = np.zeros(3) if u1 < 1e-14 else normalized(look_diff)

        u, w = get_zoom_pan_parameter(t, w0, w1, u0, u1, rho)
        cam = cam_from_params(u, w, R_f(t), focal, look1, look_diff_n)

        return cam

    else:
        raise ValueError(f"Unknown metric {metric}")

def cam_from_params(u: float, w: float, 
                    R: np.ndarray, focal: float, 
                    lookat_pos: np.ndarray, lookat_dir: np.ndarray) -> dict:
    up = R[:,1]
    view = R[:,2]
    scale = w

    lookat = lookat_pos + u*lookat_dir
    pos = lookat - scale*focal*view

    cam = {
        "position" : pos.tolist(),
            "view" : view.tolist(),
              "up" : up.tolist(),
  "frustum_scale" : scale,
           "focal" : focal
    }
    return cam

def interpolate_simple(start: dict, end: dict,
                       focal: float, metric="3DImageFlow", 
                       n: int=101, **kwargs) -> list:
    '''
    Simply interpolate between a start and end camera with n sample points
    according to the given metric.

    kwargs should contain the parameter rho if metric==3DImageFlow.
    '''

    cams = [interpolate_t(t, focal, metric, 
                          start=start, end=end, 
                          **kwargs)
                for t in np.linspace(0, 1, n)]

    return cams

def interpolate_deCasteljau(t: float, control_points: list, 
                            focal: float, metric: str, **kwargs) -> dict:

    if len(control_points) == 1:
        return control_points[0]

    new_control_points = [interpolate_t(t, focal, metric, start=cp, end=control_points[i+1], **kwargs) 
                          for i, cp in enumerate(control_points[:-1])]
    
    return interpolate_deCasteljau(t, new_control_points, focal, metric, **kwargs)

def interpolate_bezier(control_points: list, focal: float, metric: str, n: int=101, **kwargs) -> list:

    if len(control_points) == 2:
        return interpolate_simple(control_points[0], control_points[1], focal, metric, n, **kwargs)

    cams = [interpolate_deCasteljau(t, control_points, focal, metric, **kwargs)[0] 
            for t in np.linspace(0, 1, n)]
    
    return cams

def get_segment_index(t, knots):
    segment_index = bisect.bisect_left(knots, t) - 1
    return segment_index

def interpolate_CatmullRom(t: float, control_points: list, knots: list,
                           focal: float, metric="3DImageFlow", **kwargs)->dict:

    assert len(control_points) == len(knots), f"Number of control points and number of knots do not match"

    if len(control_points) == 1:
        return control_points[0]

    if t <= knots[0]:
        return control_points[0]
    if t >= knots[-1]:
        return control_points[-1]

    segment_index = get_segment_index(t, knots) 

    cpy_index_min = np.max([segment_index-1, 0])
    cpy_index_max = np.min([segment_index+3, len(control_points)])

    segment_control_points = copy.deepcopy(control_points[cpy_index_min:cpy_index_max])
    segment_knots = copy.deepcopy(knots[cpy_index_min:cpy_index_max])

    # need additional control points at the start and end to do Catmull-Rom
    if segment_index == 0:
        augmentation_point = copy.deepcopy(control_points[0])
        segment_control_points.insert(0, augmentation_point)

        augmentation_knot = -knots[1]+2*knots[0]
        segment_knots = np.insert(segment_knots, 0, augmentation_knot)

    if segment_index >= len(knots)-2:
        augmentation_point = copy.deepcopy(control_points[-1])
        segment_control_points.append(augmentation_point)

        augmentation_knot = -knots[-2] + 2*knots[-1]
        segment_knots = np.append(segment_knots, augmentation_knot)

    assert len(segment_control_points) == 4 and len(segment_knots) == 4, f"Should be 4 but is {len(segment_control_points)} and {len(segment_knots)}"

    # see
    # C. Yuksel, S. Schaefer, and J. Keyser, “On the parameterization of Catmull-Rom curves,” 
    # in 2009 SIAM/ACM Joint Conference on Geometric and Physical Modeling, 2009-10. 
    # doi: 10.1145/1629255.1629262.
    for j in range(3):
        for i in range(len(segment_control_points)-j-1):
            if j <= 1:
                s = (t - segment_knots[i])/(segment_knots[i+1+j] - segment_knots[i])
            else:
                s = (t - segment_knots[1])/(segment_knots[2] - segment_knots[1])

            cam = interpolate_t(s, focal, metric, start=segment_control_points[i], end=segment_control_points[i+1], **kwargs)
            segment_control_points[i] = cam

    return segment_control_points[0]

def disambiguate_spline(control_points: list, knots: list, spline: list):
    """
    Inserts new control points and knots to resolve any gaps/discontinuities
    that may be present in the spline.

    Depending on the original control points, this function may not detect
    the discontinuities reliably or detect some that are not present.
    """

    # this could be further improved because the current implementation
    # is prone to false positives/negatives due to absolute tolerances

    spline_positions = [np.array(c["position"]) for c in spline]

    last_fixed_segment = -1

    new_control_points = copy.deepcopy(control_points)
    new_knots = np.copy(knots)

    for i in range(1, len(spline)-2):

        t = i/(len(spline)+1)    
        segment_index = get_segment_index(t, knots)

        # insert a maximum of one control point per segment
        # to avoid forcing the path to join
        # incompatible segments
        if last_fixed_segment == segment_index:
            continue

        di_1 = spline_positions[i] - spline_positions[i-1]
        di   = spline_positions[i+1] - spline_positions[i]
        di1  = spline_positions[i+2] - spline_positions[i+1]

        norm_di_1 = np.linalg.norm(di_1)
        norm_di   = np.linalg.norm(di)
        norm_di1  = np.linalg.norm(di1)

        dot_di_di_1 = np.dot(di_1, di)/(norm_di*norm_di_1) 
        dot_di_di1  = np.dot(di1, di)/(norm_di*norm_di1)

        angle_discontinuous = (not np.isclose(dot_di_di_1, 1, atol=0.2) and \
                               not np.isclose(dot_di_di1, 1, atol=0.2))

        length_discontinuous = (not np.isclose(norm_di, norm_di_1, rtol=0.2) and \
                                not np.isclose(norm_di, norm_di1, rtol=0.2) and \
                                not np.isclose(norm_di, 0       , atol=0.01))


        if angle_discontinuous or length_discontinuous: 
        
            print(f"i:{i} t:{t} segment:{segment_index} angle:{angle_discontinuous} length:{length_discontinuous}")
            print(f"cos angle di-1 and di: {dot_di_di_1} di+1 and di: {dot_di_di1}")
            print(f"norms di-1: {norm_di_1} di: {norm_di} di+1: {norm_di1}")

            if np.any(map(lambda knot: np.isclose(t, knot), new_knots)):
                # if t is close to a knot (or the same) inserting
                # the new point can cause problems when computing the
                # spline and is probably a false positive
                continue

            cam = spline[i]

            new_segment_index = get_segment_index(t, new_knots)
        
            new_control_points.insert(segment_index+1, cam)
            new_knots = np.insert(new_knots, segment_index+1, t)

            last_fixed_segment = segment_index

    return new_control_points, new_knots

def interpolate_keyframes(control_points: list, knots: list, 
                          focal: float, method="CatmullRom", metric="3DImageFlow",
                          n: int=101, **kwargs) -> list:

    if metric == "3DImageFlow" and not "rho" in kwargs:
        kwargs["rho"] = np.sqrt(2)
        print("No named argument rho given for metric 3DImageFlow. Using default parameter sqrt(2).")

    if len(control_points) != len(knots):
        raise ValueError("Number of control points and number of knots are not equal")    
        
    if len(control_points) == 2:
        return interpolate_simple(control_points[0], control_points[1], focal, metric, n, **kwargs)

    cams = [None]*n
        
    normalized_knots = np.array(copy.deepcopy(knots))
    normalized_knots -= knots[0]
    normalized_knots = normalized_knots / (knots[-1] - knots[0])

    if method == "Linear":
        cams = []
        for i, knot in enumerate(knots[:-1]):
            nn = knots[i+1] - knots[i]
            cams.extend(interpolate_simple(control_points[i], control_points[i+1], focal, metric, nn, **kwargs))
        return cams

    with Pool() as pool:
        if method == "CatmullRom":
            n_control_points = len(control_points)
            cams = pool.map(partial(interpolate_CatmullRom, control_points=control_points, knots=normalized_knots, 
                                                        focal=focal, metric=metric, **kwargs),
                                                        np.linspace(0, 1, n))

            for i in range(4):
                control_points, normalized_knots = disambiguate_spline(control_points, normalized_knots, cams)
                if n_control_points == len(control_points):
                    break
                
                n_control_points = len(control_points)
                cams = pool.map(partial(interpolate_CatmullRom, control_points=control_points, knots=normalized_knots, 
                                                            focal=focal, metric=metric, **kwargs),
                                                            np.linspace(0, 1, n))

        elif method == "Bezier":
            cams = pool.map(partial(interpolate_deCasteljau, control_points=control_points, focal=focal,
                                                        metric=metric, **kwargs),
                                                        np.linspace(0, 1, n))
        else:
            raise ValueError(f"Method {method} unknown")

        pool.close()
        pool.join()

    return cams
