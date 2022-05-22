package backend.metal

import backend.*
import kotlinx.cinterop.CPointer
import platform.QuartzCore.CAMetalDrawableProtocol

class MetalSwapChain: HwSwapChain() {

    private var frameCompletedUserData: Any? = null
    private var frameCompletedCallback: FrameCompletedCallback? = null

    private var frameScheduledUserData: Any? = null
    private var frameScheduledCallback: FrameScheduledCallback? = null


    private var drawable: CAMetalDrawableProtocol? = null

    fun setFrameScheduledCallback(callback: FrameScheduledCallback, user: Any) {
        frameScheduledCallback = callback
        frameScheduledUserData = user

    }

    fun setFrameCompletedCallback(callback: FrameCompletedCallback, user: Any) {
        frameCompletedCallback = callback
        frameCompletedUserData = user

    }

    fun releaseDrawable() {
        drawable = null
    }

}

class MetalVertexBuffer(
    val context: MetalContext,
    bufferCount: Int,
    attributeCount: Int,
    vertexCount: Int,
    attributes: AttributeArray
) : HwVertexBuffer(
    attributes, vertexCount, bufferCount, attributeCount
)

class MetalIndexBuffer(
    context: MetalContext,
    usage: BufferUsage,
    elementSize: ULong,
    indexCount: ULong
) : HwIndexBuffer(
    elementSize, indexCount
) {
    val buffer = MetalBuffer(context, usage, elementSize * indexCount, true)
}

