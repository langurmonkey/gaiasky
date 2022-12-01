Currently none of the desired swapchain formats:

```java
GL_RGB10_A2,
GL_RGBA16F,
// The two below should only be used as a fallback, as they are linear color formats without enough bits for color
// depth, thus leading to banding.
GL_RGBA8,
GL31.GL_RGBA8_SNORM
```
are supported by OpenXR via SteamVR on linux. The supported ones are:

```java
GL_SRGB8,
GL_SRGB8_ALPHA8,
GL_DEPTH_COMPONENT_16,
GL_DEPTH_COMPONENT_24,
GL_DEPTH_COMPONENT_32,
```