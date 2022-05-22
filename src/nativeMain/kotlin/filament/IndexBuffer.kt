package filament

import backend.BufferDescriptor
import backend.ElementType
import backend.HwIndexBuffer

class IndexBuffer(val mHandle: HwIndexBuffer) {

    /**
     * Type of the index buffer
     */
    enum class IndexType(value: ElementType)  {
        USHORT(ElementType.USHORT),       //!< 16-bit indices
        UINT(ElementType.UINT),           //!< 32-bit indices
    };

    class Builder(
        var indexCount: Int = 0,
        var bufferType: IndexType = IndexType.UINT
    ) {
        fun build(engine: Engine): IndexBuffer {
            return engine.createIndexBuffer(this)
        }

    }

    suspend fun setBuffer(engine: Engine, buffer: BufferDescriptor, byteOffset: Int = 0) {
        engine.driver.updateIndexBuffer(mHandle, buffer, byteOffset);
    }
}