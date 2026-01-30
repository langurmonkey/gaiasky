monado-clean:
	-killall -9 monado-service vrcompositor vrmonitor survive-cli
	-rm -f /run/user/1000/monado_comp_ipc

monado: monado-clean
	env XRT_COMPOSITOR_FORCE_NVIDIA=0 \
	    XRT_COMPOSITOR_FORCE_WAYLAND_DIRECT=1 \
	    XRT_COMPOSITOR_FORCE_SRGB=1 \
	    SURVIVE_GLOBALSCENESOLVER=0 \
	    monado-service

monado-debug: monado-clean
	env XRT_COMPOSITOR_FORCE_NVIDIA=0 \
	    XRT_COMPOSITOR_FORCE_XCB=0 \
	    XRT_COMPOSITOR_FORCE_WAYLAND_DIRECT=1 \
	    XRT_COMPOSITOR_FORCE_SRGB=1 \
	    XRT_VULKAN_FORMAT=VK_FORMAT_B8G8R8A8_SRGB \
	    XRT_COMPOSITOR_GAMMA=0.45 \
	    SURVIVE_GLOBALSCENESOLVER=0 \
	    monado-service
