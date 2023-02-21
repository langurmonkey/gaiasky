/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.vr.openvr;

import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import org.lwjgl.openvr.OpenVR;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.openvr.VR.*;
import static org.lwjgl.openvr.VRSystem.VRSystem_GetRecommendedRenderTargetSize;
import static org.lwjgl.openvr.VRSystem.VRSystem_GetStringTrackedDeviceProperty;
import static org.lwjgl.system.MemoryStack.stackPush;

public class OpenVRManager {
    private static final Log logger = Logger.getLogger(OpenVRManager.class);

    public static void queryOpenVr() {
        logger.info("==== Querying OpenVR status ====");
        logger.info("OpenVR runtime installed: " + VR_IsRuntimeInstalled());
        logger.info("OpenVR runtime path: " + VR_RuntimePath());
        logger.info("HMD present: " + VR_IsHmdPresent());

        try (MemoryStack stack = stackPush()) {
            IntBuffer peError = stack.mallocInt(1);

            int token = VR_InitInternal(peError, 0);
            if (peError.get(0) == 0) {
                try {
                    OpenVR.create(token);

                    logger.info("Model Number : " + VRSystem_GetStringTrackedDeviceProperty(k_unTrackedDeviceIndex_Hmd, ETrackedDeviceProperty_Prop_ModelNumber_String, peError));
                    logger.info("Serial Number: " + VRSystem_GetStringTrackedDeviceProperty(k_unTrackedDeviceIndex_Hmd, ETrackedDeviceProperty_Prop_SerialNumber_String, peError));

                    IntBuffer w = stack.mallocInt(1);
                    IntBuffer h = stack.mallocInt(1);
                    VRSystem_GetRecommendedRenderTargetSize(w, h);
                    logger.info("Recommended width : " + w.get(0));
                    logger.info("Recommended height: " + h.get(0));
                } finally {
                    VR_ShutdownInternal();
                }
            } else {
                logger.error("INIT ERROR SYMBOL: " + VR_GetVRInitErrorAsSymbol(peError.get(0)));
                logger.error("INIT ERROR  DESCR: " + VR_GetVRInitErrorAsEnglishDescription(peError.get(0)));
            }
        }
    }

}
