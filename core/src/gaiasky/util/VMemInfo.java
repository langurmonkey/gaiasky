/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.util.Logger.Log;
import org.lwjgl.opengl.ATIMeminfo;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.NVXGPUMemoryInfo;
import org.lwjgl.opengl.WGLAMDGPUAssociation;

public class VMemInfo {
    private static Log logger = Logger.getLogger(VMemInfo.class);

    private static IGraphicsDeviceInfo graphicsDeviceInfo;
    private static boolean crash = false;

    public static void initialize() {
        String extensions = GlobalResources.getGLExtensions();
        if (extensions.contains("GL_NVX_gpu_memory_info")) {
            // Nvidia
            try {
                graphicsDeviceInfo = new NVIDIAVRAM();
            } catch (Exception e) {
                logger.error(e);
                crash = true;
            }
        } else if (extensions.contains("GL_ATI_meminfo")) {
            // ATI
            try {
                graphicsDeviceInfo = new ATIVRAM();
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
        if(crash)
            return -1;
        if (graphicsDeviceInfo == null) {
            initialize();
        }
        return graphicsDeviceInfo.getFreeMemory();
    }

    public static double getTotalMemory() {
        if(crash)
            return -1;
        if (graphicsDeviceInfo == null) {
            initialize();
        }
        return graphicsDeviceInfo.getTotalMemory();
    }

    public static double getUsedMemory() {
        if(crash)
            return -1;
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
