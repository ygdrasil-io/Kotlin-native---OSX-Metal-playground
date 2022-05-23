package backend.metal

import backend.HwRenderTarget
import backend.MRT

class MetalRenderTarget(
    val metalContext: MetalContext,
    width: UInt,
    height: UInt,
    val samples: Boolean,
    val colorAttachments: Array<Attachment>,
    val depthAttachment: Attachment
) : HwRenderTarget(width, height) {

    var isDefaultRenderTarget = false
    private val color = Array(MRT.MAX_SUPPORTED_RENDER_TARGET_COUNT) { Attachment() }
    private val depth = Attachment()
    private var defaultRenderTarget = false

    class Attachment(
        var metalTexture: MetalTexture? = null,
        val level: UShort = 0u,
        val layer: UInt = 0u
    ) {


    }

    fun getReadColorAttachment(index: Int): Attachment {
        assert(index < MRT.MAX_SUPPORTED_RENDER_TARGET_COUNT)
        val result = color[index];
        if (index == 0 && defaultRenderTarget) {
            result.metalTexture = metalContext.currentReadSwapChain?.acquireDrawable();
        }
        return result;
    }

}