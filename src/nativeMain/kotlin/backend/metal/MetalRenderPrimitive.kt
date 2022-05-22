package backend.metal

import backend.Attribute
import backend.HwRenderPrimitive
import platform.Metal.*

class MetalRenderPrimitive : HwRenderPrimitive() {


    // The pointers to MetalVertexBuffer and MetalIndexBuffer are "weak".
    // The MetalVertexBuffer and MetalIndexBuffer must outlive the MetalRenderPrimitive.

    var vertexBuffer: MetalVertexBuffer? = null
    var indexBuffer: MetalIndexBuffer? = null

    // This struct is used to create the pipeline description to describe vertex assembly.
    var vertexDescription = VertexDescription()


    fun setBuffers(vertexBuffer: MetalVertexBuffer, indexBuffer: MetalIndexBuffer) {
        this.vertexBuffer = vertexBuffer
        this.indexBuffer = indexBuffer

        val attributeCount = vertexBuffer.attributes.size

        vertexDescription = VertexDescription()

        // Each attribute gets its own vertex buffer.

        var bufferIndex = 0
        (0 until attributeCount).forEach { attributeIndex ->
            //for (uint32_t attributeIndex = 0 attributeIndex < attributeCount attributeIndex++) {
            val attribute = vertexBuffer.attributes[attributeIndex]
            if (attribute.buffer == Attribute.BUFFER_UNUSED) {
                val flags = attribute.flags
                val format: MTLVertexFormat = if (flags and Attribute.FLAG_INTEGER_TARGET > 0) {
                    MTLVertexFormatUInt4
                } else {
                    MTLVertexFormatFloat4
                }

                // If the attribute is not enabled, bind it to the zero buffer. It's a Metal error for a
                // shader to read from missing vertex attributes.
                vertexDescription.attributes[attributeIndex] = VertexDescription.Attribute(
                    format = format,
                    buffer = ZERO_VERTEX_BUFFER,
                    offset = 0
                )
                vertexDescription.layouts[ZERO_VERTEX_BUFFER] = VertexDescription.Layout(
                    step = MTLVertexStepFunctionConstant,
                    stride = 16
                )
            } else {
                vertexDescription.attributes[attributeIndex] = VertexDescription.Attribute(
                    format = getMetalFormat(
                        attribute.type,
                        (attribute.flags and Attribute.FLAG_NORMALIZED) > 0
                    ),
                    buffer = bufferIndex,
                    offset = 0
                )
                vertexDescription.layouts[bufferIndex] = VertexDescription.Layout(
                    step = MTLVertexStepFunctionPerVertex,
                    stride = attribute.stride
                )

                bufferIndex += 1
            }

        }
    }
}