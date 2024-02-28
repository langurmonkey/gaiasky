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

from collections import namedtuple
import copy
from functools import partial
from multiprocessing.pool import Pool

AxisAngle = namedtuple("AxisAngle", "axis angle")

def get_zoom_pan_parameter_functions(w0, w1, u0, u1, rho):
    # see
    # J. J. van Wijk and W. A. A. Nuij, "Smooth and efficient zooming and panning"
    # in IEEE Symposium on Information Visualization 2003 (IEEE Cat. No.03TH8714), 2003-10, pp. 15–23. 
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

def get_zoom_pan_parameter(w0, w1, u0, u1, rho, t):

    #assert 0 <= t and t <= 1 + 1e-14

    us, ws, S = get_zoom_pan_parameter_functions(w0, w1, u0, u1, rho)
    return us(t*S), ws(t*S)

def get_zoom_pan_parameters(w0, w1, u0, u1, rho, n):

    us, ws, S = get_zoom_pan_parameter_functions(w0, w1, u0, u1, rho)

    ss = np.linspace(0, S, n)
    return us(ss), ws(ss)

def smooth_zoom_pan(start=[0,0,1], end=[1,0,1], n=101, rho=np.sqrt(2)):
    dir = np.array(end) - np.array(start)
    dist = np.linalg.norm(dir)
    dir = dir/dist

    w0 = start[2]; w1 = end[2]
    u0 = 0; u1 = dist

    u, w = get_zoom_pan_parameters(w0, w1, u0, u1, rho, n)    

    xs = start[0] + u*dir[0]
    ys = start[1] + u*dir[1]
    zs = w

    return (xs, ys, zs)


def normalized(a):
    l2 = np.linalg.norm(a)
    if l2 < 1e-15:
        print("Norm near zero for ", a)
        return a
    return a / l2

def get_orthonormal_basis(cam: dict) -> tuple:
    pos = np.array(cam["position"])
    s = cam["frustum_scale"]
    view = normalized(np.array(cam["view"]))
    up = normalized(np.array(cam["up"]))
    right = normalized(np.cross(up, view))
    up = normalized(np.cross(view, right))

    assert np.all([not np.isclose(np.linalg.norm(v), 0) for v in [view, up, right]]), "Given vectors do not form a basis for camera orientation!"

    return pos, view, up, right, s

def get_look_at_point(cam: dict) -> list:
    pos, view, _, _, s = get_orthonormal_basis(cam)
    focal = cam["focal"]

    look = pos + s*focal*view
    return look

def get_rotation_keyframe(start: dict, end: dict):
    _, view1, up1, right1, _ = get_orthonormal_basis(start)
    _, view2, up2, right2, _ = get_orthonormal_basis(end)
    
    axisangle, _ = get_rotation({"view":view1, "up":up1, "right": right1},
                             {"view":view2, "up":up2, "right": right2})

    return axisangle

def get_rotation_angle(start: dict, end: dict) -> float:
    _, view1, up1, right1, _ = get_orthonormal_basis(start)
    _, view2, up2, right2, _ = get_orthonormal_basis(end)
    
    axisangle, _ = get_rotation({"view":view1, "up":up1, "right": right1},
                             {"view":view2, "up":up2, "right": right2})

    return axisangle.angle

def get_rotation(start: dict, end: dict, compatibility: AxisAngle = None) -> tuple:
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

    if compatibility != None:
        if np.dot(axis, compatibility.axis) < 0 and np.sign(-beta_end) != np.sign(compatibility.angle):
            if np.sign(compatibility.angle) > 0:
                beta_end -= 2*np.pi
            else:
                beta_end += 2*np.pi
        elif np.dot(axis, compatibility.axis) > 0 and np.sign(beta_end) != np.sign(compatibility.angle):
            if np.sign(compatibility.angle) > 0:
                beta_end += 2*np.pi
            else:
                beta_end -= 2*np.pi

    # interpolating rotation matrix
    R_f = lambda t: R_axis @ R_beta(t, beta_end) @ R_axis.T @ R1

    return AxisAngle(axis, beta_end), R_f

def cam_from_params(u, w, R, focal, lookat_pos, lookat_dir)->dict:
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

def frustum_transform_simple(start: dict, end: dict, focal: float, n: int=101, rho:float=np.sqrt(2), metric="3DImageFlow") -> list:
    '''
    Uses the idea of smooth zooming and panning but extended to 3D. 
    We smoothly transform one camera frustum (approximation) to another frustum
    so that the optical flow in the frustum volume is minimal.

    ### Arguments:
        - start ... start point given as a dictionary with position, view, up vector and frustum scale
        - end   ... end point given as a dictionary with position, view, up vector and frustum scale
        - focal ... camera focal length
        - n     ... number of points on the path
        - rho   ... parameter that influences how much the camera will move away (same as in smooth_zoom_pan)
    '''

    # assure orthonormal camera reference frame
    pos1, view1, up1, right1, s1 = get_orthonormal_basis(start)
    pos2, view2, up2, right2, s2 = get_orthonormal_basis(end)
    
    _, R_f = get_rotation({"view":view1, "up":up1, "right": right1},
                          {"view":view2, "up":up2, "right": right2})

    # interpolating look at points
    look1 = pos1 + s1*focal*view1
    look2 = pos2 + s2*focal*view2

    look_diff = look2 - look1

    cams = [None]*n

    if metric == "LookatLinear":
        for i, t in enumerate(np.linspace(0, 1, n)):
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

            cams[i] = cam

    elif metric == "TransformationsLinear":
        for i, t in enumerate(np.linspace(0, 1, n)):
            u = t
            w = (1-t)*s1 + t*s2

            cam = cam_from_params(u, w, R_f(t), focal, look1, look_diff)
            cams[i] = cam

    elif metric == "3DImageFlow":    
        w0 = s1; w1 = s2
        u0 = 0;  u1 = np.linalg.norm(look_diff)

        look_diff_n = np.zeros(3) if u1 < 1e-14 else normalized(look_diff)

        us, ws = get_zoom_pan_parameters(w0, w1, u0, u1, rho, n)
        cams = [cam_from_params(us[i], ws[i], R_f(t), focal, look1, look_diff_n) for i, t in enumerate(np.linspace(0, 1, n))]

    else:
        raise ValueError(f"Unknown metric {metric}")

    return cams

def frustum_transform_t(start: dict, end: dict, focal: float, t: float, rho:float=np.sqrt(2), metric="3DImageFlow", rot_compat:AxisAngle = None) -> dict:
    # assure orthonormal camera reference frame
    pos1, view1, up1, right1, s1 = get_orthonormal_basis(start)
    pos2, view2, up2, right2, s2 = get_orthonormal_basis(end)
    
    axisangle, R_f = get_rotation({"view":view1, "up":up1, "right": right1},
                          {"view":view2, "up":up2, "right": right2},
                          rot_compat)

    # interpolating look at points
    look1 = pos1 + s1*focal*view1
    look2 = pos2 + s2*focal*view2

    look_diff = look2 - look1

    if metric == "LookatLinear":
        pos = (1-t)*pos1 + t*pos2
        view = normalized((1-t)*look1 + t*look2 - pos)
        up = normalized((1-t)*up1 + t*up2)

        cam = {
        "position" : pos.tolist(),
            "view" : view.tolist(),
              "up" : up.tolist(),
  "frustum_scale" : np.linalg.norm(view),
           "focal" : focal
        }

        return cam

    elif metric == "TransformationsLinear":
        u = t
        w = (1-t)*s1 + t*s2

        cam = cam_from_params(u, w, R_f(t), focal, look1, look_diff)
        return cam

    elif metric == "3DImageFlow":

        w0 = s1; w1 = s2
        u0 = 0;  u1 = np.linalg.norm(look_diff)

        look_diff_n = np.zeros(3) if u1 < 1e-14 else normalized(look_diff)

        u, w = get_zoom_pan_parameter(w0, w1, u0, u1, rho, t)
        cam = cam_from_params(u, w, R_f(t), focal, look1, look_diff_n)

        return cam

    else:
        raise ValueError(f"Unknown metric {metric}")

def frustum_deCasteljau(t: float, control_points: list, focal: float, rho:float=np.sqrt(2), metric="3DImageFlow")->dict:

    if len(control_points) == 1:
        return control_points[0]

    new_control_points = []
    for i, cp in enumerate(control_points[:-1]):
        new_control_points.append(frustum_transform_t(cp, control_points[i+1], focal, t, rho, metric))
    
    return frustum_deCasteljau(t, new_control_points, focal, rho)

def frustum_transform_bezier(control_points: list, focal: float, n: int=101, rho:float=np.sqrt(2), metric="3DImageFlow") -> list:

    if len(control_points) == 2:
        return frustum_transform_simple(control_points[0], control_points[1], focal, n, rho, metric)

    cams = [None]*n
    for i,t in enumerate(np.linspace(0, 1, n)):
        point = frustum_deCasteljau(t, control_points, focal, rho, metric)
        cams[i] = point[0]
    return cams

def get_segment_index(t, knots):
    # could use binary search here but
    # knot array is most likely small
    segment_index = 0
    for knot in knots[1:]:
        if knot <= t:
            segment_index += 1

    return segment_index

def frustum_CatmullRom(t: float, control_points: list, knots: list, focal: float, rho:float=np.sqrt(2), metric="3DImageFlow")->dict:

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

            cam = frustum_transform_t(segment_control_points[i], segment_control_points[i+1], focal, s, rho, metric)
            segment_control_points[i] = cam

    return segment_control_points[0]

def disambiguate_spline(control_points: list, knots: list, spline: list):
    """
    If spline has jumps/discontinuities in it, we need to fix the spline
    by inserting additional control points and knots.
    """

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
                                not np.isclose(norm_di, norm_di1, rtol=0.2))

        if angle_discontinuous or length_discontinuous: 
        
            print(f"i:{i} t:{t} segment:{segment_index} angle:{angle_discontinuous} length:{length_discontinuous}")

            knot = t

            if np.any(map(lambda knot: np.isclose(t, knot), knots)):
                # if t is close to a knot (or the same) inserting
                # the new point can cause problems when computing the
                # spline
                continue

            cam = spline[i]
        
            new_control_points.insert(segment_index+1, cam)
            new_knots = np.insert(new_knots, segment_index+1, knot)

            last_fixed_segment = segment_index

    return new_control_points, new_knots

def frustum_transform_spline(control_points: list, knots: list, focal: float, n: int=101, rho:float=np.sqrt(2), method="CatmullRom", metric="3DImageFlow") -> list:

    if len(control_points) != len(knots):
        raise ValueError("Number of control points and number of knots are not equal")    
        
    if len(control_points) == 2:
        return frustum_transform_simple(control_points[0], control_points[1], focal, n, rho, metric)

    cams = [None]*n
        
    normalized_knots = np.array(copy.deepcopy(knots))
    normalized_knots -= knots[0]
    normalized_knots = normalized_knots / (knots[-1] - knots[0])

    if method == "Linear":
        cams = []
        for i, knot in enumerate(knots[:-1]):
            nn = knots[i+1] - knots[i]
            cams.extend(frustum_transform_simple(control_points[i], control_points[i+1], focal, nn, rho, metric))
        return cams

    pool = Pool()
    if method == "CatmullRom":
        n_control_points = len(control_points)
        cams = pool.map(partial(frustum_CatmullRom, control_points=control_points, knots=normalized_knots, focal=focal, rho=rho, metric=metric), np.linspace(0, 1, n))

        for i in range(4):
            print(i)

            control_points, normalized_knots = disambiguate_spline(control_points, normalized_knots, cams)
            if n_control_points == len(control_points):
                break
            
            n_control_points = len(control_points)
            cams = pool.map(partial(frustum_CatmullRom, control_points=control_points, knots=normalized_knots, focal=focal, rho=rho, metric=metric), np.linspace(0, 1, n))

    elif method == "Bezier":
        cams = pool.map(partial(frustum_deCasteljau, control_points=control_points, focal=focal, rho=rho, metric=metric), np.linspace(0, 1, n))
    else:
        pool.close()
        raise ValueError(f"Method {method} unknown")

    pool.close()
    pool.join()

    return cams
