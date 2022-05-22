package backend.metal

import backend.HwRenderTarget

class MetalRenderTarget(
    val metalContext: MetalContext,
    width: UInt,
    height: UInt,
    val samples: Boolean,
    val colorAttachments: Array<Attachment>,
    val depthAttachment: Attachment
) : HwRenderTarget(width, height) {

    class Attachment(
        val metalTexture: MetalTexture? = null,
        val level: UShort = 0u,
        val layer: UInt = 0u
    ) {


    }
}