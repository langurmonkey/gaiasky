/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.util.Logger.Log;
import org.lwjgl.opengl.ATIMeminfo;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.NVXGPUMemoryInfo;
import org.lwjgl.opengl.WGLAMDGPUAssociation;

public class VMemInfo {
    private static final Log logger = Logger.getLogger(VMemInfo.class);

    private static IGraphicsDeviceInfo graphicsDeviceInfo;
    private static boolean crash = false;

    public static void initialize() {
        String extensions = GlobalResources.getGLExtensions().toLowerCase();
        boolean nvxgpu = extensions.contains("GL_NVX_gpu_memory_info".toLowerCase());
        boolean amdgpuassoc = extensions.contains("WGL_AMD_gpu_association".toLowerCase());
        boolean atimeminfo = extensions.contains("GL_ATI_meminfo".toLowerCase());
        if (nvxgpu) {
            // Nvidia
            try {
                graphicsDeviceInfo = new NVIDIAVRAM();
            } catch (Error e) {
                logger.error(e);
                crash = true;
            }
        } else if (amdgpuassoc || atimeminfo) {
            // AMD
            try {
                graphicsDeviceInfo = new AMDVRAM(amdgpuassoc, atimeminfo);
            } catch (Exception e) {
                logger.error(e);
                crash = true;
            }
        } else {
            // None
            try {
                graphicsDeviceInfo = new NONEVRAM();
            } catch (Exception e) {
                logger.error(e);
                crash = true;
            }
        }
    }

    public static double getFreeMemory() {
        if (!crash && graphicsDeviceInfo == null) {
            initialize();
        }
        return graphicsDeviceInfo == null ? -1 : graphicsDeviceInfo.getFreeMemory();
    }

    public static double getTotalMemory() {
        if (!crash && graphicsDeviceInfo == null) {
            initialize();
        }
        return graphicsDeviceInfo == null ? -1 : graphicsDeviceInfo.getTotalMemory();
    }

    public static double getUsedMemory() {
        if (!crash && graphicsDeviceInfo == null) {
            initialize();
        }
        return graphicsDeviceInfo == null ? -1 : graphicsDeviceInfo.getUsedMemory();
    }

    public interface IGraphicsDeviceInfo {
        double getFreeMemory();

        double getTotalMemory();

        double getUsedMemory();
    }

    private static class NVIDIAVRAM implements IGraphicsDeviceInfo {
        private final int[] buff;
        private final double totalMem;

        public NVIDIAVRAM() {
            buff = new int[1];
            totalMem = computeTotalMemory();
        }

        public double getFreeMemory() {
            GL20.glGetIntegerv(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX, buff);
            double freeMem = buff[0] * 1e-3d;
            return freeMem;
        }

        public double computeTotalMemory() {
            GL20.glGetIntegerv(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX, buff);
            double totMem = buff[0] * 1e-3d;
            return totMem;
        }

        public double getTotalMemory() {
            return totalMem;
        }

        public double getUsedMemory() {
            return getTotalMemory() - getFreeMemory();
        }
    }

    private static class AMDVRAM implements IGraphicsDeviceInfo {
        private final int[] buff;
        private final double totalMem;
        private final boolean gpuassoc;
        private final boolean meminfo;

        public AMDVRAM(boolean gpuassoc, boolean meminfo) {
            this.buff = new int[4];
            this.gpuassoc = gpuassoc;
            this.meminfo = meminfo;
            totalMem = computeTotalMemory();
        }

        public double getFreeMemory() {
            if (meminfo) {
                GL20.glGetIntegerv(ATIMeminfo.GL_VBO_FREE_MEMORY_ATI, buff);
                double freeMem = buff[0] * 1e-3d;
                return freeMem;
            }
            return -1;
        }

        public double computeTotalMemory() {
            if (gpuassoc) {
                int n = WGLAMDGPUAssociation.nwglGetGPUIDsAMD(0, 0);
                int[] ids = new int[n];
                int res = WGLAMDGPUAssociation.wglGetGPUIDsAMD(ids);
                if (res > 0) {
                    WGLAMDGPUAssociation.wglGetGPUInfoAMD(ids[0], WGLAMDGPUAssociation.WGL_GPU_RAM_AMD, GL20.GL_UNSIGNED_INT, buff);
                    return buff[0] * 1e-3d;
                }
            }
            return -1;
        }

        public double getTotalMemory() {
            return totalMem;
        }

        public double getUsedMemory() {
            if (gpuassoc && meminfo) {
                return getTotalMemory() - getFreeMemory();
            }
            return -1;
        }
    }

    private static class NONEVRAM implements IGraphicsDeviceInfo {

        @Override
        public double getFreeMemory() {
            return -1;
        }

        @Override
        public double getTotalMemory() {
            return -1;
        }

        @Override
        public double getUsedMemory() {
            return -1;
        }
    }

}
