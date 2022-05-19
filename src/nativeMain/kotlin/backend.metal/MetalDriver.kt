package backend.metal

import platform.Metal.MTLRenderPipelineDescriptor

class MetalDriver {

    private val metalContext: MetalContext

    init {

        val device = MetalPlatform.createDevice()
        val highestSupportedGpuFamily = initializeSupportedGpuFamilies(device)

        println("Supported GPU families: ")
        if (highestSupportedGpuFamily.common > 0) {
            println("  MTLGPUFamilyCommon${highestSupportedGpuFamily.common}")
        }
        if (highestSupportedGpuFamily.apple > 0) {
            println(" MTLGPUFamilyApple${highestSupportedGpuFamily.apple}")
        }
        if (highestSupportedGpuFamily.mac > 0) {
            println("  MTLGPUFamilyMac${highestSupportedGpuFamily.mac}")
        }
        println("Features:")
        println("  readWriteTextureSupport: ${device.readWriteTextureSupport}")

        // In order to support texture swizzling, the GPU needs to support it and the system be running
        // iOS 13+.
        val supportsTextureSwizzling =
            highestSupportedGpuFamily.apple >= 1 ||  // all Apple GPUs
                    highestSupportedGpuFamily.mac >= 2 // newer macOS GPUs

        val maxColorRenderTargets = if (highestSupportedGpuFamily.apple >= 2 || highestSupportedGpuFamily.mac >= 1) {
            8
        } else 4

        val sampleCountLookup = (1 .. MAX_SAMPLE_COUNT).map {sampleCount ->
            device.supportsTextureSampleCount(sampleCount.toULong())
        }

        metalContext = MetalContext(
            this,
            device,
            highestSupportedGpuFamily,
            supportsTextureSwizzling,
            maxColorRenderTargets,
            sampleCountLookup
        ).apply {
            pipelineStateCache.setDevice(device);
            depthStencilStateCache.setDevice(device);
            samplerStateCache.setDevice(device);
        }

    }

}

