package backend.metal

import backend.*
import kotlinx.cinterop.CPointer
import platform.QuartzCore.CAMetalDrawableProtocol


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

