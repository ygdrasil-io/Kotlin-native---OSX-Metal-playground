package filament

import backend.*

class VertexBuffer(
    attributes: AttributeArray,
    vertexCount: Int,
    bufferCount: Int,
    attributeCount: Int
) : HwVertexBuffer(
    attributes,
    vertexCount,
    bufferCount,
    attributeCount
) {

    val mBufferObjectsEnabled = false

    class AttributeData : Attribute().apply {
        type = ElementType.FLOAT4
    }

    class Builder(
        val mAttributes: Array<AttributeData?> = Array(MAX_VERTEX_ATTRIBUTE_COUNT) { null },
        var mDeclaredAttributes: AttributeData? = null,
        var vertexCount: Int = 0,
        var bufferCount: Int = 0,
        val mBufferObjectsEnabled: Boolean = false
    ) {
        fun attribute(
            attribute: VertexAttribute,
            bufferIndex: Int,
            attributeType: ElementType,
            byteOffset: Int,
            byteStride: ULong = 0u
        ) {

            val attributeSize = getElementTypeSize(attributeType)
            val byteStride = if (byteStride == 0u.toULong()) {
                attributeSize
            } else byteStride

            if (attribute.value < MAX_VERTEX_ATTRIBUTE_COUNT && bufferIndex < MAX_VERTEX_ATTRIBUTE_COUNT) {

                /*#ifndef NDEBUG
                        if (byteOffset & 0x3u) {
                    utils::slog.d < < "[performance] VertexBuffer::Builder::attribute() "
                    "byteOffset not multiple of 4" < < utils ::io::endl;
                }
                if (byteStride and 0x3u) {
                    utils::slog.d < < "[performance] VertexBuffer::Builder::attribute() "
                    "byteStride not multiple of 4" < < utils ::io::endl;
                }
                #endif*/
                mDeclaredAttributes = mAttributes[attribute.value]?.apply {
                    buffer = bufferIndex
                    offset = byteOffset
                    stride = byteStride
                    type = attributeType
                }

            } else {
                println("Ignoring VertexBuffer attribute, the limit of $MAX_VERTEX_ATTRIBUTE_COUNT attributes has been exceeded")
            }

        }


        fun build(engine: Engine): VertexBuffer? {
            if (!(vertexCount > 0)) {
                println("vertexCount cannot be 0")
                return null;
            }
            if (!(bufferCount > 0)) {
                println("bufferCount cannot be 0")
                return null;
            }
            if (!(bufferCount <= MAX_VERTEX_BUFFER_COUNT)) {
                println("bufferCount cannot be more than $MAX_VERTEX_BUFFER_COUNT")
                return null;
            }

            // Next we check if any unused buffer slots have been allocated. This helps prevent errors
            // because uploading to an unused slot can trigger undefined behavior in the backend.
            val declaredAttributes = mDeclaredAttributes
            val attributes = mAttributes
            val attributedBuffers = BitSet()
            (0 until MAX_VERTEX_ATTRIBUTE_COUNT).forEach { j ->
                TODO("Not yet implemented")
                /*if (declaredAttributes[j]) {
                    attributedBuffers.set(attributes[j].buffer)
                }*/
            }
            if (!(attributedBuffers.size == bufferCount)) {
                println("At least one buffer slot was never assigned to an attribute.")
                return null;
            }

            return engine.createVertexBuffer(this);
        }
    }

    fun setBufferAt(engine: Engine, bufferIndex: Int, buffer: BufferDescriptor, byteOffset: Int? = null) {
        assert(!mBufferObjectsEnabled) { error("Please use setBufferObjectAt()") };
        if (bufferIndex < bufferCount) {
            assert(mBufferObjects[bufferIndex]);
            engine.driverApi.updateBufferObject(mBufferObjects[bufferIndex], std::move(buffer), byteOffset);
        } else {
            assert(bufferIndex < bufferCount) { error("bufferIndex must be < bufferCount") };
        }
    }
}