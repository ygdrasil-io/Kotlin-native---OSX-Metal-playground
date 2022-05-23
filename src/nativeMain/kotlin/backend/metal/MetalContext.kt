package backend.metal

import backend.assert_invariant
import platform.CoreVideo.CVMetalTextureCacheRef
import platform.Metal.*


const val MAX_SAMPLE_COUNT: UShort = 8u  // Metal devices support at most 8 MSAA samples


data class HighestSupportedGpuFamily(
    val common: Int,
    val apple: Int,
    val mac: Int
)

data class MetalContext(
    val metalDriver: MetalDriver,
    val device: MTLDeviceProtocol,
    val highestSupportedGpuFamily: HighestSupportedGpuFamily,
    val supportsTextureSwizzling: Boolean,
    val maxColorRenderTargets: Int,
    val sampleCountLookup: List<Boolean>,

) {
    val commandQueue = MetalPlatform.createCommandQueue(device)
    val metalBufferPool = MetalBufferPool(this)
    val metalBlitter = MetalBlitter(this)
    val metalTimerQueryFence = MetalTimerQueryFence(this)
    val pipelineStateCache = PipelineStateCache(device)
    val samplerStateCache = SamplerStateCache(device)
    val depthStencilStateCache = DepthStencilStateCache(device)
    var pendingCommandBuffer: MTLCommandBufferProtocol? = null
    val bufferPool = MetalBufferPool(this)
    var textureCache: CVMetalTextureCacheRef? = null
    var currentRenderPassEncoder: MTLRenderCommandEncoderProtocol? = null

    var currentDrawSwapChain: MetalSwapChain? = null
    var currentReadSwapChain: MetalSwapChain? = null

    fun isInRenderPass(): Boolean {
        return currentRenderPassEncoder != null;
    }

    fun submitPendingCommands() {
        pendingCommandBuffer?.let { pendingCommandBuffer ->
            assert_invariant(pendingCommandBuffer.status != MTLCommandBufferStatusCommitted)
            pendingCommandBuffer.commit()
        }
        pendingCommandBuffer = null
    }
}

fun initializeSupportedGpuFamilies(device: MTLDeviceProtocol): HighestSupportedGpuFamily {

    return HighestSupportedGpuFamily(
        common = when {
            device.supportsFamily(MTLGPUFamilyCommon3) -> 3
            device.supportsFamily(MTLGPUFamilyCommon2) -> 2
            device.supportsFamily(MTLGPUFamilyCommon1) -> 1

            else -> 0
        },
        mac = when {
            device.supportsFamily(MTLGPUFamilyMac2) -> 2
            device.supportsFamily(MTLGPUFamilyMac1) -> 1
            else -> 0
        },
        apple = when {
            device.supportsFamily(MTLGPUFamilyApple7) -> 7
            device.supportsFamily(MTLGPUFamilyApple6) -> 6
            device.supportsFamily(MTLGPUFamilyApple5) -> 5
            device.supportsFamily(MTLGPUFamilyApple4) -> 4
            device.supportsFamily(MTLGPUFamilyApple3) -> 3
            device.supportsFamily(MTLGPUFamilyApple2) -> 2
            device.supportsFamily(MTLGPUFamilyApple1) -> 1
            else -> 0
        }
    )
}

