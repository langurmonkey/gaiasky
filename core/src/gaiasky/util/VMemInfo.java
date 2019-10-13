/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import org.lwjgl.opengl.*;

public class VMemInfo {

    private static IGraphicsDeviceInfo graphicsDeviceInfo;

    public static void initialize() {
        String extensions = GlobalResources.getGLExtensions();
        if (extensions.contains("GL_NVX_gpu_memory_info")) {
            // Nvidia
            graphicsDeviceInfo = new NVIDIAVRAM();
        } else if (extensions.contains("GL_ATI_meminfo")) {
            // ATI
            graphicsDeviceInfo = new ATIVRAM();
        } else {
            // None
            graphicsDeviceInfo = new NONEVRAM();
        }
    }

    public static double getFreeMemory() {
        if (graphicsDeviceInfo == null) {
            initialize();
        }
        return graphicsDeviceInfo.getFreeMemory();
    }

    public static double getTotalMemory() {
        if (graphicsDeviceInfo == null) {
            initialize();
        }
        return graphicsDeviceInfo.getTotalMemory();
    }

    public static double getUsedMemory() {
        if (graphicsDeviceInfo == null) {
            initialize();
        }
        return graphicsDeviceInfo.getUsedMemory();
    }

    public interface IGraphicsDeviceInfo {
        double getFreeMemory();

        double getTotalMemory();

        double getUsedMemory();
    }

    private static class NVIDIAVRAM implements IGraphicsDeviceInfo {
        private int[] buff;
        private double totalMem;

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

    private static class ATIVRAM implements IGraphicsDeviceInfo {
        private int[] buff;
        private double totalMem;

        public ATIVRAM() {
            buff = new int[4];
            totalMem = computeTotalMemory();
        }

        public double getFreeMemory() {
            GL20.glGetIntegerv(ATIMeminfo.GL_VBO_FREE_MEMORY_ATI, buff);
            double freeMem = buff[0] * 1e-3d;
            return freeMem;
        }

        public double computeTotalMemory() {
            int n = WGLAMDGPUAssociation.nwglGetGPUIDsAMD(0, 0);
            int[] ids = new int[n];
            int res = WGLAMDGPUAssociation.wglGetGPUIDsAMD(ids);
            if (res > 0) {
                WGLAMDGPUAssociation.wglGetGPUInfoAMD(ids[0], WGLAMDGPUAssociation.WGL_GPU_RAM_AMD, GL20.GL_UNSIGNED_INT, buff);
                return buff[0] * 1e-3d;
            }
            return 0;
        }

        public double getTotalMemory() {
            return totalMem;
        }

        public double getUsedMemory() {
            return getTotalMemory() - getFreeMemory();
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
